/*
 * Copyright (C) 2009 The Android Open Source Project
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

#define LOG_TAG "LightsService"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android_runtime/AndroidRuntime.h"

#include <android/hardware/light/2.0/ILight.h>
#include <android/hardware/light/2.0/types.h>
#include <android-base/chrono_utils.h>
#include <utils/misc.h>
#include <utils/Log.h>
#include <map>
#include <dlfcn.h>
#include <stdio.h>

namespace android {

using Brightness = ::android::hardware::light::V2_0::Brightness;
using Flash      = ::android::hardware::light::V2_0::Flash;
using ILight     = ::android::hardware::light::V2_0::ILight;
using LightState = ::android::hardware::light::V2_0::LightState;
using Status     = ::android::hardware::light::V2_0::Status;
using Type       = ::android::hardware::light::V2_0::Type;
template<typename T>
using Return     = ::android::hardware::Return<T>;

static bool sLightSupported = true;

static bool validate(jint light, jint flash, jint brightness) {
    bool valid = true;

    if (light < 0 || light >= static_cast<jint>(Type::COUNT)) {
        ALOGE("Invalid light parameter %d.", light);
        valid = false;
    }

    if (flash != static_cast<jint>(Flash::NONE) &&
        flash != static_cast<jint>(Flash::TIMED) &&
        flash != static_cast<jint>(Flash::HARDWARE)) {
        ALOGE("Invalid flash parameter %d.", flash);
        valid = false;
    }

    if (brightness != static_cast<jint>(Brightness::USER) &&
        brightness != static_cast<jint>(Brightness::SENSOR) &&
        brightness != static_cast<jint>(Brightness::LOW_PERSISTENCE)) {
        ALOGE("Invalid brightness parameter %d.", brightness);
        valid = false;
    }

    if (brightness == static_cast<jint>(Brightness::LOW_PERSISTENCE) &&
        light != static_cast<jint>(Type::BACKLIGHT)) {
        ALOGE("Cannot set low-persistence mode for non-backlight device.");
        valid = false;
    }

    return valid;
}

static LightState constructState(
        jint colorARGB,
        jint flashMode,
        jint onMS,
        jint offMS,
        jint brightnessMode){
    Flash flash = static_cast<Flash>(flashMode);
    Brightness brightness = static_cast<Brightness>(brightnessMode);

    LightState state{};

    if (brightness == Brightness::LOW_PERSISTENCE) {
        state.flashMode = Flash::NONE;
    } else {
        // Only set non-brightness settings when not in low-persistence mode
        state.flashMode = flash;
        state.flashOnMs = onMS;
        state.flashOffMs = offMS;
    }

    state.color = colorARGB;
    state.brightnessMode = brightness;

    return state;
}

static void processReturn(
        const Return<Status> &ret,
        Type type,
        const LightState &state) {
    if (!ret.isOk()) {
        ALOGE("Failed to issue set light command.");
        return;
    }

    switch (static_cast<Status>(ret)) {
        case Status::SUCCESS:
            break;
        case Status::LIGHT_NOT_SUPPORTED:
            ALOGE("Light requested not available on this device. %d", type);
            break;
        case Status::BRIGHTNESS_NOT_SUPPORTED:
            ALOGE("Brightness parameter not supported on this device: %d",
                state.brightnessMode);
            break;
        case Status::UNKNOWN:
        default:
            ALOGE("Unknown error setting light.");
    }
}

static void setLight_native(
        JNIEnv* /* env */,
        jobject /* clazz */,
        jint light,
        jint colorARGB,
        jint flashMode,
        jint onMS,
        jint offMS,
        jint brightnessMode) {

    if (!sLightSupported) {
        return;
    }

    if (!validate(light, flashMode, brightnessMode)) {
        return;
    }

    Type type = static_cast<Type>(light);
    LightState state = constructState(
        colorARGB, flashMode, onMS, offMS, brightnessMode);

    {
        android::base::Timer t;
        sp<ILight> hal = ILight::getService();
        if (hal == nullptr) {
            sLightSupported = false;
            return;
        }
        Return<Status> ret = hal->setLight(type, state);
        processReturn(ret, type, state);
        if (t.duration() > 50ms) ALOGD("Excessive delay setting light");
    }
}

// PICO: HMD display brightness through the PICO HMD service client library.
typedef void* (*PxrGetModuleFunc)();
typedef int (*PxrSetBrightnessFunc)(void* client, int brightness);
typedef int (*PxrSetBrightnessAnimFunc)(void* client, int brightness, int time);
typedef int (*PxrGetBrightnessFunc)(void* client, int* brightness);

static void* sPxrHmdServiceLib = nullptr;
static PxrGetModuleFunc sPxrGetModule = nullptr;
static PxrSetBrightnessFunc sPxrSetBrightness = nullptr;
static PxrSetBrightnessAnimFunc sPxrSetBrightnessAnim = nullptr;
static PxrGetBrightnessFunc sPxrGetBrightness = nullptr;
static const char* sPxrHmdServiceLibName = "libhmdserviceclient.pxr.so";

void* mPxrHmdServiceClient = nullptr;

void pxrHmdServiceSetBrightness_native(
        JNIEnv* /* env */,
        jobject /* clazz */,
        jfloat brightness) {
    if (sPxrSetBrightness == nullptr) {
        ALOGE("Could not find method setBrightness in library:%s", sPxrHmdServiceLibName);
        return;
    }
    if (mPxrHmdServiceClient == nullptr) {
        mPxrHmdServiceClient = sPxrGetModule();
        if (mPxrHmdServiceClient == nullptr) {
            ALOGE("mPxrHmdServiceClient is null");
            return;
        }
    }
    ALOGE("going to call pxrhmdservice .setBrightness, brightness is %f", brightness);
    sPxrSetBrightness(mPxrHmdServiceClient, static_cast<int>(brightness * 100.0f));
}

void pxrHmdServiceSetBrightnessAnim_native(
        JNIEnv* /* env */,
        jobject /* clazz */,
        jint brightness,
        jint time) {
    if (sPxrSetBrightnessAnim == nullptr) {
        ALOGE("Could not find method SetBrightnessAnim in library:%s", sPxrHmdServiceLibName);
        return;
    }
    if (mPxrHmdServiceClient == nullptr) {
        mPxrHmdServiceClient = sPxrGetModule();
        if (mPxrHmdServiceClient == nullptr) {
            ALOGE("mPxrHmdServiceClient is null");
            return;
        }
    }
    int rec = sPxrSetBrightnessAnim(mPxrHmdServiceClient, brightness, time);
    ALOGE("going to call pxrhmdservice.setBrightnessAnim, brightness is %d, time is %d, rec is %d",
            brightness, time, rec);
}

jint pxrHmdServiceGetBrightness_native(
        JNIEnv* /* env */,
        jobject /* clazz */,
        jint brightness) {
    if (sPxrGetBrightness == nullptr) {
        ALOGE("Could not find method getBrightness in library:%s", sPxrHmdServiceLibName);
        return -1;
    }
    if (mPxrHmdServiceClient == nullptr) {
        mPxrHmdServiceClient = sPxrGetModule();
        if (mPxrHmdServiceClient == nullptr) {
            ALOGE("mPxrHmdServiceClient is null");
            return -1;
        }
    }
    int rec = sPxrGetBrightness(mPxrHmdServiceClient, &brightness);
    ALOGE("going to call pxrhmdservice.getBrightness, brightness is %d, rec is %d",
            brightness, rec);
    return brightness;
}

static const JNINativeMethod method_table[] = {
    { "setLight_native", "(IIIIII)V", (void*)setLight_native },
    { "pxrHmdServiceSetBrightness_native", "(F)V", (void*)pxrHmdServiceSetBrightness_native },
    { "pxrHmdServiceSetBrightnessAnim_native", "(II)V",
            (void*)pxrHmdServiceSetBrightnessAnim_native },
    { "pxrHmdServiceGetBrightness_native", "(I)I", (void*)pxrHmdServiceGetBrightness_native },
};

int register_android_server_LightsService(JNIEnv *env) {
    sPxrHmdServiceLib = dlopen(sPxrHmdServiceLibName, RTLD_NOW);
    if (sPxrHmdServiceLib == nullptr) {
        ALOGE("Could not open libhmdserviceclient.pxr.so");
    } else if ((sPxrGetModule = reinterpret_cast<PxrGetModuleFunc>(
            dlsym(sPxrHmdServiceLib, "getPXRModule"))) == nullptr) {
        ALOGE("dlsym getPXRModule failed.Return.");
    } else if ((sPxrSetBrightness = reinterpret_cast<PxrSetBrightnessFunc>(
            dlsym(sPxrHmdServiceLib, "setBrightness"))) == nullptr) {
        ALOGE("dlsym setBrightness failed.Return.");
    } else if ((sPxrSetBrightnessAnim = reinterpret_cast<PxrSetBrightnessAnimFunc>(
            dlsym(sPxrHmdServiceLib, "setBrightnessAnim"))) == nullptr) {
        ALOGE("dlsym setBrightnessAnim failed.Return.");
    } else if ((sPxrGetBrightness = reinterpret_cast<PxrGetBrightnessFunc>(
            dlsym(sPxrHmdServiceLib, "getBrightness"))) == nullptr) {
        ALOGE("dlsym getBrightness failed.Return.");
    } else {
        ALOGD("LightsService load pxrhmdserviceclient.so successfully.");
    }
    return jniRegisterNativeMethods(env, "com/android/server/lights/LightsService",
            method_table, NELEM(method_table));
}

};
