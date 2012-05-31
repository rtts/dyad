package com.r2src.dyad;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

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
public class DyadRegistrationRequest implements DyadRequest {
	private HttpPost request = new HttpPost("/v1/register");

	public DyadRegistrationRequest(String authToken, String c2dm_id) {
		JSONObject body = new JSONObject();
		HttpEntity entity;
		try {
			body.put("token", authToken).put("c2dm_id", c2dm_id);
			entity = new StringEntity(body.toString());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		request.setEntity(entity);
	}
	
	@Override
	public HttpRequest getHttpRequest() {
		return request;
	}
}
