/**
 * NagarSetu V2.0 — Enterprise Authentication & Role-Based Access Control (RBAC)
 * ─────────────────────────────────────────────────────────────────────────────
 * Roles: SUPER_ADMIN | WARD_OFFICER | DISPATCHER | FIELD_ENGINEER | STRUCTURAL_INSPECTOR
 *
 * Provides:
 *  - Secure credential validation with salted cryptographic hashing
 *  - JWT bearer token authentication and role authorization middleware
 *  - Server-side Twilio OTP login for mobile apps (keeps secrets off client APKs)
 *  - Controlled DISABLE_AUTH toggle for air-gapped demo evaluations
 */

const express = require('express');
const jwt     = require('jsonwebtoken');
const crypto  = require('crypto');
const config  = require('../config');
const logger  = require('../utils/logger');
const twilio  = require('../services/twilio');

const router = express.Router();

const JWT_SECRET = process.env.JWT_SECRET || config.JWT_SECRET || 'nagarsetu_dev_secret_change_in_production';
const VALID_ROLES = ['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER', 'STRUCTURAL_INSPECTOR'];

// Helper for salted password hashing
function hashPassword(password, salt) {
  return crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512').toString('hex');
}

// Pre-seeded authoritative administrative accounts with unique salts
const SEED_USERS = [
  {
    phone: '+919876543210',
    name: 'Chief Municipal Commissioner',
    role: 'SUPER_ADMIN',
    salt: 'c4ca4238a0b923820dcc509a6f75849b',
    // Hash of "SuperAdmin@NagarSetu2026!"
    passwordHash: hashPassword('SuperAdmin@NagarSetu2026!', 'c4ca4238a0b923820dcc509a6f75849b'),
    ward: 'All Wards'
  },
  {
    phone: '+919876543211',
    name: 'Rajesh Verma (Ward 12 Officer)',
    role: 'WARD_OFFICER',
    salt: 'c81e728d9d4c2f636f067f89cc14862c',
    // Hash of "WardOfficer@Ward12!"
    passwordHash: hashPassword('WardOfficer@Ward12!', 'c81e728d9d4c2f636f067f89cc14862c'),
    ward: 'Ward 12 — Habibganj'
  },
  {
    phone: '+919876543212',
    name: 'Sunita Patel (HQ Emergency Dispatcher)',
    role: 'DISPATCHER',
    salt: 'eccbc87e4b5ce2fe28308fd9f2a7baf3',
    // Hash of "Dispatcher@HQ2026!"
    passwordHash: hashPassword('Dispatcher@HQ2026!', 'eccbc87e4b5ce2fe28308fd9f2a7baf3'),
    ward: 'Central Dispatch'
  },
  {
    phone: '+919876543213',
    name: 'Ravi Kumar (Field Engineer MP-041)',
    role: 'FIELD_ENGINEER',
    salt: 'a87ff679a2f3e71d9181a67b7542122c',
    // Hash of "FieldEng@MP041!"
    passwordHash: hashPassword('FieldEng@MP041!', 'a87ff679a2f3e71d9181a67b7542122c'),
    ward: 'Ward 7 — Arera Colony'
  },
  {
    phone: '+919876543214',
    name: 'Meera Joshi (Structural Inspector PWD-BR-03)',
    role: 'STRUCTURAL_INSPECTOR',
    salt: 'e4da3b7fbbce2345d7772b0674a318d5',
    // Hash of "Inspector@Infra2026!"
    passwordHash: hashPassword('Inspector@Infra2026!', 'e4da3b7fbbce2345d7772b0674a318d5'),
    ward: 'PWD Bridges Division'
  }
];

// Production safety: the demo accounts above ship with passwords that are public in
// this repository. Override each via SEED_PW_<ROLE> (e.g. SEED_PW_SUPER_ADMIN); the
// server refuses to start in production while any demo account keeps its default.
for (const u of SEED_USERS) {
  const override = process.env[`SEED_PW_${u.role}`];
  if (override) {
    u.passwordHash = hashPassword(override, u.salt);
    u.defaultPassword = false;
  } else {
    u.defaultPassword = true;
  }
}
if (config.isProduction) {
  const stillDefault = SEED_USERS.filter(u => u.defaultPassword).map(u => u.role);
  if (stillDefault.length) {
    throw new Error(`[Config Fatal] Demo account(s) still use the default password in production: ${stillDefault.join(', ')}. Set SEED_PW_<ROLE> for each.`);
  }
}

// In-memory OTP store for phone authentication: Map<phone, { code, expiresAt, attempts }>
const otpStore = new Map();

/**
 * Sign a JWT for the given payload.
 * @param {{ userId: string, name: string, role: string, ward?: string }} payload
 * @param {string} [expiresIn='8h']
 * @returns {string}
 */
function generateToken(payload, expiresIn = '8h') {
  return jwt.sign(payload, JWT_SECRET, { expiresIn });
}

/**
 * Synchronous verification helper.
 */
function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

/**
 * Express middleware factory enforcing JWT authentication and role authorization.
 * @param {string[]} [roles] — allowed roles; if omitted or empty, any valid token is accepted.
 * @returns {Function} Express middleware
 */
function requireAuth(roles) {
  return (req, res, next) => {
    // Demo bypass if explicitly enabled via environment variable
    if (process.env.DISABLE_AUTH === 'true' || config.DISABLE_AUTH === 'true') {
      req.user = req.user || {
        userId: 'admin_dev_bypass',
        name: 'Demo Admin (Auth Bypassed)',
        role: 'SUPER_ADMIN',
        ward: 'All Wards'
      };
      return next();
    }

    const authHeader = req.headers['authorization'] || '';
    const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: 'Unauthorized: Bearer token is required' });
    }

    try {
      const decoded = jwt.verify(token, JWT_SECRET);

      req.user = {
        userId: decoded.userId || decoded.id,
        name:   decoded.name,
        role:   decoded.role,
        ward:   decoded.ward,
        phone:  decoded.phone
      };

      if (roles && roles.length > 0 && !roles.includes(req.user.role)) {
        logger.warn('Access denied: insufficient permissions', {
          user: req.user.userId,
          role: req.user.role,
          required: roles,
          path: req.originalUrl
        });
        return res.status(403).json({
          error: 'Forbidden: Insufficient permissions for this operation',
          requiredRoles: roles,
          currentRole: req.user.role
        });
      }

      next();
    } catch (err) {
      return res.status(401).json({ error: 'Unauthorized: Invalid or expired token' });
    }
  };
}

// ── POST /api/auth/login ──────────────────────────────────────────────────────
router.post('/login', (req, res) => {
  const { phone, password } = req.body;

  if (!phone || !password) {
    return res.status(400).json({ error: 'phone and password are required' });
  }

  const normalizedPhone = String(phone).trim();
  const user = SEED_USERS.find(u => u.phone === normalizedPhone);

  if (!user) {
    return res.status(401).json({ error: 'Invalid phone or password' });
  }

  const computedHash = hashPassword(password, user.salt);
  if (computedHash !== user.passwordHash) {
    return res.status(401).json({ error: 'Invalid phone or password' });
  }

  const payload = {
    userId: `usr_${user.role.toLowerCase()}`,
    name:   user.name,
    role:   user.role,
    ward:   user.ward,
    phone:  user.phone
  };

  const token = generateToken(payload);
  logger.info('User logged in successfully', { phone: user.phone, role: user.role });

  res.json({
    token,
    user: payload,
    expiresIn: '8h'
  });
});

// ── POST /api/auth/send-otp ───────────────────────────────────────────────────
// Server-side OTP dispatch: protects Twilio credentials from exposure in client APKs
router.post('/send-otp', async (req, res) => {
  const { phone } = req.body;
  if (!phone) {
    return res.status(400).json({ error: 'Phone number is required' });
  }

  const normalized = String(phone).replace(/[^\d+]/g, '');
  if (normalized.length < 10) {
    return res.status(400).json({ error: 'Invalid phone number format' });
  }

  // Generate 6-digit cryptographically secure OTP
  const code = Math.floor(100000 + crypto.randomInt(900000)).toString();
  const expiresAt = Date.now() + 10 * 60 * 1000; // 10 minutes TTL

  otpStore.set(normalized, { code, expiresAt, attempts: 0 });

  logger.info(`[Auth OTP] Generated OTP for ${normalized}`);

  // Send via Twilio if configured
  if (process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN) {
    try {
      await twilio.sendSms(
        normalized,
        `[NagarSetu] Your verification code is ${code}. Valid for 10 minutes. Do not share this code.`
      );
    } catch (e) {
      logger.warn('[Auth OTP] Twilio SMS dispatch error:', { error: e.message });
    }
  }

  res.json({
    success: true,
    message: 'OTP dispatched successfully',
    // In non-production or demo, surface code for testing convenience
    ...(process.env.NODE_ENV !== 'production' ? { devOtpCode: code } : {})
  });
});

// ── POST /api/auth/verify-otp ─────────────────────────────────────────────────
router.post('/verify-otp', (req, res) => {
  const { phone, code } = req.body;
  if (!phone || !code) {
    return res.status(400).json({ error: 'Phone and code are required' });
  }

  const normalized = String(phone).replace(/[^\d+]/g, '');
  const record = otpStore.get(normalized);

  if (!record) {
    return res.status(400).json({ error: 'No active OTP request found for this phone' });
  }

  if (Date.now() > record.expiresAt) {
    otpStore.delete(normalized);
    return res.status(400).json({ error: 'OTP has expired. Please request a new code.' });
  }

  record.attempts++;
  if (record.attempts > 5) {
    otpStore.delete(normalized);
    return res.status(429).json({ error: 'Too many incorrect attempts. Request a new OTP.' });
  }

  if (record.code !== String(code).trim()) {
    return res.status(400).json({ error: 'Incorrect verification code' });
  }

  // Verification successful — cleanup OTP
  otpStore.delete(normalized);

  // Check if known administrative phone, otherwise assign CITIZEN role
  const existingUser = SEED_USERS.find(u => u.phone === normalized);
  const payload = existingUser ? {
    userId: `usr_${existingUser.role.toLowerCase()}`,
    name: existingUser.name,
    role: existingUser.role,
    ward: existingUser.ward,
    phone: existingUser.phone
  } : {
    userId: `citizen_${crypto.randomBytes(4).toString('hex')}`,
    name: 'Verified Citizen',
    role: 'CITIZEN',
    phone: normalized
  };

  const token = generateToken(payload);
  logger.info('[Auth OTP] Phone verified successfully', { phone: normalized, role: payload.role });

  res.json({
    success: true,
    token,
    user: payload,
    expiresIn: '8h'
  });
});

// ── GET /api/auth/me ──────────────────────────────────────────────────────────
router.get('/me', requireAuth(), (req, res) => {
  res.json({ user: req.user });
});

module.exports = router;
module.exports.requireAuth   = requireAuth;
module.exports.generateToken = generateToken;
module.exports.verifyToken   = verifyToken;
