package com.oasisfeng.base;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;

/** @author Oasis */
public class Format {

    @SuppressLint("DefaultLocale")    // TODO: Use locale format
    public static String elapsedTime(final StringBuilder recycle, long elapsedSeconds) {
        if (elapsedSeconds < 100 * 60 * 60) return DateUtils.formatElapsedTime(recycle, elapsedSeconds);

        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 3600) {
            hours = elapsedSeconds / 3600;
            elapsedSeconds -= hours * 3600;
        }
        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;
        return String.format("%1$d:%2$02d:%3$02d", hours, minutes, seconds);
    }
}
