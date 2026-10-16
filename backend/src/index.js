/**
 * NagarSetu V2 — API Gateway (Phase 3)
 * ─────────────────────────────────────
 * Express + Socket.IO + Supabase Realtime
 *
 * Replaces the old volatile in-memory dataStore.js
 * with a proper Supabase-backed persistent data layer.
 */

require('dotenv').config();
const express   = require('express');
const http      = require('http');
const { Server } = require('socket.io');
const cors      = require('cors');
const helmet    = require('helmet');
const morgan    = require('morgan');
const rateLimit = require('express-rate-limit');
const path      = require('path');

const logger    = require('./utils/logger');
const db        = require('./services/db');

// ── Routes ────────────────────────────────────────────────────────────────────
const sosRoutes           = require('./routes/sos');
const incidentRoutes      = require('./routes/incidents');
const trackingRoutes      = require('./routes/tracking');
const analyticsRoutes     = require('./routes/analytics');
const hazardRoutes        = require('./routes/hazards');
const riskRoutes          = require('./routes/risk');
const routingRoutes       = require('./routes/routing');
const cctvRoutes          = require('./routes/cctv');
const usersRoutes         = require('./routes/users');
const notificationsRoutes = require('./routes/notifications');
const wardsRoutes         = require('./routes/wards');

// ── App bootstrap ─────────────────────────────────────────────────────────────
const app    = express();
const server = http.createServer(app);
const PORT   = process.env.PORT || 4000;

// ── Socket.IO (Phase 3 — Real-time event mesh) ────────────────────────────────
const io = new Server(server, {
  cors: {
    origin: process.env.FRONTEND_URL || 'http://localhost:3000',
    methods: ['GET', 'POST', 'PATCH', 'DELETE'],
  },
  pingTimeout:  30000,
  pingInterval: 15000,
});

// Make io available to route handlers
app.set('io', io);

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:3000' }));
app.use(helmet({ contentSecurityPolicy: false }));
app.use(morgan('dev'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Rate limiting — 200 req/15min per IP
app.use('/api/', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests — please slow down' },
}));

// ── API Routes ────────────────────────────────────────────────────────────────
app.use('/api/sos',           sosRoutes);
app.use('/api/incidents',     incidentRoutes);
app.use('/api/tracking',      trackingRoutes);
app.use('/api/analytics',     analyticsRoutes);
app.use('/api/hazards',       hazardRoutes);
app.use('/api/risk',          riskRoutes);
app.use('/api/routing',       routingRoutes);
app.use('/api/cctv',          cctvRoutes);
app.use('/api/users',         usersRoutes);
app.use('/api/notifications', notificationsRoutes);
app.use('/api/wards',         wardsRoutes);

// ── Dashboard Master Stats ───────────────────────────────────────────────────
app.get('/api/stats', async (req, res) => {
  try {
    const stats = await db.getDashboardStats();
    res.json(stats);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── Self-Documenting API Catalog ──────────────────────────────────────────────
app.get('/api/docs', (req, res) => {
  res.json({
    title: 'NagarSetu V2.0 Enterprise API Specification',
    version: '2.0.0',
    endpoints: [
      { path: '/api/sos', methods: ['GET', 'POST'], desc: 'Real-time SOS beacons & vitals triage' },
      { path: '/api/incidents', methods: ['GET', 'POST', 'PATCH'], desc: 'Civic incidents with automated Scikit-Learn AI verification' },
      { path: '/api/tracking', methods: ['GET', 'POST'], desc: 'Live GPS breadcrumb tracking and ping stream' },
      { path: '/api/cctv', methods: ['GET', 'POST'], desc: 'Krisis 6-camera CCTV vision wall & YOLOv8 detections' },
      { path: '/api/hazards', methods: ['GET', 'POST', 'DELETE'], desc: 'Raksha dynamic geofenced hazard zones with TTL' },
      { path: '/api/risk/heatmap', methods: ['GET'], desc: 'Dual-city spatio-temporal Gaussian risk matrix' },
      { path: '/api/risk/point', methods: ['POST'], desc: 'Coordinate continuous risk score via GradientBoostingRegressor' },
      { path: '/api/routing/calculate-routes', methods: ['POST'], desc: 'Triple path engine: Fastest, Safest (Main-Road), Eco' },
      { path: '/api/users', methods: ['GET', 'PATCH'], desc: 'Citizen trust registry & anti-spam moderation' },
      { path: '/api/notifications', methods: ['GET', 'POST'], desc: 'Municipal multi-channel notification stream' },
      { path: '/api/wards', methods: ['GET'], desc: 'Ward leaderboard & SLA compliance tracking' },
      { path: '/api/stats', methods: ['GET'], desc: 'Composite municipal KPI aggregates' },
    ]
  });
});

// ── Health check ─────────────────────────────────────────────────────────────
app.get('/api/health', (req, res) => res.json({
  status:    'ok',
  version:   '2.0.0',
  uptime:    Math.round(process.uptime()),
  timestamp: Date.now(),
  backend:   'NagarSetu V2 API Gateway',
}));

// ── Wards ─────────────────────────────────────────────────────────────────────
app.get('/api/wards', async (req, res) => {
  try {
    const wards = await db.getWards();
    res.json(wards);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── Responders ────────────────────────────────────────────────────────────────
app.get('/api/responders', async (req, res) => {
  try {
    const resp = await db.getResponders();
    res.json(resp);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── Dashboard stats ───────────────────────────────────────────────────────────
app.get('/api/stats', async (req, res) => {
  try {
    const stats = await db.getDashboardStats();
    res.json(stats);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── 404 handler ───────────────────────────────────────────────────────────────
app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// ── Error handler ─────────────────────────────────────────────────────────────
app.use((err, req, res, next) => {
  logger.error('Unhandled error', { error: err.message, stack: err.stack });
  res.status(500).json({ error: 'Internal server error' });
});

// ── Socket.IO — Real-time Event Mesh (Phase 3) ────────────────────────────────
const connectedAdmins = new Map();

io.on('connection', async (socket) => {
  const adminId = socket.handshake.query.adminId || socket.id;
  connectedAdmins.set(socket.id, { id: adminId, connectedAt: Date.now() });
  logger.info('Admin connected', { socketId: socket.id, adminId, total: connectedAdmins.size });

  // Send initial data snapshot from Supabase
  try {
    const [sos, incidents, wards, responders, stats] = await Promise.all([
      db.getSosEvents(),
      db.getIncidents(),
      db.getWards(),
      db.getResponders(),
      db.getDashboardStats(),
    ]);
    socket.emit('init', { sos, incidents, wards, responders, stats });
  } catch (e) {
    logger.warn('Failed to send init snapshot', { error: e.message });
  }

  // ─── Admin actions ──────────────────────────────────────────────
  socket.on('sos:acknowledge', async ({ id }) => {
    const updated = await db.updateSos(id, { status: 'acknowledged', acknowledged_at: new Date().toISOString() });
    io.emit('sos:updated', updated);
  });

  socket.on('sos:resolve', async ({ id }) => {
    const updated = await db.updateSos(id, { status: 'resolved', resolved_at: new Date().toISOString() });
    io.emit('sos:updated', updated);
  });

  socket.on('incident:verify', async ({ id }) => {
    const updated = await db.updateIncident(id, { status: 'verified' });
    io.emit('incident:updated', updated);
  });

  socket.on('incident:reject', async ({ id }) => {
    const updated = await db.updateIncident(id, { status: 'rejected' });
    io.emit('incident:updated', updated);
  });

  socket.on('disconnect', () => {
    connectedAdmins.delete(socket.id);
    logger.info('Admin disconnected', { socketId: socket.id, remaining: connectedAdmins.size });
  });
});

// ── Supabase Realtime subscription (forward DB changes to all admin clients) ──
db.subscribeToChanges(io);

// ── Start server ──────────────────────────────────────────────────────────────
server.listen(PORT, () => {
  logger.info(`🚀 NagarSetu V2 API Gateway running on port ${PORT}`);
});

module.exports = { app, io };
