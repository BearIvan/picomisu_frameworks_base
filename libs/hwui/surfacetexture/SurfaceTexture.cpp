/*
 * Copyright (C) 2018 The Android Open Source Project
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

#include <GLES2/gl2.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <cutils/compiler.h>
#include <gui/BufferItem.h>
#include <gui/BufferQueue.h>
#include <math/mat4.h>
#include <system/window.h>

#include <utils/Timers.h>
#include <utils/Trace.h>

#include <stddef.h>
#include <unistd.h>

#include "Matrix.h"
#include "SurfaceTexture.h"
#include "ImageConsumer.h"
#include "ImageManagerExt.h"

namespace android {

// PICO: the factory libpxrguiex accesses these fields at fixed offsets (the frame callback and
// its argument, set by setOnFrameAvailableCallbackAndTextureIdExt, are ConsumerBase fields).
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winvalid-offsetof"
struct SurfaceTextureLayout {
#if defined(__LP64__)
    static_assert(offsetof(SurfaceTexture, mPicoFrameCallback) == 1672, "");
    static_assert(offsetof(SurfaceTexture, mPicoFrameCallbackArgument) == 1680, "");
    static_assert(offsetof(SurfaceTexture, mCurrentCrop) == 1684, "");
    static_assert(offsetof(SurfaceTexture, mEGLConsumer) == 1856, "");
    static_assert(offsetof(SurfaceTexture, mImageConsumer) == 3336, "");
    static_assert(offsetof(SurfaceTexture, mHasAcquiredTexture) == 4872, "");
    static_assert(offsetof(SurfaceTexture, mIsProtectedContent) == 4873, "");
    static_assert(offsetof(SurfaceTexture, mTexNameCount) == 4876, "");
    static_assert(offsetof(SurfaceTexture, mTexNames) == 4880, "");
    static_assert(offsetof(SurfaceTexture, mFromVrCompositor) == 4964, "");
#else
    static_assert(offsetof(SurfaceTexture, mPicoFrameCallback) == 1068, "");
    static_assert(offsetof(SurfaceTexture, mPicoFrameCallbackArgument) == 1072, "");
    static_assert(offsetof(SurfaceTexture, mCurrentCrop) == 1076, "");
    static_assert(offsetof(SurfaceTexture, mHasAcquiredTexture) == 2776, "");
    static_assert(offsetof(SurfaceTexture, mIsProtectedContent) == 2777, "");
    static_assert(offsetof(SurfaceTexture, mTexNameCount) == 2780, "");
    static_assert(offsetof(SurfaceTexture, mTexNames) == 2784, "");
    static_assert(offsetof(SurfaceTexture, mFromVrCompositor) == 2868, "");
#endif
};
#pragma clang diagnostic pop

// PICO: private IGraphicBufferConsumer transaction handled by BufferQueueConsumer::onTransact.
static constexpr uint32_t kPicoConsumerConfiguration = 10000;

// PICO: ImageManagerExt latency logs are printed above this duration.
static constexpr float kSlowCallMs = 0.3f;

// Macros for including the SurfaceTexture name in log messages
#define SFT_LOGV(x, ...) ALOGV("[%s] " x, mName.string(), ##__VA_ARGS__)
#define SFT_LOGD(x, ...) ALOGD("[%s] " x, mName.string(), ##__VA_ARGS__)
#define SFT_LOGW(x, ...) ALOGW("[%s] " x, mName.string(), ##__VA_ARGS__)
#define SFT_LOGE(x, ...) ALOGE("[%s] " x, mName.string(), ##__VA_ARGS__)

static const mat4 mtxIdentity;

SurfaceTexture::SurfaceTexture(const sp<IGraphicBufferConsumer>& bq, uint32_t tex,
                               uint32_t texTarget, bool useFenceSync, bool isControlledByApp)
        : ConsumerBase(bq, isControlledByApp)
        , mCurrentCrop(Rect::EMPTY_RECT)
        , mCurrentTransform(0)
        , mCurrentScalingMode(NATIVE_WINDOW_SCALING_MODE_FREEZE)
        , mCurrentFence(Fence::NO_FENCE)
        , mCurrentTimestamp(0)
        , mCurrentDataSpace(HAL_DATASPACE_UNKNOWN)
        , mCurrentFrameNumber(0)
        , mDefaultWidth(1)
        , mDefaultHeight(1)
        , mFilteringEnabled(true)
        , mTexName(tex)
        , mUseFenceSync(useFenceSync)
        , mTexTarget(texTarget)
        , mCurrentTexture(BufferQueue::INVALID_BUFFER_SLOT)
        , mOpMode(OpMode::attachedToGL) {
    SFT_LOGV("SurfaceTexture");

    memcpy(mCurrentTransformMatrix, mtxIdentity.asArray(), sizeof(mCurrentTransformMatrix));

    mConsumer->setConsumerUsageBits(DEFAULT_USAGE_FLAGS);
}

SurfaceTexture::SurfaceTexture(const sp<IGraphicBufferConsumer>& bq, uint32_t texTarget,
                               bool useFenceSync, bool isControlledByApp)
        : ConsumerBase(bq, isControlledByApp)
        , mCurrentCrop(Rect::EMPTY_RECT)
        , mCurrentTransform(0)
        , mCurrentScalingMode(NATIVE_WINDOW_SCALING_MODE_FREEZE)
        , mCurrentFence(Fence::NO_FENCE)
        , mCurrentTimestamp(0)
        , mCurrentDataSpace(HAL_DATASPACE_UNKNOWN)
        , mCurrentFrameNumber(0)
        , mDefaultWidth(1)
        , mDefaultHeight(1)
        , mFilteringEnabled(true)
        , mTexName(0)
        , mUseFenceSync(useFenceSync)
        , mTexTarget(texTarget)
        , mCurrentTexture(BufferQueue::INVALID_BUFFER_SLOT)
        , mOpMode(OpMode::detached) {
    SFT_LOGV("SurfaceTexture");

    memcpy(mCurrentTransformMatrix, mtxIdentity.asArray(), sizeof(mCurrentTransformMatrix));

    mConsumer->setConsumerUsageBits(DEFAULT_USAGE_FLAGS);
}

status_t SurfaceTexture::setDefaultBufferSize(uint32_t w, uint32_t h) {
    Mutex::Autolock lock(mMutex);
    if (mAbandoned) {
        SFT_LOGE("setDefaultBufferSize: SurfaceTexture is abandoned!");
        return NO_INIT;
    }
    mDefaultWidth = w;
    mDefaultHeight = h;
    return mConsumer->setDefaultBufferSize(w, h);
}

status_t SurfaceTexture::updateTexImage() {
    ATRACE_CALL();
    SFT_LOGV("updateTexImage");
    if (ImageManagerExt::mDebugPerformance && mFromVrCompositor) {
        nsecs_t start = systemTime(SYSTEM_TIME_MONOTONIC);
        Mutex::Autolock lock(mMutex);
        if (mAbandoned) {
            SFT_LOGE("updateTexImage: SurfaceTexture is abandoned!");
            return NO_INIT;
        }
        status_t err = mEGLConsumer.updateTexImage(*this);
        float dt = (systemTime(SYSTEM_TIME_MONOTONIC) - start) * 1e-6f;
        if (dt > kSlowCallMs) {
            ALOGI("ImageManagerExt updateTexImage: dt = %0.3fms, w*h = %d*%d", dt, mDefaultWidth,
                  mDefaultHeight);
        }
        return err;
    }

    Mutex::Autolock lock(mMutex);

    if (mAbandoned) {
        SFT_LOGE("updateTexImage: SurfaceTexture is abandoned!");
        return NO_INIT;
    }

    return mEGLConsumer.updateTexImage(*this);
}

status_t SurfaceTexture::acquireTexture(int* texName) {
    if (ImageManagerExt::mDebugPerformance) {
        nsecs_t start = systemTime(SYSTEM_TIME_MONOTONIC);
        Mutex::Autolock lock(mMutex);
        if (mAbandoned) {
            SFT_LOGE("acquireTexture: SurfaceTexture is abandoned!");
            return NO_INIT;
        }
        status_t err = mEGLConsumer.acquireTexture(*this, texName);
        float dt = (systemTime(SYSTEM_TIME_MONOTONIC) - start) * 1e-6f;
        if (dt > kSlowCallMs) {
            ALOGI("ImageManagerExt acquireTexture: dt = %0.3fms, w*h = %d*%d", dt, mDefaultWidth,
                  mDefaultHeight);
        }
        return err;
    }

    Mutex::Autolock lock(mMutex);
    if (mAbandoned) {
        SFT_LOGE("acquireTexture: SurfaceTexture is abandoned!");
        return NO_INIT;
    }
    return mEGLConsumer.acquireTexture(*this, texName);
}

void SurfaceTexture::releaseTexture(int texName, int fenceFd) {
    Mutex::Autolock lock(mMutex);
    if (mAbandoned) {
        if (fenceFd != -1) {
            close(fenceFd);
        }
        SFT_LOGE("releaseTexture: SurfaceTexture is abandoned!");
        return;
    }
    mEGLConsumer.releaseTexture(*this, texName, fenceFd);
}

void SurfaceTexture::setTexName(int* texNames, int count) {
    // As in the factory, count is not checked against MAX_TEX_NAMES.
    mTexNameCount = count;
    if (ImageManagerExt::mDebug) {
        ALOGI("ImageManagerExt setTexName  size %d, surface id %d", count,
              mEGLConsumer.getClientId());
    }
    for (int i = 0; i < count; i++) {
        if (ImageManagerExt::mDebug) {
            ALOGI("ImageManagerExt setTexName texName %d, surface id %d", texNames[i],
                  mEGLConsumer.getClientId());
        }
        mTexNames[i].texName = texNames[i];
        mTexNames[i].slot = BufferQueue::INVALID_BUFFER_SLOT;
        mTexNames[i].acquired = false;
        mTexNames[i].freed = false;
    }
    setFromVrCompositor(true);
}

int SurfaceTexture::createFence() {
    EGLDisplay dpy = eglGetCurrentDisplay();
    EGLSyncKHR sync = eglCreateSyncKHR(dpy, EGL_SYNC_NATIVE_FENCE_ANDROID, nullptr);
    if (sync == EGL_NO_SYNC_KHR) {
        ALOGE("syncForReleaseLocked: error creating EGL fence: %#x", eglGetError());
        return UNKNOWN_ERROR;
    }
    glFlush();
    int fenceFd = eglDupNativeFenceFDANDROID(dpy, sync);
    eglDestroySyncKHR(dpy, sync);
    if (fenceFd == EGL_NO_NATIVE_FENCE_FD_ANDROID) {
        ALOGE("syncForReleaseLocked: error dup'ing native fence fd: %#x", eglGetError());
        return UNKNOWN_ERROR;
    }
    return fenceFd;
}

void SurfaceTexture::setFromVrCompositor(bool fromVrCompositor) {
    if (mFromVrCompositor == fromVrCompositor) {
        return;
    }
    mFromVrCompositor = fromVrCompositor;
    if (fromVrCompositor) {
        if (ImageManagerExt::mHasBeenInit) {
            ImageManagerExt::getInstance().initThread();
        }
        // Tell the BufferQueue (BufferQueueConsumer::onTransact) that its consumer is a VR
        // compositor, and allow 3 acquired buffers.
        sp<IBinder> binder = IInterface::asBinder(mConsumer);
        Parcel data, reply;
        data.writeInterfaceToken(String16("android.gui.IGraphicBufferConsumer"));
        data.writeInt32(mEGLConsumer.getClientId());
        data.writeInt32(ImageManagerExt::mDebug);
        binder->transact(kPicoConsumerConfiguration, data, &reply, 0);
        mConsumer->setMaxAcquiredBufferCount(3);
    }
    mEGLConsumer.setFromVrCompositor(fromVrCompositor);
}

String8 SurfaceTexture::getName() {
    return mName;
}

void SurfaceTexture::onFrameAvailable(const BufferItem& item) {
    if (item.mGraphicBuffer != nullptr) {
        Mutex::Autolock lock(mMutex);
        mEGLConsumer.onBufferQueued(item.mGraphicBuffer);
    }
    ConsumerBase::onFrameAvailable(item);
}

void SurfaceTexture::onFrameReplaced(const BufferItem& item) {
    if (item.mGraphicBuffer != nullptr) {
        Mutex::Autolock lock(mMutex);
        mEGLConsumer.onBufferQueued(item.mGraphicBuffer);
    }
    ConsumerBase::onFrameReplaced(item);
}

status_t SurfaceTexture::releaseTexImage() {
    // releaseTexImage can be invoked even when not attached to a GL context.
    ATRACE_CALL();
    SFT_LOGV("releaseTexImage");
    Mutex::Autolock lock(mMutex);

    if (mAbandoned) {
        SFT_LOGE("releaseTexImage: SurfaceTexture is abandoned!");
        return NO_INIT;
    }

    return mEGLConsumer.releaseTexImage(*this);
}

status_t SurfaceTexture::acquireBufferLocked(BufferItem* item, nsecs_t presentWhen,
                                             uint64_t maxFrameNumber) {
    status_t err = ConsumerBase::acquireBufferLocked(item, presentWhen, maxFrameNumber);
    if (err != NO_ERROR) {
        return err;
    }

    switch (mOpMode) {
        case OpMode::attachedToView:
            mImageConsumer.onAcquireBufferLocked(item);
            break;
        case OpMode::attachedToGL:
            mEGLConsumer.onAcquireBufferLocked(item, *this);
            break;
        case OpMode::detached:
            break;
    }

    return NO_ERROR;
}

status_t SurfaceTexture::releaseBufferLocked(int buf, sp<GraphicBuffer> graphicBuffer,
                                             EGLDisplay display, EGLSyncKHR eglFence) {
    // release the buffer if it hasn't already been discarded by the
    // BufferQueue. This can happen, for example, when the producer of this
    // buffer has reallocated the original buffer slot after this buffer
    // was acquired.
    status_t err = ConsumerBase::releaseBufferLocked(buf, graphicBuffer, display, eglFence);
    // We could be releasing an EGL/Vulkan buffer, even if not currently attached to a GL context.
    mImageConsumer.onReleaseBufferLocked(buf);
    mEGLConsumer.onReleaseBufferLocked(buf);
    return err;
}

status_t SurfaceTexture::detachFromContext() {
    ATRACE_CALL();
    SFT_LOGV("detachFromContext");
    Mutex::Autolock lock(mMutex);

    if (mAbandoned) {
        SFT_LOGE("detachFromContext: abandoned SurfaceTexture");
        return NO_INIT;
    }

    if (mOpMode != OpMode::attachedToGL) {
        SFT_LOGE("detachFromContext: SurfaceTexture is not attached to a GL context");
        return INVALID_OPERATION;
    }

    status_t err = mEGLConsumer.detachFromContext(*this);
    if (err == OK) {
        mOpMode = OpMode::detached;
    }

    return err;
}

status_t SurfaceTexture::attachToContext(uint32_t tex) {
    ATRACE_CALL();
    SFT_LOGV("attachToContext");
    Mutex::Autolock lock(mMutex);

    if (mAbandoned) {
        SFT_LOGE("attachToContext: abandoned SurfaceTexture");
        return NO_INIT;
    }

    if (mOpMode != OpMode::detached) {
        SFT_LOGE(
                "attachToContext: SurfaceTexture is already attached to a "
                "context");
        return INVALID_OPERATION;
    }

    if (mCurrentTexture != BufferQueue::INVALID_BUFFER_SLOT) {
        // release possible ImageConsumer cache
        mImageConsumer.onFreeBufferLocked(mCurrentTexture);
    }

    return mEGLConsumer.attachToContext(tex, *this);
}

void SurfaceTexture::attachToView() {
    ATRACE_CALL();
    Mutex::Autolock _l(mMutex);
    if (mAbandoned) {
        SFT_LOGE("attachToView: abandoned SurfaceTexture");
        return;
    }
    if (mOpMode == OpMode::detached) {
        mOpMode = OpMode::attachedToView;

        if (mCurrentTexture != BufferQueue::INVALID_BUFFER_SLOT) {
            // release possible EGLConsumer texture cache
            mEGLConsumer.onFreeBufferLocked(mCurrentTexture);
            mEGLConsumer.onAbandonLocked();
        }
    } else {
        SFT_LOGE("attachToView: already attached");
    }
}

void SurfaceTexture::detachFromView() {
    ATRACE_CALL();
    Mutex::Autolock _l(mMutex);

    if (mAbandoned) {
        SFT_LOGE("detachFromView: abandoned SurfaceTexture");
        return;
    }

    if (mOpMode == OpMode::attachedToView) {
        mOpMode = OpMode::detached;
        // Free all EglImage and VkImage before the context is destroyed.
        for (int i=0; i < BufferQueueDefs::NUM_BUFFER_SLOTS; i++) {
            mImageConsumer.onFreeBufferLocked(i);
        }
    } else {
        SFT_LOGE("detachFromView: not attached to View");
    }
}

uint32_t SurfaceTexture::getCurrentTextureTarget() const {
    return mTexTarget;
}

void SurfaceTexture::getTransformMatrix(float mtx[16]) {
    Mutex::Autolock lock(mMutex);
    memcpy(mtx, mCurrentTransformMatrix, sizeof(mCurrentTransformMatrix));
}

void SurfaceTexture::setFilteringEnabled(bool enabled) {
    Mutex::Autolock lock(mMutex);
    if (mAbandoned) {
        SFT_LOGE("setFilteringEnabled: SurfaceTexture is abandoned!");
        return;
    }
    bool needsRecompute = mFilteringEnabled != enabled;
    mFilteringEnabled = enabled;

    if (needsRecompute && mCurrentTexture == BufferQueue::INVALID_BUFFER_SLOT) {
        SFT_LOGD("setFilteringEnabled called with no current item");
    }

    if (needsRecompute && mCurrentTexture != BufferQueue::INVALID_BUFFER_SLOT) {
        computeCurrentTransformMatrixLocked();
    }
}

void SurfaceTexture::computeCurrentTransformMatrixLocked() {
    SFT_LOGV("computeCurrentTransformMatrixLocked");
    sp<GraphicBuffer> buf = (mCurrentTexture == BufferQueue::INVALID_BUFFER_SLOT)
                                    ? nullptr
                                    : mSlots[mCurrentTexture].mGraphicBuffer;
    if (buf == nullptr) {
        SFT_LOGD("computeCurrentTransformMatrixLocked: no current item");
    }
    computeTransformMatrix(mCurrentTransformMatrix, buf, mCurrentCrop, mCurrentTransform,
                           mFilteringEnabled);
}

void SurfaceTexture::computeTransformMatrix(float outTransform[16], const sp<GraphicBuffer>& buf,
                                            const Rect& cropRect, uint32_t transform,
                                            bool filtering) {
    // Transform matrices
    static const mat4 mtxFlipH(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1);
    static const mat4 mtxFlipV(1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1);
    static const mat4 mtxRot90(0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1);

    mat4 xform;
    if (transform & NATIVE_WINDOW_TRANSFORM_FLIP_H) {
        xform *= mtxFlipH;
    }
    if (transform & NATIVE_WINDOW_TRANSFORM_FLIP_V) {
        xform *= mtxFlipV;
    }
    if (transform & NATIVE_WINDOW_TRANSFORM_ROT_90) {
        xform *= mtxRot90;
    }

    if (!cropRect.isEmpty() && buf.get()) {
        float tx = 0.0f, ty = 0.0f, sx = 1.0f, sy = 1.0f;
        float bufferWidth = buf->getWidth();
        float bufferHeight = buf->getHeight();
        float shrinkAmount = 0.0f;
        if (filtering) {
            // In order to prevent bilinear sampling beyond the edge of the
            // crop rectangle we may need to shrink it by 2 texels in each
            // dimension.  Normally this would just need to take 1/2 a texel
            // off each end, but because the chroma channels of YUV420 images
            // are subsampled we may need to shrink the crop region by a whole
            // texel on each side.
            switch (buf->getPixelFormat()) {
                case PIXEL_FORMAT_RGBA_8888:
                case PIXEL_FORMAT_RGBX_8888:
                case PIXEL_FORMAT_RGBA_FP16:
                case PIXEL_FORMAT_RGBA_1010102:
                case PIXEL_FORMAT_RGB_888:
                case PIXEL_FORMAT_RGB_565:
                case PIXEL_FORMAT_BGRA_8888:
                    // We know there's no subsampling of any channels, so we
                    // only need to shrink by a half a pixel.
                    shrinkAmount = 0.5;
                    break;

                default:
                    // If we don't recognize the format, we must assume the
                    // worst case (that we care about), which is YUV420.
                    shrinkAmount = 1.0;
                    break;
            }
        }

        // Only shrink the dimensions that are not the size of the buffer.
        if (cropRect.width() < bufferWidth) {
            tx = (float(cropRect.left) + shrinkAmount) / bufferWidth;
            sx = (float(cropRect.width()) - (2.0f * shrinkAmount)) / bufferWidth;
        }
        if (cropRect.height() < bufferHeight) {
            ty = (float(bufferHeight - cropRect.bottom) + shrinkAmount) / bufferHeight;
            sy = (float(cropRect.height()) - (2.0f * shrinkAmount)) / bufferHeight;
        }

        mat4 crop(sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, 1, 0, tx, ty, 0, 1);
        xform = crop * xform;
    }

    // SurfaceFlinger expects the top of its window textures to be at a Y
    // coordinate of 0, so SurfaceTexture must behave the same way.  We don't
    // want to expose this to applications, however, so we must add an
    // additional vertical flip to the transform after all the other transforms.
    xform = mtxFlipV * xform;

    memcpy(outTransform, xform.asArray(), sizeof(xform));
}

Rect SurfaceTexture::scaleDownCrop(const Rect& crop, uint32_t bufferWidth, uint32_t bufferHeight) {
    Rect outCrop = crop;

    uint32_t newWidth = static_cast<uint32_t>(crop.width());
    uint32_t newHeight = static_cast<uint32_t>(crop.height());

    if (newWidth * bufferHeight > newHeight * bufferWidth) {
        newWidth = newHeight * bufferWidth / bufferHeight;
        ALOGV("too wide: newWidth = %d", newWidth);
    } else if (newWidth * bufferHeight < newHeight * bufferWidth) {
        newHeight = newWidth * bufferHeight / bufferWidth;
        ALOGV("too tall: newHeight = %d", newHeight);
    }

    uint32_t currentWidth = static_cast<uint32_t>(crop.width());
    uint32_t currentHeight = static_cast<uint32_t>(crop.height());

    // The crop is too wide
    if (newWidth < currentWidth) {
        uint32_t dw = currentWidth - newWidth;
        auto halfdw = dw / 2;
        outCrop.left += halfdw;
        // Not halfdw because it would subtract 1 too few when dw is odd
        outCrop.right -= (dw - halfdw);
        // The crop is too tall
    } else if (newHeight < currentHeight) {
        uint32_t dh = currentHeight - newHeight;
        auto halfdh = dh / 2;
        outCrop.top += halfdh;
        // Not halfdh because it would subtract 1 too few when dh is odd
        outCrop.bottom -= (dh - halfdh);
    }

    ALOGV("getCurrentCrop final crop [%d,%d,%d,%d]", outCrop.left, outCrop.top, outCrop.right,
          outCrop.bottom);

    return outCrop;
}

nsecs_t SurfaceTexture::getTimestamp() {
    SFT_LOGV("getTimestamp");
    Mutex::Autolock lock(mMutex);
    return mCurrentTimestamp;
}

android_dataspace SurfaceTexture::getCurrentDataSpace() {
    SFT_LOGV("getCurrentDataSpace");
    Mutex::Autolock lock(mMutex);
    return mCurrentDataSpace;
}

uint64_t SurfaceTexture::getFrameNumber() {
    SFT_LOGV("getFrameNumber");
    Mutex::Autolock lock(mMutex);
    return mCurrentFrameNumber;
}

Rect SurfaceTexture::getCurrentCrop() const {
    Mutex::Autolock lock(mMutex);
    return (mCurrentScalingMode == NATIVE_WINDOW_SCALING_MODE_SCALE_CROP)
                   ? scaleDownCrop(mCurrentCrop, mDefaultWidth, mDefaultHeight)
                   : mCurrentCrop;
}

uint32_t SurfaceTexture::getCurrentTransform() const {
    Mutex::Autolock lock(mMutex);
    return mCurrentTransform;
}

uint32_t SurfaceTexture::getCurrentScalingMode() const {
    Mutex::Autolock lock(mMutex);
    return mCurrentScalingMode;
}

sp<Fence> SurfaceTexture::getCurrentFence() const {
    Mutex::Autolock lock(mMutex);
    return mCurrentFence;
}

std::shared_ptr<FenceTime> SurfaceTexture::getCurrentFenceTime() const {
    Mutex::Autolock lock(mMutex);
    return mCurrentFenceTime;
}

void SurfaceTexture::freeBufferLocked(int slotIndex) {
    // PICO: defer the release of the cached image, and mark a texture name still bound to the
    // freed slot so that releaseTexture does not release it again.
    if (mSlots[slotIndex].mGraphicBuffer != nullptr) {
        mEGLConsumer.onFreeBufferLockedExt(mSlots[slotIndex].mGraphicBuffer->getId());
    }
    for (uint32_t i = 0; i < mTexNameCount; i++) {
        TexNameSlot& texSlot = mTexNames[i];
        if (texSlot.acquired && !texSlot.freed && texSlot.slot == slotIndex) {
            if (ImageManagerExt::mDebug) {
                ALOGI("ImageManagerExt onFreeBufferLocked slot %d , texName %d, surface id %d",
                      slotIndex, texSlot.texName, mEGLConsumer.getClientId());
            }
            texSlot.freed = true;
            break;
        }
    }
    SFT_LOGV("freeBufferLocked: slotIndex=%d", slotIndex);
    if (slotIndex == mCurrentTexture) {
        mCurrentTexture = BufferQueue::INVALID_BUFFER_SLOT;
    }
    // The slotIndex buffer could have EGL or SkImage cache, but there is no way to tell for sure.
    // Buffers can be freed after SurfaceTexture has detached from GL context or View.
    mImageConsumer.onFreeBufferLocked(slotIndex);
    mEGLConsumer.onFreeBufferLocked(slotIndex);
    ConsumerBase::freeBufferLocked(slotIndex);
}

void SurfaceTexture::abandonLocked() {
    SFT_LOGV("abandonLocked");
    mEGLConsumer.onAbandonLocked();
    ConsumerBase::abandonLocked();
    if (ImageManagerExt::mDebug) {
        ALOGI("ImageManagerExt onAbandonLocked  surface id %d", mEGLConsumer.getClientId());
    }
    for (uint32_t i = 0; i < mTexNameCount; i++) {
        mTexNames[i].acquired = false;
    }
}

status_t SurfaceTexture::setConsumerUsageBits(uint64_t usage) {
    return ConsumerBase::setConsumerUsageBits(usage | DEFAULT_USAGE_FLAGS);
}

void SurfaceTexture::dumpLocked(String8& result, const char* prefix) const {
    result.appendFormat(
            "%smTexName=%d mCurrentTexture=%d\n"
            "%smCurrentCrop=[%d,%d,%d,%d] mCurrentTransform=%#x\n",
            prefix, mTexName, mCurrentTexture, prefix, mCurrentCrop.left, mCurrentCrop.top,
            mCurrentCrop.right, mCurrentCrop.bottom, mCurrentTransform);

    ConsumerBase::dumpLocked(result, prefix);
}

sk_sp<SkImage> SurfaceTexture::dequeueImage(SkMatrix& transformMatrix, bool* queueEmpty,
                                            uirenderer::RenderState& renderState) {
    Mutex::Autolock _l(mMutex);

    if (mAbandoned) {
        SFT_LOGE("dequeueImage: SurfaceTexture is abandoned!");
        return nullptr;
    }

    if (mOpMode != OpMode::attachedToView) {
        SFT_LOGE("dequeueImage: SurfaceTexture is not attached to a View");
        return nullptr;
    }

    auto image = mImageConsumer.dequeueImage(queueEmpty, *this, renderState);
    if (image.get()) {
        uirenderer::mat4(mCurrentTransformMatrix).copyTo(transformMatrix);
    }
    return image;
}

}  // namespace android
