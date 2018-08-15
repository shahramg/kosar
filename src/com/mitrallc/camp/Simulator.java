package com.mitrallc.camp;


import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.kosar.Kosar;
import com.mitrallc.kosar.dust;


public class Simulator extends Thread {

	boolean verbose = true;

//	PrintWriter writer;
	CAMParray mycamp;
	int threadNum;
	int totalThreads;

	public long totalRunTime = 0;

	public int NumIterations;

	public static final boolean UNIQUE = false;
	
	public static final boolean warmUp = true;

	//Request [] requests;

	Map<Integer, Request[]> mapOfRequests;

	int NumBuckets;

	ConcurrentHashMap<String, KeyInfo> itemMap;
	public CAMPStats stats = new CAMPStats();
	
	int simulatorId = 0;

	public Simulator(CAMParray camp, PrintWriter writer, Map<Integer, Request[]> requests, int threadNum, int totalThreads, ConcurrentHashMap<String,KeyInfo>map, int NumBuckets, int simulatorId){
		this.mycamp = camp;
//		this.writer = writer;
		this.mapOfRequests = requests;
		this.threadNum = threadNum;
		this.totalThreads = totalThreads;
		this.itemMap = map;
		this.NumBuckets = NumBuckets;
		this.simulatorId = simulatorId;
	}

//	public Simulator(String traceFile, long cacheSize, int precision, boolean verbose, 
//			String outputFilename){
//		this.verbose = verbose;	
//	}

	@Override
	public void run() {
		long startTime = System.nanoTime(); 
		int cnt = 0;
		
		boolean cachefull = false;			
		
		for(int j = 0; j < NumIterations; j++){
			for(int k = threadNum; k < NumBuckets; k += this.totalThreads){
				Request [] requests = mapOfRequests.get(k);
				for(int i = 0; i < requests.length; i ++ ){
					cnt++;
					if(UNIQUE){
						Object o = itemMap.remove(requests[i].key);
						if(o == null)
							continue;
					}

					// the first request of each key is not counted
					if(requests[i].repeat && stats != null){
						stats.IncrementNumReqs();
						stats.IncrementTotalCost(requests[i].cost);
					}

					if( requests[i].size > mycamp.cacheSize)
					{
						if (verbose) {
							System.out.println("Item too big for cache");
						}
						continue;
					}

					dust elt = mycamp.GetKey(requests[i].key, stats);
					
//					boolean isInserted = false;
					
					if (elt == null) {						
						if(requests[i].repeat && stats != null){
							stats.IncrementCostNotInCache(requests[i].cost);
							stats.IncrementNumMisses();
						}

						if (mycamp.getMode() == CAMP.ONLINE) {						
							elt = CAMP.CreateDustFromRequest(requests[i]);							
							int fragid = Kosar.getFragment(elt.getKey());
							
							if (warmUp && !cachefull) {								
								if (mycamp.getCamps()[fragid].currentCacheUsage.get() + elt.getSize() >
									mycamp.getCamps()[fragid].cacheThreshold.get()) {	// cache is full now
									cachefull = true;
								} else {									
									mycamp.getCamps()[fragid].CAMP_Insert(elt, stats);
									
									if(UNIQUE){
										itemMap.put(requests[i].key, new KeyInfo(requests[i].key, requests[i].cost, requests[i].size));
									}

									if(requests[i].repeat == false)
										requests[i].repeat = true;
									continue;
								}
							}
							
							mycamp.InsertKey(elt, stats);
						}
					} else {
						if (requests[i].repeat && stats != null) {
							stats.IncrementNumHit();
						}
					}

					if(UNIQUE){
						itemMap.put(requests[i].key, new KeyInfo(requests[i].key, requests[i].cost, requests[i].size));
					}

					if(requests[i].repeat == false)
						requests[i].repeat = true;
				}
			}
		}	
		
		System.out.println("*****Cnt= " + cnt);
		long endTime = System.nanoTime();
		totalRunTime = (endTime - startTime);
	}

}
