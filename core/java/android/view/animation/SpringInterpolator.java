// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.android.internal.R;

/**
 * An interpolator that overshoots and settles like a damped spring
 * (factory PICO OS 5.13.7 {@code android.view.animation.SpringInterpolator}).
 *
 * @hide
 */
public class SpringInterpolator extends BaseInterpolator {
    private static final float FACTOR_DEFAULT = 0.5f;

    private float mFactor;

    public SpringInterpolator(float mFactor) {
        this.mFactor = mFactor;
    }

    public SpringInterpolator() {
        mFactor = FACTOR_DEFAULT;
    }

    public SpringInterpolator(Context context, AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    public SpringInterpolator(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.SpringInterpolator, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, R.styleable.SpringInterpolator);
        }

        mFactor = a.getFloat(R.styleable.SpringInterpolator_factor, FACTOR_DEFAULT);
        setChangingConfiguration(a.getChangingConfigurations());
        a.recycle();
    }

    @Override
    public float getInterpolation(float input) {
        if (input == 0.0f || input == 1.0f) {
            return input;
        }
        return (float) (Math.pow(2, -10 * input)
                * Math.sin((input - mFactor / 4.0) * (2 * Math.PI) / mFactor) + 1);
    }
}
