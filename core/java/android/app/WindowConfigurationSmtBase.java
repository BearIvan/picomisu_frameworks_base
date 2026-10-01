// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

/**
 * Smartisan extension of a {@link WindowConfiguration} (its {@code mSmtEx})
 * (factory PICO OS 5.13.7 {@code android.app.WindowConfigurationSmtBase}).
 *
 * @hide
 */
public class WindowConfigurationSmtBase {
    public static final int FLAG_UNDEFINED = 0;

    protected int mFlag = FLAG_UNDEFINED;
    protected WindowConfiguration mWindowConfiguration;

    public WindowConfigurationSmtBase(WindowConfiguration windowConfiguration) {
        mWindowConfiguration = windowConfiguration;
    }

    public void setFlags(int flag) {
        mFlag = flag;
    }
}
