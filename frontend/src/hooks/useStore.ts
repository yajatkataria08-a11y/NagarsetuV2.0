'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { api, incidentsApi, usersApi } from '@/lib/api';
import type {
  SosEvent, Incident, DashboardStats, WardStats,
  Responder, CitizenUser, NotificationItem, SeverityLevel,
  IncidentStatus, SosStatus
} from '@/types';

function normalizeIncident(raw: any): Incident {
  return {
    id: raw.id,
    type: raw.type || 'OTHER',
    description: raw.description || '',
    location: {
      lat: raw.lat ?? raw.location?.lat ?? 23.2599,
      lng: raw.lng ?? raw.location?.lng ?? 77.4126
    },
    address: raw.address || '',
    ward: raw.ward || 'General',
    severity: (raw.severity || 3) as SeverityLevel,
    status: (raw.status || 'pending') as IncidentStatus,
    photos: raw.photos || [],
    reportedBy: raw.reported_by || raw.reportedBy || 'anonymous',
    reportedAt: raw.reported_at ? new Date(raw.reported_at).getTime() : (raw.reportedAt || Date.now()),
    aiScore: raw.ai_score ?? raw.aiScore,
    aiRiskLevel: raw.ai_risk_level ?? raw.aiRiskLevel,
    assignedOfficer: raw.assigned_officer ?? raw.assignedOfficer,
    resolvedAt: raw.resolved_at ? new Date(raw.resolved_at).getTime() : raw.resolvedAt,
  };
}

function normalizeSos(raw: any): SosEvent {
  return {
    id: raw.id,
    userId: raw.user_id || raw.userId || 'usr_anon',
    userName: raw.user_name || raw.userName || 'Citizen',
    phone: raw.phone || '+91-XXXXXXXXXX',
    ward: raw.ward || 'General',
    location: {
      lat: raw.lat ?? raw.location?.lat ?? 23.2599,
      lng: raw.lng ?? raw.location?.lng ?? 77.4126
    },
    address: raw.address,
    batteryLevel: raw.battery_level ?? raw.batteryLevel ?? 80,
    heartRate: raw.heart_rate ?? raw.heartRate,
    status: (raw.status || 'active') as SosStatus,
    triggeredAt: raw.triggered_at ? new Date(raw.triggered_at).getTime() : (raw.triggeredAt || Date.now()),
    acknowledgedAt: raw.acknowledged_at ? new Date(raw.acknowledged_at).getTime() : raw.acknowledgedAt,
    resolvedAt: raw.resolved_at ? new Date(raw.resolved_at).getTime() : raw.resolvedAt,
    trackingToken: raw.tracking_token || raw.trackingToken,
    contacts: raw.contacts || [],
    sirenType: raw.siren_type || raw.sirenType || 'CRITICAL'
  };
}


// ─── Mock seed data for local dev (no backend needed) ─────────────────────────
const MOCK_SOS: SosEvent[] = [
  {
    id: 'sos001',
    userId: 'usr001',
    userName: 'Priya Sharma',
    phone: '+91-9876543210',
    ward: 'Ward 7 — Arera Colony',
    location: { lat: 23.2599, lng: 77.4126 },
    address: 'Near Arera Hills, Bhopal',
    batteryLevel: 34,
    heartRate: 112,
    status: 'active',
    triggeredAt: Date.now() - 3 * 60000,
    contacts: ['+91-9876543220'],
    sirenType: 'CRITICAL',
    trackingToken: 'TRK001',
  },
  {
    id: 'sos002',
    userId: 'usr003',
    userName: 'Ananya Singh',
    phone: '+91-9123456789',
    ward: 'Ward 3 — MP Nagar',
    location: { lat: 23.2310, lng: 77.4350 },
    address: 'Zone-I, MP Nagar, Bhopal',
    batteryLevel: 71,
    heartRate: 89,
    status: 'acknowledged',
    triggeredAt: Date.now() - 12 * 60000,
    contacts: ['+91-9123456790'],
    sirenType: 'HIGH',
    trackingToken: 'TRK002',
  },
];

const MOCK_INCIDENTS: Incident[] = [
  {
    id: 'inc001',
    type: 'ROAD_HAZARD',
    description: 'Large pothole on main road near TT Nagar overbridge, causing accidents',
    location: { lat: 23.2430, lng: 77.4190 },
    address: 'TT Nagar Overbridge',
    ward: 'Ward 5 — TT Nagar',
    severity: 4,
    status: 'pending',
    reportedBy: 'usr002',
    reportedAt: Date.now() - 25 * 60000,
    aiScore: 0.82,
    aiRiskLevel: 'HIGH',
  },
  {
    id: 'inc002',
    type: 'WATER_LOGGING',
    description: 'Severe waterlogging on underpass — 2 feet deep, vehicles stuck',
    location: { lat: 23.2680, lng: 77.4300 },
    address: 'Habibganj Underpass',
    ward: 'Ward 9 — Habibganj',
    severity: 5,
    status: 'ai_verified',
    reportedBy: 'usr004',
    reportedAt: Date.now() - 8 * 60000,
    aiScore: 0.94,
    aiRiskLevel: 'CRITICAL',
  },
  {
    id: 'inc003',
    type: 'HARASSMENT',
    description: 'Suspicious group following women near bus stand',
    location: { lat: 23.2550, lng: 77.4050 },
    address: 'Hamidia Road Bus Stand',
    ward: 'Ward 12 — Kolar',
    severity: 4,
    status: 'pending',
    reportedBy: 'usr005',
    reportedAt: Date.now() - 5 * 60000,
    aiScore: 0.76,
    aiRiskLevel: 'HIGH',
  },
  {
    id: 'inc004',
    type: 'POOR_LIGHTING',
    description: 'Street lights out for 3 days — entire road dark at night',
    location: { lat: 23.2480, lng: 77.4220 },
    address: 'Gulmohar Colony Road 4',
    ward: 'Ward 3 — MP Nagar',
    severity: 2,
    status: 'verified',
    reportedBy: 'usr001',
    reportedAt: Date.now() - 3 * 60 * 60000,
    aiScore: 0.61,
    aiRiskLevel: 'MODERATE',
  },
];

const MOCK_STATS: DashboardStats = {
  activeSos: 2,
  pendingIncidents: 12,
  resolvedToday: 8,
  activeResponders: 7,
  avgResponseMin: 18,
  slaBreaches: 3,
};

const MOCK_WARDS: WardStats[] = [
  { id: 'w1', wardName: 'Ward 3 — MP Nagar', zone: 'Zone A', authorityName: 'Rajesh Kumar', complaintCount: 45, resolvedCount: 38, budgetSanctioned: 2500000, budgetSpent: 1800000, slaBreaches: 2, avgResolutionHrs: 18, activeOfficers: 3, riskLevel: 'MODERATE' },
  { id: 'w2', wardName: 'Ward 7 — Arera Colony', zone: 'Zone B', authorityName: 'Priya Sharma', complaintCount: 62, resolvedCount: 51, budgetSanctioned: 3200000, budgetSpent: 2900000, slaBreaches: 5, avgResolutionHrs: 24, activeOfficers: 5, riskLevel: 'HIGH' },
  { id: 'w3', wardName: 'Ward 9 — Habibganj', zone: 'Zone A', authorityName: 'Amit Singh', complaintCount: 28, resolvedCount: 24, budgetSanctioned: 1800000, budgetSpent: 1200000, slaBreaches: 1, avgResolutionHrs: 12, activeOfficers: 2, riskLevel: 'LOW' },
  { id: 'w4', wardName: 'Ward 12 — Kolar', zone: 'Zone C', authorityName: 'Sunita Verma', complaintCount: 71, resolvedCount: 49, budgetSanctioned: 4100000, budgetSpent: 3600000, slaBreaches: 8, avgResolutionHrs: 36, activeOfficers: 4, riskLevel: 'CRITICAL' },
  { id: 'w5', wardName: 'Ward 5 — TT Nagar', zone: 'Zone A', authorityName: 'Geeta Nair', complaintCount: 55, resolvedCount: 40, budgetSanctioned: 2800000, budgetSpent: 2100000, slaBreaches: 4, avgResolutionHrs: 20, activeOfficers: 4, riskLevel: 'MODERATE' },
];

const MOCK_RESPONDERS: Responder[] = [
  { id: 'off001', name: 'Ravi Kumar', badge: 'MP-041', ward: 'Ward 7', phone: '+91-9001234567', status: 'on-duty', assignedCount: 8, resolvedCount: 7, avgResponseMin: 18 },
  { id: 'off002', name: 'Sunita Kamble', badge: 'MP-038', ward: 'Ward 9', phone: '+91-9001234568', status: 'on-duty', assignedCount: 5, resolvedCount: 5, avgResponseMin: 12 },
  { id: 'off003', name: 'Pradeep Yadav', badge: 'MP-052', ward: 'Ward 12', phone: '+91-9001234569', status: 'busy', assignedCount: 11, resolvedCount: 7, avgResponseMin: 34 },
  { id: 'off004', name: 'Anita Thakur', badge: 'MP-029', ward: 'Ward 3', phone: '+91-9001234570', status: 'on-duty', assignedCount: 4, resolvedCount: 4, avgResponseMin: 9 },
];

const MOCK_USERS: CitizenUser[] = [
  { id: 'usr001', name: 'Priya Sharma', phone: '+91-9876543210', ward: 'Ward 7', reportsCount: 14, sosCount: 1, falseReportsCount: 0, trustScore: 98, status: 'active', joinedAt: Date.now() - 120 * 86400000 },
  { id: 'usr002', name: 'Mohit Agrawal', phone: '+91-9826011223', ward: 'Ward 5', reportsCount: 9, sosCount: 0, falseReportsCount: 0, trustScore: 92, status: 'active', joinedAt: Date.now() - 95 * 86400000 },
  { id: 'usr003', name: 'Ananya Singh', phone: '+91-9123456789', ward: 'Ward 3', reportsCount: 6, sosCount: 1, falseReportsCount: 1, trustScore: 78, status: 'active', joinedAt: Date.now() - 40 * 86400000 },
  { id: 'usr004', name: 'Vikram Chouhan', phone: '+91-9425099881', ward: 'Ward 9', reportsCount: 22, sosCount: 0, falseReportsCount: 0, trustScore: 99, status: 'active', joinedAt: Date.now() - 210 * 86400000 },
  { id: 'usr005', name: 'Rohan Meena', phone: '+91-9893077665', ward: 'Ward 12', reportsCount: 5, sosCount: 0, falseReportsCount: 3, trustScore: 42, status: 'flagged', joinedAt: Date.now() - 15 * 86400000 },
  { id: 'usr006', name: 'Karan Verma', phone: '+91-9755044332', ward: 'Ward 3', reportsCount: 2, sosCount: 0, falseReportsCount: 4, trustScore: 18, status: 'banned', joinedAt: Date.now() - 60 * 86400000 },
];

const MOCK_NOTIFICATIONS: NotificationItem[] = [
  { id: 'notif001', type: 'sos', title: 'Critical SOS Activated', message: 'Priya Sharma triggered high-priority SOS near Arera Hills (Ward 7). Responders alerted.', timestamp: Date.now() - 3 * 60000, read: false, priority: 'critical', targetPage: 'sos' },
  { id: 'notif002', type: 'incident', title: 'Severe Waterlogging Verified', message: 'AI confirmed 2ft deep waterlogging at Habibganj Underpass. Dispatched to Ward 9 team.', timestamp: Date.now() - 8 * 60000, read: false, priority: 'high', targetPage: 'incidents' },
  { id: 'notif003', type: 'alert', title: 'Dynamic Hazard Geofence Active', message: 'Active flood hazard polygon created at Habibganj Underpass with 120m radius.', timestamp: Date.now() - 15 * 60000, read: false, priority: 'high', targetPage: 'hazards' },
  { id: 'notif004', type: 'system', title: 'CCTV Vision Stream Online', message: 'Krisis 6-Camera Vision Wall running with automated YOLOv8 hazard detection.', timestamp: Date.now() - 45 * 60000, read: true, priority: 'normal', targetPage: 'surveillance' },
  { id: 'notif005', type: 'alert', title: 'Citizen Fraud Flag Triggered', message: 'Rohan Meena exceeded false report threshold (3/5 rejected). Account flagged for audit.', timestamp: Date.now() - 120 * 60000, read: true, priority: 'normal', targetPage: 'users' },
];

// ─── Store State ───────────────────────────────────────────────────────────────
interface StoreState {
  sos: SosEvent[];
  incidents: Incident[];
  stats: DashboardStats;
  wards: WardStats[];
  responders: Responder[];
  users: CitizenUser[];
  notifications: NotificationItem[];
  connected: boolean;
  lastSync: number;
}

export function useStore() {
  const [state, setState] = useState<StoreState>({
    sos: MOCK_SOS,
    incidents: MOCK_INCIDENTS,
    stats: MOCK_STATS,
    wards: MOCK_WARDS,
    responders: MOCK_RESPONDERS,
    users: MOCK_USERS,
    notifications: MOCK_NOTIFICATIONS,
    connected: false,
    lastSync: Date.now(),
  });

  const socketRef = useRef<ReturnType<typeof import('socket.io-client').io> | null>(null);

  // ── Initial REST load & Socket.IO sync ─────────────────────────────────────
  useEffect(() => {
    let mounted = true;

    // Load initial data from backend REST API
    const loadInitialData = async () => {
      try {
        const [incRes, sosRes] = await Promise.allSettled([
          api.get<any[]>('/api/incidents'),
          api.get<any[]>('/api/sos'),
        ]);
        if (!mounted) return;
        setState(s => ({
          ...s,
          incidents: incRes.status === 'fulfilled' && incRes.value?.length
            ? incRes.value.map(normalizeIncident)
            : s.incidents,
          sos: sosRes.status === 'fulfilled' && sosRes.value?.length
            ? sosRes.value.map(normalizeSos)
            : s.sos,
          connected: incRes.status === 'fulfilled' || sosRes.status === 'fulfilled',
          lastSync: Date.now(),
        }));
      } catch (_) {}
    };

    loadInitialData();

    const tryConnect = async () => {
      try {
        const { io } = await import('socket.io-client');
        const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4000';
        const socket = io(backendUrl, {
          timeout: 3000,
          reconnectionAttempts: 3,
          query: { adminId: 'admin-v2' },
        });

        socket.on('connect', () => {
          if (!mounted) return;
          setState(s => ({ ...s, connected: true, lastSync: Date.now() }));
        });

        socket.on('init', (data: Partial<StoreState>) => {
          if (!mounted) return;
          setState(s => ({
            ...s,
            sos: data.sos?.length ? data.sos.map(normalizeSos) : s.sos,
            incidents: data.incidents?.length ? data.incidents.map(normalizeIncident) : s.incidents,
            stats: data.stats ?? s.stats,
            wards: data.wards?.length ? data.wards : s.wards,
            responders: data.responders?.length ? data.responders : s.responders,
          }));
        });

        socket.on('new_sos', (sos: any) => {
          if (!mounted) return;
          setState(s => ({ ...s, sos: [normalizeSos(sos), ...s.sos] }));
        });

        socket.on('new_incident', (inc: any) => {
          if (!mounted) return;
          setState(s => ({ ...s, incidents: [normalizeIncident(inc), ...s.incidents] }));
        });

        socket.on('incident:updated', (inc: any) => {
          if (!mounted) return;
          const normalized = normalizeIncident(inc);
          setState(s => ({
            ...s,
            incidents: s.incidents.map(i => i.id === normalized.id ? normalized : i),
          }));
        });

        socket.on('disconnect', () => {
          if (!mounted) return;
          setState(s => ({ ...s, connected: false }));
        });

        socketRef.current = socket;
      } catch {
        console.info('[NagarSetu] Backend offline — using mock data');
      }
    };

    tryConnect();
    return () => {
      mounted = false;
      socketRef.current?.disconnect();
    };
  }, []);

  // ── Derived helpers ──────────────────────────────────────────────────────────
  const activeSos = state.sos.filter(s => s.status === 'active' || s.status === 'acknowledged');
  const criticalIncidents = state.incidents.filter(
    i => i.severity >= 4 && i.status !== 'resolved' && i.status !== 'rejected'
  );

  const resolveSos = useCallback((id: string) => {
    api.patch(`/api/sos/${id}`, { status: 'resolved' }).catch(() => {});
    setState(s => ({
      ...s,
      sos: s.sos.map(e => e.id === id ? { ...e, status: 'resolved', resolvedAt: Date.now() } : e),
    }));
  }, []);

  const acknowledgeeSos = useCallback((id: string) => {
    api.patch(`/api/sos/${id}`, { status: 'acknowledged' }).catch(() => {});
    setState(s => ({
      ...s,
      sos: s.sos.map(e => e.id === id ? { ...e, status: 'acknowledged', acknowledgedAt: Date.now() } : e),
    }));
  }, []);

  const verifyIncident = useCallback((id: string) => {
    incidentsApi.update(id, { status: 'verified' }).catch(() => {});
    setState(s => ({
      ...s,
      incidents: s.incidents.map(i => i.id === id ? { ...i, status: 'verified' } : i),
    }));
  }, []);

  const rejectIncident = useCallback((id: string) => {
    incidentsApi.update(id, { status: 'rejected' }).catch(() => {});
    setState(s => ({
      ...s,
      incidents: s.incidents.map(i => i.id === id ? { ...i, status: 'rejected' } : i),
    }));
  }, []);

  const bulkVerifyIncidents = useCallback((ids: string[]) => {
    ids.forEach(id => incidentsApi.update(id, { status: 'verified' }).catch(() => {}));
    setState(s => ({
      ...s,
      incidents: s.incidents.map(i => ids.includes(i.id) ? { ...i, status: 'verified' } : i),
    }));
  }, []);

  const bulkRejectIncidents = useCallback((ids: string[]) => {
    ids.forEach(id => incidentsApi.update(id, { status: 'rejected' }).catch(() => {}));
    setState(s => ({
      ...s,
      incidents: s.incidents.map(i => ids.includes(i.id) ? { ...i, status: 'rejected' } : i),
    }));
  }, []);

  const bulkAssignIncidents = useCallback((ids: string[], officerName: string) => {
    ids.forEach(id => incidentsApi.update(id, { assignedOfficer: officerName, status: 'dispatched' }).catch(() => {}));
    setState(s => ({
      ...s,
      incidents: s.incidents.map(i => ids.includes(i.id) ? { ...i, assignedOfficer: officerName, status: 'dispatched' } : i),
    }));
  }, []);

  const updateUserStatus = useCallback((id: string, status: CitizenUser['status']) => {
    usersApi.updateStatus(id, status).catch(() => {});
    setState(s => ({
      ...s,
      users: s.users.map(u => u.id === id ? { ...u, status } : u),
    }));
  }, []);

  const markAllNotificationsRead = useCallback(() => {
    setState(s => ({
      ...s,
      notifications: s.notifications.map(n => ({ ...n, read: true })),
    }));
  }, []);

  const markNotificationRead = useCallback((id: string) => {
    setState(s => ({
      ...s,
      notifications: s.notifications.map(n => n.id === id ? { ...n, read: true } : n),
    }));
  }, []);

  const unreadNotifsCount = state.notifications.filter(n => !n.read).length;

  return {
    ...state,
    activeSos,
    criticalIncidents,
    unreadNotifsCount,
    resolveSos,
    acknowledgeeSos,
    verifyIncident,
    rejectIncident,
    bulkVerifyIncidents,
    bulkRejectIncidents,
    bulkAssignIncidents,
    updateUserStatus,
    markAllNotificationsRead,
    markNotificationRead,
  };
}
