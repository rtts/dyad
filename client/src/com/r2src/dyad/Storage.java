package com.r2src.dyad;


public class Storage {
	/*
	private volatile static Storage singleton;

	private SharedPreferences sharedPrefs;
	private DyadOpenHelper db;
	
	private Storage(Context context) {
		sharedPrefs = context.getSharedPreferences("Dyad", Context.MODE_PRIVATE);
		db = new DyadOpenHelper(context);
	}

	static Storage getInstance(Context context) {
		if (singleton == null) {
			synchronized (Storage.class) {
				if (singleton == null) {
					singleton = new Storage(context);
				}
			}
		}
		return singleton;
	}
	
	
	List<DyadAccount> getAccounts() {
		// TODO: method stub
		return new ArrayList<DyadAccount>();
	}
	

	boolean setGoogleAccountName(DyadAccount account, String googleAccountName) {
		SharedPreferences.Editor ed = sharedPrefs.edit();
		ed.putString("googleAccountName", googleAccountName);
		ed.commit();
		return true;
	}

	boolean setAuthToken(DyadAccount account, String authToken) {
		SharedPreferences.Editor ed = sharedPrefs.edit();
		ed.putString("authToken", authToken);
		ed.commit();
		return true;
	}

	boolean setPushRegId(String regId) {
		SharedPreferences.Editor ed = sharedPrefs.edit();
		ed.putString("pushRegId", regId);
		ed.commit();
		return true;
	}
	
	String getGoogleAccountName() {
		return sharedPrefs.getString("googleAccountName", null);
	}

	String getAuthToken() {
		return sharedPrefs.getString("authToken", null);
	}
	
	public class DyadOpenHelper extends SQLiteOpenHelper {

	    private static final int DATABASE_VERSION = 1;
	    private static final String DATABASE_NAME = "dyad";
	    
	    
	    private static final String KEY_HOST = "host";
	    private static final String KEY_GOOGLE_ACCOUNT = "google_account";
	    private static final String KEY_SESSION_TOKEN = "session_token";
	    
	    
	    private static final String DYAD_TABLE_NAME = "DyadAccount";
	    private static final String DYAD_TABLE_CREATE =
	                "CREATE TABLE " + DYAD_TABLE_NAME + " (" +
	                KEY_HOST + " TEXT, " +
	                KEY_GOOGLE_ACCOUNT + " TEXT, " +
	                KEY_SESSION_TOKEN + " TEXT);";

	    DyadOpenHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(DYAD_TABLE_CREATE);
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
	}
	*/

}
