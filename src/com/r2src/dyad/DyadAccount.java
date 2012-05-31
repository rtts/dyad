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

import android.os.Handler;

/**
 * An account with most of its state stored server-side. Once registered, it
 * only needs to hold on to it's UUID, which you can also pass to the
 * constructor to construct a registered account. (Downside: if someone steals
 * the UUID it can be used to track your significant other)
 * <p>
 * Dyad Server API version 1 accepts re-registration of an account and will
 * return an existing UUID when that happens. This should be useful to transfer
 * existing Dyad Accounts to new devices.
 */
public class DyadAccount {
	static HttpClient client = new DefaultHttpClient();

	private final HttpHost host;
	private UUID uuid;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	// private final HttpClient client = new DefaultHttpClient();
	private static final String REGISTRATION_URI = "/v1/register";

	/**
	 * Creates a local account that has not been registered with the server.
	 */
	public DyadAccount(HttpHost host) {
		this(host, null);
	}

	/**
	 * Creates an account that has already been registered with the server.
	 * 
	 * @param host
	 *            The host that runs Dyad Server.
	 * @param apiVersion
	 *            The api version of the Dyad Server.
	 * @param uuid
	 *            The UUID of an existing Dyad Account (may be null).
	 */
	public DyadAccount(HttpHost host, UUID uuid) {
		this.host = host;
		this.uuid = uuid;
	}

	/**
	 * Returns the host for this account
	 */
	public HttpHost getHost() {
		return host;
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
	 *            The auth token type "Email" is a valid OAuth2 alias for
	 *            "oauth2:https://www.googleapis.com/auth/userinfo.email".
	 *            However, the Google Authenticator plugin thinks it's not...
	 * 
	 * @param c2dm_id
	 *            The C2DM registration id of this device. Needed to receive
	 *            push messages.
	 * 
	 * @throws IOException
	 *             Network trouble
	 * 
	 * @throws DyadServerException
	 *             The server returned a response that cannot be parsed to a
	 *             JSON object.
	 * 
	 */
	public void register(String authToken, String c2dm_id)
			throws DyadServerException, IOException {
		if (authToken == null)
			throw new IllegalArgumentException("authToken is null");
		if ("".equals(authToken))
			throw new IllegalArgumentException("authToken is empty");
		if (c2dm_id == null)
			throw new IllegalArgumentException("c2dm_id is null");
		if ("".equals(c2dm_id))
			throw new IllegalArgumentException("c2dm_id is empty");

		DyadClient client = DyadClient.getInstance();

		HttpPost request = new HttpPost(REGISTRATION_URI);
		JSONObject body = new JSONObject();
		try {
			body.put("token", authToken).put("c2dm_id", c2dm_id);
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
	public void register(final String authToken, final String c2dm_id,
			final Foo foo, final Handler handler) {
		if (authToken == null)
			throw new IllegalArgumentException("authToken is null");
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
					register(authToken, c2dm_id);
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
}
