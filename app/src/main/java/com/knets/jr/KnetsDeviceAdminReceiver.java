package com.knets.jr;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class KnetsDeviceAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "KnetsJrAdmin";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device admin enabled");
        Toast.makeText(context, "Knets Jr device admin enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Device admin disabled");
        Toast.makeText(context, "Knets Jr device admin disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        Log.d(TAG, "Device admin disable requested - BLOCKING unauthorized access");
        
        // CRITICAL SYSTEM-LEVEL FIX: Launch secret code verification activity
        try {
            Intent secretCodeIntent = new Intent(context, SecretCodeVerificationActivity.class);
            secretCodeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(secretCodeIntent);
            
            // Block the disable request - user must verify secret code first
            return "Admin disable blocked. Enter secret code to continue.";
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch secret code verification", e);
            // Fallback protection - still block disable
            return "Parental controls protected. Contact parent to disable.";
        }
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        Log.d(TAG, "Password changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        Log.d(TAG, "Password failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        Log.d(TAG, "Password succeeded");
    }
}