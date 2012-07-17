package com.r2src.dyad.gcm;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;
import com.r2src.dyad.DyadGCMRequest;
import com.r2src.dyad.DyadRequest;

public class IntentService extends GCMBaseIntentService {
	
	protected IntentService(String senderId) {
		super(senderId);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onError(Context arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onMessage(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		DyadRequest request = new DyadGCMRequest(regId);
		// TODO: find a way to execute this request
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		// TODO Auto-generated method stub
	}

}
