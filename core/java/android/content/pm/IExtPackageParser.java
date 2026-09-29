// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import com.pico.util.IExtBase;

/**
 * PICO package-parser extension. The factory ET/FT permission filter
 * (parseBaseApkCommon) is not ported yet.
 * @hide
 */
public interface IExtPackageParser extends IExtBase {
    PackageParser.Package parseVrFlags(PackageParser.Package pkg);
}
