package com.mitrallc.UnitTest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

public class InvalidationThread extends Thread {
	static final int CMD_INVALIDATE = 3;

	Socket sock;
	DataInputStream inStream = null;
	DataOutputStream outStream = null;
	int response = 0;	

	HashMap<String, Vector<String>> tokenKeyMap;
	HashMap<String, String> KVS;

	boolean verbose = true;

	public InvalidationThread(Socket sock, 
			HashMap<String, Vector<String>> tokenKeyMap,
			HashMap<String, String> KVS, boolean verbose) {
		this.sock = sock;
		this.tokenKeyMap = tokenKeyMap;
		this.KVS = KVS;

		this.verbose = verbose;
	}	

	@Override
	public void run() {
		try {
			inStream = new DataInputStream(new BufferedInputStream(
					sock.getInputStream()));
			outStream = new DataOutputStream(new BufferedOutputStream(
					sock.getOutputStream()));

			while (true) {
				int len = inStream.readInt();
				byte[] b = new byte[len];

				inStream.read(b, 0, len);

				int command = ByteBuffer.wrap(Arrays.copyOfRange(b, 0, 4)).getInt();

				if (command != CMD_INVALIDATE) {
					System.out.println("Not an invalidate message.");
				} else {
					int size = ByteBuffer.wrap(Arrays.copyOfRange(b, 4, 8)).getInt();
					int offset = 8;
					for (int i = 0; i < size; i++) {
						int ikLen = ByteBuffer.wrap(Arrays.copyOfRange(b, offset, offset+4)).getInt();
						offset+=4;
						String ik = new String(ByteBuffer.wrap(Arrays.copyOfRange(b, offset, offset+ikLen)).array());
						offset+=ikLen;

						Vector<String> keys = tokenKeyMap.get(ik);
						for (String key: keys) {
							KVS.remove(key);
						}
					}
				}

				if (verbose) System.out.println("Received command: "+command);

				if (verbose) System.out.println("TriggerListener, response is "+response);						
				try {
					this.response(outStream, response);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}

			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (outStream != null) {
				try {
					outStream.flush();
					outStream.close();
					outStream = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}

			}
			if (inStream != null) {
				try {
					inStream.close();
					inStream = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}

			}				
		}
	}

	public void response(DataOutputStream outStream, int responseCode) throws IOException
	{
		outStream.writeInt(4);
		outStream.writeInt(responseCode);
		outStream.flush();
	}
}
