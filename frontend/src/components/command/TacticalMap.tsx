'use client';

import { useEffect, useRef } from 'react';
import type { SosEvent, Incident } from '@/types';

// Leaflet is client-only — dynamic import via useEffect
interface TacticalMapProps {
  sos: SosEvent[];
  incidents: Incident[];
  selectedSosId?: string;
}

export default function TacticalMap({ sos, incidents, selectedSosId }: TacticalMapProps) {
  const mapRef   = useRef<HTMLDivElement>(null);
  const leafRef  = useRef<typeof import('leaflet') | null>(null);
  const mapInst  = useRef<ReturnType<typeof import('leaflet').default.map> | null>(null);
  const markersRef = useRef<ReturnType<typeof import('leaflet').default.marker>[]>([]);

  // ── Bootstrap map ────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!mapRef.current || mapInst.current) return;

    import('leaflet').then(L => {
      leafRef.current = L;

      const map = L.default.map(mapRef.current!, {
        center: [23.2599, 77.4126], // Bhopal centre
        zoom: 13,
        zoomControl: false,
        attributionControl: false,
      });

      // Dark tactical tile layer (CartoDB Dark Matter)
      L.default.tileLayer(
        'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
        { maxZoom: 19 }
      ).addTo(map);

      // Zoom control bottom-right
      L.default.control.zoom({ position: 'bottomright' }).addTo(map);

      // Attribution bottom-left (minimal)
      L.default.control.attribution({ prefix: '© OpenStreetMap · CartoDB' })
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
      const icon = L.default.divIcon({
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

      const marker = L.default.marker([s.location.lat, s.location.lng], { icon, zIndexOffset: 1000 })
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

        const icon = L.default.divIcon({
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

        const marker = L.default.marker([inc.location.lat, inc.location.lng], { icon })
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
  }, [sos, incidents, selectedSosId]);

  return (
    <div className="relative w-full h-full overflow-hidden rounded-xl">
      {/* Scan-line overlay for tactical feel */}
      <div className="scanline-overlay absolute inset-0 pointer-events-none z-10" />
      {/* Leaflet mount point */}
      <div ref={mapRef} className="w-full h-full" />
    </div>
  );
}
