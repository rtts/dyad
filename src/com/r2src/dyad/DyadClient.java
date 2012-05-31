package com.r2src.dyad;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

public class DyadClient {

	private volatile static DyadClient singleton;
	private volatile DefaultHttpClient client;

	private DyadClient() {
		client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);
	}

	public static DyadClient getInstance() {
		if (singleton == null) {
			synchronized (DyadClient.class) {
				if (singleton == null) {
					singleton = new DyadClient();
				}
			}
		}
		return singleton;
	}

	public HttpResponse execute(HttpHost host, DyadRequest request)
			throws DyadServerException, IOException, ClientProtocolException {
		HttpRequest httpRequest = request.getHttpRequest();
		return client.execute(host, httpRequest);
	}

}
