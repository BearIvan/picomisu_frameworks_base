// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
#pragma once
namespace android {
class SwitchState {
public:
    // offset: bit 0..31 of the per-process switch state.
    static void enable(int offset);
    static void disable(int offset);
    static bool get(int offset);
};
} // namespace android
