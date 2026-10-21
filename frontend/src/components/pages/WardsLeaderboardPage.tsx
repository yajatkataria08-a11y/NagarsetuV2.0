'use client';

import { useState, useEffect } from 'react';
import {
  Building2, Users, Trophy, AlertTriangle, TrendingUp,
  Clock, ShieldCheck, DollarSign, Scale, Gavel, FileText, CheckCircle2,
  X, RefreshCw, AlertOctagon
} from 'lucide-react';
import { cn, formatINR } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import { contractorApi } from '@/lib/api';

type Store = ReturnType<typeof useStore>;

interface Contractor {
  id: string;
  name: string;
  firm_name: string;
  ward_id: string;
  category: string;
  active_contracts: number;
  performance_score: number;
  penalties_imposed_inr: number;
  sla_breach_count: number;
  status: 'active' | 'flagged' | 'suspended';
  rating: string;
  contact_phone: string;
}

interface AuditEntry {
  id: string;
  contractor_id: string;
  firm_name: string;
  penalty_amount_inr: number;
  breach_reason: string;
  officer_badge: string;
  timestamp: string;
  status: string;
}

export default function WardsLeaderboardPage({ store }: { store: Store }) {
  const [activeTab, setActiveTab] = useState<'wards' | 'contractors'>('wards');
  const [filterZone, setFilterZone] = useState('all');

  // Contractor Accountability State
  const [contractors, setContractors] = useState<Contractor[]>([]);
  const [auditTrail, setAuditTrail] = useState<AuditEntry[]>([]);
  const [contractorsLoading, setContractorsLoading] = useState(false);
  const [selectedContractor, setSelectedContractor] = useState<Contractor | null>(null);
  const [showPenaltyModal, setShowPenaltyModal] = useState(false);
  const [penaltyAmount, setPenaltyAmount] = useState('15000');
  const [breachReason, setBreachReason] = useState('Delay in pothole repair exceeding 48h statutory SLA');
  const [submittingPenalty, setSubmittingPenalty] = useState(false);

  const fetchContractorsData = async () => {
    setContractorsLoading(true);
    try {
      const [cRes, aRes] = await Promise.all([
        contractorApi.list(),
        contractorApi.auditTrail()
      ]);
      if (cRes.contractors) setContractors(cRes.contractors);
      if (aRes.auditTrail) setAuditTrail(aRes.auditTrail);
    } catch (e) {
      console.error('Failed to load contractor data', e);
    } finally {
      setContractorsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'contractors') {
      fetchContractorsData();
    }
  }, [activeTab]);

  const handleApplyPenalty = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedContractor) return;
    setSubmittingPenalty(true);
    try {
      await contractorApi.penalize(selectedContractor.id, {
        penaltyAmount: parseFloat(penaltyAmount) || 10000,
        breachReason,
        officerBadge: 'BMC-COMM-01'
      });
      setShowPenaltyModal(false);
      setSelectedContractor(null);
      await fetchContractorsData();
    } catch (err) {
      console.error('Penalty submission failed', err);
    } finally {
      setSubmittingPenalty(false);
    }
  };

  const wards = store.wards.filter(w =>
    filterZone === 'all' ? true : w.zone === filterZone
  ).sort((a, b) => {
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

  const totalContractorPenalties = contractors.reduce((sum, c) => sum + c.penalties_imposed_inr, 0);
  const flaggedContractorsCount = contractors.filter(c => c.status === 'flagged' || c.status === 'suspended').length;

  return (
    <div className="h-full overflow-y-auto p-5 space-y-5 bg-void">
      {/* ── Top Viewport Mode Switcher ────────────────────────────────────── */}
      <div className="flex items-center justify-between bg-surface/50 p-2 rounded-xl border border-white/5">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('wards')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all',
              activeTab === 'wards'
                ? 'bg-cyber text-void shadow-glow-cyber'
                : 'text-mink hover:text-ink'
            )}
          >
            <Building2 size={15} />
            Wards & Field Officers Leaderboard
          </button>
          <button
            onClick={() => setActiveTab('contractors')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all',
              activeTab === 'contractors'
                ? 'bg-amber-500 text-void shadow-glow-amber'
                : 'text-mink hover:text-ink'
            )}
          >
            <Scale size={15} />
            Contractor Accountability & Trust Ledger
            {flaggedContractorsCount > 0 && (
              <span className="px-1.5 py-0.2 rounded-full bg-siren text-white text-[9px] font-mono">
                {flaggedContractorsCount} flagged
              </span>
            )}
          </button>
        </div>

        {activeTab === 'contractors' && (
          <button
            onClick={fetchContractorsData}
            disabled={contractorsLoading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface text-mink hover:text-ink border border-white/10 text-xs font-semibold"
          >
            <RefreshCw size={12} className={cn(contractorsLoading && 'animate-spin')} />
            Refresh Ledger
          </button>
        )}
      </div>

      {activeTab === 'wards' ? (
        <>
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
        </>
      ) : (
        /* ── Contractor Accountability Mode ────────────────────────────────── */
        <div className="space-y-5">
          {/* Contractor Metrics Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="glass-card p-4 border border-amber-500/20">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xl">⚖️</span>
                <span className="text-[10px] font-bold text-amber-400 uppercase bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">Statutory SLA</span>
              </div>
              <p className="text-2xl font-mono font-black text-amber-400">{contractors.length} Registered</p>
              <p className="text-xs font-bold text-ink mt-1">Active Civic Contractors</p>
              <p className="text-[10px] text-mink mt-1">PWD Roadways, BMC Water & Sanitation</p>
            </div>

            <div className="glass-card p-4 border border-siren/20">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xl">💸</span>
                <span className="text-[10px] font-bold text-siren uppercase bg-siren/10 px-2 py-0.5 rounded border border-siren/20">Public Ledger</span>
              </div>
              <p className="text-2xl font-mono font-black text-siren">{formatINR(totalContractorPenalties)}</p>
              <p className="text-xs font-bold text-ink mt-1">Total Penalties Imposed</p>
              <p className="text-[10px] text-mink mt-1">Deducted from ongoing municipal invoices</p>
            </div>

            <div className="glass-card p-4 border border-safe/20">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xl">🏆</span>
                <span className="text-[10px] font-bold text-safe uppercase bg-safe/10 px-2 py-0.5 rounded border border-safe/20">Top Tier</span>
              </div>
              <p className="text-2xl font-mono font-black text-safe">
                {contractors.filter(c => c.rating.startsWith('A')).length} Firms
              </p>
              <p className="text-xs font-bold text-ink mt-1">A/A+ Compliance Rating</p>
              <p className="text-[10px] text-mink mt-1">Zero unresolved critical SLA defaults</p>
            </div>
          </div>

          {/* Contractors Roster Table */}
          <div className="glass-card flex flex-col overflow-hidden">
            <div className="p-4 border-b border-white/5 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-ink flex items-center gap-2">
                  <Gavel size={16} className="text-amber-400" />
                  Contractor Performance & SLA Accountability
                </h3>
                <p className="text-[10px] text-mink">Real-time performance scoring and penalty audit trail</p>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-surface/50 border-b border-white/5 text-[10px] font-bold text-mink uppercase">
                  <tr>
                    <th className="p-3">Contractor / Firm</th>
                    <th className="p-3">Category</th>
                    <th className="p-3">Assigned Ward</th>
                    <th className="p-3 text-center">Score</th>
                    <th className="p-3 text-center">Rating</th>
                    <th className="p-3 text-center">Breaches</th>
                    <th className="p-3 text-right">Penalties (INR)</th>
                    <th className="p-3 text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {contractors.map(c => (
                    <tr key={c.id} className="hover:bg-surface/40 transition-colors">
                      <td className="p-3">
                        <p className="font-bold text-ink">{c.firm_name}</p>
                        <p className="text-[10px] font-mono text-faint">{c.name} · {c.contact_phone}</p>
                      </td>
                      <td className="p-3">
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 border border-white/10 text-mink">
                          {c.category}
                        </span>
                      </td>
                      <td className="p-3 text-mink">{c.ward_id}</td>
                      <td className="p-3 text-center font-mono">
                        <span className={cn(
                          'font-bold',
                          c.performance_score >= 85 ? 'text-safe' : (c.performance_score >= 70 ? 'text-caution' : 'text-siren')
                        )}>
                          {c.performance_score}/100
                        </span>
                      </td>
                      <td className="p-3 text-center">
                        <span className={cn(
                          'px-2 py-0.5 rounded text-[10px] font-bold font-mono',
                          c.rating === 'A+' ? 'bg-safe/20 text-safe border border-safe/30' :
                          c.rating === 'A' ? 'bg-safe/10 text-safe border border-safe/20' :
                          c.rating === 'B' ? 'bg-caution/15 text-caution border border-caution/30' :
                          'bg-siren/15 text-siren border border-siren/30'
                        )}>
                          {c.rating}
                        </span>
                      </td>
                      <td className="p-3 text-center font-mono">
                        <span className={cn(
                          'px-1.5 py-0.5 rounded text-[10px] font-bold',
                          c.sla_breach_count > 0 ? 'bg-siren/15 text-siren' : 'text-faint'
                        )}>
                          {c.sla_breach_count}
                        </span>
                      </td>
                      <td className="p-3 text-right font-mono font-bold text-amber-400">
                        {formatINR(c.penalties_imposed_inr)}
                      </td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => {
                            setSelectedContractor(c);
                            setShowPenaltyModal(true);
                          }}
                          className="px-2.5 py-1 rounded bg-siren/15 hover:bg-siren/25 text-siren border border-siren/30 text-[11px] font-bold transition-colors"
                        >
                          Penalize
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Immutable Audit Trail */}
          <div className="glass-card flex flex-col overflow-hidden">
            <div className="p-4 border-b border-white/5 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-ink flex items-center gap-2">
                  <FileText size={16} className="text-cyber" />
                  Public SLA Penalty Audit Trail
                </h3>
                <p className="text-[10px] text-mink">Immutable chronological ledger of municipal financial deductions</p>
              </div>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 text-cyber border border-cyber/20 font-bold">
                {auditTrail.length} Logged Entries
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-surface/50 border-b border-white/5 text-[10px] font-bold text-mink uppercase">
                  <tr>
                    <th className="p-3">Ref ID</th>
                    <th className="p-3">Contractor Firm</th>
                    <th className="p-3">Statutory Breach Cause</th>
                    <th className="p-3 text-center">Officer Badge</th>
                    <th className="p-3 text-right">Deduction (INR)</th>
                    <th className="p-3 text-right">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {auditTrail.map(a => (
                    <tr key={a.id} className="hover:bg-surface/40 transition-colors">
                      <td className="p-3 font-mono text-[10px] text-faint">{a.id}</td>
                      <td className="p-3 font-semibold text-ink">{a.firm_name}</td>
                      <td className="p-3 text-mink">{a.breach_reason}</td>
                      <td className="p-3 text-center font-mono text-[10px] text-cyber">{a.officer_badge}</td>
                      <td className="p-3 text-right font-mono font-bold text-siren">
                        -{formatINR(a.penalty_amount_inr)}
                      </td>
                      <td className="p-3 text-right font-mono text-[10px] text-faint">
                        {new Date(a.timestamp).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── Apply Penalty Modal ───────────────────────────────────────────── */}
      {showPenaltyModal && selectedContractor && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-5 border border-siren/40 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div className="flex items-center gap-2">
                <AlertOctagon size={18} className="text-siren" />
                <h3 className="text-sm font-bold text-ink">Impose Contractor Penalty</h3>
              </div>
              <button
                onClick={() => setShowPenaltyModal(false)}
                className="text-mink hover:text-ink"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleApplyPenalty} className="space-y-4">
              <div className="p-3 rounded-lg bg-surface/50 border border-white/5 text-xs space-y-1">
                <p className="font-bold text-ink">{selectedContractor.firm_name}</p>
                <p className="text-mink font-mono text-[11px]">{selectedContractor.ward_id} · Current Score: {selectedContractor.performance_score}/100</p>
              </div>

              <div className="space-y-1.5">
                <label className="text-[11px] font-bold text-mink uppercase">
                  Penalty Fine Amount (INR)
                </label>
                <input
                  type="number"
                  value={penaltyAmount}
                  onChange={e => setPenaltyAmount(e.target.value)}
                  className="input-dark w-full text-xs font-mono py-2 px-3"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[11px] font-bold text-mink uppercase">
                  Statutory SLA Breach Reason
                </label>
                <select
                  value={breachReason}
                  onChange={e => setBreachReason(e.target.value)}
                  className="input-dark w-full text-xs py-2 px-3"
                >
                  <option value="Delay in pothole repair exceeding 48h statutory SLA">Delay in pothole repair exceeding 48h statutory SLA</option>
                  <option value="Substandard asphalt patch failure on arterial corridor">Substandard asphalt patch failure on arterial corridor</option>
                  <option value="Habibganj Underpass drainage pump deployment failure">Habibganj Underpass drainage pump deployment failure</option>
                  <option value="Unattended municipal waste dumping site breach">Unattended municipal waste dumping site breach</option>
                  <option value="Streetlight blackout cluster left unrectified for over 24h">Streetlight blackout cluster left unrectified for over 24h</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setShowPenaltyModal(false)}
                  className="px-3 py-1.5 rounded-lg text-xs font-semibold text-mink hover:text-ink"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPenalty}
                  className="px-4 py-2 rounded-lg bg-siren hover:bg-siren/90 text-white font-bold text-xs flex items-center gap-1.5 shadow-glow-siren"
                >
                  {submittingPenalty ? <RefreshCw size={14} className="animate-spin" /> : <Gavel size={14} />}
                  Deduct & Log to Ledger
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
