/**
 * NagarSetu V2 — Triple-Path Eco & Safe Navigation Engine (Phase 12)
 * ─────────────────────────────────────────────────────────────────────
 * Combines Main Road Arterial Bias (lighting + risk penalty)
 * with Municipal BRTS Green Corridor (carbon + transit).
 *
 * calculate-routes resolution order:
 *   1. OSRM public API (real road geometry, 2 s timeout)
 *   2. Synthetic waypoint calculation (instant fallback)
 *
 * evaluate-safety resolution order:
 *   1. HTTP microservice AI_SERVICE_URL/route-safety (300 ms)
 *   2. Python spawn                                  (2500 ms)
 *   3. Static in-memory response
 */

const express    = require('express');
const router     = express.Router();
const http       = require('http');
const https      = require('https');
const { spawn }  = require('child_process');
const logger     = require('../utils/logger');
const config     = require('../config');

const { PYTHON_BIN, AI_SCRIPT, AI_SERVICE_URL } = config;

// ── OSRM helper ───────────────────────────────────────────────────────────────
/**
 * Call public OSRM demo API and extract route geometry as waypoints.
 * @returns {Promise<Array<{lat:number,lng:number}>>}
 */
function fetchOsrmWaypoints(sLat, sLng, eLat, eLng) {
  return new Promise((resolve, reject) => {
    const url = `https://router.project-osrm.org/route/v1/driving/${sLng},${sLat};${eLng},${eLat}?overview=full&geometries=geojson&steps=true`;

    const req = https.get(url, { timeout: 2000 }, (res) => {
      let raw = '';
      res.on('data', c => { raw += c; });
      res.on('end', () => {
        try {
          const json = JSON.parse(raw);
          if (json.code !== 'Ok' || !json.routes || !json.routes[0]) {
            return reject(new Error('OSRM: no valid route'));
          }
          // GeoJSON coordinates are [lng, lat]
          const coords = json.routes[0].geometry.coordinates;
          if (!Array.isArray(coords) || coords.length < 2) {
            return reject(new Error('OSRM: insufficient coordinate points'));
          }
          // Downsample to ≤20 points to keep payload lean
          const step = Math.max(1, Math.floor(coords.length / 20));
          const waypoints = [];
          for (let i = 0; i < coords.length; i += step) {
            waypoints.push({ lat: coords[i][1], lng: coords[i][0] });
          }
          // Always include last point
          const last = coords[coords.length - 1];
          if (waypoints[waypoints.length - 1].lat !== last[1]) {
            waypoints.push({ lat: last[1], lng: last[0] });
          }
          resolve(waypoints);
        } catch (e) { reject(e); }
      });
    });

    req.on('timeout', () => { req.destroy(); reject(new Error('OSRM timeout')); });
    req.on('error',   reject);
  });
}

// ── HTTP safety-eval helper ───────────────────────────────────────────────────
function httpRouteSafety(payload) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(payload);
    const url  = new URL(`${AI_SERVICE_URL}/route-safety`);

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

// ── POST /api/routing/calculate-routes ───────────────────────────────────────
router.post('/calculate-routes', async (req, res) => {
  try {
    const { startLat, startLng, endLat, endLng, mode = 'WALKING', timeHour = 22 } = req.body;
    if (!startLat || !startLng || !endLat || !endLng) {
      return res.status(400).json({ error: 'start and end coordinates required' });
    }

    const sLat = parseFloat(startLat);
    const sLng = parseFloat(startLng);
    const eLat = parseFloat(endLat);
    const eLng = parseFloat(endLng);

    const localRouter = require('../services/localRouter');
    const result = localRouter.calculateDeterministicRoutes(sLat, sLng, eLat, eLng, mode, timeHour);
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── POST /api/routing/evaluate-safety ────────────────────────────────────────
// Evaluate custom polyline with ML risk engine
router.post('/evaluate-safety', async (req, res) => {
  try {
    const { waypoints, hour = 22 } = req.body;
    if (!waypoints || !Array.isArray(waypoints)) {
      return res.status(400).json({ error: 'waypoints array required' });
    }

    const aiMicroservice = require('../services/aiMicroservice');
    const result = await aiMicroservice.evaluateRouteSafety(waypoints, hour);
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
