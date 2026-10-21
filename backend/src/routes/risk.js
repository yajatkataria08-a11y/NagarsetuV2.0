/**
 * NagarSetu V2 — Risk Assessment Routes
 * ───────────────────────────────────────
 * GET  /api/risk/heatmap  — Gaussian spatio-temporal city risk grid (pure in-process)
 * POST /api/risk/point    — Single coordinate risk score (HTTP → spawn → fallback)
 */

const express = require('express');
const router  = express.Router();
const http    = require('http');
const { spawn } = require('child_process');
const db      = require('../services/db');
const logger  = require('../utils/logger');
const config  = require('../config');

const { PYTHON_BIN, AI_SCRIPT, AI_SERVICE_URL } = config;

const CITY_BOUNDS = {
  bhopal: { latMin: 23.18, latMax: 23.32, lngMin: 77.36, lngMax: 77.48 },
  indore: { latMin: 22.65, latMax: 22.80, lngMin: 75.80, lngMax: 75.95 },
};

/**
 * Try HTTP POST to AI microservice /risk endpoint with a 300 ms timeout.
 * @param {object} payload
 * @returns {Promise<object>}
 */
function httpRisk(payload) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(payload);
    const url  = new URL(`${AI_SERVICE_URL}/risk`);

    const options = {
      hostname: url.hostname,
      port:     url.port || 80,
      path:     url.pathname,
      method:   'POST',
      headers:  { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) },
      timeout:  300,
    };

    const req = http.request(options, (res) => {
      let raw = '';
      res.on('data', c => { raw += c; });
      res.on('end', () => {
        try { resolve(JSON.parse(raw)); }
        catch (e) { reject(e); }
      });
    });

    req.on('timeout', () => { req.destroy(); reject(new Error('HTTP timeout')); });
    req.on('error',   reject);
    req.write(body);
    req.end();
  });
}

// ── GET /api/risk/heatmap ─────────────────────────────────────────────────────
// Calculates spatio-temporal Gaussian risk grid purely in-process (no Python needed)
router.get('/heatmap', async (req, res) => {
  try {
    const city   = (req.query.city || 'bhopal').toLowerCase();
    const bounds = CITY_BOUNDS[city] || CITY_BOUNDS.bhopal;
    const incidents = await db.getIncidents();
    const zones  = [];
    const step   = 0.012;

    for (let lat = bounds.latMin; lat <= bounds.latMax; lat += step) {
      for (let lng = bounds.lngMin; lng <= bounds.lngMax; lng += step) {
        let riskScore = 0;
        let count     = 0;

        for (const inc of incidents) {
          if (inc.status === 'rejected') continue;
          const dLat  = (lat - inc.lat) * 111.0;
          const dLng  = (lng - inc.lng) * 111.0 * Math.cos(lat * Math.PI / 180.0);
          const distKm = Math.sqrt(dLat * dLat + dLng * dLng);

          if (distKm < 2.5) {
            count++;
            const spatialWeight  = Math.exp(-(distKm * distKm) / (2 * 0.6 * 0.6));
            const severityWeight = (inc.severity || 3) / 5.0;
            const typeWeight     = inc.type === 'HARASSMENT' ? 1.5 : inc.type === 'FIRE' ? 1.4 : inc.type === 'WATER_LOGGING' ? 1.3 : 1.0;
            riskScore += spatialWeight * severityWeight * typeWeight;
          }
        }

        const normalized = Math.min(1.0, riskScore / 2.0);
        if (normalized > 0.05 || count > 0) {
          zones.push({
            lat:           parseFloat(lat.toFixed(4)),
            lng:           parseFloat(lng.toFixed(4)),
            riskScore:     parseFloat(normalized.toFixed(3)),
            incidentCount: count,
            level:         normalized >= 0.75 ? 'CRITICAL' : normalized >= 0.45 ? 'HIGH' : normalized >= 0.20 ? 'MODERATE' : 'LOW',
          });
        }
      }
    }

    res.json({ city, zones, generatedAt: Date.now(), totalZones: zones.length });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── POST /api/risk/point ──────────────────────────────────────────────────────
// Predict specific coordinate risk via Scikit-Learn GradientBoosting
router.post('/point', async (req, res) => {
  const { lat, lng, hour, day } = req.body;
  const targetLat  = parseFloat(lat)  || 23.2599;
  const targetLng  = parseFloat(lng)  || 77.4126;
  const targetHour = parseInt(hour  != null ? hour  : new Date().getHours());
  const targetDay  = parseInt(day   != null ? day   : new Date().getDay());

  const payload = { lat: targetLat, lng: targetLng, hour: targetHour, day: targetDay };

  const fallbackResponse = (model) => ({
    lat:        targetLat,
    lng:        targetLng,
    risk_score: 45.0,
    risk_level: 'MODERATE',
    model,
  });

  // ── Tier 1: HTTP microservice ────────────────────────────────────────────
  try {
    const result = await httpRisk(payload);
    return res.json(result);
  } catch (httpErr) {
    logger.debug('[Risk] HTTP microservice unavailable, trying spawn', { reason: httpErr.message });
  }

  // ── Tier 2: Python spawn ─────────────────────────────────────────────────
  let responded = false;
  const py = spawn(PYTHON_BIN, [AI_SCRIPT, 'risk', JSON.stringify(payload)]);
  let output = '';

  const timer = setTimeout(() => {
    py.kill();
    if (!responded) { responded = true; res.json(fallbackResponse('fallback-timeout')); }
  }, 2000);

  py.stdout.on('data', chunk => { output += chunk.toString(); });

  py.on('close', code => {
    clearTimeout(timer);
    if (responded) return;
    responded = true;
    if (code === 0 && output.trim()) {
      try { return res.json(JSON.parse(output.trim())); } catch {}
    }
    res.json(fallbackResponse('fallback-native'));
  });

  py.on('error', () => {
    clearTimeout(timer);
    if (!responded) { responded = true; res.json(fallbackResponse('fallback-spawn-error')); }
  });
});

module.exports = router;
