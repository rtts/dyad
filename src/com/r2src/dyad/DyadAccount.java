package com.r2src.dyad;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.Handler;
import android.util.Log;

/**
 * An account with most of its state stored server-side. Once registered, it
 * only needs to hold on to it's UUID, which you can also pass to the
 * constructor to construct a registered account. (Downside: if someone steals
 * the UUID it can be used to track your significant other)
 * <p>
 * Dyad Server API version 1 accepts re-registration of an account and will
 * return an existing UUID when that happens. (This should be useful to transfer
 * existing Dyad Accounts to new devices.)
 */
public class DyadAccount {
	private final HttpHost host;
	private final int apiVersion;
	private UUID uuid;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final HttpClient client = new DefaultHttpClient();

	public DyadAccount(HttpHost host, int apiVersion) {
		this(host, apiVersion, null);
	}

	/**
	 * @param host
	 *            The host that runs Dyad Server.
	 * @param apiVersion
	 *            The api version of the Dyad Server.
	 * @param uuid
	 *            The UUID of an exisisting Dyad Account (may be null).
	 */
	public DyadAccount(HttpHost host, int apiVersion, UUID uuid) {
		this.host = host;
		this.apiVersion = apiVersion;
		this.uuid = uuid;
	}

	/**
	 * Returns the account's UUID (please store it somewhere safe!).
	 */
	public UUID getUUID() {
		return uuid;
	}

	/**
	 * Registers an account with the Dyad server. This is a blocking method,
	 * which returns when the registration is complete.
	 * 
	 * @param account
	 *            An Android account that will be used for authentication.
	 * 
	 * @param manager
	 *            An {@link AccountManager} object.
	 * 
	 *            TODO: Stop the accounts stuff and just ask for a Context.
	 * 
	 * @param c2dm_id
	 *            The C2DM registration id of this device. Needed to receive
	 *            push messages.
	 * 
	 * @throws OperationCanceledException
	 *             If the operation is canceled for any reason, including the
	 *             user canceling a credential request.
	 * 
	 * @throws AuthenticatorException
	 *             Don't look at us. It's not our fault.
	 * 
	 * @throws IOException
	 *             The authenticator experienced an I/O problem creating a new
	 *             auth token.
	 * 
	 * @throws WrongAccountTypeException
	 *             The supplied account is not of type "com.google".
	 * 
	 * @throws DyadServerException
	 *             The server returned a response that cannot be parsed to a
	 *             JSON object.
	 * 
	 */
	public void register(Account account, AccountManager manager, String c2dm_id)
			throws OperationCanceledException, AuthenticatorException,
			IOException, WrongAccountTypeException, DyadServerException {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (manager == null)
			throw new IllegalArgumentException("manager is null");
		if (c2dm_id == null)
			throw new IllegalArgumentException("c2dm_id is null");
		if (account.type != "com.google")
			throw new WrongAccountTypeException();

		Log.d(this.getClass().getName(), "Requesting auth token for "
				+ account.name);

		/*
		 * The auth token type "Email" is a valid OAuth2 alias for
		 * "oauth2:https://www.googleapis.com/auth/userinfo.email". However, the
		 * Google Authenticator plugin thinks it's not...
		 */
		String authToken = manager.blockingGetAuthToken(account,
				"oauth2:https://www.googleapis.com/auth/userinfo.email", false);

		if (authToken == null || authToken == "")
			throw new AuthenticatorException();
		else
			Log.d(this.getClass().getName(),
					"Successfully retrieved auth token");

		// TODO: design cooler way to describe WebApi interface
		HttpPost request = new HttpPost(WebApiV1.getUrl("register"));
		JSONObject body = new JSONObject();
		try {
			body.put("token", authToken);
			body.put("c2dm_id", c2dm_id);
		} catch (JSONException e) {
			// this will never happen
			throw new RuntimeException(e);
		}

		HttpEntity entity = new StringEntity(body.toString());
		request.setEntity(entity);

		HttpResponse response = client.execute(host, request);

		switch (response.getStatusLine().getStatusCode()) {

		// account already registered
		case 200:
			break;

		// new account created
		case 201:
			break;

		// any kind of error
		default:
			throw new DyadServerException(response);
		}

		entity = response.getEntity();
		try {
			body = new JSONObject(EntityUtils.toString(entity));
			uuid = UUID.fromString(body.getString("uuid"));
		} catch (ParseException e) {
			throw new DyadServerException(e, response);
		} catch (JSONException e) {
			throw new DyadServerException(e, response);
		}
	}

	/**
	 * Registers the account with the Dyad server. This is a non-blocking
	 * method, which calls the foo.onFinished() method when completed.
	 * 
	 * @param account
	 * @param manager
	 * @param foo
	 * @param handler
	 */
	public void register(final Account account, final AccountManager manager,
			final String c2dm_id, final Foo foo, final Handler handler) {
		if (account == null)
			throw new IllegalArgumentException("account is null");
		if (manager == null)
			throw new IllegalArgumentException("manager is null");
		if (foo == null)
			throw new IllegalArgumentException("foo is null");
		if (handler == null)
			throw new IllegalArgumentException("handler is null");
		if (c2dm_id == null)
			throw new IllegalArgumentException("c2dm_id is null");

		// thread magic!
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					register(account, manager, c2dm_id);
					handler.post(new Runnable() {
						public void run() {
							foo.onFinished();
						}
					});
				} catch (final AuthenticatorException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
						}
					});
				} catch (final WrongAccountTypeException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onError(e);
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
				} catch (final OperationCanceledException e) {
					handler.post(new Runnable() {
						public void run() {
							foo.onCanceled();
						}
					});
				}
			}
		});

	}

	public Future<List<Dyad>> getDyads() {
		// TODO: method stub
		return null;
	}

	public void authenticate(HttpRequest request) throws NotRegisteredException {
		if (uuid == null)
			throw new NotRegisteredException();
		request.addHeader("X-Dyad-Authentication", uuid.toString());
	}

	class NotRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	class WrongAccountTypeException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}
