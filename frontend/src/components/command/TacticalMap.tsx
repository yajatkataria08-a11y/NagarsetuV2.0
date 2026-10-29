'use client';

import { useEffect, useRef } from 'react';
import type { SosEvent, Incident, StructureSummary } from '@/types';

/** Escape text interpolated into Leaflet popup HTML. */
const esc = (v: unknown) =>
  String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string));

const BAND_COLOR: Record<string, string> = {
  good: '#10B981', watch: '#F59E0B', critical: '#E53935', unknown: '#64748B',
};

// Leaflet is client-only — dynamic import via useEffect
interface TacticalMapProps {
  sos: SosEvent[];
  incidents: Incident[];
  selectedSosId?: string;
  /** Bridges/structures, coloured by health band. */
  structures?: StructureSummary[];
  selectedStructureId?: string | null;
  onSelectStructure?: (id: string) => void;
  center?: [number, number];
}

export default function TacticalMap({ sos, incidents, selectedSosId, structures = [], selectedStructureId, onSelectStructure, center }: TacticalMapProps) {
  const onSelectRef = useRef(onSelectStructure);
  onSelectRef.current = onSelectStructure;
  const mapRef   = useRef<HTMLDivElement>(null);
  const leafRef  = useRef<any>(null);
  const mapInst  = useRef<any>(null);
  const markersRef = useRef<any[]>([]);

  // ── Bootstrap map ────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!mapRef.current || mapInst.current) return;

    import('leaflet').then(mod => {
      const L = (mod as any).default || mod;
      leafRef.current = L;

      const map = L.map(mapRef.current!, {
        center: center ?? [23.2599, 77.4126], // Bhopal centre unless overridden
        zoom: 13,
        zoomControl: false,
        attributionControl: false,
      });

      // Dark tactical tile layer (CartoDB Dark Matter)
      L.tileLayer(
        'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
        { maxZoom: 19 }
      ).addTo(map);

      // Zoom control bottom-right
      L.control.zoom({ position: 'bottomright' }).addTo(map);

      // Attribution bottom-left (minimal)
      L.control.attribution({ prefix: '© OpenStreetMap · CartoDB' })
        .addTo(map);

      mapInst.current = map;
    });

    return () => {
      mapInst.current?.remove();
      mapInst.current = null;
    };
  }, []);

  // ── Refresh markers when data changes ────────────────────────────────────────
  useEffect(() => {
    const L = leafRef.current;
    const map = mapInst.current;
    if (!L || !map) return;

    // Clear old markers
    markersRef.current.forEach(m => map.removeLayer(m));
    markersRef.current = [];

    // SOS markers
    sos.forEach(s => {
      if (!s.location) return;
      const isActive = s.status === 'active';
      const icon = L.divIcon({
        className: '',
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        html: `<div style="
          width:36px;height:36px;border-radius:50%;
          background:${isActive ? '#E53935' : '#F97316'};
          border:2.5px solid rgba(255,255,255,0.8);
          display:flex;align-items:center;justify-content:center;
          font-size:16px;
          box-shadow:0 0 0 ${isActive ? '8px rgba(229,57,53,0.30)' : '4px rgba(249,115,22,0.20)'};
          animation:${isActive ? 'siren-pulse 1.4s infinite' : 'none'};
        ">🚨</div>`,
      });

      const marker = L.marker([s.location.lat, s.location.lng], { icon, zIndexOffset: 1000 })
        .addTo(map)
        .bindPopup(`
          <div style="font-family:system-ui;min-width:160px">
            <p style="font-weight:700;color:#E53935;margin:0 0 4px">🚨 SOS — ${s.userName}</p>
            <p style="margin:2px 0;font-size:12px;color:#ccc">📍 ${s.ward}</p>
            <p style="margin:2px 0;font-size:12px;color:#ccc">🔋 ${s.batteryLevel}% ${s.heartRate ? `· ❤️ ${s.heartRate} BPM` : ''}</p>
            ${s.trackingToken ? `<a href="/track/${s.trackingToken}" target="_blank" style="color:#00D4FF;font-size:11px">🔗 Live Track</a>` : ''}
          </div>
        `);

      // Auto-open popup for selected SOS
      if (s.id === selectedSosId) {
        marker.openPopup();
        map.flyTo([s.location.lat, s.location.lng], 15, { duration: 1.2 });
      }

      markersRef.current.push(marker);
    });

    // Incident markers
    incidents
      .filter(i => i.location && i.status !== 'rejected')
      .forEach(inc => {
        const emoji =
          inc.type === 'ROAD_HAZARD' ? '🚧' :
          inc.type === 'HARASSMENT'  ? '⚠️' :
          inc.type === 'FLOOD_RISK'  ? '🌊' :
          inc.type === 'FIRE'        ? '🔥' :
          inc.type === 'WATER_LOGGING' ? '💧' : '📍';

        const color =
          inc.severity >= 5 ? '#E53935' :
          inc.severity >= 4 ? '#F97316' :
          inc.severity >= 3 ? '#F59E0B' : '#10B981';

        const icon = L.divIcon({
          className: '',
          iconSize: [26, 26],
          iconAnchor: [13, 13],
          html: `<div style="
            width:26px;height:26px;border-radius:50%;
            background:${color};border:2px solid rgba(255,255,255,0.7);
            display:flex;align-items:center;justify-content:center;
            font-size:12px;box-shadow:0 2px 8px rgba(0,0,0,0.5);
          ">${emoji}</div>`,
        });

        const marker = L.marker([inc.location.lat, inc.location.lng], { icon })
          .addTo(map)
          .bindPopup(`
            <div style="font-family:system-ui;min-width:160px">
              <p style="font-weight:700;margin:0 0 4px;color:#fff">${emoji} ${inc.type.replace(/_/g, ' ')}</p>
              <p style="margin:2px 0;font-size:12px;color:#ccc">📍 ${inc.ward}</p>
              <p style="margin:2px 0;font-size:11px;color:#aaa">${inc.description.slice(0, 80)}…</p>
              <p style="margin:4px 0 0;font-size:11px;color:${color}">⚡ Severity ${inc.severity}/5</p>
            </div>
          `);
        markersRef.current.push(marker);
      });

    // Structure markers (coloured by health band)
    structures.forEach(st => {
      const color = BAND_COLOR[st.health.band] ?? BAND_COLOR.unknown;
      const selected = st.id === selectedStructureId;
      const label = st.health.score === null ? '–' : Math.round(st.health.score);
      const icon = L.divIcon({
        className: '',
        iconSize: [34, 34],
        iconAnchor: [17, 17],
        html: `<div style="width:34px;height:34px;border-radius:8px;background:${color};
          border:${selected ? '3px solid #00D4FF' : '2px solid rgba(255,255,255,0.75)'};
          display:flex;align-items:center;justify-content:center;font:700 12px/1 ui-monospace,monospace;color:#0B1020;
          box-shadow:0 0 ${selected ? '14px rgba(0,212,255,0.8)' : '6px rgba(0,0,0,0.5)'};">${label}</div>`,
      });
      const marker = L.marker([st.lat, st.lng], { icon, zIndexOffset: selected ? 900 : 500 })
        .addTo(map)
        .bindPopup(`<div style="font-family:system-ui;min-width:170px">
          <p style="font-weight:700;margin:0 0 4px;color:#fff">${esc(st.name)}</p>
          <p style="margin:2px 0;font-size:12px;color:#ccc">${esc(st.type.replace(/_/g, ' '))} · ${esc(st.ward)}</p>
          <p style="margin:4px 0 0;font-size:12px;color:${color}">Health: ${st.health.score ?? 'not assessed'} (${esc(st.health.band)})</p></div>`);
      marker.on('click', () => onSelectRef.current?.(st.id));
      if (selected) map.flyTo([st.lat, st.lng], Math.max(map.getZoom(), 14), { duration: 0.8 });
      markersRef.current.push(marker);
    });
  }, [sos, incidents, selectedSosId, structures, selectedStructureId]);

  return (
    <div className="relative w-full h-full overflow-hidden rounded-xl">
      {/* Scan-line overlay for tactical feel */}
      <div className="scanline-overlay absolute inset-0 pointer-events-none z-10" />
      {/* Leaflet mount point */}
      <div ref={mapRef} className="w-full h-full" />
    </div>
  );
}
