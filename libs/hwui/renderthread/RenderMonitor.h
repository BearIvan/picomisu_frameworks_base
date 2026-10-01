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

#pragma once

#include <pthread.h>

#include <string>

#include <binder/IBinder.h>
#include <utils/StrongPointer.h>
#include <utils/Timers.h>

#include "FrameInfo.h"

namespace android {
namespace uirenderer {
namespace renderthread {

class RenderThread;

/**
 * PICO frame monitor of the render thread (factory PICO OS 5.13.7 libhwui).
 *
 * Keeps the last frames drawn by the process, splits them into "operation areas" (runs of frames
 * without a pause longer than three frame intervals), and for each area:
 *  - reports jank runs and the window frame rate to "transferserver" (ITransferServer);
 *  - asks "systransserver" (ISysTransServer) to lower or raise the display refresh rate when the
 *    window frame rate stays low or high.
 * It runs when persist.sys.monitor is set or on userdebug builds, after boot completed.
 */
class RenderMonitor {
public:
    RenderMonitor();

    // Called by CanvasContext::draw after every frame.
    void addFrame(FrameInfo& frame, const std::string& name);
    // Called when the render thread frame interval is set up or the display config changed.
    void setFrameInterval(nsecs_t frameInterval);
    void notifyMonitorStatsChanged(bool enabled);
    // An animation of |durationMs| starts: analyse its frames once it is over.
    void doAnimation(long durationMs);

private:
    static constexpr size_t kMaxFrames = 120;
    static constexpr size_t kMaxFpsSamples = 10;
    static constexpr int kMaxAvailableFps = 8;
    static constexpr int kStageCount = 7;
    static constexpr int kFrameInfoSize = static_cast<int>(FrameInfoIndex::NumIndexes);

    // The i-th oldest frame / window name of the current history.
    const int64_t* frameAt(size_t i) const {
        return mFrames[(i + 1 + mFrameIndex) % mFrameCount];
    }
    const std::string& nameAt(size_t i) const {
        return mNames[(i + 1 + mNameIndex) % mNameCount];
    }

    void loadConfig();
    void initParameter();
    void updateStageThresholds();
    void updateFrameIntervalFromDisplayType();
    void resetFpsHistory();
    void scanOperationArea();
    void doAnalysis(size_t start, size_t end);
    void reportAlarm(size_t start, size_t end, int64_t duration, int64_t total, int64_t value,
                     size_t type, int windowChanged);
    void confirmCurrentFps();
    void requestChangeDisplayFps(int fpsIndex);
    void postMonitorTaskIfNeeded(nsecs_t runAt);
    void postAnimatorTask(nsecs_t runAt);

    // Frame history (FrameInfo copies) and the window name of each frame.
    int64_t mFrames[kMaxFrames][kFrameInfoSize];
    int mFrameIndex;
    size_t mFrameCount;
    std::string mNames[kMaxFrames];
    int mNameIndex;
    size_t mNameCount;

    // Window frame rate of the last operation areas.
    double mFpsSamples[kMaxFpsSamples];
    int mFpsSampleIndex;
    size_t mFpsSampleCount;

    // Duration of the last frames.
    int64_t mFrameDurations[kMaxFrames];
    int mFrameDurationIndex;
    size_t mFrameDurationCount;

    nsecs_t mFrameInterval;
    // Allowed duration of each frame stage (see kStages in RenderMonitor.cpp).
    nsecs_t mStageThresholds[kStageCount];
    // Refresh rates the display supports (fps_config), lowest first, and their frame intervals.
    float mAvailableFps[kMaxAvailableFps];
    nsecs_t mAvailableFrameIntervals[kMaxAvailableFps];

    sp<IBinder> mTransferServer;
    sp<IBinder> mSysTransServer;
    RenderThread* mRenderThread;
    bool mMonitorTaskPosted;
    bool mAnimating;
    int mJankCount;
    nsecs_t mMaxJankDuration;
    bool mEnabled;
    nsecs_t mLastTraceDumpTime;
    int mJankThreshold;
    int mCurrentFpsIndex;
    int mAvailableFpsCount;
    pthread_mutex_t mLock;

    // Calculation config (ISysTransServer.getCalculationConfig), in transaction order.
    int mHighFpsThreshold;
    int mMidFpsThreshold;
    int mLowFpsThreshold;
    int mHighFpsScoreThreshold;
    int mLowFpsScoreThreshold;
    int mSlowFrameRatio;
    int mUnstableFrameRatio;
    int mMinStableFrames;
    int mFpsDeviationThreshold;
    int mFpsDropThreshold;
    float mSevereJankRatio;

    bool mBootCompleted;
    bool mFirstScan;
    bool mFrameIntervalChanging;
    bool mCheckFrameInterval;
    nsecs_t mFirstFrameVsync;
    int mLogControl;
};

} /* namespace renderthread */
} /* namespace uirenderer */
} /* namespace android */
