package com.oasisfeng.android.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.SharedPreferences;

/**
 * Guard against ClassCastException in getters of SharedPreferences, return default value if type mismatched.
 *
 * @author Oasis
 */
public class SafeSharedPreferences {

    public static SharedPreferences wrap(final SharedPreferences prefs) {
        if (Proxy.isProxyClass(prefs.getClass()) && Proxy.getInvocationHandler(prefs).getClass() == SafeWrapper.class) return prefs;
        return (SharedPreferences) Proxy.newProxyInstance(prefs.getClass().getClassLoader(),
                new Class<?>[] { SharedPreferences.class }, new SafeWrapper(prefs));
    }

    private static class SafeWrapper implements InvocationHandler {

        public SafeWrapper(final SharedPreferences prefs) {
            mPrefs = prefs;
        }

        @Override public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (args != null && args.length == 2 && method.getName().startsWith("get")) {   // getAll() is excluded by "args.length == 2"
                try {
                    return method.invoke(mPrefs, args);
                } catch (final ClassCastException e) {
                    return args[1];     // default value
                }
            } else return method.invoke(mPrefs, args);
        }

        private final SharedPreferences mPrefs;
    }
}
