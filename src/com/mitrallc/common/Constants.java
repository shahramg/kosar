package com.mitrallc.common;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class contains constants for message codes and duration of lease, ping
 * intervals etc.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class Constants {
	public static final ReadWriteLock filewriter = new ReentrantReadWriteLock();
	public static final Lock FILE_WRITE_LOCK = filewriter.writeLock();
	
	public static final ReadWriteLock keyQueueRWLock = new ReentrantReadWriteLock();
	public static final Lock KEY_QUEUE_READ_LOCK = keyQueueRWLock.readLock();
	public static final Lock KEY_QUEUE_WRITE_LOCK = keyQueueRWLock.writeLock();
//	public static final Semaphore keyQueueLock = new Semaphore(0, true);
	
	public static final ReadWriteLock pendingTransactionQueueRWLock = new ReentrantReadWriteLock();
	public static final Lock PENDING_TRANSACTION_READ_LOCK = pendingTransactionQueueRWLock.readLock();
	public static final Lock PENDING_TRANSACTION_WRITE_LOCK = pendingTransactionQueueRWLock.writeLock();
//	public static final Semaphore pendingTransactionQueueLock = new Semaphore(0, true); 
	
	public static final AtomicLong AI = new AtomicLong();
	
	//Delta is the interval between Pings in milliseconds
	public static long delta = 60000; 
	public static int consistencyMode = 0;
	//Epsilon is the maximum clock skew in milliseconds
	public static long epsilon = 0; 
	public static long defaultLeaseDuration = 60000;
	//This is the sleeptime between reconnection attempts
	public static final long sleepTime = 50; 
	public static final long RECONNECT_TIME = 1000;
	/*
	 * The following are client message codes. These are used in the Message ID
	 * field of the messages sent by the client to the coordinator
	 */
	public static final int CLIENT_ALL_OK = 0;
	public static final int CLIENT_RECONNECT = 2;
	public static final int CLIENT_KEY_CACHED = 4;
	public static final int CLIENT_KEY_EVICTED = 5;
	public static final int CLIENT_GET_LEASE = 6;
	public static final int CLIENT_REG_TRIGGER = 7;
	public static final int CLIENT_WR_START = 8;
	public static final int CLIENT_WR_STOP = 9;
	
	public static final int CLIENT_PING = 10;
	public static final int CLIENT_REGISTER = 200;	
	public static final int CLIENT_ACQUIRE_I_LEASE = 1;
	public static final int CLIENT_RELEASE_I_LEASE = 2;
	public static final int CLIENT_ACQUIRE_Q_LEASE = 3;
	public static final int CLIENT_COMMIT_TID = 4;
	public static final int CLIENT_ABORT_TID = 5;
	
	public static final int IQ = 0;
	public static final int NO_IQ = 1;
	public static final int I_ONLY = 2;

	/*
	 * The following are client message codes. These are used in the Message ID
	 * field of the messages sent by the client to the coordinator
	 */

	/* Multi-purpose Positive ACK message */
	public static final int COORDINATOR_ALL_OK = 0;
	/*
	 * Replies for Register Reconnect - COORDINATOR_ALL_OK included in this
	 * category
	 */
	public static final int COORDINATOR_CLEAR_CACHE = 1;
	public static final int COORDINATOR_CLEAN_CACHE = 2;
	public static final int COORDINATOR_ASSIGN_ID = 3;

	/* Replies for Lookup Message */
	public static final int COORDINATOR_LEASE_GRANTED = 0;
	public static final int COORDINATOR_BACKOFF = 1;
	
	public static final int REPLY_I_LEASE_GRANTED = 0;
	public static final int REPLY_I_LEASE_RETRY = -1;
	public static final int REPLY_LEASE_RELEASE = 0;
	public static final int REPLY_LEASE_ABORTED = -1;
	public static final int REPLY_STOLEN_SUCCESS = 1;
	public static final int REPLY_CONSUME_SUCCESS = 2;
	
	public static final int STORED= 1;
	public static final int NOT_STORED = 2;
	
	public static final int CLIENT_RELEASE_I_STEAL = 104;
	public static final int CLIENT_RELEASE_I_USEONCE = 105;
	
	public static final int CMD_INVALIDATE = 3;
	public static final int CMD_STEAL = 101;
	public static final int CMD_COPY = 102;
	public static final int CMD_USEONCE = 103;	
	
	public static final int REP_OK = 0;
	public static final int REP_REJECT = -105;
	public static final int REP_NOVAL = -101;	
}
