// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.media;

/**
 * PICO OS 5.13.7: converts the orientation of a sound source from the coordinate system of an
 * engine (OpenXR, Unity, Unreal) to the rotation quaternion of the spatializer
 * ({@link PlayerSpatialHelper#setAudioOrientation}). The native part is
 * spatial::SpatialCoordConverter of libspatialaudio. As in the factory framework.jar;
 * hidden-API whitelisted like the factory.
 *
 * @hide
 */
public class SpatialCoordConverter {
    /** Column-major 3x3 axes of the Unity coordinate system. */
    public static final float[] COORD_TRANSFORM_UNITY;
    /** Column-major 3x3 axes of the Unreal coordinate system. */
    public static final float[] COORD_TRANSFORM_UNREAL;
    /** Column-major 3x3 axes of the OpenXR coordinate system. */
    public static final float[] COORD_TRANSFORM_OPENXR;

    private long mNativeContext;

    static {
        System.loadLibrary("media_jni");
        COORD_TRANSFORM_UNITY = new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f};
        COORD_TRANSFORM_UNREAL = new float[] {0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
        COORD_TRANSFORM_OPENXR = new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    }

    public SpatialCoordConverter() {
        setup();
    }

    @Override
    protected void finalize() {
        release();
    }

    private native void setup();

    private native void release();

    /**
     * @param transform the axes of the engine coordinate system, see COORD_TRANSFORM_*.
     * @return {@link SpatialAudio#OK} or a SpatialAudio error code.
     */
    public native int setCoordinateTransform(float[] transform);

    /** @return {@link SpatialAudio#OK} or a SpatialAudio error code. */
    public native int setAdditionalCameraOrientation(float frontX, float frontY, float frontZ,
            float upX, float upY, float upZ);

    /**
     * @param orientation receives the (x, y, z, w) rotation, 4 elements.
     * @return {@link SpatialAudio#OK} or a SpatialAudio error code.
     */
    public native int convertRelativeAudioOrientation(float targetX, float targetY,
            float targetZ, float reserved0, float reserved1, float reserved2,
            float upX, float upY, float upZ, float originX, float originY, float originZ,
            float[] orientation);

    /**
     * @param orientation receives the (x, y, z, w) rotation, 4 elements.
     * @return {@link SpatialAudio#OK} or a SpatialAudio error code.
     */
    public native int convertAudioOrientation(float frontX, float frontY, float frontZ,
            float upX, float upY, float upZ, float[] orientation);
}
