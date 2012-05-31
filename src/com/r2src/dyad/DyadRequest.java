package com.r2src.dyad;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface DyadRequest {
	public HttpRequest getHttpRequest();

	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException;
}
