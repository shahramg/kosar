package com.mitrallc.communication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;

public class QLeaseReleaseMessageJob extends MessageJob {
	int isCommit;
	int transId;
	
	ConcurrentLinkedQueue<Integer> replies;
	
	public QLeaseReleaseMessageJob(int coreId, SockIO socket, Semaphore latch,
			AtomicInteger jobCounts, int transId, int isCommit,
			ConcurrentLinkedQueue<Integer> replies) {
		super(coreId, socket, latch, jobCounts);
		
		this.transId = transId;
		this.isCommit = isCommit;
		
		this.replies = replies;
	}

	@Override
	public void execute(CoreConnector coreConnector) {
		byte[] bytes;
		
		try {
			if (isCommit == 0) {	// send request to release the Q lease hold on the core
				if (KosarSoloDriver.webServer != null) {
					KosarSoloDriver.KosarQLeaseReleasedEventMonitor.newEvent(1);
				}
				bytes = coreConnector.commitTrans(socket, transId);									
			} else {
				if (KosarSoloDriver.webServer != null) {
					KosarSoloDriver.KosarQLeaseAbortEvntMonitor.newEvent(1);
				}
				bytes = coreConnector.abortTrans(socket, transId);
			}
			
			if (replies != null)
				replies.add(ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4)).getInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		} finally {
			if (socket != null)
				try {
					KosarSoloDriver.getConnectionPool(coreId).checkIn(socket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
		}		
		
		this.finishJob();
	}
}
