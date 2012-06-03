package com.r2src.dyad;

import android.app.Activity;
import android.content.Intent;

/**
 * The Bonder interface defines one method, {@link #getSecret()}, which needs to return the
 * same string on both devices that attempt to bond. Your creativity is required to find a
 * way to contact the other device and exchange a secret string.
 */
public abstract class Bonder extends Activity {
	/**
	 * Sends a broadcast to let 'observers' know that the secret was found.
	 * @param secret The shared secret that the two devices have calculated
	 */
	protected void setSharedSecret(byte[] secret) {
		Intent i = new Intent()
				.setAction("com.r2src.dyad.action.GOT_SHARED_SECRET")
				.putExtra("secret", secret);
		
		sendBroadcast(i);
	}
}
