// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.system.Os;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import smartisanos.util.FeatLog;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ProcExtraInfoSmtBase {
    public static final String LASTWORD_TAG = "FEAT_LASTWORD";
    public static final String TAG = "ProcExtraInfo";
    private HashMap<Integer, LastWord> mLastWordMap;

    private static class Inner {
        private static final ProcExtraInfoSmtBase instance = new ProcExtraInfoSmtBase();

        private Inner() {
        }
    }

    private ProcExtraInfoSmtBase() {
        this.mLastWordMap = new HashMap<>();
    }

    public static ProcExtraInfoSmtBase getInstance() {
        return Inner.instance;
    }

    private static class LastWord {
        private String mCmdline;
        private int mPid;
        private Date mTime;
        private String mWords;

        LastWord(int pid, Date time, String cmdline, String words) {
            this.mPid = pid;
            this.mTime = time;
            this.mCmdline = cmdline;
            this.mWords = words;
        }
    }

    private String getPidCmdline(int pid) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String cmdline = reader.readLine().trim();
            return cmdline;
        } catch (Exception e) {
            e.printStackTrace();
            return "<unknown>";
        }
    }

    public void setLastWord(int pid, String word) {
        FeatLog.i(TAG, LASTWORD_TAG, 0, pid + " setting lastword: " + word);
        Date now = new Date();
        String cmdline = getPidCmdline(pid);
        LastWord lastWord = new LastWord(pid, now, cmdline, word);
        synchronized (ProcExtraInfoSmtBase.class) {
            if (this.mLastWordMap.containsKey(Integer.valueOf(pid)) && this.mLastWordMap.get(Integer.valueOf(pid)).mCmdline.equals(cmdline)) {
                LastWord oldLastWord = this.mLastWordMap.get(Integer.valueOf(pid));
                FeatLog.i(TAG, LASTWORD_TAG, 10, "pid=" + pid + " cmdline=" + cmdline + " has set lastword before:");
                StringBuilder sb = new StringBuilder();
                sb.append(oldLastWord.mTime);
                sb.append(" ");
                sb.append(oldLastWord.mWords);
                FeatLog.i(TAG, LASTWORD_TAG, 10, sb.toString());
            }
            this.mLastWordMap.put(Integer.valueOf(pid), lastWord);
            FeatLog.i(TAG, LASTWORD_TAG, 20, "pid=" + pid + " cmdline=" + cmdline + " set lastword.");
        }
    }

    public String getLastWordOfPid(int pid) {
        FeatLog.i(TAG, LASTWORD_TAG, 30, "pid=" + Os.getpid() + " try to get lastword of " + pid);
        LastWord emptyOne = new LastWord(-1, new Date(), "", "");
        synchronized (ProcExtraInfoSmtBase.class) {
            if (!this.mLastWordMap.containsKey(Integer.valueOf(pid))) {
                FeatLog.e(TAG, LASTWORD_TAG, 40, "This proc hasn't set any lastword yet.");
                return null;
            }
            LastWord lastWord = this.mLastWordMap.getOrDefault(Integer.valueOf(pid), emptyOne);
            this.mLastWordMap.remove(Integer.valueOf(pid));
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String time = ft.format(lastWord.mTime);
            return time + " " + lastWord.mPid + " " + lastWord.mCmdline + " " + lastWord.mWords;
        }
    }
}
