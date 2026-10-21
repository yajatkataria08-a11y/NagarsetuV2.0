const express = require('express');
const router = express.Router();
const db = require('../services/db');
const twilioService = require('../services/twilio');

// GET /api/notifications — list notifications
router.get('/', async (req, res) => {
  try {
    const list = await db.getNotifications(req.query.type);
    res.json(list);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/notifications/read-all — mark all notifications read
router.post('/read-all', async (req, res) => {
  try {
    const result = await db.markAllNotificationsRead();
    req.app.get('io')?.emit('notifications:read_all');
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/notifications/twilio/voice — TwiML Voice Webhook Callback
router.post('/twilio/voice', (req, res) => {
  const speechText = req.query.msg || req.body?.msg || 'NagarSetu Emergency alert triggered. Responders dispatched.';
  const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Aditi" language="hi-IN">${speechText}</Say>
  <Pause length="1"/>
  <Say voice="Polly.Aditi" language="hi-IN">NagarSetu Municipal Command Center. Status confirmed.</Say>
</Response>`;
  res.type('text/xml');
  res.send(twiml);
});

// POST /api/notifications/test-call — trigger test voice IVR call
router.post('/test-call', async (req, res) => {
  try {
    const { phone, message } = req.body;
    const result = await twilioService.triggerVoiceAlert(phone || '+91-9876543210', message || 'NagarSetu tactical voice test.');
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/notifications/test-whatsapp — trigger test WhatsApp message
router.post('/test-whatsapp', async (req, res) => {
  try {
    const { phone, message } = req.body;
    const result = await twilioService.sendWhatsAppAlert(phone || '+91-9876543210', message || '🚨 NagarSetu WhatsApp alert test.');
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
