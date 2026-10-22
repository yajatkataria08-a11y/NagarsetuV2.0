const express = require('express');
const router = express.Router();
const db = require('../services/db');
const logger = require('../utils/logger');
const { requireAuth } = require('../middleware/auth');
const { sendError } = require('../utils/errors');

function isValidGeoCoord(lat, lng) {
  const pLat = parseFloat(lat);
  const pLng = parseFloat(lng);
  return !isNaN(pLat) && !isNaN(pLng) && pLat >= -90 && pLat <= 90 && pLng >= -180 && pLng <= 180;
}

// GET /api/incidents — list incidents with optional filters
router.get('/', async (req, res) => {
  try {
    const { status, ward, severity, limit } = req.query;
    const incidents = await db.getIncidents({ status, ward, severity, limit });
    res.json(incidents);
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch incidents');
  }
});

// GET /api/incidents/:id — single incident
router.get('/:id', async (req, res) => {
  try {
    const inc = await db.getIncident(req.params.id);
    if (!inc) return res.status(404).json({ error: 'Incident not found' });
    res.json(inc);
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch incident');
  }
});

// POST /api/incidents/triage — AI-powered incident classification
router.post('/triage', async (req, res) => {
  try {
    const { description, lat, lng, image_base64 } = req.body;
    if (!description && !image_base64) {
      return res.status(400).json({ error: 'Either description or image_base64 must be provided' });
    }

    const aiMicroservice = require('../services/aiMicroservice');
    const triageResult = await aiMicroservice.triageIncident({
      description: description || '',
      lat: lat ? parseFloat(lat) : null,
      lng: lng ? parseFloat(lng) : null,
      image_base64: image_base64 || null
    });

    res.json({
      status: 'success',
      ...triageResult
    });
  } catch (e) {
    logger.error('Incident triage failed', { error: e.message });
    sendError(res, e, 500, 'Failed to perform AI incident triage');
  }
});

// POST /api/incidents — submit new incident
router.post('/', async (req, res) => {
  try {
    const { type, description, lat, lng, address, ward, severity, photos, source, reportedBy } = req.body;
    
    if (lat !== undefined && lng !== undefined) {
      if (!isValidGeoCoord(lat, lng)) {
        return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
      }
    }

    const incidentData = {
      type: type || 'OTHER',
      description: description || '',
      lat: lat !== undefined ? parseFloat(lat) : 0,
      lng: lng !== undefined ? parseFloat(lng) : 0,
      address: address || '',
      ward: ward ? String(ward).trim() : 'General',
      severity: severity ? parseInt(severity) : 3,
      status: 'pending',
      photos: Array.isArray(photos) ? photos : [],
      reported_by: reportedBy || 'anonymous',
      source: source || 'web-portal',
      reported_at: new Date().toISOString()
    };

    const newInc = await db.addIncident(incidentData);
    
    // Asynchronously run AI verification
    const aiVerifier = require('../services/aiVerifier');
    aiVerifier.verifyIncident(newInc).then(async (aiRes) => {
      let updatedStatus = newInc.status;
      if (aiRes.recommended_action === 'auto_verify') {
        updatedStatus = 'verified';
      } else if (aiRes.recommended_action === 'auto_reject') {
        updatedStatus = 'rejected';
      } else {
        updatedStatus = 'ai_verified';
      }

      const updated = await db.updateIncident(newInc.id, {
        ai_score: aiRes.score,
        ai_risk_level: aiRes.risk_level,
        status: updatedStatus,
        ai_feedback: aiRes.feedback || null
      });

      req.app.get('io')?.emit('incident:updated', updated);
    }).catch(err => {
      logger.warn('AI background triage error', { error: err.message });
    });

    // Broadcast via WebSockets
    req.app.get('io')?.emit('new_incident', newInc);
    logger.info('Incident created', { id: newInc.id, type: newInc.type, ward: newInc.ward });

    res.status(201).json(newInc);
  } catch (e) {
    logger.error('Incident creation failed', { error: e.message });
    sendError(res, e, 500, 'Failed to create civic incident');
  }
});

// PATCH /api/incidents/:id — verify or resolve incident (Requires municipal role)
router.patch('/:id', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER']), async (req, res) => {
  try {
    const patch = { ...req.body };
    if (patch.status === 'resolved') {
      patch.resolved_at = new Date().toISOString();
    }
    const updated = await db.updateIncident(req.params.id, patch);
    if (!updated) return res.status(404).json({ error: 'Incident not found' });
    req.app.get('io')?.emit('incident:updated', updated);
    res.json(updated);
  } catch (e) {
    sendError(res, e, 500, 'Failed to update incident');
  }
});

module.exports = router;
