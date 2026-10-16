'use client';

import { Radio, BellRing, Clock } from 'lucide-react';
import type { Page } from '@/app/page';
import { cn } from '@/lib/utils';

const PAGE_TITLES: Record<Page, { title: string; subtitle: string }> = {
  command:    { title: 'Command Center',  subtitle: 'Real-time SOS monitoring & incident triage' },
  incidents:  { title: 'Incidents',       subtitle: 'Civic reports — AI verification & dispatch' },
  analytics:  { title: 'Ward Analytics',  subtitle: 'Performance metrics & SLA tracking' },
  responders: { title: 'Responders',      subtitle: 'Field officer roster & assignment' },
};

interface TopBarProps {
  activePage: Page;
  connected: boolean;
  activeSosCount: number;
}

export default function TopBar({ activePage, connected, activeSosCount }: TopBarProps) {
  const { title, subtitle } = PAGE_TITLES[activePage];
  const now = new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });

  return (
    <header className="flex-shrink-0 h-14 flex items-center gap-4 px-5 bg-abyss border-b border-white/5">
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
        <span>{connected ? 'Live' : 'Offline'}</span>
      </div>

      {/* Clock */}
      <div className="flex items-center gap-1.5 text-xs font-mono text-faint">
        <Clock size={11} />
        <span className="tabular-nums">{now}</span>
      </div>
    </header>
  );
}
