package com.oasisfeng.android.base;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;

/** @author Oasis */
public class Format {

    @SuppressLint("DefaultLocale")    // TODO: Use locale format
    public static String elapsedTime(final StringBuilder recycle, final long elapsedSeconds) {
        if (elapsedSeconds < 100 * 60 * 60) return DateUtils.formatElapsedTime(recycle, elapsedSeconds);

        long seconds = elapsedSeconds, hours = 0, minutes = 0;
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds = hours % 3600;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds = minutes % 60;
        }
        return String.format("%1$d:%2$02d:%3$02d", hours, minutes, seconds);
    }
}
