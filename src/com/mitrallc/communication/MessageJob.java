package com.mitrallc.communication;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.sql.SockIO;

public abstract class MessageJob {
	Semaphore latch;	
	AtomicInteger jobCounts;	
	int coreId;
	SockIO socket;
	
	public MessageJob(int coreId, SockIO socket, Semaphore latch, AtomicInteger jobCounts) {
		this.coreId = coreId;
		this.socket = socket;
		this.latch = latch;
		this.jobCounts = jobCounts;
	}
	
	public abstract void execute(CoreConnector coreConnector);
	
	/**
	 * Finish the job.
	 */
	public void finishJob() {
		if (jobCounts != null) {
			if (this.jobCounts.decrementAndGet() == 0) {
				if (latch != null) {
					this.latch.release();
				}
			}		
		}
	}
}
