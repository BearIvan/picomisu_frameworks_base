/*
 * Copyright (C) 2026 The Android Open Source Project
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

#pragma once

// PICO: the PICO system monitor client of the factory PICO OS 5.13.7 libhwui.
//
// The factory libhwui links libsysperftracker.so and, after every frame (CanvasContext::draw),
// calls mtp::SysMtpClient::getInstance()->addRenderFrame(frameInfo, windowName). The library
// is a factory prebuilt carried on the image (/system/lib64, /system/lib) and not part of the
// source tree, so it is bound at run time by the factory mangled names (as SurfaceFlinger's
// PicoSysMtpClient.h does); the call is a no-op when the library or a symbol is missing.
// The class does not use the factory names so libhwui cannot interpose the real ones.

#include <dlfcn.h>

#include <string>

#include <log/log.h>

#include "FrameInfo.h"

namespace android {
namespace uirenderer {
namespace renderthread {
namespace pico {

class SysMtpClient {
public:
    // mtp::SysMtpClient::getInstance()->addRenderFrame(frameInfo, name)
    static void addRenderFrame(FrameInfo& frameInfo, const std::string& name) {
        const Api& api = get();
        if (api.addRenderFrame != nullptr) {
            api.addRenderFrame(api.getInstance(), frameInfo, name);
        }
    }

private:
    struct Api {
        void* (*getInstance)() = nullptr;
        void (*addRenderFrame)(void*, FrameInfo&, const std::string&) = nullptr;
    };

    static const Api& get() {
        static const Api api = [] {
            Api a;
            void* handle = dlopen("libsysperftracker.so", RTLD_NOW);
            if (handle == nullptr) {
                ALOGW("PICO SysMtpClient unavailable: %s", dlerror());
                return a;
            }
            a.getInstance = reinterpret_cast<void* (*)()>(
                    dlsym(handle, "_ZN3mtp12SysMtpClient11getInstanceEv"));
            if (a.getInstance == nullptr) {
                return a;
            }
            a.addRenderFrame = reinterpret_cast<void (*)(void*, FrameInfo&, const std::string&)>(
                    dlsym(handle,
                          "_ZN3mtp12SysMtpClient14addRenderFrameERN7android10uirenderer9FrameInfo"
                          "ERKNSt3__112basic_stringIcNS5_11char_traitsIcEENS5_9allocatorIcEEEE"));
            return a;
        }();
        return api;
    }
};

} /* namespace pico */
} /* namespace renderthread */
} /* namespace uirenderer */
} /* namespace android */
