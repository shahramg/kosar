package com.mitrallc.webserver;

import com.mitrallc.camp.CAMParray;
import com.mitrallc.kosar.Kosar;
import com.mitrallc.sql.KosarSoloDriver;

public class ClientSettingsPage extends BaseSettings{
	boolean verbose=true;
	static long Memorylimit=KosarSoloDriver.CacheSize;
	public void setCacheSize(String NewVal){
		try{
			long MultiplyFactor = 1;
			
			switch(BoxVal){
			case "Bytes": break;
			case "KBytes": MultiplyFactor = 1024;
			break;
			case "MBytes": MultiplyFactor = 1024 * 1024;
			break;
			case "GBytes": MultiplyFactor = 1024 * 1024 * 1024;
			break;
			default:  System.out.println("Error in setCacheSize of ClientSettingsPage.java:  specified BoxVal "+BoxVal+" is not recognized."); break;
			}
			KosarSoloDriver.setCacheSize((new Long(NewVal)).longValue() * MultiplyFactor);
			BaseSettings.Memorylimit = KosarSoloDriver.getCacheSize();
			
//			CAMParray.CheckAndAdjust(0,0);
		
			if (verbose) System.out.println("Input CacheSize "+KosarSoloDriver.CacheSize);
		} catch (Exception e){
			System.out.println("Failed to convert the String input "+NewVal+" to a long");
			e.printStackTrace();
		}
	}
	
	public boolean setKosarOnOff(String NewVal){
		boolean kosarOnOff = super.setKosarOnOff(NewVal);
		doSetKosar(kosarOnOff);
		return kosarOnOff;
	}

	private void doSetKosar(boolean kosarOnOrOff) {
		//True is on, false is off
		KosarSoloDriver.kosarEnabled = kosarOnOrOff;
	}

	public void doResetKosar() {
		boolean rememberOldKosarEnabledValue = KosarSoloDriver.kosarEnabled;
		KosarSoloDriver.kosarEnabled = false;
		Kosar.clearCache();
		KosarSoloDriver.kosarEnabled = rememberOldKosarEnabledValue; //true;
	}
	
	public String getOnOff(){
		if(KosarSoloDriver.kosarEnabled==true)
			setKosarOnOff("OFF");
		else
			setKosarOnOff("ON");
		return "<div id=\"actiondiv\">"+	
				"<fieldset>"+
	"<legend><h2>Actions</h2></legend>"+
	"<table class=\"actionform\">"+
	"<form  name=\"actionform1\" action=\"\" method=\"post\" onSubmit=\"return warning1()\">"+
	"<tr>"+
	"<td>KOSAR</td>"+
	"<td><input type=\"submit\" style=\"background-color:"+Color+"; padding-left:19px; padding-right:19px;\" id=\"kosarOnOff\"  name=\"kosarOnOff\" value=\""+KosarOnOff+"\" >(Click to turn "+onOfftext+")</td>"+
	"</tr>"+
	"</form>"+
	"<form name=\"actionform2\" action=\"\" method=\"post\" onSubmit=\"return warning2()\">"+
                            "<tr>"+
                            "<td>KVS</td>"+
                            "<td><input id=\"kvsFlush\" type=\"submit\" name=\"kvsFlush\" value=\"FLUSH\" >(Click to flush KVS)</td>"+   
                            "</tr>"+
                            "</form>"+
                            "<form name=\"cachesubmit\" action=\"\" method=\"post\">"+
							"<tr>"+
								"<td>Cache Size: <input type=\"text\" size=\"4\" name=\"cache-size\" value=\""+BaseMainpage.FormatMemoryNoRes(KosarSoloDriver.getCacheSize())+"\"><lable id=\"cache-size\">"+" "+getBoxVal()+"</lable></td>"+
								"<td><input type=\"submit\" value=\"Submit\" ></td>"+	
								"</td>"+
							"</tr>"+
							"</form>"+
							"</table>"+
						
                            "</fieldset>"+
			"</div><!--actiondiv-->";
	}
}
