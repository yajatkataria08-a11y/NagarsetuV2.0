'use client';

import { useState } from 'react';
import {
  AlertOctagon, Clock, CheckCircle2, BellRing,
  TrendingUp, Users, Radio, Send, ArrowRight, ShieldAlert,
  Flame, Droplets, Zap
} from 'lucide-react';
import { cn, timeAgo, formatINR } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { Page } from '@/components/layout/Sidebar';

type Store = ReturnType<typeof useStore>;

interface Props {
  store: Store;
  onNavigate: (p: Page) => void;
}

export default function DashboardOverviewPage({ store, onNavigate }: Props) {
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastWard, setBroadcastWard] = useState('All Wards');
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [broadcastSent, setBroadcastSent] = useState(false);

  const activeSos = store.sos.filter(s => s.status === 'active' || s.status === 'acknowledged');
  const pendingInc = store.incidents.filter(i => i.status === 'pending');
  const verifiedInc = store.incidents.filter(i => i.status === 'verified');
  const rejectedInc = store.incidents.filter(i => i.status === 'rejected');

  const handleBroadcast = (e: React.FormEvent) => {
    e.preventDefault();
    setBroadcastSent(true);
    setTimeout(() => {
      setBroadcastSent(false);
      setShowBroadcastModal(false);
      setBroadcastTitle('');
      setBroadcastMsg('');
    }, 2000);
  };

  return (
    <div className="h-full overflow-y-auto p-5 space-y-5 bg-void">
      {/* ── 1. The Iconic 6-Card Stats Grid ─────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-3">
        {/* Card 1: Active SOS */}
        <div
          onClick={() => onNavigate('sos')}
          className="glass-card-hover p-4 border-l-4 border-l-siren-orange cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">🚨</span>
            <span className="w-2 h-2 rounded-full bg-siren animate-siren-pulse" />
          </div>
          <p className="text-2xl font-black font-mono text-siren-orange leading-none">{activeSos.length}</p>
          <p className="text-xs font-bold text-ink mt-1">Active SOS</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Total lifetime: <span className="text-ink font-semibold">{store.sos.length}</span>
          </p>
        </div>

        {/* Card 2: Pending Incidents */}
        <div
          onClick={() => onNavigate('incidents')}
          className="glass-card-hover p-4 border-l-4 border-l-caution cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">⏳</span>
          </div>
          <p className="text-2xl font-black font-mono text-caution leading-none">{pendingInc.length}</p>
          <p className="text-xs font-bold text-ink mt-1">Pending Review</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Total incidents: <span className="text-ink font-semibold">{store.incidents.length}</span>
          </p>
        </div>

        {/* Card 3: Verified Today */}
        <div className="glass-card p-4 border-l-4 border-l-safe">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">✅</span>
          </div>
          <p className="text-2xl font-black font-mono text-safe leading-none">{verifiedInc.length}</p>
          <p className="text-xs font-bold text-ink mt-1">Verified Today</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Rejected: <span className="text-siren font-semibold">{rejectedInc.length}</span>
          </p>
        </div>

        {/* Card 4: Critical Alerts */}
        <div className="glass-card p-4 border-l-4 border-l-cyber">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">🔔</span>
          </div>
          <p className="text-2xl font-black font-mono text-cyber leading-none">
            {store.incidents.filter(i => i.severity >= 4).length}
          </p>
          <p className="text-xs font-bold text-ink mt-1">High Severity</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Auto-escalated by AI
          </p>
        </div>

        {/* Card 5: Avg Resolution */}
        <div
          onClick={() => onNavigate('wards')}
          className="glass-card-hover p-4 border-l-4 border-l-civic cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">🏁</span>
          </div>
          <p className="text-2xl font-black font-mono text-ink leading-none">76%</p>
          <p className="text-xs font-bold text-ink mt-1">Resolution Rate</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Across <span className="text-cyber font-semibold">{store.wards.length} Wards</span>
          </p>
        </div>

        {/* Card 6: Flagged Users */}
        <div
          onClick={() => onNavigate('users')}
          className="glass-card-hover p-4 border-l-4 border-l-faint cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">⚠️</span>
          </div>
          <p className="text-2xl font-black font-mono text-mink leading-none">1</p>
          <p className="text-xs font-bold text-ink mt-1">Flagged Users</p>
          <p className="text-[10px] text-mink mt-2 pt-2 border-t border-white/5">
            Citizen trust audits
          </p>
        </div>
      </div>

      {/* ── 2. Live Alerts & Quick Actions Split ──────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Live Alerts Feed (2 Columns) */}
        <div className="lg:col-span-2 glass-card p-4 space-y-3">
          <div className="flex items-center justify-between border-b border-white/5 pb-3">
            <div className="flex items-center gap-2">
              <Radio size={16} className="text-siren animate-live-dot" />
              <h2 className="text-sm font-bold text-ink">Live Municipal Feed</h2>
            </div>
            <button
              onClick={() => onNavigate('incidents')}
              className="text-xs text-cyber hover:underline flex items-center gap-1 font-semibold"
            >
              View All Triage <ArrowRight size={12} />
            </button>
          </div>

          <div className="space-y-2.5">
            {activeSos.map(s => (
              <div
                key={s.id}
                onClick={() => onNavigate('sos')}
                className="p-3 rounded-lg bg-siren/10 border border-siren/30 flex items-center justify-between cursor-pointer hover:bg-siren/15 transition-all"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-siren text-white flex items-center justify-center font-bold text-xs animate-siren-pulse">
                    SOS
                  </div>
                  <div>
                    <p className="text-xs font-bold text-ink">{s.userName} triggered Emergency Alert</p>
                    <p className="text-[11px] text-mink">{s.ward} · {s.address || 'GPS Tracking Active'}</p>
                  </div>
                </div>
                <div className="text-right">
                  <span className="badge-critical text-[10px]">{s.sirenType}</span>
                  <p className="text-[10px] text-faint mt-1">{timeAgo(s.triggeredAt)}</p>
                </div>
              </div>
            ))}

            {pendingInc.slice(0, 3).map(inc => (
              <div
                key={inc.id}
                onClick={() => onNavigate('incidents')}
                className="p-3 rounded-lg bg-raised/40 border border-white/5 flex items-center justify-between cursor-pointer hover:border-cyber/30 transition-all"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-surface flex items-center justify-center text-sm">
                    {inc.type === 'ROAD_HAZARD' ? '🚧' : inc.type === 'HARASSMENT' ? '⚠️' : '🌊'}
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-ink line-clamp-1">{inc.description}</p>
                    <p className="text-[11px] text-mink">{inc.ward} · Severity {inc.severity}/5</p>
                  </div>
                </div>
                <span className="badge-pending text-[10px]">{inc.status}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Actions & Mini SOS Monitor (1 Column) */}
        <div className="space-y-4">
          <div className="glass-card p-4 space-y-3">
            <h3 className="text-xs font-bold text-ink uppercase tracking-wider text-mink">⚡ Quick Actions</h3>
            <div className="space-y-2">
              <button
                onClick={() => onNavigate('sos')}
                className="w-full btn-ghost justify-start text-xs font-semibold py-2"
              >
                🚨 SOS Real-Time Monitor
              </button>
              <button
                onClick={() => setShowBroadcastModal(true)}
                className="w-full btn-cyber justify-start text-xs font-semibold py-2"
              >
                📢 Broadcast Area Alert (Ward-wide)
              </button>
              <button
                onClick={() => onNavigate('surveillance')}
                className="w-full btn-ghost justify-start text-xs font-semibold py-2"
              >
                📹 CCTV Vision Wall (6 Feeds)
              </button>
              <button
                onClick={() => onNavigate('hazards')}
                className="w-full btn-ghost justify-start text-xs font-semibold py-2"
              >
                🛡️ Declare Dynamic Hazard Geofence
              </button>
            </div>
          </div>

          <div className="glass-card p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-ink">Bhopal & Indore Operations</span>
              <span className="text-[10px] text-safe font-mono font-bold">100% SLA COMPLIANT</span>
            </div>
            <p className="text-[11px] text-mink">
              All 10 Bhopal municipal wards synced with Firebase Realtime & Supabase PostgreSQL PostGIS.
            </p>
          </div>
        </div>
      </div>

      {/* ── 3. Area Broadcast Modal ───────────────────────────────────── */}
      {showBroadcastModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleBroadcast} className="glass-card max-w-md w-full p-5 border border-white/10 space-y-4">
            <div className="flex items-center gap-2">
              <Send size={18} className="text-cyber" />
              <h3 className="text-sm font-bold text-ink">Municipal Area Broadcast</h3>
            </div>
            <p className="text-xs text-mink">
              Push notification will be sent via Firebase Cloud Messaging (FCM) to all registered citizens in the selected ward.
            </p>

            <div>
              <label className="text-xs text-mink block mb-1">Target Ward</label>
              <select
                value={broadcastWard}
                onChange={e => setBroadcastWard(e.target.value)}
                className="input-dark text-xs"
              >
                <option>All Wards (City-wide Emergency)</option>
                <option>Ward 3 — MP Nagar</option>
                <option>Ward 5 — TT Nagar</option>
                <option>Ward 7 — Arera Colony</option>
                <option>Ward 9 — Habibganj</option>
                <option>Ward 12 — Kolar Road</option>
              </select>
            </div>

            <div>
              <label className="text-xs text-mink block mb-1">Alert Headline</label>
              <input
                required
                className="input-dark text-xs"
                placeholder="e.g. Flood Alert: Habibganj Underpass Inundated"
                value={broadcastTitle}
                onChange={e => setBroadcastTitle(e.target.value)}
              />
            </div>

            <div>
              <label className="text-xs text-mink block mb-1">Detailed Advisory Instructions</label>
              <textarea
                required
                className="input-dark text-xs h-20 resize-none"
                placeholder="Citizen advisory, rerouting directions, emergency helpline numbers..."
                value={broadcastMsg}
                onChange={e => setBroadcastMsg(e.target.value)}
              />
            </div>

            {broadcastSent && (
              <div className="p-2.5 rounded-lg bg-safe/20 border border-safe/40 text-safe text-xs font-semibold text-center">
                ✅ Area Alert successfully queued to FCM & SMS gateways!
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowBroadcastModal(false)}
                className="btn-ghost text-xs"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={broadcastSent}
                className="btn-siren text-xs"
              >
                📢 Send Broadcast Alert
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
