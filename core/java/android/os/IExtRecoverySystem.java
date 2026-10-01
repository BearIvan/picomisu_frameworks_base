// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.os;

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;

import com.pico.util.IExtBase;

import java.io.IOException;
import java.util.List;

/**
 * PICO recovery-system extension (factory PICO OS 5.13.7 android.os.IExtRecoverySystem): maps
 * the path of an update package to the path recovery sees.
 * @hide
 */
public interface IExtRecoverySystem extends IExtBase {
    /**
     * /storage/emulated/... becomes /data/media/...; a path on the first public volume becomes
     * /sdcard/...; /cache and /data/media paths stay.
     */
    static String processFilePath(Context context, String originalFilePath) throws IOException {
        String desFilePath = originalFilePath;
        if (originalFilePath.startsWith("/storage/emulated/")) {
            desFilePath = originalFilePath.replace("/storage/emulated/", "/data/media/");
        } else if (!originalFilePath.startsWith("/cache")
                && !originalFilePath.startsWith("/data/media/")) {
            StorageManager sm = (StorageManager) context.getSystemService("storage");
            String externalSDCardRootPath = "";
            List<VolumeInfo> volumes = sm.getVolumes();
            if (volumes != null && volumes.size() > 0) {
                for (VolumeInfo volume : volumes) {
                    if (volume.getType() == VolumeInfo.TYPE_PUBLIC) {
                        if (volume.getPath() != null) {
                            externalSDCardRootPath = volume.getPath().getCanonicalPath();
                        } else {
                            Log.e("RecoverySystem", "Public volume path should not be null!"
                                    + "There must be something wrong!Check it!");
                        }
                        break;
                    }
                }
            }
            if (!TextUtils.isEmpty(externalSDCardRootPath)
                    && originalFilePath.startsWith(externalSDCardRootPath)) {
                desFilePath = originalFilePath.replace(externalSDCardRootPath, "/sdcard");
            }
        }
        Log.i("RecoverySystem", "processFilePath originalFilePath:" + originalFilePath
                + " file path after process:" + desFilePath);
        return desFilePath;
    }
}
