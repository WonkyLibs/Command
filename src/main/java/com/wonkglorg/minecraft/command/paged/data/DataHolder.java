package com.wonkglorg.minecraft.command.paged.data;


public class DataHolder<T>{
	private T data;
	private long lastModified = System.currentTimeMillis();
	
	public DataHolder(T data) {
		this.data = data;
	}
	
	public void setData(T data) {
		this.data = data;
		lastModified = System.currentTimeMillis();
	}
	
	public T getData() {
		return data;
	}
	
	public long getLastModified() {
		return lastModified;
	}
}
