/**
 * Request-body schemas for the public/citizen-facing POST routes.
 *
 * Deliberately LENIENT about shape (numbers may arrive as numeric strings, extra
 * keys are ignored, nulls are tolerated) because the Android and web clients are
 * not uniform - the handlers already coerce with parseFloat/parseInt. What these
 * schemas add is BOUNDS: sizes, ranges and formats that stop abuse such as
 * unbounded SMS fan-out, store bloat, or NaN dates crashing a handler.
 * They validate only; handlers still read the original req.body.
 */
const { z } = require('zod');

const numeric = z.union([z.number(), z.string().trim().regex(/^-?\d+(\.\d+)?$/, 'must be numeric')]);
const numRange = (min, max) => numeric.refine(v => { const n = Number(v); return n >= min && n <= max; }, `must be between ${min} and ${max}`);
const str = max => z.string().max(max);
// 6-20 chars: digits, optional leading +, spaces, dashes, parentheses
const phone = z.string().trim().regex(/^\+?[0-9][0-9\s\-()]{5,19}$/, 'invalid phone number');
const contacts = z.array(phone).max(5, 'at most 5 contacts');

const SosSchema = z.object({
  userId: str(64).nullish(),
  userName: str(100).nullish(),
  phone: str(20).nullish(),
  ward: str(100).nullish(),
  lat: numRange(-90, 90),
  lng: numRange(-180, 180),
  batteryLevel: numRange(0, 100).nullish(),
  heartRate: numRange(0, 300).nullish(),
  contacts: contacts.nullish(),
  sirenType: str(20).nullish(),
});

const IncidentSchema = z.object({
  type: str(40).nullish(),
  description: str(2000).nullish(),
  lat: numRange(-90, 90).nullish(),
  lng: numRange(-180, 180).nullish(),
  address: str(300).nullish(),
  ward: str(100).nullish(),
  severity: numRange(1, 5).nullish(),
  photos: z.array(str(2_000_000)).max(5, 'at most 5 photos').nullish(),
  source: str(40).nullish(),
  reportedBy: str(100).nullish(),
});

const HazardSchema = z.object({
  name: z.string().trim().min(1).max(120),
  lat: numRange(-90, 90),
  lng: numRange(-180, 180),
  radiusMeters: numRange(1, 50000).nullish(),
  riskBoost: numRange(0.1, 10).nullish(),
  durationMinutes: numRange(1, 10080).nullish(),
  note: str(500).nullish(),
});

const TrackingStartSchema = z.object({
  userId: str(64).nullish(),
  userName: str(100).nullish(),
  lat: numRange(-90, 90),
  lng: numRange(-180, 180),
  contacts: contacts.nullish(),
});

const TrackingPingSchema = z.object({
  sessionId: z.string().min(1).max(64),
  lat: numRange(-90, 90),
  lng: numRange(-180, 180),
  batteryLevel: numRange(0, 100).nullish(),
  heartRate: numRange(0, 300).nullish(),
});

module.exports = { SosSchema, IncidentSchema, HazardSchema, TrackingStartSchema, TrackingPingSchema };
