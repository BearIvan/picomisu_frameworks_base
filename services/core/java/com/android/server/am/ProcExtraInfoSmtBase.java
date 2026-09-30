// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.system.Os;

import smartisanos.util.FeatLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Smartisan per-process "last word" store. Reconstructed from the PICO OS 5.13.7 factory
 * services; only the members reached by {@code IActivityManagerSmtEx.getLastword} are present.
 *
 * @hide
 */
public class ProcExtraInfoSmtBase {
    public static final String TAG = "ProcExtraInfo";
    public static final String LASTWORD_TAG = "FEAT_LASTWORD";

    private HashMap<Integer, LastWord> mLastWordMap = new HashMap<>();

    private ProcExtraInfoSmtBase() {
    }

    private static class Inner {
        private static final ProcExtraInfoSmtBase instance = new ProcExtraInfoSmtBase();
    }

    public static ProcExtraInfoSmtBase getInstance() {
        return Inner.instance;
    }

    private static class LastWord {
        private int mPid;
        private Date mTime;
        private String mCmdline;
        private String mWords;

        LastWord(int pid, Date time, String cmdline, String words) {
            mPid = pid;
            mTime = time;
            mCmdline = cmdline;
            mWords = words;
        }
    }

    public String getLastWordOfPid(int pid) {
        FeatLog.i(TAG, LASTWORD_TAG, 30, "pid=" + Os.getpid() + " try to get lastword of " + pid);
        LastWord emptyOne = new LastWord(-1, new Date(), "", "");
        synchronized (ProcExtraInfoSmtBase.class) {
            if (!mLastWordMap.containsKey(pid)) {
                FeatLog.e(TAG, LASTWORD_TAG, 40, "This proc hasn't set any lastword yet.");
                return null;
            }
            LastWord lastWord = mLastWordMap.getOrDefault(pid, emptyOne);
            mLastWordMap.remove(pid);
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String time = ft.format(lastWord.mTime);
            return time + " " + lastWord.mPid + " " + lastWord.mCmdline + " " + lastWord.mWords;
        }
    }
}
