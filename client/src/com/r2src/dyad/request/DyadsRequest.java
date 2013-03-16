package com.r2src.dyad.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import com.r2src.dyad.Dyad;
import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadServerException;


/**
 * Requests all dyads that are associated with given user and account. Dyads are
 * currently saved in DyadAccount. TODO: Rewrite so that DyadAccount has no state, by either working with Future objects, or giving a bundle to DyadRequestCallback's onFinished.
 */
public class DyadsRequest extends DyadRequest {

	private static final String PATH = "/v1/dyads/";

	/**
	 * Sends a GET request to the /dyads/ url. Appends the session token to the
	 * url.
	 * 
	 * @param session_token
	 *            The token that identifies the user with the server.
	 */
	public DyadsRequest(String session_token) {
		request = new HttpGet(PATH + session_token);
	}
	
	/**
	 * Transforms the response into either an error or a list of dyads.
	 * Dyads are saved in the associated account.
	 */
	@Override
	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException {

		JSONArray body;
		switch (response.getStatusLine().getStatusCode()) {

		// body contains the dyads
		case 200:
			break;

		default:
			throw new DyadServerException(response);
		}

		HttpEntity entity = response.getEntity();
		try {
			body = new JSONArray(EntityUtils.toString(entity));
			final List<Dyad> dyads = new ArrayList<Dyad>(body.length());
			for (int i = 0; i < body.length(); i++) {
				dyads.add(new Dyad(account, body.get(i).toString()));
			}
		} catch (ParseException e) {
			throw new DyadServerException(e, response);
		} catch (JSONException e) {
			throw new DyadServerException(e, response);
		}
	}

}
