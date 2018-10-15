package com.oasisfeng.android.base;

import android.text.format.DateUtils;

import java.util.Locale;

/** @author Oasis */
public class Format {

	public static String elapsedTime(final long elapsedSeconds) {
		if (elapsedSeconds < 100 * 60 * 60) return DateUtils.formatElapsedTime(null, elapsedSeconds);

		final long hours = elapsedSeconds / 3600, minutes;
		long seconds = hours % 3600;
		if (seconds >= 60) {
			minutes = seconds / 60;
			seconds = minutes % 60;
		} else minutes = 0;
		return String.format((Locale) null, "%1$d:%2$02d:%3$02d", hours, minutes, seconds);
	}
}
