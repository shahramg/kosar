package com.mitrallc.communication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;

public class ILeaseReleaseMessageJob extends MessageJob {
	int cmd;
	String key;
	int leaseNum;
	
	Set<String> iks;
	long gotFromClientId;
	ConcurrentLinkedQueue<Integer> replies;
	
	public ILeaseReleaseMessageJob(int cmd, int coreId, SockIO socket, Semaphore latch, AtomicInteger jobCounts,
			String key, int leaseNum, Set<String> iks, long gotFromClientId2, ConcurrentLinkedQueue<Integer> replies) {
		super(coreId, socket, latch, jobCounts);

		this.cmd = cmd;
		this.key = key;
		this.leaseNum = leaseNum;
		this.iks = iks;
		this.gotFromClientId = gotFromClientId2;
		this.replies = replies;
	}

	@Override
	public void execute(CoreConnector coreConnector) {
		try {
			if (leaseNum == -1) {
				replies.add(Constants.REPLY_LEASE_ABORTED);
			} else {			
				byte[] reply = coreConnector.releaseILease(cmd, socket, key, leaseNum, iks, gotFromClientId);
				int rep = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();
				replies.add(rep);
			}
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
			try {
				if (socket != null)
					KosarSoloDriver.getConnectionPool(coreId).checkIn(socket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
			}
		}
		
		finishJob();
	}
}
