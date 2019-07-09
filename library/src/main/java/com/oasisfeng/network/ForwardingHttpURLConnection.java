package com.oasisfeng.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;

import static android.os.Build.VERSION_CODES.N;

/**
 * For {@link HttpURLConnection} forwarding.
 *
 * Created by Oasis on 2015/11/20.
 */
class ForwardingHttpURLConnection extends HttpURLConnection {

	ForwardingHttpURLConnection(final HttpURLConnection delegate, final URL url) {
		super(url);
		mDelegate = delegate;
	}

	public void disconnect() {
		mDelegate.disconnect();
	}

	public boolean usingProxy() {
		return mDelegate.usingProxy();
	}

	public void connect() throws IOException {
		mDelegate.connect();
	}

	public boolean getAllowUserInteraction() {
		return mDelegate.getAllowUserInteraction();
	}

	public Object getContent() throws IOException {
		return mDelegate.getContent();
	}

	public Object getContent(final Class[] types) throws IOException {
		return mDelegate.getContent(types);
	}

	public String getContentEncoding() {
		return mDelegate.getContentEncoding();
	}

	public int getContentLength() {
		return mDelegate.getContentLength();
	}

	@RequiresApi(N) @Override public long getContentLengthLong() {
		return mDelegate.getContentLengthLong();
	}

	public String getContentType() {
		return mDelegate.getContentType();
	}

	public long getDate() {
		return mDelegate.getDate();
	}

	public boolean getDefaultUseCaches() {
		return mDelegate.getDefaultUseCaches();
	}

	public boolean getDoInput() {
		return mDelegate.getDoInput();
	}

	public boolean getDoOutput() {
		return mDelegate.getDoOutput();
	}

	public long getExpiration() {
		return mDelegate.getExpiration();
	}

	public String getHeaderField(final int pos) {
		return mDelegate.getHeaderField(pos);
	}

	public Map<String, List<String>> getHeaderFields() {
		return mDelegate.getHeaderFields();
	}

	public Map<String, List<String>> getRequestProperties() {
		return mDelegate.getRequestProperties();
	}

	public void addRequestProperty(final String field, final String newValue) {
		mDelegate.addRequestProperty(field, newValue);
	}

	public String getHeaderField(final String key) {
		return mDelegate.getHeaderField(key);
	}

	public long getHeaderFieldDate(final String field, final long defaultValue) {
		return mDelegate.getHeaderFieldDate(field, defaultValue);
	}

	public int getHeaderFieldInt(final String field, final int defaultValue) {
		return mDelegate.getHeaderFieldInt(field, defaultValue);
	}

	@RequiresApi(N) @Override public long getHeaderFieldLong(final String name, final long Default) {
		return mDelegate.getHeaderFieldLong(name, Default);
	}

	public String getHeaderFieldKey(final int posn) {
		return mDelegate.getHeaderFieldKey(posn);
	}

	public long getIfModifiedSince() {
		return mDelegate.getIfModifiedSince();
	}

	public InputStream getInputStream() throws IOException {
		return mDelegate.getInputStream();
	}

	public long getLastModified() {
		return mDelegate.getLastModified();
	}

	public OutputStream getOutputStream() throws IOException {
		return mDelegate.getOutputStream();
	}

	public Permission getPermission() throws IOException {
		return mDelegate.getPermission();
	}

	public String getRequestProperty(final String field) {
		return mDelegate.getRequestProperty(field);
	}

	public URL getURL() {
		return mDelegate.getURL();
	}

	public boolean getUseCaches() {
		return mDelegate.getUseCaches();
	}

	public void setAllowUserInteraction(final boolean newValue) {
		mDelegate.setAllowUserInteraction(newValue);
	}

	public void setDefaultUseCaches(final boolean newValue) {
		mDelegate.setDefaultUseCaches(newValue);
	}

	public void setDoInput(final boolean newValue) {
		mDelegate.setDoInput(newValue);
	}

	public void setDoOutput(final boolean newValue) {
		mDelegate.setDoOutput(newValue);
	}

	public void setIfModifiedSince(final long newValue) {
		mDelegate.setIfModifiedSince(newValue);
	}

	public void setRequestProperty(final String field, final String newValue) {
		mDelegate.setRequestProperty(field, newValue);
	}

	public void setUseCaches(final boolean newValue) {
		mDelegate.setUseCaches(newValue);
	}

	public void setConnectTimeout(final int timeoutMillis) {
		mDelegate.setConnectTimeout(timeoutMillis);
	}

	public int getConnectTimeout() {
		return mDelegate.getConnectTimeout();
	}

	public void setReadTimeout(final int timeoutMillis) {
		mDelegate.setReadTimeout(timeoutMillis);
	}

	public int getReadTimeout() {
		return mDelegate.getReadTimeout();
	}

	public String toString() {
		return mDelegate.toString();
	}

	public InputStream getErrorStream() {
		return mDelegate.getErrorStream();
	}

	public String getRequestMethod() {
		return mDelegate.getRequestMethod();
	}

	public int getResponseCode() throws IOException {
		return mDelegate.getResponseCode();
	}

	public String getResponseMessage() throws IOException {
		return mDelegate.getResponseMessage();
	}

	public void setRequestMethod(final String method) throws ProtocolException {
		mDelegate.setRequestMethod(method);
	}

	public boolean getInstanceFollowRedirects() {
		return mDelegate.getInstanceFollowRedirects();
	}

	public void setInstanceFollowRedirects(final boolean followRedirects) {
		mDelegate.setInstanceFollowRedirects(followRedirects);
	}

	public void setFixedLengthStreamingMode(final long contentLength) {
		mDelegate.setFixedLengthStreamingMode(contentLength);
	}

	public void setFixedLengthStreamingMode(final int contentLength) {
		mDelegate.setFixedLengthStreamingMode(contentLength);
	}

	public void setChunkedStreamingMode(final int chunkLength) {
		mDelegate.setChunkedStreamingMode(chunkLength);
	}

	final HttpURLConnection mDelegate;
}
