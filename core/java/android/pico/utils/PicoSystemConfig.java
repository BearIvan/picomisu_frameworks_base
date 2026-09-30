// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.utils;

import android.os.Environment;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * PICO package white/black lists and the per product/edition list of persistent
 * applications that must not be started.
 * @hide
 */
public class PicoSystemConfig {
    static final String TAG = "PicoSystemConfig";
    public static final int XML_CONTENT_TYPE_WHITELIST = 1;
    public static final int XML_CONTENT_TYPE_BLACKLIST = 2;
    private static final String XML_TAG_ITEM = "item";
    private static final String XML_ATTR_PACKAGE_NAME = "packagename";
    private static final String XML_ATTR_CLASS_NAME = "classname";
    private static final String XML_ATTR_CATEGORY = "category";
    private static final String XML_ATTR_VERSION_CODE = "versioncode";
    private static final String XML_ATTR_VERSION_NAME = "versionname";
    private static final String XML_ATTR_APP_TYPE = "type";

    static PicoSystemConfig sInstance;

    private ArraySet<String> mWhitelistVrPackages = new ArraySet<>();
    private ArraySet<String> mWhitelist2dFloatPackages = new ArraySet<>();
    private ArraySet<String> mBlacklistPackages = new ArraySet<>();
    /** "product:default:edition" key to the persistent packages that must not start. */
    private Map<String, List<String>> mNotAllowedStartPersistent = new HashMap<>();

    private final boolean DEBUG;
    private final boolean mIsToBDevice;
    private final String mCurrentProductName;
    private final String mCurrentKey;

    PicoSystemConfig() {
        DEBUG = SystemProperties.getBoolean("persist.pvr.debug", false);
        mIsToBDevice = SystemProperties.getInt("ro.pxr.externalfunc", 0) != 0;
        mCurrentProductName = getCurrentProductName();
        mCurrentKey = getCurrentKey();
        readPicoConfig(XML_CONTENT_TYPE_WHITELIST);
        readPicoConfig(XML_CONTENT_TYPE_BLACKLIST);
        Slog.w(TAG, " mCurrentKey = " + mCurrentKey);
        readPicoNeedDisablePersistentApp();
    }

    public static PicoSystemConfig getInstance() {
        synchronized (PicoSystemConfig.class) {
            if (sInstance == null) {
                sInstance = new PicoSystemConfig();
            }
            return sInstance;
        }
    }

    /**
     * Whether the persistent application {@code packageName} is on the deny list of the
     * current product and edition.
     */
    public boolean notAllowedStartPersistentApp(String packageName) {
        List<String> currentDisableList = mNotAllowedStartPersistent.get(mCurrentKey);
        boolean notAllowed = currentDisableList != null
                ? currentDisableList.contains(packageName) : false;
        Slog.w(TAG, "notAllowedStartPersistentApp packageName=" + packageName
                + ",notAllowed=" + notAllowed);
        return notAllowed;
    }

    private String getCurrentKey() {
        return mCurrentProductName + ":default:" + getCurrentEdition();
    }

    private String getCurrentProductName() {
        String productNameValue = SystemProperties.get("ro.product.name", "");
        if (productNameValue.equalsIgnoreCase("Phoenix")
                || productNameValue.equalsIgnoreCase("Phoenix_ovs")
                || productNameValue.regionMatches(0, "Phoenix", 0, 6)) {
            String retProductName = "PHX";
            String eyeTrackingSupportValue =
                    SystemProperties.get("ro.pxr.eyetracking.support", "");
            if (eyeTrackingSupportValue.equals("1")) {
                retProductName = retProductName + "PRO";
            }
            return retProductName;
        }
        String productModelValue = SystemProperties.get("ro.product.model", "");
        if (productModelValue.regionMatches(0, "Pico Neo 3", 0, 9)
                || productModelValue.regionMatches(0, "Pico Neo3", 0, 8)) {
            return "NEO3";
        }
        String pvrProductName = SystemProperties.get("ro.pvr.product.name", "");
        if (pvrProductName.regionMatches(0, "MerlinE", 0, 6)) {
            return "MerlinE";
        }
        return "NULL";
    }

    private String getCurrentEdition() {
        return mIsToBDevice ? "TOB" : "TOC";
    }

    /**
     * Reads the product/countrycode/edition/package tree of the provisioning deny list; the
     * OTA copy takes precedence over the default one, which must exist.
     */
    private void readPicoNeedDisablePersistentApp() {
        try {
            String defaultList = "/system/etc/pvrprovision/disablepackageslist_default.xml";
            String OTAList = "/system/etc/pvrprovision/disablepackageslist.xml";
            File defaultFile = new File(defaultList);
            if (!defaultFile.exists()) {
                Slog.w(TAG, "File " + defaultList + " does not exist.");
                return;
            }
            File OTAFile = new File(OTAList);
            InputStream inStream;
            if (OTAFile.exists()) {
                Slog.w(TAG, "disableApps using:" + OTAList);
                inStream = OTAFile.toURI().toURL().openStream();
            } else {
                Slog.w(TAG, "disableApps using:" + defaultList);
                inStream = defaultFile.toURI().toURL().openStream();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inStream);
            Element rootElement = document.getDocumentElement();
            NodeList productNodeList = rootElement.getElementsByTagName("product");
            for (int pd = 0; pd < productNodeList.getLength(); pd++) {
                Element productElement = (Element) productNodeList.item(pd);
                NodeList countrycodeNodeList = productElement.getElementsByTagName("countrycode");
                for (int cc = 0; cc < countrycodeNodeList.getLength(); cc++) {
                    Element countrycodeElement = (Element) countrycodeNodeList.item(cc);
                    NodeList editionNodeList = countrycodeElement.getElementsByTagName("edition");
                    for (int et = 0; et < editionNodeList.getLength(); et++) {
                        Element editionElement = (Element) editionNodeList.item(et);
                        NodeList packageNodeList = editionElement.getElementsByTagName("package");
                        for (int pk = 0; pk < packageNodeList.getLength(); pk++) {
                            Element packageElement = (Element) packageNodeList.item(pk);
                            String productName = productElement.getAttribute("name");
                            String countrycodeName = countrycodeElement.getAttribute("name");
                            String editionName = editionElement.getAttribute("name");
                            String packageName = packageElement.getAttribute("name");
                            Slog.w(TAG, "disableAppslist:productName[" + productName
                                    + "]countrycodeName[" + countrycodeName
                                    + "]editionName[" + editionName
                                    + "]packageName[" + packageName + "]");
                            String key = productName + ":" + countrycodeName + ":" + editionName;
                            List<String> value = mNotAllowedStartPersistent.get(key);
                            if (value == null) {
                                value = new ArrayList<>();
                                value.add(packageName);
                                mNotAllowedStartPersistent.put(key, value);
                            } else {
                                value.add(packageName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        File fileToParse = null;
        File fileCreatedByProvider = null;
        File filePrebuiltWhenCompile = null;
        if (type == XML_CONTENT_TYPE_WHITELIST) {
            fileCreatedByProvider = Environment.buildPath(Environment.getUserSystemDirectory(0),
                    "pvr_white_list.xml");
            filePrebuiltWhenCompile = Environment.buildPath(Environment.getRootDirectory(),
                    "etc", "whitelist");
        } else if (type == XML_CONTENT_TYPE_BLACKLIST) {
            fileCreatedByProvider = Environment.buildPath(Environment.getUserSystemDirectory(0),
                    "pvr_black_list.xml");
            filePrebuiltWhenCompile = Environment.buildPath(Environment.getRootDirectory(),
                    "etc", "blacklist");
        }
        if (fileCreatedByProvider != null && fileCreatedByProvider.exists()) {
            fileToParse = fileCreatedByProvider;
        } else if (filePrebuiltWhenCompile != null && filePrebuiltWhenCompile.exists()) {
            fileToParse = filePrebuiltWhenCompile;
        } else {
            Slog.w(TAG, "No proper file to parse pico white list! Return!");
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "fileToParse:" + fileToParse);
        }
        if (!fileToParse.canRead()) {
            Slog.w(TAG, "File " + fileToParse + " cannot be read");
            return;
        }
        if (fileToParse.isDirectory()) {
            for (File f : fileToParse.listFiles()) {
                if (!f.getPath().endsWith(".xml")) {
                    Slog.i(TAG, "Non-xml file " + f + " in " + fileToParse + " directory, ignoring");
                } else if (!f.canRead()) {
                    Slog.w(TAG, "Pico white list file " + f + " cannot be read");
                } else {
                    readPicoConfigFromXml(f, type);
                }
            }
        } else {
            // The factory logs a non-.xml name but still parses the file.
            if (!fileToParse.getPath().endsWith(".xml")) {
                Slog.i(TAG, "File " + fileToParse + " is not xml file, ignoring");
            }
            readPicoConfigFromXml(fileToParse, type);
        }
        if (DEBUG) {
            Slog.v(TAG, "Size of mWhitelistVrPackages:" + mWhitelistVrPackages.size());
            for (String packageName : mWhitelistVrPackages) {
                Slog.v(TAG, "white list VR package:" + packageName);
            }
            Slog.v(TAG, "Size of mWhitelist2dFloatPackages:" + mWhitelist2dFloatPackages.size());
            for (String packageName : mWhitelist2dFloatPackages) {
                Slog.v(TAG, "white list 2d-float package:" + packageName);
            }
            Slog.v(TAG, "Size of mBlacklistPackages:" + mBlacklistPackages.size());
            for (String packageName : mBlacklistPackages) {
                Slog.v(TAG, "black list package:" + packageName);
            }
        }
    }

    void readPicoConfigFromXml(File file, int xmlContentType) {
        FileReader str = null;
        try {
            str = new FileReader(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(str);
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                ;
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            if (xmlContentType == XML_CONTENT_TYPE_WHITELIST) {
                if ("whitelist-packages".equals(parser.getName())) {
                    mWhitelistVrPackages.clear();
                    mWhitelist2dFloatPackages.clear();
                } else {
                    throw new XmlPullParserException("Unexpected start tag in " + file
                            + ": found " + parser.getName() + ", expected 'whitelist-packages'");
                }
            } else if (xmlContentType == XML_CONTENT_TYPE_BLACKLIST) {
                if ("blacklist-packages".equals(parser.getName())) {
                    mBlacklistPackages.clear();
                } else {
                    throw new XmlPullParserException("Unexpected start tag in " + file
                            + ": found " + parser.getName() + ", expected 'blacklist-packages'");
                }
            } else {
                Slog.w(TAG, "Unknown xml content type,Ignore!");
            }
            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }
                String tagName = parser.getName();
                if (tagName.equals(XML_TAG_ITEM)) {
                    if (xmlContentType == XML_CONTENT_TYPE_WHITELIST) {
                        String packageName = parser.getAttributeValue(null, XML_ATTR_PACKAGE_NAME);
                        String appType = parser.getAttributeValue(null, XML_ATTR_APP_TYPE);
                        if ("vr".equalsIgnoreCase(appType)) {
                            mWhitelistVrPackages.add(packageName);
                        } else if ("2d-float".equalsIgnoreCase(appType)) {
                            mWhitelist2dFloatPackages.add(packageName);
                        } else {
                            Slog.w(TAG, "Unknown app type:" + appType);
                        }
                    } else if (xmlContentType == XML_CONTENT_TYPE_BLACKLIST) {
                        String packageName = parser.getAttributeValue(null, XML_ATTR_PACKAGE_NAME);
                        mBlacklistPackages.add(packageName);
                    } else {
                        Slog.w(TAG, "Unknow xml type,Ignore!");
                    }
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            Slog.w(TAG, "Error reading apps file " + file, e);
        } catch (IOException e) {
            Slog.w(TAG, "Error reading apps file " + file, e);
        } catch (Exception e) {
            Slog.w(TAG, "Error reading apps file " + file, e);
        } finally {
            if (str != null) {
                try {
                    str.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
