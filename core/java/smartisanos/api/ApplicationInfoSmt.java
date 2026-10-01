// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.api;

import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoSmtBase;
import android.util.Singleton;

/**
 * Smartisan API accessors for the {@link ApplicationInfoSmtBase} state of an
 * {@link ApplicationInfo}. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class ApplicationInfoSmt {
    static final String TAG = "ApplicationInfoSmt";

    public static final int SMARTISAN_FLAG_CATEGORY_GAME = 4;
    public static final int FLAG_FROM_PC = 0x8000;

    static final int MINUTE_TO_MILLISECOND = 60 * 1000;

    static final Singleton<ApplicationInfoSmt> sApplicationInfoSmt =
            new Singleton<ApplicationInfoSmt>() {
        @Override
        protected ApplicationInfoSmt create() {
            return new ApplicationInfoSmt();
        }
    };

    public static ApplicationInfoSmt getInstance() {
        return sApplicationInfoSmt.get();
    }

    private ApplicationInfoSmt() {
    }

    public boolean isPlutoNamespace(String namespace) {
        return ApplicationInfoSmtBase.isPlutoNamespace(namespace);
    }

    public boolean isGameApp(ApplicationInfo info) {
        if (info == null) {
            return false;
        }
        return info.getSmtEx().isGameAppSmt();
    }

    public static boolean isVideoApp(ApplicationInfo info) {
        return (info.getSmtEx().appTypeFlag & ApplicationInfoSmtBase.APP_TYPE_TELEVISION)
                        == ApplicationInfoSmtBase.APP_TYPE_TELEVISION
                || (info.getSmtEx().appTypeFlag & ApplicationInfoSmtBase.APP_TYPE_LIVE)
                        == ApplicationInfoSmtBase.APP_TYPE_LIVE
                || (info.getSmtEx().appTypeFlag & ApplicationInfoSmtBase.APP_TYPE_SHORTVIDEO)
                        == ApplicationInfoSmtBase.APP_TYPE_SHORTVIDEO;
    }

    public static boolean isIMApp(ApplicationInfo info) {
        return (info.getSmtEx().appTypeFlag & ApplicationInfoSmtBase.APP_TYPE_CHATFRIENDS)
                == ApplicationInfoSmtBase.APP_TYPE_CHATFRIENDS;
    }

    public static boolean isVideoOrGameApp(ApplicationInfo info) {
        return info.getSmtEx().isGameAppSmt() || isVideoApp(info);
    }

    public static boolean goodToOperateProc(ApplicationInfo info, long now, int type,
            int percent) {
        if (info.getSmtEx().perceptibleTime == 0) {
            return true;
        }
        int duration;
        switch (type) {
            case 4:
                duration = 3 * MINUTE_TO_MILLISECOND;
                break;
            case 8:
                if (ApplicationInfoSmtBase.APP_TYPE_MAPNAVIGATION
                        == info.getSmtEx().appTypeFlag) {
                    duration = 10 * MINUTE_TO_MILLISECOND;
                } else {
                    duration = 6 * MINUTE_TO_MILLISECOND;
                }
                break;
            case 16:
                duration = 10 * MINUTE_TO_MILLISECOND;
                break;
            default:
                return false;
        }
        return info.getSmtEx().perceptibleTime + (duration * percent / 100) < now;
    }

    public int getPeroptFlag(ApplicationInfo applicationInfo) {
        return applicationInfo.getSmtEx().peroptFlag;
    }

    public boolean isGameAppSmt(ApplicationInfo applicationInfo) {
        return applicationInfo.getSmtEx().isGameAppSmt();
    }

    public int getSmartisanFlag(ApplicationInfo applicationInfo) {
        return applicationInfo.getSmtEx().smartisanFlag;
    }
}
