package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import com.mitrallc.common.Constants;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.sql.SockIO;
import com.mitrallc.sql.KosarSoloDriver;

public class ClientRequestHandler extends Thread {	
	private boolean reject = false;
	Socket sock;
	SockIO socket;
	int response = 0;
	boolean verbose = false;
	Kosar cachemgr;

	public ClientRequestHandler(Socket sock, Kosar cachemgr) {
		this.sock = sock;
		this.cachemgr = cachemgr;
	}	

	@Override
	public void run() {
		int keyLength = -1;
		String key = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer bb = ByteBuffer.allocate(4);

		try {
			socket = new SockIO(sock);
			HashSet<String> its = new HashSet<String>();
			long clientId;
			int tid;
			int coreport;
			int size;
			int command;

			while (true) {
				byte[] b = socket.readBytes();
				ByteBuffer byteBuffer = ByteBuffer.wrap(b);
				command = byteBuffer.getInt();

				if (verbose)
					System.out.println("Receive command: cmd_id="+command);

				switch (command) {
				case Constants.CMD_INVALIDATE:
					clientId = byteBuffer.getLong();
					tid = byteBuffer.getInt();
					coreport = byteBuffer.getInt();
					size = byteBuffer.getInt();
					
					for (int i = 0; i < size; i++) {
						int ikLen = byteBuffer.getInt();				
						byte[] s = new byte[ikLen]; 
						byteBuffer.get(s, 0, ikLen);
						String ik = new String(s);
						its.add(ik);
					}

					if (clientId == KosarSoloDriver.clientData.getID()) {
						String addr = sock.getInetAddress().getHostAddress();
						addr += ":"+coreport;

						Integer coreId = KosarSoloDriver.cores.get(addr);
						if (coreId == null) {
							System.out.println("ClientRequestHandler: Invalid core id.");
						} else {
							ConcurrentHashMap<Integer, Integer> queue = KosarSoloDriver.pendingTrans.get(tid);
							if (queue == null) {
								queue = new ConcurrentHashMap<Integer, Integer>();
								queue.put(coreId,coreId);
								queue = KosarSoloDriver.pendingTrans.putIfAbsent(tid, queue);
							}
							if (queue != null) {
								queue.putIfAbsent(coreId, coreId);
							}
						}
					}

					if (verbose)
						System.out
						.println("KosarSolo Driver TriggerListener:  keylist "
								+ its);

					for (String it : its) {
						if (cachemgr != null && it.length() > 0) {
							if (KosarSoloDriver.webServer != null)
								KosarSoloDriver.KosarInvalKeysAttemptedEventMonitor.newEvent(1);
							cachemgr.DeleteIT(it);
						}
					}
					its.clear();

					if (verbose) System.out.println("InvThread response: "+response);					

					bb.putInt(response);
					socket.write(bb.array());
					socket.flush();
					bb.clear();

					break;
				case Constants.CMD_STEAL:
				case Constants.CMD_COPY:
				case Constants.CMD_USEONCE:
					if (reject) {
						bb.putInt(Constants.REP_REJECT);
						socket.write(bb.array());
						socket.flush();
						bb.clear();
						break;
					}

					keyLength = byteBuffer.getInt();					
					byte[] s = new byte[keyLength];
					byteBuffer.get(s, 0, keyLength);
					key = new String(s);

					// get the key-value from the cache
					KosarSoloDriver.getLockManager().lock(key);
					com.mitrallc.sql.ResultSet rs = KosarSoloDriver.Kache.GetQueryResult(key);

					//If the asking client sends the command to steal the 
					if(command == Constants.CMD_STEAL) {
						if (rs != null)
							KosarSoloDriver.Kache.DeleteDust(key);						
						KosarSoloDriver.stealCount.incrementAndGet();						
					} else if(command == Constants.CMD_COPY) {
						KosarSoloDriver.copyCount.incrementAndGet();
					} else if (command == Constants.CMD_USEONCE) {
						KosarSoloDriver.useonceCount.incrementAndGet();
					}					
					KosarSoloDriver.getLockManager().unlock(key);

					if (rs == null) {	// value does not exist
						bb.putInt(Constants.REP_NOVAL);
						socket.write(bb.array());
						socket.flush();
						bb.clear();

						break;
					}					

					bb.putInt(Constants.REP_OK);
					baos.write(bb.array());
					bb.clear();

					byte[] byteVal = rs.serialize();

					bb.putInt(byteVal.length);
					baos.write(bb.array());
					bb.clear();

					baos.write(byteVal);
					baos.flush();

					socket.writeBytes(baos.toByteArray());
					socket.flush();		

					baos.reset();
					break;
				default:
					System.out.println("Unknown command: cmd_id="+command);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (socket != null) {
				try {
					socket.trueClose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}				
			}
		}
	}

	public void close() {
		try {
			socket.closeAll();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}

		this.interrupt();
	}
}
