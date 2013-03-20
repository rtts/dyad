package com.r2src.dyad.example;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.ViewSwitcher;

import com.r2src.dyad.Account;
import com.r2src.dyad.DyadListener;

public class MainActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Account.reset(this);
		Account account = Account.getInstance(this);
		if (account.isRegistered()) {
			onRegistered();
		} else {
			account.setDyadListener(new DyadListener() {
				
				@Override
				public void onRegistrationFailed(Exception e) {
					e.printStackTrace();
				}
				
				@Override
				public void onRegistered() {
					MainActivity.this.onRegistered();
				}
				
				@Override
				public void onGCMRegistrationFailed(Exception e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onGCMRegistered() {
					// TODO Auto-generated method stub
					
				}
			});
		}
	}
	
	private void onRegistered() {
		ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.viewSwitcher1);
		vs.showNext();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
