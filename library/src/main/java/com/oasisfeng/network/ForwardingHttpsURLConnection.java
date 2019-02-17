package com.oasisfeng.network;

import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

/**
 * For {@link HttpsURLConnection} forwarding.
 *
 * Created by Oasis on 2019-2-17.
 */
class ForwardingHttpsURLConnection extends ForwardingHttpURLConnection {

	public String getCipherSuite() { return delegate().getCipherSuite(); }

	public Certificate[] getLocalCertificates() { return delegate().getLocalCertificates(); }

	public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException { return delegate().getServerCertificates(); }

	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException { return delegate().getPeerPrincipal(); }

	public Principal getLocalPrincipal() { return delegate().getLocalPrincipal(); }

	public static void setDefaultHostnameVerifier(final HostnameVerifier v) { HttpsURLConnection.setDefaultHostnameVerifier(v); }

	public static HostnameVerifier getDefaultHostnameVerifier() { return HttpsURLConnection.getDefaultHostnameVerifier(); }

	public void setHostnameVerifier(final HostnameVerifier v) { delegate().setHostnameVerifier(v); }

	public HostnameVerifier getHostnameVerifier() { return delegate().getHostnameVerifier(); }

	public static void setDefaultSSLSocketFactory(final SSLSocketFactory sf) { HttpsURLConnection.setDefaultSSLSocketFactory(sf); }

	public static SSLSocketFactory getDefaultSSLSocketFactory() { return HttpsURLConnection.getDefaultSSLSocketFactory(); }

	public void setSSLSocketFactory(final SSLSocketFactory sf) { delegate().setSSLSocketFactory(sf); }

	public SSLSocketFactory getSSLSocketFactory() { return delegate().getSSLSocketFactory(); }

	private HttpsURLConnection delegate() {  return (HttpsURLConnection) mDelegate; }

	ForwardingHttpsURLConnection(final HttpsURLConnection delegate, final URL url) { super(delegate, url); }
}
