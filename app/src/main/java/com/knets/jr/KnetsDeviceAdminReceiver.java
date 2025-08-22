package com.knets.jr;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        Log.d(TAG, "Device admin disable requested - blocking unauthorized disable");
        
        // Check if secret code is stored (indicates proper setup)
        SharedPreferences prefs = context.getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
        String storedSecretCode = prefs.getString("secret_code", "");
        String storedParentCode = prefs.getString("parent_code", "");
        
        if (!storedSecretCode.isEmpty() && !storedParentCode.isEmpty()) {
            // Device is properly registered - launch secret code verification
            Intent secretIntent = new Intent(context, MainActivity.class);
            secretIntent.putExtra("action", "verify_disable");
            secretIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(secretIntent);
            
            // Block the disable with security message
            return "🔒 SECURITY PROTECTION ACTIVE\n\n" +
                   "This device is protected by Knets parental controls.\n\n" +
                   "To disable device admin:\n" +
                   "1. Open Knets Jr app\n" +
                   "2. Enter your 4-digit secret code\n" +
                   "3. Follow the disable instructions\n\n" +
                   "⚠️ Unauthorized attempts are logged and reported to parents.";
        }
        
        // Fallback message if not properly set up
        return "Disabling Knets Jr will remove parental controls from this device.";
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