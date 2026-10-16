'use client';

import { useState } from 'react';
import {
  Users, ShieldCheck, ShieldAlert, Ban, Search,
  CheckCircle2, AlertTriangle, UserX, UserCheck
} from 'lucide-react';
import { cn } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { CitizenUser } from '@/types';

type Store = ReturnType<typeof useStore>;

export default function CitizenRegistryPage({ store }: { store: Store }) {
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  const users = store.users.filter(u => {
    if (filterStatus !== 'all' && u.status !== filterStatus) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      return (
        u.name.toLowerCase().includes(q) ||
        u.phone.includes(q) ||
        u.ward.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const activeCount = store.users.filter(u => u.status === 'active').length;
  const flaggedCount = store.users.filter(u => u.status === 'flagged').length;
  const bannedCount = store.users.filter(u => u.status === 'banned').length;

  return (
    <div className="flex flex-col h-full overflow-hidden bg-void">
      {/* ── 1. Top Filter & Stat Bar ──────────────────────────────────── */}
      <div className="flex-shrink-0 p-4 border-b border-white/5 bg-abyss flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="relative min-w-[220px]">
            <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-faint" />
            <input
              className="input-dark pl-8 py-1.5 text-xs w-full"
              placeholder="Search citizen name, phone, ward..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>

          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
            className="input-dark py-1.5 px-3 text-xs bg-surface cursor-pointer"
          >
            <option value="all">All Citizens ({store.users.length})</option>
            <option value="active">Active ({activeCount})</option>
            <option value="flagged">Flagged for Fraud ({flaggedCount})</option>
            <option value="banned">Banned ({bannedCount})</option>
          </select>
        </div>

        {/* Trust Metric Badges */}
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-safe/10 border border-safe/20 text-safe text-xs font-semibold">
            <ShieldCheck size={13} />
            <span>{activeCount} Verified Active</span>
          </div>
          {flaggedCount > 0 && (
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-caution/10 border border-caution/20 text-caution text-xs font-semibold">
              <AlertTriangle size={13} />
              <span>{flaggedCount} Flagged</span>
            </div>
          )}
          {bannedCount > 0 && (
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-siren/10 border border-siren/20 text-siren text-xs font-semibold">
              <Ban size={13} />
              <span>{bannedCount} Banned</span>
            </div>
          )}
        </div>
      </div>

      {/* ── 2. Citizen Table ─────────────────────────────────────────── */}
      <div className="flex-1 overflow-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead className="sticky top-0 bg-abyss border-b border-white/10 z-10 text-[11px] font-semibold text-mink uppercase tracking-wider">
            <tr>
              <th className="p-3">Citizen Name</th>
              <th className="p-3">Phone</th>
              <th className="p-3">Ward</th>
              <th className="p-3 text-center">Reports</th>
              <th className="p-3 text-center">SOS</th>
              <th className="p-3 text-center">False / Spams</th>
              <th className="p-3">Civic Trust Score</th>
              <th className="p-3">Status</th>
              <th className="p-3 text-right">Moderation Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {users.length === 0 ? (
              <tr>
                <td colSpan={9} className="p-8 text-center text-mink">
                  No citizens match the selected filter.
                </td>
              </tr>
            ) : (
              users.map((u) => {
                const isTrustHigh = u.trustScore >= 80;
                const isTrustMed = u.trustScore >= 50 && u.trustScore < 80;

                return (
                  <tr key={u.id} className="hover:bg-surface/50 transition-colors">
                    <td className="p-3">
                      <p className="font-semibold text-ink">{u.name}</p>
                      <p className="text-[10px] font-mono text-faint">{u.id}</p>
                    </td>
                    <td className="p-3 font-mono text-mink">{u.phone}</td>
                    <td className="p-3 text-ink font-medium">{u.ward}</td>
                    <td className="p-3 text-center font-mono font-bold text-ink">{u.reportsCount}</td>
                    <td className="p-3 text-center font-mono text-siren font-bold">{u.sosCount}</td>
                    <td className="p-3 text-center font-mono">
                      <span className={cn(u.falseReportsCount > 0 ? 'text-siren font-bold' : 'text-faint')}>
                        {u.falseReportsCount}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center gap-2">
                        <div className="w-20 h-2 bg-raised rounded-full overflow-hidden">
                          <div
                            className={cn(
                              'h-full rounded-full transition-all',
                              isTrustHigh ? 'bg-safe' : isTrustMed ? 'bg-caution' : 'bg-siren'
                            )}
                            style={{ width: `${u.trustScore}%` }}
                          />
                        </div>
                        <span className={cn(
                          'font-mono text-xs font-bold',
                          isTrustHigh ? 'text-safe' : isTrustMed ? 'text-caution' : 'text-siren'
                        )}>
                          {u.trustScore}%
                        </span>
                      </div>
                    </td>
                    <td className="p-3">
                      <span
                        className={cn(
                          'badge text-[10px] font-bold uppercase',
                          u.status === 'active' && 'badge-safe',
                          u.status === 'flagged' && 'badge-caution',
                          u.status === 'banned' && 'badge-siren'
                        )}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {u.status !== 'active' && (
                          <button
                            onClick={() => store.updateUserStatus(u.id, 'active')}
                            className="p-1.5 rounded hover:bg-safe/20 text-safe transition-colors"
                            title="Verify & Set Active"
                          >
                            <UserCheck size={14} />
                          </button>
                        )}
                        {u.status !== 'flagged' && (
                          <button
                            onClick={() => store.updateUserStatus(u.id, 'flagged')}
                            className="p-1.5 rounded hover:bg-caution/20 text-caution transition-colors"
                            title="Flag for Suspicious Behavior"
                          >
                            <AlertTriangle size={14} />
                          </button>
                        )}
                        {u.status !== 'banned' && (
                          <button
                            onClick={() => store.updateUserStatus(u.id, 'banned')}
                            className="p-1.5 rounded hover:bg-siren/20 text-siren transition-colors"
                            title="Ban User"
                          >
                            <Ban size={14} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* ── 3. Footer Summary ────────────────────────────────────────── */}
      <div className="flex-shrink-0 px-4 py-2 border-t border-white/5 bg-abyss flex items-center justify-between text-xs text-mink">
        <span>Showing <strong className="text-ink">{users.length}</strong> of {store.users.length} registered citizens</span>
        <span className="text-[11px] text-faint">Scored by AI Anti-Spam & False-Report Classifier</span>
      </div>
    </div>
  );
}
