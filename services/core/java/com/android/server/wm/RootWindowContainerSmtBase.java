// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import java.util.HashSet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class RootWindowContainerSmtBase extends WindowContainerSmtBase {
    protected RootWindowContainer mContainer;
    WindowState mHoldPcScreenWindow;
    boolean mPhoneStateIdel;

    public RootWindowContainerSmtBase(RootWindowContainer container) {
        super(container);
        this.mHoldPcScreenWindow = null;
        this.mPhoneStateIdel = true;
        this.mContainer = container;
    }

    boolean canShowSysDialog() {
        return true;
    }

    protected void dispatchVisibleWindowChanged(String title, String packageName) {
        for (int i = 0; i < this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.size(); i++) {
            this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.get(i).onVisibleWindowAdd(title, packageName);
        }
    }

    protected void dispatchVisibleWindowCleared() {
        for (int i = 0; i < this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.size(); i++) {
            this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.get(i).onVisibleWindowClear();
        }
    }

    protected void dispatchVisibleUidsChanged(HashSet<Integer> oldVisibleUids, HashSet<Integer> visibleUids) {
        HashSet<Integer> addedUids = new HashSet<>();
        addedUids.addAll(visibleUids);
        addedUids.removeAll(oldVisibleUids);
        HashSet<Integer> removedUids = new HashSet<>();
        removedUids.addAll(oldVisibleUids);
        removedUids.removeAll(visibleUids);
        if (addedUids.size() > 0 || removedUids.size() > 0) {
            for (int i = 0; i < this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.size(); i++) {
                this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.get(i).onVisibleUidsChange(addedUids, removedUids);
            }
        }
    }

    protected void dispatchVisibleApplicationInfosChanged(HashSet<ApplicationInfo> oldVisibleApplicationInfos, HashSet<ApplicationInfo> visibleApplicationInfos) {
        HashSet<ApplicationInfo> addedApplicationInfos = new HashSet<>();
        addedApplicationInfos.addAll(visibleApplicationInfos);
        addedApplicationInfos.removeAll(oldVisibleApplicationInfos);
        HashSet<ApplicationInfo> removedApplicationInfos = new HashSet<>();
        removedApplicationInfos.addAll(oldVisibleApplicationInfos);
        removedApplicationInfos.removeAll(visibleApplicationInfos);
        if (addedApplicationInfos.size() > 0 || removedApplicationInfos.size() > 0) {
            for (int i = 0; i < this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.size(); i++) {
                this.mContainer.mWmService.getSmtEx().mVisibleWindowChangeListeners.get(i).onVisibleApplicationInfosChange(addedApplicationInfos, removedApplicationInfos, visibleApplicationInfos);
            }
        }
    }
}
