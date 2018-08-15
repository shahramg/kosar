/**
 * 
 */
package com.mitrallc.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
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
import java.util.Calendar;
import java.util.Map;
import com.mitrallc.common.Constants;
import com.mitrallc.communication.CoreConnector;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;

/**
 * @author Shahram Ghandeharizadeh
 *
 */
public class CallableStatement implements java.sql.CallableStatement {
	private java.sql.CallableStatement clbstmt;
	private com.mitrallc.sql.Connection conn;
	private String input_StoredProcedure;
	private Object[] param_list;

	private static long queryStartTime;
	private static long cmdStartTime;
	
	private CoreConnector coreConnector = new CoreConnector();

	/**
	 * 
	 */
	public CallableStatement() {
		// TODO Auto-generated constructor stub
	}

	CallableStatement(String StoredProcedure, java.sql.CallableStatement pstmt,
			com.mitrallc.sql.Connection conn) {
		this.clbstmt = pstmt;
		this.conn = conn;
		this.input_StoredProcedure = StoredProcedure;

		this.param_list = new Object[PreparedStatement.parseNumParams(StoredProcedure)];
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#addBatch()
	 */
	@Override
	public void addBatch() throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#clearParameters()
	 */
	@Override
	public void clearParameters() throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean executeDML() throws SQLException {

		boolean retVal = true;

		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(this.input_StoredProcedure);

		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();

		if (KosarSoloDriver.kosarEnabled) {
			if (KosarSoloDriver.getFlags().coordinatorExists()) {
				retVal = this.clbstmt.execute();
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
				retVal = this.clbstmt.execute();

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
			retVal = this.clbstmt.execute();
		}
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add("Stored Procedure (Unknown) "+PreparedStatement.generateQueryString(this.input_StoredProcedure, this.param_list), false);
		}
		return retVal;		
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#execute()
	 */
	@Override
	public boolean execute() throws SQLException {
		int transId = KosarSoloDriver.transId.incrementAndGet();
		String s = PreparedStatement.generateQueryString(this.input_StoredProcedure, this.param_list);
		s=s.replaceAll(" ", "");
		if (s.startsWith("{call"))
			s = s.substring(5);
		if (s.endsWith("}"))
			s = s.substring(0, s.length()-1);

		SQLException exc = null;
		int isCommit = -1;
		boolean success = false;
		try {
			java.sql.CallableStatement proc = conn.prepareCall("{ call DO_DML(?,?,?,?) }");
			proc.setString(1, s);

			long id = KosarSoloDriver.clientData.getID();		
			proc.setLong(2, id);
			proc.setInt(3, transId);
			proc.registerOutParameter(4, Types.INTEGER);
			success = ((CallableStatement)proc).executeDML();
			isCommit = proc.getInt(4);
		} catch (SQLException e) {
			exc = e;
		}
		
		coreConnector.releaseQLease2(transId, isCommit);

		if (exc != null) {
			throw exc;
		} else if (isCommit != 0){
			throw new KosarSQLException("Transaction aborted.");
		}

		return success;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#executeQuery()
	 */
	@Override
	public ResultSet executeQuery() throws SQLException {
		ResultSet retVal = null;

		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(this.input_StoredProcedure);

		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();

		if (KosarSoloDriver.kosarEnabled) {
			if (KosarSoloDriver.getFlags().coordinatorExists()) {
				if (KosarSoloDriver.getFlags().isCoordinatorConnected()) {
					retVal = this.clbstmt.executeQuery();
				} else {
					retVal = this.clbstmt.executeQuery();
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
				retVal = this.clbstmt.executeQuery();

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
			retVal = this.clbstmt.executeQuery();
		}
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add("Stored Procedure (Query): "+PreparedStatement.generateQueryString(this.input_StoredProcedure, this.param_list), false);
		}
		return retVal;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#executeUpdate()
	 */
	@Override
	public int executeUpdate() throws SQLException {
		int retVal = 0;

		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(this.input_StoredProcedure);

		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();

		if (KosarSoloDriver.kosarEnabled) {
			if (KosarSoloDriver.getFlags().coordinatorExists()) {
				if (KosarSoloDriver.getFlags().isCoordinatorConnected()) {
					retVal = this.clbstmt.executeUpdate();
				} else {
					retVal = this.clbstmt.executeUpdate();
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
				retVal = this.clbstmt.executeUpdate();

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
			retVal = this.clbstmt.executeUpdate();
		}
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add("Stored Procedure (Update): "+PreparedStatement.generateQueryString(this.input_StoredProcedure, this.param_list) , false);
		}
		return retVal;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#getMetaData()
	 */
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return this.clbstmt.getMetaData();
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#getParameterMetaData()
	 */
	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return this.clbstmt.getParameterMetaData();
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setArray(int, java.sql.Array)
	 */
	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		this.clbstmt.setArray(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream)
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x)
			throws SQLException {
		this.clbstmt.setAsciiStream(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, long)
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)
	 */
	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream)
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, int)
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, long)
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
	 */
	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBlob(int, java.io.InputStream)
	 */
	@Override
	public void setBlob(int parameterIndex, InputStream inputStream)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBlob(int, java.io.InputStream, long)
	 */
	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBoolean(int, boolean)
	 */
	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setByte(int, byte)
	 */
	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setBytes(int, byte[])
	 */
	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader)
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, long)
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader,
			long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
	 */
	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		this.clbstmt.setClob(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setClob(int, java.io.Reader)
	 */
	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		this.clbstmt.setClob(parameterIndex, reader);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setClob(int, java.io.Reader, long)
	 */
	@Override
	public void setClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		this.clbstmt.setClob(parameterIndex, reader, length);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setDate(int, java.sql.Date)
	 */
	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		this.clbstmt.setDate(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar)
	 */
	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		this.clbstmt.setDate(parameterIndex, x, cal);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setDouble(int, double)
	 */
	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		this.clbstmt.setDouble(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setFloat(int, float)
	 */
	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		this.clbstmt.setFloat(parameterIndex, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setInt(int, int)
	 */
	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		this.clbstmt.setInt(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setLong(int, long)
	 */
	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		this.clbstmt.setLong(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNCharacterStream(int, java.io.Reader)
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value)
			throws SQLException {
		this.clbstmt.setNCharacterStream(parameterIndex, value);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=value;

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNCharacterStream(int, java.io.Reader, long)
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value,
			long length) throws SQLException {
		this.clbstmt.setNCharacterStream(parameterIndex, value);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=value;

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNClob(int, java.sql.NClob)
	 */
	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		this.clbstmt.setNClob(parameterIndex, value);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=value;

	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNClob(int, java.io.Reader)
	 */
	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		this.clbstmt.setNClob(parameterIndex, reader);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=reader;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNClob(int, java.io.Reader, long)
	 */
	@Override
	public void setNClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		this.clbstmt.setNClob(parameterIndex, reader, length);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=reader;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNString(int, java.lang.String)
	 */
	@Override
	public void setNString(int parameterIndex, String value)
			throws SQLException {
		this.clbstmt.setNString(parameterIndex, value);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=value;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNull(int, int)
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		this.clbstmt.setNull(parameterIndex, sqlType);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=sqlType;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setNull(int, int, java.lang.String)
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName)
			throws SQLException {
		this.clbstmt.setNull(parameterIndex, sqlType, typeName);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=sqlType;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setObject(int, java.lang.Object)
	 */
	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		this.clbstmt.setObject(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int)
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		this.clbstmt.setObject(parameterIndex, x, targetSqlType);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int)
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scaleOrLength) throws SQLException {
		this.clbstmt.setObject(parameterIndex, x, targetSqlType);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setRef(int, java.sql.Ref)
	 */
	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		this.clbstmt.setRef(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setRowId(int, java.sql.RowId)
	 */
	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		this.clbstmt.setRowId(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setSQLXML(int, java.sql.SQLXML)
	 */
	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject)
			throws SQLException {
		this.clbstmt.setSQLXML(parameterIndex, xmlObject);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=xmlObject;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setShort(int, short)
	 */
	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		this.clbstmt.setShort(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setString(int, java.lang.String)
	 */
	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		this.clbstmt.setString(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setTime(int, java.sql.Time)
	 */
	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		this.clbstmt.setTime(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)
	 */
	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		this.clbstmt.setTime(parameterIndex, x, cal);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		this.clbstmt.setTimestamp(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		this.clbstmt.setTimestamp(parameterIndex, x, cal);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setURL(int, java.net.URL)
	 */
	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		this.clbstmt.setURL(parameterIndex, x);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int)
	 */
	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		this.clbstmt.setUnicodeStream(parameterIndex, x, length);
		if (parameterIndex > 0 && parameterIndex <= param_list.length)
			param_list[parameterIndex-1]=x;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#addBatch(java.lang.String)
	 */
	@Override
	public void addBatch(String arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#cancel()
	 */
	@Override
	public void cancel() throws SQLException {
		this.clbstmt.cancel();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#clearBatch()
	 */
	@Override
	public void clearBatch() throws SQLException {
		this.clbstmt.clearBatch();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#clearWarnings()
	 */
	@Override
	public void clearWarnings() throws SQLException {
		this.clbstmt.clearWarnings();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#close()
	 */
	@Override
	public void close() throws SQLException {
		this.clbstmt.close();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#closeOnCompletion()
	 */
	@Override
	public void closeOnCompletion() throws SQLException {
		this.clbstmt.closeOnCompletion();

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#execute(java.lang.String)
	 */
	@Override
	public boolean execute(String arg0) throws SQLException {
		this.clbstmt.execute(arg0);
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#execute(java.lang.String, int)
	 */
	@Override
	public boolean execute(String arg0, int arg1) throws SQLException {
		this.clbstmt.execute(arg0, arg1);
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#execute(java.lang.String, int[])
	 */
	@Override
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		this.clbstmt.execute(arg0, arg1);
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		this.clbstmt.execute(arg0, arg1);
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeBatch()
	 */
	@Override
	public int[] executeBatch() throws SQLException {
		this.clbstmt.executeBatch();
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeQuery(java.lang.String)
	 */
	@Override
	public ResultSet executeQuery(String arg0) throws SQLException {
		this.clbstmt.executeQuery(arg0);
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeUpdate(java.lang.String)
	 */
	@Override
	public int executeUpdate(String arg0) throws SQLException {
		this.clbstmt.executeUpdate(arg0);
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeUpdate(java.lang.String, int)
	 */
	@Override
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		this.clbstmt.executeUpdate(arg0, arg1);
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
	 */
	@Override
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		this.clbstmt.executeUpdate(arg0, arg1);
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
	 */
	@Override
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		this.clbstmt.executeUpdate(arg0, arg1);
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return this.conn;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getFetchDirection()
	 */
	@Override
	public int getFetchDirection() throws SQLException {
		this.clbstmt.getFetchDirection();
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getFetchSize()
	 */
	@Override
	public int getFetchSize() throws SQLException {
		this.clbstmt.getFetchSize();
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getGeneratedKeys()
	 */
	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		this.clbstmt.getGeneratedKeys();
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getMaxFieldSize()
	 */
	@Override
	public int getMaxFieldSize() throws SQLException {
		this.clbstmt.getMaxFieldSize();
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getMaxRows()
	 */
	@Override
	public int getMaxRows() throws SQLException {
		this.clbstmt.getMaxRows();
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getMoreResults()
	 */
	@Override
	public boolean getMoreResults() throws SQLException {
		this.clbstmt.getMoreResults();
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getMoreResults(int)
	 */
	@Override
	public boolean getMoreResults(int arg0) throws SQLException {
		this.clbstmt.getMoreResults(arg0);
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getQueryTimeout()
	 */
	@Override
	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getResultSet()
	 */
	@Override
	public ResultSet getResultSet() throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.getResultSet();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getResultSetConcurrency()
	 */
	@Override
	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getResultSetHoldability()
	 */
	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getResultSetType()
	 */
	@Override
	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getUpdateCount()
	 */
	@Override
	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#isCloseOnCompletion()
	 */
	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.isCloseOnCompletion();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#isClosed()
	 */
	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.isClosed();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#isPoolable()
	 */
	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.isPoolable();
	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setCursorName(java.lang.String)
	 */
	@Override
	public void setCursorName(String arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setEscapeProcessing(boolean)
	 */
	@Override
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setFetchDirection(int)
	 */
	@Override
	public void setFetchDirection(int arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	@Override
	public void setFetchSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setMaxFieldSize(int)
	 */
	@Override
	public void setMaxFieldSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setMaxRows(int)
	 */
	@Override
	public void setMaxRows(int arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setPoolable(boolean)
	 */
	@Override
	public void setPoolable(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	@Override
	public void setQueryTimeout(int arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getArray(int)
	 */
	@Override
	public Array getArray(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getArray(java.lang.String)
	 */
	@Override
	public Array getArray(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBigDecimal(int)
	 */
	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBigDecimal(java.lang.String)
	 */
	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.getBigDecimal(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBigDecimal(int, int)
	 */
	@Override
	public BigDecimal getBigDecimal(int parameterIndex, int scale)
			throws SQLException {
		// TODO Auto-generated method stub
		return this.clbstmt.getBigDecimal(parameterIndex, scale);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBlob(int)
	 */
	@Override
	public Blob getBlob(int parameterIndex) throws SQLException {
		return this.clbstmt.getBlob(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBlob(java.lang.String)
	 */
	@Override
	public Blob getBlob(String parameterName) throws SQLException {
		return this.clbstmt.getBlob(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBoolean(int)
	 */
	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException {
		return this.clbstmt.getBoolean(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBoolean(java.lang.String)
	 */
	@Override
	public boolean getBoolean(String parameterName) throws SQLException {
		return this.clbstmt.getBoolean(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getByte(int)
	 */
	@Override
	public byte getByte(int parameterIndex) throws SQLException {
		return this.clbstmt.getByte(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getByte(java.lang.String)
	 */
	@Override
	public byte getByte(String parameterName) throws SQLException {
		return this.clbstmt.getByte(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBytes(int)
	 */
	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException {
		return this.clbstmt.getBytes(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getBytes(java.lang.String)
	 */
	@Override
	public byte[] getBytes(String parameterName) throws SQLException {
		return this.clbstmt.getBytes(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getCharacterStream(int)
	 */
	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		return this.clbstmt.getCharacterStream(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getCharacterStream(java.lang.String)
	 */
	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getClob(int)
	 */
	@Override
	public Clob getClob(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getClob(java.lang.String)
	 */
	@Override
	public Clob getClob(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDate(int)
	 */
	@Override
	public Date getDate(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDate(java.lang.String)
	 */
	@Override
	public Date getDate(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDate(int, java.util.Calendar)
	 */
	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDate(java.lang.String, java.util.Calendar)
	 */
	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return this.clbstmt.getDate(parameterName, cal);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDouble(int)
	 */
	@Override
	public double getDouble(int parameterIndex) throws SQLException {
		return this.getDouble(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getDouble(java.lang.String)
	 */
	@Override
	public double getDouble(String parameterName) throws SQLException {
		return this.getDouble(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getFloat(int)
	 */
	@Override
	public float getFloat(int parameterIndex) throws SQLException {
		return this.getFloat(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getFloat(java.lang.String)
	 */
	@Override
	public float getFloat(String parameterName) throws SQLException {
		return this.clbstmt.getFloat(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getInt(int)
	 */
	@Override
	public int getInt(int parameterIndex) throws SQLException {
		return this.clbstmt.getInt(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getInt(java.lang.String)
	 */
	@Override
	public int getInt(String parameterName) throws SQLException {
		return this.clbstmt.getInt(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getLong(int)
	 */
	@Override
	public long getLong(int parameterIndex) throws SQLException {
		return this.clbstmt.getLong(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getLong(java.lang.String)
	 */
	@Override
	public long getLong(String parameterName) throws SQLException {
		return this.clbstmt.getLong(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNCharacterStream(int)
	 */
	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		return this.clbstmt.getNCharacterStream(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNCharacterStream(java.lang.String)
	 */
	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		return this.clbstmt.getNCharacterStream(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNClob(int)
	 */
	@Override
	public NClob getNClob(int parameterIndex) throws SQLException {
		return this.clbstmt.getNClob(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNClob(java.lang.String)
	 */
	@Override
	public NClob getNClob(String parameterName) throws SQLException {
		return this.clbstmt.getNClob(parameterName);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNString(int)
	 */
	@Override
	public String getNString(int parameterIndex) throws SQLException {
		return this.clbstmt.getNString(parameterIndex);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getNString(java.lang.String)
	 */
	@Override
	public String getNString(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(int)
	 */
	@Override
	public Object getObject(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(java.lang.String)
	 */
	@Override
	public Object getObject(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(int, java.util.Map)
	 */
	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(java.lang.String, java.util.Map)
	 */
	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(int, java.lang.Class)
	 */
	@Override
	public <T> T getObject(int parameterIndex, Class<T> type)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getObject(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> T getObject(String parameterName, Class<T> type)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getRef(int)
	 */
	@Override
	public Ref getRef(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getRef(java.lang.String)
	 */
	@Override
	public Ref getRef(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getRowId(int)
	 */
	@Override
	public RowId getRowId(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getRowId(java.lang.String)
	 */
	@Override
	public RowId getRowId(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getSQLXML(int)
	 */
	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getSQLXML(java.lang.String)
	 */
	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getShort(int)
	 */
	@Override
	public short getShort(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getShort(java.lang.String)
	 */
	@Override
	public short getShort(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getString(int)
	 */
	@Override
	public String getString(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getString(java.lang.String)
	 */
	@Override
	public String getString(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTime(int)
	 */
	@Override
	public Time getTime(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTime(java.lang.String)
	 */
	@Override
	public Time getTime(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTime(int, java.util.Calendar)
	 */
	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTime(java.lang.String, java.util.Calendar)
	 */
	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTimestamp(int)
	 */
	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTimestamp(java.lang.String)
	 */
	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTimestamp(int, java.util.Calendar)
	 */
	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getTimestamp(java.lang.String, java.util.Calendar)
	 */
	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getURL(int)
	 */
	@Override
	public URL getURL(int parameterIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#getURL(java.lang.String)
	 */
	@Override
	public URL getURL(String parameterName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(int, int)
	 */
	@Override
	public void registerOutParameter(int parameterIndex, int sqlType)
			throws SQLException {
		this.clbstmt.registerOutParameter(parameterIndex, sqlType);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(java.lang.String, int)
	 */
	@Override
	public void registerOutParameter(String parameterName, int sqlType)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(int, int, int)
	 */
	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(int, int, java.lang.String)
	 */
	@Override
	public void registerOutParameter(int parameterIndex, int sqlType,
			String typeName) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(java.lang.String, int, int)
	 */
	@Override
	public void registerOutParameter(String parameterName, int sqlType,
			int scale) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#registerOutParameter(java.lang.String, int, java.lang.String)
	 */
	@Override
	public void registerOutParameter(String parameterName, int sqlType,
			String typeName) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setAsciiStream(java.lang.String, java.io.InputStream)
	 */
	@Override
	public void setAsciiStream(String parameterName, InputStream x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setAsciiStream(java.lang.String, java.io.InputStream, int)
	 */
	@Override
	public void setAsciiStream(String parameterName, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setAsciiStream(java.lang.String, java.io.InputStream, long)
	 */
	@Override
	public void setAsciiStream(String parameterName, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBigDecimal(java.lang.String, java.math.BigDecimal)
	 */
	@Override
	public void setBigDecimal(String parameterName, BigDecimal x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBinaryStream(java.lang.String, java.io.InputStream)
	 */
	@Override
	public void setBinaryStream(String parameterName, InputStream x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBinaryStream(java.lang.String, java.io.InputStream, int)
	 */
	@Override
	public void setBinaryStream(String parameterName, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBinaryStream(java.lang.String, java.io.InputStream, long)
	 */
	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBlob(java.lang.String, java.sql.Blob)
	 */
	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBlob(java.lang.String, java.io.InputStream)
	 */
	@Override
	public void setBlob(String parameterName, InputStream inputStream)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBlob(java.lang.String, java.io.InputStream, long)
	 */
	@Override
	public void setBlob(String parameterName, InputStream inputStream,
			long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBoolean(java.lang.String, boolean)
	 */
	@Override
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setByte(java.lang.String, byte)
	 */
	@Override
	public void setByte(String parameterName, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setBytes(java.lang.String, byte[])
	 */
	@Override
	public void setBytes(String parameterName, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setCharacterStream(java.lang.String, java.io.Reader)
	 */
	@Override
	public void setCharacterStream(String parameterName, Reader reader)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setCharacterStream(java.lang.String, java.io.Reader, int)
	 */
	@Override
	public void setCharacterStream(String parameterName, Reader reader,
			int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setCharacterStream(java.lang.String, java.io.Reader, long)
	 */
	@Override
	public void setCharacterStream(String parameterName, Reader reader,
			long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setClob(java.lang.String, java.sql.Clob)
	 */
	@Override
	public void setClob(String parameterName, Clob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setClob(java.lang.String, java.io.Reader)
	 */
	@Override
	public void setClob(String parameterName, Reader reader)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setClob(java.lang.String, java.io.Reader, long)
	 */
	@Override
	public void setClob(String parameterName, Reader reader, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setDate(java.lang.String, java.sql.Date)
	 */
	@Override
	public void setDate(String parameterName, Date x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setDate(java.lang.String, java.sql.Date, java.util.Calendar)
	 */
	@Override
	public void setDate(String parameterName, Date x, Calendar cal)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setDouble(java.lang.String, double)
	 */
	@Override
	public void setDouble(String parameterName, double x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setFloat(java.lang.String, float)
	 */
	@Override
	public void setFloat(String parameterName, float x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setInt(java.lang.String, int)
	 */
	@Override
	public void setInt(String parameterName, int x) throws SQLException {
		this.clbstmt.setInt(parameterName, x);
	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setLong(java.lang.String, long)
	 */
	@Override
	public void setLong(String parameterName, long x) throws SQLException {
		this.clbstmt.setLong(parameterName, x);

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNCharacterStream(java.lang.String, java.io.Reader)
	 */
	@Override
	public void setNCharacterStream(String parameterName, Reader value)
			throws SQLException {
		this.clbstmt.setNCharacterStream(parameterName, value);

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNCharacterStream(java.lang.String, java.io.Reader, long)
	 */
	@Override
	public void setNCharacterStream(String parameterName, Reader value,
			long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNClob(java.lang.String, java.sql.NClob)
	 */
	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNClob(java.lang.String, java.io.Reader)
	 */
	@Override
	public void setNClob(String parameterName, Reader reader)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNClob(java.lang.String, java.io.Reader, long)
	 */
	@Override
	public void setNClob(String parameterName, Reader reader, long length)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNString(java.lang.String, java.lang.String)
	 */
	@Override
	public void setNString(String parameterName, String value)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNull(java.lang.String, int)
	 */
	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setNull(java.lang.String, int, java.lang.String)
	 */
	@Override
	public void setNull(String parameterName, int sqlType, String typeName)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setObject(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setObject(String parameterName, Object x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setObject(java.lang.String, java.lang.Object, int)
	 */
	@Override
	public void setObject(String parameterName, Object x, int targetSqlType)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setObject(java.lang.String, java.lang.Object, int, int)
	 */
	@Override
	public void setObject(String parameterName, Object x, int targetSqlType,
			int scale) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setRowId(java.lang.String, java.sql.RowId)
	 */
	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setSQLXML(java.lang.String, java.sql.SQLXML)
	 */
	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setShort(java.lang.String, short)
	 */
	@Override
	public void setShort(String parameterName, short x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setString(java.lang.String, java.lang.String)
	 */
	@Override
	public void setString(String parameterName, String x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setTime(java.lang.String, java.sql.Time)
	 */
	@Override
	public void setTime(String parameterName, Time x) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setTime(java.lang.String, java.sql.Time, java.util.Calendar)
	 */
	@Override
	public void setTime(String parameterName, Time x, Calendar cal)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setTimestamp(java.lang.String, java.sql.Timestamp)
	 */
	@Override
	public void setTimestamp(String parameterName, Timestamp x)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setTimestamp(java.lang.String, java.sql.Timestamp, java.util.Calendar)
	 */
	@Override
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#setURL(java.lang.String, java.net.URL)
	 */
	@Override
	public void setURL(String parameterName, URL val) throws SQLException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.sql.CallableStatement#wasNull()
	 */
	@Override
	public boolean wasNull() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
