/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_NDEBUG 0
#define LOG_TAG "BootAnimation"

#include <stdint.h>
#include <inttypes.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>

#include <cutils/atomic.h>
#include <cutils/properties.h>

#include <androidfw/AssetManager.h>
#include <binder/IPCThreadState.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/SystemClock.h>

#include <android-base/properties.h>

#include <ui/PixelFormat.h>
#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>

#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

// TODO: Fix Skia.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include <SkBitmap.h>
#include <SkImage.h>
#include <SkStream.h>
#pragma GCC diagnostic pop

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>

#include "BootAnimation.h"

namespace android {

// PICO (factory 5.13.7 libbootanimation): the /product, /apex and /oem/media candidates and the
// dark theme animation are gone; MDM/OEM overrides live in /data and /oem/bootanimation.
static const char SYSTEM_BOOTANIMATION_FILE[] = "/system/media/bootanimation.zip";
static const char SYSTEM_ENCRYPTED_BOOTANIMATION_FILE[] = "/system/media/bootanimation-encrypted.zip";
static const char SYSTEM_SHUTDOWNANIMATION_FILE[] = "/system/media/shutdownanimation.zip";
static const char MDM_BOOTANIMATION_FILE[] = "/data/misc/cusanim/bootanimation.zip";
static const char OEM_BOOTANIMATION_FILE[] = "/data/media/0/bootanimation/bootanimation.zip";
static const char OEM_CP_BOOTANIMATION_FILE[] = "/data/misc/cusanim/bootanimation.zip";
static const char OEM2_BOOTANIMATION_FILE[] = "/oem/bootanimation/bootanimation.zip";
static const char MDM_SHUTDOWNANIMATION_FILE[] = "/data/misc/cusanim/shutdownanimation.zip";
static const char OEM_SHUTDOWNANIMATION_FILE[] = "/data/media/0/bootanimation/shutdownanimation.zip";
static const char OEM2_SHUTDOWNANIMATION_FILE[] = "/oem/bootanimation/shutdownanimation.zip";
static const char CHARGING_ANIMATION_FILE_FORMAT[] = "/system/media/charginganimation_%s.zip";

// Per panel type (ro.pvr.hmd.type) animations.
static const char SYSTEM_BOOTANIMATION_AUO_FILE[] = "/system/media/bootanimation_auo.zip";
static const char SYSTEM_BOOTANIMATION_BOE_FILE[] = "/system/media/bootanimation_boe.zip";
static const char SYSTEM_BOOTANIMATION_JDI4K_FILE[] = "/system/media/bootanimation_jdi4k.zip";
static const char SYSTEM_BOOTANIMATION_SAMSUNG_FILE[] = "/system/media/bootanimation_samsung.zip";
static const char SYSTEM_BOOTANIMATION_SHARP_FILE[] = "/system/media/bootanimation_sharp.zip";
static const char SYSTEM_BOOTANIMATION_SHARP5K_FILE[] = "/system/media/bootanimation_sharp5k.zip";
static const char SYSTEM_BOOTANIMATION_SHARP1KX2_FILE[] = "/system/media/bootanimation_sharp1kx2.zip";
static const char SYSTEM_BOOTANIMATION_JDI1KX2_FILE[] = "/system/media/bootanimation_jdi1kx2.zip";
static const char SYSTEM_BOOTANIMATION_TIANMA_FILE[] = "/system/media/bootanimation_tianma.zip";
static const char SYSTEM_BOOTANIMATION_JDI2KTO4K_FILE[] = "/system/media/bootanimation_jdi2kto4k.zip";
static const char SYSTEM_BOOTANIMATION_JDI1080P_FILE[] = "/system/media/bootanimation_jdi1080p.zip";
static const char SYSTEM_BOOTANIMATION_TIANMA2K_FILE[] = "/system/media/bootanimation_tianma2k.zip";
static const char SYSTEM_SHUTDOWNANIMATION_AUO_FILE[] = "/system/media/shutdownanimation_auo.zip";
static const char SYSTEM_SHUTDOWNANIMATION_BOE_FILE[] = "/system/media/shutdownanimation_boe.zip";
static const char SYSTEM_SHUTDOWNANIMATION_JDI4K_FILE[] = "/system/media/shutdownanimation_jdi4k.zip";
static const char SYSTEM_SHUTDOWNANIMATION_SHARP5K_FILE[] = "/system/media/shutdownanimation_sharp5k.zip";
static const char SYSTEM_SHUTDOWNANIMATION_JDI2KTO4K_FILE[] = "/system/media/shutdownanimation_jdi2kto4k.zip";
static const char SYSTEM_SHUTDOWNANIMATION_JDI1080P_FILE[] = "/system/media/shutdownanimation_jdi1080p.zip";
static const char SYSTEM_SHUTDOWNANIMATION_SAMSUNG_FILE[] = "/system/media/shutdownanimation_samsung.zip";
static const char SYSTEM_SHUTDOWNANIMATION_TIANMA_FILE[] = "/system/media/shutdownanimation_tianma.zip";
static const char SYSTEM_SHUTDOWNANIMATION_TIANMA2K_FILE[] = "/system/media/shutdownanimation_tianma2k.zip";
static const char SYSTEM_SHUTDOWNANIMATION_SHARP_FILE[] = "/system/media/shutdownanimation_sharp.zip";
static const char SYSTEM_SHUTDOWNANIMATION_SHARP1KX2_FILE[] = "/system/media/shutdownanimation_sharp1kx2.zip";
static const char SYSTEM_SHUTDOWNANIMATION_JDI1KX2_FILE[] = "/system/media/shutdownanimation_jdi1kx2.zip";

// sys.animation.status is set (to "shutdown") by the framework before it starts bootanim for
// the shutdown/QuickBoot animation; bootanimation clears it when it ends.
static const char ANIMATION_STATUS_PROP_NAME[] = "sys.animation.status";
static const char CHARGING_LEVEL_PROP_NAME[] = "persist.pvr.charging_level";

// /dev/stabd (PICO stability driver) boot animation notifications: {status, 0}.
static const int32_t STABD_BOOTANIM_START = 7;
static const int32_t STABD_BOOTANIM_STOP = 11;

static const char SYSTEM_DATA_DIR_PATH[] = "/data/system";
static const char SYSTEM_TIME_DIR_NAME[] = "time";
static const char SYSTEM_TIME_DIR_PATH[] = "/data/system/time";
static const char CLOCK_FONT_ASSET[] = "images/clock_font.png";
static const char CLOCK_FONT_ZIP_NAME[] = "clock_font.png";
static const char LAST_TIME_CHANGED_FILE_NAME[] = "last_time_change";
static const char LAST_TIME_CHANGED_FILE_PATH[] = "/data/system/time/last_time_change";
static const char ACCURATE_TIME_FLAG_FILE_NAME[] = "time_is_accurate";
static const char ACCURATE_TIME_FLAG_FILE_PATH[] = "/data/system/time/time_is_accurate";
static const char TIME_FORMAT_12_HOUR_FLAG_FILE_PATH[] = "/data/system/time/time_format_12_hour";
// Java timestamp format. Don't show the clock if the date is before 2000-01-01 00:00:00.
static const long long ACCURATE_TIME_EPOCH = 946684800000;
static constexpr char FONT_BEGIN_CHAR = ' ';
static constexpr char FONT_END_CHAR = '~' + 1;
static constexpr size_t FONT_NUM_CHARS = FONT_END_CHAR - FONT_BEGIN_CHAR + 1;
static constexpr size_t FONT_NUM_COLS = 16;
static constexpr size_t FONT_NUM_ROWS = FONT_NUM_CHARS / FONT_NUM_COLS;
static const int TEXT_CENTER_VALUE = INT_MAX;
static const int TEXT_MISSING_VALUE = INT_MIN;
static const char EXIT_PROP_NAME[] = "service.bootanim.exit";
static const int ANIM_ENTRY_NAME_MAX = 256;
static constexpr size_t TEXT_POS_LEN_MAX = 16;

int JDI493_LEFT[2] = {960, 928};
int JDI493_RIGHT[2] = {960, 2736};

static void notifyStabdStatus(int32_t status) {
    int32_t data[2] = {status, 0};
    int fd = open("/dev/stabd", O_RDWR);
    if (fd < 0) {
        ALOGW("notifyStabdStatus:write stabd driver error");
        return;
    }
    int ret;
    do {
        ret = write(fd, data, sizeof(data));
    } while (ret < 0 && errno == EINTR);
    close(fd);
}

// ---------------------------------------------------------------------------

BootAnimation::BootAnimation(sp<Callbacks> callbacks)
        : Thread(false), mClockEnabled(true), mTimeIsAccurate(false),
        mTimeFormat12Hour(false), mTimeCheckThread(nullptr), mCallbacks(callbacks),
        mChargingAnimation(false) {
    mSession = new SurfaceComposerClient();

    std::string animationStatus = android::base::GetProperty(ANIMATION_STATUS_PROP_NAME, "");
    if (animationStatus.empty()) {
        mShuttingDown = false;
    } else {
        mShuttingDown = true;
    }
    ALOGD("%sAnimationStartTiming start time: %" PRId64 "ms", mShuttingDown ? "Shutdown" : "Boot",
            elapsedRealtime());
    checkIPD();
}

void BootAnimation::checkIPD() {
    char value[PROPERTY_VALUE_MAX];
    property_get("persist.pxr.ipd.status", value, "1");
    int ipdStatus = atoi(value);
    if (mShuttingDown) {
        ALOGD(">>BootAnimation.cpp checkIPD ipd status = %d", ipdStatus);
        switch (ipdStatus) {
            case 1:
                JDI493_LEFT[1] = 1020;
                JDI493_RIGHT[1] = 2644;
                break;
            case 2:
                JDI493_LEFT[1] = 928;
                JDI493_RIGHT[1] = 2736;
                break;
            case 3:
                JDI493_LEFT[1] = 836;
                JDI493_RIGHT[1] = 2827;
                break;
            default:
                break;
        }
    }
}

BootAnimation::~BootAnimation() {
    if (mAnimation != nullptr) {
        releaseAnimation(mAnimation);
        mAnimation = nullptr;
    }
    if (mChargingAnimation) {
        property_set(CHARGING_LEVEL_PROP_NAME, "0");
        mChargingAnimation = false;
    }
    android::base::SetProperty(ANIMATION_STATUS_PROP_NAME, "");
    ALOGD("%sAnimationStopTiming start time: %" PRId64 "ms", mShuttingDown ? "Shutdown" : "Boot",
            elapsedRealtime());
}

void BootAnimation::onFirstRef() {
    status_t err = mSession->linkToComposerDeath(this);
    SLOGE_IF(err, "linkToComposerDeath failed (%s) ", strerror(-err));
    if (err == NO_ERROR) {
        // Load the animation content -- this can be slow (eg 200ms)
        // called before waitForSurfaceFlinger() in main() to avoid wait
        ALOGD("%sAnimationPreloadTiming start time: %" PRId64 "ms",
                mShuttingDown ? "Shutdown" : "Boot", elapsedRealtime());
        preloadAnimation();
        ALOGD("%sAnimationPreloadStopTiming start time: %" PRId64 "ms",
                mShuttingDown ? "Shutdown" : "Boot", elapsedRealtime());
    }
}

sp<SurfaceComposerClient> BootAnimation::session() const {
    return mSession;
}

void BootAnimation::binderDied(const wp<IBinder>&)
{
    // woah, surfaceflinger died!
    SLOGD("SurfaceFlinger died, exiting...");

    android::base::SetProperty(ANIMATION_STATUS_PROP_NAME, "");

    // calling requestExit() is not enough here because the Surface code
    // might be blocked on a condition variable that will never be updated.
    kill( getpid(), SIGKILL );
    requestExit();
    if (mChargingAnimation) {
        property_set(CHARGING_LEVEL_PROP_NAME, "0");
        mChargingAnimation = false;
    }
    ALOGD("binderDied done %d", mChargingAnimation);
}

status_t BootAnimation::initTexture(Texture* texture, AssetManager& assets,
        const char* name) {
    Asset* asset = assets.open(name, Asset::ACCESS_BUFFER);
    if (asset == nullptr)
        return NO_INIT;
    SkBitmap bitmap;
    sk_sp<SkData> data = SkData::MakeWithoutCopy(asset->getBuffer(false),
            asset->getLength());
    sk_sp<SkImage> image = SkImage::MakeFromEncoded(data);
    image->asLegacyBitmap(&bitmap, SkImage::kRO_LegacyBitmapMode);
    asset->close();
    delete asset;

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    texture->w = w;
    texture->h = h;

    glGenTextures(1, &texture->name);
    glBindTexture(GL_TEXTURE_2D, texture->name);

    switch (bitmap.colorType()) {
        case kAlpha_8_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case kARGB_4444_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_SHORT_4_4_4_4, p);
            break;
        case kN32_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case kRGB_565_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, p);
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

    return NO_ERROR;
}

status_t BootAnimation::initTexture(FileMap* map, int* width, int* height)
{
    SkBitmap bitmap;
    sk_sp<SkData> data = SkData::MakeWithoutCopy(map->getDataPtr(),
            map->getDataLength());
    sk_sp<SkImage> image = SkImage::MakeFromEncoded(data);
    image->asLegacyBitmap(&bitmap, SkImage::kRO_LegacyBitmapMode);

    // FileMap memory is never released until application exit.
    // Release it now as the texture is already loaded and the memory used for
    // the packed resource can be released.
    delete map;

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    int tw = 1 << (31 - __builtin_clz(w));
    int th = 1 << (31 - __builtin_clz(h));
    if (tw < w) tw <<= 1;
    if (th < h) th <<= 1;

    switch (bitmap.colorType()) {
        case kN32_SkColorType:
            if (!mUseNpotTextures && (tw != w || th != h)) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, nullptr);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, p);
            }
            break;

        case kRGB_565_SkColorType:
            if (!mUseNpotTextures && (tw != w || th != h)) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, nullptr);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, p);
            }
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);

    *width = w;
    *height = h;

    return NO_ERROR;
}

status_t BootAnimation::readyToRun() {
    property_get("ro.pvr.hmd.type", mHmdType, "AUO");
    property_get("ro.pxr.externalfunc", mRoExternalFunc, "0");
    property_get("persist.pxr.externalfunc", mPersistExternalFunc, "0");
    property_get("ro.product.name", mProductName, "0");
    property_get("ro.product.model", mProductModel, "0");

    mAssets.addDefaultAssets();

    mDisplayToken = SurfaceComposerClient::getInternalDisplayToken();
    if (mDisplayToken == nullptr)
        return -1;

    DisplayInfo dinfo;
    status_t status = SurfaceComposerClient::getDisplayInfo(mDisplayToken, &dinfo);
    if (status)
        return -1;

    // create the native surface
    sp<SurfaceControl> control;
    if (!strcmp(mHmdType, "JDI552KT4K") || !strcmp(mHmdType, "JDI554K") ||
            !strcmp(mHmdType, "JDI493") || !strncmp(mProductModel, "Pico Neo3", 9)) {
        if (mShuttingDown) {
            // The JDI shutdown animation is drawn on a surface with width and height swapped.
            ALOGD("make jdi shutdown anim surface");
            control = session()->createSurface(String8("BootAnimation"),
                    dinfo.h, dinfo.w, PIXEL_FORMAT_RGB_565);
        } else {
            ALOGD("make jdi boot anim surface");
            control = session()->createSurface(String8("BootAnimation"),
                    dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);
        }
    } else if (!strcmp(mHmdType, "INNOLUX5K") || !strcmp(mHmdType, "SHARP5K") ||
            !strncmp(mProductName, "Phoenix", 7)) {
        if (mShuttingDown) {
            ALOGD("make phx shutdown anim surface");
            control = session()->createSurface(String8("BootAnimation"),
                    dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);
        } else {
            ALOGD("make phx boot anim surface");
            control = session()->createSurface(String8("BootAnimation"),
                    dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);
        }
    } else {
        ALOGD("make boot anim surface");
        control = session()->createSurface(String8("BootAnimation"),
                dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);
    }

    SurfaceComposerClient::Transaction t;
    t.setLayer(control, 0x7fffffff)
        .apply();

    sp<Surface> s = control->getSurface();

    // initialize opengl and egl
    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_NONE
    };
    EGLint w, h;
    EGLint numConfigs;
    EGLConfig config;
    EGLSurface surface;
    EGLContext context;

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    eglInitialize(display, nullptr, nullptr);
    eglChooseConfig(display, attribs, &config, 1, &numConfigs);
    surface = eglCreateWindowSurface(display, config, s.get(), nullptr);
    context = eglCreateContext(display, config, nullptr, nullptr);
    eglQuerySurface(display, surface, EGL_WIDTH, &w);
    eglQuerySurface(display, surface, EGL_HEIGHT, &h);

    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE)
        return NO_INIT;

    mDisplay = display;
    mContext = context;
    mSurface = surface;
    mWidth = w;
    mHeight = h;
    mFlingerSurfaceControl = control;
    mFlingerSurface = s;
    mTargetInset = -1;

    return NO_ERROR;
}

bool BootAnimation::preloadAnimation() {
    findBootAnimationFile();
    if (!mZipFileName.isEmpty()) {
        mAnimation = loadAnimation(mZipFileName);
        return (mAnimation != nullptr);
    }

    return false;
}

// Returns the animation that replaces defaultFile: the panel/MDM/OEM boot animation, the
// charging animation selected by persist.pvr.charging_level, or the panel/MDM/OEM shutdown
// animation.
const char* BootAnimation::getAnimationFileName(const char* defaultFile, bool shuttingDown) {
    const char* file;
    if (!shuttingDown) {
        file = getBootAnimationFileName();
    } else {
        property_get(CHARGING_LEVEL_PROP_NAME, mChargingLevel, "0");
        if (strcmp(mChargingLevel, "0") != 0 && strcmp(mChargingLevel, "-1") != 0) {
            // The factory returns the buffer of a String8 that it has already destroyed; the
            // path is kept in a static buffer here instead.
            static char chargingFile[128];
            snprintf(chargingFile, sizeof(chargingFile), CHARGING_ANIMATION_FILE_FORMAT,
                    mChargingLevel);
            ALOGD("get charging animation file name %s ", chargingFile);
            if (access(chargingFile, R_OK) == 0) {
                mChargingAnimation = true;
                return chargingFile;
            }
            ALOGD("get charging animation file error, return default file.");
            return defaultFile;
        }
        ALOGD("start get shutdown animation file name.");
        file = getShutAnimationFileName();
    }
    return file != nullptr ? file : defaultFile;
}

const char* BootAnimation::getBootAnimationFileName() {
    property_get("ro.pvr.hmd.type", mHmdType, "AUO");
    property_get("ro.pxr.externalfunc", mRoExternalFunc, "0");
    property_get("persist.pxr.externalfunc", mPersistExternalFunc, "0");
    property_get("ro.product.name", mProductName, "0");
    property_get("ro.product.model", mProductModel, "0");

    const char* file;
    if (!strcmp(mHmdType, "AUO")) {
        ALOGD("BootAnimation use auo");
        file = SYSTEM_BOOTANIMATION_AUO_FILE;
    } else if (!strcmp(mHmdType, "BOE")) {
        ALOGD("BootAnimation use boe");
        file = SYSTEM_BOOTANIMATION_BOE_FILE;
    } else if (!strcmp(mHmdType, "JDI552KT4K") || !strcmp(mHmdType, "JDI554K") ||
            !strcmp(mHmdType, "JDI493")) {
        ALOGD("BootAnimation use jdi55");
        file = SYSTEM_BOOTANIMATION_JDI4K_FILE;
    } else if (!strcmp(mHmdType, "JDI35x2")) {
        ALOGD("BootAnimation use JDI35x2");
        file = SYSTEM_BOOTANIMATION_BOE_FILE;
    } else if (!strcmp(mHmdType, "Samsung")) {
        ALOGD("BootAnimation use samsung");
        file = SYSTEM_BOOTANIMATION_SAMSUNG_FILE;
    } else if (!strcmp(mHmdType, "Sharp4k")) {
        ALOGD("BootAnimation use sharp");
        file = SYSTEM_BOOTANIMATION_SHARP_FILE;
    } else if (!strcmp(mHmdType, "INNOLUX5K") || !strcmp(mHmdType, "SHARP5K")) {
        ALOGD("BootAnimation use INNOLUX5K");
        file = SYSTEM_BOOTANIMATION_SHARP5K_FILE;
    } else if (!strcmp(mHmdType, "Sharp1Kx2")) {
        ALOGD("BootAnimation use sharp");
        file = SYSTEM_BOOTANIMATION_SHARP1KX2_FILE;
    } else if (!strcmp(mHmdType, "JDI1Kx2")) {
        ALOGD("BootAnimation use JDI1Kx2");
        file = SYSTEM_BOOTANIMATION_JDI1KX2_FILE;
    } else if (!strcmp(mHmdType, "TIANMA")) {
        ALOGD("BootAnimation use TIANMA");
        file = SYSTEM_BOOTANIMATION_TIANMA_FILE;
    } else if (!strcmp(mHmdType, "JDI2KTO4K")) {
        ALOGD("BootAnimation use JDI2KTO4K");
        file = SYSTEM_BOOTANIMATION_JDI2KTO4K_FILE;
    } else if (!strcmp(mHmdType, "JDI4K")) {
        ALOGD("BootAnimation use JDI4K");
        file = SYSTEM_BOOTANIMATION_JDI4K_FILE;
    } else if (!strcmp(mHmdType, "JDI1080P")) {
        ALOGD("BootAnimation use JDI1080P");
        file = SYSTEM_BOOTANIMATION_JDI1080P_FILE;
    } else if (!strcmp(mHmdType, "Tianma2K")) {
        ALOGD("BootAnimation use TIANMA2K");
        file = SYSTEM_BOOTANIMATION_TIANMA2K_FILE;
    } else if (!strncmp(mProductName, "Phoenix", 7)) {
        ALOGD("BootAnimation use PhxDefault");
        file = SYSTEM_BOOTANIMATION_SHARP5K_FILE;
    } else if (!strncmp(mProductModel, "Pico Neo3", 9)) {
        ALOGD("BootAnimation use Neo3Default");
        file = SYSTEM_BOOTANIMATION_JDI4K_FILE;
    } else {
        ALOGD("BootAnimation use default auo");
        file = SYSTEM_BOOTANIMATION_FILE;
    }

    // strcmp() == 1: the property value starts with '1' (bionic returns the byte difference).
    if (strcmp(mRoExternalFunc, "0") == 1 || strcmp(mPersistExternalFunc, "0") == 1) {
        if (access(MDM_BOOTANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return MDM_BOOTANIMATION_FILE: %s", MDM_BOOTANIMATION_FILE);
            return MDM_BOOTANIMATION_FILE;
        }
        if (access(OEM_BOOTANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return OEM_BOOTANIMATION_FILE: %s", OEM_BOOTANIMATION_FILE);
            return OEM_BOOTANIMATION_FILE;
        }
        if (access(OEM_CP_BOOTANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return OEM_CP_BOOTANIMATION_FILE: %s",
                    OEM_CP_BOOTANIMATION_FILE);
            return OEM_CP_BOOTANIMATION_FILE;
        }
        ALOGD("getAnimationFileName OEM_CP_BOOTANIMATION_FILE: %s not access",
                OEM_CP_BOOTANIMATION_FILE);
        if (access(OEM2_BOOTANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return OEM2_BOOTANIMATION_FILE: %s",
                    OEM2_BOOTANIMATION_FILE);
            return OEM2_BOOTANIMATION_FILE;
        }
    }

    if (access(file, R_OK) == 0) {
        return file;
    }
    return SYSTEM_BOOTANIMATION_FILE;
}

const char* BootAnimation::getShutAnimationFileName() {
    property_get("ro.pvr.hmd.type", mHmdType, "AUO");
    property_get("ro.pxr.externalfunc", mRoExternalFunc, "0");
    property_get("persist.pxr.externalfunc", mPersistExternalFunc, "0");
    property_get("persist.picovr.funnylens.enable", mFunnyLens, "0");
    property_get("ro.product.name", mProductName, "0");
    property_get("ro.product.model", mProductModel, "0");

    const char* file;
    if (!strcmp(mHmdType, "AUO")) {
        ALOGD("ShutAnimation use auo");
        file = SYSTEM_SHUTDOWNANIMATION_AUO_FILE;
    } else if (!strcmp(mHmdType, "BOE")) {
        ALOGD("ShutAnimation use boe");
        file = SYSTEM_SHUTDOWNANIMATION_BOE_FILE;
    } else if (!strcmp(mHmdType, "JDI552KT4K") || !strcmp(mHmdType, "JDI554K") ||
            !strcmp(mHmdType, "JDI493")) {
        ALOGD("ShutAnimation use jdi55");
        file = SYSTEM_SHUTDOWNANIMATION_JDI4K_FILE;
    } else if (!strcmp(mHmdType, "INNOLUX5K") || !strcmp(mHmdType, "SHARP5K")) {
        ALOGD("ShutAnimation use INNOLUX5K");
        file = SYSTEM_SHUTDOWNANIMATION_SHARP5K_FILE;
    } else if (!strcmp(mHmdType, "JDI35x2")) {
        ALOGD("ShutAnimation use jdi");
        file = SYSTEM_SHUTDOWNANIMATION_BOE_FILE;
    } else if (!strcmp(mHmdType, "JDI2KTO4K")) {
        ALOGD("ShutAnimation use JDI2KTO4K");
        file = SYSTEM_SHUTDOWNANIMATION_JDI2KTO4K_FILE;
    } else if (!strcmp(mHmdType, "JDI4K")) {
        ALOGD("ShutAnimation use JDI4K");
        file = SYSTEM_SHUTDOWNANIMATION_JDI4K_FILE;
    } else if (!strcmp(mHmdType, "JDI1080P")) {
        ALOGD("ShutAnimation use JDI1080P");
        file = SYSTEM_SHUTDOWNANIMATION_JDI1080P_FILE;
    } else if (!strcmp(mHmdType, "Samsung")) {
        ALOGD("ShutAnimation use samsung");
        file = SYSTEM_SHUTDOWNANIMATION_SAMSUNG_FILE;
    } else if (!strcmp(mHmdType, "TIANMA")) {
        ALOGD("ShutAnimation use tianma");
        file = SYSTEM_SHUTDOWNANIMATION_TIANMA_FILE;
    } else if (!strcmp(mHmdType, "Tianma2K")) {
        ALOGD("ShutAnimation use tianma2k");
        file = SYSTEM_SHUTDOWNANIMATION_TIANMA2K_FILE;
    } else if (!strcmp(mHmdType, "Sharp4k")) {
        ALOGD("ShutAnimation use sharp");
        file = SYSTEM_SHUTDOWNANIMATION_SHARP_FILE;
    } else if (!strcmp(mHmdType, "Sharp1Kx2")) {
        ALOGD("ShutAnimation use sharp");
        file = SYSTEM_SHUTDOWNANIMATION_SHARP1KX2_FILE;
    } else if (!strcmp(mHmdType, "JDI1Kx2")) {
        ALOGD("ShutAnimation use jdi");
        file = SYSTEM_SHUTDOWNANIMATION_JDI1KX2_FILE;
    } else if (!strncmp(mProductName, "Phoenix", 7)) {
        ALOGD("ShutAnimation use PhxDefault");
        file = SYSTEM_SHUTDOWNANIMATION_SHARP5K_FILE;
    } else if (!strncmp(mProductModel, "Pico Neo3", 9)) {
        ALOGD("ShutAnimation use Neo3Default");
        file = SYSTEM_SHUTDOWNANIMATION_JDI4K_FILE;
    } else {
        ALOGD("ShutAnimation use default auo");
        file = SYSTEM_SHUTDOWNANIMATION_FILE;
    }

    // strcmp() == 1: the property value starts with '1' (bionic returns the byte difference).
    if (strcmp(mRoExternalFunc, "0") == 1 || strcmp(mPersistExternalFunc, "0") == 1) {
        if (access(MDM_SHUTDOWNANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return MDM_SHUTDOWNANIMATION_FILE: %s",
                    MDM_SHUTDOWNANIMATION_FILE);
            return MDM_SHUTDOWNANIMATION_FILE;
        }
        if (access(OEM_SHUTDOWNANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return OEM_SHUTDOWNANIMATION_FILE: %s",
                    OEM_SHUTDOWNANIMATION_FILE);
            return OEM_SHUTDOWNANIMATION_FILE;
        }
        if (access(OEM2_SHUTDOWNANIMATION_FILE, R_OK) == 0) {
            ALOGD("getAnimationFileName return OEM2_SHUTDOWNANIMATION_FILE: %s",
                    OEM2_SHUTDOWNANIMATION_FILE);
            return OEM2_SHUTDOWNANIMATION_FILE;
        }
    }

    if (access(file, R_OK) == 0) {
        return file;
    }
    return SYSTEM_SHUTDOWNANIMATION_FILE;
}

void BootAnimation::findBootAnimationFile() {
    // If the device has encryption turned on or is in process
    // of being encrypted we show the encrypted boot animation.
    char decrypt[PROPERTY_VALUE_MAX];
    property_get("vold.decrypt", decrypt, "");

    bool encryptedAnimation = atoi(decrypt) != 0 ||
        !strcmp("trigger_restart_min_framework", decrypt);

    if (!mShuttingDown && encryptedAnimation) {
        static const char* encryptedBootFiles[] = {SYSTEM_ENCRYPTED_BOOTANIMATION_FILE};
        for (const char* f : encryptedBootFiles) {
            if (access(getAnimationFileName(f, mShuttingDown), R_OK) == 0) {
                mZipFileName = getAnimationFileName(f, mShuttingDown);
                return;
            }
        }
    }

    std::string custAnimProp = !mShuttingDown ?
        android::base::GetProperty("persist.sys.customanim.boot", ""):
        android::base::GetProperty("persist.sys.customanim.shutdown", "");
    const char *custAnim = custAnimProp.c_str();
    ALOGD("Animation customzation path: %s", custAnim);
    if (access(custAnim, R_OK) == 0) {
        mZipFileName = custAnim;
        ALOGD("%sAnimation customzation path: %s", mShuttingDown ? "Shutdown" : "Boot", mZipFileName.c_str());
        return;
    }

    static const char* bootFiles[] =
        {OEM2_BOOTANIMATION_FILE, MDM_BOOTANIMATION_FILE, OEM_BOOTANIMATION_FILE,
         SYSTEM_BOOTANIMATION_FILE};
    static const char* shutdownFiles[] =
        {OEM2_SHUTDOWNANIMATION_FILE, MDM_SHUTDOWNANIMATION_FILE, OEM_SHUTDOWNANIMATION_FILE,
         SYSTEM_SHUTDOWNANIMATION_FILE};

    for (const char* f : (!mShuttingDown ? bootFiles : shutdownFiles)) {
        if (access(getAnimationFileName(f, mShuttingDown), R_OK) == 0) {
            mZipFileName = getAnimationFileName(f, mShuttingDown);
            return;
        }
    }
}

bool BootAnimation::threadLoop()
{
    bool r = false;
    notifyStabdStatus(STABD_BOOTANIM_START);
    // We have no bootanimation file, so we use the stock android logo
    // animation.
    if (mZipFileName.isEmpty()) {
        android();
    } else {
        movie();
    }
    if (mChargingAnimation) {
        property_set(CHARGING_LEVEL_PROP_NAME, "-1");
    }
    ALOGD("threadLoop done %d, %d", r, mChargingAnimation);

    eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(mDisplay, mContext);
    eglDestroySurface(mDisplay, mSurface);
    mFlingerSurface.clear();
    mFlingerSurfaceControl.clear();
    eglTerminate(mDisplay);
    eglReleaseThread();
    IPCThreadState::self()->stopProcess();
    notifyStabdStatus(STABD_BOOTANIM_STOP);
    return r;
}

bool BootAnimation::android()
{
    SLOGD("%sAnimationShownTiming start time: %" PRId64 "ms", mShuttingDown ? "Shutdown" : "Boot",
            elapsedRealtime());
    initTexture(&mAndroid[0], mAssets, "images/android-logo-mask.png");
    initTexture(&mAndroid[1], mAssets, "images/android-logo-shine.png");

    mCallbacks->init({});

    // clear screen
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glClearColor(0,0,0,1);
    glClear(GL_COLOR_BUFFER_BIT);
    eglSwapBuffers(mDisplay, mSurface);

    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const GLint xc = (mWidth  - mAndroid[0].w) / 2;
    const GLint yc = (mHeight - mAndroid[0].h) / 2;
    const Rect updateRect(xc, yc, xc + mAndroid[0].w, yc + mAndroid[0].h);

    glScissor(updateRect.left, mHeight - updateRect.bottom, updateRect.width(),
            updateRect.height());

    // Blend state
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const nsecs_t startTime = systemTime();
    do {
        nsecs_t now = systemTime();
        double time = now - startTime;
        float t = 4.0f * float(time / us2ns(16667)) / mAndroid[1].w;
        GLint offset = (1 - (t - floorf(t))) * mAndroid[1].w;
        GLint x = xc - offset;

        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[1].name);
        glDrawTexiOES(x,                 yc, 0, mAndroid[1].w, mAndroid[1].h);
        glDrawTexiOES(x + mAndroid[1].w, yc, 0, mAndroid[1].w, mAndroid[1].h);

        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[0].name);
        glDrawTexiOES(xc, yc, 0, mAndroid[0].w, mAndroid[0].h);

        EGLBoolean res = eglSwapBuffers(mDisplay, mSurface);
        if (res == EGL_FALSE)
            break;

        // 12fps: don't animate too fast to preserve CPU
        const nsecs_t sleepTime = 83333 - ns2us(systemTime() - now);
        if (sleepTime > 0)
            usleep(sleepTime);

        checkExit();
    } while (!exitPending());

    glDeleteTextures(1, &mAndroid[0].name);
    glDeleteTextures(1, &mAndroid[1].name);
    return false;
}

void BootAnimation::checkExit() {
    // Allow surface flinger to gracefully request shutdown
    char value[PROPERTY_VALUE_MAX];
    property_get(EXIT_PROP_NAME, value, "0");
    int exitnow = atoi(value);
    // PICO: the boot animation only ends once stationservice reports station.upgrade.flag=2
    // (or ro.pxr.station.upgrade.flag=2); the shutdown animation never ends by itself.
    int stationStatus = property_get_int32("station.upgrade.flag", 0);
    int roStationStatus = property_get_int32("ro.pxr.station.upgrade.flag", 0);
    if (mShuttingDown) {
        ALOGD(">>BootAnimation.cpp checkExit isshutdown = %d ignore exit", mShuttingDown);
        exitnow = 0;
    }
    ALOGI("checkExit exitnow %d station_status %d ", exitnow, stationStatus);
    if (exitnow && (stationStatus == 2 || roStationStatus == 2)) {
        if (mChargingAnimation) {
            property_set(CHARGING_LEVEL_PROP_NAME, "0");
            mChargingAnimation = false;
        }
        ALOGD("checkExit done %d", mChargingAnimation);
        requestExit();
        mCallbacks->shutdown();
    }
}

bool BootAnimation::validClock(const Animation::Part& part) {
    return part.clockPosX != TEXT_MISSING_VALUE && part.clockPosY != TEXT_MISSING_VALUE;
}

bool parseTextCoord(const char* str, int* dest) {
    if (strcmp("c", str) == 0) {
        *dest = TEXT_CENTER_VALUE;
        return true;
    }

    char* end;
    int val = (int) strtol(str, &end, 0);
    if (end == str || *end != '\0' || val == INT_MAX || val == INT_MIN) {
        return false;
    }
    *dest = val;
    return true;
}

// Parse two position coordinates. If only string is non-empty, treat it as the y value.
void parsePosition(const char* str1, const char* str2, int* x, int* y) {
    bool success = false;
    if (strlen(str1) == 0) {  // No values were specified
        // success = false
    } else if (strlen(str2) == 0) {  // we have only one value
        if (parseTextCoord(str1, y)) {
            *x = TEXT_CENTER_VALUE;
            success = true;
        }
    } else {
        if (parseTextCoord(str1, x) && parseTextCoord(str2, y)) {
            success = true;
        }
    }

    if (!success) {
        *x = TEXT_MISSING_VALUE;
        *y = TEXT_MISSING_VALUE;
    }
}

// Parse a color represented as an HTML-style 'RRGGBB' string: each pair of
// characters in str is a hex number in [0, 255], which are converted to
// floating point values in the range [0.0, 1.0] and placed in the
// corresponding elements of color.
//
// If the input string isn't valid, parseColor returns false and color is
// left unchanged.
static bool parseColor(const char str[7], float color[3]) {
    float tmpColor[3];
    for (int i = 0; i < 3; i++) {
        int val = 0;
        for (int j = 0; j < 2; j++) {
            val *= 16;
            char c = str[2*i + j];
            if      (c >= '0' && c <= '9') val += c - '0';
            else if (c >= 'A' && c <= 'F') val += (c - 'A') + 10;
            else if (c >= 'a' && c <= 'f') val += (c - 'a') + 10;
            else                           return false;
        }
        tmpColor[i] = static_cast<float>(val) / 255.0f;
    }
    memcpy(color, tmpColor, sizeof(tmpColor));
    return true;
}


static bool readFile(ZipFileRO* zip, const char* name, String8& outString)
{
    ZipEntryRO entry = zip->findEntryByName(name);
    SLOGE_IF(!entry, "couldn't find %s", name);
    if (!entry) {
        return false;
    }

    FileMap* entryMap = zip->createEntryFileMap(entry);
    zip->releaseEntry(entry);
    SLOGE_IF(!entryMap, "entryMap is null");
    if (!entryMap) {
        return false;
    }

    outString.setTo((char const*)entryMap->getDataPtr(), entryMap->getDataLength());
    delete entryMap;
    return true;
}

// The font image should be a 96x2 array of character images.  The
// columns are the printable ASCII characters 0x20 - 0x7f.  The
// top row is regular text; the bottom row is bold.
status_t BootAnimation::initFont(Font* font, const char* fallback) {
    status_t status = NO_ERROR;

    if (font->map != nullptr) {
        glGenTextures(1, &font->texture.name);
        glBindTexture(GL_TEXTURE_2D, font->texture.name);

        status = initTexture(font->map, &font->texture.w, &font->texture.h);

        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    } else if (fallback != nullptr) {
        status = initTexture(&font->texture, mAssets, fallback);
    } else {
        return NO_INIT;
    }

    if (status == NO_ERROR) {
        font->char_width = font->texture.w / FONT_NUM_COLS;
        font->char_height = font->texture.h / FONT_NUM_ROWS / 2;  // There are bold and regular rows
    }

    return status;
}

void BootAnimation::drawText(const char* str, const Font& font, bool bold, int* x, int* y) {
    glEnable(GL_BLEND);  // Allow us to draw on top of the animation
    glBindTexture(GL_TEXTURE_2D, font.texture.name);

    const int len = strlen(str);
    const int strWidth = font.char_width * len;

    if (*x == TEXT_CENTER_VALUE) {
        *x = (mWidth - strWidth) / 2;
    } else if (*x < 0) {
        *x = mWidth + *x - strWidth;
    }
    if (*y == TEXT_CENTER_VALUE) {
        *y = (mHeight - font.char_height) / 2;
    } else if (*y < 0) {
        *y = mHeight + *y - font.char_height;
    }

    int cropRect[4] = { 0, 0, font.char_width, -font.char_height };

    for (int i = 0; i < len; i++) {
        char c = str[i];

        if (c < FONT_BEGIN_CHAR || c > FONT_END_CHAR) {
            c = '?';
        }

        // Crop the texture to only the pixels in the current glyph
        const int charPos = (c - FONT_BEGIN_CHAR);  // Position in the list of valid characters
        const int row = charPos / FONT_NUM_COLS;
        const int col = charPos % FONT_NUM_COLS;
        cropRect[0] = col * font.char_width;  // Left of column
        cropRect[1] = row * font.char_height * 2; // Top of row
        // Move down to bottom of regular (one char_heigh) or bold (two char_heigh) line
        cropRect[1] += bold ? 2 * font.char_height : font.char_height;
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, cropRect);

        glDrawTexiOES(*x, *y, 0, font.char_width, font.char_height);

        *x += font.char_width;
    }

    glDisable(GL_BLEND);  // Return to the animation's default behaviour
    glBindTexture(GL_TEXTURE_2D, 0);
}

// We render 12 or 24 hour time.
void BootAnimation::drawClock(const Font& font, const int xPos, const int yPos) {
    static constexpr char TIME_FORMAT_12[] = "%l:%M";
    static constexpr char TIME_FORMAT_24[] = "%H:%M";
    static constexpr int TIME_LENGTH = 6;

    time_t rawtime;
    time(&rawtime);
    struct tm* timeInfo = localtime(&rawtime);

    char timeBuff[TIME_LENGTH];
    const char* timeFormat = mTimeFormat12Hour ? TIME_FORMAT_12 : TIME_FORMAT_24;
    size_t length = strftime(timeBuff, TIME_LENGTH, timeFormat, timeInfo);

    if (length != TIME_LENGTH - 1) {
        SLOGE("Couldn't format time; abandoning boot animation clock");
        mClockEnabled = false;
        return;
    }

    char* out = timeBuff[0] == ' ' ? &timeBuff[1] : &timeBuff[0];
    int x = xPos;
    int y = yPos;
    drawText(out, font, false, &x, &y);
}

bool BootAnimation::parseAnimationDesc(Animation& animation)
{
    String8 desString;

    if (!readFile(animation.zip, "desc.txt", desString)) {
        return false;
    }
    char const* s = desString.string();

    // Parse the description file
    for (;;) {
        const char* endl = strstr(s, "\n");
        if (endl == nullptr) break;
        String8 line(s, endl - s);
        const char* l = line.string();
        int fps = 0;
        int width = 0;
        int height = 0;
        int count = 0;
        int pause = 0;
        char path[ANIM_ENTRY_NAME_MAX];
        char color[7] = "000000"; // default to black if unspecified
        char clockPos1[TEXT_POS_LEN_MAX + 1] = "";
        char clockPos2[TEXT_POS_LEN_MAX + 1] = "";

        char pathType;
        if (sscanf(l, "%d %d %d", &width, &height, &fps) == 3) {
            // SLOGD("> w=%d, h=%d, fps=%d", width, height, fps);
            animation.width = width;
            animation.height = height;
            animation.fps = fps;
        } else if (sscanf(l, " %c %d %d %s #%6s %16s %16s",
                          &pathType, &count, &pause, path, color, clockPos1, clockPos2) >= 4) {
            //SLOGD("> type=%c, count=%d, pause=%d, path=%s, color=%s, clockPos1=%s, clockPos2=%s",
            //    pathType, count, pause, path, color, clockPos1, clockPos2);
            Animation::Part part;
            part.playUntilComplete = pathType == 'c';
            part.count = count;
            part.pause = pause;
            part.path = path;
            part.audioData = nullptr;
            part.animation = nullptr;
            if (!parseColor(color, part.backgroundColor)) {
                SLOGE("> invalid color '#%s'", color);
                part.backgroundColor[0] = 0.0f;
                part.backgroundColor[1] = 0.0f;
                part.backgroundColor[2] = 0.0f;
            }
            parsePosition(clockPos1, clockPos2, &part.clockPosX, &part.clockPosY);
            animation.parts.add(part);
        }
        else if (strcmp(l, "$SYSTEM") == 0) {
            // SLOGD("> SYSTEM");
            Animation::Part part;
            part.playUntilComplete = false;
            part.count = 1;
            part.pause = 0;
            part.audioData = nullptr;
            part.animation = loadAnimation(String8(SYSTEM_BOOTANIMATION_FILE));
            if (part.animation != nullptr)
                animation.parts.add(part);
        }
        s = ++endl;
    }

    return true;
}

bool BootAnimation::preloadZip(Animation& animation)
{
    // read all the data structures
    const size_t pcount = animation.parts.size();
    void *cookie = nullptr;
    ZipFileRO* zip = animation.zip;
    if (!zip->startIteration(&cookie)) {
        return false;
    }

    ZipEntryRO entry;
    char name[ANIM_ENTRY_NAME_MAX];
    while ((entry = zip->nextEntry(cookie)) != nullptr) {
        const int foundEntryName = zip->getEntryFileName(entry, name, ANIM_ENTRY_NAME_MAX);
        if (foundEntryName > ANIM_ENTRY_NAME_MAX || foundEntryName == -1) {
            SLOGE("Error fetching entry file name");
            continue;
        }

        const String8 entryName(name);
        const String8 path(entryName.getPathDir());
        const String8 leaf(entryName.getPathLeaf());
        if (leaf.size() > 0) {
            if (entryName == CLOCK_FONT_ZIP_NAME) {
                FileMap* map = zip->createEntryFileMap(entry);
                if (map) {
                    animation.clockFont.map = map;
                }
                continue;
            }

            for (size_t j = 0; j < pcount; j++) {
                if (path == animation.parts[j].path) {
                    uint16_t method;
                    // supports only stored png files
                    if (zip->getEntryInfo(entry, &method, nullptr, nullptr, nullptr, nullptr, nullptr)) {
                        if (method == ZipFileRO::kCompressStored) {
                            FileMap* map = zip->createEntryFileMap(entry);
                            if (map) {
                                Animation::Part& part(animation.parts.editItemAt(j));
                                if (leaf == "audio.wav") {
                                    // a part may have at most one audio file
                                    part.audioData = (uint8_t *)map->getDataPtr();
                                    part.audioLength = map->getDataLength();
                                } else if (leaf == "trim.txt") {
                                    part.trimData.setTo((char const*)map->getDataPtr(),
                                                        map->getDataLength());
                                } else {
                                    Animation::Frame frame;
                                    frame.name = leaf;
                                    frame.map = map;
                                    frame.trimWidth = animation.width;
                                    frame.trimHeight = animation.height;
                                    frame.trimX = 0;
                                    frame.trimY = 0;
                                    part.frames.add(frame);
                                }
                            }
                        } else {
                            SLOGE("bootanimation.zip is compressed; must be only stored");
                        }
                    }
                }
            }
        }
    }

    // If there is trimData present, override the positioning defaults.
    for (Animation::Part& part : animation.parts) {
        const char* trimDataStr = part.trimData.string();
        for (size_t frameIdx = 0; frameIdx < part.frames.size(); frameIdx++) {
            const char* endl = strstr(trimDataStr, "\n");
            // No more trimData for this part.
            if (endl == nullptr) {
                break;
            }
            String8 line(trimDataStr, endl - trimDataStr);
            const char* lineStr = line.string();
            trimDataStr = ++endl;
            int width = 0, height = 0, x = 0, y = 0;
            if (sscanf(lineStr, "%dx%d+%d+%d", &width, &height, &x, &y) == 4) {
                Animation::Frame& frame(part.frames.editItemAt(frameIdx));
                frame.trimWidth = width;
                frame.trimHeight = height;
                frame.trimX = x;
                frame.trimY = y;
            } else {
                SLOGE("Error parsing trim.txt, line: %s", lineStr);
                break;
            }
        }
    }

    zip->endIteration(cookie);

    return true;
}

bool BootAnimation::movie()
{
    if (mAnimation == nullptr) {
        mAnimation = loadAnimation(mZipFileName);
    }

    if (mAnimation == nullptr)
        return false;

    // mCallbacks->init() may get called recursively,
    // this loop is needed to get the same results
    for (const Animation::Part& part : mAnimation->parts) {
        if (part.animation != nullptr) {
            mCallbacks->init(part.animation->parts);
        }
    }
    mCallbacks->init(mAnimation->parts);

    bool anyPartHasClock = false;
    for (size_t i=0; i < mAnimation->parts.size(); i++) {
        if(validClock(mAnimation->parts[i])) {
            anyPartHasClock = true;
            break;
        }
    }
    if (!anyPartHasClock) {
        mClockEnabled = false;
    }

    // Check if npot textures are supported
    mUseNpotTextures = false;
    String8 gl_extensions;
    const char* exts = reinterpret_cast<const char*>(glGetString(GL_EXTENSIONS));
    if (!exts) {
        glGetError();
    } else {
        gl_extensions.setTo(exts);
        if ((gl_extensions.find("GL_ARB_texture_non_power_of_two") != -1) ||
            (gl_extensions.find("GL_OES_texture_npot") != -1)) {
            mUseNpotTextures = true;
        }
    }

    // Blend required to draw time on top of animation frames.
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_BLEND);

    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    bool clockFontInitialized = false;
    if (mClockEnabled) {
        clockFontInitialized =
            (initFont(&mAnimation->clockFont, CLOCK_FONT_ASSET) == NO_ERROR);
        mClockEnabled = clockFontInitialized;
    }

    if (mClockEnabled && !updateIsTimeAccurate()) {
        mTimeCheckThread = new TimeCheckThread(this);
        mTimeCheckThread->run("BootAnimation::TimeCheckThread", PRIORITY_NORMAL);
    }

    playAnimation(*mAnimation);

    if (mTimeCheckThread != nullptr) {
        mTimeCheckThread->requestExit();
        mTimeCheckThread = nullptr;
    }

    if (clockFontInitialized) {
        glDeleteTextures(1, &mAnimation->clockFont.texture.name);
    }

    releaseAnimation(mAnimation);
    mAnimation = nullptr;

    return false;
}

bool BootAnimation::playAnimation(const Animation& animation)
{
    const nsecs_t startTime = systemTime();
    const size_t pcount = animation.parts.size();
    nsecs_t frameDuration = s2ns(1) / animation.fps;
    const int animationX = (mWidth - animation.width) / 2;
    const int animationY = (mHeight - animation.height) / 2;

    // PICO: ro.picovr.bootanima.splitscreen=1 (default) draws every frame once per lens at the
    // panel-specific lens centres.
    char splitScreen[PROPERTY_VALUE_MAX];
    property_get("ro.picovr.bootanima.splitscreen", splitScreen, "1");
    property_get("ro.pvr.hmd.type", mHmdType, "AUO");
    property_get("persist.picovr.funnylens.enable", mFunnyLens, "0");
    property_get("ro.product.name", mProductName, "0");
    property_get("ro.product.model", mProductModel, "0");

    // Draws the frame centred on (leftX, leftY) and on (rightX, rightY).
    auto drawSplitScreen = [&animation](int leftX, int leftY, int rightX, int rightY) {
        glDrawTexiOES(leftX - animation.width / 2, leftY - animation.height / 2,
                      0, animation.width, animation.height);
        glDrawTexiOES(rightX - animation.width / 2, rightY - animation.height / 2,
                      0, animation.width, animation.height);
    };

    SLOGD("width=%d,height=%d ...%sAnimationShownTiming start time: %" PRId64 "ms", mWidth,
            mHeight, mShuttingDown ? "Shutdown" : "Boot", elapsedRealtime());
    for (size_t i=0 ; i<pcount ; i++) {
        const Animation::Part& part(animation.parts[i]);
        const size_t fcount = part.frames.size();
        glBindTexture(GL_TEXTURE_2D, 0);

        // Handle animation package
        if (part.animation != nullptr) {
            playAnimation(*part.animation);
            if (exitPending())
                break;
            continue; //to next part
        }

        for (int r=0 ; !part.count || r<part.count ; r++) {
            // Exit any non playuntil complete parts immediately
            if(exitPending() && !part.playUntilComplete)
                break;

            mCallbacks->playPart(i, part, r);

            glClearColor(
                    part.backgroundColor[0],
                    part.backgroundColor[1],
                    part.backgroundColor[2],
                    1.0f);

            for (size_t j=0 ; j<fcount && (!exitPending() || part.playUntilComplete) ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                nsecs_t lastFrame = systemTime();

                if (r > 0) {
                    glBindTexture(GL_TEXTURE_2D, frame.tid);
                } else {
                    if (part.count != 1) {
                        glGenTextures(1, &frame.tid);
                        glBindTexture(GL_TEXTURE_2D, frame.tid);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    }
                    int w, h;
                    initTexture(frame.map, &w, &h);
                }

                const int xc = animationX + frame.trimX;
                const int yc = animationY + frame.trimY;
                Region clearReg(Rect(mWidth, mHeight));
                if (strcmp(splitScreen, "1") != 0) {
                    clearReg.subtractSelf(Rect(xc, yc, xc+frame.trimWidth, yc+frame.trimHeight));
                }
                if (!clearReg.isEmpty()) {
                    Region::const_iterator head(clearReg.begin());
                    Region::const_iterator tail(clearReg.end());
                    glEnable(GL_SCISSOR_TEST);
                    while (head != tail) {
                        const Rect& r2(*head++);
                        glScissor(r2.left, mHeight - r2.bottom, r2.width(), r2.height());
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                    glDisable(GL_SCISSOR_TEST);
                }
                if (strcmp(splitScreen, "1") != 0) {
                    // PICO draws the whole animation rectangle, not the trimmed frame.
                    glDrawTexiOES(xc, mHeight - (yc + animation.height),
                                  0, animation.width, animation.height);
                } else if (!strcmp(mHmdType, "Samsung")) {
                    drawSplitScreen(723, 714, 723, 1860);
                } else if (!strcmp(mHmdType, "BOE")) {
                    drawSplitScreen(743, 800, 2137, 800);
                } else if (!strcmp(mHmdType, "JDI552KT4K")) {
                    if (mShuttingDown) {
                        drawSplitScreen(644, 720, 1927, 720);
                    } else {
                        drawSplitScreen(720, 644, 720, 1927);
                    }
                } else if (!strcmp(mHmdType, "JDI554K")) {
                    if (mShuttingDown) {
                        drawSplitScreen(965, 1080, 2874, 1080);
                    } else {
                        drawSplitScreen(1080, 965, 1080, 2874);
                    }
                } else if (!strcmp(mHmdType, "JDI35x2")) {
                    drawSplitScreen(743, 800, 2137, 800);
                } else if (!strcmp(mHmdType, "JDI2KTO4K")) {
                    drawSplitScreen(720, 668, 720, 1896);
                } else if (!strcmp(mHmdType, "JDI4K")) {
                    drawSplitScreen(1080, 965, 1080, 2874);
                } else if (!strcmp(mHmdType, "JDI493")) {
                    // The shutdown surface is rotated (see readyToRun()); checkIPD() moved
                    // JDI493_*[1] for the shutdown animation.
                    if (mShuttingDown) {
                        drawSplitScreen(JDI493_LEFT[1], JDI493_LEFT[0],
                                        JDI493_RIGHT[1], JDI493_RIGHT[0]);
                    } else {
                        drawSplitScreen(JDI493_LEFT[0], JDI493_LEFT[1],
                                        JDI493_RIGHT[0], JDI493_RIGHT[1]);
                    }
                } else if (!strcmp(mHmdType, "INNOLUX5K") || !strcmp(mHmdType, "SHARP5K")) {
                    drawSplitScreen(1080, 1080, 3240, 1080);
                } else if (!strcmp(mHmdType, "JDI1080P")) {
                    drawSplitScreen(488, 965, 1568, 956);
                } else if (!strcmp(mHmdType, "Sharp4k")) {
                    drawSplitScreen(717, 735, 1858, 735);
                } else if (!strcmp(mHmdType, "Sharp1Kx2")) {
                    drawSplitScreen(766, 720, 2113, 720);
                } else if (!strcmp(mHmdType, "JDI1Kx2")) {
                    if (strcmp(mFunnyLens, "1") != 0) {
                        drawSplitScreen(818, 850, 2061, 850);
                    } else {
                        drawSplitScreen(720, 850, 2160, 850);
                    }
                } else if (!strcmp(mHmdType, "TIANMA")) {
                    drawSplitScreen(723, 714, 723, 1860);
                } else if (!strcmp(mHmdType, "Tianma2K")) {
                    drawSplitScreen(720, 623, 720, 1936);
                } else if (!strncmp(mProductModel, "Pico Neo3", 9)) {
                    drawSplitScreen(592, 606, 1584, 606);
                } else if (!strncmp(mProductName, "Phoenix", 7)) {
                    drawSplitScreen(1080, 1080, 3240, 1080);
                } else {
                    drawSplitScreen(592, 606, 1584, 606);
                }
                if (mClockEnabled && mTimeIsAccurate && validClock(part)) {
                    drawClock(animation.clockFont, part.clockPosX, part.clockPosY);
                }
                handleViewport(frameDuration);

                eglSwapBuffers(mDisplay, mSurface);

                nsecs_t now = systemTime();
                nsecs_t delay = frameDuration - (now - lastFrame);
                //SLOGD("%lld, %lld", ns2ms(now - lastFrame), ns2ms(delay));
                lastFrame = now;

                if (delay > 0) {
                    struct timespec spec;
                    spec.tv_sec  = (now + delay) / 1000000000;
                    spec.tv_nsec = (now + delay) % 1000000000;
                    int err;
                    do {
                        err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, nullptr);
                    } while (err<0 && errno == EINTR);
                }
            }

            // PICO: exit is checked once per part play instead of once per frame; with the
            // external function (MDM) enabled, not before the animation has run for 3 s.
            nsecs_t now = systemTime();
            if (strcmp(mRoExternalFunc, "0") == 1 || strcmp(mPersistExternalFunc, "0") == 1) {
                if (ns2us(now - startTime) / 1000.0 > 3000) {
                    checkExit();
                }
            } else {
                checkExit();
            }

            usleep(part.pause * ns2us(frameDuration));

            // For infinite parts, we've now played them at least once, so perhaps exit
            if(exitPending() && !part.count && mCurrentInset >= mTargetInset)
                break;
        }

    }

    // Free textures created for looping parts now that the animation is done; PICO clears the
    // screen to black (three buffers) before each texture is deleted.
    for (const Animation::Part& part : animation.parts) {
        if (part.count != 1) {
            const size_t fcount = part.frames.size();
            for (size_t j = 0; j < fcount; j++) {
                const Animation::Frame& frame(part.frames[j]);
                for (int k = 0; k < 3; k++) {
                    glBindTexture(GL_TEXTURE_2D, frame.tid);
                    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT);
                    eglSwapBuffers(mDisplay, mSurface);
                }
                glDeleteTextures(1, &frame.tid);
            }
        }
    }

    return true;
}

void BootAnimation::handleViewport(nsecs_t timestep) {
    if (mShuttingDown || !mFlingerSurfaceControl || mTargetInset == 0) {
        return;
    }
    if (mTargetInset < 0) {
        // Poll the amount for the top display inset. This will return -1 until persistent properties
        // have been loaded.
        mTargetInset = android::base::GetIntProperty("persist.sys.displayinset.top",
                -1 /* default */, -1 /* min */, mHeight / 2 /* max */);
    }
    if (mTargetInset <= 0) {
        return;
    }

    if (mCurrentInset < mTargetInset) {
        // After the device boots, the inset will effectively be cropped away. We animate this here.
        float fraction = static_cast<float>(mCurrentInset) / mTargetInset;
        int interpolatedInset = (cosf((fraction + 1) * M_PI) / 2.0f + 0.5f) * mTargetInset;

        SurfaceComposerClient::Transaction()
                .setCrop(mFlingerSurfaceControl, Rect(0, interpolatedInset, mWidth, mHeight))
                .apply();
    } else {
        // At the end of the animation, we switch to the viewport that DisplayManager will apply
        // later. This changes the coordinate system, and means we must move the surface up by
        // the inset amount.
        Rect layerStackRect(0, 0, mWidth, mHeight - mTargetInset);
        Rect displayRect(0, mTargetInset, mWidth, mHeight);

        SurfaceComposerClient::Transaction t;
        t.setPosition(mFlingerSurfaceControl, 0, -mTargetInset)
                .setCrop(mFlingerSurfaceControl, Rect(0, mTargetInset, mWidth, mHeight));
        t.setDisplayProjection(mDisplayToken, 0 /* orientation */, layerStackRect, displayRect);
        t.apply();

        mTargetInset = mCurrentInset = 0;
    }

    int delta = timestep * mTargetInset / ms2ns(200);
    mCurrentInset += delta;
}

void BootAnimation::releaseAnimation(Animation* animation) const
{
    for (Vector<Animation::Part>::iterator it = animation->parts.begin(),
         e = animation->parts.end(); it != e; ++it) {
        if (it->animation)
            releaseAnimation(it->animation);
    }
    if (animation->zip)
        delete animation->zip;
    delete animation;
}

BootAnimation::Animation* BootAnimation::loadAnimation(const String8& fn)
{
    if (mLoadedFiles.indexOf(fn) >= 0) {
        SLOGE("File \"%s\" is already loaded. Cyclic ref is not allowed",
            fn.string());
        return nullptr;
    }
    ZipFileRO *zip = ZipFileRO::open(fn);
    if (zip == nullptr) {
        SLOGE("Failed to open animation zip \"%s\": %s",
            fn.string(), strerror(errno));
        return nullptr;
    }

    Animation *animation =  new Animation;
    animation->fileName = fn;
    animation->zip = zip;
    animation->clockFont.map = nullptr;
    mLoadedFiles.add(animation->fileName);

    parseAnimationDesc(*animation);
    if (!preloadZip(*animation)) {
        return nullptr;
    }


    mLoadedFiles.remove(fn);
    return animation;
}

bool BootAnimation::updateIsTimeAccurate() {
    static constexpr long long MAX_TIME_IN_PAST =   60000LL * 60LL * 24LL * 30LL;  // 30 days
    static constexpr long long MAX_TIME_IN_FUTURE = 60000LL * 90LL;  // 90 minutes

    if (mTimeIsAccurate) {
        return true;
    }
    if (mShuttingDown) return true;
    struct stat statResult;

    if(stat(TIME_FORMAT_12_HOUR_FLAG_FILE_PATH, &statResult) == 0) {
        mTimeFormat12Hour = true;
    }

    if(stat(ACCURATE_TIME_FLAG_FILE_PATH, &statResult) == 0) {
        mTimeIsAccurate = true;
        return true;
    }

    FILE* file = fopen(LAST_TIME_CHANGED_FILE_PATH, "r");
    if (file != nullptr) {
      long long lastChangedTime = 0;
      fscanf(file, "%lld", &lastChangedTime);
      fclose(file);
      if (lastChangedTime > 0) {
        struct timespec now;
        clock_gettime(CLOCK_REALTIME, &now);
        // Match the Java timestamp format
        long long rtcNow = (now.tv_sec * 1000LL) + (now.tv_nsec / 1000000LL);
        if (ACCURATE_TIME_EPOCH < rtcNow
            && lastChangedTime > (rtcNow - MAX_TIME_IN_PAST)
            && lastChangedTime < (rtcNow + MAX_TIME_IN_FUTURE)) {
            mTimeIsAccurate = true;
        }
      }
    }

    return mTimeIsAccurate;
}

BootAnimation::TimeCheckThread::TimeCheckThread(BootAnimation* bootAnimation) : Thread(false),
    mInotifyFd(-1), mSystemWd(-1), mTimeWd(-1), mBootAnimation(bootAnimation) {}

BootAnimation::TimeCheckThread::~TimeCheckThread() {
    // mInotifyFd may be -1 but that's ok since we're not at risk of attempting to close a valid FD.
    close(mInotifyFd);
}

bool BootAnimation::TimeCheckThread::threadLoop() {
    bool shouldLoop = doThreadLoop() && !mBootAnimation->mTimeIsAccurate
        && mBootAnimation->mClockEnabled;
    if (!shouldLoop) {
        close(mInotifyFd);
        mInotifyFd = -1;
    }
    return shouldLoop;
}

bool BootAnimation::TimeCheckThread::doThreadLoop() {
    static constexpr int BUFF_LEN (10 * (sizeof(struct inotify_event) + NAME_MAX + 1));

    // Poll instead of doing a blocking read so the Thread can exit if requested.
    struct pollfd pfd = { mInotifyFd, POLLIN, 0 };
    ssize_t pollResult = poll(&pfd, 1, 1000);

    if (pollResult == 0) {
        return true;
    } else if (pollResult < 0) {
        SLOGE("Could not poll inotify events");
        return false;
    }

    char buff[BUFF_LEN] __attribute__ ((aligned(__alignof__(struct inotify_event))));;
    ssize_t length = read(mInotifyFd, buff, BUFF_LEN);
    if (length == 0) {
        return true;
    } else if (length < 0) {
        SLOGE("Could not read inotify events");
        return false;
    }

    const struct inotify_event *event;
    for (char* ptr = buff; ptr < buff + length; ptr += sizeof(struct inotify_event) + event->len) {
        event = (const struct inotify_event *) ptr;
        if (event->wd == mSystemWd && strcmp(SYSTEM_TIME_DIR_NAME, event->name) == 0) {
            addTimeDirWatch();
        } else if (event->wd == mTimeWd && (strcmp(LAST_TIME_CHANGED_FILE_NAME, event->name) == 0
                || strcmp(ACCURATE_TIME_FLAG_FILE_NAME, event->name) == 0)) {
            return !mBootAnimation->updateIsTimeAccurate();
        }
    }

    return true;
}

void BootAnimation::TimeCheckThread::addTimeDirWatch() {
        mTimeWd = inotify_add_watch(mInotifyFd, SYSTEM_TIME_DIR_PATH,
                IN_CLOSE_WRITE | IN_MOVED_TO | IN_ATTRIB);
        if (mTimeWd > 0) {
            // No need to watch for the time directory to be created if it already exists
            inotify_rm_watch(mInotifyFd, mSystemWd);
            mSystemWd = -1;
        }
}

status_t BootAnimation::TimeCheckThread::readyToRun() {
    mInotifyFd = inotify_init();
    if (mInotifyFd < 0) {
        SLOGE("Could not initialize inotify fd");
        return NO_INIT;
    }

    mSystemWd = inotify_add_watch(mInotifyFd, SYSTEM_DATA_DIR_PATH, IN_CREATE | IN_ATTRIB);
    if (mSystemWd < 0) {
        close(mInotifyFd);
        mInotifyFd = -1;
        SLOGE("Could not add watch for %s", SYSTEM_DATA_DIR_PATH);
        return NO_INIT;
    }

    addTimeDirWatch();

    if (mBootAnimation->updateIsTimeAccurate()) {
        close(mInotifyFd);
        mInotifyFd = -1;
        return ALREADY_EXISTS;
    }

    return NO_ERROR;
}

// ---------------------------------------------------------------------------

}
; // namespace android
