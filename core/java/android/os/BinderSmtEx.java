// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import android.util.Log;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smartisan binder transaction statistics. Reconstructed from the PICO OS 5.13.7 factory
 * framework (pure Java, no native methods; the factory framework itself never fills the
 * statistics maps).
 *
 * @hide
 */
public class BinderSmtEx {
    static final String TAG = "Binder";

    public static final int STATS_MODE_NONE = 0;
    public static final int STATS_MODE_CALL = 1 << 0;
    public static final int STATS_MODE_UID = 1 << 1;
    public static final int STATS_MODE_ALL = 1 << 2;

    protected static volatile boolean sTransStatsModeCall;
    protected static volatile boolean sTransStatsModeUid;
    protected static volatile boolean sTransStatsModeAll;
    protected static volatile boolean sMonitorCallingThread;

    protected static volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, TransStats>>
            sTransStats = new ConcurrentHashMap<>(100);
    protected static volatile ConcurrentHashMap<Integer, Stats> sTransStatsUid =
            new ConcurrentHashMap<>(30);
    protected static volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, TransStats>>
            sTransStatsAll = new ConcurrentHashMap<>(100);

    private Binder mBinder;

    public BinderSmtEx() {
    }

    public BinderSmtEx(Binder binder) {
        mBinder = binder;
    }

    public static void setTransStatsEnable(boolean enable, int mode) {
        if ((mode & STATS_MODE_CALL) != 0) {
            sTransStatsModeCall = enable;
        }
        if ((mode & STATS_MODE_UID) != 0) {
            sTransStatsModeUid = enable;
        }
        if ((mode & STATS_MODE_ALL) != 0) {
            sTransStatsModeAll = enable;
        }
        Slog.d("BinderTrans", "setTransStatsEnable, enable=" + enable + ", mode=" + mode
                + ", sTransStats(" + sTransStatsModeCall + ") :" + sTransStats.size()
                + ", sTransStatsUid(" + sTransStatsModeUid + ") :" + sTransStatsUid.size()
                + ", sTransStatsAll(" + sTransStatsModeAll + ") :" + sTransStatsAll.size());
    }

    public static void setMonitorCallingThreadEnable(boolean enable) {
        sMonitorCallingThread = enable;
    }

    public static boolean enableMonitorCallingThread() {
        return sMonitorCallingThread;
    }

    public static void clearTransStats(int mode) {
        if ((mode & STATS_MODE_CALL) != 0) {
            sTransStats.clear();
        }
        if ((mode & STATS_MODE_UID) != 0) {
            sTransStatsUid.clear();
        }
        if ((mode & STATS_MODE_ALL) != 0) {
            sTransStatsAll.clear();
        }
    }

    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, TransStats>>
            getTransStats() {
        return sTransStats;
    }

    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, TransStats>>
            getAllTransStats() {
        return sTransStatsAll;
    }

    public static Object[] getTransStatsUid() {
        final int N = sTransStatsUid.size();
        if (N > 1000 || N <= 0) {
            Slog.e("BinderStats", "getTransStatsUid, error stats map size=" + N);
            return null;
        }
        ConcurrentHashMap<Integer, Long> uids = new ConcurrentHashMap<>(N);
        for (Map.Entry<Integer, Stats> e : sTransStatsUid.entrySet()) {
            Stats st = e.getValue();
            uids.put(e.getKey(), st.cpuTime);
        }
        return uids.entrySet().toArray();
    }

    public static String getThreadName(int pid) {
        String threadName = "";
        File file = new File("/proc/" + pid + "/comm");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            threadName = reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "Get thread : " + pid + "  name failed!", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return threadName;
    }

    public static void logTransStats(int maxUidSize, int mode) {
        final boolean TSMCall = sTransStatsModeCall;
        final boolean TSMUid = sTransStatsModeUid;
        final boolean TSMAll = sTransStatsModeAll;
        final boolean monitorThreadName = sMonitorCallingThread;
        final boolean TSGoodToGo = TSMUid || TSMCall || TSMAll;
        if (TSGoodToGo) {
            StringBuilder buf = new StringBuilder(256);
            buf.setLength(0);
            buf.append("Binder Trans Stat:\ntime : count : code : descr");
            buf.append(maxUidSize == -1 ? "\n" : " : uids_count\n");
            Slog.d("BinderTrans", buf.toString());

            Iterator<Map.Entry<String, ConcurrentHashMap<Integer, TransStats>>> it =
                    getAllTransStats().entrySet().iterator();
            ArrayList<TransStatsEntry> list = new ArrayList<>(100);
            while (it.hasNext()) {
                Map.Entry<String, ConcurrentHashMap<Integer, TransStats>> e = it.next();
                String descr = e.getKey();
                for (Map.Entry<Integer, TransStats> e1 : e.getValue().entrySet()) {
                    Integer transCode = e1.getKey();
                    TransStats stats = e1.getValue();
                    List<UIDTrans> uidTranses = new ArrayList<>(stats.uidMap.values());
                    Collections.sort(uidTranses, new Comparator<UIDTrans>() {
                        @Override
                        public int compare(UIDTrans t0, UIDTrans t1) {
                            if (t0.count > t1.count) {
                                return -1;
                            } else if (t0.count < t1.count) {
                                return 1;
                            }
                            return 0;
                        }
                    });
                    list.add(new TransStatsEntry(descr, transCode, stats.timeSum, stats.count,
                            uidTranses));
                }
            }
            Collections.sort(list, TransStatsEntry.getComparator());

            final int N = list.size();
            for (int i = 0; i < N; i++) {
                buf.setLength(0);
                TransStatsEntry ent = list.get(i);
                long sum = ent.sum;
                if (sum > 1000) {
                    buf.append(sum / 1000 + " : " + ent.count + " : " + ent.code + " : "
                            + ent.descr);
                } else {
                    buf.append(sum / 1000.0f + " : " + ent.count + " : " + ent.code + " : "
                            + ent.descr);
                }
                if (ent.uidTranses != null && maxUidSize != -1) {
                    final int UIDSize = ent.uidTranses.size();
                    for (int j = 0; j < UIDSize && j < maxUidSize; j++) {
                        UIDTrans trans = ent.uidTranses.get(j);
                        buf.append("  " + trans.uid + ":" + trans.count + ":"
                                + (trans.timeSum > 1000 ? trans.timeSum / 1000
                                        : trans.timeSum / 1000.0f));
                        if (monitorThreadName) {
                            buf.append("[");
                            List<ThreadTrans> threadTranses =
                                    new ArrayList<>(trans.threadsMap.values());
                            Collections.sort(threadTranses, new Comparator<ThreadTrans>() {
                                @Override
                                public int compare(ThreadTrans t0, ThreadTrans t1) {
                                    if (t0.count > t1.count) {
                                        return -1;
                                    } else if (t0.count < t1.count) {
                                        return 1;
                                    }
                                    return 0;
                                }
                            });
                            for (int k = 0; k < threadTranses.size(); k++) {
                                ThreadTrans threadTrans = threadTranses.get(k);
                                buf.append("(" + threadTrans.threadName + ":" + threadTrans.count
                                        + ":" + (threadTrans.timeSum > 1000
                                                ? threadTrans.timeSum / 1000
                                                : threadTrans.timeSum / 1000.0f)
                                        + ")");
                            }
                            buf.append("]");
                        }
                    }
                }
                buf.append("\n");
                Slog.d("BinderTrans", buf.toString());
            }
        } else {
            Slog.d("BinderTrans", "Binder Transaction Stats not enabled");
        }
        clearTransStats(mode);
    }

    /** @hide */
    public class TransStats {
        public int count;
        public long timeSum;
        public ConcurrentHashMap<Integer, UIDTrans> uidMap = new ConcurrentHashMap<>(10);
    }

    /** @hide */
    public class ThreadTrans {
        public String threadName;
        public int count;
        public long timeSum = 0;

        public ThreadTrans(String threadName, int count) {
            this.threadName = threadName;
            this.count = count;
        }
    }

    /** @hide */
    public class UIDTrans {
        public int uid;
        public int count;
        public long timeSum = 0;
        public ConcurrentHashMap<String, ThreadTrans> threadsMap = new ConcurrentHashMap<>(10);

        public UIDTrans(int uid, int count) {
            this.uid = uid;
            this.count = count;
        }
    }

    /** @hide */
    public static class TransStatsEntry {
        String descr;
        int code;
        long sum;
        int count;
        List<UIDTrans> uidTranses;

        public TransStatsEntry(String descr, int code, long sum, int count,
                List<UIDTrans> uidTranses) {
            this.descr = descr;
            this.code = code;
            this.sum = sum;
            this.count = count;
            this.uidTranses = uidTranses;
        }

        public static Comparator<TransStatsEntry> getComparator() {
            return new Comparator<TransStatsEntry>() {
                @Override
                public int compare(TransStatsEntry lhs, TransStatsEntry rhs) {
                    if (lhs.sum == rhs.sum) {
                        return 0;
                    }
                    return lhs.sum > rhs.sum ? -1 : 1;
                }
            };
        }
    }

    /** @hide */
    public static class Stats {
        long cpuTime;

        public Stats(long cpu) {
            cpuTime = cpu;
        }
    }
}
