/**
 * NagarSetu V2 — Hazard Zone Routes
 * ────────────────────────────────────
 * GET    /api/hazards       — list active hazard zones with TTL
 * POST   /api/hazards       — create a temporary dynamic hazard geofence (Command Center)
 * DELETE /api/hazards/:id   — remove a hazard zone by id
 *
 * TTL janitor: runs every 30 s, auto-expires zones and emits `hazard:expired` via Socket.IO.
 */

const express = require('express');
const router  = express.Router();
const db      = require('../services/db');

// Module-level io reference, set by index.js after Socket.IO initialises
let _io = null;

// ── GET /api/hazards ──────────────────────────────────────────────────────────
router.get('/', async (req, res) => {
  try {
    const zones = await db.getActiveHazardZones();
    res.json(zones);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── GET /api/hazards/summary ──────────────────────────────────────────────────
router.get('/summary', async (req, res) => {
  try {
    const summary = await db.getHazardSummary();
    res.json({ status: 'success', ...summary });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── POST /api/hazards/check-point ─────────────────────────────────────────────
router.post('/check-point', async (req, res) => {
  try {
    const { lat, lng } = req.body;
    if (lat === undefined || lng === undefined) {
      return res.status(400).json({ error: 'lat and lng are required in body' });
    }
    const result = await db.checkHazardCollision(lat, lng);
    res.json({ status: 'success', lat: parseFloat(lat), lng: parseFloat(lng), ...result });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── POST /api/hazards ─────────────────────────────────────────────────────────
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
      center_lat:    parseFloat(lat),
      center_lng:    parseFloat(lng),
      radius_meters: parseInt(radiusMeters) || 300,
      risk_boost:    parseFloat(riskBoost)  || 1.5,
      expires_at:    expiresAt,
      note:          note || '',
      active:        true,
    };

    if (db.supabase) {
      const { data: created, error } = await db.supabase
        .from('hazard_zones').insert(data).select().single();
      if (error) throw error;
      req.app.get('io')?.emit('hazard:created', created);
      return res.status(201).json(created);
    }

    // Persist to disk store via db layer
    const created = await db.createHazardZone(data);
    req.app.get('io')?.emit('hazard:created', created);
    res.status(201).json(created);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── DELETE /api/hazards/:id ───────────────────────────────────────────────────
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const deleted = await db.deleteHazardZone(id);
    if (!deleted) return res.status(404).json({ error: 'Hazard zone not found' });
    req.app.get('io')?.emit('hazard:deleted', { id });
    res.json({ success: true, id });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── TTL Janitor (runs every 30 s) ─────────────────────────────────────────────
setInterval(async () => {
  try {
    const expired = await db.expireHazardZones();
    if (expired && expired.length > 0) {
      (_io || null)?.emit('hazard:expired', expired);
    }
  } catch (e) {
    // Silently swallow janitor errors to avoid crashing the server
  }
}, 30 * 1000);

// Called by index.js: hazardRoutes.setIo(io)
module.exports        = router;
module.exports.setIo  = (io) => { _io = io; };
