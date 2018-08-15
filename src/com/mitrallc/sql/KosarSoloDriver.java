package com.mitrallc.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.mitrallc.sqltrig.QueryToTrigger;
import com.mitrallc.webserver.BaseSettings;
import com.mitrallc.webserver.EventMonitor;
import com.mitrallc.webserver.BaseHttpServer;
import com.mitrallc.webserver.Last100SQLQueries;
import com.mitrallc.webserver.MyHttpHandler;
import com.mitrallc.common.ClientDataStructures;
import com.mitrallc.common.Constants;
import com.mitrallc.common.DynamicArray;
import com.mitrallc.common.Flags;
import com.mitrallc.common.LockManager;
import com.mitrallc.communication.CacheModeController;
//import com.mitrallc.communication.ClientConnector;
import com.mitrallc.communication.CoreConnector;
import com.mitrallc.communication.MessageJob;
import com.mitrallc.communication.MessageWorker;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.kosar.KosarListener;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;

public class KosarSoloDriver implements Driver {
	Driver driver;
	//	public static boolean SimulationMode = true;
	public static String rdbmsdriver = "";
	public static String urlPrefix = "kosarsolo:";
	private static final boolean VERBOSE = false;
	public static Kosar Kache = null;
	private boolean KosarRegThread = false;
	private static regthread TRT;
	public Semaphore initSemaphore = new Semaphore(1, true);
	public static ClientDataStructures clientData = new ClientDataStructures();
	public static DynamicArray keyQueue;	
	public static DynamicArray pendingTransactionArray;
	//A KOSAR system with a core may only have this many replicas of a sql query string cached
	//at a given moment. This may be specified to -1, which dictates no limit.
	//This value is received from the CORE when the client registers itself with it.
	public static int numReplicas;

	public static boolean coordConnected = false;

	//	public static volatile ExecutorService triggerRegService = Executors
	//			.newSingleThreadExecutor();
//	private static volatile ExecutorService coordinatorReconnectService = Executors
//			.newSingleThreadExecutor();
	//	public static volatile ExecutorService keyCachingService = Executors
	//			.newCachedThreadPool();

//	private static volatile Future<Boolean> reconnectionCompleted = null;

	public static Flags flags = new Flags();
	private static LockManager lockManager = new LockManager();

	//	private static PingThread pingThread = null; 

	private static final String CORES = "cores";
	private static final String DBPORT = "dbport";
	private static final String INIT_CONNECTIONS = "initconnections";
	private static final String KOSAR_ENABLED = "kosarEnabled";
	private static final String WEBSERVERPORT = "webserverport";
	private static final String RDBMS = "rdbms";
	private static final String RDBMSDriver = "rdbmsdriver";
	private static final String CLIENTSPORT = "clientsport";
	public static BaseHttpServer webServer = null;

	public static EventMonitor KosarTriggerRegEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarKeysCachedEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarCacheHitsEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarEvictedKeysEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarQueryResponseTimeEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarDMLUpdateEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarRTEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarInvalidatedKeysEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarInvalKeysAttemptedEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	
	// total read time
	public static EventMonitor KosarReadTimeMonitor = new EventMonitor(BaseSettings.getGranularity());	
	// total read time if the value is found from another client
	public static EventMonitor KosarReadTimeFromOthersMonitor = new EventMonitor(BaseSettings.getGranularity());	
	// total read time if the value is computed from RDBMS
	public static EventMonitor KosarReadTimeFromDBMSMonitor = new EventMonitor(BaseSettings.getGranularity());	
	// total read time if the value cannot be found from another client, and finally needs to compute from DBMS
	public static EventMonitor KosarReadTimeFromOthersAndDBMSMonitor = new EventMonitor(BaseSettings.getGranularity());
	
	public static EventMonitor KosarNumAskAnotherClientEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarNumGotKeyFromAnotherClientEventMonitor = new EventMonitor(BaseSettings.getGranularity());

	// for IQ
	public static EventMonitor KosarILeaseGrantedEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarQLeaseReleasedEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarReadBackoffEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarQLeaseAbortEvntMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarILeaseReleasedEventMonitor = new EventMonitor(BaseSettings.getGranularity());

	public static Last100SQLQueries last100readQueries = new Last100SQLQueries();
	public static Last100SQLQueries last100updateQueries = new Last100SQLQueries();

	public static AtomicInteger copyCount = new AtomicInteger(0);
	public static AtomicInteger stealCount = new AtomicInteger(0);
	public static AtomicInteger useonceCount = new AtomicInteger(0);

	public static ConcurrentHashMap<String,Integer> cores;
	public static int webserverport;
	public static int server_port;
	public static int db_port;
	public static int init_connections;
	public static int min_connections;
	public static int max_connections;
	public static boolean kosarEnabled;
	public static long CacheSize = 512*1024*1024L;

	public static Random rand = new Random();

	public static int clientsport;
	public static AtomicInteger transId = new AtomicInteger(0);

	static boolean firsttime = true;
	//	public static long lowWaterMark = 1*1024*1024*1024; //1 GB
	//	public static long highWaterMark = 2*1024*1024; //2 MB

	private static AtomicLong UsedCacheSpace = new AtomicLong(0);

	// an array of connection pools to the cores
	private static SockIOPool[] connection_pools;

	//	private static final String cfgfile = "kosar.cfg";
	private static final String cfgfile = "kosarOracle.cfg";
	private static String CONFIG_FILE = "./"+cfgfile;

	public static boolean monitoring = false;

	// a hash map of connection pool to the clients
	public static ConcurrentHashMap<String, SockIOPool> clientPoolMap = new ConcurrentHashMap<String, SockIOPool>();

	public static ConcurrentLinkedQueue<MessageJob>[] msgJobQueue;
	public static MessageWorker[] msgWorkers;
	public static Semaphore[] msgJobSemaphores;

	public static ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> pendingTrans = new ConcurrentHashMap<>();

	public static void UseCacheSpace (long objsize){
		UsedCacheSpace.addAndGet(objsize);
	}

	public static long getCacheSize() {
		return CacheSize;
	}

	public static void setCacheSize(long cacheSize) {
		CacheSize = cacheSize;
		//		highWaterMark = lowWaterMark+CacheSize;
	}

	// ~ Static fields/initializers
	// ---------------------------------------------
	//
	// Register ourselves with the DriverManager
	//
	static {
		System.out.println("KosarSoloDriver");
		try {
			// load server details
			Properties serverProperties = new Properties();
			String sysEnvStr = System.getenv("KOSAR_HOME");
			if (sysEnvStr == null)
				sysEnvStr = System.getenv("kosar_home");
			if (sysEnvStr != null){
				sysEnvStr = sysEnvStr.trim();
				if (! sysEnvStr.endsWith(cfgfile)) CONFIG_FILE = sysEnvStr+"/"+cfgfile;
				else CONFIG_FILE=sysEnvStr;
				System.out.println("Config file "+CONFIG_FILE);
			}

			serverProperties.load(new FileInputStream(new File(CONFIG_FILE)));			
			kosarEnabled = ((serverProperties.getProperty(KOSAR_ENABLED) == null ? "false"
					: serverProperties.getProperty(KOSAR_ENABLED))
					.equals("true"));

			String rdbmstype = serverProperties.getProperty(RDBMS);
			if (rdbmstype != null && rdbmstype.compareToIgnoreCase("mysql")==0){
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.MySQL);
			} else if (rdbmstype != null && rdbmstype.compareToIgnoreCase("oracle")==0) {
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.Oracle);
			} else {
				System.out.println("KosarSoloDriver ERROR:  RDBMS type is either not defined or unknown.");
				System.out.println("KosarSoloDriver Suggested Fix:  Specify an rdbms tag with a target RDBMS in the configuration file, e.g., rdbms=mysql");
				System.out.println("KosarSoloDriver Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarSoloDriver Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying whether kosar is enabled, port number of the web server, listening port for clients, and RDBMS specs.");
				System.exit(-1);
			}

			rdbmsdriver = serverProperties.getProperty(RDBMSDriver);
			if (rdbmsdriver == null) {
				System.out.println("KosarSoloDriver ERROR:  RDBMS driver is not defined.");
				System.out.println("KosarSoloDriver Suggested Fix:  Specify an rdbms tag with a target RDBMS in the configuration file, e.g., rdbms=mysql");
				System.out.println("KosarSoloDriver Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarSoloDriver Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying whether kosar is enabled, port number of the web server, listening port for clients, and RDBMS specs.");
				System.exit(-1);
			}
			
			if(kosarEnabled) {
				// start the web server if the port is provided
				String port = serverProperties.getProperty(WEBSERVERPORT);
				if(port != null) {
					webserverport = Integer.decode(port);
					webServer = new BaseHttpServer(webserverport, "CLIENT", new MyHttpHandler());
				}
				
				// connect to Cores
				String coremachines = serverProperties.getProperty(CORES);				
				if (coremachines != null) {
					String[] cores_list = coremachines.split(";");
					cores = new ConcurrentHashMap<String, Integer>();
					for (String c: cores_list)
						cores.put(c, cores.size());
				}

				db_port = Integer
						.valueOf(serverProperties.getProperty(DBPORT) == null ? "0"
								: serverProperties.getProperty(DBPORT));
				init_connections = Integer.valueOf(serverProperties
						.getProperty(INIT_CONNECTIONS) == null ? "0"
								: serverProperties.getProperty(INIT_CONNECTIONS));
				clientsport = Integer.valueOf(serverProperties
						.getProperty(CLIENTSPORT) == null ? "-1"
								: serverProperties.getProperty(CLIENTSPORT));

				if (cores == null || cores.size() == 0) {
					System.out.println("KOSAR is enabled. Run without Core.");
					flags.setCoordinatorExists(false);
					CacheModeController.enableQueryCaching();

					/* If there is no coordinator and Kosar is enabled, the double delete step needs
					 * to be handled by the client. The following data structures: pendingTransactionList
					 * and keyQueue support this function. 
					 */
					pendingTransactionArray = new DynamicArray();
					keyQueue = new DynamicArray();
				} else {
					System.out.println("KOSAR is enabled. Run with Core.");
					flags.setCoordinatorExists(true);
				}
			} else {
				System.out.println("KOSAR is disabled. Run without cache.");
				flags.setCoordinatorExists(false);
				CacheModeController.disableQueryCaching();
			}

		} catch (FileNotFoundException f) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			f.printStackTrace(System.out);
			flags.setCoordinatorExists(false);
			CacheModeController.disableQueryCaching();
			throw new RuntimeException("Can't register driver!");
		} catch (IOException io) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			io.printStackTrace(System.out);
			flags.setCoordinatorExists(false);
			CacheModeController.disableQueryCaching();
			throw new RuntimeException("Can't register driver!");
		}
		catch (Exception E) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			throw new RuntimeException("Can't register driver!");
		}

		try {
			java.sql.DriverManager.registerDriver(new KosarSoloDriver());
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace(System.out);
			}
		}
		firsttime = true; //Reset the low water mark after the first connect because the amount of memory will change
	}

	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Construct a new driver and register it with DriverManager
	 * 
	 * @throws SQLException
	 *             if a database error occurs.
	 */
	public KosarSoloDriver() throws SQLException {
		// Required for Class.forName().newInstance()
		//this.driver = new oracle.jdbc.driver.OracleDriver();
		//this.driver = new com.mysql.jdbc.Driver();

		try {
			Object newObject = Class.forName(rdbmsdriver).newInstance();
			this.driver = (Driver) newObject;
		} catch (InstantiationException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");
			// TODO Auto-generated catch block
			e1.printStackTrace(System.out);
			System.exit(-1);
		} catch (IllegalAccessException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");

			e1.printStackTrace(System.out);
		} catch (ClassNotFoundException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");

			e1.printStackTrace(System.out);
		}

		if (VERBOSE)
			System.out.println("Initializing KosarSoloDriver");

	}

	@Override
	public boolean acceptsURL(String arg0) throws SQLException {
		if (VERBOSE)
			System.out.println("acceptsURL( " + arg0 + " )");

		String urlWithoutCOSAR = arg0;

		// Check if "cosar:" prefix is in the URL
		// If it is, strip the prefix and pass remaining URL on to underlying
		// driver
		int start = arg0.indexOf(urlPrefix);
		if (start >= 0) {
			urlWithoutCOSAR = arg0.substring(urlPrefix.length());
		} else {
			if (VERBOSE)
				System.out.println("Not a COSAR URL: " + arg0);
		}

		return this.driver.acceptsURL(urlWithoutCOSAR);
	}


	public static long CurrentCacheUsedSpace(boolean SimulationMode){
		return UsedCacheSpace.get();

		//		if (SimulationMode) return usage;
		//		
		//		long usedMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		//		long CurrentSize = usedMemory - lowWaterMark;
		//		System.out.println("usedMemory="+usedMemory+", lowWaterMark="+lowWaterMark);
		//		if (CurrentSize < 0) return 0;
		//
		//		return CurrentSize;
	}

	//	public static void SetLowMemoryWaterMark(){
	//		if (!firsttime) return;
	//		lowWaterMark = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	//		highWaterMark = lowWaterMark + CacheSize;
	//		System.out.println("KosarSoloDriver:  Set memory usage (low water mark) to "+lowWaterMark/(1024*1024)+" MB.");
	//		firsttime = false;
	//		return;
	//	}

	/***
	 * Attempts to create a wrapped version of a database connection to the
	 * given URL.
	 */
	@Override
	public Connection connect(String arg0, Properties arg1) throws SQLException {
		Connection conn = null;

		if (VERBOSE)
			System.out.println("connect( " + arg0 + " )");

		// Check if "kosarsolo:" prefix is in the URL
		// If it is, strip the prefix and pass remaining URL on to underlying
		// driver
		String urlWithoutCOSAR = arg0;
		int start = arg0.indexOf(urlPrefix);
		if (start >= 0) {
			urlWithoutCOSAR = arg0.substring(urlPrefix.length());
			if (VERBOSE)
				System.out.println("final connect to " + urlWithoutCOSAR);
		}
		
		if (!kosarEnabled) {
			try {
				conn =  this.driver.connect(urlWithoutCOSAR, arg1);
				if (conn != null) {
					return new com.mitrallc.sql.Connection(conn);
				} else {
					throw new SQLException("Cannot get connection.");
				}
			} catch (SQLException e) {				
				throw new KosarSQLException(e.getMessage());
			}
		}

		try {
			initSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

		// Connection for SQLTrig			
		try {
			conn = this.driver.connect(urlWithoutCOSAR, arg1);
		} catch (SQLException e) {
			initSemaphore.release();
			throw new KosarSQLException(e.getMessage());
		} 		

		if (!KosarRegThread && !flags.coordinatorExists()) {
			TRT = new regthread(conn);
			TRT.setName("RegThread");
			TRT.start();
			TRT.isRunning = true;
			KosarRegThread = true;
			regthread.OracleRegisterFunctions(conn);
		} else {
			if (regthread.isRegistered == false) {
				regthread.db_conn = conn;					
				regthread.OracleRegisterPackages(conn);
				regthread.OracleRegisterFunctions(conn);
				regthread.OracleRegisterProcs(conn);
				regthread.isRegistered = true;
			}
		}

		if (kosarEnabled && Kache == null) {
			//Allocate a connection for kosar
			try {
				conn = this.driver.connect(urlWithoutCOSAR, arg1);
				if (conn == null)
					System.out.println("\n\tKosarSoloDriver Error:  Failed to establish a db connection");
				Kache = new Kosar(urlWithoutCOSAR, arg1, conn);
			} catch (SQLException e) {
				initSemaphore.release();
				throw new KosarSQLException(e.getMessage());
			}				
		}

		try {
			conn = this.driver.connect(urlWithoutCOSAR, arg1);			
		} catch (SQLException e) {
			initSemaphore.release();
			throw new KosarSQLException(e.getMessage());
		}

		if (conn != null) {
			conn = new com.mitrallc.sql.Connection(conn);
		}

		if (cores != null && cores.size() > 0 && !coordConnected) {
			coordConnected = true;

			// initialize the connection pool
			connection_pools = new SockIOPool[cores.size()];	

			long clientid = -1;
			clientid = genID(KosarSoloDriver.clientData.getInvalidationPort());

			// Get the IP address of the client
			for (String core: cores.keySet()) {
				SockIOPool pool = new SockIOPool();
				pool.setServer(core);
				pool.setInitConn(KosarSoloDriver.init_connections+1);
				pool.initialize();

				connection_pools[cores.get(core)] = pool;

				// send register messages
				while (true) {
					try {
						SockIO sock = pool.getConnection();

						// generate a unique i

						// wait for reply in which coordinator sends id.
						// then store id and change port information that is stored.
						CoreConnector coreConnector = new CoreConnector();
						byte[] reply = coreConnector.register(sock, clientid,
								KosarSoloDriver.clientData.getInvalidationPort());

						// the reply structure
						// CLIENT_REGISTER client_id lease_term

						// gets the first 4 bytes for the client ID
						//						KosarSoloDriver.clientData.setID(ByteBuffer.wrap(Arrays.copyOfRange(reply, 4, 8)).getInt());
						KosarSoloDriver.clientData.setID(clientid);

						// get the next 4 bytes for the lease term
						Constants.delta = ByteBuffer.wrap(Arrays.copyOfRange(reply, reply.length-8,  reply.length-4)).getInt();
						Constants.consistencyMode = ByteBuffer.wrap(Arrays.copyOfRange(reply, reply.length-4,  reply.length)).getInt();
						
						String msg = "The system is runing in %s mode";
						switch (Constants.consistencyMode) {
						case Constants.IQ:
							System.out.println(String.format(msg, "IQ"));
							break;
						case Constants.I_ONLY:
							System.out.println(String.format(msg, "I_ONLY"));
							break;
						case Constants.NO_IQ:
							System.out.println(String.format(msg, "NO_IQ"));
							break;
						}

						System.out.print("Initial Client ID: "+KosarSoloDriver.clientData.getID());

						// enable query caching
						KosarSoloDriver.getFlags().setCoordinatorConnected(true);
						CacheModeController.enableQueryCaching();

						//						pingThread = new PingThread(idx, sock);
						//						if (!pingThread.isRunning()) {
						//							pingThread.start();
						//						}
						pool.checkIn(sock);
						break;
					} catch (ConnectException c) {
						System.out.println("connection exception");
						pool.shutdownPool();
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace(System.out);
						}
					} catch (SocketTimeoutException e) {
						System.out.println("socket timeout exception");
						pool.shutdownPool();
						try {
							Thread.sleep(50);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace(System.out);
						}
					} catch (IOException e) {
						System.out.println("io exception");
						pool.shutdownPool();
						try {
							Thread.sleep(50);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace(System.out);
						}
					}  finally {
						// do not checkIn the socket, since it is used by the Ping thread
						//						try {
						//							KosarSoloDriver.getConnectionPool().checkIn(socket);
						//						} catch (IOException e) {
						//							// TODO Auto-generated catch block
						//							e.printStackTrace();
						//						}
					}
				}
			}

			// start the message workers
			msgJobQueue = new ConcurrentLinkedQueue[Kosar.NumFragments];
			for (int i = 0; i < msgJobQueue.length; i++) {
				msgJobQueue[i] = new ConcurrentLinkedQueue<>();
			}

			msgJobSemaphores = new Semaphore[Kosar.NumFragments];
			for (int i = 0; i < msgJobSemaphores.length; i++) {
				msgJobSemaphores[i] = new Semaphore(0);
			}

			msgWorkers = new MessageWorker[Kosar.NumFragments*10];
			for (int i = 0; i < Kosar.NumFragments*10; i++) {
				MessageWorker w = new MessageWorker(msgJobQueue[i/10], msgJobSemaphores[i/10]);
				Thread msgWorkerThread = new Thread(w);
				msgWorkerThread.setName("MSGWorker");
				msgWorkerThread.start();
				msgWorkers[i] = w;
			}		
		}		

		initSemaphore.release();

		return conn;
	}

	public static int getNumReplicas() {
		return numReplicas;
	}

	public static void setNumReplicas(int numReplicas) {
		KosarSoloDriver.numReplicas = numReplicas;
	}

	@Override
	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
			throws SQLException {
		return this.driver.getPropertyInfo(arg0, arg1);
	}

	@Override
	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}

	public static SockIOPool getConnectionPool(int idx) {
		if (idx < 0 || idx >= connection_pools.length)
			return null;
		else
			return connection_pools[idx];
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	public static LockManager getLockManager() {
		return lockManager;
	}

	public static Flags getFlags() {
		return flags;
	}

	public static void setLockManager(Flags f) {
		KosarSoloDriver.flags = f;
	}

	public static String getCoreAddress() {
		if (cores != null && cores.size() > 0) {
			String s = "";
			for (String core : cores.keySet()) {
				s += core+" ";
			}
			return s;
		}

		return "";
	}

	public static int getCoreForKey(String key) {
		if (cores == null || cores.size() == 0)
			return -1;

		int hash = key.hashCode();
		hash = hash < 0 ? ((~hash) + 1) : hash;

		return hash % cores.size();
	}

	public static long genID(int port) {
		String ipaddr = null;
		try {
			ipaddr = getLocalHostLANAddress().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
			System.out.println("Error: Cannot get local IP address. GenID failed.");
			System.exit(-1);
		}

		// Parse IP parts into an int array
		int[] ip = new int[4];
		String[] parts = ipaddr.split("\\.");

		for (int i = 0; i < 4; i++) {
			ip[i] = Integer.parseInt(parts[i]);
		}

		// Add the above IP parts into an int number representing your IP 
		// in a 32-bit binary form
		long ipNumber = 0;
		for (int i = 0; i < 4; i++) {
			ipNumber += ip[i] << (24 - (8 * i));
		}

		ipNumber = ipNumber << 32;

		return ipNumber + (long)port;
	}

	/**
	 * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
	 * <p/>
	 * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
	 * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
	 * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
	 * specify the algorithm used to select the address returned under such circumstances, and will often return the
	 * loopback address, which is not valid for network communication. Details
	 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
	 * <p/>
	 * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
	 * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
	 * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
	 * first site-local address if the machine has more than one), but if the machine does not hold a site-local
	 * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
	 * <p/>
	 * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
	 * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
	 * <p/>
	 *
	 * @throws UnknownHostException If the LAN address of the machine cannot be found.
	 */
	private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
		try {
			InetAddress candidateAddress = null;
			// Iterate all NICs (network interface cards)...
			for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
				NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
				// Iterate all IP addresses assigned to each card...
				for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
					InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
					if (!inetAddr.isLoopbackAddress()) {

						if (inetAddr.isSiteLocalAddress()) {
							// Found non-loopback site-local address. Return it immediately...
							return inetAddr;
						}
						else if (candidateAddress == null) {
							// Found non-loopback address, but not necessarily site-local.
							// Store it as a candidate to be returned if site-local address is not subsequently found...
							candidateAddress = inetAddr;
							// Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
							// only the first. For subsequent iterations, candidate will be non-null.
						}
					}
				}
			}
			if (candidateAddress != null) {
				// We did not find a site-local address, but we found some other non-loopback address.
				// Server might have a non-site-local address assigned to its NIC (or it might be running
				// IPv6 which deprecates the "site-local" concept).
				// Return this non-loopback candidate address...
				return candidateAddress;
			}
			// At this point, we did not find a non-loopback address.
			// Fall back to returning whatever InetAddress.getLocalHost() returns...
			InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
			if (jdkSuppliedAddress == null) {
				throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
			}
			return jdkSuppliedAddress;
		}
		catch (Exception e) {
			UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
			unknownHostException.initCause(e);
			throw unknownHostException;
		}
	}	


	public static void closeSockets() {
		if (!kosarEnabled)
			return;
		
		Kache.KosarMonitor.done = true;
		
		if (!flags.coordinatorExists()) {
			TRT.isRunning = false;			
		}
		
		if (webServer != null) {
			webServer.stop();
		}

		if (msgWorkers != null) {
			for (MessageWorker msgWorker : msgWorkers) {
				msgWorker.stop();			
			}
		}

		if (Kache.kosarListeners != null) {
			for (int i = 0; i < Kache.kosarListeners.length; i++) {
				KosarListener kl = Kache.kosarListeners[i];
	
				if (kl != null)
					kl.closeSocket();
			}
		}
		
		// output stats
		PrintWriter writer;
		try {
			writer = new PrintWriter("cache-stats.txt", "UTF-8");
			writer.println("Num Cache hit: "+KosarCacheHitsEventMonitor.numberOfTotalEvents());
			writer.println("Num Query requests: "+KosarReadTimeMonitor.numberOfTotalEvents());
			writer.println("Num Evictions: "+KosarEvictedKeysEventMonitor.numberOfTotalEvents());
			writer.println("Total Query Response Time: "+KosarQueryResponseTimeEventMonitor.numberOfTotalEvents());
			writer.println("Num Reads: "+KosarReadTimeMonitor.numberOfTotalEvents());
			writer.println("Average time per read: "+ KosarReadTimeMonitor.getArevageTimePerEvent());
			writer.println("Average time read from others: "+ KosarReadTimeFromOthersMonitor.getArevageTimePerEvent());
			writer.println("Average time read from dbms: "+ KosarReadTimeFromDBMSMonitor.getArevageTimePerEvent());
			writer.println("Average time read from others then dbms: "+ KosarReadTimeFromOthersAndDBMSMonitor.getArevageTimePerEvent());
			writer.println("Num Updates: "+KosarDMLUpdateEventMonitor.numberOfTotalEvents());
			writer.println("Average time per update: "+ KosarDMLUpdateEventMonitor.getArevageTimePerEvent());
			writer.println("Total Roundtrip time (both reads and writes): "+KosarRTEventMonitor.numberOfTotalEvents());
			writer.println("Num Key Invalidated: "+KosarInvalidatedKeysEventMonitor.numberOfTotalEvents());
			writer.println("Total Invalidate attempted: "+KosarInvalKeysAttemptedEventMonitor.numberOfTotalEvents());
			writer.println("Num Ask other clients: "+KosarNumAskAnotherClientEventMonitor.numberOfTotalEvents());
			writer.println("Num Got from other clients: "+KosarNumGotKeyFromAnotherClientEventMonitor.numberOfTotalEvents());
			writer.println("Num I lease granted: "+KosarILeaseGrantedEventMonitor.numberOfTotalEvents());
			writer.println("Num I lease released: "+KosarILeaseReleasedEventMonitor.numberOfTotalEvents());
			writer.println("Num I lease backoff: "+KosarReadBackoffEventMonitor.numberOfTotalEvents());
			writer.println("Num Q lease granted: "+KosarQLeaseReleasedEventMonitor.numberOfTotalEvents());
			writer.println("Num Q lease aborted: "+KosarQLeaseAbortEvntMonitor.numberOfTotalEvents());
			writer.println("Num COPY: "+copyCount);
			writer.println("Num STEAL: "+stealCount);
			writer.println("Num USEONCE: "+useonceCount);

			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//	public static void startReconnectThread(long startReconnectTime) {
	//		synchronized (CoordinatorReconnector.class) {
	//			// each thread will send it's start time to the function
	//			// start reconnect thread only if start time is greater than the
	//			// last time reconnection was done successfully
	//			if (startReconnectTime > KosarSoloDriver.lastReconnectTime
	//					// and if any previous reconnection was completed
	//					&& (null == reconnectionCompleted || reconnectionCompleted
	//					.isDone())) {
	//				// coordinator is disconnected
	//				KosarSoloDriver.flags.setCoordinatorConnected(false);
	//				reconnectionCompleted = coordinatorReconnectService
	//						.submit(new CoordinatorReconnector());
	//			}
	//		}
	//	}
}
