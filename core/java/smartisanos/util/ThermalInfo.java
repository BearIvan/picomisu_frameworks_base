// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Board temperature from the quiet-therm thermal zone. Reconstructed from the PICO OS 5.13.7
 * factory framework.
 *
 * @hide
 */
public class ThermalInfo {
    private static final String TAG = "ThermalInfo";
    private static final String THERMAL_BASE_PATH = "/sys/class/thermal/";
    private static final String QUIET_THERMAL = "quiet-therm-adc";
    private static String mBoardTempPath = null;

    public static String getBoardTemperature() {
        if (mBoardTempPath == null && !getBoardTempPath()) {
            Log.e(TAG, "NOT SUPPORT GET BOARD TEMPERATURE FEATURE");
            return "NOT SUPPORT";
        }
        return getSysNodeFileContent(mBoardTempPath);
    }

    public static boolean getBoardTempPath() {
        File thermalFile = new File(THERMAL_BASE_PATH);
        File[] thermalFiles = thermalFile.listFiles();
        for (File file : thermalFiles) {
            if (file.getName().startsWith("thermal_zone")) {
                String path = THERMAL_BASE_PATH + file.getName();
                if (QUIET_THERMAL.equals(getSysNodeFileContent(path + "/type"))) {
                    mBoardTempPath = path + "/temp";
                    return true;
                }
            }
        }
        return false;
    }

    private static String getSysNodeFileContent(String path) {
        File file = new File(path);
        String result = null;
        BufferedReader reader = null;
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            result = reader.readLine();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getSysNodeFileContent", e);
        } catch (IOException e) {
            Log.e(TAG, "getSysNodeFileContent", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
