package com.mitrallc.webserver;

import com.mitrallc.sql.KosarSoloDriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.json.simple.JSONObject;
public class ClientMainpage extends BaseMainpage {
	private static final int MINUTE =60000;
	private static final int HOUR = 60 * MINUTE;
	private static final int DAY = 24 * HOUR;
	public static long days=0;
	public static long hours=0;
	public static long minutes=0;
	
	public String content(){
		

		Stylesheet s = new Stylesheet();
	return "<!doctype html>" +
"<html>"+
"<head>"+
s.content()+
	"<meta charset=\"UTF-8\">"+
	"<title>KOSAR</title>"+
"</head>"+
"<body onload=\"ajaxFunction()\">"+
	"<div id=\"container\">"+
		"<div id=\"banner\">"+
			"<h1>kosar "+name+"</h1>"+
		"</div><!--banner-->"+
		"<div id=\"content\">"+
		"<div id=\"menu\">"+
			"<ul>"+
				getMenuString() +
				"</ul>"+
		"</div><!--menu-->"+
		"<div id=\"main\">"+
		"<div id=\"maincontents\">"+
			"<h2>Performance Metrics</h2>"+
			"<div id=\"JsonMain\">"+
			"</div>"+
			"</div><!--maincontents-->"+
		"</div><!--main-->"+
		"<br class=\"space\"/>"+
			"</div><!--content-->"+
			"<div class=\"footer\"><p>&copy 2014 by Mitra LLC</p></div>"+
	"</div><!--container-->"+
	"<script>"+
	"var myVar=NULL;"+

"function ajaxFunction(){"+
"clearTimeout(myVar);"+
"var xmlhttp;"+
"if (window.XMLHttpRequest)"+
"{ "+
"xmlhttp=new XMLHttpRequest();"+
"}"+
"else"+
"{"+
"xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");"+
"}"+

"xmlhttp.onreadystatechange=function()"+
"{"+
"if (xmlhttp.readyState==4)"+
"{"+
"var jsonObj = JSON.parse(xmlhttp.responseText);"+

	"document.getElementById(\"JsonMain\").innerHTML=jsonObj.main;"+
	"var refreshTime=(jsonObj.Refresh)*1000;"+
	"myVar= setTimeout (function(){ajaxFunction();}, refreshTime);"+ 
	"}"+
	"}\n"+
	"xmlhttp.open(\"GET\",\"http://"+BaseHttpServer.hostIP+":"+BaseHttpServer.Port+"/Json\",true);"+
	"xmlhttp.send();"+
	"}"+
	"</script>"+
	"</body>"+
	"</html>";

	}
	public static String FormatDouble(double inval){
        if ( Double.isNaN(inval) ) return "Not defined";
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(inval);
    }
//	public String getSave(){
//
//		try {
//			BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\shivajahangiri\\Downloads\\new9.html"));
//
//			StringBuilder sb = new StringBuilder();
//			String line = br.readLine();
//
//			while (line != null) {
//				sb.append(line);
//				sb.append(System.lineSeparator());
//				line = br.readLine();
//			}
//			String everything = sb.toString();
//
//			return everything;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return "";
//		}
//	}
    public static double ComputeRatioMax1(double top, double bottom){
        if (top/bottom > 1) return 1*100;
        else return (double)100*top/bottom;
    }

	public String getJDBCStatString() {
		return "<dd>- Number of cached queries: " + FormatInts( KosarSoloDriver.KosarKeysCachedEventMonitor.numberOfTotalEvents() )+"</dd>"+
				"<dd>- Average read latency: " + (double)(KosarSoloDriver.KosarReadTimeMonitor.getTotalDuration()) / KosarSoloDriver.KosarReadTimeMonitor.numberOfTotalEvents() +"</dd>"+
                "<dd>- Hit rate: " + this.FormatDouble(  this.ComputeRatioMax1(
                        (double)KosarSoloDriver.KosarCacheHitsEventMonitor.numberOfTotalEvents()
                        ,(double)KosarSoloDriver.KosarReadTimeMonitor.numberOfTotalEvents() ) )+"%</dd>"+
            
            "<dd>- Sliding window hit rate: " + this.FormatDouble( this.ComputeRatioMax1((double)KosarSoloDriver.KosarCacheHitsEventMonitor.numberOfEventsPerGranularity()
            ,(double)KosarSoloDriver.KosarReadTimeMonitor.numberOfEventsPerGranularity() ) ) +"% per "+ClientSettingsPage.getGranularity() +" second"
                        +"</dd>"+
            
            "<dd>- Number of cached keys/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts( KosarSoloDriver.KosarKeysCachedEventMonitor.numberOfEventsPerGranularity() )+"</dd>"+
            "<dd>- Number of registered triggers/"+ClientSettingsPage.getGranularity()+" second: " +KosarSoloDriver.KosarTriggerRegEventMonitor.numberOfEventsPerGranularity()+
                    " (Total registered triggers: " + KosarSoloDriver.KosarTriggerRegEventMonitor.numberOfTotalEvents() +")</dd>"+
            "<dd>- Number of invalidated keys recieved/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarInvalKeysAttemptedEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
            "<dd>- Number of evicted keys/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarEvictedKeysEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		
		"<dd>- Number of I lease back-off/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarReadBackoffEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		"<dd>- Number of I lease granted/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarILeaseGrantedEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		"<dd>- Number of Q lease granted/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarQLeaseReleasedEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		"<dd>- Number of Q lease aborted/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarQLeaseAbortEvntMonitor.numberOfEventsPerGranularity())+"</dd>"+
		"<dd>- Number of I lease released/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarILeaseReleasedEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		
		"<dd>- Number of steal/copy/useonce requests/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarNumAskAnotherClientEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
		"<dd>- Number of cache hit from other clients/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarNumGotKeyFromAnotherClientEventMonitor.numberOfEventsPerGranularity())+"</dd>";
	}
	public String getQueryResponseStatString() {
		return  "<dt><strong>DBMS</strong></dt>"+
                "<dd>- Average SQL query response time/"+ClientSettingsPage.getGranularity()+" second: " +this.FormatDouble(KosarSoloDriver.KosarQueryResponseTimeEventMonitor.averageEventDurationPerGranularity())+" milliseconds</dd>"+
                "<dd>- Average DML response time/"+ClientSettingsPage.getGranularity()+" second: " +this.FormatDouble(KosarSoloDriver.KosarDMLUpdateEventMonitor.averageEventDurationPerGranularity())+" milliseconds</dd>"+
                "<dd>- Number of SQL queries/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarQueryResponseTimeEventMonitor.numberOfEventsPerGranularity())+"</dd>"+
                "<dd>- Number of DML queries/"+ClientSettingsPage.getGranularity()+" second: " +FormatInts(KosarSoloDriver.KosarDMLUpdateEventMonitor.numberOfEventsPerGranularity())+"</dd>";
	}
	public String getMenuString() {
		return  "<li><a href=\"mainpage\">Performance Metrics</a></li>"+
                "<li><a href=\"Cached\">Cached Queries</a></li>"+
                "<li><a href=\"Triggers\">Triggers</a></li>"+
                "<li><a href=\"SQL\">SQL Stats</a></li>"+
                "<li><a href=\"Setting\">Settings</a></li>"+
                "<li><a href=\"/LastQueries\">Last 100 Queries</a></li>"+
				"<li><a href=\"/LastIDU\">Last 100 Insert/Delete/Update</a></li>";
	}
	public String settime(){
	
		Calendar currentTime=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long diff = currentTime.getTime().getTime() - BaseHttpServer.startTime.getTime().getTime();

		if (diff > DAY) {
			days=(diff/ DAY);
			  diff %= DAY;
			}
			if (diff> HOUR) {
			  hours=(diff/ HOUR);
			  diff %= HOUR;
			}
			if (diff > MINUTE) {
				minutes=(diff/ MINUTE);
				
			}
			
		return (" "+days+" Days, "+hours+" Hours, "+minutes+" Minutes" );
		
	}
public String json(){
		
		JSONObject obj= new JSONObject();
		obj.put("Refresh", ClientSettingsPage.getRefreshVal());
		obj.put("main", "<p>Refresh every "+ClientSettingsPage.getRefreshVal()+" seconds</p>"+
				"<p>Executing for the past"+settime()+" </p>"+
				"<p> Number of queries processed last " + ClientSettingsPage.getGranularity()+" seconds: " 
					+ FormatInts(KosarSoloDriver.KosarReadTimeMonitor.numberOfEventsPerGranularity()) +
					" (Total = "+FormatInts(KosarSoloDriver.KosarReadTimeMonitor.numberOfTotalEvents())+")</p>"+
					"Average execution time last "+ClientSettingsPage.getGranularity()+" seconds: "+this.FormatDouble(KosarSoloDriver.KosarRTEventMonitor.averageEventDurationPerGranularity())+" milliseconds</dd>"+
				"<p> Number of insert/delete/update commands processed last " + ClientSettingsPage.getGranularity()+" seconds: " 
					+ FormatInts(KosarSoloDriver.KosarDMLUpdateEventMonitor.numberOfEventsPerGranularity()) +
					" (Total = "+FormatInts(KosarSoloDriver.KosarDMLUpdateEventMonitor.numberOfTotalEvents())+")</p>"+
				"<dl id=\"DBMS\">"+
					"<dt><strong>KOSAR</strong><dt>"+
						getJVMStatString()+
						"<dd>- Used Cache Space: "+ BaseMainpage.FormatMemory(KosarSoloDriver.CurrentCacheUsedSpace(false))+"</dd>"+
						getJDBCStatString()+
				"</dl>"+
				"<dl id=\"DBMS\">"+
						getQueryResponseStatString()+
				"</dl>"
		//+getSave());
				);
		StringWriter out=new StringWriter();
		try {
			obj.writeJSONString(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String jsonText=out.toString();
		return jsonText;
	}
}

