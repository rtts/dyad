package com.r2src.dyad.request;

import java.io.IOException;

import org.apache.http.HttpResponse;

import com.r2src.dyad.Account;
import com.r2src.dyad.ServerException;


public class StreamRequest extends Request {

	public StreamRequest() {
		// TODO method stub
	}
	
	@Override
	public void onFinished(HttpResponse response, Account account)
			throws ServerException, IOException {
		// TODO method stub

	}

}
