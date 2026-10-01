/*
 * Copyright (C) 2026 Picomisu contributors
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

#define LOG_TAG "ExtSurfaceImpl"

// PICO: natives of android.view.ExtSurfaceImpl (2D VR software canvas).
//
// As in the factory PICO OS 5.13.7 libandroid_runtime the natives are exported with their
// JNI names and are resolved by ART's dynamic JNI lookup (the zygote loads libandroid, which
// depends on this library, with the boot class loader). register_android_view_ExtSurfaceImpl
// exists in the factory library but nothing calls it; it is kept, not called, as there.
//
// The VR skip-draw path draws into a 1x1 opaque canvas without locking or queuing a producer
// buffer, and keeps an extra Surface reference until ExtSurfaceImpl's finally block releases
// it. Deviation: every canvas gets its own 4-byte pixel allocation. The factory points the
// bitmap of every lock at one shared malloc(1) block (android::tempMemFor2DVrBits) that unlock
// frees, which is too small for an RGBA pixel and is freed under any other canvas still
// drawing into it.

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android/graphics/GraphicsJNI.h"
#include "core_jni_helpers.h"

#include <gui/Surface.h>

#include <SkBitmap.h>
#include <SkImageInfo.h>

namespace android {

int register_android_view_ExtSurfaceImpl(JNIEnv* env);

static const void* sRefBaseOwner;

} // namespace android

using namespace android;

extern "C" JNIEXPORT jlong JNICALL
Java_android_view_ExtSurfaceImpl_nativeLockCanvasFor2DVr(JNIEnv* env, jclass /* clazz */,
        jlong nativeObject, jobject canvasObj) {
    sp<Surface> surface(reinterpret_cast<Surface*>(nativeObject));
    if (!Surface::isValid(surface)) {
        doThrowIAE(env);
        return 0;
    }

    SkBitmap bitmap;
    if (!bitmap.tryAllocPixels(SkImageInfo::Make(1, 1, kRGBA_8888_SkColorType,
                                                kOpaque_SkAlphaType))) {
        jniThrowException(env, "android/view/Surface$OutOfResourcesException", nullptr);
        return 0;
    }
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(bitmap);

    sp<Surface> lockedSurface(surface);
    lockedSurface->incStrong(&sRefBaseOwner);
    return reinterpret_cast<jlong>(lockedSurface.get());
}

extern "C" JNIEXPORT void JNICALL
Java_android_view_ExtSurfaceImpl_nativeUnlockCanvasAndPostFor2DVr(JNIEnv* env,
        jclass /* clazz */, jlong nativeObject, jobject canvasObj) {
    sp<Surface> surface(reinterpret_cast<Surface*>(nativeObject));
    if (!Surface::isValid(surface)) {
        return;
    }

    // detach the canvas from the bitmap; nothing is queued to the producer
    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(SkBitmap());
}

extern "C" JNIEXPORT void JNICALL
Java_android_view_ExtSurfaceImpl_nativeReleaseSurfaceObject(JNIEnv* /* env */,
        jclass /* clazz */, jlong nativeObject) {
    sp<Surface> surface(reinterpret_cast<Surface*>(nativeObject));
    surface->decStrong(&sRefBaseOwner);
}

namespace android {

static const JNINativeMethod gExtSurfaceImplMethods[] = {
    // The factory table declares the lock as returning V, which does not match the DEX
    // (long nativeLockCanvasFor2DVr); registering it would abort. Use the DEX signature.
    {"nativeLockCanvasFor2DVr", "(JLandroid/graphics/Canvas;)J",
            (void*)Java_android_view_ExtSurfaceImpl_nativeLockCanvasFor2DVr},
    {"nativeUnlockCanvasAndPostFor2DVr", "(JLandroid/graphics/Canvas;)V",
            (void*)Java_android_view_ExtSurfaceImpl_nativeUnlockCanvasAndPostFor2DVr},
    {"nativeReleaseSurfaceObject", "(J)V",
            (void*)Java_android_view_ExtSurfaceImpl_nativeReleaseSurfaceObject},
};

int register_android_view_ExtSurfaceImpl(JNIEnv* env) {
    return RegisterMethodsOrDie(env, "android/view/ExtSurfaceImpl", gExtSurfaceImplMethods,
                                NELEM(gExtSurfaceImplMethods));
}

} // namespace android
