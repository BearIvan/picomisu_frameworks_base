// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.SparseArray;
import com.android.server.job.JobSchedulerShellCommand;
import java.util.HashSet;
import java.util.Set;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SmtUidDictionary<T> {
    private Object[] uidSystem = new Object[50];
    private Object[] uidUserLevel1 = new Object[200];
    private Object[] uidUserLevel2 = new Object[20];
    private SparseArray<T> uidOther = new SparseArray<>(50);
    protected Set<Integer> uidSets = new HashSet(150);

    public void setValueByUid(int uid, T value, boolean needSynchronize) {
        if (needSynchronize) {
            setValueByUidLocked(uid, value);
        } else {
            setValueByUidInner(uid, value);
        }
    }

    public T getValueByUid(int uid, boolean needSynchronize) {
        if (needSynchronize) {
            return getValueByUidLocked(uid);
        }
        return getValueByUidInner(uid);
    }

    public Set<Integer> getAllUidSet() {
        return this.uidSets;
    }

    public int getSize() {
        return this.uidSets.size();
    }

    private void setValueByUidLocked(int uid, T value) {
        synchronized (this) {
            setValueByUidInner(uid, value);
        }
    }

    private void setValueByUidInner(int uid, T value) {
        if (uid >= 1000 && uid < 1100) {
            int key = uid + JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            if (key >= 50) {
                Object[] objArr = this.uidSystem;
                if (objArr.length - 1 < key) {
                    this.uidSystem = enlargeArray(objArr, 100);
                }
            }
            this.uidSystem[key] = value;
        } else if (uid >= 10000 && uid < 10500) {
            int key2 = uid - 10000;
            if (key2 >= 400) {
                Object[] objArr2 = this.uidUserLevel1;
                if (objArr2.length - 1 < key2) {
                    this.uidUserLevel1 = enlargeArray(objArr2, 500);
                }
            } else if (key2 >= 300) {
                Object[] objArr3 = this.uidUserLevel1;
                if (objArr3.length - 1 < key2) {
                    this.uidUserLevel1 = enlargeArray(objArr3, 400);
                }
            } else if (key2 >= 200) {
                Object[] objArr4 = this.uidUserLevel1;
                if (objArr4.length - 1 < key2) {
                    this.uidUserLevel1 = enlargeArray(objArr4, 300);
                }
            }
            this.uidUserLevel1[key2] = value;
        } else if (uid >= 20000 && uid < 20100) {
            int key3 = uid - 20000;
            if (key3 >= 20) {
                Object[] objArr5 = this.uidUserLevel2;
                if (objArr5.length - 1 < key3) {
                    this.uidUserLevel2 = enlargeArray(objArr5, 100);
                }
            }
            this.uidUserLevel2[key3] = value;
        } else {
            this.uidOther.put(uid, value);
        }
        if (value == null) {
            this.uidSets.remove(Integer.valueOf(uid));
        } else {
            this.uidSets.add(Integer.valueOf(uid));
        }
    }

    private static Object[] enlargeArray(Object[] srcArr, int enlargeLength) {
        Object[] destArr = new Object[enlargeLength];
        System.arraycopy(srcArr, 0, destArr, 0, srcArr.length);
        return destArr;
    }

    private T getValueByUidLocked(int uid) {
        T valueByUidInner;
        synchronized (this) {
            valueByUidInner = getValueByUidInner(uid);
        }
        return valueByUidInner;
    }

    private T getValueByUidInner(int i) {
        Object obj = null;
        if (i >= 1000 && i < 1100) {
            int i2 = i + JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            Object[] objArr = this.uidSystem;
            if (i2 < objArr.length) {
                obj = objArr[i2];
            }
        } else if (i >= 10000 && i < 10500) {
            int i3 = i - 10000;
            Object[] objArr2 = this.uidUserLevel1;
            if (i3 < objArr2.length) {
                obj = objArr2[i3];
            }
        } else if (i >= 20000 && i < 20100) {
            int i4 = i - 20000;
            Object[] objArr3 = this.uidUserLevel2;
            if (i4 < objArr3.length) {
                obj = objArr3[i4];
            }
        } else {
            obj = this.uidOther.get(i);
        }
        if (obj != null) {
            return (T) obj;
        }
        return null;
    }
}
