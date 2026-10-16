'use client';

import { useState } from 'react';
import { useStore } from '@/hooks/useStore';
import Sidebar from '@/components/layout/Sidebar';
import TopBar from '@/components/layout/TopBar';
import CommandPage from '@/components/pages/CommandPage';
import IncidentsPage from '@/components/pages/IncidentsPage';
import AnalyticsPage from '@/components/pages/AnalyticsPage';
import RespondersPage from '@/components/pages/RespondersPage';

export type Page = 'command' | 'incidents' | 'analytics' | 'responders';

export default function Home() {
  const [activePage, setActivePage] = useState<Page>('command');
  const store = useStore();

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-void">
      {/* Left Sidebar Navigation */}
      <Sidebar
        activePage={activePage}
        onNavigate={setActivePage}
        activeSosCount={store.activeSos.length}
        pendingCount={store.incidents.filter(i => i.status === 'pending').length}
        connected={store.connected}
      />

      {/* Main Content Area */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        {/* Top Header Bar */}
        <TopBar
          activePage={activePage}
          connected={store.connected}
          activeSosCount={store.activeSos.length}
        />

        {/* Page Content */}
        <main className="flex-1 overflow-hidden">
          {activePage === 'command'    && <CommandPage   store={store} />}
          {activePage === 'incidents'  && <IncidentsPage store={store} />}
          {activePage === 'analytics'  && <AnalyticsPage store={store} />}
          {activePage === 'responders' && <RespondersPage store={store} />}
        </main>
      </div>
    </div>
  );
}
