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
 * A request to send the device's GCM id to the Dyad Server. Only valid for already
 * registered accounts.
 * 
 * TODO: implement this request server-side
 */
public class DyadGCMRequest extends DyadRequest {

	private static final String PATH = "/v1/gcm";

	/**
	 * Creates a new GCM request.

	 * @param RegId
	 *            The GCM registration id of the device.
	 */
	public DyadGCMRequest(String regId) {
		request = new HttpPost(PATH);
		JSONObject body = new JSONObject();
		HttpEntity entity;
		try {
			body.put("gcm_id", regId);
			entity = new StringEntity(body.toString());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		((HttpPost) request).setEntity(entity);
	}

	/**
	 * Handles the response
	 */
	@Override
	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException {

		JSONObject body;
		switch (response.getStatusLine().getStatusCode()) {

		case 200:
			break;
		
		// TODO: handle the possible server responses

		default:
			throw new DyadServerException(response);
		}
	}
}
