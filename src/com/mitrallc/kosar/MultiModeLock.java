package com.mitrallc.kosar;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiModeLock {

	private final boolean usingSemaphore = true;

	public int id;
	//AtomicInteger lock = new AtomicInteger(1);
	
	AtomicBoolean spinLock = new AtomicBoolean(true);
	Semaphore lock = new Semaphore(1);
	
	
	public MultiModeLock(){
		Random r = new Random();
		id = r.nextInt(Integer.MAX_VALUE);
	}
	
	private void acquireSemaphore(){
		try{
			lock.acquire();
		}
		catch(Exception e){
			e.printStackTrace(System.out);
		}		
	}
	
	public void acquire(){
		
		if(usingSemaphore){
			acquireSemaphore();
		}
		else{
			boolean previous;
			while(true){	
				previous = spinLock.getAndSet(false);
				if(previous == true){
					break;
				}
			}
		}
	//	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}
	
	public boolean tryAcquire(){
		
		boolean retVal = false;
		
		if(usingSemaphore){
			retVal = lock.tryAcquire();
		}
		else{
			retVal = spinLock.getAndSet(false);
		}
		
	//	if(retVal)
	//		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		return retVal;
		
	}
	
	public void release(){
	
		if(usingSemaphore){
			lock.release();
		}
		else{
			boolean previous = spinLock.getAndSet(true);
		
			if(previous)
				System.out.println("Not good");
		}
		//Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

	}
}
