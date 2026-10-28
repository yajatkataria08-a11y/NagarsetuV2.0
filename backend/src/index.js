/**
 * NagarSetu V2 — API Gateway (Phase 3)
 * ─────────────────────────────────────
 * Express + Socket.IO + Supabase Realtime
 *
 * Replaces the old volatile in-memory dataStore.js
 * with a proper Supabase-backed persistent data layer.
 */

require('dotenv').config();
const config    = require('./config');
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
const sensorsRoutes       = require('./routes/sensors');
const legalRoutes         = require('./routes/legal');
const contractorRoutes    = require('./routes/contractors');
// Note: DISABLE_AUTH=true in .env — dashboard works in demo mode without login
const authRoutes          = require('./middleware/auth');

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

// Inject io into hazards module so the TTL janitor can emit hazard:expired
hazardRoutes.setIo(io);

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:3000' }));
app.use(helmet({ contentSecurityPolicy: false }));
app.use(morgan('dev'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));
app.use('/streams', express.static(path.join(__dirname, '../public/streams')));
app.use('/public',  express.static(path.join(__dirname, '../public')));

// Rate limiting:
// 1. Dedicated Emergency Limiter (high capacity to prevent carrier NAT throttling during real crises)
const emergencyLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5000,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Emergency rate limit exceeded — please retry shortly' },
});

// 2. Standard API Rate Limiter
const standardLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests — please slow down' },
  skip: (req) => req.baseUrl === '/api/sos' || req.originalUrl.startsWith('/api/sos') || req.originalUrl.startsWith('/api/tracking/ping')
});

// ── API Routes (Mount emergency endpoints with dedicated limiter) ─────────────
app.use('/api/sos',           emergencyLimiter, sosRoutes);
app.use('/api/tracking/ping', emergencyLimiter);
app.use('/api/',              standardLimiter);

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
app.use('/api/ingest',        sensorsRoutes);
app.use('/api/legal',         legalRoutes);
app.use('/api/contractors',   contractorRoutes);
app.use('/api/auth',          authRoutes);

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
      { path: '/api/cctv', methods: ['GET', 'POST'], desc: 'Municipal 6-camera CCTV vision wall & YOLOv8 detections' },
      { path: '/api/hazards', methods: ['GET', 'POST', 'DELETE'], desc: 'Dynamic geofenced hazard zones with TTL' },
      { path: '/api/risk/heatmap', methods: ['GET'], desc: 'Dual-city spatio-temporal Gaussian risk matrix' },
      { path: '/api/risk/point', methods: ['POST'], desc: 'Coordinate continuous risk score via GradientBoostingRegressor' },
      { path: '/api/routing/calculate-routes', methods: ['POST'], desc: 'Triple path engine: Fastest, Safest (Main-Road), Eco' },
      { path: '/api/users', methods: ['GET', 'PATCH'], desc: 'Citizen trust registry & anti-spam moderation' },
      { path: '/api/notifications', methods: ['GET', 'POST'], desc: 'Municipal multi-channel notification stream' },
      { path: '/api/wards', methods: ['GET'], desc: 'Ward leaderboard & SLA compliance tracking' },
      { path: '/api/legal/violations', methods: ['GET', 'POST'], desc: 'BIMSTEC 154-violation traffic code engine & fine calculator' },
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

// ── Socket.IO — Real-time Event Mesh with JWT Handshake Auth ──────────────────
const connectedAdmins = new Map();
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || config.JWT_SECRET || 'nagarsetu_dev_secret_change_in_production';

// Socket.IO authentication middleware
io.use((socket, next) => {
  if (process.env.DISABLE_AUTH === 'true' || config.DISABLE_AUTH === 'true') {
    socket.user = { userId: 'admin_dev_bypass', name: 'Demo Admin', role: 'SUPER_ADMIN' };
    return next();
  }

  const token = socket.handshake.auth?.token ||
    (socket.handshake.headers?.['authorization']?.startsWith('Bearer ')
      ? socket.handshake.headers['authorization'].slice(7)
      : socket.handshake.query?.token);

  if (!token) {
    // Unauthenticated public connection (e.g. public tracking viewer or anonymous observer)
    socket.user = null;
    return next();
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    socket.user = decoded;
    return next();
  } catch (err) {
    logger.warn('Socket connection token verification failed', { socketId: socket.id });
    socket.user = null;
    return next();
  }
});

io.on('connection', async (socket) => {
  const isAuthorizedOfficer = socket.user && ['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER'].includes(socket.user.role);

  if (isAuthorizedOfficer) {
    connectedAdmins.set(socket.id, { id: socket.user.userId, role: socket.user.role, connectedAt: Date.now() });
    socket.join('command_center_officers');
    logger.info('Authorized municipal officer connected to real-time mesh', {
      socketId: socket.id,
      user: socket.user.userId,
      role: socket.user.role,
      total: connectedAdmins.size
    });

    // Send initial confidential municipal data snapshot
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
      logger.warn('Failed to send init snapshot to authorized officer', { error: e.message });
    }
  } else {
    logger.info('Public unauthenticated client connected to public event channel', { socketId: socket.id });
    // Emit only non-confidential macro stats to unauthenticated connections
    db.getDashboardStats().then(stats => {
      socket.emit('stats:public', stats);
    }).catch(() => {});
  }

  // Helper verifying socket role before executing mutating actions
  function requireSocketRole(roles, handler) {
    return async (data) => {
      if (!socket.user || !roles.includes(socket.user.role)) {
        logger.warn('Unauthorized socket action attempted', {
          socketId: socket.id,
          user: socket.user?.userId,
          requiredRoles: roles
        });
        return socket.emit('error', { error: 'Unauthorized: Insufficient role permissions for this operation' });
      }
      return handler(data);
    };
  }

  // ─── Guarded Admin actions ────────────────────────────────────────
  socket.on('sos:acknowledge', requireSocketRole(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER'], async ({ id }) => {
    const updated = await db.updateSos(id, { status: 'acknowledged', acknowledged_at: new Date().toISOString() });
    io.emit('sos:updated', updated);
  }));

  socket.on('sos:resolve', requireSocketRole(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER'], async ({ id }) => {
    const updated = await db.updateSos(id, { status: 'resolved', resolved_at: new Date().toISOString() });
    io.emit('sos:updated', updated);
  }));

  socket.on('incident:verify', requireSocketRole(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER'], async ({ id }) => {
    const updated = await db.updateIncident(id, { status: 'verified' });
    io.emit('incident:updated', updated);
  }));

  socket.on('incident:reject', requireSocketRole(['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER'], async ({ id }) => {
    const updated = await db.updateIncident(id, { status: 'rejected' });
    io.emit('incident:updated', updated);
  }));

  socket.on('disconnect', () => {
    connectedAdmins.delete(socket.id);
    logger.info('Socket disconnected', { socketId: socket.id, remaining: connectedAdmins.size });
  });
});

// ── Supabase Realtime subscription (forward DB changes to all admin clients) ──
db.subscribeToChanges(io);

// ── Start server ──────────────────────────────────────────────────────────────
server.listen(PORT, () => {
  logger.info(`🚀 NagarSetu V2 API Gateway running on port ${PORT}`);
});

module.exports = { app, io };
