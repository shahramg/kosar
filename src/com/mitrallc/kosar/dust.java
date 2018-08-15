package com.mitrallc.kosar;


public class dust {
	String key=null;
	String value=null;

	com.mitrallc.sql.ResultSet cached_rowset = null;
	Object payload=null;
	private long lastWrite = 0;
	long gumballTS = 0;
	boolean gumball = false;
	
	boolean verbose = false;

	dust prev = null;  //implements the LRU cache replacement policy
	dust next = null;  //implements the LRU cache replacement policy

	private long MySize=1;
	private int CostSize=0;
	private long Priority=0;
	
	private int InitialCost=1;

	private String QueryTemplate=null;
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int GetInitialCost(){
		return InitialCost;
	}
	
	public void SetInitialCost(int cost){
		if (verbose) System.out.println("cost is "+cost);
		if (cost < 1) cost = 1;
		InitialCost = cost;
	}

	public String getQueryTemplate() {
		return QueryTemplate;
	}

	public void setQueryTemplate(String queryTemplate) {
		QueryTemplate = queryTemplate;
	}

	/*	
	public void setGumball(){
		gumballTS = System.currentTimeMillis();
		gumball = true;
	}
	public void clearGumball(){
		gumball = false;
	}

	public boolean isGumball(){
		return gumball;
	}

	public long getGumballTS(){
		return gumballTS;
	}*/
	public void setPriority(long prio){
		Priority = prio;
		return;
	}

	public long getPriority(){
		return Priority;
	}

	public void setCostSize(int CsSz){
		CostSize = CsSz;
		return;
	}

	public int getCostSize(){
		return CostSize;
	}

	public void setSize(long sz){
		MySize = sz;
		return;
	}

	public long getSize(){
		return MySize;
	}

	public void setNext(dust p){
		this.next = p;
	}

	public dust getNext(){
		return this.next;
	}

	public void setPrev(dust p){
		this.prev = p;
	}

	public dust getPrev(){
		return this.prev;
	}

	public void setKey(String k){
		if (verbose) System.out.println(""+k);
		key = k;
	}

	public String getKey(){
		return key;
	}

	public void setPayLoad (Object pl){
		payload = pl;
	}

	public void setRS (com.mitrallc.sql.ResultSet cr){
		//KosarSoloDriver.getLockManager().lockKey(key);
		lastWrite = System.currentTimeMillis();
		cached_rowset = cr;
		//KosarSoloDriver.getLockManager().unlockKey(key);
	}

	public com.mitrallc.sql.ResultSet getRS() {
		return cached_rowset;
	}

	public void setLastWrite(long Tmiss){
		lastWrite=Tmiss;
	}

	public long getLastWrite(){
		return this.lastWrite;
	}
}
