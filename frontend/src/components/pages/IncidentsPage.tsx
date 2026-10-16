'use client';

import { useState } from 'react';
import { Search, Filter } from 'lucide-react';
import IncidentCard from '@/components/command/IncidentCard';
import { formatIncidentType } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { IncidentStatus, IncidentType } from '@/types';

type Store = ReturnType<typeof useStore>;

const STATUSES: IncidentStatus[] = ['pending', 'ai_verified', 'verified', 'dispatched', 'resolved', 'rejected'];
const STATUS_LABELS: Record<IncidentStatus, string> = {
  pending:     'Pending',
  ai_verified: 'AI Verified',
  verified:    'Verified',
  dispatched:  'Dispatched',
  resolved:    'Resolved',
  rejected:    'Rejected',
};

export default function IncidentsPage({ store }: { store: Store }) {
  const [search, setSearch]           = useState('');
  const [filterStatus, setFilterStatus] = useState<IncidentStatus | 'all'>('all');
  const [filterSev, setFilterSev]     = useState<number>(0);

  const filtered = store.incidents.filter(i => {
    if (filterStatus !== 'all' && i.status !== filterStatus) return false;
    if (filterSev && i.severity < filterSev) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        i.description.toLowerCase().includes(q) ||
        i.ward.toLowerCase().includes(q) ||
        i.type.toLowerCase().includes(q)
      );
    }
    return true;
  }).sort((a, b) => b.reportedAt - a.reportedAt);

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* Filter bar */}
      <div className="flex-shrink-0 flex items-center gap-3 px-4 py-3 border-b border-white/5 bg-abyss">
        {/* Search */}
        <div className="relative flex-1 max-w-xs">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-faint" />
          <input
            className="input-dark pl-8 py-1.5 text-xs"
            placeholder="Search incidents…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        {/* Status filter */}
        <div className="flex items-center gap-1">
          {(['all', ...STATUSES] as const).map(s => (
            <button
              key={s}
              onClick={() => setFilterStatus(s)}
              className={`px-2.5 py-1 text-[11px] font-semibold rounded-lg transition-colors ${
                filterStatus === s
                  ? 'bg-cyber/20 text-cyber border border-cyber/30'
                  : 'bg-raised/40 text-mink hover:bg-muted hover:text-ink'
              }`}
            >
              {s === 'all' ? 'All' : STATUS_LABELS[s]}
            </button>
          ))}
        </div>

        {/* Severity filter */}
        <div className="flex items-center gap-2 text-xs text-mink ml-auto">
          <Filter size={12} />
          <span>Sev ≥</span>
          <select
            value={filterSev}
            onChange={e => setFilterSev(Number(e.target.value))}
            className="bg-raised border border-white/10 rounded text-xs text-ink px-1.5 py-1 focus:outline-none focus:border-cyber/40"
          >
            <option value={0}>Any</option>
            <option value={3}>3</option>
            <option value={4}>4</option>
            <option value={5}>5</option>
          </select>
        </div>

        <span className="text-[11px] text-mink">{filtered.length} results</span>
      </div>

      {/* Incident grid */}
      <div className="flex-1 overflow-y-auto p-4">
        {filtered.length === 0 ? (
          <div className="flex items-center justify-center h-full text-faint text-sm">
            No incidents match the current filters.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
            {filtered.map(inc => (
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
        )}
      </div>
    </div>
  );
}
