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
// The VR skip-draw path never locks or queues a producer buffer (factory 0x1c4ac8 / 0x1c4c60):
// the lock checks the Surface and its producer, allocates the scratch pixel
// android::tempMemFor2DVrBits (malloc(1)) if there is none, points a 1x1 opaque RGBA bitmap
// (SkBitmap::setInfo with the row bytes of PIXEL_FORMAT_RGBX_8888, SkBitmap::setPixels) at it
// and installs the bitmap on the Java canvas, then keeps an extra Surface reference until
// ExtSurfaceImpl's finally block releases it; the unlock detaches the bitmap and frees the
// scratch pixel. Whatever the view hierarchy draws in that mode is discarded, on the factory as
// here. As on the factory the 4-byte pixel lives in a malloc(1) block (the allocator rounds it
// up to its smallest size class) and the block is shared by all 2D VR canvases of the process,
// which lock, draw and unlock in turn on their UI thread.

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android/graphics/GraphicsJNI.h"
#include "core_jni_helpers.h"

#include <gui/Surface.h>
#include <ui/PixelFormat.h>

#include <SkBitmap.h>
#include <SkImageInfo.h>

#include <stdlib.h>

namespace android {

int register_android_view_ExtSurfaceImpl(JNIEnv* env);

static const void* sRefBaseOwner;

// Scratch pixel of the 2D VR canvases (factory global android::tempMemFor2DVrBits).
void* tempMemFor2DVrBits = nullptr;

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

    if (tempMemFor2DVrBits == nullptr) {
        tempMemFor2DVrBits = malloc(1);
    }
    SkImageInfo info = SkImageInfo::Make(1, 1, kRGBA_8888_SkColorType, kOpaque_SkAlphaType);
    SkBitmap bitmap;
    bitmap.setInfo(info, bytesPerPixel(PIXEL_FORMAT_RGBX_8888));
    bitmap.setPixels(tempMemFor2DVrBits);
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
    if (tempMemFor2DVrBits != nullptr) {
        free(tempMemFor2DVrBits);
        tempMemFor2DVrBits = nullptr;
    }
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
