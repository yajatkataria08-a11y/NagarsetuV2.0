const express = require('express');
const router = express.Router();
const db = require('../services/db');
const { requireAuth } = require('../middleware/auth');

// GET /api/cctv — list all CCTV streams with live AI threat detection status
router.get('/', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
  try {
    const cameras = await db.getCctvCameras();
    res.json(cameras);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/cctv/:id/detect — push/simulate YOLOv8 visual detection on a camera stream
router.post('/:id/detect', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER']), async (req, res) => {
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

// POST /api/cctv/:id/analyze-frame — run computer vision on uploaded camera frame / video stream
router.post('/:id/analyze-frame', async (req, res) => {
  try {
    const path = require('path');
    const fs = require('fs');
    const { image, photo, type, description } = req.body;
    const aiMicroservice = require('../services/aiMicroservice');

    const cameras = await db.getCctvCameras();
    const cam = cameras.find(c => c.id === req.params.id);

    let videoPath = null;
    if (cam && cam.feed_url) {
      if (cam.feed_url.startsWith('/streams/')) {
        const localFile = path.resolve(__dirname, '../../public', '.' + cam.feed_url);
        if (fs.existsSync(localFile)) {
          videoPath = localFile;
        }
      } else if (cam.feed_url.startsWith('http')) {
        videoPath = cam.feed_url;
      }
    }

    const [result, infraResult] = await Promise.all([
      aiMicroservice.analyzeFrame({
        image: image || photo,
        video_path: videoPath,
        type: type || (cam ? cam.active_threat : 'ROAD_HAZARD'),
        description: description || (cam ? cam.name : '')
      }),
      aiMicroservice.analyzeInfraFrame({
        image: image || photo,
        conf_threshold: 0.25
      })
    ]);

    // If infra defects detected, combine them with primary threat
    if (infraResult && infraResult.detections && infraResult.detections.length > 0) {
      result.detections = (result.detections || []).concat(infraResult.detections);
      if (!result.primary_threat || result.primary_threat === 'none') {
        result.primary_threat = infraResult.primary_defect;
        result.confidence = infraResult.detections[0]?.confidence || 0.85;
        result.priority = infraResult.highest_priority || 'HIGH';
        result.department = infraResult.departments?.[0] || 'PWD_BRIDGES';
      }
    }

    if (result && result.primary_threat && result.primary_threat !== 'none') {
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
        mode: result.detection_mode || 'LIVE_YOLO_DETECTION',
        timestamp: Date.now()
      });
    } else if (result && result.primary_threat === 'none') {
      await db.updateCctvDetection(
        req.params.id,
        null,
        0,
        []
      );

      req.app.get('io')?.emit('cctv:detection', {
        cameraId: req.params.id,
        threat: null,
        confidence: 0,
        detections: [],
        mode: result.detection_mode || 'LIVE_YOLO_NOMINAL',
        timestamp: Date.now()
      });
    }

    res.json({
      success: true,
      camera: cam ? cam.id : req.params.id,
      analysis: result,
      infra_analysis: infraResult
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/cctv/:id/analyze-infra — run SHM crack/structural defect inspection
router.post('/:id/analyze-infra', async (req, res) => {
  try {
    const { image, photo, conf_threshold } = req.body;
    const aiMicroservice = require('../services/aiMicroservice');
    const infraResult = await aiMicroservice.analyzeInfraFrame({
      image: image || photo,
      conf_threshold: conf_threshold || 0.25
    });

    let segResult = null;
    if (infraResult.primary_defect === 'crack' || (infraResult.detections && infraResult.detections.some(d => d.label === 'crack'))) {
      segResult = await aiMicroservice.segmentCracks({
        image: image || photo,
        conf_threshold: conf_threshold || 0.25
      });
    }

    res.json({
      success: true,
      cameraId: req.params.id,
      defects: infraResult,
      segmentation: segResult
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
