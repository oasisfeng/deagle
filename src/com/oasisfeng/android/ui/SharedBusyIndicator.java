package com.oasisfeng.android.ui;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.oasisfeng.android.base.Versions;

/**
 * Shared busy indicator (indeterminate progress bar) for activity, with reference count.
 *
 * You must call {@link Activity#requestWindowFeature(int)} with {@link Window#FEATURE_INDETERMINATE_PROGRESS} first
 * (usually in {@link Activity#onCreate(Bundle)} ).
 *
 * @author Oasis
 */
public class SharedBusyIndicator {

    public class AcquiredBusyIndicator {

        AcquiredBusyIndicator() {
            if (mCount.incrementAndGet() == 1)
                mActivity.setProgressBarIndeterminateVisibility(true);
        }

        public synchronized void release() {
            if (released) {
                if (Versions.DEBUG) throw new IllegalStateException("Already released");
                else return;
            }
            if (mCount.decrementAndGet() == 0)
                mActivity.setProgressBarIndeterminateVisibility(false);
            released = true;
        }

        private boolean released;
    }

    /** BusyIndicator should only be created only once for an activity */
    public SharedBusyIndicator(final Activity activity) {
        mActivity = activity;
    }

    public AcquiredBusyIndicator acquire() {
        return new AcquiredBusyIndicator();     // TODO: Use weak reference to track leaking.
    }

    private final Activity mActivity;
    private final AtomicInteger mCount = new AtomicInteger();
}
