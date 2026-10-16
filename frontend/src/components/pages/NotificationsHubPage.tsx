'use client';

import { useState } from 'react';
import {
  Bell, AlertOctagon, ClipboardList, ShieldAlert,
  Settings, CheckCircle2, ArrowRight
} from 'lucide-react';
import { cn, timeAgo } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';
import type { Page } from '@/components/layout/Sidebar';

type Store = ReturnType<typeof useStore>;

interface Props {
  store: Store;
  onNavigate: (p: Page) => void;
}

export default function NotificationsHubPage({ store, onNavigate }: Props) {
  const [activeTab, setActiveTab] = useState<'all' | 'sos' | 'incident' | 'alert' | 'system'>('all');

  const filtered = store.notifications.filter(n =>
    activeTab === 'all' ? true : n.type === activeTab
  ).sort((a, b) => b.timestamp - a.timestamp);

  const unreadCount = store.notifications.filter(n => !n.read).length;

  return (
    <div className="flex flex-col h-full overflow-hidden bg-void">
      {/* ── 1. Top Header & Tabs ──────────────────────────────────────── */}
      <div className="flex-shrink-0 p-4 border-b border-white/5 bg-abyss flex flex-wrap items-center justify-between gap-3">
        {/* Tabs */}
        <div className="flex items-center gap-1 bg-surface p-1 rounded-lg border border-white/10">
          <button
            onClick={() => setActiveTab('all')}
            className={cn(
              'px-3 py-1.5 rounded text-xs font-semibold transition-all',
              activeTab === 'all' ? 'bg-raised text-ink font-bold' : 'text-mink hover:text-ink'
            )}
          >
            All Notifications
          </button>
          <button
            onClick={() => setActiveTab('sos')}
            className={cn(
              'px-3 py-1.5 rounded text-xs font-semibold transition-all flex items-center gap-1.5',
              activeTab === 'sos' ? 'bg-siren/20 text-siren font-bold' : 'text-mink hover:text-ink'
            )}
          >
            🚨 SOS Live
          </button>
          <button
            onClick={() => setActiveTab('incident')}
            className={cn(
              'px-3 py-1.5 rounded text-xs font-semibold transition-all flex items-center gap-1.5',
              activeTab === 'incident' ? 'bg-cyber/20 text-cyber font-bold' : 'text-mink hover:text-ink'
            )}
          >
            📋 Incidents
          </button>
          <button
            onClick={() => setActiveTab('alert')}
            className={cn(
              'px-3 py-1.5 rounded text-xs font-semibold transition-all flex items-center gap-1.5',
              activeTab === 'alert' ? 'bg-siren-orange/20 text-siren-orange font-bold' : 'text-mink hover:text-ink'
            )}
          >
            ⚠️ Alerts & Hazards
          </button>
          <button
            onClick={() => setActiveTab('system')}
            className={cn(
              'px-3 py-1.5 rounded text-xs font-semibold transition-all flex items-center gap-1.5',
              activeTab === 'system' ? 'bg-surface text-ink font-bold' : 'text-mink hover:text-ink'
            )}
          >
            ⚙️ System
          </button>
        </div>

        {/* Mark All Read */}
        {unreadCount > 0 && (
          <button
            onClick={store.markAllNotificationsRead}
            className="btn-ghost text-xs py-1 px-3 border border-white/10 flex items-center gap-1.5 text-mink hover:text-ink"
          >
            <CheckCircle2 size={13} className="text-safe" />
            Mark all read ({unreadCount})
          </button>
        )}
      </div>

      {/* ── 2. Notifications Feed ─────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto p-5 space-y-3">
        {filtered.length === 0 ? (
          <div className="text-center py-16 space-y-2">
            <Bell size={32} className="mx-auto text-faint" />
            <p className="text-sm font-semibold text-mink">No notifications in this category</p>
            <p className="text-xs text-faint">Real-time alerts will trigger as municipal events occur.</p>
          </div>
        ) : (
          filtered.map((n) => {
            const isUnread = !n.read;

            return (
              <div
                key={n.id}
                onClick={() => store.markNotificationRead(n.id)}
                className={cn(
                  'glass-card p-4 transition-all duration-150 flex items-start justify-between gap-4 cursor-pointer',
                  isUnread
                    ? n.priority === 'critical'
                      ? 'border-l-4 border-l-siren bg-siren/5'
                      : 'border-l-4 border-l-cyber bg-cyber/5'
                    : 'border-l-4 border-l-transparent opacity-80 hover:opacity-100'
                )}
              >
                <div className="flex items-start gap-3.5">
                  <div
                    className={cn(
                      'w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5',
                      n.type === 'sos' && 'bg-siren/15 text-siren',
                      n.type === 'incident' && 'bg-cyber/15 text-cyber',
                      n.type === 'alert' && 'bg-siren-orange/15 text-siren-orange',
                      n.type === 'system' && 'bg-raised text-mink'
                    )}
                  >
                    {n.type === 'sos' && <AlertOctagon size={18} className="animate-siren-pulse" />}
                    {n.type === 'incident' && <ClipboardList size={18} />}
                    {n.type === 'alert' && <ShieldAlert size={18} />}
                    {n.type === 'system' && <Settings size={18} />}
                  </div>

                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-ink">{n.title}</span>
                      {isUnread && (
                        <span className="w-1.5 h-1.5 rounded-full bg-cyber" />
                      )}
                      <span
                        className={cn(
                          'text-[10px] px-1.5 py-0.2 rounded font-mono font-bold uppercase',
                          n.priority === 'critical' && 'bg-siren/20 text-siren',
                          n.priority === 'high' && 'bg-siren-orange/20 text-siren-orange',
                          n.priority === 'normal' && 'bg-surface text-faint'
                        )}
                      >
                        {n.priority}
                      </span>
                    </div>

                    <p className="text-xs text-mink leading-relaxed max-w-2xl">{n.message}</p>
                    <span className="text-[10px] text-faint font-mono block">{timeAgo(n.timestamp)}</span>
                  </div>
                </div>

                {n.targetPage && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      store.markNotificationRead(n.id);
                      onNavigate(n.targetPage as Page);
                    }}
                    className="btn-ghost text-xs py-1.5 px-3 flex items-center gap-1 text-cyber hover:bg-cyber/10 flex-shrink-0"
                  >
                    <span>View</span>
                    <ArrowRight size={13} />
                  </button>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
