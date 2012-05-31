package com.r2src.dyad;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

public class DyadBondRequest implements DyadRequest {
	private HttpPost request = new HttpPost("/v1/bond");
	
	public DyadBondRequest(String secret) {
		JSONObject body = new JSONObject();
		HttpEntity entity;
		try {
			body.put("token", secret);
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
		// TODO authenticate request
		return request;
	}

}
