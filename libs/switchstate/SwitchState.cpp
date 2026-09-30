// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
#include <switchstate/SwitchState.h>
#include <android/log.h>
namespace android {
namespace {
int sSwitchState;
}
void SwitchState::enable(int offset) {
    if (offset >= 32) {
        __android_log_print(ANDROID_LOG_WARN, nullptr, "too big offset: %d", offset);
        return;
    }
    sSwitchState |= 1 << offset;
}
void SwitchState::disable(int offset) {
    if (offset >= 32) {
        __android_log_print(ANDROID_LOG_WARN, nullptr, "too big offset: %d", offset);
        return;
    }
    sSwitchState &= ~(1 << offset);
}
bool SwitchState::get(int offset) {
    if (offset >= 32) {
        __android_log_print(ANDROID_LOG_WARN, nullptr, "too big offset: %d", offset);
        return false;
    }
    // The factory tests the bit and returns a signed "greater than zero".
    return (sSwitchState & (1 << offset)) > 0;
}
} // namespace android
