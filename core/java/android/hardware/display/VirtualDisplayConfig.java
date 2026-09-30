/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.display;

import static android.view.Display.DEFAULT_DISPLAY;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;

import java.lang.annotation.Annotation;

/**
 * Holds configuration used to create {@link VirtualDisplay} instances.
 *
 * Backport of the Android 11 class (android-11.0.0_r48) in the form found in the factory
 * PICO OS 5.13.7 framework.jar: the codegen output of the Android 11 source, with the
 * AnnotationValidations checks it needs (absent in Android 10) kept as static members of
 * this class.
 *
 * @hide
 */
public final class VirtualDisplayConfig implements Parcelable {
    /**
     * The name of the virtual display, must be non-empty.
     */
    @NonNull
    private String mName;

    /**
     * The width of the virtual display in pixels. Must be greater than 0.
     */
    @IntRange(from = 1)
    private int mWidth;

    /**
     * The height of the virtual display in pixels. Must be greater than 0.
     */
    @IntRange(from = 1)
    private int mHeight;

    /**
     * The density of the virtual display in dpi. Must be greater than 0.
     */
    @IntRange(from = 1)
    private int mDensityDpi;

    /**
     * A combination of virtual display flags.
     * {@link DisplayManager#VIRTUAL_DISPLAY_FLAG_PUBLIC},
     * {@link DisplayManager#VIRTUAL_DISPLAY_FLAG_PRESENTATION},
     * {@link DisplayManager#VIRTUAL_DISPLAY_FLAG_SECURE},
     * {@link DisplayManager#VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY},
     * or {@link DisplayManager#VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR}.
     */
    private int mFlags = 0;

    /**
     * The surface to which the content of the virtual display should be rendered, or null if
     * there is none initially.
     */
    @Nullable
    private Surface mSurface = null;

    /**
     * The unique identifier for the display. Shouldn't be displayed to the user.
     */
    @Nullable
    private String mUniqueId = null;

    /**
     * The id of the display that the virtual display should mirror, or
     * {@link android.view.Display#DEFAULT_DISPLAY} if there is none initially.
     */
    private int mDisplayIdToMirror = DEFAULT_DISPLAY;

    /* package-private */ VirtualDisplayConfig(
            @NonNull String name,
            @IntRange(from = 1) int width,
            @IntRange(from = 1) int height,
            @IntRange(from = 1) int densityDpi,
            int flags,
            @Nullable Surface surface,
            @Nullable String uniqueId,
            int displayIdToMirror) {
        this.mName = name;
        validate(
                NonNull.class, null, mName);
        this.mWidth = width;
        validate(
                IntRange.class, null, mWidth,
                "from", 1);
        this.mHeight = height;
        validate(
                IntRange.class, null, mHeight,
                "from", 1);
        this.mDensityDpi = densityDpi;
        validate(
                IntRange.class, null, mDensityDpi,
                "from", 1);
        this.mFlags = flags;
        this.mSurface = surface;
        this.mUniqueId = uniqueId;
        this.mDisplayIdToMirror = displayIdToMirror;
    }

    /**
     * The name of the virtual display, must be non-empty.
     */
    public @NonNull String getName() {
        return mName;
    }

    /**
     * The width of the virtual display in pixels. Must be greater than 0.
     */
    public @IntRange(from = 1) int getWidth() {
        return mWidth;
    }

    /**
     * The height of the virtual display in pixels. Must be greater than 0.
     */
    public @IntRange(from = 1) int getHeight() {
        return mHeight;
    }

    /**
     * The density of the virtual display in dpi. Must be greater than 0.
     */
    public @IntRange(from = 1) int getDensityDpi() {
        return mDensityDpi;
    }

    /**
     * A combination of virtual display flags.
     */
    public int getFlags() {
        return mFlags;
    }

    /**
     * The surface to which the content of the virtual display should be rendered, or null if
     * there is none initially.
     */
    public @Nullable Surface getSurface() {
        return mSurface;
    }

    /**
     * The unique identifier for the display. Shouldn't be displayed to the user.
     */
    public @Nullable String getUniqueId() {
        return mUniqueId;
    }

    /**
     * The id of the display that the virtual display should mirror, or
     * {@link android.view.Display#DEFAULT_DISPLAY} if there is none initially.
     */
    public int getDisplayIdToMirror() {
        return mDisplayIdToMirror;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        int flg = 0;
        if (mSurface != null) flg |= 0x20;
        if (mUniqueId != null) flg |= 0x40;
        dest.writeInt(flg);
        dest.writeString(mName);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeInt(mDensityDpi);
        dest.writeInt(mFlags);
        if (mSurface != null) dest.writeTypedObject(mSurface, flags);
        if (mUniqueId != null) dest.writeString(mUniqueId);
        dest.writeInt(mDisplayIdToMirror);
    }

    @Override
    public int describeContents() { return 0; }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    /* package-private */ VirtualDisplayConfig(@NonNull Parcel in) {
        int flg = in.readInt();
        String name = in.readString();
        int width = in.readInt();
        int height = in.readInt();
        int densityDpi = in.readInt();
        int flags = in.readInt();
        Surface surface = (flg & 0x20) == 0 ? null : (Surface) in.readTypedObject(Surface.CREATOR);
        String uniqueId = (flg & 0x40) == 0 ? null : in.readString();
        int displayIdToMirror = in.readInt();

        this.mName = name;
        validate(
                NonNull.class, null, mName);
        this.mWidth = width;
        validate(
                IntRange.class, null, mWidth,
                "from", 1);
        this.mHeight = height;
        validate(
                IntRange.class, null, mHeight,
                "from", 1);
        this.mDensityDpi = densityDpi;
        validate(
                IntRange.class, null, mDensityDpi,
                "from", 1);
        this.mFlags = flags;
        this.mSurface = surface;
        this.mUniqueId = uniqueId;
        this.mDisplayIdToMirror = displayIdToMirror;
    }

    public static final @NonNull Parcelable.Creator<VirtualDisplayConfig> CREATOR
            = new Parcelable.Creator<VirtualDisplayConfig>() {
        @Override
        public VirtualDisplayConfig[] newArray(int size) {
            return new VirtualDisplayConfig[size];
        }

        @Override
        public VirtualDisplayConfig createFromParcel(@NonNull Parcel in) {
            return new VirtualDisplayConfig(in);
        }
    };

    /**
     * A builder for {@link VirtualDisplayConfig}
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {

        private @NonNull String mName;
        private @IntRange(from = 1) int mWidth;
        private @IntRange(from = 1) int mHeight;
        private @IntRange(from = 1) int mDensityDpi;
        private int mFlags;
        private @Nullable Surface mSurface;
        private @Nullable String mUniqueId;
        private int mDisplayIdToMirror;

        private long mBuilderFieldsSet = 0L;

        /**
         * Creates a new Builder.
         *
         * @param name
         *   The name of the virtual display, must be non-empty.
         * @param width
         *   The width of the virtual display in pixels. Must be greater than 0.
         * @param height
         *   The height of the virtual display in pixels. Must be greater than 0.
         * @param densityDpi
         *   The density of the virtual display in dpi. Must be greater than 0.
         */
        public Builder(
                @NonNull String name,
                @IntRange(from = 1) int width,
                @IntRange(from = 1) int height,
                @IntRange(from = 1) int densityDpi) {
            mName = name;
            validate(
                    NonNull.class, null, mName);
            mWidth = width;
            validate(
                    IntRange.class, null, mWidth,
                    "from", 1);
            mHeight = height;
            validate(
                    IntRange.class, null, mHeight,
                    "from", 1);
            mDensityDpi = densityDpi;
            validate(
                    IntRange.class, null, mDensityDpi,
                    "from", 1);
        }

        /**
         * The name of the virtual display, must be non-empty.
         */
        public @NonNull Builder setName(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mName = value;
            return this;
        }

        /**
         * The width of the virtual display in pixels. Must be greater than 0.
         */
        public @NonNull Builder setWidth(@IntRange(from = 1) int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mWidth = value;
            return this;
        }

        /**
         * The height of the virtual display in pixels. Must be greater than 0.
         */
        public @NonNull Builder setHeight(@IntRange(from = 1) int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mHeight = value;
            return this;
        }

        /**
         * The density of the virtual display in dpi. Must be greater than 0.
         */
        public @NonNull Builder setDensityDpi(@IntRange(from = 1) int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mDensityDpi = value;
            return this;
        }

        /**
         * A combination of virtual display flags.
         */
        public @NonNull Builder setFlags(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10;
            mFlags = value;
            return this;
        }

        /**
         * The surface to which the content of the virtual display should be rendered, or null if
         * there is none initially.
         */
        public @NonNull Builder setSurface(@NonNull Surface value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x20;
            mSurface = value;
            return this;
        }

        /**
         * The unique identifier for the display. Shouldn't be displayed to the user.
         */
        public @NonNull Builder setUniqueId(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x40;
            mUniqueId = value;
            return this;
        }

        /**
         * The id of the display that the virtual display should mirror, or
         * {@link android.view.Display#DEFAULT_DISPLAY} if there is none initially.
         */
        public @NonNull Builder setDisplayIdToMirror(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x80;
            mDisplayIdToMirror = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull VirtualDisplayConfig build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x100; // Mark builder used

            if ((mBuilderFieldsSet & 0x10) == 0) {
                mFlags = 0;
            }
            if ((mBuilderFieldsSet & 0x20) == 0) {
                mSurface = null;
            }
            if ((mBuilderFieldsSet & 0x40) == 0) {
                mUniqueId = null;
            }
            if ((mBuilderFieldsSet & 0x80) == 0) {
                mDisplayIdToMirror = DEFAULT_DISPLAY;
            }
            VirtualDisplayConfig o = new VirtualDisplayConfig(
                    mName,
                    mWidth,
                    mHeight,
                    mDensityDpi,
                    mFlags,
                    mSurface,
                    mUniqueId,
                    mDisplayIdToMirror);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x100) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    // The Android 11 com.android.internal.util.AnnotationValidations checks used above.

    public static void validate(Class<IntRange> annotation, IntRange ignored, int value,
            String paramName, long param) {
        switch (paramName) {
            case "from":
                if (value < param) {
                    invalid(annotation, value, paramName, param);
                }
                break;
            case "to":
                if (value > param) {
                    invalid(annotation, value, paramName, param);
                }
                break;
        }
    }

    public static void validate(Class<NonNull> annotation, NonNull ignored, Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
    }

    private static void invalid(Class<? extends Annotation> annotation, Object value) {
        invalid("@" + annotation.getSimpleName(), value);
    }

    private static void invalid(Class<? extends Annotation> annotation, Object value,
            String paramName, Object param) {
        String paramPrefix = "value".equals(paramName) ? "" : paramName + " = ";
        invalid("@" + annotation.getSimpleName() + "(" + paramPrefix + param + ")", value);
    }

    private static void invalid(String valueKind, Object value) {
        throw new IllegalStateException("Invalid " + valueKind + ": " + value);
    }
}
