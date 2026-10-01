// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * Smartisan per-package uids for packages that run as the system uid, persisted in
 * /data/syslog/local/.system_uid. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SmtUidUtil {
    private static SmtUidStatus sSystemUidStatus = new SmtUidStatus();
    private static boolean sInitCompleted = false;

    public static int getSystemUidForPackage(String packageName) {
        if (!sInitCompleted) {
            readSystemUidStatusFromFile();
            sInitCompleted = true;
        }
        return sSystemUidStatus.getSystemUidForPackage(packageName);
    }

    public static HashMap<String, Integer> getSmtUidMap() {
        return new HashMap<>(sSystemUidStatus.mSystemUid);
    }

    public static void resetSmtUidIfNeeded() {
        sSystemUidStatus.resetSmtUidIfNeeded();
    }

    public static void readSystemUidStatusFromFile() {
        Object obj = readObjectFromFile("/data/syslog/local/.system_uid");
        if (obj != null) {
            try {
                sSystemUidStatus = (SmtUidStatus) obj;
            } catch (Exception e) {
                sSystemUidStatus = new SmtUidStatus();
            }
        }
    }

    public static void writeSystemUidStatusToFile() {
        writeObjectToFile("/data/syslog/local/.system_uid", sSystemUidStatus);
    }

    private static void writeObjectToFile(String fileName, Object obj) {
        FileOutputStream out = null;
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(obj);
            objOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Object readObjectFromFile(String fileName) {
        Object temp = null;
        File file = new File(fileName);
        ObjectInputStream objIn = null;
        if (file.exists()) {
            try {
                objIn = new ObjectInputStream(new FileInputStream(file));
                temp = objIn.readObject();
                objIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (objIn != null) {
                    try {
                        objIn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return temp;
    }
}
