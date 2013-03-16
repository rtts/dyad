package com.r2src.dyad;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import com.r2src.dyad.request.DyadRequest;

import android.os.Handler;

/**
 * A client to execute {@link DyadRequests}.
 * 
 */
public class DyadClient {
	private volatile DefaultHttpClient client;
	public final ExecutorService executor = Executors.newCachedThreadPool();

	public DyadClient() {
		// initialize thread-safe http client
		client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);
	}

	/**
	 * Executes a {@link DyadRequest} and returns the response.
	 * 
	 * @param request
	 * @param dyadAccount
	 */
	public HttpResponse execute(DyadRequest request, DyadAccount dyadAccount)
			throws IOException, DyadServerException {
		HttpResponse response = client.execute(dyadAccount.getHost(),
				request.getHttpRequest());
		request.onFinished(response, dyadAccount);
		return response;
	}

	/**
	 * Spawns a worker thread making the request. TODO: It is also possible to
	 * let asyncRequest return a Future<HttpResponse> Simply change the return
	 * type and change the Runnable into a Callable.
	 */
	public void asyncExecute(final DyadRequest request,
			final DyadAccount dyadAccount, final DyadRequestCallback callback,
			final Handler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler is null");
		if (callback == null)
			throw new IllegalArgumentException("foo is null");

		// thread magic!
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					execute(request, dyadAccount);
					handler.post(new Runnable() {
						public void run() {
							callback.onFinished();
						}
					});
				} catch (final IOException e) {
					handler.post(new Runnable() {
						public void run() {
							callback.onError(e);
						}
					});
				} catch (final DyadServerException e) {
					handler.post(new Runnable() {
						public void run() {
							callback.onError(e);
						}
					});
				}
			}
		});
	}
}
