package com.mitrallc.webserver;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;

import org.json.simple.JSONObject;

import com.mitrallc.sqltrig.QTmeta;
import com.mitrallc.sqltrig.QueryToTrigger;

public class ClientSQLPage extends BaseSQL {
	
	public void getSQLStats() {
		int counter = 0;
		
		int TotalReqPerGranularity = 0;
		int TotalReq = 0;
		sqlStats="";

		Enumeration<QTmeta> eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();){
			QTmeta qtelt = e.nextElement();
			TotalReqPerGranularity += qtelt.getKVSHitsMoving();
			TotalReq += qtelt.getKVSHits();

		}
		
		eqt = QueryToTrigger.TriggerCache.elements();
		for (Enumeration<QTmeta> e = eqt; e.hasMoreElements();counter++){
			QTmeta qtelt = e.nextElement();
			sqlStats += "<dl>"+ (counter+1) +") "+
					qtelt.getQueryTemplate() +
					"</dt>"+
					"<dd>- Number of instances in the KVS: "+qtelt.getNumQueryInstances()+"</dd>"+
					//"<dd>- Executed by the RDBMS 4</dd>"+
					"<dd>- "+ClientMainpage.FormatInts(qtelt.getKVSHitsMoving())+" processed by the KVS in "+ClientSettingsPage.getGranularity()+" seconds (Total="+ClientMainpage.FormatInts(qtelt.getKVSHits())+")</dd>"+
					"<dd>- "+ClientMainpage.FormatDouble(ClientMainpage.ComputeRatioMax1((double) qtelt.getKVSHitsMoving() , (double) TotalReqPerGranularity))+"% of queries processed by the KVS in "+ClientSettingsPage.getGranularity()+" seconds (Total="+ClientMainpage.FormatDouble(ClientMainpage.ComputeRatioMax1((double) qtelt.getKVSHits() , (double) TotalReq))+"%)</dd>"+
					"</dl>";
		}
	}
//	public String jsonStat(){
//		JSONObject obj= new JSONObject();
//		obj.put("stat", sqlStats);
//		obj.put("Refresh", ClientSettingsPage.getRefreshVal());
//		StringWriter out=new StringWriter();
//		try {
//			obj.writeJSONString(out);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String jsonText=out.toString();
//		return jsonText;
//	}

}
