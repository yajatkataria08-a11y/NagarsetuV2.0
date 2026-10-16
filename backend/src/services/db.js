/**
 * NagarSetu V2 — Supabase DB Layer (Phase 2 + 3)
 * Replaces in-memory dataStore.js with Supabase persistent storage.
 * Exposes typed CRUD functions + Realtime subscription.
 */

require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');
const logger = require('../utils/logger');

// ── Supabase clients ──────────────────────────────────────────────────────────
const SUPABASE_URL     = process.env.SUPABASE_URL || '';
const SUPABASE_SERVICE = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

// Service-role client bypasses RLS (admin operations)
const supabase = SUPABASE_URL && SUPABASE_SERVICE
  ? createClient(SUPABASE_URL, SUPABASE_SERVICE, {
      auth: { autoRefreshToken: false, persistSession: false },
    })
  : null;

const isLive = !!supabase;
if (!isLive) logger.warn('[DB] Supabase keys missing — using empty stub mode');

// ── Helper: unwrap Supabase response ─────────────────────────────────────────
function unwrap({ data, error }) {
  if (error) throw new Error(error.message);
  return data;
}

// ── SOS Events ───────────────────────────────────────────────────────────────
async function getSosEvents(status) {
  if (!supabase) return [];
  let q = supabase.from('sos_events').select('*').order('triggered_at', { ascending: false }).limit(50);
  if (status) q = q.eq('status', status);
  return unwrap(await q);
}

async function addSosEvent(data) {
  if (!supabase) return { id: 'mock-' + Date.now(), ...data };
  return unwrap(await supabase.from('sos_events').insert(data).select().single());
}

async function updateSos(id, patch) {
  if (!supabase) return { id, ...patch };
  return unwrap(await supabase.from('sos_events').update(patch).eq('id', id).select().single());
}

// ── Incidents ─────────────────────────────────────────────────────────────────
async function getIncidents(filters = {}) {
  if (!supabase) return [];
  let q = supabase.from('incidents').select('*').order('reported_at', { ascending: false }).limit(100);
  if (filters.status)   q = q.eq('status', filters.status);
  if (filters.ward)     q = q.eq('ward', filters.ward);
  if (filters.severity) q = q.gte('severity', filters.severity);
  return unwrap(await q);
}

async function getIncidentById(id) {
  if (!supabase) return null;
  return unwrap(await supabase.from('incidents').select('*').eq('id', id).single());
}

async function addIncident(data) {
  if (!supabase) return { id: 'mock-' + Date.now(), ...data, status: 'pending' };
  return unwrap(await supabase.from('incidents').insert(data).select().single());
}

async function updateIncident(id, patch) {
  if (!supabase) return { id, ...patch };
  return unwrap(await supabase.from('incidents').update(patch).eq('id', id).select().single());
}

// ── Wards ─────────────────────────────────────────────────────────────────────
async function getWards() {
  if (!supabase) return [];
  return unwrap(await supabase.from('wards').select('*').order('sla_breaches', { ascending: false }));
}

// ── Responders ────────────────────────────────────────────────────────────────
async function getResponders(status) {
  if (!supabase) return [];
  let q = supabase.from('responders').select('*').order('avg_response_min');
  if (status) q = q.eq('status', status);
  return unwrap(await q);
}

// Find nearest on-duty responders within radius_m metres of lat/lng (PostGIS)
async function getRespondersNear(lat, lng, radiusM = 5000) {
  if (!supabase) return [];
  return unwrap(await supabase.rpc('responders_near', { lat, lng, radius_m: radiusM }));
}

// ── Tracking Sessions ─────────────────────────────────────────────────────────
async function createTrackingSession(data) {
  if (!supabase) return { session_id: 'mock-' + Date.now(), ...data };
  return unwrap(await supabase.from('tracking_sessions').insert(data).select().single());
}

async function getTrackingSession(sessionId) {
  if (!supabase) return null;
  return unwrap(await supabase.from('tracking_sessions')
    .select('*').eq('session_id', sessionId).eq('active', true).single());
}

async function updateTrackingLocation(sessionId, lat, lng) {
  if (!supabase) return;
  await supabase.from('tracking_sessions').update({
    lat, lng,
    geom: `SRID=4326;POINT(${lng} ${lat})`,
  }).eq('session_id', sessionId);
}

// ── Dashboard Stats ──────────────────────────────────────────────────────────
async function getDashboardStats() {
  if (!supabase) return {
    activeSos: 0, pendingIncidents: 0, resolvedToday: 0,
    activeResponders: 0, avgResponseMin: 0, slaBreaches: 0,
  };

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const [
    { count: activeSos },
    { count: pendingIncidents },
    { count: resolvedToday },
    { count: activeResponders },
  ] = await Promise.all([
    supabase.from('sos_events').select('*', { count: 'exact', head: true }).eq('status', 'active'),
    supabase.from('incidents').select('*', { count: 'exact', head: true }).eq('status', 'pending'),
    supabase.from('incidents').select('*', { count: 'exact', head: true })
      .eq('status', 'resolved').gte('resolved_at', today.toISOString()),
    supabase.from('responders').select('*', { count: 'exact', head: true }).neq('status', 'off-duty'),
  ]);

  const { data: slaData } = await supabase.from('wards').select('sla_breaches');
  const slaBreaches = (slaData || []).reduce((s, w) => s + (w.sla_breaches || 0), 0);

  const { data: respData } = await supabase.from('responders').select('avg_response_min').neq('status', 'off-duty');
  const avgResponseMin = respData?.length
    ? Math.round(respData.reduce((s, r) => s + r.avg_response_min, 0) / respData.length)
    : 0;

  return {
    activeSos:        activeSos || 0,
    pendingIncidents: pendingIncidents || 0,
    resolvedToday:    resolvedToday || 0,
    activeResponders: activeResponders || 0,
    avgResponseMin,
    slaBreaches,
  };
}

// ── Hazard Zones ──────────────────────────────────────────────────────────────
async function getActiveHazardZones() {
  if (!supabase) return [];
  const now = new Date().toISOString();
  return unwrap(await supabase.from('hazard_zones')
    .select('*').eq('active', true).or(`expires_at.is.null,expires_at.gt.${now}`));
}

// ── Supabase Realtime — push DB changes to Socket.IO clients ─────────────────
function subscribeToChanges(io) {
  if (!supabase) {
    logger.info('[DB] Supabase not configured — skipping Realtime subscriptions');
    return;
  }

  // New SOS event from Android app
  supabase.channel('sos-inserts')
    .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'sos_events' }, ({ new: row }) => {
      logger.info('[Realtime] New SOS', { id: row.id });
      io?.emit('new_sos', row);
    })
    .subscribe();

  // New incident report
  supabase.channel('incident-inserts')
    .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'incidents' }, ({ new: row }) => {
      logger.info('[Realtime] New incident', { id: row.id, type: row.type });
      io?.emit('new_incident', row);
    })
    .subscribe();

  // Incident status changes
  supabase.channel('incident-updates')
    .on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'incidents' }, ({ new: row }) => {
      io?.emit('incident:updated', row);
    })
    .subscribe();

  // SOS status updates
  supabase.channel('sos-updates')
    .on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'sos_events' }, ({ new: row }) => {
      io?.emit('sos:updated', row);
    })
    .subscribe();

  // Tracking location updates
  supabase.channel('tracking-updates')
    .on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'tracking_sessions' }, ({ new: row }) => {
      io?.emit(`tracking:${row.session_id}`, { lat: row.lat, lng: row.lng, updatedAt: Date.now() });
    })
    .subscribe();

  logger.info('[DB] Supabase Realtime subscriptions registered');
}

module.exports = {
  supabase,
  getSosEvents, addSosEvent, updateSos,
  getIncidents, getIncidentById, addIncident, updateIncident,
  getWards,
  getResponders, getRespondersNear,
  createTrackingSession, getTrackingSession, updateTrackingLocation,
  getDashboardStats,
  getActiveHazardZones,
  subscribeToChanges,
};
