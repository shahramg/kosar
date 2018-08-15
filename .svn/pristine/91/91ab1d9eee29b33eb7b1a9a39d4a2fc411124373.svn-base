package com.mitrallc.communication;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.ResultSet;
import com.mitrallc.sql.SockIO;
import com.mitrallc.sql.SockIOPool;

public class ClientConnector extends Thread{
	int serverPort;
	ServerSocket ss;
	public ClientConnector(int port) {
		this.serverPort = port;
	}
	
	public void run() {		
		Socket s = null;
		try {
			ss = new ServerSocket(serverPort);
			while(true) {
				s = ss.accept();
//				ClientHandler ch = new ClientHandler(s);
//				ch.start();
			}
		} catch (IOException ioex) {
			ioex.printStackTrace(System.out);
		} finally {
			try {
				if (null != ss && !ss.isClosed())
					ss.close();
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}
		}
	}
	
	public static ResultSet copyValue(long fromClientId, int action,
			String addr, String query, byte[] unmarshallBuffer, ByteBuffer buffer) {
		ResultSet res = null;		
		if (fromClientId == KosarSoloDriver.clientData.getID()) {
//			KosarSoloDriver.getLockManager().lock(query);
			res = KosarSoloDriver.Kache.GetQueryResult(query);
//			KosarSoloDriver.getLockManager().unlock(query);			
			return res;
		}
		
		try {
			SockIOPool pool = KosarSoloDriver.clientPoolMap.get(addr);
			
			// initialize the connection pool for this client 
			// if such a pool does not exist
			if (pool == null) {
				synchronized (KosarSoloDriver.clientPoolMap) {
					pool = KosarSoloDriver.clientPoolMap.get(addr);
					if (pool == null) {
						System.out.println("Client connection pool not initialized.");
						pool = new SockIOPool();					
						pool.setServer(addr);
						pool.setInitConn(KosarSoloDriver.init_connections);
						pool.initialize();		
						KosarSoloDriver.clientPoolMap.put(addr, pool);
					}
				}
			}
			
			SockIO socketIO = pool.getConnection();
//			buffer = ByteBuffer.allocate(8 + query.getBytes().length);
			buffer.position(0);
			buffer.putInt(action);
			buffer.putInt(query.getBytes().length);
			buffer.put(query.getBytes());
			
			buffer.position(0);
			buffer.limit(8 + query.getBytes().length);
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			
			socketIO.writeBytes(bytes);
			
			buffer.clear();

			ByteArrayInputStream bis = new ByteArrayInputStream(socketIO.readBytes());
			DataInputStream in = new DataInputStream(bis);
			int commandId = in.readInt();
			if (Constants.REP_OK == commandId) {
				int len = in.readInt();
				res = new ResultSet();
				res.deserialize(in, unmarshallBuffer);
			}
			in.close();
			bis.close();		
			
			pool.checkIn(socketIO);
		} catch (ConnectException e) {
			e.printStackTrace(System.out);
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
		return res;
	}	
}
