const express  = require('express');
const router   = express.Router();
const db       = require('../services/db');
const twilio   = require('../services/twilio');
const logger   = require('../utils/logger');
const { v4: uuid } = require('uuid');
const { requireAuth } = require('../middleware/auth');
const { sendError } = require('../utils/errors');

function isValidGeoCoord(lat, lng) {
  const pLat = parseFloat(lat);
  const pLng = parseFloat(lng);
  return !isNaN(pLat) && !isNaN(pLng) && pLat >= -90 && pLat <= 90 && pLng >= -180 && pLng <= 180;
}

// GET /api/sos — list active SOS events (restricted to dispatch staff)
router.get('/', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const events = await db.getSosEvents(req.query.status);
    // Strip PII fields from list response
    const safe = events.map(({ tracking_token, phone, ...rest }) => rest);
    res.json(safe);
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch SOS events');
  }
});

// POST /api/sos — trigger new SOS (from Android app or Wear OS)
router.post('/', async (req, res) => {
  try {
    const { userId, userName, phone, ward, lat, lng, batteryLevel, heartRate, contacts, sirenType } = req.body;
    
    if (!isValidGeoCoord(lat, lng)) {
      return res.status(400).json({ error: 'Valid numerical lat (-90 to 90) and lng (-180 to 180) are required' });
    }

    const trackingToken = uuid().replace(/-/g, '').slice(0, 12).toUpperCase();
    const trackingUrl   = `${process.env.FRONTEND_URL || 'http://localhost:3000'}/track/${trackingToken}`;

    // Persist to Supabase / store
    const sos = await db.addSosEvent({
      citizen_id:      userId || 'anonymous_citizen',
      user_name:       userName || 'Citizen in Distress',
      phone:           phone || '',
      ward:            ward || 'Unknown Ward',
      lat:             parseFloat(lat),
      lng:             parseFloat(lng),
      battery_level:   batteryLevel ? parseInt(batteryLevel) : 0,
      heart_rate:      heartRate ? parseInt(heartRate) : null,
      status:          'active',
      siren_type:      sirenType || 'HIGH',
      tracking_token:  trackingToken,
      contacts:        Array.isArray(contacts) ? contacts : [],
    });

    // Create live tracking session
    await db.createTrackingSession({
      session_id:  trackingToken,
      citizen_id:  userId || 'anonymous_citizen',
      user_name:   userName || 'Citizen in Distress',
      lat:         parseFloat(lat),
      lng:         parseFloat(lng),
      expires_at:  new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(), // 2hr TTL
    });

    // Broadcast to authenticated command center dashboards via Socket.IO
    req.app.get('io')?.emit('new_sos', sos);

    // Multi-tier emergency escalation: SMS + WhatsApp + 60s Voice IVR fallback
    if (contacts?.length) {
      twilio.dispatchEmergencyEscalation(sos, req.app.get('io'));
    }

    logger.info('SOS triggered', { id: sos.id, user: userName, ward, trackingUrl });
    res.status(201).json({ ...sos, trackingUrl });
  } catch (e) {
    logger.error('SOS create failed', { error: e.message });
    sendError(res, e, 500, 'Failed to trigger emergency SOS');
  }
});

// POST /api/sos/broadcast-sms — Server-side SMS dispatch for client applications
router.post('/broadcast-sms', async (req, res) => {
  try {
    const { phone, message } = req.body;
    if (!phone || !message) {
      return res.status(400).json({ error: 'phone and message are required' });
    }

    const result = await twilio.sendSms(phone, message);
    res.json({ success: true, result });
  } catch (e) {
    sendError(res, e, 500, 'Server-side emergency SMS dispatch failed');
  }
});

// PATCH /api/sos/:id — update SOS status (Requires administrative dispatch role)
router.patch('/:id', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const patch = {};
    if (req.body.status === 'acknowledged') patch.acknowledged_at = new Date().toISOString();
    if (req.body.status === 'resolved')     patch.resolved_at     = new Date().toISOString();
    patch.status = req.body.status;

    const updated = await db.updateSos(req.params.id, patch);
    if (!updated) return res.status(404).json({ error: 'SOS event not found' });

    req.app.get('io')?.emit('sos:updated', updated);
    res.json(updated);
  } catch (e) {
    sendError(res, e, 500, 'Failed to update SOS status');
  }
});

// POST /api/sos/:id/dispatch — dispatch nearest responder via SMS (Requires dispatch role)
router.post('/:id/dispatch', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
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
    sendError(res, e, 500, 'Failed to dispatch responder unit');
  }
});

module.exports = router;
