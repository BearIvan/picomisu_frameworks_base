// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;

/**
 * System error-style alert dialog whose buttons are enabled only after it has started (factory
 * PICO OS 5.13.7 services.jar com.android.server.BasePermissionDialog, from the later CAF
 * snapshot; unreferenced on the factory except by {@link
 * com.android.server.storage.StorageFullDialog}).
 */
public class BasePermissionDialog extends AlertDialog {
    private final Handler mInfoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mState = false;
                setEnabled(true);
            }
        }
    };
    private boolean mState = true;

    public BasePermissionDialog(Context dialogCon) {
        super(dialogCon, 0);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams permInfo = getWindow().getAttributes();
        permInfo.setTitle("Permission Dialog");
        permInfo.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        getWindow().setAttributes(permInfo);
        setIconAttribute(com.android.internal.R.attr.alertDialogIcon);
    }

    @Override
    public void onStart() {
        super.onStart();
        setEnabled(false);
        Button b = (Button) findViewById(com.android.internal.R.id.button1);
        if (b != null) {
            b.setFocusable(true);
            b.setFocusableInTouchMode(true);
            b.requestFocus();
        }
        mInfoHandler.sendMessage(mInfoHandler.obtainMessage(0));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mState) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setEnabled(boolean setState) {
        Button btn = getButton(BUTTON_POSITIVE);
        if (btn != null) {
            btn.setEnabled(setState);
        }
        btn = getButton(BUTTON_NEGATIVE);
        if (btn != null) {
            btn.setEnabled(setState);
        }
        btn = getButton(BUTTON_NEUTRAL);
        if (btn != null) {
            btn.setEnabled(setState);
        }
    }
}
