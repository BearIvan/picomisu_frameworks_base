// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
#pragma once
#include <jni.h>
namespace android::picomisu {
// The same implementation is compiled into libandroid_runtime and its probe.
void surfaceFreezeSelfListening(JNIEnv*, jclass, jlong nativeObject);
}
