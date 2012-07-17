package com.r2src.dyad;

public interface DyadRequestCallback {
	public void onFinished();
	public void onError(Exception e);
	public void onCanceled();
}
