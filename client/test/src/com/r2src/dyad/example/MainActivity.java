package com.r2src.dyad.example;

import org.apache.http.HttpHost;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadListener;
import com.r2src.dyad.R;
import com.r2src.dyad.DyadAccount.GoogleAccountException;

public class MainActivity extends Activity implements DyadListener {
	private static final String TAG = "MainActivity";
	
	private static final String SENDER_ID = "137457006510";
	
	DyadAccount account;
	TextView txtSuccess, txtFailure;
	private String sessionToken;
	private String accountName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtSuccess = (TextView) findViewById(R.id.txt_registration_succeeded);
		txtFailure = (TextView) findViewById(R.id.txt_registration_failed);

		sessionToken = getSharedPreferences("test", MODE_PRIVATE)
				.getString("session_token", null);
		accountName = getSharedPreferences("test", MODE_PRIVATE)
				.getString("googleAccountName", null);

		account = new DyadAccount(new HttpHost("goliath", 9951), this, this, sessionToken);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void register(View v) {
		txtSuccess.setVisibility(View.INVISIBLE);
		txtFailure.setVisibility(View.INVISIBLE);
		if (sessionToken != null) {
			txtFailure.setText("Already registered!");
			txtFailure.setVisibility(View.VISIBLE);
		} else {
			try {
				account.register(this, accountName);
			} catch (GoogleAccountException e) {
				Log.w(TAG, "Saved Google Account does not exist anymore.");
				account.register(this);
				e.printStackTrace();
			}
		}
	}

	public void registerGCM(View v) {
		txtSuccess.setVisibility(View.INVISIBLE);
		txtFailure.setVisibility(View.INVISIBLE);
		account.registerGCM(this, SENDER_ID);
	}
	
	@Override
	public void onRegistered() {
		txtSuccess.setVisibility(View.VISIBLE);
		getSharedPreferences("test", MODE_PRIVATE).edit()
				.putString("session_token", account.getSessionToken())
				.commit();
		getSharedPreferences("test", MODE_PRIVATE)
				.edit()
				.putString("googleAccountName",
						account.getGoogleAccountName()).commit();
	}
	
	@Override
	public void onRegistrationFailed(Exception e) {
		txtFailure.setVisibility(View.VISIBLE);
		txtFailure.setText(e.getClass().toString() + ": "
				+ e.getLocalizedMessage());
		if (e.getLocalizedMessage() != null)
			Log.e(TAG, e.getLocalizedMessage());
	}

	@Override
	public void onGCMRegistered() {
		txtSuccess.setVisibility(View.VISIBLE);
	}

	@Override
	public void onGCMRegistrationFailed(Exception e) {
		txtFailure.setVisibility(View.VISIBLE);
		txtFailure.setText(e.getClass().toString() + ": "
				+ e.getLocalizedMessage());
		if (e.getLocalizedMessage() != null)
			Log.w(TAG, e.getLocalizedMessage());
	}
}
