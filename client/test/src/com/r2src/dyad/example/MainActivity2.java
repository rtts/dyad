package com.r2src.dyad.example;

import org.apache.http.HttpHost;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.r2src.dyad.DyadAccount;
import com.r2src.dyad.R;

public class MainActivity2 extends Activity {
	DyadAccount account;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    	account = new DyadAccount("137457006510", new HttpHost("goliath", 9951), this) {
    		@Override
    		public void onRegistered() {
    			findViewById(R.id.txt_registration_succeeded).setVisibility(View.VISIBLE);
    		}
    		
    		@Override
    		public void onRegistrationFailed(Exception e) {
    			TextView v = (TextView) findViewById(R.id.txt_registration_failed);
    			v.setVisibility(View.VISIBLE);
    			v.setText(e.getClass().toString() + ": " + e.getLocalizedMessage());
    			if (e.getLocalizedMessage() != null) Log.e("MAINACTIVITY", e.getLocalizedMessage());
    		}

    		@Override
    		public void onGCMRegistered() {
    			// TODO Auto-generated method stub
    			
    		}

    		@Override
    		public void onGCMRegistrationFailed(Exception e) {
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
