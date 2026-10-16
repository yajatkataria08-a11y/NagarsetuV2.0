'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { Activity, AlertOctagon, CheckCircle2, Users } from 'lucide-react';
import SosCard from '@/components/command/SosCard';
import IncidentCard from '@/components/command/IncidentCard';
import { cn } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';

// Leaflet must be imported client-side only
const TacticalMap = dynamic(() => import('@/components/command/TacticalMap'), { ssr: false });

type Store = ReturnType<typeof useStore>;

// ─── KPI mini-cards ───────────────────────────────────────────────────────────
function KpiBar({ store }: { store: Store }) {
  return (
    <div className="flex-shrink-0 grid grid-cols-4 gap-2 px-3 py-2 border-b border-white/5">
      <KpiItem icon={AlertOctagon} value={store.stats.activeSos} label="Active SOS" accent="text-siren" />
      <KpiItem icon={Activity}     value={store.stats.pendingIncidents} label="Pending" accent="text-siren-orange" />
      <KpiItem icon={CheckCircle2} value={store.stats.resolvedToday}   label="Resolved Today" accent="text-safe" />
      <KpiItem icon={Users}        value={store.stats.activeResponders} label="On Duty" accent="text-cyber" />
    </div>
  );
}

function KpiItem({ icon: Icon, value, label, accent }: { icon: React.ElementType; value: number; label: string; accent: string }) {
  return (
    <div className="glass-card p-2.5 flex items-center gap-2">
      <Icon size={14} className={cn(accent, 'flex-shrink-0')} />
      <div>
        <p className={cn('text-base font-bold font-mono leading-none', accent)}>{value}</p>
        <p className="text-[10px] text-mink mt-0.5">{label}</p>
      </div>
    </div>
  );
}

// ─── Left Panel — SOS Queue ────────────────────────────────────────────────────
function SosPanel({ store, selectedSosId, onSelect }: { store: Store; selectedSosId?: string; onSelect: (id?: string) => void }) {
  const queue = store.sos.filter(s => s.status !== 'resolved').sort((a, b) => {
    // Critical first, then by time
    const pri = { CRITICAL: 3, HIGH: 2, MODERATE: 1 };
    const diff = (pri[b.sirenType] ?? 0) - (pri[a.sirenType] ?? 0);
    return diff !== 0 ? diff : b.triggeredAt - a.triggeredAt;
  });

  return (
    <aside className="w-72 flex-shrink-0 flex flex-col border-r border-white/5 overflow-hidden">
      <div className="flex-shrink-0 flex items-center justify-between px-3 py-2.5 border-b border-white/5">
        <div className="flex items-center gap-2">
          {queue.some(s => s.status === 'active') && <div className="dot-sos" />}
          <span className="text-xs font-bold text-ink">SOS Queue</span>
        </div>
        <span className="text-[10px] font-bold bg-siren/20 text-siren px-1.5 py-0.5 rounded-full">
          {queue.filter(s => s.status === 'active').length} Active
        </span>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-2">
        {queue.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-2 text-faint">
            <CheckCircle2 size={28} />
            <p className="text-xs">No active SOS events</p>
          </div>
        ) : queue.map(s => (
          <SosCard
            key={s.id}
            sos={s}
            isSelected={s.id === selectedSosId}
            onClick={() => onSelect(s.id === selectedSosId ? undefined : s.id)}
            onAcknowledge={store.acknowledgeeSos}
            onResolve={store.resolveSos}
          />
        ))}
      </div>
    </aside>
  );
}

// ─── Right Panel — Incident Triage ────────────────────────────────────────────
function IncidentPanel({ store }: { store: Store }) {
  const active = store.incidents
    .filter(i => i.status !== 'resolved' && i.status !== 'rejected')
    .sort((a, b) => b.severity - a.severity || b.reportedAt - a.reportedAt);

  return (
    <aside className="w-72 flex-shrink-0 flex flex-col border-l border-white/5 overflow-hidden">
      <div className="flex-shrink-0 flex items-center justify-between px-3 py-2.5 border-b border-white/5">
        <span className="text-xs font-bold text-ink">Incident Triage</span>
        <span className="text-[10px] font-bold bg-siren-orange/20 text-siren-orange px-1.5 py-0.5 rounded-full">
          {active.filter(i => i.status === 'pending').length} Pending
        </span>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-2">
        {active.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-2 text-faint">
            <CheckCircle2 size={28} />
            <p className="text-xs">All incidents cleared</p>
          </div>
        ) : active.map(inc => (
          <IncidentCard
            key={inc.id}
            incident={inc}
            isSelected={false}
            onClick={() => {}}
            onVerify={store.verifyIncident}
            onReject={store.rejectIncident}
          />
        ))}
      </div>
    </aside>
  );
}

// ─── Main Command Page ─────────────────────────────────────────────────────────
export default function CommandPage({ store }: { store: Store }) {
  const [selectedSosId, setSelectedSosId] = useState<string | undefined>();

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* KPI Strip */}
      <KpiBar store={store} />

      {/* 3-Panel Zero-Scroll Layout */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left — SOS Priority Queue */}
        <SosPanel store={store} selectedSosId={selectedSosId} onSelect={setSelectedSosId} />

        {/* Centre — Tactical Map */}
        <main className="flex-1 p-2 min-w-0">
          <TacticalMap
            sos={store.sos}
            incidents={store.incidents}
            selectedSosId={selectedSosId}
          />
        </main>

        {/* Right — Incident Triage */}
        <IncidentPanel store={store} />
      </div>
    </div>
  );
}
