/**
 * NagarSetu V2 — BIMSTEC Legal AI Knowledge Base & Fine Resolution Engine
 * ────────────────────────────────────────────────────────────────────────
 * Implements 154 granular traffic violations across 7 BIMSTEC jurisdictions:
 * India (IN), Bangladesh (BD), Bhutan (BT), Myanmar (MM), Nepal (NP), Sri Lanka (LK), Thailand (TH).
 *
 * Features:
 *  - 3-tier fine resolution (Vehicle Multiplier x Repeat Offence Multiplier)
 *  - Currency translation via pegged / market exchange rates
 *  - Conversational NLP matcher for citizen inquiries
 *  - Statutory section citations (Motor Vehicles Act 1988 amended 2019 / BIMSTEC equivalents)
 */

const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const logger = require('../utils/logger');

// Load database of 154 BIMSTEC violations
const DB_PATH = path.join(__dirname, '../../data/bimstec_traffic_code.json');
let legalData = {
  jurisdictions: {},
  vehicleMultipliers: {},
  repeatMultipliers: {},
  violations: []
};

try {
  if (fs.existsSync(DB_PATH)) {
    const raw = fs.readFileSync(DB_PATH, 'utf-8');
    legalData = JSON.parse(raw);
    logger.info(`[BIMSTEC Legal] Loaded ${legalData.violations.length} statutory violations across 7 nations`);
  } else {
    logger.warn(`[BIMSTEC Legal] Warning: Database file not found at ${DB_PATH}`);
  }
} catch (err) {
  logger.error('[BIMSTEC Legal] Failed to parse legal database:', err);
}

// ── GET /api/legal/nations ───────────────────────────────────────────────────
router.get('/nations', (req, res) => {
  return res.json({
    status: 'success',
    totalNations: Object.keys(legalData.jurisdictions).length,
    nations: Object.entries(legalData.jurisdictions).map(([code, data]) => ({
      code,
      ...data
    }))
  });
});

// ── GET /api/legal/categories ─────────────────────────────────────────────────
router.get('/categories', (req, res) => {
  const categoryCounts = {};
  legalData.violations.forEach(v => {
    categoryCounts[v.category] = (categoryCounts[v.category] || 0) + 1;
  });

  const categoryMetadata = {
    SPEEDING:          { label: 'Speeding & Racing', icon: 'Gauge' },
    HELMET:            { label: 'Helmet & Protective Gear', icon: 'HardHat' },
    SEATBELT:          { label: 'Seatbelt & Safety Harness', icon: 'ShieldAlert' },
    SIGNAL:            { label: 'Signal & Junctions', icon: 'TrafficCone' },
    DRUNK_DRIVING:     { label: 'Drunk & Intoxicated Driving', icon: 'Wine' },
    MOBILE_DRIVING:    { label: 'Distracted & Mobile Driving', icon: 'Smartphone' },
    LICENSING:         { label: 'Licensing & Registration', icon: 'FileText' },
    INSURANCE:         { label: 'Insurance & Emissions', icon: 'FileCheck' },
    OVERLOADING:       { label: 'Commercial Overloading', icon: 'Scale' },
    PARKING:           { label: 'Parking & Obstruction', icon: 'CircleParking' },
    LANE_DRIVING:      { label: 'Lane Discipline & Overtaking', icon: 'GitCommit' },
    EMERGENCY_VEHICLE: { label: 'Emergency Vehicle Obstruction', icon: 'Siren' },
    NOISE_POLLUTION:   { label: 'Noise Pollution & Horns', icon: 'VolumeX' },
    PERMITS:           { label: 'Transit Permits & Taxis', icon: 'Bus' },
    HIT_AND_RUN:       { label: 'Accident & Hit and Run', icon: 'CarCrash' },
    EQUIPMENT:         { label: 'Vehicle Equipment & Lamps', icon: 'Wrench' },
    PEDESTRIAN:        { label: 'Pedestrian & Cyclist Rights', icon: 'Footprints' },
    TOLL_BORDER:       { label: 'FASTag, Toll & Border Crossings', icon: 'CreditCard' },
    MISC:              { label: 'Miscellaneous Civic Codes', icon: 'FileSpreadsheet' }
  };

  const categories = Object.entries(categoryCounts).map(([cat, count]) => ({
    key: cat,
    label: categoryMetadata[cat]?.label || cat,
    icon: categoryMetadata[cat]?.icon || 'FileText',
    count
  }));

  return res.json({
    status: 'success',
    totalCategories: categories.length,
    categories
  });
});

// ── GET /api/legal/violations ─────────────────────────────────────────────────
router.get('/violations', (req, res) => {
  const { q, category, nation = 'IN', urgency, page = 1, limit = 50 } = req.query;

  let filtered = [...legalData.violations];

  if (nation && legalData.jurisdictions[nation.toUpperCase()]) {
    filtered = filtered.filter(v => v.applicableCountries.includes(nation.toUpperCase()));
  }

  if (category && category !== 'ALL') {
    filtered = filtered.filter(v => v.category.toUpperCase() === category.toUpperCase());
  }

  if (urgency && urgency !== 'ALL') {
    filtered = filtered.filter(v => v.urgencyLevel.toUpperCase() === urgency.toUpperCase());
  }

  if (q && q.trim().length > 0) {
    const queryTokens = q.toLowerCase().trim().split(/\s+/);
    filtered = filtered.filter(v => {
      const matchText = `${v.title} ${v.section} ${v.category} ${v.statutoryNote} ${v.keywords.join(' ')}`.toLowerCase();
      return queryTokens.every(token => matchText.includes(token));
    });
  }

  const offset = (parseInt(page) - 1) * parseInt(limit);
  const paginated = filtered.slice(offset, offset + parseInt(limit));

  // Augment with country-specific currency conversion
  const nationData = legalData.jurisdictions[nation.toUpperCase()] || legalData.jurisdictions['IN'];
  const exchangeRate = nationData.rate || 1.0;
  const currencySymbol = nationData.symbol || '₹';

  const enriched = paginated.map(v => ({
    ...v,
    convertedBaseFine: Math.round(v.baseFineInr * exchangeRate),
    currency: nationData.currency,
    currencySymbol
  }));

  return res.json({
    status: 'success',
    total: filtered.length,
    page: parseInt(page),
    limit: parseInt(limit),
    totalPages: Math.ceil(filtered.length / parseInt(limit)),
    nation: nationData,
    violations: enriched
  });
});

// ── POST /api/legal/calculate-fine ───────────────────────────────────────────
router.post('/calculate-fine', (req, res) => {
  try {
    const {
      violationId,
      violationIds,
      vehicleType = 'CAR',
      repeatOffence = 1,
      nationCode = 'IN'
    } = req.body;

    const nation = legalData.jurisdictions[nationCode.toUpperCase()] || legalData.jurisdictions['IN'];
    const vehicleMeta = legalData.vehicleMultipliers[vehicleType.toUpperCase()] || legalData.vehicleMultipliers['CAR'];
    const repeatMeta = legalData.repeatMultipliers[String(repeatOffence)] || legalData.repeatMultipliers['1'];

    const targetIds = violationIds && Array.isArray(violationIds) ? violationIds : (violationId ? [violationId] : []);
    if (targetIds.length === 0) {
      return res.status(400).json({ error: 'Please provide violationId or violationIds' });
    }

    const calculatedItems = [];
    let grandTotalInr = 0;
    let totalPenaltyPoints = 0;
    let maxJailMonths = 0;
    let courtRequired = false;

    for (const id of targetIds) {
      const violation = legalData.violations.find(v => v.id.toUpperCase() === id.toUpperCase());
      if (!violation) continue;

      const baseInr = violation.baseFineInr;
      const vMult = vehicleMeta.multiplier;
      const rMult = repeatMeta.multiplier;
      const subtotalInr = Math.round(baseInr * vMult * rMult);

      grandTotalInr += subtotalInr;
      totalPenaltyPoints += violation.penaltyPoints;
      if (violation.jailTermMonths > maxJailMonths) maxJailMonths = violation.jailTermMonths;
      if (violation.mandatoryCourt) courtRequired = true;

      const rate = nation.rate || 1.0;
      const convertedSubtotal = Math.round(subtotalInr * rate);

      calculatedItems.push({
        violationId: violation.id,
        title: violation.title,
        section: violation.section,
        baseFineInr: baseInr,
        vehicleMultiplier: vMult,
        repeatMultiplier: rMult,
        subtotalInr,
        convertedSubtotal,
        penaltyPoints: violation.penaltyPoints,
        jailTermMonths: violation.jailTermMonths,
        mandatoryCourt: violation.mandatoryCourt,
        statutoryNote: violation.statutoryNote,
        formula: `${nation.symbol}${Math.round(baseInr * rate)} × ${vMult} (${vehicleMeta.label}) × ${rMult} (${repeatMeta.label}) = ${nation.symbol}${convertedSubtotal}`
      });
    }

    const finalConvertedTotal = Math.round(grandTotalInr * (nation.rate || 1.0));

    return res.json({
      status: 'success',
      calculation: {
        nation: nation.name,
        nationCode: nationCode.toUpperCase(),
        currency: nation.currency,
        currencySymbol: nation.symbol,
        exchangeRateVsInr: nation.rate,
        vehicleType: vehicleMeta.label,
        offenceStage: repeatMeta.label,
        items: calculatedItems,
        grandTotalInr,
        grandTotal: finalConvertedTotal,
        formattedTotal: `${nation.symbol}${finalConvertedTotal.toLocaleString()}`,
        totalPenaltyPoints,
        maxJailMonths,
        mandatoryCourt: courtRequired || (repeatOffence >= 3),
        statutoryJurisdiction: nation.law,
        emergencyHelpline: nation.emergency,
        trafficHelpline: nation.trafficHelpline,
        demeritWarning: totalPenaltyPoints >= 12
          ? 'CRITICAL DEMERIT WARNING: Accumulation >= 12 points leads to mandatory 1-year driving license suspension under Section 19.'
          : (totalPenaltyPoints >= 6 ? 'WARNING: Demerit points count is elevated.' : 'CLEAR: Below suspension threshold.')
      }
    });
  } catch (err) {
    logger.error('[BIMSTEC Legal] Fine calculation error:', err);
    return res.status(500).json({ error: 'Internal fine calculation failed: ' + err.message });
  }
});

// ── POST /api/legal/query-bot ─────────────────────────────────────────────────
router.post('/query-bot', (req, res) => {
  const { query, nationCode = 'IN', vehicleType = 'CAR' } = req.body;
  if (!query || query.trim().length === 0) {
    return res.status(400).json({ error: 'Query is required' });
  }

  const cleanQuery = query.toLowerCase().trim();
  const tokens = cleanQuery.replace(/[^a-z0-9\u0900-\u097f ]/g, ' ').split(/\s+/).filter(t => t.length > 1);

  // Score each violation by token overlap
  const scored = legalData.violations.map(v => {
    let score = 0;
    const searchSpace = `${v.title} ${v.category} ${v.section} ${v.statutoryNote} ${v.keywords.join(' ')}`.toLowerCase();
    
    // Check key intent patterns
    if (cleanQuery.includes('helmet') && v.category === 'HELMET') score += 10;
    if ((cleanQuery.includes('speed') || cleanQuery.includes('tez')) && v.category === 'SPEEDING') score += 10;
    if ((cleanQuery.includes('red light') || cleanQuery.includes('signal') || cleanQuery.includes('batti')) && v.category === 'SIGNAL') score += 10;
    if ((cleanQuery.includes('drunk') || cleanQuery.includes('sharab') || cleanQuery.includes('alcohol')) && v.category === 'DRUNK_DRIVING') score += 10;
    if ((cleanQuery.includes('seatbelt') || cleanQuery.includes('belt')) && v.category === 'SEATBELT') score += 10;
    if ((cleanQuery.includes('phone') || cleanQuery.includes('mobile')) && v.category === 'MOBILE_DRIVING') score += 10;
    if ((cleanQuery.includes('parking') || cleanQuery.includes('park')) && v.category === 'PARKING') score += 10;
    if ((cleanQuery.includes('overload') || cleanQuery.includes('wazan')) && v.category === 'OVERLOADING') score += 10;
    if ((cleanQuery.includes('ambulance') || cleanQuery.includes('siren')) && v.category === 'EMERGENCY_VEHICLE') score += 10;
    if ((cleanQuery.includes('horn') || cleanQuery.includes('noise')) && v.category === 'NOISE_POLLUTION') score += 10;
    if ((cleanQuery.includes('license') || cleanQuery.includes('licence') || cleanQuery.includes('rc')) && v.category === 'LICENSING') score += 10;

    for (const token of tokens) {
      if (searchSpace.includes(token)) score += 2;
    }

    return { violation: v, score };
  });

  scored.sort((a, b) => b.score - a.score);
  const bestMatch = scored[0] && scored[0].score > 2 ? scored[0].violation : null;

  const nation = legalData.jurisdictions[nationCode.toUpperCase()] || legalData.jurisdictions['IN'];
  const exchangeRate = nation.rate || 1.0;
  const sym = nation.symbol || '₹';

  if (!bestMatch) {
    return res.json({
      status: 'success',
      answer: `I could not locate an exact match for "${query}". Try asking about helmet fines, red light violations, drunk driving, speeding, triple riding, or commercial overloading under ${nation.law}. For urgent traffic assistance, call ${nation.trafficHelpline} or Emergency ${nation.emergency}.`,
      matchedViolation: null,
      suggestions: [
        'Helmet fine kitna hai?',
        'Red light jump penalty',
        'Drunk driving challan',
        'Speeding fine for car',
        'Ambulance rasta na dene ka fine',
        'No parking challan MP Nagar'
      ]
    });
  }

  const baseConverted = Math.round(bestMatch.baseFineInr * exchangeRate);
  const repeatConverted = Math.round(bestMatch.baseFineInr * exchangeRate * 2.0);

  const answer = `In ${nation.name} under ${bestMatch.section}:\n` +
    `• Violation: ${bestMatch.title}\n` +
    `• 1st Offence Fine: ${sym}${baseConverted.toLocaleString()}\n` +
    `• Repeat Offence Fine: ${sym}${repeatConverted.toLocaleString()} (2x multiplier)\n` +
    `• Penalty Points: ${bestMatch.penaltyPoints} points\n` +
    (bestMatch.jailTermMonths > 0 ? `• Imprisonment Risk: Up to ${bestMatch.jailTermMonths} months\n` : '') +
    (bestMatch.mandatoryCourt ? `• Mandatory Court Appearance required\n` : '') +
    `• Statutory Guidance: ${bestMatch.statutoryNote}\n` +
    `Helpline: ${nation.trafficHelpline} | Emergency: ${nation.emergency}`;

  return res.json({
    status: 'success',
    answer,
    matchedViolation: {
      ...bestMatch,
      convertedBaseFine: baseConverted,
      currency: nation.currency,
      currencySymbol: sym
    },
    topSuggestions: scored.slice(1, 4).map(s => s.violation.title)
  });
});

module.exports = router;
