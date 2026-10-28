/**
 * Infrastructure-health API tests (/api/structures).
 *
 * Needs a running backend started with AI_SERVICE_URL pointing at the stub this
 * file starts (default http://127.0.0.1:8099), e.g.
 *     AI_SERVICE_URL=http://127.0.0.1:8099 JWT_SECRET=ci node src/index.js
 * The stub keeps CI independent of Python; the real service is covered by
 * ai_engine/tests and by the docker-compose smoke check.
 */
const http = require('http');
const assert = require('assert').strict;
const { io } = require('socket.io-client');

const API_BASE = process.env.TEST_API_BASE || 'http://localhost:4000';
const STUB_PORT = parseInt(process.env.TEST_AI_STUB_PORT || '8099', 10);
const SENSOR_KEY = process.env.SENSOR_API_KEY || 'nagarsetu_iot_sensor_key_2026';

// 1x1 PNG
const PNG_B64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='.repeat(1);
const BIG_B64 = PNG_B64 + 'A'.repeat(200); // >100 chars; magic bytes still PNG

// ── Stub AI service ───────────────────────────────────────────────────────────
const W = { defect: 0.4, anomaly: 0.35, rul: 0.25 };
function stubScore(c) {
  const used = Object.entries(c).filter(([, v]) => typeof v === 'number');
  if (!used.length) return { score: null, band: 'unknown', components: c, components_used: [] };
  const wsum = used.reduce((a, [k]) => a + W[k], 0);
  const risk = used.reduce((a, [k, v]) => a + W[k] * v, 0) / wsum;
  const score = Math.round((100 - risk) * 10) / 10;
  return { score, band: score >= 70 ? 'good' : score >= 40 ? 'watch' : 'critical', components: c, components_used: used.map(([k]) => k).sort() };
}
const stub = http.createServer((req, res) => {
  let raw = '';
  req.on('data', d => (raw += d));
  req.on('end', () => {
    const b = raw ? JSON.parse(raw) : {};
    const send = (code, obj) => { res.writeHead(code, { 'content-type': 'application/json' }); res.end(JSON.stringify(obj)); };
    if (req.url === '/infra/score') return send(200, stubScore({ defect: null, anomaly: null, rul: null, ...b }));
    if (req.url === '/infra/health') {
      if (b.samples && b.samples[0] === 999) return send(500, { detail: 'boom' });
      if (b.samples && b.samples[0] === 998) return send(422, { detail: 'bad samples' });
      const out = { defects: null, anomaly: null, rul: null };
      const comps = { defect: null, anomaly: null, rul: null };
      if (b.image) {
        out.defects = { detector: 'unavailable', detections: [], mask_source: 'classical_cv', severity: 'moderate',
          crack: { found: true, length_mm: 40, max_width_mm: 0.6, mean_width_mm: 0.4 }, notes: ['stub'],
          annotated_image: 'data:image/jpeg;base64,' + Buffer.from('fakejpeg').toString('base64') };
        comps.defect = 52;
      }
      if (b.samples) {
        const bad = b.samples[0] > 100;
        out.anomaly = { flag: bad, score: bad ? 2 : 0.4, threshold: 1, ratio: bad ? 3.5 : 0.4, features: { rms: 1 }, data_source: 'simulated', warning: 'stub: simulated' };
        comps.anomaly = bad ? 100 : 12;
      }
      if (b.rul) { out.rul = { p10_years: 5, p50_years: 9, p90_years: 15 }; comps.rul = 35; }
      out.health = stubScore(comps);
      return send(200, out);
    }
    send(404, { detail: 'not found' });
  });
});

// ── HTTP helpers / harness ────────────────────────────────────────────────────
function api(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, API_BASE);
    const data = body === undefined ? null : JSON.stringify(body);
    const req = http.request({
      hostname: url.hostname, port: parseInt(url.port) || 4000, path: url.pathname, method, timeout: 8000,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...headers },
    }, res => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        const buf = Buffer.concat(chunks);
        let parsed; try { parsed = JSON.parse(buf.toString()); } catch { parsed = buf; }
        resolve({ status: res.statusCode, body: parsed, headers: res.headers });
      });
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    if (data) req.write(data);
    req.end();
  });
}
const bearer = t => ({ Authorization: `Bearer ${t}` });
const login = async (phone, password) => (await api('POST', '/api/auth/login', { phone, password })).body.token;

let pass = 0, fail = 0; const failures = [];
async function test(name, fn) {
  try { await fn(); pass++; console.log(`  [PASS] ${name}`); }
  catch (e) { fail++; failures.push(`${name}: ${e.message}`); console.log(`  [FAIL] ${name} — ${e.message}`); }
}

async function run() {
  await new Promise((ok, no) => stub.once('error', no).listen(STUB_PORT, '127.0.0.1', ok));
  console.log('\n=== NagarSetu V2.0 - Infrastructure Health API Tests ===\n');

  const admin = await login('+919876543210', 'SuperAdmin@NagarSetu2026!');
  const inspector = await login('+919876543214', 'Inspector@Infra2026!');
  const officer = await login('+919876543211', 'WardOfficer@Ward12!');
  assert.ok(admin && inspector && officer, 'login fixtures');

  let sid;
  await test('POST /api/structures without token -> 401', async () => {
    assert.equal((await api('POST', '/api/structures', { name: 'x' })).status, 401);
  });
  await test('POST /api/structures as INSPECTOR -> 403 (admin only)', async () => {
    const r = await api('POST', '/api/structures', { name: 'Nope', type: 'culvert', lat: 1, lng: 1 }, bearer(inspector));
    assert.equal(r.status, 403);
  });
  await test('POST /api/structures rejects invalid body -> 400', async () => {
    const r = await api('POST', '/api/structures', { name: 'A', type: 'spaceship', lat: 999, lng: 0 }, bearer(admin));
    assert.equal(r.status, 400); assert.ok(r.body.issues.length >= 2);
  });
  await test('POST /api/structures creates a structure with unknown health', async () => {
    const r = await api('POST', '/api/structures', { name: 'Test Bridge (fictional)', type: 'road_bridge', lat: 22.72, lng: 75.86 }, bearer(admin));
    assert.equal(r.status, 201); assert.equal(r.body.health.band, 'unknown'); assert.equal(r.body.health.score, null);
    sid = r.body.id;
  });

  await test('GET /api/structures without token -> 401', async () => {
    assert.equal((await api('GET', '/api/structures')).status, 401);
  });
  await test('GET /api/structures lists structures (no history leaked)', async () => {
    const r = await api('GET', '/api/structures', undefined, bearer(officer));
    assert.equal(r.status, 200); const s = r.body.find(x => x.id === sid);
    assert.ok(s && s.health.band === 'unknown' && !('health_history' in s));
  });
  await test('GET /api/structures/:id 401 / 404 / 400 / 200', async () => {
    assert.equal((await api('GET', `/api/structures/${sid}`)).status, 401);
    assert.equal((await api('GET', '/api/structures/nope', undefined, bearer(admin))).status, 404);
    assert.equal((await api('GET', '/api/structures/..%2Fetc', undefined, bearer(admin))).status, 400);
    const r = await api('GET', `/api/structures/${sid}`, undefined, bearer(admin));
    assert.equal(r.status, 200); assert.equal(r.body.latest_inspection, null);
  });

  // ── inspect ──
  const inspectBody = { image: BIG_B64, reference_mm: 10, reference_px: 100, rul: {} };
  await test('POST /:id/inspect without token -> 401', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, inspectBody)).status, 401);
  });
  await test('POST /:id/inspect as WARD_OFFICER -> 403', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, inspectBody, bearer(officer))).status, 403);
  });
  await test('POST /:id/inspect unknown structure -> 404', async () => {
    assert.equal((await api('POST', '/api/structures/nope/inspect', inspectBody, bearer(inspector))).status, 404);
  });
  await test('POST /:id/inspect validates body (400) and image type (415)', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, { image: 'short' }, bearer(inspector))).status, 400);
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, { ...inspectBody, reference_px: undefined }, bearer(inspector))).status, 400);
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, { ...inspectBody, extra: 1 }, bearer(inspector))).status, 400);
    const notImg = Buffer.from('this is definitely not an image '.repeat(8)).toString('base64');
    assert.equal((await api('POST', `/api/structures/${sid}/inspect`, { image: notImg }, bearer(inspector))).status, 415);
  });

  // socket listener for the happy path
  const events = [];
  const sock = io(API_BASE, { auth: { token: admin }, transports: ['websocket'] });
  await new Promise((ok, no) => { sock.on('connect', ok); sock.on('connect_error', no); });
  sock.on('structure:health_update', d => events.push(['health', d]));
  sock.on('hazard:auto_created', d => events.push(['hazard', d]));

  let iid;
  await test('POST /:id/inspect stores inspection, updates health, emits socket event', async () => {
    const r = await api('POST', `/api/structures/${sid}/inspect`, inspectBody, bearer(inspector));
    assert.equal(r.status, 201, JSON.stringify(r.body));
    assert.equal(r.body.inspection.crack.max_width_mm, 0.6);
    assert.equal(r.body.health.components.defect, 52);
    assert.equal(r.body.health.components.anomaly, null, 'unmeasured anomaly stays null, not "healthy"');
    assert.ok(typeof r.body.health.score === 'number');
    iid = r.body.inspection.id;
    await new Promise(r2 => setTimeout(r2, 300));
    assert.ok(events.some(([k, d]) => k === 'health' && d.id === sid && d.source === 'inspection'), 'socket event');
  });
  await test('GET inspection image requires auth and returns image/jpeg', async () => {
    const url = `/api/structures/${sid}/inspections/${iid}/image`;
    assert.equal((await api('GET', url)).status, 401);
    const r = await api('GET', url, undefined, bearer(officer));
    assert.equal(r.status, 200); assert.match(r.headers['content-type'], /image\/jpeg/);
    assert.equal((await api('GET', `/api/structures/${sid}/inspections/insp_nope/image`, undefined, bearer(officer))).status, 404);
  });

  // ── telemetry ──
  await test('POST /:id/telemetry without credentials -> 401; wrong sensor key -> 401', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(32).fill(0.1) })).status, 401);
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(32).fill(0.1) }, { 'x-sensor-api-key': 'wrong' })).status, 401);
  });
  await test('POST /:id/telemetry as WARD_OFFICER -> 403', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(32).fill(0.1) }, bearer(officer))).status, 403);
  });
  await test('POST /:id/telemetry validates samples (400)', async () => {
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: [1, 2, 3] }, bearer(inspector))).status, 400);
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(32).fill('x') }, bearer(inspector))).status, 400);
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(50001).fill(0) }, bearer(inspector))).status, 400);
  });
  await test('POST /:id/telemetry via sensor key keeps defect component, adds anomaly', async () => {
    const r = await api('POST', `/api/structures/${sid}/telemetry`, { samples: Array(256).fill(0.1), fs: 128 }, { 'x-sensor-api-key': SENSOR_KEY });
    assert.equal(r.status, 201, JSON.stringify(r.body));
    assert.equal(r.body.health.components.defect, 52); assert.equal(r.body.health.components.anomaly, 12);
    assert.equal(r.body.anomaly.data_source, 'simulated');
    assert.ok(r.body.warnings.length >= 1, 'model caveat surfaced to caller');
  });
  await test('AI service failure -> 503 and NO health change; AI 4xx -> 422', async () => {
    const before = (await api('GET', `/api/structures/${sid}`, undefined, bearer(admin))).body.health;
    const r = await api('POST', `/api/structures/${sid}/telemetry`, { samples: [999, ...Array(40).fill(0)] }, bearer(inspector));
    assert.equal(r.status, 503);
    const after = (await api('GET', `/api/structures/${sid}`, undefined, bearer(admin))).body.health;
    assert.deepEqual(after, before);
    assert.equal((await api('POST', `/api/structures/${sid}/telemetry`, { samples: [998, ...Array(40).fill(0)] }, bearer(inspector))).status, 422);
  });
  await test('critical score escalates once to a hazard zone and emits hazard:auto_created', async () => {
    events.length = 0;
    const r = await api('POST', `/api/structures/${sid}/telemetry`, { samples: [500, ...Array(100).fill(0)] }, bearer(inspector));
    assert.equal(r.status, 201);
    assert.ok(r.body.health.score < 40, `score ${r.body.health.score}`);
    assert.ok(r.body.escalation && r.body.escalation.hazard_id);
    const r2 = await api('POST', `/api/structures/${sid}/telemetry`, { samples: [500, ...Array(100).fill(0)] }, bearer(inspector));
    assert.equal(r2.body.escalation.hazard_id, r.body.escalation.hazard_id, 'deduplicated');
    assert.equal(r2.body.escalation.existing, true);
    await new Promise(x => setTimeout(x, 300));
    assert.equal(events.filter(([k]) => k === 'hazard').length, 1, 'exactly one hazard event');
    const hz = (await api('GET', '/api/hazards')).body;
    const z = (Array.isArray(hz) ? hz : hz.hazards || []).find(h => h.structure_id === sid);
    assert.ok(z && z.requires_verification === true, 'hazard flagged for verification');
  });

  // ── history ──
  await test('GET /:id/history 401 / 404 / 200 with ordered history', async () => {
    assert.equal((await api('GET', `/api/structures/${sid}/history`)).status, 401);
    assert.equal((await api('GET', '/api/structures/nope/history', undefined, bearer(admin))).status, 404);
    const r = await api('GET', `/api/structures/${sid}/history`, undefined, bearer(officer));
    assert.equal(r.status, 200);
    assert.ok(r.body.health_history.length >= 4 && r.body.inspections.length === 1 && r.body.readings.length >= 3);
    assert.ok(r.body.inspections[0].image_url.includes(iid));
    assert.ok(!JSON.stringify(r.body).includes('annotated_image'), 'no inline base64 in history');
  });

  sock.close(); stub.close();
  const total = pass + fail;
  console.log(`\n${'='.repeat(60)}\nInfra API Test Results: ${pass}/${total} PASSED\n${'='.repeat(60)}`);
  failures.forEach(f => console.log('  - ' + f));
  return fail === 0;
}

if (require.main === module) {
  run().then(ok => process.exit(ok ? 0 : 1)).catch(e => { console.error('[FATAL]', e); process.exit(1); });
}
module.exports = { run };
