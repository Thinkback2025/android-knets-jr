# Knets Jr - Enhanced Background Service APK

**Version:** Background Service Enhanced  
**Build Date:** August 23, 2025  
**Target SDK:** Android 6.0 - 14+ (API 23-34)

## 🎯 Key Enhancements

### **Critical Background Service Fixes:**
- ✅ **Auto-launch after device restart** - No user intervention needed
- ✅ **Battery optimization bypass** - Prevents Android from killing service
- ✅ **Wake lock permissions** - Ensures network requests complete
- ✅ **Continuous 30-second polling** - Reliable real-time communication
- ✅ **Background-only operation** - Works without UI
- ✅ **Enhanced foreground service** - Persistent operation

### **New Components:**
1. **BootReceiver.java** - Auto-start functionality after device restart
2. **Enhanced AndroidManifest.xml** - Critical background permissions
3. **Background initialization** - UI-less operation mode
4. **Comprehensive permissions** - Battery, wake lock, foreground service

## 🔧 Technical Specifications

### **Added Permissions:**
```xml
<!-- CRITICAL: Background activity permissions -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<!-- Auto-start permissions -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.QUICKBOOT_POWERON" />
```

### **Server Communication:**
- **Polling Interval:** 30 seconds
- **Android ID Support:** Automatic fallback and server redirect
- **Network Timeout:** 15 seconds (connect) / 15 seconds (read)
- **Server URL:** https://109f494a-e49e-4a8a-973f-659f67493858-00-23mfa5oss8rxi.janeway.replit.dev

## 📱 Installation & Setup

### **Installation Steps:**
1. Download and install APK on child device
2. Complete 3-step setup:
   - **Step 1:** Parent code verification + Manual IMEI input
   - **Step 2:** Secret code verification  
   - **Step 3:** Enable device admin permissions
3. App automatically starts background services
4. **Restart device** - App will auto-start and resume monitoring

### **Expected Behavior:**
- Continuous background operation after setup
- Auto-start after device restart (no user action needed)
- Real-time response to parent requests (within 30 seconds)
- Device shows as "online" consistently in parent dashboard

## 🔍 Troubleshooting

### **If Device Shows Offline:**
1. Check if setup was completed (all 3 steps)
2. Restart device - should auto-start background services
3. Check battery optimization settings (should be bypassed automatically)
4. Verify network connectivity

### **Performance Expectations:**
- **Before:** 40+ minute offline periods, manual restarts required
- **After:** Continuous 30-second polling, fully automatic operation

## 🚀 Key Features

### **Enhanced 3-Step Workflow:**
1. **Database-verified parent code** + **Manual IMEI collection**
2. **4-digit security code** for device admin
3. **Automatic device admin** and location permissions

### **Background Service Architecture:**
- **ServerPollingService** - Continuous parent command monitoring
- **LocationService** - GPS tracking when requested
- **BootReceiver** - Auto-start after device restart
- **KnetsDeviceAdminReceiver** - Device control capabilities

### **Real-Time Commands:**
- ENABLE_LOCATION - Activate GPS tracking
- REQUEST_LOCATION - Send immediate location update
- LOCK_DEVICE - Remote device lock
- UNLOCK_DEVICE - Remote device unlock

## 📊 Technical Details

### **Server Integration:**
- RESTful API communication
- JSON command processing
- Real-time command acknowledgment
- Android ID → IMEI mapping

### **Security Features:**
- Device admin protection
- Encrypted local storage
- Server-verified parent codes
- IMEI-based device identification

This enhanced APK resolves all polling service issues and provides reliable, continuous background operation for comprehensive parental control.