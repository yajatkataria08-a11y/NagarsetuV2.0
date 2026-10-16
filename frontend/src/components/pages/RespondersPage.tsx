'use client';

import { Phone, ShieldCheck, MapPin, TrendingUp } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';

type Store = ReturnType<typeof useStore>;

const STATUS_STYLE: Record<string, string> = {
  'on-duty': 'badge-low',
  'busy':    'badge-moderate',
  'off-duty':'badge-pending',
};

export default function RespondersPage({ store }: { store: Store }) {
  const responders = store.responders;
  const onDuty = responders.filter(r => r.status === 'on-duty').length;

  return (
    <div className="h-full overflow-y-auto p-4 space-y-4">
      {/* Quick stats */}
      <div className="grid grid-cols-3 gap-3">
        <div className="kpi-card">
          <p className="kpi-label">On Duty</p>
          <p className="kpi-value text-safe">{onDuty}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">Busy</p>
          <p className="kpi-value text-caution">{responders.filter(r => r.status === 'busy').length}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">Avg Response</p>
          <p className="kpi-value text-cyber">
            {Math.round(responders.reduce((s, r) => s + r.avgResponseMin, 0) / (responders.length || 1))}m
          </p>
        </div>
      </div>

      {/* Responder cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
        {responders.map(r => (
          <div key={r.id} className="glass-card-hover p-4 space-y-3">
            {/* Header */}
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-civic to-cyber/50 flex items-center justify-center text-sm font-bold text-ink flex-shrink-0">
                {r.name.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-ink truncate">{r.name}</p>
                <p className="text-[11px] text-mink">Badge #{r.badge}</p>
              </div>
              <span className={STATUS_STYLE[r.status] ?? 'badge-pending'}>
                {r.status.replace('-', ' ')}
              </span>
            </div>

            {/* Info grid */}
            <div className="grid grid-cols-2 gap-2 text-[11px]">
              <div className="flex items-center gap-1.5 text-mink">
                <MapPin size={11} className="text-faint" />
                <span className="truncate">{r.ward}</span>
              </div>
              <div className="flex items-center gap-1.5 text-mink">
                <Phone size={11} className="text-faint" />
                <span className="truncate">{r.phone}</span>
              </div>
              <div className="flex items-center gap-1.5 text-mink">
                <ShieldCheck size={11} className="text-faint" />
                <span>{r.resolvedCount}/{r.assignedCount} resolved</span>
              </div>
              <div className="flex items-center gap-1.5 text-mink">
                <TrendingUp size={11} className="text-faint" />
                <span>~{r.avgResponseMin}m response</span>
              </div>
            </div>

            {/* Performance bar */}
            <div>
              <div className="flex items-center justify-between text-[10px] text-mink mb-1">
                <span>Resolution rate</span>
                <span className="text-safe font-mono font-bold">
                  {r.assignedCount ? Math.round((r.resolvedCount / r.assignedCount) * 100) : 0}%
                </span>
              </div>
              <div className="h-1 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-safe rounded-full"
                  style={{ width: `${r.assignedCount ? Math.round((r.resolvedCount / r.assignedCount) * 100) : 0}%` }}
                />
              </div>
            </div>

            {/* Dispatch button */}
            {r.status !== 'off-duty' && (
              <button className="btn-ghost w-full justify-center text-[11px] py-1.5">
                <Phone size={11} />
                Dispatch via SMS
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
