package com.r2src.dyad;

import java.io.IOException;
import java.util.ArrayList;
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gcm.GCMRegistrar;

/**
 * A client to execute {@link DyadRequest}s.
 */
public abstract class DyadClient {
	private final String SENDER_ID;
	private final HttpHost HOST;
	private List<DyadAccount> accounts = new ArrayList<DyadAccount>();
	private volatile DefaultHttpClient client;
	public final ExecutorService executor = Executors.newCachedThreadPool();

	public DyadClient(String senderId, HttpHost host) {
		if (senderId == null) throw new IllegalArgumentException("Sender ID is null");
		if (host == null) throw new IllegalArgumentException("Host is null");
		SENDER_ID = senderId;
		HOST = host;

		client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);
	}
	
	public void addDyadAccount(DyadAccount account) {
		accounts.add(account);
	}
	
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
	public void register(DyadAccount account, Activity activity) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (activity == null)
			throw new IllegalArgumentException("activity is null");

		AccountManager.get(activity).getAuthTokenByFeatures("com.google",
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, null, null, new AccountRegistrationCallback(account, activity), new Handler());
	}

	/**
	 * Registers the account with the Dyad Server. Returns immediately and will
	 * broadcast either a {@link #ACTION_REGISTERED_INTENT} when finished, or a
	 * {@link #ACTION_REGISTRATION_FAILED_INTENT} on error.
	 * <p>
	 * This method expects the name of the Google account to be registered as its
	 * second argument. Useful if you already know which Google account to
	 * register.
	 * <p>
	 * The chosen account name will be available in the REGISTERED intent so you
	 * can store it somewhere.
	 */
	public void register(DyadAccount account, Activity activity,
			String googleAccountName) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (activity == null)
			throw new IllegalArgumentException("activity is null");
		if (googleAccountName == null)
			throw new IllegalArgumentException("accountName is null");

		Account googleAccount = null;
		AccountManager manager = AccountManager.get(activity);
		Account[] accounts = manager.getAccounts();
		for (Account a : accounts) {
			if (a.name.equals(googleAccountName)) {
				googleAccount = a;
				break;
			}
		}
		if (googleAccount == null) {
			throw new IllegalArgumentException("account " + googleAccountName
					+ " does not exist");
		}
		if (! googleAccount.type.equals("com.google")) {
			throw new IllegalArgumentException("account " + googleAccountName
					+ " is not a Google account");
		}

		manager.getAuthToken(googleAccount,
				"oauth2:https://www.googleapis.com/auth/userinfo.email", null,
				activity, new AccountRegistrationCallback(account, activity), new Handler());
	}

	/**
	 * Registers the device's GCM registration id with the Dyad Server. Upon
	 * success, the ACTION_GCM_REGISTERED_INTENT will be broadcast. Otherwise, expect
	 * an ACTION_GCM_REGISTRATION_FAILED_INTENT.
	 * <p>
	 * @throws UnsupportedOperationException If the device running the app cannot handle GCM appropriately.
	 * @throws IllegalStateException If the Android Manifest doesn't meet all the GCM requirements.
	 */
	public void registerGCM(DyadAccount account, final Activity activity) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (activity == null)
			throw new IllegalArgumentException("activity is null");

		GCMRegistrar.checkDevice(activity);
		GCMRegistrar.checkManifest(activity);
		final String regId = GCMRegistrar.getRegistrationId(activity);
		if (regId.equals("")) {
			GCMRegistrar.register(activity, SENDER_ID);
		} else {
			DyadRequest request = new DyadGCMRequest(regId);
			asyncExecute(request, account, new DyadRequestCallback() {
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
	 * Used as the argument for {@link AccountManager#getAuthToken} and
	 * {@link AccountManager#getAuthTokenByFeatures}. Broadcasts the REGISTERED
	 * or REGISTRATION_FAILED intents.
	 */
	private class AccountRegistrationCallback implements AccountManagerCallback<Bundle> {
		LocalBroadcastManager lbm;
		DyadAccount account;
		
		public AccountRegistrationCallback(DyadAccount account, Context context) {
			super();
			lbm = LocalBroadcastManager.getInstance(context);
			this.account = account;
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
				asyncExecute(request, account, new DyadRequestCallback() {
				
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

}
