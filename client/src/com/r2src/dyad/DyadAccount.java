package com.r2src.dyad;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gcm.GCMRegistrar;

/**
 * An account with all of its state stored server-side. Once registered, it only
 * needs to hold on to it's session token, which you can also pass to the
 * constructor to construct a registered account.
 * <p>
 * Dyad Server API version 1 accepts re-registration of an account and will
 * return a new session token when that happens. This should be useful to
 * <ol>
 * <li>Refresh the auth token when it expires
 * <li>Transfer existing Dyad Accounts to new devices.
 * </ol>
 * All the methods of this class are non-blocking and safe to call from the UI
 * thread.
 */
public class DyadAccount {
	
	private final String senderId;
	private final HttpHost host;
	private String sessionToken;
	private List<Dyad> dyads;
	private volatile DefaultHttpClient client;
	public final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * Broadcasted when the registration process succeeded.
	 * The Intent includes a field {@link #KEY_GOOGLE_ACCOUNT_NAME}.
	 */
	public static final String ACTION_REGISTERED_INTENT = "com.r2src.dyad.REGISTERED";
	/**
	 * Broadcasted when the registration process could not be finished.
	 * The Intent includes a field {@link #KEY_EXCEPTION}
	 */
	public static final String ACTION_REGISTRATION_FAILED_INTENT = "com.r2src.dyad.REGISTRATION_FAILED";

	/**
	 * Broadcasted when the registration process succeeded.
	 * The Intent includes a field {@link #KEY_GOOGLE_ACCOUNT_NAME}.
	 */
	public static final String ACTION_GCM_REGISTERED_INTENT = "com.r2src.dyad.GCM_REGISTERED";
	/**
	 * Broadcasted when the registration process could not be finished.
	 * The Intent includes a field {@link #KEY_EXCEPTION}
	 */
	public static final String ACTION_GCM_REGISTRATION_FAILED_INTENT = "com.r2src.dyad.GCM_REGISTRATION_FAILED";
	
	/**
	 * Bundle key used for the Google account name in Intents that send information about the registration process.
	 */
	public static final String KEY_GOOGLE_ACCOUNT_NAME = "googleAccountName";
	/**
	 * Bundle key used to communicate an exception message.
	 */
	public static final String KEY_EXCEPTION = "exception";

	/**
	 * Creates a local account that has not yet been registered with the server.
	 * 
	 * @param senderId
	 * 	          The GCM project ID from the URL at the API Console
	 * @param host
	 *            The host that runs Dyad Server.
	 */
	public DyadAccount(String senderId, HttpHost host) {
		this(senderId, host, null);
	}

	/**
	 * (Re)creates an account that has already been registered with the server.
	 * 
	 * @param senderId
	 * 	          The GCM project ID from the URL at the API Console
	 * 
	 * @param host
	 *            The host that runs Dyad Server.
	 * 
	 * @param sessionToken
	 *            The session token of an existing Dyad Account.
	 */
	public DyadAccount(String senderId, HttpHost host, String sessionToken) {
		if (senderId == null) throw new IllegalArgumentException("Sender ID is null");
		this.senderId = senderId;
		if (host == null) throw new IllegalArgumentException("Host is null");
		this.host = host;
		this.sessionToken = sessionToken; // allowed to be null
		
		// initialize thread-safe http client
		client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);
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
	 * Registers the account with the Dyad Server. Returns immediately and will
	 * broadcast either a {@link #ACTION_REGISTERED_INTENT} when finished, or a
	 * {@link #ACTION_REGISTRATION_FAILED_INTENT} on error.
	 * <p>
	 * This method calls the Account Manager's
	 * {@link AccountManager#getAuthTokenByFeatures} method which lets the user
	 * choose a Google account to register with the Dyad server. The chosen
	 * account name will be available in the REGISTERED intent so you can store
	 * it somewhere.
	 */
	public void register(final Activity activity) {
		if (activity == null)
			throw new IllegalArgumentException("activity is null");

		AccountManager.get(activity).getAuthTokenByFeatures("com.google",
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, null, null, new AccountRegistrationCallback(activity), new Handler());
	}

	/**
	 * Registers the account with the Dyad Server. Returns immediately and will
	 * broadcast either a {@link #ACTION_REGISTERED_INTENT} when finished, or a
	 * {@link #ACTION_REGISTRATION_FAILED_INTENT} on error.
	 * <p>
	 * This method expects the Google account name to be registered as its
	 * second argument. Useful if you already know which Google account to
	 * register.
	 * <p>
	 * The chosen account name will be available in the REGISTERED intent so you
	 * can store it somewhere.
	 */
	public void register(final Activity activity, String accountName) {
		if (activity == null)
			throw new IllegalArgumentException("activity is null");
		if (accountName == null)
			throw new IllegalArgumentException("accountName is null");

		Account account = null;
		AccountManager manager = AccountManager.get(activity);
		Account[] accounts = manager.getAccounts();
		for (Account a : accounts) {
			if (a.name.equals(accountName)) {
				account = a;
				break;
			}
		}
		if (account == null) {
			throw new IllegalArgumentException("account " + accountName
					+ " does not exist");
		}

		manager.getAuthToken(account,
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, new AccountRegistrationCallback(activity), new Handler());
	}

	/**
	 * Registers the device's GCM registration id with the Dyad Server. Upon
	 * success, the ACTION_GCM_REGISTERED_INTENT will be broadcast. Otherwise, expect
	 * an ACTION_GCM_REGISTRATION_FAILED_INTENT.
	 * <p>
	 * @throws UnsupportedOperationException If the device running the app cannot handle GCM appropriately.
	 * @throws IllegalStateException If the Android Manifest doesn't meet all the GCM requirements.
	 */
	public void registerGCM(final Activity activity) {
		if (activity == null)
			throw new IllegalArgumentException("activity is null");

		GCMRegistrar.checkDevice(activity);
		GCMRegistrar.checkManifest(activity);
		final String regId = GCMRegistrar.getRegistrationId(activity);
		if (regId.equals("")) {
			GCMRegistrar.register(activity, senderId);
		} else {
			DyadRequest request = new DyadGCMRequest(regId);
			asyncExecute(request, this, new DyadRequestCallback() {
				LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);
				@Override
				public void onFinished() {
					lbm.sendBroadcast(new Intent(ACTION_GCM_REGISTERED_INTENT));
				}

				@Override
				public void onError(Exception e) {
					lbm.sendBroadcast(new Intent(ACTION_GCM_REGISTRATION_FAILED_INTENT).putExtra(KEY_EXCEPTION, e.getLocalizedMessage()));
				}

				@Override
				public void onCanceled() {
					lbm.sendBroadcast(new Intent(ACTION_GCM_REGISTRATION_FAILED_INTENT));
				}
			}, new Handler());
		}
	}

	/**
	 * Starts the process of forming a Dyad.
	 * <p>
	 * This method will start the supplied {@link Bonder} activity to perform
	 * the actual bonding. After the bonding process is complete, this method
	 * will send the resulting shared secret to the Dyad Server.
	 * <p>
	 * When the server receives two matching secrets, the Dyad is complete.
	 */
	public void requestDyad(Activity activity, Bonder bonder, final DyadRequestCallback foo,
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
				asyncExecute(request, DyadAccount.this, foo, handler);

			}
		}, new IntentFilter("com.r2src.dyad.action.GOT_SHARED_SECRET"));

		activity.startActivity(new Intent(activity, bonder.getClass()));
	}

	/**
	 * Fetches the list of Dyads from the server.
	 */
	public void getDyads(String sessionToken, DyadRequestCallback foo, Handler handler) {
		if (sessionToken == null)
			throw new IllegalArgumentException("Session Token is null");
		DyadRequest request = new DyadsRequest(sessionToken);
		asyncExecute(request, this, foo, handler);
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

	/**
	 * Used as the argument for {@link AccountManager#getAuthToken} and
	 * {@link AccountManager#getAuthTokenByFeatures}. Broadcasts the REGISTERED
	 * or REGISTRATION_FAILED intents.
	 */
	private class AccountRegistrationCallback implements AccountManagerCallback<Bundle> {
		LocalBroadcastManager lbm;
		
		public AccountRegistrationCallback(Context context) {
			super();
			lbm = LocalBroadcastManager.getInstance(context);
		}
		
		@Override
		public void run(AccountManagerFuture<Bundle> future) {
			Bundle b;
			try {
				b = future.getResult();
				String authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
				final String accountName = b
						.getString(AccountManager.KEY_ACCOUNT_NAME);

				// perform the API request in a separate thread
				DyadRequest request = new DyadRegistrationRequest(authToken);
				asyncExecute(request, DyadAccount.this, new DyadRequestCallback() {
				
					@Override
					public void onFinished() {
						lbm.sendBroadcast(new Intent(ACTION_REGISTERED_INTENT).putExtra(KEY_GOOGLE_ACCOUNT_NAME, accountName));
					}

					@Override
					public void onError(Exception e) {
						lbm.sendBroadcast(new Intent(ACTION_REGISTRATION_FAILED_INTENT).putExtra(KEY_EXCEPTION, e.getLocalizedMessage()));
					}

					@Override
					public void onCanceled() {
						lbm.sendBroadcast(new Intent(ACTION_REGISTRATION_FAILED_INTENT));
					}
				}, new Handler());
			} catch (Exception e) {
				lbm.sendBroadcast(new Intent(ACTION_REGISTRATION_FAILED_INTENT).putExtra(KEY_EXCEPTION, e.getLocalizedMessage()));
			}
		}
	}
	/**
	 * Helper method to spawn a worker thread making the web api request. TODO:
	 * It is also possible to let asyncRequest return a Future<HttpResponse>
	 * Simply change the return type and change the Runnable into a Callable.
	 */
	public void asyncExecute(final DyadRequest request,
			final DyadAccount dyadAccount, final DyadRequestCallback callback, final Handler handler) {
		if (handler == null)
			throw new IllegalArgumentException("handler is null");
		if (callback == null)
			throw new IllegalArgumentException("foo is null");

		// thread magic!
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					HttpResponse response = client.execute(dyadAccount.getHost(),
							request.getHttpRequest());
					request.onFinished(response, dyadAccount);
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

	public HttpResponse execute(DyadRequest request, DyadAccount dyadAccount)
			throws IOException, DyadServerException {
		HttpResponse response = client.execute(dyadAccount.getHost(),
				request.getHttpRequest());
		request.onFinished(response, dyadAccount);
		return response;
	}
	
	private class NotRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;
	}

}
