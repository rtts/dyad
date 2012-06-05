package com.r2src.dyad;

import org.apache.http.HttpResponse;

public class DyadServerException extends Exception {
	private static final long serialVersionUID = 1L;
	public final HttpResponse serverResponse;
	
	public DyadServerException(Exception cause, HttpResponse response) {
		initCause(cause);
		serverResponse = response;
	}

	public DyadServerException(HttpResponse response) {
		serverResponse = response;
	}
	
}
