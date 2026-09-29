// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package org.picomisu.runtime;

import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplay;
import android.view.Display;
import android.view.IExtSurface;
import android.graphics.Canvas;
import android.graphics.Color;
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
    private static native int surfaceReferences(Surface surface);
    private static native long producerOperations(int fixture);
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
        IExtSurface emptyExt = empty.getExt();
        boolean emptyLockRejected = false, emptyUnlockRejected = false;
        try { emptyExt.lockCanvasFor2DVr(); }
        catch (IllegalStateException expected) { emptyLockRejected = true; }
        try { emptyExt.unlockCanvasAndPostFor2DVr(new Canvas()); }
        catch (IllegalStateException expected) { emptyUnlockRejected = true; }
        check(emptyLockRejected && emptyUnlockRejected, "vr-canvas-released-surface-validation");
        IExtSurface extension = direct.getExt();
        check(extension == direct.getExt() && extension instanceof com.pico.util.IExtBase,
                "vr-canvas-extension-identity");
        int references = surfaceReferences(direct);
        long operations = producerOperations(0);
        Canvas canvas = extension.lockCanvasFor2DVr();
        canvas.drawColor(Color.GREEN);
        check(canvas.getWidth() == 1 && canvas.getHeight() == 1 && canvas.isOpaque()
                && surfaceReferences(direct) == references + 1 && producerOperations(0) == operations,
                "vr-canvas-native-pixel-and-retained-surface");
        boolean doubleLockRejected = false, wrongCanvasRejected = false;
        try { extension.lockCanvasFor2DVr(); }
        catch (IllegalArgumentException expected) { doubleLockRejected = true; }
        try { extension.unlockCanvasAndPostFor2DVr(new Canvas()); }
        catch (IllegalArgumentException expected) { wrongCanvasRejected = true; }
        check(doubleLockRejected && wrongCanvasRejected
                && surfaceReferences(direct) == references + 1,
                "vr-canvas-lock-and-identity-errors-preserve-reference");
        extension.unlockCanvasAndPostFor2DVr(canvas);
        boolean unlockedRejected = false;
        try { extension.unlockCanvasAndPostFor2DVr(canvas); }
        catch (IllegalStateException expected) { unlockedRejected = true; }
        check(unlockedRejected && canvas.getWidth() == 0 && canvas.getHeight() == 0
                && surfaceReferences(direct) == references && producerOperations(0) == operations,
                "vr-canvas-unlock-detaches-without-queue");
        boolean cycles = true;
        for (int cycle = 0; cycle < 10; ++cycle) {
            Canvas again = extension.lockCanvasFor2DVr();
            cycles &= again == canvas && again.getWidth() == 1;
            extension.unlockCanvasAndPostFor2DVr(again);
            cycles &= surfaceReferences(direct) == references;
        }
        check(cycles && producerOperations(0) == operations, "vr-canvas-repeated-lifetime");
        extension.lockCanvasFor2DVr();
        direct.transferFrom(initial);
        extension.unlockCanvasAndPostFor2DVr(canvas);
        Field locked = extension.getClass().getDeclaredField("mLockedObject");
        locked.setAccessible(true);
        check(direct.isValid() && !initial.isValid() && locked.getLong(extension) == 0
                && canvas.getWidth() == 0 && producerOperations(0) == operations,
                "vr-canvas-retains-original-surface-on-transfer");
        direct.release();
        initial.release();
        replacement.release();
        clearFixtures();
        System.out.println("framework-runtime-probe passed=" + passed);
    }
}
