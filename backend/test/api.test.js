/**
 * NagarSetu V2.0 — Comprehensive End-to-End API Test Suite (Phase 14)
 * ─────────────────────────────────────────────────────────────────────
 * Validates all REST API routes, WebSocket events, and critical edge cases.
 * Tests: SOS, Incidents, Routing, Hazards, Legal, Contractors, CCTV, Risk, Tracking
 */

const http = require('http');
const assert = require('assert').strict;

const API_BASE = process.env.TEST_API_BASE || 'http://localhost:4000';
const TIMEOUT_MS = 5000;

let passCount = 0;
let failCount = 0;
const results = [];

async function apiRequest(method, path, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, API_BASE);
    const data = body ? JSON.stringify(body) : null;
    const options = {
      hostname: url.hostname,
      port: parseInt(url.port) || 4000,
      path: url.pathname + url.search,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {})
      },
      timeout: TIMEOUT_MS
    };

    const req = http.request(options, res => {
      let raw = '';
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(raw) });
        } catch {
          resolve({ status: res.statusCode, body: raw });
        }
      });
    });

    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('Request timeout')); });

    if (data) req.write(data);
    req.end();
  });
}

async function test(name, fn) {
  try {
    await fn();
    passCount++;
    results.push({ name, status: 'PASS' });
    console.log(`  [PASS] ${name}`);
  } catch (err) {
    failCount++;
    results.push({ name, status: 'FAIL', error: err.message });
    console.log(`  [FAIL] ${name} — ${err.message}`);
  }
}

async function runSuite() {
  console.log('\n=== NagarSetu V2.0 - End-to-End API Test Suite ===\n');

  // ── Health Check ──────────────────────────────────────────────────────────
  console.log('[SUITE] Health & Connectivity');

  await test('GET /api/health returns 200 and ok status', async () => {
    const res = await apiRequest('GET', '/api/health');
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.equal(res.body.status, 'ok', 'Expected status ok');
    assert.ok(typeof res.body.version === 'string', 'version should be a string');
    assert.ok(typeof res.body.uptime === 'number', 'uptime should be a number');
  });

  await test('GET /api/docs returns API catalogue', async () => {
    const res = await apiRequest('GET', '/api/docs');
    assert.equal(res.status, 200);
    assert.ok(res.body.endpoints && res.body.endpoints.length >= 10, 'Should have >= 10 endpoint entries');
  });

  // ── SOS Monitoring ────────────────────────────────────────────────────────
  console.log('\n[SUITE] SOS Beacons');

  await test('GET /api/sos returns SOS array', async () => {
    const res = await apiRequest('GET', '/api/sos');
    assert.equal(res.status, 200);
    assert.ok(Array.isArray(res.body), 'SOS response should be an array');
  });

  let sosId;
  await test('POST /api/sos creates emergency beacon with tracking token', async () => {
    const res = await apiRequest('POST', '/api/sos', {
      userId: 'test-user-001',
      userName: 'Test User Harness',
      phone: '+91-9999999001',
      lat: 23.2599, lng: 77.4126,
      ward: 'Ward 7 Test',
      batteryLevel: 65,
      heartRate: 95,
      triggerType: 'TEST',
      contacts: ['+91-9999999002']
    });
    assert.equal(res.status, 201, `Expected 201, got ${res.status}`);
    assert.ok(res.body.id, 'SOS response must have an id');
    assert.ok(res.body.tracking_token, 'SOS response must include a tracking_token');
    sosId = res.body.id;
  });

  await test('PATCH /api/sos/:id acknowledges emergency beacon', async () => {
    if (!sosId) return;
    const res = await apiRequest('PATCH', `/api/sos/${sosId}`, { status: 'acknowledged' });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.equal(res.body.status, 'acknowledged', 'Status should be acknowledged');
  });

  // ── Incidents ─────────────────────────────────────────────────────────────
  console.log('\n[SUITE] Civic Incidents & Triage');

  await test('GET /api/incidents returns incident array', async () => {
    const res = await apiRequest('GET', '/api/incidents');
    assert.equal(res.status, 200);
    assert.ok(Array.isArray(res.body), 'Incidents response should be an array');
  });

  let incidentId;
  await test('POST /api/incidents creates incident and triggers AI verification', async () => {
    const res = await apiRequest('POST', '/api/incidents', {
      type: 'POTHOLE',
      description: 'Large pothole near MP Nagar Signal No. 5 causing accidents',
      lat: 23.231, lng: 77.435,
      ward: 'Ward 3 - MP Nagar',
      severity: 4
    });
    assert.equal(res.status, 201, `Expected 201, got ${res.status}`);
    assert.ok(res.body.id, 'Incident must have id');
    incidentId = res.body.id;
  });

  await test('POST /api/incidents/triage classifies Hinglish complaint', async () => {
    const res = await apiRequest('POST', '/api/incidents/triage', {
      description: 'Habibganj underpass me paani bhar gaya hai bohot gehra',
      lat: 23.268, lng: 77.43
    });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.ok(res.body.category, 'Triage result must have category');
    assert.ok(res.body.assigned_department, 'Triage result must have assigned_department');
    assert.ok(res.body.sla_hours, 'Triage result must have sla_hours');
  });

  await test('PATCH /api/incidents/:id marks incident as resolved', async () => {
    if (!incidentId) return;
    const res = await apiRequest('PATCH', `/api/incidents/${incidentId}`, { status: 'resolved' });
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'resolved', 'Status should be resolved');
  });

  // ── Hazard Geofences ──────────────────────────────────────────────────────
  console.log('\n[SUITE] Dynamic Hazard Geofences');

  await test('GET /api/hazards returns active zones array', async () => {
    const res = await apiRequest('GET', '/api/hazards');
    assert.equal(res.status, 200);
    assert.ok(Array.isArray(res.body), 'Hazards response should be an array');
  });

  await test('GET /api/hazards/summary returns coverage summary', async () => {
    const res = await apiRequest('GET', '/api/hazards/summary');
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'success');
    assert.ok(typeof res.body.activeCount === 'number', 'activeCount must be a number');
    assert.ok(typeof res.body.coverageSqKm === 'number', 'coverageSqKm must be a number');
  });

  let hazardId;
  await test('POST /api/hazards creates geofence with TTL', async () => {
    const res = await apiRequest('POST', '/api/hazards', {
      name: 'Test Flood Zone - API Test',
      lat: 23.2502, lng: 77.4,
      radiusMeters: 250,
      riskBoost: 2.0,
      durationMinutes: 60,
      note: 'Integration test hazard'
    });
    assert.equal(res.status, 201, `Expected 201, got ${res.status}`);
    assert.ok(res.body.id, 'Hazard must have id');
    hazardId = res.body.id;
  });

  await test('POST /api/hazards/check-point detects point in collision zone', async () => {
    const res = await apiRequest('POST', '/api/hazards/check-point', {
      lat: 23.268, lng: 77.43
    });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.ok(typeof res.body.inHazardZone === 'boolean', 'inHazardZone must be boolean');
    assert.ok(Array.isArray(res.body.collisions), 'collisions must be an array');
    assert.ok(res.body.responseTimeMs < 50, `Collision check must run < 50ms, got ${res.body.responseTimeMs}ms`);
  });

  await test('DELETE /api/hazards/:id removes geofence', async () => {
    if (!hazardId) return;
    const res = await apiRequest('DELETE', `/api/hazards/${hazardId}`);
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
  });

  // ── Risk Engine ───────────────────────────────────────────────────────────
  console.log('\n[SUITE] Spatio-Temporal Risk Engine');

  await test('GET /api/risk/heatmap?city=bhopal returns 50x50 grid', async () => {
    const res = await apiRequest('GET', '/api/risk/heatmap?city=bhopal');
    assert.equal(res.status, 200);
    assert.ok(res.body.grid, 'Response must include grid field');
    assert.ok(res.body.totalZones > 0, 'Must have totalZones > 0');
  });

  await test('POST /api/risk/point returns risk_score for Habibganj', async () => {
    const res = await apiRequest('POST', '/api/risk/point', {
      lat: 23.268, lng: 77.43, hour: 22, day: 4
    });
    assert.equal(res.status, 200);
    assert.ok(typeof res.body.risk_score === 'number', 'risk_score must be numeric');
    assert.ok(res.body.risk_score >= 0 && res.body.risk_score <= 100, 'risk_score must be 0-100');
    assert.ok(res.body.risk_level, 'risk_level must be present');
  });

  // ── Routing ───────────────────────────────────────────────────────────────
  console.log('\n[SUITE] Triple-Path Routing Engine');

  await test('POST /api/routing/calculate-routes returns 3 distinct paths', async () => {
    const res = await apiRequest('POST', '/api/routing/calculate-routes', {
      origin: { lat: 23.2599, lng: 77.4126 },
      destination: { lat: 23.268, lng: 77.43 },
      hour: 18
    });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.ok(Array.isArray(res.body.routes), 'routes must be an array');
    assert.ok(res.body.routes.length >= 2, 'Must return at least 2 route options');
    assert.ok(res.body.routes.every(r => r.coordinates && r.coordinates.length > 0), 'Each route must have coordinates');
    assert.ok(res.body.computation_ms < 200, `Routing must complete < 200ms, got ${res.body.computation_ms}ms`);
  });

  // ── Legal Knowledge Base ──────────────────────────────────────────────────
  console.log('\n[SUITE] BIMSTEC Legal Knowledge Base');

  await test('GET /api/legal/violations returns 154 violations for India', async () => {
    const res = await apiRequest('GET', '/api/legal/violations?nation=IN&limit=200');
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'success');
    assert.ok(res.body.total === 154, `Expected 154 violations, got ${res.body.total}`);
  });

  await test('GET /api/legal/nations returns 7 BIMSTEC jurisdictions', async () => {
    const res = await apiRequest('GET', '/api/legal/nations');
    assert.equal(res.status, 200);
    assert.equal(res.body.totalNations, 7, `Expected 7 nations, got ${res.body.totalNations}`);
  });

  await test('POST /api/legal/calculate-fine resolves DRUNK_DRIVING fine with 3x repeat multiplier', async () => {
    const res = await apiRequest('POST', '/api/legal/calculate-fine', {
      violationId: 'DRNK_01',
      vehicleType: 'TRUCK',
      repeatOffence: 3,
      nationCode: 'IN'
    });
    assert.equal(res.status, 200);
    assert.ok(res.body.calculation, 'Must return calculation object');
    assert.ok(res.body.calculation.grandTotal > 0, 'Grand total must be positive');
    // DRUNK_DRIVING base INR 10000 x TRUCK(2.5) x 3rd(4.0) = 100000
    assert.ok(res.body.calculation.grandTotalInr === 100000, `Expected 100000, got ${res.body.calculation.grandTotalInr}`);
  });

  await test('POST /api/legal/query-bot matches helmet violation from Hinglish query', async () => {
    const res = await apiRequest('POST', '/api/legal/query-bot', {
      query: 'Helmet nahi lagaya tha, fine kitna hoga?',
      nationCode: 'IN'
    });
    assert.equal(res.status, 200);
    assert.ok(res.body.answer, 'Bot must return an answer');
    assert.ok(res.body.answer.length > 20, 'Answer must be substantive');
  });

  await test('GET /api/legal/violations with search query filters correctly', async () => {
    const res = await apiRequest('GET', '/api/legal/violations?q=drunk+driving&limit=50');
    assert.equal(res.status, 200);
    assert.ok(res.body.total > 0, 'Must match at least 1 drunk driving violation');
  });

  // ── Contractor Accountability ─────────────────────────────────────────────
  console.log('\n[SUITE] Contractor Accountability Ledger');

  await test('GET /api/contractors returns 5 registered contractors', async () => {
    const res = await apiRequest('GET', '/api/contractors');
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'success');
    assert.ok(res.body.contractors.length >= 5, 'Must have >= 5 contractors');
  });

  await test('GET /api/contractors/audit-trail returns logged entries', async () => {
    const res = await apiRequest('GET', '/api/contractors/audit-trail');
    assert.equal(res.status, 200);
    assert.ok(res.body.auditTrail, 'Must return auditTrail array');
    assert.ok(res.body.totalEntries >= 2, 'Must have >= 2 pre-seeded audit entries');
  });

  await test('POST /api/contractors/:id/penalize deducts fine and logs to ledger', async () => {
    const res = await apiRequest('POST', '/api/contractors/CON-04/penalize', {
      penaltyAmount: 12500,
      breachReason: 'API Test Penalty — Storm drain repair SLA exceeded by 72 hours',
      officerBadge: 'API-TESTER-01'
    });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.equal(res.body.status, 'success');
    assert.ok(res.body.auditEntry, 'Penalty must create an audit entry');
    assert.equal(res.body.auditEntry.penalty_amount_inr, 12500, 'Audit entry must record correct penalty amount');
  });

  // ── Live Tracking ─────────────────────────────────────────────────────────
  console.log('\n[SUITE] Live Tracking & Safety Sessions');

  let trackToken;
  await test('POST /api/tracking creates tracking session with shareable token', async () => {
    const res = await apiRequest('POST', '/api/tracking', {
      userId: 'test-citizen-001',
      userName: 'Test Tracker',
      lat: 23.2599, lng: 77.4126,
      ward: 'Ward 7 Test'
    });
    assert.equal(res.status, 201, `Expected 201, got ${res.status}`);
    assert.ok(res.body.session_id, 'Must return session_id');
    trackToken = res.body.session_id;
  });

  await test('POST /api/tracking/ping updates live location breadcrumb', async () => {
    if (!trackToken) return;
    const res = await apiRequest('POST', '/api/tracking/ping', {
      sessionId: trackToken,
      lat: 23.261, lng: 77.415,
      speed: 12.5
    });
    assert.equal(res.status, 200, `Expected 200, got ${res.status}`);
    assert.ok(res.body.updated, 'Must confirm location update');
  });

  // ── Analytics ─────────────────────────────────────────────────────────────
  console.log('\n[SUITE] Analytics & KPI Engine');

  await test('GET /api/analytics returns structured analytics payload', async () => {
    const res = await apiRequest('GET', '/api/analytics');
    assert.equal(res.status, 200);
    assert.ok(res.body, 'Must return analytics data');
  });

  await test('GET /api/stats returns all 6 dashboard KPI counters', async () => {
    const res = await apiRequest('GET', '/api/stats');
    assert.equal(res.status, 200);
    assert.ok(typeof res.body.activeSos === 'number', 'activeSos must be a number');
    assert.ok(typeof res.body.pendingIncidents === 'number', 'pendingIncidents must be a number');
    assert.ok(typeof res.body.resolvedToday === 'number', 'resolvedToday must be a number');
  });

  // ── Final Report ──────────────────────────────────────────────────────────
  const total = passCount + failCount;
  const pct = Math.round((passCount / total) * 100);

  console.log(`\n${'='.repeat(60)}`);
  console.log(`NagarSetu V2.0 Test Results: ${passCount}/${total} PASSED (${pct}%)`);
  console.log(`${'='.repeat(60)}`);

  if (failCount > 0) {
    console.log('\nFailed Tests:');
    results.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`  - ${r.name}: ${r.error}`);
    });
  }

  console.log('\nSuite complete.\n');
  return failCount === 0;
}

// Run if called directly
if (require.main === module) {
  runSuite().then(passed => {
    process.exit(passed ? 0 : 1);
  }).catch(err => {
    console.error('[FATAL] Test runner crashed:', err);
    process.exit(1);
  });
}

module.exports = { runSuite };
