package com.mitrallc.kosar;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.mitrallc.sql.KosarSoloDriver;

public class Stats extends Thread {

	private static int FreqInMillisec = 10*1000;

	private static AtomicInteger NumReqs = new AtomicInteger(0);
	private static AtomicInteger NumHits = new AtomicInteger(0);
	private static AtomicInteger NumKeyValues = new AtomicInteger(0);
	private static AtomicInteger NumTriggers = new AtomicInteger(0);
	private static AtomicInteger NumProcs = new AtomicInteger(0);
	private static AtomicInteger NumCopyHit = new AtomicInteger(0);
	private static AtomicInteger NumCopyMiss = new AtomicInteger(0);
	private static AtomicInteger NumKeyCopyStored = new AtomicInteger(0);
	private static AtomicInteger NumBackOffs = new AtomicInteger(0);
	
	private static AtomicInteger NumCopy = new AtomicInteger(0);
	private static AtomicLong TotalTime = new AtomicLong(0);

	public static boolean done = false;

	public void IncrementNumReqs(){
		NumReqs.incrementAndGet();
	}

	public void IncrementNumBackOffs(){
		NumBackOffs.incrementAndGet();
	}
	public void IncrementNumHit() {
		NumHits.incrementAndGet();
	}
	
	public void IncrementNumCopyHit() {
		NumCopyHit.incrementAndGet();
	}

	public void IncrementNumKeyValues() {
		NumKeyValues.incrementAndGet();
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarKeysCachedEventMonitor.newEvent(1);
	}

	public void DecrementNumKeyValues() {
		NumKeyValues.decrementAndGet();
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarKeysCachedEventMonitor.newEvent(-1);
	}

	public void IncrementNumTriggers() {
		NumTriggers.incrementAndGet();
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarTriggerRegEventMonitor.newEvent(1);
	}

	public void IncrementNumProcs() {
		NumProcs.incrementAndGet();
	}
	
	public void IncrementNumCopyMiss() {
		NumCopyMiss.incrementAndGet();
	}
	
	public void IncrementNumCopy(long totalTime) {
		NumCopy.incrementAndGet();
		TotalTime.addAndGet(totalTime);
	}

	public Stats(int howfreq){
		FreqInMillisec = howfreq;
	}

	public void run() {
		for ( ; ; ){
			if (done)
				break;

			try {
				Thread.sleep(FreqInMillisec);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
			}
			//System.out.println("Num of elements in KVs = "+kosar.GetNumKeys() + " NumOfDeletedKeys= " + kosar.getNumDeletedKeys()+" NumOfDeletedCalls = " + kosar.getNumDeletedCalls());
			System.out.println("Num requests = "+NumReqs+", NumHits = "+NumHits+", "
					+ "# Key Values = "+NumKeyValues+", # Reg Triggers = "+NumTriggers+", "
							+ "# Reg Procs = "+NumProcs+", # NumCopyHit = "+KosarSoloDriver.KosarNumGotKeyFromAnotherClientEventMonitor.numberOfTotalEvents()+", "
									+ "# NumCopyMiss = "+NumCopyMiss+", # NumKeyCopyStored = "+NumKeyCopyStored+", "
											+ "# NumBackoffs="+NumBackOffs + ", # Avr copy time"+ (TotalTime.get() / (double)NumCopy.get()));
		}
	}

	public void IncrementKeyCopyStored() {
		NumKeyCopyStored.incrementAndGet();
	}
}
