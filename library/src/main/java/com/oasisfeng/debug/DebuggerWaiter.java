package com.oasisfeng.debug;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import com.oasisfeng.pattern.PseudoContentProvider;

/**
 * Special content provider for easy debugger attaching at the starting stage of specific process.
 *
 * Declare this content provider in your AndroidManifest.xml, with "android:process" attribute set to the desired process.
 *
 * Created by Oasis on 2017/3/20.
 */
public class DebuggerWaiter extends PseudoContentProvider {

	private static final long WAITING_TIMEOUT = 3_000;

	public static void waitIfNeeded(final Context context) {
		if (shouldWait(context)) new Waiter().doWait();
	}

	@Override public boolean onCreate() {
		waitIfNeeded(context());
		return false;
	}

	private static boolean shouldWait(final Context context) {
		// Skip in non-debuggable build. (BuildConfig.DEBUG may not be true since Deagle is a library dependency which is probably built as release)
		if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return false;
		// Skip if USB is not connected except on emulator. TODO: Support ADB over TCP.
		if (! Build.FINGERPRINT.contains("generic")) {
			final Intent usb_state = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
			if (usb_state == null || ! usb_state.getBooleanExtra("connected", false)) return false;
		}
		// Skip if running in main process. (IDE could attach debugger to main process upon launch)
		final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		final int pid = android.os.Process.myPid();
		for (final ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses())
			if (process.pid == pid) {
				if (process.pkgList[0].equals(process.processName)) return false;
				break;
			}
		return true;
	}

	static final class Waiter {

		void doWait() {
			Log.d(TAG, "Start waiting for debugger...");
			mWaitingThread.start();
			try {
				synchronized (mSignal) { mSignal.wait(WAITING_TIMEOUT); }
			} catch (final InterruptedException ignored) {}
			if (Debug.isDebuggerConnected()) Log.d(TAG, "Debugger connected.");
			else Log.d(TAG, "Resume without debugger.");
		}

		private final Thread mWaitingThread = new Thread() { @Override public void run() {
			Debug.waitForDebugger();
			synchronized (mSignal) { mSignal.notifyAll(); }
		}};

		private final Object mSignal = new Object();
	}

	private static final String TAG = "Debug.Wait" ;
}
