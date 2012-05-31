package com.r2src.dyad;

import java.io.IOException;

import org.apache.http.HttpResponse;

public interface Bar {
	public void onFinished(HttpResponse response) throws DyadServerException,
	IOException;
}
