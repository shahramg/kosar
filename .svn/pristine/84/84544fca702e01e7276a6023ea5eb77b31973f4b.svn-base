package com.mitrallc.webserver;

public class Last100SQLQueries {
	final int SIZE = 100;
	String[] queries = new String[SIZE];
	boolean[] Hit = new boolean[SIZE];
	int counter = 0;
	final static String HTMLforCache = "<li><p class=\"align\"><font color=\"#008000\">";
	final static String HTMLNoCache = "<li><p class=\"align\">";
	
	final static String EndHTMLforCache = "</font></p></li>";
	final static String EndHTMLNoCache = "</p></li>";
	
	
	public void add(String newQuery, boolean CacheHit) {
		int idx = counter%SIZE;
		if (idx >= 0 && idx < SIZE){
			queries[idx] = newQuery;
			Hit[idx]=CacheHit;
		}
		counter++;
		if (counter < 0) counter=1;
	}
	
	public String getQueryList() {
		String message = "";
		String query = "";
		int idx=0;
		message += "<ol>";
		for (int i = 0; i < SIZE; i++) {
		//for(String query : queries) {
			idx = (counter+1+i)%SIZE;
			if(queries[idx] != null){
				if (Hit[idx]) message += "" + HTMLforCache + queries[idx] + EndHTMLforCache;
				else  message += "" + HTMLNoCache + queries[idx] + EndHTMLNoCache;
			}
		}
		message += "</ol>";
		
		return message;
	}
}
