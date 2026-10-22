const express = require('express');
const router = express.Router();
const db = require('../services/db');
const twilio = require('../services/twilio');
const logger = require('../utils/logger');
const { sendError } = require('../utils/errors');

function isValidGeoCoord(lat, lng) {
  const pLat = parseFloat(lat);
  const pLng = parseFloat(lng);
  return !isNaN(pLat) && !isNaN(pLng) && pLat >= -90 && pLat <= 90 && pLng >= -180 && pLng <= 180;
}

// POST /api/tracking/start — initiates a new live tracking session
router.post('/start', async (req, res) => {
  try {
    const { userId, userName, lat, lng, contacts = [] } = req.body;

    if (!isValidGeoCoord(lat, lng)) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }

    const session = await db.createTrackingSession({
      user_id: userId,
      user_name: userName || 'Citizen',
      lat: parseFloat(lat),
      lng: parseFloat(lng),
    });

    // Send SMS tracking link to emergency contacts
    const trackingUrl = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/track/${session.session_id}`;
    if (contacts && contacts.length > 0) {
      for (const phone of contacts) {
        await twilio.sendTrackingLink(phone, userName || 'Citizen', trackingUrl);
      }
    }

    req.app.get('io')?.emit('tracking:started', session);
    res.status(201).json({ success: true, session, trackingUrl });
  } catch (e) {
    sendError(res, e, 500, 'Failed to start tracking session');
  }
});

// GET /api/tracking/location?session_id=XYZ — live location poll
router.get('/location', async (req, res) => {
  try {
    const sid = req.query.session_id;
    if (!sid) return res.status(400).json({ error: 'session_id query parameter is required' });

    const session = await db.getTrackingSession(sid);
    if (!session) {
      // In a real safety product, never return invented simulated coordinates for unknown/expired sessions
      return res.status(404).json({ error: 'Tracking session not found or expired' });
    }

    res.json({
      sessionId: session.session_id,
      lat: session.lat,
      lng: session.lng,
      batteryLevel: session.battery_level || null,
      heartRate: session.heart_rate || null,
      userName: session.user_name,
      updatedAt: new Date(session.created_at || Date.now()).getTime()
    });
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch tracking location');
  }
});

// GET /api/tracking/history?session_id=XYZ — breadcrumb coordinate path
router.get('/history', async (req, res) => {
  try {
    const sid = req.query.session_id;
    if (!sid) return res.status(400).json({ error: 'session_id query parameter is required' });

    const session = await db.getTrackingSession(sid);
    if (!session) {
      return res.status(404).json({ error: 'Tracking session not found or expired' });
    }

    res.json({
      sessionId: session.session_id,
      breadcrumbs: session.history || []
    });
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch tracking history');
  }
});

// POST /api/tracking/ping — mobile device sending current coordinates
router.post('/ping', async (req, res) => {
  try {
    const { sessionId, lat, lng, batteryLevel, heartRate } = req.body;
    if (!sessionId) {
      return res.status(400).json({ error: 'sessionId is required' });
    }
    if (!isValidGeoCoord(lat, lng)) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }

    const updated = await db.updateTrackingLocation(
      sessionId,
      parseFloat(lat),
      parseFloat(lng),
      batteryLevel ? parseInt(batteryLevel) : null,
      heartRate ? parseInt(heartRate) : null
    );

    if (!updated) {
      return res.status(404).json({ error: 'Tracking session not found or expired' });
    }

    req.app.get('io')?.emit(`tracking:${sessionId}`, {
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      batteryLevel: batteryLevel || null,
      heartRate: heartRate || null,
      updatedAt: Date.now()
    });

    res.json({ success: true, timestamp: Date.now() });
  } catch (e) {
    sendError(res, e, 500, 'Failed to record tracking ping');
  }
});

module.exports = router;
