package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.common.Constants;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;
import com.mitrallc.sqltrig.QueryToTrigger;

public class CoreConnector {
	ConcurrentLinkedQueue<Integer> replies = 
			new ConcurrentLinkedQueue<Integer>();
	
	Semaphore latch = new Semaphore(0);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	ByteBuffer bb = ByteBuffer.allocate(4);
	ByteBuffer bb2 = ByteBuffer.allocate(8);
	
	public byte[] register(SockIO sock, long clientid, int port) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer bb = ByteBuffer.allocate(4 + 8 + 4);
		bb.putInt(Constants.CLIENT_REGISTER);
		bb.putLong(clientid);
		bb.putInt(port);
		baos.write(bb.array());

		// convert to byte array
		byte[] registerMessage = baos.toByteArray();

		// send message
		sock.write(registerMessage);
		sock.flush();			

		baos.reset();
		bb = null;

		// wait for reply in which coordinator sends id.
		// then store id and change port information that is stored.
		return sock.readBytes();
	}

	private HashMap<Integer, Set<String>> coresToIks(Set<String> iks) {
		HashMap<Integer, Set<String>> hm = new HashMap<Integer, Set<String>>();

		if (iks == null) {
			System.out.println("iks = null.");
			return hm;
		}

		for (String ik: iks) {
			int idx = KosarSoloDriver.getCoreForKey(ik);

			Set<String> et = hm.get(idx);
			if (et == null) {
				et = new HashSet<String>();
				et.add(ik);
				hm.put(new Integer(idx), et);
			} else {
				et.add(ik);
			}
		}

		return hm;
	}
	
	public int acquireILease2(String sql, ConcurrentHashMap<Integer, 
			HashMap<Integer, Set<String>>> grantedILeases, ConcurrentHashMap<Long,String>clients) {
		QueryToTrigger qt = KosarSoloDriver.Kache.qt;
//		String ParamQry = qt.TokenizeWhereClause(sql);
//		if (ParamQry == null)
//			ParamQry = sql; // qry has no where clause

		Vector<String> internal_key_list = new Vector<String>();
		Vector<String> trgrs = new Vector<String>();

		qt.TransformQuery(sql, trgrs, internal_key_list, regthread.db_conn);
//		String[] keys = KosarSoloDriver.Kache.getIks(sql);
//		for (String key: keys) {
//			internal_key_list.add(key);
//		}

		Vector<String> trigs = new Vector<String>();

		// maintain a mapping between internal keys and Core
		Set<String> iks = new HashSet<String>();
		iks.addAll(internal_key_list);
		HashMap<Integer, Set<String>> map = coresToIks(iks);

		Set<Integer> cores = map.keySet();		
		if (cores.size() == 0) {
			System.out.println("Error: No core to acquire I leases");
			System.exit(-1);
		}

		AtomicInteger jobCounts = new AtomicInteger(cores.size());
		for (Integer core: cores) {			
			SockIO socket = null;
			socket = KosarSoloDriver.getConnectionPool(core).getConnection();
			Set<String> internal_tokens = map.get(core);

			MessageJob job = new ILeaseRequestMessageJob(core.intValue(), socket, latch, jobCounts,
					sql, internal_tokens, trigs, grantedILeases, clients, replies);
			int x = KosarSoloDriver.rand.nextInt(Kosar.NumFragments);
			KosarSoloDriver.msgJobQueue[x].add(job);
			KosarSoloDriver.msgJobSemaphores[x].release();
		}		

		try {
			latch.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
//		System.out.println("aIL: "+latch.availablePermits());
		
		if (replies.size() != cores.size()) {
			System.out.println("Error AcquireILease: reply size="+replies.size()
					+" core size="+cores.size());
		}

		// analyze the replies to get the results
		int result = Constants.REPLY_I_LEASE_RETRY;
		int cnt = 0;
		for (int rep : replies) {
			if (rep == Constants.REPLY_I_LEASE_RETRY) {
				result = Constants.REPLY_I_LEASE_RETRY;
				break;
			}

			if ((rep == Constants.CMD_COPY) || 
					(rep == Constants.CMD_STEAL) || (rep == Constants.CMD_USEONCE)) {
				if (result != Constants.CMD_STEAL) {
					result = rep;
				}
			}

			if (rep == Constants.REPLY_I_LEASE_GRANTED) {
				cnt++;
			}
		}

		if (cnt == replies.size()) {
			result = Constants.REPLY_I_LEASE_GRANTED;
		}
		
		replies.clear();

		return result;		
	}
	

	public byte[] acquireILease(SockIO socket, String key, Set<String> tokens, Vector<String> triggers) throws IOException {
		/* send i lease request */		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(Constants.CLIENT_ACQUIRE_I_LEASE); 		// add command type
		baos.write(bb.array());
		bb.clear();

//		ByteBuffer bb2 = ByteBuffer.allocate(8);
		bb2.putLong(KosarSoloDriver.clientData.getID());
		baos.write(bb2.array());						// add client id.		
		bb2.clear();		

		// add internal key length
		bb.putInt(tokens.size());
		baos.write(bb.array());
		bb.clear();

		// add key length
		byte[] keyBytes = key.getBytes();
		bb.putInt(keyBytes.length);
		baos.write(bb.array());
		bb.clear();

		Vector<String> sendTrigs = new Vector<String>();
		//		String sql = QueryToTrigger.TokenizeWhereClause(key);
		//		QTmeta qtm = null;
		//		
		//		if (sql != null)
		//			qtm = QueryToTrigger.TriggerCache.get(sql);
		//		
		//		if (qtm == null || !qtm.isTriggersRegistered()) {
		//			if (triggers != null)
		//				sendTrigs = triggers;
		//		}

		// add trigger length
		bb.putInt(sendTrigs.size());
		baos.write(bb.array());
		bb.clear();

		// add clients size
		// to be fixed
		bb.putInt(0);
		baos.write(bb.array());
		bb.clear();			

		// add key
		baos.write(keyBytes);

		// add internal key		
		if (tokens != null) {
			for (String ik : tokens) {
				byte[] ikbytes = ik.getBytes();
				bb.putInt(ikbytes.length);
				baos.write(bb.array());
				bb.clear();
				baos.write(ikbytes);
			}
		}

		//		writeToFile(key + ": \n");
		for (String trigger : sendTrigs) {
			//			writeToFile(trigger + "\n");
			byte[] trigBytes = trigger.getBytes();
			bb.putInt(trigBytes.length);
			baos.write(bb.array());
			bb.clear();

			baos.write(trigBytes);
		}
		//		writeToFile("\n");

		baos.flush();

		socket.write(baos.toByteArray());
		socket.flush();

		baos.reset();

		return socket.readBytes();
	}

	public byte[] releaseILease(int cmd,
			SockIO socket, String key, int leaseNumber, Set<String> tokens, long gotFromClientId) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
//		ByteBuffer bb = ByteBuffer.allocate(4);

		bb.putInt(cmd);
		baos.write(bb.array());
		bb.clear();

//		ByteBuffer bb2 = ByteBuffer.allocate(8);
		bb2.putLong(KosarSoloDriver.clientData.getID());
		baos.write(bb2.array());						// add client id.		
		bb2.clear();		

		bb.putInt(leaseNumber);
		baos.write(bb.array());
		bb.clear();

		bb.putInt(tokens.size());
		baos.write(bb.array());
		bb.clear();

		byte[] keyBytes = key.getBytes();

		bb.putInt(keyBytes.length);
		baos.write(bb.array());
		bb.clear();
		
		if (cmd == Constants.CLIENT_RELEASE_I_STEAL || cmd == Constants.CLIENT_RELEASE_I_USEONCE) {
			bb2.putLong(gotFromClientId);
			baos.write(bb2.array());
			bb2.clear();			
		}

		baos.write(keyBytes);

		// add internal key			
		for (String ik : tokens) {
			byte[] ikbytes = ik.getBytes();
			bb.putInt(ikbytes.length);
			baos.write(bb.array());
			bb.clear();
			baos.write(ikbytes);
		}

		baos.flush();

		socket.write(baos.toByteArray());
		socket.flush();

		baos.reset();

		return socket.readBytes();
	}
	
	public void releaseQLease2(int transId, int isCommit) {
		ConcurrentHashMap<Integer, Integer> cores = KosarSoloDriver.pendingTrans.remove(transId);		
		if (cores == null) {
			System.out.println("ReleaseQLease: Cannot find cores corresponding to this transaction Id");
			return;
		}

		for (int coreId : cores.keySet()) {
			SockIO socket = null;
			socket = KosarSoloDriver.getConnectionPool(coreId).getConnection();
			MessageJob job = new QLeaseReleaseMessageJob(coreId, socket, null, null,
					transId, isCommit, null);		
			int x = KosarSoloDriver.rand.nextInt(Kosar.NumFragments);
			KosarSoloDriver.msgJobQueue[x].add(job);
			KosarSoloDriver.msgJobSemaphores[x].release();
		}
	}	

	public byte[] commitTrans(SockIO socket, int transId) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(Constants.CLIENT_COMMIT_TID); 		// add command type
		baos.write(bb.array());
		bb.clear();		

//		ByteBuffer bb2 = ByteBuffer.allocate(8);
		bb2.putLong(KosarSoloDriver.clientData.getID());
		baos.write(bb2.array());						// add client id.		
		bb2.clear();			

		bb.putInt(transId); 		// add transaction id
		baos.write(bb.array());
		bb.clear();			

		baos.flush();

		socket.write(baos.toByteArray());
		socket.flush();

		baos.reset();

		byte[] reply = socket.readBytes();

		return reply;
	}

	public byte[] abortTrans(SockIO socket, int transId) throws IOException {
//		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(Constants.CLIENT_ABORT_TID); 		// add command type
		baos.write(bb.array());
		bb.clear();		

//		ByteBuffer bb2 = ByteBuffer.allocate(8);
		bb2.putLong(KosarSoloDriver.clientData.getID());
		baos.write(bb2.array());						// add client id.		
		bb2.clear();		

		bb.putInt(transId); 		// add transaction id
		baos.write(bb.array());
		bb.clear();			

		baos.flush();

		socket.write(baos.toByteArray());
		socket.flush();

		baos.reset();

		return socket.readBytes();		
	}

	public int releaseILease2(int cmd, String sql,
			ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases, long gotFromClientId) {
		int retval = Constants.NOT_STORED;

		if (grantedILeases == null || grantedILeases.size() == 0) {
			return retval;
		}		
		
		AtomicInteger jobCounts = new AtomicInteger(grantedILeases.keySet().size());
		for (Integer idx : grantedILeases.keySet()) {
			SockIO socket = null;

			HashMap<Integer, Set<String>> data = grantedILeases.get(idx);
			Set<Integer> leases = data.keySet();
			Set<String> iks;

			int leaseNum = -1;
			for (Integer lease: leases) {
				leaseNum = lease; 
			}

			iks = data.get(leaseNum);				
			socket = KosarSoloDriver.getConnectionPool(idx).getConnection();
			
			MessageJob job = new ILeaseReleaseMessageJob(cmd, idx.intValue(), socket, latch, jobCounts, 
					sql, leaseNum, iks, gotFromClientId, replies);
			int x = KosarSoloDriver.rand.nextInt(Kosar.NumFragments);
			KosarSoloDriver.msgJobQueue[x].add(job);
			KosarSoloDriver.msgJobSemaphores[x].release();
		} 
		
		try {
			latch.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}	
		
//		System.out.println(latch.availablePermits());
		
		if (replies.size() != grantedILeases.keySet().size()) {
			System.out.println("Error ReleaseILease: reply size="+replies.size()
				+" core size="+grantedILeases.keySet().size());
		}

		int cnt = 0;
		boolean aborted = false;
		for (int rep : replies) {
			if (rep == Constants.REPLY_LEASE_ABORTED) {
				retval = Constants.NOT_STORED;
				aborted = true;
				break;
			}

			if (aborted == false && (rep == Constants.REPLY_LEASE_RELEASE ||
					rep == Constants.REPLY_STOLEN_SUCCESS)) {
				cnt++;
			}			
		}

		if (aborted == false && cnt == replies.size()) {
			retval = Constants.STORED;
		}	
		
		replies.clear();

		return retval;
	}
	
}