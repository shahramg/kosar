package com.mitrallc.camp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.mitrallc.kosar.Kosar;

//import com.camp.algorithm.CAMP;
//import com.camp.algorithm.Request;

public class MainClass {

	static String OutputPath = "/home/hieun/Desktop/KOSAR/Simulator/Simulations";
	//	static String OutputPath = "C:\\KOSAR\\Simulator\\Simulations";
	static PerformanceTimer performanceTimer;

	static int NumThreads = -1;
	static int Precision = -1;
	static int NumQueries = 10;

	//	static int NumIterations = 1000;
	static int NumIterations = 1;
//		static int NumIterations = 100;
//		static int NumIterations = 10;

//	static Integer [] ThreadsToRun = { 1,2,3,4,8 };
	//static Integer [] ThreadsToRun = { 1,2,3,4,5,6,7,8,16,32,64 };
	//	static Integer[] ThreadsToRun = { 1, 8 };
//		static Integer [] ThreadsToRun = { 8 };
//	static Integer[] ThreadsToRun = { 4 };
		static Integer[] ThreadsToRun = { 1 };

		static int[] FragmentsToRun = { 1 };
//	static int[] FragmentsToRun = {13};
	//	static int[] FragmentsToRun = { 1,13,53,101 } ;

	//	static Integer [] PrecisionsToRun = { 1, 8 }; //{ 4,5,6,8,16,24 }; // Fix so 32 will run correctly
	static Integer [] PrecisionsToRun = {8};

	//static Integer [] PrecisionsToRun = { 1,2,4,8,16,24 }; // Fix so 32 will run correctly

//		static Double [] InsertionProbabilitiesToRun = { 0.001, 0.005, 0.01, 0.02, 0.05,
//			0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.99, 0.991, 0.995, 0.9999, 1.0 };
	static Double [] InsertionProbabilitiesToRun = { 1.0 };
//		static Double[] InsertionProbabilitiesToRun = { .8 };

//		static double[] cachePercentages = { .2 };

//	static double[] cachePercentages = { .2, .4,.6,.8, 1.0 };
	//	static Integer [] PrecisionsToRun = { .1 };
//		static double[] cachePercentages = { 0.3 };
	static double[] cachePercentages = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };

	static ArrayList<Integer> PrecisionList = new ArrayList<Integer>();
	static ArrayList<Integer> ThreadList = new ArrayList<Integer>();

	//	public static final String RootDirectory = "C:\\KOSAR\\";
	public static final String RootDirectory = "/home/hieun/Desktop/KOSAR/";

	//	public static final String InputDirectory = "InputFiles\\";
	//	public static final String OutputDirectory = "OutputFiles_JAVA\\";

	public static final String InputDirectory = "InputFiles/";
	public static final String OutputDirectory = "OutputFiles_JAVA/";

	static String moreInfo = RootDirectory + OutputDirectory + "MoreInfo.csv";

	static int mode = CAMP.ONLINE;

	public static String [] FilesToRun = 
		{
		//	"traceZipf0.27-short.dat"
//		"traceZipf0.27-hint.dat",
		//			"traceZipf0.27-hint-expcost.dat",
//		"traceZipf0.99-hint.dat",
//					"traceZipf0.99-hint-expcost.dat",
					"traceZipf0.27-7200_trc1.log",
//					"traceZipf0.27-7200_trc2.log",
//					"traceZipf0.27-7200_trc3.log",
//					"traceZipf0.27-7200_trc4.log",
//					"traceZipf0.27-7200_trc5.log",
//					"traceZipf0.27-7200_trc6.log",
//					"traceZipf0.27-7200_trc7.log",
//					"traceZipf0.27-7200_trc8.log",
		//			"traceZipf0.27-7200_trc8.log-0",
		//			"traceZipf0.27-7200_trc8.log-1",
		//			"traceZipf0.27-7200_trc8.log-2",
		//			"traceZipf0.27-7200_trc8.log-3",
		//			"traceZipf0.27-7200_trc8.log-4",
		//			"traceZipf0.27-7200_trc8.log-5",
		//			"traceZipf0.27-7200_trc8.log-6",
		//			"traceZipf0.27-7200_trc8.log-7",
		//			"traceZipf0.27-7200_trc8.log-8",
		//			"traceZipf0.27-7200_trc8.log-9",
//					"traceZipf0.27-7200_trc9.log",
//					"traceZipf0.27-7200_trc10.log",
		//		"trace.log"
//			"traces_z0.27_1.txt", "traces_z0.27_2.txt", "traces_z0.27_3.txt", "traces_z0.27_4.txt", "traces_z0.27_5.txt",
//			"traces_z0.27_6.txt", "traces_z0.27_7.txt", "traces_z0.27_8.txt", "traces_z0.27_9.txt",	"traces_z0.27_10.txt",		
//		
//			"traces_z0.01_1.txt", "traces_z0.01_2.txt", "traces_z0.01_3.txt", "traces_z0.01_4.txt", "traces_z0.01_5.txt",
//			"traces_z0.01_6.txt", "traces_z0.01_7.txt", "traces_z0.01_8.txt", "traces_z0.01_9.txt",	"traces_z0.01_10.txt",
//			
//			"traces_z0.5_1.txt", "traces_z0.5_2.txt", "traces_z0.5_3.txt", "traces_z0.5_4.txt", "traces_z0.5_5.txt",
//			"traces_z0.5_6.txt", "traces_z0.5_7.txt", "traces_z0.5_8.txt", "traces_z0.5_9.txt",	"traces_z0.5_10.txt",
//			
//			"traces_z0.99_1.txt", "traces_z0.99_2.txt", "traces_z0.99_3.txt", "traces_z0.99_4.txt", "traces_z0.99_5.txt",
//			"traces_z0.99_6.txt", "traces_z0.99_7.txt", "traces_z0.99_8.txt", "traces_z0.99_9.txt",	"traces_z0.99_10.txt",
		};

	public static boolean PrintOutput = false;

	public static void RunSimulation(SimulationInput si){

		PrintWriter writer;
		if(PrintOutput == false)
			writer = null;
		else{
			try{
				writer = new PrintWriter(si.outputFile);
			}
			catch(Exception e){
				e.printStackTrace();
				return;
			}
		}
		//cacheSize = 167000;
		CAMParray mycamp = new CAMParray(si.cacheSize, si.precision, si.insertionProbability, mode);

		Simulator simulators[] = new Simulator [ si.numThreads ];

		for(int i = 0 ; i < si.numThreads; i ++){

			Simulator simulator = new Simulator( mycamp, writer, si.requests, i, si.numThreads, si.map, si.numBuckets, i);
			simulator.NumIterations = NumIterations;
			simulators[i] = simulator;
		}
		
		// populate the keys for the offline algorithm
		if (mode == CAMP.OFFLINE) {
			mycamp.offlineWarmUp(si.keyInfoArr, simulators[0].stats);
		}

		performanceTimer.StartTrackTime(si);
		for(Simulator s : simulators){
			s.start();
		}

		for(Simulator s: simulators){
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
		performanceTimer.EndTrackTime();
		//		for(Simulator s: simulators){
		//			System.out.println("Run Time for " + s.getId() + " : " + s.totalRunTime);
		//		}

		//		System.out.println("Total LRUQueue: " + LRUQueue.queueCnt.get());

		if(writer != null)
			writer.flush();

		StatsHelper helper = new StatsHelper();
		helper.CompileStats(simulators, mycamp);
		helper.numThreads = si.numThreads;
		helper.PrintStats(si.outputFile);	
		
		for (Simulator s : simulators) {
			s.stats.PrintStats();
		}

		for(int i = 0; i < si.numBuckets; i++){
			Request [] reqs = si.requests.get(i);
			for(Request req : reqs) {
				req.repeat = req.orig_repeat;
			}	
		}

		WriteLineToFile(((double)helper.numMisses) / helper.numReqs + "," + 
				((float)helper.costNotInCache) / helper.totalCost + ","+helper.numBytesWritten, moreInfo);
		
		simulators = null;
		for (CAMP camp : mycamp.getCamps()) {
			camp.reset();
			camp = null;
		}
		mycamp = null;
	}

	public static void SetupAndRunSimulation(String traceFile, String exp, long cacheSize, boolean verbose){

		String outputFile;

		Map<Integer, Request[]> hashmapRequests = null;
		RequestManager requestManager = new RequestManager();

		if(traceFile != ""){
			//requests = requestManager.GetRequests(traceFile);
			outputFile = traceFile.substring(0, traceFile.lastIndexOf('.'));
			outputFile = traceFile.substring(traceFile.lastIndexOf("//"));

			System.out.println("Output file line (line 132) Main Class " + outputFile);

		}
		else if (exp != ""){

			if(exp.compareTo("exp1") == 0){

				//	requests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;
				System.out.println("Output file line (line 147) Main Class " + outputFile);

			}
			else if(exp.compareTo("exp2") == 0){
				// requests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;

				System.out.println("Output file line (line 158) Main Class " + outputFile);


			} 
			if(exp.compareTo("100U") == 0){

				int numTotalRequests =  160000;
				int numUniqueRequests = 1600;
				int [] requestSizes = {100};
				//int [] requestCosts = {1,  13, 57, 123, 641, 1013, 1987, 3021};
				int [] requestCosts = {1,  13, 57, 123};


				hashmapRequests = requestManager.GenerateExperiment(numTotalRequests, numUniqueRequests, requestSizes, requestCosts);
				System.out.println("Requests Generated for experiment: " + exp);
				outputFile = "/" + exp + NumQueries;

				System.out.println("Output file line (line 177) Main Class " + outputFile);


			}
			else{
				System.out.println("Unknown experiment");
				return;
			}
		}
		else{
			//requests = requestManager.GenerateAllUniqueRequests(NumQueries);
			// requests = requestManager.GenerateAllIncreasingRequests(NumQueries);		
			System.out.println("Unique Requests Generated");
			outputFile = "/UniqueQueries_" + NumQueries;

			System.out.println("Output file line (line 192) Main Class " + outputFile);


		}

		long uniqueQuerySize = requestManager.UniqueQuerySize;

		if(cacheSize == -1){ // means run percentages
			for(double cachePercent : cachePercentages){

				long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

				System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);


				for(Integer precision : PrecisionList){

					System.out.println("Precision: " + precision);

					for(Integer numThreadsToRun: ThreadList){

						System.out.println("Threads: " + numThreadsToRun);

						for(Double insertionProbability: InsertionProbabilitiesToRun){
							System.out.println("Insertion Probability: " + insertionProbability);

							System.out.println("***** Cache Percent = " + cachePercent + " : " + simulationCacheSize + " *****");
							String tempOutputFile = OutputPath + outputFile + "_" + cachePercent +"_output.txt";

							System.out.println("Output line (line 223) Main Class " + tempOutputFile);


							SimulationInput si = new SimulationInput();
							si.outputFile = tempOutputFile;
							si.cacheSize = simulationCacheSize;
							si.numThreads = numThreadsToRun;
							si.precision = precision;
							si.insertionProbability = insertionProbability;

							if(hashmapRequests == null)
								si.requests = new HashMap<Integer, Request[]>();

								si.requests = hashmapRequests;
								si.map = requestManager.map;
								si.traceFile = "Unique requests";
								si.cachePercentage= cachePercent;
								si.numBuckets = requestManager.NumBuckets;
								RunSimulation(si);

						}
					}
				}
			}
		}

		else{ //use inputted value
			String tempOutputFile = OutputPath + outputFile + "_" + cacheSize +"_output.txt";

			System.out.println("Output file line (line 247) Main Class " + tempOutputFile);


			for(Integer precision : PrecisionList){
				System.out.println("Precision: " + precision);
				for(Integer numThreadsToRun: ThreadList){				
					System.out.println("Threads: " + numThreadsToRun);
					for(Double insertionProbability: InsertionProbabilitiesToRun){
						System.out.println("Insertion Probability: " + insertionProbability);

						SimulationInput si = new SimulationInput();
						si.outputFile = tempOutputFile;
						si.cacheSize = cacheSize;
						si.numThreads = numThreadsToRun;
						si.precision = precision;
						si.insertionProbability = insertionProbability;
						si.requests = hashmapRequests;
						si.map = requestManager.map;
						si.traceFile = "Unique requests";
						si.cachePercentage= 0;
						RunSimulation(si);	

					}
				}
			}
		}
	}

	public static void SimulateFiles(long cacheSize, boolean verbose){

		String outputFile = RootDirectory + OutputDirectory + "AllFilesOutput.csv";

		File file = new File(outputFile);
		File file1 = new File(moreInfo);

		//if file doesnt exists, then create it
		try{
			if(file.exists()){
				file.delete();
			}
			file.createNewFile();

			if (file1.exists()) {
				file1.delete();
			}
			file1.createNewFile();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		//stats.GetMissToRequestRatio(), stats.GetCostToMissRatio(), LRUHT.size(), PQ.size());

		// reset file

		for(String filename : FilesToRun){

			String FullPath = RootDirectory + InputDirectory + filename;

			System.out.println("input file line (line 303) Main Class " + FullPath);


			RequestManager requestManager = new RequestManager();
			Map<Integer, Request[]> hashmapRequests = requestManager.GetRequests(FullPath);

			int NumBuckets = requestManager.NumBuckets;
			long uniqueQuerySize = requestManager.UniqueQuerySize;

			WriteLineToFile("File,InsertionProb,Precision,CacheSize,missrate,costmiss", moreInfo);

			for (int numfrags : FragmentsToRun) {
				Kosar.NumFragments = numfrags;

				for(double cachePercent : cachePercentages){

					long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

					WriteLineToFile("File: " + filename + ",#Fragments: " + numfrags + ",Cache Percent: " + cachePercent + ",Cache Size: " + simulationCacheSize, outputFile);

					WriteLineToFile("Precision, Num Threads, Miss To Request, Cost To Miss, Num Key Values, # LRU Queues, "
							+ "#LRUCreation, #LRUDeletaion, Heap Elements, HeapAdds, HeapRemoves,"
							+ "HeapTopRemoves, HeapChanges,time (s), requests per sec, evictions,"
							+ "RCDelFull,RCDelKeyNotFound,RCInsertCacheFull,RCInsertSameKey,"
							+ "RCUpdateKeyNotFound, RCUpdateLRUQueueNotFound", outputFile);

					System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);

					for(Integer precision : PrecisionList){

						System.out.println("Precision: " + precision);

						for(Integer numThreadsToRun: ThreadList){
							System.out.println("Threads: " + numThreadsToRun);
							
							for(Double insertionProbability: InsertionProbabilitiesToRun){
								System.out.println("Insertion Probability: " + insertionProbability);

								SimulationInput si = new SimulationInput();
								si.outputFile = outputFile;
								si.cacheSize = simulationCacheSize;
								si.numThreads = numThreadsToRun;
								si.precision = precision;
								si.insertionProbability = insertionProbability;
								si.requests = hashmapRequests;
								si.map = requestManager.map;
								si.traceFile = filename;
								si.cachePercentage= cachePercent;
								si.numBuckets = NumBuckets;	
								si.keyInfoArr = requestManager.keyInfoArr;

								WriteToFile(filename+","+insertionProbability+","+precision+","+cachePercent+",", moreInfo);
								RunSimulation(si);

							}
						}
					}

					WriteLineToFile("", outputFile);

				}
			}
		}

		//CreateScript(cacheSize);
		// Create Script

	}

	public static void WriteLineToFile(String data, String outputFile){

		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)))) {
			out.println(data);
			out.close();
		}catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public static void WriteToFile(String data, String outputFile){

		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)))) {
			out.print(data);
			out.close();
		}catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/*
	public static void CreateScript(int cacheSize){

		String C_Template = 

		String Line_Template = "./simulate %s %d %d";


		for(String filename : FilesToRun){

			String FullPath = RootDirectory + InputDirectory + filename;
			RequestManager requestManager = new RequestManager();
			Request [] requests = requestManager.GetRequests(FullPath);

			long uniqueQuerySize = requestManager.UniqueQuerySize;

			for(double cachePercent : cachePercentages){

				long simulationCacheSize = (long) (uniqueQuerySize * cachePercent);

				WriteToFile("File: " + filename + ",Cache Percent: " + cachePercent + ",Cache Size: " + simulationCacheSize, outputFile);

				WriteToFile("Precision,Miss To Request, Cost To Miss, Num Key Values, # LRU Queues, Heap Elements", outputFile);

				System.out.println("**** Cache Size : " + cachePercent + " - " + simulationCacheSize);

				for(Integer precision : PrecisionList){

					System.out.println("Precision: " + precision);

					for(Integer numThreadsToRun: ThreadList){

						System.out.println("Threads: " + numThreadsToRun);

						RunSimulation(outputFile, simulationCacheSize, numThreadsToRun, precision, requests );
						//	public static void RunSimulation(String outputFile, long cacheSize, int numThreads, int precision, Request requests []){			
					}
				}

				WriteToFile("", outputFile);

			}
		}

	}

	 */

	public static void main(String[] args) {

		String format = "-threads numthreads -precision precision -file <tracefile> -cache <cachesize> -verbose <verbose>[0 or 1] ";

		performanceTimer = new PerformanceTimer();

		//String format = "filename cache-size precision [verbose (0 or 1)]";

		if(args.length < 2){
			System.out.println("Usage: " + format);
			return;
		}

		String traceFile = "";
		long cacheSize = -1;
		boolean verbose = false;
		String experiment = "";

		try{

			for(int i = 0; i < args.length; i++){

				if(args[i].compareTo("-threads") == 0 && i+1 < args.length){
					NumThreads = Integer.parseInt(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-precision") == 0 && i+1 < args.length){
					Precision = Integer.parseInt(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-exp") == 0 && i+1 < args.length){
					experiment = args[i+1];
					i++;
				}
				else if(args[i].compareTo("-file") == 0 && i+1 < args.length){
					traceFile = args[i+1];
					i++;
				}
				else if(args[i].compareTo("-file-to-run") == 0 && i+1 < args.length){
					// set file to run
					FilesToRun = new String[] { args[i+1] };
					i++;
				}
				else if(args[i].compareTo("-insert-prob-to-run") == 0 && i+1 < args.length){
					// set file to run
					InsertionProbabilitiesToRun = new Double[] { Double.parseDouble(args[i+1]) };
					i++;
				} 				
				else if(args[i].compareTo("-cache") == 0 && i+1 < args.length){
					cacheSize = Long.parseLong(args[i+1]);
					i++;
				}
				else if(args[i].compareTo("-verbose") == 0 && i+1 < args.length){
					verbose = Integer.parseInt(args[i+1]) == 1 ? true : false;
					i++;
				}
				else if (args[i].compareTo("-mode") == 0 && i+1 < args.length) {
					String s = args[i+1];
					if (s.equals("offline")) {
						mode = CAMP.OFFLINE;
					} else {
						mode = CAMP.ONLINE;
					}
					i++;
				}
				else{
					System.out.println("Unknown command line parameter: " + args[i]+ ". Usage: " + format);
					return;
				}
			}
		}
		catch(Exception e){	
			System.out.println("Commandline argument is not of correct type. " + format);
			return;
		}

		if(NumThreads == -1)
			ThreadList = new ArrayList<Integer>(Arrays.asList(ThreadsToRun));
		else{
			ThreadList.add(NumThreads);
		}

		if(Precision == -1)
			PrecisionList = new ArrayList<Integer>(Arrays.asList(PrecisionsToRun));
		else{
			PrecisionList.add(Precision);
		}			

		if(experiment.compareTo("files") == 0){
			SimulateFiles(cacheSize, verbose);
		}
		else{
			SetupAndRunSimulation(traceFile, experiment, cacheSize, verbose);
		}

		performanceTimer.CloseWriter();

	}
}
