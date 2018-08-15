package com.mitrallc.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.common.Constants;
import com.mitrallc.communication.CacheModeController;
import com.mitrallc.communication.ClientConnector;
import com.mitrallc.communication.CoreConnector;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sqltrig.QueryToTrigger;

public class PreparedStatement implements java.sql.PreparedStatement {
	private java.sql.PreparedStatement stmt;
	private com.mitrallc.sql.Connection conn;
	private String input_query;
	private Object[] param_list;

	//	private com.mitrallc.sql.ResultSet mitraRS;
	private static boolean transparentCaching = true;
	private static long cmdStartTime;
	//	private static Logger logger = Logger.getLogger(Statement.class.getName());

	public static final String insert = "INSERT ";
	public static final String delete = "DELETE ";
	public static final String update = "Update ";

	public static AtomicInteger numWrites = new AtomicInteger(0);
	public static AtomicInteger numCommit = new AtomicInteger(0);
	public static AtomicInteger numAbort = new AtomicInteger(0);

	private static final String CLASSNAME_STRING = "class java.lang.String";
	ConcurrentHashMap<Integer, HashMap<Integer, Set<String>>> grantedILeases = 
			new ConcurrentHashMap<>();
	ConcurrentHashMap<Long, String> clients = 
			new ConcurrentHashMap<Long, String>();
	private CoreConnector coreConnector = new CoreConnector();

	byte[] deserialize_buffer = new byte[1024];
	ByteBuffer buffer = ByteBuffer.allocate(1024);

	com.mitrallc.sql.CallableStatement callableStatament = null;

	PreparedStatement(String query, java.sql.PreparedStatement pstmt,
			com.mitrallc.sql.Connection conn) {
		this.stmt = pstmt;
		this.conn = conn;
		this.input_query = query;

		this.param_list = new Object[parseNumParams(query)];
	}

	public static int parseNumParams(String query) {
		int start = 0;
		int found = 0;
		int num_params = 0;
		while (true) {
			found = query.indexOf('?', start);
			if (found < start) {
				break;
			} else {
				start = found + 1;
				num_params++;
			}
		}

		return num_params;
	}

	public static String generateQueryString(String input_query, Object[] param_array) {		
		String final_query = "";
		int start = 0;
		int end = 0;

		if (param_array.length == 0) {
			return input_query;
		}

		for (int i = 0; i < param_array.length; i++) {
			end = input_query.indexOf('?', start);
			if (end > start) {
				final_query += input_query.substring(start, end);
				if (param_array[i] == null)
					final_query += "null";
				else {
					param_array[i].getClass().toString();
					if (param_array[i].getClass().toString()
							.equals(CLASSNAME_STRING)) {
						final_query += "'";
					}
					final_query += param_array[i].toString();
					if (param_array[i].getClass().toString()
							.equals(CLASSNAME_STRING)) {
						final_query += "'";
					}
				}
				start = end + 1;
			} else {
				return null;
			}
		}

		if (start < input_query.length()) {
			final_query += input_query.substring(start, input_query.length());
		}

		return final_query;
	}

	@Override
	public void addBatch(String arg0) throws SQLException {
		this.stmt.addBatch(arg0);
	}

	@Override
	public void cancel() throws SQLException {
		this.stmt.cancel();
	}

	@Override
	public void clearBatch() throws SQLException {
		this.stmt.clearBatch();
	}

	@Override
	public void clearWarnings() throws SQLException {
		this.stmt.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		if (this.callableStatament != null)
			this.callableStatament.close();
		this.stmt.close();
	}

	@Override
	public boolean execute(String arg0) throws SQLException {
		return this.stmt.execute(arg0);
	}

	@Override
	public boolean execute(String arg0, int arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return this.stmt.executeBatch();
	}

	@Override
	public ResultSet executeQuery(String arg0) throws KosarSQLException {
		return this.executeQuery(arg0, false);
	}

	public ResultSet executeQuery(String sql, boolean preparedStatement)
			throws KosarSQLException {
		int rt = 0;
		com.mitrallc.sql.ResultSet cached_rowset = null;

		try {
			// If this is an update operation, connect to DB directly.
			if (this.input_query.toUpperCase().trim().startsWith(insert)
					|| this.input_query.toUpperCase().trim().startsWith(update)
					|| this.input_query.toUpperCase().trim().startsWith(delete)){
				System.out.println("WARNING:  executeQuery is used to process an SQL update command.");
				this.executeUpdate();
				return null;
			}

			if(KosarSoloDriver.webServer != null) {
				cmdStartTime = System.currentTimeMillis();
			}

			if (KosarSoloDriver.kosarEnabled
					&& CacheModeController.isCacheReadAllowed() == true 
					&& this.conn.getAutoCommit()
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
							cached_rowset = executeQueryAgainstDB(preparedStatement, sql);

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
								attemptToCache(sql, Tmiss, cached_rowset);
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
								cached_rowset = executeQueryAgainstDB(preparedStatement, sql);
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
							
//							if (id != KosarSoloDriver.clientData.getID())
								retval = coreConnector.releaseILease2(cmd, sql, grantedILeases, id);
//							else
//								retval = coreConnector.releaseILease2(cmd, sql, grantedILeases, -1);
							
							if (retval == Constants.STORED) {
								if (KosarSoloDriver.webServer != null) {
									KosarSoloDriver.KosarILeaseReleasedEventMonitor.newEvent(1);
								}
								Kosar.KosarMonitor.IncrementKeyCopyStored();
//								if (id != KosarSoloDriver.clientData.getID())
								attemptToCache(sql, Tmiss, cached_rowset);
							}

							KosarSoloDriver.getLockManager().unlock(sql);
							
							if (KosarSoloDriver.webServer != null) {
								if (id == -1)	// go to DBMS
									KosarSoloDriver.KosarReadTimeFromOthersAndDBMSMonitor.newEvent((int)(System.currentTimeMillis() - cmdStartTime));
								else if (id != KosarSoloDriver.clientData.getID())
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
						cached_rowset = executeQueryAgainstDB(preparedStatement, sql);

						KosarSoloDriver.getLockManager().lock(sql);
						attemptToCache(sql, Tmiss, cached_rowset);
						KosarSoloDriver.getLockManager().unlock(sql);
					}

					break;
				}
			} else {
				// If kosar does not exist or cache read is not allowed or
				// auto-commit is not on, execute against DB.				
				cached_rowset = executeQueryAgainstDB(preparedStatement, sql);
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

	private com.mitrallc.sql.ResultSet executeQueryAgainstDB(boolean preparedStatement, String sql)
			throws SQLException {
		
		long queryStartTime = 0;		
		if (KosarSoloDriver.webServer != null)
			queryStartTime = System.currentTimeMillis();
		
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			rs = this.stmt.executeQuery();
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

	public void attemptToCache(String sql, long Tmiss, com.mitrallc.sql.ResultSet result)
			throws KosarSQLException {
		try {
			if (CacheModeController.isCacheUpdateAllowed()
					&& this.conn.getAutoCommit()
					&& KosarSoloDriver.Kache != null) {

				if (transparentCaching) {
					KosarSoloDriver.Kache.attemptToCache(sql,
							(new com.mitrallc.sql.ResultSet(result)),
							Tmiss);
					
//					if (!stored) {
//						// get tokens
//						String[] tokenList = Kosar.getIks(sql);
//						for (String token : tokenList) {
//							Kosar.ITs[Kosar.getFragment(token)].get(token).getQueryStringMap().remove(sql);
//						}
//					}
				}
			}
		} catch (SQLException s) {
			//KosarSoloDriver.getLockManager().unlockKey(sql);
			throw new KosarSQLException(s.getMessage());
		}
	}


	@Override
	public int executeUpdate(String arg0) throws SQLException {
		int retVal = 0;

		regthread.BusyWaitForRegThread(arg0);
		
		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();
		
		if (KosarSoloDriver.kosarEnabled && conn.getAutoCommit()) {
			if (KosarSoloDriver.getFlags().coordinatorExists()) {
				if (KosarSoloDriver.getFlags().isCoordinatorConnected()) {
					int transId = KosarSoloDriver.transId.incrementAndGet();
					String s = generateQueryString(this.input_query, this.param_list);

					int isCommit = -1;
					SQLException exc = null;
					try {
						if (callableStatament == null) {
							callableStatament = (com.mitrallc.sql.CallableStatement)
									conn.prepareCall("{ call DO_DML(?,?,?,?) }");
						}

						callableStatament.setString(1, s);

						long id = KosarSoloDriver.clientData.getID();
						callableStatament.setString(2, String.valueOf(id));
						callableStatament.setInt(3, transId);
						callableStatament.registerOutParameter(4, Types.INTEGER);
						callableStatament.executeDML();
						isCommit = callableStatament.getInt(4);
						numWrites.incrementAndGet();
					} catch (SQLException e) {
						exc = e;
					} finally {
						if (callableStatament != null)
							callableStatament.clearParameters();
					}

					if (Constants.consistencyMode == Constants.IQ) {
						coreConnector.releaseQLease2(transId, isCommit);
					}
					
					if (exc != null)
						throw exc;
					else if (isCommit != 0) {
						throw new SQLException("Transaction aborted.");
					}
				} else {
					retVal = this.stmt.executeUpdate();
				}
			} else {
				/* Deletion of Keys from the Cache without Coordinator
				 * Beginning of Transaction; set start time of transaction;
				 * Data structure found & described in com.mitrallc.common.DynamicArray
				 */
				Constants.PENDING_TRANSACTION_WRITE_LOCK.lock();
				long currentStart = Constants.AI.incrementAndGet();
				KosarSoloDriver.pendingTransactionArray.add(currentStart);
				Constants.PENDING_TRANSACTION_WRITE_LOCK.unlock();

				/*
				 * Execution of this statement goes to the DBMS.
				 * Returns to com.mitrallc.kosar.TriggerListener
				 */
				retVal = this.stmt.executeUpdate();

				/*
				 * For keys whose timestamp lies between the start
				 * and end times of the transaction, remove from the cache.
				 * Those keys have been updated in some way and should not
				 * be read from the cache.
				 */
				Constants.KEY_QUEUE_READ_LOCK.lock();
				long endtime = Constants.AI.incrementAndGet();
				for(int i = 0; i < KosarSoloDriver.keyQueue.size(); i++)
					if(KosarSoloDriver.keyQueue.getCounter(i) > currentStart
							&& KosarSoloDriver.keyQueue.getCounter(i) < endtime) {
						String keylist = new String(KosarSoloDriver.keyQueue.getKeyList(i));
						String[] its = keylist.trim().split(" ");
						for (int j = 0; j < its.length; j++)
							KosarSoloDriver.Kache.DeleteIT(its[j]);
					}
				Constants.KEY_QUEUE_READ_LOCK.unlock();

				/*
				 * If the current transaction is the oldest in the array,
				 * and we need to remove keys from the queue whose timestamp
				 * is before the lowest of either the start-time of the next oldest
				 * transaction or the end-time of the oldest.
				 * 
				 * Explanation: If the current transaction (oldest) ended AFTER
				 * another transaction begins, we don't want to remove keys from the
				 * queue that correspond to the next transaction.  Otherwise, keys
				 * may not be deleted
				 */	
				long cleanupTime=-1;
				Constants.PENDING_TRANSACTION_READ_LOCK.lock();
				if(KosarSoloDriver.pendingTransactionArray.getIndexOf(currentStart) == 0) {
					if(KosarSoloDriver.pendingTransactionArray.size() > 1
							&& KosarSoloDriver.pendingTransactionArray.getCounter(1) < endtime)
						cleanupTime = KosarSoloDriver.pendingTransactionArray.getCounter(1);
					else cleanupTime = endtime;
				}
				Constants.PENDING_TRANSACTION_READ_LOCK.unlock();

				if (cleanupTime != -1){
					Constants.KEY_QUEUE_WRITE_LOCK.lock();
					KosarSoloDriver.keyQueue.removeUpTo(cleanupTime);
					/*for(int i = 0; i < KosarSoloDriver.keyQueue.size(); i++)
								if(KosarSoloDriver.keyQueue.getCounter(i) < cleanupTime)
									KosarSoloDriver.keyQueue.remove(
											KosarSoloDriver.keyQueue.getCounter(i));*/
					Constants.KEY_QUEUE_WRITE_LOCK.unlock();
				}

				/*
				 * Finally, remove the transaction from the list.
				 */
				Constants.PENDING_TRANSACTION_WRITE_LOCK.lock();
				KosarSoloDriver.pendingTransactionArray.remove(currentStart);
				Constants.PENDING_TRANSACTION_WRITE_LOCK.unlock();
			}
		} else {
			retVal = this.stmt.executeUpdate();
		}
		
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0,false);
		}

		return retVal;
	}

	@Override
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.stmt.getConnection();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return this.stmt.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		return this.stmt.getFetchSize();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return this.stmt.getGeneratedKeys();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return this.stmt.getMaxFieldSize();
	}

	@Override
	public int getMaxRows() throws SQLException {
		return this.stmt.getMaxRows();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return this.stmt.getMoreResults();
	}

	@Override
	public boolean getMoreResults(int arg0) throws SQLException {
		return this.stmt.getMoreResults(arg0);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return this.stmt.getQueryTimeout();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return this.stmt.getResultSet();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return this.stmt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return this.stmt.getResultSetHoldability();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return this.stmt.getResultSetType();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return this.stmt.getUpdateCount();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return this.stmt.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.stmt.isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return this.stmt.isPoolable();
	}

	@Override
	public void setCursorName(String arg0) throws SQLException {
		this.stmt.setCursorName(arg0);
	}

	@Override
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		this.stmt.setEscapeProcessing(arg0);
	}

	@Override
	public void setFetchDirection(int arg0) throws SQLException {
		this.stmt.setFetchDirection(arg0);
	}

	@Override
	public void setFetchSize(int arg0) throws SQLException {
		this.stmt.setFetchSize(arg0);
	}

	@Override
	public void setMaxFieldSize(int arg0) throws SQLException {
		this.stmt.setMaxFieldSize(arg0);
	}

	@Override
	public void setMaxRows(int arg0) throws SQLException {
		this.stmt.setMaxRows(arg0);
	}

	@Override
	public void setPoolable(boolean arg0) throws SQLException {
		this.stmt.setPoolable(arg0);
	}

	@Override
	public void setQueryTimeout(int arg0) throws SQLException {
		this.stmt.setQueryTimeout(arg0);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.stmt.isWrapperFor(iface);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return this.stmt.unwrap(iface);
	}

	@Override
	public void addBatch() throws SQLException {
		this.stmt.addBatch();
	}

	@Override
	public void clearParameters() throws SQLException {
		this.stmt.clearParameters();
	}

	@Override
	public boolean execute() throws SQLException {

		return this.stmt.execute();
	}

	@Override
	public ResultSet executeQuery() throws SQLException {

		// System.out.println("ExecuteQuery(): "+this.input_query);
		return this.executeQuery(
				this.generateQueryString(this.input_query, this.param_list),
				true);
	}

	@Override
	public int executeUpdate() throws SQLException {
		return this.executeUpdate(this.generateQueryString(this.input_query,
				this.param_list));
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {

		return this.stmt.getMetaData();
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {

		return this.stmt.getParameterMetaData();
	}

	@Override
	public void setArray(int arg0, Array arg1) throws SQLException {

		this.stmt.setArray(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {

		this.stmt.setBigDecimal(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void setBlob(int arg0, Blob arg1) throws SQLException {

		this.stmt.setBlob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBlob(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setBlob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBlob(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setBlob(arg0, arg1, arg2);
	}

	@Override
	public void setBoolean(int arg0, boolean arg1) throws SQLException {

		this.stmt.setBoolean(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setByte(int arg0, byte arg1) throws SQLException {

		this.stmt.setByte(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBytes(int arg0, byte[] arg1) throws SQLException {

		this.stmt.setBytes(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1) throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1, int arg2)
			throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setClob(int arg0, Clob arg1) throws SQLException {

		this.stmt.setClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setClob(int arg0, Reader arg1) throws SQLException {

		this.stmt.setClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {

		this.stmt.setClob(arg0, arg1, arg2);
	}

	@Override
	public void setDate(int arg0, Date arg1) throws SQLException {

		this.stmt.setDate(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {

		this.stmt.setDate(arg0, arg1, arg2);

	}

	@Override
	public void setDouble(int arg0, double arg1) throws SQLException {

		this.stmt.setDouble(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setFloat(int arg0, float arg1) throws SQLException {

		this.stmt.setFloat(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setInt(int arg0, int arg1) throws SQLException {
		this.stmt.setInt(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setLong(int arg0, long arg1) throws SQLException {

		this.stmt.setLong(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {

		this.stmt.setNCharacterStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {

		this.stmt.setNCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setNClob(int arg0, NClob arg1) throws SQLException {

		this.stmt.setNClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNClob(int arg0, Reader arg1) throws SQLException {

		this.stmt.setNClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {

		this.stmt.setNClob(arg0, arg1, arg2);
	}

	@Override
	public void setNString(int arg0, String arg1) throws SQLException {

		this.stmt.setNString(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNull(int arg0, int arg1) throws SQLException {

		this.stmt.setNull(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNull(int arg0, int arg1, String arg2) throws SQLException {

		this.stmt.setNull(arg0, arg1, arg2);
	}

	@Override
	public void setObject(int arg0, Object arg1) throws SQLException {

		this.stmt.setObject(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setObject(int arg0, Object arg1, int arg2) throws SQLException {

		this.stmt.setObject(arg0, arg1, arg2);
	}

	@Override
	public void setObject(int arg0, Object arg1, int arg2, int arg3)
			throws SQLException {

		this.stmt.setObject(arg0, arg1, arg2, arg3);
	}

	@Override
	public void setRef(int arg0, Ref arg1) throws SQLException {

		this.stmt.setRef(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setRowId(int arg0, RowId arg1) throws SQLException {

		this.stmt.setRowId(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {

		this.stmt.setSQLXML(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setShort(int arg0, short arg1) throws SQLException {

		this.stmt.setShort(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setString(int arg0, String arg1) throws SQLException {

		this.stmt.setString(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTime(int arg0, Time arg1) throws SQLException {

		this.stmt.setTime(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {

		this.stmt.setTime(arg0, arg1, arg2);
	}

	@Override
	public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {

		this.stmt.setTimestamp(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2)
			throws SQLException {

		this.stmt.setTimestamp(arg0, arg1, arg2);
	}

	@Override
	public void setURL(int arg0, URL arg1) throws SQLException {

		this.stmt.setURL(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setUnicodeStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setUnicodeStream(arg0, arg1, arg2);
	}

	@Override
	public void closeOnCompletion() throws SQLException {

	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

}
