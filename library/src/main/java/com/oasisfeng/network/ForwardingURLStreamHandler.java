package com.oasisfeng.network;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Created by Oasis on 2015/11/20.
 */
abstract class ForwardingURLStreamHandler extends URLStreamHandler {

	ForwardingURLStreamHandler(final URLStreamHandler delegate) {
		this.delegate = delegate;
	}

	protected URLConnection openConnection(final URL u) throws IOException {
		try {
			return (URLConnection) URLStreamHandler_openConnection.invoke(delegate, u);
		} catch (final InvocationTargetException e) {
			final Throwable actual = e.getTargetException();
			if (actual instanceof IOException) throw (IOException) actual;
			if (actual instanceof RuntimeException) throw (RuntimeException) actual;
			throw new RuntimeException(actual);
		} catch (final IllegalAccessException e) { throw new IllegalStateException(e); }    // Should never happen.
	}

	protected URLConnection openConnection(final URL u, final Proxy proxy) throws IOException {
		try {
			return (URLConnection) URLStreamHandler_openConnection_Proxy.invoke(delegate, u, proxy);
		} catch (final InvocationTargetException e) {
			final Throwable actual = e.getTargetException();
			if (actual instanceof IOException) throw (IOException) actual;
			if (actual instanceof RuntimeException) throw (RuntimeException) actual;
			throw new RuntimeException(actual);
		} catch (final IllegalAccessException e) { throw new IllegalStateException(e); }    // Should never happen.
	}

	private final URLStreamHandler delegate;

	private static final Method URLStreamHandler_openConnection;
	private static final Method URLStreamHandler_openConnection_Proxy;

	static {
		try {
			URLStreamHandler_openConnection = URLStreamHandler.class.getDeclaredMethod("openConnection", URL.class);
			URLStreamHandler_openConnection.setAccessible(true);
			URLStreamHandler_openConnection_Proxy = URLStreamHandler.class.getDeclaredMethod("openConnection", URL.class, Proxy.class);
			URLStreamHandler_openConnection_Proxy.setAccessible(true);
		} catch (final NoSuchMethodException e) {
			throw new IllegalStateException("Incompatible ROM: No method URLStreamHandler.openConnection(URL)");
		}
	}
}
