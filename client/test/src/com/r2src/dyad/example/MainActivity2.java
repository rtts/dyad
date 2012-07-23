package com.r2src.dyad.example;

import org.apache.http.HttpHost;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.R;
import com.r2src.dyad.R.layout;
import com.r2src.dyad.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.NavUtils;

public class MainActivity2 extends Activity {
	DyadAccount account;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    	account = new DyadAccount("137457006510", new HttpHost("edge", 9951), this) {
    		@Override
    		public void onRegistered() {
    			findViewById(R.id.txt_registration_succeeded).setVisibility(View.VISIBLE);
    		}
    		
    		@Override
    		public void onRegistrationFailed(Exception e) {
    			findViewById(R.id.txt_registration_failed).setVisibility(View.VISIBLE);
    			Log.e("MAINACTIVITY", e.getLocalizedMessage());
    		}

    		@Override
    		public void onGCMRegistered() {
    			// TODO Auto-generated method stub
    			
    		}

    		@Override
    		public void onGCMRegistrationFailed(String error) {
    			// TODO Auto-generated method stub
    			
    		}
    	};
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void register(View v) {
    	account.register(this);
    }



    
}
