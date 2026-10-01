// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Smartisan extension of {@link Intent}: Smartisan flags and extras and the launch timing.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class IntentSmtBase {
    public static final String ACTION_FROM_JOB_SERVICE = "android.intent.action.JOB_SERVICE";
    public static final String ACTION_SM_FINGERPRINT_ENROLL_STATUS =
            "com.smartisanos.fingerprint.enroll_status";
    public static final String ACTION_SM_SQUEEZE_SHORTCUTS =
            "com.smartisanos.action.SM_SQUEEZE_SHORTCUT";
    public static final String ACTION_STATUS_BAR_CLICKED =
            "android.intent.action.STATUS_BAR_CLICKED";
    public static final String EXTRA_FROM_SYSTEM_UI = "from_system_ui";
    public static final String EXTRA_SMARTISAN_ANIM_RESOURCE_ID =
            "smartisanos.intent.extra.ANIM_RESOURCE_ID";
    public static final String EXTRA_SMARTISAN_KEYGUARD_LAUNCH_CAMERA =
            "smartisanos.intent.extra.KEYGUARD_LAUNCH_CAMERA";
    public static final String EXTRA_SM_FINGERPRINT_ENROLL_STATUS = "is_enrollment_on_going";
    public static final String EXTRA_SM_PACKAGE_ACTIVE_INFO =
            "com.smartisanos.intent.extra.PACKAGE_ACTIVE";
    public static final String EXTRA_SM_SQUEEZE_SHORTCUTS =
            "com.smartisanos.action.SM_SQUEEZE_SHORTCUT";

    public static final int FLAG_ACTIVITY_SECURITY_MODE = 0x00000400;
    public static final int FLAG_NEW_TASK_FOR_ACTIVITY_RESULT = 0x00000800;
    public static final int FLAG_RECEIVER_SM_USER_AWARE = 0x00100000;
    public static final int FLAG_SHOW_SMARTISAN_TRANSITION = 0x80000000;

    public static final int FLAG_SM_NO_AMPLIFICATION = 0x00000001;
    public static final int FLAG_SM_IN_CALL_SCREEN = 0x00000002;
    public static final int FLAG_SM_REMOVE_CONTAINER_IMMEDIATE = 0x00000004;
    public static final int FLAG_SM_FORCE_REOPEN_WHEN_RESUMED = 0x00000008;
    public static final int FLAG_SM_REMOVE_ACTIVE_NETWORKINFO_CACHE = 0x00000008;
    public static final int FLAG_SM_TASK_NOT_TRIMMABLE = 0x00000010;
    public static final int FLAG_SM_TRACK_BROADCAST = 0x00000020;

    private long mLaunchStartTime = -1;
    private long mAMSStartTime = -1;
    protected Intent mIntent;
    protected int mSmFlags;
    protected Bundle mSmtExExtras;

    public IntentSmtBase(Intent intent) {
        mIntent = intent;
    }

    public void readFromParcel(Parcel in) {
        mSmFlags = in.readInt();
        mSmtExExtras = in.readBundle();
        mLaunchStartTime = in.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSmFlags);
        dest.writeBundle(mSmtExExtras);
        dest.writeLong(mLaunchStartTime);
    }

    public void copy(IntentSmtBase o, int copyMode) {
        mSmFlags = o.mSmFlags;
        mSmtExExtras = o.mSmtExExtras;
        mLaunchStartTime = o.mLaunchStartTime;
    }

    public Intent putSmtExtra(String name, int value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putInt(name, value);
        return mIntent;
    }

    public Intent putSmtExtra(String name, long value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putLong(name, value);
        return mIntent;
    }

    public Intent putSmtExtra(String name, float value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putFloat(name, value);
        return mIntent;
    }

    public Intent putSmtExtra(String name, boolean value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putBoolean(name, value);
        return mIntent;
    }

    public Intent putSmtExtra(String name, IBinder value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putIBinder(name, value);
        return mIntent;
    }

    public Intent putSmtExtra(String name, Parcelable value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putParcelable(name, value);
        return mIntent;
    }

    public int getSmtIntExtra(String name, int defaultValue) {
        return mSmtExExtras == null ? defaultValue : mSmtExExtras.getInt(name, defaultValue);
    }

    public long getSmtLongExtra(String name, long defaultValue) {
        return mSmtExExtras == null ? defaultValue : mSmtExExtras.getLong(name, defaultValue);
    }

    public float getSmtFloatExtra(String name, float defaultValue) {
        return mSmtExExtras == null ? defaultValue : mSmtExExtras.getFloat(name, defaultValue);
    }

    public boolean getSmtBooleanExtra(String name, boolean defaultValue) {
        return mSmtExExtras == null ? defaultValue
                : mSmtExExtras.getBoolean(name, defaultValue);
    }

    public IBinder getSmtIBinderExtra(String name) {
        return mSmtExExtras == null ? null : mSmtExExtras.getIBinder(name);
    }

    public <T extends Parcelable> T getSmtParcelableExtra(String name) {
        return mSmtExExtras == null ? null : mSmtExExtras.<T>getParcelable(name);
    }

    public void removeSmtExtra(String name) {
        if (mSmtExExtras != null) {
            mSmtExExtras.remove(name);
            if (mSmtExExtras.size() == 0) {
                mSmtExExtras = null;
            }
        }
    }

    public Intent replaceSmtExtras(Bundle extras) {
        mSmtExExtras = extras != null ? new Bundle(extras) : null;
        return mIntent;
    }

    public Bundle getExtras() {
        return mSmtExExtras;
    }

    public int getSmFlags() {
        return mSmFlags;
    }

    public void removeSmFlags(int flags) {
        mSmFlags &= ~flags;
    }

    public void markLaunchStartTime(long time) {
        mLaunchStartTime = time;
    }

    public long getLaunchStartTime() {
        return mLaunchStartTime;
    }

    public void markAMSStartTime(long time) {
        mAMSStartTime = time;
    }

    public long getAMSStartTime() {
        return mAMSStartTime;
    }

    public Intent putSmtExtra(String name, String value) {
        if (mSmtExExtras == null) {
            mSmtExExtras = new Bundle();
        }
        mSmtExExtras.putString(name, value);
        return mIntent;
    }

    public String getSmtStringExtra(String name) {
        return mSmtExExtras == null ? null : mSmtExExtras.getString(name);
    }

    public Intent addSmFlags(int flags) {
        mSmFlags |= flags;
        return mIntent;
    }
}
