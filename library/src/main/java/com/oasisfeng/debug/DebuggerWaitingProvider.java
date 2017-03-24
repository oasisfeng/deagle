package com.oasisfeng.debug;

import android.content.pm.ApplicationInfo;
import android.os.Debug;
import android.util.Log;

import com.oasisfeng.pattern.LocalContentProvider;

/**
 * Special content provider for easy debugger attaching at the starting stage of specific process.
 *
 * Declare this content provider in your AndroidManifest.xml, with "android:process" attribute set to the desired process.
 *
 * Created by Oasis on 2017/3/20.
 */
public class DebuggerWaitingProvider extends LocalContentProvider {

	private static final long WAITING_TIMEOUT = 3_000;

	@Override public boolean onCreate() {
		if ((context().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return false;	// Skip in non-debuggable build
		Log.d(TAG, "Start waiting for debugger...");
		mWaitingThread.start();
		try {
			synchronized (mSignal) { mSignal.wait(WAITING_TIMEOUT); }
		} catch (final InterruptedException ignored) {}
		if (Debug.isDebuggerConnected()) Log.d(TAG, "Debugger connected.");
		else Log.d(TAG, "Resume without debugger.");
		return false;
	}

	private final Thread mWaitingThread = new Thread() { @Override public void run() {
		Debug.waitForDebugger();
		synchronized (mSignal) { mSignal.notifyAll(); }
	}};

	private final Object mSignal = new Object();

	private static final String TAG = "Debug.Wait" ;
}
