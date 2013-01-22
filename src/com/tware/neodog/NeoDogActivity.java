package com.tware.neodog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class NeoDogActivity extends Activity {
	private static final int  PROCESS_NOT_FOUND = 0;
	private static final int  PROCESS_EXISTS 	= 1;
	private static final int  TIMER_RUNNING 	= 2;
	private static final int  RUNIN_PASS	 	= 3;
	private static final String TAG = "NeoDog";
	private static final String LOGPATH	= "/sdcard/NeoDog.txt";
	private static final String cfg_path= "/sdcard/neodog.cfg";
	private static String 	PROCNAME= "com.qualcomm.qx.neocore";
	private static String 	PROCPACK= "com.qualcomm.qx.neocore.Neocore";
	private static int  	RUNINTIME= 720;
	
	private List<RunningAppProcessInfo> mListAppInfo = null;
	private SimpleDateFormat now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ActivityManager am = null;
	private Timer timer = new Timer();
	private TimerTask task;
	private TextView vStat;
	private TextView vFailTime;
	private Handler handler;
	private int duration = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        vStat 		= (TextView)findViewById(R.id.view_status);
        vFailTime 	= (TextView)findViewById(R.id.view_time);
        
        am = (ActivityManager )getSystemService(Context.ACTIVITY_SERVICE);
        
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED))
        {
        	Log.e(TAG, "Media not ready yet.");
        	Toast.makeText(getApplicationContext(), "Media not ready yet!", Toast.LENGTH_LONG).show();
        	System.exit(1);
        }
        
        /* here to loading config from /sdcard/ */
        loadConfig cfg = new loadConfig();
        if (cfg.init(cfg_path))
        {
        	if(cfg.getRuninTime() != 0)
        	{
        		RUNINTIME 	= cfg.getRuninTime();
        	}
        	if (cfg.getProcessName() != null && cfg.getProcessPack() != null)
        	{
        		PROCNAME	= cfg.getProcessName();
        		PROCPACK	= cfg.getProcessPack();
        	}
        }else{
        	Log.e(TAG, "Loading config file failed. Using default settings.");
        }
        
        Toast.makeText(getApplicationContext(), 
        		"Runin: " + RUNINTIME*10 + "s" + "\nProcName: " + PROCNAME+"\nProcPack: " + PROCPACK,
        		Toast.LENGTH_LONG)
        		.show();
        
        task = new TimerTask(){
			@Override
			public void run() {
				duration ++;
				
				if (duration >= RUNINTIME )
				{
					handler.sendEmptyMessage(RUNIN_PASS);
				}
				
				if (queryProcessByName(PROCNAME))
				{
					handler.sendEmptyMessage(PROCESS_EXISTS);
				}else{
					handler.sendEmptyMessage(PROCESS_NOT_FOUND);
				}
			}
        };

        handler = new Handler(){
        	public void handleMessage(Message msg)
        	{
        		switch(msg.what){
        			case PROCESS_NOT_FOUND:
    					killTimer();
    					logToFile(LOGPATH, PROCNAME, "stoped "); 					
    					vStat.setVisibility(View.VISIBLE);
    					vFailTime.setVisibility(View.VISIBLE);
    					vStat.setTextSize(100);
    					vStat.setBackgroundColor(Color.RED);
    					vStat.setText(R.string.str_fail);
    					int hour = 0;
    					int min	 = 0;
    					int sec	 = 0;
    					int duration1 = duration * 10; /* get back to sec */
    					if (duration1 >= 3600)
    					{
    						hour = duration1/360;
    						if (duration1%360 >= 60)
    						{
    							min	 = (duration1%360)/60;
    							sec  = (duration1%360)%60;
    						}else{
    							sec  = duration1%360;
    						}
    					}else if (duration1 >= 60 && duration1 < 3600){
    						hour = 0;
    						min	 = duration1/60;
    						sec	 = duration1%60;
    					}else{
    						hour = 0;
    						min	 = 0;
    						sec	 = duration1;
    					}
    					vFailTime.setTextColor(Color.WHITE);    					
    					vFailTime.setText(getResources().getString(R.string.str_fail_time) + " " + hour +":"+ min + ":" +sec);
    					Toast.makeText(getApplicationContext(),
    							PROCNAME + "has stoped yet!", Toast.LENGTH_LONG).show();
        				break;
        		
        			case PROCESS_EXISTS:
    					logToFile(LOGPATH, PROCNAME, "running");
    					Toast.makeText(getApplicationContext(), 
    									getResources().getString(R.string.str_process)+ " " + 
    									(int)(((float)duration/RUNINTIME)*100) +"%",
    								Toast.LENGTH_LONG).show();
        				break;
        				
        			case TIMER_RUNNING:
        				/* Not define yet */
        				break;
        			
        			case RUNIN_PASS:
        				killTimer();
    					logToFile(LOGPATH, "Runin", "PASS ");
    					vStat.setVisibility(View.VISIBLE);
    					vStat.setTextSize(100);
    					vStat.setBackgroundColor(Color.GREEN);
    					vStat.setText(R.string.str_pass);
    					Intent i = new Intent();
        				i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        				i.setClass(getApplicationContext(), NeoDogActivity.class);
        				startActivity(i);
    					break;       				
        		}
        	}
        };

		try{
			timer.schedule(task, 7000, 10000);
			Intent i = new Intent();
			i.setClassName(PROCNAME, PROCPACK);
			Log.d(TAG, "neocore started");
			startActivity(i);
		}catch(ActivityNotFoundException e)
		{
			Log.d(TAG, PROCNAME + " not found");
			Toast.makeText(getApplicationContext(), PROCNAME + " not found", Toast.LENGTH_LONG).show();
			return;
		}	
    }
    
    
    public boolean queryProcessByName(String procName)
    {
    	procName = procName.trim();
    	
    	if (procName.length()<=0)
    	{
    		return false;
    	}
    	
   		if (!(mListAppInfo == null) && !mListAppInfo.isEmpty())
   		{
   			mListAppInfo.clear();
   		}

        mListAppInfo = am.getRunningAppProcesses();
        
        int size = mListAppInfo.size();
        for (int i=0; i< size; i++)
        {
        	if (mListAppInfo.get(i).processName.equalsIgnoreCase(this.getPackageName()))
        	{
        		if (mListAppInfo.get(i).importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
        		{
            		Log.e(TAG, "Fail reason: NeoDog has become Foreground without Pass!");
        			killTimer();
            		handler.sendEmptyMessage(PROCESS_NOT_FOUND);
        			return false;
        		}
        	}
        	
        	if (mListAppInfo.get(i).processName.equalsIgnoreCase(procName))
        	{
        		Log.e(TAG, "Found Process " + procName);
        		return true;
        	}
        	Log.i(TAG, mListAppInfo.get(i).processName);
        }
        Log.d(TAG, "Proccess not found!");
        return false;
    }
    
    public boolean logToFile(String file, String text, String status) 
    {
    	File f = new File(file);
    	if (! f.exists())
    	{
    		try {
				f.createNewFile();
			} catch (IOException e) {
				return false;
			}
    	}
    	
    	if (! f.isFile())
    	{
    		return false;
    	}

    	try {
			FileOutputStream fw = new FileOutputStream(f, true);
			fw.write(("NAME: " + text + " STATUS: " + status + " TIME: " + now.format(new Date()) + "\n").getBytes());
			fw.flush();
			fw.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException");
			return false;
		} catch (IOException e) {
			Log.e(TAG, "IOException");
			return false;
		}
    	Log.d(TAG, "Log to File Ok.");
		return true;
    }
    
    public void killTimer()
    {
		if (timer != null)
		{
			Log.i(TAG, "Timer had stoped!");
			timer.cancel();
			timer = null;
		}
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
    	{
    		new AlertDialog.Builder(NeoDogActivity.this)
    		.setTitle(R.string.str_warning).setMessage(R.string.str_exit)
    		.setIcon(R.drawable.dog1)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
    			public void onClick(DialogInterface dialog, int whichButton)
    			{
    				finish();
    			}
    		})
    		.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

    			public void onClick(DialogInterface dialog, int whichButton)
    			{
    				dialog.dismiss();
    				Log.d(TAG, "Cancal Exit");
    			}
    		}).show();
    		return true;
    	}
		return false;
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	Log.d(TAG, "onDestory()");
		killTimer();  
    	System.exit(0);
    }
}