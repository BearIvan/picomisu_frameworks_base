// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.widget;

import android.view.ViewSmtBase;

/**
 * Smartisan extension base of an {@link AbsListView}: fsync/fdatasync throttling during
 * scrolling (factory PICO OS 5.13.7 {@code android.widget.AbsListViewSmtBase}; the factory has no
 * subclass and registers no JNI for the native methods, so it is never used).
 *
 * @hide
 */
public abstract class AbsListViewSmtBase extends ViewSmtBase {
    protected AbsListView mAbsListView;

    public AbsListViewSmtBase(AbsListView absListView) {
        super(absListView);
        mAbsListView = absListView;
    }

    void disableFsync() {
        nativeUpdateFsyncAndFdatasync();
    }

    void resetFsync() {
        nativeResetFsyncAndFdatasync();
    }

    protected native void nativeUpdateFsyncAndFdatasync();

    protected native void nativeResetFsyncAndFdatasync();
}
