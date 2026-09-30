// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.pm;

import android.os.Parcel;
import android.os.SystemProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smartisan/PICO extension state attached to every {@link ApplicationInfo} (reachable
 * through {@code ApplicationInfo.getSmtEx()}). Reconstructed from the PICO OS 5.13.7
 * factory framework: it carries performance, prefetch, VR and permission flags that are
 * parcelled together with the owning {@link ApplicationInfo}.
 *
 * @hide
 */
public class ApplicationInfoSmtBase {
    /** Application type flags ({@link #appTypeFlag}); high bits select a category group. */
    public static final int APP_TYPE_BANKLENDING = 0x00020400;
    public static final int APP_TYPE_BOOKKEEPING = 0x00100400;
    public static final int APP_TYPE_BROWSER = 0x00010004;
    public static final int APP_TYPE_BUSSUBWAY = 0x00020010;
    public static final int APP_TYPE_CAMERA = 0x00400020;
    public static final int APP_TYPE_CAR = 0x00080008;
    public static final int APP_TYPE_CHATFRIENDS = 0x00010100;
    public static final int APP_TYPE_CHILDRENENCYCLOPEDIA = 0x00040200;
    public static final int APP_TYPE_CLOUDDISK = 0x00040040;
    public static final int APP_TYPE_COMICS = 0x00020800;
    public static final int APP_TYPE_COMMUNICATION = 0x00000100;
    public static final int APP_TYPE_COMMUNITY = 0x00100100;
    public static final int APP_TYPE_DOMESTICSERVICES = 0x00040008;
    public static final int APP_TYPE_EARLYCHILDHOOSEDUCATION = 0x00020200;
    public static final int APP_TYPE_EBOOK = 0x00040800;
    public static final int APP_TYPE_EDUCATION = 0x00000080;
    public static final int APP_TYPE_EFFICIENTOFFICE = 0x00020040;
    public static final int APP_TYPE_ENTERTAINMENT = 0x00020008;
    public static final int APP_TYPE_EXAMINATION = 0x00080080;
    public static final int APP_TYPE_FEMALEHEALTH = 0x00100200;
    public static final int APP_TYPE_FICTION = 0x00010800;
    public static final int APP_TYPE_FINANCE = 0x00000400;
    public static final int APP_TYPE_FOREIGNLANGUAGE = 0x00020080;
    public static final int APP_TYPE_GAME = 0x00002000;
    public static final int APP_TYPE_HEALTHCARE = 0x00200008;
    public static final int APP_TYPE_HOBBY = 0x00040100;
    public static final int APP_TYPE_HOTELACCOMODATION = 0x00040010;
    public static final int APP_TYPE_IMAGEPROCESSING = 0x00200020;
    public static final int APP_TYPE_INPUTMETHOD = 0x00020004;
    public static final int APP_TYPE_INVAILD = 0x00000000;
    public static final int APP_TYPE_INVESTMENTFINANCE = 0x00080400;
    public static final int APP_TYPE_JOBSEARCH = 0x00080040;
    public static final int APP_TYPE_KMUSIC = 0x00080020;
    public static final int APP_TYPE_LIVE = 0x00020020;
    public static final int APP_TYPE_MAPNAVIGATION = 0x00010010;
    public static final int APP_TYPE_MARRIAGE = 0x00080100;
    public static final int APP_TYPE_MEDIA = 0x00000020;
    public static final int APP_TYPE_MOTHERCOMMUNITY = 0x00080200;
    public static final int APP_TYPE_MUSIC = 0x00040020;
    public static final int APP_TYPE_NEWS = 0x00080800;
    public static final int APP_TYPE_NOTES = 0x00200080;
    public static final int APP_TYPE_OFFICESOFAWARE = 0x00010040;
    public static final int APP_TYPE_OPTIMIZATION = 0x00040004;
    public static final int APP_TYPE_PARENTING = 0x00000200;
    public static final int APP_TYPE_PAY = 0x00010400;
    public static final int APP_TYPE_PHONESMS = 0x00020100;
    public static final int APP_TYPE_PRODUCTIVITY = 0x00000040;
    public static final int APP_TYPE_READINGS = 0x00000800;
    public static final int APP_TYPE_REALESTATEHOME = 0x00100008;
    public static final int APP_TYPE_RINGTONES = 0x00200004;
    public static final int APP_TYPE_SAFETYMANAGEMENT = 0x00080004;
    public static final int APP_TYPE_SHOPPING = 0x00010008;
    public static final int APP_TYPE_SHORTVIDEO = 0x00100020;
    public static final int APP_TYPE_SOFTWARE = 0x00001000;
    public static final int APP_TYPE_SPORT = 0x00400008;
    public static final int APP_TYPE_STOCKLOTTERY = 0x00040400;
    public static final int APP_TYPE_STORYSONGS = 0x00010200;
    public static final int APP_TYPE_STUDY = 0x00010080;
    public static final int APP_TYPE_SYSTEM = 0x00000002;
    public static final int APP_TYPE_TELEVISION = 0x00010020;
    public static final int APP_TYPE_THEMEWALLPAPER = 0x00100004;
    public static final int APP_TYPE_TICKETS = 0x00100010;
    public static final int APP_TYPE_TOOLS = 0x00000008;
    public static final int APP_TYPE_TRAINING = 0x00100080;
    public static final int APP_TYPE_TRANSLATION = 0x00040080;
    public static final int APP_TYPE_TRANSPORTATION = 0x00000010;
    public static final int APP_TYPE_TRAVEL = 0x00080010;
    public static final int APP_TYPE_UNKNOWN = 0x00000001;
    public static final int APP_TYPE_USECAR = 0x00200010;
    public static final int APP_TYPE_UTILITIES = 0x00000004;

    /** Performance optimisation flags ({@link #peroptFlag}). */
    public static final int PEROPT_FLAG_BG_CONTROL = 0x00000001;
    public static final int PEROPT_FLAG_BLAMED_TOP_APP = 0x00000080;
    public static final int PEROPT_FLAG_CATEGORY_MEM_RESIDENT = 0x00000020;
    public static final int PEROPT_FLAG_CLEAN_APP_AT_SCREEN_OFF = 0x00000100;
    public static final int PEROPT_FLAG_COLLECT_ANR_INFO_TTVR = 0x00000200;
    public static final int PEROPT_FLAG_COMPACT_UFS = 0x00400000;
    public static final int PEROPT_FLAG_COMPACT_ZRAM = 0x00800000;
    public static final int PEROPT_FLAG_FORCESTOP_RESTART = 0x00040000;
    public static final int PEROPT_FLAG_FORCESTOP_WHITE_LIST_FOR_3DCONTROL = 0x00000400;
    public static final int PEROPT_FLAG_FORCE_2DAPP_FOR_3DCONTROL = 0x00000002;
    public static final int PEROPT_FLAG_GAME_BALANCE = 0x00004000;
    public static final int PEROPT_FLAG_KILL_AT_DOZE = 0x00000040;
    public static final int PEROPT_FLAG_LAUNCH_BOOST = 0x00000004;
    public static final int PEROPT_FLAG_NOT_ALLOW_START_ACTIVITY_IN_BACKGROUND = 0x00080000;
    public static final int PEROPT_FLAG_OOM_ADJUST = 0x00001000;
    public static final int PEROPT_FLAG_PERFORMANCE_TOOL = 0x00020000;
    public static final int PEROPT_FLAG_PREFETCH_WHITE_LIST_APP = 0x00000800;
    public static final int PEROPT_FLAG_PRIMARY_PROF = 0x08000000;
    public static final int PEROPT_FLAG_RECALCULATE_LAYER_TYPE = 0x00002000;
    public static final int PEROPT_FLAG_STARTING_WINDOW = 0x00000008;
    public static final int PEROPT_FLAG_SUPER_APP = 0x40000000;
    public static final int PEROPT_FLAG_VRSHELL_AGAINST_BROWSER = 0x00200000;
    public static final int PEROPT_FLAG_VR_SHELL_CPUSET = 0x00000010;
    public static final int PEROPT_FLAG_XR_KEEP_ALIVE = 0x00100000;
    public static final int PEROPT_PACKAGE_HIGH_PRIORITY_FOR_PC = 0x04000000;
    public static final int PEROPT_PICO_FLAG_DISABLE_CAPTURE_KEY = 0x00008000;
    public static final int PEROPT_PICO_FLAG_KEEP_ALIVE_SWAP_MEM = 0x00010000;

    /** Smartisan flags ({@link #smartisanFlag}). */
    public static final int SMARTISAN_FLAG_ACT_CAN_MOVED_TO_DIFF_DISPLAY = 0x00004000;
    public static final int SMARTISAN_FLAG_APP_DEBUG = 0x00000080;
    public static final int SMARTISAN_FLAG_BG_CONTROL_UNSET = 0x00000001;
    public static final int SMARTISAN_FLAG_BOOST_NETWORK = 0x00001000;
    public static final int SMARTISAN_FLAG_CATEGORY_APP_COLD_AND_WARM_LAUNCH = 0x00000020;
    public static final int SMARTISAN_FLAG_CATEGORY_APP_COLD_LAUNCH = 0x00000010;
    public static final int SMARTISAN_FLAG_CATEGORY_GAME = 0x00000004;
    public static final int SMARTISAN_FLAG_CATEGORY_GMS = 0x00000100;
    public static final int SMARTISAN_FLAG_CATEGORY_STARTING_WINDOW = 0x00000040;
    public static final int SMARTISAN_FLAG_DISABLE_BOOM_APP = 0x00002000;
    public static final int SMARTISAN_FLAG_FROM_PC = 0x00000400;
    public static final int SMARTISAN_FLAG_OVERRIDE_RESOLUTION = 0x00000200;
    public static final int SMARTISAN_FLAG_PLATFORM_SIGNATURE = 0x00000002;
    public static final int SMARTISAN_FLAG_SMT_SYSTEM_APP_CAN_UNINSTALL = 0x00000008;
    public static final int SMARTISAN_FLAG_TNT_COMPATIBILITY_MODE = 0x00000800;

    /** Bits of the per-application information requests. */
    public static final int APP_INFO_FLAG_APP_FPS_INFO = 0x00000004;
    public static final int APP_INFO_FLAG_APP_REMOVE_UNITY_CHOREOGRAPHER_VSYNC_INFO = 0x00000008;
    public static final int APP_INFO_FLAG_DEXOAT_APP = 0x00000001;
    public static final int APP_INFO_FLAG_DOWNLOAD_PROFILE = 0x00000002;
    public static final int APP_INFO_FLAG_MEM_BASE_INFO = 0x00000002;
    public static final int APP_INFO_FLAG_PERMISSION_INFO = 0x00000010;
    public static final int APP_INFO_FLAG_PROC_MEM_INFO = 0x00000001;

    public static final int APP_COMPOSITION_INFO_SKIP_SINGLE_LAYER = 1;

    /** Values of {@link #mStrictModeFlags}. */
    public static final int STRICT_MODE_FLAG_APP = 1;
    public static final int STRICT_MODE_FLAG_SYSTEM_APP = 2;

    /** Values of {@link #isLimited}. */
    public static final int UNLIMIT = 0;
    public static final int LIMIT = 1;

    /** Values of {@link #isAllowPrefetch}. */
    public static final int INITIAL_VALUE = -1;
    public static final int NOT_ALLOW_PREFETCH = 0;
    public static final int ALLOW_PREFETCH = 1;

    /** Values of {@link #beKilledType} and check types of {@link #goodToOperateProc}. */
    public static final int TYPE_NONE = 0;
    public static final int TYPE_FORCE_CLEANED = 1;
    public static final int TYPE_BG_HIGH_CPU = 2;
    public static final int TYPE_CHECK_PUSH_TO_BG = 4;
    public static final int TYPE_CHECK_BAD_BG = 8;
    public static final int TYPE_CHECK_HIGH_PRI_BG = 16;

    /** Values of {@link #shellAppType}. */
    public static final int TYPE_APP_INSHELL = 1;
    public static final int TYPE_SHELL_APP = 2;
    public static final int TYPE_VRFORGROUND = 3;

    /** Bits of {@link #vrAppEngine}. */
    public static final int VR_APP_ENGINE_UNITY = 1;
    public static final int VR_APP_ENGINE_UE4 = 2;

    /** Special values of {@link #vrOomAdj}. */
    public static final int PICO_SYSTEM_ADJ_WITH_TOP = -9001;
    public static final int PICO_UNKNOW_ADJ = 9999;

    public static final String PACKAGE_NAME_RUNTIME = "com.pico.xr.openxr_runtime";

    public int appTypeFlag = APP_TYPE_INVAILD;
    public int peroptFlag = PEROPT_FLAG_BG_CONTROL;
    public List<String> prefetchPackVersions = new ArrayList<>();
    public int uidGroupID = -1;
    public int perfToolType = 0;
    public Map<String, Long> smtProcessMemInfo = new HashMap<>(0);
    public int beKilledType = TYPE_NONE;
    public long beKilledTime;
    public int killedTimes;
    public long perceptibleTime;
    public long mBeKilledByForceStopTime;
    public int smartisanFlag = SMARTISAN_FLAG_BG_CONTROL_UNSET | SMARTISAN_FLAG_APP_DEBUG;
    public int forceDisplayFlags = 0;
    public int autoDisplayFlags = 8;
    public int[] smartisanFlingBoostValues;
    public int smartisanLaunchBoostValue;
    public boolean compositeService;
    public int smartisanBgControlAppAdjLevel;
    public String smtStartingWindowType;
    public boolean isFromSystemUI;
    public boolean smartisanCollectProf;
    public int perfProfile = 0;
    public int isLimited = UNLIMIT;
    public boolean isPrefetch;
    public boolean isLastPrefetch;
    public boolean keepAliveForVr;
    public int doPrefetch = 0;
    public int funcTracking = 0;
    public int mStrictModeFlags = 0;
    public boolean isVrApp = false;
    public int vrAppEngine = 0;
    public int vrAppSdkVersionCode = 0;

    public boolean isUnityEngine() {
        return (vrAppEngine & VR_APP_ENGINE_UNITY) == VR_APP_ENGINE_UNITY;
    }

    public boolean isUe4Engine() {
        return (vrAppEngine & VR_APP_ENGINE_UE4) == VR_APP_ENGINE_UE4;
    }

    public boolean isVrShell = false;

    public boolean isDexApp = false;

    public boolean downloadProfileFlag = false;

    public static boolean dex2oatSwitch =
            SystemProperties.getBoolean("persist.sys.dex2oat.switch", true);

    public int mBinderStatFlags = 0;

    public int mOverrideClassSDK = 0;

    public boolean setDefaultProcessGroupFlag = true;

    public boolean removeUnityChoreographerVsync = false;

    public boolean permissionFlag = false;

    /** Owning application info; not parcelled. */
    public transient ApplicationInfo mInfo;

    public ApplicationInfoSmtBase(ApplicationInfo info) {
        mInfo = info;
    }

    public int shellAppType = 0;

    public int vrOomAdj = PICO_UNKNOW_ADJ;

    /** Copies the parcelled extension state (everything except {@link #mInfo}). */
    public void copyFrom(ApplicationInfoSmtBase ApplicationInfoSmtBase) {
        appTypeFlag = ApplicationInfoSmtBase.appTypeFlag;
        perfProfile = ApplicationInfoSmtBase.perfProfile;
        peroptFlag = ApplicationInfoSmtBase.peroptFlag;
        smartisanFlingBoostValues = ApplicationInfoSmtBase.smartisanFlingBoostValues;
        smartisanLaunchBoostValue = ApplicationInfoSmtBase.smartisanLaunchBoostValue;
        compositeService = ApplicationInfoSmtBase.compositeService;
        smartisanBgControlAppAdjLevel = ApplicationInfoSmtBase.smartisanBgControlAppAdjLevel;
        smtStartingWindowType = ApplicationInfoSmtBase.smtStartingWindowType;
        isFromSystemUI = ApplicationInfoSmtBase.isFromSystemUI;
        isDexFileOptOn = ApplicationInfoSmtBase.isDexFileOptOn;
        smartisanCollectProf = ApplicationInfoSmtBase.smartisanCollectProf;
        smtProcessMemInfo = ApplicationInfoSmtBase.smtProcessMemInfo;
        mBeKilledByForceStopTime = ApplicationInfoSmtBase.mBeKilledByForceStopTime;
        isLimited = ApplicationInfoSmtBase.isLimited;
        isPrefetch = ApplicationInfoSmtBase.isPrefetch;
        keepAliveForVr = ApplicationInfoSmtBase.keepAliveForVr;
        beKilledTime = ApplicationInfoSmtBase.beKilledTime;
        perceptibleTime = ApplicationInfoSmtBase.perceptibleTime;
        smartisanFlag = ApplicationInfoSmtBase.smartisanFlag;
        beKilledType = ApplicationInfoSmtBase.beKilledType;
        forceDisplayFlags = ApplicationInfoSmtBase.forceDisplayFlags;
        autoDisplayFlags = ApplicationInfoSmtBase.autoDisplayFlags;
        doPrefetch = ApplicationInfoSmtBase.doPrefetch;
        mStrictModeFlags = ApplicationInfoSmtBase.mStrictModeFlags;
        mTransitionStartTimeNs = ApplicationInfoSmtBase.mTransitionStartTimeNs;
        isVrApp = ApplicationInfoSmtBase.isVrApp;
        mBinderStatFlags = ApplicationInfoSmtBase.mBinderStatFlags;
        mOverrideClassSDK = ApplicationInfoSmtBase.mOverrideClassSDK;
        funcTracking = ApplicationInfoSmtBase.funcTracking;
        vrAppEngine = ApplicationInfoSmtBase.vrAppEngine;
        isAllowPrefetch = ApplicationInfoSmtBase.isAllowPrefetch;
        prefetchPackVersions = ApplicationInfoSmtBase.prefetchPackVersions;
        shellAppType = ApplicationInfoSmtBase.shellAppType;
        vrOomAdj = ApplicationInfoSmtBase.vrOomAdj;
        permissionFlag = ApplicationInfoSmtBase.permissionFlag;
        vrAppSdkVersionCode = ApplicationInfoSmtBase.vrAppSdkVersionCode;
        isDexApp = ApplicationInfoSmtBase.isDexApp;
    }

    /** Reads the extension state in the order written by {@link #writeToParcel}. */
    public void readFromParcel(Parcel parcel) {
        appTypeFlag = parcel.readInt();
        perfProfile = parcel.readInt();
        peroptFlag = parcel.readInt();
        int len = parcel.readInt();
        if (len > 0) {
            smartisanFlingBoostValues = new int[len];
            parcel.readIntArray(smartisanFlingBoostValues);
        }
        smartisanLaunchBoostValue = parcel.readInt();
        compositeService = parcel.readBoolean();
        smartisanBgControlAppAdjLevel = parcel.readInt();
        smtStartingWindowType = parcel.readString();
        isFromSystemUI = parcel.readBoolean();
        isDexFileOptOn = parcel.readInt();
        smartisanCollectProf = parcel.readBoolean();
        parcel.readMap(smtProcessMemInfo, null);
        mBeKilledByForceStopTime = parcel.readLong();
        isLimited = parcel.readInt();
        isPrefetch = parcel.readBoolean();
        keepAliveForVr = parcel.readBoolean();
        beKilledTime = parcel.readLong();
        perceptibleTime = parcel.readLong();
        smartisanFlag = parcel.readInt();
        beKilledType = parcel.readInt();
        forceDisplayFlags = parcel.readInt();
        autoDisplayFlags = parcel.readInt();
        doPrefetch = parcel.readInt();
        mStrictModeFlags = parcel.readInt();
        mTransitionStartTimeNs = parcel.readLong();
        isVrApp = parcel.readBoolean();
        mBinderStatFlags = parcel.readInt();
        mOverrideClassSDK = parcel.readInt();
        funcTracking = parcel.readInt();
        vrAppEngine = parcel.readInt();
        isAllowPrefetch = parcel.readInt();
        parcel.readStringList(prefetchPackVersions);
        shellAppType = parcel.readInt();
        vrOomAdj = parcel.readInt();
        permissionFlag = parcel.readBoolean();
        vrAppSdkVersionCode = parcel.readInt();
        isDexApp = parcel.readBoolean();
    }

    /** Writes the extension state; called from {@code ApplicationInfo.writeToParcel}. */
    public void writeToParcel(Parcel parcel) {
        parcel.writeInt(appTypeFlag);
        parcel.writeInt(perfProfile);
        parcel.writeInt(peroptFlag);
        int len = 0;
        if (smartisanFlingBoostValues != null) {
            len = smartisanFlingBoostValues.length;
        }
        parcel.writeInt(len);
        if (len > 0) {
            parcel.writeIntArray(smartisanFlingBoostValues);
        }
        parcel.writeInt(smartisanLaunchBoostValue);
        parcel.writeBoolean(compositeService);
        parcel.writeInt(smartisanBgControlAppAdjLevel);
        parcel.writeString(smtStartingWindowType);
        parcel.writeBoolean(isFromSystemUI);
        parcel.writeInt(isDexFileOptOn);
        parcel.writeBoolean(smartisanCollectProf);
        parcel.writeMap(smtProcessMemInfo);
        parcel.writeLong(mBeKilledByForceStopTime);
        parcel.writeInt(isLimited);
        parcel.writeBoolean(isPrefetch);
        parcel.writeBoolean(keepAliveForVr);
        parcel.writeLong(beKilledTime);
        parcel.writeLong(perceptibleTime);
        parcel.writeInt(smartisanFlag);
        parcel.writeInt(beKilledType);
        parcel.writeInt(forceDisplayFlags);
        parcel.writeInt(autoDisplayFlags);
        parcel.writeInt(doPrefetch);
        parcel.writeInt(mStrictModeFlags);
        parcel.writeLong(mTransitionStartTimeNs);
        parcel.writeBoolean(isVrApp);
        parcel.writeInt(mBinderStatFlags);
        parcel.writeInt(mOverrideClassSDK);
        parcel.writeInt(funcTracking);
        parcel.writeInt(vrAppEngine);
        parcel.writeInt(isAllowPrefetch);
        parcel.writeStringList(prefetchPackVersions);
        parcel.writeInt(shellAppType);
        parcel.writeInt(vrOomAdj);
        parcel.writeBoolean(permissionFlag);
        parcel.writeInt(vrAppSdkVersionCode);
        parcel.writeBoolean(isDexApp);
    }

    public int isDexFileOptOn = 0;

    /** Whether a process killed with {@link #beKilledType} may be started in background again. */
    public boolean goodToStartInBG(long now) {
        if (beKilledType == TYPE_NONE) {
            return true;
        }
        if (TYPE_FORCE_CLEANED == beKilledType) {
            return false;
        }
        if (TYPE_BG_HIGH_CPU == beKilledType) {
            return now - beKilledTime > killedTimes * 10 * 60 * 1000;
        }
        return true;
    }

    /** Whether enough time passed since {@link #perceptibleTime} for the given check type. */
    public boolean goodToOperateProc(long now, int type) {
        if (perceptibleTime == 0) {
            return true;
        }
        if (type == TYPE_CHECK_PUSH_TO_BG && perceptibleTime + 3 * 60 * 1000 < now) {
            return true;
        }
        if (type == TYPE_CHECK_BAD_BG && perceptibleTime + 6 * 60 * 1000 < now) {
            return true;
        }
        if (type == TYPE_CHECK_HIGH_PRI_BG && perceptibleTime + 10 * 60 * 1000 < now) {
            return true;
        }
        return false;
    }

    public static boolean isPlutoNamespace(String namespace) {
        return false;
    }

    public boolean isGameAppSmt() {
        return (smartisanFlag & SMARTISAN_FLAG_CATEGORY_GAME) != 0
                || (appTypeFlag & APP_TYPE_GAME) != 0;
    }

    public boolean isPrefetchApp() {
        return doPrefetch != 0 && doPrefetch < 3;
    }

    public long mTransitionStartTimeNs = 0;

    public int isAllowPrefetch = INITIAL_VALUE;
}
