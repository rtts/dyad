package com.r2src.dyad;

import java.util.List;

import org.apache.http.HttpHost;

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
import android.util.Log;

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
	
	private static final String TAG = "DyadAccount";

	private final HttpHost host;
	private final DyadListener listener;

	private volatile String sessionToken;
	private volatile String googleAccountName;

	DyadClient client = new DyadClient();

	private List<Dyad> dyads;

	/**
	 * Broadcasted when the registration process succeeded. The Intent includes
	 * a field {@link #KEY_GOOGLE_ACCOUNT_NAME}.
	 */
	public static final String ACTION_GCM_REGISTERED_INTENT = "com.r2src.dyad.GCM_REGISTERED";
	/**
	 * Broadcasted when the registration process could not be finished. The
	 * Intent includes a field {@link #KEY_EXCEPTION}
	 */
	public static final String ACTION_GCM_REGISTRATION_FAILED_INTENT = "com.r2src.dyad.GCM_REGISTRATION_FAILED";

	/**
	 * Bundle key used to communicate an exception message.
	 */
	public static final String KEY_EXCEPTION = "exception";

	/**
	 * Creates a Dyad Account.
	 * <p>
	 * Call {@link #register} to let this Dyad Account register itself with the
	 * Dyad Server.
	 * 
	 * @param host
	 *            The {@link HttpHost} representing Dyad Server's location.
	 *            Required.
	 * @param listener
	 *            Handles Dyad events. Required.
	 * @param context
	 *            Used for registering a local broadcast receiver that catches
	 *            GCM broadcasts. Required.
	 * @param sessionToken
	 *            Authenticates requests for the Dyad Server. If provided,
	 *            consider this account registered. Optional.
	 */
	public DyadAccount(HttpHost host, DyadListener dyadListener,
			Context context, String sessionToken) {
		if (host == null)
			throw new IllegalArgumentException("Host is null");
		if (dyadListener == null)
			throw new IllegalArgumentException("Dyad Listener is null");
		if (context == null)
			throw new IllegalArgumentException("Context is null");
		this.host = host;
		this.listener = dyadListener;
		this.sessionToken = sessionToken;

		// register the various intents
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_GCM_REGISTERED_INTENT);
		filter.addAction(ACTION_GCM_REGISTRATION_FAILED_INTENT);
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
		lbm.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_GCM_REGISTERED_INTENT.equals(action)) {
					listener.onGCMRegistered();
				} else if (ACTION_GCM_REGISTRATION_FAILED_INTENT.equals(action)) {
					listener.onGCMRegistrationFailed(new GCMException(intent
							.getStringExtra(KEY_EXCEPTION)));
				}
			}

		}, filter);
	}

	/**
	 * Registers the Dyad Account with the Dyad Server.
	 * <p>
	 * This method calls {@link AccountManager#getAuthTokenByFeatures}, which
	 * lets the user choose a Google account to register with the Dyad server.
	 * <p>
	 * After registration, {@link DyadListener#onRegistered} is called when the
	 * registration was successful, and
	 * {@link DyadListener#onRegistrationFailed} when it failed.
	 * <p>
	 * This is a non-blocking call. It can be called safely from the main
	 * thread.
	 * 
	 * @param activity
	 *            The currently active activity, that will be used to let the
	 *            user choose a Google Account. Required.
	 */
	public void register(final Activity activity) {
		if (activity == null)
			throw new IllegalArgumentException("activity is null");

		Log.v(TAG, "Registering...");
		Log.v(TAG, "Looking for Google Account...");
		AccountManager.get(activity).getAuthTokenByFeatures("com.google",
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, null, null, new AccountRegistrationCallback(),
				new Handler());
	}

	/**
	 * Registers the Dyad Account with the Dyad Server.
	 * <p>
	 * After registration, {@link DyadListener#onRegistered} is called when the
	 * registration was successful, and
	 * {@link DyadListener#onRegistrationFailed} when it failed.
	 * <p>
	 * This is a non-blocking call. It can be called safely from the main
	 * thread.
	 * 
	 * @param activity
	 *            The currently active activity, that will be used to let the
	 *            user choose a Google Account. Required.
	 * @param googleAccountName
	 *            The name of the Google Account that the user wants to use for
	 *            the registration. Optional (if null, calls {@link #register(Activity)}).
	 * 
	 * @throws GoogleAccountException
	 *             If the given account name is not present in the
	 *             device's Google accounts.
	 */
	public void register(final Activity activity, String googleAccountName) throws GoogleAccountException {
		if (activity == null)
			throw new IllegalArgumentException("activity is null");
		if (googleAccountName == null) {
			Log.v(TAG,
					"Register method called with empty Google Account Name, calling Register(Activity)...");
			register(activity);
		}

		Log.v(TAG, "Looking for Google Account...");
		Account account = null;
		AccountManager manager = AccountManager.get(activity);
		Account[] accounts = manager.getAccountsByType("com.google");
		for (Account a : accounts) {
			if (a.name.equals(googleAccountName)) {
				account = a;
				break;
			}
		}
		if (account == null) {
			Log.v(TAG, "Google Account not found...");
			throw new GoogleAccountException("Google account " + googleAccountName
					+ " does not exist");
		}
		Log.v(TAG, "Google Account found...");
		Log.v(TAG, "Retrieving oauth2 token for this account...");
		manager.getAuthToken(account,
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, new AccountRegistrationCallback(), new Handler());
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
	public synchronized void setSessionToken(String token) {
		sessionToken = token;
	}

	/**
	 * Returns the account's name.
	 */
	public String getGoogleAccountName() {
		return googleAccountName;
	}

	/**
	 * Set the account's name
	 */
	protected synchronized void setGoogleAccountName(String name) {
		googleAccountName = name;
	}

	/**
	 * Registers the device's GCM registration id with the Dyad Server. Upon
	 * success, the ACTION_GCM_REGISTERED_INTENT will be broadcast. Otherwise,
	 * expect an ACTION_GCM_REGISTRATION_FAILED_INTENT.
	 * <p>
	 * 
	 * @param senderId
	 *            Identifies and authenticates this app when requesting a GCM
	 *            registration id for the device. Required.
	 * 
	 * @throws UnsupportedOperationException
	 *             If the device running the app cannot handle GCM
	 *             appropriately.
	 * @throws IllegalStateException
	 *             If the Android Manifest doesn't meet all the GCM
	 *             requirements.
	 */
	public void registerGCM(final Context context, final String senderId) {
		if (context == null)
			throw new IllegalArgumentException("context is null");

		GCMRegistrar.checkDevice(context);
		GCMRegistrar.checkManifest(context);
		final String regId = GCMRegistrar.getRegistrationId(context);
		if (regId.equals("")) {
			GCMRegistrar.register(context, senderId);
		} else {
			DyadRequest request = new DyadGCMRequest(regId);
			client.asyncExecute(request, this, new DyadRequestCallback() {
				@Override
				public void onFinished() {
					listener.onGCMRegistered();
				}

				@Override
				public void onError(Exception e) {
					listener.onGCMRegistrationFailed(e);
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
	public void requestDyad(Activity activity, Bonder bonder,
			final DyadRequestCallback foo, final Handler handler) {
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
				client.asyncExecute(request, DyadAccount.this, foo, handler);

			}
		}, new IntentFilter("com.r2src.dyad.action.GOT_SHARED_SECRET"));

		activity.startActivity(new Intent(activity, bonder.getClass()));
	}

	/**
	 * Fetches the list of Dyads from the server.
	 */
	public void getDyads(String sessionToken, DyadRequestCallback foo,
			Handler handler) {
		if (sessionToken == null)
			throw new IllegalArgumentException("Session Token is null");
		DyadRequest request = new DyadsRequest(sessionToken);
		client.asyncExecute(request, this, foo, handler);
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
	 * Registers the Google Account with the Dyad Server, using the retrieved
	 * oauth2 token as authentication.
	 * 
	 * Calls {@link DyadListener#onRegistered} when registering with the Dyad
	 * Server succeeds, {@link DyadListener#onRegistrationFailed} when it fails.
	 * 
	 * Used as the argument for {@link AccountManager#getAuthToken} and
	 * {@link AccountManager#getAuthTokenByFeatures}.
	 * 
	 * 
	 * 
	 * auth token was
	 */
	private class AccountRegistrationCallback implements
			AccountManagerCallback<Bundle> {

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
				client.asyncExecute(request, DyadAccount.this,
						new DyadRequestCallback() {

							@Override
							public void onFinished() {
								DyadAccount.this
										.setGoogleAccountName(accountName);
								listener.onRegistered();
							}

							@Override
							public void onError(Exception e) {
								listener.onRegistrationFailed(e);
							}

						}, new Handler());
			} catch (Exception e) {
				listener.onRegistrationFailed(e);
			}
		}
	}

	private class NotRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private class GCMException extends Exception {
		private static final long serialVersionUID = 1L;
		public GCMException(String error) {
			super(error);
		}
	}
	
	public class GoogleAccountException extends Exception {
		private static final long serialVersionUID = 1L;
		public GoogleAccountException(String error) {
			super(error);
		}
	}

}
