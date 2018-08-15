package com.mitrallc.camp;

import java.io.PrintWriter;

public class CAMPStats {
			
	public int heapAccesses = 0;
	public int numReqs = 0;
	public int numHits = 0;
	public long costNotInCache = 0;
	public long totalCost = 0;
	public int numMisses = 0;
	public int totalNumMisses = 0;
	
	public long lookupTime = 0;
	public long insertTime = 0;
	public long deleteTime = 0;
	
	public long evictions = 0;
	
	public int numAddedHeaps = 0;
	public int numRemovedTopHeapNodes = 0;
	public int numRemovedHeapNodes = 0;
	public int numHeapChanges = 0;
	
	public int numCreatedLRUQueues = 0;
	public int numDeletedLRUQueues = 0;
	
	
	public int cntDeleteCacheEmpty = 0;
	public int cntUpdateLRUQueueNotFound = 0;
	public int cntUpdateKeyNotFound = 0;
	public int cntInsertSameKey = 0;
	public int cntInsertCacheFull = 0;
	public int cntDeleteKeyNotFound = 0;
	
	public long numBytesWritten = 0;
	
	public void IncrementHeapAdds() {
		numAddedHeaps++;
	}
	
	public void IncrementHeapRemoveTop() {
		numRemovedTopHeapNodes++;
	}
	
	public void IncrementHeapRemovals() {
		numRemovedHeapNodes++;
	}
	
	public void IncrementHeapChanges() {
		numHeapChanges++;
	}
	
	public void IncrementLRUQueueCreations() {
		numCreatedLRUQueues++;
	}
	
	public void IncrementLRUQueueDeletions() {
		numDeletedLRUQueues++;
	}
	
	public void IncrementHeapAccesses(){
		heapAccesses++;
	}
	
	public void IncrementNumMisses(){
		numMisses++;
	}
	
	public void IncrementTotalNumMisses(){
		totalNumMisses++;
	}
	
	public void IncrementNumReqs(){
		numReqs++;
	}
	
	public void IncrementNumHit() {
		numHits++;
	}
	
	public void IncrementCostNotInCache(int cost) {
		costNotInCache+=cost;
	}
	
	public void IncrementTotalCost(int cost) {
		totalCost+=cost;
	}
	
	public void IncrementInsertTime(long time){
		insertTime+= time;
	}
	
	public void IncrementDeleteTime(long time){
		deleteTime+=time;
	}
	
	public void IncrementLookupTime (long time){
		lookupTime+=time;
	}
	
	public void IncrementEvictions (){
		evictions++;
	}
	
	public void PrintStats(){
		System.out.println("Num Requests, " + numReqs);
		System.out.println("Num Hits, " + numHits);
		System.out.println("Cost In Cache, " + this.costNotInCache);
		System.out.println("Total Cost, " + totalCost);
		
		double hitToRequestRatio = ((double)numHits) / numReqs;
		double missToRequestRatio = ((double)numMisses) / numReqs;
		double costToMissRatio = ((float)costNotInCache) / totalCost;
		
		System.out.println("Hit to Request Ratio, " + hitToRequestRatio);
		System.out.println("Miss To Request Ratio, " + missToRequestRatio);
		System.out.println("Cost To Miss Ratio, " + costToMissRatio);
		System.out.println("Heap Accesses, " + heapAccesses);
		System.out.println("Num of Bytes Written"+ numBytesWritten);
		
	/*	System.out.println();
		System.out.println("Timing,");
		System.out.println("Lookup time, " + lookupTime);
		System.out.println("Insert time, " + insertTime);
		System.out.println("Delete time, " + deleteTime);
	*/
	}
	
	public double GetHitToRequestRatio(){
		
		return ((double)numHits) / numReqs;
		
	}
	
	public double GetMissToRequestRatio(){
		
		return ((double)numMisses) / numReqs;
		
	}
	
	public double GetCostToMissRatio(){
		
		return ((float)costNotInCache) / totalCost;
		
	}
	
	public void PrintStatsToFile(PrintWriter writer){
		
		if(writer == null)
			return;
			
		double hitToRequestRatio = ((double)numHits) / numReqs;
		double missToRequestRatio = ((double)numMisses) / numReqs;
		double costToMissRatio = ((float)costNotInCache) / totalCost;

		writer.println("Hit to Request Ratio, " + hitToRequestRatio);
		writer.println("Miss To Request Ratio, " + missToRequestRatio);
		writer.println("Cost To Miss Ratio, " + costToMissRatio);
	
		
	}

	public long getNumBytesWritten() {
		return numBytesWritten;
	}
}
	
