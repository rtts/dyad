package com.r2src.dyad;


import android.os.Handler;

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
	private String id;
	private DyadClient client = DyadClient.getInstance();
	
	/**
	 * Protected constructor -- call {@link DyadAccount.newDyad()} to create a
	 * new Dyad.
	 */
	protected Dyad(DyadAccount account, String id) {
		if (account == null)
			throw new IllegalArgumentException("account can't be null");
		this.account = account;
	}
	
	/**
	 * Get a stream for communication
	 * 
	 */
	public void getStream(DyadRequestCallback foo, Handler handler) {
		DyadRequest request = new DyadStreamRequest();
		client.asyncExecute(request, account, foo, handler);
	}

	public String getId() {
		return id;
	}
}
