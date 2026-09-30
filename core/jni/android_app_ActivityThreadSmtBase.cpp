// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Smartisan prefetch natives of ActivityThreadSmtBase (factory PICO OS 5.13.7
// register_android_app_ActivityThreadSmtBase): the process prefetch state kept by the
// libbinder SoundSettings singleton.
#include "jni.h"
#include <binder/SoundSettings.h>
#include <nativehelper/JNIHelp.h>
#include "core_jni_helpers.h"

namespace android {

static void android_app_ActivityThreadSmtBase_initPrefetch(JNIEnv* env, jobject clazz,
        jboolean isPrefetch) {
    SoundSettings::getInstance()->initPrefetch(isPrefetch);
}

static void android_app_ActivityThreadSmtBase_onPrefetchRealStart(JNIEnv* env, jobject clazz,
        jboolean realStart) {
    SoundSettings::getInstance()->onSoundCallback(realStart);
}

static const JNINativeMethod gActivityThreadSmtBaseMethods[] = {
    { "initPrefetch", "(Z)V", (void*) android_app_ActivityThreadSmtBase_initPrefetch },
    { "onPrefetchRealStart", "(Z)V", (void*) android_app_ActivityThreadSmtBase_onPrefetchRealStart },
};

int register_android_app_ActivityThreadSmtBase(JNIEnv* env) {
    return RegisterMethodsOrDie(env, "android/app/ActivityThreadSmtBase",
            gActivityThreadSmtBaseMethods, NELEM(gActivityThreadSmtBaseMethods));
}

} // namespace android
