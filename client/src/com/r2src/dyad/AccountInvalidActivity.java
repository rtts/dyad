package com.r2src.dyad;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class AccountInvalidActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_invalid);
        findViewById(R.id.btn_add_account).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
    }

    
}
