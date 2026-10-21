const express = require('express');
const router = express.Router();
const db = require('../services/db');
const logger = require('../utils/logger');
const aiMicroservice = require('../services/aiMicroservice');

// GET /api/incidents — list incidents with filtering
router.get('/', async (req, res) => {
  try {
    const list = await db.getIncidents({
      status: req.query.status,
      ward: req.query.ward,
      severity: req.query.severity ? parseInt(req.query.severity) : undefined
    });
    res.json(list);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// GET /api/incidents/:id
router.get('/:id', async (req, res) => {
  try {
    const inc = await db.getIncidentById(req.params.id);
    if (!inc) return res.status(404).json({ error: 'Incident not found' });
    res.json(inc);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/incidents/triage — Real-time Multilingual SLA & Duplicate Triage (Phase 12)
router.post('/triage', async (req, res) => {
  try {
    const { description, lat, lng, photos } = req.body;
    if (!description) {
      return res.status(400).json({ error: 'Description is required for triage' });
    }

    const existing = await db.getIncidents({ status: 'pending' });
    const triageResult = await aiMicroservice.triageIncident({
      description,
      lat: lat ? parseFloat(lat) : undefined,
      lng: lng ? parseFloat(lng) : undefined,
      imageUrl: Array.isArray(photos) ? photos[0] : photos,
      existingIncidents: existing
    });

    res.json({
      status: 'success',
      ...triageResult
    });
  } catch (e) {
    logger.error('Incident triage failed', { error: e.message });
    res.status(500).json({ error: e.message });
  }
});

// POST /api/incidents — submit new incident (from Android RoadWatch, QR portal, etc.)
router.post('/', async (req, res) => {
  try {
    const { type, description, lat, lng, address, ward, severity, photos, source, reportedBy } = req.body;
    
    const incidentData = {
      type: type || 'OTHER',
      description: description || '',
      lat: lat ? parseFloat(lat) : 23.2599,
      lng: lng ? parseFloat(lng) : 77.4126,
      address: address || '',
      ward: ward || 'General',
      severity: severity ? parseInt(severity) : 3,
      status: 'pending',
      photos: photos || [],
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
    res.status(500).json({ error: e.message });
  }
});

// PATCH /api/incidents/:id — verify or resolve incident
router.patch('/:id', async (req, res) => {
  try {
    const patch = { ...req.body };
    if (patch.status === 'resolved') {
      patch.resolved_at = new Date().toISOString();
    }
    const updated = await db.updateIncident(req.params.id, patch);
    req.app.get('io')?.emit('incident:updated', updated);
    res.json(updated);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
