'use client';

import { useState, useEffect } from 'react';
import { useStore } from '@/hooks/useStore';
import Sidebar, { type Page } from '@/components/layout/Sidebar';
import TopBar from '@/components/layout/TopBar';

// ── 11 Authentic NagarSetu + V2 Tactical Pages ──────────────────────────────
import DashboardOverviewPage from '@/components/pages/DashboardOverviewPage';
import SosPage from '@/components/pages/SosPage';
import LiveGisMapPage from '@/components/pages/LiveGisMapPage';
import SurveillancePage from '@/components/pages/SurveillancePage';
import HazardsPage from '@/components/pages/HazardsPage';
import IncidentsPage from '@/components/pages/IncidentsPage';
import AnalyticsPage from '@/components/pages/AnalyticsPage';
import WardsLeaderboardPage from '@/components/pages/WardsLeaderboardPage';
import CitizenRegistryPage from '@/components/pages/CitizenRegistryPage';
import NotificationsHubPage from '@/components/pages/NotificationsHubPage';
import SettingsPage from '@/components/pages/SettingsPage';
import DriveLegalPage from '@/components/pages/DriveLegalPage';

export default function Home() {
  const [activePage, setActivePage] = useState<Page>('dashboard');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const store = useStore();

  useEffect(() => {
    const saved = localStorage.getItem('nagarsetu_theme') as 'dark' | 'light' | null;
    if (saved) {
      setTheme(saved);
      if (saved === 'light') {
        document.documentElement.classList.remove('dark');
        document.documentElement.classList.add('light');
      } else {
        document.documentElement.classList.add('dark');
        document.documentElement.classList.remove('light');
      }
    }
  }, []);

  const toggleTheme = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('nagarsetu_theme', next);
    if (next === 'light') {
      document.documentElement.classList.remove('dark');
      document.documentElement.classList.add('light');
    } else {
      document.documentElement.classList.add('dark');
      document.documentElement.classList.remove('light');
    }
  };

  const pendingIncidentsCount = store.incidents.filter(i => i.status === 'pending').length;

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-void font-sans select-none transition-colors duration-200">
      {/* ── Left Sidebar Navigation (All 11 Modules) ──────────────────── */}
      <Sidebar
        activePage={activePage}
        onNavigate={setActivePage}
        activeSosCount={store.activeSos.length}
        pendingCount={pendingIncidentsCount}
        unreadNotifsCount={store.unreadNotifsCount}
        connected={store.connected}
        theme={theme}
        onToggleTheme={toggleTheme}
      />

      {/* ── Main Content Area ────────────────────────────────────────── */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        {/* Dynamic Header */}
        <TopBar
          activePage={activePage}
          connected={store.connected}
          activeSosCount={store.activeSos.length}
          theme={theme}
          onToggleTheme={toggleTheme}
        />

        {/* ── Prominent DEMO DATA / OFFLINE Banner ───────────────────────── */}
        {!store.connected && (
          <div className="flex-shrink-0 flex items-center justify-between px-5 py-2 bg-gradient-to-r from-amber-500/15 via-amber-500/10 to-transparent border-b border-amber-500/30 text-amber-300 text-xs font-medium z-10">
            <div className="flex items-center gap-2.5">
              <span className="flex h-2.5 w-2.5 rounded-full bg-amber-400 animate-ping" />
              <span className="font-mono font-bold tracking-wider uppercase text-[11px] bg-amber-500/20 px-2 py-0.5 rounded border border-amber-500/30 text-amber-300">
                DEMO DATA / OFFLINE
              </span>
              <span className="text-amber-200/90 text-xs">
                Backend API Gateway is offline or running in mock simulation mode. Real-time PostGIS sync is idle.
              </span>
            </div>
            <div className="flex items-center gap-2 text-[11px] font-mono text-amber-400/80">
              <span className="hidden sm:inline">Local Synthetic Store Active</span>
              <span className="px-1.5 py-0.5 bg-amber-500/20 rounded text-[10px]">LOCAL SEED</span>
            </div>
          </div>
        )}

        {/* Viewport Router */}
        <main className="flex-1 overflow-hidden relative">
          {activePage === 'dashboard'     && <DashboardOverviewPage store={store} onNavigate={setActivePage} />}
          {activePage === 'sos'           && <SosPage store={store} />}
          {activePage === 'map'           && <LiveGisMapPage store={store} />}
          {activePage === 'surveillance'  && <SurveillancePage />}
          {activePage === 'hazards'       && <HazardsPage />}
          {activePage === 'incidents'     && <IncidentsPage store={store} />}
          {activePage === 'analytics'     && <AnalyticsPage store={store} />}
          {activePage === 'wards'         && <WardsLeaderboardPage store={store} />}
          {activePage === 'users'         && <CitizenRegistryPage store={store} />}
          {activePage === 'legal'         && <DriveLegalPage />}
          {activePage === 'notifications' && <NotificationsHubPage store={store} onNavigate={setActivePage} />}
          {activePage === 'settings'      && <SettingsPage />}
        </main>
      </div>
    </div>
  );
}
