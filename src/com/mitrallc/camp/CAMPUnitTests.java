package com.mitrallc.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.mitrallc.kosar.dust;

public class CAMPUnitTests {
	public static final int EXP_CAMP = 0, EXP_CAMP_EXT = 1;
	
	public static boolean checkNumBytesWritten(Random rng, List<Request> requests, int campMode, long totalKeySize) {
		// itnitialize CAMP
		CAMP camp = new CAMP(null, (long)(totalKeySize * 0.3), 8, 1, CAMP.ONLINE);
		CAMPStats stats = new CAMPStats();
		int missCount = 0;
		
		// detect whether the keys are of same size or not
		int size = requests.get(0).size;
		boolean sameSize = true;
		for (Request r : requests) {
			if (r.size != size) {
				sameSize = false;
				break;
			}	
		}
		
		long numBytesWritten = 0;
		
		for (Request r : requests) {
			dust elt = camp.GetKey(r.key, stats);
			if (elt == null) {
				missCount++;
				
				// create new dust
				elt = new dust();
				elt.setKey(r.key);			
				byte[] pl = new byte[r.size];
				elt.setPayLoad(pl);
				elt.setSize(r.size);				
				elt.SetInitialCost(r.cost);				
				
				double prob = camp.calculateWeightedProb(rng, elt);
				boolean inserted = (prob < 1);			
				
				if (!inserted) {
					missCount--;
				} else {
					camp.InsertKey(elt, stats);
					if (!sameSize) {
						numBytesWritten += elt.getSize();
					}
				}
			}
		}
		
		if (sameSize) {
			numBytesWritten = missCount * size;
		}
		
		System.out.println("NumBytesWritten: " + numBytesWritten);
		System.out.println("NumBytesWritten from CAMP: " + stats.getNumBytesWritten());
		
		assert(numBytesWritten == stats.getNumBytesWritten());
		
		return (numBytesWritten == stats.getNumBytesWritten());
	}
	
	public static void main(String[] args) {
		// random generator
		// the seed number should match with the random generator in CAMP used for 
		// weighted probabilistic for admission control
		Random randomGenerator = new Random(7);
		
		// sample key
		ArrayList<KeyInfo> sampleKeys = new ArrayList<KeyInfo>();
		sampleKeys.add(new KeyInfo("key1", 1024, 1024));
		sampleKeys.add(new KeyInfo("key2", 1024, 1024));
		sampleKeys.add(new KeyInfo("key3", 1024, 1024));
		sampleKeys.add(new KeyInfo("key4", 1024, 1024));
		sampleKeys.add(new KeyInfo("key5", 1024, 1024));
		sampleKeys.add(new KeyInfo("key6", 1024, 1024));
		sampleKeys.add(new KeyInfo("key7", 1024, 1024));
		sampleKeys.add(new KeyInfo("key8", 1024, 1024));
		sampleKeys.add(new KeyInfo("key9", 1024, 1024));
		sampleKeys.add(new KeyInfo("key10", 1024, 1024));
		
		// generate the request list
		Random rand = new Random();
		ArrayList<Request> requests = new ArrayList<Request>();
		for (int i = 0; i < 100; i++) {
			int index = rand.nextInt(10);
			requests.add(new Request(i, sampleKeys.get(index).getKey(), sampleKeys.get(index).getSize(), 
					sampleKeys.get(index).firstCost, false));
		}
		
		long totalKeySize = 0;
		for (KeyInfo ki: sampleKeys) {
			totalKeySize += ki.size;
		}
		
		checkNumBytesWritten(randomGenerator, requests, EXP_CAMP, totalKeySize);
		
//		requests.clear();
//		RequestManager requestManager = new RequestManager();
//		Map<Integer, Request[]> requestMap = requestManager.GetRequests("/home/hieun/Desktop/KOSAR/InputFiles/traceZipf0.27-7200_trc1.log");
//		
//		for (int i = 0; i < requestManager.NumBuckets; i++) {
//			Request[] ra = requestMap.get(i);
//			
//			for (Request r : ra) {
//				requests.add(r);
//			}
//		}
//		
//		checkNumBytesWritten(randomGenerator, requests, EXP_CAMP, requestManager.UniqueQuerySize);
	}
}
