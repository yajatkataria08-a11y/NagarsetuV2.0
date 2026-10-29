/** Input-bound tests for the public POST routes (SOS, incident, hazard, tracking). */
const http = require('http');
const assert = require('assert').strict;
const API_BASE = process.env.TEST_API_BASE || 'http://localhost:4000';

function api(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, API_BASE);
    const data = body === undefined ? null : JSON.stringify(body);
    const req = http.request({ hostname: url.hostname, port: parseInt(url.port) || 4000, path: url.pathname, method, timeout: 8000,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...headers } }, res => {
      let raw = ''; res.on('data', c => (raw += c));
      res.on('end', () => { let b; try { b = JSON.parse(raw); } catch { b = raw; } resolve({ status: res.statusCode, body: b }); });
    });
    req.on('error', reject); req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    if (data) req.write(data); req.end();
  });
}
let pass = 0, fail = 0; const failures = [];
async function test(name, fn) {
  try { await fn(); pass++; console.log(`  [PASS] ${name}`); } catch (e) { fail++; failures.push(`${name}: ${e.message}`); console.log(`  [FAIL] ${name} — ${e.message}`); }
}
const bad = async (path, body, headers) => {
  const r = await api('POST', path, body, headers);
  assert.equal(r.status, 400, `expected 400, got ${r.status} ${JSON.stringify(r.body).slice(0, 120)}`);
  assert.equal(r.body.error, 'Invalid request body');
};

async function run() {
  console.log('\n=== NagarSetu V2.0 - Input Validation Tests ===\n');
  const login = await api('POST', '/api/auth/login', { phone: '+919876543210', password: 'SuperAdmin@NagarSetu2026!' });
  const auth = { Authorization: `Bearer ${login.body.token}` };

  await test('SOS: valid payload (numeric strings, dashed phone) still accepted', async () => {
    const r = await api('POST', '/api/sos', { userName: 'T', lat: '22.7', lng: '75.8', batteryLevel: '55', heartRate: 90 });
    assert.equal(r.status, 201, JSON.stringify(r.body).slice(0, 150));
  });
  await test('SOS: rejects out-of-range / missing coordinates and vitals', async () => {
    await bad('/api/sos', { lat: 91, lng: 75 });
    await bad('/api/sos', { lat: 22 });
    await bad('/api/sos', { lat: 22, lng: 75, batteryLevel: 500 });
    await bad('/api/sos', { lat: 'abc', lng: 75 });
  });
  await test('SOS: contact list is bounded and phone-shaped (no SMS fan-out)', async () => {
    await bad('/api/sos', { lat: 22, lng: 75, contacts: Array(6).fill('+919999999999') });
    await bad('/api/sos', { lat: 22, lng: 75, contacts: ['not a phone'] });
    await bad('/api/sos', { lat: 22, lng: 75, contacts: [{ evil: 1 }] });
  });
  await test('Incident: valid accepted; oversize description/photos/severity rejected', async () => {
    assert.equal((await api('POST', '/api/incidents', { type: 'POTHOLE', description: 'ok', lat: 22.7, lng: 75.8, severity: 3 })).status, 201);
    await bad('/api/incidents', { description: 'x'.repeat(2001) });
    await bad('/api/incidents', { photos: Array(6).fill('a') });
    await bad('/api/incidents', { severity: 9 });
    await bad('/api/incidents', { lat: 200, lng: 10 });
  });
  await test('Hazard: valid accepted; NaN duration, huge radius, empty name rejected', async () => {
    assert.equal((await api('POST', '/api/hazards', { name: 'Val test', lat: 22.7, lng: 75.8, radiusMeters: 200, durationMinutes: 30 }, auth)).status, 201);
    await bad('/api/hazards', { name: 'x', lat: 22.7, lng: 75.8, durationMinutes: 'abc' }, auth);
    await bad('/api/hazards', { name: 'x', lat: 22.7, lng: 75.8, radiusMeters: 1e9 }, auth);
    await bad('/api/hazards', { name: '   ', lat: 22.7, lng: 75.8 }, auth);
  });
  await test('Tracking start: valid accepted; contact fan-out and junk phones rejected', async () => {
    assert.equal((await api('POST', '/api/tracking/start', { userName: 'T', lat: 22.7, lng: 75.8 })).status, 201);
    await bad('/api/tracking/start', { lat: 22.7, lng: 75.8, contacts: Array(50).fill('+919999999999') });
    await bad('/api/tracking/start', { lat: 22.7, lng: 75.8, contacts: ['<script>'] });
  });
  await test('Tracking ping: requires sessionId and sane values', async () => {
    await bad('/api/tracking/ping', { lat: 22.7, lng: 75.8 });
    await bad('/api/tracking/ping', { sessionId: 'x', lat: 22.7, lng: 75.8, batteryLevel: -5 });
    await bad('/api/tracking/ping', { sessionId: 'x'.repeat(65), lat: 22.7, lng: 75.8 });
  });

  console.log(`\n${'='.repeat(60)}\nValidation Test Results: ${pass}/${pass + fail} PASSED\n${'='.repeat(60)}`);
  failures.forEach(f => console.log('  - ' + f));
  return fail === 0;
}
if (require.main === module) run().then(ok => process.exit(ok ? 0 : 1)).catch(e => { console.error('[FATAL]', e); process.exit(1); });
module.exports = { run };
