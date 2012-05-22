package com.r2src.dyad;

import java.io.IOException;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Dyad {
	private static final String TAG = "Dyad";
	private Bonder bonder;
	State state;
	
	enum State {
		VIRGIN,
		REGISTERED,
		LOGGED_IN,
		BONDED
	};
	
	public void setBonder(Bonder b) {
		bonder = b;
	}
	
	/**
	 * Registers the user with the Dyad server
	 * @param activity Needed because the accountmanager might prompt the user
	 */
	public void register(Activity activity) {

		try {
			Account account = getAccount(activity);
			Log.d(TAG, "Account name: " + account.getAccountName());
			if (account.getAuthToken() != null && account.getAuthToken() != "") {
				Log.d(TAG, "Non-empty Account Token found");
			}
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//request(token);
		//send(request);
		
	}
	
	public Account getAccount(Activity activity) throws OperationCanceledException, AuthenticatorException, IOException {
		AccountManager manager = AccountManager.get(activity);
		
		/*
		 * The account type "com.google" matches all Google accounts.
		 * In case of multiple accounts, the getAuthTokenByFeatures
		 * function takes care of prompting the user for a choice.
		 * 
		 * The auth token type "Email" is a valid OAuth2 alias for
		 * "oauth2:https://www.googleapis.com/auth/userinfo.email"
		 * 
		 * The eventually returned token is thus an OAuth2 token to be
		 * used server-side to request the user's email address from
		 * the GData API
		 */
		AccountManagerFuture<Bundle> future = manager.getAuthTokenByFeatures(
				"com.google", "Email", null, activity, null, null, null, null);
		Bundle bundle;
		bundle = future.getResult();
		
		String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
		String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		if (accountName == null || authToken == null)
			throw new AuthenticatorException();
		return new Account(accountName, authToken);
	}
	
	class Account {
		String accountName;
		String authToken;
		
		public Account(String accountName, String authToken) {
			this.accountName = accountName;
			this.authToken = authToken;
		}
		
		public String getAccountName() {
			return accountName;
		}
		
		public String getAuthToken() {
			return authToken;
		}
	}
	
	public boolean login() {
		return false;
		
	}
	public boolean bond() {
		bonder.createBond();
		//...
		return false;
	}
	public DyadStream getStream() {
		return null;
		
	}
	
	public static Dyad load(String s) {
		return null;
	}
	
	public String dump() {
		return null;
	}
}
