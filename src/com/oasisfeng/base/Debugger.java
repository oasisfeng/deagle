package com.oasisfeng.base;

import android.os.Debug;

/** @author Oasis */
public class Debugger {

    public static final void waitForever() {
        if (Versions.DEBUG) Debug.waitForDebugger();
    }

    /** @planned */
    public static final void tryAttach() { throw new UnsupportedOperationException(); }
}
