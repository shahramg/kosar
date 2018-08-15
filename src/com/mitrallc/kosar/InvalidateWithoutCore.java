package com.mitrallc.kosar;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;

public class InvalidateWithoutCore implements Runnable {

	private Socket sock;
	private Kosar cachemgr;

	private boolean verbose = false;

	public InvalidateWithoutCore(Socket sock, Kosar cachemgr) {
		this.sock = sock;
		this.cachemgr = cachemgr;			
	}

	@Override
	public void run() {
		DataInputStream inStream = null;
		DataOutputStream outStream = null;

		byte[] byte_array;
		String datazone = null;
		String keylist;
		int datazoneSize = -1;
		int keylistSize = -1;
		int response = 0;		

		try {
			inStream = new DataInputStream(new BufferedInputStream(
					sock.getInputStream()));
			outStream = new DataOutputStream(
					new BufferedOutputStream(sock.getOutputStream()));

			short command = inStream.readShort();
			if (command == 1111) {
				System.out.println("flag change");
				datazoneSize = inStream.readInt();
				keylistSize = inStream.readInt();
				if (datazoneSize > 0) {
					byte_array = new byte[datazoneSize];
					inStream.readFully(byte_array);
					datazone = new String(byte_array);
				}
				if (keylistSize > 0) {
					byte_array = new byte[keylistSize];
					inStream.readFully(byte_array);
					keylist = new String(byte_array);
					if (verbose)	System.out.println(keylist);
				}
			} else {
				if(verbose)System.out.println("noflag change");
				datazoneSize = inStream.readInt();
				keylistSize = inStream.readInt();
				if (verbose) System.out.println("datazoneSize="+datazoneSize+", keylistSize="+keylistSize);
				if (datazoneSize >= 1000 || keylistSize >= 100000){
					System.out.println("Error in TriggerListener:  Payload is corrupt.  Skipping the rest");
					System.out.println("datazoneSize="+datazoneSize+", keylistSize="+keylistSize);
					System.out.println("Limits are 1000 for datazoneSize and 100,000 for keylistSize (all numbers in bytes)");
					response = 1;
				} else {
					if (datazoneSize > 0) {
						byte_array = new byte[datazoneSize];
						inStream.readFully(byte_array);
						datazone = new String(byte_array);
					}
					if (verbose && datazone != null) System.out.println("datazone "+datazone);

					if (keylistSize > 0) {
						byte_array = new byte[keylistSize];
						inStream.readFully(byte_array);
						/* When in single node mode,
						 * The following step adds this key set to the Client's KeyQueue.
						 * This will be used later to implement the double delete operation
						 * to eliminate staleness because of a R-W race condition.
						 * 
						 */
						if(KosarSoloDriver.kosarEnabled) {
							Constants.KEY_QUEUE_WRITE_LOCK.lock();
							KosarSoloDriver.keyQueue.add(Constants.AI.incrementAndGet(), byte_array);
							if (verbose){
								keylist = new String(byte_array);
								String[] its = keylist.trim().split(" ");
								System.out.print("Delete: ");
								for (int s1=0; s1<its.length; s1++)
									System.out.println(""+its[s1]+ " ");
								System.out.println("");
							}
							Constants.KEY_QUEUE_WRITE_LOCK.unlock();
						}

						keylist = new String(byte_array);
						String[] its = keylist.trim().split(" ");
						HashMap<String, String> H = new HashMap<String, String>();
						if (verbose)
							System.out
							.println("KosarSolo Driver TriggerListener:  keylist "
									+ keylist);
						for (int s1 = 0; s1 < its.length; s1++)
							if (cachemgr != null && its[s1].length() > 0) {
								if (H.get(its[s1])==null){
									if (KosarSoloDriver.webServer != null)
										KosarSoloDriver.KosarInvalKeysAttemptedEventMonitor.newEvent(1);
									cachemgr.DeleteIT(its[s1]);
									H.put(its[s1], "1");
								}
							}

						H.clear();
						H = null;
					}
				}
			}
			if (verbose) System.out.println("TriggerListener, response is "+response);
			this.response(outStream, response);

			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} finally {				
			if (outStream != null) {
				try {
					outStream.flush();
					outStream.close();
					outStream = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(System.out);
				}
			}
			if (inStream != null) {
				try {
					inStream.close();
					inStream = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(System.out);
				}
			}	

			if (sock != null) {
				try {
					sock.close();
					sock = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(System.out);
				}
			}					
		}

		//			KosarListener.NumThreads.decrementAndGet();
	}

	public void response(DataOutputStream outStream, int responseCode) throws IOException
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(responseCode);
		//			outStream.writeInt(responseCode);
		outStream.write(bb.array());
		bb.clear();
		bb = null;
	}	
}
