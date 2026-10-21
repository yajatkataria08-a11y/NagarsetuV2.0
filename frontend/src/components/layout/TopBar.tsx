'use client';

import { Radio, BellRing, Clock } from 'lucide-react';
import type { Page } from '@/components/layout/Sidebar';
import { cn } from '@/lib/utils';

const PAGE_TITLES: Record<Page, { title: string; subtitle: string }> = {
  dashboard:     { title: 'Municipal Command Overview',         subtitle: 'Real-time citywide pulse, critical alerts & incident metrics' },
  sos:           { title: 'SOS Monitoring & Siren Operations',  subtitle: 'Live emergency beacons, vital telemetry & rapid dispatch' },
  map:           { title: 'Tactical GIS Map',                   subtitle: 'Dual Bhopal-Indore GIS layers, risk heatmaps & fly-to-ward' },
  surveillance:  { title: 'Tactical 6-Camera Vision Wall',      subtitle: 'Zero-scroll municipal CCTV grid with automated YOLOv8 hazard detection' },
  hazards:       { title: 'Dynamic Hazard Geofences',           subtitle: 'Temporary danger zones with automated TTL expiration & broadcast' },

  incidents:     { title: 'Incidents Management & Triage',      subtitle: 'Multi-select bulk verification, officer assignment & AI triage' },
  analytics:     { title: 'Ward Analytics & SLA Metrics',       subtitle: 'Performance benchmarks, complaint volume & resolution rates' },
  wards:         { title: 'Ward & Officer Leaderboard',         subtitle: 'Ward rankings, municipal budget tracking & officer dispatch roster' },
  users:         { title: 'Citizen Trust Registry',             subtitle: 'Citizen reliability scoring, spam detection & moderation tools' },
  legal:         { title: 'DriveLegal™ BIMSTEC Knowledge Base', subtitle: '154 codified traffic violations across 7 nations with dynamic fine resolution' },
  notifications: { title: 'Municipal Notifications Hub',       subtitle: 'Real-time multi-channel incident, SOS & system alerts' },
  settings:      { title: 'System Configuration & APIs',        subtitle: 'Supabase PostGIS, Firebase sync, AI thresholds & native app bridge' },
};

interface TopBarProps {
  activePage: Page;
  connected: boolean;
  activeSosCount: number;
}

export default function TopBar({ activePage, connected, activeSosCount }: TopBarProps) {
  const { title, subtitle } = PAGE_TITLES[activePage] || {
    title: 'Command Center',
    subtitle: 'NagarSetu Municipal Command Center',
  };
  const now = new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });

  return (
    <header className="flex-shrink-0 h-14 flex items-center gap-4 px-5 bg-abyss border-b border-white/5 z-10">
      {/* Title */}
      <div className="flex-1 min-w-0">
        <h1 className="text-sm font-bold text-ink leading-none">{title}</h1>
        <p className="text-[11px] text-mink mt-0.5 truncate">{subtitle}</p>
      </div>

      {/* Active SOS indicator */}
      {activeSosCount > 0 && (
        <div className="flex items-center gap-2 bg-siren/10 border border-siren/30 rounded-lg px-3 py-1.5 animate-siren-pulse">
          <BellRing size={12} className="text-siren" />
          <span className="text-xs font-bold text-siren">
            {activeSosCount} ACTIVE SOS
          </span>
        </div>
      )}

      {/* Connection Pill */}
      <div className={cn(
        'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium border',
        connected
          ? 'bg-safe/10 border-safe/30 text-safe'
          : 'bg-muted/40 border-white/10 text-mink'
      )}>
        <Radio size={11} className={connected ? 'animate-live-dot' : ''} />
        <span>{connected ? 'Live PostGIS' : 'Demo State'}</span>
      </div>

      {/* Clock */}
      <div className="flex items-center gap-1.5 text-xs font-mono text-faint">
        <Clock size={11} />
        <span className="tabular-nums">{now}</span>
      </div>
    </header>
  );
}
