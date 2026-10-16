'use client';

import { useEffect, useRef, useState } from 'react';
import {
  MapPin, AlertOctagon, Flame, Shield, Layers,
  Compass, Crosshair, RefreshCw, Search, Building2, ChevronRight
} from 'lucide-react';
import { cn } from '@/lib/utils';
import type { useStore } from '@/hooks/useStore';

type Store = ReturnType<typeof useStore>;

type MapMode = 'incidents' | 'sos' | 'heat' | 'all';
type City = 'bhopal' | 'indore';

const CITY_COORDS: Record<City, [number, number]> = {
  bhopal: [23.2599, 77.4126],
  indore: [22.7196, 75.8577],
};

const WARD_LOCATIONS: { name: string; city: City; lat: number; lng: number; count: number }[] = [
  { name: 'Ward 3 — MP Nagar', city: 'bhopal', lat: 23.2310, lng: 77.4350, count: 45 },
  { name: 'Ward 5 — TT Nagar', city: 'bhopal', lat: 23.2430, lng: 77.4190, count: 55 },
  { name: 'Ward 7 — Arera Colony', city: 'bhopal', lat: 23.2599, lng: 77.4126, count: 62 },
  { name: 'Ward 9 — Habibganj', city: 'bhopal', lat: 23.2680, lng: 77.4300, count: 28 },
  { name: 'Ward 12 — Kolar', city: 'bhopal', lat: 23.1850, lng: 77.4200, count: 71 },
  { name: 'Ward 18 — Rajwada', city: 'indore', lat: 22.7185, lng: 75.8560, count: 34 },
  { name: 'Ward 24 — Vijay Nagar', city: 'indore', lat: 22.7533, lng: 75.8937, count: 52 },
  { name: 'Ward 31 — Palasia', city: 'indore', lat: 22.7230, lng: 75.8820, count: 41 },
];

export default function LiveGisMapPage({ store }: { store: Store }) {
  const [currentCity, setCurrentCity] = useState<City>('bhopal');
  const [mapMode, setMapMode] = useState<MapMode>('all');
  const [showIncidents, setShowIncidents] = useState(true);
  const [showSos, setShowSos] = useState(true);
  const [showHeat, setShowHeat] = useState(true);
  const [showWards, setShowWards] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [coordsDisplay, setCoordsDisplay] = useState('23.2599° N, 77.4126° E');
  const [selectedWard, setSelectedWard] = useState<string | null>(null);

  const mapRef = useRef<HTMLDivElement>(null);
  const leafRef = useRef<any>(null);
  const mapInst = useRef<any>(null);
  const markersLayerRef = useRef<any>(null);

  // ── Bootstrap Leaflet map ────────────────────────────────────────────────
  useEffect(() => {
    if (!mapRef.current || mapInst.current) return;

    import('leaflet').then((mod) => {
      const L = (mod as any).default || mod;
      leafRef.current = L;

      const map = L.map(mapRef.current!, {
        center: CITY_COORDS[currentCity],
        zoom: 13,
        zoomControl: false,
        attributionControl: false,
      });

      // Dark tactical tiles
      L.tileLayer(
        'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
        { maxZoom: 19 }
      ).addTo(map);

      L.control.zoom({ position: 'bottomright' }).addTo(map);

      map.on('mousemove', (e: any) => {
        setCoordsDisplay(`${e.latlng.lat.toFixed(4)}° N, ${e.latlng.lng.toFixed(4)}° E`);
      });

      markersLayerRef.current = L.layerGroup().addTo(map);
      mapInst.current = map;
      renderMarkers();
    });

    return () => {
      mapInst.current?.remove();
      mapInst.current = null;
    };
  }, []);

  // ── Render markers when data or filters change ───────────────────────────
  const renderMarkers = () => {
    const L = leafRef.current;
    const map = mapInst.current;
    const layer = markersLayerRef.current;
    if (!L || !map || !layer) return;

    layer.clearLayers();

    // 1. SOS Live Markers (Orange/Red pulsing)
    if (showSos && (mapMode === 'sos' || mapMode === 'all')) {
      store.sos
        .filter((s) => s.status === 'active' || s.status === 'acknowledged')
        .forEach((sos) => {
          const icon = L.divIcon({
            className: 'sos-marker',
            html: `
              <div style="position:relative;width:34px;height:34px;">
                <div style="position:absolute;inset:0;background:rgba(255,45,85,0.4);border-radius:50%;animation:ping 1.5s cubic-bezier(0,0,0.2,1) infinite;"></div>
                <div style="position:absolute;inset:4px;background:#FF2D55;border:2px solid #fff;border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 0 12px #FF2D55;">
                  <span style="font-size:13px;">🚨</span>
                </div>
              </div>
            `,
            iconSize: [34, 34],
            iconAnchor: [17, 17],
          });

          const m = L.marker([sos.location.lat, sos.location.lng], { icon });
          m.bindPopup(`
            <div style="background:#0F172A;color:#F8FAFC;padding:12px;border-radius:8px;font-family:sans-serif;font-size:12px;min-width:200px;border:1px solid rgba(255,45,85,0.4);">
              <div style="font-weight:bold;color:#FF6200;font-size:13px;margin-bottom:4px;">🚨 ACTIVE SOS: ${sos.userName}</div>
              <div><strong>Ward:</strong> ${sos.ward}</div>
              <div><strong>Phone:</strong> ${sos.phone}</div>
              <div><strong>Battery:</strong> ${sos.batteryLevel}%</div>
              ${sos.heartRate ? `<div><strong>Heart Rate:</strong> ❤️ ${sos.heartRate} BPM</div>` : ''}
              <div style="margin-top:8px;font-size:10px;color:#94A3B8;">${sos.address || 'GPS Fix locked'}</div>
            </div>
          `);
          layer.addLayer(m);
        });
    }

    // 2. Incidents Markers (Civic Cyan / Amber)
    if (showIncidents && (mapMode === 'incidents' || mapMode === 'all')) {
      store.incidents
        .filter((i) => i.status !== 'resolved' && i.status !== 'rejected')
        .forEach((inc) => {
          if (searchQuery.trim()) {
            const q = searchQuery.toLowerCase();
            if (!inc.description.toLowerCase().includes(q) && !inc.ward.toLowerCase().includes(q)) {
              return;
            }
          }

          const isCritical = inc.severity >= 4;
          const bgCol = isCritical ? '#FF6200' : '#00D4FF';

          const icon = L.divIcon({
            className: 'inc-marker',
            html: `
              <div style="width:24px;height:24px;background:${bgCol};border:2px solid #060B14;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:bold;color:#060B14;box-shadow:0 0 8px ${bgCol};">
                ${inc.severity}
              </div>
            `,
            iconSize: [24, 24],
            iconAnchor: [12, 12],
          });

          const m = L.marker([inc.location.lat, inc.location.lng], { icon });
          m.bindPopup(`
            <div style="background:#0F172A;color:#F8FAFC;padding:12px;border-radius:8px;font-family:sans-serif;font-size:12px;min-width:220px;border:1px solid rgba(0,212,255,0.3);">
              <div style="font-weight:bold;color:#00D4FF;margin-bottom:4px;">${inc.type.replace('_', ' ')}</div>
              <p style="margin-bottom:6px;line-height:1.4;">${inc.description}</p>
              <div><strong>Ward:</strong> ${inc.ward}</div>
              <div><strong>Severity:</strong> Level ${inc.severity}/5</div>
              <div><strong>Status:</strong> ${inc.status}</div>
            </div>
          `);
          layer.addLayer(m);
        });
    }

    // 3. Risk Heatmap Circles
    if (showHeat && (mapMode === 'heat' || mapMode === 'all')) {
      store.incidents.forEach((inc) => {
        const circle = L.circle([inc.location.lat, inc.location.lng], {
          radius: inc.severity * 120,
          color: inc.severity >= 4 ? '#FF2D55' : '#FF6200',
          fillColor: inc.severity >= 4 ? '#FF2D55' : '#FF6200',
          fillOpacity: 0.18,
          weight: 1,
        });
        layer.addLayer(circle);
      });
    }

    // 4. Ward Boundaries / Markers
    if (showWards && (mapMode === 'all' || mapMode === 'heat')) {
      WARD_LOCATIONS.filter((w) => w.city === currentCity).forEach((w) => {
        const circle = L.circle([w.lat, w.lng], {
          radius: 600,
          color: '#818CF8',
          fillColor: '#818CF8',
          fillOpacity: 0.08,
          weight: 2,
          dashArray: '4, 4',
        });
        layer.addLayer(circle);
      });
    }
  };

  useEffect(() => {
    renderMarkers();
  }, [mapMode, showIncidents, showSos, showHeat, showWards, searchQuery, store.incidents, store.sos, currentCity]);

  // ── Switch City ──────────────────────────────────────────────────────────
  const handleCitySwitch = (city: City) => {
    setCurrentCity(city);
    if (mapInst.current) {
      mapInst.current.flyTo(CITY_COORDS[city], 13, { duration: 1.2 });
    }
  };

  // ── Fly to Ward ──────────────────────────────────────────────────────────
  const flyToWard = (lat: number, lng: number, name: string) => {
    setSelectedWard(name);
    if (mapInst.current) {
      mapInst.current.flyTo([lat, lng], 15, { duration: 1.5 });
    }
  };

  const locateMe = () => {
    if (navigator.geolocation && mapInst.current) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          mapInst.current?.flyTo([pos.coords.latitude, pos.coords.longitude], 15);
        },
        () => alert('Could not retrieve current location')
      );
    }
  };

  const activeSosCount = store.sos.filter((s) => s.status === 'active' || s.status === 'acknowledged').length;
  const activeIncCount = store.incidents.filter((i) => i.status !== 'resolved' && i.status !== 'rejected').length;
  const resolvedCount = store.incidents.filter((i) => i.status === 'resolved').length;

  return (
    <div className="flex flex-col h-full overflow-hidden bg-void">
      {/* ── 1. Top City & Map Mode Bar ───────────────────────────────── */}
      <div className="flex-shrink-0 px-4 py-2.5 bg-abyss border-b border-white/5 flex flex-wrap items-center justify-between gap-3">
        {/* City Toggle */}
        <div className="flex items-center gap-1 bg-surface p-1 rounded-lg border border-white/10">
          <button
            onClick={() => handleCitySwitch('bhopal')}
            className={cn(
              'px-3 py-1 rounded text-xs font-bold transition-all',
              currentCity === 'bhopal'
                ? 'bg-civic text-white shadow-glow-civic'
                : 'text-mink hover:text-ink'
            )}
          >
            🏛️ Bhopal Central
          </button>
          <button
            onClick={() => handleCitySwitch('indore')}
            className={cn(
              'px-3 py-1 rounded text-xs font-bold transition-all',
              currentCity === 'indore'
                ? 'bg-cyber text-void shadow-glow-cyber font-black'
                : 'text-mink hover:text-ink'
            )}
          >
            🏙️ Indore SmartCity
          </button>
        </div>

        {/* Mode Switcher */}
        <div className="flex items-center gap-1 bg-surface p-1 rounded-lg border border-white/10">
          <button
            onClick={() => setMapMode('incidents')}
            className={cn(
              'px-2.5 py-1 rounded text-xs font-semibold transition-all',
              mapMode === 'incidents' ? 'bg-raised text-ink' : 'text-mink hover:text-ink'
            )}
          >
            📍 Incidents
          </button>
          <button
            onClick={() => setMapMode('sos')}
            className={cn(
              'px-2.5 py-1 rounded text-xs font-semibold transition-all',
              mapMode === 'sos' ? 'bg-siren/20 text-siren' : 'text-mink hover:text-ink'
            )}
          >
            🚨 SOS Live
          </button>
          <button
            onClick={() => setMapMode('heat')}
            className={cn(
              'px-2.5 py-1 rounded text-xs font-semibold transition-all',
              mapMode === 'heat' ? 'bg-siren-orange/20 text-siren-orange' : 'text-mink hover:text-ink'
            )}
          >
            🌡️ Risk Heat
          </button>
          <button
            onClick={() => setMapMode('all')}
            className={cn(
              'px-2.5 py-1 rounded text-xs font-semibold transition-all',
              mapMode === 'all' ? 'bg-cyber/20 text-cyber font-bold' : 'text-mink hover:text-ink'
            )}
          >
            🗺️ All Layers
          </button>
        </div>

        {/* Layer Checkboxes */}
        <div className="flex items-center gap-3 text-xs text-mink">
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={showIncidents}
              onChange={(e) => setShowIncidents(e.target.checked)}
              className="accent-civic"
            />
            <span>Incidents</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={showSos}
              onChange={(e) => setShowSos(e.target.checked)}
              className="accent-siren"
            />
            <span>SOS</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={showHeat}
              onChange={(e) => setShowHeat(e.target.checked)}
              className="accent-siren-orange"
            />
            <span>Heatmap</span>
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={showWards}
              onChange={(e) => setShowWards(e.target.checked)}
              className="accent-purple-400"
            />
            <span>Wards</span>
          </label>
        </div>

        {/* Locate & Search */}
        <div className="flex items-center gap-2">
          <button
            onClick={locateMe}
            className="p-1.5 rounded-lg bg-surface hover:bg-raised text-mink hover:text-ink border border-white/5 transition-colors"
            title="Locate Me"
          >
            <Crosshair size={14} />
          </button>
          <button
            onClick={renderMarkers}
            className="p-1.5 rounded-lg bg-surface hover:bg-raised text-mink hover:text-ink border border-white/5 transition-colors"
            title="Refresh Data"
          >
            <RefreshCw size={14} />
          </button>
          <div className="relative">
            <Search size={12} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-faint" />
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search ward or keyword..."
              className="input-dark pl-7 py-1 text-xs w-44"
            />
          </div>
          <span className="text-[11px] font-mono text-faint whitespace-nowrap">{coordsDisplay}</span>
        </div>
      </div>

      {/* ── 2. Main Map Canvas + Right Sidebar ────────────────────────── */}
      <div className="flex-1 flex min-h-0 relative">
        {/* Map Viewport */}
        <div className="flex-1 relative h-full">
          <div ref={mapRef} className="w-full h-full bg-void" />

          {/* Floating Stat Pills Overlay (Authentic NagarSetu feature) */}
          <div className="absolute top-4 left-4 z-[400] flex items-center gap-2 bg-abyss/80 backdrop-blur-md px-3 py-2 rounded-xl border border-white/10 shadow-lg">
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-siren/10 text-siren text-xs font-bold border border-siren/30">
              <span className="w-2 h-2 rounded-full bg-siren animate-siren-pulse" />
              <span>{activeSosCount} Active SOS</span>
            </div>
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-civic/10 text-cyber text-xs font-bold border border-civic/30">
              <span className="w-2 h-2 rounded-full bg-cyber" />
              <span>{activeIncCount} Incidents</span>
            </div>
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-safe/10 text-safe text-xs font-bold border border-safe/30">
              <span className="w-2 h-2 rounded-full bg-safe" />
              <span>{resolvedCount} Resolved</span>
            </div>
          </div>
        </div>

        {/* Right Sidebar: Legend & Quick Ward Jump */}
        <div className="w-72 flex-shrink-0 bg-abyss border-l border-white/5 p-4 flex flex-col gap-4 overflow-y-auto">
          {/* Map Legend */}
          <div className="glass-card p-3 space-y-2.5">
            <h3 className="text-xs font-bold text-ink uppercase tracking-wider">🗺️ Tactical Legend</h3>
            <div className="space-y-2 text-xs text-mink">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-siren animate-siren-pulse" />
                <span>Critical SOS (Sev 5)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-siren-orange" />
                <span>High Severity (4)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-cyber" />
                <span>Civic Incident (1-3)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-siren-orange opacity-40 border border-siren-orange" />
                <span>Gaussian Risk Heat Zone</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded border border-dashed border-indigo-400 bg-indigo-500/10" />
                <span>Municipal Ward Boundary</span>
              </div>
            </div>
          </div>

          {/* Quick Ward Jump List */}
          <div className="glass-card p-3 space-y-2 flex-1 flex flex-col min-h-0">
            <h3 className="text-xs font-bold text-ink uppercase tracking-wider flex items-center justify-between">
              <span>🏛️ Quick Ward Jump</span>
              <span className="text-[10px] text-mink">{currentCity.toUpperCase()}</span>
            </h3>
            <p className="text-[11px] text-mink">Click any ward to center tactical camera.</p>

            <div className="space-y-1.5 overflow-y-auto flex-1 pr-1">
              {WARD_LOCATIONS.filter((w) => w.city === currentCity).map((ward) => (
                <button
                  key={ward.name}
                  onClick={() => flyToWard(ward.lat, ward.lng, ward.name)}
                  className={cn(
                    'w-full flex items-center justify-between p-2 rounded-lg text-left text-xs transition-colors',
                    selectedWard === ward.name
                      ? 'bg-cyber/15 text-cyber border border-cyber/30 font-bold'
                      : 'bg-surface hover:bg-raised text-mink hover:text-ink'
                  )}
                >
                  <div className="flex items-center gap-2 truncate">
                    <Building2 size={13} className="text-faint flex-shrink-0" />
                    <span className="truncate">{ward.name}</span>
                  </div>
                  <span className="text-[10px] font-mono px-1.5 py-0.5 bg-raised rounded text-faint">
                    {ward.count} rep
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
