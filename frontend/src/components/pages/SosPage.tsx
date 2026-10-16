'use client';

import { useState } from 'react';
import {
  AlertOctagon, CheckCircle2, Phone, ExternalLink,
  Cpu, Battery, Clock, MapPin, RefreshCw, Send, ShieldAlert
} from 'lucide-react';
import { cn, timeAgo } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { SosEvent, SosStatus } from '@/types';

type Store = ReturnType<typeof useStore>;

export default function SosPage({ store }: { store: Store }) {
  const [filter, setFilter] = useState<'all' | SosStatus>('all');
  const [dispatchingId, setDispatchingId] = useState<string | null>(null);

  const filtered = store.sos.filter(s => {
    if (filter === 'all') return true;
    return s.status === filter;
  }).sort((a, b) => {
    const pri = { CRITICAL: 3, HIGH: 2, MODERATE: 1 };
    const diff = (pri[b.sirenType] ?? 0) - (pri[a.sirenType] ?? 0);
    return diff !== 0 ? diff : b.triggeredAt - a.triggeredAt;
  });

  const activeCount = store.sos.filter(s => s.status === 'active').length;

  const handleDispatch = (id: string) => {
    setDispatchingId(id);
    setTimeout(() => {
      store.acknowledgeeSos(id);
      setDispatchingId(null);
    }, 1500);
  };

  return (
    <div className="h-full overflow-y-auto p-5 space-y-4 bg-void">
      {/* ── Glowing SOS Alert Banner ─────────────────────────────────── */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-siren/15 via-siren-orange/10 to-transparent border border-siren/30 flex items-center justify-between shadow-glow-siren/20">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-siren flex items-center justify-center text-white text-2xl font-black shadow-glow-siren animate-siren-pulse">
            🚨
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-black font-mono text-siren-orange">{activeCount}</span>
              <span className="text-base font-bold text-ink">Active SOS Emergencies</span>
            </div>
            <p className="text-xs text-mink">Real-time triage active across Bhopal & Indore municipal bounds</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-mono text-siren font-bold animate-pulse">MONITORING ACTIVE</span>
        </div>
      </div>

      {/* ── Filters & Controls ───────────────────────────────────────── */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-1.5">
          {[
            { id: 'all',          label: 'All SOS' },
            { id: 'active',       label: '🔴 Active' },
            { id: 'acknowledged', label: '🟡 Acknowledged' },
            { id: 'resolved',     label: '🟢 Resolved' },
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setFilter(tab.id as any)}
              className={cn(
                'px-3 py-1.5 rounded-lg text-xs font-semibold transition-all',
                filter === tab.id
                  ? 'bg-cyber/20 text-cyber border border-cyber/40'
                  : 'bg-raised/60 text-mink hover:text-ink hover:bg-muted'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <span className="text-xs text-mink">Showing {filtered.length} SOS events</span>
      </div>

      {/* ── SOS Cards Grid ──────────────────────────────────────────── */}
      {filtered.length === 0 ? (
        <div className="glass-card p-12 text-center text-faint space-y-2">
          <CheckCircle2 size={36} className="mx-auto text-safe" />
          <p className="text-sm font-semibold text-ink">No SOS events in this status</p>
          <p className="text-xs text-mink">Emergency channels are continuously monitoring.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(sos => {
            const isActive = sos.status === 'active';
            const isAcked  = sos.status === 'acknowledged';

            return (
              <div
                key={sos.id}
                className={cn(
                  'glass-card p-4 relative overflow-hidden border space-y-3 transition-all',
                  isActive ? 'border-siren/60 shadow-glow-siren/30' : 'border-white/5'
                )}
              >
                {/* Header */}
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className={cn(
                      'w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold text-white',
                      isActive ? 'bg-siren animate-siren-pulse' : 'bg-surface border border-white/10'
                    )}>
                      {sos.userName.charAt(0)}
                    </div>
                    <div>
                      <p className="text-sm font-bold text-ink">{sos.userName}</p>
                      <p className="text-xs text-mink">{sos.phone}</p>
                    </div>
                  </div>

                  <span className={cn(
                    'text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider',
                    sos.sirenType === 'CRITICAL' ? 'badge-critical' :
                    sos.sirenType === 'HIGH'     ? 'badge-high' : 'badge-moderate'
                  )}>
                    {sos.sirenType}
                  </span>
                </div>

                {/* Location & Ward */}
                <div className="bg-abyss/60 p-2.5 rounded-lg border border-white/5 space-y-1 text-xs">
                  <div className="flex items-center gap-1.5 text-ink font-medium">
                    <MapPin size={13} className="text-cyber flex-shrink-0" />
                    <span className="truncate">{sos.ward}</span>
                  </div>
                  <p className="text-[11px] text-mink pl-4 truncate">{sos.address || 'GPS Coordinates Broadcasted'}</p>
                </div>

                {/* Telemetry Strip */}
                <div className="grid grid-cols-3 gap-2 text-[11px]">
                  <div className="glass-card p-2 text-center">
                    <p className="text-faint text-[9px] uppercase font-bold">Battery</p>
                    <p className={cn('font-mono font-bold mt-0.5', sos.batteryLevel < 20 ? 'text-siren' : 'text-safe')}>
                      🔋 {sos.batteryLevel}%
                    </p>
                  </div>
                  <div className="glass-card p-2 text-center">
                    <p className="text-faint text-[9px] uppercase font-bold">Heart Rate</p>
                    <p className="font-mono font-bold text-siren mt-0.5">
                      ❤️ {sos.heartRate || 108} BPM
                    </p>
                  </div>
                  <div className="glass-card p-2 text-center">
                    <p className="text-faint text-[9px] uppercase font-bold">Elapsed</p>
                    <p className="font-mono font-semibold text-mink mt-0.5">
                      {timeAgo(sos.triggeredAt)}
                    </p>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-2 pt-1 border-t border-white/5">
                  {sos.trackingToken && (
                    <a
                      href={`/track/${sos.trackingToken}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn-ghost text-xs py-1.5 flex-1 justify-center"
                    >
                      <ExternalLink size={12} /> Live Link
                    </a>
                  )}

                  {isActive && (
                    <button
                      onClick={() => handleDispatch(sos.id)}
                      disabled={dispatchingId === sos.id}
                      className="btn-cyber text-xs py-1.5 flex-1 justify-center"
                    >
                      {dispatchingId === sos.id ? 'Dispatching…' : '👮 Dispatch'}
                    </button>
                  )}

                  {(isActive || isAcked) && (
                    <button
                      onClick={() => store.resolveSos(sos.id)}
                      className="btn-safe text-xs py-1.5 px-3"
                    >
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
