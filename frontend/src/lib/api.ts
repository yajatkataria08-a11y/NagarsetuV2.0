/**
 * NagarSetu V2.0 — Centralized API Client
 * Handles all HTTP communication with the backend API Gateway at :4000
 * Uses environment variable NEXT_PUBLIC_API_URL for cross-environment support
 */

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4000';

export async function apiFetch<T = unknown>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err?.error || `API error ${res.status}`);
  }
  return res.json() as T;
}

export const api = {
  get: <T = unknown>(path: string) => apiFetch<T>(path),
  post: <T = unknown>(path: string, body: unknown) =>
    apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  patch: <T = unknown>(path: string, body: unknown) =>
    apiFetch<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: <T = unknown>(path: string) => apiFetch<T>(path, { method: 'DELETE' }),
};

// ── Domain-specific API helpers ──────────────────────────────────────────────

export const hazardsApi = {
  list: () => api.get<HazardZoneRaw[]>('/api/hazards'),
  create: (body: {
    name: string; lat: number; lng: number;
    radiusMeters: number; riskBoost: number; durationMinutes: number; note?: string;
  }) => api.post<HazardZoneRaw>('/api/hazards', body),
  delete: (id: string) => api.delete(`/api/hazards/${id}`),
};

export const cctvApi = {
  list: () => api.get<CctvCamera[]>('/api/cctv'),
  detect: (id: string, body: { threat: string; confidence: number; detections: Detection[] }) =>
    api.post(`/api/cctv/${id}/detect`, body),
};

export const legalApi = {
  nations: () => api.get<{ status: string; totalNations: number; nations: any[] }>('/api/legal/nations'),
  categories: () => api.get<{ status: string; totalCategories: number; categories: any[] }>('/api/legal/categories'),
  violations: (params?: { q?: string; category?: string; nation?: string; urgency?: string; page?: number; limit?: number }) => {
    const query = new URLSearchParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') query.set(k, String(v));
      });
    }
    return api.get<{ status: string; total: number; page: number; limit: number; totalPages: number; violations: any[]; nation: any }>(`/api/legal/violations?${query.toString()}`);
  },
  calculateFine: (body: { violationId?: string; violationIds?: string[]; vehicleType?: string; repeatOffence?: number; nationCode?: string }) =>
    api.post<{ status: string; calculation: any }>('/api/legal/calculate-fine', body),
  queryBot: (body: { query: string; nationCode?: string; vehicleType?: string }) =>
    api.post<{ status: string; answer: string; matchedViolation: any; topSuggestions?: string[]; suggestions?: string[] }>('/api/legal/query-bot', body),
};

export const contractorApi = {
  list: (status?: string) => api.get<{ status: string; total: number; contractors: any[] }>(`/api/contractors${status ? `?status=${status}` : ''}`),
  auditTrail: () => api.get<{ status: string; totalEntries: number; auditTrail: any[] }>('/api/contractors/audit-trail'),
  penalize: (id: string, body: { penaltyAmount: number; breachReason: string; officerBadge?: string }) =>
    api.post<{ status: string; contractor: any; auditEntry: any }>(`/api/contractors/${id}/penalize`, body),
};

export const riskApi = {
  heatmap: (city: 'bhopal' | 'indore') => api.get<RiskHeatmap>(`/api/risk/heatmap?city=${city}`),
  point: (body: { lat: number; lng: number; hour?: number; day?: number }) =>
    api.post<RiskPoint>('/api/risk/point', body),
};

export const usersApi = {
  list: (status?: string) => api.get<CitizenUserRaw[]>(`/api/users${status ? `?status=${status}` : ''}`),
  updateStatus: (id: string, status: 'active' | 'flagged' | 'banned') =>
    api.patch(`/api/users/${id}/status`, { status }),
};

export const incidentsApi = {
  list: (params?: { status?: string; ward?: string; severity?: number }) => {
    const q = new URLSearchParams(
      Object.entries(params || {}).filter(([, v]) => v != null).map(([k, v]) => [k, String(v)])
    ).toString();
    return api.get<IncidentRaw[]>(`/api/incidents${q ? `?${q}` : ''}`);
  },
  update: (id: string, patch: { status?: string; assignedOfficer?: string }) =>
    api.patch<IncidentRaw>(`/api/incidents/${id}`, patch),
};

export const analyticsApi = {
  stats: () => api.get<StatsRaw>('/api/stats'),
  wards: () => api.get<WardRaw[]>('/api/wards'),
};

// ── Raw API types (what the backend returns) ─────────────────────────────────

export interface HazardZoneRaw {
  id: string;
  name: string;
  lat?: number; center_lat?: number;
  lng?: number; center_lng?: number;
  radius_meters: number;
  risk_boost: number;
  expires_at: string;
  note?: string;
  active?: boolean;
}

export interface CctvCamera {
  id: string;
  name: string;
  ward: string;
  lat: number;
  lng: number;
  feed_url: string;
  status: 'online' | 'alert' | 'offline';
  fps: number;
  resolution: string;
  active_threat?: string;
  threat_confidence: number;
  detections?: Detection[];
}

export interface Detection {
  label: string;
  confidence: number;
  bbox?: [number, number, number, number];
}

export interface RiskHeatmap {
  city: string;
  zones: Array<{ lat: number; lng: number; riskScore: number; incidentCount: number; level: string }>;
  totalZones: number;
  generatedAt: number;
}

export interface RiskPoint {
  lat: number; lng: number;
  risk_score: number; risk_level: string; model: string;
}

export interface CitizenUserRaw {
  id: string;
  name: string;
  phone: string;
  ward: string;
  trust_score: number;
  status: 'active' | 'flagged' | 'banned';
  role: string;
  reports: number;
  created_at?: string;
}

export interface IncidentRaw {
  id: string;
  type: string;
  description: string;
  lat: number; lng: number;
  ward: string;
  severity: number;
  status: string;
  ai_score?: number;
  ai_risk_level?: string;
  assigned_officer?: string;
  reported_at?: string;
  photos?: string[];
}

export interface StatsRaw {
  activeSos: number;
  pendingIncidents: number;
  resolvedToday: number;
  activeResponders: number;
  slaBreaches: number;
  budgetSpent?: number;
}

export interface WardRaw {
  id: string;
  name: string;
  officer: string;
  pending: number;
  resolved: number;
  sla_breaches?: number;
  budget_total?: number;
  budget_spent?: number;
  resolution_rate?: number;
}
