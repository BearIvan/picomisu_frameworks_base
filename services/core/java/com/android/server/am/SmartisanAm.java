// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.IApplicationThread;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SmartisanAm {

    public static class SmartisanAmUtils {
        private ActivityManagerService mActivityService;
        private static SmartisanAmUtils mInstance = new SmartisanAmUtils();
        public static int FEAT_SCENE_ADJUST_FREQ_SWITCH_OFFSET = 0;

        private SmartisanAmUtils() {
        }

        public static SmartisanAmUtils getInstance() {
            return mInstance;
        }

        public void initService(ActivityManagerService service) {
            this.mActivityService = service;
        }

        public void notifyClientsMonitorStatsChanged(boolean open) {
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        IApplicationThread thread = proc.thread;
                        if (thread != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                data.writeInt(open ? 1 : 0);
                                binder.transact(1021, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void notifyClientsEnableMonitorBinderTrans(boolean enable, int mode) {
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        IApplicationThread thread = proc.thread;
                        if (thread != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                data.writeInt(enable ? 1 : 0);
                                data.writeInt(mode);
                                binder.transact(1023, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void logClientsBinderTrans(int maxUid, int mode) {
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        IApplicationThread thread = proc.thread;
                        if (thread != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                data.writeInt(maxUid);
                                data.writeInt(mode);
                                binder.transact(1024, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void openAppMainThreadLooperTrace(String packageName) {
            IApplicationThread thread;
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        if (packageName != null && packageName.equals(proc.info.packageName) && (thread = proc.thread) != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                binder.transact(1025, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void openAppMainThreadLooperMonitor(int pid) {
            IApplicationThread thread;
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        if (pid == proc.pid && (thread = proc.thread) != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                binder.transact(1026, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void getAppMainSlowOperations(int pid, int index) {
            IApplicationThread thread;
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        if (pid == proc.pid && (thread = proc.thread) != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                data.writeInt(index);
                                binder.transact(1027, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void notifySwitchState(int offset, boolean enable) {
            synchronized (this.mActivityService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord proc : this.mActivityService.mProcessList.mLruProcesses) {
                        IApplicationThread thread = proc.thread;
                        if (thread != null) {
                            try {
                                IBinder binder = thread.asBinder();
                                Parcel data = Parcel.obtain();
                                data.writeInt(offset);
                                data.writeInt(enable ? 1 : 0);
                                binder.transact(1032, data, null, 1);
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void putIntToSettings(String key, int value) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return;
            }
            Settings.System.putInt(activityManagerService.mContext.getContentResolver(), key, value);
        }

        public int getIntFromSettings(String key, int defaultValue) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return defaultValue;
            }
            return Settings.System.getInt(activityManagerService.mContext.getContentResolver(), key, defaultValue);
        }

        public void putLongToSettings(String key, long value) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return;
            }
            Settings.System.putLong(activityManagerService.mContext.getContentResolver(), key, value);
        }

        public long getLongFromSettings(String key, long defaultValue) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return defaultValue;
            }
            return Settings.System.getLong(activityManagerService.mContext.getContentResolver(), key, defaultValue);
        }

        public void putStringToSettings(String key, String value) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return;
            }
            Settings.System.putString(activityManagerService.mContext.getContentResolver(), key, value);
        }

        public String getStringFromSettings(String key) {
            ActivityManagerService activityManagerService = this.mActivityService;
            if (activityManagerService == null) {
                return null;
            }
            return Settings.System.getString(activityManagerService.mContext.getContentResolver(), key);
        }
    }
}
