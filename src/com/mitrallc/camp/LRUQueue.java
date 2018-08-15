package com.mitrallc.camp;
import com.mitrallc.kosar.HeapElt;
import com.mitrallc.kosar.dust;

public class LRUQueue {
	private dust tail=null;
	private dust head=null;
	private HeapElt HeapEntry=null;
	private int roundedCost;
	private int count = 0;

	public LRUQueue() {	}
	
	public void reset() {
		tail = null;
		head = null;
		HeapEntry = null;
		roundedCost = 0;
		count = 0;
	}
	
	public int size(){
		return count;
	}
	
	public int getRoundedCost(){
		return roundedCost;
	}
	
	public HeapElt getHeapEntry(){
		return HeapEntry;
	}
	
	public dust peek(){
		return head;
	}
	
	public void setRoundedCost(int cost){
		roundedCost = cost;
	}
	
	public void setHeapEntry(HeapElt elt){
		HeapEntry=elt;
	}
	
	public void Display(){
		dust elt = head;
		while (elt != null){
			System.out.print(""+elt.getKey()+" (priority="+elt.getPriority()+")");
			elt = elt.getNext();
		}
	}
	
	public boolean add(dust elt){
        count++;
        if (tail == null){
            elt.setPrev(null);
            elt.setNext(null);
            tail = elt;
        }
        else {
            tail.setNext(elt);
            elt.setPrev(tail);
            tail = elt;
            elt.setNext(null);
        }

        if (head == null) head = tail;
        return true;
	}
	
	public boolean remove(dust elt){
		count--;
		if (elt.getNext() != null) elt.getNext().setPrev(elt.getPrev());
		if (elt.getPrev() != null) elt.getPrev().setNext(elt.getNext());
		if (head == elt) head = elt.getNext();
		if (tail == elt) tail = elt.getPrev();
		elt.setNext(null);
		elt.setPrev(null);
		return true;
	}
	
	public boolean isEmpty(){
		return head == null;
	}
	
	public int getLowestCost(){
		dust elt = this.head;
		return elt.getCostSize();
	}
	
	public synchronized boolean EvictHead(){
		if (this.head == null) return false;
		dust elt = this.head;
//		if(KosarSoloDriver.webServer != null)
//			KosarSoloDriver.KosarEvictedKeysEventMonitor.newEvent(1);
		this.Delete(this.head);
		//Remove the element from the RS
		com.mitrallc.kosar.Kosar.DeleteCachedQry(elt.getKey());
		
		//Remove the element from an instance of the Query Template
//		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
//		qtelt.decNumQueryInstances(); //decrement the number of query instances
//		qtelt.deleteQInstance(elt.getKey());
		return true;
	}

	public synchronized void Append(dust elt){
		//Inserts dust element at the tail of the LRU queue
		elt.setNext(null);
		elt.setPrev(tail); //Set the prev to the tail of the lru queue
		if (head == null) head = elt; //Empty LRU queue
		else tail.setNext(elt);
		tail = elt;
	}
	
	public synchronized void Delete (dust elt){
		//Deletes dust element from the LRU queue
		if (elt.getNext() != null) elt.getNext().setPrev( elt.getPrev() );
		else tail = elt.getPrev();

		if (elt.getPrev() != null) elt.getPrev().setNext( elt.getNext() );
		else head = elt.getNext();
	}
}
