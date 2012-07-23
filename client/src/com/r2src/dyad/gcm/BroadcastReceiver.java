package com.r2src.dyad.gcm;

import android.content.Context;

public class BroadcastReceiver extends
		com.google.android.gcm.GCMBroadcastReceiver {

	@Override
	protected String getGCMIntentServiceClassName(Context context) {
        return "com.r2src.dyad.gcm.IntentService";
    }

}
