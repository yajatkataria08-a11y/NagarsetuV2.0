/**
 * NagarSetu V2 — Sensor Telemetry Ingest (Phase 5)
 * ──────────────────────────────────────────────────
 * POST /api/ingest/sensor
 *   Accepts IoT sensor readings, maintains a per-device sliding-window buffer,
 *   and auto-escalates to hazard zones when thresholds are breached.
 *
 * Escalation rules (consecutive readings):
 *   water_depth > 0.45 m  × 3  → flood alert    (500 m, 2.0 boost, 2 h TTL)
 *   gas_ppm     > 70      × 2  → gas hazard      (300 m, 3.0 boost, 1 h TTL)
 *   temperature > 48 °C   × 2  → extreme heat    (400 m, 1.5 boost, 3 h TTL)
 *   decibel     > 110 dB  × 4  → noise/blast     (200 m, 1.8 boost, 30 min TTL)
 */

const express = require('express');
const router  = express.Router();
const db      = require('../services/db');
const logger  = require('../utils/logger');

// Allowed sensor types
const VALID_TYPES = new Set(['temperature', 'water_depth', 'gas_ppm', 'decibel', 'flood_level']);

// Sliding-window buffer: Map<"deviceId:type", Array<reading>>
const sensorBuffer = new Map();
const BUFFER_SIZE  = 5;

/**
 * Append a reading to the device+type sliding window.
 * @returns {Array} updated window (newest last)
 */
function pushReading(deviceId, type, reading) {
  const key = `${deviceId}:${type}`;
  if (!sensorBuffer.has(key)) sensorBuffer.set(key, []);
  const window = sensorBuffer.get(key);
  window.push(reading);
  if (window.length > BUFFER_SIZE) window.shift();
  return window;
}

/**
 * Count how many of the last `n` readings in `window` satisfy `predicate`.
 * The check starts from the most recent reading.
 */
function countConsecutiveTrailing(window, n, predicate) {
  const slice = window.slice(-n);
  return slice.length === n && slice.every(predicate);
}

// Escalation rule definitions
const ESCALATION_RULES = [
  {
    type:      'water_depth',
    predicate: v => v > 0.45,
    count:     3,
    label:     ward => `Auto: Flood Alert ${ward}`,
    radius:    500,
    boost:     2.0,
    ttlMin:    120,
  },
  {
    type:      'gas_ppm',
    predicate: v => v > 70,
    count:     2,
    label:     ward => `Auto: Gas Hazard ${ward}`,
    radius:    300,
    boost:     3.0,
    ttlMin:    60,
  },
  {
    type:      'temperature',
    predicate: v => v > 48,
    count:     2,
    label:     ward => `Auto: Extreme Heat ${ward}`,
    radius:    400,
    boost:     1.5,
    ttlMin:    180,
  },
  {
    type:      'decibel',
    predicate: v => v > 110,
    count:     4,
    label:     ward => `Auto: Noise/Blast Alert ${ward}`,
    radius:    200,
    boost:     1.8,
    ttlMin:    30,
  },
];

// ── POST /api/ingest/sensor ───────────────────────────────────────────────────
router.post('/sensor', async (req, res) => {
  try {
    const { deviceId, type, value, unit, lat, lng, ward, timestamp } = req.body;

    // ── Validation ─────────────────────────────────────────────────────────
    if (!deviceId || !type || value == null) {
      return res.status(400).json({ error: 'deviceId, type, and value are required' });
    }
    if (!VALID_TYPES.has(type)) {
      return res.status(400).json({
        error: `Invalid sensor type. Must be one of: ${[...VALID_TYPES].join(', ')}`,
      });
    }
    const numericValue = parseFloat(value);
    if (isNaN(numericValue)) {
      return res.status(400).json({ error: 'value must be numeric' });
    }

    // ── Insert into sliding-window buffer ───────────────────────────────────
    const reading = {
      value:     numericValue,
      unit:      unit || '',
      lat:       parseFloat(lat)  || 0,
      lng:       parseFloat(lng)  || 0,
      ward:      ward || 'Unknown',
      timestamp: timestamp || new Date().toISOString(),
    };

    const window = pushReading(deviceId, type, reading);

    // ── Check escalation rules ───────────────────────────────────────────────
    let escalated      = false;
    let hazardCreated  = null;

    for (const rule of ESCALATION_RULES) {
      if (rule.type !== type) continue;

      if (countConsecutiveTrailing(window, rule.count, r => rule.predicate(r.value))) {
        const wardLabel = ward ? `[${ward}]` : '';
        const hazardData = {
          name:          rule.label(wardLabel),
          center_lat:    parseFloat(lat)  || 0,
          center_lng:    parseFloat(lng)  || 0,
          radius_meters: rule.radius,
          risk_boost:    rule.boost,
          expires_at:    new Date(Date.now() + rule.ttlMin * 60000).toISOString(),
          note:          `Auto-created by sensor ${deviceId} (${type}=${numericValue}${unit || ''})`,
          active:        true,
        };

        hazardCreated = await db.createHazardZone(hazardData);
        escalated     = true;

        // Emit event via Socket.IO if available
        req.app.get('io')?.emit('hazard:auto_created', hazardCreated);

        logger.warn('[Sensors] Escalation triggered', {
          deviceId, type, value: numericValue, rule: rule.label(wardLabel),
        });
        break; // Only the first matching rule fires per reading
      }
    }

    res.json({
      received:        true,
      escalated,
      ...(hazardCreated ? { hazard_created: hazardCreated } : {}),
      buffer_size:     window.length,
    });
  } catch (e) {
    logger.error('[Sensors] Ingest error', { error: e.message });
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
