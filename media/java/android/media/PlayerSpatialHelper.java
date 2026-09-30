// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.media;

/**
 * PICO: per-player control of the spatial audio rendering of an {@link AudioTrack} or a
 * {@link MediaPlayer}: orientation and pose of the sound source and per-player
 * spatialization. The calls are forwarded to the player's {@link PlayerSpatialHelperImpl}.
 * Methods return 0 on success and a negative error otherwise (-3 when the helper was
 * released or the player has no audio session yet).
 *
 * @hide
 */
public class PlayerSpatialHelper {
    private static final String TAG = "PlayerSpatialHelper";

    private PlayerSpatialHelperImpl mImpl;

    public PlayerSpatialHelper(AudioTrack track) {
        mImpl = track.getSpatialHelper();
    }

    public PlayerSpatialHelper(MediaPlayer player) {
        mImpl = player.getSpatialHelper();
    }

    public int setAudioOrientation(float rotX, float rotY, float rotZ, float rotW) {
        if (mImpl == null) {
            return -3;
        }
        return mImpl.setAudioOrientation(rotX, rotY, rotZ, rotW);
    }

    public int setAudioPose(float rotX, float rotY, float rotZ, float rotW,
            float posX, float posY, float posZ) {
        if (mImpl == null) {
            return -3;
        }
        return mImpl.setAudioPose(rotX, rotY, rotZ, rotW, posX, posY, posZ);
    }

    public int setSpatializationEnabled(boolean enabled) {
        if (mImpl == null) {
            return -3;
        }
        return mImpl.setSpatializationEnabled(enabled);
    }

    public boolean isSpatializationEnabled() {
        if (mImpl == null) {
            return false;
        }
        return mImpl.isSpatializationEnabled();
    }

    public void release() {
        if (mImpl != null) {
            mImpl = null;
        }
    }

    @Override
    protected void finalize() {
        release();
    }
}
