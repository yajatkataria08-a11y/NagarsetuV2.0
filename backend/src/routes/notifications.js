const express = require('express');
const router = express.Router();
const db = require('../services/db');

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

module.exports = router;
