// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app.backup;

import com.pico.util.IExtBase;

import java.io.IOException;
import java.util.List;

/**
 * PICO backup agent extension: full backup restricted to the include/exclude paths of a PICO
 * backup request.
 * @hide
 */
public interface IExtBackupAgent extends IExtBase {
    default boolean backupByPaths(List<String> includePaths, List<String> excludePaths) {
        return false;
    }

    void onFullBackup(BackupAgent agent, FullBackupDataOutput data, List<String> includePaths,
            List<String> excludePaths) throws IOException;
}
