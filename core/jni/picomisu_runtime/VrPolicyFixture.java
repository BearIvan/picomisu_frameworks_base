// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package org.picomisu.runtime;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IExtActivityInfo;
import android.content.pm.IExtApplicationInfo;
import android.content.pm.ExtPackageParserImpl;
import android.content.pm.ExtPackageParserUtils;
import android.content.pm.IExtPackageParser;
import android.content.pm.PackageParser;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.pico.utils.Features;
import android.pico.utils.PicoSystemConfig;
import android.util.ArrayMap;
import android.view.Display;
import android.view.ExtViewRootImplImpl;
import android.view.IExtViewRootImpl;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Deterministic VR policy scenarios for both the factory and the Source framework.
 * Prints "vr-policy key=value" lines; hidden members are reached by reflection only where
 * the field or constructor is not accessible. Nothing is installed or sent to services.
 */
public final class VrPolicyFixture {
    private static final String PREFIX = "vr-policy ";
    private static PrintStream sOut;
    private static int sLines;
    static String sSetting;

    private VrPolicyFixture() {}

    public static void main(String[] args) throws Exception {
        System.out.println("vr-policy-fixture lines=" + run(System.out));
    }

    /** Runs every scenario and returns the number of result lines. */
    public static int run(PrintStream out) throws Exception {
        sOut = out;
        sLines = 0;
        activityInfo();
        applicationInfo();
        packageParser();
        packageParserBase();
        virtualDisplayConfig();
        activityThreadAndViewRoot();
        return sLines;
    }

    private static void emit(String key, Object value) {
        sOut.println(PREFIX + key + "=" + value);
        sLines++;
    }

    private static String describe(IExtActivityInfo ext) {
        return ext.getVrActivityFlag() + "/" + ext.isVrActivity() + "/" + ext.isVrActivityForceRender()
                + "/" + ext.get2dAppPosition() + "/" + ext.get2dAppTheme() + "/" + ext.getAppProperties();
    }

    private static String describe(IExtApplicationInfo ext) {
        return ext.getVrAppFlag() + "/" + ext.isVrApp() + "/" + ext.isAllComponentVr() + "/"
                + ext.isVrApplication() + "/" + ext.getLaunchActivityOrientation() + "/"
                + ext.get2dAppDensity() + "/" + ext.get2dAppPortraitWidth() + "/"
                + ext.get2dAppPortraitHeight() + "/" + ext.get2dAppLandscapeWidth() + "/"
                + ext.get2dAppLandscapeHeight() + "/" + ext.get2dAppForceOrientation() + "/"
                + ext.get2dAppDefaultOrientation() + "/" + ext.getDisplayId() + "/"
                + ext.get2dAppOrientation();
    }

    private static String hex(Parcel parcel) {
        byte[] bytes = parcel.marshall();
        StringBuilder text = new StringBuilder();
        for (byte b : bytes) {
            text.append(String.format("%02x", b & 0xff));
        }
        return text.toString();
    }

    private static Bundle bundle(String... pairs) {
        Bundle bundle = new Bundle();
        for (int i = 0; i < pairs.length; i += 2) {
            bundle.putString(pairs[i], pairs[i + 1]);
        }
        return bundle;
    }

    private static void activityInfo() {
        emit("activity.default", describe(new ActivityInfo().getExt()));
        for (int flag = 0; flag < 4; ++flag) {
            ActivityInfo info = new ActivityInfo();
            info.getExt().setVrActivity(flag);
            emit("activity.flag." + flag, describe(info.getExt()));
        }
        ActivityInfo forced = new ActivityInfo();
        forced.getExt().setVrActivityForceRenderFlag(0);
        emit("activity.force-arg-ignored", describe(forced.getExt()));
        forced.getExt().setVrActivity(1);
        emit("activity.force-then-vr", describe(forced.getExt()));

        ActivityInfo feature = new ActivityInfo();
        feature.getExt().updateAppFeature(null);
        emit("activity.feature.null", describe(feature.getExt()));
        feature.getExt().updateAppFeature(bundle("pico.vr.position", "  NEAR ", "pico.vr.app.prop",
                "a=1", "pico.vr.theme", " NoCaptionBar | noNavigationBar||x "));
        emit("activity.feature.full", describe(feature.getExt()));
        feature.getExt().updateAppFeature(bundle("pico.vr.position", "", "pico.vr.theme", ""));
        emit("activity.feature.empty", describe(feature.getExt()));
        ActivityInfo navigation = new ActivityInfo();
        navigation.getExt().updateAppFeature(bundle("pico.vr.theme", "NONAVIGATIONBAR"));
        emit("activity.feature.navigation", describe(navigation.getExt()));

        ActivityInfo original = new ActivityInfo();
        original.applicationInfo = new ApplicationInfo();
        original.getExt().setVrActivity(3);
        original.getExt().updateAppFeature(bundle("pico.vr.position", "Left", "pico.vr.app.prop",
                "p", "pico.vr.theme", "nocaptionbar"));
        ActivityInfo copy = new ActivityInfo(original);
        emit("activity.copy", describe(copy.getExt()) + "/" + (copy.getExt() != original.getExt()));

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityInfo parceled = ActivityInfo.CREATOR.createFromParcel(parcel);
        emit("activity.parcel", describe(parceled.getExt()) + "/" + parcel.dataAvail());
        parcel.recycle();
        Parcel ext = Parcel.obtain();
        original.getExt().writeToParcel(ext, 0);
        emit("activity.ext-parcel", hex(ext));
        ext.setDataPosition(0);
        ActivityInfo read = new ActivityInfo();
        read.getExt().readFromParcel(ext);
        emit("activity.ext-read", describe(read.getExt()) + "/" + ext.dataAvail());
        ext.recycle();
    }

    private static void applicationInfo() {
        emit("app.default", describe(new ApplicationInfo().getExt()));
        for (int flag = 0; flag < 8; ++flag) {
            ApplicationInfo info = new ApplicationInfo();
            info.getExt().setVrAppFlag(flag);
            emit("app.flag." + flag, describe(info.getExt()));
        }
        ApplicationInfo orientation = new ApplicationInfo();
        StringBuilder mapped = new StringBuilder();
        for (int value = -2; value <= 14; ++value) {
            mapped.append(orientation.getExt().get2dAppOrientation(value)).append(',');
        }
        emit("app.orientation.map", mapped);
        orientation.getExt().set2dAppDefaultOrientation(1);
        orientation.getExt().setLaunchActivityOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        emit("app.orientation.default", orientation.getExt().get2dAppOrientation());
        orientation.getExt().setLaunchActivityOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        emit("app.orientation.launch", orientation.getExt().get2dAppOrientation());
        orientation.getExt().set2dAppForceOrientation(1);
        emit("app.orientation.force", orientation.getExt().get2dAppOrientation() + "/"
                + orientation.getExt().get2dAppOrientation(0));

        ApplicationInfo original = new ApplicationInfo();
        IExtApplicationInfo ext = original.getExt();
        ext.setVrAppFlag(5);
        ext.set2dAppDensity(240);
        ext.set2dAppPortraitWidth(11);
        ext.set2dAppPortraitHeight(12);
        ext.set2dAppLandscapeWidth(13);
        ext.set2dAppLandscapeHeight(14);
        ext.set2dAppForceOrientation(0);
        ext.set2dAppDefaultOrientation(1);
        ext.setLaunchActivityOrientation(7);
        ext.setDisplayId(9);
        ApplicationInfo copy = new ApplicationInfo(original);
        emit("app.copy", describe(copy.getExt()) + "/" + (copy.getExt() != ext));
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ApplicationInfo parceled = ApplicationInfo.CREATOR.createFromParcel(parcel);
        emit("app.parcel", describe(parceled.getExt()) + "/" + parcel.dataAvail());
        parcel.recycle();
        Parcel raw = Parcel.obtain();
        ext.writeToParcel(raw);
        emit("app.ext-parcel", hex(raw));
        raw.setDataPosition(0);
        ApplicationInfo read = new ApplicationInfo();
        read.getExt().readFromParcel(raw);
        emit("app.ext-read", describe(read.getExt()) + "/" + raw.dataAvail());
        raw.recycle();
    }

    private static PackageParser.Activity activity(PackageParser.Package owner, String name)
            throws Exception {
        Constructor<PackageParser.Activity> constructor = PackageParser.Activity.class
                .getDeclaredConstructor(PackageParser.Package.class, String.class, ActivityInfo.class);
        constructor.setAccessible(true);
        ActivityInfo info = new ActivityInfo();
        info.name = name;
        info.packageName = owner.packageName;
        PackageParser.Activity activity = constructor.newInstance(owner, name, info);
        owner.activities.add(activity);
        return activity;
    }

    private static void intent(PackageParser.Activity activity, String action, String... categories) {
        PackageParser.ActivityIntentInfo intent = new PackageParser.ActivityIntentInfo(activity);
        if (action != null) {
            intent.addAction(action);
        }
        for (String category : categories) {
            intent.addCategory(category);
        }
        activity.intents.add(intent);
    }

    private static Bundle meta(Object... pairs) {
        Bundle bundle = new Bundle();
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i + 1] instanceof Boolean) {
                bundle.putBoolean((String) pairs[i], (Boolean) pairs[i + 1]);
            } else {
                bundle.putString((String) pairs[i], (String) pairs[i + 1]);
            }
        }
        return bundle;
    }

    private static void parsed(IExtPackageParser parser, String name, PackageParser.Package pkg) {
        String result;
        try {
            PackageParser.Package returned = parser.parseVrFlags(pkg);
            IExtApplicationInfo app = pkg.applicationInfo.getExt();
            StringBuilder text = new StringBuilder();
            text.append(returned == pkg).append('|').append(app.getVrAppFlag()).append('|')
                    .append(app.getLaunchActivityOrientation());
            for (PackageParser.Activity activity : pkg.activities) {
                text.append('|').append(activity.className.substring(activity.className
                        .lastIndexOf('.') + 1)).append(':').append(describe(activity.info.getExt()));
            }
            result = text.toString();
        } catch (Throwable e) {
            result = "exception:" + e.getClass().getName();
        }
        emit("parser." + name, result);
    }

    private static void packageParser() throws Exception {
        PackageParser packageParser = new PackageParser();
        Field field = PackageParser.class.getDeclaredField("mExt");
        field.setAccessible(true);
        IExtPackageParser parser = (IExtPackageParser) field.get(packageParser);

        PackageParser.Package app = new PackageParser.Package("org.picomisu.vrapp");
        app.mAppMetaData = meta("com.picovr.type", "vr");
        PackageParser.Activity plain = activity(app, "org.picomisu.vrapp.Plain");
        intent(plain, Intent.ACTION_MAIN, Intent.CATEGORY_LAUNCHER);
        plain.info.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        activity(app, "org.picomisu.vrapp.Flat").metaData = meta("pvr.app.type", "2d");
        PackageParser.Activity forced = activity(app, "org.picomisu.vrapp.Forced");
        forced.metaData = meta("forceRenderActivity", true, "pico.vr.position", "Near",
                "pico.vr.theme", "noCaptionBar");
        PackageParser.Activity alias = activity(app, "org.picomisu.vrapp.Alias");
        alias.info.targetActivity = "org.picomisu.vrapp.Forced";
        activity(app, "android.app.AppDetailsActivity");
        activity(app, "org.picomisu.vrapp.AfterDetails");
        parsed(parser, "vr-app-metadata", app);
        parsed(parser, "vr-app-metadata-repeated", app);

        PackageParser.Package intents = new PackageParser.Package("org.picomisu.intents");
        PackageParser.Activity daydream = activity(intents, "org.picomisu.intents.Daydream");
        intent(daydream, Intent.ACTION_MAIN, "com.google.intent.category.DAYDREAM");
        PackageParser.Activity noAction = activity(intents, "org.picomisu.intents.NoAction");
        intent(noAction, Intent.ACTION_VIEW, "org.khronos.openxr.intent.category.IMMERSIVE_HMD");
        PackageParser.Activity openxr = activity(intents, "org.picomisu.intents.OpenXr");
        intent(openxr, Intent.ACTION_MAIN, Intent.CATEGORY_LAUNCHER,
                "org.khronos.openxr.intent.category.IMMERSIVE_HMD");
        openxr.info.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        activity(intents, "org.picomisu.intents.Component").info.requestedVrComponent =
                "org.picomisu/.Listener";
        PackageParser.Activity flatDaydream = activity(intents, "org.picomisu.intents.FlatDaydream");
        flatDaydream.metaData = meta("com.picovr.type", "2D");
        intent(flatDaydream, Intent.ACTION_MAIN, "com.google.intent.category.DAYDREAM");
        activity(intents, "org.picomisu.intents.Plain");
        parsed(parser, "intent-categories", intents);

        PackageParser.Package tags = new PackageParser.Package("org.picomisu.tags");
        tags.mAppMetaData = meta("com.picovr.type", "2d", "pvr.app.type", "vr");
        activity(tags, "org.picomisu.tags.Upper").metaData = meta("pvr.app.type", "VR");
        activity(tags, "org.picomisu.tags.Other").metaData = meta("com.picovr.type", "3d",
                "pvr.app.type", "vr", "pico.vr.app.prop", "q");
        activity(tags, "org.picomisu.tags.Plain");
        parsed(parser, "tag-order", tags);

        PackageParser.Package secondTag = new PackageParser.Package("org.picomisu.second");
        secondTag.mAppMetaData = meta("com.picovr.type", "3d", "pvr.app.type", "Vr");
        activity(secondTag, "org.picomisu.second.Plain");
        activity(secondTag, "org.picomisu.second.Flat").metaData = meta("com.picovr.type", "2d");
        parsed(parser, "second-tag", secondTag);

        String listed = "org.picomisu.whitelisted";
        PicoSystemConfig.getInstance().getPicoWhitelistVrPackages().add(listed);
        PackageParser.Package white = new PackageParser.Package(listed);
        activity(white, "org.picomisu.whitelisted.Plain");
        activity(white, "org.picomisu.whitelisted.Flat").metaData = meta("pvr.app.type", "2d");
        parsed(parser, "white-list", white);
        PicoSystemConfig.getInstance().getPicoWhitelistVrPackages().remove(listed);

        PackageParser.Package missing = new PackageParser.Package("org.picomisu.missing");
        activity(missing, "org.picomisu.missing.Alias").info.targetActivity = "org.picomisu.missing.None";
        parsed(parser, "missing-target", missing);
    }

    private static String names(PackageParser.Package pkg) {
        StringBuilder text = new StringBuilder();
        for (PackageParser.Permission permission : pkg.permissions) {
            text.append(permission.info.name).append(',');
        }
        text.append('|');
        for (PackageParser.PermissionGroup group : pkg.permissionGroups) {
            text.append(group.info.name).append(',');
        }
        text.append('|').append(pkg.requestedPermissions);
        return text.toString();
    }

    private static PackageParser.Package permissions(String packageName) {
        PackageParser.Package pkg = new PackageParser.Package(packageName);
        String[][] declared = {
            {"com.picovr.permission.FACE_TRACKING", "org.picomisu.group.FACE"},
            {"com.picovr.permission.EYE_TRACKING", "org.picomisu.group.EYE"},
            {"org.picomisu.permission.OTHER", null},
        };
        for (String[] item : declared) {
            PermissionInfo info = new PermissionInfo();
            info.name = item[0];
            info.group = item[1];
            pkg.permissions.add(new PackageParser.Permission(pkg, info));
        }
        for (String name : new String[] {"org.picomisu.group.EYE", "org.picomisu.group.FACE",
                "org.picomisu.group.OTHER"}) {
            PermissionGroupInfo info = new PermissionGroupInfo();
            info.name = name;
            pkg.permissionGroups.add(new PackageParser.PermissionGroup(pkg, info));
        }
        return pkg;
    }

    private static void baseApk(IExtPackageParser parser, String name, PackageParser.Package pkg) {
        String result;
        try {
            parser.parseBaseApkCommon(pkg);
            result = names(pkg);
        } catch (Throwable e) {
            result = "exception:" + e.getClass().getName();
        }
        emit("parser.base." + name, result);
    }

    /** ET/FT permission filter of the factory PackageParser.parseBaseApkCommon hook. */
    private static void packageParserBase() throws Exception {
        PackageParser packageParser = new PackageParser();
        Field field = PackageParser.class.getDeclaredField("mExt");
        field.setAccessible(true);
        IExtPackageParser parser = (IExtPackageParser) field.get(packageParser);
        emit("parser.base.features", Features.supportFTFeature() + "/" + Features.supportETFeature()
                + "/" + ExtPackageParserImpl.DEF_ADD_ET_PERMISSION);
        baseApk(parser, "platform", permissions("android"));
        baseApk(parser, "other-package", permissions("org.picomisu.permissions"));
        PackageParser.Package calibration = new PackageParser.Package("com.tobii.usercalibration.neo3");
        baseApk(parser, "et-calibration", calibration);
        baseApk(parser, "et-calibration-repeated", calibration);
        PackageParser.Package requested = new PackageParser.Package("com.tobii.usercalibration.neo3");
        requested.requestedPermissions.add("com.picovr.permission.EYE_TRACKING");
        baseApk(parser, "et-calibration-requested", requested);
        baseApk(parser, "et-other", new PackageParser.Package("org.picomisu.eye"));
    }

    /**
     * 2D virtual-display configuration of ExtPackageParserUtils: JSON entries, platform
     * defaults and per-package overrides. File loading is disabled (mAppConfigsLoaded) so that
     * the results do not depend on the device files and properties.
     */
    @SuppressWarnings("unchecked")
    private static void virtualDisplayConfig() throws Exception {
        Class<?> utils = Class.forName("android.content.pm.ExtPackageParserUtils");
        Class<?> configClass = Class.forName(
                "android.content.pm.ExtPackageParserUtils$VirtualDisplayConfig");
        Constructor<?> constructor = configClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method parse = utils.getDeclaredMethod("parseVirtualDisplayConfig", JSONObject.class,
                configClass);
        parse.setAccessible(true);
        String project = Features.getProjectName();
        String[][] entries = {
            {"full", "{\"packageName\":\"org.picomisu.full\",\"density\":\"320\","
                    + "\"forceOrientation\":\"1\",\"defaultOrientation\":\"0\","
                    + "\"portraitWidth\":\"100\",\"portraitHeight\":\"200\","
                    + "\"landscapeWidth\":\"300\",\"landscapeHeight\":\"400\"}"},
            {"other-platform", "{\"platform\":\"picomisu-a|picomisu-b\",\"density\":\"100\"}"},
            {"this-platform", "{\"platform\":\"picomisu-a|" + project + "\",\"density\":\"150\"}"},
            {"numbers", "{\"packageName\":\"org.picomisu.numbers\",\"density\":240,"
                    + "\"portraitWidth\":-5}"},
            {"invalid", "{\"density\":\"12\",\"forceOrientation\":\"x\",\"portraitWidth\":\"7\"}"},
            {"empty", "{}"},
        };
        for (String[] entry : entries) {
            Object config = constructor.newInstance();
            String result;
            try {
                result = parse.invoke(null, new JSONObject(entry[1]), config) + "|" + config;
            } catch (Throwable e) {
                result = "exception:" + e.getClass().getName();
            }
            emit("vdc.parse." + entry[0], result);
        }

        Field loaded = utils.getDeclaredField("mAppConfigsLoaded");
        loaded.setAccessible(true);
        Field platform = utils.getDeclaredField("mPlatformVirtualDisplayConfig");
        platform.setAccessible(true);
        Field overrides = utils.getDeclaredField("mAppVirtualDisplayOverrideConfigs");
        overrides.setAccessible(true);
        Object previousPlatform = platform.get(null);
        ArrayMap<String, Object> map = (ArrayMap<String, Object>) overrides.get(null);
        ArrayMap<String, Object> previousOverrides = new ArrayMap<>(map);
        boolean previousLoaded = loaded.getBoolean(null);
        try {
            loaded.setBoolean(null, true);
            Object platformConfig = constructor.newInstance();
            parse.invoke(null, new JSONObject("{\"density\":\"320\",\"portraitWidth\":\"1000\","
                    + "\"portraitHeight\":\"2000\",\"landscapeWidth\":\"3000\","
                    + "\"landscapeHeight\":\"4000\"}"), platformConfig);
            platform.set(null, platformConfig);
            Object appConfig = constructor.newInstance();
            parse.invoke(null, new JSONObject("{\"packageName\":\"org.picomisu.override\","
                    + "\"density\":\"0\",\"portraitWidth\":\"555\",\"portraitHeight\":\"-1\","
                    + "\"landscapeHeight\":\"777\",\"forceOrientation\":\"2\"}"), appConfig);
            map.clear();
            map.put("org.picomisu.override", appConfig);

            ApplicationInfo plain = new ApplicationInfo();
            plain.packageName = "org.picomisu.plain";
            ExtPackageParserUtils.applyVirtualDisplayConfigToApp(plain);
            emit("vdc.apply.platform", describe(plain.getExt()));
            ApplicationInfo override = new ApplicationInfo();
            override.packageName = "org.picomisu.override";
            ExtPackageParserUtils.applyVirtualDisplayConfigToApp(override);
            emit("vdc.apply.override", describe(override.getExt()));
            override.getExt().set2dAppDensity(1);
            override.getExt().set2dAppDefaultOrientation(5);
            ExtPackageParserUtils.applyVirtualDisplayConfigToApp(override);
            emit("vdc.apply.override-again", describe(override.getExt()));

            // Already loaded: no reload replaces the configuration set above.
            ExtPackageParserUtils.loadVirtualDisplayConfigsFromFiles(false);
            ApplicationInfo after = new ApplicationInfo();
            after.packageName = "org.picomisu.override";
            ExtPackageParserUtils.applyVirtualDisplayConfigToApp(after);
            emit("vdc.load-skipped", describe(after.getExt()) + "/"
                    + (platform.get(null) == platformConfig) + "/" + map.size());

            // parseVrFlags applies the configuration to the parsed application.
            PackageParser packageParser = new PackageParser();
            Field field = PackageParser.class.getDeclaredField("mExt");
            field.setAccessible(true);
            IExtPackageParser parser = (IExtPackageParser) field.get(packageParser);
            PackageParser.Package pkg = new PackageParser.Package("org.picomisu.override");
            activity(pkg, "org.picomisu.override.Main");
            parser.parseVrFlags(pkg);
            emit("vdc.parse-vr-flags", describe(pkg.applicationInfo.getExt()));
        } finally {
            platform.set(null, previousPlatform);
            map.clear();
            map.putAll(previousOverrides);
            loaded.setBoolean(null, previousLoaded);
        }
    }

    /** Settings stub: IContentProvider proxy answering Settings "call" with sSetting. */
    static final class LocalResolver extends ContentResolver {
        private final IContentProvider mProvider = (IContentProvider) Proxy.newProxyInstance(
                IContentProvider.class.getClassLoader(), new Class<?>[] {IContentProvider.class},
                (proxy, method, values) -> {
                    if ("call".equals(method.getName())) {
                        Bundle result = new Bundle();
                        if (sSetting != null) {
                            result.putString("value", sSetting);
                        }
                        return result;
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    return null;
                });

        LocalResolver(Context context) {
            super(context);
        }

        @Override
        protected IContentProvider acquireProvider(Context c, String name) {
            return mProvider;
        }

        @Override
        public boolean releaseProvider(IContentProvider icp) {
            return true;
        }

        @Override
        protected IContentProvider acquireUnstableProvider(Context c, String name) {
            return mProvider;
        }

        @Override
        public boolean releaseUnstableProvider(IContentProvider icp) {
            return true;
        }

        @Override
        public void unstableProviderDied(IContentProvider icp) {}
    }

    static final class LocalContext extends ContextWrapper {
        private final ApplicationInfo mInfo = new ApplicationInfo();
        private ContentResolver mResolver;

        LocalContext() {
            super(null);
            mInfo.packageName = "org.picomisu.runtime";
            mInfo.targetSdkVersion = 29;
        }

        @Override
        public ContentResolver getContentResolver() {
            if (mResolver == null) {
                mResolver = new LocalResolver(this);
            }
            return mResolver;
        }

        @Override public ApplicationInfo getApplicationInfo() { return mInfo; }
        @Override public String getPackageName() { return mInfo.packageName; }
        @Override public String getOpPackageName() { return mInfo.packageName; }
        @Override public String getBasePackageName() { return mInfo.packageName; }
        @Override public int getUserId() { return 0; }
        @Override public Context getApplicationContext() { return this; }
    }

    private static Object unsafe() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        for (String name : new String[] {"THE_ONE", "theUnsafe"}) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(null);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("Unsafe unavailable");
    }

    @SuppressWarnings("unchecked")
    static <T> T allocate(Class<T> type) throws Exception {
        Object unsafe = unsafe();
        Method method = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return (T) method.invoke(unsafe, type);
    }

    static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    static Object get(Object target, Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /** A view root with only the fields read by the VR policy and software drawing. */
    static ViewRootImpl root(Context context, String title, int displayId) throws Exception {
        ViewRootImpl root = allocate(ViewRootImpl.class);
        set(root, ViewRootImpl.class, "mContext", context);
        WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
        attributes.setTitle(title);
        set(root, ViewRootImpl.class, "mWindowAttributes", attributes);
        Display display = allocate(Display.class);
        set(display, Display.class, "mDisplayId", displayId);
        set(root, ViewRootImpl.class, "mDisplay", display);
        return root;
    }

    static void display(ViewRootImpl root, int displayId) throws Exception {
        set(get(root, ViewRootImpl.class, "mDisplay"), Display.class, "mDisplayId", displayId);
    }

    /** An activity record whose decor view is attached to {@code root}. */
    static Object record(ViewRootImpl root, int vrFlag, boolean withActivity, boolean withDecor)
            throws Exception {
        Class<?> recordClass = Class.forName("android.app.ActivityThread$ActivityClientRecord");
        Constructor<?> constructor = recordClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object record = constructor.newInstance();
        ActivityInfo info = new ActivityInfo();
        info.getExt().setVrActivity(vrFlag);
        set(record, recordClass, "activityInfo", info);
        if (withActivity) {
            Activity activity = allocate(Activity.class);
            if (withDecor) {
                View decor = allocate(View.class);
                Object attach = allocate(Class.forName("android.view.View$AttachInfo"));
                set(attach, attach.getClass(), "mViewRootImpl", root);
                set(decor, View.class, "mAttachInfo", attach);
                set(activity, Activity.class, "mDecor", decor);
            }
            set(record, recordClass, "activity", activity);
        }
        return record;
    }

    @SuppressWarnings("unchecked")
    static void activities(ActivityThread thread, Object... records) throws Exception {
        ArrayMap<Object, Object> map = (ArrayMap<Object, Object>) get(thread, ActivityThread.class,
                "mActivities");
        map.clear();
        for (Object record : records) {
            map.put(new Binder(), record);
        }
    }

    static String call(java.util.concurrent.Callable<Object> callable) {
        try {
            return String.valueOf(callable.call());
        } catch (Throwable e) {
            return "exception:" + e.getClass().getName();
        }
    }

    /**
     * An ActivityThread with only its activity map and PICO extension; the real constructor
     * needs a Looper and creates Binder objects that these scenarios do not use.
     */
    static ActivityThread newActivityThread() throws Exception {
        ActivityThread thread = allocate(ActivityThread.class);
        set(thread, ActivityThread.class, "mActivities", new ArrayMap<>());
        Constructor<?> ext = Class.forName("android.app.ExtActivityThreadImpl")
                .getConstructor(ActivityThread.class);
        set(thread, ActivityThread.class, "mExt", ext.newInstance(thread));
        return thread;
    }

    private static void activityThreadAndViewRoot() throws Exception {
        Field current = ActivityThread.class.getDeclaredField("sCurrentActivityThread");
        current.setAccessible(true);
        Object previous = current.get(null);
        ActivityThread thread = newActivityThread();
        current.set(null, thread);
        try {
            policies(thread);
        } finally {
            current.set(null, previous);
            sSetting = null;
        }
        LocalContext context = new LocalContext();
        ViewRootImpl root = root(context, "org.picomisu/.Main", 0);
        sSetting = "1";
        current.set(null, null);
        try {
            emit("viewroot.no-activity-thread",
                    call(() -> new ExtViewRootImplImpl(root).isSkipDrawVrActivity()));
        } finally {
            current.set(null, previous);
            sSetting = null;
        }
    }

    private static void policies(ActivityThread thread) throws Exception {
        LocalContext context = new LocalContext();
        ViewRootImpl root = root(context, "org.picomisu/.Main", 0);
        ViewRootImpl other = root(context, "org.picomisu/.Other", 0);
        String[][] cases = {
            {"empty"}, {"match-2d", "0"}, {"match-vr", "1"}, {"match-vr-force", "3"},
            {"match-force-only", "2"}, {"other-root-vr", "1"}, {"no-decor-vr", "1"},
            {"no-activity", "1"},
        };
        for (String[] item : cases) {
            String name = item[0];
            if (name.equals("empty")) {
                activities(thread);
            } else if (name.equals("other-root-vr")) {
                activities(thread, record(other, 1, true, true));
            } else if (name.equals("no-decor-vr")) {
                activities(thread, record(root, 1, true, false));
            } else if (name.equals("no-activity")) {
                activities(thread, record(root, 1, false, false));
            } else {
                activities(thread, record(root, Integer.parseInt(item[1]), true, true));
            }
            emit("thread." + name, call(() -> thread.getExt().isActivityForceRender(root)));
        }

        Object[][] views = {
            // name, setting, record flag (-1: none), title, display
            {"default-setting", null, 1, "org.picomisu/.Main", 0},
            {"setting-1", "1", 1, "org.picomisu/.Main", 0},
            {"setting-0", "0", 1, "org.picomisu/.Main", 0},
            {"setting-2", "2", 1, "org.picomisu/.Main", 0},
            {"setting-invalid", "x", 1, "org.picomisu/.Main", 0},
            {"force-render", "1", 3, "org.picomisu/.Main", 0},
            {"non-vr", "1", 0, "org.picomisu/.Main", 0},
            {"no-match", "1", -1, "org.picomisu/.Main", 0},
            {"display-1", "1", 1, "org.picomisu/.Main", 1},
            {"permission-controller", "1", 1,
                    "com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity", 0},
        };
        for (Object[] item : views) {
            sSetting = (String) item[1];
            ViewRootImpl view = root(context, (String) item[3], (Integer) item[4]);
            int flag = (Integer) item[2];
            if (flag < 0) {
                activities(thread);
            } else {
                activities(thread, record(view, flag, true, true));
            }
            ExtViewRootImplImpl ext = new ExtViewRootImplImpl(view);
            String value = call(() -> ext.isSkipDrawVrActivity());
            emit("viewroot." + item[0], value + "/checked="
                    + get(ext, ExtViewRootImplImpl.class, "mIsCheckedSkipDraw"));
        }

        sSetting = "1";
        ViewRootImpl cached = root(context, "org.picomisu/.Main", 0);
        activities(thread, record(cached, 1, true, true));
        IExtViewRootImpl ext = new ExtViewRootImplImpl(cached);
        StringBuilder sequence = new StringBuilder();
        sequence.append(ext.isSkipDrawVrActivity());
        sSetting = "0";
        activities(thread);
        sequence.append(',').append(ext.isSkipDrawVrActivity());
        display(cached, 2);
        sequence.append(',').append(ext.isSkipDrawVrActivity());
        display(cached, 0);
        sequence.append(',').append(ext.isSkipDrawVrActivity());
        ((WindowManager.LayoutParams) get(cached, ViewRootImpl.class, "mWindowAttributes"))
                .setTitle("com.android.permissioncontroller");
        sequence.append(',').append(ext.isSkipDrawVrActivity());
        emit("viewroot.cached-policy", sequence);

        sSetting = "0";
        ViewRootImpl disabled = root(context, "org.picomisu/.Main", 0);
        activities(thread, record(disabled, 1, true, true));
        IExtViewRootImpl off = new ExtViewRootImplImpl(disabled);
        StringBuilder later = new StringBuilder().append(off.isSkipDrawVrActivity());
        sSetting = "1";
        later.append(',').append(off.isSkipDrawVrActivity());
        emit("viewroot.cached-disabled", later);
        activities(thread);
    }
}
