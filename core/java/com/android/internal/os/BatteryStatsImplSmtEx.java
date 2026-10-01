// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

/**
 * Smartisan extension of {@link BatteryStatsImpl}. Reconstructed from the PICO OS 5.13.7
 * factory framework; as there, the counters of {@link UidSmtEx} are never created and the
 * background timer getters return null.
 *
 * @hide
 */
public class BatteryStatsImplSmtEx {
    private BatteryStatsImpl mBatteryStatsImpl;

    public BatteryStatsImplSmtEx(BatteryStatsImpl batteryStatsImpl) {
        mBatteryStatsImpl = batteryStatsImpl;
    }

    /** @hide */
    public static class UidSmtEx {
        private BatteryStatsImpl.Uid mUid;
        BatteryStatsImpl.LongSamplingCounter mMobileRadioApBgWakeupCount;
        BatteryStatsImpl.LongSamplingCounter mWifiRadioApBgWakeupCount;
        BatteryStatsImpl.LongSamplingCounter mMobileRadioActiveBgTime;

        public UidSmtEx(BatteryStatsImpl.Uid uid) {
            mUid = uid;
        }

        public BatteryStatsImpl.Timer getCameraTurnedOnBackgroundTimer() {
            if (mUid.mCameraTurnedOnTimer == null) {
                return null;
            }
            return null;
        }

        public BatteryStatsImpl.Timer getAudioTurnedOnBackgroundTimer() {
            if (mUid.mAudioTurnedOnTimer == null) {
                return null;
            }
            return null;
        }

        public BatteryStatsImpl.Timer getVideoTurnedOnBackgroundTimer() {
            if (mUid.mVideoTurnedOnTimer == null) {
                return null;
            }
            return null;
        }

        public long getMobileRadioApBgWakeupCount(int which) {
            if (mMobileRadioApBgWakeupCount != null) {
                return mMobileRadioApBgWakeupCount.getCountLocked(which);
            }
            return 0;
        }

        public long getWifiRadioApBgWakeupCount(int which) {
            if (mWifiRadioApBgWakeupCount != null) {
                return mWifiRadioApBgWakeupCount.getCountLocked(which);
            }
            return 0;
        }

        public long getMobileRadioActiveBgTime(int which) {
            if (mMobileRadioActiveBgTime != null) {
                return mMobileRadioActiveBgTime.getCountLocked(which);
            }
            return 0;
        }
    }
}
