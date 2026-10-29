// ============================================================
// NagarSetu V2 — Shared TypeScript Type Definitions
// ============================================================

export type SeverityLevel = 1 | 2 | 3 | 4 | 5;
export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
export type SosStatus = 'active' | 'acknowledged' | 'dispatched' | 'resolved';
export type IncidentStatus = 'pending' | 'ai_verified' | 'verified' | 'dispatched' | 'resolved' | 'rejected';

export type IncidentType =
  | 'ROAD_HAZARD'
  | 'HARASSMENT'
  | 'FLOOD_RISK'
  | 'POOR_LIGHTING'
  | 'UNSAFE_SPOT'
  | 'SUSPICIOUS_ACTIVITY'
  | 'FIRE'
  | 'MEDICAL'
  | 'POTHOLE'
  | 'SEWAGE'
  | 'WATER_LOGGING'
  | 'GARBAGE'
  | 'CRIME'
  | 'OTHER';

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface SosEvent {
  id: string;
  userId: string;
  userName: string;
  phone: string;
  ward: string;
  location: GeoPoint;
  address?: string;
  batteryLevel: number;
  heartRate?: number;
  status: SosStatus;
  triggeredAt: number;
  acknowledgedAt?: number;
  resolvedAt?: number;
  trackingToken?: string;
  contacts: string[];
  sirenType: 'CRITICAL' | 'HIGH' | 'MODERATE';
}

export interface Incident {
  id: string;
  type: IncidentType;
  description: string;
  location: GeoPoint;
  address?: string;
  ward: string;
  severity: SeverityLevel;
  status: IncidentStatus;
  photos?: string[];
  reportedBy: string;
  reportedAt: number;
  aiScore?: number;
  aiRiskLevel?: RiskLevel;
  aiFeedback?: string;
  assignedOfficer?: string;
  resolvedAt?: number;
}

export interface Responder {
  id: string;
  name: string;
  badge: string;
  ward: string;
  phone: string;
  status: 'on-duty' | 'busy' | 'off-duty';
  location?: GeoPoint;
  assignedCount: number;
  resolvedCount: number;
  avgResponseMin: number;
}

export interface WardStats {
  id: string;
  wardName: string;
  zone: string;
  authorityName: string;
  complaintCount: number;
  resolvedCount: number;
  budgetSanctioned: number;
  budgetSpent: number;
  slaBreaches: number;
  avgResolutionHrs: number;
  activeOfficers: number;
  riskLevel?: RiskLevel;
}

export interface RiskZone {
  lat: number;
  lng: number;
  riskScore: number;
  level: RiskLevel;
  radius?: number;
  incidentCount?: number;
}

export interface DashboardStats {
  activeSos: number;
  pendingIncidents: number;
  resolvedToday: number;
  activeResponders: number;
  avgResponseMin: number;
  slaBreaches: number;
}

export interface HazardZone {
  id: string;
  name: string;
  lat: number;
  lng: number;
  radiusMeters: number;
  riskBoost: number;
  expiresAt: number;
  note?: string;
}

export interface TrackingSession {
  sessionId: string;
  userId: string;
  lat: number;
  lng: number;
  expiresAt: number;
  active: boolean;
}

export interface CitizenUser {
  id: string;
  name: string;
  phone: string;
  ward: string;
  reportsCount: number;
  sosCount: number;
  falseReportsCount: number;
  trustScore: number; // 0 - 100
  status: 'active' | 'flagged' | 'banned';
  joinedAt: number;
}

export interface NotificationItem {
  id: string;
  type: 'sos' | 'incident' | 'alert' | 'system';
  title: string;
  message: string;
  timestamp: number;
  read: boolean;
  priority: 'critical' | 'high' | 'normal';
  targetPage?: string;
}



// ── Infrastructure health ───────────────────────────────────────────────────
export type HealthBand = 'good' | 'watch' | 'critical' | 'unknown';

export interface HealthComponents {
  defect: number | null;
  anomaly: number | null;
  rul: number | null;
}

export interface StructureHealth {
  score: number | null;
  band: HealthBand;
  updated_at: string | null;
  components?: HealthComponents;
  components_used?: string[];
}

export interface StructureSummary {
  id: string;
  name: string;
  type: string;
  ward: string;
  lat: number;
  lng: number;
  fictional?: boolean;
  health: StructureHealth;
}

export interface CrackMeasurement {
  found: boolean;
  length_px: number;
  max_width_px: number;
  mean_width_px: number;
  length_mm: number | null;
  max_width_mm: number | null;
  mean_width_mm: number | null;
  mm_per_px: number | null;
  severity: 'hairline' | 'moderate' | 'severe' | null;
  components: number;
}

export interface RulResult {
  p10_years: number;
  p50_years: number;
  p90_years: number;
  already_critical: boolean;
  a0_m: number;
  a_c_m: number;
  delta_sigma_mpa?: number;
  cycles_per_day?: number;
  assumptions: Record<string, string | number>;
}

export interface InspectionSummary {
  id: string;
  created_at: string;
  inspector?: string;
  detector?: string;
  mask_source?: string;
  crack?: CrackMeasurement;
  severity?: string | null;
  detections: { label: string; confidence: number }[];
  rul: RulResult | null;
  notes: string[];
  image_url: string | null;
}

export interface ReadingRecord {
  id: string;
  created_at: string;
  score: number;
  threshold: number;
  ratio: number;
  flag: boolean;
  data_source?: string;
}

export interface StructureHistory {
  structure_id: string;
  health_history: { score: number | null; band: HealthBand; at: string; source: string }[];
  inspections: InspectionSummary[];
  readings: ReadingRecord[];
}

export interface StructureHealthEvent {
  id: string;
  name: string;
  lat: number;
  lng: number;
  score: number | null;
  band: HealthBand;
  previous_band: HealthBand;
  components: HealthComponents;
  updated_at: string;
  source: string;
}
