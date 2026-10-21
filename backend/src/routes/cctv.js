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

// POST /api/cctv/:id/analyze-frame — run computer vision on uploaded camera frame / image
router.post('/:id/analyze-frame', async (req, res) => {
  try {
    const { image, photo, type, description } = req.body;
    const config = require('../config');
    const { spawn } = require('child_process');

    const payload = JSON.stringify({
      image: image || photo,
      type: type || 'ROAD_HAZARD',
      description: description || ''
    });

    const py = spawn(config.PYTHON_BIN, [config.AI_SCRIPT, 'vision', payload]);
    let output = '';

    py.stdout.on('data', chunk => { output += chunk.toString(); });
    py.on('close', async (code) => {
      let result = { verified: false, primary_threat: 'none', detections: [] };
      if (code === 0 && output.trim()) {
        try { result = JSON.parse(output.trim()); } catch (_) {}
      }

      if (result.primary_threat && result.primary_threat !== 'none') {
        await db.updateCctvDetection(
          req.params.id,
          result.primary_threat,
          result.confidence || 0.85,
          result.detections || []
        );

        req.app.get('io')?.emit('cctv:detection', {
          cameraId: req.params.id,
          threat: result.primary_threat,
          confidence: result.confidence,
          detections: result.detections,
          timestamp: Date.now()
        });
      }

      res.json({ success: true, analysis: result });
    });

    py.on('error', () => {
      res.json({ success: false, error: 'Vision inference failed' });
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
