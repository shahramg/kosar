package com.mitrallc.kosar;

import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.mitrallc.mysqltrig.MySQLQueryToTrigger;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.ResultSet;
import com.mitrallc.sqltrig.OracleQueryToTrigger;
import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class Kosar {
	private static boolean IssuingTriggerCMDS=false;
	private static boolean gumball = false;
	private static boolean camp = true;

	private static final int NumTriggerListenerThreads = 1;
	public static int NumFragments = 101;
	private static long Delta = 31;
	private static long inflation = 2; // factor of 2
	private static long Tadjust = 0;
	@SuppressWarnings("unchecked")
	public static ConcurrentHashMap<String, dust>[] RS = (ConcurrentHashMap<String, dust>[])new ConcurrentHashMap[NumFragments];

	@SuppressWarnings("unchecked")
	public static ConcurrentHashMap<String, InternalTokenElement>[] ITs = (ConcurrentHashMap<String, InternalTokenElement>[])new ConcurrentHashMap[NumFragments]; // Its type is really
	
	public static ConcurrentHashMap<String, String[]> keysToIks = new ConcurrentHashMap<>();

	public static java.sql.Connection db_conn = null;
//	private com.mitrallc.sql.ResultSet a = null;

	public static QueryToTrigger qt = null;

	private static boolean verbose = false;

	private static int MonitorFreq = 10 * 1000;
	
//	public static ConcurrentHashMap<String, String> cached = new ConcurrentHashMap<String, String>();

	public static Semaphore StartTrigListeners = new Semaphore(0, true);
	public static Stats KosarMonitor = new Stats(MonitorFreq);

	public static KosarListener[] kosarListeners = new KosarListener[NumTriggerListenerThreads];

	// this is the replacement technique module
	public static ReplacementTechnique RP = new ReplacementTechnique(KosarSoloDriver.CacheSize);

	public static boolean isIssuingTrigCMDS() {
		return IssuingTriggerCMDS;
	}

	public static void setIssuingTrigCMDS(boolean issuingCMDS) {
		IssuingTriggerCMDS = issuingCMDS;
	}

	public static int getFragment(String key) {
		if (NumFragments <= 0) {
			return -1;
		}

		/*
		 * int retHash = 5381; int len = 0;
		 * 
		 * while (len < key.length()) { retHash = ((retHash << 5) + retHash) +
		 * key.charAt(len); len++; }
		 * 
		 * if (retHash < 0) { retHash = -retHash; }
		 */
		
//		int len = 0;
//		int hcode = 0;
//		while (len < key.length()) {
//			hcode = (hcode << 19) - hcode + key.charAt(len);
//			len++;
//		}
//		
//		hcode = hcode < 0 ? ((~hcode) + 1) : hcode;
//		hcode = hcode % NumFragments;
//		
//		return hcode;

		int hash = key.hashCode();
		
		hash = hash < 0 ? ((~hash) + 1) : hash;
		return hash % NumFragments;
	}

	public void DeleteIT(String internalToken) {
		InternalTokenElement ite = ITs[getFragment(internalToken)].get(internalToken);
		if (ite == null)
			if (gumball) {
				ite = new InternalTokenElement();
				ite.setGumball();			
				ite = ITs[getFragment(internalToken)].putIfAbsent(internalToken, ite);
				if (ite != null) {
					ite.setGumball();
				}
			}

		if (ite != null) {
			for (String qry: ite.getQueryStringKeySet()) {
				KosarSoloDriver.getLockManager().lock(qry);
				DeleteDust(qry);
				KosarSoloDriver.getLockManager().unlock(qry);				
			}
		}
	}

	/***
	 * This method removes a dust by eliminating all the pointers to it:
	 * 1.  Removes it from the RS
	 * 2.  Removes it from the replacement policy
	 * 3.  Removes it from the QTmeta data structure that tracks instances of a query template 
	 */
	public static void DeleteDust(dust elt){
		DeleteCachedQry(elt.getKey());
		if (camp)
			RP.DeleteKV(elt, getFragment(elt.getKey()));
//		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
//		qtelt.deleteQInstance(elt.getKey());
	}

	public static void DeleteDust(String qry){
		int fragid = getFragment(qry);
		dust elt = RS[fragid].get(qry);

		//This delete may reference a dust that does not exist
		if (elt == null) return;

		if (verbose)
			System.out.println("Delete " + qry);

		DeleteCachedQry(qry);

		//Remove from the replacement policy data structure
		if (camp)
			RP.DeleteKV(elt, fragid);

		//Remove from a list of the query instances for the template of this query
//		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
//		if (qtelt != null) qtelt.deleteQInstance(elt.getKey());
	}

	public static dust DeleteCachedQry(String qry) {
		int fragid = getFragment(qry);
		dust ds = RS[fragid].get(qry);

		if(ds == null) {
			//ERROR, the following line should be removed
			//FIX in response to the statement above:  ds = new dust();
			//REMOVE ABOVE TWO LINES AFTER EXTENSIVE TESTING
//			System.out.println("kosar.java:  DeleteCachedQry, element is null.");
		}
		else {
			//ERROR, the ds element must be removed from RS to free memory
			//FIX in response to the statement above: ds.setRS(null);
			//REMOVE ABOVE TWO LINES AFTER EXTENSIVE TESTING
			if (KosarSoloDriver.webServer != null)
				KosarSoloDriver.KosarInvalidatedKeysEventMonitor.newEvent(1);			
			
			if (verbose)
				System.out.println("Delete cached qry="+qry);
			((ConcurrentHashMap<String, dust>) RS[fragid]).remove(qry);

			//			if (KosarSoloDriver.webServer != null) 
			//				KosarSoloDriver.KosarInvalidatedKeysEventMonitor.newEvent(1);
		}
		return ds;
	}

	public static void clearCache() {
		if (verbose) System.out.println("Flushing the KVS.");
		KosarSoloDriver.kosarEnabled = false;
		for (int i = 0; i < NumFragments; i++) {
			if (RS[i] != null)
				RS[i].clear();
			if (ITs[i] != null)
				ITs[i].clear();
		}
		
		if (camp)
			RP = new ReplacementTechnique();
		
//		QueryToTrigger.FlushQTQI();

		if (KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarKeysCachedEventMonitor.reset();

		//KosarSoloDriver.kosarEnabled = true;
		System.gc();
	}

	public com.mitrallc.sql.ResultSet GetQueryResult(String qry) {
		com.mitrallc.sql.ResultSet myres = null;
		int fragid = getFragment(qry);
		KosarMonitor.IncrementNumReqs();
		// If qry exists then return resultset
		// If either ki-gi exists or no entry exists then return null with Tmiss
		// timestamp.		
		dust ds = RS[fragid].get(qry);

		if( ds != null) {
			//If the query template of this query instance has been disabled then do not serve using the KVS
			QTmeta qtelt = QueryToTrigger.TriggerCache.get(ds.getQueryTemplate());
			if (!qtelt.CacheQryInstances()) {
				System.out.println("Cache disabled");
				return null;
			}

			com.mitrallc.sql.ResultSet localDS = ds.getRS();
			if(localDS != null){
				KosarMonitor.IncrementNumHit();
				myres = new com.mitrallc.sql.ResultSet(localDS);
//				myres = localDS;
				
				if (camp)
					RP.RegisterHit(ds, fragid);

				//Register stats:  Increment the number of query instances for this template
				if (KosarSoloDriver.webServer != null){
//					QTmeta qtm = QueryToTrigger.TriggerCache.get(ds.getQueryTemplate());
//					qtm.addKVSHits();
					qtelt.addKVSHits();
				}
			}
		}
		return myres;
	}

	public boolean attemptToCache(String sql, com.mitrallc.sql.ResultSet rs,
			long Tmiss) {
		if (verbose)
			System.out.println("Qry: " + sql);

		int fragid = getFragment(sql);

		dust ds = RS[fragid].get(sql);

		/******  Get Internal Keylist ******/
		String ParamQry = qt.TokenizeWhereClause(sql);
		if (ParamQry == null)
			ParamQry = sql; // qry has no where clause

		QTmeta qtm = new QTmeta();
		qtm.setQueryTemplate(ParamQry);
		QTmeta oqtm = QueryToTrigger.TriggerCache.putIfAbsent(ParamQry,qtm);		
		if (oqtm != null) {
			qtm = oqtm;
		}

		if (!qtm.CacheQryInstances()) {
			System.out.println("Query template is disabled");
			return false; //Query template is disabled; return without trying to cache
		}

		if (KosarSoloDriver.flags.coordinatorExists() || qtm.isTriggersRegistered()){
			// The triggers associated with the query have already been
			// registered.
			// Insert ki-vi with its time stamp set to Tmiss.
			boolean stored = putInCache(sql, ds, fragid, Tmiss, rs, ParamQry);

			//Register stats:  Increment the number of query instances for this template
			if (KosarSoloDriver.webServer != null){
//				qtm = QueryToTrigger.TriggerCache.get(ParamQry);
				qtm.addNumQueryInstances();
			}

			return stored;
		} else {
//			System.out.println("Core not exist or trigger is not registered.");
			if (!qtm.isTrigsInProgress()){
				regthread.AddQry(sql); //Add the query to be registered
				qtm.setTrigsInProgress(true);  //Mark the query template so that it is not inserted over and over again
			}
			
			return false;
		}
	}

	public Kosar(String url, Properties arg1, java.sql.Connection conn) {
		// Initialize kosar's connection to the RDBMS
		db_conn = conn;
		// start the thread to register triggers
		for (int i = 0; i < NumFragments; i++) {
			RS[i] = new ConcurrentHashMap<String, dust>();
			if (RS[i] == null) {
				System.out
				.println("KosarSolo:  Failed to initialize a hashmap for (IT, key).");
				return;
			}
		}

		for (int i = 0; i < NumFragments; i++) {
			ITs[i] = new ConcurrentHashMap<String, InternalTokenElement>();
			if (ITs[i] == null) {
				System.out
				.println("KosarSolo:  Failed to initialize a hashmap for ITs.");
				return;
			}
		}

		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			qt = new MySQLQueryToTrigger();
			break;
		case Oracle:
			qt = new OracleQueryToTrigger();
			break;
		default: 
			System.out.println("Error in kosar constructor:  RDBMS type is unknown");
			System.out.println("Check KosarSoloDriver to verify it reads the RDBMS type from the configuration file and sets the type of the RDBMS");
			System.out.println("Aboring, cannot proceed forward");
			System.exit(-1);
			break;
		}

		if (qt == null) {
			System.out
			.println("KosarSolo:  Failed to obtain a QueryToTrigger() instance:  Expect stale data.");
			return;
		}

		/*if (KosarSoloDriver.getFlags().coordinatorExists()) {
			synchronized (QueryToTrigger.class) {
				if (QueryToTrigger.getIPport() == null
						|| QueryToTrigger.getIPport().equals("")) {
					new TriggerListener().setIpPortAddress();
				}
			}

		} else {*/
		if (!KosarSoloDriver.flags.coordinatorExists()) {
			for (int j = 0; j < NumTriggerListenerThreads; j++) {
				KosarListener TL = new KosarListener(this, false); // Start the
				kosarListeners[j] = TL;
				// registration
				// background
				// thread
				new Thread(TL, "KosarListener" + j).start();
				try {
					StartTrigListeners.acquire(); // Wait for the
					// TriggerListeners to start
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
			}
		} else {
			KosarListener TL = new KosarListener(this, true);
			kosarListeners[0] = TL;
			
			new Thread(TL, "KosarListener").start();
			try {
				StartTrigListeners.acquire(); // Wait for the
				// TriggerListeners to start
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
			}
		}
		//}

		synchronized (KosarMonitor) {
			if (!KosarMonitor.isAlive())
				KosarMonitor.start();
		}

		return;
	}

	public void addKeyToInternalTokenElt(String sql) {
		// get list of tokens
		String[] tokenList = Kosar.getIks(sql);
		
		HashSet<String> uniqueTokens = new HashSet<String>();
		for (String token : tokenList) {			
			if (uniqueTokens.contains(token)) {
				continue;
			}
			uniqueTokens.add(token);
			
			int fragId = getFragment(token);			
			InternalTokenElement placeHolderITE = new InternalTokenElement();
			placeHolderITE.getQueryStringMap().put(sql, "1");
			
			placeHolderITE = ITs[fragId].putIfAbsent(token, placeHolderITE);

			if (placeHolderITE != null) {
				placeHolderITE.getQueryStringMap().putIfAbsent(sql, "1");
			}			
		}
	}

	public boolean putInCache(String sql, dust ds, int fragid, long Tmiss, ResultSet rs, String QueryTemplate) {
		long TC = System.currentTimeMillis();
		int ElapsedTime = (int)(TC-Tmiss);


		// If (TC-Tmiss > Delta) then ignore the put operation
		if (gumball && ElapsedTime >= Delta) {
			Tadjust = System.currentTimeMillis();
			Delta = inflation * (ElapsedTime);
			return false;
		}

		// If (Tmiss < Tadjust) then ignore the put operation.
		if (gumball && Tmiss <= Tadjust) {
			return false;
		}

		//Get Internal keys
		String[] tokenList = Kosar.getIks(sql);

		if (verbose){
			System.out.println("qry="+sql+", "+tokenList.length+" tokens");
			for (int i=0; i < tokenList.length; i++){
				System.out.println("\t Token="+tokenList[i]);
			}
		}

		if (gumball) {
			for (int i=0; i < tokenList.length; i++) {
				String token = tokenList[i];
				InternalTokenElement ite = ITs[getFragment(token)].get(token);
				
				// If (gi exists and Tmiss is before Tgi) then ignore the put operation
				if (ite != null && ite.isGumball() && Tmiss <= ite.getGumballTS()) {
					return false;
				}
				// If (vi exists and its time stamp is after Tmiss) then ignore the put
				// operation.
				if (ite != null && !ite.isGumball() && ds != null && Tmiss <= ds.getLastWrite()) {
					return false;
				}
			}
		}

		if (ds == null)
			ds = new dust();
		ds.setKey(sql);
		ds.setSize(rs.getSz());
		ds.setRS(rs);
		ds.setLastWrite(Tmiss);
		ds.setQueryTemplate(QueryTemplate);
		ds.SetInitialCost(ElapsedTime);
		// Associate internal keys with the query string

		//Try to insert in the cache.  If there is insufficient memory then do not cache.
		if (camp)
			if (!RP.InsertKV(ds, fragid)) {
				return false;
			}

//		/*
//		 * Iterate through internal key list
//		 */
//		for (int i=0; i < tokenList.length; i++) {
//			String token = tokenList[i];
//			
//			int fid = getFragment(token);
//
//			InternalTokenElement placeHolderITE = new InternalTokenElement(); //create new ITE and new map
//			placeHolderITE.getQueryStringMap().put(sql, "1");
//
//			placeHolderITE = ITs[fid].putIfAbsent(token, placeHolderITE);
//			
//			if (placeHolderITE != null) {
//				placeHolderITE.getQueryStringMap().putIfAbsent(sql, "1");				
//			}
//			
//			if (verbose)
//				System.out.println("Caching IT="+token+", qry="+sql);
//		}

		KosarMonitor.IncrementNumKeyValues();
		// Place the query and its result in the hash table.
		RS[fragid].put(sql, ds);
//		if (cached.get(sql) == null) {
//			cached.put(sql, "1");
//		} else {
//			System.out.println("Cached before");
//		}
		
		//Maintain this query instance as a cached entry for its template
//		if (ds == null) System.out.println("Error:  ds is NULL");
//		if (ds.getQueryTemplate() == null) System.out.println("Error:  ds method QueryTemplate returned NULL!");
//		else {
//			QTmeta qtelt = QueryToTrigger.TriggerCache.get( ds.getQueryTemplate() );
//			if (qtelt != null) qtelt.setQInstances(sql, ds);
//			else System.out.println("Error, qtelt is null");
//		}
		
		return true;
	}

	//	/**
	//	 * This getInternalTokensFromQry must be replaced with the GetKey method of QueryToTrigger for consistency across all SQL RDBMSs.
	//	 * @param qry
	//	 * @return
	//	 */
	//
	//	public Vector<String> getInternalTokensFromQry(String qry) {
	//		/******  Get Internal Keylist ******/
	//		String ParamQry = qt.TokenizeWhereClause(qry);
	//		if (ParamQry == null)
	//			ParamQry = qry; // qry has no where clause
	//
	//		Vector<String> internal_key_list = new Vector<String>();
	//		Vector<String> trgr = new Vector<String>();
	//
	//		qt.TransformQuery(qry, trgr, internal_key_list, regthread.db_conn);
	//		/***********************************/
	//
	//
	//		return internal_key_list;
	//	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public Vector<String> getInternalTokensFromQry(String qry) {
		/******  Get Internal Keylist ******/
		String ParamQry = qt.TokenizeWhereClause(qry);
		if (ParamQry == null)
			ParamQry = qry; // qry has no where clause

		Vector<String> internal_key_list = new Vector<String>();
		Vector<String> trgr = new Vector<String>();

		qt.TransformQuery(qry, trgr, internal_key_list, regthread.db_conn);
		/***********************************/


		return internal_key_list;
	}
	
	public static String[] getIks(String key) {
//		String queryTemplate = Kosar.qt.TokenizeWhereClause(key);
		
		String[] tokenList = keysToIks.get(key);
		
		if (tokenList == null) {
			// get tokens
			StringBuffer COSARKey = new StringBuffer();
			Kosar.qt.GetKey(key, COSARKey, Kosar.db_conn);

			tokenList = COSARKey.toString().trim().split(" ");	
			
			keysToIks.putIfAbsent(key, tokenList);
		}
		
		return tokenList;
	}
}
