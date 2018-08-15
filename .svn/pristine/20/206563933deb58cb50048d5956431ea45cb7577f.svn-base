package com.mitrallc.camp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import com.mitrallc.kosar.dust;
import com.mitrallc.sql.KosarSoloDriver;

public class CMDline {

	
	public static void usageMessage()
	{
		System.out.println("CAMP Command Line ");
		System.out.println("Usage: java com.mitrallc.camp.CMDline -S size -P precision");
		System.out.println("  -S size: Specify memory size in bytes");
		System.out.println("  -P precision: Specify precision, an integer between 1 and 31");
		System.out.println();
	}
	
	public static void help()
	{

		System.out.println("Commands:");
		System.out.println("  1. insert key value size cost ");
		System.out.println("  2. get key - returns the value for the specified key");
		System.out.println("  3. delete key - deletes the value for the specified key");
		System.out.println("  4. reset size - changes the available memory size to the specified size in bytes");
		System.out.println("  5. dump - show CAMP's heap and LRU queues");
		System.out.println("  6. evict - deletes the lowest priority key-value pair");
		System.out.println("  7. help - prints this set of commands");
		System.out.println("  8. quit - Quit");
	}
	
	public static long ConvertToLong(String val){
		try{
			Long mylong = new Long(val);
			return mylong.longValue();
		} catch (Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int ConvertToInt(String val){
		try{
			Integer myint = new Integer(val);
			return myint.intValue();
		} catch (Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	
	public static dust GenerateDust(String key, String value, int cost, int sz){
		dust ds = new dust();
		ds.setKey(key);
		ds.setValue(key);
		ds.SetInitialCost(cost);
		ds.setSize(sz);
		return ds;
	}
	
	public static void DisplayStatus(CAMParray mycamp){
		System.out.println("KosarSoloDriver:");
		System.out.println("\t Cache size:"+KosarSoloDriver.getCacheSize());
		System.out.println("\t Used space:"+KosarSoloDriver.CurrentCacheUsedSpace(true));
		System.out.println("CAMP:");
		mycamp.ShowStats(0);
	}
	
	public static void main(String[] args) {
		int argindex=0;
		long CampSize=2048;
		int CampPrecision = 4;
		
		CAMPStats stats = new CAMPStats();

		Properties props=new Properties();
		while ( (argindex<args.length) && (args[argindex].startsWith("-")) )
		{
			if ( (args[argindex].compareTo("-help")==0) ||
					(args[argindex].compareTo("--help")==0) ||
					(args[argindex].compareTo("-?")==0) ||
					(args[argindex].compareTo("--?")==0) )
			{
				usageMessage();
				System.exit(0);
			}
			
			if (args[argindex].compareTo("-S")==0)
			{
				argindex++;
				if (argindex>=args.length)
				{
					usageMessage();
					System.exit(0);
				}
				props.setProperty("size",args[argindex]);
				argindex++;
			} else if (args[argindex].compareTo("-P")==0)
			{
				argindex++;
				if (argindex>=args.length)
				{
					usageMessage();
					System.exit(0);
				}
				props.setProperty("precision",args[argindex]);
				argindex++;
			}
		}
		
		if (argindex!=args.length)
		{
			usageMessage();
			System.exit(0);
		}
		
		if (argindex != 4){
			usageMessage();
			System.exit(0);			
		}
		
		System.out.println("CAMP (size = "+props.getProperty("size")+", precision = "+props.getProperty("precision")+")");
		try {
			CampSize = ( new Long(props.getProperty("size")));
			CampPrecision = ( new Integer(props.getProperty("precision")));
			
		} catch (Exception e){
			System.out.println(e.getStackTrace());
		}
		System.out.println("CAMP Command Line client (size="+CampSize+", precision="+CampPrecision+")");
		System.out.println("Type \"help\" for command line help");
		System.out.println("Start with \"help\" for usage info");
		
		//Initialize KOSAR and CAMP here
		CAMParray mycamp = new CAMParray(CampSize, CampPrecision, 1.0, CAMP.ONLINE);
//		KosarSoloDriver.SimulationMode = true;
		KosarSoloDriver.setCacheSize(CampSize);
		
		//main loop
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		for (boolean done=false; !done;)
		{
			//get user input
			System.out.print("> ");
			
			String input=null;

			try
			{
				input=br.readLine();
			}
			catch (IOException e)
			{
				e.printStackTrace(System.out);
				System.exit(1);
			}
			
			if (input.compareTo("")==0) 
			{
				continue;
			}

			if (input.compareTo("help")==0) 
			{
				help();
				continue;
			}
			
			if (input.compareTo("quit")==0)
			{
				done = true;
				break;
			}
			
			String[] tokens=input.split(" ");

			/*
			 * handle Commands:
			  1. insert key value size cost ");
			  2. get key - returns the value for the specified key");
			  3. delete key - deletes the value for the specified key");
			  4. reset size - changes the available memory size to the specified size in bytes");
			  5. dump - show CAMP's heap and LRU queues");
			  6. evict
			  7. help - prints this set of commands");
			  8. quit - Quit");
			  */
			if (tokens[0].compareTo("insert")==0)
			{
				if (tokens.length == 5){
					System.out.println("insert key="+tokens[1]+" value="+tokens[2]+" size="+tokens[3]+" cost="+tokens[4]);
					
					dust ds = GenerateDust(tokens[1], tokens[2], ConvertToInt(tokens[4]), ConvertToInt(tokens[3]) );
//					mycamp.InsertKV(ds, 0, stats);
					mycamp.InsertKey(ds, stats);
				} else System.out.println("insert key value size cost:  Error, wrong number of arguments.  "+tokens.length+" arguments detected.");
			} else if (tokens[0].compareTo("get")==0)
			{
				if (tokens.length == 2){
					System.out.println("get "+tokens[1]);
				} else System.out.println("get key:  Error, wrong number of arguments.  "+tokens.length+" arguments detected.");
			} else if (tokens[0].compareTo("delete")==0)
			{
				if (tokens.length == 2){
					System.out.println("delete "+tokens[1]);
					dust elt = mycamp.Get(tokens[1]);
					if (elt == null){
						System.out.println("Key "+tokens[1]+" does not exist.");
					} else {
//						mycamp.DeleteKV(elt, 0);	
						mycamp.getCamps()[0].CAMP_Delete(stats);
					}
					
				} else System.out.println("delete key:  Error, wrong number of arguments.  "+tokens.length+" arguments detected.");
			} else if (tokens[0].compareTo("reset")==0)
			{
				if (tokens.length == 2){
					System.out.println("reset memory size to "+tokens[1]+" bytes");
				} else System.out.println("reset size:  Error, wrong number of arguments.  "+tokens.length+" arguments detected.");
			} else if (tokens[0].compareTo("dump")==0)
			{
				DisplayStatus(mycamp);
			} else if (tokens[0].compareTo("evict")==0)
			{
				dust elt = mycamp.Evict(0);
				if (elt != null){
					System.out.println("Evicted the lowest priority key-value pair "+elt.getKey());
				} else {
					System.out.println("Error, evict failed to remove an element.");
				}
			} else if (tokens[0].compareTo("quit")==0) 
				done=true;
		}
	}
}
