package com.mitrallc.UnitTest;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mitrallc.config.DBConnector;
import com.mitrallc.mysqltrig.regthread;

public class SetupKosarTriggers {
	
	public static String[] cores_arr = new String[] {
		"10.0.0.210:8888",
		"10.0.0.220:8888",
		"10.0.0.175:8888",
	};
	public static int num_cores = 1;

	public static void RegTrigMultiDelete()
	{

		Connection conn = null;
		Statement st = null;

		try
		{
			Class.forName ("oracle.jdbc.driver.OracleDriver");
			String url = "jdbc:oracle:thin:@10.0.0.245:1521:orcl";
			String username = "hieunguyen";
			String password = "hieunguyen123";
			conn = DriverManager.getConnection(url,username,password);
			st = conn.createStatement();


			st.execute("CREATE OR REPLACE LIBRARY COSAR_DLL_LIB AS \'C:\\hieun\\COSARTriggerAPI.dll\';");


			//			st.execute(
			//				"CREATE OR REPLACE FUNCTION COSARDeleteCall  ( " +
			//				"		   datazone   IN VARCHAR2, " +
			//				"		   cosar_key  IN VARCHAR2, " +
			//				"		   flags	  IN BINARY_INTEGER ) "+
			//				"		RETURN BINARY_INTEGER " +
			//				"		AS LANGUAGE C "+
			//				"		   LIBRARY COSAR_DLL_LIB  "+
			//				"		   NAME \"COSARDelete\"  "+
			//				"		   PARAMETERS ( "+
			//				"		      datazone, "+
			//				"		      datazone LENGTH, "+
			//				"		      cosar_key, "+
			//				"		      cosar_key LENGTH, "+
			//				"			  flags int); "
			//			);
			//			
			//			st.execute(
			//					"CREATE OR REPLACE FUNCTION COSARTriggerDeleteCall  ( " +
			//					"		   datazone   IN VARCHAR2, " +
			//					"		   cosar_key  IN VARCHAR2, " +
			//					"		   flags	  IN BINARY_INTEGER ) "+
			//					"		RETURN BINARY_INTEGER " +
			//					"		AS LANGUAGE C "+
			//					"		   LIBRARY COSAR_DLL_LIB  "+
			//					"		   NAME \"COSARTriggerDelete\"  "+
			//					"		   PARAMETERS ( "+
			//					"		      datazone, "+
			//					"		      datazone LENGTH, "+
			//					"		      cosar_key, "+
			//					"		      cosar_key LENGTH, "+
			//					"			  flags int); "
			//				);
			/*st.execute("CREATE OR REPLACE FUNCTION TCNTriggerDeleteMultiCall  ( " +
					"          ipaddrs    IN VARCHAR2, " +
					"		   datazone   IN VARCHAR2, " +
					"		   cosar_key  IN VARCHAR2, " +
					"		   flags	  IN BINARY_INTEGER ) "+
					"		RETURN BINARY_INTEGER " 
					);*/
			st.execute(
					"CREATE OR REPLACE FUNCTION KOSARTriggerDeleteMultiCall  ( " +
							"          ipaddrs    IN VARCHAR2, " +
							"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2, " +
							"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"KOSARTriggerDeleteMulti\"  "+
							"		   PARAMETERS ( "+				
							"             ipaddrs,  "+
							"             ipaddrs LENGTH, "+
							"		      datazone, "+
							"		      datazone LENGTH, "+						
							"		      cosar_key, "+
							"		      cosar_key LENGTH, "+
							"			  flags int); "
					);

			st.execute(
					"CREATE OR REPLACE FUNCTION KOSARServerTrigDelMulti  ( " +
							"client_id IN NUMBER, "+
							"trans_id IN NUMBER, "+
							//					"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2) " +
							//					"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"KOSARTrigDelMultiKeys\"  "+
							"		   PARAMETERS ( "+
							//					"		      datazone, "+
							//					"		      datazone LENGTH, "+
							"			  client_id, "+
							"			  trans_id, "+
							"		      cosar_key, "+
							"		      cosar_key LENGTH); "
							//					"			  flags int); "
					);
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}	
		finally
		{
			try
			{
				if( st != null )
				{
					st.close();
				}
				if( conn != null )
				{
					conn.close();				
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}			
		}
	}

	public static void RegTrigSingleDelete()
	{

		Connection conn = null;
		Statement st = null;

		try
		{
			Class.forName ("oracle.jdbc.driver.OracleDriver");
			String url = DBConnector.getConnectionString(); 
			conn = DriverManager.getConnection(url,DBConnector.getUsername(),DBConnector.getPassword());
			st = conn.createStatement();


			st.execute("CREATE OR REPLACE LIBRARY COSAR_DLL_LIB AS \'C:\\cosar\\x64\\release\\COSARTriggerAPI.dll\';");


			st.execute(
					"CREATE OR REPLACE FUNCTION COSARDeleteCall  ( " +
							"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2, " +
							"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"COSARDelete\"  "+
							"		   PARAMETERS ( "+
							"		      datazone, "+
							"		      datazone LENGTH, "+
							"		      cosar_key, "+
							"		      cosar_key LENGTH, "+
							"			  flags int); "
					);

			st.execute(
					"CREATE OR REPLACE FUNCTION COSARTriggerDeleteCall  ( " +
							"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2, " +
							"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"COSARTriggerDelete\"  "+
							"		   PARAMETERS ( "+
							"		      datazone, "+
							"		      datazone LENGTH, "+
							"		      cosar_key, "+
							"		      cosar_key LENGTH, "+
							"			  flags int); "
					);

			st.execute(
					"CREATE OR REPLACE FUNCTION COSARTriggerDeleteMultiCall  ( " +
							"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2, " +
							"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"COSARTriggerDeleteMulti\"  "+
							"		   PARAMETERS ( "+
							"		      datazone, "+
							"		      datazone LENGTH, "+
							"		      cosar_key, "+
							"		      cosar_key LENGTH, "+
							"			  flags int); "
					);


			st.execute(
					"CREATE OR REPLACE FUNCTION COSARDeleteMultiCall  ( " +
							"		   datazone   IN VARCHAR2, " +
							"		   cosar_key  IN VARCHAR2, " +
							"		   flags	  IN BINARY_INTEGER ) "+
							"		RETURN BINARY_INTEGER " +
							"		AS LANGUAGE C "+
							"		   LIBRARY COSAR_DLL_LIB  "+
							"		   NAME \"COSARDeleteMulti\"  "+
							"		   PARAMETERS ( "+
							"		      datazone, "+
							"		      datazone LENGTH, "+
							"		      cosar_key, "+
							"		      cosar_key LENGTH, "+
							"			  flags int); "
					);

			st.execute(
					"CREATE OR REPLACE PROCEDURE invalidateFriendCacheEntry ( "+
							"			user_id IN NUMBER, "+
							"			recursion IN NUMBER ) "+
							" IS "+
							"  		ffid friends.friendid%TYPE; " +
							"  		fuid friends.userid%TYPE; " +
							"       ret_val BINARY_INTEGER; " +
							"  		k3  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  		friendprofile  CLOB := TO_CLOB('ProfileJSP'); " +
							"  		ids CLOB; " +
							"  		CURSOR FriendCursor IS " +
							"			select userid, friendid " +
							"			from friends " +
							"			where status='2' and " +
							"(userid=user_id OR " +
							"friendid=user_id); " +
							" BEGIN "+
							//" 	dbms_output.put_line('COSAR DELETE ' || CONCAT(friendprofile,user_id) ); "+
							"   ret_val := COSARDeleteCall('RAYS',CONCAT(friendprofile,user_id), 0); "+
							"  	OPEN FriendCursor; " +
							"  	LOOP " +
							"     FETCH FriendCursor INTO fuid, ffid; " +
							"     EXIT WHEN FriendCursor%NOTFOUND; " +
							"    IF ffid != user_id THEN " +
							"		ids := CONCAT(ffid,'-'); " + 
							"		ids := CONCAT(ids,user_id); " +		
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k3,ids),0); " +
							//"		dbms_output.put_line('InvalidateFriendCacheEntry recursion = ' || recursion || ': k3 = ' || CONCAT(k3,ids) ); " +
							"       IF (recursion!=1) THEN invalidateFriendCacheEntry(ffid, 1); END IF; " +
							"     END IF; " +
							"    IF fuid != user_id THEN " + 
							"		ids := CONCAT(fuid,'-'); " +
							"		ids := CONCAT(ids,user_id); " +		
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k3,ids),0); " +
							//"		dbms_output.put_line('InvalidateFriendCacheEntry recursion = ' || recursion || 'k3 = ' || CONCAT(k3,ids) ); " + 
							"      IF (recursion!=1) THEN invalidateFriendCacheEntry(fuid, 1); END IF; " +
							"    END IF; 	" +
							" END LOOP; " +
							" CLOSE FriendCursor; " + 
							" END; "
					);


			/*
			st.execute(		
					"CREATE OR REPLACE TRIGGER COSAR_DLL_TRIGGER " +
					"AFTER UPDATE ON cosar.test_table " +
					"DECLARE " +
					"	func_ret_val BINARY_INTEGER; " +
					"BEGIN " +
					"	func_ret_val := COSARDeleteCall(\'testkey1\',\'testValue1\',0); " +
					"   dbms_output.put_line('func_ret_val = ' || func_ret_val); " +
					"END; "
			);
			//*/

			st.execute(
					"CREATE OR REPLACE TRIGGER StreamStatusChg3 " +
							"BEFORE UPDATE ON user_cameras " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  k1  CLOB := TO_CLOB('CameraTabJSP'); " +
							"  k2  CLOB := TO_CLOB('ProfileUserInformationJSP'); " +
							"  k3  CLOB := TO_CLOB('LoadLivestreamsJSP'); " +
							"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  k5  CLOB := TO_CLOB('ProfileJSP'); " +
							"  ids CLOB; " +
							"  ret_val BINARY_INTEGER; " +
							"BEGIN " +
							"  uid := :NEW.user_id; " +
							/* dbms_output.put_line('Trigger is before; uid = ' || uid); */
							"  k1 := CONCAT(k1,uid); " +
							"  k2 := CONCAT(k2,uid); " +
							"  k3 := CONCAT(k3,uid); " +
							"  ret_val := COSARDeleteCall('RAYS',k1,0); " +
							//"  dbms_output.put_line('k1 delete ret_val=' || ret_val); " +
							"  ret_val := COSARDeleteCall('RAYS',k2,0); " +
							//"  dbms_output.put_line('k2 delete ret_val=' || ret_val); " +
							"  ret_val := COSARDeleteCall('RAYS',k3,0); " +
							//"  dbms_output.put_line('k1 = ' || k1 || ', k2 = ' || k2 || ', k3 = ' || k3); " +  
							"  invalidateFriendCacheEntry(uid, 0); " + 
							" END; "
					);

			//			st.execute(
			//					"CREATE OR REPLACE TRIGGER StreamStatusChg3 " +
			//					"BEFORE UPDATE ON user_cameras " +
			//					"FOR EACH ROW " +
			//					"DECLARE " +
			//					"  uid number := 5; " +
			//					"  k1  CLOB := TO_CLOB('CameraTabJSP'); " +
			//					"  k2  CLOB := TO_CLOB('ProfileUserInformationJSP'); " +
			//					"  k3  CLOB := TO_CLOB('LoadLivestreamsJSP'); " +
			//					"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
			//					"  k5  CLOB := TO_CLOB('ProfileJSP'); " +
			//					"  ids CLOB; " +
			//					"  ret_val BINARY_INTEGER; " +
			//					"  ffid friends.friendid%TYPE; " +
			//					"  fuid friends.userid%TYPE; " +
			//					"  CURSOR FriendCursor IS " +
			//					"	   select friendid, userid from friends " +
			//					"	   where userid in " +
			//					"		(select friendid " +
			//					"		from friends " +
			//					"		where status='2' AND ( userid=uid OR friendid=uid )) " +
			//					"	   OR friendid in " +
			//					"		(select userid " +
			//					"		from friends " +
			//					"		where status='2' AND ( userid=uid OR friendid=uid )); " +
			//					"BEGIN " +
			//					"  uid := :NEW.user_id; " +
			//					  /* dbms_output.put_line('Trigger is before; uid = ' || uid); */
			//					"  k1 := CONCAT(k1,uid); " +
			//					"  k2 := CONCAT(k2,uid); " +
			//					"  k3 := CONCAT(k3,uid); " +
			//					"  dbms_output.put_line('k1 = ' || k1 || ', k2 = ' || k2); " +
			//					"  ret_val := COSARDeleteCall('RAYS',k1,0); " +
			//					//"  dbms_output.put_line('k1 delete ret_val=' || ret_val); " +
			//					"  ret_val := COSARDeleteCall('RAYS',k2,0); " +
			//					//"  dbms_output.put_line('k2 delete ret_val=' || ret_val); " +
			//					"  ret_val := COSARDeleteCall('RAYS',k3,0); " +
			//					"  OPEN FriendCursor; " +
			//					"  LOOP " +
			//					"     FETCH FriendCursor INTO ffid, fuid; " +
			//					"     EXIT WHEN FriendCursor%NOTFOUND; " +
			//					"	  ids := CONCAT(fuid, '-'); " +
			//					"	  ids := CONCAT(ids, ffid); " +
			//					"     ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
			//					"	  ids := CONCAT(ffid, '-'); " +
			//					"	  ids := CONCAT(ids, fuid); " +
			//					"     ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
			//					"     IF ffid = uid THEN " +
			//					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k5,fuid),0); " +
			//					"	  END IF; " +
			//					"     IF fuid = uid THEN " +
			//					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k5,ffid),0); " +
			//					"	  END IF; " +
			////					"     IF ffid != uid THEN " +
			////					"		ids := CONCAT(uid,'-'); " +
			////					"		ids := CONCAT(ids,ffid); " +		
			////					//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
			////					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
			////					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k5,ffid),0); " +
			////					"     END IF; " +
			////					"     IF fuid != uid THEN " +
			////					"		ids := CONCAT(fuid,'-'); " +
			////					"		ids := CONCAT(ids,uid); " +		
			////					//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
			////					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
			////					"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k5,fuid),0); " +
			////					"     END IF; " +					
			//					"  END LOOP; " +
			//					"  CLOSE FriendCursor; " +
			//					"END; "
			//			);
			//			

			st.execute(
					"CREATE OR REPLACE TRIGGER UserInfoChangeTrigger " +
							"BEFORE UPDATE OR DELETE ON users " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  k1  CLOB := TO_CLOB('ProfileJSP'); " +
							"  k2  CLOB := TO_CLOB('RetrFrndsJSP'); " +					
							"  k3  CLOB := TO_CLOB('LoadLivestreamsJSP'); " +					
							"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  ids CLOB; " +
							//"  k5  CLOB := TO_CLOB('ProfileUserInformationJSP'); " +
							"  ret_val BINARY_INTEGER; " +
							"  ffid friends.friendid%TYPE; " +
							"  fuid friends.userid%TYPE; " +
							"  CURSOR FriendCursor IS " +
							"      select friendid, userid from friends " +
							"      where status='2' AND ( userid=uid OR friendid=uid ); " +
							"BEGIN " +
							"  uid := :OLD.user_id; " +
							/* dbms_output.put_line('Trigger is before; uid = ' || uid); */
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,uid),0); " +
							"  dbms_output.put_line('k2 delete ret_val=' || ret_val); " +
							"  OPEN FriendCursor; " +
							"  LOOP " +
							"     FETCH FriendCursor INTO ffid, fuid; " +
							"     EXIT WHEN FriendCursor%NOTFOUND; " +
							"     IF ffid != uid THEN " +
							"		ids := CONCAT(uid,'-'); " +
							"		ids := CONCAT(ids,ffid); " +		
							//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +
							"     IF fuid != uid THEN " +
							"		ids := CONCAT(fuid,'-'); " +
							"		ids := CONCAT(ids,uid); " +		
							//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +					
							"  END LOOP; " +
							"  CLOSE FriendCursor; " +
							"END; "
					);


			st.execute(
					"CREATE OR REPLACE TRIGGER UserInfoInsertTrigger " +
							"BEFORE INSERT ON users " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  k1  CLOB := TO_CLOB('ProfileJSP'); " +
							"  k2  CLOB := TO_CLOB('RetrFrndsJSP'); " +					
							"  k3  CLOB := TO_CLOB('LoadLivestreamsJSP'); " +					
							"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  ids CLOB; " +
							//"  k5  CLOB := TO_CLOB('ProfileUserInformationJSP'); " +
							"  ret_val BINARY_INTEGER; " +
							"  ffid friends.friendid%TYPE; " +
							"  fuid friends.userid%TYPE; " +
							"  CURSOR FriendCursor IS " +
							"      select friendid, userid from friends " +
							"      where status='2' AND ( userid=uid OR friendid=uid ); " +
							"BEGIN " +
							"  uid := :NEW.user_id; " +
							/* dbms_output.put_line('Trigger is before; uid = ' || uid); */
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,uid),0); " +
							"  dbms_output.put_line('k2 delete ret_val=' || ret_val); " +
							"  OPEN FriendCursor; " +
							"  LOOP " +
							"     FETCH FriendCursor INTO ffid, fuid; " +
							"     EXIT WHEN FriendCursor%NOTFOUND; " +
							"     IF ffid != uid THEN " +
							"		ids := CONCAT(uid,'-'); " +
							"		ids := CONCAT(ids,ffid); " +		
							//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +
							"     IF fuid != uid THEN " +
							"		ids := CONCAT(fuid,'-'); " +
							"		ids := CONCAT(ids,uid); " +		
							//"		dbms_output.put_line('k4 = ' || CONCAT(k4,ids) ); " +
							"     	ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +					
							"  END LOOP; " +
							"  CLOSE FriendCursor; " +
							"END; "
					);

			st.execute(
					"CREATE OR REPLACE TRIGGER FriendshipChangeTrigger " +
							"BEFORE UPDATE OR DELETE ON friends " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  fid number; " +
							"  k1  CLOB := TO_CLOB('ProfileJSP'); " +
							"  k2  CLOB := TO_CLOB('RetrFrndsJSP'); " +					
							"  k3  CLOB := TO_CLOB('LoadRecentRecomVideosJSP'); " +					
							"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  ids CLOB; " +
							"  ret_val BINARY_INTEGER; " +
							"BEGIN " +
							"  uid := :OLD.userid; " +
							"  fid := :OLD.friendid; " +
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,fid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,fid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,fid),0); " +
							"  ids := CONCAT( uid, '-' ); " +
							"  ids := CONCAT( ids, fid ); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"  ids := CONCAT( fid, '-' ); " +
							"  ids := CONCAT( ids, uid ); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"  dbms_output.put_line('delete ret_val=' || ret_val); " +					
							"END; "
					);


			st.execute(
					"CREATE OR REPLACE TRIGGER FriendshipInsertTrigger " +
							"BEFORE INSERT ON friends " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  fid number; " +
							"  k1  CLOB := TO_CLOB('ProfileJSP'); " +
							"  k2  CLOB := TO_CLOB('RetrFrndsJSP'); " +					
							"  k3  CLOB := TO_CLOB('LoadRecentRecomVideosJSP'); " +					
							"  k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"  ids CLOB; " +
							"  ret_val BINARY_INTEGER; " +
							"BEGIN " +
							"  uid := :NEW.userid; " +
							"  fid := :NEW.friendid; " +
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,fid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k2,fid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,uid),0); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k3,fid),0); " +
							"  ids := CONCAT( uid, '-' ); " +
							"  ids := CONCAT( ids, fid ); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"  ids := CONCAT( fid, '-' ); " +
							"  ids := CONCAT( ids, uid ); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"  dbms_output.put_line('delete ret_val=' || ret_val); " +					
							"END; "
					);


			st.execute(
					"CREATE OR REPLACE TRIGGER RecommendedVideoInsertTrigger " +
							"BEFORE INSERT ON recommended_videos " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  k1  CLOB := TO_CLOB('LoadRecentRecomVideosJSP'); " +
							"  ret_val BINARY_INTEGER; " +
							"BEGIN " +
							"  uid := :NEW.recommended_to; " +
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  dbms_output.put_line('delete ret_val=' || ret_val); " +					
							"END; "
					);

			st.execute(
					"CREATE OR REPLACE TRIGGER RecommendedVideoChangeTrigger " +
							"BEFORE UPDATE OR DELETE ON recommended_videos " +
							"FOR EACH ROW " +
							"DECLARE " +
							"  uid number := 5; " +
							"  k1  CLOB := TO_CLOB('LoadRecentRecomVideosJSP'); " +
							"  ret_val BINARY_INTEGER; " +
							"BEGIN " +
							"  uid := :OLD.recommended_to; " +
							"  dbms_output.put_line('uid = ' || uid); " +
							"  ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"  dbms_output.put_line('delete ret_val=' || ret_val); " +					
							"END; "
					);

			st.execute(
					"CREATE OR REPLACE TRIGGER userCameraCHGTrigger " + 
							"BEFORE DELETE OR UPDATE ON user_cameras " +
							"FOR EACH ROW " +
							"DECLARE " +
							"uid number := 5; " +
							"k1  CLOB := TO_CLOB('CameraTabJSP'); " +
							"ret_val BINARY_INTEGER;  " +
							"BEGIN " +
							"	uid := :OLD.user_id; " +
							//"	dbms_output.put_line('userCameraCHGTrigger replace this printout with the cosar delete call for key' || CONCAT(k1,uid));
							"	ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"END;"		
					);

			st.execute(
					"CREATE OR REPLACE TRIGGER msgDelUpdTrigger " +
							"BEFORE DELETE OR UPDATE ON messageinbox " +
							"FOR EACH ROW " +
							"DECLARE " +
							"uid number := 5; " +
							"k1  CLOB := TO_CLOB('ProfileJSP');  " +
							"k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"ids CLOB; " +
							"ret_val BINARY_INTEGER;  " +
							"ffid friends.friendid%TYPE;  " +
							"fuid friends.userid%TYPE;  " +
							"CURSOR FriendCursor IS  " +
							"      select friendid, userid from friends  " +
							"      where status='2' AND ( userid=uid OR friendid=uid ); " +  
							"BEGIN " +
							"uid := :OLD.receiver; " +
							//"dbms_output.put_line('msgTrigger replace this printout with the cosar delete call for key' || CONCAT(k1,uid));
							"ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"LOOP " + 
							"     FETCH FriendCursor INTO ffid, fuid; " + 
							"     EXIT WHEN FriendCursor%NOTFOUND; " +
							"     IF ffid != uid THEN " + 
							"               ids := CONCAT(uid,'-'); " + 
							"               ids := CONCAT(ids,ffid); " + 
							//"               dbms_output.put_line('msgTrigger 1 replace with cosar delete call for k4 = ' || CONCAT(k4,ids) ); 
							"               ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0);  " +
							"     END IF; " + 
							"     IF fuid != uid THEN " +
							"               ids := CONCAT(fuid,'-'); " +
							"               ids := CONCAT(ids,uid); " + 
							//"               dbms_output.put_line('msgTrigger 2.1 replace with cosar delete call for k4 = ' || CONCAT(k4,ids) ); 
							"       		ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +
							"  END LOOP; " + 
							"  CLOSE FriendCursor; " + 
							"END; "
					);


			st.execute(
					"CREATE OR REPLACE TRIGGER msgInsertTrigger " +
							"BEFORE INSERT ON messageinbox " +
							"FOR EACH ROW " +
							"DECLARE " +
							"uid number := 5; " +
							"k1  CLOB := TO_CLOB('ProfileJSP');  " +
							"k4  CLOB := TO_CLOB('FriendProfileJSP'); " +
							"ids CLOB; " +
							"ret_val BINARY_INTEGER;  " +
							"ffid friends.friendid%TYPE;  " +
							"fuid friends.userid%TYPE;  " +
							"CURSOR FriendCursor IS  " +
							"      select friendid, userid from friends  " +
							"      where status='2' AND ( userid=uid OR friendid=uid ); " +  
							"BEGIN " +
							"uid := :NEW.receiver; " +
							//"dbms_output.put_line('msgTrigger replace this printout with the cosar delete call for key' || CONCAT(k1,uid));
							"ret_val := COSARDeleteCall('RAYS',CONCAT(k1,uid),0); " +
							"LOOP " + 
							"     FETCH FriendCursor INTO ffid, fuid; " + 
							"     EXIT WHEN FriendCursor%NOTFOUND; " +
							"     IF ffid != uid THEN " + 
							"               ids := CONCAT(uid,'-'); " + 
							"               ids := CONCAT(ids,ffid); " + 
							//"               dbms_output.put_line('msgTrigger 1 replace with cosar delete call for k4 = ' || CONCAT(k4,ids) ); 
							"               ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0);  " +
							"     END IF; " + 
							"     IF fuid != uid THEN " +
							"               ids := CONCAT(fuid,'-'); " +
							"               ids := CONCAT(ids,uid); " + 
							//"               dbms_output.put_line('msgTrigger 2.1 replace with cosar delete call for k4 = ' || CONCAT(k4,ids) ); 
							"       		ret_val := COSARDeleteCall('RAYS',CONCAT(k4,ids),0); " +
							"     END IF; " +
							"  END LOOP; " + 
							"  CLOSE FriendCursor; " + 
							"END; "
					);


		}
		catch( Exception e )
		{
			e.printStackTrace();
		}	
		finally
		{
			try
			{
				if( st != null )
				{
					st.close();
				}
				if( conn != null )
				{
					conn.close();				
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}			
		}
	}


	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// read the input
		if (args.length == 0) {
			System.out.println("Input invalid.");
			return;
		}
		
		int totalNumCores = Integer.parseInt(args[0]);
		if (args.length < 1 + totalNumCores + 1) {
			System.out.println("Input invalid. Wrong num cores.");
			return;
		}
		
		cores_arr = new String[totalNumCores];
		for (int i = 0; i < totalNumCores; i++) {
			cores_arr[i] = args[i+1];
		}
		
		num_cores = Integer.parseInt(args[1+totalNumCores]);

		Connection conn = null;
		Statement st = null;

		Class.forName ("oracle.jdbc.driver.OracleDriver");
		String url = "jdbc:oracle:thin:@10.0.0.107:1521:orcl";
		String username = "cosar";
		String password = "gocosar";
		conn = DriverManager.getConnection(url,username,password);
		st = conn.createStatement();

		cleanDBMS(st);		
		
		try {
			st.execute("CREATE OR REPLACE LIBRARY COSAR_DLL_LIB AS \'C:\\hieun\\COSARTriggerAPI.dll\';");			
			
			genTrigsAndProcs("/home/hieun/Desktop/triggers_10000.sql", st);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		st.close();
		conn.close();
	}

	public static void cleanDBMS(Statement st) throws SQLException {
		String sql = "BEGIN "+
				"FOR I IN (SELECT DISTINCT OBJECT_NAME FROM ALL_OBJECTS WHERE OBJECT_TYPE IN ('PROCEDURE') and OWNER='HIEUNGUYEN') LOOP "+
				"EXECUTE IMMEDIATE 'DROP PROCEDURE '||I.OBJECT_NAME; "+
				"END LOOP; "+
				"END;";
		st.execute(sql);	

		sql = "BEGIN "+
				"FOR I IN (SELECT DISTINCT OBJECT_NAME FROM ALL_OBJECTS WHERE OBJECT_TYPE IN ('TRIGGER') and OWNER='HIEUNGUYEN') LOOP "+
				"EXECUTE IMMEDIATE 'DROP TRIGGER '||I.OBJECT_NAME; "+
				"END LOOP; "+
				"END;";		
		st.execute(sql);
	}

	public static boolean genTrigsAndProcs(String aSQLScriptFilePath, Statement stmt) throws IOException,SQLException {
		boolean isScriptExecuted = false;
		
		// compute the cores string
		String cores = "";
		for (int i = 0; i < num_cores; i++ ) {
			cores += cores_arr[i]+",";
		}
		cores = cores.substring(0, cores.length()-1);
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(aSQLScriptFilePath));
			String str;
//			StringBuffer sb = new StringBuffer();
			while ((str = in.readLine()) != null) {
				if (!str.replaceAll(" ", "").equals("")) {
					str = str.replaceAll("10.0.0.210:8888", cores);
					System.out.println(str);
					stmt.execute(str);
				}
			}
			in.close();
			isScriptExecuted = true;
		} catch (Exception e) {
			System.err.println("Failed to Execute" + aSQLScriptFilePath +". The error is"+ e.getMessage());
		}
		return isScriptExecuted;
	} 
}

