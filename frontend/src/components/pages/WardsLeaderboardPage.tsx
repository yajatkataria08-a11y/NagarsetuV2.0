'use client';

import { useState } from 'react';
import {
  Building2, Users, Trophy, AlertTriangle, TrendingUp,
  Clock, ShieldCheck, DollarSign
} from 'lucide-react';
import { cn, formatINR } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';

type Store = ReturnType<typeof useStore>;

export default function WardsLeaderboardPage({ store }: { store: Store }) {
  const [filterZone, setFilterZone] = useState('all');

  const wards = store.wards.filter(w =>
    filterZone === 'all' ? true : w.zone === filterZone
  ).sort((a, b) => {
    // Rank by resolution rate descending
    const rateA = a.resolvedCount / a.complaintCount;
    const rateB = b.resolvedCount / b.complaintCount;
    return rateB - rateA;
  });

  const totalSanctioned = store.wards.reduce((acc, w) => acc + w.budgetSanctioned, 0);
  const totalSpent = store.wards.reduce((acc, w) => acc + w.budgetSpent, 0);
  const totalBreaches = store.wards.reduce((acc, w) => acc + w.slaBreaches, 0);
  const totalComplaints = store.wards.reduce((acc, w) => acc + w.complaintCount, 0);
  const totalResolved = store.wards.reduce((acc, w) => acc + w.resolvedCount, 0);
  const avgOverallRate = Math.round((totalResolved / totalComplaints) * 100);

  return (
    <div className="h-full overflow-y-auto p-5 space-y-5 bg-void">
      {/* ── 1. Municipal Ward KPIs ────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="glass-card p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">🏛️</span>
            <span className="text-[10px] font-bold text-safe uppercase bg-safe/10 px-2 py-0.5 rounded border border-safe/20">Overall</span>
          </div>
          <p className="text-2xl font-mono font-black text-safe">{avgOverallRate}%</p>
          <p className="text-xs font-bold text-ink mt-1">Resolution Rate</p>
          <p className="text-[10px] text-mink mt-1">{totalResolved} of {totalComplaints} resolved</p>
        </div>

        <div className="glass-card p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">⚠️</span>
            <span className="text-[10px] font-bold text-siren uppercase bg-siren/10 px-2 py-0.5 rounded border border-siren/20">Action Req</span>
          </div>
          <p className="text-2xl font-mono font-black text-siren">{totalBreaches}</p>
          <p className="text-xs font-bold text-ink mt-1">SLA Breaches</p>
          <p className="text-[10px] text-mink mt-1">Exceeded 48h municipal window</p>
        </div>

        <div className="glass-card p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">💰</span>
            <span className="text-[10px] font-bold text-cyber uppercase bg-cyber/10 px-2 py-0.5 rounded border border-cyber/20">Municipal Fund</span>
          </div>
          <p className="text-2xl font-mono font-black text-cyber">{formatINR(totalSanctioned)}</p>
          <p className="text-xs font-bold text-ink mt-1">Sanctioned Budget</p>
          <p className="text-[10px] text-mink mt-1">Spent: {formatINR(totalSpent)} ({Math.round((totalSpent / totalSanctioned) * 100)}%)</p>
        </div>

        <div className="glass-card p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xl">👮</span>
            <span className="text-[10px] font-bold text-cyber uppercase bg-cyber/10 px-2 py-0.5 rounded border border-cyber/20">Active Roster</span>
          </div>
          <p className="text-2xl font-mono font-black text-ink">{store.responders.length}</p>
          <p className="text-xs font-bold text-ink mt-1">Field Officers</p>
          <p className="text-[10px] text-mink mt-1">Covering Bhopal & Indore Zones</p>
        </div>
      </div>

      {/* ── 2. Two-Column Leaderboard Tables ──────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Ward Performance Table */}
        <div className="glass-card flex flex-col overflow-hidden">
          <div className="p-4 border-b border-white/5 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Trophy size={16} className="text-caution" />
              <div>
                <h3 className="text-sm font-bold text-ink">Ward Leaderboard</h3>
                <p className="text-[10px] text-mink">Ranked by complaint resolution efficiency</p>
              </div>
            </div>
            <select
              value={filterZone}
              onChange={e => setFilterZone(e.target.value)}
              className="input-dark text-xs py-1 px-2"
            >
              <option value="all">All Zones</option>
              <option value="Zone A">Zone A</option>
              <option value="Zone B">Zone B</option>
              <option value="Zone C">Zone C</option>
            </select>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-surface/50 border-b border-white/5 text-[10px] font-bold text-mink uppercase">
                <tr>
                  <th className="p-3 w-8">#</th>
                  <th className="p-3">Ward</th>
                  <th className="p-3">Authority</th>
                  <th className="p-3 text-center">Complaints</th>
                  <th className="p-3">Resolution</th>
                  <th className="p-3">Budget</th>
                  <th className="p-3 text-center">SLA Breaches</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {wards.map((w, idx) => {
                  const rate = Math.round((w.resolvedCount / w.complaintCount) * 100);
                  const isTop = idx === 0;

                  return (
                    <tr key={w.id} className="hover:bg-surface/40 transition-colors">
                      <td className="p-3 font-mono font-bold text-mink">
                        {isTop ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : `${idx + 1}`}
                      </td>
                      <td className="p-3 font-semibold text-ink">
                        {w.wardName}
                        <span className="block text-[10px] text-faint font-normal">{w.zone}</span>
                      </td>
                      <td className="p-3 text-mink">{w.authorityName}</td>
                      <td className="p-3 text-center font-mono">
                        <span className="text-ink font-semibold">{w.complaintCount}</span>
                        <span className="text-faint text-[10px] block">{w.resolvedCount} fixed</span>
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-1.5">
                          <div className="w-14 h-1.5 bg-raised rounded-full overflow-hidden">
                            <div
                              className={cn(
                                'h-full rounded-full',
                                rate >= 80 ? 'bg-safe' : rate >= 65 ? 'bg-caution' : 'bg-siren'
                              )}
                              style={{ width: `${rate}%` }}
                            />
                          </div>
                          <span className="font-mono text-[10px] font-bold">{rate}%</span>
                        </div>
                      </td>
                      <td className="p-3 font-mono text-[11px]">
                        <div>{formatINR(w.budgetSpent)}</div>
                        <div className="text-[10px] text-faint">of {formatINR(w.budgetSanctioned)}</div>
                      </td>
                      <td className="p-3 text-center">
                        <span
                          className={cn(
                            'px-1.5 py-0.5 rounded text-[10px] font-bold font-mono',
                            w.slaBreaches > 3 ? 'bg-siren/15 text-siren' : 'bg-raised text-mink'
                          )}
                        >
                          {w.slaBreaches}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Officer Leaderboard Table */}
        <div className="glass-card flex flex-col overflow-hidden">
          <div className="p-4 border-b border-white/5 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users size={16} className="text-cyber" />
              <div>
                <h3 className="text-sm font-bold text-ink">Officer Leaderboard</h3>
                <p className="text-[10px] text-mink">Active field officers and municipal dispatch response</p>
              </div>
            </div>
            <span className="text-[10px] font-mono px-2 py-0.5 bg-safe/10 text-safe rounded border border-safe/20 font-bold">
              All Units Active
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-surface/50 border-b border-white/5 text-[10px] font-bold text-mink uppercase">
                <tr>
                  <th className="p-3">Officer</th>
                  <th className="p-3">Ward</th>
                  <th className="p-3 text-center">Assigned</th>
                  <th className="p-3 text-center">Resolved</th>
                  <th className="p-3 text-center">Avg Time</th>
                  <th className="p-3 text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {store.responders.map((off) => (
                  <tr key={off.id} className="hover:bg-surface/40 transition-colors">
                    <td className="p-3">
                      <p className="font-semibold text-ink">{off.name}</p>
                      <p className="text-[10px] font-mono text-faint">{off.badge} · {off.phone}</p>
                    </td>
                    <td className="p-3 text-mink font-medium">{off.ward}</td>
                    <td className="p-3 text-center font-mono font-semibold text-ink">{off.assignedCount}</td>
                    <td className="p-3 text-center font-mono font-semibold text-safe">{off.resolvedCount}</td>
                    <td className="p-3 text-center font-mono text-[11px] text-mink">
                      {off.avgResponseMin} min
                    </td>
                    <td className="p-3 text-right">
                      <span
                        className={cn(
                          'badge text-[10px] font-bold uppercase',
                          off.status === 'on-duty' && 'badge-safe',
                          off.status === 'busy' && 'badge-caution',
                          off.status === 'off-duty' && 'badge-siren'
                        )}
                      >
                        {off.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
