// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerSmtEx;
import android.widget.TextView;

import com.android.internal.policy.DecorView;
import com.android.internal.widget.CachingIconView;

/**
 * Creates the Smartisan extension of a {@link View} for {@link View#getSmtEx}
 * (factory PICO OS 5.13.7 {@code android.view.ViewSmtExFactory}).
 *
 * @hide
 */
public class ViewSmtExFactory {
    public static ViewSmtBase createSmtEx(View view) {
        if (view instanceof TextView) {
            return new ViewSmtBase((TextView) view);
        } else if (view instanceof ImageButton) {
            return new ViewSmtBase((ImageButton) view);
        } else if (view instanceof CachingIconView) {
            return new ViewSmtBase((CachingIconView) view);
        } else if (view instanceof ImageView) {
            return new ViewSmtBase((ImageView) view);
        } else if (view instanceof Spinner) {
            return new SpinnerSmtEx((Spinner) view);
        } else if (view instanceof AbsListView) {
            return new ViewSmtBase((AbsListView) view);
        } else if (view instanceof DecorView) {
            return new ViewSmtBase((DecorView) view);
        } else if (view instanceof ViewGroup) {
            return new ViewSmtBase((ViewGroup) view);
        }
        return new ViewSmtBase(view);
    }
}
