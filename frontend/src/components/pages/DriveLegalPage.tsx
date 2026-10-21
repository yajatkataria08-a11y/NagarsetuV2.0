'use client';

import { useState, useEffect, useMemo } from 'react';
import {
  Scale, Search, ShieldAlert, Car, Bike, Truck, Bus,
  CheckCircle2, AlertTriangle, HelpCircle, ExternalLink, RefreshCw,
  Send, Sparkles, BookOpen, ChevronRight, FileText, Globe, Gavel
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { legalApi } from '@/lib/api';

interface NationInfo {
  code: string;
  name: string;
  flag: string;
  currency: string;
  symbol: string;
  rate: number;
  law: string;
  emergency: string;
  trafficHelpline: string;
}

interface Violation {
  id: string;
  category: string;
  title: string;
  section: string;
  rawSection: string;
  baseFineInr: number;
  convertedBaseFine?: number;
  currency?: string;
  currencySymbol?: string;
  penaltyPoints: number;
  jailTermMonths: number;
  mandatoryCourt: boolean;
  statutoryNote: string;
  urgencyLevel: 'CRITICAL' | 'HIGH' | 'STANDARD';
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  matchedViolation?: Violation;
  timestamp: string;
}

const VEHICLES = [
  { id: 'BIKE', label: 'Two Wheeler', icon: Bike, mult: 1.0 },
  { id: 'CAR', label: 'Car / SUV', icon: Car, mult: 1.0 },
  { id: 'AUTO', label: 'Auto Rickshaw', icon: Car, mult: 1.2 },
  { id: 'BUS', label: 'Bus / Transit', icon: Bus, mult: 2.0 },
  { id: 'TRUCK', label: 'Heavy Truck', icon: Truck, mult: 2.5 },
];

const REPEAT_TIERS = [
  { tier: 1, label: '1st Offence', mult: 1.0 },
  { tier: 2, label: '2nd Offence', mult: 2.0 },
  { tier: 3, label: '3rd+ Offence', mult: 4.0 },
];

export default function DriveLegalPage() {
  const [activeTab, setActiveTab] = useState<'explorer' | 'bot'>('explorer');
  const [nations, setNations] = useState<NationInfo[]>([]);
  const [selectedNation, setSelectedNation] = useState<string>('IN');
  const [violations, setViolations] = useState<Violation[]>([]);
  const [categories, setCategories] = useState<{ key: string; label: string; count: number }[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);

  // Calculator State
  const [selectedVehicle, setSelectedVehicle] = useState<string>('CAR');
  const [repeatOffence, setRepeatOffence] = useState<number>(1);
  const [selectedViolationIds, setSelectedViolationIds] = useState<string[]>([]);
  const [fineResult, setFineResult] = useState<any>(null);
  const [calcLoading, setCalcLoading] = useState<boolean>(false);

  // Bot State
  const [botMessages, setBotMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'bot',
      text: 'Namaste! I am the DriveLegal AI Assistant. Ask me any traffic fine, MV Act statutory section, or BIMSTEC penalty question in Hindi or English.',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [botInput, setBotInput] = useState<string>('');
  const [botSearching, setBotSearching] = useState<boolean>(false);

  // Load Nations & Categories
  useEffect(() => {
    async function loadMeta() {
      try {
        const [nationsRes, catsRes] = await Promise.all([
          legalApi.nations(),
          legalApi.categories()
        ]);
        if (nationsRes.nations) setNations(nationsRes.nations);
        if (catsRes.categories) setCategories(catsRes.categories);
      } catch (e) {
        console.error('Failed to load metadata', e);
      }
    }
    loadMeta();
  }, []);

  // Fetch Violations on Filters Change
  useEffect(() => {
    async function fetchViolations() {
      setLoading(true);
      try {
        const res = await legalApi.violations({
          nation: selectedNation,
          category: selectedCategory === 'ALL' ? undefined : selectedCategory,
          q: searchQuery.trim() || undefined,
          limit: 154
        });
        if (res.violations) {
          setViolations(res.violations);
          if (selectedViolationIds.length === 0 && res.violations.length > 0) {
            setSelectedViolationIds([res.violations[0].id]);
          }
        }
      } catch (e) {
        console.error('Failed to fetch violations', e);
      } finally {
        setLoading(false);
      }
    }
    const t = setTimeout(fetchViolations, 150);
    return () => clearTimeout(t);
  }, [selectedNation, selectedCategory, searchQuery]);

  // Recalculate Fine whenever selection, vehicle, repeat, or nation changes
  useEffect(() => {
    if (selectedViolationIds.length === 0) {
      setFineResult(null);
      return;
    }
    async function calculate() {
      setCalcLoading(true);
      try {
        const res = await legalApi.calculateFine({
          violationIds: selectedViolationIds,
          vehicleType: selectedVehicle,
          repeatOffence,
          nationCode: selectedNation
        });
        if (res.calculation) {
          setFineResult(res.calculation);
        }
      } catch (e) {
        console.error('Fine calculation error', e);
      } finally {
        setCalcLoading(false);
      }
    }
    calculate();
  }, [selectedViolationIds, selectedVehicle, repeatOffence, selectedNation]);

  const currentNation = useMemo(() => {
    return nations.find(n => n.code === selectedNation) || nations[0] || {
      code: 'IN', name: 'India', flag: '🇮🇳', currency: 'INR', symbol: '₹', rate: 1.0,
      law: 'Motor Vehicles Act 1988 (Amended 2019)', emergency: '112', trafficHelpline: '0755-2443344'
    };
  }, [nations, selectedNation]);

  const toggleViolationSelect = (id: string) => {
    setSelectedViolationIds(prev =>
      prev.includes(id)
        ? (prev.length > 1 ? prev.filter(x => x !== id) : prev)
        : [...prev, id]
    );
  };

  const handleSendBot = async (customQuery?: string) => {
    const q = (customQuery || botInput).trim();
    if (!q) return;

    const userMsg: ChatMessage = {
      id: String(Date.now()),
      sender: 'user',
      text: q,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setBotMessages(prev => [...prev, userMsg]);
    if (!customQuery) setBotInput('');
    setBotSearching(true);

    try {
      const res = await legalApi.queryBot({
        query: q,
        nationCode: selectedNation,
        vehicleType: selectedVehicle
      });

      const botMsg: ChatMessage = {
        id: String(Date.now() + 1),
        sender: 'bot',
        text: res.answer,
        matchedViolation: res.matchedViolation,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      setBotMessages(prev => [...prev, botMsg]);
    } catch (e) {
      setBotMessages(prev => [...prev, {
        id: String(Date.now() + 1),
        sender: 'bot',
        text: 'Network error querying legal knowledge base. Please check backend connection.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }]);
    } finally {
      setBotSearching(false);
    }
  };

  return (
    <div className="flex flex-col h-full bg-void overflow-hidden text-ink">
      {/* ── Top Header ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between px-6 py-4 bg-abyss border-b border-white/5 flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center shadow-glow-amber">
            <Scale size={20} className="text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-extrabold tracking-tight text-ink">DriveLegal™ BIMSTEC Knowledge Base</h1>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/30 font-bold">
                154 Statutory Violations
              </span>
            </div>
            <p className="text-xs text-mink">
              Multi-Jurisdictional Traffic Code Engine & Automated Fine Multiplier Grid
            </p>
          </div>
        </div>

        {/* Tab & Nation Selector */}
        <div className="flex items-center gap-3">
          {/* Nation Selector */}
          <div className="flex items-center bg-surface border border-white/10 rounded-lg p-1 gap-1">
            <Globe size={14} className="text-mink ml-1.5" />
            <select
              value={selectedNation}
              onChange={e => setSelectedNation(e.target.value)}
              className="bg-transparent text-xs font-semibold text-ink outline-none cursor-pointer py-1 pr-2"
            >
              {nations.map(n => (
                <option key={n.code} value={n.code} className="bg-abyss text-ink">
                  {n.flag} {n.name} ({n.currency})
                </option>
              ))}
            </select>
          </div>

          {/* Mode Switcher */}
          <div className="flex bg-surface border border-white/10 rounded-lg p-1">
            <button
              onClick={() => setActiveTab('explorer')}
              className={cn(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-bold transition-all',
                activeTab === 'explorer' ? 'bg-amber-500 text-void shadow-sm' : 'text-mink hover:text-ink'
              )}
            >
              <BookOpen size={13} />
              Statutory Explorer
            </button>
            <button
              onClick={() => setActiveTab('bot')}
              className={cn(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-bold transition-all',
                activeTab === 'bot' ? 'bg-amber-500 text-void shadow-sm' : 'text-mink hover:text-ink'
              )}
            >
              <Sparkles size={13} />
              DriveLegal AI Bot
            </button>
          </div>
        </div>
      </div>

      {/* ── Main Viewport ───────────────────────────────────────────────────── */}
      {activeTab === 'explorer' ? (
        <div className="flex-1 flex overflow-hidden">
          {/* Left Column: Search & Violations List */}
          <div className="w-7/12 flex flex-col border-r border-white/5 bg-void/50">
            {/* Filter Bar */}
            <div className="p-4 border-b border-white/5 space-y-3 bg-abyss/40">
              <div className="flex items-center gap-3">
                <div className="relative flex-1">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-mink" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                    placeholder="Search by section (e.g. 183), violation (helmet, drunk), or keyword..."
                    className="w-full pl-9 pr-4 py-2 rounded-lg bg-surface border border-white/10 text-xs text-ink placeholder-faint focus:border-amber-500 outline-none"
                  />
                </div>
                <div className="text-xs text-mink font-mono whitespace-nowrap">
                  Showing <span className="text-amber-400 font-bold">{violations.length}</span> of 154
                </div>
              </div>

              {/* Category Pills */}
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
                <button
                  onClick={() => setSelectedCategory('ALL')}
                  className={cn(
                    'px-2.5 py-1 rounded-full text-[11px] font-semibold whitespace-nowrap transition-colors',
                    selectedCategory === 'ALL'
                      ? 'bg-amber-500/20 text-amber-400 border border-amber-500/40'
                      : 'bg-surface text-mink hover:text-ink border border-white/5'
                  )}
                >
                  All Categories
                </button>
                {categories.map(c => (
                  <button
                    key={c.key}
                    onClick={() => setSelectedCategory(c.key)}
                    className={cn(
                      'px-2.5 py-1 rounded-full text-[11px] font-semibold whitespace-nowrap transition-colors',
                      selectedCategory === c.key
                        ? 'bg-amber-500/20 text-amber-400 border border-amber-500/40'
                        : 'bg-surface text-mink hover:text-ink border border-white/5'
                    )}
                  >
                    {c.label} ({c.count})
                  </button>
                ))}
              </div>
            </div>

            {/* Violations Scroll List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              {loading ? (
                <div className="flex flex-col items-center justify-center h-48 text-mink gap-2">
                  <RefreshCw className="animate-spin text-amber-400" size={24} />
                  <p className="text-xs">Loading codified BIMSTEC violations...</p>
                </div>
              ) : violations.length === 0 ? (
                <div className="text-center py-12 text-mink">
                  <FileText size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm font-semibold">No statutory violations found</p>
                  <p className="text-xs text-faint">Try adjusting your query or category filter</p>
                </div>
              ) : (
                violations.map(v => {
                  const isSelected = selectedViolationIds.includes(v.id);
                  const isCritical = v.urgencyLevel === 'CRITICAL';
                  const fine = v.convertedBaseFine ?? v.baseFineInr;
                  const sym = v.currencySymbol || currentNation.symbol;

                  return (
                    <div
                      key={v.id}
                      onClick={() => toggleViolationSelect(v.id)}
                      className={cn(
                        'p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3',
                        isSelected
                          ? 'bg-amber-500/10 border-amber-500/40 shadow-sm'
                          : 'bg-abyss/50 hover:bg-abyss border-white/5 hover:border-white/10'
                      )}
                    >
                      <div className="pt-0.5">
                        <div
                          className={cn(
                            'w-4 h-4 rounded border flex items-center justify-center transition-colors',
                            isSelected ? 'bg-amber-500 border-amber-500 text-void' : 'border-white/20'
                          )}
                        >
                          {isSelected && <CheckCircle2 size={12} className="stroke-[3]" />}
                        </div>
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-[10px] font-mono font-bold px-1.5 py-0.5 rounded bg-white/5 text-amber-400 border border-white/10">
                            {v.rawSection ? `Sec ${v.rawSection}` : v.id}
                          </span>
                          <span className="text-xs font-bold text-ink truncate">{v.title}</span>
                          {isCritical && (
                            <span className="text-[9px] px-1.5 py-0.2 rounded bg-siren/20 text-siren border border-siren/30 font-bold uppercase ml-auto">
                              Arrest / DL Risk
                            </span>
                          )}
                        </div>

                        <p className="text-[11px] text-mink line-clamp-1 mb-1.5">
                          {v.statutoryNote}
                        </p>

                        <div className="flex items-center gap-4 text-[11px] text-faint font-mono">
                          <span>Base: <strong className="text-ink">{sym}{fine.toLocaleString()}</strong></span>
                          <span>Points: <strong className="text-amber-400">{v.penaltyPoints} pts</strong></span>
                          {v.jailTermMonths > 0 && (
                            <span className="text-siren font-semibold">Jail: ≤{v.jailTermMonths}mo</span>
                          )}
                          {v.mandatoryCourt && (
                            <span className="text-orange-400 font-semibold">Court Mandatory</span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Right Column: Live Calculator & Multiplier Grid */}
          <div className="w-5/12 flex flex-col bg-abyss p-5 overflow-y-auto space-y-5">
            <div>
              <h2 className="text-sm font-bold text-ink flex items-center gap-2">
                <Gavel size={16} className="text-amber-400" />
                Dynamic Fine Resolution Engine
              </h2>
              <p className="text-xs text-mink mt-0.5">
                Calculates fine scaling based on vehicle class, repeat multiplier, and {currentNation.name} law.
              </p>
            </div>

            {/* Vehicle Selector */}
            <div className="space-y-2">
              <label className="text-[11px] font-bold text-faint uppercase tracking-wider">
                1. Vehicle Category
              </label>
              <div className="grid grid-cols-5 gap-1.5">
                {VEHICLES.map(v => {
                  const Icon = v.icon;
                  const isChosen = selectedVehicle === v.id;
                  return (
                    <button
                      key={v.id}
                      onClick={() => setSelectedVehicle(v.id)}
                      className={cn(
                        'flex flex-col items-center justify-center p-2 rounded-xl border text-center transition-all',
                        isChosen
                          ? 'bg-amber-500/20 border-amber-500/50 text-amber-400 shadow-sm'
                          : 'bg-surface/50 border-white/5 text-mink hover:text-ink'
                      )}
                    >
                      <Icon size={18} className="mb-1" />
                      <span className="text-[10px] font-bold truncate">{v.label}</span>
                      <span className="text-[9px] font-mono text-faint">{v.mult}×</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Repeat Offence Tier */}
            <div className="space-y-2">
              <label className="text-[11px] font-bold text-faint uppercase tracking-wider">
                2. Offence History Tier
              </label>
              <div className="grid grid-cols-3 gap-2">
                {REPEAT_TIERS.map(t => {
                  const isChosen = repeatOffence === t.tier;
                  return (
                    <button
                      key={t.tier}
                      onClick={() => setRepeatOffence(t.tier)}
                      className={cn(
                        'py-2 px-3 rounded-xl border text-center transition-all',
                        isChosen
                          ? 'bg-amber-500/20 border-amber-500/50 text-amber-400 shadow-sm'
                          : 'bg-surface/50 border-white/5 text-mink hover:text-ink'
                      )}
                    >
                      <div className="text-xs font-bold">{t.label}</div>
                      <div className="text-[10px] font-mono text-faint">{t.mult}× Multiplier</div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Calculation Result Breakdown Card */}
            {calcLoading ? (
              <div className="p-6 rounded-2xl bg-surface/50 border border-white/10 flex items-center justify-center text-mink text-xs gap-2">
                <RefreshCw className="animate-spin text-amber-400" size={16} />
                Computing statutory multipliers...
              </div>
            ) : fineResult ? (
              <div className="space-y-4">
                <div className="p-5 rounded-2xl bg-gradient-to-br from-surface to-abyss border border-amber-500/30 shadow-lg space-y-4">
                  <div className="flex items-center justify-between border-b border-white/10 pb-3">
                    <div>
                      <span className="text-[10px] font-mono text-faint uppercase">Estimated Total Challan</span>
                      <div className="text-2xl font-extrabold text-amber-400 tracking-tight font-mono">
                        {fineResult.formattedTotal}
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] font-mono text-faint uppercase">Cumulative Demerit</span>
                      <div className={cn(
                        'text-lg font-bold font-mono',
                        fineResult.totalPenaltyPoints >= 6 ? 'text-siren' : 'text-amber-400'
                      )}>
                        {fineResult.totalPenaltyPoints} Points
                      </div>
                    </div>
                  </div>

                  {/* Itemized Breakdown */}
                  <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                    {fineResult.items.map((item: any, idx: number) => (
                      <div key={idx} className="p-2.5 rounded-lg bg-surface/40 border border-white/5 text-xs space-y-1">
                        <div className="flex items-center justify-between font-bold text-ink">
                          <span className="truncate pr-2">{item.title}</span>
                          <span className="font-mono text-amber-400 whitespace-nowrap">
                            {fineResult.currencySymbol}{item.convertedSubtotal.toLocaleString()}
                          </span>
                        </div>
                        <div className="text-[10px] font-mono text-faint">
                          {item.formula}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Warning Badges */}
                  <div className="space-y-1.5 pt-1">
                    {fineResult.mandatoryCourt && (
                      <div className="flex items-center gap-2 p-2 rounded-lg bg-siren/15 border border-siren/30 text-siren text-[11px] font-semibold">
                        <AlertTriangle size={14} className="flex-shrink-0" />
                        <span>Mandatory Court Summons (Non-Compoundable on repeat/severe offence)</span>
                      </div>
                    )}
                    {fineResult.maxJailMonths > 0 && (
                      <div className="flex items-center gap-2 p-2 rounded-lg bg-orange-500/15 border border-orange-500/30 text-orange-400 text-[11px] font-semibold">
                        <Gavel size={14} className="flex-shrink-0" />
                        <span>Imprisonment Term: Up to {fineResult.maxJailMonths} months</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Statutory Jurisdiction Box */}
                <div className="p-4 rounded-xl bg-surface/40 border border-white/5 space-y-2 text-xs">
                  <div className="text-[11px] font-bold text-faint uppercase tracking-wider">
                    Statutory Framework
                  </div>
                  <p className="text-ink font-semibold flex items-center gap-1.5">
                    <span>{currentNation.flag}</span>
                    <span>{fineResult.statutoryJurisdiction}</span>
                  </p>
                  <div className="flex items-center justify-between text-[11px] text-mink pt-1 border-t border-white/5 font-mono">
                    <span>Traffic Helpline: <strong className="text-ink">{fineResult.trafficHelpline}</strong></span>
                    <span>Emergency: <strong className="text-cyber">{fineResult.emergencyHelpline}</strong></span>
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : (
        /* ── Bot Viewport ─────────────────────────────────────────────────── */
        <div className="flex-1 flex flex-col bg-abyss/30 max-w-4xl mx-auto w-full p-6 overflow-hidden">
          {/* Quick suggestion pills */}
          <div className="flex items-center gap-2 overflow-x-auto pb-3 flex-shrink-0 scrollbar-none">
            {[
              'Helmet fine kitna hai?',
              'Red light jump penalty',
              'Drunk driving challan',
              'Speeding fine for car',
              'Overloading truck penalty',
              'Ambulance rasta na dene ka fine'
            ].map((prompt, i) => (
              <button
                key={i}
                onClick={() => handleSendBot(prompt)}
                className="px-3 py-1.5 rounded-full bg-surface border border-white/10 text-xs text-mink hover:text-ink hover:border-amber-500/40 transition-colors whitespace-nowrap"
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Chat Messages */}
          <div className="flex-1 overflow-y-auto space-y-4 pr-2 py-2">
            {botMessages.map(msg => {
              const isBot = msg.sender === 'bot';
              return (
                <div
                  key={msg.id}
                  className={cn('flex gap-3 max-w-[85%]', isBot ? 'mr-auto' : 'ml-auto flex-row-reverse')}
                >
                  <div
                    className={cn(
                      'w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold',
                      isBot ? 'bg-amber-500 text-void' : 'bg-cyber text-void'
                    )}
                  >
                    {isBot ? <Sparkles size={14} /> : 'U'}
                  </div>

                  <div
                    className={cn(
                      'p-4 rounded-2xl text-xs space-y-2 leading-relaxed',
                      isBot
                        ? 'bg-surface border border-white/10 text-ink'
                        : 'bg-cyber/15 border border-cyber/30 text-ink'
                    )}
                  >
                    <p className="whitespace-pre-line">{msg.text}</p>
                    <span className="block text-[10px] text-faint font-mono text-right">
                      {msg.timestamp}
                    </span>
                  </div>
                </div>
              );
            })}

            {botSearching && (
              <div className="flex items-center gap-2 text-mink text-xs">
                <RefreshCw size={14} className="animate-spin text-amber-400" />
                <span>Searching 154 statutory BIMSTEC violations...</span>
              </div>
            )}
          </div>

          {/* Chat Input */}
          <div className="pt-3 flex-shrink-0">
            <form
              onSubmit={e => {
                e.preventDefault();
                handleSendBot();
              }}
              className="flex items-center gap-2 bg-surface border border-white/10 rounded-xl p-2 focus-within:border-amber-500 transition-colors"
            >
              <input
                type="text"
                value={botInput}
                onChange={e => setBotInput(e.target.value)}
                placeholder="Ask about traffic fines, sections, penalties (e.g. 'Triple riding fine in Bhopal')..."
                className="flex-1 bg-transparent px-3 py-1.5 text-xs text-ink placeholder-faint outline-none"
              />
              <button
                type="submit"
                disabled={botSearching || !botInput.trim()}
                className="w-9 h-9 rounded-lg bg-amber-500 hover:bg-amber-400 disabled:opacity-40 text-void flex items-center justify-center transition-colors"
              >
                <Send size={15} />
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
