package com.r2src.dyad;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A request both parties should make to instantiate a bonded Dyad.
 */
public class DyadBondRequest extends DyadRequest {

	private static final String PATH = "/v1/bond";

	/**
	 * Constructor
	 * 
	 * @param secret
	 *            A secret that is known to both parties that want to bond.
	 */
	public DyadBondRequest(String secret) {
		request = new HttpPost(PATH);
		JSONObject body = new JSONObject();
		HttpEntity entity;
		try {
			body.put("secret", secret);
			entity = new StringEntity(body.toString());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		((HttpPost) request).setEntity(entity);
	}

	@Override
	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException {
		// TODO parse response, instantiate Dyad

	}

}
