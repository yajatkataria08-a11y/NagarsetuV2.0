const express = require('express');
const router = express.Router();
const db = require('../services/db');
const { requireAuth } = require('../middleware/auth');

// GET /api/wards — list municipal wards with budget and SLA metrics
router.get('/', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const wards = await db.getWards();
    res.json(wards);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// GET /api/wards/officers — list field officers and performance metrics
router.get('/officers', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const list = await db.getResponders(req.query.status);
    res.json(list);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
