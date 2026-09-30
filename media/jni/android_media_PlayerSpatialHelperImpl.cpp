// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

// PICO OS 5.13.7: JNI of android.media.PlayerSpatialHelperImpl, reconstructed from the factory
// libmedia_jni (register_android_media_PlayerSpatialHelper). The Java object owns a
// spatial::SpatialAudio of libspatialaudio in mNativeContext.

#define LOG_TAG "SpatialAudioImpl-JNI"

#include <mutex>

#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <spatialaudio/SpatialAudio.h>
#include <utils/Log.h>

using spatial::SpatialAudio;

static const char* const kClassPathName = "android/media/PlayerSpatialHelperImpl";

struct fields_t {
    jfieldID context;
};
static fields_t fields;

static std::mutex sLock;

static SpatialAudio* getSpatialAudio(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(sLock);
    return reinterpret_cast<SpatialAudio*>(env->GetLongField(thiz, fields.context));
}

static SpatialAudio* setSpatialAudio(JNIEnv* env, jobject thiz, SpatialAudio* audio) {
    std::lock_guard<std::mutex> lock(sLock);
    SpatialAudio* old = reinterpret_cast<SpatialAudio*>(env->GetLongField(thiz, fields.context));
    env->SetLongField(thiz, fields.context, reinterpret_cast<jlong>(audio));
    return old;
}

static void nativeSetup(JNIEnv* env, jobject thiz, jobject /* weakThis */) {
    // As in the factory, a SpatialAudio set up before is not released.
    setSpatialAudio(env, thiz, new SpatialAudio());
}

static void nativeRelease(JNIEnv* env, jobject thiz) {
    SpatialAudio* audio = setSpatialAudio(env, thiz, nullptr);
    if (audio != nullptr) {
        delete audio;
    }
}

static void nativeSetSessionId(JNIEnv* env, jobject thiz, jint sessionId) {
    SpatialAudio* audio = getSpatialAudio(env, thiz);
    if (audio == nullptr) {
        ALOGE("setSessionId invalid state");
        return;
    }
    audio->setSessionId(sessionId);
}

static jint nativeSetAudioOrientation(JNIEnv* env, jobject thiz,
        jfloat rotX, jfloat rotY, jfloat rotZ, jfloat rotW) {
    SpatialAudio* audio = getSpatialAudio(env, thiz);
    if (audio == nullptr) {
        ALOGE("setAudioOrientation invalid state");
        return -3;
    }
    return audio->setAudioOrientation(rotX, rotY, rotZ, rotW);
}

static jint nativeSetAudioPose(JNIEnv* env, jobject thiz,
        jfloat rotX, jfloat rotY, jfloat rotZ, jfloat rotW,
        jfloat posX, jfloat posY, jfloat posZ) {
    SpatialAudio* audio = getSpatialAudio(env, thiz);
    if (audio == nullptr) {
        ALOGE("setAudioPose invalid state");
        return -3;
    }
    return audio->setAudioPose(rotX, rotY, rotZ, rotW, posX, posY, posZ);
}

static jboolean nativeIsSpatializationEnabled(JNIEnv* env, jobject thiz) {
    SpatialAudio* audio = getSpatialAudio(env, thiz);
    if (audio == nullptr) {
        ALOGE("isSpatializationEnabled invalid state");
        return false;
    }
    return audio->isSpatializationEnabled();
}

static jint nativeSetSpatializationEnabled(JNIEnv* env, jobject thiz, jboolean enabled) {
    SpatialAudio* audio = getSpatialAudio(env, thiz);
    if (audio == nullptr) {
        // The factory logs the message of isSpatializationEnabled here.
        ALOGE("isSpatializationEnabled invalid state");
        return -3;
    }
    return audio->setSpatializationEnabled(enabled);
}

static const JNINativeMethod gMethods[] = {
    {"nativeSetup", "(Ljava/lang/Object;)V", (void *)nativeSetup},
    {"nativeRelease", "()V", (void *)nativeRelease},
    {"nativeSetSessionId", "(I)V", (void *)nativeSetSessionId},
    {"nativeSetAudioOrientation", "(FFFF)I", (void *)nativeSetAudioOrientation},
    {"nativeSetAudioPose", "(FFFFFFF)I", (void *)nativeSetAudioPose},
    {"nativeIsSpatializationEnabled", "()Z", (void *)nativeIsSpatializationEnabled},
    {"nativeSetSpatializationEnabled", "(Z)I", (void *)nativeSetSpatializationEnabled},
};

int register_android_media_PlayerSpatialHelper(JNIEnv *env) {
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
