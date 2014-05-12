package com.oasisfeng.android.i18n;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

/** @author Oasis */
public class Locales {

    public static Locale getFrom(final Context context) {
        return context.getResources().getConfiguration().locale;
    }

    /**
     * Switch the locale of current app to explicitly specified or default one
     *
     * @param locale null for default
     */
    public static boolean switchTo(final Context context, final Locale locale) {
        final Resources resources = context.getResources();
        final Configuration configuration = resources.getConfiguration();
        if (match(locale, configuration.locale)) return false;
        setConfigurationLocale(configuration, locale);
        resources.updateConfiguration(configuration, null);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static void setConfigurationLocale(final Configuration configuration, final Locale locale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            configuration.locale = locale;
        else configuration.setLocale(locale);
    }

    private static boolean match(final Locale locale1, final Locale locale2) {
    	return match(locale1.getLanguage(), locale2.getLanguage())
    			&& match(locale1.getCountry(), locale2.getCountry())
    			&& match(locale1.getVariant(), locale2.getVariant());
	}

    private static boolean match(final String value1, final String value2) {
    	return (value1 == null && value2 == null) || "".equals(value1) || "".equals(value2)
    			|| (value1 != null && value1.equals(value2));
    }
}
