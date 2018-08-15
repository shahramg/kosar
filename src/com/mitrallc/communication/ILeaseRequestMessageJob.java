package com.mitrallc.communication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;

public class ILeaseRequestMessageJob extends MessageJob {
	String sql;
	Set<String> iks;
	Vector<String> trigs;
	
	ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases;
	ConcurrentHashMap<Long,String> clients;
	ConcurrentLinkedQueue<Integer> replies;
	
	public ILeaseRequestMessageJob(int coreId, SockIO socket, Semaphore latch, AtomicInteger jobCounts,
			String key, Set<String> iks, Vector<String> trigs,
			ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases, 
			ConcurrentHashMap<Long, String> clients,
			ConcurrentLinkedQueue<Integer> replies) {
		
		super(coreId, socket, latch, jobCounts);
		
		this.sql = key;
		this.iks = iks;
		this.trigs = trigs;
		this.grantedILeases = grantedILeases;
		this.clients = clients;
		this.replies = replies;
	}
	
	@Override
	public void execute(CoreConnector coreConnector) {
		try {
			byte[] response = coreConnector.acquireILease(socket, sql, iks, trigs);
	
			if (response == null) {
				System.out.println("I lease request: reply= null");
				System.exit(-1);
			}
	
			int reply = ByteBuffer.wrap(Arrays.copyOfRange(response, 0, 4)).getInt();
			switch (reply) {
			case Constants.REPLY_I_LEASE_GRANTED:
			case Constants.CMD_COPY:
			case Constants.CMD_STEAL:
			case Constants.CMD_USEONCE:
				int lease = ByteBuffer.wrap(Arrays.copyOfRange(response, 4, 8)).getInt();

				HashMap<Integer, Set<String>> et = new HashMap<>();
				et.put(lease, iks);
				grantedILeases.put(coreId, et);
				break;					
			case Constants.REPLY_I_LEASE_RETRY:										
				et = new HashMap<>();
				et.put(-1, iks);			// lease = -1 means failing to get I lease
				grantedILeases.put(coreId, et);
				break;
			}
			
			// get the details of the clients which are storing the keys
			if (reply == Constants.CMD_COPY ||
					reply == Constants.CMD_STEAL || reply == Constants.CMD_USEONCE) {
				int nclients = ByteBuffer.wrap(Arrays.copyOfRange(response, 8, 12)).getInt();
				int offset=12;
				for (int i = 0; i < nclients; i++) {
					// get client id
					long clientid = ByteBuffer.wrap(Arrays.copyOfRange(response, offset, offset+8)).getLong();
					offset+=8;
					
					// get client size
					int clientsize = ByteBuffer.wrap(Arrays.copyOfRange(response, offset, offset+4)).getInt();
					offset+=4;
					
					// get client info "ip:port"
					String client = new String(Arrays.copyOfRange(response, offset, offset+clientsize));
					offset += clientsize;
					clients.put(clientid, client);
				}
			}
			
			replies.add(reply);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {
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
