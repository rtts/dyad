package com.r2src.dyad;

public class WebApiV1 {
	private static final String version = "v1"
	private static final String register = "register";
			
	public static String getUrl(String call) {
		switch (call) {
		case register:
			return join(version, register);
		}
	}
	
	private static String join(String s1 .. String sn) {
		return s1 + "/" + s2 + "/" + ... + sn
	}
}
