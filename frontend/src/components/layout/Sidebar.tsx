'use client';

import {
  LayoutDashboard, AlertOctagon, BarChart3,
  Users, ShieldCheck, Zap,
} from 'lucide-react';
import type { Page } from '@/app/page';
import { cn } from '@/lib/utils';

interface SidebarProps {
  activePage: Page;
  onNavigate: (p: Page) => void;
  activeSosCount: number;
  pendingCount: number;
  connected: boolean;
}

const NAV_ITEMS: {
  id: Page;
  icon: React.ElementType;
  label: string;
  badge?: (p: SidebarProps) => number | null;
  badgeClass?: string;
}[] = [
  {
    id: 'command',
    icon: LayoutDashboard,
    label: 'Command Center',
    badge: p => p.activeSosCount || null,
    badgeClass: 'bg-siren text-white animate-siren-pulse',
  },
  {
    id: 'incidents',
    icon: AlertOctagon,
    label: 'Incidents',
    badge: p => p.pendingCount || null,
    badgeClass: 'bg-siren-orange text-void',
  },
  {
    id: 'analytics',
    icon: BarChart3,
    label: 'Ward Analytics',
  },
  {
    id: 'responders',
    icon: Users,
    label: 'Responders',
  },
];

export default function Sidebar({ activePage, onNavigate, activeSosCount, pendingCount, connected }: SidebarProps) {
  const props = { activePage, onNavigate, activeSosCount, pendingCount, connected };

  return (
    <aside className="w-60 flex-shrink-0 flex flex-col bg-abyss border-r border-white/5 z-10">
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/5">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-civic to-cyber flex items-center justify-center shadow-glow-cyber flex-shrink-0">
          <Zap size={18} className="text-void" />
        </div>
        <div>
          <p className="text-sm font-bold text-ink leading-none">NagarSetu</p>
          <p className="text-[10px] text-mink tracking-widest uppercase mt-0.5">V2 · Command</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-2 py-3 space-y-0.5">
        <p className="section-label">Operations</p>

        {NAV_ITEMS.map(item => {
          const Icon = item.icon;
          const count = item.badge?.(props);
          const isActive = activePage === item.id;

          return (
            <button
              key={item.id}
              onClick={() => onNavigate(item.id)}
              className={cn(
                'nav-item w-full text-left',
                isActive && 'active'
              )}
            >
              <Icon size={16} className={cn('flex-shrink-0', isActive ? 'text-cyber' : 'text-faint')} />
              <span className="flex-1 truncate">{item.label}</span>
              {count != null && count > 0 && (
                <span className={cn(
                  'text-[10px] font-bold min-w-[20px] h-5 rounded-full flex items-center justify-center px-1.5',
                  item.badgeClass ?? 'bg-muted text-mink'
                )}>
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* System Status Footer */}
      <div className="px-4 py-3 border-t border-white/5">
        <div className="flex items-center gap-2">
          <div className={connected ? 'dot-live' : 'dot-offline'} />
          <span className="text-xs text-mink">
            {connected ? 'Backend Live' : 'Mock Data Mode'}
          </span>
        </div>
        <div className="flex items-center gap-1 mt-2 text-[10px] text-faint">
          <ShieldCheck size={10} />
          <span>NagarSetu V2.0 · Bhopal</span>
        </div>
      </div>
    </aside>
  );
}
