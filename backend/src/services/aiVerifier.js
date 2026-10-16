const { spawn } = require('child_process');
const path = require('path');
const logger = require('../utils/logger');

const AI_SCRIPT = path.resolve(__dirname, '../../../ai_engine/unified_civic_ai.py');
// Candidates for Python executable
const PYTHON_BINS = [
  'C:\\Users\\yajat\\AppData\\Local\\Programs\\Python\\Python311\\python.exe',
  'python3',
  'python'
];

/**
 * Verifies incident credibility using Scikit-Learn NLP model + NagarSetu heuristics
 */
async function verifyIncident(incident) {
  return new Promise((resolve) => {
    let pythonPath = PYTHON_BINS[0];
    const payload = JSON.stringify({
      description: incident.description || '',
      severity: incident.severity || 3,
      ward: incident.ward || '',
      timestamp: Date.now()
    });

    const py = spawn(pythonPath, [AI_SCRIPT, 'verify', payload]);
    let output = '';
    let timedOut = false;

    const timer = setTimeout(() => {
      timedOut = true;
      py.kill();
      resolve(fallbackHeuristicVerification(incident));
    }, 2500);

    py.stdout.on('data', (chunk) => {
      output += chunk.toString();
    });

    py.on('close', (code) => {
      clearTimeout(timer);
      if (timedOut) return;

      if (code === 0 && output.trim()) {
        try {
          const res = JSON.parse(output.trim());
          if (res.score != null) {
            return resolve(res);
          }
        } catch (e) {
          logger.warn('Failed to parse AI output, falling back to heuristics', { error: e.message });
        }
      }
      resolve(fallbackHeuristicVerification(incident));
    });

    py.on('error', (err) => {
      clearTimeout(timer);
      logger.warn('Python process spawn failed, using in-memory verification', { error: err.message });
      resolve(fallbackHeuristicVerification(incident));
    });
  });
}

/**
 * Fast in-memory heuristic fallback (0ms)
 */
function fallbackHeuristicVerification(incident) {
  const desc = (incident.description || '').toLowerCase();
  const sev = incident.severity || 3;
  let score = 0.5;
  const tags = [];

  const HIGH_RISK = ['weapon', 'threat', 'attack', 'bleeding', 'fire', 'explosion', 'pothole', 'accident', 'overflow', 'waterlogging', 'unsafe'];
  const NOISE = [/test/i, /demo/i, /^.{0,8}$/, /(.)\1{4,}/];

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
  score = Math.max(0.0, Math.min(1.0, score));

  return {
    score: parseFloat(score.toFixed(3)),
    genuine: score >= 0.45,
    risk_level: score >= 0.8 ? 'HIGH' : score >= 0.5 ? 'MODERATE' : 'LOW',
    recommended_action: score >= 0.8 ? 'auto_verify' : score < 0.25 ? 'auto_reject' : 'manual_review',
    tags
  };
}

module.exports = {
  verifyIncident,
  fallbackHeuristicVerification
};
