package com.r2src.dyad;

public interface DyadListener {
	
	/**
	 * Called when the {@link DyadAccount} has been successfully registered with the Dyad Server.
	 */
	public abstract void onRegistered();
	public abstract void onRegistrationFailed(Exception e);

	public abstract void onGCMRegistered();
	public abstract void onGCMRegistrationFailed(Exception e);
	
}
