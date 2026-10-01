// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Smartisan "do not disturb" mode helper: reads the Smartisan secure settings (NoDisturbOn,
 * limited period, notify when screen on, TNT mode) and tells whether a package is currently
 * disturbed (factory PICO OS 5.13.7 {@code android.os.DisturbModeUtils}; nothing in the factory
 * jars calls it, so its settings observer and screen receiver are never registered).
 *
 * @hide
 */
public class DisturbModeUtils {
    public static final String TAG = "DisturbModeUtils";
    public static final boolean SMDBG = SystemProperties.getInt("ro.debuggable", 0) == 1;

    public static final String NO_DISTURB_ON = "NoDisturbOn";
    public static final String LIMITED_NO_DISTURB_ON = "LimitedNoDisturbOn";
    public static final String NO_DISTURB_START_TIME = "NoDisturbStartTime";
    public static final String NO_DISTURB_STOP_TIME = "NoDisturbStopTime";
    public static final String NOTIFY_WHEN_SCREEN_ON = "NoDisturbNotifyWhenScreenOn";
    public static final String TNT_NO_DISTURB_ON = "TNT_NO_DISTURB_ON";
    public static final int DEFAULT_START_TIME = 2200;
    public static final int DEFAULT_STOP_TIME = 800;
    public static final int ON = 1;
    public static final int OFF = 0;

    private static DisturbModeUtils mInstance = null;

    private static final ArrayList<String> mWhiteList = new ArrayList<>();
    private static final ArrayList<String> mLightUpScreenWhiteList = new ArrayList<>();
    private static final ArrayList<String> mNotificationCallRecordWhiteList = new ArrayList<>();

    static {
        mWhiteList.add("com.android.phone");
        mWhiteList.add("com.smartisanos.clock");
        mWhiteList.add("android");
        mWhiteList.add("com.smartisanos.music");
        mWhiteList.add("com.smartisanos.cloudsync");
        mWhiteList.add("com.android.server.telecom");
        mWhiteList.add("com.android.mms");
        mWhiteList.add("com.kongzue.wakeup");
        mWhiteList.add("com.smartisanos.ime");

        mLightUpScreenWhiteList.add("com.bullet.messenger");
        mLightUpScreenWhiteList.add("com.tencent.mm");

        mNotificationCallRecordWhiteList.add("com.tencent.mm");
    }

    private final Context mContext;
    private final HandlerThread mDisturbModeThread;
    private final DisturbModeSettingsObserver mDisturbModeSettingsObserver;

    volatile boolean mNoDisturbOn = false;
    volatile boolean mLimitedNoDisturbOn = false;
    volatile boolean mNotifyWhenScreenOn = false;
    volatile boolean mScreenOn = false;
    volatile int mNoDisturbStartTime = DEFAULT_START_TIME;
    volatile int mNoDisturbStopTime = DEFAULT_STOP_TIME;
    volatile boolean mTntNoDisturbOn = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SMDBG) {
                Log.d(TAG, "on receive action:" + action);
            }
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOn = true;
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOn = false;
            }
        }
    };

    private static boolean isNoDisturbOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), NO_DISTURB_ON, OFF) == ON;
    }

    private static boolean isLimitedNoDisturbOn(Context context) {
        return isNoDisturbOn(context) && Settings.Secure.getInt(context.getContentResolver(),
                LIMITED_NO_DISTURB_ON, OFF) == ON;
    }

    private static int getLimitedStartTime(Context context) {
        if (isLimitedNoDisturbOn(context)) {
            return Settings.Secure.getInt(context.getContentResolver(), NO_DISTURB_START_TIME,
                    DEFAULT_START_TIME);
        }
        return -1;
    }

    private static int getLimitedStopTime(Context context) {
        if (isLimitedNoDisturbOn(context)) {
            return Settings.Secure.getInt(context.getContentResolver(), NO_DISTURB_STOP_TIME,
                    DEFAULT_STOP_TIME);
        }
        return -1;
    }

    private static boolean isTntNoDisturbOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), TNT_NO_DISTURB_ON, OFF)
                == ON;
    }

    public static boolean isInDisturbMode(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        return isInDisturbMode(context, info != null ? info.packageName : null);
    }

    public static boolean isInDisturbMode(Context context, String pkg) {
        if (mWhiteList.contains(pkg)) {
            return false;
        }
        return isInDisturbModePeriod(context);
    }

    public static boolean isInDisturbModePeriod(Context context) {
        if (!isNoDisturbOn(context)) {
            return false;
        }
        if (!isLimitedNoDisturbOn(context)) {
            return true;
        }
        int starttime = getLimitedStartTime(context);
        int endtime = getLimitedStopTime(context);
        int nowtime = getNowTime();
        if (starttime < endtime) {
            if (starttime <= nowtime && nowtime < endtime) {
                return true;
            }
        } else if (starttime >= endtime) {
            if (starttime <= nowtime || nowtime < endtime) {
                return true;
            }
        }
        return false;
    }

    public static int getNowTime() {
        int hour = 0;
        int minute = 0;
        Calendar c = Calendar.getInstance();
        if (c != null) {
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }
        return hour * 100 + minute;
    }

    public static boolean isNotifyWhenScreenOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), NOTIFY_WHEN_SCREEN_ON, OFF)
                == ON;
    }

    public static DisturbModeUtils getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DisturbModeUtils(context);
        }
        return mInstance;
    }

    private DisturbModeUtils(Context context) {
        if (SMDBG) {
            Log.d(TAG, TAG);
        }
        mContext = context;
        mDisturbModeThread = new HandlerThread("disturbmode",
                Process.THREAD_PRIORITY_BACKGROUND);
        mDisturbModeThread.start();
        Handler hander = new Handler(mDisturbModeThread.getLooper());
        mDisturbModeSettingsObserver = new DisturbModeSettingsObserver(hander);
        mDisturbModeSettingsObserver.observe();
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mScreenOn = pm.isScreenOn();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mReceiver, filter, null, hander);
    }

    class DisturbModeSettingsObserver extends ContentObserver {
        DisturbModeSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(NO_DISTURB_ON),
                    false, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor(NOTIFY_WHEN_SCREEN_ON),
                    false, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor(LIMITED_NO_DISTURB_ON),
                    false, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor(NO_DISTURB_START_TIME),
                    false, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor(NO_DISTURB_STOP_TIME),
                    false, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor(TNT_NO_DISTURB_ON),
                    false, this);
            mNoDisturbOn = isNoDisturbOn(mContext);
            mNotifyWhenScreenOn = isNotifyWhenScreenOn(mContext);
            mLimitedNoDisturbOn = isLimitedNoDisturbOn(mContext);
            mNoDisturbStartTime = getLimitedStartTime(mContext);
            mNoDisturbStopTime = getLimitedStopTime(mContext);
            mTntNoDisturbOn = isTntNoDisturbOn(mContext);
            if (SMDBG) {
                Log.d(TAG, "DisturbModeSettingsObserver observe mNoDisturbOn:" + mNoDisturbOn
                        + " mLimitedNoDisturbOn:" + mLimitedNoDisturbOn
                        + " mNoDisturbStartTime:" + mNoDisturbStartTime
                        + " mNoDisturbStopTime:" + mNoDisturbStopTime
                        + " mNotifyWhenScreenOn:" + mNotifyWhenScreenOn);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            String lastpath = uri.getLastPathSegment();
            if (lastpath == null) {
                return;
            }
            if (lastpath.startsWith(NO_DISTURB_ON) || lastpath.startsWith(LIMITED_NO_DISTURB_ON)) {
                mNoDisturbOn = isNoDisturbOn(mContext);
                mLimitedNoDisturbOn = isLimitedNoDisturbOn(mContext);
                mNoDisturbStartTime = getLimitedStartTime(mContext);
                mNoDisturbStopTime = getLimitedStopTime(mContext);
            } else if (lastpath.startsWith(NO_DISTURB_START_TIME)) {
                mNoDisturbStartTime = getLimitedStartTime(mContext);
            } else if (lastpath.startsWith(NO_DISTURB_STOP_TIME)) {
                mNoDisturbStopTime = getLimitedStopTime(mContext);
            } else if (lastpath.startsWith(NOTIFY_WHEN_SCREEN_ON)) {
                mNotifyWhenScreenOn = isNotifyWhenScreenOn(mContext);
            } else if (lastpath.startsWith(TNT_NO_DISTURB_ON)) {
                mTntNoDisturbOn = isTntNoDisturbOn(mContext);
            }
            if (SMDBG) {
                Log.d(TAG, "DisturbModeSettingsObserver onChange lastpath" + lastpath
                        + " mNoDisturbOn:" + mNoDisturbOn
                        + " mLimitedNoDisturbOn:" + mLimitedNoDisturbOn
                        + " mNoDisturbStartTime:" + mNoDisturbStartTime
                        + " mNoDisturbStopTime:" + mNoDisturbStopTime
                        + " mNotifyWhenScreenOn:" + mNotifyWhenScreenOn);
            }
        }
    }

    private boolean isNoDisturbOn() {
        return mNoDisturbOn;
    }

    private boolean isLimitedNoDisturbOn() {
        if (isNoDisturbOn()) {
            return mLimitedNoDisturbOn;
        }
        return false;
    }

    private int getLimitedStartTime() {
        if (isLimitedNoDisturbOn()) {
            return mNoDisturbStartTime;
        }
        return -1;
    }

    private int getLimitedStopTime() {
        if (isLimitedNoDisturbOn()) {
            return mNoDisturbStopTime;
        }
        return -1;
    }

    public boolean isInDisturbMode(String pkg) {
        if (mWhiteList.contains(pkg)) {
            return false;
        }
        return isInDisturbModePeriod();
    }

    public boolean isInDisturbModePeriod() {
        if (!isNoDisturbOn()) {
            return false;
        }
        if (!isLimitedNoDisturbOn()) {
            return true;
        }
        int starttime = getLimitedStartTime();
        int endtime = getLimitedStopTime();
        int nowtime = getNowTime();
        if (starttime < endtime) {
            if (starttime <= nowtime && nowtime < endtime) {
                return true;
            }
        } else if (starttime >= endtime) {
            if (starttime <= nowtime || nowtime < endtime) {
                return true;
            }
        }
        return false;
    }

    public boolean isInDisturbModeWithPolicy(String pkg) {
        if (mWhiteList.contains(pkg)) {
            return false;
        }
        if (mNotifyWhenScreenOn && mScreenOn) {
            if (SMDBG) {
                Slog.d(TAG, "not in disturb mode with notify screen on policy");
            }
            return false;
        }
        return isInDisturbModePeriod();
    }

    public boolean isInDisturbModeForLightScreen(String pkg) {
        if (mLightUpScreenWhiteList.contains(pkg)) {
            return false;
        }
        return isInDisturbMode(pkg);
    }

    public boolean isInDisturbModeForNotificationRecord(String pkg) {
        if (mNotificationCallRecordWhiteList.contains(pkg)) {
            return false;
        }
        return isInDisturbMode(pkg);
    }

    public boolean isInTntDisturbMode() {
        return mTntNoDisturbOn;
    }
}
