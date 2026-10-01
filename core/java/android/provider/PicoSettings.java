// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Client of the PICO settings provider ("com.pico.settings"): the app white list (type "vr"
 * etc.) and black list (factory PICO OS 5.13.7 framework android.provider.PicoSettings).
 * @hide
 */
public class PicoSettings {
    public static final String AUTHORITY = "com.pico.settings";
    private static final String TAG = "PicoSettings";

    /** @hide */
    public static final class WhiteList implements BaseColumns {
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_CLASS_NAME = "classname";
        public static final String COLUMN_PACKAGE_NAME = "packagename";
        public static final String COLUMN_TYPE = "type";
        public static final Uri CONTENT_URI = Uri.parse("content://com.pico.settings/whitelist");
        public static final String TABLE_NAME = "whitelist";

        /** Type of the package (or package and class) in the white list, or null. */
        public static String getType(ContentResolver cr, String packageName, String className) {
            if (TextUtils.isEmpty(packageName)) {
                Log.w(TAG, "packageName could not be null or empty!");
                return null;
            }
            String selection = "packagename = ?";
            String[] selectionArgs = {packageName};
            if (!TextUtils.isEmpty(className)) {
                selection = "packagename = ? and classname = ?";
                selectionArgs = new String[]{packageName, className};
            }
            Cursor c = null;
            try {
                c = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
                if (c == null || !c.moveToFirst()) {
                    return null;
                }
                return c.getString(c.getColumnIndex(COLUMN_TYPE));
            } catch (Exception e) {
                Log.e(TAG, "PicoSettings getType occurs Exception!", e);
                return null;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        /** White list entries matching the given non-empty fields, as JSON, or null. */
        public static String query(ContentResolver contentResolver, String packageName,
                String className, String type) {
            String selection = null;
            ArrayList<String> args = new ArrayList<>();
            if (!TextUtils.isEmpty(packageName)) {
                if (TextUtils.isEmpty(selection)) {
                    selection = "packagename = ?";
                }
                args.add(packageName);
            }
            if (!TextUtils.isEmpty(className)) {
                selection = TextUtils.isEmpty(selection) ? "classname = ?"
                        : selection + " and classname = ?";
                args.add(className);
            }
            if (!TextUtils.isEmpty(type)) {
                selection = TextUtils.isEmpty(selection) ? "type = ?"
                        : selection + " and type = ?";
                args.add(type);
            }
            String[] selectionArgs = null;
            if (args.size() > 0) {
                selectionArgs = new String[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    selectionArgs[i] = args.get(i);
                }
            }
            JSONArray items = new JSONArray();
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(CONTENT_URI, null, selection, selectionArgs, null);
                while (cursor.moveToNext()) {
                    String pkg = cursor.getString(
                            cursor.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME));
                    String cls = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_NAME));
                    String t = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                    JSONObject item;
                    try {
                        item = new JSONObject();
                        item.put("package_name", pkg);
                        item.put("class_name", cls);
                        item.put("type", t);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        item = null;
                    }
                    items.put(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot query from " + CONTENT_URI, e);
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
            if (items.length() <= 0) {
                Log.e(TAG, "Query result is empty.");
                return null;
            }
            JSONObject result = new JSONObject();
            try {
                result.put("whitelist_items", items);
            } catch (JSONException e) {
                Log.e(TAG, "Exception occurs when query--organize json data");
                e.printStackTrace();
            }
            if (result.has("whitelist_items")) {
                return result.toString();
            }
            if (cursor != null) {
                cursor.close();
            }
            Log.e(TAG, "Put json data to JsonObject failed!");
            return null;
        }

        public static boolean insert(ContentResolver cr, String packageName, String className,
                String type) {
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException(
                        "PackageName could not be null when insert data to database.");
            }
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_PACKAGE_NAME, packageName);
                cv.put(COLUMN_CLASS_NAME, className);
                cv.put(COLUMN_TYPE, TextUtils.isEmpty(type) ? "vr" : type);
                cv.put(COLUMN_CATEGORY, "user");
                cr.insert(CONTENT_URI, cv);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Can not insert packageName " + packageName + " className " + className
                        + " type " + type + " to database.", e);
                return false;
            }
        }

        public static boolean update(ContentResolver cr, String oldPackageName,
                String oldClassName, String newPackageName, String newClassName, String newType) {
            if (TextUtils.isEmpty(oldPackageName)) {
                throw new IllegalArgumentException("Both oldPackageName or newPackageName can not"
                        + " be null when update data to database.");
            }
            String selection = "packagename = ?";
            String[] selectionArgs = {oldPackageName};
            if (!TextUtils.isEmpty(oldClassName)) {
                selection = "packagename = ? and classname = ?";
                selectionArgs = new String[]{oldPackageName, oldClassName};
            }
            try {
                ContentValues cv = new ContentValues();
                if (!TextUtils.isEmpty(newPackageName)) {
                    cv.put(COLUMN_PACKAGE_NAME, newPackageName);
                }
                if (!TextUtils.isEmpty(newClassName)) {
                    cv.put(COLUMN_CLASS_NAME, newClassName);
                }
                cv.put(COLUMN_TYPE, newType);
                cr.update(CONTENT_URI, cv, selection, selectionArgs);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Can not update oldPackageName " + oldPackageName + " oldClassName "
                        + oldClassName + "to newPackageName " + newPackageName + " newClassName "
                        + newClassName + " newType " + newType + " to database.", e);
                return false;
            }
        }

        public static int delete(ContentResolver cr, String packageName, String className) {
            String selection = null;
            String[] selectionArgs = null;
            if (!TextUtils.isEmpty(packageName)) {
                selection = "packagename = ?";
                selectionArgs = new String[]{packageName};
            }
            if (!TextUtils.isEmpty(className)) {
                if (TextUtils.isEmpty(packageName)) {
                    selection = "classname = ?";
                    selectionArgs = new String[]{className};
                } else {
                    selection = selection + " and classname = ?";
                    selectionArgs = new String[]{packageName, className};
                }
            }
            try {
                return cr.delete(CONTENT_URI, selection, selectionArgs);
            } catch (Exception e) {
                Log.e(TAG, "Can not delete packageName " + packageName + " className " + className
                        + " from database.", e);
                return 0;
            }
        }
    }

    /** @hide */
    public static final class BlackList implements BaseColumns {
        private static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_CLASS_NAME = "classname";
        public static final String COLUMN_PACKAGE_NAME = "packagename";
        public static final String COLUMN_VERSION_CODE = "versioncode";
        public static final String COLUMN_VERSION_NAME = "versionname";
        public static final Uri CONTENT_URI = Uri.parse("content://com.pico.settings/blacklist");
        public static final String TABLE_NAME = "blacklist";

        public static boolean insert(ContentResolver cr, String packageName, String className,
                String versionCode, String versionName) {
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException(
                        "PackageName could not be null when insert data to database.");
            }
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_PACKAGE_NAME, packageName);
                cv.put(COLUMN_CLASS_NAME, className);
                cv.put(COLUMN_VERSION_CODE, versionCode);
                cv.put(COLUMN_VERSION_NAME, versionName);
                cv.put(COLUMN_CATEGORY, "user");
                cr.insert(CONTENT_URI, cv);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Can not insert packageName " + packageName + " className " + className
                        + " versionCode " + versionCode + " versionName" + versionName
                        + " to database.", e);
                return false;
            }
        }

        public static int delete(ContentResolver cr, String packageName, String className) {
            String selection = null;
            String[] selectionArgs = null;
            if (!TextUtils.isEmpty(packageName)) {
                selection = "packagename = ?";
                selectionArgs = new String[]{packageName};
            }
            if (!TextUtils.isEmpty(className)) {
                if (TextUtils.isEmpty(packageName)) {
                    selection = "classname = ?";
                    selectionArgs = new String[]{className};
                } else {
                    selection = selection + " and classname = ?";
                    selectionArgs = new String[]{packageName, className};
                }
            }
            try {
                return cr.delete(CONTENT_URI, selection, selectionArgs);
            } catch (Exception e) {
                Log.e(TAG, "Can not delete packageName " + packageName + " className " + className
                        + " from database.", e);
                return 0;
            }
        }

        public static boolean update(ContentResolver cr, String oldPackageName,
                String oldClassName, String newPackageName, String newClassName,
                String newVersionCode, String newVersionName) {
            if (TextUtils.isEmpty(oldPackageName)) {
                throw new IllegalArgumentException(
                        "oldPackageName can not be null when update data to database.");
            }
            String selection = "packagename = ?";
            String[] selectionArgs = {oldPackageName};
            if (!TextUtils.isEmpty(oldClassName)) {
                selection = "packagename = ? and classname = ?";
                selectionArgs = new String[]{oldPackageName, oldClassName};
            }
            try {
                ContentValues cv = new ContentValues();
                if (!TextUtils.isEmpty(newPackageName)) {
                    cv.put(COLUMN_PACKAGE_NAME, newPackageName);
                }
                if (!TextUtils.isEmpty(newClassName)) {
                    cv.put(COLUMN_CLASS_NAME, newClassName);
                }
                if (!TextUtils.isEmpty(newVersionCode)) {
                    cv.put(COLUMN_VERSION_CODE, newVersionCode);
                }
                if (!TextUtils.isEmpty(newVersionName)) {
                    cv.put(COLUMN_VERSION_NAME, newVersionName);
                }
                cr.update(CONTENT_URI, cv, selection, selectionArgs);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Can not update oldPackageName " + oldPackageName + " oldClassName "
                        + oldClassName + "to newPackageName " + newPackageName + " newClassName "
                        + newClassName + " newVersionCode " + newVersionCode + " newVersionName "
                        + newVersionName + " to database.", e);
                return false;
            }
        }

        /** Black list entries matching the given non-empty fields, as JSON, or null. */
        public static String query(ContentResolver contentResolver, String packageName,
                String className, String versionCode, String versionName) {
            String selection = null;
            ArrayList<String> args = new ArrayList<>();
            if (!TextUtils.isEmpty(packageName)) {
                if (TextUtils.isEmpty(selection)) {
                    selection = "packagename = ?";
                }
                args.add(packageName);
            }
            if (!TextUtils.isEmpty(className)) {
                selection = TextUtils.isEmpty(selection) ? "classname = ?"
                        : selection + " and classname = ?";
                args.add(className);
            }
            if (!TextUtils.isEmpty(versionCode)) {
                selection = TextUtils.isEmpty(selection) ? "versioncode = ?"
                        : selection + " and versioncode = ?";
                args.add(versionCode);
            }
            if (!TextUtils.isEmpty(versionName)) {
                selection = TextUtils.isEmpty(selection) ? "versionname = ?"
                        : selection + " and versionname = ?";
                args.add(versionName);
            }
            String[] selectionArgs = null;
            if (args.size() > 0) {
                selectionArgs = new String[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    selectionArgs[i] = args.get(i);
                }
            }
            JSONArray items = new JSONArray();
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(CONTENT_URI, null, selection, selectionArgs, null);
                while (cursor.moveToNext()) {
                    String pkg = cursor.getString(
                            cursor.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME));
                    String cls = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_NAME));
                    String code = cursor.getString(
                            cursor.getColumnIndexOrThrow(COLUMN_VERSION_CODE));
                    String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(COLUMN_VERSION_NAME));
                    JSONObject item;
                    try {
                        item = new JSONObject();
                        item.put("package_name", pkg);
                        item.put("class_name", cls);
                        item.put("version_code", code);
                        item.put("version_name", name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        item = null;
                    }
                    items.put(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot query from " + CONTENT_URI, e);
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
            if (items.length() <= 0) {
                Log.e(TAG, "Query result is empty.");
                return null;
            }
            JSONObject result = new JSONObject();
            try {
                result.put("blacklist_items", items);
            } catch (JSONException e) {
                Log.e(TAG, "Exception occurs when query--organize json data");
                e.printStackTrace();
            }
            if (result.has("blacklist_items")) {
                return result.toString();
            }
            if (cursor != null) {
                cursor.close();
            }
            Log.e(TAG, "Put json data to JsonObject failed!");
            return null;
        }
    }
}
