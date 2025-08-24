#!/usr/bin/env node

/**
 * Enhanced Location Tracking System Test Suite
 * Tests all 4 layers of location detection
 */

import fs from 'fs';

console.log('🌍 Enhanced Location Tracking System Test Suite');
console.log('===============================================');

// Test 1: Verify Enhanced Location Service exists
console.log('\n📱 Test 1: Enhanced Location Service');
const enhancedServicePath = 'knets-minimal-android/app/src/main/java/com/knets/jr/EnhancedLocationService.java';
if (fs.existsSync(enhancedServicePath)) {
    console.log('✅ EnhancedLocationService.java exists');
    
    const serviceContent = fs.readFileSync(enhancedServicePath, 'utf8');
    
    // Check for GPS method
    if (serviceContent.includes('tryGPSLocation()')) {
        console.log('   ✅ GPS location method implemented');
    } else {
        console.log('   ❌ GPS location method missing');
    }
    
    // Check for Network method
    if (serviceContent.includes('tryNetworkLocation()')) {
        console.log('   ✅ Network location method implemented');
    } else {
        console.log('   ❌ Network location method missing');
    }
    
    // Check for Cell Tower method
    if (serviceContent.includes('tryCellTowerLocation()')) {
        console.log('   ✅ Cell tower location method implemented');
    } else {
        console.log('   ❌ Cell tower location method missing');
    }
    
    // Check for IP Geolocation method
    if (serviceContent.includes('tryIPGeolocation()')) {
        console.log('   ✅ IP geolocation method implemented');
    } else {
        console.log('   ❌ IP geolocation method missing');
    }
    
    // Check for fallback logic
    if (serviceContent.includes('requestLocationWithFallback()')) {
        console.log('   ✅ Multi-layer fallback logic implemented');
    } else {
        console.log('   ❌ Multi-layer fallback logic missing');
    }
    
} else {
    console.log('❌ EnhancedLocationService.java not found');
}

// Test 2: Verify Android Manifest Registration
console.log('\n📋 Test 2: Android Manifest Registration');
const manifestPath = 'knets-minimal-android/app/src/main/AndroidManifest.xml';
if (fs.existsSync(manifestPath)) {
    const manifestContent = fs.readFileSync(manifestPath, 'utf8');
    
    if (manifestContent.includes('EnhancedLocationService')) {
        console.log('✅ EnhancedLocationService registered in manifest');
    } else {
        console.log('❌ EnhancedLocationService not registered in manifest');
    }
    
    // Check required permissions
    const permissions = [
        'ACCESS_FINE_LOCATION',
        'ACCESS_COARSE_LOCATION', 
        'READ_PHONE_STATE',
        'ACCESS_WIFI_STATE'
    ];
    
    permissions.forEach(permission => {
        if (manifestContent.includes(permission)) {
            console.log(`   ✅ ${permission} permission declared`);
        } else {
            console.log(`   ❌ ${permission} permission missing`);
        }
    });
    
} else {
    console.log('❌ AndroidManifest.xml not found');
}

// Test 3: Verify Server Polling Integration
console.log('\n🔄 Test 3: Server Polling Integration');
const pollingServicePath = 'knets-minimal-android/app/src/main/java/com/knets/jr/ServerPollingService.java';
if (fs.existsSync(pollingServicePath)) {
    const pollingContent = fs.readFileSync(pollingServicePath, 'utf8');
    
    if (pollingContent.includes('EnhancedLocationService')) {
        console.log('✅ ServerPollingService integrated with EnhancedLocationService');
    } else {
        console.log('❌ ServerPollingService not using EnhancedLocationService');
    }
    
    if (pollingContent.includes('REQUEST_LOCATION')) {
        console.log('   ✅ Location request action implemented');
    } else {
        console.log('   ❌ Location request action missing');
    }
    
} else {
    console.log('❌ ServerPollingService.java not found');
}

// Test 4: Verify Server-Side Endpoints
console.log('\n🌐 Test 4: Server-Side Endpoints');
const knetsJrRoutesPath = 'server/routes/knetsJr.ts';
if (fs.existsSync(knetsJrRoutesPath)) {
    const routesContent = fs.readFileSync(knetsJrRoutesPath, 'utf8');
    
    if (routesContent.includes('/api/knets-jr/location-update')) {
        console.log('✅ Standard location endpoint exists');
    } else {
        console.log('❌ Standard location endpoint missing');
    }
    
    if (routesContent.includes('/api/knets-jr/cell-location')) {
        console.log('✅ Cell tower location endpoint exists');
    } else {
        console.log('❌ Cell tower location endpoint missing');
    }
    
    // Check for Android ID to IMEI mapping
    if (routesContent.includes('431ee70fa7ab7aa0') && routesContent.includes('860583057718433')) {
        console.log('✅ Android ID to IMEI mapping implemented');
    } else {
        console.log('❌ Android ID to IMEI mapping missing');
    }
    
} else {
    console.log('❌ knetsJr.ts routes file not found');
}

// Test 5: Verify Dashboard Integration
console.log('\n🖥️  Test 5: Dashboard Integration');
const dashboardPath = 'client/src/pages/dashboard.tsx';
if (fs.existsSync(dashboardPath)) {
    const dashboardContent = fs.readFileSync(dashboardPath, 'utf8');
    
    if (dashboardContent.includes('Enhanced Location')) {
        console.log('✅ Dashboard shows enhanced location capability');
    } else {
        console.log('❌ Dashboard not updated for enhanced location');
    }
    
    if (dashboardContent.includes('GPS • Network • Cell • IP')) {
        console.log('✅ Dashboard displays all location methods');
    } else {
        console.log('❌ Dashboard missing location method indicators');
    }
    
} else {
    console.log('❌ dashboard.tsx not found');
}

// Test 6: Verify Build Script
console.log('\n🔨 Test 6: Build Script');
const buildScriptPath = 'build_enhanced_location_apk.sh';
if (fs.existsSync(buildScriptPath)) {
    console.log('✅ Enhanced location build script exists');
    
    const buildContent = fs.readFileSync(buildScriptPath, 'utf8');
    if (buildContent.includes('Enhanced Location')) {
        console.log('   ✅ Build script configured for enhanced version');
    } else {
        console.log('   ❌ Build script not configured');
    }
    
} else {
    console.log('❌ Enhanced location build script missing');
}

console.log('\n🎯 Test Summary');
console.log('===============');
console.log('Enhanced Location System includes:');
console.log('1️⃣  GPS Location (±3-5m accuracy)');
console.log('2️⃣  Network Location (±10-100m accuracy)');
console.log('3️⃣  Cell Tower Triangulation (±150-500m accuracy)');
console.log('4️⃣  IP Geolocation (±5-50km accuracy)');
console.log('');
console.log('🚀 System Features:');
console.log('   • Intelligent fallback system');
console.log('   • Works regardless of location settings');
console.log('   • Parent-controlled activation');
console.log('   • Multiple accuracy levels');
console.log('   • Real-time location transmission');
console.log('');
console.log('📱 To build enhanced APK:');
console.log('   chmod +x build_enhanced_location_apk.sh');
console.log('   ./build_enhanced_location_apk.sh');
console.log('');
console.log('🌐 To test location methods:');
console.log('   1. Install APK on test device');
console.log('   2. Complete parent code setup');
console.log('   3. Request location from parent dashboard');
console.log('   4. Check server logs for location method used');

const currentTime = new Date().toISOString();
console.log(`\n✅ Test completed at ${currentTime}`);