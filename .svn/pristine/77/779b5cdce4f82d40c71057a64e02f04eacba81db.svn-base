package com.mitrallc.common;

import java.util.concurrent.Semaphore;

public class LockManager {
	private static int NUM_FRAGMENTS = 1009;
	
	private Semaphore[] locks;
	
	public LockManager() {
		locks = new Semaphore[NUM_FRAGMENTS];
		for (int i = 0; i < NUM_FRAGMENTS; i++) {
			locks[i] = new Semaphore(1, true);
		}
	}
	
	public void lock(String key) {
		int idx = getFragment(key);		

		try {
			locks[idx].acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
	}
	
	public void unlock(String key) {
		int idx = getFragment(key);		
		locks[idx].release();
	}
	
	private static int getFragment(String key) {
		if (NUM_FRAGMENTS <= 0) {
			return -1;
		}
		
//		int len = 0;
//		int hcode = 0;
//		while (len < key.length()) {
//			hcode = (hcode << 19) - hcode + key.charAt(len);
//			len++;
//		}
//		
//		hcode = hcode < 0 ? ((~hcode) + 1) : hcode;
//		hcode = hcode % NUM_FRAGMENTS;
//		
//		return hcode;		

		int hash = key.hashCode();
		hash = (hash < 0 ? ((~hash) + 1) : hash);
		return hash % NUM_FRAGMENTS;
	}
}
