package com.r2src.dyad.request;

import java.io.IOException;

import org.apache.http.HttpResponse;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadServerException;


public class DyadStreamRequest extends DyadRequest {

	public DyadStreamRequest() {
		// TODO method stub
	}
	
	@Override
	public void onFinished(HttpResponse response, DyadAccount account)
			throws DyadServerException, IOException {
		// TODO method stub

	}

}
