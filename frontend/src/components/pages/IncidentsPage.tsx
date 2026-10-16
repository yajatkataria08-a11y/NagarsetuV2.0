'use client';

import { useState } from 'react';
import {
  Search, CheckCircle2, XCircle, UserCheck, ShieldAlert,
  ArrowUpDown, Filter, AlertTriangle, Clock
} from 'lucide-react';
import { cn, timeAgo, formatIncidentType } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { IncidentStatus, IncidentType } from '@/types';

type Store = ReturnType<typeof useStore>;

const INCIDENT_TYPES: IncidentType[] = [
  'ROAD_HAZARD',
  'HARASSMENT',
  'FLOOD_RISK',
  'POOR_LIGHTING',
  'UNSAFE_SPOT',
  'SUSPICIOUS_ACTIVITY',
  'WATER_LOGGING',
  'POTHOLE',
  'FIRE',
  'MEDICAL',
];

const WARDS = [
  'All Wards',
  'Ward 3 — MP Nagar',
  'Ward 5 — TT Nagar',
  'Ward 7 — Arera Colony',
  'Ward 9 — Habibganj',
  'Ward 12 — Kolar',
];

export default function IncidentsPage({ store }: { store: Store }) {
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [filterType, setFilterType] = useState<string>('all');
  const [filterWard, setFilterWard] = useState<string>('All Wards');
  const [filterSev, setFilterSev] = useState<number>(0);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedOfficer, setSelectedOfficer] = useState('');

  const filtered = store.incidents.filter(i => {
    if (filterStatus !== 'all' && i.status !== filterStatus) return false;
    if (filterType !== 'all' && i.type !== filterType) return false;
    if (filterWard !== 'All Wards' && !i.ward.includes(filterWard.split(' — ')[0])) return false;
    if (filterSev > 0 && i.severity < filterSev) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      return (
        i.id.toLowerCase().includes(q) ||
        i.description.toLowerCase().includes(q) ||
        i.ward.toLowerCase().includes(q) ||
        i.type.toLowerCase().includes(q)
      );
    }
    return true;
  }).sort((a, b) => b.reportedAt - a.reportedAt);

  const toggleSelectAll = () => {
    if (selectedIds.length === filtered.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filtered.map(i => i.id));
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const handleBulkVerify = () => {
    store.bulkVerifyIncidents(selectedIds);
    setSelectedIds([]);
  };

  const handleBulkReject = () => {
    store.bulkRejectIncidents(selectedIds);
    setSelectedIds([]);
  };

  const handleBulkAssign = (officer: string) => {
    if (!officer) return;
    store.bulkAssignIncidents(selectedIds, officer);
    setSelectedIds([]);
    setAssignModalOpen(false);
  };

  return (
    <div className="flex flex-col h-full overflow-hidden bg-void">
      {/* ── 1. Top Filters Bar ────────────────────────────────────────── */}
      <div className="flex-shrink-0 p-4 border-b border-white/5 bg-abyss flex flex-wrap items-center gap-2.5">
        <div className="relative flex-1 min-w-[200px] max-w-xs">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-faint" />
          <input
            className="input-dark pl-8 py-1.5 text-xs w-full"
            placeholder="Search ID, keyword, ward..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <select
          value={filterStatus}
          onChange={e => setFilterStatus(e.target.value)}
          className="input-dark py-1.5 px-2.5 text-xs bg-surface cursor-pointer"
        >
          <option value="all">All Status</option>
          <option value="pending">Pending</option>
          <option value="ai_verified">AI Verified</option>
          <option value="verified">Verified</option>
          <option value="dispatched">Dispatched</option>
          <option value="resolved">Resolved</option>
          <option value="rejected">Rejected</option>
        </select>

        <select
          value={filterType}
          onChange={e => setFilterType(e.target.value)}
          className="input-dark py-1.5 px-2.5 text-xs bg-surface cursor-pointer"
        >
          <option value="all">All Incident Types</option>
          {INCIDENT_TYPES.map(t => (
            <option key={t} value={t}>{formatIncidentType(t)}</option>
          ))}
        </select>

        <select
          value={filterWard}
          onChange={e => setFilterWard(e.target.value)}
          className="input-dark py-1.5 px-2.5 text-xs bg-surface cursor-pointer"
        >
          {WARDS.map(w => (
            <option key={w} value={w}>{w}</option>
          ))}
        </select>

        <select
          value={filterSev}
          onChange={e => setFilterSev(Number(e.target.value))}
          className="input-dark py-1.5 px-2.5 text-xs bg-surface cursor-pointer"
        >
          <option value={0}>Any Severity</option>
          <option value={3}>Medium+ (3+)</option>
          <option value={4}>High+ (4+)</option>
          <option value={5}>Critical Only (5)</option>
        </select>
      </div>

      {/* ── 2. Bulk Action Bar (Visible when items selected) ──────────── */}
      {selectedIds.length > 0 && (
        <div className="flex-shrink-0 px-4 py-2.5 bg-cyber/10 border-b border-cyber/30 flex items-center justify-between animate-fadeIn">
          <div className="flex items-center gap-3">
            <span className="text-xs font-bold text-cyber">
              {selectedIds.length} incident{selectedIds.length > 1 ? 's' : ''} selected
            </span>
            <div className="h-4 w-px bg-cyber/30" />
            <button
              onClick={handleBulkVerify}
              className="btn-cyber text-xs py-1 px-3 flex items-center gap-1.5"
            >
              <CheckCircle2 size={13} />
              Verify Selected
            </button>
            <button
              onClick={handleBulkReject}
              className="btn-danger text-xs py-1 px-3 flex items-center gap-1.5"
            >
              <XCircle size={13} />
              Reject Selected
            </button>
            <button
              onClick={() => setAssignModalOpen(true)}
              className="btn-ghost text-xs py-1 px-3 border border-white/10 flex items-center gap-1.5"
            >
              <UserCheck size={13} />
              Assign Officer
            </button>
          </div>
          <button
            onClick={() => setSelectedIds([])}
            className="text-xs text-mink hover:text-ink underline"
          >
            Clear selection
          </button>
        </div>
      )}

      {/* ── 3. Incidents Full Data Table ──────────────────────────────── */}
      <div className="flex-1 overflow-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead className="sticky top-0 bg-abyss border-b border-white/10 z-10 text-[11px] font-semibold text-mink uppercase tracking-wider">
            <tr>
              <th className="p-3 w-10 text-center">
                <input
                  type="checkbox"
                  checked={filtered.length > 0 && selectedIds.length === filtered.length}
                  onChange={toggleSelectAll}
                  className="rounded border-white/20 accent-cyber cursor-pointer"
                />
              </th>
              <th className="p-3 w-20">ID</th>
              <th className="p-3">Type</th>
              <th className="p-3 min-w-[240px]">Description & Location</th>
              <th className="p-3">Ward</th>
              <th className="p-3 text-center">Severity</th>
              <th className="p-3">Status</th>
              <th className="p-3">AI Score</th>
              <th className="p-3">Reported</th>
              <th className="p-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={10} className="p-8 text-center text-mink">
                  No incidents match the selected filters.
                </td>
              </tr>
            ) : (
              filtered.map((inc) => {
                const isSelected = selectedIds.includes(inc.id);
                return (
                  <tr
                    key={inc.id}
                    className={cn(
                      'transition-colors hover:bg-surface/50',
                      isSelected ? 'bg-cyber/5' : ''
                    )}
                  >
                    <td className="p-3 text-center">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => toggleSelect(inc.id)}
                        className="rounded border-white/20 accent-cyber cursor-pointer"
                      />
                    </td>
                    <td className="p-3 font-mono text-faint font-semibold">{inc.id}</td>
                    <td className="p-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-raised border border-white/10 text-ink">
                        {formatIncidentType(inc.type)}
                      </span>
                    </td>
                    <td className="p-3">
                      <p className="text-ink font-medium leading-snug line-clamp-2">{inc.description}</p>
                      {inc.address && (
                        <p className="text-[10px] text-mink mt-0.5 flex items-center gap-1">
                          📍 {inc.address}
                        </p>
                      )}
                    </td>
                    <td className="p-3 text-ink font-semibold">{inc.ward}</td>
                    <td className="p-3 text-center">
                      <div className="inline-flex items-center gap-1">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <span
                            key={i}
                            className={cn(
                              'w-1.5 h-1.5 rounded-full',
                              i < inc.severity
                                ? inc.severity >= 4
                                  ? 'bg-siren'
                                  : inc.severity === 3
                                  ? 'bg-caution'
                                  : 'bg-safe'
                                : 'bg-white/10'
                            )}
                          />
                        ))}
                      </div>
                    </td>
                    <td className="p-3">
                      <span
                        className={cn(
                          'badge text-[10px] font-bold uppercase',
                          inc.status === 'verified' && 'badge-safe',
                          inc.status === 'ai_verified' && 'badge-cyber',
                          inc.status === 'pending' && 'badge-caution',
                          inc.status === 'rejected' && 'badge-siren',
                          inc.status === 'dispatched' && 'badge-cyber',
                          inc.status === 'resolved' && 'badge-safe'
                        )}
                      >
                        {inc.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="p-3">
                      {inc.aiScore != null ? (
                        <div className="flex items-center gap-1.5">
                          <div className="w-12 h-1.5 bg-raised rounded-full overflow-hidden">
                            <div
                              className={cn(
                                'h-full rounded-full',
                                inc.aiScore >= 0.8 ? 'bg-safe' : inc.aiScore >= 0.5 ? 'bg-caution' : 'bg-siren'
                              )}
                              style={{ width: `${Math.round(inc.aiScore * 100)}%` }}
                            />
                          </div>
                          <span className="text-[10px] font-mono text-mink">
                            {Math.round(inc.aiScore * 100)}%
                          </span>
                        </div>
                      ) : (
                        <span className="text-[10px] text-faint">—</span>
                      )}
                    </td>
                    <td className="p-3 text-[11px] text-mink whitespace-nowrap">
                      {timeAgo(inc.reportedAt)}
                    </td>
                    <td className="p-3 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1">
                        {inc.status !== 'verified' && (
                          <button
                            onClick={() => store.verifyIncident(inc.id)}
                            className="p-1 rounded hover:bg-safe/20 text-safe transition-colors"
                            title="Verify Incident"
                          >
                            <CheckCircle2 size={15} />
                          </button>
                        )}
                        {inc.status !== 'rejected' && (
                          <button
                            onClick={() => store.rejectIncident(inc.id)}
                            className="p-1 rounded hover:bg-siren/20 text-siren transition-colors"
                            title="Reject Incident"
                          >
                            <XCircle size={15} />
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

      {/* ── 4. Footer Info Bar ────────────────────────────────────────── */}
      <div className="flex-shrink-0 px-4 py-2 border-t border-white/5 bg-abyss flex items-center justify-between text-xs text-mink">
        <span>Showing <strong className="text-ink">{filtered.length}</strong> of {store.incidents.length} total incidents</span>
        <span className="text-[11px] text-faint">Auto-refreshed via PostGIS Supabase Sync</span>
      </div>

      {/* ── 5. Assign Officer Modal ───────────────────────────────────── */}
      {assignModalOpen && (
        <div className="fixed inset-0 bg-void/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card max-w-md w-full p-5 space-y-4">
            <h3 className="text-sm font-bold text-ink">Assign Incidents to Field Officer</h3>
            <p className="text-xs text-mink">
              Assigning {selectedIds.length} selected incident{selectedIds.length > 1 ? 's' : ''} to an active responder.
            </p>
            <select
              value={selectedOfficer}
              onChange={e => setSelectedOfficer(e.target.value)}
              className="input-dark w-full py-2 text-xs"
            >
              <option value="">Select an active officer...</option>
              {store.responders.map(r => (
                <option key={r.id} value={r.name}>
                  {r.name} ({r.badge}) — {r.ward} [{r.status}]
                </option>
              ))}
            </select>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setAssignModalOpen(false)}
                className="btn-ghost text-xs px-3 py-1.5"
              >
                Cancel
              </button>
              <button
                disabled={!selectedOfficer}
                onClick={() => handleBulkAssign(selectedOfficer)}
                className="btn-cyber text-xs px-4 py-1.5 disabled:opacity-50"
              >
                Confirm Assignment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
