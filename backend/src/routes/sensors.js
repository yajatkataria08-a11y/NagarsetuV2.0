/**
 * NagarSetu V2 — Authenticated IoT Sensor Telemetry Ingest (Phase 5)
 * ─────────────────────────────────────────────────────────────────
 * POST /api/ingest/sensor
 *   Accepts verified IoT sensor readings with API key authentication,
 *   maintains a bounded sliding-window memory buffer (preventing memory leaks/DOS),
 *   and auto-escalates to hazard zones when persistent thresholds are breached.
 */

const express = require('express');
const router  = express.Router();
const db      = require('../services/db');
const logger  = require('../utils/logger');
const config  = require('../config');
const { sendError } = require('../utils/errors');

// Allowed sensor types
const VALID_TYPES = new Set(['temperature', 'water_depth', 'gas_ppm', 'decibel', 'flood_level']);

// Sliding-window buffer: Map<"deviceId:type", Array<reading>>
const sensorBuffer = new Map();
const BUFFER_SIZE  = 5;
const MAX_BUFFER_KEYS = 100; // Hard cap on stored sensor keys to prevent unbounded memory growth

const SENSOR_API_KEY = process.env.SENSOR_API_KEY || config.SENSOR_API_KEY || 'nagarsetu_iot_sensor_key_2026';

/**
 * Validates sensor API key in header (x-sensor-api-key or Bearer token)
 */
function requireSensorAuth(req, res, next) {
  if (process.env.DISABLE_AUTH === 'true' || config.DISABLE_AUTH === 'true') {
    return next();
  }

  const authHeader = req.headers['authorization'] || '';
  const bearerKey = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
  const headerKey = req.headers['x-sensor-api-key'] || bearerKey;

  if (!headerKey || headerKey !== SENSOR_API_KEY) {
    logger.warn('[Sensors] Unauthorized telemetry ingest attempt', {
      ip: req.ip,
      providedKey: headerKey ? 'invalid' : 'missing'
    });
    return res.status(401).json({ error: 'Unauthorized: Invalid or missing x-sensor-api-key header' });
  }

  next();
}

/**
 * Prunes inactive keys and caps total buffer size
 */
function pruneSensorBuffer() {
  const now = Date.now();
  // Evict entries with no activity for 30 minutes
  for (const [key, window] of sensorBuffer.entries()) {
    const newest = window[window.length - 1];
    if (newest && (now - new Date(newest.timestamp).getTime() > 30 * 60 * 1000)) {
      sensorBuffer.delete(key);
    }
  }

  // If still above maximum capacity, remove oldest keys
  if (sensorBuffer.size > MAX_BUFFER_KEYS) {
    const excess = sensorBuffer.size - MAX_BUFFER_KEYS;
    const keysToEvict = Array.from(sensorBuffer.keys()).slice(0, excess);
    for (const k of keysToEvict) {
      sensorBuffer.delete(k);
    }
    logger.info(`[Sensors] Evicted ${excess} stale device buffers to enforce memory cap`);
  }
}

/**
 * Append a reading to the device+type sliding window.
 * @returns {Array} updated window (newest last)
 */
function pushReading(deviceId, type, reading) {
  pruneSensorBuffer();

  const key = `${deviceId}:${type}`;
  if (!sensorBuffer.has(key)) {
    sensorBuffer.set(key, []);
  }
  const window = sensorBuffer.get(key);
  window.push(reading);
  if (window.length > BUFFER_SIZE) window.shift();
  return window;
}

/**
 * Count how many of the last `n` readings in `window` satisfy `predicate`.
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
router.post('/sensor', requireSensorAuth, async (req, res) => {
  try {
    const { deviceId, type, value, unit, lat, lng, ward, timestamp } = req.body;

    // ── Validation ─────────────────────────────────────────────────────────
    if (!deviceId || typeof deviceId !== 'string' || deviceId.trim().length === 0) {
      return res.status(400).json({ error: 'Valid non-empty deviceId string is required' });
    }
    if (!type || !VALID_TYPES.has(type)) {
      return res.status(400).json({
        error: `Invalid sensor type. Must be one of: ${[...VALID_TYPES].join(', ')}`,
      });
    }

    const numericValue = parseFloat(value);
    if (isNaN(numericValue) || !isFinite(numericValue)) {
      return res.status(400).json({ error: 'value must be a valid finite number' });
    }

    const parsedLat = parseFloat(lat);
    const parsedLng = parseFloat(lng);
    if (isNaN(parsedLat) || isNaN(parsedLng) || parsedLat < -90 || parsedLat > 90 || parsedLng < -180 || parsedLng > 180) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }

    // ── Insert into sliding-window buffer ───────────────────────────────────
    const reading = {
      value:     numericValue,
      unit:      unit || '',
      lat:       parsedLat,
      lng:       parsedLng,
      ward:      ward ? String(ward).trim() : 'Unknown Ward',
      timestamp: timestamp || new Date().toISOString(),
    };

    const cleanDeviceId = String(deviceId).slice(0, 64);
    const window = pushReading(cleanDeviceId, type, reading);

    // ── Check escalation rules ───────────────────────────────────────────────
    let escalated      = false;
    let hazardCreated  = null;

    for (const rule of ESCALATION_RULES) {
      if (rule.type !== type) continue;

      if (countConsecutiveTrailing(window, rule.count, r => rule.predicate(r.value))) {
        const wardLabel = reading.ward ? `[${reading.ward}]` : '';
        const hazardData = {
          name:          rule.label(wardLabel),
          center_lat:    parsedLat,
          center_lng:    parsedLng,
          radius_meters: rule.radius,
          risk_boost:    rule.boost,
          expires_at:    new Date(Date.now() + rule.ttlMin * 60000).toISOString(),
          note:          `Auto-created by authenticated sensor ${cleanDeviceId} (${type}=${numericValue}${unit || ''})`,
          active:        true,
        };

        hazardCreated = await db.createHazardZone(hazardData);
        escalated     = true;

        // Emit event via Socket.IO if available
        req.app.get('io')?.emit('hazard:auto_created', hazardCreated);

        logger.warn('[Sensors] Escalation triggered', {
          deviceId: cleanDeviceId,
          type,
          value: numericValue,
          rule: rule.label(wardLabel),
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
    sendError(res, e, 500, 'Sensor telemetry ingestion error');
  }
});

module.exports = router;
