package com.tware.neodog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

public class loadConfig{
	private static int 		CFG_RUNTIME = 0;
	private static String 	CFG_PROCESSNAME = null;
	private static String 	CFG_PROCESSPACK = null;
	private static String 	TAG = "NeoDog_LoadConfig";
	
	public boolean init(String filePath)
	{
		
		if (filePath == null)
		{
			Log.e(TAG, "File Path is NULL");
			return false;
		}
		
		File f = new File(filePath);
		
		if (f.exists()&& f.isFile())
		{
			Log.i(TAG, f.getAbsoluteFile()+ " found");
			try {
				BufferedReader fr = new BufferedReader(new FileReader(f));
				String str = fr.readLine();
				do{
					if (str == null)
					{
						Log.e(TAG, "read null from " + f.getAbsoluteFile());
						return false;
					}
					if (!str.startsWith("#") && str.trim().length()>= 11 )
					{
						String [] strSplit = new String[80];
						strSplit = str.split("=");
						if (strSplit != null && strSplit[0].equalsIgnoreCase("RuninTime") && strSplit.length >= 2)
						{
							Log.i(TAG, "CFG_RUNTIME defined: " + strSplit[1]);
							/* Here divid 10 means main program has 10s timer. */
							CFG_RUNTIME = Integer.parseInt(strSplit[1].trim())/10;
						}
						else if(strSplit != null && strSplit[0].equalsIgnoreCase("PROCESSNAME") && strSplit.length >= 2)
						{
							Log.i(TAG, "CFG_PROCESSNAME defined: " + strSplit[1]);
							CFG_PROCESSNAME = strSplit[1].trim();
						}
						else if(strSplit != null && strSplit[0].equalsIgnoreCase("PROCESSPACK") && strSplit.length >= 2)
						{
							Log.i(TAG, "CFG_PROCESSPACK defined: " + strSplit[1]);
							CFG_PROCESSPACK = strSplit[1].trim();
						}
						
					}
				}while((str = fr.readLine())!=null);
	
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not exist");
				return false;
			} catch (IOException e) {
				Log.e(TAG, "File IO error");
				return false;
			}
			return true;
		}
		
		Log.e(TAG, "File not exist");
		return false;
	}
	
	public int getRuninTime()
	{
		return CFG_RUNTIME;
	}
	
	public String getProcessName()
	{
		return CFG_PROCESSNAME;
	}
	
	public String getProcessPack()
	{
		return CFG_PROCESSPACK;
	}
	
	
}