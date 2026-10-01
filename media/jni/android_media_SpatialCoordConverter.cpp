// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

// PICO OS 5.13.7: JNI of android.media.SpatialCoordConverter, reconstructed from the factory
// libmedia_jni (register_android_media_SpatialCoordConverter). The Java object owns a
// spatial::SpatialCoordConverter of libspatialaudio in mNativeContext.

#define LOG_TAG "SpatialCoordConverter-JNI"

#include <mutex>

#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <spatialaudio/SpatialCoordConverter.h>
#include <utils/Log.h>

using spatial::SpatialCoordConverter;

static const char* const kClassPathName = "android/media/SpatialCoordConverter";

// SpatialAudio.ERR_UNKNOWN / ERR_INVALID_PARAMETER
static const jint kErrUnknown = -1;
static const jint kErrInvalidParameter = -4;

// Size of the (x, y, z, w) orientation quaternion returned to Java.
static const jsize kOrientationLength = 4;

struct fields_t {
    jfieldID context;
};
static fields_t fields;

static std::mutex sLock;

static SpatialCoordConverter* getConverter(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(sLock);
    return reinterpret_cast<SpatialCoordConverter*>(env->GetLongField(thiz, fields.context));
}

static SpatialCoordConverter* setConverter(JNIEnv* env, jobject thiz,
        SpatialCoordConverter* converter) {
    std::lock_guard<std::mutex> lock(sLock);
    SpatialCoordConverter* old =
            reinterpret_cast<SpatialCoordConverter*>(env->GetLongField(thiz, fields.context));
    env->SetLongField(thiz, fields.context, reinterpret_cast<jlong>(converter));
    return old;
}

static void setup(JNIEnv* env, jobject thiz) {
    // As in the factory, a converter set up before is not released.
    setConverter(env, thiz, new SpatialCoordConverter());
}

static void release(JNIEnv* env, jobject thiz) {
    SpatialCoordConverter* converter = setConverter(env, thiz, nullptr);
    if (converter != nullptr) {
        delete converter;
    }
}

static jint setCoordinateTransform(JNIEnv* env, jobject thiz, jfloatArray jTransform) {
    if (jTransform == nullptr) {
        ALOGE("%s transform is null", __func__);
        return kErrInvalidParameter;
    }
    // The factory copies the whole array, whatever its length.
    jsize length = env->GetArrayLength(jTransform);
    float transform[length];
    env->GetFloatArrayRegion(jTransform, 0, length, transform);
    SpatialCoordConverter* converter = getConverter(env, thiz);
    if (converter == nullptr) {
        return kErrUnknown;
    }
    return converter->setCoordinateTransform(transform);
}

static jint setAdditionalCameraOrientation(JNIEnv* env, jobject thiz,
        jfloat frontX, jfloat frontY, jfloat frontZ, jfloat upX, jfloat upY, jfloat upZ) {
    SpatialCoordConverter* converter = getConverter(env, thiz);
    if (converter == nullptr) {
        return kErrUnknown;
    }
    return converter->setAdditionalCameraOrientation(frontX, frontY, frontZ, upX, upY, upZ);
}

static jint convertRelativeAudioOrientation(JNIEnv* env, jobject thiz,
        jfloat targetX, jfloat targetY, jfloat targetZ,
        jfloat reserved0, jfloat reserved1, jfloat reserved2,
        jfloat upX, jfloat upY, jfloat upZ,
        jfloat originX, jfloat originY, jfloat originZ,
        jfloatArray jOrientation) {
    if (jOrientation == nullptr || env->GetArrayLength(jOrientation) != kOrientationLength) {
        ALOGE("%s invalid array length, required length is %d", __func__, kOrientationLength);
        return kErrInvalidParameter;
    }
    SpatialCoordConverter* converter = getConverter(env, thiz);
    if (converter == nullptr) {
        return kErrUnknown;
    }
    float orientation[kOrientationLength];
    int result = converter->convertRelativeAudioOrientation(targetX, targetY, targetZ,
            reserved0, reserved1, reserved2, upX, upY, upZ, originX, originY, originZ,
            orientation, sizeof(orientation));
    if (result == 0) {
        env->SetFloatArrayRegion(jOrientation, 0, kOrientationLength, orientation);
    }
    return result;
}

static jint convertAudioOrientation(JNIEnv* env, jobject thiz,
        jfloat frontX, jfloat frontY, jfloat frontZ, jfloat upX, jfloat upY, jfloat upZ,
        jfloatArray jOrientation) {
    if (jOrientation == nullptr || env->GetArrayLength(jOrientation) != kOrientationLength) {
        ALOGE("%s invalid array length, required length is %d", __func__, kOrientationLength);
        return kErrInvalidParameter;
    }
    SpatialCoordConverter* converter = getConverter(env, thiz);
    if (converter == nullptr) {
        return kErrUnknown;
    }
    float orientation[kOrientationLength];
    int result = converter->convertAudioOrientation(frontX, frontY, frontZ, upX, upY, upZ,
            orientation, sizeof(orientation));
    if (result == 0) {
        env->SetFloatArrayRegion(jOrientation, 0, kOrientationLength, orientation);
    }
    return result;
}

static const JNINativeMethod gMethods[] = {
    {"setup", "()V", (void *)setup},
    {"release", "()V", (void *)release},
    {"setCoordinateTransform", "([F)I", (void *)setCoordinateTransform},
    {"setAdditionalCameraOrientation", "(FFFFFF)I", (void *)setAdditionalCameraOrientation},
    {"convertRelativeAudioOrientation", "(FFFFFFFFFFFF[F)I",
            (void *)convertRelativeAudioOrientation},
    {"convertAudioOrientation", "(FFFFFF[F)I", (void *)convertAudioOrientation},
};

int register_android_media_SpatialCoordConverter(JNIEnv *env) {
    int result = -1;
    jclass clazz = env->FindClass(kClassPathName);
    if (clazz != nullptr) {
        result = env->RegisterNatives(clazz, gMethods, NELEM(gMethods));
        env->DeleteLocalRef(clazz);
    }
    if (result < 0) {
        ALOGE("register methods failed %d", result);
        return -1;
    }

    clazz = env->FindClass(kClassPathName);
    if (clazz == nullptr) {
        ALOGE("can't find class %s", kClassPathName);
        return -1;
    }
    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.context == nullptr) {
        ALOGE("can't find field %s in class %s", "mNativeContext", kClassPathName);
        env->DeleteLocalRef(clazz);
        return -1;
    }
    env->DeleteLocalRef(clazz);
    return 0;
}
