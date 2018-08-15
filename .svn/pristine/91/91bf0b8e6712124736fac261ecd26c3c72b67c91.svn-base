package com.mitrallc.UnitTest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.mitrallc.common.Constants;
import com.mitrallc.communication.CoreConnector;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool;
import com.mitrallc.sql.SockIO;

public class KosarCommunicationUnitTest {
	public static HashMap<String, Vector<String>> keyTokenMap = 
			new HashMap<String, Vector<String>>();
	public static HashMap<String, Vector<String>> tokenKeyMap =
			new HashMap<String, Vector<String>>();
	public static CoreConnector coreConnector = new CoreConnector();

	public static final String SERVER_IP = "10.0.0.220";
	public static final int port = 8888;

//	public static final String CONNECTION_STRING = "jdbc:oracle:thin:@10.0.0.215:1521:orcl";
	public static final String CONNECTION_STRING = "kosarsolo:jdbc:oracle:thin:@//10.0.0.215:1521/ORCL";
	public static final String USERNAME = "hieunguyen";
	public static final String PASSWORD = "hieunguyen123";

	private static SockIOPool sockPool = new SockIOPool();

	// an in-memory key-value storage for testing
	public static HashMap<String, String> KVS = new HashMap<String, String>();

	static Connection conn = null;
	static Statement st = null;
	private static PreparedStatement preparedStatement;

//	static int trans_id = 0;

	private static boolean verbose = true;

//	public static int genTransId() {
//		return ++trans_id;
//	}

	public static void main(String[] args) throws InterruptedException, IOException, SQLException, ClassNotFoundException {
		// start the trigger listener
//		TrigListener trigListener = new KosarCommunicationUnitTest().
//				new TrigListener(4004, tokenKeyMap, KVS);
//		trigListener.start();

		genKeys();

//		// connect with the Core
		sockPool.setServer(SERVER_IP+":"+port);
		sockPool.setInitConn(10);
		sockPool.initialize();		

////		Class.forName ("oracle.jdbc.driver.OracleDriver");
		Class.forName("com.mitrallc.sql.KosarSoloDriver");
		conn = DriverManager.getConnection(CONNECTION_STRING,USERNAME,PASSWORD);
		
		st = conn.createStatement();
//
//		// register packages, stored procedure and triggers with the RDBMS
//		registerDB();
//		genSampleTriggers(st);
//
//		// register with the core
//		register();
		
		HashMap<String, ByteIterator> result = new HashMap<String, ByteIterator>();
		Vector<HashMap<String, ByteIterator>> vecRes = new Vector<HashMap<String,ByteIterator>>();
		for (int i = 0; i < 100; i++) {
			getUserProfile(i, i, result, false, false);
			viewPendingRequests(i, vecRes, false, false, false);
			getListOfFriends(i, i, null, vecRes, false, false, false);
		}

		for (int i = 0; i < 100; i++ ) {
			doReadActs();
			inviteFriend(1, 20);

			doReadActs();		
			rejectFriend(1,20);

			doReadActs();		
			inviteFriend(1, 20);

			doReadActs();
			acceptFriend(1,20);

			doReadActs();
			thawFriendship(1,20);
		}

		closeConnection();
	}

	private static void closeConnection() {
		try
		{
			if( st != null )
			{
				st.close();
			}
			if( conn != null )
			{
				conn.close();				
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}			
	}

	private static void doReadActs() throws IOException {
		Object[] keys = keyTokenMap.keySet().toArray();

		for (Object key : keys) {			
			doRead((String)key, keyTokenMap.get(key));
		}

//		assert(KVS.get("key1").equals("1"));
//		assert(KVS.get("key2").equals("1"));
//		assert(KVS.get("key3").equals("1"));
	}

	private static void thawFriendship(int inviterId, int inviteeId) throws SQLException, IOException {
		String dml = "DELETE FROM friendship WHERE (inviterid="+inviterId+" and inviteeid= "+inviteeId+") OR (inviterid="+inviterId+" and inviteeid= "+inviteeId+") and status=2";

		int ret = executeUpdate(dml);
		if (ret == 0) {
			if (verbose) System.out.println("ThawFriendship "+inviterId+","+inviteeId+" Success.");

//			assert(KVS.get("key1") == null);
//			assert(KVS.get("key3") == null);
//			assert(KVS.get("key2").equals("1"));
		} else {
			if (verbose) System.out.println("ThawFriendship "+inviterId+","+inviteeId+" Fail.");
		}	
	}

	private static void acceptFriend(int inviterId, int inviteeId) throws SQLException, IOException {
		String dml = "UPDATE friendship SET status = 2 WHERE inviterid="+inviterId+" and inviteeid= "+inviteeId;

		int ret = executeUpdate(dml);
		if (ret == 0) {
			if (verbose) System.out.println("AcceptFriend "+inviterId+","+inviteeId+" Success.");

//			assert(KVS.get("key1") == null);
//			assert(KVS.get("key3") == null);
//			assert(KVS.get("key2").equals("1"));
		} else {
			if (verbose) System.out.println("AcceptFriend "+inviterId+","+inviteeId+" Fail.");
		}	
	}

	private static void rejectFriend(int inviterId, int inviteeId) throws SQLException, IOException {
		String dml = "DELETE FROM friendship WHERE inviterid="+inviterId+" and inviteeid="+inviteeId+" and status=1";

		int ret = executeUpdate(dml);
		if (ret == 0) {
			if (verbose) System.out.println("RejectFriend "+inviterId+","+inviteeId+" Success.");

//			assert(KVS.get("key1") == null);
//			assert(KVS.get("key3") == null);
//			assert(KVS.get("key2").equals("1"));
		} else {
			if (verbose) System.out.println("RejectFriend "+inviterId+","+inviteeId+" Fail.");
		}	
	}

	private static void inviteFriend(int inviterId, int inviteeId) throws SQLException, IOException {
		String dml = "insert into friendship(inviterId,inviteeId,status) values ("+inviterId+","+inviteeId+",1)";

		int ret = executeUpdate(dml);

		if (ret == 0) {
			if (verbose) System.out.println("InviteFriend "+inviterId+","+inviteeId+" Success.");

//			assert(KVS.get("key1") == null);
//			assert(KVS.get("key3") == null);
//			assert(KVS.get("key2").equals("1"));
		} else {
			if (verbose) System.out.println("InviteFriend "+inviterId+","+inviteeId+" Fail.");
		}
	}
	
	public static int getUserProfile(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		//if (true) return 0;
		//Statement st = null;
		ResultSet rs = null;
		int retVal = 0;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query="";


		try {
			//friend count\query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,tpic FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
			
			//query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE inviteeid<50 and status = 1 and inviterid = ?";
			//query = "select count(*) as col_0_0_ from HIBERNATE_USER user0_";
			//query = "select userdetail0_.userId as userId1_0_0_, userdetail0_.userName as userName2_0_0_ from UserDetails userdetail0_ where userdetail0_.userId=?";

			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, profileOwnerID);
			rs = preparedStatement.executeQuery();
//			if (st == null) {
//				System.out.println("Error, st is null");
//			} else rs = st.executeQuery(query);

			if (rs.next())
				System.out.println("Retrieved userid="+rs.getInt(1)+", name="+rs.getString(4)) ;
			else
				System.out.println("No qualifying row.");


		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}
		return retVal;
	}
	
	public static int viewPendingRequests(int profileOwnerID,
			Vector<HashMap<String, ByteIterator>> result, boolean friendListReq, boolean insertImage, boolean testMode) {

		int retVal = 0;
		ResultSet rs = null;
		if(profileOwnerID < 0)
			return -1;

		String query = "";
		if(insertImage)
			query = "SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,tpic FROM users, friendship WHERE inviteeid=? and status = 1 and inviterid = userid";
		else 
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE inviteeid=? and status = 1 and inviterid = userid";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			rs = preparedStatement.executeQuery();
			int cnt=0;
			while (rs.next()){
				cnt++;
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				//get the number of columns and their names
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value = "";
					if(col_name.equalsIgnoreCase("tpic")){
						// Get as a BLOB
						Blob aBlob = rs.getBlob(col_name);
						byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
						value = allBytesInBlob.toString();
						if(testMode){
							//dump to file
							try{
								FileOutputStream fos = new FileOutputStream(profileOwnerID+"-"+cnt+"-thumbimage.bmp");
								fos.write(allBytesInBlob);
								fos.close();
							}catch(Exception ex){
							}

						}
					}else
						value = rs.getString(col_name);

					//values.put(col_name, new StringByteIterator(value));
				}
				result.add(values);
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}

	public static int getListOfFriends(int requesterID, int profileOwnerID,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean friendListReq, boolean insertImage, boolean testMode) {

		int retVal = 0;
		ResultSet rs = null;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query ="";
		if(insertImage)
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,tpic FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
		else
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, profileOwnerID);
			rs = preparedStatement.executeQuery();
			int cnt =0;
			while (rs.next()){
				cnt++;
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				if (fields != null) {
					for (String field : fields) {
						String value = rs.getString(field);
						//						values.put(field, new StringByteIterator(value));
					}
					result.add(values);
				}else{
					//get the number of columns and their names
					//Statement st = conn.createStatement();
					//ResultSet rst = st.executeQuery("SELECT * FROM users");
					ResultSetMetaData md = rs.getMetaData();
					int col = md.getColumnCount();
					for (int i = 1; i <= col; i++){
						String col_name = md.getColumnName(i);
						String value="";
						if(col_name.equalsIgnoreCase("tpic")){
							// Get as a BLOB
							Blob aBlob = rs.getBlob(col_name);
							byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
							value = allBytesInBlob.toString();
							if(testMode){
								//dump to file
								try{
									FileOutputStream fos = new FileOutputStream(profileOwnerID+"-"+cnt+"-thumbimage.bmp");
									fos.write(allBytesInBlob);
									fos.close();
								}catch(Exception ex){
								}
							}
						}else
							value = rs.getString(col_name);


						//						values.put(col_name, new StringByteIterator(value));
					}
					result.add(values);
				}
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}


	private static void registerDB() {
		try {
			st = conn.createStatement();

			// create the package
//			regthread.OracleRegisterPackages(conn);

			// create the stored procedure
//			regthread.OracleRegisterProcs(conn);

			// create the functions
//			regthread.OracleRegisterFunctions(conn);

			// create the triggers
			genSampleTriggers(st);
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}		
	}

	private static void register() throws InterruptedException, IOException {
		// keep trying until the message is received
		while (true) {
			SockIO socket = sockPool.getConnection();
			if (socket == null) {
				Thread.sleep(50);
				continue;
			}
			
			long clientId = KosarSoloDriver.genID(port);

			byte[] reply = coreConnector.register(socket, clientId, 4004);

			// the reply structure
			// CLIENT_REGISTER client_id lease_term
			int response = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();

			if(response == Constants.CLIENT_REGISTER) {				
				KosarSoloDriver.clientData.setID(ByteBuffer.wrap(Arrays.copyOfRange(reply, 4, 8)).getInt());
//				KosarSoloDriver.clientData.setLeaseTerm(ByteBuffer.wrap(Arrays.copyOfRange(reply, 8, 12)).getInt());

				if (verbose) {
					System.out.println("Connected to server " + SERVER_IP + ". "
							+ "ClientId=" + clientId);
				}
				break;
			} else {
				if (verbose)
					System.out.println("Error connect to server. Retry...");

				Thread.sleep(50);				
			}

			sockPool.checkIn(socket);
		}
	}

	// do read acts to register the map key-token to the Core
	private static void doRead(String key, Vector<String> tokens) throws IOException {
		int leaseNumber = -1;		

		SockIO socket = sockPool.getConnection();
		if (socket == null) {
			return;
		}

		while (true) {
			Set<String> tks = new HashSet<String>();
			tks.addAll(tokens);
			byte[] reply = coreConnector.acquireILease(socket, key, tks, null);
	
			int response = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();
	
			if (response == Constants.REPLY_I_LEASE_GRANTED) {
				leaseNumber = ByteBuffer.wrap(Arrays.copyOfRange(reply, 4, 8)).getInt();
				if (verbose) System.out.println("I lease granted. Lease number = "+leaseNumber);
	
				KVS.put(key, "1");
	
				// release the lease
				coreConnector.releaseILease(Constants.CLIENT_RELEASE_I_LEASE, socket, key, leaseNumber, tks, -1);
	
				if (response == Constants.REPLY_LEASE_RELEASE) {
					if (verbose) System.out.println("I lease released.");					
				} else if (response == Constants.REPLY_LEASE_ABORTED) { 
					if (verbose) System.out.println("I lease revoked.");
				} else {
					if (verbose) System.out.println("Server returns incorrect value.");					
				}
				
				break;
			} else if (response == Constants.REPLY_I_LEASE_RETRY){
				if (verbose) System.out.println("I lease rejected.");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			} else {
				if (verbose) System.out.println("Server returns incorrect value.");
				System.exit(-1);
			}
		}

		sockPool.checkIn(socket);		
	}

	private static int executeUpdate(String dml) {
//		int transId = genTransId();
		
		int ret = 0;
		
		try {
			st = conn.createStatement();
			st.executeUpdate(dml);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = -1;
		}
		
		
		return ret;

//		java.sql.CallableStatement st = conn.prepareCall("{ call do_dml(?,?,?,?) }");
//		st.setString(1, dml);
//		st.setInt(2, ByteBuffer.wrap(KosarSoloDriver.clientData.getID()).getInt());
////		st.setInt(3, transId);
//		st.registerOutParameter(4, Types.INTEGER);

//		st.execute();

//		int ret = st.getInt(4);

//		if (ret != 0) {
//			// release the Q lease
//			if (verbose) System.out.println("Trans aborts: "+transId);
//			abortTrans(transId);
//		} else {		
//			// release the Q lease
//			if (verbose) System.out.println("Trans commits: "+transId);
//			ret = commitTrans(transId);
//		}

//		return ret;
	}

	private static int abortTrans(int transId) throws IOException {
		int ret = 0;
		SockIO socket = sockPool.getConnection();
		if (socket == null) {
			return -1;
		}

		byte[] reply = coreConnector.abortTrans(socket, transId);
		ret = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();		

		sockPool.checkIn(socket);

		return ret;
	}

	private static int commitTrans(int transId) throws IOException {
		int ret = 0;
		SockIO socket = sockPool.getConnection();
		if (socket == null) {
			return -1;
		}

		byte[] reply = coreConnector.commitTrans(socket, transId);
		ret = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();		

		sockPool.checkIn(socket);

		return ret;
	}

	public static void genSampleTriggers(Statement st) throws SQLException {
		// drop all others triggers if existed to prevent confusing the test
		ResultSet rs = st.executeQuery("select OBJECT_NAME, OWNER from SYS.ALL_OBJECTS where UPPER(OBJECT_TYPE) = 'TRIGGER' and OWNER='HIEUNGUYEN' and OBJECT_NAME like 'USERS%' order by OBJECT_NAME");
		if (rs != null) {
			while (rs.next()) {
				String trigger = rs.getString(1);
				st.execute("DROP TRIGGER IF EXISTS "+trigger);
			}
		}

		st.executeQuery("select OBJECT_NAME, OWNER from SYS.ALL_OBJECTS where UPPER(OBJECT_TYPE) = 'TRIGGER' and OWNER='HIEUNGUYEN' and OBJECT_NAME like 'FRIENDSHIP%' order by OBJECT_NAME");
		if (rs != null) {
			while (rs.next()) {
				String trigger = rs.getString(1);
				st.execute("DROP TRIGGER IF EXISTS "+trigger);
			}
		}		

		// create three sample triggers on the FRIENDSHIP table
		st.execute("create or replace TRIGGER  FRIENDSHIP_DEL BEFORE DELETE ON FRIENDSHIP FOR EACH ROW "
				+ "DECLARE DELETEIT_DLL_VAL BINARY_INTEGER; DELETEKEY CLOB; "
				+ "BEGIN DELETEKEY := 'token1 token2 token3'; "
				+ "IF (DELETEKEY != ' ') "
				+ "THEN DeleteIT_DLL_Val := KOSARServerTrigDelMulti('10.0.0.220:8888', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");
		st.execute("create or replace TRIGGER  FRIENDSHIP_INS BEFORE INSERT ON FRIENDSHIP FOR EACH ROW "
				+ "DECLARE DELETEIT_DLL_VAL BINARY_INTEGER; DELETEKEY CLOB; "
				+ "BEGIN DELETEKEY := 'token1 token2 token3'; "
				+ "IF (DELETEKEY != ' ') "
				+ "THEN  DeleteIT_DLL_Val := KOSARServerTrigDelMulti('10.0.0.220:8888', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");
		st.execute("create or replace TRIGGER  FRIENDSHIP_UPT "
				+ "BEFORE UPDATE ON FRIENDSHIP FOR EACH ROW "
				+ "DECLARE DELETEIT_DLL_VAL BINARY_INTEGER; DELETEKEY CLOB; "
				+ "BEGIN DELETEKEY := 'token1 token2 token3 '; "
				+ "IF (DELETEKEY != ' ') THEN  "
				+ "DeleteIT_DLL_Val := KOSARServerTrigDelMulti('10.0.0.220:8888', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN RAISE_APPLICATION_ERROR( -20001, 'Failed to connect to KOSAR KVS CORE.'); END IF; END IF; END;");		
	}

	public static void genKeys() {
		keyTokenMap.put("key1", new Vector<String>(Arrays.asList(
				new String[] {"token1", "token2", "token3"})));
		keyTokenMap.put("key2", new Vector<String>(Arrays.asList(
				new String[] {"token4", "token5", "token6"})));
		keyTokenMap.put("key3", new Vector<String>(Arrays.asList(
				new String[] {"token1", "token4", "token3"})));

		tokenKeyMap.put("token1", new Vector<String>(Arrays.asList(
				new String[] {"key1", "key3"})));
		tokenKeyMap.put("token2", new Vector<String>(Arrays.asList(
				new String[] {"key1"})));
		tokenKeyMap.put("token3", new Vector<String>(Arrays.asList(
				new String[] {"key1", "key3"})));
		tokenKeyMap.put("token4", new Vector<String>(Arrays.asList(
				new String[] {"key2", "key3"})));
		tokenKeyMap.put("token5", new Vector<String>(Arrays.asList(
				new String[] {"key2"})));
		tokenKeyMap.put("token6", new Vector<String>(Arrays.asList(
				new String[] {"key2"})));
	}

	class TrigListener extends Thread {
		ServerSocket serverSock = null;

		HashMap<String, Vector<String>> tokenKeyMap;
		HashMap<String, String> KVS;

		static final int MAX_CONN = 10;

		public TrigListener(int port, 
				HashMap<String, Vector<String>> tokenKeyMap,
				HashMap<String, String> KVS) {
			this.tokenKeyMap = tokenKeyMap;
			this.KVS = KVS;

			try {
				serverSock = new ServerSocket(port);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void run() {
			Socket client = null;
			int i = 0;
			for (;;) {
				if (i < MAX_CONN) {
					i++;

					//If there is no core or it's the first time, then connect every iteration.
					//Otherwise, the loop should go right to command = inStream.readShort()
					try {
						client = serverSock.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					InvalidationThread invThread = new InvalidationThread(client, tokenKeyMap, KVS, verbose);
					invThread.start();		
				} else {
					break;
				}
			}			
		}
	}
}