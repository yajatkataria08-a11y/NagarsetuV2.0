'use client';

import { useState } from 'react';
import {
  LayoutDashboard, AlertOctagon, Map, Video, ShieldAlert,
  ClipboardList, BarChart3, Building2, Users, Bell, Settings,
  Zap, Sun, Moon, ShieldCheck, Scale, Landmark
} from 'lucide-react';
import { cn } from '@/lib/utils';

export type Page =
  | 'dashboard'
  | 'sos'
  | 'map'
  | 'surveillance'
  | 'hazards'
  | 'infra'
  | 'incidents'
  | 'analytics'
  | 'wards'
  | 'users'
  | 'legal'
  | 'notifications'
  | 'settings';

interface SidebarProps {
  activePage: Page;
  onNavigate: (p: Page) => void;
  activeSosCount: number;
  pendingCount: number;
  unreadNotifsCount: number;
  connected: boolean;
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
}

const NAV_ITEMS: {
  id: Page;
  icon: React.ElementType;
  label: string;
  section: string;
  badge?: (p: SidebarProps) => number | null;
  badgeClass?: string;
}[] = [
  // Overview
  { id: 'dashboard',    icon: LayoutDashboard, label: 'Overview',           section: 'COMMAND' },
  { id: 'sos',          icon: AlertOctagon,    label: 'SOS Monitor',        section: 'COMMAND', badge: p => p.activeSosCount || null, badgeClass: 'bg-siren text-white animate-siren-pulse' },
  { id: 'map',          icon: Map,              label: 'Live GIS Map',       section: 'COMMAND' },
  
  // Tactical Intelligence
  { id: 'surveillance', icon: Video,            label: 'CCTV Vision Wall',   section: 'TACTICAL INTELLIGENCE', badge: () => 2, badgeClass: 'bg-siren text-white' },
  { id: 'hazards',      icon: ShieldAlert,      label: 'Hazard Geofences',   section: 'TACTICAL INTELLIGENCE' },
  { id: 'infra',        icon: Landmark,         label: 'Infrastructure Health', section: 'TACTICAL INTELLIGENCE' },


  // Operations
  { id: 'incidents',    icon: ClipboardList,    label: 'Incidents & Triage', section: 'MUNICIPAL OPERATIONS', badge: p => p.pendingCount || null, badgeClass: 'bg-siren-orange text-void' },
  { id: 'analytics',    icon: BarChart3,        label: 'Analytics & Trends', section: 'MUNICIPAL OPERATIONS' },
  { id: 'wards',        icon: Building2,        label: 'Ward & Officers',    section: 'MUNICIPAL OPERATIONS' },
  { id: 'users',        icon: Users,            label: 'Citizen Registry',   section: 'MUNICIPAL OPERATIONS' },
  { id: 'legal',        icon: Scale,            label: 'DriveLegal BIMSTEC', section: 'MUNICIPAL OPERATIONS', badge: () => 154, badgeClass: 'bg-amber-500/20 text-amber-400 border border-amber-500/40' },

  // System
  { id: 'notifications',icon: Bell,             label: 'Notifications',      section: 'SYSTEM', badge: p => p.unreadNotifsCount || null, badgeClass: 'bg-cyber text-void' },
  { id: 'settings',     icon: Settings,         label: 'Settings & APIs',    section: 'SYSTEM' },
];

export default function Sidebar(props: SidebarProps) {
  const { activePage, onNavigate, connected, theme, onToggleTheme } = props;

  let currentSection = '';

  return (
    <aside className="w-64 flex-shrink-0 flex flex-col bg-abyss border-r border-white/5 z-20">
      {/* Brand Header */}
      <div className="flex items-center justify-between px-4 py-4 border-b border-white/5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-civic to-cyber flex items-center justify-center shadow-glow-cyber flex-shrink-0">
            <Zap size={20} className="text-void" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-base font-extrabold text-ink tracking-tight">NagarSetu</span>
              <span className="text-[10px] font-mono px-1.5 py-0.2 bg-cyber/15 text-cyber border border-cyber/30 rounded font-bold">V2.0</span>
            </div>
            <p className="text-[10px] text-mink tracking-wider uppercase font-semibold">Municipal Command Center</p>
          </div>
        </div>
      </div>

      {/* Nav List */}
      <nav className="flex-1 px-3 py-3 overflow-y-auto space-y-1">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const count = item.badge?.(props);
          const isActive = activePage === item.id;
          const showSection = item.section !== currentSection;
          if (showSection) currentSection = item.section;

          return (
            <div key={item.id}>
              {showSection && (
                <p className="text-[10px] font-bold tracking-[0.14em] uppercase text-faint px-3 pt-3 pb-1">
                  {item.section}
                </p>
              )}
              <button
                onClick={() => onNavigate(item.id)}
                className={cn(
                  'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all duration-150',
                  isActive
                    ? 'bg-cyber/15 text-cyber border-l-2 border-cyber shadow-glow-cyber/20 font-bold'
                    : 'text-mink hover:bg-raised/70 hover:text-ink'
                )}
              >
                <Icon size={16} className={cn('flex-shrink-0', isActive ? 'text-cyber' : 'text-faint')} />
                <span className="flex-1 text-left truncate">{item.label}</span>
                {count != null && count > 0 && (
                  <span className={cn(
                    'text-[10px] font-bold min-w-[20px] h-5 rounded-full flex items-center justify-center px-1.5',
                    item.badgeClass ?? 'bg-muted text-mink'
                  )}>
                    {count}
                  </span>
                )}
              </button>
            </div>
          );
        })}
      </nav>

      {/* Footer Controls: Theme Toggle & Connectivity */}
      <div className="px-4 py-3 border-t border-white/5 bg-surface/40 space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className={connected ? 'dot-live' : 'dot-offline'} />
            <span className="text-xs text-mink font-medium">
              {connected ? 'Socket.IO Live' : 'Demo Mode'}
            </span>
          </div>

          <button
            onClick={onToggleTheme}
            className="p-1.5 rounded-lg bg-raised text-mink hover:text-ink border border-white/5 transition-colors"
            title="Toggle Light / Dark Mode"
          >
            {theme === 'dark' ? <Sun size={14} className="text-caution" /> : <Moon size={14} className="text-cyber" />}
          </button>
        </div>

        <div className="flex items-center gap-1.5 text-[10px] text-faint font-mono">
          <ShieldCheck size={11} className="text-safe" />
          <span>Bhopal & Indore Municipal Corp</span>
        </div>
      </div>
    </aside>
  );
}
