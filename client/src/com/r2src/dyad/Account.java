package com.r2src.dyad;

import org.apache.http.HttpHost;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.r2src.dyad.request.DyadRequestCallback;
import com.r2src.dyad.request.RegisterRequest;
import com.r2src.dyad.request.Request;
import com.r2src.dyad.request.Requester;

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
public class Account {
	private static final String TAG = "DyadAccount";
	private static final String KEY_SESSION_TOKEN = "sessionToken";
	private static final String KEY_GOOGLE_ACCOUNT_NAME = "googleAccountName";
	private static String GCM_SENDER_ID;
	private static HttpHost HOST;

	private volatile String sessionToken;
	private volatile String googleAccountName;

	Requester client = new Requester();

	private volatile static Account singleton;

	private volatile DyadListener dyadListener;
	
	public static final String ACTION_REGISTERED = "com.r2src.dyad.ACTION_REGISTERED";

	private Account(final FragmentActivity activity) {
		Log.d(TAG, "Creating dyad account singleton object...");
		
		readApplicationMetaData(activity);
		readSharedPreferences(activity);
		
		if (isRegistered()) {
			Log.v(TAG, "Dyad singleton account created and complete");
		} else {
			Log.v(TAG, "Dyad singleton account created but not complete");
			register(activity);
		}
		
	}

	/**
	 * Finds and sets the Session Token & Google Account Name from {@link SharedPreferences}
	 */
	private void readSharedPreferences(final Context context) {
		Log.d(TAG, "Retrieving persistent data from shared preferences...");
		SharedPreferences pref = context.getSharedPreferences("Dyad", Context.MODE_PRIVATE);
		setSessionToken(pref.getString(KEY_SESSION_TOKEN, null));
		setGoogleAccountName(pref.getString(KEY_GOOGLE_ACCOUNT_NAME, null));

		Log.v(TAG, sessionToken == null ? "No session token found in shared preferences"
				: "Session token found in shared preferences");
		Log.v(TAG, googleAccountName == null ? "No google account name found in shared preferences"
				: "Google account name found in shared preferences");
	}

	/**
	 * Finds and sets the GCM Sender Id & Dyad Server Http Host from Application's metadata
	 */
	private void readApplicationMetaData(final Context context) {
		Log.d(TAG, "Retrieving meta data from application manifest...");
		Bundle metaData;
		try {
			metaData = context.getPackageManager().getApplicationInfo(
					context.getPackageName(), PackageManager.GET_META_DATA).metaData;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e); // Never happens
		}
				
		if (metaData == null) throw new MetaDataMissingException("all metadata");
		
		String hostname = metaData.getString("com.r2src.dyad.DYAD_SERVER_HOST");
		int port 		= metaData.getInt("com.r2src.dyad.DYAD_SERVER_PORT");
		GCM_SENDER_ID	= Integer.toString(metaData.getInt("com.r2src.dyad.GCM_SENDER_ID"));

		Log.v(TAG, "Server address: " + hostname + ":" + port);

		if (hostname == null) 		throw new MetaDataMissingException("Host Name");
		if (port == 0) 				throw new MetaDataMissingException("Port");
		if (GCM_SENDER_ID == null) 	throw new MetaDataMissingException("GCM Sender Id");
		else Log.v(TAG, "GCM Sender ID found and not null");

		HOST = new HttpHost(hostname, port);
	}
	
	/**
	 * Returns the Account associated with this device. If the account is not yet registered
	 * with the server, this is done in a background thread and a Broadcast is sent on completion.
	 * @param activity
	 * @return
	 */
	public static Account getInstance(FragmentActivity activity) {
		if (singleton == null) {
			synchronized (Account.class) {
				if (singleton == null) {
					singleton = new Account(activity);
				}
			}
		}
		return singleton;
	}
	
	public boolean isRegistered() {
		return googleAccountName != null && sessionToken != null
				&& googleAccountName.length() > 0 && sessionToken.length() > 0;
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
	public void register(final FragmentActivity activity) {
		if (activity == null)
			throw new IllegalArgumentException("Activity is null");

		Log.d(TAG, "Starting registration process...");
		if (googleAccountName == null) {
			getAuthToken(null, activity);
		} else {
			android.accounts.Account account = retrieveGoogleAccount(googleAccountName, activity);
			if (account != null) {
				getAuthToken(account, activity);
			} else {
				launchGoogleAccountNotFoundDialog(activity);
			}
		}
	}

	/**
	 * Finds the Google Account associated with the given Google Account Name
	 */
	private android.accounts.Account retrieveGoogleAccount(
			String accountName,
			Context context) {
		Log.d(TAG, "Retrieving google account associated with stored account name...");
		android.accounts.Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
		android.accounts.Account account = null;
		for (android.accounts.Account a : accounts) {
			if (a.name.equals(accountName)) {
				account = a;
				break;
			}
		}
		return account;
	}

	private void launchGoogleAccountNotFoundDialog(
			final FragmentActivity activity) {
		Log.d(TAG, "Google account not found, launching dialog...");
		
		new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);

				builder.setMessage(R.string.account_invalid_dialog_message)
				       .setTitle(R.string.account_invalid_dialog_title);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			               Log.d(TAG, "User chose to find a new google account...");
			        	   setGoogleAccountName(null);
			               register(activity);
			           }
			       });
				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   Log.d(TAG, "User chose not to choose a new google account...");
			        	   getDialog().cancel();
			        	   //TODO: Good exception message
			        	   dyadListener.onRegistrationFailed(null);
			           }
			       });
				return builder.create();
			}
		}.show(activity.getSupportFragmentManager(), "InvalidAccountDialog");
	}


	private void getAuthToken(android.accounts.Account googleAccount, 
							  FragmentActivity activity) {
		AccountManager manager = AccountManager.get(activity);
		if (googleAccount == null) {
			Log.d(TAG, "No stored google account name, choosing one...");
			manager.getAuthTokenByFeatures("com.google",
					"oauth2:https://www.googleapis.com/auth/userinfo.email",
					null, activity, null, null,
					new AccountRegistrationCallback(), new Handler());
		} else {
			Log.d(TAG, "Google account retrieved, fetching OAuth2 token for this account...");
			manager.getAuthToken(googleAccount,
					"oauth2:https://www.googleapis.com/auth/userinfo.email",
					null, activity, new AccountRegistrationCallback(),
					new Handler());
		}
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
		HOST = host;
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

	public synchronized void setDyadListener(DyadListener listener) {
		dyadListener = listener;
	}

	/**
	 * Registers the device's GCM registration id with the Dyad Server. Upon
	 * success, the ACTION_GCM_REGISTERED_INTENT will be broadcast. Otherwise,
	 * expect an ACTION_GCM_REGISTRATION_FAILED_INTENT.
	 * <p>
	 * 
	 * @param GCM_SENDER_ID
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
	/*
	public void registerGCM(final Context context) {
		if (context == null)
			throw new IllegalArgumentException("context is null");
		GCMRegistrar.checkDevice(context);
		GCMRegistrar.checkManifest(context);
		final String regId = GCMRegistrar.getRegistrationId(context);
		if (regId.equals("")) {
			GCMRegistrar.register(context, GCM_SENDER_ID);
		} else {
			registerGCM(regId);
		}
	}

	private void registerGCM(final String regId) {
		Request request = new RegisterGCMRequest(regId);
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
	*/

	/**
	 * Starts the process of forming a Dyad.
	 * <p>
	 * This method will start the supplied {@link Bonder} activity to perform
	 * the actual bonding. After the bonding process is complete, this method
	 * will send the resulting shared secret to the Dyad Server.
	 * <p>
	 * When the server receives two matching secrets, the Dyad is complete.
	 */
	/*
	public void requestDyad(Activity activity, Bonder bonder,
			final DyadRequestCallback foo, final Handler handler) {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);
		lbm.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final String secret = intent.getStringExtra("secret");
				Request request = new BondRequest(secret);
				try {
					authenticate(request);
				} catch (final NotRegisteredException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
						}
					});
				}
				client.asyncExecute(request, Account.this, foo, handler);

			}
		}, new IntentFilter("com.r2src.dyad.action.GOT_SHARED_SECRET"));

		activity.startActivity(new Intent(activity, bonder.getClass()));
	}
	*/

	/**
	 * Fetches the list of Dyads from the server.
	 */
	/*
	public void getDyads(String sessionToken, DyadRequestCallback foo,
			Handler handler) {
		if (sessionToken == null)
			throw new IllegalArgumentException("Session Token is null");
		Request request = new DyadsRequest(sessionToken);
		client.asyncExecute(request, this, foo, handler);
	}
	*/

	/**
	 * Authenticates a {@link Request} by adding a custom header containing
	 * the session token.
	 * 
	 * @param request
	 *            The request to be authenticated.
	 * 
	 * @throws NotRegisteredException
	 *             If there is no session token. Handle this by calling
	 *             {@link #register} first.
	 */
	public void authenticate(Request request) throws NotRegisteredException {
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
				Request request = new RegisterRequest(authToken);
				client.asyncExecute(request, Account.this,
						new DyadRequestCallback() {


							@Override
							public void onFinished() {
								Log.d(TAG, "Choosing google account completed...");
								setGoogleAccountName(accountName);
								dyadListener.onRegistered();
							}

							@Override
							public void onError(Exception e) {
								Log.w(TAG, "Registering google account with DyadServer failed...");
								dyadListener.onRegistrationFailed(e);
							}

						}, new Handler());
			} catch (Exception e) {
				Log.d(TAG, "No google account chosen...");
				dyadListener.onRegistrationFailed(e);
			}
		}
	}

	private class NotRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	public class GoogleAccountException extends Exception {
		private static final long serialVersionUID = 1L;

		public GoogleAccountException(String error) {
			super(error);
		}
	}
	
	public static void reset(Context context) {
		Log.d(TAG, "Resetting...");

		SharedPreferences.Editor edit = context.getSharedPreferences("Dyad", Context.MODE_PRIVATE).edit();
		edit.putString(KEY_SESSION_TOKEN, null);
		edit.putString(KEY_GOOGLE_ACCOUNT_NAME, null);
		edit.commit();
	}

}
