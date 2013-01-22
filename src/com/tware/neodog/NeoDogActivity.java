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
	private static final int  RUNINTIME			= 720;
	private static final String TAG = "NeoDog";
	private static final String LOGPATH	= "/sdcard/NeoDog.txt";
	private static final String PROCNAME= "com.qualcomm.qx.neocore";
	
	private List<RunningAppProcessInfo> mListAppInfo = null;
	private SimpleDateFormat now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ActivityManager am = null;
	private Timer timer = new Timer();
	private TimerTask task;
	private TextView vStat;
	private Handler handler;
	private int duration = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        vStat = (TextView)findViewById(R.id.view_status);
        
        am = (ActivityManager )getSystemService(Context.ACTIVITY_SERVICE);
        
        
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
    					logToFile(LOGPATH, PROCNAME, "stoped ");
    					
    					vStat.setVisibility(View.VISIBLE);
    					vStat.setTextSize(100);
    					vStat.setBackgroundColor(Color.RED);
    					vStat.setText(R.string.str_fail);
    					Toast.makeText(getApplicationContext(), PROCNAME + "has stoped yet!", Toast.LENGTH_LONG).show();
    					if (timer != null)
    					{
    						timer.cancel();
    						timer = null;
    					}
        				break;
        		
        			case PROCESS_EXISTS:
    					logToFile(LOGPATH, PROCNAME, "running");
    					Toast.makeText(getApplicationContext(), 
    							getResources().getString(R.string.str_process) + (int)(((float)duration/RUNINTIME)*100) +"%",
    								Toast.LENGTH_LONG).show();
        				break;
        				
        			case TIMER_RUNNING:
        				/* Not define yet */
        				break;
        			
        			case RUNIN_PASS:
    					if (timer != null)
    					{
    						timer.cancel();
    						timer = null;
    					}
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
			i.setClassName("com.qualcomm.qx.neocore", "com.qualcomm.qx.neocore.Neocore");
			Log.d(TAG, "neocore started");
			startActivity(i);
		}catch(ActivityNotFoundException e)
		{
			Log.d(TAG, "neocore not found");
			Toast.makeText(getApplicationContext(), "neocore not found", Toast.LENGTH_LONG).show();
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
    				System.exit(0);
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
    
}