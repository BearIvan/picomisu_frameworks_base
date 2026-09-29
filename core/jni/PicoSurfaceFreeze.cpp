// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
#include "PicoSurfaceFreeze.h"
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <log/log.h>
namespace android::picomisu {
void surfaceFreezeSelfListening(JNIEnv*, jclass, jlong nativeObject) {
    sp<Surface> surface = reinterpret_cast<Surface*>(nativeObject);
    if (!surface) {
        ALOGE("Surface freeze listener: native Surface is null");
        return;
    }
    const sp<IGraphicBufferProducer> producer = surface->getIGraphicBufferProducer();
    if (!producer) {
        ALOGE("Surface freeze listener: producer is null");
        return;
    }
    producer->listenFreezeSelf();
}
} // namespace android::picomisu
