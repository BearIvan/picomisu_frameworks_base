// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Smartisan extension state of the {@link RootActivityContainer}. Reconstructed from the
 * PICO OS 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class RootActivityContainerSmtBase extends ConfigurationContainerSmtBase {
    private RootActivityContainer mRootActivityContainer;

    public RootActivityContainerSmtBase(RootActivityContainer rootActivityContainer) {
        super(rootActivityContainer);
        mRootActivityContainer = rootActivityContainer;
    }

    /** Top full screen stack of the topmost display that has one, or null. */
    public ActivityStack getTopDisplayFullScreenStack() {
        for (int i = mRootActivityContainer.mActivityDisplays.size() - 1; i >= 0; i--) {
            ActivityStack focusedStack = mRootActivityContainer.mActivityDisplays.get(i)
                    .getActivityDisplaySmtEx().mTopFullScreenStack;
            if (focusedStack != null) {
                return focusedStack;
            }
        }
        return null;
    }
}
