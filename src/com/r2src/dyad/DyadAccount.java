package com.r2src.dyad;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

/**
 * An account with all of its state stored server-side. Once registered, it only
 * needs to hold on to it's session token, which you can also pass to the
 * constructor to construct a registered account.
 * <p>
 * Dyad Server API version 1 accepts re-registration of an account and will
 * return a new session token when that happens. This should be useful to
 * <ol>
 * <li>Update the c2dm_id when it has changed
 * <li>Refresh the session token when it expires
 * <li>Transfer existing Dyad Accounts to new devices.
 * </ol>
 * All the methods of this class are non-blocking and safe to call from the UI
 * thread.
 */
public class DyadAccount {

	private final HttpHost host;
	private String sessionToken;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private DyadClient client = DyadClient.getInstance();

	/**
	 * Creates a local account that has not yet been registered with the server.
	 * 
	 * @param host
	 *            The host that runs Dyad Server.
	 */
	public DyadAccount(HttpHost host) {
		this(host, null);
	}

	/**
	 * (Re)creates an account that has already been registered with the server.
	 * 
	 * @param host
	 *            The host that runs Dyad Server.
	 * 
	 * @param sessionToken
	 *            The session token of an existing Dyad Account.
	 */
	public DyadAccount(HttpHost host, String sessionToken) {
		this.host = host;
		this.sessionToken = sessionToken;
	}

	/**
	 * Returns the host for this account
	 */
	public HttpHost getHost() {
		return host;
	}

	/**
	 * Returns the account's session token (please store it somewhere safe!).
	 */
	public String getSessionToken() {
		return sessionToken;
	}

	/**
	 * Sets the account's session token.
	 */
	public void setSessionToken(String token) {
		sessionToken = token;
	}

	/**
	 * Registers the account with the Dyad server.
	 * 
	 * @param authToken
	 *            An auth token as returned by
	 * 
	 *            <pre>
	 * AccountManagerFuture&lt;Bundle&gt; future = manager.getAuthTokenByFeatures(
	 * 		&quot;com.google&quot;, &quot;oauth2:https://www.googleapis.com/auth/userinfo.email&quot;,
	 * 		null, activity, null, null, null, null);
	 * Bundle bundle = future.getResult(); // blocking call
	 * authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
	 * </pre>
	 * 
	 * @param c2dm_id
	 *            The C2DM registration id of this device. Needed to receive
	 *            push messages.
	 * 
	 * @param foo
	 *            A {@link Foo} object providing the callbacks for this request.
	 * 
	 * @param handler
	 *            The Android {@link Handler} of the thread the callbacks should
	 *            be run on.
	 */
	public void register(final String authToken, final String c2dm_id,
			final Foo foo, final Handler handler) {
		if (authToken == null)
			throw new IllegalArgumentException("authToken is null");
		if (c2dm_id == null)
			throw new IllegalArgumentException("c2dm_id is null");

		DyadRequest request = new DyadRegistrationRequest(authToken, c2dm_id);
		asyncRequest(request, foo, handler);
	}

	/**
	 * Starts the process of forming a Dyad.
	 * <p>
	 * This method will start the supplied {@link Bonder} activity to perform
	 * the actual bonding. After the bonding process is complete, this method
	 * will send the resulting shared secret to the Dyad Server.
	 * <p>
	 * When the server receives two matching secrets, the Dyad is complete TODO
	 */
	public void requestDyad(Activity activity, Bonder bonder, final Foo foo,
			final Handler handler) {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);
		lbm.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final String secret = intent.getStringExtra("secret");
				DyadRequest request = new DyadBondRequest(secret);
				try {
					authenticate(request);
				} catch (final NotRegisteredException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
						}
					});
				}
				asyncRequest(request, foo, handler);

			}
		}, new IntentFilter("com.r2src.dyad.action.GOT_SHARED_SECRET"));

		activity.startActivity(new Intent(activity, bonder.getClass()));
	}

	/**
	 * Fetches the list of Dyads from the server.
	 */
	public Future<List<Dyad>> getDyads() {
		// TODO: method stub
		return null;
	}

	/**
	 * Authenticates a {@link DyadRequest} by adding a custom header containing
	 * the session token.
	 * 
	 * @param request
	 *            The request to be authenticated.
	 * 
	 * @throws NotRegisteredException
	 *             If there is no session token. Handle this by calling
	 *             {@link #register} first.
	 */
	public void authenticate(DyadRequest request) throws NotRegisteredException {
		if (sessionToken == null)
			throw new NotRegisteredException();
		request.getHttpRequest().addHeader("X-Dyad-Authentication",
				sessionToken.toString());
	}

	private class NotRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Helper method to spawn a worker thread making the web api request.
	 */
	private void asyncRequest(final DyadRequest request, final Foo foo,
			final Handler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler is null");
		if (foo == null)
			throw new IllegalArgumentException("foo is null");

		// thread magic!
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					HttpResponse response = client.execute(host, request);
					request.onFinished(response, DyadAccount.this);
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
