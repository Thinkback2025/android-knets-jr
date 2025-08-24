#!/bin/bash

echo "🌍 Building Enhanced Multi-Layer Location Tracking APK"
echo "======================================================="

# Set up enhanced build directory
ENHANCED_DIR="knets-android-ENHANCED-LOCATION"
BUILD_DIR="knets-android-ENHANCED-LOCATION-BUILD"

echo "📱 Creating enhanced location build directory..."
rm -rf $BUILD_DIR
cp -r knets-minimal-android $BUILD_DIR

cd $BUILD_DIR

echo "🔧 Configuring enhanced location tracking..."

# Update app name for enhanced version
sed -i 's/<string name="app_name">Knets Jr<\/string>/<string name="app_name">Knets Jr Enhanced<\/string>/' app/src/main/res/values/strings.xml

# Update application ID to distinguish from standard version
sed -i 's/applicationId "com.knets.jr"/applicationId "com.knets.jr.enhanced"/' app/build.gradle

echo "🌟 Enhanced Location Features:"
echo "   ✅ GPS Location (High accuracy)"
echo "   ✅ Network Location (WiFi/Cellular towers)"  
echo "   ✅ Cell Tower Triangulation (Works offline)"
echo "   ✅ IP Geolocation (Universal fallback)"
echo "   ✅ Multi-method fallback logic"
echo "   ✅ Parent-controlled activation"

echo "📋 Verifying enhanced service registration..."
if grep -q "EnhancedLocationService" app/src/main/AndroidManifest.xml; then
    echo "   ✅ EnhancedLocationService registered in manifest"
else
    echo "   ❌ EnhancedLocationService not found in manifest"
    exit 1
fi

if [ -f "app/src/main/java/com/knets/jr/EnhancedLocationService.java" ]; then
    echo "   ✅ EnhancedLocationService.java found"
else
    echo "   ❌ EnhancedLocationService.java not found"
    exit 1
fi

echo "🔨 Building enhanced APK..."
chmod +x gradlew

# Clean and build
./gradlew clean
if [ $? -ne 0 ]; then
    echo "❌ Clean failed"
    exit 1
fi

./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

# Copy APK to main directory
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp app/build/outputs/apk/debug/app-debug.apk ../knets-jr-enhanced-location.apk
    echo "✅ Enhanced APK built successfully: knets-jr-enhanced-location.apk"
    
    # Get APK size
    APK_SIZE=$(du -h ../knets-jr-enhanced-location.apk | cut -f1)
    echo "📦 APK Size: $APK_SIZE"
    
else
    echo "❌ APK not found after build"
    exit 1
fi

cd ..

echo ""
echo "🎯 Enhanced Location System Ready!"
echo "=================================="
echo "Package: knets-jr-enhanced-location.apk"
echo ""
echo "🌍 Location Tracking Methods:"
echo "   1️⃣  GPS (±3-5m accuracy)"
echo "   2️⃣  Network/WiFi (±10-100m accuracy)"  
echo "   3️⃣  Cell Tower (±150-500m accuracy)"
echo "   4️⃣  IP Geolocation (±5-50km accuracy)"
echo ""
echo "🚀 Features:"
echo "   • Works regardless of location settings"
echo "   • Intelligent fallback system" 
echo "   • Parent-controlled activation"
echo "   • Real-time location transmission"
echo "   • Multiple accuracy levels"
echo ""
echo "📱 Install with: adb install knets-jr-enhanced-location.apk"
echo "🌐 Compatible with: Android 6.0+ (API 23+)"

# Create package for distribution
echo "📦 Creating distribution package..."
rm -rf $ENHANCED_DIR
mkdir -p $ENHANCED_DIR

cp knets-jr-enhanced-location.apk $ENHANCED_DIR/
cp -r $BUILD_DIR/app/src/main/java/com/knets/jr/EnhancedLocationService.java $ENHANCED_DIR/
cp ANDROID_13_COMPATIBILITY_COMPLETE.md $ENHANCED_DIR/README.md

cat > $ENHANCED_DIR/ENHANCED_LOCATION_FEATURES.md << 'EOF'
# Knets Jr Enhanced Location Tracking

## Multi-Layer Location System

This enhanced version uses a sophisticated 4-tier location tracking system:

### 1. GPS Location (Highest Accuracy: ±3-5m)
- Uses device GPS for precise positioning
- Requires location permissions
- Best accuracy for outdoor locations

### 2. Network Location (High Accuracy: ±10-100m) 
- Uses WiFi access points and cellular towers
- Works indoors and with location services
- Good accuracy in urban areas

### 3. Cell Tower Triangulation (Medium Accuracy: ±150-500m)
- Uses cellular tower information for positioning
- Works without location services enabled
- Provides coverage in most areas with cell service

### 4. IP Geolocation (Basic Accuracy: ±5-50km)
- Uses internet IP address for location
- Universal fallback method
- Works on any internet connection

## How It Works

When a parent requests location:
1. System tries GPS first for best accuracy
2. Falls back to Network location if GPS unavailable
3. Uses Cell tower data if network location fails  
4. Finally uses IP geolocation as last resort

This ensures parents can always get location data regardless of:
- Child tampering with location settings
- Indoor/outdoor location
- Device configuration
- Network conditions

## Privacy Features

- No continuous tracking (only when parent requests)
- Parent-controlled activation
- Secure data transmission
- Local storage with server verification
EOF

tar -czf knets-android-ENHANCED-LOCATION.tar.gz $ENHANCED_DIR/

echo "✅ Distribution package created: knets-android-ENHANCED-LOCATION.tar.gz"
echo ""
echo "🎉 Enhanced location system ready for deployment!"