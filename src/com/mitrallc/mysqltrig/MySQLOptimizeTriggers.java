package com.mitrallc.mysqltrig;

import com.mitrallc.mysqltrig.mysqlTrigGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/*
 * Author:  Shahram Ghandeharizadeh
 * Date:  Aug 16, 2011
 * Methods of this class transform a vector of triggers into a smaller 
 * subset by merging the body of those vectors that reference the same 
 * table for a given operation, e.g., table user_cameras where the operation
 * is update/insert/delete.
 */

public class MySQLOptimizeTriggers {
	static mysqlTrigGenerator otg = null;
	static boolean verbose = false;

	static String[] sampleqrys = {
		"CREATE TRIGGER  FRIENDSHIP10191019699U BEFORE UPDATE ON FRIENDSHIP FOR EACH ROW BEGIN DECLARE DeleteIT_DLL_Val int; DECLARE KEYTODELETE  varchar(2048) DEFAULT ' '; DECLARE DELETEKEY varchar(2048) DEFAULT ' ';  IF (OLD.INVITEEID <> NEW.INVITEEID OR OLD.INVITERID <> NEW.INVITERID OR OLD.status <> NEW.status) THEN  SET KEYTODELETE = CONCAT('_IIB2',CONCAT(OLD.inviterID, OLD.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); SET KEYTODELETE = CONCAT('_IIB2',CONCAT(NEW.inviterID, NEW.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); SET KEYTODELETE = CONCAT('_IIB2',CONCAT(OLD.inviteeID, OLD.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); SET KEYTODELETE = CONCAT('_IIB2',CONCAT(NEW.inviteeID, NEW.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); END IF; IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KosarDelete('', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN UPDATE `Failed to connect to KOSAR KVS CORE.` SET x=1; END IF; END IF; END;",
		"CREATE TRIGGER  FRIENDSHIP1019109699U BEFORE UPDATE ON FRIENDSHIP FOR EACH ROW BEGIN DECLARE DeleteIT_DLL_Val int; DECLARE KEYTODELETE  varchar(2048) DEFAULT ' '; DECLARE DELETEKEY varchar(2048) DEFAULT ' ';  IF (OLD.INVITEEID <> NEW.INVITEEID OR OLD.INVITERID <> NEW.INVITERID OR OLD.status <> NEW.status) THEN   SET KEYTODELETE = CONCAT('_IIB2',CONCAT(OLD.inviteeID, OLD.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); SET KEYTODELETE = CONCAT('_IIB2',CONCAT(NEW.inviteeID, NEW.status)); SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE)); END IF; IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KosarDelete('', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN UPDATE `Failed to connect to KOSAR KVS CORE.` SET x=1; END IF; END IF; END;"
		};

	public String InArg = "DELETEKEY";
	public String ProcedureArgument = ", "+InArg+" IN OUT CLOB";

	public static String KosarDeleteCall = "IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KosarDelete('"+MySQLQueryToTrigger.getIPport()+"', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN UPDATE `Failed to connect to KOSAR KVS CORE.` SET x=1; END IF; END IF;";

	public static String TargetInsertTable(String trigger){
		int start = trigger.indexOf(otg.insert);
		String result = null;
		int end;
		if (start >= 0){
			result = trigger.substring( start+otg.insert.length() );
			result = result.trim();
			end = result.indexOf(" ");
			result = result.substring(0,end);
		}
		return result;
	}

	public static String TargetDeleteTable(String trigger){
		int start = trigger.indexOf(otg.delete);
		String result = null;
		int end;
		if (start >= 0){
			result = trigger.substring( start+otg.delete.length() );
			result = result.trim();
			end = result.indexOf(" ");
			result = result.substring(0,end);
		}
		return result;
	}

	public static String TargetUpdateTable(String trigger){
		int start = trigger.indexOf(otg.update);
		String result = null;
		int end;
		if (start >= 0){
			result = trigger.substring( start+otg.update.length() );
			result = result.trim();
			end = result.indexOf(" ");
			result = result.substring(0,end);
		}
		return result;
	}

	public String DeleteMeMergeTriggers(Vector<String> tz){
		StringBuffer Merged = new StringBuffer();
		int bodystarts, bodyends;
		String result = null;
		String elt;
		Vector<String> C = new Vector<String>();
		Vector<String> S = new Vector<String>();
		HashMap<String,String> procs = new HashMap<String,String>();


		//Iterate on elements of tz
		for (int i=0; i < tz.size(); i++){
			elt = tz.elementAt(i).toUpperCase();
			if (elt.indexOf(otg.BeginVariableDec) >= 0) C.add(elt);
			else S.add(elt);
		}
		//Note that they reference the same table
		//We assume the complex one use the same variables.  Hence:
		//Maintain the header of one and merge in the body of the rest.
		if (C.size() > 0) {
			elt = C.elementAt(0);
			Merged.append( elt.substring(0, elt.indexOf(otg.EndVariableDec)+otg.EndVariableDec.length() ) );
		} else {
			elt = S.elementAt(0);
			Merged.append( elt.substring(0, elt.indexOf(otg.EndVariableDec)+otg.EndVariableDec.length() ));
		}
		//Merge the body of the triggers into one and maintain the variables once.
		for (int i=0; i < C.size(); i++){
			elt = C.elementAt(i);
			bodystarts = elt.indexOf(otg.EndVariableDec) + otg.EndVariableDec.length();
			bodyends = elt.indexOf(otg.EndTrigger);
			String body = elt.substring(bodystarts, bodyends);
			Merged.append( elt.substring(bodystarts, bodyends) );
		}
		for (int i=0; i < S.size(); i++){
			elt = S.elementAt(i);
			bodystarts = elt.indexOf(otg.EndVariableDec) + otg.EndVariableDec.length();
			bodyends = elt.indexOf(otg.EndTrigger);

			//Parse the procedure calls and eliminate duplicates using a hash map.
			String procedures = elt.substring(bodystarts, bodyends);
			boolean comp = false;
			int procstarts = 0;
			int procends = procedures.length();
			while (comp != true){
				procends = procedures.substring(procstarts,procends).indexOf(";")+1;
				String proccall = procedures.substring(procstarts,procends);
				String found = procs.get(proccall);
				if (found == null){
					Merged.append( proccall );
					procs.put(proccall, "1");
				}
			}

			//Merged.append( elt.substring(bodystarts, bodyends));
		}
		if (Merged.length() > 0){
			Merged.append(otg.EndTrigger);
			result = Merged.toString();
		}
		return result;
	}

	public static int findBodyStart(String elt){
		int bodystarts = elt.indexOf(otg.EndVariableDec) + otg.EndVariableDec.length();
		boolean done = false;
		while (!done){
			String procedureBody = elt.substring(bodystarts);
			if (procedureBody.indexOf(mysqlTrigGenerator.BeginVariableDec) >= 0){
				int delta = procedureBody.indexOf(";")+1;
				bodystarts += delta;
			} else done = true;
		}
		return bodystarts;
	}

	public String TrigName(String trig){
		String BeforeToken = "CREATE TRIGGER";
		String AfterToken = "BEFORE";
		int start = trig.indexOf(BeforeToken)+BeforeToken.length();
		int end = trig.indexOf(AfterToken);
		return trig.substring(start,end).trim();
	}

	public static String MergeTriggers(Vector<String> tz){
		if (verbose){
			System.out.println("MergeTriggers");
			for (int i = 0; i < tz.size(); i++)
				System.out.println("\t "+tz.elementAt(i));
		}
		StringBuffer Merged = new StringBuffer();
		int bodystarts=-1, bodyends=-1;
		boolean InsertDivider = false;
		String result = null;
		String elt;
		HashMap<String,String> procs = new HashMap<String,String>();
		HashMap<String,Vector<String>> ifs = new HashMap<String,Vector<String> >();
		HashMap<String,String> concats = new HashMap<String,String>();
		HashMap<String,String> df = new HashMap<String,String>();

		elt =tz.elementAt(0).toUpperCase();

		//ERROR, with MySQL, The following code should be removed
		//		if (elt.indexOf(otg.BeginVariableDec) >= 0){
		//			Merged.append( elt.substring(0, elt.indexOf(otg.EndVariableDec)+otg.EndVariableDec.length() ) + " SET DELETEKEY = ' '; SET KEYTODELETE = ' '; ");
		//		}
		//		else System.out.println("Error in OracleOptimizeTrigger.MergeTriggers: DECLARE keyword is missing.  All triggers must have this.");
		//		
		if (elt.indexOf(otg.BeginVariableDec) >= 0){
		
			//Merged.append("DROP TRIGGER IF EXISTS "+TrigName(elt)+"; ");
			Merged.append( elt.substring(0, elt.indexOf(otg.EndVariableDec)+otg.EndVariableDec.length() ) );
			Merged.append(" DECLARE DeleteIT_DLL_Val int; DECLARE KEYTODELETE  varchar(2048) DEFAULT ' '; DECLARE DELETEKEY varchar(2048) DEFAULT ' '; ");
		}
		

		//With MySQL, the following code does not work because BEGIN is no longer at the end of the variable declaration
		//We should proceed to BEGIN and iterate DECLARE variables until they are exhausted to identify bodystarts
		for (int i=0; i < tz.size(); i++){
			elt = tz.elementAt(i); //.toUpperCase();
			bodystarts = findBodyStart( elt );
			//bodystarts = elt.indexOf(otg.EndVariableDec) + otg.EndVariableDec.length();
			bodyends = elt.indexOf(otg.EndTrigger);

			//Parse the procedure calls and eliminate duplicates using a hash map.
			String procedureBody=elt;
			if (bodystarts >=0 && bodyends >= 0)
				procedureBody = elt.substring(bodystarts, bodyends);

			boolean comp = false;
			int procstarts = 0;
			int procends = procedureBody.length();
			while (comp != true){
				procends = procedureBody.substring(procstarts,procends).indexOf(";")+1;
				if (procends <= 1){
					comp = true;
					continue;
				}
				String proccall = procedureBody.substring(procstarts,procstarts+procends).trim();
				if (proccall.indexOf("IF (DELETEKEY != ' ') THEN  SET D") == 0){
					int firstEndIf = procedureBody.substring(procstarts).indexOf(" END IF;") + " END IF;".length();
					procstarts += firstEndIf;
					String remainder = procedureBody.substring(procstarts);
					int secEndIf = remainder.indexOf(" END IF;");
					procstarts += secEndIf + " END IF;".length();
				} else if (proccall.indexOf("IF ") >= 0){
					int ifstart = proccall.indexOf("(")+1;
					int ifends = proccall.indexOf(")");
					String predicate = proccall.substring(ifstart, ifends).trim();

					//Advance the pointer
					procstarts += procedureBody.substring(procstarts,procstarts+procends).indexOf(")");

					//Take the whole body of the if statement
					ifstart = procedureBody.substring(procstarts).indexOf(" THEN ") + " THEN ".length();
					ifends = procedureBody.substring(procstarts+ifstart).indexOf(" END IF; ");

					String body = procedureBody.substring(procstarts+ifstart, procstarts+ifstart+ifends).trim();
					//if (body.contains("513")) System.out.println("body "+body);
					procstarts += ifstart+ifends;
					//String body = proccall.substring(0, ifends).trim();

					Vector<String> found = ifs.get(predicate);
					if (found == null){
						Vector<String> V = new Vector<String>();
						V.add(body);
						ifs.put(predicate, V);
					}
					else {
						boolean exists = false;
						for (int k=0; k <found.size(); k++)
							if (found.elementAt(k).contains(body)) exists = true;
						if (! exists)
							found.add(body);
					}
					//ifends = procedureBody.substring(procstarts,procstarts+procends).indexOf(" END IF; ");
					procstarts += " END IF; ".length();
				} else if (proccall.indexOf("SET KEYTODELETE = ' '") >= 0 || proccall.indexOf("SET DeleteIT_DLL_Val = COSAR") >= 0 || proccall.indexOf("SET DELETEKEY = ' '") >= 0 ){
					//Drop tokens for KEYTODELETE
					procstarts += procends;

				} else if (proccall.indexOf("SET KEYTODELETE = CONCAT") >= 0){
					concats.put(proccall, "1");
					procstarts += procends;
				} else if (proccall.indexOf("SET DELETEKEY = CONCAT(DELETEKEY, ")>= 0){
					df.put(proccall, "1");
					procstarts += procends;
				} else {
					String found = procs.get(proccall);
					if (found == null){
						
						if (proccall.indexOf("SET ") != 0 && proccall.indexOf("CALL")<0)
							Merged.append( "CALL " + proccall );
						else Merged.append(" "+proccall);
						
						procs.put(proccall, "1");
						InsertDivider = true; //If there is a follow-on then place dividers
					}
					procstarts += procends;
				}
				procends = procedureBody.length();
				if (procstarts >= procends) comp = true;
			}
		}

		Set<String> dfelt = df.keySet();
		for (String c : dfelt)
		{
			if ((c.indexOf("SET DELETEKEY = CONCAT(DELETEKEY, ' ');") < 0 && c.indexOf("SET DELETEKEY = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE));") < 0)){
				if (InsertDivider){
					Merged.append(" SET DELETEKEY = CONCAT(DELETEKEY, ' ');");
				} else if (df.size() > 1) InsertDivider = true;
				Merged.append(c);
			}
		}

		Set<String> concat = concats.keySet();
		for ( String c : concat)
		{
			int stidx = c.indexOf("CONCAT");
			int eidx = c.indexOf(";");
			String cc = c.substring(stidx, eidx);
			if (InsertDivider) {
				Merged.append(" SET DELETEKEY = CONCAT(DELETEKEY, ' ');");
			} else if (concat.size() > 1) InsertDivider = true;
			Merged.append(" SET DELETEKEY = CONCAT(DELETEKEY, "+cc+");");
			//Merged.append(" DELETEKEY = CONCAT(DELETEKEY, ' ');");
		}

		if (InsertDivider && ifs.keySet().size() > 0) //Insert a space if there is more to come
			Merged.append(" SET DELETEKEY = CONCAT(DELETEKEY, ' ');");

		Set<String> keys = ifs.keySet();
		//		String body;
		//		for ( String key : keys)
		//		{
		//			body = ifs.get(key);
		//			if (body.indexOf("KosarDelete") < 0) Merged.append(" IF ("+key+") THEN "+body+" END IF; ");
		//		}

		for ( String key : keys)
		{
			Vector<String> bd = ifs.get(key);
			if (bd == null){
				System.out.println("Error, body of the if statement is empty.  Condition is "+key);
				continue;
			} else {
				if (bd.size() == 1 && bd.elementAt(0).indexOf("KosarDelete") >= 0) ;
				else {
					Merged.append(" IF ("+key+") THEN ");
					if (bd.size() == 1)
						Merged.append(" "+bd.elementAt(0));
					else{
						//Eliminate duplicate procedure calls and invocations across different if statements
						Merged.append(MergeTriggers(bd));
					}
					Merged.append(" END IF; ");
				}
			}
		}


		if (Merged.length() > 0 && bodystarts >=0 && bodyends >= 0){
			Merged.append(otg.EndTrigger);
		}
		return Merged.toString();
	}

	public static String RewriteProc(String proc){
		String newst = proc.replaceAll("/[*], INOUT DELETEKEY varchar[(]2048[)][*]/", ", INOUT DELETEKEY varchar(2048)");
		newst = newst.replaceAll("DECLARE DeleteIT_DLL_Val int;", "");
		newst = newst.replaceAll("SET DeleteIT_DLL_Val = KOSARTriggerDeleteCall[(]'RAYS', KEYTODELETE, 0[)];", "SET DeleteKey = CONCAT(DELETEKEY, CONCAT(' ', KEYTODELETE));".toUpperCase());
		return newst;
	}

	public static void OptimizeProcs(Vector<String> procs, Vector<String> outV){
		//For each procedure, extend its argument to consume a Delete key as an input argument
		//Replace the text of KOSARTriggerDeleteCall with the concat call.
		for (int i = 0; i < procs.size(); i++){
			outV.add(RewriteProc(procs.elementAt(i)));
		}
	}

	public static String OptimizeTrigger(String trig){
		trig = trig.replaceAll("/[*] DECLARE DELETEKEY varchar[(]2048[)] DEFAULT ' '; [*]/", "DECLARE DELETEKEY varchar(2048) DEFAULT ' ';");
		//trig = trig.replaceAll("SET KEYTODELETE", "SET DELETEKEY = ' '; SET KEYTODELETE");
		trig = trig.replaceAll("SET DELETEKEY = ' '; SET DELETEKEY = CONCAT(DELETEKEY, ' ');", "SET DELETEKEY = ' ';");
		//trig = trig.replaceAll("/* DeleteKey IN OUT CLOB; */".toUpperCase(), "DeleteKey IN OUT CLOB;".toUpperCase());
		//trig = trig.replaceAll("/[*]DeleteKey = ' ';[*]/".toUpperCase(), "DeleteKey = ' ';".toUpperCase());
		trig = trig.replaceAll("/[*], DELETEKEY[*]/", ", DELETEKEY");
		trig = trig.replaceAll("SET DeleteIT_DLL_Val = KOSARTriggerDeleteCall[(]'RAYS', KEYTODELETE, 0[)];", "SET DeleteKey = CONCAT(DeleteKey, CONCAT(' ', KeyToDelete));".toUpperCase());
		if (trig.indexOf("IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KosarDelete(") < 0)
			trig = trig.replaceAll("END;", "IF (DELETEKEY != ' ') THEN  SET DeleteIT_DLL_Val = KosarDelete('"+MySQLQueryToTrigger.getIPport()+"', 'RAYS', DELETEKEY, 0); IF (DeleteIT_DLL_Val != 0) THEN UPDATE `Failed to connect to KOSAR KVS CORE.` SET x=1; END IF; END IF; END;");
		trig = trig.replaceAll("DECLARE DELETEKEY varchar[(]2048[)] DEFAULT ' ';  SET DELETEKEY = ' ';","DECLARE DELETEKEY varchar(2048) DEFAULT ' ';");


		return trig;
	}

	public static void Optimize(Vector<String> inV, Vector<String> outV, int Level){
		String elt;
		Vector<String> InsertTrigs = new Vector<String>();
		Vector<String> DeleteTrigs = new Vector<String>();
		Vector<String> UpdateTrigs = new Vector<String>();

		Vector<String> InsertKeys = new Vector<String>();
		Vector<String> DeleteKeys = new Vector<String>();
		Vector<String> UpdateKeys = new Vector<String>();

		Vector<String> tz = new Vector<String>();
		Vector<String> tzkey = new Vector<String>();

		Vector<String> procs = new Vector<String>();
		Vector<String> procskey = new Vector<String>();

		String tgtTable, optTrigger;

		if (Level < 1 || Level > 2){
			//Simply copy the vectors and return
			for (int i = 0; i < inV.size(); i++) outV.addElement(inV.elementAt(i));

			return;
		}


		if (inV == null){
			System.out.println("Error (OracleOptimizeTriggers:Optimize):  Input Vector of triggers/procedures is null.");
			System.out.println("Suggested fix:  Pass in a vector populated by the QueryToTrigger with triggers/procedures.");
			return;
		}
		if (outV == null){
			System.out.println("Error (OracleOptimizeTriggers:Optimize):  Output vector of triggers/procedures is null.");
			System.out.println("Suggested fix:  Initialize a vector and pass it in.  Optimize populates this outV with a re-write of triggers.");
			return;
		}



		//Make a pass of inV to (a) push all the stored procedures into outV and (b) construct a vector of triggers
		for (int i =0; i < inV.size(); i++){
			elt = inV.elementAt(i); //.toUpperCase();
			if (elt.indexOf(otg.StartProc) >=0 && Level == 1){
				outV.add(elt);
			}
			if (elt.indexOf(otg.StartProc) >=0 && Level == 2){
				procs.add(elt);
			}

			if (elt.indexOf(otg.StartTrig) >=0){
				if (elt.indexOf(otg.insert) >= 0){
					InsertTrigs.add(elt);
				}
				else if (elt.indexOf(otg.delete) >=0 ){
					DeleteTrigs.add(elt);
				}
				else if (elt.indexOf(otg.update) >=0 ){
					UpdateTrigs.add(elt);
				}
				else System.out.println("Error (OracleOptimizeTriggers:Optimize): trigger not recognized.");
			}
		}

		if (Level==2){
			OptimizeProcs(procs, outV);
		}

		while (InsertTrigs.size() > 0){
			int i = InsertTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			//			Vector<String> ks = new Vector<String>();
			//			String LastKey = InsertKeys.elementAt(i);
			//			ks.add(LastKey);
			//Get Trigger name
			elt = InsertTrigs.elementAt(i);
			tgtTable = TargetInsertTable(elt);

			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = InsertTrigs.elementAt(i);
				String newtgt = TargetInsertTable(elt);
				if (newtgt.equals(tgtTable)){
					tz.add(elt);
					InsertTrigs.removeElementAt(i);
					//				
					//					if (! InsertKeys.elementAt(i).equals(LastKey)){
					//						LastKey = InsertKeys.elementAt(i);
					//						ks.add(LastKey);
					//					}
					//					InsertKeys.removeElementAt(i);
				}
			}

			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger = MergeTriggers(tz);
				outV.add(optTrigger);
				//				for (int j = 0; j < ks.size(); j++){
				//					outV.add(optTrigger);
				//					outKey.add(ks.elementAt(j));
				//				}
			}
			//InsertTrigs.removeElementAt(tgt);
		}

		while (DeleteTrigs.size() > 0){
			int i = DeleteTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			//			Vector<String> ks = new Vector<String>();
			//			String LastKey = DeleteKeys.elementAt(i);
			//			ks.add(LastKey);
			//Get Trigger name
			elt = DeleteTrigs.elementAt(i);
			tgtTable = TargetDeleteTable(elt);
			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = DeleteTrigs.elementAt(i);
				String newtgt = TargetDeleteTable(elt);
				if (newtgt.equals(tgtTable)) {
					tz.add(elt);
					DeleteTrigs.removeElementAt(i);

					//					if (! DeleteKeys.elementAt(i).equals(LastKey)){
					//						LastKey = DeleteKeys.elementAt(i);
					//						ks.add(LastKey);
					//					}
					//					DeleteKeys.removeElementAt(i);
				}
			}
			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger = MergeTriggers(tz);
				outV.add(optTrigger);
				//				for (int j = 0; j < ks.size(); j++){
				//					outV.add(optTrigger);
				//				}
			}
			//DeleteTrigs.removeElementAt(tgt);
		}

		while (UpdateTrigs.size() > 0){
			int i = UpdateTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			//			Vector<String> ks = new Vector<String>();
			//			String LastKey = UpdateKeys.elementAt(i);
			//			ks.add(LastKey);
			//Get Trigger name
			elt = UpdateTrigs.elementAt(i);
			tgtTable = TargetUpdateTable(elt);
			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = UpdateTrigs.elementAt(i);
				String newtgt = TargetUpdateTable(elt);
				if (newtgt.equals(tgtTable)){
					tz.add(elt);
					UpdateTrigs.removeElementAt(i);

					//					if (! UpdateKeys.elementAt(i).equals(LastKey)){
					//						LastKey = UpdateKeys.elementAt(i);
					//						ks.add(LastKey);
					//					}
					//UpdateKeys.removeElementAt(i);
				}
			}
			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger=MergeTriggers(tz);
				outV.add(optTrigger);
				//				for (int j = 0; j < ks.size(); j++){
				//					outV.add(optTrigger);
				//				}
			}
			//UpdateTrigs.removeElementAt(tgt);
		}

		if (Level == 2){
			for (int i = 0; i < outV.size(); i++){
				if (outV.elementAt(i).indexOf(otg.StartTrig) >=0){
					String newtrig = OptimizeTrigger(outV.elementAt(i));
					outV.setElementAt(newtrig, i);
				}
			}
		}

		return;
	}

	public void OldOptimize(Vector<String> inV, Vector<String> outV, Vector<String> inKey, Vector<String> outKey, int Level){
		String elt;
		Vector<String> InsertTrigs = new Vector<String>();
		Vector<String> DeleteTrigs = new Vector<String>();
		Vector<String> UpdateTrigs = new Vector<String>();

		Vector<String> InsertKeys = new Vector<String>();
		Vector<String> DeleteKeys = new Vector<String>();
		Vector<String> UpdateKeys = new Vector<String>();

		Vector<String> tz = new Vector<String>();
		Vector<String> tzkey = new Vector<String>();

		Vector<String> procs = new Vector<String>();
		Vector<String> procskey = new Vector<String>();

		String tgtTable, optTrigger;

		if (Level < 1 || Level > 2){
			//Simply copy the vectors and return
			for (int i = 0; i < inV.size(); i++) outV.addElement(inV.elementAt(i));

			for (int i = 0; i < inKey.size(); i++) outKey.addElement(inKey.elementAt(i));
			return;
		}

		//		if (Level < 1 || Level > 2){
		//			System.out.println("Error, Level value "+Level+" is not recognized and returning.");
		//			System.out.println("Optimize supports only two levels of optimizations:");
		//			System.out.println("\tLevel 1:  Multiple triggers on the same table are merged into one.\n");
		//			System.out.println("\tLevel 2:  Keys produced by different procedures referenced by a trigger are merged into one COSAR delete call.\n");
		//			System.out.println("To fix the error, ensure that Optimize method of OracleOptimizeTrigg is invoked with either 1 or 2 as the value of Level");
		//			return;
		//		}

		if (inV.size() != inKey.size()){
			System.out.println("Error (OracleOptimizeTriggers:Optimize):  Number of triggers "+inV.size()+" does not match the number of keys "+inKey.size());
			System.out.println("These must match in order for the optimization to proceed forward.");
			if (inV.size() > inKey.size())
				System.out.println("Suggested solution:  Generate duplicate keys by repeating them to match the size of the two vectors.");
			else System.out.println("Suggested solution:  Generate duplicate triggers by repeating their body such that their total number matches the number of keys");
		}

		if (inV == null){
			System.out.println("Error (OracleOptimizeTriggers:Optimize):  Input Vector of triggers/procedures is null.");
			System.out.println("Suggested fix:  Pass in a vector populated by the QueryToTrigger with triggers/procedures.");
			return;
		}
		if (outV == null){
			System.out.println("Error (OracleOptimizeTriggers:Optimize):  Output vector of triggers/procedures is null.");
			System.out.println("Suggested fix:  Initialize a vector and pass it in.  Optimize populates this outV with a re-write of triggers.");
			return;
		}



		//Make a pass of inV to (a) push all the stored procedures into outV and (b) construct a vector of triggers
		for (int i =0; i < inV.size(); i++){
			elt = inV.elementAt(i); //.toUpperCase();
			if (elt.indexOf(otg.StartProc) >=0 && Level == 1){
				outV.add(elt);
				outKey.add(inKey.elementAt(i));
			}
			if (elt.indexOf(otg.StartProc) >=0 && Level == 2){
				procs.add(elt);
				procskey.add(inKey.elementAt(i));
			}

			if (elt.indexOf(otg.StartTrig) >=0){
				if (elt.indexOf(otg.insert) >= 0){
					InsertTrigs.add(elt);
					InsertKeys.add(inKey.elementAt(i));
				}
				else if (elt.indexOf(otg.delete) >=0 ){
					DeleteTrigs.add(elt);
					DeleteKeys.add(inKey.elementAt(i));
				}
				else if (elt.indexOf(otg.update) >=0 ){
					UpdateTrigs.add(elt);
					UpdateKeys.add(inKey.elementAt(i));
				}
				else System.out.println("Error (OracleOptimizeTriggers:Optimize): trigger not recognized.");
			}
		}

		if (Level==2){
			OptimizeProcs(procs, outV);
			for (int i = 0; i < procs.size(); i++){
				outKey.add(procskey.elementAt(i));
			}
		}

		while (InsertTrigs.size() > 0){
			int i = InsertTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			Vector<String> ks = new Vector<String>();
			String LastKey = InsertKeys.elementAt(i);
			ks.add(LastKey);
			//Get Trigger name
			elt = InsertTrigs.elementAt(i);
			tgtTable = TargetInsertTable(elt);

			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = InsertTrigs.elementAt(i);
				String newtgt = TargetInsertTable(elt);
				if (newtgt.equals(tgtTable)){
					tz.add(elt);
					InsertTrigs.removeElementAt(i);

					if (! InsertKeys.elementAt(i).equals(LastKey)){
						LastKey = InsertKeys.elementAt(i);
						ks.add(LastKey);
					}
					InsertKeys.removeElementAt(i);
				}
			}

			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
				outKey.add(ks.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger = MergeTriggers(tz);
				//				for (int j = 0; j < ks.size(); j++){
				//					outV.add(optTrigger);
				//					outKey.add(ks.elementAt(j));
				//				}
			}
			//InsertTrigs.removeElementAt(tgt);
		}

		while (DeleteTrigs.size() > 0){
			int i = DeleteTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			Vector<String> ks = new Vector<String>();
			String LastKey = DeleteKeys.elementAt(i);
			ks.add(LastKey);
			//Get Trigger name
			elt = DeleteTrigs.elementAt(i);
			tgtTable = TargetDeleteTable(elt);
			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = DeleteTrigs.elementAt(i);
				String newtgt = TargetDeleteTable(elt);
				if (newtgt.equals(tgtTable)) {
					tz.add(elt);
					DeleteTrigs.removeElementAt(i);

					if (! DeleteKeys.elementAt(i).equals(LastKey)){
						LastKey = DeleteKeys.elementAt(i);
						ks.add(LastKey);
					}
					DeleteKeys.removeElementAt(i);
				}
			}
			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
				outKey.add(ks.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger = MergeTriggers(tz);
				for (int j = 0; j < ks.size(); j++){
					outV.add(optTrigger);
					outKey.add(ks.elementAt(j));
				}
			}
			//DeleteTrigs.removeElementAt(tgt);
		}

		while (UpdateTrigs.size() > 0){
			int i = UpdateTrigs.size()-1;
			int tgt = i;
			tz = new Vector<String>();
			Vector<String> ks = new Vector<String>();
			String LastKey = UpdateKeys.elementAt(i);
			ks.add(LastKey);
			//Get Trigger name
			elt = UpdateTrigs.elementAt(i);
			tgtTable = TargetUpdateTable(elt);
			//Collect all triggers with the same name
			for ( ; i >= 0; i--){
				elt = UpdateTrigs.elementAt(i);
				String newtgt = TargetUpdateTable(elt);
				if (newtgt.equals(tgtTable)){
					tz.add(elt);
					UpdateTrigs.removeElementAt(i);

					if (! UpdateKeys.elementAt(i).equals(LastKey)){
						LastKey = UpdateKeys.elementAt(i);
						ks.add(LastKey);
					}
					UpdateKeys.removeElementAt(i);
				}
			}
			//If the count of these triggers is one then there is no need for optimization
			if (tz.size() == 1) {
				outV.add(tz.elementAt(0));
				outKey.add(ks.elementAt(0));
			}
			//Otherwise, optimize and insert the final trigger into outV 
			else {
				optTrigger=MergeTriggers(tz);
				for (int j = 0; j < ks.size(); j++){
					outV.add(optTrigger);
					outKey.add(ks.elementAt(j));
				}
			}
			//UpdateTrigs.removeElementAt(tgt);
		}

		if (Level == 2){
			for (int i = 0; i < outV.size(); i++){
				if (outV.elementAt(i).indexOf(otg.StartTrig) >=0)
					outV.setElementAt(OptimizeTrigger(outV.elementAt(i)), i);
			}
		}

		return;
	}

	public static void main(String[] args) {
		MySQLOptimizeTriggers tg = new MySQLOptimizeTriggers();
		Vector<String> inV = new Vector<String>();
		Vector<String> outV = new Vector<String>();

		Vector<String> inK = new Vector<String>();
		Vector<String> outK = new Vector<String>();

		for (int i = 0; i < 2; i++)
			inK.add("5uc.virtual_clip_id=rv.virtual_clip_idu.user_id=rv.recommended_to");
		for (int i=2; i < sampleqrys.length; i++)
			inK.add("5uc.virtual_clip_id=rv.virtual_clip_idu.user_id=rv.recommended_to");

		System.out.println("\n--------------------------------------------");
		for (int i = 0; i < sampleqrys.length; i++){
			System.out.println(""+i+".  Key="+inK.elementAt(i)+", "+sampleqrys[i]);
			inV.add(sampleqrys[i]);
		}

		System.out.println("\n--------------------------------------------");
		tg.Optimize(inV, outV, 2);
		for (int i=0; i < outV.size(); i++){
			System.out.println(""+outV.elementAt(i));
		}
	}
}
