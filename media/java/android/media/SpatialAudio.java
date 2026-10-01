// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.media;

/**
 * PICO OS 5.13.7: result codes and spatialization constants of the per-player spatial audio
 * API ({@link PlayerSpatialHelper}, {@link SpatialCoordConverter}), as in the factory
 * framework.jar. Hidden-API whitelisted like the factory.
 *
 * @hide
 */
public class SpatialAudio {
    public static final int OK = 0;
    public static final int ERR_UNKNOWN = -1;
    public static final int ERR_INVALID_PLAYER = -2;
    public static final int ERR_INVALID_PLAYER_STATE = -3;
    public static final int ERR_INVALID_PARAMETER = -4;

    public static final int SPATIALIZATION_BEHAVIOR_AUTO = 0;
    public static final int SPATIALIZATION_BEHAVIOR_NEVER = 1;
    public static final int SPATIALIZATION_BEHAVIOR_ALWAYS = 100;

    public static final int SPATIALIZATION_TYPE_AUDIO_CHANNELS = 0;
    public static final int SPATIALIZATION_TYPE_AMBISONIC = 1;

    private SpatialAudio() {
    }
}
