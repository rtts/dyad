package com.r2src.dyad;

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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.r2src.dyad.request.DyadBondRequest;
import com.r2src.dyad.request.DyadGCMRequest;
import com.r2src.dyad.request.DyadRegisterRequest;
import com.r2src.dyad.request.DyadRequest;
import com.r2src.dyad.request.DyadsRequest;

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
	private static final String KEY_GOOGLE_ACCOUNT_NAME = "googleAccountName";

	private static final String KEY_SESSION_TOKEN = "sessionToken";

	private static final String TAG = "DyadAccount";

	private static String SENDER_ID;
	private static HttpHost HOST;

	private DyadListener dyadListener;

	private volatile String sessionToken;
	private volatile String googleAccountName;

	DyadClient client = new DyadClient();

	private volatile static DyadAccount singleton;

	private DyadAccount(Context context) {
		Log.d(TAG, "Creating dyad account singleton object...");
		Log.d(TAG, "Retrieving meta data from application manifest...");
		Bundle b = context.getApplicationInfo().metaData;

		String hostname = b.getString("com.r2src.dyad.HOST_NAME");
		int port = b.getInt("com.r2src.dyad.PORT");
		SENDER_ID = b.getString("com.r2src.dyad.GCM_SENDER_ID");

		Log.v(TAG, "Host name: " + hostname);
		Log.v(TAG, "Port number: " + port);

		if (hostname == null)
			throw new MetaDataMissingException("Host Name");
		if (port == 0)
			throw new MetaDataMissingException("Port");
		if (SENDER_ID == null)
			throw new MetaDataMissingException("Sender Id");
		else
			Log.v(TAG, "Sender ID found and not null");

		HOST = new HttpHost(hostname, port);

		Log.d(TAG, "Retrieving persistent data from shared preferences...");

		SharedPreferences pref = context.getSharedPreferences("Dyad",
				Context.MODE_PRIVATE);
		sessionToken = pref.getString(KEY_SESSION_TOKEN, null);
		googleAccountName = pref.getString(KEY_GOOGLE_ACCOUNT_NAME, null);

		Log.v(TAG, sessionToken == null ? "No session token found"
				: "Session token found");
		Log.v(TAG, googleAccountName == null ? "No google account name found"
				: "Google Account Name found");

	}

	public static DyadAccount getInstance(Context context) {
		if (singleton == null) {
			synchronized (DyadAccount.class) {
				if (singleton == null) {
					singleton = new DyadAccount(context);
				}
			}
		}
		return singleton;
	}

	/**
	 * Sets the {@link DyadListener} instance for this Dyad Account.
	 */
	public void setDyadListener(DyadListener dyadListener) {
		this.dyadListener = dyadListener;
	}

	/**
	 * Thrown when one of the expected keys in the Android Manifest's Meta Data
	 * is missing. Unrecoverable.
	 */
	public class MetaDataMissingException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public MetaDataMissingException(String missingKey) {
			super("Missing " + missingKey + " in the Android Manifest.");
		}
	}

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
	 * Registers the Dyad Account with the Dyad Server.
	 * <p>
	 * If no Google Account is associated with the application yet, calls
	 * {@link AccountManager#getAuthTokenByFeatures}, which lets the user choose
	 * a Google account to register with the Dyad server. Otherwise, checks if
	 * the Google account still exists on the device and calls
	 * {@link AccountManager#getAuthToken}.
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
	public void register(final Activity activity) throws GoogleAccountException {
		if (activity == null)
			throw new IllegalArgumentException("Activity is null");

		AccountManager manager = AccountManager.get(activity);

		Log.d(TAG, "Registering Dyad Account with Dyad Server...");
		if (googleAccountName == null) {
			Log.d(TAG, "Choosing Google Account...");
			manager.getAuthTokenByFeatures("com.google",
					"oauth2:https://www.googleapis.com/auth/userinfo.email",
					null, activity, null, null,
					new AccountRegistrationCallback(), new Handler());
		} else {
			Account account = null;
			Account[] accounts = manager.getAccountsByType("com.google");
			for (Account a : accounts) {
				if (a.name.equals(googleAccountName)) {
					account = a;
					break;
				}
			}
			if (account == null) {
				Log.d(TAG, "Google Account not found...");
				Intent intent = new Intent();
                intent.setClassName("com.r2src.dyad",
                        "com.r2src.dyad.AccountInvalidActivity");
                intent.putExtra(KEY_GOOGLE_ACCOUNT_NAME, googleAccountName);
				activity.startActivity(intent);
				// TODO Activity should call listener in some way or another.
			} else {
				Log.d(TAG, "Google Account found...");
				Log.d(TAG, "Retrieving OAuth2 token for this account...");
				manager.getAuthToken(account,
					"oauth2:https://www.googleapis.com/auth/userinfo.email",
					null, activity, new AccountRegistrationCallback(),
					new Handler());
			}
		}
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
	 *            the registration. Optional (if null, calls
	 *            {@link #register(Activity)}).
	 * 
	 * @throws GoogleAccountException
	 *             If the given account name is not present in the device's
	 *             Google accounts.
	 */
	public void register(final Activity activity, String googleAccountName)
			throws GoogleAccountException {
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
			throw new GoogleAccountException("Google account "
					+ googleAccountName + " does not exist");
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
		return HOST;
	}

	/**
	 * Sets the host for this account.
	 * <p>
	 * By default, the hostname and port defined in the manifest are used. Use
	 * this method to override the default host.
	 */
	public void setHost(HttpHost host) {
		this.HOST = host;
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
	 * @param SENDER_ID
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
	public void registerGCM(final Context context) {
		if (context == null)
			throw new IllegalArgumentException("context is null");
		GCMRegistrar.checkDevice(context);
		GCMRegistrar.checkManifest(context);
		final String regId = GCMRegistrar.getRegistrationId(context);
		if (regId.equals("")) {
			GCMRegistrar.register(context, SENDER_ID);
		} else {
			registerGCM(regId);
		}
	}

	private void registerGCM(final String regId) {
		DyadRequest request = new DyadGCMRequest(regId);
		client.asyncExecute(request, this, new DyadRequestCallback() {
			@Override
			public void onFinished() {
				dyadListener.onGCMRegistered();
			}

			@Override
			public void onError(Exception e) {
				dyadListener.onGCMRegistrationFailed(e);
			}
		}, new Handler());
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
				DyadRequest request = new DyadRegisterRequest(authToken);
				client.asyncExecute(request, DyadAccount.this,
						new DyadRequestCallback() {

							@Override
							public void onFinished() {
								DyadAccount.this
										.setGoogleAccountName(accountName);
								dyadListener.onRegistered();
							}

							@Override
							public void onError(Exception e) {
								dyadListener.onRegistrationFailed(e);
							}

						}, new Handler());
			} catch (Exception e) {
				dyadListener.onRegistrationFailed(e);
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
