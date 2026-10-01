// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
#pragma once
#include <jni.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <log/log.h>
namespace android {
// Factory android::nativeFreezeSelfListening (0x11ec14), a static function of
// android_view_Surface.cpp: Surface.nativeFreezeSelfListening(J)V. Defined here so that
// libandroid_runtime (android_view_Surface.cpp, LOG_TAG "Surface") and its probe compile the
// same body, each with internal linkage as in the factory.
static void nativeFreezeSelfListening(JNIEnv*, jclass, jlong nativeObject) {
    sp<Surface> surface(reinterpret_cast<Surface*>(nativeObject));
    if (!Surface::isValid(surface)) {
        ALOGE("listenFreezeSelf fail for surface is not valid");
        return;
    }
    surface->getIGraphicBufferProducer()->listenFreezeSelf();
}
} // namespace android
