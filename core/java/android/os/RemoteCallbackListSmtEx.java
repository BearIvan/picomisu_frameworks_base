// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

/**
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class RemoteCallbackListSmtEx {

    static class CallbackSmtEx {
        private int mPid;
        private int mUid;

        CallbackSmtEx() {
        }

        public int getPid() {
            return this.mPid;
        }

        public int getUid() {
            return this.mUid;
        }

        public void setPid(int pid) {
            this.mPid = pid;
        }

        public void setUid(int uid) {
            this.mUid = uid;
        }
    }

    public static int getRegisteredCallbackPid(RemoteCallbackList host, int index) {
        synchronized (host) {
            if (!host.mKilled && host.getBroadcastCallback(index) != null) {
                return host.getBroadcastCallback(index).getSmtEx().getPid();
            }
            return -1;
        }
    }

    public static int getRegisteredCallbackUid(RemoteCallbackList host, int index) {
        synchronized (host) {
            if (!host.mKilled && host.getBroadcastCallback(index) != null) {
                return host.getBroadcastCallback(index).getSmtEx().getUid();
            }
            return -1;
        }
    }
}
