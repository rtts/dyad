package com.r2src.dyad.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadServerException;


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

	/**
	 * TODO: what should happen locally when bond succeeds?
	 */
	@Override
	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException {
		switch (response.getStatusLine().getStatusCode()) {
		case 200: // successful bonding
			//?;
		case 202: // secret stored, wait for push
			break;
		default:
		
		}
	}

}
