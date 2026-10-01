// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content;

import android.os.RemoteException;

import java.util.List;

/**
 * Smartisan copy history client base of {@link ClipboardManager}
 * (factory PICO OS 5.13.7 {@code android.content.ClipboardManagerSmtBase}; the factory has no
 * subclass, so it is never instantiated).
 *
 * @hide
 */
public abstract class ClipboardManagerSmtBase {
    private static final int GENERAL_SPENT = 1024;
    protected static final int MAX_CLIP_SIZE = 1024 * 1024 - GENERAL_SPENT;

    protected ClipboardManager mClipboardManager;
    protected IClipboardSmtEx mISmtEx;

    public ClipboardManagerSmtBase(ClipboardManager clipboardManager) {
        mClipboardManager = clipboardManager;
    }

    public void delete(CopyHistoryItem item) {
        try {
            getServiceSmtEx().delete(item);
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    public void insert(List<CopyHistoryItem> items) {
        try {
            getServiceSmtEx().insert(items);
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    public List<CopyHistoryItem> getCopyHistory() {
        try {
            return getServiceSmtEx().getCopyHistory();
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    protected IClipboardSmtEx getServiceSmtEx() {
        return mISmtEx;
    }
}
