// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Icon;

import java.util.Arrays;
import java.util.List;

/**
 * Smartisan notification helpers: app icon for notifications of non-system packages and
 * rounded bitmap icons (factory PICO OS 5.13.7 {@code android.app.NotificationSmtBase}; nothing in
 * the factory jars calls it).
 *
 * @hide
 */
public class NotificationSmtBase {
    protected static final String TAG = "NotificationSmtEx";

    private static final List<String> NOTIFICATION_WHITE_LIST = Arrays.asList(
            "com.android.phone",
            "com.android.mms",
            "com.android.email",
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.exchange",
            "com.smartisanos.appstore",
            "com.android.bluetooth",
            "com.smartisanos.recorder",
            "com.android.calendar",
            "com.smartisanos.cleaner",
            "com.smartisanos.clock",
            "com.smartisanos.cloudsync",
            "com.android.server.telecom",
            "com.redteamobile.roaming",
            "com.smartisanos.screenrecorder",
            "com.android.providers.downloads",
            "com.android.gallery3d",
            "com.android.musicfx",
            "com.smartisanos.updater",
            "com.android.desktop.systemui",
            "com.smartisanos.textboom",
            "com.ss.android.smartisan.browser",
            "com.android.browser",
            "com.smartisan.unionpush.proxy",
            "com.smartisan.smpush",
            "com.smartisanos.gamestore",
            "com.smartisanos.boston.phone");

    /** Returns the application icon of {@code context}, or 0 for white-listed packages. */
    public static int getNotificationAppIcon(Context context) {
        int iconId = 0;
        if (context != null) {
            try {
                String pkg = context.getPackageName();
                if (!NOTIFICATION_WHITE_LIST.contains(pkg)) {
                    ApplicationInfo info = context.getApplicationInfo();
                    iconId = info.icon;
                }
            } catch (Exception e) {
                return iconId;
            }
        }
        return iconId;
    }

    /** Returns a copy of a bitmap {@code icon} with rounded corners. */
    public static Icon getRoundedCornerBitmap(Icon icon) {
        if (icon == null || icon.getType() != Icon.TYPE_BITMAP || icon.getBitmap() == null) {
            return icon;
        }
        Bitmap bitmap = icon.getBitmap();
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 9.0f;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return Icon.createWithBitmap(output);
    }
}
