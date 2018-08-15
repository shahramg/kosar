package com.mitrallc.sqltrig;

import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.kosar.dust;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.webserver.EventMonitor;
import com.mitrallc.webserver.ClientSettingsPage;

public class QTmeta {
	Vector<String> tg=null;
	public EventMonitor KVSHitEventMonitor = new EventMonitor(ClientSettingsPage.getGranularity());
	private String QueryTemplate;
	private boolean CacheInstances=true;
	private boolean TriggersRegistered=false;
	private boolean TrigsInProgress=false;
	
	public boolean isTrigsInProgress() {
		return TrigsInProgress;
	}

	public void setTrigsInProgress(boolean trigsInProgress) {
		TrigsInProgress = trigsInProgress;
	}

	public boolean isTriggersRegistered() {
		return TriggersRegistered;
	}

	public void setTriggersRegistered(boolean triggersRegistered) {
		TriggersRegistered = triggersRegistered;
	}

//	private ConcurrentHashMap<String, dust>[] QInstances = new ConcurrentHashMap[Kosar.NumFragments];
	
//	public boolean FlushQT(){
//		for (int i=0; i < Kosar.NumFragments; i++){
//			if (QInstances[i] != null) QInstances[i].clear();
//		}
//		return true;
//	}
	
//	public boolean FlushQInstances() {
//		//Remove query instances using the QInstances data structure
//		for (int i=0; i < Kosar.NumFragments; i++){
//			Enumeration<dust> eqt = QInstances[i].elements();
//			for (Enumeration<dust> e = eqt; e.hasMoreElements();){
//				dust elt = e.nextElement();
//				Kosar.DeleteDust(elt);
//			}
//		}
//		return false;
//	}

//	public void setQInstances(String qi, dust elt) {
//		int fragid = Kosar.getFragment(qi);
//		if (QInstances[fragid] == null)
//			QInstances[fragid] = new ConcurrentHashMap<String, dust>();
//		
//			QInstances[fragid].put(qi, elt);
//		return;
//	}
	
//	public void deleteQInstance(String qi){
//		if(Kosar.getFragment(qi) != -1 && QInstances[Kosar.getFragment(qi)] != null)
//			QInstances[Kosar.getFragment(qi)].remove(qi);
//		return;
//	}

	public boolean CacheQryInstances() {
		return CacheInstances;
	}

	public void setCachingInstances(boolean CacheInst) {
		CacheInstances = CacheInst;
	}

	public int getKVSHitsMoving() {
		if (KosarSoloDriver.webServer == null) return 0;
		return KVSHitEventMonitor.numberOfEventsPerGranularity();
	}
	
	public int getKVSHits() {
		if (KosarSoloDriver.webServer == null) return 0;
		return KVSHitEventMonitor.numberOfTotalEvents();
		
		//return KVSHits;
	}

	public void addKVSHits() {
		if (KosarSoloDriver.webServer == null) return ;
		KVSHitEventMonitor.newEvent(1);
	}

	//NumQueryInstances is the number of cached query instances for a template
	private int NumQueryInstances = 0;
	
	public int getNumQueryInstances() {
		return NumQueryInstances;
	}

	public void addNumQueryInstances() {
		NumQueryInstances+=1;
	}
	
	public void decNumQueryInstances() {
		NumQueryInstances-=1;
	}


	public String getQueryTemplate() {
		return QueryTemplate;
	}

	public void setQueryTemplate(String queryTemplate) {
		QueryTemplate = queryTemplate;
	}

	public Vector<String> getTg() {
		return tg;
	}

	public void setTg(Vector<String> tg) {
		this.tg = tg;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
