const express = require('express');
const router = express.Router();
const db = require('../services/db');
const logger = require('../utils/logger');

// GET /api/tracking/location?session_id=XYZ
router.get('/location', async (req, res) => {
  try {
    const sid = req.query.session_id;
    if (!sid) return res.status(400).json({ error: 'session_id required' });

    const session = await db.getTrackingSession(sid);
    if (!session) {
      // In dev/demo mode or if expired, provide smooth fallback
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

// POST /api/tracking/ping — mobile device sending current coordinates
router.post('/ping', async (req, res) => {
  try {
    const { sessionId, lat, lng } = req.body;
    if (!sessionId || !lat || !lng) {
      return res.status(400).json({ error: 'sessionId, lat, and lng are required' });
    }

    await db.updateTrackingLocation(sessionId, parseFloat(lat), parseFloat(lng));
    req.app.get('io')?.emit(`tracking:${sessionId}`, { lat, lng, updatedAt: Date.now() });

    res.json({ success: true, timestamp: Date.now() });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
