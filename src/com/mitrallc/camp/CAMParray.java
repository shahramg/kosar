package com.mitrallc.camp;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.mitrallc.kosar.dust;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.sql.KosarSoloDriver;

public class CAMParray {
	private CAMP[] mycamp = new CAMP[ Kosar.NumFragments ];
	private double myfrac=0.9;
	private long memsize = 0;
	private static boolean verbose = false;
	
	private int CAMPprecision = 8;
	private double insertionProbability=1.0;
	
	int mode;
	long cacheSize;
	
	AtomicLong max_allow = new AtomicLong(0);	// max id for the free LRU queue
	AtomicLong next_number = new AtomicLong(0);	// ticket serving
	
	// a hash map for storing free queues
//	ConcurrentHashMap<Long, LRUQueue> freeQueues = new ConcurrentHashMap<Long, LRUQueue>();
	
	public void ShowStats(int fragid){
		System.out.println("\t Instance "+fragid);
		mycamp[fragid].Display();
		
	}
	
	public dust Evict(int fragid){
		return mycamp[fragid].CAMP_RemoveLowestPriority(null);
	}
	
	public dust Get(String key){
		return GetKey(key, null);
	}
	
	/**
	 * Gets 
	 * @param key
	 * @param stats
	 * @return
	 */
	public dust GetKey(String key, CAMPStats stats) {
		int fragid = Kosar.getFragment(key);
		return mycamp[fragid].GetKey(key, stats);
	}
	
	/**
	 * Puts
	 * @param elt
	 * @param stats
	 * @return
	 */
	public boolean InsertKey(dust elt, CAMPStats stats) {
		int fragid = Kosar.getFragment(elt.getKey());
		return mycamp[fragid].InsertKey(elt, stats);
	}
	
	public boolean CheckAndAdjust(int fragid, long incomingsize){
		//If the highWaterMark is larger than available memory then evict an element
		//If no eviction candidate exists then evict from another LRU Queue
		//long usedMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		//long freemem = Runtime.getRuntime().freeMemory();
		//if (verbose) System.out.println("FreeMem "+freemem/(1024*1024)+" MB, Current memory usage "+usedMemory/(1024*1024)+" MB, highWaterMark is "+ KosarSoloDriver.highWaterMark/(1024*1024) +" MB");
		boolean res=true;
		dust elt = null;
		//if (usedMemory > KosarSoloDriver.highWaterMark || KosarSoloDriver.CurrentCacheSize() > memsize ){
		if (verbose) System.out.println("Used space "+KosarSoloDriver.CurrentCacheUsedSpace(false)+", Limit is "+KosarSoloDriver.getCacheSize());

		while (KosarSoloDriver.CurrentCacheUsedSpace(false)+incomingsize > KosarSoloDriver.getCacheSize() ){
			if (verbose) System.out.println("CAMP.InsertKV:  Evicting a victim with Used space "+KosarSoloDriver.CurrentCacheUsedSpace(false)+", Limit is "+KosarSoloDriver.getCacheSize());
			elt = mycamp[fragid].CAMP_RemoveLowestPriority(null);
			if (elt == null){
				for (int i=0; i < Kosar.NumFragments; i++){
					if ( ( elt = mycamp[ (fragid + i) % Kosar.NumFragments ].CAMP_RemoveLowestPriority(null) ) != null ){
						i = Kosar.NumFragments;
						res = true;
					}
				}
			} //else KosarSoloDriver.UseCacheSpace(- elt.getSize()); //deduct the used space 
			
//			boolean done = true;
//			for (int i=0; i < kosar.NumFragments; i++){
//				if (mycamp[(fragid+i)%kosar.NumFragments].GetNumberOfKeyItems() > 1) done=false;
//			}
//			if (done) break;
			
		}
		
		if (KosarSoloDriver.CurrentCacheUsedSpace(false) > KosarSoloDriver.getCacheSize() ) res=false;
		return res;
	}
	
	public boolean InsertKV(dust elt, int fragid, CAMPStats stats){
		//If no item is evicted then ignore the insert operation
		if (verbose) System.out.println("Insert qry with size "+elt.getSize()+", cost "+elt.GetInitialCost()+", priority=");
		boolean res = CheckAndAdjust(fragid, elt.getSize());
		if (res) mycamp[fragid].CAMP_Insert(elt,stats);
		return res;
	}
	
	public void DeleteKV(dust elt, int fragid){
		mycamp[fragid].CAMP_Maint_Delete(elt,null);
		return;
	}
	
	public void RegisterHit(dust elt, int fragid){
		mycamp[fragid].CAMP_Update(elt, null);  //update the status of this element
		return;
	}
	
	public void Reset(){
		mycamp = new CAMP[ Kosar.NumFragments ];
		return;
	}
	
	public void InitializeCAMParray (
			long cachesize,  //in bytes
			int mode
			){
		long memsize = cachesize;
		System.out.println("CAMP:  Memory is initialized to "+memsize+" bytes.");
		
        // Get current size of heap in bytes
        long startingMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        if (heapMaxSize < (startingMemory+memsize)){
        	System.out.println("LRU:  The specified memsize "+memsize+" PLUS the startingMemory "+startingMemory+" exceeds the JVM size "+heapMaxSize);
        	memsize = (int) (heapMaxSize - startingMemory);
        	System.out.println("LRU:  Resetting the KVS size to "+memsize);
        }
        
        //Initialize array of LRU queues
        mycamp = new CAMP[ Kosar.NumFragments ];
        
        memsize = memsize / Kosar.NumFragments;
        
        for (int i=0; i < Kosar.NumFragments; i++)
        	mycamp[i] = new CAMP(this, memsize, CAMPprecision, insertionProbability, mode);
	}
	
	public CAMParray(int mode){
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        memsize = heapMaxSize-heapSize;
        memsize = (long)(((double) memsize) * myfrac); //Take over the specified fraction of memory
        
        InitializeCAMParray(memsize, mode);
        
        this.mode = mode;
        this.cacheSize = memsize;
	}
	
	
	public CAMParray (long cachesize,  //in bytes
			int mode){
		InitializeCAMParray(cachesize, mode);
		this.mode = mode;
		this.cacheSize = cachesize;
	}
	
	public CAMParray (
			long cachesize,  //in bytes
			int precision,
			double insprob,
			int mode
			){
		CAMPprecision = precision;
		insertionProbability = insprob;
		InitializeCAMParray(cachesize, mode);
		
		this.mode = mode;
		this.cacheSize = cachesize;
	}
	
	public int getMode() {
		return mode;
	}
	
	public CAMP[] getCamps() {
		return mycamp;
	}

	public void offlineWarmUp(ArrayList<Object> keyInfoArr, CAMPStats stats) {
		int index = 0;
		int missCount = 0;
		int totalCount = 0;
		long missCost = 0;
		long totalCost = 0;
		
		while (index < keyInfoArr.size()) {
			KeyInfo ki = (KeyInfo) keyInfoArr.get(index);
			int fragid = Kosar.getFragment(ki.getKey());
			boolean success = mycamp[fragid].WarmUp(ki.key, ki.size, (int)(ki.totalCost / ki.count), stats);
			if (!success) {
				missCount += ki.count - 1;
				missCost += ki.totalCost - ki.firstCost;
			}
			
			totalCount += ki.count - 1;
			totalCost += ki.totalCost - ki.firstCost;
			index++;
		}
		
		System.out.println("MissCount = " + missCount + "; TotalCount = " + totalCount);
		System.out.println("MissCost = " + missCost + "; TotalCost = " + totalCost);
		System.out.println("Sanity Check: Miss Rate = " + (double)missCount / totalCount);
		System.out.println("Sanity Check: Cost Miss = " + (double)missCost / totalCost);
	}

}
