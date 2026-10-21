/**
 * NagarSetu V2 — AI Verification Service
 * ────────────────────────────────────────
 * Three-tier resolution strategy (fastest → most resilient):
 *   1. HTTP microservice  → config.AI_SERVICE_URL/verify   (300 ms timeout)
 *   2. Python child_process spawn via config.PYTHON_BIN    (2500 ms timeout)
 *   3. In-process heuristic fallback                       (0 ms, always works)
 */

const { spawn }  = require('child_process');
const http       = require('http');
const logger     = require('../utils/logger');
const config     = require('../config');

/**
 * Attempt to call the AI microservice over HTTP.
 * Resolves with parsed JSON or rejects on timeout / non-200 / parse error.
 * @param {object} payload
 * @returns {Promise<object>}
 */
function httpVerify(payload) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(payload);
    const url  = new URL(`${config.AI_SERVICE_URL}/verify`);

    const options = {
      hostname: url.hostname,
      port:     url.port || 80,
      path:     url.pathname,
      method:   'POST',
      headers:  { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) },
      timeout:  300,
    };

    const req = http.request(options, (res) => {
      let raw = '';
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(raw);
          if (json.score != null) return resolve(json);
          reject(new Error('Missing score field'));
        } catch (e) { reject(e); }
      });
    });

    req.on('timeout', () => { req.destroy(); reject(new Error('HTTP timeout')); });
    req.on('error',   reject);
    req.write(body);
    req.end();
  });
}

/**
 * Verifies incident credibility using Scikit-Learn NLP model + NagarSetu heuristics.
 * @param {object} incident
 * @returns {Promise<object>}
 */
async function verifyIncident(incident) {
  const payload = {
    description: incident.description || '',
    severity:    incident.severity    || 3,
    ward:        incident.ward        || '',
    timestamp:   Date.now(),
  };

  // ── Tier 1: HTTP microservice ──────────────────────────────────────────────
  try {
    const result = await httpVerify(payload);
    logger.info('[AI] Verified via HTTP microservice');
    return result;
  } catch (httpErr) {
    logger.debug('[AI] HTTP microservice unavailable, falling back to spawn', { reason: httpErr.message });
  }

  // ── Tier 2: Python child_process ───────────────────────────────────────────
  return new Promise((resolve) => {
    const jsonPayload = JSON.stringify(payload);
    const py = spawn(config.PYTHON_BIN, [config.AI_SCRIPT, 'verify', jsonPayload]);
    let output   = '';
    let timedOut = false;

    const timer = setTimeout(() => {
      timedOut = true;
      py.kill();
      resolve(fallbackHeuristicVerification(incident));
    }, 2500);

    py.stdout.on('data', (chunk) => { output += chunk.toString(); });

    py.on('close', (code) => {
      clearTimeout(timer);
      if (timedOut) return;

      if (code === 0 && output.trim()) {
        try {
          const res = JSON.parse(output.trim());
          if (res.score != null) return resolve(res);
        } catch (e) {
          logger.warn('[AI] Failed to parse Python output, using heuristics', { error: e.message });
        }
      }
      resolve(fallbackHeuristicVerification(incident));
    });

    py.on('error', (err) => {
      clearTimeout(timer);
      logger.warn('[AI] Python spawn failed, using in-memory verification', { error: err.message });
      resolve(fallbackHeuristicVerification(incident));
    });
  });
}

/**
 * Fast in-memory heuristic fallback (0 ms latency)
 * @param {object} incident
 * @returns {object}
 */
function fallbackHeuristicVerification(incident) {
  const desc = (incident.description || '').toLowerCase();
  const sev  = incident.severity || 3;
  let score  = 0.5;
  const tags = [];

  const HIGH_RISK = ['weapon', 'threat', 'attack', 'bleeding', 'fire', 'explosion', 'pothole', 'accident', 'overflow', 'waterlogging', 'unsafe'];
  const NOISE     = [/test/i, /demo/i, /^.{0,8}$/, /(.)\\1{4,}/];

  for (const pat of NOISE) {
    if (pat.test(desc)) {
      score -= 0.35;
      tags.push('noise-detected');
      break;
    }
  }

  for (const kw of HIGH_RISK) {
    if (desc.includes(kw)) {
      score += 0.08;
      tags.push('high-risk');
    }
  }

  score += ((sev - 1) / 4.0) * 0.2;
  score  = Math.max(0.0, Math.min(1.0, score));

  return {
    score:              parseFloat(score.toFixed(3)),
    genuine:            score >= 0.45,
    risk_level:         score >= 0.8 ? 'HIGH' : score >= 0.5 ? 'MODERATE' : 'LOW',
    recommended_action: score >= 0.8 ? 'auto_verify' : score < 0.25 ? 'auto_reject' : 'manual_review',
    tags,
  };
}

module.exports = { verifyIncident, fallbackHeuristicVerification };
