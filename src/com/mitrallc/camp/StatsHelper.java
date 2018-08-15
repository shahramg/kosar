package com.mitrallc.camp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class StatsHelper {

	public  int numThreads = 0;
	
	public  int totalHeapAccesses = 0;
	public   int numReqs = 0;
	public   int numHits = 0;
	public   long costNotInCache = 0;
	public   long totalCost = 0;
	public   int numMisses = 0;
	public   int totalNumMisses = 0;
	
	public  int numKeyValues = 0;
	public  int numLRUQueues = 0;
	public  int numHeapElts = 0;
	
	public  long cacheSize = 0;
	public  long cacheThreshold = 0;
	public  long currentCacheUsage = 0;
	
	public  long lookupTime = 0;
	public  long insertTime = 0;
	public  long deleteTime = 0;
	
	public  long totalTime = 0;
	
	public long evictions = 0;	
	
	public long totalHeapAdds = 0;
	public long totalHeapRemoves = 0;
	public long totalHeapChanges = 0;
	public long totalHeapTopRemoves = 0;
	public long totalLRUQueueCreations = 0;
	public long totalLRUQUeueDeletions = 0;
	
	public int totalCntDeleteCacheEmpty = 0;
	public int totalCntUpdateLRUQueueNotFound = 0;
	public int totalCntUpdateKeyNotFound = 0;
	public int totalCntInsertSameKey = 0;
	public int totalCntInsertCacheFull = 0;

	private int totalCntDeleteKeyNotFound=0;

	public long numBytesWritten;
	
	public  double GetHitToRequestRatio(){
		
		return ((double)numHits) / numReqs;
		
	}
	
	public  double GetMissToRequestRatio(){
		
		return ((double)numMisses) / numReqs;
		
	}
	
	public  double GetCostToMissRatio(){
		
		return ((float)costNotInCache) / totalCost;
		
	}
	
	 void CompileStats(Simulator simulators [], CAMParray camp){	
		
		for(Simulator s: simulators){
			totalHeapAccesses += s.stats.heapAccesses;
			numReqs += s.stats.numReqs;
			numHits += s.stats.numHits;
			costNotInCache += s.stats.costNotInCache;
			totalCost += s.stats.totalCost;
			numMisses += s.stats.numMisses;
			totalNumMisses += s.stats.totalNumMisses;
			totalTime += s.totalRunTime;
			evictions += s.stats.evictions;
			
			totalHeapAdds += s.stats.numAddedHeaps;
			totalHeapChanges += s.stats.numHeapChanges;
			totalHeapRemoves += s.stats.numRemovedHeapNodes;
			totalHeapTopRemoves += s.stats.numRemovedTopHeapNodes;
			totalLRUQueueCreations += s.stats.numCreatedLRUQueues;
			totalLRUQUeueDeletions += s.stats.numDeletedLRUQueues;
			
			totalCntDeleteCacheEmpty += s.stats.cntDeleteCacheEmpty;
			totalCntInsertCacheFull += s.stats.cntInsertCacheFull;
			totalCntInsertSameKey += s.stats.cntInsertSameKey;
			totalCntUpdateKeyNotFound += s.stats.cntUpdateKeyNotFound;
			totalCntUpdateLRUQueueNotFound += s.stats.cntUpdateLRUQueueNotFound;
			totalCntDeleteKeyNotFound += s.stats.cntDeleteKeyNotFound;
			numBytesWritten += s.stats.numBytesWritten;
		}
		
		for (CAMP c : camp.getCamps()) {
			numKeyValues += c.GetNumberOfKeyItems();
			numLRUQueues += c.LRUHT.size();
			numHeapElts += c.PQ.size();			
		}
		
		cacheSize = camp.getCamps()[0].cacheSize;
		cacheThreshold = camp.getCamps()[0].cacheThreshold.get();
		currentCacheUsage = camp.getCamps()[0].currentCacheUsage.get();
	}
	
	 void PrintStats(String OutputFilename){
		
		String template = "%d,%d,%f,%f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%f,%f,%d,%d,%d,%d,%d,%d,%d,%d";

		double hitToRequestRatio = ((double)numHits) / numReqs;
		double missToRequestRatio = ((double)numMisses) / numReqs;
		double costToMissRatio = ((float)costNotInCache) / totalCost;
		
		double avgTime = (totalTime /  1000000000.0) / numThreads;
		double requestsPerSecond = numReqs / (avgTime);
		
		
		String line = String.format(template, CampRounding.Precision, numThreads,
				GetMissToRequestRatio(), GetCostToMissRatio(), 
				numKeyValues, numLRUQueues, totalLRUQueueCreations, totalLRUQUeueDeletions, numHeapElts, totalHeapAdds,
				totalHeapRemoves, totalHeapTopRemoves, totalHeapChanges, avgTime, requestsPerSecond, evictions,
				totalCntDeleteCacheEmpty, totalCntDeleteKeyNotFound, totalCntInsertCacheFull,
				totalCntInsertSameKey, totalCntUpdateKeyNotFound, totalCntUpdateLRUQueueNotFound,
				numBytesWritten);

		System.out.println(line);

		File file = new File(OutputFilename);

		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
			}
		}

		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(OutputFilename, true)));
			out.println(line);
			out.flush();
			out.close();
		}catch (IOException e) {
			//exception handling left as an exercise for the reader
		}

		System.out.println("Precision, " + CampRounding.Precision);	
		System.out.println("Cache Size, " + cacheSize);
		System.out.println("Threshold Size, " + cacheThreshold);
		System.out.println("Cache Usage, " + currentCacheUsage);
		System.out.println("Number of Key Items, " + numKeyValues);

		System.out.println("Number of heap elements, " + numHeapElts);
		System.out.println("Number of LRU Queues, " + numLRUQueues);

		System.out.println("Num Requests, " + numReqs);
		System.out.println("Num Hits, " + numHits);
		System.out.println("Cost In Cache, " + costNotInCache);
		System.out.println("Total Cost, " + totalCost);
		
		
		
		System.out.println("Hit to Request Ratio, " + hitToRequestRatio);
		System.out.println("Miss To Request Ratio, " + missToRequestRatio);
		System.out.println("Cost To Miss Ratio, " + costToMissRatio);
		System.out.println("Heap Accesses, " + totalHeapAccesses);
		
		System.out.println("Total Heap Elt created: " + totalHeapAdds);
		System.out.println("Total Heap Elt removed: " + totalHeapRemoves);
		System.out.println("Total Heap Elt top removed: " + totalHeapTopRemoves);
		System.out.println("Total Heap Elt changes: " + totalHeapChanges);
		System.out.println("Total LRUQueue created: " + totalLRUQueueCreations);
		System.out.println("Total LRUQueue deleted: " + totalLRUQUeueDeletions);
		
		System.out.println("DeleteCacheEmpty: " + totalCntDeleteCacheEmpty);
		System.out.println("DeleteCacheKeyNotFound: " + totalCntDeleteKeyNotFound);
		System.out.println("UpdateLRUQueueNotFound: " + totalCntUpdateLRUQueueNotFound);
		System.out.println("UpdateKeyNotFound: " + totalCntUpdateKeyNotFound);
		System.out.println("InsertKeyExists: " + totalCntInsertSameKey);
		System.out.println("InsertCacheFull: " + totalCntInsertCacheFull);
		
		System.out.println("NumBytesWritten: " + numBytesWritten);		
		
		System.out.println("Average Thread Run Time in seconds: " + avgTime);
		System.out.println();
	}
}
