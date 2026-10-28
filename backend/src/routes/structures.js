/**
 * NagarSetu V2.0 — Infrastructure Health API  (mounted at /api/structures)
 * ─────────────────────────────────────────────────────────────────────────
 *   GET  /                          list structures + latest health
 *   POST /                          register a structure (SUPER_ADMIN)
 *   GET  /:id                       one structure + latest inspection summary
 *   POST /:id/inspect               image -> AI service -> save defects -> update health
 *   POST /:id/telemetry             sensor window -> anomaly -> update health
 *   GET  /:id/history               health history, inspections, recent readings
 *   GET  /:id/inspections/:iid/image   annotated image (authenticated)
 *
 * Integrity rules:
 *   - No fallback numbers. If the AI service is unreachable the route answers 503;
 *     a structural health score is never fabricated.
 *   - Health combines the latest defect, anomaly and RUL risks; components that
 *     were never measured are reported as null and excluded (not scored as healthy).
 *   - Score < 40 reuses the hazard-zone escalation (one active zone per structure).
 */

const express = require('express');
const crypto  = require('crypto');
const fs      = require('fs');
const path    = require('path');
const { z }   = require('zod');

const db      = require('../services/db');
const ai      = require('../services/aiMicroservice');
const logger  = require('../utils/logger');
const config  = require('../config');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

const READ_ROLES   = ['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER', 'STRUCTURAL_INSPECTOR'];
const INSPECT_ROLES = ['SUPER_ADMIN', 'FIELD_ENGINEER', 'STRUCTURAL_INSPECTOR'];
const SENSOR_API_KEY = process.env.SENSOR_API_KEY || config.SENSOR_API_KEY;

const DATA_ROOT = process.env.DATA_FILE ? path.dirname(path.resolve(process.env.DATA_FILE)) : config.DATA_DIR;
const IMAGE_DIR = path.join(DATA_ROOT, 'inspections');

const MAX_IMAGE_B64 = 8 * 1024 * 1024; // ~6 MB decoded
const ID_RE = /^[A-Za-z0-9_-]{1,64}$/;

// ── Validation ────────────────────────────────────────────────────────────────
const RulSchema = z.object({
  crack_length_mm:    z.number().positive().max(100000).optional(),
  critical_length_mm: z.number().positive().max(100000).optional(),
  delta_sigma_mpa:    z.number().positive().max(1000).optional(),
  cycles_per_day:     z.number().positive().max(1e8).optional(),
}).strict();

const InspectSchema = z.object({
  image:        z.string().min(100).max(MAX_IMAGE_B64),
  reference_mm: z.number().positive().max(100000).optional(),
  reference_px: z.number().positive().max(100000).optional(),
  rul:          RulSchema.optional(),
}).strict().refine(
  v => (v.reference_mm === undefined) === (v.reference_px === undefined),
  { message: 'reference_mm and reference_px must be given together' }
);

const StructureSchema = z.object({
  name: z.string().trim().min(2).max(120),
  type: z.enum(['road_bridge', 'flyover', 'footbridge', 'rail_overbridge', 'culvert', 'viaduct', 'causeway', 'arch_bridge', 'other']),
  lat:  z.number().min(-90).max(90),
  lng:  z.number().min(-180).max(180),
  ward: z.string().trim().max(80).optional(),
}).strict();

const TelemetrySchema = z.object({
  samples: z.array(z.number()).min(16).max(50000),
  fs:      z.number().positive().max(20000).optional(),
}).strict();

function parseBody(schema, req, res) {
  const r = schema.safeParse(req.body);
  if (!r.success) {
    res.status(400).json({
      error: 'Invalid request body',
      issues: r.error.issues.map(i => ({ path: i.path.join('.'), message: i.message })),
    });
    return null;
  }
  return r.data;
}

/** base64 (optionally data-URI) must decode to a JPEG or PNG. */
function looksLikeImage(b64) {
  const raw = b64.includes('base64,') ? b64.split('base64,')[1] : b64;
  const head = Buffer.from(raw.slice(0, 16), 'base64');
  const jpeg = head[0] === 0xff && head[1] === 0xd8;
  const png  = head[0] === 0x89 && head[1] === 0x50 && head[2] === 0x4e && head[3] === 0x47;
  return jpeg || png;
}

// ── Auth: sensor key OR role-based JWT (telemetry only) ───────────────────────
function safeEqual(a, b) {
  const x = Buffer.from(String(a)), y = Buffer.from(String(b));
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}

function requireSensorOrRole(roles) {
  const jwtGate = requireAuth(roles);
  return (req, res, next) => {
    const key = req.headers['x-sensor-api-key'];
    if (key) {
      if (SENSOR_API_KEY && safeEqual(key, SENSOR_API_KEY)) { req.sensor = true; return next(); }
      return res.status(401).json({ error: 'Unauthorized: invalid x-sensor-api-key' });
    }
    return jwtGate(req, res, next);
  };
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function aiFailure(res, e) {
  const m = /HTTP (4\d\d)/.exec(e.message || '');
  if (m) {
    logger.warn('[Structures] AI service rejected request', { error: e.message });
    return res.status(422).json({ error: 'AI service could not process the input', detail: e.message.slice(0, 300) });
  }
  logger.error('[Structures] AI service unavailable', { error: e.message });
  return res.status(503).json({ error: 'AI service unavailable - no health score was computed or stored' });
}

function publicView(st) {
  const { health_history, ...rest } = st;
  return rest;
}

/** Merge new component risks into the stored ones, then ask the AI service to score them. */
async function recomputeHealth(st, patch) {
  const prev = st.health || {};
  const comps = { defect: null, anomaly: null, rul: null, ...(prev.components || {}), ...patch };
  const stamps = { ...(prev.components_updated_at || {}) };
  const now = new Date().toISOString();
  for (const k of Object.keys(patch)) stamps[k] = patch[k] === null ? null : now;
  const scored = await ai.infra.score(comps);
  return { ...scored, components_updated_at: stamps };
}

async function publishHealth(req, st, health, source, aiWarnings = []) {
  const previousBand = st.health?.band || 'unknown';
  const updated = await db.setStructureHealth(st.id, health, source);
  const payload = {
    id: updated.id, name: updated.name, lat: updated.lat, lng: updated.lng,
    score: health.score, band: health.band, previous_band: previousBand,
    components: health.components, updated_at: updated.health.updated_at, source,
  };
  req.app.get('io')?.emit('structure:health_update', payload);

  let escalation = null;
  if (typeof health.score === 'number' && health.score < 40) {
    const active = await db.getActiveHazardZones();
    if (!active.some(h => h.structure_id === st.id)) {
      const zone = await db.createHazardZone({
        name: `Structural alert: ${st.name}`,
        center_lat: st.lat, center_lng: st.lng,
        radius_meters: 150, risk_boost: 2.0,
        expires_at: new Date(Date.now() + 120 * 60000).toISOString(),
        note: `Auto-created: health score ${health.score} (<40). Unverified - requires engineer confirmation.` +
              (aiWarnings.length ? ` Model caveats: ${aiWarnings.join(' | ')}` : ''),
        active: true, structure_id: st.id, requires_verification: true,
      });
      req.app.get('io')?.emit('hazard:auto_created', zone);
      logger.warn('[Structures] Escalated critical structure to hazard zone', { structure: st.id, score: health.score });
      escalation = { hazard_id: zone.id };
    } else {
      escalation = { hazard_id: active.find(h => h.structure_id === st.id).id, existing: true };
    }
  }
  return { payload, escalation };
}

// ── GET / ─────────────────────────────────────────────────────────────────────
router.get('/', requireAuth(READ_ROLES), async (req, res) => {
  const list = await db.getStructures();
  res.json(list.map(s => ({
    id: s.id, name: s.name, type: s.type, ward: s.ward, lat: s.lat, lng: s.lng,
    health: { score: s.health.score, band: s.health.band, updated_at: s.health.updated_at },
  })));
});

// ── POST / ────────────────────────────────────────────────────────────────────
router.post('/', requireAuth(['SUPER_ADMIN']), async (req, res) => {
  const body = parseBody(StructureSchema, req, res);
  if (!body) return;
  const created = await db.createStructure({ ...body, ward: body.ward || 'Unassigned' });
  res.status(201).json(publicView(created));
});

// ── GET /:id ──────────────────────────────────────────────────────────────────
router.get('/:id', requireAuth(READ_ROLES), async (req, res) => {
  if (!ID_RE.test(req.params.id)) return res.status(400).json({ error: 'Invalid structure id' });
  const st = await db.getStructureById(req.params.id);
  if (!st) return res.status(404).json({ error: 'Structure not found' });
  const [latest] = await db.getInspections(st.id, 1);
  res.json({ ...publicView(st), latest_inspection: latest ? summarizeInspection(st.id, latest) : null });
});

function summarizeInspection(structureId, i) {
  return {
    id: i.id, created_at: i.created_at, inspector: i.inspector,
    detector: i.defects?.detector, mask_source: i.defects?.mask_source,
    crack: i.defects?.crack, severity: i.defects?.severity, detections: i.defects?.detections || [],
    rul: i.rul || null, notes: i.defects?.notes || [],
    image_url: i.image_file ? `/api/structures/${structureId}/inspections/${i.id}/image` : null,
  };
}

// ── POST /:id/inspect ─────────────────────────────────────────────────────────
router.post('/:id/inspect', requireAuth(INSPECT_ROLES), async (req, res) => {
  try {
    if (!ID_RE.test(req.params.id)) return res.status(400).json({ error: 'Invalid structure id' });
    const st = await db.getStructureById(req.params.id);
    if (!st) return res.status(404).json({ error: 'Structure not found' });
    const body = parseBody(InspectSchema, req, res);
    if (!body) return;
    if (!looksLikeImage(body.image)) return res.status(415).json({ error: 'image must be a JPEG or PNG (base64)' });

    let result;
    try {
      result = await ai.infra.health(body);
    } catch (e) { return aiFailure(res, e); }

    // Persist annotated image on disk (not in store.json); id is server-generated.
    const inspId = 'insp_' + crypto.randomBytes(6).toString('hex');
    let imageFile = null;
    const annotated = result.defects?.annotated_image;
    if (annotated) {
      fs.mkdirSync(IMAGE_DIR, { recursive: true });
      imageFile = `${inspId}.jpg`;
      fs.writeFileSync(path.join(IMAGE_DIR, imageFile), Buffer.from(annotated.split('base64,')[1], 'base64'));
    }
    const { annotated_image, ...defects } = result.defects || {};

    const health = await recomputeHealth(st, {
      defect: result.health.components.defect ?? null,
      rul:    result.health.components.rul ?? null,   // new crack measurement invalidates the old RUL
    });
    const inspection = await db.addInspection({
      id: inspId, structure_id: st.id, inspector: req.user?.userId || 'unknown',
      defects, rul: result.rul || null, image_file: imageFile,
      health_after: { score: health.score, band: health.band },
    });
    const warnings = [...(defects.notes || [])];
    const { payload, escalation } = await publishHealth(req, st, health, 'inspection', warnings);

    res.status(201).json({
      inspection: summarizeInspection(st.id, inspection),
      annotated_image: annotated || null,
      health: payload, health_detail: health, escalation,
    });
  } catch (e) {
    logger.error('[Structures] inspect failed', { error: e.message });
    res.status(500).json({ error: 'Failed to process inspection' });
  }
});

// ── POST /:id/telemetry ───────────────────────────────────────────────────────
router.post('/:id/telemetry', requireSensorOrRole(INSPECT_ROLES), async (req, res) => {
  try {
    if (!ID_RE.test(req.params.id)) return res.status(400).json({ error: 'Invalid structure id' });
    const st = await db.getStructureById(req.params.id);
    if (!st) return res.status(404).json({ error: 'Structure not found' });
    const body = parseBody(TelemetrySchema, req, res);
    if (!body) return;

    let result;
    try {
      result = await ai.infra.health({ samples: body.samples, fs: body.fs });
    } catch (e) { return aiFailure(res, e); }

    const a = result.anomaly;
    const health = await recomputeHealth(st, { anomaly: result.health.components.anomaly ?? null });
    const reading = await db.addReading({
      structure_id: st.id, source: req.sensor ? 'sensor_key' : (req.user?.userId || 'user'),
      fs: body.fs || null, n_samples: body.samples.length,
      score: a.score, threshold: a.threshold, ratio: a.ratio, flag: a.flag,
      features: a.features, data_source: a.data_source,
    });
    const warnings = a.warning ? [a.warning] : [];
    const { payload, escalation } = await publishHealth(req, st, health, 'telemetry', warnings);

    res.status(201).json({ reading, anomaly: a, health: payload, health_detail: health, escalation, warnings });
  } catch (e) {
    logger.error('[Structures] telemetry failed', { error: e.message });
    res.status(500).json({ error: 'Failed to process telemetry' });
  }
});

// ── GET /:id/history ──────────────────────────────────────────────────────────
router.get('/:id/history', requireAuth(READ_ROLES), async (req, res) => {
  if (!ID_RE.test(req.params.id)) return res.status(400).json({ error: 'Invalid structure id' });
  const st = await db.getStructureById(req.params.id);
  if (!st) return res.status(404).json({ error: 'Structure not found' });
  const [inspections, readings] = await Promise.all([db.getInspections(st.id, 50), db.getReadings(st.id, 200)]);
  res.json({
    structure_id: st.id,
    health_history: st.health_history,
    inspections: inspections.map(i => summarizeInspection(st.id, i)),
    readings,
  });
});

// ── GET /:id/inspections/:iid/image ───────────────────────────────────────────
router.get('/:id/inspections/:iid/image', requireAuth(READ_ROLES), async (req, res) => {
  const { id, iid } = req.params;
  if (!ID_RE.test(id) || !ID_RE.test(iid)) return res.status(400).json({ error: 'Invalid id' });
  const insp = (await db.getInspections(id, 1000)).find(i => i.id === iid);
  if (!insp || !insp.image_file) return res.status(404).json({ error: 'Image not found' });
  const file = path.join(IMAGE_DIR, path.basename(insp.image_file));
  if (!fs.existsSync(file)) return res.status(404).json({ error: 'Image not found' });
  res.type('image/jpeg').sendFile(file);
});

module.exports = router;
