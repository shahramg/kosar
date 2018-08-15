package com.mitrallc.kosar;

import com.mitrallc.camp.CAMP;
import com.mitrallc.camp.CAMParray;

public class ReplacementTechnique {
	long memsize = -1;
	private CAMParray campArray=null;

	
	boolean caching = false;
	boolean verbose = true;
	private long MinMemory = 1024;
	
	public boolean InsertKV(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.InsertKV:  input key-value pair is null.");
			return false;
		}
		if (fragid < 0 || fragid >= Kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.InsertKV:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+Kosar.NumFragments);
			return false;
		}
		if (!caching){
			if (verbose) System.out.println("ReplacementTechnique.InsertKV:  caching is disabled due to a cache size smaller than the specified Min "+MinMemory);
			elt.setRS(null);
			return false;
		}
		return campArray.InsertKV(elt, fragid, null);
	}
	
	public void DeleteKV(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.DeleteKV:  input key-value pair is null.");
			return;
		}
		if (fragid < 0 || fragid >= Kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.DeleteKV:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+Kosar.NumFragments);
			return;
		}
		campArray.DeleteKV(elt, fragid);
		return;
	}
	
	public void RegisterHit(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.RegisterHit:  input key-value pair is null.");
			return;
		}
		if (fragid < 0 || fragid >= Kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.RegisterHit:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+Kosar.NumFragments);
			return;
		}
		campArray.RegisterHit(elt, fragid);
		return;
	}
	
//	public void Reset(){
//		//selectedTechnique = new LRU(memsize);
//		selectedTechnique = new CAMParray(memsize);
//		return;
//	}
	
	public ReplacementTechnique(){
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        if ( (heapMaxSize - heapSize) > (MinMemory) ){
        	caching = true;
        	campArray = new CAMParray(CAMP.ONLINE); //LRU();
        }
	}
	
	public ReplacementTechnique(
			long cachesize //in bytes
			){
		if (verbose) System.out.print("Initialize the cache replacement technique with size "+cachesize+" bytes....");
		if (cachesize >= MinMemory ) 
			caching = true;  //If available cachesize is less than the minimum specified amount of memory then there is no caching
		memsize = cachesize;
		
		//selectedTechnique = new LRU(memsize);

		campArray = new CAMParray(memsize, CAMP.ONLINE);
		if (verbose) System.out.println("Done!");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
