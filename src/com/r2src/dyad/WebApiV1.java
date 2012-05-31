package com.r2src.dyad;

public abstract class WebApiV1 {
	
	private static final String version = "v1";
	private static final String register = "register";
			
	public static String getUrl(String call) {
		if (register.equals(call)) {
			return join(version, register);
		}
		return null;
	}
	
	private static String join(String... strings) {
		String result = "";
		for (String s : strings) {
			result = result + "/" + s;
		}
		return result; 
	}
}