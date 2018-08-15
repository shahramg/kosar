package com.mitrallc.camp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RequestManager {

	public long NumRequests = 0;
	public long TotalQuerySize = 0;
	public long UniqueQuerySize = 0;
	public int NumUniqueRequests = 0;

	public long NumCountedRequests = 0;

	static final String templateKey = "profileImage%d";

	//public Request ScrambledRequests [];

	public int NumBuckets = 1000;

	public ConcurrentHashMap <String, KeyInfo> map = new ConcurrentHashMap<String, KeyInfo>();

	public ArrayList<Object> keyInfoArr = new ArrayList<Object>();

	public RequestManager(){

		/*(for(int i = 0; i < RequestHashMaps.length; i++){
			RequestHashMaps[i] = new HashMap<String,Request>();
		} */
	}

	public Map<Integer,Request[]> GetRequests(String filename){

		ArrayList<Request> requests = new ArrayList<Request>();

		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;

			while ((line = br.readLine()) != null) {

				if (line.startsWith("seq")) {
					continue;
				}

				String [] splitStr = line.split(",");
				int index = Integer.parseInt(splitStr[0]);
				String key = splitStr[1].replace(":", "");
				int size = Integer.parseInt(splitStr[2]);
				int cost = Integer.parseInt(splitStr[3]);
				
				boolean repeat = false;
				if(splitStr.length > 4){
					int rep = Integer.parseInt(splitStr[4]);
					if(rep == 1){
						repeat = true;
					}
					else
						repeat = false;
					//	repeat = Boolean.parseBoolean(splitStr[4]);
				}

				KeyInfo ki = map.get(key);
				if(ki == null){
					ki = new KeyInfo(key, cost, size);
					map.put(key, ki);
					keyInfoArr.add(ki);
					UniqueQuerySize += size;
					NumUniqueRequests++;
				} else {
					ki.incrRef(cost);
					repeat = true;
					NumCountedRequests++;
				}

				Request r = new Request(index, key, size, cost, repeat);				
				requests.add(r);		

				//RequestHashMaps[index % NumBuckets].put(key, r);					

				NumRequests++;
				TotalQuerySize += size;
			}
			br.close();
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage());
			return null;
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
			return null;
		}
		catch(Exception ex){
			System.out.println(ex.getMessage());
			return null;
		}

		ConcurrentHashMap <Integer, Request[]> retMap = new ConcurrentHashMap<Integer, Request[]>();

		int elementsPerArray = requests.size() / NumBuckets;

		for(int i = 0; i < NumBuckets; i++){
			ArrayList<Request> elements = new ArrayList<Request>();
			for(int j = 0; j < elementsPerArray; j++){
				Request r = requests.get( i * elementsPerArray + j );				
				elements.add( r );
			}
			Request [] reqs = elements.toArray(new Request[elements.size()]);
			retMap.put(i, reqs);
		}		

		// (requests.size() % NumBuckets) requests are not used
		// the map and keyInfoArr need to be adjusted
		// the total number of requests that will be processed is elementsPerArray * NumBuckets 
		for (int i = elementsPerArray * NumBuckets; i < requests.size(); i++) {
			Request r = requests.get(i);
			KeyInfo ki = map.get(r.key);
			int c = ki.decrRef(r.cost);
			if (c <= 0) {
				map.remove(r.key);
				keyInfoArr.remove(ki);
			}
		}

		// sort keys
		Object[] objs = keyInfoArr.toArray();
		Arrays.sort(objs);

		//		long totalsize = 0;
		//		
		//		PrintWriter out = null;
		//		try {
		//			out = new PrintWriter(new BufferedWriter(new FileWriter("/home/hieun/Desktop/plot.csv", true)));
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//		for (Object obj : objs) {
		//			KeyInfo ki = (KeyInfo)obj;
		//			totalsize += ki.size;			
		//			out.println(ki.key + "," + (double)(ki.totalCost - ki.firstCost) / (ki.size * NumCountedRequests));
		//		}
		//		
		//		long size = 0;
		//		int cnt = 0;
		//		int i = 0;
		//		boolean isZeroWeight = false;
		//		for (Object obj : objs) {
		//			cnt++;
		//			KeyInfo ki = (KeyInfo)obj;
		//			size += ki.size;
		//			if (size > ((double)i + 1) * 0.2 * totalsize) {
		//				if (i < 5) {
		//					out.println(cnt);
		//					i++;
		//				}
		//			}
		//			
		//			if (ki.totalCost - ki.firstCost == 0 && isZeroWeight == false) {
		//				out.println("zero_line,"+cnt);
		//				isZeroWeight = true;
		//			}
		//		}
		//
		//		out.close();

		keyInfoArr = new ArrayList<Object>(Arrays.asList(objs));

		return retMap;
	}


	public Map<Integer,Request[]> GenerateExperiment(int numTotalRequests, int numUniqueRequests, int[] requestSizes, int[] requestCosts){

		//Request requests[] = new Request[numTotalRequests];
		this.NumRequests = numTotalRequests;

		this.NumBuckets = requestCosts.length;

		Map<Integer,Request[]> reqs = new HashMap<Integer, Request[]>();

		int numUniquePerCost = numUniqueRequests / requestCosts.length;

		int idx = 0;
		for(int i = 0; i < requestCosts.length; i++){

			Request [] tempReqs = new Request[numUniquePerCost];
			for(int j = 0; j < numUniquePerCost; j++)
			{
				Request request = new Request(idx, String.format(templateKey, idx % numUniqueRequests), 
						requestSizes[idx % requestSizes.length ],
						requestCosts[i],
						true);

				tempReqs[j] = request;

				if(i < numUniqueRequests){
					UniqueQuerySize += requestSizes[i % requestSizes.length];
					map.put(request.key, new KeyInfo(request.key, request.cost, request.size));			
				}
				idx++;
			}
			reqs.put(i, tempReqs);			
		}

		/*
		for(int i = 0; i < numTotalRequests; i++){

			Request request = new Request(i, String.format(templateKey, i % numUniqueRequests), 
								requestSizes[i % requestSizes.length],
								requestCosts[i % requestCosts.length],
								true);




			//requests.add(arg0)

			//RequestHashMaps[i % numUniqueRequests].put(request.key, request);

			//requests[i] = request;

			TotalQuerySize += requestSizes[i % requestSizes.length];
		} */

		/*	for(int i = 0; i < numUniqueRequests; i++){

			for(Request req : RequestHashMaps[i].values())
				System.out.println(req.key);		
		} */

		return reqs;
	}

	public Request[] GenerateAllIncreasingRequests(int numOfRequests){

		Request requests [] = new Request[numOfRequests];

		this.NumRequests = numOfRequests;

		int values [] = {1, 10, 100, 1000};

		for(int i = 0; i < requests.length; i ++){
			Request request = new Request(i, String.format(templateKey, i % 100), 1000, values[i % 4], true );

			TotalQuerySize += 1000;
			UniqueQuerySize += 1000;

			/*	requests[i].key = String.format(templateKey, i);
			requests[i].cost = 1000;
			requests[i].size = 1000;
			requests[i].repeat = true; */
			requests[i] = request;
		}

		return requests;

	}

	public Request[] GenerateAllUniqueRequests(int numOfRequests){

		Request requests [] = new Request[numOfRequests];

		this.NumRequests = numOfRequests;

		String templateKey = "profileImage%d";

		for(int i = 0; i < requests.length; i ++){
			Request request = new Request(i, String.format(templateKey, i), 1000, 1000, true );

			TotalQuerySize += 1000;
			UniqueQuerySize += 1000;

			/*	requests[i].key = String.format(templateKey, i);
			requests[i].cost = 1000;
			requests[i].size = 1000;
			requests[i].repeat = true; */
			requests[i] = request;
		}

		return requests;

	}

	public long CacheSize(String filename){

		long fileSize = 0;

		return fileSize;
	}








	public static void main(String[] args) {

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter a trace file:");
		String traceFile = "";
		try {
			traceFile = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}


		long fileSize = 0;

		try{
			BufferedReader br = new BufferedReader(new FileReader(traceFile));
			String line;

			while ((line = br.readLine()) != null) {

				String [] splitStr = line.split(",");
				int size = Integer.parseInt(splitStr[2]);
				fileSize += size;

			}
			br.close();
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage());
			return;
		}
		catch(IOException ex){
			System.out.println(ex.getMessage());
			return;
		}
		catch(Exception ex){
			System.out.println(ex.getMessage());
			return;
		}


		System.out.println("Total size of Queries: " + fileSize);

	}

}
