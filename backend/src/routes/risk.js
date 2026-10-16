const express = require('express');
const router = express.Router();
const db = require('../services/db');

// GET /api/risk/heatmap — calculates spatio-temporal risk grid
router.get('/heatmap', async (req, res) => {
  try {
    const incidents = await db.getIncidents();
    const zones = [];
    
    // Bhopal bounding box grid computation
    const latMin = 23.20, latMax = 23.30, step = 0.01;
    const lngMin = 77.38, lngMax = 77.48;

    for (let lat = latMin; lat <= latMax; lat += step) {
      for (let lng = lngMin; lng <= lngMax; lng += step) {
        let riskScore = 0;
        let count = 0;

        for (const inc of incidents) {
          if (inc.status === 'rejected') continue;
          const dLat = (lat - inc.lat) * 111.0;
          const dLng = (lng - inc.lng) * 111.0 * Math.cos(lat * Math.PI / 180.0);
          const distKm = Math.sqrt(dLat * dLat + dLng * dLng);

          if (distKm < 2.0) {
            count++;
            const spatialWeight = Math.exp(-(distKm * distKm) / (2 * 0.5 * 0.5));
            const severityWeight = (inc.severity || 3) / 5.0;
            const typeWeight = inc.type === 'HARASSMENT' ? 1.5 : inc.type === 'FIRE' ? 1.4 : 1.0;
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
      zones,
      generatedAt: Date.now(),
      totalZones: zones.length
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
