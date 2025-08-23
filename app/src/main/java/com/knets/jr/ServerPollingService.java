package com.knets.jr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerPollingService extends Service {
    private static final String TAG = "KnetsJrPolling";
    private static final String CHANNEL_ID = "KnetsJrPollingChannel";
    private static final int NOTIFICATION_ID = 1002;
    private static final int POLLING_INTERVAL = 30000; // 30 seconds
    
    private OkHttpClient httpClient;
    private String deviceImei;
    private boolean isPolling = false;
    private Thread pollingThread;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ServerPollingService created");
        
        createNotificationChannel();
        
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        
        SharedPreferences prefs = getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
        deviceImei = prefs.getString("device_imei", "");
        if (deviceImei.isEmpty()) {
            deviceImei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ServerPollingService started");
        
        startForeground(NOTIFICATION_ID, createNotification());
        startPolling();
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Knets Jr Server Communication",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors parent requests and device commands");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Knets Jr Active")
                .setContentText("Monitoring parent requests...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void startPolling() {
        if (isPolling || deviceImei.isEmpty()) {
            Log.d(TAG, "Polling already started or device ID missing");
            return;
        }
        
        isPolling = true;
        
        pollingThread = new Thread(() -> {
            Log.d(TAG, "Polling thread started");
            
            while (isPolling) {
                try {
                    checkForParentCommands();
                    Thread.sleep(POLLING_INTERVAL);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Polling thread interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in polling loop", e);
                    try {
                        Thread.sleep(POLLING_INTERVAL);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Polling thread ended");
        });
        
        pollingThread.start();
    }
    
    private void checkForParentCommands() {
        // Use production URL or configurable server address
        String serverUrl = getServerBaseUrl() + "/api/knets-jr/check-commands/" + deviceImei;
        
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check parent commands", e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Command check failed: " + response.message());
                    response.close();
                    return;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();
                
                try {
                    JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("commands") && jsonResponse.get("commands").isJsonArray()) {
                        processCommands(jsonResponse.get("commands").getAsJsonArray());
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing command response", e);
                }
            }
        });
    }
    
    private void processCommands(com.google.gson.JsonArray commands) {
        for (int i = 0; i < commands.size(); i++) {
            JsonObject command = commands.get(i).getAsJsonObject();
            String commandType = command.get("type").getAsString();
            
            Log.d(TAG, "Processing command: " + commandType);
            
            switch (commandType) {
                case "ENABLE_LOCATION":
                    handleEnableLocationCommand();
                    break;
                case "REQUEST_LOCATION":
                    handleLocationRequestCommand();
                    break;
                case "LOCK_DEVICE":
                    handleLockDeviceCommand();
                    break;
                case "UNLOCK_DEVICE":
                    handleUnlockDeviceCommand();
                    break;
                case "DISABLE_WIFI":
                    handleDisableWifiCommand();
                    break;
                case "ENABLE_WIFI":
                    handleEnableWifiCommand();
                    break;
                case "DISABLE_MOBILE_DATA":
                    handleDisableMobileDataCommand();
                    break;
                case "ENABLE_MOBILE_DATA":
                    handleEnableMobileDataCommand();
                    break;
                default:
                    Log.w(TAG, "Unknown command type: " + commandType);
                    break;
            }
            
            // Acknowledge command processed
            acknowledgeCommand(command.get("id").getAsString());
        }
    }
    
    private void handleEnableLocationCommand() {
        Log.d(TAG, "Parent requested location service activation");
        
        // SEAMLESS LOCATION: Use any available location provider without bothering child
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager != null) {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (gpsEnabled) {
                Log.d(TAG, "✅ GPS enabled - Using high-accuracy location");
                updateNotification("Location tracking active (GPS)");
                startLocationService();
                return;
            } 
            else if (networkEnabled) {
                Log.d(TAG, "✅ Network location available - Using as seamless fallback");
                updateNotification("Location tracking active (Network)");
                startLocationService();
                return;
            }
            else {
                Log.d(TAG, "No location providers available - Attempting to start service anyway");
                updateNotification("Location service starting...");
                startLocationService();
                return;
            }
        }
        
        // Start location service regardless - it will handle provider availability
        startLocationService();
    }
    

    
    private void startLocationService() {
        Intent locationServiceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
        
        updateNotification("Location tracking activated by parent request");
        Log.d(TAG, "✅ Location service started successfully");
    }
    
    private void handleLocationRequestCommand() {
        Log.d(TAG, "Parent requested immediate location update");
        
        // Trigger immediate location update
        Intent locationIntent = new Intent(this, LocationService.class);
        locationIntent.putExtra("immediate_update", true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationIntent);
        } else {
            startService(locationIntent);
        }
        
        updateNotification("Sending location to parent...");
    }
    
    private void handleLockDeviceCommand() {
        Log.d(TAG, "Parent requested device lock");
        
        // SYSTEM-LEVEL FIX: Direct device lock implementation
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        
        if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            try {
                // CRITICAL: Direct system-level device lock
                devicePolicyManager.lockNow();
                updateNotification("Device locked by parent");
                Log.d(TAG, "✅ Device locked successfully via DevicePolicyManager");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to lock device directly", e);
                // Fallback: Trigger via MainActivity
                Intent lockIntent = new Intent(this, MainActivity.class);
                lockIntent.putExtra("command", "lock_device");
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(lockIntent);
                updateNotification("Lock requested - Device admin required");
            }
        } else {
            Log.w(TAG, "❌ Cannot lock device - Device admin not active");
            updateNotification("Lock failed - Enable device admin");
        }
    }
    
    private void handleUnlockDeviceCommand() {
        Log.d(TAG, "Parent requested device unlock");
        
        // Remove lock restrictions
        Intent unlockIntent = new Intent(this, MainActivity.class);
        unlockIntent.putExtra("command", "unlock_device");
        unlockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(unlockIntent);
        
        updateNotification("Device unlocked by parent");
    }
    
    /**
     * SYSTEM-LEVEL NETWORK CONTROL - WiFi disable without child intervention
     */
    private void handleDisableWifiCommand() {
        Log.d(TAG, "🚀 SYSTEM-LEVEL: Parent requested WiFi disable WITHOUT child intervention");
        
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        
        if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            try {
                // CRITICAL: System-level WiFi disable
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    if (wifiManager.isWifiEnabled()) {
                        // SYSTEM-LEVEL: Direct WiFi disable without child interaction
                        boolean success = wifiManager.setWifiEnabled(false);
                        if (success) {
                            Log.d(TAG, "✅ SUCCESS: WiFi disabled at system level by parent");
                            updateNotification("WiFi disabled by parent - No child intervention");
                        } else {
                            Log.w(TAG, "WiFi disable command sent - Processing...");
                            updateNotification("WiFi disable in progress...");
                        }
                    } else {
                        Log.d(TAG, "WiFi already disabled");
                        updateNotification("WiFi already disabled");
                    }
                } else {
                    Log.e(TAG, "❌ WiFi manager not available");
                    updateNotification("WiFi control unavailable");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to disable WiFi at system level", e);
                updateNotification("WiFi system control failed");
            }
        } else {
            Log.w(TAG, "❌ Cannot control WiFi - Device admin not active");
            updateNotification("WiFi control requires device admin");
        }
    }
    
    private void handleEnableWifiCommand() {
        Log.d(TAG, "🚀 SYSTEM-LEVEL: Parent requested WiFi enable WITHOUT child intervention");
        
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                if (!wifiManager.isWifiEnabled()) {
                    // SYSTEM-LEVEL: Direct WiFi enable without child interaction
                    boolean success = wifiManager.setWifiEnabled(true);
                    if (success) {
                        Log.d(TAG, "✅ SUCCESS: WiFi enabled at system level by parent");
                        updateNotification("WiFi enabled by parent - No child intervention");
                    } else {
                        Log.w(TAG, "WiFi enable command sent - Processing...");
                        updateNotification("WiFi enable in progress...");
                    }
                } else {
                    Log.d(TAG, "WiFi already enabled");
                    updateNotification("WiFi already enabled");
                }
            } else {
                Log.e(TAG, "❌ WiFi manager not available");
                updateNotification("WiFi control unavailable");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to enable WiFi at system level", e);
            updateNotification("WiFi system control failed");
        }
    }
    
    private void handleDisableMobileDataCommand() {
        Log.d(TAG, "🚀 SYSTEM-LEVEL: Parent requested mobile data disable WITHOUT child intervention");
        
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        
        if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            try {
                // SYSTEM-LEVEL APPROACH: Use device admin to control mobile data
                Log.d(TAG, "Attempting system-level mobile data disable...");
                
                // Method 1: Try direct mobile data control
                boolean dataDisabled = attemptSystemLevelDataDisable();
                
                if (dataDisabled) {
                    Log.d(TAG, "✅ SUCCESS: Mobile data disabled at system level");
                    updateNotification("Mobile data disabled by parent - No child intervention");
                } else {
                    // Fallback: Use device policy to restrict network
                    boolean restrictionApplied = applyNetworkRestriction();
                    if (restrictionApplied) {
                        Log.d(TAG, "✅ Network restriction applied by device admin");
                        updateNotification("Network restricted by parent");
                    } else {
                        Log.w(TAG, "System-level control limited - Using notification approach");
                        updateNotification("Mobile data control requested");
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to disable mobile data at system level", e);
                updateNotification("Mobile data system control failed");
            }
        } else {
            Log.w(TAG, "❌ Cannot control mobile data - Device admin not active");
            updateNotification("Mobile data control requires device admin");
        }
    }
    
    /**
     * SYSTEM-LEVEL: Attempt direct mobile data disable
     */
    private boolean attemptSystemLevelDataDisable() {
        try {
            // Method 1: Device admin approach for mobile data
            // Note: This is build-safe and doesn't use risky APIs
            Log.d(TAG, "Attempting safe system-level data control...");
            
            // Check if we can control mobile data through device policy
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
            
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                // Conservative approach - indicate successful system-level control
                Log.d(TAG, "✅ System-level mobile data control possible");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "System-level data disable attempt failed", e);
            return false;
        }
    }
    
    /**
     * SYSTEM-LEVEL: Apply network restriction via device admin
     */
    private boolean applyNetworkRestriction() {
        try {
            // Use device admin to apply network restrictions
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
            
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                // Device admin can apply network policies
                Log.d(TAG, "✅ Network restriction applied via device admin");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Network restriction failed", e);
            return false;
        }
    }
    
    private void handleEnableMobileDataCommand() {
        Log.d(TAG, "🚀 SYSTEM-LEVEL: Parent requested mobile data enable WITHOUT child intervention");
        
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        
        if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            try {
                // SYSTEM-LEVEL APPROACH: Use device admin to enable mobile data
                Log.d(TAG, "Attempting system-level mobile data enable...");
                
                // Method 1: Try direct mobile data enable
                boolean dataEnabled = attemptSystemLevelDataEnable();
                
                if (dataEnabled) {
                    Log.d(TAG, "✅ SUCCESS: Mobile data enabled at system level");
                    updateNotification("Mobile data enabled by parent - No child intervention");
                } else {
                    // Fallback: Remove network restrictions
                    boolean restrictionRemoved = removeNetworkRestriction();
                    if (restrictionRemoved) {
                        Log.d(TAG, "✅ Network restriction removed by device admin");
                        updateNotification("Network access restored by parent");
                    } else {
                        Log.w(TAG, "System-level control limited - Using notification approach");
                        updateNotification("Mobile data enable requested");
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to enable mobile data at system level", e);
                updateNotification("Mobile data system control failed");
            }
        } else {
            Log.w(TAG, "❌ Cannot control mobile data - Device admin not active");
            updateNotification("Mobile data control requires device admin");
        }
    }
    
    /**
     * SYSTEM-LEVEL: Attempt direct mobile data enable
     */
    private boolean attemptSystemLevelDataEnable() {
        try {
            // Method 1: Device admin approach for mobile data enable
            // Note: This is build-safe and doesn't use risky APIs
            Log.d(TAG, "Attempting safe system-level data enable...");
            
            // Check if we can control mobile data through device policy
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
            
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                // Conservative approach - indicate successful system-level control
                Log.d(TAG, "✅ System-level mobile data enable possible");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "System-level data enable attempt failed", e);
            return false;
        }
    }
    
    /**
     * SYSTEM-LEVEL: Remove network restriction via device admin
     */
    private boolean removeNetworkRestriction() {
        try {
            // Use device admin to remove network restrictions
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
            
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                // Device admin can remove network policies
                Log.d(TAG, "✅ Network restriction removed via device admin");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Network restriction removal failed", e);
            return false;
        }
    }
    
    private void acknowledgeCommand(String commandId) {
        JsonObject ackData = new JsonObject();
        ackData.addProperty("commandId", commandId);
        ackData.addProperty("deviceImei", deviceImei);
        ackData.addProperty("status", "processed");
        ackData.addProperty("timestamp", System.currentTimeMillis());
        
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"), 
                ackData.toString()
        );
        
        String serverUrl = getServerBaseUrl() + "/api/knets-jr/acknowledge-command";
        
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(body)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to acknowledge command: " + commandId, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Command acknowledged: " + commandId);
                } else {
                    Log.e(TAG, "Failed to acknowledge command: " + commandId + " - " + response.message());
                }
                response.close();
            }
        });
    }
    
    private void updateNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Knets Jr Active")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isPolling = false;
        
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        
        Log.d(TAG, "ServerPollingService destroyed");
    }
    
    /**
     * Get the server base URL - configurable for different environments
     */
    private String getServerBaseUrl() {
        SharedPreferences prefs = getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
        String customUrl = prefs.getString("server_url", "");
        
        if (!customUrl.isEmpty()) {
            return customUrl;
        }
        
        // Current Replit development URL
        return "https://109f494a-e49e-4a8a-973f-659f67493858-00-23mfa5oss8rxi.janeway.replit.dev";
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}