package com.mitrallc.control;

/**
 * This thread is responsible for sending a ping message to the coordinator
 * (when in multi-node mode) at regular intervals, so that the coordinator 
 * knows it is alive and connected.
 * 
 * The interval duaration can be changed in com.mitrallc.common.Constants.java
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;

public class PingThread extends Thread {
	private SockIO socket = null;
	private volatile boolean stopThread = false; 
	private volatile boolean running;
	private int coreIndex = -1;

	public PingThread(int coreIndex, SockIO socket) {
		setDaemon(true);
		
		this.coreIndex = coreIndex;
		this.socket = socket;
	}

	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Sets stop variable
	 */
	public void stopThread() {
		this.stopThread = true;
		this.interrupt();
	}

	@Override
	public void run() {
		this.running = true;

		while (!this.stopThread) {
			try {
				// Create and send register message
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(Constants.CLIENT_PING);
				baos.write(bb.array());
				bb.clear();
				bb.putLong(KosarSoloDriver.clientData.getID());
				baos.write(bb.array());						// add client id.		
				bb.clear();		
				socket.write(baos.toByteArray());
				socket.flush();
				
				byte[] reply = socket.readBytes();
				
				System.out.println(ByteBuffer.wrap(Arrays.copyOfRange(reply, 0,  reply.length)).getInt());

				// Sleep
				Thread.sleep(Constants.delta - Constants.epsilon);
			} catch (InterruptedException ie) {
				// do nothing
			} catch (Exception e) {
//				KosarSoloDriver
//						.startReconnectThread(System.currentTimeMillis());
				e.printStackTrace(System.out);
			}
		}
		
		// finally, checkin the socket
		try {
			KosarSoloDriver.getConnectionPool(coreIndex).checkIn(socket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
		
		this.running = false;
		this.stopThread = false;
	}

	public SockIO getSocket() {
		return socket;
	}
}
