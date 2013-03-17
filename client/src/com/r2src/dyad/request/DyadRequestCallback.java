package com.r2src.dyad.request;

public interface DyadRequestCallback {
	public void onFinished();
	public void onError(Exception e);
}
