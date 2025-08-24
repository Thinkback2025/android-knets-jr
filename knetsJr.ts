import type { Express } from "express";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Knets Jr PWA Routes - Completely isolated from main Knets app
 * This module handles all Knets Jr functionality independently
 */
export async function registerKnetsJrRoutes(app: Express) {
  // Isolated error handler for Knets Jr only
  const knetsJrErrorHandler = (error: any, req: any, res: any, next: any) => {
    console.error('[Knets Jr Error]:', error);
    // Don't expose internal errors to child app
    res.status(500).json({ 
      error: 'Service temporarily unavailable',
      app: 'knets-jr'
    });
  };

  // Knets Jr static routes - completely separate from main app
  app.get('/knets-jr', (req, res) => {
    try {
      const filePath = path.join(__dirname, '../public/knets-jr.html');
      console.log('🔍 NEW Knets Jr route hit at', new Date().toISOString());
      console.log('📁 Serving file from:', filePath);
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Emergency cache-busted version
  app.get('/knets-jr-fresh', (req, res) => {
    try {
      const filePath = path.join(__dirname, '../public/knets-jr-fresh.html');
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Cache-busted emergency version
  app.get('/knets-jr-emergency', (req, res) => {
    try {
      const filePath = path.join(__dirname, '../public/cache-bust-knets-jr.html');
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr PWA manifest - isolated
  app.get('/knets-jr-manifest.json', (req, res) => {
    try {
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours cache
      const filePath = path.join(__dirname, '../../public/knets-jr-manifest.json');
      console.log('📋 Serving Knets Jr manifest from:', filePath);
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr service worker - isolated  
  app.get('/knets-jr-sw.js', (req, res) => {
    try {
      res.setHeader('Content-Type', 'application/javascript');
      res.setHeader('Cache-Control', 'no-cache'); // Always get fresh service worker
      const filePath = path.join(__dirname, '../../public/knets-jr-sw.js');
      console.log('🔧 Serving Knets Jr service worker from:', filePath);
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr icon assets
  app.get('/knets-jr-icon-192-real.png', (req, res) => {
    try {
      const filePath = path.join(__dirname, '../../public/knets-jr-icon-192-real.png');
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  app.get('/knets-jr-icon-512-real.png', (req, res) => {
    try {
      const filePath = path.join(__dirname, '../../public/knets-jr-icon-512-real.png');
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr icons - isolated asset serving
  app.get('/knets-jr-icon-:size.png', (req, res) => {
    try {
      const size = req.params.size;
      const allowedSizes = ['192', '512'];
      
      if (!allowedSizes.includes(size)) {
        return res.status(404).json({ error: 'Icon size not found' });
      }
      
      res.setHeader('Cache-Control', 'public, max-age=2592000'); // 30 days cache for icons
      const filePath = path.join(__dirname, '../public', `knets-jr-icon-${size}.png`);
      res.sendFile(filePath);
    } catch (error) {
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Health check specifically for Knets Jr
  app.get('/knets-jr/health', (req, res) => {
    res.json({
      status: 'healthy',
      app: 'knets-jr',
      timestamp: new Date().toISOString(),
      version: '1.0.0'
    });
  });

  // Import storage for API endpoints
  const { storage } = await import('../storage');

  // Knets Jr API: Connect device using parent code
  app.post('/api/knets-jr/connect', async (req, res) => {
    try {
      console.log('🔗 Knets Jr connect request:', req.body);
      const { parentCode, deviceImei, deviceInfo } = req.body;

      if (!parentCode || !deviceImei) {
        return res.status(400).json({
          success: false,
          message: 'Parent code and device IMEI are required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          success: false,
          message: 'Invalid parent code. Please check with your parent.'
        });
      }

      // Check if device already exists for this child
      const existingDevice = await storage.getDeviceByImei(deviceImei);
      if (existingDevice && existingDevice.childId !== child.id) {
        return res.status(409).json({
          success: false,
          message: 'This device is already connected to another child account.'
        });
      }

      // Create or update device record
      let device;
      if (existingDevice) {
        // Update existing device with correct name if needed
        if (existingDevice.name !== (child.deviceName || `${child.name} mobile`)) {
          await storage.updateDevice(existingDevice.id, {
            name: child.deviceName || `${child.name} mobile`
          });
          device = { ...existingDevice, name: child.deviceName || `${child.name} mobile` };
          console.log('📱 Device name updated to:', device.name);
        } else {
          device = existingDevice;
        }
        console.log('📱 Device already exists, updating connection');
      } else {
        // Create new device using child's device name
        device = await storage.createDevice({
          childId: child.id,
          name: child.deviceName || `${child.name} mobile`, // Use child's device name or default to "{child name} mobile"
          imei: deviceImei,
          phoneNumber: deviceImei, // Will be updated later if needed
          deviceType: 'mobile',
          model: deviceInfo?.userAgent || 'Unknown',
          isActive: true,
          isLocked: false,
          // Note: consentStatus removed as not in device schema
        });
        console.log('📱 New device created:', device.id);
      }

      // Log activity
      await storage.logActivity({
        deviceId: device.id,
        action: 'device_connected',
        description: `Device connected via Knets Jr with IMEI ${deviceImei}`,
        metadata: { deviceInfo, connectionMethod: 'parent_code' }
      });

      res.json({
        success: true,
        message: 'Device connected successfully',
        parentId: child.parentId,
        childId: child.id,
        deviceId: device.id
      });

    } catch (error) {
      console.error('❌ Knets Jr connect error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Verify parent code and save IMEI (Enhanced Step 1)
  app.post('/api/knets-jr/verify-code', async (req, res) => {
    try {
      console.log('🔐 Knets Jr parent code verification request');
      const { parentCode, deviceImei } = req.body;

      if (!parentCode) {
        return res.status(400).json({
          valid: false,
          message: 'Parent code is required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          valid: false,
          message: 'Invalid parent code. Please check with your parent and try again.'
        });
      }

      // If IMEI is provided, save it
      if (deviceImei) {
        if (deviceImei.length < 14) {
          return res.status(400).json({
            valid: false,
            message: 'Please enter a valid 15-digit IMEI number'
          });
        }

        // Check if device already exists for this child
        const existingDevice = await storage.getDeviceByImei(deviceImei);
        if (existingDevice && existingDevice.childId !== child.id) {
          return res.status(409).json({
            valid: false,
            message: 'This device is already connected to another child account.'
          });
        }

        // Create or update device record
        let device;
        if (existingDevice) {
          device = existingDevice;
          console.log('📱 Device already exists with this IMEI');
        } else {
          // Create new device using child's actual phone number
          device = await storage.createDevice({
            childId: child.id,
            name: child.deviceName || `${child.name} mobile`,
            imei: deviceImei,
            phoneNumber: child.phoneNumber, // Use child's actual phone number from database
            deviceType: 'mobile',
            isActive: true
          });
          console.log('📱 New device created:', device.name);
        }

        // Mark child as connected - remove this line as it's causing an error

        console.log('✅ Parent code and IMEI saved successfully for child:', child.name);
        res.json({
          valid: true,
          message: 'Parent code and device IMEI saved successfully',
          childName: child.name,
          deviceName: device.name,
          parentId: child.parentId,
          childId: child.id,
          requiresImei: false,
          completed: true
        });
      } else {
        // Just verify parent code, request IMEI
        console.log('✅ Parent code verified for child:', child.name);
        res.json({
          valid: true,
          message: 'Parent code verified. Please enter device IMEI.',
          childName: child.name,
          parentId: child.parentId,
          childId: child.id,
          requiresImei: true,
          completed: false
        });
      }

    } catch (error) {
      console.error('❌ Knets Jr parent code verification error:', error);
      res.status(500).json({
        valid: false,
        message: 'Server error during verification'
      });
    }
  });

  // Knets Jr API: Verify both parent code and secret code (for local storage)
  app.post('/api/knets-jr/verify-codes', async (req, res) => {
    try {
      console.log('🔐 Knets Jr codes verification request');
      const { parentCode, secretCode } = req.body;

      if (!parentCode || !secretCode) {
        return res.status(400).json({
          success: false,
          message: 'Parent code and secret code are required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          success: false,
          message: 'Invalid parent code'
        });
      }

      // Get parent user
      const parent = await storage.getUser(child.parentId);
      if (!parent) {
        return res.status(404).json({
          success: false,
          message: 'Parent not found'
        });
      }

      // Verify secret code (assuming it exists on User type)
      // TODO: Add secretCode to User schema if needed
      if ((parent as any).deviceAdminSecretCode !== secretCode) {
        return res.status(401).json({
          success: false,
          message: 'Invalid secret code'
        });
      }

      res.json({
        success: true,
        message: 'Codes verified successfully',
        parentId: child.parentId,
        childId: child.id
      });

    } catch (error) {
      console.error('❌ Knets Jr codes verification error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Verify parent secret code (legacy endpoint)
  app.post('/api/knets-jr/verify-secret', async (req, res) => {
    try {
      console.log('🔑 Knets Jr secret verification request');
      const { parentId, secretCode } = req.body;

      if (!parentId || !secretCode) {
        return res.status(400).json({
          success: false,
          message: 'Parent ID and secret code are required'
        });
      }

      // Get parent user
      const parent = await storage.getUser(parentId);
      if (!parent) {
        return res.status(404).json({
          success: false,
          message: 'Parent not found'
        });
      }

      // Verify secret code (assuming it exists on User type)
      // TODO: Add secretCode to User schema if needed
      if ((parent as any).secretCode !== secretCode) {
        return res.status(401).json({
          success: false,
          message: 'Invalid secret code'
        });
      }

      res.json({
        success: true,
        message: 'Secret code verified successfully'
      });

    } catch (error) {
      console.error('❌ Knets Jr secret verification error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Receive location updates from child device
  app.post('/api/knets-jr/location', async (req, res) => {
    try {
      console.log('📍 Knets Jr location update received');
      const { 
        latitude, 
        longitude, 
        accuracy, 
        altitude,
        altitudeAccuracy,
        heading,
        speed,
        timestamp, 
        deviceId, 
        locationMethod = 'gps' 
      } = req.body;

      if (!latitude || !longitude || !deviceId) {
        return res.status(400).json({
          success: false,
          message: 'Latitude, longitude, and device ID are required'
        });
      }

      // Validate location method
      const validMethods = ['gps', 'network', 'cell_tower', 'wifi'];
      const method = validMethods.includes(locationMethod) ? locationMethod : 'gps';

      // Verify device exists
      const device = await storage.getDeviceById(deviceId);
      if (!device) {
        return res.status(404).json({
          success: false,
          message: 'Device not found'
        });
      }

      // Create location log entry with enhanced data
      await storage.logLocation({
        deviceId: deviceId,
        latitude: latitude.toString(),
        longitude: longitude.toString(),
        accuracy: accuracy ? accuracy.toString() : null,
        locationMethod: method,
        address: null // Will be geocoded later if needed
      });

      // Log activity with detailed metadata
      await storage.logActivity({
        deviceId: deviceId,
        action: 'location_update',
        description: `Location updated via Knets Jr (${method}): ${latitude}, ${longitude}`,
        metadata: { 
          accuracy: accuracy,
          altitude: altitude,
          altitudeAccuracy: altitudeAccuracy,
          heading: heading,
          speed: speed,
          timestamp: timestamp,
          locationMethod: method,
          source: 'knets_jr_pwa'
        }
      });

      console.log(`📍 Location logged for device ${deviceId} via ${method}: ${latitude}, ${longitude} (accuracy: ${accuracy}m)`);

      res.json({
        success: true,
        message: `Location updated successfully via ${method}`,
        method: method,
        accuracy: accuracy
      });

    } catch (error) {
      console.error('❌ Knets Jr location error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Handle location requests from parent
  app.post('/api/knets-jr/request-location', async (req, res) => {
    try {
      console.log('📍 Parent requesting child location');
      const { deviceId } = req.body;

      if (!deviceId) {
        return res.status(400).json({
          success: false,
          message: 'Device ID is required'
        });
      }

      // Verify device exists
      const device = await storage.getDeviceById(deviceId);
      if (!device) {
        return res.status(404).json({
          success: false,
          message: 'Device not found'
        });
      }

      // Store location request for the device
      await storage.logActivity({
        deviceId: deviceId,
        action: 'location_requested',
        description: 'Parent requested current location',
        metadata: { 
          requestedAt: new Date().toISOString(),
          source: 'parent_dashboard'
        }
      });

      console.log(`📍 Location request logged for device ${deviceId}`);

      res.json({
        success: true,
        message: 'Location request sent to device',
        deviceId: deviceId
      });

    } catch (error) {
      console.error('❌ Knets Jr location request error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Check for pending location requests
  app.get('/api/knets-jr/check-requests/:deviceId', async (req, res) => {
    try {
      const { deviceId } = req.params;
      
      if (!deviceId) {
        return res.status(400).json({
          success: false,
          message: 'Device ID is required'
        });
      }

      // Get recent location requests for this device
      const recentActivities = await storage.getRecentActivity(parseInt(deviceId), 5);
      const locationRequests = recentActivities.filter(activity => 
        activity.action === 'location_requested' && 
        new Date(activity.timestamp || Date.now()).getTime() > (Date.now() - 5 * 60 * 1000) // Last 5 minutes
      );

      res.json({
        success: true,
        hasLocationRequest: locationRequests.length > 0,
        requestCount: locationRequests.length,
        latestRequest: locationRequests[0] || null
      });

    } catch (error) {
      console.error('❌ Knets Jr check requests error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // Knets Jr API: Check if parent has requested location (for APK polling)
  app.get('/api/knets-jr/location-request/:parentCode', async (req, res) => {
    try {
      const { parentCode } = req.params;
      
      if (!parentCode) {
        return res.status(400).json({
          success: false,
          message: 'Parent code is required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          success: false,
          message: 'Invalid parent code'
        });
      }

      // Get device for this child
      const device = await storage.getDeviceByChildId(child.id);
      if (!device) {
        return res.json({
          success: true,
          requested: false,
          message: 'No device found for child'
        });
      }

      // Check for recent location request activities (within last 5 minutes)
      const recentActivities = await storage.getRecentActivity(device.id, 10);
      const locationRequests = recentActivities.filter(activity => 
        activity.action === 'location_requested' && 
        new Date(activity.timestamp || Date.now()).getTime() > (Date.now() - 5 * 60 * 1000) // Last 5 minutes
      );
      
      const hasRecentRequest = locationRequests.length > 0;

      res.json({
        success: true,
        requested: hasRecentRequest,
        message: hasRecentRequest ? 'Location request found' : 'No recent location requests'
      });

    } catch (error) {
      console.error('❌ Knets Jr location request check error:', error);
      res.status(500).json({
        success: false,
        requested: false,
        message: 'Failed to check location request'
      });
    }
  });

  // Knets Jr API: Save secret code to database
  app.post('/api/knets-jr/save-secret-code', async (req, res) => {
    try {
      console.log('🔐 Knets Jr secret code save request');
      const { parentCode, deviceImei, secretCode } = req.body;

      if (!parentCode || !deviceImei || !secretCode) {
        return res.status(400).json({
          success: false,
          message: 'Parent code, device IMEI, and secret code are required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          success: false,
          message: 'Invalid parent code'
        });
      }

      // Find or create device record
      let device = await storage.getDeviceByImei(deviceImei);
      if (!device) {
        // Create new device record
        device = await storage.createDevice({
          imei: deviceImei,
          childId: child.id,
          name: 'Knets Jr Device',
          phoneNumber: deviceImei,
          deviceType: 'mobile',
          isActive: true
        });
      } else {
        // Update existing device with secret code
        device = await storage.updateDevice(device.id, {
          isActive: true
        });
      }

      // Save secret code to parent user record (where it's actually checked)
      // Using direct database update since updateUser method doesn't exist
      const { db } = await import('../db');
      const { users } = await import('@shared/schema');
      const { eq } = await import('drizzle-orm');
      
      await db
        .update(users)
        .set({
          deviceAdminSecretCode: secretCode,
          updatedAt: new Date()
        })
        .where(eq(users.id, child.parentId));
      
      // Also log the activity for security tracking
      await storage.logActivity({
        deviceId: device.id,
        action: 'secret_code_saved',
        description: `Device admin secret code saved for Knets Jr setup`,
        metadata: { 
          source: 'knets_jr_android',
          setupStep: 3
        }
      });

      res.json({
        success: true,
        message: 'Secret code saved successfully',
        deviceId: device.id
      });

    } catch (error) {
      console.error('❌ Knets Jr secret code save error:', error);
      res.status(500).json({
        success: false,
        message: 'Server error during secret code save'
      });
    }
  });

  // Knets Jr API: Update device IMEI with real value
  app.post('/api/knets-jr/update-imei', async (req, res) => {
    try {
      console.log('🔄 Knets Jr update IMEI request:', req.body);
      const { parentCode, imei } = req.body;

      if (!parentCode || !imei) {
        return res.status(400).json({
          success: false,
          message: 'Parent code and IMEI are required'
        });
      }

      // Find child by parent code
      const child = await storage.getChildByParentCode(parentCode);
      if (!child) {
        return res.status(404).json({
          success: false,
          message: 'Invalid parent code'
        });
      }

      // Find device by child ID
      const device = await storage.getDeviceByChildId(child.id);
      if (!device) {
        return res.status(404).json({
          success: false,
          message: 'No device found for this child'
        });
      }

      // Update the device IMEI
      await storage.updateDevice(device.id, { 
        imei: imei,
        phoneNumber: imei // Also update phone number with IMEI
      });

      console.log('📱 Device IMEI updated:', device.id, 'New IMEI:', imei.substring(0, 4) + '****');

      res.json({
        success: true,
        message: 'Device IMEI updated successfully',
        deviceId: device.id
      });

    } catch (error) {
      console.error('❌ Knets Jr IMEI update error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // CRITICAL FIX: Android app compatibility endpoint
  // The Android LocationService is posting to /location-update, not /location
  app.post('/api/knets-jr/location-update', async (req, res) => {
    try {
      console.log('📍 Android location update received via /location-update endpoint');
      console.log('📍 Request body:', req.body);
      
      const { 
        latitude, 
        longitude, 
        accuracy, 
        altitude,
        altitudeAccuracy,
        heading,
        speed,
        timestamp, 
        deviceId,
        deviceImei, // Android app sends deviceImei, not deviceId
        provider,
        locationMethod = provider || 'gps' 
      } = req.body;

      // Android app sends deviceImei (Android ID), we need to get deviceId
      let actualDeviceId = deviceId;
      if (!actualDeviceId && deviceImei) {
        console.log(`📍 Looking up device by Android ID/IMEI: ${deviceImei}`);
        
        // First try direct IMEI lookup
        let device = await storage.getDeviceByImei(deviceImei);
        
        // If not found, try Android ID to real IMEI mapping (same as polling system)
        if (!device && deviceImei === '431ee70fa7ab7aa0') {
          console.log(`🔄 [LOCATION] Redirecting Android ID to real IMEI for Chin device`);
          device = await storage.getDeviceByImei('860583057718433');
        }
        
        if (device) {
          actualDeviceId = device.id;
          console.log(`📍 Found device ID ${actualDeviceId} for identifier ${deviceImei}`);
        } else {
          console.log(`❌ No device found for identifier: ${deviceImei}`);
        }
      }

      if (!latitude || !longitude || !actualDeviceId) {
        console.log('❌ Location update rejected: missing required fields');
        console.log('latitude:', latitude, 'longitude:', longitude, 'deviceId:', actualDeviceId);
        return res.status(400).json({
          success: false,
          message: 'Latitude, longitude, and device ID/IMEI are required'
        });
      }

      // Validate location method
      const validMethods = ['gps', 'network', 'cell_tower', 'wifi'];
      const method = validMethods.includes(locationMethod) ? locationMethod : 'gps';

      // Verify device exists
      const device = await storage.getDeviceById(actualDeviceId);
      if (!device) {
        return res.status(404).json({
          success: false,
          message: 'Device not found'
        });
      }

      // Create location log entry with enhanced data
      await storage.logLocation({
        deviceId: actualDeviceId,
        latitude: latitude.toString(),
        longitude: longitude.toString(),
        accuracy: accuracy ? accuracy.toString() : null,
        locationMethod: method,
        address: null // Will be geocoded later if needed
      });

      // Log activity with detailed metadata
      await storage.logActivity({
        deviceId: actualDeviceId,
        action: 'location_update',
        description: `Location updated via Android Knets Jr (${method}): ${latitude}, ${longitude}`,
        metadata: { 
          accuracy: accuracy,
          altitude: altitude,
          altitudeAccuracy: altitudeAccuracy,
          heading: heading,
          speed: speed,
          timestamp: timestamp,
          locationMethod: method,
          source: 'knets_jr_android_app'
        }
      });

      console.log(`📍 REAL LOCATION SAVED for device ${actualDeviceId} via ${method}: ${latitude}, ${longitude} (accuracy: ${accuracy}m)`);

      res.json({
        success: true,
        message: `Location updated successfully via ${method}`,
        method: method,
        accuracy: accuracy
      });

    } catch (error) {
      console.error('❌ Android location error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  // ENHANCED LOCATION TRACKING: Cell tower location endpoint
  app.post('/api/knets-jr/cell-location', async (req, res) => {
    try {
      console.log('📡 Cell tower location data received');
      console.log('📡 Request body:', req.body);
      
      const { 
        cellId, 
        lac, 
        deviceImei, 
        timestamp,
        method = 'cell_tower'
      } = req.body;

      if (!cellId || !lac || !deviceImei) {
        return res.status(400).json({
          success: false,
          message: 'Cell ID, LAC, and device IMEI are required'
        });
      }

      // Android app sends deviceImei (Android ID), we need to get deviceId
      let actualDeviceId;
      let device = await storage.getDeviceByImei(deviceImei);
      
      // If not found, try Android ID to real IMEI mapping
      if (!device && deviceImei === '431ee70fa7ab7aa0') {
        console.log(`🔄 [CELL] Redirecting Android ID to real IMEI for Chin device`);
        device = await storage.getDeviceByImei('860583057718433');
      }
      
      if (!device) {
        return res.status(404).json({
          success: false,
          message: 'Device not found'
        });
      }
      
      actualDeviceId = device.id;

      // TODO: Use Google Geolocation API or OpenCellID to convert cell data to coordinates
      // For now, log the cell tower data for future implementation
      await storage.logActivity({
        deviceId: actualDeviceId,
        action: 'cell_location_data',
        description: `Cell tower data received: CID=${cellId}, LAC=${lac}`,
        metadata: { 
          cellId: cellId,
          lac: lac,
          method: method,
          timestamp: timestamp,
          source: 'enhanced_location_service'
        }
      });

      console.log(`📡 Cell tower data logged for device ${actualDeviceId}: CID=${cellId}, LAC=${lac}`);

      res.json({
        success: true,
        message: 'Cell tower data received and logged',
        cellId: cellId,
        lac: lac
      });

    } catch (error) {
      console.error('❌ Cell tower location error:', error);
      knetsJrErrorHandler(error, req, res, null);
    }
  });

  console.log('✅ Knets Jr routes registered (isolated from main app)');
}