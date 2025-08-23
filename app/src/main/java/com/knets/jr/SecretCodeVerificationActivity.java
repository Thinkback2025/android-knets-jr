package com.knets.jr;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * CRITICAL SYSTEM-LEVEL PROTECTION: Secret code verification before device admin disable
 */
public class SecretCodeVerificationActivity extends Activity {
    private static final String TAG = "KnetsSecretVerify";
    
    private EditText etSecretCode;
    private Button btnVerify, btnCancel;
    private TextView tvMessage;
    
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_verification);
        
        initializeViews();
        setupDeviceAdmin();
        
        Log.d(TAG, "Secret code verification screen launched");
    }
    
    private void initializeViews() {
        etSecretCode = findViewById(R.id.etSecretCodeVerify);
        btnVerify = findViewById(R.id.btnVerifySecret);
        btnCancel = findViewById(R.id.btnCancelDisable);
        tvMessage = findViewById(R.id.tvVerificationMessage);
        
        tvMessage.setText("Enter your 4-digit secret code to disable parental controls:");
        
        // Auto-enable verify button when 4 digits entered
        etSecretCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                btnVerify.setEnabled(s.length() == 4);
            }
        });
        
        btnVerify.setOnClickListener(v -> verifySecretCode());
        btnCancel.setOnClickListener(v -> {
            showToast("Device admin disable cancelled");
            finish();
        });
    }
    
    private void setupDeviceAdmin() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminReceiver = new ComponentName(this, KnetsDeviceAdminReceiver.class);
        preferences = getSharedPreferences("knets_jr", Context.MODE_PRIVATE);
    }
    
    private void verifySecretCode() {
        String enteredCode = etSecretCode.getText().toString().trim();
        String storedSecretCode = preferences.getString("secret_code", "");
        
        if (storedSecretCode.isEmpty()) {
            showToast("No secret code set - Contact parent");
            finish();
            return;
        }
        
        if (enteredCode.equals(storedSecretCode)) {
            // CRITICAL: Secret code verified - allow device admin disable
            Log.d(TAG, "✅ Secret code verified - Allowing device admin disable");
            showToast("Secret code verified. Disabling device admin...");
            
            try {
                // Programmatically disable device admin
                devicePolicyManager.removeActiveAdmin(deviceAdminReceiver);
                showToast("Parental controls disabled successfully");
                
                // Clear stored data
                preferences.edit()
                    .putBoolean("workflow_completed", false)
                    .putBoolean("device_admin_enabled", false)
                    .apply();
                    
            } catch (Exception e) {
                Log.e(TAG, "Failed to disable device admin", e);
                showToast("Failed to disable device admin");
            }
            
            finish();
            
        } else {
            // CRITICAL: Wrong secret code - block access
            Log.w(TAG, "❌ Invalid secret code entered");
            showToast("Wrong secret code. Access denied.");
            etSecretCode.setText("");
            etSecretCode.requestFocus();
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back press to avoid bypass
        showToast("Enter secret code or cancel to exit");
    }
}