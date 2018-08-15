package com.mitrallc.UnitTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

//import com.mitrallc.sql.SockIOPool;

public class TestLatencyCopyValue {
//	public static final String SERVER_IP = "10.0.0.220";
//	public static final int port = 8888;

	public static final String CONNECTION_STRING = "kosarsolo:jdbc:oracle:thin:@//10.0.0.195:1521/ORCL";
	public static final String USERNAME = "hieunguyen";
	public static final String PASSWORD = "hieunguyen123";
	
//	private static SockIOPool sockPool = new SockIOPool();

	public static HashMap<String, String> KVS = new HashMap<String, String>();

	static Connection conn = null;
	static Statement st = null;
	private static PreparedStatement preparedStatement;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
//		sockPool.setServer(SERVER_IP+":"+port);
//		sockPool.setInitConn(10);
//		sockPool.initialize();		

		Class.forName("com.mitrallc.sql.KosarSoloDriver");
		conn = DriverManager.getConnection(CONNECTION_STRING,USERNAME,PASSWORD);
		
		preparedStatement = conn.prepareStatement("SELECT * FROM USERTABLE WHERE YCSB_KEY=?");
		
		preparedStatement.setInt(1, 5);
		
		int loop = 1000;
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < loop; i++) {
			preparedStatement.executeQuery();
		}
		long total = System.currentTimeMillis() - start;
		
		System.out.println("Average Time: "+total/(double)loop);
	}
}
