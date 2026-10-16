const express = require('express');
const router = express.Router();
const db = require('../services/db');

// GET /api/hazards — list active hazard zones with TTL
router.get('/', async (req, res) => {
  try {
    const zones = await db.getActiveHazardZones();
    res.json(zones);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/hazards — create a temporary dynamic hazard geofence (from Command Center)
router.post('/', async (req, res) => {
  try {
    const { name, lat, lng, radiusMeters, riskBoost, durationMinutes, note } = req.body;
    if (!name || !lat || !lng) {
      return res.status(400).json({ error: 'name, lat, and lng required' });
    }

    const expiresAt = durationMinutes 
      ? new Date(Date.now() + durationMinutes * 60000).toISOString()
      : null;

    const data = {
      name,
      center_lat: parseFloat(lat),
      center_lng: parseFloat(lng),
      radius_meters: parseInt(radiusMeters) || 300,
      risk_boost: parseFloat(riskBoost) || 1.5,
      expires_at: expiresAt,
      note: note || '',
      active: true
    };

    if (db.supabase) {
      const { data: created, error } = await db.supabase.from('hazard_zones').insert(data).select().single();
      if (error) throw error;
      req.app.get('io')?.emit('hazard:created', created);
      return res.status(201).json(created);
    }

    const fallback = { id: 'hz_' + Date.now(), ...data };
    req.app.get('io')?.emit('hazard:created', fallback);
    res.status(201).json(fallback);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
