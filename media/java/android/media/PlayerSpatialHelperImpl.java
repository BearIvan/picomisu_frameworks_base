// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.media;

import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * PICO: spatial audio state of one player ({@link MediaPlayer} or {@link AudioTrack}),
 * created lazily by {@link PlayerBase#getSpatialHelper()} and released with the player.
 * The native part (libmedia_jni, spatial::SpatialAudio of libspatialaudio) forwards the
 * orientation, pose and spatialization state of the player's audio session to the
 * ISpatializer of the audio policy service.
 */
class PlayerSpatialHelperImpl {
    private static final String TAG = "PlayerSpatialHelperImpl";
    private static final int INVALID_SESSION_ID = -1;

    static {
        System.loadLibrary("media_jni");
    }

    // Accessed by native methods: spatial::SpatialAudio instance.
    private long mNativeContext;

    private WeakReference<Object> mPlayer;
    private int mSessionId = INVALID_SESSION_ID;

    PlayerSpatialHelperImpl(Object player) {
        if (player == null) {
            return;
        }
        if (!(player instanceof MediaPlayer) && !(player instanceof AudioTrack)) {
            return;
        }
        mPlayer = new WeakReference<Object>(player);
        nativeSetup(new WeakReference<PlayerSpatialHelperImpl>(this));
        updateAudioSessionId(mPlayer, "init");
    }

    private int updateAudioSessionId(WeakReference<Object> playerRef, String caller) {
        if (playerRef == null) {
            return -2;
        }
        Object player = playerRef.get();
        if (player == null) {
            return -2;
        }
        int sessionId;
        if (player instanceof MediaPlayer) {
            sessionId = ((MediaPlayer) player).getAudioSessionId();
        } else if (player instanceof AudioTrack) {
            if (mSessionId == INVALID_SESSION_ID) {
                sessionId = ((AudioTrack) player).getAudioSessionId();
            } else {
                sessionId = mSessionId;
            }
        } else {
            Log.i(TAG, caller + " unknown player " + player);
            return -2;
        }
        if (mSessionId != sessionId) {
            nativeSetSessionId(sessionId);
            mSessionId = sessionId;
        }
        return sessionId > 0 ? 0 : -3;
    }

    @Override
    protected void finalize() {
        release();
    }

    public boolean isSpatializationEnabled() {
        if (updateAudioSessionId(mPlayer, "isSpatializationEnabled") != 0) {
            return false;
        }
        return nativeIsSpatializationEnabled();
    }

    void release() {
        Log.d(TAG, "release " + mPlayer);
        if (mPlayer != null) {
            nativeRelease();
            mSessionId = INVALID_SESSION_ID;
            mPlayer.clear();
            mPlayer = null;
        }
    }

    public int setAudioOrientation(float rotX, float rotY, float rotZ, float rotW) {
        int ret = updateAudioSessionId(mPlayer, "setAudioOrientation");
        if (ret != 0) {
            return ret;
        }
        return nativeSetAudioOrientation(rotX, rotY, rotZ, rotW);
    }

    public int setAudioPose(float rotX, float rotY, float rotZ, float rotW,
            float posX, float posY, float posZ) {
        int ret = updateAudioSessionId(mPlayer, "setAudioPose");
        if (ret != 0) {
            return ret;
        }
        return nativeSetAudioPose(rotX, rotY, rotZ, rotW, posX, posY, posZ);
    }

    public int setSpatializationEnabled(boolean enabled) {
        int ret = updateAudioSessionId(mPlayer, "setSpatializationEnabled");
        if (ret != 0) {
            return ret;
        }
        return nativeSetSpatializationEnabled(enabled);
    }

    private native void nativeSetup(Object weakThis);
    private native void nativeRelease();
    private native void nativeSetSessionId(int sessionId);
    private native int nativeSetAudioOrientation(float rotX, float rotY, float rotZ, float rotW);
    private native int nativeSetAudioPose(float rotX, float rotY, float rotZ, float rotW,
            float posX, float posY, float posZ);
    private native boolean nativeIsSpatializationEnabled();
    private native int nativeSetSpatializationEnabled(boolean enabled);
}
