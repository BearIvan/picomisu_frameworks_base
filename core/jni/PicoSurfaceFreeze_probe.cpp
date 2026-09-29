// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Actual bridge source, or authenticated factory JNI body selected by address.
#include "PicoSurfaceFreeze.h"
#include <binder/FreezeManager.h>
#include <gui/BufferItem.h>
#include <gui/BufferQueueConsumer.h>
#include <gui/BufferQueueCore.h>
#include <gui/BufferQueueProducer.h>
#include <gui/IConsumerListener.h>
#include <gui/IProducerListener.h>
#include <gui/Surface.h>
#include <system/window.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
#include <new>
#include <utility>
#include <unistd.h>
#include "../../../native/libs/gui/tests/PicoFreezeRegistryFake.h"
using namespace android;
namespace { sp<picomisu::ServiceManager> fakeManager; }
namespace android { sp<IServiceManager> defaultServiceManager() { return fakeManager; } }
namespace {
using Entry = void (*)(JNIEnv*, jclass, jlong);
template<class T, size_t Reserve, class... Args>
sp<T> padded(Args&&... args) {
    static_assert(sizeof(T) <= Reserve, "Insufficient object storage");
    return ::new (::operator new(Reserve)) T(std::forward<Args>(args)...);
}
class Idle : public BnConsumerListener {
public:
    void onFrameAvailable(const BufferItem&) override {}
    void onBuffersReleased() override {}
    void onSidebandStreamChanged() override {}
};
struct Item {
    BufferItem* p = ::new (::operator new(1024)) BufferItem;
    ~Item() { p->~BufferItem(); ::operator delete(p); }
};
sp<GraphicBuffer> buffer() {
    auto b = padded<GraphicBuffer, 512>();
    b->width = b->height = b->stride = b->layerCount = 1;
    b->format = HAL_PIXEL_FORMAT_RGBA_8888;
    return b;
}
bool check(bool ok, const char* label) {
    if (ok) printf("surface-freeze %s=1\n", label);
    else fprintf(stderr, "surface-freeze fixture failed: %s\n", label);
    return ok;
}
}
int main(int argc, char** argv) {
    Entry call = picomisu::surfaceFreezeSelfListening;
    void* library = nullptr;
    if (argc == 2 && std::strcmp(argv[1], "--factory") == 0) {
        library = dlopen("libandroid_runtime.so", RTLD_NOW | RTLD_LOCAL);
        if (!library) { fprintf(stderr, "factory JNI load failed: %s\n", dlerror()); return 1; }
        void* registration = dlsym(library, "_ZN7android29register_android_view_SurfaceEP7_JNIEnv");
        Dl_info info{};
        if (!registration || !dladdr(registration, &info) || !info.dli_fbase) return 1;
#if defined(__aarch64__)
        constexpr uintptr_t offset = 0x11ec14;
#else
        constexpr uintptr_t offset = 0xbe449; // Thumb tag retained.
#endif
        call = reinterpret_cast<Entry>(reinterpret_cast<uintptr_t>(info.dli_fbase) + offset);
    } else if (argc != 1) return 1;
    fakeManager = new picomisu::ServiceManager;
    sp<picomisu::FreezeService> service = new picomisu::FreezeService;
    fakeManager->service = IInterface::asBinder(service);
    auto* manager = FreezeManager::getInstance();
    auto selected = manager->getService();
    if (!check(selected && IInterface::asBinder(selected) == fakeManager->service &&
               fakeManager->lookups == 1 && !fakeManager->wrongName, "fake-service-preflight")) return 1;
    call(nullptr, nullptr, 0);
    if (!check(service->registrations.empty(), "null-native-surface-is-ignored")) return 1;
    {
        sp<IGraphicBufferProducer> empty;
        auto surface = padded<Surface, 4096>(empty, false);
        call(nullptr, nullptr, reinterpret_cast<jlong>(surface.get()));
        if (!check(service->registrations.empty(), "null-producer-is-ignored")) return 1;
    }
    {
        auto core = padded<BufferQueueCore, 8192>();
        auto producer = padded<BufferQueueProducer, 1024>(core);
        auto consumer = padded<BufferQueueConsumer, 1024>(core);
        IGraphicBufferProducer::QueueBufferOutput output;
        if (consumer->BufferQueueConsumer::connect(new Idle, false) != NO_ERROR ||
            producer->BufferQueueProducer::connect(nullptr, NATIVE_WINDOW_API_CPU, false, &output) != NO_ERROR ||
            producer->BufferQueueProducer::setMaxDequeuedBufferCount(1) != NO_ERROR ||
            producer->BufferQueueProducer::allowAllocation(false) != NO_ERROR) return 1;
        auto attach = [&] (int* slot) {
            auto b = buffer();
            return producer->BufferQueueProducer::attachCachedBuffer(slot, b, b->getId());
        };
        int first = -1;
        if (attach(&first) != NO_ERROR) return 1;
        sp<GraphicBuffer> requested;
        if (producer->BufferQueueProducer::requestBuffer(first, &requested) != NO_ERROR) return 1;
        IGraphicBufferProducer::QueueBufferInput input(0, false, HAL_DATASPACE_UNKNOWN,
                Rect(1, 1), NATIVE_WINDOW_SCALING_MODE_FREEZE, 0, Fence::NO_FENCE);
        if (producer->BufferQueueProducer::queueBuffer(first, input, &output) != NO_ERROR) return 1;
        Item item;
        if (consumer->BufferQueueConsumer::acquireBuffer(item.p, 0) != NO_ERROR) return 1;
        int held = -1;
        if (attach(&held) != NO_ERROR) return 1;
        sp<IGraphicBufferProducer> interface = producer;
        auto surface = padded<Surface, 4096>(interface, false);
        call(nullptr, nullptr, reinterpret_cast<jlong>(surface.get()));
        if (!check(service->registrations.size() == 1 &&
                   service->registrations.back().pid == getpid() && !service->registrations.back().flag,
                   "surface-jni-registers-local-producer")) return 1;
        call(nullptr, nullptr, reinterpret_cast<jlong>(surface.get()));
        if (!check(service->registrations.size() == 1, "repeated-jni-registration-is-shared")) return 1;
        service->latest()->onUnFreeze(getpid());
        int replacement = -1;
        if (!check(attach(&replacement) == NO_ERROR && replacement != first &&
                   consumer->BufferQueueConsumer::releaseBuffer(first, 1, EGL_NO_DISPLAY,
                           EGL_NO_SYNC_KHR, Fence::NO_FENCE) == NO_ERROR,
                   "jni-to-producer-slot-recovery")) return 1;
    }
    if (library) dlclose(library);
    puts("surface-freeze-probe passed=6");
    return 0;
}
