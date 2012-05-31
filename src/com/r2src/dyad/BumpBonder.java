package com.r2src.dyad;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.bump.api.BumpAPIIntents;
import com.bump.api.IBumpAPI;

public abstract class BumpBonder extends Bonder {
	private IBumpAPI api;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();
	        try {
	            if (action.equals(BumpAPIIntents.DATA_RECEIVED)) {
	                Log.i("Bump Test", "Received data from: " + api.userIDForChannelID(intent.getLongExtra("channelID", 0)));
	                Log.i("Bump Test", "Data: " + new String(intent.getByteArrayExtra("data")));
	            } else if (action.equals(BumpAPIIntents.MATCHED)) {
	                api.confirm(intent.getLongExtra("proposedChannelID", 0), true);
	            } else if (action.equals(BumpAPIIntents.CHANNEL_CONFIRMED)) {
	                api.send(intent.getLongExtra("channelID", 0), "Hello, world!".getBytes());
	            } else if (action.equals(BumpAPIIntents.CONNECTED)) {
	                api.enableBumping();
	            }
	        } catch (RemoteException e) {}
	    }
	};
	
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			api = IBumpAPI.Stub.asInterface(binder);
			try {
				api.configure(getBumpApiKey(), getBumpUserName());
			} catch (RemoteException e) {}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		bindService(new Intent(IBumpAPI.class.getName()),
				connection, 
				Context.BIND_AUTO_CREATE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BumpAPIIntents.CHANNEL_CONFIRMED);
		filter.addAction(BumpAPIIntents.DATA_RECEIVED);
		filter.addAction(BumpAPIIntents.NOT_MATCHED);
		filter.addAction(BumpAPIIntents.MATCHED);
		filter.addAction(BumpAPIIntents.CONNECTED);
		registerReceiver(receiver, filter);
		
	}
	
	public abstract String getBumpApiKey();
	public abstract String getBumpUserName();
	
}
