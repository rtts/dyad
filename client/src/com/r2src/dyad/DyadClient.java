package com.r2src.dyad;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.os.Handler;

/**
 * A client to execute {@link DyadRequest}s.
 */
public class DyadClient {

	private volatile static DyadClient singleton;
	private volatile DefaultHttpClient client;
	public final ExecutorService executor = Executors.newCachedThreadPool();

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

	/**
	 * Helper method to spawn a worker thread making the web api request.
	 */
	public void asyncRequest(final DyadRequest request,
			final DyadAccount dyadAccount, final Foo foo, final Handler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler is null");
		if (foo == null)
			throw new IllegalArgumentException("foo is null");

		// thread magic!
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					HttpResponse response = client.execute(dyadAccount.host,
							request.getHttpRequest());
					request.onFinished(response, dyadAccount);
					handler.post(new Runnable() {
						public void run() {
							foo.onFinished();
						}
					});
				} catch (final IOException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
						}
					});
				} catch (final DyadServerException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
						}
					});
				}
			}
		});
	}

}
