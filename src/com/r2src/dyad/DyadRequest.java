package com.r2src.dyad;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * Base class for all Web API requests to a Dyad Server.
 * 
 * Most subclasses should override the constructor to provide new requests
 * with the required information.
 */
public abstract class DyadRequest {

	protected HttpRequest request;

	/**
	 * Get the underlying {@link HttpRequest}.
	 */
	public HttpRequest getHttpRequest() {
		return request;
	}

	/**
	 * A method to handle the response.
	 */
	public abstract void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException;
}
