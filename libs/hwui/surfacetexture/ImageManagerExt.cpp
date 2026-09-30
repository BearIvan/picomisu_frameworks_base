// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
//
// PICO ImageManagerExt, reconstructed from the factory libhwui (PICO OS 5.13.7).

#undef LOG_TAG
#define LOG_TAG "ImageManagerExt"

#include "ImageManagerExt.h"

#include <cutils/properties.h>
#include <processgroup/sched_policy.h>
#include <pthread.h>
#include <unistd.h>
#include <utils/Log.h>

#include <algorithm>

namespace android {

ANDROID_SINGLETON_STATIC_INSTANCE(ImageManagerExt);

bool ImageManagerExt::mHasBeenInit = false;
bool ImageManagerExt::mDebug = false;
bool ImageManagerExt::mDebugPerformance = false;

ImageManagerExt::ImageManagerExt() {
    // bit 0: verbose ImageManagerExt logs, bit 2: SurfaceTexture latency logs.
    int debug = property_get_int32("persist.pvr.debug.image_manager", 0);
    if (debug & 1) {
        mDebug = true;
    }
    if (debug & 4) {
        mDebugPerformance = true;
    }
}

ImageManagerExt::~ImageManagerExt() {
    ALOGE("ImageManagerExt ExitThread");
    mMutex.lock();
    mRunning = false;
    mMutex.unlock();
    mCondition.notify_all();
    if (mThread.joinable()) {
        mThread.join();
    }
}

int ImageManagerExt::initThread() {
    mMutex.lock();
    if (mRunning) {
        int tid = mThreadId;
        mMutex.unlock();
        return tid;
    }
    mRunning = true;
    mMutex.unlock();

    ALOGI("ImageManagerExt initThread begin");
    mHasBeenInit = true;
    checkAndUpdateEglState();

    mThread = std::thread([this]() { threadMain(); });
    pthread_setname_np(mThread.native_handle(), "ImageManagerExt");

    {
        std::unique_lock<std::mutex> lock(mMutex);
        if (mThreadId != -1) {
            ALOGI("ImageManagerExt initThread: %d ", mThreadId);
            return mThreadId;
        }
        mThreadInitCondition.wait(lock);
    }
    ALOGI("ImageManagerExt initThread %d ", mThreadId);
    return mThreadId;
}

status_t ImageManagerExt::checkAndUpdateEglState() {
    EGLDisplay dpy = eglGetCurrentDisplay();
    if (mEglDisplay == EGL_NO_DISPLAY) {
        mEglDisplay = dpy;
    }
    if (mEglDisplay != dpy || dpy == EGL_NO_DISPLAY) {
        return INVALID_OPERATION;
    }
    mEglDisplay = dpy;
    return NO_ERROR;
}

void ImageManagerExt::addImageManagerListener(ImageManagerListener* listener) {
    std::lock_guard<std::mutex> lock(mListenerMutex);
    mListeners.push_back(listener);
    if (mDebug) {
        ALOGI("ImageManagerExt addImageManagerListener %d", listener->getClientId());
    }
}

void ImageManagerExt::removeImageManagerListener(ImageManagerListener* listener) {
    std::lock_guard<std::mutex> lock(mListenerMutex);
    auto it = std::find(mListeners.begin(), mListeners.end(), listener);
    if (it != mListeners.end()) {
        if (mDebug) {
            ALOGI("ImageManagerExt removeImageManagerListener %d", listener->getClientId());
        }
        mListeners.erase(it);
    }
}

void ImageManagerExt::cacheAsync(int clientId, const sp<GraphicBuffer>& buffer,
                                 const std::shared_ptr<Barrier>& barrier) {
    if (buffer == nullptr) {
        // As in the factory, a null buffer completes the barrier immediately (the barrier
        // must then be non-null).
        barrier->lock.lock();
        barrier->done = true;
        barrier->result = BAD_VALUE;
        barrier->lock.unlock();
        barrier->condition.notify_one();
        return;
    }
    QueueEntry entry;
    entry.operation = CACHE;
    entry.clientId = clientId;
    entry.buffer = buffer;
    entry.bufferId = buffer->getId();
    entry.barrier = barrier;
    queueOperation(std::move(entry));
}

void ImageManagerExt::releaseAsync(int clientId, uint64_t bufferId,
                                   const std::shared_ptr<Barrier>& barrier) {
    QueueEntry entry;
    entry.operation = RELEASE;
    entry.clientId = clientId;
    entry.bufferId = bufferId;
    entry.barrier = barrier;
    queueOperation(std::move(entry));
}

void ImageManagerExt::queueOperation(const QueueEntry&& entry) {
    mMutex.lock();
    mQueue.emplace_back(entry);
    mMutex.unlock();
    mCondition.notify_one();
}

status_t ImageManagerExt::cache(int clientId, const sp<GraphicBuffer>& buffer) {
    std::shared_ptr<Barrier> barrier = std::make_shared<Barrier>();
    cacheAsync(clientId, buffer, barrier);
    barrier->lock.lock();
    while (!barrier->done) {
        barrier->condition.wait(barrier->lock);
    }
    status_t result = barrier->result;
    barrier->lock.unlock();
    return result;
}

void ImageManagerExt::threadMain() {
    set_sched_policy(0, SP_FOREGROUND);

    mMutex.lock();
    bool running = mRunning;
    mThreadId = gettid();
    mMutex.unlock();
    mThreadInitCondition.notify_all();

    while (running) {
        QueueEntry entry;
        mMutex.lock();
        while (true) {
            running = mRunning;
            if (!mQueue.empty() || !running) {
                break;
            }
            mCondition.wait(mMutex);
        }
        if (!running) {
            mMutex.unlock();
            break;
        }
        entry = mQueue.front();
        mQueue.pop_front();
        mMutex.unlock();

        status_t result = NO_ERROR;
        if (entry.operation == CACHE) {
            std::lock_guard<std::mutex> lock(mListenerMutex);
            for (ImageManagerListener* listener : mListeners) {
                if (listener->getClientId() == entry.clientId) {
                    result = listener->cacheExternalTextureBufferInternal(mEglDisplay,
                                                                          entry.buffer);
                    break;
                }
            }
        } else if (entry.operation == RELEASE) {
            std::lock_guard<std::mutex> lock(mListenerMutex);
            for (ImageManagerListener* listener : mListeners) {
                if (listener->getClientId() == entry.clientId) {
                    listener->unbindExternalTextureBufferInternal(entry.bufferId);
                    break;
                }
            }
        }

        if (entry.barrier != nullptr) {
            entry.barrier->lock.lock();
            entry.barrier->result = result;
            entry.barrier->done = true;
            entry.barrier->lock.unlock();
            entry.barrier->condition.notify_one();
        }
    }

    if (mDebug) {
        ALOGI("Reached end of threadMain, terminating ImageManagerExt thread!");
    }
}

}  // namespace android
