// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * File store of the Smartisan blurred starting window (bsw): the cached first-frame or
 * starting-window screenshots of the apps (/data/blurwindow/&lt;package&gt;.jpg) and their
 * properties. Reconstructed from the factory PICO OS 5.13.7 services.
 *
 * @hide
 */
public class BlurStartingWindowUtils {
    static final int BITMAP_SCALE = 8;
    static final int BLUR_PARAMETER = 3;
    static final int COMPRESS_RATIO = 90;
    static boolean DEBUG_BSW = SystemProperties.getBoolean("debug.bsw", false);
    static final String DEFAULT_BASE_DIR = "/data/blurwindow/";
    public static final boolean FEATURE_BSW_ENABLED = true;
    private static final String IMAGE_TYPE_JPG = ".jpg";
    public static final String KEY_WINDOW_FLAG = "window_flag";
    private static final String PROPERTIES_FILE_SUFFIX = ".properties";
    static final int SLEEP_TIME = 50;
    private static final String SPECIAL_IMAGE_SUFFIX = "_bswsmspecial";
    static final String SYSTEM_MEDIA_DIR = "/system/media/";
    static final String TAG = "BlurStartingWindowUtils";
    public static final int TYPE_BLUR_WINDOW = 1;
    public static final String TYPE_DEFAULT_STARTING_WINDOW = "default";
    public static final String TYPE_DEFAULT_TNT_STARTING_WINDOW = "TNT";
    public static final String TYPE_PREVIEW_STARTING_WINDOW = "preview";
    public static final String TYPE_PREVIEW_TNT_STARTING_WINDOW = "preview/TNT";
    public static final int TYPE_TRANSLUCENT_WINDOW = 2;

    static synchronized boolean checkImageAllBlack(Bitmap bm) {
        int[] buffer = new int[bm.getWidth() * bm.getHeight()];
        bm.getPixels(buffer, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
        boolean allBlack = true;
        int firstColor = buffer[0];
        if (firstColor != 0) {
            return false;
        }
        for (int color : buffer) {
            if (color != firstColor) {
                allBlack = false;
                break;
            }
        }
        return allBlack;
    }

    static synchronized void saveScreenshotBitmap(String fileName, Bitmap bm,
            int specialCompressRatio, String folderType) {
        File dir;
        if (TYPE_DEFAULT_STARTING_WINDOW.equals(folderType)) {
            dir = new File(DEFAULT_BASE_DIR);
        } else if (folderType != null && !"".equals(folderType)) {
            dir = new File(DEFAULT_BASE_DIR + "/" + folderType);
        } else {
            return;
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir.getPath(), fileName + IMAGE_TYPE_JPG);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Slog.e(TAG, "IOException when creating " + fileName + IMAGE_TYPE_JPG + " file");
                return;
            }
            if (!file.exists()) {
                return;
            }
            FileOutputStream out = null;
            AtomicFile af = new AtomicFile(file);
            try {
                out = af.startWrite();
                int compressRatio = specialCompressRatio > 0
                        ? specialCompressRatio : COMPRESS_RATIO;
                if (bm.compress(Bitmap.CompressFormat.JPEG, compressRatio, out)) {
                    out.flush();
                }
                af.finishWrite(out);
            } catch (Exception e) {
                Slog.e(TAG, "Exception when save file " + fileName + IMAGE_TYPE_JPG);
                if (out != null) {
                    af.failWrite(out);
                }
            } finally {
                if (bm != null) {
                    bm.recycle();
                }
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "Exception when call out.close()");
                    }
                }
            }
        } else {
            Slog.w(TAG, "File " + fileName + IMAGE_TYPE_JPG + " exists, return with no action");
        }
    }

    static synchronized void storeBswInfo(String key, String value, String fileName,
            String folderType) {
        if (key == null || "".equals(key) || value == null || "".equals(value)) {
            return;
        }
        File file;
        if (TYPE_DEFAULT_STARTING_WINDOW.equals(folderType)) {
            file = new File(DEFAULT_BASE_DIR, fileName + PROPERTIES_FILE_SUFFIX);
        } else if (folderType != null && !"".equals(folderType)) {
            file = new File(DEFAULT_BASE_DIR + folderType, fileName + PROPERTIES_FILE_SUFFIX);
        } else {
            return;
        }
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Slog.e(TAG, "IOException when creating " + fileName + PROPERTIES_FILE_SUFFIX
                        + " file");
                return;
            }
        }
        OutputStream out = null;
        InputStream in = null;
        try {
            Properties pps = new Properties();
            in = new FileInputStream(file);
            pps.load(in);
            pps.setProperty(key, value);
            out = new FileOutputStream(file);
            pps.store(out, "store key=" + key + " value=" + value);
            out.flush();
            out.close();
            in.close();
        } catch (IOException e) {
            Slog.e(TAG, "IOException when store " + fileName + PROPERTIES_FILE_SUFFIX + " file");
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException when store " + fileName + PROPERTIES_FILE_SUFFIX
                            + " file");
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException when store " + fileName + PROPERTIES_FILE_SUFFIX
                            + " file");
                }
            }
        }
    }

    public static synchronized String getBswInfoByKey(String key, String fileName,
            String folderType) {
        if (key == null || "".equals(key)) {
            return "";
        }
        File file;
        if (TYPE_DEFAULT_STARTING_WINDOW.equals(folderType)) {
            file = new File(DEFAULT_BASE_DIR, fileName + PROPERTIES_FILE_SUFFIX);
        } else if (folderType != null && !"".equals(folderType)) {
            // As on the factory: the folder itself, not the properties file inside it.
            file = new File(DEFAULT_BASE_DIR + "/" + folderType);
        } else {
            return "";
        }
        if (!file.exists()) {
            return "";
        }
        Properties pps = new Properties();
        InputStream in = null;
        String value = "";
        try {
            in = new FileInputStream(file);
            pps.load(in);
            value = pps.getProperty(key);
            in.close();
        } catch (IOException e) {
            Slog.e(TAG, "IOException when parsing " + fileName + PROPERTIES_FILE_SUFFIX + " file");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException when store " + fileName + PROPERTIES_FILE_SUFFIX
                            + " file");
                }
            }
        }
        return value;
    }

    public static String checkFileExist(String fileName, String folderPath) {
        if (fileName == null || "".equals(fileName)) {
            return "";
        }
        File file;
        if (TYPE_DEFAULT_STARTING_WINDOW.equals(folderPath)) {
            file = new File(DEFAULT_BASE_DIR, fileName + IMAGE_TYPE_JPG);
        } else if (folderPath != null && !"".equals(folderPath)) {
            file = new File(DEFAULT_BASE_DIR + folderPath, fileName + IMAGE_TYPE_JPG);
            if (!file.exists()) {
                file = new File(SYSTEM_MEDIA_DIR + folderPath, fileName + IMAGE_TYPE_JPG);
            }
        } else {
            return "";
        }
        if (!file.exists()) {
            return "";
        }
        return file.getAbsolutePath();
    }

    public static void deleteBswFileIfExist(String packageName) {
        File bswFile = new File(DEFAULT_BASE_DIR, packageName + IMAGE_TYPE_JPG);
        if (bswFile.exists() && bswFile.isFile() && !bswFile.delete()) {
            Slog.w(TAG, "Couldn't delete bsw file: " + packageName + IMAGE_TYPE_JPG);
        }
        File propertiesFile = new File(DEFAULT_BASE_DIR, packageName + PROPERTIES_FILE_SUFFIX);
        if (propertiesFile.exists() && propertiesFile.isFile() && !propertiesFile.delete()) {
            Slog.w(TAG, "Couldn't delete bsw file: " + packageName + PROPERTIES_FILE_SUFFIX);
        }
        String specialPkgName = convertSpecialImageName(packageName);
        File specialBswFile = new File(DEFAULT_BASE_DIR, specialPkgName + IMAGE_TYPE_JPG);
        if (specialBswFile.exists() && specialBswFile.isFile() && !specialBswFile.delete()) {
            Slog.w(TAG, "Couldn't delete bsw file: " + specialPkgName + IMAGE_TYPE_JPG);
        }
        File specialPropertiesFile = new File(DEFAULT_BASE_DIR,
                specialPkgName + PROPERTIES_FILE_SUFFIX);
        if (specialPropertiesFile.exists() && specialPropertiesFile.isFile()
                && !specialPropertiesFile.delete()) {
            Slog.w(TAG, "Couldn't delete bsw file: " + specialPkgName + PROPERTIES_FILE_SUFFIX);
        }
    }

    public static void deleteAllBswFiles() {
        File bswDir = new File(DEFAULT_BASE_DIR);
        if (bswDir.exists() && bswDir.isDirectory()) {
            try {
                for (File file : bswDir.listFiles()) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, "deleteAllBswFiles exception", e);
            }
        }
    }

    public static Bitmap decodeFileToBitmap(String fileName, String typeFolder) {
        String fileExistPath = checkFileExist(fileName, typeFolder);
        if (fileExistPath != null && !"".equals(fileExistPath)) {
            Bitmap bm = BitmapFactory.decodeFile(fileExistPath);
            return bm;
        }
        Slog.w(TAG, "file:" + fileExistPath + "not exsits");
        return null;
    }

    public static String convertSpecialImageName(String packageName) {
        return packageName + SPECIAL_IMAGE_SUFFIX;
    }

    static void checkTime(long startTime, String where) {
        long now = SystemClock.elapsedRealtime();
        Slog.w(TAG, "Operation cost: " + (now - startTime) + "ms, operation : " + where);
    }

    /** Stack blur (Mario Klingemann's algorithm) of a copy of sentBitmap. */
    static Bitmap fastblur(Bitmap sentBitmap, int radius) {
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (radius < 1) {
            return null;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve the alpha channel.
                pix[yi] = (pix[yi] & 0xff000000) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = vmin[y] + x;
                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }

    public static Bitmap getBitmapWithoutTitlebar(Bitmap srcBm, int paddingBar) {
        return Bitmap.createBitmap(srcBm, 0, 0, 0, 0);
    }
}
