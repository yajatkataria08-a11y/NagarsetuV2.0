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
