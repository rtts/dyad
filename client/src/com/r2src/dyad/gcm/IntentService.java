package com.r2src.dyad.gcm;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gcm.GCMBaseIntentService;
import com.r2src.dyad.Account;
import com.r2src.dyad.request.RegisterGCMRequest;
import com.r2src.dyad.request.Request;


/**
 * The service needed for GCM. Doesn't do anything by itself, but re-broadcasts all events to
 * be picked up by a {@link Account} object
 *
 */
public class IntentService extends GCMBaseIntentService {
	
	@Override
	protected void onError(Context context, String error) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(Account.ACTION_GCM_REGISTRATION_FAILED_INTENT)
						.putExtra(Account.KEY_EXCEPTION, error));
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onRegistered(Context context, String regId) {
		// TODO handle request from here (create dyadaccount)
		Request request = new RegisterGCMRequest(regId);
		
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(Account.ACTION_GCM_REGISTERED_INTENT).putExtra("regId", regId));
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected String[] getSenderIds(Context context) {
		String senderId = context.getApplicationInfo().metaData.getString("com.r2src.dyad.GCM_SENDER_ID");
		if (senderId == null) {
			throw new RuntimeException("GCM Sender ID is not present in the Android Manifest.");
		}
		String[] senderIds = {senderId};
		return senderIds;
	}
	
}
