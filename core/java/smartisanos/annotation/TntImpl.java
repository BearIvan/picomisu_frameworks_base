// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a TNT implementation class (factory PICO OS 5.13.7 {@code smartisanos.annotation.TntImpl}).
 *
 * @hide
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface TntImpl {
    Class implement() default Void.class;
}
