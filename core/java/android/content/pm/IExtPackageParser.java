// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import com.pico.util.IExtBase;

/**
 * PICO package-parser extension: VR flags, 2D virtual-display configuration and the
 * eye/face-tracking permission filter.
 * @hide
 */
public interface IExtPackageParser extends IExtBase {
    void parseBaseApkCommon(PackageParser.Package pkg);
    PackageParser.Package parseVrFlags(PackageParser.Package pkg);
}
