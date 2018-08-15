package com.mitrallc.sql;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SockIOPool {
	public Semaphore poolSemaphore;
	public ConcurrentLinkedQueue<SockIO> availPool;
	private int initConn = 10;
	private String server;
	
//	private AtomicBoolean initialized = new AtomicBoolean(false);

	public SockIOPool(String server, int con) {
		this.server = server;
		initConn = con;

		initialize();
	}
	
	public SockIOPool() {
		availPool = null;
		poolSemaphore = null;
	}

//	public boolean isInitialized() {
//		return initialized.get();
//	}
	
	public void setServer(String s) {
		server = s;
	}
	
	public void setInitConn(int i) {
		initConn = i;
	}
	
	public void initialize() {
		if (null == availPool)
			availPool = new ConcurrentLinkedQueue<SockIO>();		
		
		for (int j = 0; j < initConn; j++) {
			SockIO socket = null;
			while (socket == null) {
				try { 
					socket = createSocket(server);
				}  catch (Exception e) {
					System.out.println(String.format("ip %s %s", this.server, e.getMessage()));
				}
			}
			this.availPool.add(socket);
		}		
		
		poolSemaphore = new Semaphore(initConn);
//		initialized.getAndSet(true);
	}
	
	public static SockIO createSocket(String host) throws ConnectException {
		SockIO socket = null;
		try {
			String[] ip = host.split(":");
			Socket so = new Socket(ip[0], Integer.parseInt(ip[1]));
			socket = new SockIO(so);
		} catch (ConnectException c) {
			throw c;
		} catch (Exception ex) {
			socket = null;
		}
		return socket;
	}

	public void shutdownPool() {
		if (availPool.size() != initConn) {
			System.out
					.println("DB pool size not match the number of DB connections");
			System.exit(0);
		}
		
		for (SockIO sock : availPool) {
			try {
				sock.closeAll();
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}
		}
		availPool.clear();
		availPool = null;
		poolSemaphore = null;
	}

	public SockIO getConnection() {
//		// wait until the socket pool is initialized
//		while (!isInitialized()) {
//			try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace(System.out);
//			}
//		}
		
		try {
			poolSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		SockIO db = this.availPool.poll();
		return db;
	}

	public void checkIn(SockIO db) throws IOException {
		db.out.flush();
		this.availPool.add(db);
		poolSemaphore.release();
	}

}
