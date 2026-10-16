const express = require('express');
const router = express.Router();
const db = require('../services/db');

// GET /api/analytics/wards — get ward performance data
router.get('/wards', async (req, res) => {
  try {
    const wards = await db.getWards();
    res.json(wards);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// GET /api/analytics/summary — overall municipal summary
router.get('/summary', async (req, res) => {
  try {
    const stats = await db.getDashboardStats();
    res.json(stats);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
