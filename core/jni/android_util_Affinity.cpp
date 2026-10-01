// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

// PICO OS 5.13.7: JNI of android.util.Affinity, reconstructed from the factory
// libandroid_runtime (register_android_util_Affinity, android_util_Affinity_bindToCpu).

#define LOG_TAG "Affinity"

#include <sched.h>
#include <sys/syscall.h>
#include <unistd.h>

#include <utils/Log.h>

#include "core_jni_helpers.h"

namespace android {

// Binds the calling thread to the CPUs whose bits are set in |cpu|.
void android_util_Affinity_bindToCpu(JNIEnv* env, jclass clazz, jint cpu)
{
    int cpus = sysconf(_SC_NPROCESSORS_CONF);
    ALOGD("get cpu number = %d\n", cpus);
    cpu_set_t mask;
    CPU_ZERO(&mask);
    for (int i = 0; i < cpus; i++) {
        if ((1 << i) & cpu) {
            CPU_SET(i, &mask);
            ALOGD("set cpu  = %d\n", i);
        }
    }
    pid_t tid = syscall(__NR_gettid);
    ALOGD("tid = %d\n", tid);
    if (sched_setaffinity(tid, sizeof(mask), &mask) == -1) {
        ALOGD("sched_set affinity error");
    }
}

static const JNINativeMethod gMethods[] = {
    {"bindToCpu", "(I)V", (void*)android_util_Affinity_bindToCpu},
};

int register_android_util_Affinity(JNIEnv* env)
{
    return RegisterMethodsOrDie(env, "android/util/Affinity", gMethods, NELEM(gMethods));
}

} // namespace android
