/**
 * NagarSetu V2 — Municipal Contractor Accountability & Public Trust Ledger (Phase 13)
 * ───────────────────────────────────────────────────────────────────────────────────
 * Enforces SLA compliance, transparent contractor scoring, and financial penalty deduction
 * for PWD, BMC Water, and Sanitation contracts.
 */

const express = require('express');
const router = express.Router();
const logger = require('../utils/logger');
const { requireAuth } = require('../middleware/auth');
const { sendError } = require('../utils/errors');

// Seed Contractors Roster
let contractors = [
  {
    id: 'CON-01',
    name: 'Suresh Singhania',
    firm_name: 'Shree Ram Infra & Roadways Pvt Ltd',
    ward_id: 'Ward 7 — Arera Colony',
    category: 'PWD_ROAD_MAINTENANCE',
    active_contracts: 4,
    performance_score: 78,
    penalties_imposed_inr: 45000,
    sla_breach_count: 2,
    status: 'active',
    rating: 'B+',
    contact_phone: '+91-9826011223'
  },
  {
    id: 'CON-02',
    name: 'Radheshyam Verma',
    firm_name: 'Narmada Green Clean Services',
    ward_id: 'Ward 9 — Habibganj',
    category: 'BMC_SANITATION',
    active_contracts: 2,
    performance_score: 94,
    penalties_imposed_inr: 0,
    sla_breach_count: 0,
    status: 'active',
    rating: 'A+',
    contact_phone: '+91-9826022334'
  },
  {
    id: 'CON-03',
    name: 'Vikram Joshi',
    firm_name: 'Bhopal Bright Power & Grid Corp',
    ward_id: 'Ward 5 — TT Nagar',
    category: 'ELECTRICITY_BOARD',
    active_contracts: 3,
    performance_score: 84,
    penalties_imposed_inr: 15000,
    sla_breach_count: 1,
    status: 'active',
    rating: 'A',
    contact_phone: '+91-9826033445'
  },
  {
    id: 'CON-04',
    name: 'Manoj Patidar',
    firm_name: 'Malwa Stormwater & Drainage Engineers',
    ward_id: 'Ward 12 — Kolar',
    category: 'BMC_WATER',
    active_contracts: 5,
    performance_score: 62,
    penalties_imposed_inr: 85000,
    sla_breach_count: 4,
    status: 'flagged',
    rating: 'C',
    contact_phone: '+91-9826044556'
  },
  {
    id: 'CON-05',
    name: 'Anil Malviya',
    firm_name: 'SmartCity Civil & Paver Solutions',
    ward_id: 'Ward 3 — MP Nagar',
    category: 'PWD_ROAD_MAINTENANCE',
    active_contracts: 2,
    performance_score: 88,
    penalties_imposed_inr: 10000,
    sla_breach_count: 1,
    status: 'active',
    rating: 'A',
    contact_phone: '+91-9826055667'
  }
];

// Immutable Public Audit Ledger
let auditTrail = [
  {
    id: 'AUD-001',
    contractor_id: 'CON-04',
    firm_name: 'Malwa Stormwater & Drainage Engineers',
    penalty_amount_inr: 25000,
    breach_reason: 'Habibganj Underpass drainage pump deployment delay exceeding 48h SLA during heavy rain',
    officer_badge: 'BMC-COMM-01',
    timestamp: new Date(Date.now() - 48 * 3600000).toISOString(),
    status: 'deducted_from_invoice'
  },
  {
    id: 'AUD-002',
    contractor_id: 'CON-01',
    firm_name: 'Shree Ram Infra & Roadways Pvt Ltd',
    penalty_amount_inr: 20000,
    breach_reason: 'Substandard cold-mix asphalt patch failure on TT Nagar Overbridge within 14 days',
    officer_badge: 'PWD-EE-04',
    timestamp: new Date(Date.now() - 24 * 3600000).toISOString(),
    status: 'deducted_from_invoice'
  }
];

// ── GET /api/contractors ──────────────────────────────────────────────────────
router.get('/', (req, res) => {
  const { status, category } = req.query;
  let result = [...contractors];
  if (status) result = result.filter(c => c.status === status);
  if (category) result = result.filter(c => c.category === category);
  res.json({
    status: 'success',
    total: result.length,
    contractors: result
  });
});

// ── GET /api/contractors/audit-trail ──────────────────────────────────────────
router.get('/audit-trail', (req, res) => {
  res.json({
    status: 'success',
    totalEntries: auditTrail.length,
    auditTrail
  });
});

// ── POST /api/contractors/:id/penalize ────────────────────────────────────────
router.post('/:id/penalize', requireAuth(['SUPER_ADMIN', 'WARD_OFFICER']), (req, res) => {
  try {
    const { id } = req.params;
    const { penaltyAmount, breachReason, officerBadge } = req.body;

    if (!penaltyAmount || !breachReason) {
      return res.status(400).json({ error: 'penaltyAmount and breachReason are required' });
    }

    const contractor = contractors.find(c => c.id.toUpperCase() === id.toUpperCase());
    if (!contractor) {
      return res.status(404).json({ error: 'Contractor not found' });
    }

    const fine = parseFloat(penaltyAmount);
    contractor.penalties_imposed_inr += fine;
    contractor.sla_breach_count += 1;
    contractor.performance_score = Math.max(20, contractor.performance_score - 6);

    // Update rating based on performance score
    if (contractor.performance_score >= 90) contractor.rating = 'A+';
    else if (contractor.performance_score >= 80) contractor.rating = 'A';
    else if (contractor.performance_score >= 70) contractor.rating = 'B';
    else if (contractor.performance_score >= 60) {
      contractor.rating = 'C';
      contractor.status = 'flagged';
    } else {
      contractor.rating = 'D';
      contractor.status = 'suspended';
    }

    const auditEntry = {
      id: `AUD-${Date.now().toString().slice(-4)}`,
      contractor_id: contractor.id,
      firm_name: contractor.firm_name,
      penalty_amount_inr: fine,
      breach_reason: breachReason,
      officer_badge: officerBadge || req.user?.name || 'BMC-AUDIT-CHIEF',
      timestamp: new Date().toISOString(),
      status: 'deducted_from_invoice'
    };

    auditTrail.unshift(auditEntry);

    // Broadcast update via WebSockets
    req.app.get('io')?.emit('contractor:penalized', { contractor, auditEntry });

    logger.info(`[Contractor Accountability] Penalized ${contractor.firm_name} by INR ${fine} for '${breachReason}'`);

    res.json({
      status: 'success',
      contractor,
      auditEntry
    });
  } catch (e) {
    logger.error('Penalize contractor failed', { error: e.message });
    sendError(res, e, 500, 'Failed to penalize contractor');
  }
});

module.exports = router;
