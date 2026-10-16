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

export default function Home() {
  const [activePage, setActivePage] = useState<Page>('dashboard');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const store = useStore();

  const toggleTheme = () => {
    setTheme(t => (t === 'dark' ? 'light' : 'dark'));
  };

  useEffect(() => {
    if (theme === 'light') {
      document.documentElement.classList.add('light-mode');
    } else {
      document.documentElement.classList.remove('light-mode');
    }
  }, [theme]);

  const pendingIncidentsCount = store.incidents.filter(i => i.status === 'pending').length;

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-void font-sans select-none">
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
        />

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
          {activePage === 'notifications' && <NotificationsHubPage store={store} onNavigate={setActivePage} />}
          {activePage === 'settings'      && <SettingsPage />}
        </main>
      </div>
    </div>
  );
}
