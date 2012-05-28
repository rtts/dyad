package com.r2src.dyad;

/**
 * The Bonder interface defines one method, {@link #getSecret()}, which needs to return the
 * same string on both devices that attempt to bond. Your creativity is required to find a
 * way to contact the other device and exchange a secret string.
 */
public interface Bonder {
	public String getSecret();
}
