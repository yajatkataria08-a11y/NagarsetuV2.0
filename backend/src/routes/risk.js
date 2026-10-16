const express = require('express');
const router = express.Router();
const db = require('../services/db');
const { spawn } = require('child_process');
const path = require('path');
const logger = require('../utils/logger');

const AI_SCRIPT = path.resolve(__dirname, '../../../ai_engine/unified_civic_ai.py');
const PYTHON_BIN = 'C:\\Users\\yajat\\AppData\\Local\\Programs\\Python\\Python311\\python.exe';

const CITY_BOUNDS = {
  bhopal: { latMin: 23.18, latMax: 23.32, lngMin: 77.36, lngMax: 77.48 },
  indore: { latMin: 22.65, latMax: 22.80, lngMin: 75.80, lngMax: 75.95 },
};

// GET /api/risk/heatmap?city=bhopal|indore — calculates spatio-temporal risk grid
router.get('/heatmap', async (req, res) => {
  try {
    const city = (req.query.city || 'bhopal').toLowerCase();
    const bounds = CITY_BOUNDS[city] || CITY_BOUNDS.bhopal;
    const incidents = await db.getIncidents();
    const zones = [];
    const step = 0.012;

    for (let lat = bounds.latMin; lat <= bounds.latMax; lat += step) {
      for (let lng = bounds.lngMin; lng <= bounds.lngMax; lng += step) {
        let riskScore = 0;
        let count = 0;

        for (const inc of incidents) {
          if (inc.status === 'rejected') continue;
          const dLat = (lat - inc.lat) * 111.0;
          const dLng = (lng - inc.lng) * 111.0 * Math.cos(lat * Math.PI / 180.0);
          const distKm = Math.sqrt(dLat * dLat + dLng * dLng);

          if (distKm < 2.5) {
            count++;
            const spatialWeight = Math.exp(-(distKm * distKm) / (2 * 0.6 * 0.6));
            const severityWeight = (inc.severity || 3) / 5.0;
            const typeWeight = inc.type === 'HARASSMENT' ? 1.5 : inc.type === 'FIRE' ? 1.4 : inc.type === 'WATER_LOGGING' ? 1.3 : 1.0;
            riskScore += spatialWeight * severityWeight * typeWeight;
          }
        }

        const normalized = Math.min(1.0, riskScore / 2.0);
        if (normalized > 0.05 || count > 0) {
          zones.push({
            lat: parseFloat(lat.toFixed(4)),
            lng: parseFloat(lng.toFixed(4)),
            riskScore: parseFloat(normalized.toFixed(3)),
            incidentCount: count,
            level: normalized >= 0.75 ? 'CRITICAL' : normalized >= 0.45 ? 'HIGH' : normalized >= 0.20 ? 'MODERATE' : 'LOW'
          });
        }
      }
    }

    res.json({
      city,
      zones,
      generatedAt: Date.now(),
      totalZones: zones.length
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/risk/point — predict specific coordinate risk via Scikit-Learn GradientBoosting
router.post('/point', async (req, res) => {
  const { lat, lng, hour, day } = req.body;
  const targetLat = parseFloat(lat) || 23.2599;
  const targetLng = parseFloat(lng) || 77.4126;
  const targetHour = parseInt(hour != null ? hour : new Date().getHours());
  const targetDay = parseInt(day != null ? day : new Date().getDay());

  const py = spawn(PYTHON_BIN, [
    AI_SCRIPT,
    'risk',
    JSON.stringify({ lat: targetLat, lng: targetLng, hour: targetHour, day: targetDay })
  ]);

  let output = '';
  const timer = setTimeout(() => {
    py.kill();
    // In-memory fallback
    res.json({
      lat: targetLat,
      lng: targetLng,
      risk_score: 45.0,
      risk_level: 'MODERATE',
      model: 'fallback-timeout'
    });
  }, 2000);

  py.stdout.on('data', chunk => { output += chunk.toString(); });
  py.on('close', code => {
    clearTimeout(timer);
    if (code === 0 && output.trim()) {
      try {
        const json = JSON.parse(output.trim());
        return res.json(json);
      } catch {}
    }
    res.json({
      lat: targetLat,
      lng: targetLng,
      risk_score: 45.0,
      risk_level: 'MODERATE',
      model: 'fallback-native'
    });
  });

  py.on('error', () => {
    clearTimeout(timer);
    res.json({
      lat: targetLat,
      lng: targetLng,
      risk_score: 45.0,
      risk_level: 'MODERATE',
      model: 'fallback-spawn-error'
    });
  });
});

module.exports = router;
