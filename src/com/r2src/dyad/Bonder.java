package com.r2src.dyad;

import android.app.Activity;

/**
 * The Bonder interface defines one method, {@link #getSecret()}, which needs to return the
 * same string on both devices that attempt to bond. Your creativity is required to find a
 * way to contact the other device and exchange a secret string.
 */
public abstract class Bonder extends Activity {
	public abstract String getSecret();
}
