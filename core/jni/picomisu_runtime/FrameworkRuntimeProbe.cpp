// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Loaded before AndroidRuntime. All service-manager lookups stay inside this process.
#include <jni.h>
#include <utils/StrongPointer.h>
#include <android_runtime/android_view_Surface.h>
#include <binder/FreezeManager.h>
#include <gui/BufferItem.h>
#include <gui/BufferQueueConsumer.h>
#include <gui/BufferQueueCore.h>
#include <gui/BufferQueueProducer.h>
#include <gui/IConsumerListener.h>
#include <gui/Surface.h>
#include <system/window.h>
#include <dlfcn.h>
#include <cstdio>
#include <memory>
#include <mutex>
#include <unistd.h>
#include <vector>
#include "../../../../native/libs/gui/tests/PicoFreezeRegistryFake.h"

using namespace android;
namespace {
sp<picomisu::ServiceManager> fakeManager;
sp<picomisu::FreezeService> service;
std::once_flag serviceOnce;
void initService() {
    std::call_once(serviceOnce, [] {
        fakeManager = new picomisu::ServiceManager;
        service = new picomisu::FreezeService;
        fakeManager->service = IInterface::asBinder(service);
    });
}
class Idle : public BnConsumerListener {
public:
    void onFrameAvailable(const BufferItem&) override {}
    void onBuffersReleased() override {}
    void onSidebandStreamChanged() override {}
};
sp<GraphicBuffer> buffer() {
    sp<GraphicBuffer> b = new GraphicBuffer;
    b->width = b->height = b->stride = b->layerCount = 1;
    b->format = HAL_PIXEL_FORMAT_RGBA_8888;
    return b;
}
struct Fixture {
    sp<BufferQueueCore> core = new BufferQueueCore;
    sp<BufferQueueProducer> producer = new BufferQueueProducer(core);
    sp<BufferQueueConsumer> consumer = new BufferQueueConsumer(core);
    IGraphicBufferProducer::QueueBufferOutput output;
    int acquired = -1;
    uint64_t frame = 0;
    status_t attach(int* slot) {
        auto b = buffer();
        return producer->attachCachedBuffer(slot, b, b->getId());
    }
    bool prepare() {
        if (consumer->connect(new Idle, false) != NO_ERROR ||
            producer->connect(nullptr, NATIVE_WINDOW_API_CPU, false, &output) != NO_ERROR ||
            producer->setMaxDequeuedBufferCount(1) != NO_ERROR ||
            producer->allowAllocation(false) != NO_ERROR) return false;
        if (attach(&acquired) != NO_ERROR) return false;
        sp<GraphicBuffer> requested;
        if (producer->requestBuffer(acquired, &requested) != NO_ERROR) return false;
        IGraphicBufferProducer::QueueBufferInput input(0, false, HAL_DATASPACE_UNKNOWN,
                Rect(1, 1), NATIVE_WINDOW_SCALING_MODE_FREEZE, 0, Fence::NO_FENCE);
        if (producer->queueBuffer(acquired, input, &output) != NO_ERROR) return false;
        BufferItem item;
        if (consumer->acquireBuffer(&item, 0) != NO_ERROR) return false;
        frame = item.mFrameNumber;
        int held = -1;
        return attach(&held) == NO_ERROR;
    }
    bool recover() {
        int replacement = -1;
        if (attach(&replacement) != INVALID_OPERATION || service->registrations.empty()) return false;
        service->latest()->onUnFreeze(getpid());
        return attach(&replacement) == NO_ERROR && replacement != acquired &&
                consumer->releaseBuffer(acquired, frame, EGL_NO_DISPLAY, EGL_NO_SYNC_KHR,
                        Fence::NO_FENCE) == NO_ERROR;
    }
};
std::vector<std::unique_ptr<Fixture>> fixtures;
}
namespace android {
sp<IServiceManager> defaultServiceManager() { initService(); return fakeManager; }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_picomisu_runtime_FrameworkRuntimeProbe_preflight(JNIEnv*, jclass) {
    initService();
    const char* libs[] = {"libart.so", "libandroid_runtime.so", "libgui.so"};
    const char* symbols[] = {"JNI_CreateJavaVM", "_ZN7android29register_android_view_SurfaceEP7_JNIEnv",
            "_ZN7android19BufferQueueProducer16listenFreezeSelfEv"};
    for (size_t i = 0; i < 3; ++i) {
        void* handle = dlopen(libs[i], RTLD_NOW | RTLD_NOLOAD);
        Dl_info info{};
        void* address = handle ? dlsym(handle, symbols[i]) : nullptr;
        bool ok = address && dladdr(address, &info) && info.dli_fname;
        if (ok) std::printf("runtime-library %s %s\n", libs[i], info.dli_fname);
        if (handle) dlclose(handle);
        if (!ok) return JNI_FALSE;
    }
    std::fflush(stdout);
    auto selected = FreezeManager::getInstance()->getService();
    return selected && IInterface::asBinder(selected) == fakeManager->service &&
            fakeManager->lookups == 1 && !fakeManager->wrongName;
}
extern "C" JNIEXPORT jobject JNICALL
Java_org_picomisu_runtime_FrameworkRuntimeProbe_newSurface(JNIEnv* env, jclass, jint index) {
    if (index < 0 || size_t(index) != fixtures.size()) return nullptr;
    auto f = std::make_unique<Fixture>();
    if (!f->prepare()) return nullptr;
    sp<Surface> surface = new Surface(f->producer, false);
    jobject result = android_view_Surface_createFromSurface(env, surface);
    if (result) fixtures.push_back(std::move(f));
    return result;
}
extern "C" JNIEXPORT jint JNICALL
Java_org_picomisu_runtime_FrameworkRuntimeProbe_registrations(JNIEnv*, jclass) {
    return service->registrations.size();
}
extern "C" JNIEXPORT jboolean JNICALL
Java_org_picomisu_runtime_FrameworkRuntimeProbe_recover(JNIEnv*, jclass, jint index) {
    return index >= 0 && size_t(index) < fixtures.size() && fixtures[index]->recover();
}
extern "C" JNIEXPORT void JNICALL
Java_org_picomisu_runtime_FrameworkRuntimeProbe_clearFixtures(JNIEnv*, jclass) { fixtures.clear(); }
