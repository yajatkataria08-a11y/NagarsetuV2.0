const express = require('express');
const router = express.Router();
const db = require('../services/db');
const twilio = require('../services/twilio');
const logger = require('../utils/logger');

// POST /api/tracking/start — initiates a new live tracking session (e.g. from RakshaScreen or QR)
router.post('/start', async (req, res) => {
  try {
    const { userId, userName, lat, lng, contacts = [] } = req.body;
    const session = await db.createTrackingSession({
      user_id: userId,
      user_name: userName,
      lat: parseFloat(lat) || 23.2599,
      lng: parseFloat(lng) || 77.4126,
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
    res.status(500).json({ error: e.message });
  }
});

// GET /api/tracking/location?session_id=XYZ — live location poll
router.get('/location', async (req, res) => {
  try {
    const sid = req.query.session_id;
    if (!sid) return res.status(400).json({ error: 'session_id required' });

    const session = await db.getTrackingSession(sid);
    if (!session) {
      return res.json({
        sessionId: sid,
        lat: 23.2599 + (Math.random() - 0.5) * 0.002,
        lng: 77.4126 + (Math.random() - 0.5) * 0.002,
        batteryLevel: 42,
        heartRate: 104,
        updatedAt: Date.now(),
        userName: 'Priya Sharma (Simulated)',
        ward: 'Ward 7 — Arera Colony'
      });
    }

    res.json({
      sessionId: session.session_id,
      lat: session.lat,
      lng: session.lng,
      batteryLevel: session.battery_level || 50,
      heartRate: session.heart_rate || null,
      userName: session.user_name,
      updatedAt: new Date(session.created_at).getTime()
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// GET /api/tracking/history?session_id=XYZ — breadcrumb coordinate path
router.get('/history', async (req, res) => {
  try {
    const sid = req.query.session_id;
    if (!sid) return res.status(400).json({ error: 'session_id required' });

    const session = await db.getTrackingSession(sid);
    if (!session) {
      return res.json({ sessionId: sid, breadcrumbs: [] });
    }

    res.json({
      sessionId: session.session_id,
      breadcrumbs: session.history || []
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/tracking/ping — mobile device sending current coordinates
router.post('/ping', async (req, res) => {
  try {
    const { sessionId, lat, lng, batteryLevel, heartRate } = req.body;
    if (!sessionId || !lat || !lng) {
      return res.status(400).json({ error: 'sessionId, lat, and lng are required' });
    }

    const updated = await db.updateTrackingLocation(
      sessionId,
      parseFloat(lat),
      parseFloat(lng),
      batteryLevel ? parseInt(batteryLevel) : null,
      heartRate ? parseInt(heartRate) : null
    );

    req.app.get('io')?.emit(`tracking:${sessionId}`, {
      lat,
      lng,
      batteryLevel,
      heartRate,
      updatedAt: Date.now()
    });

    res.json({ success: true, timestamp: Date.now() });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
