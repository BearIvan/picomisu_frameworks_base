// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.util.ArraySet;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public class AlarmManagerServiceSmt {
    public static Vector<ArrayList<TriggerAlarmInfo>> sTriggerAlarmInfos = new Vector<>();

    public static class TriggerAlarmInfo {
        public int count;
        public String packageName;
        public Set<String> tags = new ArraySet();

        public TriggerAlarmInfo(String packageName, int count) {
            this.packageName = packageName;
            this.count = count;
        }

        public String toString() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("packageName", this.packageName);
                jsonObject.put("count", this.count);
                jsonObject.put("tags", this.tags);
                return jsonObject.toString();
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public static void uploadTriggerAlarmInfos() {
        if (sTriggerAlarmInfos.size() == 0) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("triggerAlarmInfos", sTriggerAlarmInfos);
            sTriggerAlarmInfos.clear();
        } catch (JSONException e) {
        }
    }
}
