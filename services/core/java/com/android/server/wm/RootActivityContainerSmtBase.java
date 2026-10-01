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

    /**
     * When the top activity of a display's top stack belongs to {@code pkg}, the package of the
     * bottom activity of the first stack (from the top) whose bottom activity is of another
     * package; otherwise null.
     */
    public String isAnyDisplayStackTopLocked(String pkg) {
        int numDisplays = mRootActivityContainer.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ActivityDisplay display = mRootActivityContainer.mActivityDisplays.get(displayNdx);
            ActivityStack stackTop = display.getTopStack();
            if (stackTop != null && stackTop.getTopActivity() != null
                    && pkg.equals(stackTop.getTopActivity().packageName)) {
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = display.getChildAt(stackNdx);
                    ActivityRecord bottom;
                    if (stack != null && stack.getChildAt(0) != null
                            && (bottom = stack.getChildAt(0).getChildAt(0)) != null
                            && bottom.packageName != null && !bottom.packageName.equals(pkg)) {
                        return bottom.packageName;
                    }
                }
            }
        }
        return null;
    }
}
