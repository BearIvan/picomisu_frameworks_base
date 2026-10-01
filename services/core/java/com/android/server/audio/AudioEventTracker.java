// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Reports stream volume changes to the PICO telemetry ("teatracker") through the
 * pxr_notification service, as in the PICO OS 5.13.7 factory services.
 */
class AudioEventTracker {
    private static final boolean FEAT_ENABLED = true;
    private static final int MSG_ON_VOLUME_CHANGED = 0;
    private static final String TAG = "AS.AudioEventTracker";
    private static final String kEventSwitchAudioOutputVolume = "switch_audio_output_volume";
    private static final String kKeyForegroundPackageNameFar = "foreground_package_name_far";
    private static final String kKeyForegroundPackageNameNear = "foreground_package_name_near";
    private static final String kKeyOutputType = "output_type";
    private static final String kKeyPackageName = "package_name";
    private static final String kKeyStreamType = "stream_type";
    private static final String kKeySwitchDirection = "switch_direction";
    private static final String kKeySwitchVolumeUiAfter = "volume_ui_switch_after";
    private static final String kKeySwitchVolumeUiBefore = "volume_ui_switch_before";
    private static final String kTeaTrackerEvent = "teatracker_event_action";

    private MyHandler mHandler;
    private PxrNotificationClient mNotificationClient;
    private LooperThread mThread;

    private static class Holder {
        static AudioEventTracker mInstance = new AudioEventTracker();

        private Holder() {
        }
    }

    public static AudioEventTracker getInstance() {
        return Holder.mInstance;
    }

    private AudioEventTracker() {
        mNotificationClient = new PxrNotificationClient();
        mThread = new LooperThread();
        mThread.start();
        waitForHandlerCreation();
    }

    private class LooperThread extends Thread {
        LooperThread() {
            super("AS.AudioEventTrackerThread");
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (AudioEventTracker.this) {
                mHandler = new MyHandler();
                AudioEventTracker.this.notify();
            }
            Looper.loop();
        }
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ON_VOLUME_CHANGED) {
                onVolumeChanged((VolumeChangedEvent) msg.obj);
            }
        }
    }

    private void waitForHandlerCreation() {
        synchronized (this) {
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting on handler.");
                }
            }
        }
    }

    public void sendVolumeChangedEvent(int streamType, int oldIndex, int index, int device,
            String callingPackage) {
        VolumeChangedEvent event =
                new VolumeChangedEvent(streamType, oldIndex, index, device, callingPackage);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_ON_VOLUME_CHANGED, event));
    }

    public void onVolumeChanged(VolumeChangedEvent event) {
        if (event.oldIndex == event.index) {
            return;
        }
        int oldIndex = rescaleIndex(event.oldIndex);
        int index = rescaleIndex(event.index);
        String switchDirection = index - oldIndex > 0 ? "louder" : "lower";
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        try {
            writer.beginObject();
            writer.name(kKeyOutputType).value(outputDeviceToString(event.device));
            writer.name(kKeyStreamType).value(event.streamType);
            writer.name(kKeySwitchVolumeUiBefore).value(oldIndex);
            writer.name(kKeySwitchVolumeUiAfter).value(index);
            writer.name(kKeySwitchDirection).value(switchDirection);
            writer.name(kKeyPackageName).value(event.callingPackage);
            writer.name(kKeyForegroundPackageNameNear).value("");
            writer.name(kKeyForegroundPackageNameFar).value("");
            writer.endObject();
            writer.close();
            String message = sw.toString();
            Log.d(TAG, message);
            try {
                mNotificationClient.sendPxrMessage(kTeaTrackerEvent, 0,
                        kEventSwitchAudioOutputVolume, 0, message);
            } catch (RemoteException e) {
                Log.e(TAG, "Send message received exception: " + e);
            }
        } catch (IOException e) {
            Log.e(TAG, "Build json exception: " + e);
        }
    }

    private static int rescaleIndex(int index) {
        return (index + 5) / 10;
    }

    private static String outputDeviceToString(int device) {
        ArrayList<String> devices = new ArrayList<>();
        // DEVICE_OUT_WIRED_HEADSET | DEVICE_OUT_WIRED_HEADPHONE
        boolean isWiredHeadsetAnalog = (device & 0x4) != 0 || (device & 0x8) != 0;
        if (isWiredHeadsetAnalog) {
            devices.add("wired_headset_analog");
        }
        // DEVICE_OUT_USB_HEADSET | DEVICE_OUT_USB_ACCESSORY | DEVICE_OUT_USB_DEVICE
        boolean isWiredHeadsetDigital = (device & 0x4000000) != 0 || (device & 0x2000) != 0
                || (device & 0x4000) != 0;
        if (isWiredHeadsetDigital) {
            devices.add("wired_headset_digital");
        }
        // DEVICE_OUT_BLUETOOTH_A2DP* | DEVICE_OUT_BLUETOOTH_SCO_HEADSET/_CARKIT
        boolean isBluetoothHeadset = (device & 0x80) != 0 || (device & 0x100) != 0
                || (device & 0x200) != 0 || (device & 0x10) != 0 || (device & 0x20) != 0;
        if (isBluetoothHeadset) {
            devices.add("bluetooth_headset");
        }
        // DEVICE_OUT_REMOTE_SUBMIX
        boolean isRemoteSubmix = (device & 0x8000) != 0;
        if (isRemoteSubmix) {
            devices.add("remote_submix");
        }
        // DEVICE_OUT_SPEAKER
        boolean isSpeaker = (device & 0x2) != 0;
        if (isSpeaker) {
            devices.add("speaker");
        }
        StringBuilder sb = new StringBuilder();
        boolean firstFlag = true;
        for (String s : devices) {
            if (!firstFlag) {
                sb.append(',');
            }
            firstFlag = false;
            sb.append(s);
        }
        return sb.toString();
    }
}
