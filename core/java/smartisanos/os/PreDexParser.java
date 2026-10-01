// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser of the Smartisan PreDex.xml list. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class PreDexParser {
    private static final String TAG = "PreDexParser";
    private static final String DEFAULT_PATH = "/system/etc/";
    private static final String UPDATE_PATH = "/data/system/";
    private static final String PEROPT_FILE_NAME = "PreDex.xml";
    private static final String TAG_NAME_PREDEXFILE = "PreDexFile";

    private static PreDexParser mInstance = null;

    private int mVersion = 0;
    private List<PreDexApp> mPreDexFiles = new ArrayList<PreDexApp>(8);

    private PreDexParser() {
    }

    public static PreDexParser getInstance() {
        if (mInstance == null) {
            mInstance = new PreDexParser();
        }
        return mInstance;
    }

    public void init() throws XmlPullParserException, IOException {
        InputStream is = null;
        File f = new File(UPDATE_PATH + PEROPT_FILE_NAME);
        if (f.exists()) {
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                f = null;
                return;
            }
        } else {
            f = new File(DEFAULT_PATH + PEROPT_FILE_NAME);
            if (f.exists()) {
                try {
                    is = new FileInputStream(f);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    f = null;
                    return;
                }
            } else {
                Log.e(TAG, "## Pre dex xml not exist.");
                f = null;
                return;
            }
        }
        parse(is);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parse(InputStream is) throws XmlPullParserException, IOException {
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
                            String versionString = parser.getAttributeValue(null, "version");
                            int version = Integer.parseInt(versionString);
                            if (version > mVersion) {
                                mVersion = version;
                                mPreDexFiles.clear();
                            } else {
                                Log.w(TAG, "version err, mVersion: " + mVersion
                                        + ", new version: " + version);
                            }
                        } else if (parser.getName().equals(TAG_NAME_PREDEXFILE)) {
                            String packageName = parser.getAttributeValue(null, "name");
                            String secondDir = parser.getAttributeValue(null, "secondDir");
                            int type = Integer.parseInt(parser.getAttributeValue(null, "type"));
                            String outDir = parser.getAttributeValue(null, "outDir");
                            mPreDexFiles.add(new PreDexApp(packageName, secondDir, type, outDir));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Slog.e(TAG, "parse predex file error!", e);
        }
    }

    public void clear() {
        mPreDexFiles.clear();
    }

    public List<PreDexApp> getPreDexApps() {
        return mPreDexFiles;
    }

    /** @hide */
    public class PreDexApp {
        public String packageName;
        public String secondeDir;
        public int type;
        public String outDir;

        public PreDexApp(String packageName, String secondeDir, int type, String outDir) {
            this.packageName = packageName;
            this.secondeDir = secondeDir;
            this.type = type;
            this.outDir = outDir;
        }
    }
}
