package com.knets.jr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * Boot receiver to auto-start Knets Jr polling service after device restart
 * Critical for continuous background operation without user intervention
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "KnetsJrBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device boot completed - checking Knets Jr setup status");
            
            // Check if Knets Jr setup was completed
            SharedPreferences prefs = context.getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
            boolean workflowCompleted = prefs.getBoolean("workflow_completed", false);
            
            if (workflowCompleted) {
                Log.d(TAG, "Knets Jr setup completed - auto-starting background services");
                
                // Start main activity in background (no UI)
                Intent mainIntent = new Intent(context, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mainIntent.putExtra("auto_start", true);
                
                try {
                    context.startActivity(mainIntent);
                    Log.d(TAG, "MainActivity started for background initialization");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting MainActivity on boot", e);
                }
                
                // Start polling service directly
                Intent pollingIntent = new Intent(context, ServerPollingService.class);
                pollingIntent.putExtra("auto_start", true);
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(pollingIntent);
                    } else {
                        context.startService(pollingIntent);
                    }
                    Log.d(TAG, "ServerPollingService auto-started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting ServerPollingService on boot", e);
                }
            } else {
                Log.d(TAG, "Knets Jr setup not completed - skipping auto-start");
            }
        }
    }
}