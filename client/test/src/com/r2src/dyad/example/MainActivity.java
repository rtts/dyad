package com.r2src.dyad.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.DyadAccount.GoogleAccountException;
import com.r2src.dyad.DyadListener;
import com.r2src.dyad.R;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
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

		account = new DyadAccount(new MyDyadListener(), this, sessionToken);
		
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
		account.registerGCM(this);
	}
	
	private class MyDyadListener implements DyadListener {
		@Override
		public void onRegistered() {
			Log.v(TAG, "Registered " + account.getGoogleAccountName());
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
				Log.w(TAG, e.getLocalizedMessage());
		}

		@Override
		public void onGCMRegistered() {
			Log.v(TAG, "GCM Registration succeeded");
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
}
