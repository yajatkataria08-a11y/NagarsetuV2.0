/**
 * NagarSetu V2 — JWT Authentication Middleware & Demo Auth Route (Phase 10 foundation)
 * ──────────────────────────────────────────────────────────────────────────────────────
 * Roles: SUPER_ADMIN | WARD_OFFICER | DISPATCHER | FIELD_ENGINEER
 *
 * Development shortcut: set DISABLE_AUTH=true in .env to bypass all checks.
 */

const express  = require('express');
const jwt      = require('jsonwebtoken');

const router   = express.Router();

const JWT_SECRET  = process.env.JWT_SECRET || 'nagarsetu_dev_secret_change_in_production';
const VALID_ROLES = ['SUPER_ADMIN', 'WARD_OFFICER', 'DISPATCHER', 'FIELD_ENGINEER'];

// ── generateToken ─────────────────────────────────────────────────────────────
/**
 * Sign a JWT for the given payload.
 * @param {{ userId: string, name: string, role: string }} payload
 * @param {string} [expiresIn='8h']
 * @returns {string}
 */
function generateToken(payload, expiresIn = '8h') {
  return jwt.sign(payload, JWT_SECRET, { expiresIn });
}

// ── requireAuth middleware factory ────────────────────────────────────────────
/**
 * Returns Express middleware that enforces JWT authentication.
 * @param {string[]} [roles] — allowed role values; if omitted, any valid token is accepted
 * @returns {Function} Express middleware
 */
function requireAuth(roles) {
  return (req, res, next) => {
    // Development bypass
    if (process.env.DISABLE_AUTH === 'true') return next();

    const authHeader = req.headers['authorization'] || '';
    const token      = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: 'Authentication required' });
    }

    try {
      const decoded = jwt.verify(token, JWT_SECRET);

      // Attach user to request
      req.user = {
        userId: decoded.userId,
        name:   decoded.name,
        role:   decoded.role,
      };

      // Role check
      if (roles && roles.length > 0 && !roles.includes(req.user.role)) {
        return res.status(403).json({ error: 'Insufficient permissions' });
      }

      next();
    } catch (err) {
      return res.status(401).json({ error: 'Authentication required' });
    }
  };
}

// ── POST /api/auth/login ──────────────────────────────────────────────────────
// Demo authentication for showcase purposes.
// Real production auth should validate against a database.
router.post('/login', (req, res) => {
  const { phone, password } = req.body;

  if (!phone || !password) {
    return res.status(400).json({ error: 'phone and password are required' });
  }

  let payload = null;

  if (password === 'nagarsetu_admin') {
    // Super-admin master password
    payload = { userId: 'admin_001', name: 'NagarSetu Admin', role: 'SUPER_ADMIN' };
  } else if (/^officer_(.+)$/.test(password)) {
    // Ward officer: password is officer_<ward>
    const ward = password.match(/^officer_(.+)$/)[1];
    payload = {
      userId: `officer_${ward}`,
      name:   `Ward Officer (${ward})`,
      role:   'WARD_OFFICER',
      ward,
    };
  } else {
    return res.status(401).json({ error: 'Invalid credentials' });
  }

  const token = generateToken(payload);
  res.json({
    token,
    user:     payload,
    expiresIn: '8h',
  });
});

module.exports        = router;
module.exports.requireAuth   = requireAuth;
module.exports.generateToken = generateToken;
