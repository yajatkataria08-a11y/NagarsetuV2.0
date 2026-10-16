const express = require('express');
const router = express.Router();
const db = require('../services/db');

// GET /api/cctv — list all CCTV streams with live AI threat detection status
router.get('/', async (req, res) => {
  try {
    const cameras = await db.getCctvCameras();
    res.json(cameras);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/cctv/:id/detect — push/simulate YOLOv8 visual detection on a camera stream
router.post('/:id/detect', async (req, res) => {
  try {
    const { threat, confidence, detections } = req.body;
    const cam = await db.updateCctvDetection(req.params.id, threat, confidence, detections);
    if (!cam) return res.status(404).json({ error: 'Camera not found' });

    // Broadcast CCTV detection event to all web clients
    req.app.get('io')?.emit('cctv:detection', {
      cameraId: cam.id,
      threat,
      confidence,
      detections,
      timestamp: Date.now()
    });

    res.json({ success: true, camera: cam });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
