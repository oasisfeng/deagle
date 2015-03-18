package com.oasisfeng.android.util;

/**
 * Used in multi-catch clause together with other {@link ReflectiveOperationException} derived exception types,
 * to avoid {@link NoClassDefFoundError} before Android 4.4.
 *
 * Created by Oasis on 2015/3/11.
 */
public class MultiCatchROECompat extends RuntimeException {}
