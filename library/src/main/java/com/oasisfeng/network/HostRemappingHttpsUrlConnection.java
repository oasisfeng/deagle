package com.oasisfeng.network;

import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * ForwardingURLConnection for host remapping
 *
 * Created by Oasis on 2015/11/20.
 */
public class HostRemappingHttpsUrlConnection extends ForwardingHttpsURLConnection {

	public static boolean install(final Map<String/* original host */, String/* mapped IP / host */> mapping) {
		try {
			new URL("https://www.google.com/");
		} catch (final MalformedURLException e) {	// URL() will check stream handler.
			Log.e(TAG, "No URL handler for HTTPS: " + e);
			return false;
		}
		final Hashtable<String, URLStreamHandler> stream_handlers;
		try {
			final Field URL_handlers = URL.class.getDeclaredField(SDK_INT >= N ? "handlers" : "streamHandlers");
			URL_handlers.setAccessible(true);
			//noinspection unchecked
			stream_handlers = (Hashtable<String, URLStreamHandler>) URL_handlers.get(null);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
			Log.e(TAG, "Partially incompatible ROM", e);
			return false;
		}
		final URLStreamHandler https_handler = stream_handlers.get("https");
		if (https_handler instanceof HostRemappingHttpsURLStreamHandler) ((HostRemappingHttpsURLStreamHandler) https_handler).putAll(mapping);
		else stream_handlers.put("https", new HostRemappingHttpsURLStreamHandler(https_handler, mapping));
		return true;
	}

	protected HostRemappingHttpsUrlConnection(final URL url) throws IOException {
		super((HttpsURLConnection) url.openConnection(), url);
	}

	private static class HostRemappingHttpsURLStreamHandler extends ForwardingURLStreamHandler {

		@Override protected URLConnection openConnection(final URL u) throws IOException {
			final String mapped = mHostMapping.get(u.getHost());
			final URL url = mapped == null ? u : new URL(u.getProtocol(), mapped, u.getPort(), u.getFile());
			Log.d(TAG, "Request " + (u == url ? u : (u + " -> " + mapped)));
			final HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(url);
			final String host = u.getHost();
			connection.setRequestProperty("Host", host);
			connection.setHostnameVerifier((hostname, session) -> true);
			return connection;
		}

		void putAll(final Map<String, String> mapping) {
			mHostMapping.putAll(mapping);
		}

		HostRemappingHttpsURLStreamHandler(final URLStreamHandler downstream, final Map<String, String> mapping) {
			super(downstream);
			mHostMapping = mapping;
		}

		private final Map<String, String> mHostMapping;
	}

	private static final String TAG = "HttpRP";
}
