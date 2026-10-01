/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "RenderMonitor.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <algorithm>
#include <vector>

#include <binder/IServiceManager.h>
#include <binder/Parcel.h>
#include <cutils/properties.h>
#include <log/log.h>
#include <utils/String16.h>
#include <utils/Trace.h>

#include "RenderThread.h"

namespace android {
namespace uirenderer {
namespace renderthread {

namespace {

constexpr char kTransferServerDescriptor[] = "com.android.internal.app.ITransferServer";
constexpr char kSysTransServerDescriptor[] = "com.android.internal.app.ISysTransServer";

// ITransferServer transactions.
constexpr uint32_t TRANSACTION_REPORT_JANK = 1;
constexpr uint32_t TRANSACTION_REPORT_JUNK = 4;  // "REPORT_JUNK_TRANSACTION"
constexpr uint32_t TRANSACTION_REPORT_WINDOW_FPS = 5;
// ISysTransServer transactions.
constexpr uint32_t TRANSACTION_REQUEST_CHANGE_DISPLAY_FPS = 1;
constexpr uint32_t TRANSACTION_GET_CALCULATION_CONFIG = 4;

constexpr nsecs_t kTraceDumpInterval = 10000000001;  // 10 s

// Frame stages weighed by reportAlarm to tell why a frame was late: FrameInfo indices of the
// stage start and end, and the stage weight.
struct Stage {
    int start;
    int end;
    int weight;
};
constexpr Stage kStages[] = {
        {static_cast<int>(FrameInfoIndex::IntendedVsync), static_cast<int>(FrameInfoIndex::Vsync),
         150},
        {static_cast<int>(FrameInfoIndex::HandleInputStart),
         static_cast<int>(FrameInfoIndex::AnimationStart), 100},
        {static_cast<int>(FrameInfoIndex::AnimationStart),
         static_cast<int>(FrameInfoIndex::PerformTraversalsStart), 100},
        {static_cast<int>(FrameInfoIndex::PerformTraversalsStart),
         static_cast<int>(FrameInfoIndex::SyncQueued), 100},
        {static_cast<int>(FrameInfoIndex::SyncStart),
         static_cast<int>(FrameInfoIndex::IssueDrawCommandsStart), 75},
        {static_cast<int>(FrameInfoIndex::IssueDrawCommandsStart),
         static_cast<int>(FrameInfoIndex::SwapBuffers), 75},
        {static_cast<int>(FrameInfoIndex::SwapBuffers),
         static_cast<int>(FrameInfoIndex::FrameCompleted), 75},
};

constexpr int kIntendedVsync = static_cast<int>(FrameInfoIndex::IntendedVsync);
constexpr int kVsync = static_cast<int>(FrameInfoIndex::Vsync);
constexpr int kIssueDrawCommandsStart = static_cast<int>(FrameInfoIndex::IssueDrawCommandsStart);
constexpr int kFrameCompleted = static_cast<int>(FrameInfoIndex::FrameCompleted);
constexpr int kFrameCompletedWallTime = static_cast<int>(FrameInfoIndex::FrameCompletedWallTime);

}  // namespace

// persist.sys.monitor.window / .fps / .systrace: trace dumps for one window.
static char* sCheckFPSWindow = nullptr;
static int sCheckFPSWindowLen = 0;
static int sCheckFPSThreshold = 55;
static int sMonitorSystrace = 0;

RenderMonitor::RenderMonitor()
        : mFrameIndex(-1)
        , mFrameCount(0)
        , mNameIndex(-1)
        , mNameCount(0)
        , mFpsSampleIndex(-1)
        , mFpsSampleCount(0)
        , mFrameDurationIndex(-1)
        , mFrameDurationCount(0)
        , mFrameInterval(16666666)
        , mRenderThread(&RenderThread::getInstance())
        , mMonitorTaskPosted(false)
        , mAnimating(false)
        , mJankCount(0)
        , mMaxJankDuration(0)
        , mEnabled(false)
        , mLastTraceDumpTime(0)
        , mJankThreshold(3)
        , mCurrentFpsIndex(0)
        , mAvailableFpsCount(0)
        , mFirstScan(false)
        , mCheckFrameInterval(false)
        , mFirstFrameVsync(0)
        , mLogControl(0) {
    pthread_mutex_init(&mLock, nullptr);
    mBootCompleted = true;
    mFrameIntervalChanging = false;
    ALOGD("RenderMonitor init!");
    if (property_get_int32("sys.boot_completed", 0) == 0) {
        mBootCompleted = false;
        return;
    }

    int monitor = property_get_int32("persist.sys.monitor", 0);
    char value[128];
    property_get("ro.build.type", value, "");
    if (!(monitor & 1) && memcmp(value, "userdebug", sizeof("userdebug")) != 0) {
        ALOGD("RenderMonitor closed!");
    } else {
        int length = property_get("persist.sys.monitor.window", value, nullptr);
        sMonitorSystrace = property_get_int32("persist.sys.monitor.systrace", 0);
        if (length > 0) {
            sCheckFPSWindow = static_cast<char*>(malloc(length + 1));
            strncpy(sCheckFPSWindow, value, length);
            sCheckFPSWindow[length] = '\0';
            sCheckFPSWindowLen = length;
            sCheckFPSThreshold = property_get_int32("persist.sys.monitor.fps", 55);
            ALOGD("RenderMonitor sCheckFPSWindow: %s, sCheckFPSThreshold: %d, "
                  "sCheckFPSWindowLen: %d",
                  sCheckFPSWindow, sCheckFPSThreshold, sCheckFPSWindowLen);
        }
        mEnabled = true;
        mTransferServer = defaultServiceManager()->getService(String16("transferserver"));
        mSysTransServer = defaultServiceManager()->getService(String16("systransserver"));
        mJankThreshold = property_get_int32("persist.sys.monitor.jank_threshold", 3);
        if (mTransferServer != nullptr) {
            ALOGD("RenderMonitor init completed!");
        } else {
            ALOGE("RenderMonitor get transfer service failed!");
        }
    }

    mLastTraceDumpTime = 0;
    mMaxJankDuration = 0;
    mFirstScan = true;
    loadConfig();
    updateStageThresholds();
    initParameter();
    mCheckFrameInterval = false;
    mLogControl = property_get_int32("persist.sys.monitor.log.control", 0);
}

// Supported refresh rates from /system/etc/fps_config ("<fps> <fps> ..."); 72/90/120 without it.
void RenderMonitor::loadConfig() {
    char line[256] = {};
    char path[256];
    sprintf(path, "/system/etc/fps_config");
    FILE* file = fopen(path, "r");
    if (file == nullptr) {
        mAvailableFps[0] = 72.0f;
        mAvailableFps[1] = 90.0f;
        mAvailableFps[2] = 120.0f;
        mCurrentFpsIndex = 2;
        mAvailableFpsCount = 3;
    } else {
        fgets(line, sizeof(line), file);
        fclose(file);
        int count = 0;
        char* token = strtok(line, " ");
        while (token != nullptr) {
            float fps = atof(token);
            mAvailableFps[count] = fps;
            count++;
            if (fps == static_cast<float>(count)) {
                ALOGD("RenderMonitor read fps config completed!");
            }
            token = strtok(nullptr, " ");
            ALOGD("RenderMonitor mAvailableFps %f", fps);
        }
        mAvailableFpsCount = count - 1;
        mCurrentFpsIndex = count - 2;
        ALOGD("RenderMonitor loadConfig %d, %d", count - 1, count - 2);
    }
    for (int i = 0; i < mAvailableFpsCount; i++) {
        mAvailableFrameIntervals[i] = static_cast<nsecs_t>(1000000000.0f / mAvailableFps[i]);
    }
}

// Calculation config: defaults, replaced by ISysTransServer.getCalculationConfig() (11 ints).
void RenderMonitor::initParameter() {
    mHighFpsThreshold = 85;
    mMidFpsThreshold = 70;
    mLowFpsThreshold = 55;
    mHighFpsScoreThreshold = 0;
    mLowFpsScoreThreshold = 0;
    mSlowFrameRatio = 1;
    mUnstableFrameRatio = 1;
    mMinStableFrames = 10;
    mFpsDeviationThreshold = 10;
    mFpsDropThreshold = 60;
    mSevereJankRatio = 3.0f;
    if (mSysTransServer == nullptr) {
        return;
    }
    Parcel data, reply;
    data.writeInterfaceToken(String16(kSysTransServerDescriptor));
    mSysTransServer->transact(TRANSACTION_GET_CALCULATION_CONFIG, data, &reply, 0);
    reply.readExceptionCode();
    std::vector<int32_t> config;
    reply.readInt32Vector(&config);
    if (config.size() == 11) {
        mHighFpsThreshold = config[0];
        mMidFpsThreshold = config[1];
        mLowFpsThreshold = config[2];
        mHighFpsScoreThreshold = config[3];
        mLowFpsScoreThreshold = config[4];
        mSlowFrameRatio = config[5];
        mUnstableFrameRatio = config[6];
        mMinStableFrames = config[7];
        mFpsDeviationThreshold = config[8];
        mFpsDropThreshold = config[9];
        mSevereJankRatio = static_cast<float>(config[10]);
    }
}

void RenderMonitor::updateStageThresholds() {
    mStageThresholds[0] = mFrameInterval;
    mStageThresholds[1] = static_cast<nsecs_t>(mFrameInterval * 0.15);
    mStageThresholds[2] = mStageThresholds[1];
    mStageThresholds[3] = mStageThresholds[1];
    mStageThresholds[4] = static_cast<nsecs_t>(mFrameInterval * 0.2);
    mStageThresholds[5] = static_cast<nsecs_t>(mFrameInterval * 0.55);
    mStageThresholds[6] = mStageThresholds[4];
}

// The PICO display type (sys.pvr.display.type) is the refresh rate the VR runtime picked.
void RenderMonitor::updateFrameIntervalFromDisplayType() {
    char value[PROPERTY_VALUE_MAX];
    property_get("sys.pvr.display.type", value, "72");
    mFrameInterval = 1000000000 / atoi(value);
    for (int i = 0; i < mAvailableFpsCount; i++) {
        if (mFrameInterval / 10 == mAvailableFrameIntervals[i] / 10) {
            mCurrentFpsIndex = i;
            break;
        }
    }
}

void RenderMonitor::resetFpsHistory() {
    mFpsSampleCount = 0;
    mFpsSampleIndex = -1;
    mFrameDurationCount = 0;
    mFrameDurationIndex = -1;
}

void RenderMonitor::addFrame(FrameInfo& frame, const std::string& name) {
    if (mTransferServer == nullptr || !mEnabled) {
        return;
    }
    frame.set(FrameInfoIndex::FrameCompletedWallTime) = systemTime(SYSTEM_TIME_REALTIME);

    mFrameIndex = (mFrameIndex + 1) % kMaxFrames;
    if (mFrameCount < kMaxFrames) {
        mFrameCount++;
    }
    memcpy(mFrames[mFrameIndex], frame.data(), sizeof(mFrames[0]));
    mNameIndex = (mNameIndex + 1) % kMaxFrames;
    if (mNameCount < kMaxFrames) {
        mNameCount++;
    }
    mNames[mNameIndex] = name;

    const size_t count = mFrameCount;
    if (mCheckFrameInterval) {
        mCheckFrameInterval = false;
        nsecs_t drift = frame[FrameInfoIndex::IntendedVsync] - mFirstFrameVsync - mFrameInterval;
        if (llabs(drift) >= 3) {
            updateFrameIntervalFromDisplayType();
        }
    }
    if (count == kMaxFrames) {
        scanOperationArea();
        return;
    }
    if (count == 1) {
        mCheckFrameInterval = true;
        mFirstFrameVsync = frame[FrameInfoIndex::IntendedVsync];
    }
    postMonitorTaskIfNeeded(frame[FrameInfoIndex::FrameCompleted] + 1000000000);
}

// Analyses the frames once no frame was drawn for three frame intervals.
// The factory defines this and postAnimatorTask inline (no out-of-line copy; the posted
// lambdas are mangled as {lambda()#1} of these members).
inline void RenderMonitor::postMonitorTaskIfNeeded(nsecs_t runAt) {
    if (mMonitorTaskPosted) {
        return;
    }
    mMonitorTaskPosted = true;
    mRenderThread->queue().postAt(runAt, [this]() {
        mMonitorTaskPosted = false;
        if (mFrameCount == 0) {
            return;
        }
        nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
        const int64_t* last = mFrames[(mFrameCount + mFrameIndex) % mFrameCount];
        if (now - last[kFrameCompleted] > 3 * mFrameInterval) {
            scanOperationArea();
            return;
        }
        postMonitorTaskIfNeeded(now + 1000000000);
    });
}

inline void RenderMonitor::postAnimatorTask(nsecs_t runAt) {
    mRenderThread->queue().postAt(runAt, [this]() {
        ATRACE_NAME("scanOperationAreaForAnimator");
        scanOperationArea();
        mAnimating = false;
    });
}

void RenderMonitor::setFrameInterval(nsecs_t frameInterval) {
    mFrameIntervalChanging = true;
    scanOperationArea();
    mFrameIntervalChanging = false;
    if (!mBootCompleted) {
        mFrameInterval = frameInterval;
        return;
    }
    updateFrameIntervalFromDisplayType();
    resetFpsHistory();
}

void RenderMonitor::notifyMonitorStatsChanged(bool enabled) {
    if (mEnabled == enabled) {
        return;
    }
    ALOGD("RenderMonitor notifyMonitorStatsChanged %d", enabled);
    mEnabled = enabled;
    if (!enabled || mTransferServer != nullptr) {
        return;
    }
    mTransferServer = defaultServiceManager()->getService(String16("transferserver"));
    mJankThreshold = property_get_int32("persist.sys.monitor.jank_threshold", 3);
    loadConfig();
    updateStageThresholds();
    initParameter();
}

void RenderMonitor::doAnimation(long durationMs) {
    ATRACE_CALL();
    nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
    if (mAnimating) {
        return;
    }
    mAnimating = true;
    mFrameCount = 0;
    mNameCount = 0;
    mFrameIndex = -1;
    mNameIndex = -1;
    postAnimatorTask(now + durationMs * 1000000);
}

// Splits the frame history at pauses longer than three frame intervals and analyses each part.
void RenderMonitor::scanOperationArea() {
    size_t count = mFrameCount;
    if (count == 0) {
        return;
    }
    if (count == 1) {
        doAnalysis(0, 0);
    } else {
        size_t start = 0;
        size_t i;
        for (i = 1; i < count; i++) {
            nsecs_t pause = frameAt(i)[kIntendedVsync] - frameAt(i - 1)[kFrameCompleted];
            if (pause > 3 * mFrameInterval) {
                doAnalysis(start, i - 1);
                count = mFrameCount;
                start = i;
            }
        }
        doAnalysis(start, i - 1);
    }
    mFirstScan = false;
    mFrameCount = 0;
    mFrameIndex = -1;
    mNameCount = 0;
    mNameIndex = -1;
}

void RenderMonitor::doAnalysis(size_t start, size_t end) {
    pthread_mutex_lock(&mLock);
    const char* startName = nameAt(start).c_str();
    const char* endName = nameAt(end).c_str();
    const int windowChanged = strcmp(startName, endName);
    const bool fullHistory = start == 0 && end == kMaxFrames - 1;
    const int count = static_cast<int>(end - start) + 1;
    double fps[static_cast<uint32_t>(count)];

    int64_t jankTotal = 0;
    int severeJanks = 0;
    if (start <= end) {
        const int64_t total = frameAt(end)[kFrameCompleted] - frameAt(start)[kIntendedVsync];
        // The current run of janky frames: [jankStart, jankEnd], kMaxFrames when there is none.
        size_t jankStart = kMaxFrames;
        size_t jankEnd = 0;
        int64_t maxDuration = 0;
        int64_t score = 0;
        for (size_t i = start; i <= end; i++) {
            const int64_t* frame = frameAt(i);
            int64_t begin = frame[kIntendedVsync];
            if (i > start) {
                begin = std::max(begin, frameAt(i - 1)[kIssueDrawCommandsStart]);
            }
            const int64_t duration = frame[kFrameCompleted] - begin;
            const float ratio = static_cast<float>(duration) / static_cast<float>(mFrameInterval);
            if (ratio >= 2.0f) {
                if (jankStart == kMaxFrames) {
                    jankStart = i;
                }
                if (duration > maxDuration) {
                    maxDuration = duration;
                }
                if (!(ratio < mSevereJankRatio)) {
                    severeJanks++;
                }
                score = static_cast<int64_t>(ratio * 3.0f + static_cast<float>(score));
                jankEnd = i;
            } else if (jankStart < kMaxFrames) {
                if (i - jankEnd >= 3) {
                    jankTotal = frameAt(jankEnd)[kFrameCompleted] - frameAt(jankStart)[kIntendedVsync];
                    reportAlarm(jankStart, jankEnd, maxDuration, total,
                                score + 3 * static_cast<int64_t>(i - 1 - jankEnd), 0,
                                windowChanged);
                    jankStart = kMaxFrames;
                    jankEnd = 0;
                    maxDuration = 0;
                    score = 0;
                } else if (ratio > 1.0f) {
                    score = static_cast<int64_t>(static_cast<float>(score) - ratio);
                } else {
                    score -= 3;
                }
            }

            mFrameDurationIndex = (mFrameDurationIndex + 1) % kMaxFrames;
            if (mFrameDurationCount < kMaxFrames) {
                mFrameDurationCount++;
            }
            mFrameDurations[mFrameDurationIndex] = duration;
            if (fullHistory) {
                fps[i] = 1000000000.0 / static_cast<double>(std::max(duration, mFrameInterval));
            }
        }
        if (jankStart < kMaxFrames) {
            reportAlarm(jankStart, jankEnd, maxDuration, total,
                        3 * static_cast<int64_t>(end - jankEnd) + score, 0, windowChanged);
        }
    }

    // Trace dump requests (persist.sys.monitor.systrace bits: 1 animation, 2 jank of the
    // persist.sys.monitor.window window, 4 jank of any window), at most every 10 s.
    const int jankCount = mJankCount;
    bool dump = false;
    if (sCheckFPSWindow != nullptr &&
        ((jankCount >= 1 && (sMonitorSystrace & 2)) || ((sMonitorSystrace & 1) && mAnimating)) &&
        strncmp(sCheckFPSWindow, endName, sCheckFPSWindowLen) == 0) {
        dump = true;
    } else if (jankCount >= 1 && (sMonitorSystrace & 4)) {
        dump = true;
    }
    if (dump) {
        ALOGD("RenderMonitor dump atrace begin!");
        const nsecs_t endTime = frameAt(end)[kFrameCompleted];
        const nsecs_t sinceLastDump = endTime - mLastTraceDumpTime;
        if (sinceLastDump < kTraceDumpInterval) {
            ALOGD("RenderMonitor dump atrace faile n: %d, time %lld!", count,
                  static_cast<long long>(sinceLastDump));
        } else {
            const nsecs_t total = endTime - frameAt(start)[kIntendedVsync];
            nsecs_t average = total / count;
            if (average < mFrameInterval) {
                average = mFrameInterval;
            }
            const int64_t windowFps = 1000000000 / average;
            ALOGD("RenderMonitor n: %d, fps: %d, total: %lld", count, static_cast<int>(windowFps),
                  static_cast<long long>(total));
            if (sCheckFPSThreshold > windowFps || mJankCount >= 1) {
                reportAlarm(start, end, jankTotal, mMaxJankDuration, static_cast<int>(windowFps),
                            1, windowChanged);
                mLastTraceDumpTime = endTime;
            }
        }
    }
    mJankCount = 0;
    mMaxJankDuration = 0;

    const nsecs_t total = frameAt(end)[kFrameCompleted] - frameAt(start)[kIntendedVsync];
    nsecs_t average = total / count;
    if (average < mFrameInterval) {
        average = mFrameInterval;
    }
    const double windowFps = 1000000000.0 / static_cast<double>(average);
    const std::string windowName = nameAt(start);
    const int pid = getpid();
    if (mLogControl != 0) {
        ALOGD("RenderMonitor 2DAPP WindowName: %s, pid:%d, FPS:%f", windowName.c_str(), pid,
              windowFps);
    }
    if (mTransferServer != nullptr) {
        Parcel data, reply;
        data.writeInterfaceToken(String16(kTransferServerDescriptor));
        data.writeInt32(pid);
        data.writeDouble(windowFps);
        data.writeString16(String16(windowName.c_str()));
        data.writeInt32(mCurrentFpsIndex);
        data.writeInt64(total);
        data.writeInt32(0);
        mTransferServer->transact(TRANSACTION_REPORT_WINDOW_FPS, data, &reply,
                                  IBinder::FLAG_ONEWAY);
    }

    if (!mFrameIntervalChanging) {
        // At the highest refresh rate, a full history that stays below mFpsDropThreshold
        // without many faster frames lowers the refresh rate right away.
        if (fullHistory && mCurrentFpsIndex == mAvailableFpsCount - 1 &&
            windowFps < static_cast<double>(mFpsDropThreshold)) {
            bool unstable = false;
            if (count >= 2) {
                const int limit = count / 10;
                int deviations = 0;
                for (int i = 1; i < count; i++) {
                    if (fps[i] - windowFps > static_cast<double>(mFpsDeviationThreshold)) {
                        deviations++;
                        if (deviations >= mUnstableFrameRatio * limit) {
                            unstable = true;
                            break;
                        }
                    }
                }
            }
            if (!unstable) {
                requestChangeDisplayFps(mCurrentFpsIndex - 1);
                resetFpsHistory();
                pthread_mutex_unlock(&mLock);
                return;
            }
        }
        if (count - severeJanks > mMinStableFrames) {
            mFpsSampleIndex = (mFpsSampleIndex + 1) % kMaxFpsSamples;
            if (mFpsSampleCount < kMaxFpsSamples) {
                mFpsSampleCount++;
            }
            mFpsSamples[mFpsSampleIndex] = windowFps;
        }
        if (mFpsSampleCount == kMaxFpsSamples) {
            confirmCurrentFps();
        }
    }
    pthread_mutex_unlock(&mLock);
}

// type 0: a run of janky frames [start, end] (value: jank score, 0 or less is not reported);
// type 1: a trace dump request for the window (value: window frame rate).
void RenderMonitor::reportAlarm(size_t start, size_t end, int64_t duration, int64_t total,
                                int64_t value, size_t type, int windowChanged) {
    const int64_t elapsed = frameAt(end)[kFrameCompleted] - frameAt(start)[kIntendedVsync];
    if (type == 0) {
        if (end < start || value < 1 || elapsed < mFrameInterval * mJankThreshold) {
            return;
        }
    }
    const int flag = mFirstScan ? 1 : windowChanged;
    const int pid = getpid();
    const std::string name = nameAt(start);

    // The frame stage that went over its threshold the most (7: none).
    int64_t stageScore[kStageCount] = {};
    if (start <= end) {
        for (size_t i = start; i <= end; i++) {
            const int64_t* frame = frameAt(i);
            int64_t begin = frame[kIntendedVsync];
            if (i != start) {
                begin = std::max(begin, frameAt(i - 1)[kIssueDrawCommandsStart]);
            }
            const float ratio = static_cast<float>(frame[kFrameCompleted] - begin) /
                    static_cast<float>(mFrameInterval);
            if (!(ratio > 2.0f)) {
                continue;
            }
            for (int j = 0; j < kStageCount; j++) {
                int64_t stage = frame[kStages[j].start] > 0
                        ? frame[kStages[j].end] - frame[kStages[j].start]
                        : 0;
                if (i == 0) {
                    stage = frameAt(0)[kVsync] - begin;
                } else if (stage < 0) {
                    stage = 0;
                }
                stage -= mStageThresholds[j];
                stageScore[j] += stage > 0 ? stage * kStages[j].weight : 0;
            }
        }
    }
    int reason = 0;
    for (int j = 1; j < kStageCount; j++) {
        if (stageScore[j] > stageScore[reason]) {
            reason = j;
        }
    }
    if (stageScore[reason] < 1) {
        reason = kStageCount;
    }

    // Jank level: frame intervals the run took, from 6 down to the jank threshold.
    int level;
    if (mJankThreshold >= 6) {
        level = mJankThreshold;
    } else {
        const double frames = static_cast<double>(elapsed) / static_cast<double>(mFrameInterval);
        level = 6;
        while (!(frames > static_cast<double>(level))) {
            if (level <= mJankThreshold) {
                level = mJankThreshold;
                break;
            }
            level--;
        }
    }

    if (mTransferServer == nullptr) {
        return;
    }
    Parcel data, reply;
    data.writeInterfaceToken(String16(kTransferServerDescriptor));
    uint32_t code;
    if (type == 1) {
        data.writeInt32(pid);
        data.writeInt32(static_cast<int32_t>(value));
        data.writeInt32(flag);
        data.writeInt64(duration);
        data.writeInt64(total);
        data.writeInt64(frameAt(end)[kFrameCompletedWallTime]);
        data.writeString16(String16(name.c_str()));
        ALOGD("RenderMonitor REPORT_JUNK_TRANSACTION, transCode: %d, pid:%d, fps: %d, "
              "total: %lld",
              static_cast<int>(TRANSACTION_REPORT_JUNK), pid, static_cast<int>(value),
              static_cast<long long>(duration));
        code = TRANSACTION_REPORT_JUNK;
    } else if (type == 0) {
        mJankCount++;
        if (elapsed > mMaxJankDuration) {
            mMaxJankDuration = elapsed;
        }
        data.writeInt32(pid);
        data.writeInt32(0);
        data.writeInt32(reason);
        data.writeInt32(flag);
        data.writeInt32(level);
        data.writeInt64(duration);
        data.writeInt64(elapsed);
        data.writeInt64(frameAt(end)[kFrameCompletedWallTime]);
        data.writeString16(String16(name.c_str()));
        data.writeInt32(mCurrentFpsIndex);
        code = TRANSACTION_REPORT_JANK;
    } else {
        code = 0;
    }
    mTransferServer->transact(code, data, &reply, IBinder::FLAG_ONEWAY);
}

// Called with ten window frame rate samples: raise or lower the refresh rate by one step.
void RenderMonitor::confirmCurrentFps() {
    const int current = mCurrentFpsIndex;
    const int count = static_cast<int>(mFpsSampleCount);
    double samples[static_cast<uint32_t>(mFpsSampleCount)];
    for (int i = 0; i < count; i++) {
        samples[i] = mFpsSamples[(mFpsSampleIndex + 1 + i) % mFpsSampleCount];
    }
    std::sort(samples, samples + count);

    int target = current;
    if (current == mAvailableFpsCount - 1) {
        int score = 0;
        for (int i = 0; i < count; i++) {
            if (samples[i] >= static_cast<double>(mHighFpsThreshold)) {
                score += 3;
            } else {
                score += samples[i] < static_cast<double>(mMidFpsThreshold) ? -1 : 1;
            }
        }
        target = current - (score < mHighFpsScoreThreshold ? 1 : 0);
    } else if (current == 0) {
        bool allAbove = true;
        for (int i = 0; i < count; i++) {
            if (samples[i] <= static_cast<double>(mLowFpsThreshold)) {
                allAbove = false;
                break;
            }
        }
        if (!allAbove) {
            target = 0;
        } else {
            int slowFrames = 0;
            const int durations = static_cast<int>(mFrameDurationCount) - 1;
            for (size_t i = 0; i < static_cast<size_t>(durations); i++) {
                if (mFrameDurations[(mFrameDurationIndex + 1 + i) % mFrameDurationCount] >
                    mAvailableFrameIntervals[1]) {
                    slowFrames++;
                }
            }
            const int vote =
                    slowFrames > mSlowFrameRatio * (static_cast<int>(mFrameDurationCount) / 10)
                    ? -1
                    : 1;
            target = vote > mLowFpsScoreThreshold ? 1 : 0;
        }
    }
    if (target != current) {
        requestChangeDisplayFps(target);
    }
    resetFpsHistory();
}

void RenderMonitor::requestChangeDisplayFps(int fpsIndex) {
    if (mSysTransServer == nullptr) {
        return;
    }
    Parcel data, reply;
    data.writeInterfaceToken(String16(kSysTransServerDescriptor));
    data.writeInt32(getpid());
    data.writeInt32(fpsIndex);
    data.writeInt32(0);
    mSysTransServer->transact(TRANSACTION_REQUEST_CHANGE_DISPLAY_FPS, data, &reply,
                              IBinder::FLAG_ONEWAY);
}

} /* namespace renderthread */
} /* namespace uirenderer */
} /* namespace android */
