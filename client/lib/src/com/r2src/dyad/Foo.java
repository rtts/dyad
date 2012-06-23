package com.r2src.dyad;

public interface Foo {
	public void onFinished();
	public void onError(Exception e);
	public void onCanceled();
}
