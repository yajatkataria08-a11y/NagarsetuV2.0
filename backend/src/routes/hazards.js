/**
 * NagarSetu V2 — Authenticated Hazard Zone Routes
 * ────────────────────────────────────────────────
 * GET    /api/hazards            — list active hazard zones with TTL
 * GET    /api/hazards/summary    — summary statistics of hazard zones
 * POST   /api/hazards/check-point— check if a coordinate falls in a hazard zone
 * POST   /api/hazards            — create dynamic hazard geofence (Requires Auth)
 * DELETE /api/hazards/:id        — remove a hazard zone by id (Requires Auth)
 *
 * TTL janitor: runs every 30 s, auto-expires zones and emits `hazard:expired` via Socket.IO.
 */

const express = require('express');
const router  = express.Router();
const db      = require('../services/db');
const { requireAuth } = require('../middleware/auth');
const { sendError } = require('../utils/errors');

function isValidGeoCoord(lat, lng) {
  const pLat = parseFloat(lat);
  const pLng = parseFloat(lng);
  return !isNaN(pLat) && !isNaN(pLng) && pLat >= -90 && pLat <= 90 && pLng >= -180 && pLng <= 180;
}

// Module-level io reference, set by index.js after Socket.IO initialises
let _io = null;

// ── GET /api/hazards ──────────────────────────────────────────────────────────
router.get('/', async (req, res) => {
  try {
    const zones = await db.getActiveHazardZones();
    res.json(zones);
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch active hazard zones');
  }
});

// ── GET /api/hazards/summary ──────────────────────────────────────────────────
router.get('/summary', async (req, res) => {
  try {
    const summary = await db.getHazardSummary();
    res.json({ status: 'success', ...summary });
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch hazard summary');
  }
});

// ── POST /api/hazards/check-point ─────────────────────────────────────────────
router.post('/check-point', async (req, res) => {
  try {
    const { lat, lng } = req.body;
    if (!isValidGeoCoord(lat, lng)) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }
    const result = await db.checkHazardCollision(lat, lng);
    res.json({ status: 'success', lat: parseFloat(lat), lng: parseFloat(lng), ...result });
  } catch (e) {
    sendError(res, e, 500, 'Failed to check hazard collision');
  }
});

// ── POST /api/hazards (Requires Auth) ─────────────────────────────────────────
router.post('/', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const { name, lat, lng, radiusMeters, riskBoost, durationMinutes, note } = req.body;
    if (!name || typeof name !== 'string' || !name.trim()) {
      return res.status(400).json({ error: 'name is required and must be a non-empty string' });
    }
    if (!isValidGeoCoord(lat, lng)) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }

    const expiresAt = durationMinutes
      ? new Date(Date.now() + durationMinutes * 60000).toISOString()
      : null;

    const data = {
      name:          name.trim(),
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
    sendError(res, e, 500, 'Failed to create hazard zone');
  }
});

// ── DELETE /api/hazards/:id (Requires Auth) ───────────────────────────────────
router.delete('/:id', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const { id } = req.params;
    const deleted = await db.deleteHazardZone(id);
    if (!deleted) return res.status(404).json({ error: 'Hazard zone not found' });
    req.app.get('io')?.emit('hazard:deleted', { id });
    res.json({ success: true, id });
  } catch (e) {
    sendError(res, e, 500, 'Failed to delete hazard zone');
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
    // Silent fail in background timer
  }
}, 30000);

module.exports = router;
module.exports.setIo = (io) => { _io = io; };
