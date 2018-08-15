package com.mitrallc.mysqltrig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.mitrallc.common.Constants;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIO;
import com.mitrallc.sqltrig.*;

/**
 * @author Shahram Ghandeharizadeh, Sat, July 27, 2014 This class registers
 *         triggers with a target RDBMS A key data structure is a vector that
 *         contains the queries to be registered with the RDBMS.
 */

public class regthread extends Thread {
	int counter = 0;
	public static Connection db_conn = null;
	private static boolean verbose = false;
	private static MySQLQueryToTrigger MySQLqt = null;
	private static OracleQueryToTrigger ORCLqt = null;
	public boolean isRunning = false;

	private static String BeginErrMsgInit = "Error in rgthread initialization (started by KosarSolo):";

	public static Vector workitems = new Vector();

	public static Semaphore DoRegTrigg = new Semaphore(0, true);
	private static Semaphore RDBMScmds = new Semaphore(1, true);

	protected static ConcurrentHashMap<String, TableInfo> TableMetaData = new ConcurrentHashMap<String, TableInfo>();
	protected static ConcurrentHashMap<String, String> Procs = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, String> Packages = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, String> Libraries = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, String> Functions = new ConcurrentHashMap<String, String>();
	public static boolean isRegistered = false;

	private static String CurrentSQLCmd = null;

	public static boolean AddQry(String qry) {
		try {
			regthread.RDBMScmds.acquire();
			regthread.workitems.addElement(qry);
			regthread.RDBMScmds.release();
			Kosar.setIssuingTrigCMDS(true); // Stop query processing

			regthread.DoRegTrigg.release();

			// regthread to do its job
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
			Kosar.setIssuingTrigCMDS(false); // continue query processing
		}
		return false;
	}

	public static void getIT(String qry, StringBuffer COSARKey){
		Vector<String> trgrVector = new Vector<String>();
		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			MySQLqt.TQ(qry, trgrVector, COSARKey, db_conn, QueryToTrigger.OpType.GETKEY);
			break;
		case Oracle:
			ORCLqt.TQ(qry, trgrVector, COSARKey, db_conn, QueryToTrigger.OpType.GETKEY);
			break;
		default:
			System.out
			.println("Error, regthread:  target unknown system.  No trigger will be generated.");
			break;
		}
		return;
	}

	public static void BusyWaitForRegThread(String cmd) {
		while (Kosar.isIssuingTrigCMDS() == true) {
			if (CurrentSQLCmd != null && cmd.equals(CurrentSQLCmd))
				return;
			else
				try {
					//System.out.print("Sleep for 100 msec");
					sleep(100);
					//System.out.println("... Awakened!");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
		}
	}

	private static void updateWSmonitor(String cmd){
		if(KosarSoloDriver.webServer != null){
			if (cmd.contains("TRIGGER"))
				Kosar.KosarMonitor.IncrementNumTriggers();
		}
	}

	public int ExecuteCommand(Connection db_conn, String cmd) {
		Statement st = null;
		int retval = -1;

		if (verbose)
			System.out.println("Execute Command "+ cmd);

		if(KosarSoloDriver.getFlags().coordinatorExists()) {
			if(verbose)
				System.out.println("core exists." +
						"\nWriting to CORE " + cmd);
			//No need to transmit DROP Trigger commands to the KVSCORE
			//The KVSCORE will generate these on its own.
			if(cmd.contains("DROP TRIGGER")) {
				return 0;
			}

			for (String core: KosarSoloDriver.cores.keySet()) {
				SockIO socket = null;
				try {
					socket = KosarSoloDriver.getConnectionPool(KosarSoloDriver.cores.get(core)).getConnection();

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(7);
					baos.write(bb.array());
					bb.clear();
					baos.write(cmd.getBytes());
					baos.flush();
					socket.write(baos.toByteArray());
					socket.flush();

					retval = socket.readByte();
					baos.reset();
				} catch (IOException e) {
					e.printStackTrace(System.out);
				} finally {
					if (socket != null) {
						try {
							KosarSoloDriver.getConnectionPool(KosarSoloDriver.cores.get(core)).checkIn(socket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace(System.out);
						}
					}
				}
			}
		} else {
			try {
				st = db_conn.createStatement();
				CurrentSQLCmd = cmd; // Must specify in order to avoid a
				// deadlock with execute query
				// Open the gates temporarily to register the trigger
				st.executeUpdate(cmd);
				CurrentSQLCmd = null;
				retval = 0;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
				retval = -1;
			} finally {
				try {
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
			}
		}

		updateWSmonitor(cmd);

		return retval;
	}

	public static void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}

	public void MySQLRegisterExistingTriggers(Connection db_conn) {
		String triggername, tablename, triggertype, triggertiming, triggerstmt;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the triggers.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("show triggers");
			while (rs.next()) {
				triggername = rs.getString("Trigger");
				tablename = rs.getString("Table").toUpperCase();
				triggertype = rs.getString("Timing");
				triggerstmt = rs.getString("Statement");
				triggertiming = rs.getString("Event");

				if (verbose) {
					System.out.println("Trig Name=" + triggername);
					System.out.println("Table=" + tablename);
					System.out.println("Statement=" + triggerstmt);
					System.out.println("Timing=" + triggertype);
					System.out.println("Event=" + triggertiming);
				}
				String trigger = mysqlTrigGenerator.AssembleFromDBMS(
						triggername, tablename, triggertype, triggertiming,
						triggerstmt);

				if (verbose)
					System.out.println("Trigger " + trigger);
				TableInfo tbl = TableMetaData.get(tablename);
				if (tbl == null) {
					if (verbose)
						System.out
						.println("tbl entry does not exist; creating!");
					tbl = new TableInfo();
					tbl.setTableName(tablename); // add the table name for this
					// entry

					// Now, insert the tbl entry for future lookup
					TableMetaData.put(tablename, tbl);
				}
				tbl.addVRegTrig(trigger);
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLQueryToTrigger.MySQLPopulate Error: Failed to process: show triggers\n");
			e.printStackTrace(System.out);
		} finally {
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace(System.out);
				}

		}
	}

	public void MySQLRegisterExistingProcs(Connection db_conn) {
		String procname, procbody;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the procedures.");
			st = db_conn.createStatement();
			//dbname = db_conn.getCatalog();
			//System.out.println("dbname = "+dbname);

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("select db, name, body from mysql.proc where Db = DATABASE();");
			while (rs.next()) {
				procname = rs.getString("Name").trim();
				procbody = rs.getString("body").trim();

				if (verbose) {
					System.out.println("Proc Name=" + procname);
				}

				Procs.put(procname, procbody);
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLRegisterExistingProcs Error: Failed to process: show procedure status\n");
			e.printStackTrace(System.out);
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in MySQLRegisterExistingProcs method:  Failed to close Statement/ResultSet");
					e.printStackTrace(System.out);
				}
		}
	}

	public void MySQLPopulate(Connection db_conn) {
		MySQLRegisterExistingProcs(db_conn);
		MySQLRegisterExistingTriggers(db_conn);
	}

	public static void OracleRegisterTriggers(Connection db_conn) {
		String triggername, tablename, triggertype, triggertiming, triggerstmt;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the triggers.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("select * from USER_TRIGGERS");
			while (rs.next()) {
				triggername = rs.getString("TRIGGER_NAME");
				tablename = rs.getString("TABLE_NAME").toUpperCase();
				triggertype = rs.getString("TRIGGER_TYPE");
				triggerstmt = rs.getString("TRIGGER_BODY");
				triggertiming = rs.getString("TRIGGERING_EVENT");

				if (verbose) {
					System.out.println("Trig Name=" + triggername);
					System.out.println("Table=" + tablename);
					System.out.println("Statement=" + triggerstmt);
					System.out.println("Timing=" + triggertype);
					System.out.println("Event=" + triggertiming);
				}
				//Kosar Triggers do not have a SELECT statement
				//Do not incorporate these as they are specific to an application
				if ( !triggerstmt.toUpperCase().contains("SELECT") ) {
					String trigger = OracleTrigGenerator.AssembleFromDBMS(
							triggername, tablename, triggertype, triggertiming,
							triggerstmt);

					if (verbose)
						System.out.println("Trigger " + trigger);
					TableInfo tbl = TableMetaData.get(tablename);
					if (tbl == null) {
						if (verbose)
							System.out
							.println("tbl entry does not exist; creating!");
						tbl = new TableInfo();
						tbl.setTableName(tablename); // add the table name for this
						// entry

						// Now, insert the tbl entry for future lookup
						TableMetaData.put(tablename, tbl);
					}
					tbl.addVRegTrig(trigger);
				}
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLQueryToTrigger.MySQLPopulate Error: Failed to process: show triggers\n");
			e.printStackTrace(System.out);
		} finally {
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace(System.out);
				}

		}
	}

	public static void OracleRegisterProcs(Connection db_conn) {
		String procname;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out.println("regthread, querying oracle for the procedures.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("SELECT * FROM ALL_OBJECTS WHERE OBJECT_TYPE IN ('PROCEDURE')");
			while (rs.next()) {
				procname = rs.getString("OBJECT_NAME").trim();

				if (verbose) 
					System.out.println("Proc Name=" + procname);

				Procs.put(procname.toUpperCase(), "Exists");
			}

			// register the do_dml procedure
			String s = Procs.get("DO_DML".toUpperCase());
			if (s == null) {
				st.execute("create or replace procedure DO_DML( dml_string IN VARCHAR2, client_id IN VARCHAR, trans_id IN BINARY_INTEGER, ret_val OUT BINARY_INTEGER) as "+
						"begin "+
						"TRANS_ID_PKG.trans_id := trans_id; "+
						"TRANS_ID_PKG.client_id := client_id; "+
						"TRANS_ID_PKG.ret_val := 0; "+          
						"execute immediate 'BEGIN '|| dml_string || ';END;'; "+          
						"ret_val := TRANS_ID_PKG.ret_val; "+          
						"if (ret_val != 0) then "+
						"rollback; "+
						"end if; "+
						"end DO_DML;  ");
				Packages.putIfAbsent("DO_DML", "Exists");
			}
		} catch (SQLException e) {
			System.out
			.println("OracleQueryToTrigger.OraclePopulate Error: Failed to process SQL command to display existing procedures\n");
			e.printStackTrace(System.out);
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace(System.out);
				}
		}
	}

	public static void OracleRegisterPackages(Connection db_conn) {
		String pkgname;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("SELECT * FROM ALL_OBJECTS "
					+ "WHERE OBJECT_TYPE IN ('PACKAGE') AND upper(OBJECT_NAME)=upper('TRANS_ID_PKG')");
			while (rs.next()) {
				pkgname = rs.getString("OBJECT_NAME").trim();

				if (verbose) 
					System.out.println("Package Name=" + pkgname);

				Packages.put(pkgname.toUpperCase(), "Exists");			
			}

			// register the do_dml procedure
			String s = Packages.get("TRANS_ID_PKG".toUpperCase());
			if (s == null) {
				// create the package
				st.execute("create or replace package TRANS_ID_PKG is "+
						"trans_id BINARY_INTEGER; "+
						"ret_val BINARY_INTEGER; "+
						"client_id VARCHAR(100); "+
						"end TRANS_ID_PKG; "
						);
				Packages.putIfAbsent("TRANS_ID_PKG", "Exists");
			}
		} catch (SQLException e) {
			System.out
			.println("OracleQueryToTrigger.OraclePopulate Error: Failed to process SQL command to display existing packages\n");
			e.printStackTrace(System.out);
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
		}
	}

	public static void OracleRegisterFunctions(Connection db_conn) {
		Statement st = null;
		ResultSet rs = null;
		try {
			st = db_conn.createStatement();

			rs = st.executeQuery("SELECT * FROM ALL_OBJECTS "
					+ "WHERE OBJECT_TYPE IN ('LIBRARY') AND upper(OBJECT_NAME)=upper('COSAR_DLL_LIB')");

			String name = "";
			while (rs.next()) {
				name = rs.getString("OBJECT_NAME").trim();

				if (verbose) 
					System.out.println("Package Name=" + name);

				Libraries.put(name.toUpperCase(), "Exists");			
			}
			rs.close();

			String s = Packages.get("COSAR_DLL_LIB");

			if (s == null) {
				st.execute("CREATE OR REPLACE LIBRARY COSAR_DLL_LIB AS \'C:\\hieun\\COSARTriggerAPI.dll\';");
				Libraries.putIfAbsent("COSAR_DLL_LIB", "Exists");
			}

			rs = st.executeQuery("SELECT * FROM ALL_OBJECTS "
					+ "WHERE OBJECT_TYPE IN ('FUNCTION')");

			while (rs.next()) {
				name = rs.getString("OBJECT_NAME").trim();

				if (verbose) 
					System.out.println("Package Name=" + name);

				Functions.put(name.toUpperCase(), "Exists");			
			}

			s = null;
			s = Functions.get("KOSARTriggerDeleteMultiCall");

			if (s == null) {
				// this function is used for 'running KOSAR without the CORE' only
				st.execute(
						"CREATE OR REPLACE FUNCTION KOSARTriggerDeleteMultiCall  ( " +
								"          ipaddrs    IN VARCHAR2, " +
								"		   datazone   IN VARCHAR2, " +
								"		   cosar_keys  IN VARCHAR2, " +
								"		   flags	  IN BINARY_INTEGER ) "+
								"		RETURN BINARY_INTEGER " +
								"		AS LANGUAGE C "+
								"		   LIBRARY COSAR_DLL_LIB  "+
								"		   NAME \"KOSARTriggerDeleteMulti\"  "+
								"		   PARAMETERS ( "+				
								"             ipaddrs,  "+
								"             ipaddrs LENGTH, "+
								"		      datazone, "+
								"		      datazone LENGTH, "+						
								"		      cosar_keys, "+
								"		      cosar_keys LENGTH, "+
								"			  flags); "
						);
				Functions.putIfAbsent("KOSARTriggerDeleteMultiCall", "Exists");
			}

			s = null;
			s = Functions.get("KOSARServerTrigDelMultiClients");

			if (s == null) {
				// this function is used for 'KOSAR with the CORE'
				st.execute(
						"CREATE OR REPLACE FUNCTION KOSARServerTrigDelMultiClients  ( " +
								"ipaddr IN VARCHAR2, "+
								"datazone in VARCHAR2, "+
								"client_id IN VARCHAR, "+
								"trans_id IN BINARY_INTEGER, "+
								"cosar_keys  IN VARCHAR2, "
								+ "flags IN BINARY_INTEGER) " +							
								"		RETURN BINARY_INTEGER " +
								"		AS LANGUAGE C "+
								"		   LIBRARY COSAR_DLL_LIB  "+
								"		   NAME \"KOSARTrigDelMultiClient\"  "+
								"		   PARAMETERS ( "+
								"			  ipaddr,"+
								"			  ipaddr LENGTH,"+
								"			  datazone,"+
								"			  datazone LENGTH,"+
								"		      cosar_keys, "+
								"		      cosar_keys LENGTH, "+
								"			  trans_id, "+
								"			  client_id,"+
								"			  client_id LENGTH,"
								+ "flags); "							
						);
				Functions.putIfAbsent("KOSARServerTrigDelMultiClients", "Exists");
			}

			s = null;
			s = Functions.get("KOSARServerTrigDelMulti");

			if (s == null) {
				// the function that the trigger would call
				st.execute("create or replace function KOSARServerTrigDelMulti(ipaddr IN VARCHAR2, datazone IN VARCHAR2, cosar_keys  IN VARCHAR2, flags IN BINARY_INTEGER) return binary_integer "+ 
						"is trans_id BINARY_INTEGER; client_id VARCHAR(100); "+
						"begin "+
						"trans_id := TRANS_ID_PKG.trans_id; "+
						"client_id := TRANS_ID_PKG.client_id; "+

					  "TRANS_ID_PKG.ret_val := KOSARServerTrigDelMultiClients(ipaddr, datazone, client_id, trans_id, cosar_keys, flags); "+  
					  "return TRANS_ID_PKG.ret_val; "+
						"end; ");

				Functions.putIfAbsent("KOSARServerTrigDelMulti", "Exists");
			}
		} catch (SQLException e) {
			System.out
			.println("OracleQueryToTrigger.OraclePopulate Error: Failed to process SQL command to display existing packages\n");
			e.printStackTrace(System.out);
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					e.printStackTrace(System.out);
				}
		}		
	}

	public static void OraclePopulate(Connection db_conn) {
		//		OracleRegisterPackages(db_conn);			
		OracleRegisterProcs(db_conn);		
		//		OracleRegisterFunctions(db_conn);
		OracleRegisterTriggers(db_conn);	
	}

	public boolean MySQLRegProc(String cmdToProcess) {
		if (verbose)
			System.out.println("Register Procedure"+ cmdToProcess);
		String procname = mysqlTrigGenerator.GetProcName(cmdToProcess).trim();
		// If the proc exists then report error
		if (Procs.get(procname) != null){
			System.out.println("\nregthread, error, procedure "+procname+" already exists");
			System.out.println("\t Existing procedure: "+Procs.get(procname));
			System.out.println("\t New procedure being authored: "+cmdToProcess);
		}
		else {
			ExecuteCommand(db_conn, cmdToProcess);
			Procs.put(procname, "Exists");
		}
		return true;
	}

	public boolean MySQLRegTrig(String cmd) {
		//System.out.println("------------------------------");
		//System.out.println("STARTING REGTRIG FOR " + cmd);
		MySQLQueryToTrigger.TriggerType triggertype;
		String TblName, TriggerName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = mysqlTrigGenerator.WhatIsTriggerType(cmd);
		TblName = mysqlTrigGenerator.TableName(cmd, triggertype);

		// Invalid trigger - it must have a tablename.
		if (TblName == null)
			return false;

		// If so, create a �DROP IF EXISTS� command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a �DROP IF EXISTS� command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			if(verbose)
				System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);

		//If the existing trigger is null then register command and return success
		if (ExistTrigger == null){
			if (ExecuteCommand(db_conn, cmd) == 0)
				tbl.addVRegTrig(cmd); // add the trigger only after it is registered with the DBMS
		}

		//If the existing trigger is the same as the incoming cmd then do nothing
		if (ExistTrigger != null
				&& mysqlTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)) {
			// No need to do anything; we are done with this incoming
			// trigger
		} else {
			if (ExistTrigger != null) {
				if(verbose) {
					System.out.println("ExistTriger "+ExistTrigger);
					System.out.println("cmd "+cmd);
				}
				// Now, merge the new trigger with the existing one
				Vector<String> inV = new Vector<String>();
				inV.add(cmd);
				inV.add(ExistTrigger);
				Vector<String> outV = new Vector<String>();
				MySQLOptimizeTriggers.Optimize(inV, outV, 2);
				cmd = outV.elementAt(0);
				//If the resulting trigger is the same as existing then there is nothing to be done
				if ( mysqlTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)){
					//Do nothing, the existing trigger is a superset
				} else {
					// Generate the trigger drop command and merge with the
					// incoming trigger
					TriggerName = mysqlTrigGenerator.GetTriggerName(ExistTrigger);
					String dropcommand = ("DROP TRIGGER IF EXISTS " + TriggerName);
					ExecuteCommand(db_conn, dropcommand);
					tbl.trigremove(ExistTrigger);

					if (ExecuteCommand(db_conn, cmd) == 0)
						tbl.addVRegTrig(cmd); // add the trigger only after it is
					// registered with the DBMS
				}
			}
		}
		return true;
	}

	// This should change to process an object of type query
	private boolean mysqlRegQry(String qry) {
		boolean res = true;
		Vector<String> key = new Vector<String>();
		Vector<String> ProcsTrigs = new Vector<String>();
		Vector<String> trgs = new Vector<String>();
		String cmd;

		// Generate triggers
		String QueryTemplate = MySQLqt.TransformQuery(qry, ProcsTrigs, key,
				db_conn);

		if (QueryTemplate == null) {
			// Query template has been deactivated. Do not register its
			// triggers.
			// This should not happen
			System.out.println("WARNING regthread::MySQLRegQry;  A null query tamplate indicating it has been deactived.  This should not happen.");
			System.out.println("Qry "+qry);
			return false;
		}

		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache
				.get(QueryTemplate);
		if (qtelt.isTriggersRegistered()) {
			if (verbose)
				System.out
				.println("regthread:  query template already registered.");
			if (verbose)
				System.out.println("\t" + QueryTemplate);
			return true;
		}

		// Prior to registering triggers, stop both queries and DML commands
		// from being issued to the server
		// because triggers are being dropped and added. No update should sneak
		// in between.
		// This is realized by stopping query/DML processing.
		Kosar.setIssuingTrigCMDS(true);

		// Process procedures first and construct a vector of triggers
		for (int i = 0; i < ProcsTrigs.size(); i++) {
			String cmdToProcess = ProcsTrigs.elementAt(i).toString();
			if (cmdToProcess.indexOf(mysqlTrigGenerator.StartProc) >= 0) {
				MySQLRegProc(cmdToProcess);
			} else
				trgs.addElement(cmdToProcess); // Must be a trigger, save for
			// processing
		}

		// Iterate triggers and register them
		for (int i = 0; i < trgs.size(); i++) {
			cmd = trgs.elementAt(i).toString();
			MySQLRegTrig(cmd);
		}

		// Enable query processing asap
		Kosar.setIssuingTrigCMDS(false);

		// Set the query template to indicate all its triggers have been
		// registered
		qtelt.setTg(trgs);
		qtelt.setTriggersRegistered(true);

		return res;
	}

	private boolean oracleRegProc(String cmdToProcess){
		String procname = mysqlTrigGenerator.GetProcName(cmdToProcess).trim();

		// If the proc exists then report error
		if (Procs.get(procname) != null)
			System.out
			.println("regthread, error, procedure already exists");
		else {
			ExecuteCommand(db_conn, cmdToProcess);
			Procs.put(procname, "Exists");
		}
		return true;
	}

	private boolean oracleRegTrig(String cmd){
		QueryToTrigger.TriggerType triggertype;
		String TblName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = OracleTrigGenerator.WhatIsTriggerType(cmd);
		TblName = OracleTrigGenerator.TableName(cmd, triggertype);

		//Invalid trigger - it must have a tablename.
		if (TblName == null) return false;

		// If so, create a �DROP IF EXISTS� command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a �DROP IF EXISTS� command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);


		//If the existing trigger is null then register command and return success
		if (ExistTrigger == null){
			if (ExecuteCommand(db_conn, cmd) == 0)
				tbl.addVRegTrig(cmd); // add the trigger only after it is registered with the DBMS
		}

		//If the existing trigger is the same as the incoming cmd then do nothing
		if (ExistTrigger != null
				&& OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)) {
			// No need to do anything; we are done with this incoming
			// trigger
		} else {
			if (ExistTrigger != null) {
				// Now, merge the new trigger with the existing one
				Vector<String> inV = new Vector<String>();
				inV.add(cmd);
				inV.add(ExistTrigger);
				Vector<String> outV = new Vector<String>();
				System.out.println(""+cmd);
				System.out.println(""+ExistTrigger);
				OracleOptimizeTriggers.Optimize(inV, outV, 2);
				cmd = outV.elementAt(0);
				//If the resulting trigger is the same as existing then there is nothing to be done
				if ( OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)){
					//Do nothing, the existing trigger is a superset
				} else {
					if (ExecuteCommand(db_conn, cmd) == 0){
						tbl.trigremove(ExistTrigger);
						tbl.addVRegTrig(cmd); // add the trigger only after it is
						// registered with the DBMS
					}
				}
			}
		}
		return true;
	}

	public static String MergeTrigWithExistingOne(String cmd){
		QueryToTrigger.TriggerType triggertype;
		String TblName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = OracleTrigGenerator.WhatIsTriggerType(cmd);
		TblName = OracleTrigGenerator.TableName(cmd, triggertype);

		//Invalid trigger - it must have a tablename.
		if (TblName == null) return null;

		// If so, create a �DROP IF EXISTS� command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a �DROP IF EXISTS� command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);


		//If the existing trigger is null then register command and return success
		if (ExistTrigger == null){
			return cmd;
		}

		//If the existing trigger is the same as the incoming cmd then do nothing
		if (ExistTrigger != null
				&& OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)) {
			// No need to do anything; we are done with this incoming
			// trigger
		} else {
			if (ExistTrigger != null) {
				// Now, merge the new trigger with the existing one
				Vector<String> inV = new Vector<String>();
				inV.add(cmd);
				inV.add(ExistTrigger);
				Vector<String> outV = new Vector<String>();
				//				System.out.println(""+cmd);
				//				System.out.println(""+ExistTrigger);
				OracleOptimizeTriggers.Optimize(inV, outV, 2);
				cmd = outV.elementAt(0);
				//If the resulting trigger is the same as existing then there is nothing to be done
				if ( OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)){
					//Do nothing, the existing trigger is a superset
				} else {
					return cmd;
				}
			}
		}
		return null;
	}

	public static void UpdateTrigTable(String cmd) {
		QueryToTrigger.TriggerType triggertype;
		String TblName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = OracleTrigGenerator.WhatIsTriggerType(cmd);
		TblName = OracleTrigGenerator.TableName(cmd, triggertype);

		//Invalid trigger - it must have a tablename.
		if (TblName == null) return;

		// If so, create a �DROP IF EXISTS� command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a �DROP IF EXISTS� command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);

		if (ExistTrigger != null) {
			tbl.trigremove(ExistTrigger);
		}
		tbl.addVRegTrig(cmd); // add the trigger only after it is		
	}

	private boolean oracleRegQry(String qry) {
		boolean res = true;
		Vector<String> key = new Vector<String>();
		Vector<String> ProcsTrigs = new Vector<String>();
		Vector<String> trgs = new Vector<String>();
		String cmd;

		// Generate triggers
		String queryTemplate = ORCLqt.TransformQuery(qry, ProcsTrigs, key, db_conn);

		if (queryTemplate == null) {
			// Query template has been deactivated. Do not register its triggers.
			// This should not happen
			System.out.println("WARNINING regthread::OracleRegQry;  A null query tamplate indicating it has been deactived.  This should not happen.");
			System.out.println("Qry "+qry);
			return false;
		}

		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(queryTemplate);
		if (qtelt.isTriggersRegistered()) {
			if (verbose)
				System.out
				.println("regthread:  query template already registered.");
			if (verbose)
				System.out.println("\t" + queryTemplate);
			return true;
		}
		// Prior to registering triggers, stop both queries and DML commands
		// from being issued to the server
		// because triggers are being dropped and added. No update should sneak
		// in between.
		// This is realized by stopping query/DML processing.
		Kosar.setIssuingTrigCMDS(true);

		// Process procedures first and construct a vector of triggers
		for (int i = 0; i < ProcsTrigs.size(); i++) {
			String cmdToProcess = ProcsTrigs.elementAt(i).toString();
			if (cmdToProcess.indexOf(OracleTrigGenerator.StartProc) >= 0) {
				oracleRegProc(cmdToProcess);
			} else
				trgs.addElement(cmdToProcess); // Must be a trigger, save for
			// processing
		}

		// Iterate triggers and register
		for (int i = 0; i < trgs.size(); i++) {
			cmd = trgs.elementAt(i).toString();
			oracleRegTrig(cmd);
		}

		// Set the query template to indicate all its triggers have been
		// registered
		qtelt.setTg(trgs);
		qtelt.setTriggersRegistered(true);

		// Enable query processing asap
		Kosar.setIssuingTrigCMDS(false);		

		return res;
	}

	public void run() {
		while (isRunning) {
			try {
				if (!DoRegTrigg.tryAcquire()) {
					Thread.sleep(1000);
					continue;
				}
				
				RDBMScmds.acquire();				
				String qry = workitems.remove(0).toString();
				RDBMScmds.release();

				if (verbose)
					System.out.println("Register qry "+qry);

				switch (QueryToTrigger.getTargetSystem()) {
				case MySQL:
					mysqlRegQry(qry);
					break;
				case Oracle:
					oracleRegQry(qry);
					break;
				default:
					break;
				}
			} catch (InterruptedException e) {
				System.out.println("Got an exception when registering the query");
				e.printStackTrace(System.out);
			}
			if (verbose)
				System.out.println(counter++);

			// Enable query processing
			Kosar.setIssuingTrigCMDS(false);
			if (verbose) System.out.println("After Exception, flag is "+Kosar.isIssuingTrigCMDS());
		}
	}

	public regthread(Connection con1) {
		if (verbose)
			System.out.println("Starting the Trigger Registeration Thread.");
		if (con1 == null) {
			System.out.println("" + BeginErrMsgInit
					+ " Input JDBC connection is null.");
			System.out
			.println("Continuing execution without trigger registeration:  Expect stale data.");
			return;
		}

		// Get a connection
		db_conn = con1;

		// Instantiate the appropriate translator for a target RDBMS
		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			MySQLqt = new MySQLQueryToTrigger();
			MySQLPopulate(db_conn);
			break;
		case Oracle:
			ORCLqt = new OracleQueryToTrigger();
			OraclePopulate(db_conn);
			break;
		default:
			System.out
			.println("Error, regthread:  target unknown system.  No trigger will be generated.");
			break;
		}
	}

	public static void main(String[] args) {
		//Stand alone test
		Vector<String> V = new Vector<String>();
		String QueryTemplate;

		try {
			// Create a connection object to the dbms
			Class.forName ("oracle.jdbc.driver.OracleDriver");
			db_conn = DriverManager.getConnection("jdbc:oracle:thin:hieunguyen/hieunguyen123@//10.0.0.215:1521/ORCL","hieunguyen","hieunguyen123");
		} catch(Exception e){
			e.printStackTrace();
			return ;
		}
		ORCLqt = new OracleQueryToTrigger();
		OraclePopulate(db_conn);
		//oracleRegQry("SELECT count(*) FROM  friendship WHERE (inviterID = 50 OR inviteeID = 50) AND status = 2 ");
		//oracleRegQry("SELECT count(*) FROM  friendship WHERE inviteeID = 60 AND status = 1 ");
	}
}
