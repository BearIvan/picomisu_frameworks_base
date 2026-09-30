// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
//
// PICO ImageManagerExt: a process-wide worker thread that creates and destroys the EGLImages of
// the buffers queued to SurfaceTextures that are consumed by a VR compositor (see
// SurfaceTexture::setFromVrCompositor). Reconstructed from the factory libhwui (PICO OS 5.13.7);
// the class layout is observed by the factory libpxrguiex (it inlines
// Singleton<ImageManagerExt>::getInstance() with operator new(sizeof(ImageManagerExt))).

#pragma once

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <ui/GraphicBuffer.h>
#include <utils/Errors.h>
#include <utils/Singleton.h>
#include <utils/StrongPointer.h>

#include <condition_variable>
#include <deque>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

namespace android {

/*
 * ImageManagerListener is implemented by the EGLConsumer of every SurfaceTexture that is fed by a
 * VR compositor. ImageManagerExt calls it on its own thread to create (cache) and destroy (unbind)
 * the EGLImages of that consumer's buffers. The vtable slot order is factory ABI.
 */
class ImageManagerListener {
public:
    virtual ~ImageManagerListener() {}
    virtual int getClientId() = 0;
    virtual status_t cacheExternalTextureBufferInternal(EGLDisplay display,
                                                        const sp<GraphicBuffer>& buffer) = 0;
    virtual void unbindExternalTextureBufferInternal(uint64_t bufferId) = 0;
};

class ANDROID_API ImageManagerExt : public Singleton<ImageManagerExt> {
public:
    /*
     * Barrier lets a caller wait for the completion of a queued operation.
     */
    struct Barrier {
        std::mutex lock;
        std::condition_variable_any condition;
        bool done = false;
        status_t result = NO_ERROR;
    };

    enum Operation { RELEASE = 0, CACHE = 1 };

    struct QueueEntry {
        int operation = RELEASE;
        int clientId = 0;
        sp<GraphicBuffer> buffer;
        uint64_t bufferId = 0;
        std::shared_ptr<Barrier> barrier;
    };

    ImageManagerExt();
    ~ImageManagerExt();

    // Starts the worker thread (once) and returns its tid.
    int initThread();
    void threadMain();

    status_t checkAndUpdateEglState();

    void addImageManagerListener(ImageManagerListener* listener);
    void removeImageManagerListener(ImageManagerListener* listener);

    void cacheAsync(int clientId, const sp<GraphicBuffer>& buffer,
                    const std::shared_ptr<Barrier>& barrier);
    void releaseAsync(int clientId, uint64_t bufferId, const std::shared_ptr<Barrier>& barrier);
    void queueOperation(const QueueEntry&& entry);
    status_t cache(int clientId, const sp<GraphicBuffer>& buffer);

    static bool mHasBeenInit;
    static bool mDebug;
    static bool mDebugPerformance;

private:
    friend class Singleton<ImageManagerExt>;

    std::thread mThread;
    int mThreadId = -1;
    std::condition_variable_any mCondition;
    std::mutex mMutex;
    std::condition_variable mThreadInitCondition;
    std::deque<QueueEntry> mQueue;
    EGLDisplay mEglDisplay = EGL_NO_DISPLAY;
    bool mRunning = false;
    std::mutex mListenerMutex;
    std::vector<ImageManagerListener*> mListeners;
};

#if defined(__LP64__)
static_assert(sizeof(ImageManagerExt) == 296, "factory arm64 sizeof(ImageManagerExt)");
static_assert(sizeof(ImageManagerExt::QueueEntry) == 40, "factory arm64 QueueEntry");
static_assert(sizeof(ImageManagerExt::Barrier) == 112, "factory arm64 Barrier");
#else
static_assert(sizeof(ImageManagerExt) == 76, "factory arm32 sizeof(ImageManagerExt)");
#endif

}  // namespace android
