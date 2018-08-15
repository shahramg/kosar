package com.mitrallc.sql;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
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

public class Statement implements java.sql.Statement {
	private java.sql.Statement stmt;
	private com.mitrallc.sql.Connection conn;

	private static boolean transparentCaching = true;
	private boolean verbose=false;
	
	private com.mitrallc.sql.ResultSet mitraRS;
	private byte[] deserialize_buffer = new byte[1024];
	private ByteBuffer buffer = ByteBuffer.allocate(1024);
	
	ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases = 
			new ConcurrentHashMap<>();
	ConcurrentHashMap<Long, String> clients = 
			new ConcurrentHashMap<Long, String>();
	private CoreConnector coreConnector = new CoreConnector();
	
	private static long cmdStartTime;
	
	public Statement( java.sql.Statement stmt, com.mitrallc.sql.Connection conn )
	{
		this.stmt = stmt;
		this.conn = conn;
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isWrapperFor(arg0);
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.unwrap(arg0);
	}

	@Override
	public void addBatch(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.addBatch(arg0);
	}

	@Override
	public void cancel() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.cancel();
	}

	@Override
	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.clearBatch();
	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.close();
	}
	
	public static boolean isTransparentCaching() {
		return transparentCaching;
	}

	public static void setTransparentCaching(boolean transparentCaching) {
		Statement.transparentCaching = transparentCaching;
	}

	@Override
	public boolean execute(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0);
	}

	@Override
	public boolean execute(String arg0, int arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public int[] executeBatch() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.executeBatch();
	}
	
	private void executeQueryAgainstDB(String sql)
			throws SQLException {
		long queryStartTime = 0;
		if (KosarSoloDriver.webServer != null)
			queryStartTime = System.currentTimeMillis();
		
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			rs = this.stmt.executeQuery(sql);
			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}
		
		this.mitraRS = new com.mitrallc.sql.ResultSet(rs);
		
		if(KosarSoloDriver.webServer != null){
			KosarSoloDriver.last100readQueries.add(sql,false);
			KosarSoloDriver.KosarQueryResponseTimeEventMonitor.newEvent(
					(int)(System.currentTimeMillis()-queryStartTime));
		}
	}

	@Override
	public com.mitrallc.sql.ResultSet executeQuery(String sql) throws SQLException {
		int rt = 0;
		this.mitraRS = null;

		if(KosarSoloDriver.webServer != null) {
			cmdStartTime = System.currentTimeMillis();
		}

		try {
			// If this is an update operation, connect to DB directly.
			if (sql.toUpperCase().trim().startsWith("INSERT")
					|| sql.toUpperCase().trim().startsWith("UPDATE")
					|| sql.toUpperCase().trim().startsWith("DELETE")){
				System.out.println("WARNING:  executeQuery is used to process an SQL update command.");
				this.executeUpdate(sql);
				return null;
			}

			//			NDC.push("PreparedStatement.executeQuery");

			/*
			 * If this is not an update statement, check if:
			 * 
			 * 1. Kosar is enabled (controlled by setting kosarenabled to true
			 * in the config file)
			 * 
			 * 2. CacheRead is allowed (Cache read is allowed by default if
			 * Kosar is enabled. If the client loses connection to the
			 * coordinator for a duration of time longer than the standard lease
			 * duration, cache read is disallowed.
			 * 
			 * 3. The connection has auto-commit turned on.
			 */
			if (verbose){
				System.out.println(sql);
				System.out.println("kosarEnabled="+KosarSoloDriver.kosarEnabled);
				System.out.println("isCachedReadAllowed="+CacheModeController.isCacheReadAllowed());
				System.out.println("AutoCommit="+this.conn.getAutoCommit());
			}

			if (KosarSoloDriver.kosarEnabled
					&& CacheModeController.isCacheReadAllowed() == true 
					&& this.conn.getAutoCommit()
					&& QueryToTrigger.IsQuerySupported(sql)) {

				com.mitrallc.sql.ResultSet cached_rowset = null;
				while (true) {
					// check the cache
					KosarSoloDriver.getLockManager().lock(sql);
					cached_rowset = KosarSoloDriver.Kache.GetQueryResult(sql);
					KosarSoloDriver.getLockManager().unlock(sql);

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
							executeQueryAgainstDB(sql);

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
								if (KosarSoloDriver.webServer != null) {
									KosarSoloDriver.KosarILeaseReleasedEventMonitor.newEvent(1);
								}
								attemptToCache(sql, Tmiss);								
							}

							KosarSoloDriver.getLockManager().unlock(sql);						
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
								executeQueryAgainstDB(sql);
							} else {
								this.mitraRS = cached_rowset;
								KosarSoloDriver.KosarNumGotKeyFromAnotherClientEventMonitor.newEvent(1);
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
								attemptToCache(sql, Tmiss);	
							}

							KosarSoloDriver.getLockManager().unlock(sql);
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

							grantedILeases.clear();
							clients.clear();
							// keep-asking the core for the I lease
							continue;							
						}
						
						grantedILeases.clear();
						clients.clear();
					} else {					
						// block while triggers are being registered
						regthread.BusyWaitForRegThread(sql);

						//If there is no coordinator, cache the item directly.
						executeQueryAgainstDB(sql);

						KosarSoloDriver.getLockManager().lock(sql);
						attemptToCache(sql, Tmiss);
						KosarSoloDriver.getLockManager().unlock(sql);
					}

					break;
				}
			} else {
				// If kosar does not exist or cache read is not allowed or
				// auto-commit is not on, execute against DB.				
				executeQueryAgainstDB(sql);
				//				logger.debug("DBMS Execute Query: " + sql);
			}
			//			NDC.pop();
			KosarSoloDriver.last100readQueries.add(sql,false);
		} catch (SQLException s) {
			throw new KosarSQLException(s.getMessage());
			//		} catch (UnsupportedEncodingException e) {
			//			System.out.println("Unsupported Encoding Exception for command");
			//			e.printStackTrace();
		}
		if (KosarSoloDriver.webServer != null){
			rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarReadTimeMonitor.newEvent(rt);
		}		
		return this.mitraRS;
	}

	public void attemptToCache(String sql, long Tmiss)
			throws KosarSQLException {
		try {
			if (CacheModeController.isCacheUpdateAllowed()
					&& this.conn.getAutoCommit()
					&& KosarSoloDriver.Kache != null) {

				if (transparentCaching) {
					KosarSoloDriver.Kache.attemptToCache(sql,
							(new com.mitrallc.sql.ResultSet(this.mitraRS)),
							Tmiss);
				}
			}
		} catch (SQLException s) {
			//KosarSoloDriver.getLockManager().unlockKey(sql);
			throw new KosarSQLException(s.getMessage());
		}
	}

	@Override
	public int executeUpdate(String arg0) throws SQLException {
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		
		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();

		java.sql.PreparedStatement st = conn.prepareStatement(arg0);
		int retVal = st.executeUpdate();
		
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0, false);
		}
		return retVal;
	}
	
	@Override
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		// TODO Auto-generated method stub
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		
		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();
		
		int retVal = this.stmt.executeUpdate(arg0, arg1);
		
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0, false);
		}
		
		return retVal;
	}

	@Override
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		
		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();
		
		int retVal = this.stmt.executeUpdate(arg0, arg1);
		
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0, false);
		}
		
		return retVal;
	}

	@Override
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		
		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();
		
		int retVal = this.stmt.executeUpdate(arg0, arg1);
		
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0, false);
		}
		
		return retVal;
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getConnection();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getFetchSize();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getGeneratedKeys();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMaxFieldSize();
	}

	@Override
	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMaxRows();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMoreResults();
	}

	@Override
	public boolean getMoreResults(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMoreResults();
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getQueryTimeout();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSet();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetHoldability();
	}

	@Override
	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetType();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getUpdateCount();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isPoolable();
	}

	@Override
	public void setCursorName(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setCursorName(arg0);
	}

	@Override
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setEscapeProcessing(arg0);
	}

	@Override
	public void setFetchDirection(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setFetchDirection(arg0);
	}

	@Override
	public void setFetchSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setFetchSize(arg0);
	}

	@Override
	public void setMaxFieldSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setMaxFieldSize(arg0);
	}

	@Override
	public void setMaxRows(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setMaxRows(arg0);
	}

	@Override
	public void setPoolable(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setPoolable(arg0);
	}

	@Override
	public void setQueryTimeout(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setQueryTimeout(arg0);
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public static void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}
}
