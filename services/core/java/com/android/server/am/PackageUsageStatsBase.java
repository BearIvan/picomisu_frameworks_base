// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.Handler;
import android.util.Log;
import android.util.Slog;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import smartisanos.util.SmtRingBuffer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class PackageUsageStatsBase {
    static final int HistoryCount = 10;
    protected static final int UPDATE_PACKAGE_STATS_DURATION = 5000;
    static long totalActive;
    static int totalCount;
    static final int unit10m = 600000;
    static final int unit1h = 3600000;
    static final int unit1m = 60000;
    static final int unit24h = 86400000;
    static final int unit2h = 7200000;
    static final int unit30m = 1800000;
    static final int unit5m = 300000;
    Handler mHandler;
    ArrayList<PackageUsageInfo> mLruStats = new ArrayList<>(50);
    SmtUidDictionaryExt<PackageUsageInfo> sud = new SmtUidDictionaryExt<>();
    static final String TAG = "PackUsage";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);

    static class PackageUsageInfo {
        History current;
        History oldHistories;
        String packageName;
        History pendinghis;
        long resumeStart;
        int uid;
        int order = 1;
        int weight = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        SmtRingBuffer<History> history = new SmtRingBuffer<>(10);

        static class History {
            long active;
            int count;
            long resumeEnd;

            History() {
            }

            History(int c, long a, long r) {
                this.count = c;
                this.active = a;
                this.resumeEnd = r;
            }

            History(History o) {
                this.count = o.count;
                this.active = o.active;
                this.resumeEnd = o.resumeEnd;
            }

            void reset(int c, long a, long r) {
                this.count = c;
                this.active = a;
                this.resumeEnd = r;
            }

            void add(History o, int countLimit) {
                this.active += o.active;
                this.resumeEnd = o.resumeEnd;
                int i = this.count;
                if (i < countLimit) {
                    this.count = i + o.count;
                } else {
                    this.active = (this.active * ((long) countLimit)) / ((long) (countLimit + 1));
                }
            }

            public String toString() {
                return "PUI.History{ count=" + this.count + ", active=" + this.active + ", resumeEnd=" + this.resumeEnd + " }";
            }
        }

        public void updatePackageActive(long when, boolean resumed) {
            History history;
            History item;
            boolean hisFull = this.history.isFull();
            if (resumed) {
                if (hisFull) {
                    synchronized (this.history) {
                        item = (History) this.history.get(0);
                    }
                    History history2 = this.oldHistories;
                    if (history2 == null) {
                        this.oldHistories = new History(item);
                        if (PackageUsageStatsBase.DEBUG) {
                            Slog.d(PackageUsageStatsBase.TAG, "updatePackageActive new history item for" + this.packageName);
                        }
                    } else {
                        history2.add(item, 10);
                    }
                    PackageUsageStatsBase.totalCount -= item.count;
                    PackageUsageStatsBase.totalActive -= item.active;
                } else {
                    item = new History();
                }
                this.current = item;
                this.resumeStart = when;
                return;
            }
            long delta = when - this.resumeStart;
            if (delta >= 0 && (history = this.current) != null) {
                history.reset(1, delta, when);
                History tmp = this.pendinghis;
                if (tmp != null) {
                    tmp.add(this.current, 10);
                } else {
                    this.pendinghis = this.current;
                }
                PackageUsageStatsBase.totalActive += delta;
                PackageUsageStatsBase.totalCount++;
            } else {
                Slog.w(PackageUsageStatsBase.TAG, "error in updatePackageActive, when=" + when + ", resumed=" + resumed + ", resumeStart=" + this.resumeStart);
            }
            this.current = null;
            this.resumeStart = -1L;
        }

        PackageUsageInfo(long start, String name, int userId) {
            this.resumeStart = -1L;
            this.uid = userId;
            this.packageName = name;
            this.resumeStart = start;
        }

        private int computeFactor(long resumeDelta, long span) {
            if (span <= 3600000) {
                return 291;
            }
            if (resumeDelta == 0) {
                return (int) ((30000 * span) / 86400000);
            }
            if (resumeDelta < 60000) {
                return (int) ((16000 * span) / 86400000);
            }
            if (resumeDelta < unit5m) {
                return (int) ((13000 * span) / 86400000);
            }
            if (resumeDelta < 600000) {
                return (int) ((12000 * span) / 86400000);
            }
            if (resumeDelta < 1800000) {
                return (int) ((11000 * span) / 86400000);
            }
            if (resumeDelta > unit2h) {
                return (int) ((8000 * span) / 86400000);
            }
            return (int) ((JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY * span) / 86400000);
        }

        int computeWeight(long when, History item) {
            long resumeDelta = when - item.resumeEnd;
            long span = 86400000 - resumeDelta;
            int count = item.count;
            if (count > 1) {
                int fact = ((10 - count) * 10) / 10;
                if (fact < 2) {
                    fact = 2;
                }
                span = (((long) fact) * span) / 10;
                if (PackageUsageStatsBase.DEBUG) {
                    Slog.d(PackageUsageStatsBase.TAG, "reduce span of oldHistories fact=" + fact);
                }
            }
            int factor = computeFactor(resumeDelta, span);
            int wcount = 0;
            if (PackageUsageStatsBase.totalCount != 0) {
                wcount = (factor * count) / PackageUsageStatsBase.totalCount;
            }
            int wactive = 0;
            if (PackageUsageStatsBase.totalActive != 0) {
                wactive = (int) ((((long) factor) * item.active) / PackageUsageStatsBase.totalActive);
            }
            int res = wcount + wactive;
            if (PackageUsageStatsBase.DEBUG) {
                Slog.d(PackageUsageStatsBase.TAG, "compute history weight of: " + this.packageName + ", factor=" + factor + ", span=" + span + ", one weight=" + res + "(+" + wcount + ", +" + wactive + "), item=" + item + ", uid=" + this.uid);
            }
            return res;
        }

        void updateWeight(long when) {
            int i = PackageUsageStatsBase.totalCount;
            long j = PackageUsageStatsBase.totalActive;
            if (this.pendinghis != null) {
                synchronized (this.history) {
                    this.history.add(this.pendinghis);
                }
                this.pendinghis = null;
            }
            int N = this.history.size();
            this.weight = 0;
            for (int i2 = 0; i2 < N; i2++) {
                History item = (History) this.history.get(i2);
                this.weight += computeWeight(when, item);
            }
            History tmp = this.current;
            if (tmp != null) {
                long j2 = this.resumeStart;
                if (j2 > 0) {
                    tmp.reset(1, when - j2, when);
                    this.weight += computeWeight(when, tmp);
                }
            }
            History history = this.oldHistories;
            if (history != null) {
                this.weight += computeWeight(when, history);
            }
            if (PackageUsageStatsBase.DEBUG) {
                Slog.d(PackageUsageStatsBase.TAG, "updateWeight weight=" + this.weight);
            }
        }

        boolean isRecent(long when, long delta) {
            if (this.current != null) {
                return true;
            }
            History item = this.pendinghis;
            if (item == null) {
                item = (History) this.history.getLast();
            }
            return item == null || 0 == item.resumeEnd || delta > when - item.resumeEnd;
        }

        public String toString() {
            Object objValueOf;
            StringBuilder sb = new StringBuilder();
            sb.append("PUI{ uid=");
            sb.append(this.uid);
            sb.append(", order=");
            sb.append(this.order);
            sb.append(", packageName=");
            sb.append(this.packageName);
            sb.append(", weight=");
            sb.append(this.weight);
            sb.append(", current=");
            sb.append(this.current);
            sb.append(", resumeEnd=");
            History history = this.pendinghis;
            if (history != null) {
                objValueOf = Long.valueOf(history.resumeEnd);
            } else if (this.history.getLast() != null) {
                history = (History) this.history.getLast();
                objValueOf = Long.valueOf(history.resumeEnd);
            } else {
                objValueOf = "null";
            }
            sb.append(objValueOf);
            sb.append(" }");
            return sb.toString();
        }
    }

    PackageUsageStatsBase(Handler h) {
        this.mHandler = h;
    }

    public ArrayList<ProcessRecord> sortByPackageUsage(ArrayList<ProcessRecord> records) {
        synchronized (this.sud) {
            Collections.sort(records, new Comparator<ProcessRecord>() {
                @Override
                public int compare(ProcessRecord lhs, ProcessRecord rhs) {
                    PackageUsageInfo lp = PackageUsageStatsBase.this.sud.getValue(lhs);
                    PackageUsageInfo rp = PackageUsageStatsBase.this.sud.getValue(rhs);
                    int rweight = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    int lweight = lp != null ? lp.weight : -1000;
                    if (rp != null) {
                        rweight = rp.weight;
                    }
                    return lweight - rweight;
                }
            });
        }
        return records;
    }

    int getOrder(int uid, int max) {
        PackageUsageInfo info = this.sud.getValueByUid(uid, false);
        int ret = info != null ? info.order : this.sud.getSize();
        if (ret > max) {
            return max;
        }
        return ret;
    }

    boolean isRecent(long when, long delta, int uid) {
        PackageUsageInfo info = this.sud.getValueByUid(uid, false);
        if (info != null) {
            return info.isRecent(when, delta);
        }
        return false;
    }

    String dumpInfo(int uid) {
        PackageUsageInfo info = this.sud.getValueByUid(uid, false);
        return info != null ? info.toString() : "null";
    }

    protected class SmtUidDictionaryExt<T> extends SmtUidDictionary<T> {
        private Map<String, T> sharedUidMap = new HashMap(10);

        protected SmtUidDictionaryExt() {
        }

        public void setSharedUidMap(String pkgName, T t) {
            this.sharedUidMap.put(pkgName, t);
        }

        public Map<String, T> getSharedUidMap() {
            return this.sharedUidMap;
        }

        public T getValue(ProcessRecord app) {
            if (app.uid == 1000) {
                return getSharedUidMap().get(app.processName);
            }
            return getValueByUid(app.uid, false);
        }

        @Override
        public int getSize() {
            return (this.uidSets.size() + this.sharedUidMap.size()) - 1;
        }
    }

    public ArrayList<Integer> sortNoSystemAppByPackageUsage(ArrayList<Integer> uids) {
        synchronized (this.sud) {
            Collections.sort(uids, new Comparator<Integer>() {
                @Override
                public int compare(Integer lhs, Integer rhs) {
                    PackageUsageInfo lp = PackageUsageStatsBase.this.sud.getValueByUid(lhs.intValue(), false);
                    PackageUsageInfo rp = PackageUsageStatsBase.this.sud.getValueByUid(rhs.intValue(), false);
                    int rweight = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    int lweight = lp != null ? lp.weight : -1000;
                    if (rp != null) {
                        rweight = rp.weight;
                    }
                    return lweight - rweight;
                }
            });
        }
        return uids;
    }

    public ArrayList<ProcessRecord> sortByAdjAndPackageUsage(ArrayList<ProcessRecord> records) {
        synchronized (this.sud) {
            Collections.sort(records, new Comparator<ProcessRecord>() {
                @Override
                public int compare(ProcessRecord lhs, ProcessRecord rhs) {
                    if (lhs.setAdj != rhs.setAdj) {
                        return rhs.setAdj - lhs.setAdj;
                    }
                    PackageUsageInfo lp = PackageUsageStatsBase.this.sud.getValue(lhs);
                    PackageUsageInfo rp = PackageUsageStatsBase.this.sud.getValue(rhs);
                    int rweight = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    int lweight = lp != null ? lp.weight : -1000;
                    if (rp != null) {
                        rweight = rp.weight;
                    }
                    return lweight - rweight;
                }
            });
        }
        return records;
    }
}
