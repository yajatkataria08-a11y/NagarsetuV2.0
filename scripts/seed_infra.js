/**
 * NagarSetu V2.0 — Infrastructure seed (structures only)
 *
 *   node scripts/seed_infra.js            # adds/refreshes the 9 demo structures
 *
 * Every structure here is FICTIONAL and carries no invented damage: health starts
 * as "unknown" until a real inspection or telemetry window is submitted. Do not add
 * real, named bridges with made-up defects - that would read as a factual claim.
 *
 * Only touches `structures`; other store collections are left alone. Honors
 * DATA_FILE like the backend does.
 */
const fs = require('fs');
const path = require('path');

const STORE = process.env.DATA_FILE
  ? path.resolve(process.env.DATA_FILE)
  : path.resolve(__dirname, '../backend/data/store.json');

const SEED = [
  ['Demo Bridge 01 — Canal Crossing East',      'road_bridge',   22.7196, 75.8577, 'Ward 11'],
  ['Demo Bridge 02 — Ring Road Flyover Span A', 'flyover',       22.7503, 75.8937, 'Ward 4'],
  ['Demo Bridge 03 — Riverside Footbridge',     'footbridge',    22.7090, 75.8490, 'Ward 9'],
  ['Demo Bridge 04 — Rail Overbridge North',    'rail_overbridge', 22.7332, 75.8728, 'Ward 2'],
  ['Demo Bridge 05 — Market Link Culvert',      'culvert',       22.7011, 75.8812, 'Ward 15'],
  ['Demo Bridge 06 — Industrial Estate Viaduct','viaduct',       22.6890, 75.8650, 'Ward 18'],
  ['Demo Bridge 07 — Lake Causeway',            'causeway',      22.7415, 75.8340, 'Ward 6'],
  ['Demo Bridge 08 — Old City Arch Bridge',     'arch_bridge',   22.7180, 75.8560, 'Ward 1'],
  ['Demo Bridge 09 — University Pedestrian Deck','footbridge',   22.7560, 75.8800, 'Ward 3'],
];

const store = fs.existsSync(STORE) ? JSON.parse(fs.readFileSync(STORE, 'utf-8')) : {};
const keep = (store.structures || []).filter(s => !String(s.id).startsWith('str_seed_'));
const seeded = SEED.map(([name, type, lat, lng, ward], i) => ({
  id: `str_seed_${String(i + 1).padStart(2, '0')}`,
  name, type, lat, lng, ward, fictional: true,
  health: { score: null, band: 'unknown', updated_at: null,
            components: { defect: null, anomaly: null, rul: null }, components_used: [] },
  health_history: [],
  created_at: new Date().toISOString(),
}));
store.structures = [...seeded, ...keep];
store.inspections = store.inspections || [];
store.readings = store.readings || [];
fs.mkdirSync(path.dirname(STORE), { recursive: true });
fs.writeFileSync(STORE + '.tmp', JSON.stringify(store, null, 2));
fs.renameSync(STORE + '.tmp', STORE);
console.log(`Seeded ${seeded.length} fictional structures into ${STORE}`);
