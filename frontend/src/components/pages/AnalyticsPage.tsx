'use client';

import { TrendingUp, TrendingDown, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { cn, formatINR } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { RiskLevel } from '@/types';

type Store = ReturnType<typeof useStore>;

const RISK_COLOURS: Record<RiskLevel, string> = {
  CRITICAL: 'text-siren bg-siren/10 border-siren/30',
  HIGH:     'text-siren-orange bg-siren-orange/10 border-siren-orange/30',
  MODERATE: 'text-caution bg-caution/10 border-caution/30',
  LOW:      'text-safe bg-safe/10 border-safe/30',
};

export default function AnalyticsPage({ store }: { store: Store }) {
  const wards = store.wards;

  // Sort by SLA breaches desc
  const sorted = [...wards].sort((a, b) => b.slaBreaches - a.slaBreaches);

  return (
    <div className="h-full overflow-y-auto p-4 space-y-4">
      {/* Overview KPIs */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="Total Complaints" value={wards.reduce((s, w) => s + w.complaintCount, 0)} icon={AlertTriangle} color="text-siren-orange" />
        <StatCard label="Resolved"         value={wards.reduce((s, w) => s + w.resolvedCount, 0)}  icon={CheckCircle2}  color="text-safe" />
        <StatCard label="SLA Breaches"     value={wards.reduce((s, w) => s + w.slaBreaches, 0)}    icon={TrendingDown}  color="text-siren" />
        <StatCard label="Active Officers"  value={wards.reduce((s, w) => s + w.activeOfficers, 0)} icon={TrendingUp}    color="text-cyber" />
      </div>

      {/* Ward table */}
      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
          <h2 className="text-sm font-bold text-ink">Ward-Level Performance</h2>
          <span className="text-[11px] text-mink">{wards.length} wards</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-white/5">
                {['Ward', 'Zone', 'Authority', 'Complaints', 'Resolved %', 'Budget Used', 'SLA Breaches', 'Avg Hrs', 'Risk'].map(h => (
                  <th key={h} className="px-4 py-2.5 text-left text-[11px] font-semibold text-mink uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sorted.map((w, idx) => {
                const resolvedPct = Math.round((w.resolvedCount / w.complaintCount) * 100);
                const budgetUsedPct = Math.round((w.budgetSpent / w.budgetSanctioned) * 100);

                return (
                  <tr
                    key={w.id}
                    className={cn(
                      'border-b border-white/5 transition-colors hover:bg-raised/40',
                      idx % 2 === 0 ? 'bg-transparent' : 'bg-abyss/30'
                    )}
                  >
                    <td className="px-4 py-3 font-medium text-ink">{w.wardName}</td>
                    <td className="px-4 py-3 text-mink">{w.zone}</td>
                    <td className="px-4 py-3 text-mink">{w.authorityName}</td>
                    <td className="px-4 py-3 font-mono text-ink">{w.complaintCount}</td>

                    {/* Resolved % with mini progress bar */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-16 h-1.5 bg-muted rounded-full overflow-hidden">
                          <div
                            className={cn('h-full rounded-full', resolvedPct >= 80 ? 'bg-safe' : resolvedPct >= 60 ? 'bg-caution' : 'bg-siren')}
                            style={{ width: `${resolvedPct}%` }}
                          />
                        </div>
                        <span className={cn('font-mono font-bold', resolvedPct >= 80 ? 'text-safe' : resolvedPct >= 60 ? 'text-caution' : 'text-siren')}>
                          {resolvedPct}%
                        </span>
                      </div>
                    </td>

                    {/* Budget */}
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-0.5">
                        <span className="font-mono text-ink">{formatINR(w.budgetSpent)} / {formatINR(w.budgetSanctioned)}</span>
                        <div className="w-20 h-1 bg-muted rounded-full overflow-hidden">
                          <div
                            className={cn('h-full rounded-full', budgetUsedPct >= 90 ? 'bg-siren' : budgetUsedPct >= 70 ? 'bg-caution' : 'bg-cyber')}
                            style={{ width: `${Math.min(budgetUsedPct, 100)}%` }}
                          />
                        </div>
                      </div>
                    </td>

                    {/* SLA Breaches */}
                    <td className="px-4 py-3">
                      <span className={cn(
                        'font-mono font-bold',
                        w.slaBreaches === 0 ? 'text-safe' : w.slaBreaches <= 3 ? 'text-caution' : 'text-siren'
                      )}>
                        {w.slaBreaches}
                      </span>
                    </td>

                    <td className="px-4 py-3 font-mono text-mink">{w.avgResolutionHrs}h</td>

                    {/* Risk badge */}
                    <td className="px-4 py-3">
                      {w.riskLevel && (
                        <span className={cn('text-[10px] font-bold px-2 py-0.5 rounded-full border', RISK_COLOURS[w.riskLevel])}>
                          {w.riskLevel}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, icon: Icon, color }: { label: string; value: number; icon: React.ElementType; color: string }) {
  return (
    <div className="kpi-card">
      <div className="flex items-center gap-2">
        <Icon size={14} className={cn(color, 'flex-shrink-0')} />
        <p className="kpi-label">{label}</p>
      </div>
      <p className={cn('kpi-value mt-1', color)}>{value}</p>
    </div>
  );
}
