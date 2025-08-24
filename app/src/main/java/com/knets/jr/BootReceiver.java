package com.knets.jr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * Boot Receiver for Auto-Launch functionality
 * Automatically starts Knets Jr app and services after device boot/restart
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "KnetsBootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "🚀 Boot event received: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            Intent.ACTION_REBOOT.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "🔄 Device boot completed - Initializing Knets Jr auto-launch");
            
            // Check if app setup is completed
            SharedPreferences preferences = context.getSharedPreferences("knets_jr_prefs", Context.MODE_PRIVATE);
            boolean workflowCompleted = preferences.getBoolean("workflow_completed", false);
            
            if (workflowCompleted) {
                Log.d(TAG, "✅ Knets Jr setup completed - Starting background services");
                
                // Start polling service automatically
                Intent pollingIntent = new Intent(context, ServerPollingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(pollingIntent);
                } else {
                    context.startService(pollingIntent);
                }
                
                // Start enhanced location service if needed
                Intent locationIntent = new Intent(context, EnhancedLocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent);
                } else {
                    context.startService(locationIntent);
                }
                
                Log.d(TAG, "🌐 Background services started automatically");
                
                // Auto-launch main app in background (optional)
                launchAppInBackground(context);
                
            } else {
                Log.d(TAG, "⚠️ Knets Jr setup not completed - Skipping auto-launch");
                
                // Still launch app to complete setup
                launchAppInBackground(context);
            }
        }
    }
    
    /**
     * Launch the main app in background mode
     */
    private void launchAppInBackground(Context context) {
        try {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.putExtra("auto_launched", true);
            
            context.startActivity(launchIntent);
            Log.d(TAG, "📱 Main app launched automatically");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to auto-launch main app", e);
        }
    }
}