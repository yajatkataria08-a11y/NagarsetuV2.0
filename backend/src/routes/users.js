const express = require('express');
const router = express.Router();
const db = require('../services/db');
const { requireAuth } = require('../middleware/auth');
const { sendError } = require('../utils/errors');

// GET /api/users — list citizens with optional status filter (Restricted to officers)
router.get('/', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const list = await db.getUsers(req.query.status);
    res.json(list);
  } catch (e) {
    sendError(res, e, 500, 'Failed to fetch user list');
  }
});

// PATCH /api/users/:id/status — moderate user status (active, flagged, banned) (Super admin / ward officer only)
router.patch('/:id/status', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER']), async (req, res) => {
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
    sendError(res, e, 500, 'Failed to update user status');
  }
});

module.exports = router;
