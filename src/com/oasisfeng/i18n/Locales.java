package com.oasisfeng.i18n;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

/** @author Oasis */
public class Locales {

    /**
     * Switch the locale of current app to explicitly specified or default one
     *
     * @param locale null for default
     */
    public static void switchTo(final Context context, final Locale locale) {
        final Resources resources = context.getResources();
        final Configuration configuration = resources.getConfiguration();
        setConfigurationLocale(configuration, locale);
        resources.updateConfiguration(configuration, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static void setConfigurationLocale(final Configuration configuration, final Locale locale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            configuration.locale = locale;
        else configuration.setLocale(locale);
    }
}
