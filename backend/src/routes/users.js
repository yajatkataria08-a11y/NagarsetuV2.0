const express = require('express');
const router = express.Router();
const db = require('../services/db');

// GET /api/users — list citizens with optional status filter
router.get('/', async (req, res) => {
  try {
    const list = await db.getUsers(req.query.status);
    res.json(list);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// PATCH /api/users/:id/status — moderate user status (active, flagged, banned)
router.patch('/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    if (!['active', 'flagged', 'banned'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status. Must be active, flagged, or banned.' });
    }

    const updated = await db.updateUserStatus(req.params.id, status);
    if (!updated) return res.status(404).json({ error: 'User not found' });

    req.app.get('io')?.emit('user:updated', updated);
    res.json({ success: true, user: updated });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
