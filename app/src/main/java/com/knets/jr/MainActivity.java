package com.knets.jr;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// Android 13+ specific imports
import android.window.OnBackInvokedDispatcher;
import android.window.OnBackInvokedCallback;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.core.os.BuildCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "KnetsJr";
    private static final int DEVICE_ADMIN_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final int PHONE_STATE_PERMISSION_REQUEST = 1003;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1004;
    
    private EditText etParentCode, etSecretCode;
    private TextView tvSecretCodeLabel;
    private Button btnConnect, btnEnableDeviceAdmin, btnEnableLocation;
    private TextView tvStatus, tvStep, tvDeviceInfo;
    private ProgressBar progressBar;
    
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;
    private String deviceImei = "";
    private String storedParentCode = "";
    private String storedSecretCode = "";
    private int currentStep = 1;
    
    // 3-Step Workflow States
    private boolean parentCodeVerified = false;
    private boolean secretCodeVerified = false;
    private boolean deviceAdminEnabled = false;
    private boolean workflowCompleted = false;
    
    private OkHttpClient httpClient;
    private SharedPreferences preferences;
    
    // Android 13+ specific callbacks
    private OnBackInvokedCallback backInvokedCallback;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Android 13+ security initialization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupAndroid13Compatibility();
        }
        
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeServices();
        loadStoredData();
        updateUI();
        
        Log.d(TAG, "MainActivity created - Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
    }
    
    private void initializeViews() {
        etParentCode = findViewById(R.id.etParentCode);
        etSecretCode = findViewById(R.id.etSecretCode);
        tvSecretCodeLabel = findViewById(R.id.tvSecretCodeLabel);
        btnConnect = findViewById(R.id.btnConnect);
        btnEnableDeviceAdmin = findViewById(R.id.btnEnableDeviceAdmin);
        btnEnableLocation = findViewById(R.id.btnEnableLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tvStep = findViewById(R.id.tvStep);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up click listeners
        btnConnect.setOnClickListener(v -> handleConnectStep());
        btnEnableDeviceAdmin.setOnClickListener(v -> enableDeviceAdmin());
        btnEnableLocation.setOnClickListener(v -> requestLocationPermissions());
        
        // Auto-hide input field after code is stored (smart UI behavior)
        etParentCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Convert input to uppercase for consistency
                String input = s.toString().toUpperCase();
                if (!input.equals(s.toString())) {
                    etParentCode.setText(input);
                    etParentCode.setSelection(input.length());
                }
                
                if (input.length() == 10) {
                    // Auto-advance when 10-character code is complete
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Verify Code");
                } else {
                    btnConnect.setEnabled(false);
                    btnConnect.setText("Save Code");
                }
            }
        });
        
        // Secret code input validation (smart UI behavior)
        etSecretCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 4) {
                    // Auto-advance when 4-digit secret code is complete
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Save Secret Code");
                }
            }
        });
    }
    
    private void initializeServices() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        preferences = getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
        
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    private void loadStoredData() {
        storedParentCode = preferences.getString("parent_code", "");
        storedSecretCode = preferences.getString("secret_code", "");
        parentCodeVerified = preferences.getBoolean("parent_code_verified", false);
        secretCodeVerified = preferences.getBoolean("secret_code_verified", false);
        deviceAdminEnabled = devicePolicyManager.isAdminActive(deviceAdminReceiver);
        workflowCompleted = preferences.getBoolean("workflow_completed", false);
        
        // Determine current step based on state
        updateCurrentStep();
        
        // Get device IMEI
        getDeviceImei();
        
        Log.d(TAG, "Loaded state - Parent Code Verified: " + (parentCodeVerified ? "✓" : "✗") + 
               " Secret Code Verified: " + (secretCodeVerified ? "✓" : "✗") + 
               " Admin: " + (deviceAdminEnabled ? "✓" : "✗"));
    }
    
    private void updateCurrentStep() {
        if (!parentCodeVerified) currentStep = 1;
        else if (!secretCodeVerified) currentStep = 2;
        else if (!deviceAdminEnabled) currentStep = 3;
        else currentStep = 4; // Completed
    }
    
    private void updateUI() {
        // Smart UI behavior: Hide input fields after successful verification
        if (parentCodeVerified) {
            etParentCode.setVisibility(View.GONE);
            findViewById(R.id.tvCodeLabel).setVisibility(View.GONE);
        } else {
            if (!storedParentCode.isEmpty()) {
                etParentCode.setText(storedParentCode);
            }
        }
        
        // Show secret code input after parent code verification
        if (parentCodeVerified && !secretCodeVerified) {
            tvSecretCodeLabel.setVisibility(View.VISIBLE);
            etSecretCode.setVisibility(View.VISIBLE);
        } else if (secretCodeVerified) {
            tvSecretCodeLabel.setVisibility(View.GONE);
            etSecretCode.setVisibility(View.GONE);
        }
        
        String stepText = "Step " + currentStep + " of 3: ";
        String statusText = "";
        
        // Reset button visibility
        btnConnect.setVisibility(View.GONE);
        btnEnableDeviceAdmin.setVisibility(View.GONE);
        btnEnableLocation.setVisibility(View.GONE);
        
        switch (currentStep) {
            case 1:
                stepText += "Parent Code Verification";
                statusText = "Enter your 10-digit parent code to connect with Knets dashboard";
                btnConnect.setVisibility(View.VISIBLE);
                btnConnect.setText("Verify Parent Code");
                btnConnect.setEnabled(etParentCode.getText().length() == 10);
                break;
                
            case 2:
                stepText += "Secret Code Verification";
                statusText = "Enter your 4-digit security code for device administration";
                btnConnect.setVisibility(View.VISIBLE);
                btnConnect.setText("Verify Secret Code");
                btnConnect.setEnabled(etSecretCode.getText().length() == 4);
                break;
                
            case 3:
                stepText += "Enable Device Admin";
                statusText = "Device administrator permission is essential for:\n\n" +
                           "• Remote lock/unlock of device\n" +
                           "• Screen time monitoring and control\n" +
                           "• App usage restrictions\n" +
                           "• Network access management\n" +
                           "• Schedule enforcement\n\n" +
                           "This ensures complete parental control from your dashboard.";
                btnEnableDeviceAdmin.setVisibility(View.VISIBLE);
                btnEnableDeviceAdmin.setText("Enable Device Admin");
                break;
                
            case 5:
                stepText += "Enable Location Services";
                statusText = "Allow location access for GPS tracking";
                btnEnableLocation.setVisibility(View.VISIBLE);
                break;
                
            case 6:
                stepText += "Register Device";
                statusText = "Registering device with Knets...";
                registerDevice();
                break;
                
            case 7:
                stepText += "Complete Setup";
                statusText = "Finalizing Knets Jr setup...";
                completeSetup();
                break;
                
            case 4:
                showCompletedState();
                break;
        }
        
        tvStep.setText(stepText);
        tvStatus.setText(statusText);
        
        // Update device info
        updateDeviceInfo();
        
        // Update progress bar
        int progress = Math.min(currentStep * 100 / 3, 100);
        progressBar.setProgress(progress);
    }
    
    private void handleConnectStep() {
        if (currentStep == 1) {
            // Step 1: Verify parent code
            String code = etParentCode.getText().toString().trim();
            if (code.length() != 10) {
                showToast("Please enter a 10-digit parent code");
                return;
            }
            
            storedParentCode = code;
            preferences.edit()
                    .putString("parent_code", code)
                    .apply();
            
            verifyCodeWithServer();
            
        } else if (currentStep == 2) {
            // Step 2: Verify secret code
            String secretCode = etSecretCode.getText().toString().trim();
            if (secretCode.length() != 4) {
                showToast("Please enter a 4-digit secret code");
                return;
            }
            
            storedSecretCode = secretCode;
            preferences.edit()
                    .putString("secret_code", secretCode)
                    .apply();
                    
            verifySecretCodeWithServer(secretCode);
        }
    }
    
    private void verifyCodeWithServer() {
        if (storedParentCode.isEmpty()) {
            showToast("No parent code found");
            return;
        }
        
        showProgress("Verifying code...");
        
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("parentCode", storedParentCode);
        jsonBody.addProperty("deviceImei", deviceImei);
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                jsonBody.toString()
        );
        
        String serverUrl = getServerBaseUrl() + "/api/knets-jr/verify-code";
        
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(body)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast("Code verification failed: " + e.getMessage());
                    Log.e(TAG, "Code verification failed", e);
                });
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    hideProgress();
                    
                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                            boolean valid = jsonResponse.get("valid").getAsBoolean();
                            
                            if (valid) {
                                parentCodeVerified = true;
                                preferences.edit()
                                        .putBoolean("parent_code_verified", true)
                                        .apply();
                                
                                showToast("Parent code verification successful");
                                updateCurrentStep();
                                updateUI();
                            } else {
                                showToast("Invalid parent code. Please check and try again.");
                            }
                        } catch (Exception e) {
                            showToast("Error processing server response");
                            Log.e(TAG, "Error parsing verification response", e);
                        }
                    } else {
                        showToast("Code verification failed: " + response.message());
                    }
                });
            }
        });
    }
    
    private void verifySecretCodeWithServer(String secretCode) {
        showProgress("Verifying secret code...");
        
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("parentCode", storedParentCode);
        jsonBody.addProperty("secretCode", secretCode);
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                jsonBody.toString()
        );
        
        String serverUrl = getServerBaseUrl() + "/api/knets-jr/verify-codes";
        
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(body)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast("Secret code verification failed: " + e.getMessage());
                    Log.e(TAG, "Secret code verification failed", e);
                });
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    hideProgress();
                    
                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                            boolean success = jsonResponse.get("success").getAsBoolean();
                            
                            if (success) {
                                secretCodeVerified = true;
                                preferences.edit()
                                        .putBoolean("secret_code_verified", true)
                                        .apply();
                                
                                showToast("Secret code verification successful");
                                updateCurrentStep();
                                updateUI();
                            } else {
                                showToast("Invalid secret code. Please check and try again.");
                            }
                        } catch (Exception e) {
                            showToast("Error processing server response");
                            Log.e(TAG, "Error parsing secret verification response", e);
                        }
                    } else {
                        showToast("Secret code verification failed: " + response.message());
                    }
                });
            }
        });
    }
    
    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable device admin to allow Knets parental controls");
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST);
    }
    
    private void requestLocationPermissions() {
        // Android 13+ compatible permission request
        String[] permissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ needs notification permission too
            permissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            permissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        
        ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST);
    }
    
    private void registerDevice() {
        if (!parentCodeVerified || deviceImei.isEmpty()) {
            showToast("Cannot register: Code not verified or IMEI not available");
            return;
        }
        
        showProgress("Registering device...");
        
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("parentCode", storedParentCode);
        jsonBody.addProperty("deviceImei", deviceImei);
        jsonBody.addProperty("deviceInfo", getDeviceInfoJson());
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                jsonBody.toString()
        );
        
        Request request = new Request.Builder()
                .url(getServerBaseUrl() + "/api/knets-jr/register-device")
                .post(body)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showToast("Device registration failed: " + e.getMessage());
                    Log.e(TAG, "Device registration failed", e);
                });
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    hideProgress();
                    
                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                            boolean success = jsonResponse.get("success").getAsBoolean();
                            
                            if (success) {
                                showToast("Device registered successfully!");
                                completeSetup();
                            } else {
                                showToast("Device registration failed: " + jsonResponse.get("message").getAsString());
                            }
                        } catch (Exception e) {
                            showToast("Error processing registration response");
                            Log.e(TAG, "Error parsing registration response", e);
                        }
                    } else {
                        showToast("Device registration failed: " + response.message());
                    }
                });
            }
        });
    }
    
    private void completeSetup() {
        // Start server polling service for auto-enable location functionality
        Intent pollingIntent = new Intent(this, ServerPollingService.class);
        startForegroundService(pollingIntent);
        
        // Store device IMEI for services
        if (!deviceImei.isEmpty()) {
            preferences.edit()
                    .putString("device_imei", deviceImei)
                    .apply();
        }
        
        workflowCompleted = true;
        preferences.edit()
                .putBoolean("workflow_completed", true)
                .apply();
        
        currentStep = 4;
        showToast("Knets Jr setup completed!");
        updateUI();
        
        Log.d(TAG, "3-step workflow completed - Location auto-enable activated");
    }
    
    private void showCompletedState() {
        // Hide all buttons and show completion message
        btnConnect.setVisibility(View.GONE);
        btnEnableDeviceAdmin.setVisibility(View.GONE);
        btnEnableLocation.setVisibility(View.GONE);
        
        tvStatus.setText("✅ Knets Jr setup completed successfully!\n\n" +
                "Your device is now connected to your parent's dashboard with:\n\n" +
                "• Remote device control enabled\n" +
                "• Automatic location services (activated when requested)\n" +
                "• Screen time monitoring\n" +
                "• App usage tracking\n\n" +
                "The app will run in the background and automatically respond to parent requests.");
    }
    
    private void getDeviceImei() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ compatibility
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        deviceImei = telephonyManager.getImei();
                        if (deviceImei == null || deviceImei.isEmpty()) {
                            // Fallback to device ID
                            deviceImei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting IMEI", e);
                    deviceImei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                }
            } else {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_PHONE_STATE}, 
                        PHONE_STATE_PERMISSION_REQUEST);
            }
        } else {
            // Pre-Android 13 method
            try {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                        == PackageManager.PERMISSION_GRANTED && telephonyManager != null) {
                    deviceImei = telephonyManager.getDeviceId();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting device ID", e);
            }
            
            if (deviceImei == null || deviceImei.isEmpty()) {
                deviceImei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }
        
        Log.d(TAG, "Device ID obtained: " + (deviceImei != null ? deviceImei.substring(0, 4) + "****" : "null"));
    }
    
    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private String getDeviceInfoJson() {
        JsonObject deviceInfo = new JsonObject();
        deviceInfo.addProperty("model", Build.MODEL);
        deviceInfo.addProperty("manufacturer", Build.MANUFACTURER);
        deviceInfo.addProperty("androidVersion", Build.VERSION.RELEASE);
        deviceInfo.addProperty("apiLevel", Build.VERSION.SDK_INT);
        deviceInfo.addProperty("deviceImei", deviceImei);
        return deviceInfo.toString();
    }
    
    private void updateDeviceInfo() {
        String info = "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                     "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                     "ID: " + (deviceImei != null ? deviceImei.substring(0, Math.min(8, deviceImei.length())) + "..." : "Unknown");
        tvDeviceInfo.setText(info);
    }
    
    private void showProgress(String message) {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }
    
    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Toast: " + message);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            deviceAdminEnabled = devicePolicyManager.isAdminActive(deviceAdminReceiver);
            
            if (deviceAdminEnabled) {
                showToast("Device admin enabled successfully!");
                completeSetup();
            } else {
                showToast("Device admin is required for parental controls");
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean fineLocationGranted = grantResults.length > 0 && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean coarseLocationGranted = grantResults.length > 1 && 
                    grantResults[1] == PackageManager.PERMISSION_GRANTED;
            
            if (fineLocationGranted && coarseLocationGranted) {
                showToast("Location permissions granted!");
                
                // For Android 10+ request background location separately
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 
                            LOCATION_PERMISSION_REQUEST + 1);
                }
            } else {
                showToast("Location permissions are required for GPS tracking");
            }
        } else if (requestCode == PHONE_STATE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceImei();
                updateDeviceInfo();
            }
        }
    }
    
    /**
     * Get the server base URL - configurable for different environments
     */
    private String getServerBaseUrl() {
        // Use production URL when deployed
        // For development, use the current Replit URL
        SharedPreferences prefs = getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
        String customUrl = prefs.getString("server_url", "");
        
        if (!customUrl.isEmpty()) {
            return customUrl;
        }
        
        // Default to current Replit production URL
        // EMERGENCY FIX: Use production URL that works - replit.app production domain
        return "https://knets.replit.app";
    }
    
    /**
     * Android 13+ compatibility setup for critical security and runtime requirements
     */
    private void setupAndroid13Compatibility() {
        try {
            // Setup OnBackInvokedCallback for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                backInvokedCallback = new OnBackInvokedCallback() {
                    @Override
                    public void onBackInvoked() {
                        // Handle back gesture for Android 13+
                        handleBackAction();
                    }
                };
                
                // Register the callback
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, 
                    backInvokedCallback
                );
            }
            
            // Setup traditional back pressed for older versions
            backPressedCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    handleBackAction();
                }
            };
            getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
            
            Log.d(TAG, "Android 13+ compatibility setup completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Android 13+ compatibility", e);
            // Continue without crashing - fallback to basic functionality
        }
    }
    
    /**
     * Handle back action for both Android 13+ and older versions
     */
    private void handleBackAction() {
        if (workflowCompleted) {
            // Move app to background instead of closing during active monitoring
            moveTaskToBack(true);
        } else {
            // Prevent accidental exit during setup
            showToast("Complete the setup to use Knets Jr");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up Android 13+ callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backInvokedCallback != null) {
            try {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering back callback", e);
            }
        }
        
        if (backPressedCallback != null) {
            backPressedCallback.remove();
        }
    }
}