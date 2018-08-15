package com.mitrallc.camp;

import java.util.HashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.System;

import com.mitrallc.kosar.BinaryHeap;
import com.mitrallc.camp.CAMPStats;
import com.mitrallc.kosar.HeapElt;
import com.mitrallc.kosar.HeapEltComparator;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.kosar.MultiModeLock;
import com.mitrallc.kosar.dust;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sqltrig.QTmeta;

public class CAMP {
	
	//public static final boolean SimulationMode = true;

	public long cacheSize;
	public AtomicLong currentCacheUsage = new AtomicLong(0);
	public AtomicLong cacheThreshold;
	public int NumThreads;
	public long totalTime = 0;
	public boolean verbose = true;
	public final int NumConcurrentHT = 29;
	
//	MultiModeLock MML = new MultiModeLock();

	AtomicLong MinPriority = new AtomicLong(0);
	AtomicLong MaxSize = new AtomicLong(1);

//	LRUQueue MinLRUQ = null;
	public Map<Integer,LRUQueue> LRUHT = new ConcurrentHashMap<Integer,LRUQueue>(); //HashTable of costsize to lruqueue;

	public BinaryHeap<HeapElt> PQ;

//	Map<String, dust> KVS = new ConcurrentHashMap<String, dust>();
	Comparator<HeapElt> comparator = new HeapEltComparator();

	HashMap<Double, Integer> minCostMap = new HashMap<Double, Integer>();
	double currentMinCost = Double.MAX_VALUE;
	
	@SuppressWarnings("unchecked")
	Map<Integer, MultiModeLock>[] QueueLocks = new ConcurrentHashMap[NumConcurrentHT];
	MultiModeLock[] QueueSemaphores = new MultiModeLock[NumConcurrentHT];

	CampRounding campRounding;
	Random randomGenerator = new Random(7);
	private double InsertionProbability=1.0;

	boolean usingProbability = false;
	boolean usingMinCost = false;
	public boolean weighted = true;
	
	public static final int ONLINE = 1, OFFLINE = 2;
	int mode = ONLINE;
	
	int maxCostSize = 1;
	Object lock = new Object();
	
	CAMParray mycamp;
	
//	ConcurrentLinkedQueue<LRUQueue> poolLRUQueues = new ConcurrentLinkedQueue<LRUQueue>();	
	

	public CAMP(CAMParray mycamp, long cacheSize, int precision, double probability, int mode){		
		this.mycamp = mycamp;
		this.cacheSize = cacheSize;
		this.mode = mode;
		cacheThreshold = new AtomicLong(cacheSize);
		campRounding = new CampRounding(precision);
		InsertionProbability = probability;

		PQ = new BinaryHeap<HeapElt>();

		for(int i = 0; i < NumConcurrentHT; i++){
			QueueSemaphores[i] = new MultiModeLock();
			QueueLocks[i] = new ConcurrentHashMap<Integer, MultiModeLock>();
		}
	}
	
	public void Display(){
		System.out.println("\t \t Number of elements "+GetNumberOfKeyItems());
		System.out.println("\t \t MinPriority " + MinPriority.get());
		PQ.Display();
	}

	private void AdjustMax(long NewSize){
		if (NewSize > MaxSize.get()) MaxSize.getAndSet(NewSize);
		return;
	}

	private int ComputeCost(double MySize, int MyCost){
		int result = (int) ((MaxSize.get() / MySize) * MyCost);
		return campRounding.RoundCost(result);
	}

	public void calculateMinCost(){

		double minCost = Double.MAX_VALUE;

		for (Map.Entry<Double, Integer> entry : minCostMap.entrySet())
		{
			if(entry.getKey() < minCost)
				minCost = entry.getKey();
		}

		currentMinCost = minCost;
	}
	
	private dust getFromKosar(String key) {
		int fragid = Kosar.getFragment(key);
		return Kosar.RS[fragid].get(key);
	}
	
	private boolean insertToKosar(String key, dust ds) {
		int fragid = Kosar.getFragment(key);
		Kosar.RS[fragid].put(key, ds);
		
//		String[] tokenList = Kosar.getIks(key);		
//		for (String token : tokenList) {
//			Kosar.ITs[Kosar.getFragment(token)].get(token).getQueryStringMap().put(key, "1");
//		}		
//		Kosar.keysToIks.remove(key);
		
		return true;	
	}
	
	private void removeFromKosar(String key) {
		int fragid = Kosar.getFragment(key);
		Kosar.RS[fragid].remove(key);
		
		// get tokens
//		String[] tokenList = Kosar.getIks(key);		
//		for (String token : tokenList) {
//			Kosar.ITs[Kosar.getFragment(token)].get(token).getQueryStringMap().remove(key);
//		}		
//		Kosar.keysToIks.remove(key);
	}
	
	public static dust CreateDustFromRequest(Request r){
		dust elt = new dust();
		elt.setKey(r.key);
		elt.setPayLoad( new char[r.size] );
		elt.setSize( r.size );
		elt.SetInitialCost(r.cost);
		return elt;
	}

//	/*
//	 * Nov 20, 2014
//	 * Shahram Ghandeharizadeh
//	 * The following is used with the simulator that uses trace files to generate requests
//	 * With this simulator, the first reference for a key-value pair is not counted as a 
//	 * cache miss because all such requests observe a miss.
//	*/
//	public void CAMP_Handle_Request(Request r, CAMPStats stats){
//
//		if(r.repeat && stats != null){
//			stats.IncrementNumReqs();
//			stats.IncrementTotalCost(r.cost);
//		}
//
//		if( r.size > cacheSize)
//		{
//			Print("Item too big for cache");
//			return;	
//		}
//
//		AdjustMax( r.size );
//
//
//		dust elt = CAMP_Lookup(r);
//
//		if(elt == null){ // not in cache
//			elt = CreateDustFromRequest(r);
//
//			double currentEltCost = ((double) r.cost) / r.size;
//
//			if(usingProbability){
//				if(currentCacheUsage.get() + r.size > cacheThreshold.get()){
//					double prob = randomGenerator.nextDouble();
//					if(prob < InsertionProbability )
//					{
//						if(r.repeat && stats != null){
//							stats.IncrementCostNotInCache(r.cost);
//							stats.IncrementNumMisses();
//						}
//						return;
//					}
//
//					MakeRoom(r.size, stats);
//
//				} 
//			}
//			else if(usingMinCost){
//				if(currentCacheUsage.get() + r.size > cacheThreshold.get()){
//					double prob = randomGenerator.nextDouble();
//					if(currentEltCost <= currentMinCost){
//						if(prob < InsertionProbability )
//						{
//							if(r.repeat && stats != null){
//								stats.IncrementCostNotInCache(r.cost);
//								stats.IncrementNumMisses();
//							}
//
//							return;
//						}
//					}
//
//					MakeRoom(r.size, stats);		
//				}	
//
//				Integer currentNum = minCostMap.get(currentEltCost);
//				if(currentNum == null)
//				{
//					minCostMap.put(currentEltCost, 1);		
//				}
//				else{
//					currentNum++;
//					minCostMap.put(currentEltCost, currentNum);
//				}
//
//				if(currentEltCost < currentMinCost){
//					currentMinCost = currentEltCost;
//				} 
//			}
//			else{
//				MakeRoom(r.size, stats);
//			}
//			CAMP_Insert(elt, stats);
//
//			if(r.repeat && stats != null){
//				stats.IncrementCostNotInCache(r.cost);
//				stats.IncrementNumMisses();
//			}
//		}
//		else{ // in cache	
//			CAMP_Update(elt, stats);
//			stats.IncrementNumHit();	
//		}
//	}
//
//	public dust CAMP_Lookup(Request r){
//
//		dust elt = KVS.get(r.key);
//		return elt;
//	}
	

//	public MultiModeLock GetLockForRoundedCost(int roundedcost){
//
//		int n = roundedcost % NumConcurrentHT;
//
//		MultiModeLock queueLock = QueueLocks[n].get(roundedcost);
//
//		try{
//			if(queueLock == null){
//				MultiModeLock ql = new MultiModeLock();
//
//				QueueSemaphores[n].acquire();
//
//				if( (queueLock = QueueLocks[n].get(roundedcost)) == null){
//					QueueLocks[n].put(roundedcost, ql);
//					queueLock = ql;
//				}
//				QueueSemaphores[n].release();
//			}
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//
//		return queueLock;
//	}

	public void CAMP_Insert( dust elt, CAMPStats stats ){
		// two global variables are used here (MaxSize and MinPriority)
		// do we need synchronization?
		int roundedcost = ComputeCost(elt.getSize(), elt.GetInitialCost());
		elt.setCostSize( roundedcost );
		elt.setPriority( MinPriority.get() + roundedcost );
//		System.out.println("MinPriority:"+ MinPriority.get());
		synchronized(lock) {
			maxCostSize  = maxCostSize < roundedcost ? roundedcost : maxCostSize;
		}
		
		MultiModeLock queueLock = this.GetLockForRoundedCost(elt.getCostSize());
		queueLock.acquire();
//		MML.acquire();
		
		dust checkElt = getFromKosar(elt.getKey());//KVS.get(elt.getKey());
		if (checkElt != null) {
//			System.out.println(cntInsertSameKey + ". CAMP_Insert: key-value pair already exists.");
//			MML.release();
			queueLock.release();
			if (stats != null)
				stats.cntInsertSameKey++;
			return;
		}		
		
		if (currentCacheUsage.get() + elt.getSize() > cacheThreshold.get()) {
//			System.out.println(cntInsertCacheFull + ". CAMP_Insert: No space available for this key.");
//			MML.release();
			queueLock.release();
			if (stats != null)
				stats.cntInsertCacheFull++;
			return;
		}

		insertToKosar(elt.getKey(), elt);//KVS.put(elt.getKey(), elt);		
		
		if (stats != null)
			stats.numBytesWritten += elt.getSize();

		LRUQueue LQ = LRUHT.get( elt.getCostSize() );
		if(LQ == null) {
//			long id = mycamp.next_number.incrementAndGet();			
//			LQ = mycamp.freeQueues.get(id);
//			if (LQ == null) {
//				long max_id = mycamp.max_allow.get();
//				while (max_id < id) {
//					max_id = mycamp.max_allow.incrementAndGet();
//					mycamp.freeQueues.put(max_id, new LRUQueue());
//				}
//				
//				// at this point, the LRUQueue for LQ should be available
//				while (LQ == null) {
//					LQ = mycamp.freeQueues.get(id);
//				}
//			}
//			// no longer keep the LRU queue in the free queues
//			mycamp.freeQueues.remove(id);
			LQ = new LRUQueue();
			
			if (stats != null)
				stats.IncrementLRUQueueCreations();
			LQ.setRoundedCost( elt.getCostSize() );
			LRUHT.put(elt.getCostSize(), LQ);			
		}

		LQ.add(elt);

		if ( LQ.getHeapEntry() == null )
		{
			HeapElt HN = new HeapElt(elt.getPriority());
			HN.setQueue(LQ);
			LQ.setHeapEntry(HN);

			if (stats != null) stats.IncrementHeapAccesses();
			PQ.add(HN);
			if (stats != null)
				stats.IncrementHeapAdds();
		}
		
		/* Manage Stats */
		currentCacheUsage.getAndAdd( elt.getSize() );
		KosarSoloDriver.UseCacheSpace(elt.getSize());

		queueLock.release();
//		MML.release();
	}
	
	public void CAMP_Update(dust elt, CAMPStats stats){
//		MML.acquire();
		int costSize = elt.getCostSize();		
		
		MultiModeLock queueLock = this.GetLockForRoundedCost(costSize);
		queueLock.acquire();

		LRUQueue queue = LRUHT.get(costSize);
		if(queue == null){
//			System.out.println(cntUpdateLRUQueueNotFound + ". CAMP_Update:  LRUQueue is null");			
			queueLock.release();
//			MML.release();
			if (stats != null)
				stats.cntUpdateLRUQueueNotFound++;
			return;
		}

		HeapElt heapElt = queue.getHeapEntry();
		assert(heapElt != null);
		if(heapElt == null) {
			System.out.println("Error in CAMP_Update: Heap Element in deletion is null");
			System.exit(-1);
			queueLock.release();			
			return;
		}

		dust checkElt = getFromKosar(elt.getKey());//KVS.get(elt.getKey());
		if (checkElt == null) {
			queueLock.release();
			if (stats != null)
				stats.cntUpdateKeyNotFound++;
//			System.out.println(cntUpdateKeyNotFound + ". CAMP_Update: Failed to remove element.");	
//			MML.release();
			return;
		}

		boolean removed = queue.remove(elt);
		if(!removed){
			System.out.println("Error in CAMP_Update: Fail to remove element from queue");
			System.exit(-1);
			queueLock.release();
//			MML.release();
			return;
		}

		if(queue.isEmpty()){	
			LRUQueue removedQueue = LRUHT.remove(queue.getRoundedCost());
			if (stats != null)
				stats.IncrementLRUQueueDeletions();

			if(removedQueue == null){
				System.out.println("Error: Queue could not be removed from hashtable");
			}
			
			if (stats != null) stats.IncrementHeapAccesses();
			boolean removedFirstElt = PQ.remove(heapElt);
			if (stats != null)
			stats.IncrementHeapRemovals();
			heapElt.setQueue(null);
			removedQueue.setHeapEntry(null);
			
//			removedQueue.reset();			
//			long id = mycamp.max_allow.incrementAndGet();
//			mycamp.freeQueues.put(id, removedQueue);

			if(!removedFirstElt)
				System.out.println("UPdate Error: First Heap Element not properly removed");
		}
		else{
			dust firstEltInQueue = queue.peek();
			if(elt.getPriority() != firstEltInQueue.getPriority()){
				PQ.ChangeEltValue(heapElt, firstEltInQueue.getPriority());
				if (stats != null)
					stats.IncrementHeapChanges();
				if (stats != null) stats.IncrementHeapAccesses();
			}
		}
		
		removeFromKosar(elt.getKey());//KVS.remove(elt.getKey());				

		currentCacheUsage.addAndGet(-elt.getSize());
		KosarSoloDriver.UseCacheSpace(-elt.getSize());
//		HeapElt front = PQ.peek();
//		if (front != null)
//			MinPriority.set(front.getPriority());
		
		queueLock.release();
		
		/* Insert the key into the appropriate queue */
		
		int roundedcost = ComputeCost(elt.getSize(), elt.GetInitialCost());
		elt.setCostSize( roundedcost );
		elt.setPriority( MinPriority.get() + roundedcost );
		synchronized (lock) {
			maxCostSize = maxCostSize < roundedcost ? roundedcost : maxCostSize;
		}

		queueLock = this.GetLockForRoundedCost(roundedcost);
		queueLock.acquire();
		
		if (currentCacheUsage.get() + elt.getSize() > cacheThreshold.get()) {
//			System.out.println(cntInsertCacheFull + ". CAMP_Insert: No space available for this key.");
//			MML.release();
			queueLock.release();
			if (stats != null)
				stats.cntInsertCacheFull++;
			return;
		}
		
		dust check = getFromKosar(elt.getKey());//KVS.get(elt.getKey());
		if (check != null) {
			queueLock.release();
			if (stats != null)
				stats.cntInsertSameKey++;
			return;
		}
		
		LRUQueue LQ;
		LQ = LRUHT.get( elt.getCostSize() );
		if(LQ == null){
//			long id = mycamp.next_number.incrementAndGet();			
//			LQ = mycamp.freeQueues.get(id);
//			if (LQ == null) {
//				long max_id = mycamp.max_allow.get();
//				while (max_id < id) {
//					max_id = mycamp.max_allow.incrementAndGet();
//					mycamp.freeQueues.put(max_id, new LRUQueue());
//				}
//				
//				while (LQ == null) {
//					LQ = mycamp.freeQueues.get(id);
//				}			
//			}			
//			mycamp.freeQueues.remove(id);
			LQ = new LRUQueue();
			
			if (stats != null) stats.IncrementLRUQueueCreations();
			LQ.setRoundedCost( elt.getCostSize() );
			LRUHT.put(elt.getCostSize(), LQ);			
		}

		LQ.add(elt);

		if ( LQ.getHeapEntry() == null )
		{
			HeapElt HN = new HeapElt(elt.getPriority());
			HN.setQueue(LQ);
			LQ.setHeapEntry(HN);

			if (stats != null) stats.IncrementHeapAccesses();
			PQ.add(HN);
			if (stats != null) stats.IncrementHeapAdds();
		}
		
		insertToKosar(elt.getKey(), elt);//KVS.put(elt.getKey(), elt);

		/* Manage Stats */
		currentCacheUsage.getAndAdd( elt.getSize() );
		KosarSoloDriver.UseCacheSpace(elt.getSize());
		
		queueLock.release();

//		MML.release();
	}

	public void CAMP_Maint_Delete(dust deleteElt, CAMPStats stats){
//		MML.acquire();
		int costSize = deleteElt.getCostSize();
		
		MultiModeLock queueLock = this.GetLockForRoundedCost(costSize);
		queueLock.acquire();

		LRUQueue queue = LRUHT.get(costSize);
		if(queue == null){
			//queueLock.release();
//			System.out.println("Error in CAMP_Maint_Delete:  LRUQueue is null");
			queueLock.release();
//			MML.release();
			return;
		}

		HeapElt heapElt = queue.getHeapEntry();
		if(heapElt == null)
			System.out.println("Error in CAMP_Maint_Delete: Heap Element in deletion is null");


		boolean removed = queue.remove(deleteElt);
		if(!removed){
			//queueLock.release();
			System.out.println("Error in CAMP_Maint_Delete: Failed to remove element.");
			queueLock.release();
//			MML.release();
			return;
		}


		//if (KosarSoloDriver.SimulationMode){
			removeFromKosar(deleteElt.getKey());//KVS.remove(deleteElt.getKey());
		//}	

		if(queue.isEmpty()){
			if (stats != null) stats.IncrementHeapAccesses();
			boolean removedFirstElt = PQ.remove(heapElt);
			if (stats != null) stats.IncrementHeapRemovals();
			heapElt.setQueue(null);

			LRUQueue removedQueue = LRUHT.remove(queue.getRoundedCost());
			if (stats != null) stats.IncrementLRUQueueDeletions();

			if(removedQueue == null){
				System.out.println("Error: Queue could not be removed from hashtable");
			}
			removedQueue.setHeapEntry(null);

			if(!removedFirstElt)
				System.out.println("Error: First Heap Element not properly removed");
		}
		else{

			dust firstEltInQueue = queue.peek();
			if(deleteElt.getPriority() != firstEltInQueue.getPriority()){
				PQ.ChangeEltValue(heapElt, firstEltInQueue.getPriority());
				if (stats != null) stats.IncrementHeapChanges();
				if (stats != null) stats.IncrementHeapAccesses();
			}
		}

		currentCacheUsage.addAndGet(-deleteElt.getSize());
		KosarSoloDriver.UseCacheSpace(-deleteElt.getSize());
		queueLock.release();
//		MML.release();
	}
	
	
	public dust CAMP_Delete( CAMPStats stats){
		MultiModeLock queueLock = null;
		LRUQueue queue=null;
		
////		MML.acquire();
//		if (verbose) System.out.println("CAMP_Delete:  Current cache usage "+currentCacheUsage.get());
		if (currentCacheUsage.get() < 1){
			// cache is empty
			stats.cntDeleteCacheEmpty++;
//			MML.release();
			return null; //cannot handle less than one element in the queue
		}	

		while(true){
			try{
				queue = PQ.peek().getQueue();
				if (queue != null) break;
				else if (verbose) System.out.println("Error, queue is null in CAMP_Delete");

//				queueLock = this.GetLockForRoundedCost(queue.getRoundedCost());
//
//				if(queueLock.tryAcquire())
//					break;
			} catch(NullPointerException e){ // could be caught because queue could be changed
				if (verbose) System.out.println("Exception in CAMP_Delete with currentCacheUsage "+currentCacheUsage.get());
//				MML.release();
//				return null; //cannot handle less than one element in the queue
//				if (queue == null) System.out.println("queue is null.");
//				System.exit(-1);
//				System.out.println(e.getMessage());
			}
		}
		
		// lock the queue based-on its cost-size
		queueLock = this.GetLockForRoundedCost(queue.getRoundedCost());		
		queueLock.acquire();

		dust deleteElt = queue.peek();
		if(deleteElt == null){
//			System.out.println("Error in CAMP_Delete:  deleteElt is null with queue consisting of "+queue.size()+" elements");
//			Display();
			//Thread.dumpStack();
			queueLock.release();
			stats.cntDeleteKeyNotFound++;
//			MML.release();
			return null;
		}

		HeapElt heapElt = queue.getHeapEntry();
		if(heapElt == null){
			System.out.println("Error: Heap Element in deletion is null");
			System.exit(-1);
			queueLock.release();
//			MML.release();
			return null;
		}
		
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarEvictedKeysEventMonitor.newEvent(1);
		
		removeFromKosar(deleteElt.getKey());//KVS.remove(deleteElt.getKey());
		
//		if (!KosarSoloDriver.SimulationMode){
			com.mitrallc.kosar.Kosar.DeleteCachedQry(deleteElt.getKey());

			//Remove the element from an instance of the Query Template
//			QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(deleteElt.getQueryTemplate());
//			qtelt.deleteQInstance(deleteElt.getKey());
//		}


		// remote the key from the queue
		queue.remove(deleteElt);

		if (stats != null) stats.IncrementEvictions();

		if(usingMinCost){
			double removedEltCost = ((double) deleteElt.GetInitialCost()) / deleteElt.getSize();
			Integer currentNum = this.minCostMap.get(removedEltCost);

			if(currentNum == 0)
				System.out.println("Error");

			currentNum--;
			if(currentNum == 0){
				minCostMap.remove(removedEltCost);
				if(removedEltCost == currentMinCost){
					calculateMinCost();
				}
			} 
		}
		
		if(queue.isEmpty()){
			LRUQueue removedQueue = LRUHT.remove(queue.getRoundedCost());
			if (stats != null)
				stats.IncrementLRUQueueDeletions();

			if(removedQueue == null){
				System.out.println("Error: Queue could not be removed from hashtable");
				System.exit(-1);
				queueLock.release();
//				MML.release();
				return deleteElt;
			}

			if (stats != null) stats.IncrementHeapAccesses();
			boolean removedFirstElt = PQ.remove(heapElt);
			if (stats != null)
				stats.IncrementHeapRemoveTop();
			heapElt.setQueue(null);
			removedQueue.setHeapEntry(null);
			
			// reset and put it into the free queues
//			removedQueue.reset();			
//			long id = mycamp.max_allow.incrementAndGet();
//			mycamp.freeQueues.put(id, removedQueue);			

			if(!removedFirstElt){
				System.out.println("Delete Error: First Heap Element not properly removed");
				System.exit(-1);
				queueLock.release();
//				MML.release();
				return deleteElt;
			}
		} else{
			dust firstEltInQueue = queue.peek();

			if(deleteElt.getPriority() != firstEltInQueue.getPriority()){
				if (stats != null) stats.IncrementHeapAccesses();				
				PQ.ChangeEltValue(heapElt, firstEltInQueue.getPriority());
				if (stats != null)
					stats.IncrementHeapChanges();
			}
		}
		
		HeapElt front = PQ.peek();
		if (front != null)
			MinPriority.set(front.getPriority());

		currentCacheUsage.addAndGet(-deleteElt.getSize());
		KosarSoloDriver.UseCacheSpace(-deleteElt.getSize());

		queueLock.release();
//		MML.release();
		return deleteElt;
	}

	public dust CAMP_RemoveLowestPriority(CAMPStats stats){

		dust Elt=null;

		while(Elt == null){
			try{ // Due to race conditions, must use try/catch if pointers become null
				Elt = CAMP_Delete(stats);
			} catch(Exception e){
				System.out.println("Caught error in RemoveLowest Priority");
				e.printStackTrace(System.out);
			}
		}
		return Elt;
	}

	public void MakeRoom( long newElementSize, CAMPStats stats){
		while(currentCacheUsage.get() + newElementSize > cacheThreshold.get()){
			CAMP_RemoveLowestPriority(stats);
		}	
	}

	public void Print(String str){
		if(verbose){
			System.out.println(str);
		}
	}

	public int GetNumberOfKeyItems(){

		int sum = 0;

		Object [] heapElts = PQ.array;

		for(Object elt : heapElts){
			if(elt == null)
				continue;
			HeapElt heapElt = (HeapElt) elt;
			LRUQueue queue = heapElt.getQueue();
			if(queue == null){
				System.out.println("Error in CAMP.GetNumberOfKeyItems:  A valid heap element with a null queue.");
				continue;
			}

			sum += queue.size();
		}

		return sum;
	}

	public void PrintWaited(long startTime, long endTime, String place){

		long threshold = 5000;
		long duration = endTime - startTime;
		if(duration > threshold)
			System.out.println("Waited for " + place + " for " + duration);

	}

	public dust GetKey(String key, CAMPStats stats) {	
		dust elt = getFromKosar(key);//KVS.get(key);
		
		if (elt != null) {		
			// cache hit, update key priority
			if (mode == ONLINE) {
				CAMP_Update(elt, stats);
			}
		}
		
		return elt;
	}
	
	public double calculateWeightedProb(Random rng, dust elt) {
		double prob = rng.nextDouble();
		
		if (!weighted) {
			return prob;
		} else {
			double currentEltCost = ComputeCost(elt.getSize(), elt.GetInitialCost());
			return prob * maxCostSize / currentEltCost;
		}
	}

	public boolean InsertKey(dust elt, CAMPStats stats) {		
		if (mode == OFFLINE) {
			return true;
		}
		
//		AdjustMax( elt.getSize() );
//		double currentEltCost = ((double) elt.GetInitialCost()) / elt.getSize();
		double currentEltCost = ComputeCost(elt.getSize(), elt.GetInitialCost());

		// decides whether to make room for this key-value pair
		if(usingProbability){
			if(currentCacheUsage.get() + elt.getSize() > cacheThreshold.get()){			
//				double insertionProb = InsertionProbability * currentEltCost / maxCostSize;
//				if(prob < insertionProb )				
				double prob = calculateWeightedProb(randomGenerator, elt);				
				if (prob < InsertionProbability)
				{
					MakeRoom(elt.getSize(), stats);
				} else {
					return false;
				}
			} 
		}
		else if(usingMinCost){
			if(currentCacheUsage.get() + elt.getSize() > cacheThreshold.get()){
				double prob = randomGenerator.nextDouble();
				if(currentEltCost <= currentMinCost){
					if(prob < InsertionProbability )
					{
						MakeRoom(elt.getSize(), stats);		
					} else {
						return false;
					}
				} else {				
					MakeRoom(elt.getSize(), stats);	
				}
			}	

			Integer currentNum = minCostMap.get(currentEltCost);
			if(currentNum == null)
			{
				minCostMap.put(currentEltCost, 1);		
			}
			else{
				currentNum++;
				minCostMap.put(currentEltCost, currentNum);
			}

			if(currentEltCost < currentMinCost){
				currentMinCost = currentEltCost;
			} 
		}
		else {
			MakeRoom(elt.getSize(), stats);
		}
		
		// the KVS should have space for this key-value pair at this point
		// insert the key-value pair
		AdjustMax( elt.getSize() );
		CAMP_Insert(elt, stats);
		
		return true;
	}
	
	public MultiModeLock GetLockForRoundedCost(int roundedcost){

        int n = roundedcost % NumConcurrentHT;

        MultiModeLock queueLock = QueueLocks[n].get(roundedcost);

        try{
            if(queueLock == null){
                MultiModeLock ql = new MultiModeLock();

                QueueSemaphores[n].acquire();
                
                if( (queueLock = QueueLocks[n].get(roundedcost)) == null){
                    QueueLocks[n].put(roundedcost, ql);
                    queueLock = ql;
                }
                QueueSemaphores[n].release();
            }
        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }

        return queueLock;
    }

	public boolean WarmUp(String key, int size, int cost, CAMPStats stats) {
		if (currentCacheUsage.get() + size <= cacheThreshold.get()) {
			dust elt = new dust();
			elt.setKey(key);
			elt.setPayLoad( new char[size] );
			elt.setSize( size );
			elt.SetInitialCost(cost);
			insertToKosar(key, elt);//KVS.put(key, elt);
			
			if (stats != null)
				stats.numBytesWritten += size;
			
			currentCacheUsage.getAndAdd(size);	
			
			return true;
		} else {
			return false;
		}
	}

	public void reset() {
//		camp.KVS.clear();
		LRUHT.clear();
	}
}