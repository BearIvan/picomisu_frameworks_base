// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.utils;

import android.os.Environment;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * PICO package white/black lists. Only the list parts of the factory class are
 * ported; product/edition keys and the persistent-app deny list are not.
 * @hide
 */
public class PicoSystemConfig {
    static final String TAG = "PicoSystemConfig";
    public static final int XML_CONTENT_TYPE_WHITELIST = 1;
    public static final int XML_CONTENT_TYPE_BLACKLIST = 2;
    private static final String XML_TAG_ITEM = "item";
    private static final String XML_ATTR_PACKAGE_NAME = "packagename";
    private static final String XML_ATTR_APP_TYPE = "type";

    static PicoSystemConfig sInstance;

    private final boolean DEBUG;
    private ArraySet<String> mWhitelistVrPackages = new ArraySet<>();
    private ArraySet<String> mWhitelist2dFloatPackages = new ArraySet<>();
    private ArraySet<String> mBlacklistPackages = new ArraySet<>();

    PicoSystemConfig() {
        DEBUG = SystemProperties.getBoolean("persist.pvr.debug", false);
        readPicoConfig(XML_CONTENT_TYPE_WHITELIST);
        readPicoConfig(XML_CONTENT_TYPE_BLACKLIST);
    }

    public static PicoSystemConfig getInstance() {
        synchronized (PicoSystemConfig.class) {
            if (sInstance == null) {
                sInstance = new PicoSystemConfig();
            }
            return sInstance;
        }
    }

    public ArraySet<String> getPicoBlacklistPackages() {
        return mBlacklistPackages;
    }

    public ArraySet<String> getPicoWhitelist2dFloatPackages() {
        return mWhitelist2dFloatPackages;
    }

    public ArraySet<String> getPicoWhitelistVrPackages() {
        return mWhitelistVrPackages;
    }

    public void updatePicoConfig(int type) {
        Slog.v(TAG, "updatePicoConfig...type:" + type);
        readPicoConfig(type);
    }

    void readPicoConfig(int type) {
        File userFile = null;
        File systemFile = null;
        if (type == XML_CONTENT_TYPE_WHITELIST) {
            userFile = Environment.buildPath(Environment.getUserSystemDirectory(0),
                    "pvr_white_list.xml");
            systemFile = Environment.buildPath(Environment.getRootDirectory(), "etc", "whitelist");
        } else if (type == XML_CONTENT_TYPE_BLACKLIST) {
            userFile = Environment.buildPath(Environment.getUserSystemDirectory(0),
                    "pvr_black_list.xml");
            systemFile = Environment.buildPath(Environment.getRootDirectory(), "etc", "blacklist");
        }
        File file;
        if (userFile != null && userFile.exists()) {
            file = userFile;
        } else if (systemFile != null && systemFile.exists()) {
            file = systemFile;
        } else {
            Slog.w(TAG, "No proper file to parse pico white list! Return!");
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "fileToParse:" + file);
        }
        if (!file.canRead()) {
            Slog.w(TAG, "File " + file + " cannot be read");
            return;
        }
        if (file.isDirectory()) {
            for (File item : file.listFiles()) {
                if (!item.getPath().endsWith(".xml")) {
                    Slog.i(TAG, "Non-xml file " + item + " in " + file + " directory, ignoring");
                } else if (!item.canRead()) {
                    Slog.w(TAG, "Pico white list file " + item + " cannot be read");
                } else {
                    readPicoConfigFromXml(item, type);
                }
            }
        } else {
            // The factory logs a non-.xml name but still parses the file.
            if (!file.getPath().endsWith(".xml")) {
                Slog.i(TAG, "File " + file + " is not xml file, ignoring");
            }
            readPicoConfigFromXml(file, type);
        }
        if (DEBUG) {
            Slog.v(TAG, "Size of mWhitelistVrPackages:" + mWhitelistVrPackages.size());
            for (String name : mWhitelistVrPackages) {
                Slog.v(TAG, "white list VR package:" + name);
            }
            Slog.v(TAG, "Size of mWhitelist2dFloatPackages:" + mWhitelist2dFloatPackages.size());
            for (String name : mWhitelist2dFloatPackages) {
                Slog.v(TAG, "white list 2d-float package:" + name);
            }
            Slog.v(TAG, "Size of mBlacklistPackages:" + mBlacklistPackages.size());
            for (String name : mBlacklistPackages) {
                Slog.v(TAG, "black list package:" + name);
            }
        }
    }

    void readPicoConfigFromXml(File file, int type) {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            int event;
            while ((event = parser.next()) != XmlPullParser.START_TAG
                    && event != XmlPullParser.END_DOCUMENT) {
            }
            if (event != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            if (type == XML_CONTENT_TYPE_WHITELIST) {
                if (!"whitelist-packages".equals(parser.getName())) {
                    throw new XmlPullParserException("Unexpected start tag in " + file + ": found "
                            + parser.getName() + ", expected 'whitelist-packages'");
                }
                mWhitelistVrPackages.clear();
                mWhitelist2dFloatPackages.clear();
            } else if (type == XML_CONTENT_TYPE_BLACKLIST) {
                if (!"blacklist-packages".equals(parser.getName())) {
                    throw new XmlPullParserException("Unexpected start tag in " + file + ": found "
                            + parser.getName() + ", expected 'blacklist-packages'");
                }
                mBlacklistPackages.clear();
            } else {
                Slog.w(TAG, "Unknown xml content type,Ignore!");
            }
            final int depth = parser.getDepth();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (event != XmlPullParser.END_TAG || parser.getDepth() > depth)) {
                if (event == XmlPullParser.END_TAG || event == XmlPullParser.TEXT) {
                    continue;
                }
                if (!XML_TAG_ITEM.equals(parser.getName())) {
                    XmlUtils.skipCurrentTag(parser);
                    continue;
                }
                if (type == XML_CONTENT_TYPE_WHITELIST) {
                    String name = parser.getAttributeValue(null, XML_ATTR_PACKAGE_NAME);
                    String appType = parser.getAttributeValue(null, XML_ATTR_APP_TYPE);
                    if ("vr".equalsIgnoreCase(appType)) {
                        mWhitelistVrPackages.add(name);
                    } else if ("2d-float".equalsIgnoreCase(appType)) {
                        mWhitelist2dFloatPackages.add(name);
                    } else {
                        Slog.w(TAG, "Unknown app type:" + appType);
                    }
                } else if (type == XML_CONTENT_TYPE_BLACKLIST) {
                    mBlacklistPackages.add(parser.getAttributeValue(null, XML_ATTR_PACKAGE_NAME));
                } else {
                    Slog.w(TAG, "Unknow xml type,Ignore!");
                }
            }
        } catch (XmlPullParserException | IOException e) {
            Slog.w(TAG, "Error reading apps file " + file, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
