// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package org.picomisu.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads every class of the factory boot class path JARs that the Source image keeps outside
 * framework.jar through the boot class loader, without initialising it, and reports the class
 * status taken from the compiled boot image (compile-time verification) and its declared members.
 * The same probe JAR runs on the factory framework; lines "factory-classpath key=value" are
 * compared by tools/compare-wire-fixture.py.
 */
public final class FactoryClassPathFixture {
    static final int EXPECTED_LINES = 218;
    static final String[] JARS = {"sysmonitor-framework", "sys-framework", "devicemiddlewareimpl",
            "vrex-framework", "tcmiface", "telephony-ext", "qcom.fmradio", "QPerformance",
            "UxPerformance", "WfdCommon"};
    // art/runtime/class_status.h (Android 10); mirror::Class keeps it in the top four bits.
    private static final String[] STATUS = {"NotReady", "Retired", "ErrorResolved",
            "ErrorUnresolved", "Idx", "Loaded", "Resolving", "Resolved", "Verifying",
            "RetryVerificationAtRuntime", "VerifyingAtRuntime", "Verified", "SuperclassValidated",
            "Initializing", "Initialized"};
    private static PrintStream sOut;
    private static int sLines;

    private FactoryClassPathFixture() {}

    public static void main(String[] args) throws Exception {
        System.out.println("factory-classpath-fixture lines=" + run(System.out));
    }

    static void emit(String key, Object value) {
        sOut.println("factory-classpath " + key + "=" + value);
        sLines++;
    }

    static int run(PrintStream out) throws Exception {
        sOut = out;
        sLines = 0;
        String[] bootClassPath = System.getProperty("java.boot.class.path").split(":");
        Field status = Class.class.getDeclaredField("status");
        status.setAccessible(true);
        for (String jar : JARS) {
            String path = null;
            for (String entry : bootClassPath) {
                if (new File(entry).getName().equals(jar + ".jar")) path = entry;
            }
            if (path == null) {
                emit("jar." + jar, "absent");
                continue;
            }
            List<String> names = classNames(path);
            emit("jar." + jar, names.size());
            for (String name : names) {
                emit(jar + "/" + name, describe(name, status));
            }
        }
        return sLines;
    }

    private static String describe(String name, Field status) {
        Class<?> clazz;
        try {
            clazz = Class.forName(name, false, null);
        } catch (Throwable e) {
            return "load:" + e.getClass().getName();
        }
        String result;
        try {
            int value = status.getInt(clazz) >>> 28;
            result = value < STATUS.length ? STATUS[value] : "status" + value;
        } catch (Throwable e) {
            result = "status:" + e.getClass().getName();
        }
        if (clazz.getClassLoader() != Object.class.getClassLoader()) result += "|nonboot";
        try {
            result += "|f" + clazz.getDeclaredFields().length + "|m" + clazz.getDeclaredMethods().length
                    + "|c" + clazz.getDeclaredConstructors().length;
        } catch (Throwable e) {
            result += "|members:" + e.getClass().getName();
        }
        return result;
    }

    /** Class names of classes*.dex in one JAR, read from the DEX class_defs. */
    private static List<String> classNames(String path) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(path)) {
            for (int index = 1; ; ++index) {
                ZipEntry entry = zip.getEntry(index == 1 ? "classes.dex" : "classes" + index + ".dex");
                if (entry == null) break;
                byte[] dex = read(zip.getInputStream(entry));
                int stringIds = u32(dex, 60);
                int typeIds = u32(dex, 68);
                int count = u32(dex, 96);
                int classDefs = u32(dex, 100);
                for (int i = 0; i < count; ++i) {
                    int type = u32(dex, classDefs + i * 32);
                    int string = u32(dex, typeIds + type * 4);
                    String descriptor = mutf8(dex, u32(dex, stringIds + string * 4));
                    names.add(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    private static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        for (int n; (n = in.read(buffer)) > 0; ) bytes.write(buffer, 0, n);
        in.close();
        return bytes.toByteArray();
    }

    private static int u32(byte[] data, int offset) {
        return (data[offset] & 0xff) | (data[offset + 1] & 0xff) << 8 | (data[offset + 2] & 0xff) << 16
                | (data[offset + 3] & 0xff) << 24;
    }

    private static String mutf8(byte[] data, int offset) {
        while ((data[offset] & 0x80) != 0) offset++;  // skip the uleb128 UTF-16 length
        offset++;
        StringBuilder text = new StringBuilder();
        while (data[offset] != 0) {
            int a = data[offset++] & 0xff;
            if (a < 0x80) {
                text.append((char) a);
            } else if ((a & 0xe0) == 0xc0) {
                text.append((char) (((a & 0x1f) << 6) | (data[offset++] & 0x3f)));
            } else {
                int b = data[offset++] & 0x3f;
                text.append((char) (((a & 0x0f) << 12) | (b << 6) | (data[offset++] & 0x3f)));
            }
        }
        return text.toString();
    }
}
