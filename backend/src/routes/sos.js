const express  = require('express');
const router   = express.Router();
const db       = require('../services/db');
const twilio   = require('../services/twilio');
const logger   = require('../utils/logger');
const { v4: uuid } = require('uuid');

// GET /api/sos — list active SOS events
router.get('/', async (req, res) => {
  try {
    const events = await db.getSosEvents(req.query.status);
    res.json(events);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/sos — trigger new SOS (from Android app or Wear OS)
router.post('/', async (req, res) => {
  try {
    const { userId, userName, phone, ward, lat, lng, batteryLevel, heartRate, contacts, sirenType } = req.body;
    if (!lat || !lng) return res.status(400).json({ error: 'lat/lng required' });

    const trackingToken = uuid().replace(/-/g, '').slice(0, 12).toUpperCase();
    const trackingUrl   = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/track/${trackingToken}`;

    // Persist to Supabase
    const sos = await db.addSosEvent({
      citizen_id:      userId,
      user_name:       userName || 'Unknown',
      phone:           phone || '',
      ward:            ward || 'Unknown',
      lat:             parseFloat(lat),
      lng:             parseFloat(lng),
      battery_level:   batteryLevel || 0,
      heart_rate:      heartRate || null,
      status:          'active',
      siren_type:      sirenType || 'HIGH',
      tracking_token:  trackingToken,
      contacts:        contacts || [],
    });

    // Also create tracking session (Raksha-inspired live track link)
    await db.createTrackingSession({
      session_id:  trackingToken,
      citizen_id:  userId,
      user_name:   userName || 'Unknown',
      lat:         parseFloat(lat),
      lng:         parseFloat(lng),
      expires_at:  new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(), // 2hr TTL
    });

    // Broadcast to all admin dashboards via Socket.IO
    req.app.get('io')?.emit('new_sos', sos);

    // Multi-tier emergency escalation: SMS + WhatsApp + 60s Voice IVR fallback
    if (contacts?.length) {
      twilio.dispatchEmergencyEscalation(sos, req.app.get('io'));
    }

    logger.info('SOS triggered', { id: sos.id, user: userName, ward, trackingUrl });
    res.status(201).json({ ...sos, trackingUrl });
  } catch (e) {
    logger.error('SOS create failed', { error: e.message });
    res.status(500).json({ error: e.message });
  }
});

// PATCH /api/sos/:id — update SOS status
router.patch('/:id', async (req, res) => {
  try {
    const patch = {};
    if (req.body.status === 'acknowledged') patch.acknowledged_at = new Date().toISOString();
    if (req.body.status === 'resolved')     patch.resolved_at     = new Date().toISOString();
    patch.status = req.body.status;

    const updated = await db.updateSos(req.params.id, patch);
    req.app.get('io')?.emit('sos:updated', updated);
    res.json(updated);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/sos/:id/dispatch — dispatch nearest responder via SMS
router.post('/:id/dispatch', async (req, res) => {
  try {
    const sos = await db.getSosEvents();
    const event = sos.find(s => s.id === req.params.id);
    if (!event) return res.status(404).json({ error: 'SOS not found' });

    // Find nearest on-duty responders via PostGIS
    const responders = await db.getRespondersNear(event.lat, event.lng, 8000);
    if (!responders.length) return res.status(404).json({ error: 'No available responders nearby' });

    const nearest = responders[0];
    const trackingUrl = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/track/${event.tracking_token}`;
    const msg = `🚨 DISPATCH: SOS at ${event.ward}\n📍 Live location: ${trackingUrl}\nVictim: ${event.user_name} | Battery: ${event.battery_level}%\nRespond immediately — NagarSetu Command`;

    await twilio.sendSms(nearest.phone, msg);
    const updated = await db.updateSos(req.params.id, { status: 'dispatched', assigned_officer: nearest.id });
    req.app.get('io')?.emit('sos:updated', updated);

    logger.info('Responder dispatched', { sosId: req.params.id, responder: nearest.name });
    res.json({ dispatched: true, responder: nearest, updated });
  } catch (e) {
    logger.error('Dispatch failed', { error: e.message });
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
