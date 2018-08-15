package com.mitrallc.camp;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.camp.Request;

public class SimulationInput {

	String outputFile;
	long cacheSize;
	double cachePercentage;
	int numThreads;
	int precision;
	double insertionProbability;
	Map<Integer, Request[]> requests;
	int numBuckets;
	
	ConcurrentHashMap<String, KeyInfo> map;
	String traceFile;
	
	ArrayList<Object> keyInfoArr;
	
}
