package com.r2src.dyad.gcm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gcm.GCMBaseIntentService;
import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadGCMRequest;
import com.r2src.dyad.DyadRequest;

public class IntentService extends GCMBaseIntentService {

	protected IntentService(String senderId) {
		super(senderId);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onError(Context context, String error) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(DyadAccount.ACTION_GCM_REGISTRATION_FAILED_INTENT)
						.putExtra(DyadAccount.KEY_EXCEPTION, error));
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onRegistered(Context context, String regId) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(DyadAccount.ACTION_GCM_REGISTERED_INTENT));
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		// TODO Auto-generated method stub
	}

}
