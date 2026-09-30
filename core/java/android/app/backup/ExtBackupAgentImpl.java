// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app.backup;

import android.app.backup.FullBackup.BackupScheme.PathWithRequiredFlags;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PICO backup agent extension. A PICO backup request carries "domain:path" include/exclude
 * entries; when present they replace the manifest backup rules of the agent.
 * @hide
 */
public class ExtBackupAgentImpl implements IExtBackupAgent {
    private static final String TAG = "BackupAgent";
    private BackupAgent mBase;

    public ExtBackupAgentImpl(BackupAgent base) {
        mBase = base;
    }

    @Override
    public boolean backupByPaths(List<String> includePaths, List<String> excludePaths) {
        return (includePaths != null && !includePaths.isEmpty())
                || (excludePaths != null && !excludePaths.isEmpty());
    }

    @Override
    public void onFullBackup(BackupAgent agent, FullBackupDataOutput data,
            List<String> includePaths, List<String> excludePaths) throws IOException {
        if (!backupByPaths(includePaths, excludePaths)) {
            agent.onFullBackup(data);
            return;
        }

        FullBackup.BackupScheme backupScheme = FullBackup.getBackupScheme(mBase);
        Map<String, Set<PathWithRequiredFlags>> manifestIncludeMap = new ArrayMap<>();
        for (String domainAndPath : includePaths) {
            if (TextUtils.isEmpty(domainAndPath) || !domainAndPath.contains(":")) {
                Log.w(TAG, "include domainAndPath " + domainAndPath + " is Invalid !");
                continue;
            }
            String[] split = domainAndPath.split(":");
            if (split == null || split.length != 2) {
                Log.w(TAG, "include split domainAndPath " + domainAndPath + " is Invalid !");
                continue;
            }
            Log.i(TAG, "include domain " + split[0] + " filePath " + split[1]);
            String domainToken = backupScheme.getTokenForXmlDomain(split[0]);
            if (TextUtils.isEmpty(domainToken)) {
                Log.w(TAG, "include domainToken " + domainToken + " is Invalid !");
                continue;
            }
            if (TextUtils.isEmpty(split[1]) || split[1].contains("..")
                    || split[1].contains("//")) {
                Log.w(TAG, "include filePath " + split[1] + " is Invalid !");
                continue;
            }
            File domainDirectory = backupScheme.getDirectoryForCriteriaDomain(split[0]);
            if (domainDirectory == null) {
                Log.w(TAG, "include domain=" + split[0] + "getDirectory invalid; skipping");
                continue;
            }
            File canonicalFile = new File(domainDirectory, split[1]);
            Set<PathWithRequiredFlags> includeSet = manifestIncludeMap.get(split[0]);
            if (includeSet == null) {
                includeSet = new ArraySet<>();
                manifestIncludeMap.put(domainToken, includeSet);
            }
            Log.w(TAG, "add include PathWithRequiredFlags path=" + canonicalFile.getCanonicalPath()
                    + " domainToken=" + split[0]);
            includeSet.add(new PathWithRequiredFlags(canonicalFile.getCanonicalPath(), 0));
            if ("database".equals(domainToken) && !canonicalFile.isDirectory()) {
                String canonicalJournalPath = canonicalFile.getCanonicalPath() + "-journal";
                includeSet.add(new PathWithRequiredFlags(canonicalJournalPath, 0));
                Log.w(TAG, "include ...automatically generated " + canonicalJournalPath
                        + ". Ignore if nonexistent.");
                String canonicalWalPath = canonicalFile.getCanonicalPath() + "-wal";
                includeSet.add(new PathWithRequiredFlags(canonicalWalPath, 0));
                Log.w(TAG, "include ...automatically generated " + canonicalWalPath
                        + ". Ignore if nonexistent.");
            }
            if ("sharedpref".equals(domainToken) && !canonicalFile.isDirectory()
                    && !canonicalFile.getCanonicalPath().endsWith(".xml")) {
                String canonicalXmlPath = canonicalFile.getCanonicalPath() + ".xml";
                includeSet.add(new PathWithRequiredFlags(canonicalXmlPath, 0));
                Log.w(TAG, "include ...automatically generated " + canonicalXmlPath
                        + ". Ignore if nonexistent.");
            }
        }

        ArraySet<PathWithRequiredFlags> manifestExcludeSet = new ArraySet<>();
        for (String domainAndPath : excludePaths) {
            if (TextUtils.isEmpty(domainAndPath) || !domainAndPath.contains(":")) {
                Log.w(TAG, "exclude domainAndPath " + domainAndPath + " is Invalid !");
                continue;
            }
            String[] split = domainAndPath.split(":");
            if (split == null || split.length != 2) {
                Log.w(TAG, "exclude split domainAndPath " + domainAndPath + " is Invalid !");
                continue;
            }
            Log.i(TAG, "exclude domain " + split[0] + " filePath " + split[1]);
            String domainToken = backupScheme.getTokenForXmlDomain(split[0]);
            if (TextUtils.isEmpty(domainToken)) {
                Log.w(TAG, "exclude domainToken " + domainToken + " is Invalid !");
                continue;
            }
            if (TextUtils.isEmpty(split[1]) || split[1].contains("..")
                    || split[1].contains("//")) {
                Log.w(TAG, "exclude filePath " + split[1] + " is Invalid !");
                continue;
            }
            File domainDirectory = backupScheme.getDirectoryForCriteriaDomain(split[0]);
            if (domainDirectory == null) {
                Log.w(TAG, "exclude domain=" + split[0] + "getDirectory invalid; skipping");
                continue;
            }
            File canonicalFile = new File(domainDirectory, split[1]);
            Log.w(TAG, "add exclude PathWithRequiredFlags path=" + canonicalFile.getCanonicalPath()
                    + " domainToken=" + split[0]);
            manifestExcludeSet.add(new PathWithRequiredFlags(canonicalFile.getCanonicalPath(), 0));
            if ("database".equals(domainToken) && !canonicalFile.isDirectory()) {
                String canonicalJournalPath = canonicalFile.getCanonicalPath() + "-journal";
                manifestExcludeSet.add(new PathWithRequiredFlags(canonicalJournalPath, 0));
                Log.w(TAG, "exclude ...automatically generated " + canonicalJournalPath
                        + ". Ignore if nonexistent.");
                String canonicalWalPath = canonicalFile.getCanonicalPath() + "-wal";
                manifestExcludeSet.add(new PathWithRequiredFlags(canonicalWalPath, 0));
                Log.w(TAG, "exclude ...automatically generated " + canonicalWalPath
                        + ". Ignore if nonexistent.");
            }
            if ("sharedpref".equals(domainToken) && !canonicalFile.isDirectory()
                    && !canonicalFile.getCanonicalPath().endsWith(".xml")) {
                String canonicalXmlPath = canonicalFile.getCanonicalPath() + ".xml";
                manifestExcludeSet.add(new PathWithRequiredFlags(canonicalXmlPath, 0));
                Log.w(TAG, "exclude ...automatically generated " + canonicalXmlPath
                        + ". Ignore if nonexistent.");
            }
        }

        final String packageName = mBase.getPackageName();
        final ApplicationInfo appInfo = mBase.getApplicationInfo();

        // System apps have control over where their default storage context
        // is pointed, so we're always explicit when building paths.
        final Context ceContext = mBase.createCredentialProtectedStorageContext();
        final String rootDir = ceContext.getDataDir().getCanonicalPath();
        final String filesDir = ceContext.getFilesDir().getCanonicalPath();
        final String noBackupDir = ceContext.getNoBackupFilesDir().getCanonicalPath();
        final String databaseDir = ceContext.getDatabasePath("foo").getParentFile()
                .getCanonicalPath();
        final String sharedPrefsDir = ceContext.getSharedPreferencesPath("foo").getParentFile()
                .getCanonicalPath();
        final String cacheDir = ceContext.getCacheDir().getCanonicalPath();
        final String codeCacheDir = ceContext.getCodeCacheDir().getCanonicalPath();

        final Context deContext = mBase.createDeviceProtectedStorageContext();
        final String deviceRootDir = deContext.getDataDir().getCanonicalPath();
        final String deviceFilesDir = deContext.getFilesDir().getCanonicalPath();
        final String deviceNoBackupDir = deContext.getNoBackupFilesDir().getCanonicalPath();
        final String deviceDatabaseDir = deContext.getDatabasePath("foo").getParentFile()
                .getCanonicalPath();
        final String deviceSharedPrefsDir = deContext.getSharedPreferencesPath("foo")
                .getParentFile().getCanonicalPath();
        final String deviceCacheDir = deContext.getCacheDir().getCanonicalPath();
        final String deviceCodeCacheDir = deContext.getCodeCacheDir().getCanonicalPath();

        final String libDir = (appInfo.nativeLibraryDir != null)
                ? new File(appInfo.nativeLibraryDir).getCanonicalPath()
                : null;

        // Directories the framework never backs up, whatever the requested paths are.
        final ArraySet<String> traversalExcludeSet = new ArraySet<String>();

        traversalExcludeSet.add(filesDir);
        traversalExcludeSet.add(noBackupDir);
        traversalExcludeSet.add(databaseDir);
        traversalExcludeSet.add(sharedPrefsDir);
        traversalExcludeSet.add(cacheDir);
        traversalExcludeSet.add(codeCacheDir);

        traversalExcludeSet.add(deviceFilesDir);
        traversalExcludeSet.add(deviceNoBackupDir);
        traversalExcludeSet.add(deviceDatabaseDir);
        traversalExcludeSet.add(deviceSharedPrefsDir);
        traversalExcludeSet.add(deviceCacheDir);
        traversalExcludeSet.add(deviceCodeCacheDir);

        if (libDir != null) {
            traversalExcludeSet.add(libDir);
        }

        // Root dir first.
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.ROOT_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(rootDir);

        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.DEVICE_ROOT_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(deviceRootDir);

        // Data dir next.
        traversalExcludeSet.remove(filesDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.FILES_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(filesDir);

        traversalExcludeSet.remove(deviceFilesDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.DEVICE_FILES_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(deviceFilesDir);

        // Database directory.
        traversalExcludeSet.remove(databaseDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.DATABASE_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(databaseDir);

        traversalExcludeSet.remove(deviceDatabaseDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.DEVICE_DATABASE_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(deviceDatabaseDir);

        // SharedPrefs.
        traversalExcludeSet.remove(sharedPrefsDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.SHAREDPREFS_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(sharedPrefsDir);

        traversalExcludeSet.remove(deviceSharedPrefsDir);
        mBase.applyXmlFiltersAndDoFullBackupForDomain(
                packageName, FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN, manifestIncludeMap,
                manifestExcludeSet, traversalExcludeSet, data);
        traversalExcludeSet.add(deviceSharedPrefsDir);

        // getExternalFilesDir() location associated with this app; processes running as the
        // system UID are not permitted to access external storage.
        if (Process.myUid() != Process.SYSTEM_UID) {
            File efLocation = mBase.getExternalFilesDir(null);
            if (efLocation != null) {
                mBase.applyXmlFiltersAndDoFullBackupForDomain(
                        packageName, FullBackup.MANAGED_EXTERNAL_TREE_TOKEN, manifestIncludeMap,
                        manifestExcludeSet, traversalExcludeSet, data);
            }
        }
    }
}
