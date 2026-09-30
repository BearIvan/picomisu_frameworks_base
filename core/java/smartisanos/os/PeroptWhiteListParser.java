// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoSmtBase;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.os.BackgroundThread;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smartisan performance optimisation white lists ("peropt"): per package, activity and
 * application-info flags read from {@code /system/etc/} (defaults) or {@code /data/system/}
 * (updates pushed by the Smartisan data sync provider), applied to {@link ApplicationInfo}
 * through its Smartisan extension. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class PeroptWhiteListParser {
    public static final boolean DEBUG_PEROPT_LIST =
            SystemProperties.getBoolean("debug.smartisanos.peropt.list", false);
    private static final boolean DEBUG_APPTYPE =
            SystemProperties.getBoolean("debug.smartisanos.apptype", false);
    private static final String TAG = "PeroptWhiteListParser";

    public static final String UPDATE_PATH = "/data/system/";
    public static final String UPDATE_TEMP_PATH = "/data/syslog/";
    private static final String DEFAULT_PATH = "/system/etc/";

    private static final String PEROPT_PACKAGE_FILE_NAME = "OptPackageWhiteList.xml";
    private static final String PEROPT_ACTIVITY_FILE_NAME = "OptActivityWhiteList.xml";
    private static final String PEROPT_APP_INFO_FILE_NAME = "OptAppInfoWhiteList.xml";
    private static final String APPTYPEIDCONNECTION_FILE_NAME = "AppTypeIdConnection.xml";
    private static final String PERF_PROFILE_FILE_NAME = "PerfProfile.xml";
    private static final String POWER_WAKELOCK_BLACK_LIST_FILE_NAME =
            "PowerWakelockBlackList.xml";
    private static final String APP_COMPOSITION_FILE_NAME = "AppCompositionWhiteList.xml";
    private static final String CHAIN_BOOT_FILE_NAME = "chainboot.xml";
    private static final String GAME_LIST_FILE_NAME = "game_list.json";
    private static final String GMS_LIST_FILE_NAME = "gms_list.json";
    private static final String TNT_COMPATIBILITY_APP_LIST_FILE_NAME =
            "tnt_compatibility_apps.json";
    private static final String BOOST_NETWORK_FILE_NAME = "boost_network.json";
    private static final String BOOM_DISABLE_LIST_FILE_NAME = "boom_disable_list.json";
    private static final String ACTIVITIES_MOVE_TO_DIFF_DISPLAY =
            "acts_can_moved_to_diff_display_list.json";
    public static final String REVONE_FILE_NAME = "revone_window_config.xml";

    public static final String PEROPT_PACKAGE_TASK_NAME = "system_package_white_list";
    public static final String PEROPT_ACTIVITY_TASK_NAME = "system_activity_white_list";
    public static final String CHAIN_BOOT_TASK_NAME = "peropt_chain_boot";
    public static final String GAME_LIST_TASK_NAME = "peropt_game_list";
    public static final String GMS_LIST_TASK_NAME = "peropt_gms_list";
    public static final String REVONE_LIST_TASK_NAME = "peropt_revone_window_config_list_enc";
    public static final String TNT_COMPATIBILITY_APPS_TASK_NAME =
            "peropt_tnt_compatibility_apps";
    public static final String PEROPT_SMTOPS_POLICY = "smtops_policy";
    public static final String BOOST_NETWORK_TASK_NAME = "boost_network";
    public static final String POWER_WAKELOCK_BLACK_LIST_TASK_NAME =
            "peropt_power_wakelock_black_list";
    public static final String POWER_APPTYPEIDCONNECTION_LIST_TASK_NAME =
            "peropt_apptype_id_list";
    public static final String MODIFY_PACKAGE_TASK_NAME = "modify_package_list";
    public static final String STARTING_WINDOW_LIST_TASK_NAME = "starting_window_list";

    public static final String PEROPT_WHITE_LIST_UPDATE =
            "com.android.providers.downloads.ACTION_SYNC_DATA_FINISH";
    public static final String PEROPT_PACKAGE_WHITE_LIST_UPDATE =
            "com.android.providers.downloads.ACTION_SYNC_PACKAGE_DATA_FINISH";
    public static final String ACTION_CONFIG_UPDATE_FEED_BACK =
            "com.android.providers.downloads.ACTION_SYNC_DATA_FINISH_FEEDBACK";
    private static final String PEROPT_APPINFO_WHITE_LIST_UPDATE =
            "com.android.providers.downloads.ACTION_APPINFO_WHITE_LIST_UPDATE";
    private static final String URI = "content://com.smartisanos.datasync/sync_datas";

    private static final String CHECK_APP_TYPE_KEY = "applicationinfo";
    private static final String PACKAGENAME = "packageName";
    private static final String APPTYPES = "apptypes";
    private static final String TYPE_DEFAULT_STARTING_WINDOW = "default";

    private static final int MSG_UPDATE_APPTYPE = 1;
    private static final int MSG_PACKAGE_REMOVED = 2;
    private static final int MSG_CHECK_APPTYPE = 3;

    private static final int SAVE_DATA_TYPE_PACKAGE = 0;
    private static final int SAVE_DATA_TYPE_ACTIVITY = 1;
    private static final int SAVE_DATA_TYPE_APP_INFO = 2;

    /** Update list types ({@link #getUpdateListTypeByTaskName}). */
    public static final int GAME = 0;
    public static final int GMS = 1;
    public static final int SYSTEM_PACKAGE = 2;
    public static final int CHAIN_BOOT = 3;
    public static final int REV_ONE = 4;
    public static final int TNT_COMPATIBILITY_MODE = 5;
    public static final int SYSTEM_ACTIVITY = 6;
    public static final int SMTOPS_POLICY = 7;
    public static final int BOOST_NETWORK = 8;
    public static final int POWER_WAKELOCK_BLACK_LIST = 9;
    public static final int MOVE_ACT_TO_DIFF_DISPLAY = 10;

    /** Parser and storage results. */
    public static final int PARSER_FAILD = 1;
    public static final int PARSER_SUCCESS = 2;
    public static final int SAVE_FILE_FAILD = 3;
    public static final int SAVE_FILE_SUCCESS = 4;
    public static final int DEVICE_NOT_MATCH = 5;
    public static final int VERSION_TOO_LOW = 7;

    /** Performance profiles ({@code PerfProfile.xml}). */
    public static final int DEFAULT_PROFILE = 1;
    public static final int GAME_NET_PROFILE = 3;
    public static final int GAME_PROFILE = 4;
    public static final int VIDEO_PROFILE = 5;
    public static final int BENCHMARK_PROFILE = 8;
    public static final int SCREEN_OFF_PROFILE = 9;
    public static final int PC_MODE_PROFILE = 10;
    public static final int PROFILE_NUM = 10;

    /** Activity white list flags ({@code OptActivityWhiteList.xml}). */
    public static final int SMFLAG_ACTIVITY_DISABLE_START = 0x00000001;
    public static final int SMFLAG_ACTIVITY_SHOW_WHEN_LOCKED = 0x00000002;
    public static final int SMFLAG_ACTIVITY_UNBLOCK_SHOW_WHEN_LOCKED = 0x00000004;
    public static final int SMFLAG_ACTIVITY_GHOST = 0x00000008;
    public static final int SMFLAG_ACTIVITY_INTERPECT_START_PROCESS = 0x00000010;
    public static final int SMFLAG_ACTIVITY_HOME = 0x00000020;
    public static final int SMFLAG_ACTIVITY_LAYER_HARDWARE = 0x00000040;
    public static final int SMFLAG_ACTIVITY_NOTCH_BLACK_STATUS_BAR = 0x00000080;
    public static final int SMFLAG_ACTIVITY_DISABLE_GESTURE = 0x00000100;
    public static final int SMFLAG_ACTIVITY_DISABLE_MENU = 0x00000200;
    public static final int SMFLAG_ACTIVITY_REPLACE_HOME_WITH_BACK = 0x00000400;
    public static final int SMFLAG_ACTIVITY_DISABLE_BOTTOM_BACK = 0x00000800;
    public static final int SMFLAG_ACTIVITY_DISABLE_LEFT_BACK = 0x00001000;
    public static final int SMFLAG_ACTIVITY_DISABLE_RIGHT_BACK = 0x00002000;
    public static final int SMFLAG_ACTIVITY_REPORT_MULTIWINDOW_MODE = 0x00004000;
    public static final int SMFLAG_ACTIVITY_SMARTISANOS_HOME = 0x00008000;

    public static final String APP_COMPOSITION_PACKAGE_NAME_PATTERN_STRING = "NS_APP\\[(.+)\\]";
    public static final Pattern APP_COMPOSITION_PACKAGE_NAME_PATTERN =
            Pattern.compile(APP_COMPOSITION_PACKAGE_NAME_PATTERN_STRING);

    private int mVersionPackage = 0;
    private int mVersionActivity = 0;
    private int mVersionAppInfo = 0;
    private int mVersionAppComposition = 0;
    private static String mDevice = SystemProperties.get("ro.product.device");

    private static Object mLock = new Object();

    public static PeroptWhiteListParser mInstance;
    private static IPackageManager mPackageManger;
    private Context mContext;
    private String mReceivedMD5;
    private PeroptHandler mHandler;
    private BroadcastReceiver mBcReceiver;

    public static HashMap<String, WhiteItem> packageWhiteList = new HashMap<>();
    private static HashMap<String, WhiteItem> mAppInfoList = new HashMap<>();
    private static HashMap<String, WhiteItem> mEnduranceAppInfoList = new HashMap<>();
    private static HashMap<String, WhiteItem> mAppCompositionWhiteList = new HashMap<>();

    public static HashSet<String> sGameList = new HashSet<>();
    public static HashSet<String> sGmsList = new HashSet<>();
    public static HashSet<String> sBoostNetworkList = new HashSet<>();
    public static HashMap<String, Integer> sAppType = new HashMap<>(256);
    public static HashMap<Integer, Integer> sAppTypeId = new HashMap<>(64);
    public static HashMap<String, Integer> sPerfProfiles = new HashMap<>();
    public static HashSet<String> sTntCompatibilityApps = new HashSet<>();
    public static HashSet<String> sBoomDisableList = new HashSet<>();
    public static HashMap<String, Vector<String>> sPowerWakelockBlackList = new HashMap<>();
    public static HashSet<String> sActivitiesCanBeMovedDiffDisplayList = new HashSet<>();

    private PeroptWhiteListParser() {
        initWhiteList(PEROPT_APP_INFO_FILE_NAME);
        initWhiteList(PEROPT_PACKAGE_FILE_NAME);
        initWhiteList(PEROPT_ACTIVITY_FILE_NAME);
        initWhiteList(APPTYPEIDCONNECTION_FILE_NAME);
        initWhiteList(PERF_PROFILE_FILE_NAME);
        initWhiteList(POWER_WAKELOCK_BLACK_LIST_FILE_NAME);

        initWhiteList(APP_COMPOSITION_FILE_NAME);

        initPackageTypeList(GAME_LIST_FILE_NAME, sGameList);
        initPackageTypeList(GMS_LIST_FILE_NAME, sGmsList);
        initPackageTypeList(TNT_COMPATIBILITY_APP_LIST_FILE_NAME, sTntCompatibilityApps);
        initPackageTypeList(BOOST_NETWORK_FILE_NAME, sBoostNetworkList);
        initPackageTypeList(BOOM_DISABLE_LIST_FILE_NAME, sBoomDisableList);
        initPackageTypeList(ACTIVITIES_MOVE_TO_DIFF_DISPLAY, sActivitiesCanBeMovedDiffDisplayList);
        Slog.i(TAG, "initPackageTypeList sActivitiesCanBeMovedDiffDisplayList = "
                + sActivitiesCanBeMovedDiffDisplayList);
        for (String pkg : sActivitiesCanBeMovedDiffDisplayList) {
            Slog.i(TAG, "initPackageTypeList pkg = " + pkg);
        }

        mHandler = new PeroptHandler(BackgroundThread.getHandler().getLooper());
    }

    private void initWhiteListForAppInfo(String fileName) {
        Slog.e(TAG, "initWhiteListForAppInfo");
        InputStream is = null;
        File updateFile = new File(fileName);
        if (updateFile.exists()) {
            try {
                is = new FileInputStream(updateFile);
                if (DEBUG_PEROPT_LIST) Log.i(TAG, "use Updated Peropt White List");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        if (is == null) {
            Log.e(TAG, "## initXmlWhiteList Peropt White List Unknown error is == null.");
            return;
        }

        if (fileName.contains(PEROPT_APP_INFO_FILE_NAME)) {
            parserAppInfoFromPush(is);
        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initWhiteList(String fileName) {
        InputStream is = null;
        if (!isUseDefultPath(fileName)) {
            File updateFile = new File(UPDATE_PATH + fileName);
            if (updateFile.exists()) {
                try {
                    is = new FileInputStream(updateFile);
                    if (DEBUG_PEROPT_LIST) Log.i(TAG, "use Updated Peropt White List");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
            }
        } else {
            File defaultFile = new File(DEFAULT_PATH + fileName);
            if (defaultFile.exists()) {
                try {
                    is = new FileInputStream(defaultFile);
                    if (DEBUG_PEROPT_LIST) Log.i(TAG, "use Default Peropt White List");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                Log.e(TAG, "## Peropt White List xml not exist.");
                return;
            }
        }

        if (is == null) {
            Log.e(TAG, "## initXmlWhiteList Peropt White List Unknown error is == null.");
            return;
        }

        if (PEROPT_PACKAGE_FILE_NAME.equals(fileName)) {
            parserPackage(is);
        } else if (PEROPT_ACTIVITY_FILE_NAME.equals(fileName)) {
            parserActivity(is);
        } else if (APPTYPEIDCONNECTION_FILE_NAME.equals(fileName)
                || PERF_PROFILE_FILE_NAME.equals(fileName)
                || POWER_WAKELOCK_BLACK_LIST_FILE_NAME.equals(fileName)) {
            notUpdateTypeParser(is);
        } else if (PEROPT_APP_INFO_FILE_NAME.equals(fileName)) {
            parserAppInfo(is);
        } else if (APP_COMPOSITION_FILE_NAME.equals(fileName)) {
            parserAppComposition(is);
        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isUseDefultPath(String fileName) {
        InputStream updateIs = null;
        int updateVersion = -1;
        int defaultVersion = -1;
        File updateFile = new File(UPDATE_PATH + fileName);
        if (updateFile.exists()) {
            try {
                updateIs = new FileInputStream(updateFile);
                updateVersion = parserVersion(updateIs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        InputStream defaultIs = null;
        File defaultFile = new File(DEFAULT_PATH + fileName);
        if (defaultFile.exists()) {
            try {
                defaultIs = new FileInputStream(defaultFile);
                defaultVersion = parserVersion(defaultIs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            if (updateIs != null) {
                updateIs.close();
                updateIs = null;
            }
            if (defaultIs != null) {
                defaultIs.close();
                defaultIs = null;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (DEBUG_PEROPT_LIST) {
            Log.i(TAG, "defaultVersion = " + defaultVersion + " ; updateVersion = "
                    + updateVersion);
        }
        int version = 0;
        if (defaultVersion > updateVersion) {
            version = defaultVersion;
        } else {
            version = updateVersion;
        }
        switch (fileName) {
            case PEROPT_APP_INFO_FILE_NAME: mVersionAppInfo = version; break;
            case PEROPT_PACKAGE_FILE_NAME: mVersionPackage = version; break;
            case PEROPT_ACTIVITY_FILE_NAME: mVersionActivity = version; break;
            case APP_COMPOSITION_FILE_NAME: mVersionAppComposition = version; break;
        }

        return defaultVersion > updateVersion;
    }

    private void initPackageTypeList(String listName, HashSet<String> list) {
        File file = new File(UPDATE_PATH + listName);
        if (!file.exists()) {
            file = new File(DEFAULT_PATH + listName);
            if (!file.exists()) {
                Slog.w(TAG, "initPackageTypeList failed, lose file [" + listName + "]");
                return;
            }
        }
        if (!parserPackageTypeList(file, list)) {
            Slog.w(TAG, "initPackageTypeList failed, [" + file.getAbsolutePath()
                    + "] use default file try again");
            parserPackageTypeList(new File(DEFAULT_PATH + listName), list);
        }
    }

    private boolean parserPackageTypeList(File file, HashSet<String> list) {
        try {
            list.clear();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[4096];
            int count = -1;
            while ((count = fis.read(buf)) > 0) {
                baos.write(buf, 0, count);
            }
            fis.close();
            byte[] data = baos.toByteArray();
            JSONArray array = new JSONArray(new String(data));
            int length = array.length();
            for (int i = 0; i < length; i++) {
                String pkg = array.getString(i);
                list.add(pkg);
            }
            return true;
        } catch (Exception e) {
            list.clear();
            e.printStackTrace();
        }
        return false;
    }

    private boolean notUpdateTypeParser(InputStream is) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("AppTypeId")) {
                            int appId = Integer.valueOf(parser.getAttributeValue(null, "name"));
                            int appType = Integer.parseInt(
                                    parser.getAttributeValue(null, "value"), 2);
                            if (DEBUG_APPTYPE) {
                                Slog.i(TAG, "parse AppTypeId id = " + appId + ",, type = "
                                        + appType);
                            }
                            sAppTypeId.put(appId, appType);
                        } else if (parser.getName().equals("application")) {
                            String name = parser.getAttributeValue(null, "name");
                            int profile = Integer.parseInt(
                                    parser.getAttributeValue(null, "profile"));
                            if (DEBUG_PEROPT_LIST) {
                                Slog.i(TAG, "parse perfile name = " + name + ", profile = "
                                        + profile);
                            }
                            sPerfProfiles.put(name, profile);
                        } else if (parser.getName().equals("package")) {
                            String packageName = parser.getAttributeValue(null, "name");
                            String tag = parser.getAttributeValue(null, "value");
                            if (DEBUG_PEROPT_LIST) {
                                Slog.i(TAG, "parse wakelock black list packageName = "
                                        + packageName + ", tag = " + tag);
                            }
                            Vector vector = sPowerWakelockBlackList.get(packageName);
                            if (vector == null) {
                                vector = new Vector();
                            }
                            vector.addElement(tag);
                            sPowerWakelockBlackList.put(packageName, vector);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int loadData(String taskName) {
        if (DEBUG_PEROPT_LIST) Log.i(TAG, " ### read db from download center.");
        int result = -1;
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = Uri.parse(URI);
        String[] params = new String[] {taskName};
        Cursor cursor = resolver.query(uri, null, null, params, null);
        byte[] data = null;
        try {
            if (cursor != null) {
                int md5Index = cursor.getColumnIndex("md5");
                int dataIndex = cursor.getColumnIndex("data");
                if (cursor.moveToFirst()) {
                    do {
                        mReceivedMD5 = cursor.getString(md5Index);
                        data = cursor.getBlob(dataIndex);
                    } while (cursor.moveToNext());
                }
                if (data != null) {
                    if (DEBUG_PEROPT_LIST) {
                        Log.i(TAG, "read data from provider! md5: " + mReceivedMD5
                                + ", data: " + new String(data));
                    }
                    result = updateDataFromRemote(data, taskName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            cursor.close();
        }
        return result;
    }

    private int updateDataFromRemote(byte[] data, String taskName) {
        int result = -1;
        sendFeedBack(taskName);
        if (CHAIN_BOOT_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + CHAIN_BOOT_FILE_NAME, data);
            return result;
        } else if (GAME_LIST_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + GAME_LIST_FILE_NAME, data);
            initPackageTypeList(GAME_LIST_FILE_NAME, sGameList);
            return result;
        } else if (GMS_LIST_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + GMS_LIST_FILE_NAME, data);
            initPackageTypeList(GMS_LIST_FILE_NAME, sGmsList);
            return result;
        } else if (REVONE_LIST_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + REVONE_FILE_NAME, data);
            return result;
        } else if (TNT_COMPATIBILITY_APPS_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + TNT_COMPATIBILITY_APP_LIST_FILE_NAME, data);
            initPackageTypeList(TNT_COMPATIBILITY_APP_LIST_FILE_NAME, sTntCompatibilityApps);
            return result;
        } else if (BOOST_NETWORK_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + BOOST_NETWORK_FILE_NAME, data);
            initPackageTypeList(BOOST_NETWORK_FILE_NAME, sBoostNetworkList);
            return result;
        } else if (POWER_WAKELOCK_BLACK_LIST_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + POWER_WAKELOCK_BLACK_LIST_FILE_NAME, data);
            sPowerWakelockBlackList.clear();
            initWhiteList(POWER_WAKELOCK_BLACK_LIST_FILE_NAME);
            return result;
        } else if (POWER_APPTYPEIDCONNECTION_LIST_TASK_NAME.equals(taskName)) {
            result = saveToStorage(UPDATE_PATH + APPTYPEIDCONNECTION_FILE_NAME, data);
            sAppTypeId.clear();
            initWhiteList(APPTYPEIDCONNECTION_FILE_NAME);
            return result;
        }

        InputStream is = new ByteArrayInputStream(data);

        synchronized (mLock) {
            if (PEROPT_PACKAGE_TASK_NAME.equals(taskName)) {
                result = parserPackage(is);
            } else if (PEROPT_ACTIVITY_TASK_NAME.equals(taskName)) {
                result = parserActivity(is);
            }

            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result != PARSER_SUCCESS) {
                return result;
            }

            if (DEBUG_PEROPT_LIST) Log.i(TAG, "### finishloading, update map");
            if (PEROPT_PACKAGE_TASK_NAME.equals(taskName)) {
                result = saveDataToXmlFile(UPDATE_PATH + PEROPT_PACKAGE_FILE_NAME,
                        SAVE_DATA_TYPE_PACKAGE);
            } else if (PEROPT_ACTIVITY_TASK_NAME.equals(taskName)) {
                result = saveDataToXmlFile(UPDATE_PATH + PEROPT_ACTIVITY_FILE_NAME,
                        SAVE_DATA_TYPE_ACTIVITY);
            }
        }
        return result;
    }

    private int parserPackage(InputStream is) {
        ArrayList<Integer> valueList = null;
        String key = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            WhiteItem item = null;
            String pkgName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals("package")) {
                            pkgName = parser.getAttributeValue(null, "name");
                            int flag = 0;
                            if (parser.getAttributeValue(null, "value") != null) {
                                flag = Integer.parseInt(parser.getAttributeValue(null, "value"),
                                        2);
                            }
                            int vrFlag = 0;
                            if (parser.getAttributeValue(null, "vrValue") != null) {
                                vrFlag = Integer.parseInt(
                                        parser.getAttributeValue(null, "vrValue"), 2);
                            }
                            item = new WhiteItem();
                            item.SMFlag = flag;
                            if ((vrFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_DEXOAT_APP) != 0) {
                                item.SMValue.put("DexAppFlag", 1);
                            }
                            if ((vrFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_DOWNLOAD_PROFILE)
                                    != 0) {
                                item.SMValue.put("downloadProfileFlag", 1);
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_LAUNCH_BOOST) != 0) {
                                int boostTime = Integer.valueOf(
                                        parser.getAttributeValue(null, "LaunchBoost"));
                                if (boostTime != 0) {
                                    item.SMValue.put("LaunchBoost", boostTime);
                                }
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PREFETCH_WHITE_LIST_APP)
                                    != 0) {
                                String versionName = parser.getAttributeValue(null,
                                        "VersionName");
                                if (versionName != null && !versionName.isEmpty()) {
                                    item.SMValue.put("VersionName", versionName);
                                }
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_STARTING_WINDOW) != 0) {
                                String swType = parser.getAttributeValue(null, "StartingWindow");
                                swType = swType != null ? swType : TYPE_DEFAULT_STARTING_WINDOW;
                                item.SMValue.put("StartingWindow", swType);
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PRIMARY_PROF) != 0) {
                                item.SMValue.put("PrimaryProf", 1);
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PERFORMANCE_TOOL) != 0) {
                                int tooltype = Integer.valueOf(
                                        parser.getAttributeValue(null, "tooltype"));
                                item.SMValue.put("tooltype", tooltype);
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_VR_SHELL_CPUSET) != 0) {
                                int cpusetType = Integer.valueOf(
                                        parser.getAttributeValue(null, "type"));
                                item.SMValue.put("type", cpusetType);
                            }
                            if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_OOM_ADJUST) != 0) {
                                int adj = Integer.valueOf(parser.getAttributeValue(null, "adj"));
                                item.SMValue.put("adj", adj);
                            }
                            WhiteItem oldItem = packageWhiteList.get(pkgName);
                            if (oldItem != null) {
                                oldItem.SMFlag |= item.SMFlag;
                                if (!item.SMValue.isEmpty()) {
                                    oldItem.SMValue.putAll(item.SMValue);
                                }
                            } else {
                                packageWhiteList.put(pkgName, item);
                            }
                        } else if (tagName.equals("config")) {
                            String deviceString = parser.getAttributeValue(null, "device");
                            if (deviceString != null && !"".equals(deviceString)
                                    && !deviceString.equals(mDevice)) {
                                Log.w(TAG, "device err, mDevice: " + mDevice + ", new device: "
                                        + deviceString);
                                is.close();
                                return DEVICE_NOT_MATCH;
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return PARSER_FAILD;
        }
        return PARSER_SUCCESS;
    }

    private int parserVersion(InputStream is) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals("config")) {
                            String deviceString = parser.getAttributeValue(null, "device");
                            if (deviceString != null && !"".equals(deviceString)
                                    && !deviceString.equals(mDevice)) {
                                Log.w(TAG, "device err, mDevice: " + mDevice + ", new device: "
                                        + deviceString);
                                return -1;
                            }
                            String versionString = parser.getAttributeValue(null, "version");
                            if (versionString == null) {
                                return -1;
                            }
                            int version = Integer.parseInt(versionString);
                            return version;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    private int parserActivity(InputStream is) {
        ArrayList<Integer> valueList = null;
        String key = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("config")) {
                            String deviceString = parser.getAttributeValue(null, "device");
                            if (deviceString != null && !"".equals(deviceString)
                                    && !deviceString.equals(mDevice)) {
                                Log.w(TAG, "device err, mDevice: " + mDevice + ", new device: "
                                        + deviceString);
                                is.close();
                                return DEVICE_NOT_MATCH;
                            }
                        } else if (parser.getName().equals("activity")) {
                            String name = parser.getAttributeValue(null, "name");
                            int flag = Integer.parseInt(parser.getAttributeValue(null, "value"), 2);
                            WhiteItem item = new WhiteItem();
                            item.SMFlag = flag;
                            activityWhiteList.put(name, item);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return PARSER_FAILD;
        }
        return PARSER_SUCCESS;
    }

    private int parserAppInfo(InputStream is) {
        ArrayList<Integer> valueList = null;
        String key = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            WhiteItem item = null;
            String pkgName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals("package")) {
                            pkgName = parser.getAttributeValue(null, "name");
                            int flag = Integer.parseInt(
                                    parser.getAttributeValue(null, "appInfoFlag"), 2);
                            item = mAppInfoList.get(pkgName);
                            if (item == null) {
                                item = new WhiteItem();
                                item.appInfoFlag = flag;
                            }
                            if ((flag & ApplicationInfoSmtBase.APP_INFO_FLAG_APP_FPS_INFO) != 0) {
                                String appInfoJsonConfig = "";
                                long appLastTime = 0;
                                String appInfoJsonConfigValue =
                                        parser.getAttributeValue(null, "appInfoJsonConfig");
                                if (appInfoJsonConfigValue != null) {
                                    appInfoJsonConfig = appInfoJsonConfigValue;
                                }
                                String appLastTimeValue =
                                        parser.getAttributeValue(null, "appLastTime");
                                if (appLastTimeValue != null) {
                                    appLastTime = Long.parseLong(appLastTimeValue);
                                }
                                Slog.i(TAG, "parserAppInfo name=" + pkgName + " flag= " + flag
                                        + " appInfoJsonConfig=" + appInfoJsonConfig
                                        + " appLastTime=" + appLastTime);
                                item.SMValue.put("appInfoJsonConfig", appInfoJsonConfig);
                                item.SMValue.put("appLastTime", appLastTime);
                            }
                            if ((flag & ApplicationInfoSmtBase
                                    .APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO) != 0) {
                                boolean removeUnityChoreographerVsync = Boolean.parseBoolean(
                                        parser.getAttributeValue(null,
                                                "REMOVE_UNITY_CHOREOGRAPHER_VSYNC"));
                                if (removeUnityChoreographerVsync) {
                                    item.SMValue.put("REMOVE_UNITY_CHOREOGRAPHER_VSYNC",
                                            removeUnityChoreographerVsync);
                                }
                            }
                            if ((flag & ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO)
                                    != 0) {
                                boolean permissionFlag = Boolean.parseBoolean(
                                        parser.getAttributeValue(null, "permissionFlag"));
                                if (permissionFlag) {
                                    item.SMValue.put("permissionFlag", permissionFlag);
                                }
                            }
                            item.appInfoFlag |= flag;
                        } else if (tagName.equals("process")) {
                            String processName = parser.getAttributeValue(null, "name");
                            int mem = Integer.valueOf(parser.getAttributeValue(null, "mem"));
                            if (item != null) {
                                item.processInfo.put(processName, mem);
                            }
                            item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("package")) {
                            mAppInfoList.put(pkgName, item);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse AppInfo", e);
            e.printStackTrace();
            return PARSER_FAILD;
        }
        return PARSER_SUCCESS;
    }

    private int parserAppComposition(InputStream is) {
        ArrayList<Integer> valueList = null;
        String key = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            WhiteItem item = null;
            String pkgName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals("package")) {
                            pkgName = parser.getAttributeValue(null, "name");
                            int flag = Integer.parseInt(
                                    parser.getAttributeValue(null, "appCompositionFlag"), 2);
                            item = new WhiteItem();
                            item.appCompositionFlag = flag;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("package")) {
                            mAppCompositionWhiteList.put(pkgName, item);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return PARSER_FAILD;
        }
        return PARSER_SUCCESS;
    }

    private int parserAppInfoFromPush(InputStream is) {
        Slog.e(TAG, "parserAppInfoFromPush");
        ArrayList<Integer> valueList = null;
        String key = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            WhiteItem item = null;
            String pkgName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals("package")) {
                            pkgName = parser.getAttributeValue(null, "name");
                            int flag = Integer.parseInt(
                                    parser.getAttributeValue(null, "appInfoFlag"), 2);
                            item = mAppInfoList.get(pkgName);
                            if (item == null) {
                                item = new WhiteItem();
                                item.appInfoFlag = flag;
                            }
                            if ((flag & ApplicationInfoSmtBase.APP_INFO_FLAG_APP_FPS_INFO) != 0) {
                                String appInfoJsonConfig =
                                        "{\"app_fps\":0,\"eyebufferHeight\":0,\"eyebufferWidth\":0}";
                                long appLastTime = 0;
                                String appInfoJsonConfigValue =
                                        parser.getAttributeValue(null, "appInfoJsonConfig");
                                if (appInfoJsonConfigValue != null) {
                                    appInfoJsonConfig = appInfoJsonConfigValue;
                                }
                                String appLastTimeValue =
                                        parser.getAttributeValue(null, "appLastTime");
                                if (appLastTimeValue != null) {
                                    appLastTime = Long.parseLong(appLastTimeValue);
                                }
                                Slog.i(TAG, "parserAppInfo name=" + pkgName + " flag= " + flag
                                        + " appInfoJsonConfig=" + appInfoJsonConfig
                                        + " appLastTime=" + appLastTime);
                                item.SMValue.put("appInfoJsonConfig", appInfoJsonConfig);
                                item.SMValue.put("appLastTime", appLastTime);
                            }
                            if ((flag & ApplicationInfoSmtBase
                                    .APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO) != 0) {
                                boolean removeUnityChoreographerVsync = Boolean.parseBoolean(
                                        parser.getAttributeValue(null,
                                                "REMOVE_UNITY_CHOREOGRAPHER_VSYNC"));
                                if (removeUnityChoreographerVsync) {
                                    item.SMValue.put("REMOVE_UNITY_CHOREOGRAPHER_VSYNC",
                                            removeUnityChoreographerVsync);
                                }
                            }
                            if ((flag & ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO)
                                    != 0) {
                                boolean permissionFlag = Boolean.parseBoolean(
                                        parser.getAttributeValue(null, "permissionFlag"));
                                if (permissionFlag) {
                                    item.SMValue.put("permissionFlag", permissionFlag);
                                }
                            }
                            item.appInfoFlag |= flag;
                        } else if (tagName.equals("process")) {
                            String processName = parser.getAttributeValue(null, "name");
                            int mem = Integer.valueOf(parser.getAttributeValue(null, "mem"));
                            if (item != null) {
                                item.processInfo.put(processName, mem);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("package")) {
                            mAppInfoList.put(pkgName, item);
                            try {
                                Slog.i(TAG, "parserAppInfoFromPush updateAppInfo");
                                String appInfoJsonConfig =
                                        "{\"app_fps\":0,\"eyebufferHeight\":0,\"eyebufferWidth\":0}";
                                long appLastTime = -1;
                                boolean removeUnityChoreographerVsync = false;
                                boolean permissionFlag = false;
                                if (item.SMValue.get("appInfoJsonConfig") != null) {
                                    appInfoJsonConfig =
                                            item.SMValue.get("appInfoJsonConfig").toString();
                                }
                                if (item.SMValue.get("appLastTime") != null) {
                                    appLastTime = Long.parseLong(
                                            item.SMValue.get("appLastTime").toString());
                                }
                                if (item.SMValue.get("removeUnityChoreographerVsync") != null) {
                                    removeUnityChoreographerVsync = Boolean.parseBoolean(item
                                            .SMValue.get("removeUnityChoreographerVsync")
                                            .toString());
                                }
                                if (item.SMValue.get("permissionFlag") != null) {
                                    permissionFlag = Boolean.parseBoolean(
                                            item.SMValue.get("permissionFlag").toString());
                                }
                                mPackageManger.getISmtEx().updateAppInfo(pkgName,
                                        appInfoJsonConfig, appLastTime,
                                        removeUnityChoreographerVsync, permissionFlag, false,
                                        false);
                            } catch (RemoteException e) {
                                Slog.e(TAG, "parserAppInfoFromPush updateAppInfo failed: pkg: "
                                        + pkgName + " exception:" + e);
                            }
                        }
                        break;
                }
                eventType = parser.next();
            }
            saveDataToXmlFile(UPDATE_PATH + PEROPT_APP_INFO_FILE_NAME, SAVE_DATA_TYPE_APP_INFO);
        } catch (Exception e) {
            e.printStackTrace();
            return PARSER_FAILD;
        }
        return PARSER_SUCCESS;
    }

    private int saveDataToXmlFile(String writeToFile, int type) {
        if (DEBUG_PEROPT_LIST) Log.i(TAG, "### save current config to file. type: " + type);
        int result = -1;
        XmlSerializer serializer = Xml.newSerializer();
        FileOutputStream fos = null;
        String enter = System.getProperty("line.separator");
        try {
            fos = new FileOutputStream(writeToFile);
            serializer.setOutput(fos, "utf-8");

            // <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            serializer.startDocument("UTF-8", true);
            changeLine(serializer, enter);

            serializer.startTag("", "config");

            switch (type) {
                case SAVE_DATA_TYPE_PACKAGE:
                    serializer.attribute("", "version", String.valueOf(mVersionPackage));
                    changeLine(serializer, enter);
                    handlePackageAttribute(serializer, enter);
                    break;
                case SAVE_DATA_TYPE_ACTIVITY:
                    serializer.attribute("", "version", String.valueOf(mVersionActivity));
                    changeLine(serializer, enter);
                    handleActivityAttribute(serializer, enter);
                    break;
                case SAVE_DATA_TYPE_APP_INFO:
                    serializer.attribute("", "version", String.valueOf(mVersionAppInfo));
                    changeLine(serializer, enter);
                    handleAppInfoAttribute(serializer, enter);
                    break;
                default:
                    break;
            }

            serializer.endTag("", "config");
            serializer.endDocument();

            result = SAVE_FILE_SUCCESS;
        } catch (Exception e) {
            result = SAVE_FILE_FAILD;
            e.printStackTrace();
            Log.e(TAG, "saveDataToXmlFile exception: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private void handlePackageAttribute(XmlSerializer serializer, String enter) {
        try {
            Iterator<String> it = packageWhiteList.keySet().iterator();

            while (it.hasNext()) {
                String packageName = it.next();
                if (packageWhiteList.containsKey(packageName)) {
                    serializer.startTag("", "package");
                    serializer.attribute("", "name", packageName);
                    WhiteItem item = packageWhiteList.get(packageName);
                    boolean hasMemInfo = item.hasMemInfo();
                    int flag = item.SMFlag;
                    serializer.attribute("", "value", Integer.toBinaryString(flag));
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_LAUNCH_BOOST) != 0) {
                        int tempInt = (Integer) item.SMValue.get("LaunchBoost");
                        serializer.attribute("", "LaunchBoost", Integer.toString(tempInt));
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PREFETCH_WHITE_LIST_APP) != 0) {
                        String versionName = (String) item.SMValue.get("VersionName");
                        serializer.attribute("", "VersionName", versionName);
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_STARTING_WINDOW) != 0) {
                        String tempStr = (String) item.SMValue.get("StartingWindow");
                        if (!TYPE_DEFAULT_STARTING_WINDOW.equals(tempStr)) {
                            serializer.attribute("", "StartingWindow", tempStr);
                        }
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PRIMARY_PROF) != 0) {
                        serializer.attribute("", "PrimaryProf", "1");
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_PERFORMANCE_TOOL) != 0) {
                        int tempInt = (Integer) item.SMValue.get("tooltype");
                        serializer.attribute("", "tooltype", Integer.toString(tempInt));
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_VR_SHELL_CPUSET) != 0) {
                        int tempInt = (Integer) item.SMValue.get("type");
                        serializer.attribute("", "type", Integer.toString(tempInt));
                    }
                    if ((flag & ApplicationInfoSmtBase.PEROPT_FLAG_OOM_ADJUST) != 0) {
                        int tempInt = (Integer) item.SMValue.get("adj");
                        serializer.attribute("", "adj", Integer.toString(tempInt));
                    }
                    serializer.endTag("", "package");
                    changeLine(serializer, enter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "handleAttribute exception: " + e.getMessage());
        }
    }

    private void handleAppInfoAttribute(XmlSerializer serializer, String enter) {
        try {
            Iterator<String> it = mAppInfoList.keySet().iterator();

            while (it.hasNext()) {
                String packageName = it.next();
                WhiteItem item = mAppInfoList.get(packageName);
                if (item != null) {
                    serializer.startTag("", "package");
                    serializer.attribute("", "name", packageName);
                    boolean hasProcMemInfo = (item.appInfoFlag
                            & ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO) != 0;
                    serializer.attribute("", "appInfoFlag",
                            Integer.toBinaryString(item.appInfoFlag));
                    if (item.SMValue.get("appInfoJsonConfig") != null) {
                        serializer.attribute("", "appInfoJsonConfig",
                                item.SMValue.get("appInfoJsonConfig").toString());
                        Slog.i(TAG, packageName + " appInfoJsonConfig = "
                                + item.SMValue.get("appInfoJsonConfig").toString());
                    }
                    if (item.SMValue.get("appLastTime") != null) {
                        serializer.attribute("", "appLastTime",
                                item.SMValue.get("appLastTime").toString());
                    }
                    if (item.SMValue.get("REMOVE_UNITY_CHOREOGRAPHER_VSYNC") != null) {
                        serializer.attribute("", "REMOVE_UNITY_CHOREOGRAPHER_VSYNC",
                                item.SMValue.get("REMOVE_UNITY_CHOREOGRAPHER_VSYNC").toString());
                    }
                    if (item.SMValue.get("permissionFlag") != null) {
                        serializer.attribute("", "permissionFlag",
                                item.SMValue.get("permissionFlag").toString());
                    }
                    if (hasProcMemInfo) {
                        changeLine(serializer, enter);
                    }

                    for (String key : item.processInfo.keySet()) {
                        serializer.startTag("", "process");
                        serializer.attribute("", "name", key);
                        serializer.attribute("", "mem",
                                Integer.toString(item.processInfo.get(key)));
                        serializer.endTag("", "process");
                        changeLine(serializer, enter);
                    }

                    serializer.endTag("", "package");
                    changeLine(serializer, enter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "handleAttribute exception: " + e.getMessage());
        }
    }

    private void handleActivityAttribute(XmlSerializer serializer, String enter) {
        try {
            Iterator<String> itOfActivity = activityWhiteList.keySet().iterator();
            String activityName = "";
            int SMFlag = 0;
            while (itOfActivity.hasNext()) {
                serializer.startTag("", "activity");
                activityName = itOfActivity.next();
                serializer.attribute("", "name", activityName);
                WhiteItem item = activityWhiteList.get(activityName);
                SMFlag = item.SMFlag;
                serializer.attribute("", "value", Integer.toBinaryString(SMFlag));
                serializer.endTag("", "activity");
                changeLine(serializer, enter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "handleAttribute exception: " + e.getMessage());
        }
    }

    public static void changeLine(XmlSerializer serializer, String enter) {
        try {
            serializer.text(enter);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "changeLine exception: " + e.getMessage());
        }
    }

    /**
     * Whether single-layer (VR) composition is skipped for the surface or virtual display
     * named {@code name} ({@code AppCompositionWhiteList.xml}).
     */
    public static boolean isSkipSingleLayerComposition(String name) {
        try {
            WhiteItem packageNameWI = mAppCompositionWhiteList.get(name);
            if (packageNameWI != null && (packageNameWI.appCompositionFlag
                    & ApplicationInfoSmtBase.APP_COMPOSITION_INFO_SKIP_SINGLE_LAYER) != 0) {
                return true;
            }
            Matcher matcher = APP_COMPOSITION_PACKAGE_NAME_PATTERN.matcher(name);
            if (matcher.find()) {
                WhiteItem VDNameWI = mAppCompositionWhiteList.get(matcher.group(1));
                if (VDNameWI != null && (VDNameWI.appCompositionFlag
                        & ApplicationInfoSmtBase.APP_COMPOSITION_INFO_SKIP_SINGLE_LAYER) != 0) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isSkipSingleLayerComposition exception: " + e.getMessage());
        }
        return false;
    }

    /*
     * Must be called by the system server before any update is received.
     */
    public void setContext(Context context) {
        mContext = context;
        mPackageManger = AppGlobals.getPackageManager();
        initAppInfoBcReceiver();
    }

    private void sendFeedBack(String taskName) {
        Intent feedback = new Intent();
        feedback.setAction(ACTION_CONFIG_UPDATE_FEED_BACK);
        feedback.putExtra("md5", mReceivedMD5);
        feedback.putExtra("name", taskName);

        if (DEBUG_PEROPT_LIST) Log.i(TAG, "sendFeedBack: " + taskName + ", md5: " + mReceivedMD5);
        mContext.sendBroadcastAsUser(feedback, UserHandle.ALL);
    }

    private void initAppInfoBcReceiver() {
        Log.d(TAG, "initBcReceiver ");
        mBcReceiver = new UpdateAppInfoBroadcastReceiver();
        IntentFilter filter = new IntentFilter(PEROPT_APPINFO_WHITE_LIST_UPDATE);
        mContext.registerReceiver(mBcReceiver, filter);
    }

    private class UpdateAppInfoBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PEROPT_APPINFO_WHITE_LIST_UPDATE:
                    int isAbEnable = intent.getIntExtra("is_enable", -1);
                    boolean isRollBack = isAbEnable == 0;
                    String path = intent.getStringExtra("path");
                    String feature = intent.getStringExtra("feature");
                    Log.d(TAG, "receive path : " + path + " feature: " + feature
                            + " isRollBack:" + isRollBack);
                    if (!"update_appinfo".equals(feature)) {
                        sendResultToSlardar(isRollBack, 3);
                        Log.d(TAG, "receive unexpected feature: " + feature);
                        break;
                    }
                    if (!isRollBack) {
                        initWhiteListForAppInfo(path);
                    }
                    if (SystemProperties.getBoolean("persist.smartisanos.peropt.appinfo.debug",
                            false)) {
                        sendResultToSlardar(isRollBack, 3);
                    } else {
                        sendResultToSlardar(isRollBack, 2);
                    }

                    break;

                default:
                    Log.d(TAG, "receive invalid action: " + action);
                    break;
            }
        }
    }

    private void sendResultToSlardar(boolean isRollBack, int result) {
        Intent callBackintent = new Intent("bytedance.slardaros.feature_result");
        callBackintent.setComponent(new ComponentName("com.bytedance.os.slardar",
                "com.bytedance.os.slardar.CustomService"));
        callBackintent.putExtra("key_ab_feature", "update_appinfo");
        callBackintent.putExtra("key_ab_result", result);
        String onOff = "on";
        if (isRollBack) {
            onOff = "off";
        }
        callBackintent.putExtra("key_ab_phase", onOff);
        mContext.startService(callBackintent);
    }

    public static void updatePeroptFlagValue(ApplicationInfo info) {
        updatePeroptFlagValue(info, null);
    }

    public static void updatePeroptFlagValue(ApplicationInfo info, PackageParser.Package pkg) {
        if (info == null) {
            return;
        }
        info.getSmtEx().smartisanFlag &= ~ApplicationInfoSmtBase.SMARTISAN_FLAG_BG_CONTROL_UNSET;
        if (packageWhiteList.containsKey(info.packageName)) {
            WhiteItem wi = packageWhiteList.get(info.packageName);
            if (wi != null) {
                info.getSmtEx().peroptFlag = wi.SMFlag;
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL) != 0) {
                    info.getSmtEx().peroptFlag &= ~ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL;
                } else {
                    info.getSmtEx().peroptFlag |= ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_LAUNCH_BOOST) != 0) {
                    info.getSmtEx().smartisanLaunchBoostValue =
                            (Integer) wi.SMValue.get("LaunchBoost");
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PREFETCH_WHITE_LIST_APP) != 0) {
                    updatePrefetchAllow(info.getSmtEx(), pkg, (String) wi.SMValue.get("VersionName"));
                    prefetchWhiteList.add(info);
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_STARTING_WINDOW) != 0) {
                    String swType = (String) wi.SMValue.get("StartingWindow");
                    info.getSmtEx().smtStartingWindowType =
                            swType != null ? swType : TYPE_DEFAULT_STARTING_WINDOW;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PRIMARY_PROF) != 0) {
                    info.getSmtEx().smartisanCollectProf =
                            (Integer) wi.SMValue.get("PrimaryProf") == 1;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PERFORMANCE_TOOL) != 0) {
                    info.getSmtEx().perfToolType = (Integer) wi.SMValue.get("tooltype");
                }
                if (ApplicationInfoSmtBase.dex2oatSwitch && wi.SMValue
                        .containsKey("DexAppFlag") && (Integer) wi.SMValue
                        .get("DexAppFlag") == 1) {
                    info.getSmtEx().isDexApp = true;
                    Slog.i(TAG, "In packageWhiteList: " + info.packageName + " isDexApp= "
                            + info.getSmtEx().isDexApp);
                }
                if (wi.SMValue.containsKey("downloadProfileFlag") && (Integer) wi.SMValue
                        .get("downloadProfileFlag") == 1) {
                    info.getSmtEx().downloadProfileFlag = true;
                    Slog.i(TAG, "In packageWhiteList: " + info.packageName
                            + " downloadProfileFlag= " + info.getSmtEx().downloadProfileFlag);
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_VR_SHELL_CPUSET) != 0) {
                    info.getSmtEx().shellAppType = (Integer) wi.SMValue.get("type");
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_OOM_ADJUST) != 0) {
                    info.getSmtEx().vrOomAdj = (Integer) wi.SMValue.get("adj");
                }
            }
        }

        WhiteItem wi = mAppInfoList.get(info.packageName);
        if (wi != null
                && (wi.appInfoFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO) != 0) {
            info.getSmtEx().smtProcessMemInfo = wi.processInfo;
            Slog.i(TAG, "In AppInfoList: " + info.packageName + " processInfo = "
                    + wi.processInfo);
        }
        if (wi != null
                && (wi.appInfoFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_APP_FPS_INFO) != 0) {
            info.appInfoJsonConfig = wi.SMValue.get("appInfoJsonConfig").toString();
            info.appLastTime = Long.valueOf(wi.SMValue.get("appLastTime").toString());
            Slog.i(TAG, "In AppInfoList: " + info.packageName + " appInfoJsonConfig = "
                    + info.appInfoJsonConfig + " appLastTime = " + info.appLastTime);
        }
        if (wi != null && (wi.appInfoFlag & ApplicationInfoSmtBase
                .APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO) != 0) {
            info.getSmtEx().removeUnityChoreographerVsync = Boolean.valueOf(
                    wi.SMValue.get("REMOVE_UNITY_CHOREOGRAPHER_VSYNC").toString());
            Slog.i(TAG, "In AppInfoList: " + info.packageName
                    + " removeUnityChoreographerVsync = "
                    + info.getSmtEx().removeUnityChoreographerVsync);
        }
        if (wi != null
                && (wi.appInfoFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO) != 0) {
            info.getSmtEx().permissionFlag = Boolean.valueOf(
                    wi.SMValue.get("permissionFlag").toString());
            Slog.i(TAG, "In AppInfoList: " + info.packageName + " permissionFlag = "
                    + info.getSmtEx().permissionFlag);
        }

        try {
            if (mPackageManger == null) {
                mPackageManger = AppGlobals.getPackageManager();
            }
            if (mPackageManger != null) {
                Slog.i(TAG, "In packageWhiteList: updateAppInfo " + info.packageName);
                mPackageManger.getISmtEx().updateAppInfo(info.packageName,
                        info.appInfoJsonConfig,
                        info.appLastTime,
                        info.getSmtEx().removeUnityChoreographerVsync,
                        info.getSmtEx().permissionFlag,
                        info.getSmtEx().isDexApp,
                        info.getSmtEx().downloadProfileFlag);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "updatePeroptFlagValue updateAppInfo failed: pkg: " + info.packageName
                    + " exception:" + e);
        }
    }

    private static void updatePrefetchAllow(ApplicationInfoSmtBase info,
            PackageParser.Package pkg, String versions) {
        if (versions == null) return;
        String[] versionNames = versions.split(",");
        String versionName = "";
        if (pkg == null) {
            IPackageManager pm = AppGlobals.getPackageManager();
            PackageInfo pkgInfo = null;
            try {
                pkgInfo = pm.getPackageInfo(info.mInfo.packageName, 0,
                        UserHandle.getCallingUserId());
            } catch (RemoteException e) {
                Slog.w(TAG, "packagemamager getApplicationInfo error ", e);
            }
            if (pkgInfo == null) {
                Slog.w(TAG, "update version pkg " + pkgInfo.packageName + " is null");
                return;
            }
            versionName = pkgInfo.versionName;
        } else {
            versionName = pkg.mVersionName;
        }

        for (String version : versionNames) {
            if (version.equals(versionName)) {
                info.isAllowPrefetch = ApplicationInfoSmtBase.ALLOW_PREFETCH;
                break;
            }
        }
    }

    public static void updatePeroptFlagValue(ApplicationInfoSmtBase infoSmtEx,
            String packageName) {
        if (infoSmtEx == null) {
            return;
        }
        infoSmtEx.smartisanFlag &= ~ApplicationInfoSmtBase.SMARTISAN_FLAG_BG_CONTROL_UNSET;
        if (packageWhiteList.containsKey(packageName)) {
            WhiteItem wi = packageWhiteList.get(packageName);
            if (wi != null) {
                infoSmtEx.peroptFlag = wi.SMFlag;
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL) != 0) {
                    infoSmtEx.peroptFlag &= ~ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL;
                } else {
                    infoSmtEx.peroptFlag |= ApplicationInfoSmtBase.PEROPT_FLAG_BG_CONTROL;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_LAUNCH_BOOST) != 0) {
                    infoSmtEx.smartisanLaunchBoostValue = (Integer) wi.SMValue.get("LaunchBoost");
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PREFETCH_WHITE_LIST_APP) != 0) {
                    updatePrefetchAllow(infoSmtEx, null, (String) wi.SMValue.get("VersionName"));
                    prefetchWhiteList.add(infoSmtEx.mInfo);
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_STARTING_WINDOW) != 0) {
                    String swType = (String) wi.SMValue.get("StartingWindow");
                    infoSmtEx.smtStartingWindowType =
                            swType != null ? swType : TYPE_DEFAULT_STARTING_WINDOW;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PRIMARY_PROF) != 0) {
                    infoSmtEx.smartisanCollectProf = (Integer) wi.SMValue.get("PrimaryProf") == 1;
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_PERFORMANCE_TOOL) != 0) {
                    infoSmtEx.perfToolType = (Integer) wi.SMValue.get("tooltype");
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_VR_SHELL_CPUSET) != 0) {
                    infoSmtEx.shellAppType = (Integer) wi.SMValue.get("type");
                }
                if ((wi.SMFlag & ApplicationInfoSmtBase.PEROPT_FLAG_OOM_ADJUST) != 0) {
                    infoSmtEx.vrOomAdj = (Integer) wi.SMValue.get("adj");
                }
            }
        }

        WhiteItem wi = mAppInfoList.get(packageName);
        if (wi != null
                && (wi.appInfoFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO) != 0) {
            infoSmtEx.smtProcessMemInfo = wi.processInfo;
        }
    }

    /*
     * Whether the package is in the package white list with any of the {@code type} flags.
     */
    public static boolean inWhiteList(String pkgName, int type) {
        synchronized (mLock) {
            if (packageWhiteList.containsKey(pkgName) && packageWhiteList.get(pkgName) != null
                    && (packageWhiteList.get(pkgName).SMFlag & type) != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean inSmartisanList(String pkgName, int type) {
        synchronized (mLock) {
            if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GAME
                    && isGamePackage(pkgName)) {
                return true;
            } else if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GMS
                    && sGmsList.contains(pkgName)) {
                return true;
            } else if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE
                    && sTntCompatibilityApps.contains(pkgName)) {
                return true;
            } else if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP
                    && sBoomDisableList.contains(pkgName)) {
                return true;
            } else if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_BOOST_NETWORK
                    && sBoostNetworkList.contains(pkgName)) {
                return true;
            } else if (type == ApplicationInfoSmtBase.SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY
                    && sActivitiesCanBeMovedDiffDisplayList.contains(pkgName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGamePackage(String pkg) {
        if (pkg == null) {
            return false;
        }
        for (String packageName : sGameList) {
            if (pkg.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns the singleton, creating it (and reading the white lists) on first use.
     */
    public static PeroptWhiteListParser getInstance() {
        if (mInstance == null) {
            mInstance = new PeroptWhiteListParser();
        }
        return mInstance;
    }

    public static int getLaunchBoostValue(String pkgName) {
        Integer boostTime = null;
        if (packageWhiteList.containsKey(pkgName)) {
            boostTime = (Integer) packageWhiteList.get(pkgName).SMValue.get("LaunchBoost");
        }
        return boostTime == null ? -1 : boostTime.intValue();
    }

    public static boolean isCollectProfOn(String pkgName) {
        if (packageWhiteList.containsKey(pkgName)) {
            return (packageWhiteList.get(pkgName).SMFlag
                    & ApplicationInfoSmtBase.PEROPT_FLAG_PRIMARY_PROF) != 0;
        }
        return false;
    }

    /*
     * Activity white list (OptActivityWhiteList.xml), keyed by the activity class name or the
     * short component name; SMFlag holds SMFLAG_ACTIVITY_* bits.
     */
    public static HashMap<String, WhiteItem> activityWhiteList = new HashMap<>();
    public static ArrayList<ApplicationInfo> prefetchWhiteList = new ArrayList<>();

    public static class WhiteItem {
        public String name;
        public String pkgName;
        public int SMFlag;
        public int appInfoFlag;
        public int appCompositionFlag;
        public HashMap<String, Object> SMValue = new HashMap<>();
        public HashMap<String, Integer> processInfo = new HashMap<>(0);

        @Override
        public String toString() {
            return "white item name = " + name + ", SMFlag = " + Integer.toHexString(SMFlag);
        }

        public boolean hasMemInfo() {
            return (appInfoFlag & ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO) != 0;
        }
    }

    /*
     * Returns the SMFLAG_ACTIVITY_* flags of the activity, 0 if it is not in the white list.
     */
    public static int getActivityWhiteListType(String activityName) {
        WhiteItem item = activityWhiteList.get(activityName);
        if (item != null) {
            return item.SMFlag;
        }
        return 0;
    }

    /*
     * Replaces the SMFLAG_ACTIVITY_* flags of an activity that is in the white list.
     */
    public static void updateActivityWhiteListType(ActivityInfo info, int flag) {
        if (info == null) {
            return;
        }
        if (activityWhiteList.containsKey(info.name)) {
            activityWhiteList.get(info.name).SMFlag = flag;
        }
    }

    public static void setGhostActivities(List<ComponentName> activities) {
        Iterator iter = activityWhiteList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            WhiteItem item = (WhiteItem) entry.getValue();
            item.SMFlag &= ~SMFLAG_ACTIVITY_GHOST;
        }
        for (ComponentName name : activities) {
            WhiteItem oldItem = activityWhiteList.get(name.getClassName());
            if (oldItem != null) {
                oldItem.pkgName = name.getPackageName();
                oldItem.SMFlag |= SMFLAG_ACTIVITY_GHOST;
            } else {
                WhiteItem newItem = new WhiteItem();
                newItem.name = name.getClassName();
                newItem.pkgName = name.getPackageName();
                newItem.SMFlag |= SMFLAG_ACTIVITY_GHOST;
                activityWhiteList.put(name.getClassName(), newItem);
            }
        }
    }

    public static final int getPerfProfile(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return DEFAULT_PROFILE;
        }
        if (sPerfProfiles.containsKey(appInfo.packageName)) {
            return sPerfProfiles.get(appInfo.packageName);
        }

        return DEFAULT_PROFILE;
    }

    public static final int getPerfProfile(ApplicationInfoSmtBase appInfoSmtEx,
            String packageName) {
        if (appInfoSmtEx == null) {
            return DEFAULT_PROFILE;
        }
        if (sPerfProfiles.containsKey(packageName)) {
            return sPerfProfiles.get(packageName);
        }

        return DEFAULT_PROFILE;
    }

    public static void updateFlagValueByType(ApplicationInfo appInfo, int type) {
        if (appInfo == null) {
            return;
        }

        switch (type) {
            case GAME: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GAME);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GAME, value);
                break;
            }
            case GMS: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GMS);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GMS, value);
                break;
            }
            case SYSTEM_PACKAGE: {
                updatePeroptFlagValue(appInfo);
                break;
            }
            case TNT_COMPATIBILITY_MODE: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE,
                        value);
                break;
            }
            case ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP, value);
                break;
            }
            case BOOST_NETWORK: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_BOOST_NETWORK);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_BOOST_NETWORK, value);
                break;
            }
            case MOVE_ACT_TO_DIFF_DISPLAY: {
                boolean value = inSmartisanList(appInfo.packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY);
                setFlag(appInfo, ApplicationInfoSmtBase.SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY,
                        value);
                break;
            }
        }
    }

    public static void updateFlagValueByType(ApplicationInfoSmtBase appInfoSmtEx, int type,
            String packageName) {
        if (appInfoSmtEx == null) {
            return;
        }

        switch (type) {
            case GAME: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GAME);
                setFlag(appInfoSmtEx, ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GAME, value);
                break;
            }
            case GMS: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GMS);
                setFlag(appInfoSmtEx, ApplicationInfoSmtBase.SMARTISAN_FLAG_CATEGORY_GMS, value);
                break;
            }
            case SYSTEM_PACKAGE: {
                updatePeroptFlagValue(appInfoSmtEx, packageName);
                break;
            }
            case TNT_COMPATIBILITY_MODE: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE);
                setFlag(appInfoSmtEx, ApplicationInfoSmtBase.SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE,
                        value);
                break;
            }
            case ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP);
                setFlag(appInfoSmtEx, ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP,
                        value);
                break;
            }
            case BOOST_NETWORK: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_BOOST_NETWORK);
                setFlag(appInfoSmtEx, ApplicationInfoSmtBase.SMARTISAN_FLAG_BOOST_NETWORK, value);
                break;
            }
            case MOVE_ACT_TO_DIFF_DISPLAY: {
                boolean value = inSmartisanList(packageName,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY);
                setFlag(appInfoSmtEx,
                        ApplicationInfoSmtBase.SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY, value);
                break;
            }
        }
    }

    public void updateAppInfoByType(ApplicationInfo appInfo, File file, int type) {
        InputStream is = null;
        if (file.exists()) {
            try {
                is = new FileInputStream(file);
                if (DEBUG_PEROPT_LIST) Log.i(TAG, "Update appInfo " + type);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                file = null;
                return;
            }

            FileInputStream fis = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                fis = new FileInputStream(file);
                byte[] buf = new byte[4096];
                int count = -1;
                while ((count = fis.read(buf)) > 0) {
                    baos.write(buf, 0, count);
                }
                byte[] data = baos.toByteArray();
                boolean result = updateAppInfoByType(appInfo, new String(data), type);
                if (result) {
                    file.delete();
                }
            } catch (Exception e) {
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public boolean updateAppInfoByType(ApplicationInfo appInfo, String jsonStr, int type) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray proc = null;
            if (jsonObject.has("proc")) {
                proc = (JSONArray) jsonObject.get("proc");
            }
            String pkgName = (String) jsonObject.get("app_name");
            String appInfoJsonConfig = "";
            if (jsonObject.has("appInfoJsonConfig")) {
                appInfoJsonConfig = (String) jsonObject.get("appInfoJsonConfig");
            }
            long appLastTime = System.currentTimeMillis();
            boolean removeUnityChoreographerVsync = false;
            if (jsonObject.has("remove_unity_choreographer_vsync") && Boolean.parseBoolean(
                    (String) jsonObject.get("remove_unity_choreographer_vsync"))) {
                removeUnityChoreographerVsync = true;
            }

            boolean permissionFlag = false;
            if (jsonObject.has("permissionFlag")
                    && Boolean.parseBoolean((String) jsonObject.get("permissionFlag"))) {
                permissionFlag = true;
            }

            Log.d(TAG, "updateAppInfoByType pkgName= " + pkgName + " appInfoJsonConfig = "
                    + appInfoJsonConfig + " remove_unity_choreographer_vsync= "
                    + removeUnityChoreographerVsync + " permissionFlag=" + permissionFlag);

            if (mPackageManger != null) {
                try {
                    mPackageManger.getISmtEx().parseAppRefreshRate(jsonStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int length = 0;
            if (proc != null) {
                length = proc.length();
            }
            Log.d(TAG, "updateAppInfoByType length = " + length);
            if (length > 0 && appInfo.packageName.equals(pkgName)) {
                HashMap<String, Integer> processInfo = new HashMap<>(length);
                for (int i = 0; i < proc.length(); i++) {
                    JSONObject jo = proc.getJSONObject(i);
                    String key = (String) jo.get("proc_name");
                    Number value = (Number) jo.get("memory");
                    int memValue = value.intValue();
                    Log.d(TAG, "updateAppInfoByType proc_name= " + key + " memValue = " + memValue);
                    processInfo.put(key, memValue);
                }
                appInfo.getSmtEx().smtProcessMemInfo = processInfo;

                WhiteItem item = mAppInfoList.get(pkgName);
                if (item == null) {
                    item = new WhiteItem();
                    mAppInfoList.put(pkgName, item);
                }
                if (appInfoJsonConfig.length() > 0) {
                    item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_APP_FPS_INFO;
                    item.SMValue.put("appInfoJsonConfig", appInfoJsonConfig);
                    item.SMValue.put("appLastTime", appLastTime);
                    appInfo.appInfoJsonConfig = appInfoJsonConfig;
                    appInfo.appLastTime = appLastTime;
                }
                if (removeUnityChoreographerVsync) {
                    item.appInfoFlag |= ApplicationInfoSmtBase
                            .APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO;
                    item.SMValue.put("REMOVE_UNITY_CHOREOGRAPHER_VSYNC",
                            removeUnityChoreographerVsync);
                    appInfo.getSmtEx().removeUnityChoreographerVsync =
                            removeUnityChoreographerVsync;
                }
                if (permissionFlag) {
                    item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO;
                    item.SMValue.put("permissionFlag", permissionFlag);
                    appInfo.getSmtEx().permissionFlag = permissionFlag;
                }
                item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_PROC_MEM_INFO;
                item.processInfo = processInfo;
                int res = saveDataToXmlFile(UPDATE_PATH + PEROPT_APP_INFO_FILE_NAME,
                        SAVE_DATA_TYPE_APP_INFO);
                try {
                    mPackageManger.getISmtEx().updateAppInfo(pkgName, appInfoJsonConfig,
                            appLastTime, removeUnityChoreographerVsync, permissionFlag,
                            appInfo.getSmtEx().isDexApp, appInfo.getSmtEx().downloadProfileFlag);
                } catch (RemoteException e) {
                    Slog.e(TAG, "updateAppInfoByType updateAppInfo failed: pkg: " + pkgName
                            + " exception:" + e);
                }
                return res == SAVE_FILE_SUCCESS;
            } else if (appInfo.packageName.equals(pkgName)) {
                WhiteItem item = mAppInfoList.get(pkgName);
                if (item == null) {
                    item = new WhiteItem();
                    mAppInfoList.put(pkgName, item);
                }
                if (appInfoJsonConfig.length() > 0) {
                    item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_APP_FPS_INFO;
                    item.SMValue.put("appInfoJsonConfig", appInfoJsonConfig);
                    item.SMValue.put("appLastTime", appLastTime);
                    appInfo.appInfoJsonConfig = appInfoJsonConfig;
                    appInfo.appLastTime = appLastTime;
                }
                if (removeUnityChoreographerVsync) {
                    item.appInfoFlag |= ApplicationInfoSmtBase
                            .APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO;
                    item.SMValue.put("REMOVE_UNITY_CHOREOGRAPHER_VSYNC",
                            removeUnityChoreographerVsync);
                    appInfo.getSmtEx().removeUnityChoreographerVsync =
                            removeUnityChoreographerVsync;
                }
                if (permissionFlag) {
                    item.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO;
                    item.SMValue.put("permissionFlag", permissionFlag);
                    appInfo.getSmtEx().permissionFlag = permissionFlag;
                }
                int res = saveDataToXmlFile(UPDATE_PATH + PEROPT_APP_INFO_FILE_NAME,
                        SAVE_DATA_TYPE_APP_INFO);
                appInfoJsonConfig = getEnduranceAppInfo(appInfo);
                if (appInfoJsonConfig.length() > 0) {
                    appInfo.appInfoJsonConfig = appInfoJsonConfig;
                }
                try {
                    mPackageManger.getISmtEx().updateAppInfo(pkgName, appInfoJsonConfig,
                            appLastTime, removeUnityChoreographerVsync, permissionFlag,
                            appInfo.getSmtEx().isDexApp, appInfo.getSmtEx().downloadProfileFlag);
                } catch (RemoteException e) {
                    Slog.e(TAG, "updateAppInfoByType updateAppInfo failed: pkg: " + pkgName
                            + " exception:" + e);
                }
                return res == SAVE_FILE_SUCCESS;
            }
        } catch (Exception e) {
            Log.d(TAG, "updateAppInfoByType exception", e);
        }
        return false;
    }

    private String getEnduranceAppInfo(ApplicationInfo appInfo) {
        String enduranceJsonConfig = "";
        if (mEnduranceAppInfoList.containsKey(appInfo.packageName)) {
            String config = (String) mEnduranceAppInfoList.get(appInfo.packageName)
                    .SMValue.get("appInfoJsonConfig");
            if (config != null) {
                try {
                    if (!TextUtils.isEmpty(appInfo.appInfoJsonConfig)) {
                        JSONObject defaultConfig = new JSONObject(appInfo.appInfoJsonConfig);
                        JSONObject enduranceConfig = new JSONObject(config);
                        for (String key : enduranceConfig.keySet()) {
                            int val = enduranceConfig.optInt(key);
                            if (val != 0) {
                                defaultConfig.put(key, val);
                            }
                        }
                        enduranceJsonConfig = defaultConfig.toString();
                    } else {
                        enduranceJsonConfig = config;
                    }
                    return enduranceJsonConfig;
                } catch (Exception e) {
                    Log.e(TAG, "getEnduranceAppInfo exception", e);
                }
            }
        }
        return appInfo.appInfoJsonConfig;
    }

    public void updateEnduranceAppInfo(ApplicationInfo appInfo, String jsonStr, boolean enable) {
        String appInfoJsonConfig = "";
        if (appInfo != null) {
            String pkgName = appInfo.packageName;
            WhiteItem item = mEnduranceAppInfoList.get(pkgName);
            if (enable) {
                if (item == null) {
                    item = new WhiteItem();
                    mEnduranceAppInfoList.put(pkgName, item);
                }
                item.SMValue.put("appInfoJsonConfig", jsonStr);
                appInfoJsonConfig = getEnduranceAppInfo(appInfo);
            } else if (item != null) {
                mEnduranceAppInfoList.remove(pkgName);
                WhiteItem newItem = mAppInfoList.get(pkgName);
                if (newItem != null) {
                    appInfoJsonConfig = (String) newItem.SMValue.get("appInfoJsonConfig");
                }
            }

            appInfo.appInfoJsonConfig = appInfoJsonConfig;
            try {
                ApplicationInfoSmtBase smtEx = appInfo.getSmtEx();
                mPackageManger.getISmtEx().updateAppInfo(pkgName, appInfoJsonConfig,
                        appInfo.appLastTime, smtEx.removeUnityChoreographerVsync,
                        smtEx.permissionFlag, smtEx.isDexApp, smtEx.downloadProfileFlag);
            } catch (RemoteException e) {
                Log.e(TAG, "updateEnduranceAppInfo failed: pkg: " + pkgName + " exception:" + e);
            }
        }
    }

    public void updateVrVideoAppInfo(ApplicationInfo appInfo, String jsonStr) {
        if (appInfo == null || appInfo.packageName == null) {
            return;
        }

        String pkgName = appInfo.packageName;
        try {
            ApplicationInfoSmtBase smtEx = appInfo.getSmtEx();
            mPackageManger.getISmtEx().updateAppInfo(pkgName, jsonStr, appInfo.appLastTime,
                    smtEx.removeUnityChoreographerVsync, smtEx.permissionFlag, smtEx.isDexApp,
                    smtEx.downloadProfileFlag);
        } catch (RemoteException e) {
            Log.e(TAG, "updateVrVideoAppInfo failed: pkg: " + pkgName + " exception:" + e);
        }
    }

    public void deleteAppInfoListByPkgName(String pkgName) {
        mEnduranceAppInfoList.remove(pkgName);
        WhiteItem appInfoWhiteItem = mAppInfoList.get(pkgName);
        WhiteItem newAppInfoWhiteItem = null;
        boolean permissionFlag = false;
        if (appInfoWhiteItem != null && appInfoWhiteItem.SMValue.containsKey("permissionFlag")) {
            permissionFlag = Boolean.valueOf(
                    appInfoWhiteItem.SMValue.get("permissionFlag").toString());
            newAppInfoWhiteItem = new WhiteItem();
            newAppInfoWhiteItem.appInfoFlag |= ApplicationInfoSmtBase.APP_INFO_FLAG_PERMISSION_INFO;
            newAppInfoWhiteItem.SMValue.put("permissionFlag", permissionFlag);
            Log.d(TAG, " deleteAppInfo  " + pkgName + " but not remove permissionFlag");
        }
        if (mAppInfoList.remove(pkgName) != null) {
            if (newAppInfoWhiteItem != null && permissionFlag) {
                mAppInfoList.put(pkgName, newAppInfoWhiteItem);
            }
            saveDataToXmlFile(UPDATE_PATH + PEROPT_APP_INFO_FILE_NAME, SAVE_DATA_TYPE_APP_INFO);
            try {
                mPackageManger.getISmtEx().updateAppInfo(pkgName, "", 0, false, permissionFlag,
                        false, false);
            } catch (RemoteException e) {
                Slog.e(TAG, "deleteAppInfoListByPkgName updateAppInfo failed: pkg " + pkgName
                        + " exception: " + e);
            }
        } else {
            Log.e(TAG, "no such package: " + pkgName);
        }
    }

    /*
     * Applies the activity white list flags of {@code activities}; only {@link #SYSTEM_ACTIVITY}
     * is handled.
     */
    public static void updateFlagValueByType(ArrayList<PackageParser.Activity> activities,
            int type) {
        if (activities == null) {
            return;
        }

        if (SYSTEM_ACTIVITY == type) {
            for (int i = 0; i < activities.size(); i++) {
                PackageParser.Activity a = activities.get(i);
                if (a != null) {
                    WhiteItem item = activityWhiteList.get(a.getComponentName().getPackageName()
                            + "/" + a.getComponentName().getShortClassName());
                    if (item != null) {
                        a.info.getSmtEx().smXMLFlags |= item.SMFlag;
                    }
                }
            }
        }
    }

    private static void setFlag(ApplicationInfo appInfo, int flag, boolean value) {
        if (appInfo == null) {
            return;
        }
        if (value) {
            appInfo.getSmtEx().smartisanFlag |= flag;
        } else {
            appInfo.getSmtEx().smartisanFlag &= ~flag;
        }
    }

    private static void setFlag(ApplicationInfoSmtBase appInfoSmtEx, int flag, boolean value) {
        if (appInfoSmtEx == null) {
            return;
        }
        if (value) {
            appInfoSmtEx.smartisanFlag |= flag;
        } else {
            appInfoSmtEx.smartisanFlag &= ~flag;
        }
    }

    private static int saveToStorage(String path, byte[] data) {
        int result = -1;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            int count = -1;
            byte[] buf = new byte[4096];
            while ((count = bais.read(buf)) > 0) {
                fos.write(buf, 0, count);
            }
            fos.flush();
            bais.close();
            data = null;
            result = SAVE_FILE_SUCCESS;
            return result;
        } catch (Exception e) {
            result = SAVE_FILE_FAILD;
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private static int getAppType(String name) {
        if (name != null && sAppType.containsKey(name)) {
            return sAppType.get(name);
        }

        return 0;
    }

    private static void setAppType(ApplicationInfo info, int flag) {
        if (info != null && flag != 0) {
            if (DEBUG_APPTYPE) {
                Slog.i(TAG, "setAppType, packageName = " + info.packageName + ", flag is " + flag);
            }
            info.getSmtEx().appTypeFlag = flag;
            if (mPackageManger != null) {
                try {
                    mPackageManger.getISmtEx().updateAppTypeInfo(info.packageName, flag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setAppType(String packageName, int flag) {
        if (packageName != null && flag != 0) {
            if (DEBUG_APPTYPE) Slog.i(TAG, "packageName = " + packageName + ",flag = " + flag);
            if (mPackageManger != null) {
                try {
                    mPackageManger.getISmtEx().updateAppTypeInfo(packageName, flag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateAppTypeMayFromExpandService(ApplicationInfo info) {
        if (DEBUG_APPTYPE) Slog.i(TAG, "updateAppTypeMayFromExpandService");
        if (info == null) {
            if (DEBUG_APPTYPE) {
                Slog.i(TAG, "updateAppTypeMayFromExpandService application info is null");
            }
            return;
        }
        if (!sAppType.containsKey(info.packageName)) {
            Message message = Message.obtain();
            message.what = MSG_CHECK_APPTYPE;
            Bundle bundle = new Bundle();
            bundle.putParcelable(CHECK_APP_TYPE_KEY, info);
            message.setData(bundle);
            mHandler.sendMessage(message);
        } else {
            setAppType(info, getAppType(info.packageName));
        }
    }

    private int checkPackageTypeFromDB(String packageName) {
        if (DEBUG_APPTYPE) Slog.i(TAG, "checkPackageTypeFromDB packageName = " + packageName);
        try {
            Uri uri = Uri.parse("content://com.smartisanos.expandservice.launcher");
            ArrayList<String> packageLists = new ArrayList<>();
            packageLists.add(packageName);
            ContentResolver resolver = mContext.getContentResolver();
            Bundle extra = new Bundle();
            extra.putStringArrayList("apps", packageLists);
            Bundle result = resolver.call(uri, "query_app_category", null, extra);
            if (result == null) {
                Slog.w(TAG, "checkPackageTypeFromDB reslut is null");
                return 0;
            }
            Bundle data = result.getBundle(packageName);
            if (data == null) {
                Slog.w(TAG, "checkPackageTypeFromDB data is null");
                return 0;
            }
            String category = data.getString("id");
            String[] subCategoryIds = data.getStringArray("subId");
            int appType = changeQueryResultToApplicationInfoType(category, subCategoryIds);
            return appType;
        } catch (Exception e) {
            Slog.w(TAG, "exception in checkPackageTypeFromDB");
            e.printStackTrace();
        }
        return ApplicationInfoSmtBase.APP_TYPE_UNKNOWN;
    }

    private int changeQueryResultToApplicationInfoType(String category, String[] subCategoryIds) {
        int appType = 0;
        if (TextUtils.isEmpty(category)) {
            Slog.w(TAG, "category is null, set apptype unknown");
            return ApplicationInfoSmtBase.APP_TYPE_UNKNOWN;
        }
        int id = Integer.valueOf(category);
        if (sAppTypeId.get(id) == null) {
            Slog.w(TAG, "id :" + id + " is not in sAppTypeId, set apptype unknown");
            return ApplicationInfoSmtBase.APP_TYPE_UNKNOWN;
        }
        int appId = sAppTypeId.get(id);
        if (DEBUG_APPTYPE) Slog.i(TAG, "appId is = " + appTypeInt2String(appId));
        appType |= appId;
        if (subCategoryIds != null) {
            int N = subCategoryIds.length;
            for (int i = 0; i < N; i++) {
                int subId = Integer.valueOf(subCategoryIds[i]);
                if (sAppTypeId.get(subId) != null) {
                    int subAppId = sAppTypeId.get(subId);
                    if (DEBUG_APPTYPE) Slog.i(TAG, "subAppId is = " + appTypeInt2String(subAppId));
                    appType |= subAppId;
                }
            }
        }

        return appType;
    }

    public void registerPackageBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(mPackageReceiver, intentFilter);
    }

    private class PeroptHandler extends Handler {
        public PeroptHandler(Looper looper) {
            super(looper);
        }

        private String MsgInt2Str(int msgid) {
            switch (msgid) {
                case MSG_UPDATE_APPTYPE:
                    return "MSG_UPDATE_APPTYPE";
                case MSG_PACKAGE_REMOVED:
                    return "MSG_PACKAGE_REMOVED";
                case MSG_CHECK_APPTYPE:
                    return "MSG_CHECK_APPTYPE";
                default:
                    return "Unknown msgid";
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (DEBUG_APPTYPE) Slog.i(TAG, "handlemessage msg  = " + MsgInt2Str(msg.what));
            switch (msg.what) {
                case MSG_UPDATE_APPTYPE:
                    Bundle bundle = msg.getData();
                    if (bundle == null) {
                        return;
                    }
                    String packageName = bundle.getString(PACKAGENAME);
                    String appTypes = bundle.getString(APPTYPES);
                    int appType = splitAppTypeFromExpandService(appTypes);
                    if (appType != 0) {
                        sAppType.put(packageName, appType);
                        setAppType(packageName, appType);
                    }
                    break;
                case MSG_PACKAGE_REMOVED:
                    String removeName = msg.obj.toString();
                    sAppType.remove(removeName);
                    break;
                case MSG_CHECK_APPTYPE:
                    Bundle checkBundle = msg.getData();
                    ApplicationInfo info = checkBundle.getParcelable(CHECK_APP_TYPE_KEY);
                    if (info == null) {
                        if (DEBUG_APPTYPE) Slog.i(TAG, "application info is null when check apptype");
                        return;
                    }
                    if (!sAppType.containsKey(info.packageName)) {
                        if (isAppTypeSystem(info)) {
                            if (DEBUG_APPTYPE) {
                                Slog.i(TAG, "package : " + info.packageName + " is system app");
                            }
                            sAppType.put(info.packageName, ApplicationInfoSmtBase.APP_TYPE_SYSTEM);
                            setAppType(info, ApplicationInfoSmtBase.APP_TYPE_SYSTEM);
                        } else {
                            int appId = checkPackageTypeFromDB(info.packageName);
                            if (appId != 0) {
                                sAppType.put(info.packageName, appId);
                                setAppType(info, appId);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (DEBUG_APPTYPE) Slog.i(TAG, "broadcast action = " + action);
                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        Message message = Message.obtain();
                        message.what = MSG_PACKAGE_REMOVED;
                        Uri data = intent.getData();
                        String ssp;
                        if (data != null && (ssp = data.getSchemeSpecificPart()) != null) {
                            if (DEBUG_APPTYPE) Slog.i(TAG, "remove package name = " + ssp);
                            message.obj = ssp;
                            mHandler.sendMessage(message);
                        }
                    }
                }
            }
        }
    };

    private int splitAppTypeFromExpandService(String result) {
        if (result == null) {
            return 0;
        }

        try {
            JSONObject jsonObject = new JSONObject(result);
            String category = (String) jsonObject.get("id");
            String subCategoryIds = (String) jsonObject.get("sub_id");
            if (DEBUG_APPTYPE) {
                Slog.i(TAG, "splitAppTypeFromExpandService category =" + category
                        + ",subCategoryIds = " + subCategoryIds);
            }
            String[] subIds = null;
            if (!TextUtils.isEmpty(subCategoryIds)) {
                subIds = subCategoryIds.split(",");
            }
            return changeQueryResultToApplicationInfoType(category, subIds);
        } catch (Exception e) {
            Slog.w(TAG, "exception when split apptype from expandservice result");
            e.printStackTrace();
        }

        return ApplicationInfoSmtBase.APP_TYPE_UNKNOWN;
    }

    private static boolean isAppTypeSystem(ApplicationInfo info) {
        if (info != null) {
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        return false;
    }

    public void updateSmartisanFlagValue(ApplicationInfo info, PackageParser.Package pkg) {
        updatePeroptFlagValue(info, pkg);
        updateFlagValueByType(info, GAME);
        updateFlagValueByType(info, GMS);
        updateFlagValueByType(info, TNT_COMPATIBILITY_MODE);
        updateFlagValueByType(info, BOOST_NETWORK);
        updateFlagValueByType(info, MOVE_ACT_TO_DIFF_DISPLAY);
        info.getSmtEx().perfProfile = getPerfProfile(info);
    }

    public void updateSmartisanFlagValue(ApplicationInfoSmtBase infoSmtEx, String packageName) {
        updatePeroptFlagValue(infoSmtEx, packageName);
        updateFlagValueByType(infoSmtEx, GAME, packageName);
        updateFlagValueByType(infoSmtEx, GMS, packageName);
        updateFlagValueByType(infoSmtEx, TNT_COMPATIBILITY_MODE, packageName);
        updateFlagValueByType(infoSmtEx, BOOST_NETWORK, packageName);
        updateFlagValueByType(infoSmtEx, MOVE_ACT_TO_DIFF_DISPLAY, packageName);
        infoSmtEx.perfProfile = getPerfProfile(infoSmtEx, packageName);
    }

    public void updateAppTypesMapFromCallback(String packageName, String appTypes) {
        Message message = mHandler.obtainMessage(MSG_UPDATE_APPTYPE);
        Bundle bundle = new Bundle();
        bundle.putString(PACKAGENAME, packageName);
        bundle.putString(APPTYPES, appTypes);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    public int getUpdateListTypeByTaskName(String taskName) {
        int type = -1;
        if (CHAIN_BOOT_TASK_NAME.equals(taskName)) {
            type = CHAIN_BOOT;
        } else if (PEROPT_PACKAGE_TASK_NAME.equals(taskName)) {
            type = SYSTEM_PACKAGE;
        } else if (PEROPT_ACTIVITY_TASK_NAME.equals(taskName)) {
            type = SYSTEM_ACTIVITY;
        } else if (GAME_LIST_TASK_NAME.equals(taskName)) {
            type = GAME;
        } else if (GMS_LIST_TASK_NAME.equals(taskName)) {
            type = GMS;
        } else if (REVONE_LIST_TASK_NAME.equals(taskName)) {
            type = REV_ONE;
        } else if (TNT_COMPATIBILITY_APPS_TASK_NAME.equals(taskName)) {
            type = TNT_COMPATIBILITY_MODE;
        } else if (PEROPT_SMTOPS_POLICY.equals(taskName)) {
            type = SMTOPS_POLICY;
        } else if (BOOST_NETWORK_TASK_NAME.equals(taskName)) {
            type = BOOST_NETWORK;
        } else if (POWER_WAKELOCK_BLACK_LIST_TASK_NAME.equals(taskName)) {
            type = POWER_WAKELOCK_BLACK_LIST;
        }

        return type;
    }

    private static String appTypeInt2String(int apptype) {
        switch (apptype) {
            case ApplicationInfoSmtBase.APP_TYPE_INVAILD:
                return "APP_TYPE_INVAILD";
            case ApplicationInfoSmtBase.APP_TYPE_UNKNOWN:
                return "APP_TYPE_UNKNOWN";
            case ApplicationInfoSmtBase.APP_TYPE_SYSTEM:
                return "APP_TYPE_SYSTEM";
            case ApplicationInfoSmtBase.APP_TYPE_UTILITIES:
                return "APP_TYPE_UTILITIES";
            case ApplicationInfoSmtBase.APP_TYPE_BROWSER:
                return "APP_TYPE_BROWSER";
            case ApplicationInfoSmtBase.APP_TYPE_INPUTMETHOD:
                return "APP_TYPE_INPUTMETHOD";
            case ApplicationInfoSmtBase.APP_TYPE_OPTIMIZATION:
                return "APP_TYPE_OPTIMIZATION";
            case ApplicationInfoSmtBase.APP_TYPE_SAFETYMANAGEMENT:
                return "APP_TYPE_SAFETYMANAGEMENT";
            case ApplicationInfoSmtBase.APP_TYPE_THEMEWALLPAPER:
                return "APP_TYPE_THEMEWALLPAPER";
            case ApplicationInfoSmtBase.APP_TYPE_RINGTONES:
                return "APP_TYPE_RINGTONES";
            case ApplicationInfoSmtBase.APP_TYPE_TOOLS:
                return "APP_TYPE_TOOLS";
            case ApplicationInfoSmtBase.APP_TYPE_SHOPPING:
                return "APP_TYPE_SHOPPING";
            case ApplicationInfoSmtBase.APP_TYPE_ENTERTAINMENT:
                return "APP_TYPE_ENTERTAINMENT";
            case ApplicationInfoSmtBase.APP_TYPE_DOMESTICSERVICES:
                return "APP_TYPE_DOMESTICSERVICES";
            case ApplicationInfoSmtBase.APP_TYPE_CAR:
                return "APP_TYPE_CAR";
            case ApplicationInfoSmtBase.APP_TYPE_REALESTATEHOME:
                return "APP_TYPE_REALESTATEHOME";
            case ApplicationInfoSmtBase.APP_TYPE_HEALTHCARE:
                return "APP_TYPE_HEALTHCARE";
            case ApplicationInfoSmtBase.APP_TYPE_SPORT:
                return "APP_TYPE_SPORT";
            case ApplicationInfoSmtBase.APP_TYPE_TRANSPORTATION:
                return "APP_TYPE_TRANSPORTATION";
            case ApplicationInfoSmtBase.APP_TYPE_MAPNAVIGATION:
                return "APP_TYPE_MAPNAVIGATION";
            case ApplicationInfoSmtBase.APP_TYPE_BUSSUBWAY:
                return "APP_TYPE_BUSSUBWAY";
            case ApplicationInfoSmtBase.APP_TYPE_HOTELACCOMODATION:
                return "APP_TYPE_HOTELACCOMODATION";
            case ApplicationInfoSmtBase.APP_TYPE_TRAVEL:
                return "APP_TYPE_TRAVEL";
            case ApplicationInfoSmtBase.APP_TYPE_TICKETS:
                return "APP_TYPE_TICKETS";
            case ApplicationInfoSmtBase.APP_TYPE_USECAR:
                return "APP_TYPE_USECAR";
            case ApplicationInfoSmtBase.APP_TYPE_MEDIA:
                return "APP_TYPE_MEDIA";
            case ApplicationInfoSmtBase.APP_TYPE_TELEVISION:
                return "APP_TYPE_TELEVISION";
            case ApplicationInfoSmtBase.APP_TYPE_LIVE:
                return "APP_TYPE_LIVE";
            case ApplicationInfoSmtBase.APP_TYPE_MUSIC:
                return "APP_TYPE_MUSIC";
            case ApplicationInfoSmtBase.APP_TYPE_KMUSIC:
                return "APP_TYPE_KMUSIC";
            case ApplicationInfoSmtBase.APP_TYPE_SHORTVIDEO:
                return "APP_TYPE_SHORTVIDEO";
            case ApplicationInfoSmtBase.APP_TYPE_IMAGEPROCESSING:
                return "APP_TYPE_IMAGEPROCESSING";
            case ApplicationInfoSmtBase.APP_TYPE_CAMERA:
                return "APP_TYPE_CAMERA";
            case ApplicationInfoSmtBase.APP_TYPE_PRODUCTIVITY:
                return "APP_TYPE_PRODUCTIVITY";
            case ApplicationInfoSmtBase.APP_TYPE_OFFICESOFAWARE:
                return "APP_TYPE_OFFICESOFAWARE";
            case ApplicationInfoSmtBase.APP_TYPE_EFFICIENTOFFICE:
                return "APP_TYPE_EFFICIENTOFFICE";
            case ApplicationInfoSmtBase.APP_TYPE_CLOUDDISK:
                return "APP_TYPE_CLOUDDISK";
            case ApplicationInfoSmtBase.APP_TYPE_JOBSEARCH:
                return "APP_TYPE_JOBSEARCH";
            case ApplicationInfoSmtBase.APP_TYPE_EDUCATION:
                return "APP_TYPE_EDUCATION";
            case ApplicationInfoSmtBase.APP_TYPE_STUDY:
                return "APP_TYPE_STUDY";
            case ApplicationInfoSmtBase.APP_TYPE_FOREIGNLANGUAGE:
                return "APP_TYPE_FOREIGNLANGUAGE";
            case ApplicationInfoSmtBase.APP_TYPE_TRANSLATION:
                return "APP_TYPE_TRANSLATION";
            case ApplicationInfoSmtBase.APP_TYPE_EXAMINATION:
                return "APP_TYPE_EXAMINATION";
            case ApplicationInfoSmtBase.APP_TYPE_TRAINING:
                return "APP_TYPE_TRAINING";
            case ApplicationInfoSmtBase.APP_TYPE_NOTES:
                return "APP_TYPE_NOTES";
            case ApplicationInfoSmtBase.APP_TYPE_COMMUNICATION:
                return "APP_TYPE_COMMUNICATION";
            case ApplicationInfoSmtBase.APP_TYPE_CHATFRIENDS:
                return "APP_TYPE_CHATFRIENDS";
            case ApplicationInfoSmtBase.APP_TYPE_PHONESMS:
                return "APP_TYPE_PHONESMS";
            case ApplicationInfoSmtBase.APP_TYPE_HOBBY:
                return "APP_TYPE_HOBBY";
            case ApplicationInfoSmtBase.APP_TYPE_MARRIAGE:
                return "APP_TYPE_MARRIAGE";
            case ApplicationInfoSmtBase.APP_TYPE_COMMUNITY:
                return "APP_TYPE_COMMUNITY";
            case ApplicationInfoSmtBase.APP_TYPE_PARENTING:
                return "APP_TYPE_PARENTING";
            case ApplicationInfoSmtBase.APP_TYPE_STORYSONGS:
                return "APP_TYPE_STORYSONGS";
            case ApplicationInfoSmtBase.APP_TYPE_EARLYCHILDHOOSEDUCATION:
                return "APP_TYPE_EARLYCHILDHOOSEDUCATION";
            case ApplicationInfoSmtBase.APP_TYPE_CHILDRENENCYCLOPEDIA:
                return "APP_TYPE_CHILDRENENCYCLOPEDIA";
            case ApplicationInfoSmtBase.APP_TYPE_MOTHERCOMMUNITY:
                return "APP_TYPE_MOTHERCOMMUNITY";
            case ApplicationInfoSmtBase.APP_TYPE_FEMALEHEALTH:
                return "APP_TYPE_FEMALEHEALTH";
            case ApplicationInfoSmtBase.APP_TYPE_FINANCE:
                return "APP_TYPE_FINANCE";
            case ApplicationInfoSmtBase.APP_TYPE_PAY:
                return "APP_TYPE_PAY";
            case ApplicationInfoSmtBase.APP_TYPE_BANKLENDING:
                return "APP_TYPE_BANKLENDING";
            case ApplicationInfoSmtBase.APP_TYPE_STOCKLOTTERY:
                return "APP_TYPE_STOCKLOTTERY";
            case ApplicationInfoSmtBase.APP_TYPE_INVESTMENTFINANCE:
                return "APP_TYPE_INVESTMENTFINANCE";
            case ApplicationInfoSmtBase.APP_TYPE_BOOKKEEPING:
                return "APP_TYPE_BOOKKEEPING";
            case ApplicationInfoSmtBase.APP_TYPE_READINGS:
                return "APP_TYPE_READINGS";
            case ApplicationInfoSmtBase.APP_TYPE_FICTION:
                return "APP_TYPE_FICTION";
            case ApplicationInfoSmtBase.APP_TYPE_COMICS:
                return "APP_TYPE_COMICS";
            case ApplicationInfoSmtBase.APP_TYPE_EBOOK:
                return "APP_TYPE_EBOOK";
            case ApplicationInfoSmtBase.APP_TYPE_NEWS:
                return "APP_TYPE_NEWS";
            case ApplicationInfoSmtBase.APP_TYPE_SOFTWARE:
                return "APP_TYPE_SOFTWARE";
            case ApplicationInfoSmtBase.APP_TYPE_GAME:
                return "APP_TYPE_GAME";
            default:
                return "UnKnown apptype";
        }
    }

    /*
     * Replaces the cached list of the given smartisan flag type and its storage file.
     */
    public static boolean updateStorageAndCache(int type, HashSet<String> list) {
        String fileName = null;
        switch (type) {
            case ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP:
                fileName = BOOM_DISABLE_LIST_FILE_NAME;
                break;
        }
        byte[] data = null;
        JSONArray array = new JSONArray();
        try {
            if (list != null) {
                for (String s : list) {
                    array.put(s);
                }
            }
        } catch (Exception e) {
            array = null;
            e.printStackTrace();
        }
        if (array != null) {
            data = array.toString().getBytes();
        }
        if (fileName == null || data == null) {
            return false;
        }
        if (saveToStorage(UPDATE_PATH + fileName, data) == SAVE_FILE_SUCCESS) {
            switch (type) {
                case ApplicationInfoSmtBase.SMARTISAN_FLAG_DISABLE_BOOM_APP:
                    synchronized (sBoomDisableList) {
                        sBoomDisableList.clear();
                        if (list != null) {
                            sBoomDisableList.addAll(list);
                        }
                    }
                    break;
            }
        } else {
            return false;
        }
        return true;
    }

    public static boolean inPowerWakelockBlackList(String packageName, String tag) {
        if (TextUtils.isEmpty(tag) || TextUtils.isEmpty(packageName)) {
            return false;
        }
        Vector vector = sPowerWakelockBlackList.get(packageName);
        if (vector != null && vector.indexOf(tag) > -1) {
            return true;
        }

        return false;
    }

    /*
     * Installs a package white list staged in /data/syslog/ when it is newer than the current
     * one.
     */
    public boolean getUpdatePackageList() {
        File tempFile = new File(UPDATE_TEMP_PATH + PEROPT_PACKAGE_FILE_NAME);
        if (copyFile(tempFile, UPDATE_PATH)) {
            initWhiteList(PEROPT_PACKAGE_FILE_NAME);
            return true;
        }
        return false;
    }

    private boolean checkTempFile(File tempFile, File newFile) {
        int updateVersion = -1;
        int defaultVersion = -1;
        int tempVersion = -1;
        InputStream tempIs = null;
        if (tempFile.exists()) {
            try {
                tempIs = new FileInputStream(tempFile);
                tempVersion = parserVersion(tempIs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                if (tempIs != null) {
                    tempIs.close();
                    tempIs = null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            return false;
        }

        InputStream updateIs = null;
        if (newFile.exists()) {
            try {
                updateIs = new FileInputStream(newFile);
                updateVersion = parserVersion(updateIs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                if (updateIs != null) {
                    updateIs.close();
                    updateIs = null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Log.i(TAG, "updateVersion = " + updateVersion + " ; tempVersion = " + tempVersion);

        if (tempVersion > updateVersion) {
            return true;
        }
        return false;
    }

    public boolean copyFile(File tempFile, String updatePath) {
        boolean result = false;
        if (tempFile == null || updatePath == null) {
            return result;
        }

        File updateFile = new File(updatePath + PEROPT_PACKAGE_FILE_NAME);
        if (updateFile.exists()) {
            if (!checkTempFile(tempFile, updateFile)) {
                return result;
            }
            updateFile.delete();
        }
        try {
            updateFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            srcChannel = new FileInputStream(tempFile).getChannel();
            dstChannel = new FileOutputStream(updateFile).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
        try {
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
