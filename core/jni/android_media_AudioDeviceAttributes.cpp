/*
 * Copyright (C) 2020 The Android Open Source Project
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

// PICO OS 5.13.7: Android 13 android_media_AudioDeviceAttributes.cpp as in the factory
// libandroid_runtime (register_android_media_AudioDeviceAttributes,
// createAudioDeviceAttributesFromNative, createAudioDeviceTypeAddrFromJava), on the PICO
// libaudioclient AudioDeviceTypeAddrForSpatial.

#include "android_media_AudioDeviceAttributes.h"
#include "android_media_AudioErrors.h"
#include "core_jni_helpers.h"

#include <nativehelper/ScopedLocalRef.h>
#include <nativehelper/ScopedUtfChars.h>

using namespace android;

static jclass gAudioDeviceAttributesClass;
static jmethodID gAudioDeviceAttributesCstor;
static struct {
    jfieldID mAddress;
    jfieldID mNativeType;
    // other fields unused by JNI
} gAudioDeviceAttributesFields;

namespace android {

jint createAudioDeviceAttributesFromNative(JNIEnv *env, jobject *jAudioDeviceAttributes,
                                 const AudioDeviceTypeAddrForSpatial *devTypeAddr) {
    jint jStatus = (jint)AUDIO_JAVA_SUCCESS;
    jint jNativeType = (jint)devTypeAddr->mType;
    ScopedLocalRef<jstring> jAddress(env, env->NewStringUTF(devTypeAddr->getAddress()));

    *jAudioDeviceAttributes = env->NewObject(gAudioDeviceAttributesClass,
            gAudioDeviceAttributesCstor, jNativeType, jAddress.get());

    return jStatus;
}

jint createAudioDeviceTypeAddrFromJava(JNIEnv *env, AudioDeviceTypeAddrForSpatial *devTypeAddr,
                                       const jobject jDevice) {
    devTypeAddr->mType = (audio_devices_t)env->GetIntField(jDevice,
                         gAudioDeviceAttributesFields.mNativeType);

    jstring jAddress = (jstring)env->GetObjectField(jDevice,
                       gAudioDeviceAttributesFields.mAddress);
    devTypeAddr->setAddress(ScopedUtfChars(env, jAddress).c_str());

    return AUDIO_JAVA_SUCCESS;
}
} // namespace android

int register_android_media_AudioDeviceAttributes(JNIEnv *env) {
    jclass audioDeviceTypeAddressClass =
            FindClassOrDie(env, "android/media/AudioDeviceAttributes");
    gAudioDeviceAttributesClass = MakeGlobalRefOrDie(env, audioDeviceTypeAddressClass);
    gAudioDeviceAttributesCstor =
            GetMethodIDOrDie(env, audioDeviceTypeAddressClass, "<init>",
                             "(ILjava/lang/String;)V");

    gAudioDeviceAttributesFields.mNativeType =
            GetFieldIDOrDie(env, gAudioDeviceAttributesClass, "mNativeType", "I");
    gAudioDeviceAttributesFields.mAddress =
            GetFieldIDOrDie(env, gAudioDeviceAttributesClass, "mAddress",
                            "Ljava/lang/String;");

    return 0;
}
