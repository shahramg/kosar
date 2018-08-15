package com.mitrallc.camp;

/**
 * Storing the key information
 * @author Hieu
 *
 */
public class KeyInfo implements Comparable<KeyInfo> {
	String key;
	int firstCost;
	long totalCost;	// total cost for all references of this key
	int size;
	int count;
	
	public KeyInfo(String key, int cost, int size) {
		this.key = key;
		this.size = size;
		this.totalCost = cost;		// do not count the first cost
		this.firstCost = cost;
		
		// start the count from 1
		this.count = 1;
	}

	@Override
	public int compareTo(KeyInfo o) {
		double diff = (totalCost - firstCost) / (double)size - (o.totalCost - o.firstCost) / (double)o.size;
		
		if (diff == 0)
			return 0;
		else if (diff < 0)
			return 1;
		else
			return -1;
	}
	
	public int incrRef(int c) {
		totalCost += c;
		count++;
		
		return count;
	}
	
	public int decrRef(int c) {
		totalCost -= c;
		count--;
		
		return count;
	}
	
	public String getKey() {
		return key;
	}
	
	public long getTotalCost() {
		return totalCost;
	}
	
	public int getSize() {
		return size;
	}
	
	public int getCount() {
		return count;
	}
}