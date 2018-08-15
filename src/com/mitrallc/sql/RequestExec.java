package com.mitrallc.sql;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.common.Constants;
import com.mitrallc.communication.CacheModeController;
import com.mitrallc.communication.ClientConnector;
import com.mitrallc.communication.CoreConnector;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sqltrig.QueryToTrigger;

public class RequestExec {
	public static final String insert = "INSERT";
	public static final String update = "UPDATE";
	public static final String delete = "DELETE";
	
	public ResultSet executeQuery(String input_query, Connection conn, String sql,
			CoreConnector coreConnector, ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases,
			ConcurrentHashMap<Long, String> clients, boolean preparedStatement,
			 com.mitrallc.sql.PreparedStatement stmt,
			 byte[] deserialize_buffer, ByteBuffer buffer) throws KosarSQLException {
		long cmdStartTime = 0;
		int rt = 0;
		com.mitrallc.sql.ResultSet cached_rowset = null;

		try {
			// If this is an update operation, connect to DB directly.
			if (input_query.toUpperCase().trim().startsWith(insert)
					|| input_query.toUpperCase().trim().startsWith(update)
					|| input_query.toUpperCase().trim().startsWith(delete)){
				System.out.println("WARNING:  executeQuery is used to process an SQL update command.");
				this.executeUpdate();
				return null;
			}

			if(KosarSoloDriver.webServer != null) {
				cmdStartTime = System.currentTimeMillis();
			}

			if (KosarSoloDriver.kosarEnabled
					&& CacheModeController.isCacheReadAllowed() == true 
					&& conn.getAutoCommit()
					&& QueryToTrigger.IsQuerySupported(sql)) {
				while (true) {
					// check the cache
					cached_rowset = KosarSoloDriver.Kache.GetQueryResult(sql);

					if (cached_rowset != null) {
						if(KosarSoloDriver.webServer != null){
							rt = (int)(System.currentTimeMillis()-cmdStartTime);
							KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
							KosarSoloDriver.KosarCacheHitsEventMonitor.newEvent(1);
							KosarSoloDriver.last100readQueries.add(sql,true);
							KosarSoloDriver.KosarReadTimeMonitor.newEvent(rt);
						}

						return cached_rowset;
					}
					long Tmiss = System.currentTimeMillis();

					if (KosarSoloDriver.getFlags().coordinatorExists() 
							&& KosarSoloDriver.getFlags()
							.isCoordinatorConnected()) {
						int reply = coreConnector.acquireILease2(sql, grantedILeases, clients);

						if (reply == Constants.REPLY_I_LEASE_GRANTED) {
							if (KosarSoloDriver.webServer != null) {
								KosarSoloDriver.KosarILeaseGrantedEventMonitor.newEvent(1);
							}

							// query RDBMS for the results
							cached_rowset = executeQueryAgainstDB(preparedStatement, sql, conn, stmt);

							int retval = -1;

							// release the I lease							
							KosarSoloDriver.getLockManager().lock(sql);

							KosarSoloDriver.Kache.addKeyToInternalTokenElt(sql);
							
							if (Constants.consistencyMode == Constants.NO_IQ) {
								retval = Constants.STORED;
							} else {
								retval = coreConnector.releaseILease2(Constants.CLIENT_RELEASE_I_LEASE, sql, grantedILeases, -1);
							}
							
							if (retval == Constants.STORED) {
								attemptToCache(sql, Tmiss, cached_rowset, conn);
								if (KosarSoloDriver.webServer != null) {
									KosarSoloDriver.KosarILeaseReleasedEventMonitor.newEvent(1);
								}
							}

							KosarSoloDriver.getLockManager().unlock(sql);						
							
							if (KosarSoloDriver.webServer != null) {
								KosarSoloDriver.KosarReadTimeFromDBMSMonitor.newEvent((int)(System.currentTimeMillis() - cmdStartTime));
							}
						} else if (reply == Constants.CMD_COPY || reply == Constants.CMD_STEAL || reply == Constants.CMD_USEONCE) {
							if (KosarSoloDriver.webServer != null) {
								KosarSoloDriver.KosarILeaseGrantedEventMonitor.newEvent(1);
							}
							
							int retval = -1;

							if (clients.size() == 0) {
								System.out.println("Read key: Error: no clients info");
								System.exit(-1);
							}

							long id = -1;
							List<Long> cli_list = new ArrayList<Long>(clients.keySet());
							Collections.shuffle(cli_list);
							
							// go through each client until the value is found
							while (cli_list.size() > 0 && cached_rowset == null) {
								id = cli_list.remove(0);
								
								// get the value from the client
								long start = System.currentTimeMillis();
								cached_rowset = ClientConnector.copyValue(id, reply, clients.get(id), sql, deserialize_buffer, buffer);
								
								if (KosarSoloDriver.webServer != null) {
									Kosar.KosarMonitor.IncrementNumCopy(System.currentTimeMillis()-start);
									KosarSoloDriver.KosarNumAskAnotherClientEventMonitor.newEvent(1);
								}									
							}

							if (cached_rowset == null) {
								if (KosarSoloDriver.webServer != null)
									Kosar.KosarMonitor.IncrementNumCopyMiss();
								cached_rowset = executeQueryAgainstDB(preparedStatement, sql, conn, stmt);
								id = -1;
							} else {
								if (KosarSoloDriver.webServer != null) {
									KosarSoloDriver.KosarNumGotKeyFromAnotherClientEventMonitor.newEvent(1);
								}
							}

							int cmd = -1;
							if (reply == Constants.CMD_STEAL) {
								cmd = Constants.CLIENT_RELEASE_I_STEAL;
							} else if (reply == Constants.CMD_USEONCE) {
								cmd = Constants.CLIENT_RELEASE_I_USEONCE;
							}

							KosarSoloDriver.getLockManager().lock(sql);	

							KosarSoloDriver.Kache.addKeyToInternalTokenElt(sql);
							
							retval = coreConnector.releaseILease2(cmd, sql, grantedILeases, id);							
							
							if (retval == Constants.STORED) {
								if (KosarSoloDriver.webServer != null) {
									KosarSoloDriver.KosarILeaseReleasedEventMonitor.newEvent(1);
								}
								Kosar.KosarMonitor.IncrementKeyCopyStored();
								attemptToCache(sql, Tmiss, cached_rowset, conn);
							}

							KosarSoloDriver.getLockManager().unlock(sql);
							
							if (KosarSoloDriver.webServer != null) {
								if (id == -1)	// go to DBMS
									KosarSoloDriver.KosarReadTimeFromOthersAndDBMSMonitor.newEvent((int)(System.currentTimeMillis() - cmdStartTime));
								else
									KosarSoloDriver.KosarReadTimeFromOthersMonitor.newEvent((int)(System.currentTimeMillis() - cmdStartTime));
							}
						} else if (reply == Constants.REPLY_I_LEASE_RETRY) {
							// release all the I leases granted so far
							coreConnector.releaseILease2(Constants.CLIENT_RELEASE_I_LEASE, sql, grantedILeases, -1);

							if (KosarSoloDriver.webServer != null) {
								KosarSoloDriver.KosarReadBackoffEventMonitor.newEvent(1);
							}

							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace(System.out);
							}

							// keep-asking the core for the I lease
							grantedILeases.clear();
							clients.clear();
							continue;							
						}		
						
						grantedILeases.clear();
						clients.clear();
					} else {
						// block while triggers are being registered
						regthread.BusyWaitForRegThread(sql);

						//If there is no coordinator, cache the item directly.
						cached_rowset = executeQueryAgainstDB(preparedStatement, sql, conn, stmt);

						KosarSoloDriver.getLockManager().lock(sql);
						attemptToCache(sql, Tmiss, cached_rowset, conn);
						KosarSoloDriver.getLockManager().unlock(sql);
					}

					break;
				}
			} else {
				// If kosar does not exist or cache read is not allowed or
				// auto-commit is not on, execute against DB.				
				cached_rowset = executeQueryAgainstDB(preparedStatement, sql, conn, stmt);
			}
			if (KosarSoloDriver.webServer != null)
				KosarSoloDriver.last100readQueries.add(sql,false);
		} catch (SQLException s) {
			throw new KosarSQLException(s.getMessage());
		}
		
		if (KosarSoloDriver.webServer != null){
			rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarReadTimeMonitor.newEvent(rt);
		}

		return cached_rowset;		
	}

	private com.mitrallc.sql.ResultSet executeQueryAgainstDB(boolean preparedStatement,
			String sql, Connection conn,  com.mitrallc.sql.PreparedStatement stmt) throws SQLException {
		long queryStartTime = 0;		
		if (KosarSoloDriver.webServer != null)
			queryStartTime = System.currentTimeMillis();
		
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			rs = stmt.executeQuery();
			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}

		com.mitrallc.sql.ResultSet result = null;

		result = new com.mitrallc.sql.ResultSet(rs);
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarQueryResponseTimeEventMonitor.newEvent(
					(int)(System.currentTimeMillis()-queryStartTime));

		return result;
	}

	private void executeUpdate() {
		// TODO Auto-generated method stub
		
	}
	
	public void attemptToCache(String sql, long Tmiss, 
			com.mitrallc.sql.ResultSet result, Connection conn)
			throws KosarSQLException {
		try {
			if (CacheModeController.isCacheUpdateAllowed()
					&& conn.getAutoCommit()
					&& KosarSoloDriver.Kache != null) {

					KosarSoloDriver.Kache.attemptToCache(sql,
							(new com.mitrallc.sql.ResultSet(result)),
							Tmiss);
				
			}
		} catch (SQLException s) {
			//KosarSoloDriver.getLockManager().unlockKey(sql);
			throw new KosarSQLException(s.getMessage());
		}
	}	
}
