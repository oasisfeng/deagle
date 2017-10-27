package com.oasisfeng.debug;

import android.os.Build;

/**
 * Emulator-related helpers
 *
 * Created by Oasis on 2017/10/25.
 */
public class Emulator {

	public static boolean isRunningInEmulator() {
		if (sRunningInEmulator == null) sRunningInEmulator = checkRunningInEmulator();
		return sRunningInEmulator;
	}

	private static boolean checkRunningInEmulator() {
		return Build.FINGERPRINT.startsWith("generic")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator")
				|| Build.MODEL.contains("Android SDK built for x86")
				|| "google_sdk".equals(Build.PRODUCT);
	}

	private static Boolean sRunningInEmulator;
}
