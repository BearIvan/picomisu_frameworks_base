// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package org.picomisu.runtime;

import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplay;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.os.Binder;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;

/** Executes the real boot-classpath callers; display and freeze services are local fixtures. */
public final class FrameworkRuntimeProbe {
    private static int passed;
    private static native boolean preflight();
    private static native Surface newSurface(int fixture);
    private static native int registrations();
    private static native boolean recover(int fixture);
    private static native void clearFixtures();
    private static native int nativeDisplayFlags(long transaction, android.os.IBinder token);

    private static void check(boolean result, String label) {
        if (!result) throw new AssertionError(label);
        passed++;
        System.out.println("framework-runtime " + label + "=1");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Absolute fixture library path required");
        System.out.println("runtime-boot-classpath " + System.getProperty("java.boot.class.path"));
        System.load(args[0]);
        check(preflight(), "source-runtime-and-local-service");
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        Binder token = new Binder();
        Field nativeObject = SurfaceControl.Transaction.class.getDeclaredField("mNativeObject");
        nativeObject.setAccessible(true);
        long handle = nativeObject.getLong(transaction);
        boolean flagsOk = true;
        for (int flags : new int[] {0, 0x100000, -1, 0xa555aaaa}) {
            flagsOk &= transaction.setDisplayFlags(token, flags) == transaction;
            flagsOk &= nativeDisplayFlags(handle, token) == flags;
        }
        check(flagsOk, "surface-control-display-flags-jni-values");
        Field globalTransaction = SurfaceControl.class.getDeclaredField("sGlobalTransaction");
        globalTransaction.setAccessible(true);
        boolean staticOk;
        synchronized (SurfaceControl.class) {
            Object previous = globalTransaction.get(null);
            globalTransaction.set(null, transaction);
            try {
                SurfaceControl.setDisplayFlags(token, 0x100000);
                staticOk = nativeDisplayFlags(handle, token) == 0x100000;
            } finally {
                globalTransaction.set(null, previous);
            }
        }
        check(staticOk, "surface-control-display-flags-static-wrapper");
        boolean nullRejected = false;
        try { transaction.setDisplayFlags(null, 0); }
        catch (IllegalArgumentException expected) { nullRejected = true; }
        check(nullRejected, "surface-control-display-flags-null-token-validation");
        transaction.close();
        Surface empty = new Surface();
        empty.registerFreezeSelf();
        empty.release();
        empty.registerFreezeSelf();
        check(registrations() == 0, "empty-and-released-surface-noop");

        Surface direct = newSurface(0);
        direct.registerFreezeSelf();
        check(registrations() == 1, "java-surface-registers-producer");
        direct.registerFreezeSelf();
        check(registrations() == 1, "repeated-java-registration-is-shared");
        check(recover(0), "java-surface-jni-producer-recovery");

        final int[] setCalls = {0};
        final int[] stateCalls = {0};
        final boolean[] lastState = {false};
        IDisplayManager localDisplay = (IDisplayManager) Proxy.newProxyInstance(
                IDisplayManager.class.getClassLoader(), new Class<?>[] {IDisplayManager.class},
                (proxy, method, values) -> {
                    switch (method.getName()) {
                        case "getPreferredWideGamutColorSpaceId": return 0;
                        case "setVirtualDisplaySurface": setCalls[0]++; return null;
                        case "setVirtualDisplayState":
                            stateCalls[0]++; lastState[0] = (Boolean) values[1]; return null;
                        case "asBinder": return null;
                        default: throw new AssertionError("Unexpected display call: " + method.getName());
                    }
                });
        Constructor<DisplayManagerGlobal> globalConstructor =
                DisplayManagerGlobal.class.getDeclaredConstructor(IDisplayManager.class);
        globalConstructor.setAccessible(true);
        DisplayManagerGlobal global = globalConstructor.newInstance(localDisplay);
        Constructor<VirtualDisplay> constructor = VirtualDisplay.class.getDeclaredConstructor(
                DisplayManagerGlobal.class, Display.class, IVirtualDisplayCallback.class, Surface.class);
        constructor.setAccessible(true);
        Surface initial = newSurface(1);
        VirtualDisplay display = constructor.newInstance(global, null, null, initial);
        check(display.getSurface() == initial && recover(1), "virtual-display-constructor-recovery");
        Surface replacement = newSurface(2);
        display.setSurface(replacement);
        check(display.getSurface() == replacement && setCalls[0] == 1
                && stateCalls[0] == 1 && lastState[0] && recover(2),
                "virtual-display-set-surface-recovery");
        display.setSurface(replacement);
        check(setCalls[0] == 1 && stateCalls[0] == 1 && registrations() == 1,
                "unchanged-virtual-display-surface-noop");
        display.setSurface(null);
        check(display.getSurface() == null && setCalls[0] == 2 && stateCalls[0] == 2
                && !lastState[0] && registrations() == 1,
                "virtual-display-null-surface-detaches");
        direct.release();
        initial.release();
        replacement.release();
        clearFixtures();
        System.out.println("framework-runtime-probe passed=" + passed);
    }
}
