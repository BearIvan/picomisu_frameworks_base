// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.storage;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

import com.android.server.BasePermissionDialog;

/**
 * "Storage full" system dialog (factory PICO OS 5.13.7 services.jar
 * com.android.server.storage.StorageFullDialog, from the later CAF snapshot; never constructed
 * on the factory).
 */
class StorageFullDialog extends BasePermissionDialog {
    private static final String TAG = "StorageFullDialog";
    static final int ACTION_DISMISS_SELF = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_DISMISS_SELF:
                default:
                    dismiss();
                    break;
            }
        }
    };
    private Context mContext;
    private DeviceStorageMonitorService mService;

    public StorageFullDialog(Context context, DeviceStorageMonitorService service) {
        super(context);
        mContext = context;
        mService = service;
        final Resources res = context.getResources();
        setCancelable(false);
        setButton(BUTTON_POSITIVE, res.getString(com.android.internal.R.string.ok),
                mHandler.obtainMessage(ACTION_DISMISS_SELF));
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("StorageFull");
        attrs.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SYSTEM_ERROR
                | WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        getWindow().setAttributes(attrs);
    }
}
