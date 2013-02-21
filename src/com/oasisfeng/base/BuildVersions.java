package com.oasisfeng.base;

import android.os.Build;
import android.os.Build.VERSION_CODES;

/** @author Oasis */
public class BuildVersions extends VERSION_CODES {

    /**
     * Convenient helper for better granularity with @{@link android.annotation.TargetApi TargetApi}.
     *
     * <p>Example code snippet:<pre>
       BuildVersions.since(VERSION_CODES.ICE_CREAM_SANDWICH, new Runnable() { @TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH) @Override public void run() {
            ...
       }});</pre>
     */
    public static void since(final int level, final Runnable code) {
        if (Build.VERSION.SDK_INT >= level) code.run();
    }
}
