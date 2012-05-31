package com.r2src.dyad;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Intent;

/**
 * A Dyad is a pair of individuals maintaining a socially significant
 * relationship. The prime purpose of a Dyad is to establish a peer-to-peer
 * communication channel between these individuals, which can then be used to
 * transfer any type of streaming data.
 * <p>
 * Dyads need to be bonded by calling {@link #bond} after setting a
 * {@link Bonder} object with the {@link #setBonder} method. When a second
 * device calls the bond method and its bonder supplies the same secret, the
 * server will bond the devices and the Dyad is fully operational.
 * <p>
 * A fully operational Dyad can be used to request a {@link DyadStream} object
 * by calling {@link #getStream}.
 */
public class Dyad {
	private DyadAccount account;
	private Bonder bonder;
	private final ExecutorService exec = Executors.newCachedThreadPool();

	/**
	 * Protected constructor -- call {@link DyadAccount.newDyad()} to create a
	 * new Dyad.
	 */
	protected Dyad(DyadAccount account) {
		if (account == null)
			throw new IllegalArgumentException("account can't be null");
		this.account = account;
	}

	/**
	 * Set a bonder that implements the {@link Bonder} interface
	 */
	public void setBonder(Bonder bonder) {
		if (account == null)
			throw new IllegalArgumentException("bonder can't be null");
		this.bonder = bonder;
	}

	/**
	 * Performs the bonding. Starts an activity that the specific bonder
	 * implementation can use to find a shared secret and pass this to the
	 * server.
	 */
	public void bond(Activity activity, Foo foo) {
		activity.startActivity(new Intent(activity, bonder.getClass()));
		// TODO send secret to server
		
	}
	
	/**
	 * Get a stream for communication
	 * 
	 */
	public DyadStream getStream() {
		// TODO
		return null;
	}
}
