package com.r2src.dyad;

import org.apache.http.HttpResponse;

public class ServerException extends Exception {
	private static final long serialVersionUID = 1L;
	public final HttpResponse serverResponse;
	
	public ServerException(Exception cause, HttpResponse response) {
		initCause(cause);
		serverResponse = response;
	}

	public ServerException(HttpResponse response) {
		serverResponse = response;
	}
	
}
