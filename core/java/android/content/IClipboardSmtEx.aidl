// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 framework.jar (Stub/Proxy of
// android.content.IClipboardSmtEx): transaction codes 1 delete, 2 insert, 3 getCopyHistory.
package android.content;

import android.content.CopyHistoryItem;

/**
 * Smartisan clipboard copy-history extension.
 * {@hide}
 */
interface IClipboardSmtEx {
    void delete(in CopyHistoryItem item);
    void insert(in List<CopyHistoryItem> items);
    List<CopyHistoryItem> getCopyHistory();
}
