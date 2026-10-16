'use client';

import { useEffect, useRef, useState } from 'react';
import { MapPin, Battery, Heart, Clock, ArrowLeft } from 'lucide-react';

interface TrackingData {
  lat: number;
  lng: number;
  batteryLevel?: number;
  heartRate?: number;
  updatedAt: number;
  userName: string;
  ward: string;
}

// Mock tracking data — replace with real API call
const MOCK_TRACKING: TrackingData = {
  lat: 23.2599,
  lng: 77.4126,
  batteryLevel: 34,
  heartRate: 112,
  updatedAt: Date.now() - 45000,
  userName: 'Priya Sharma',
  ward: 'Ward 7 — Arera Colony',
};

export default function TrackingPage({ params }: { params: { token: string } }) {
  const [data, setData] = useState<TrackingData | null>(MOCK_TRACKING);
  const [elapsed, setElapsed] = useState(0);
  const mapRef  = useRef<HTMLDivElement>(null);
  const mapInst = useRef<ReturnType<typeof import('leaflet').default.map> | null>(null);
  const markerRef = useRef<ReturnType<typeof import('leaflet').default.circleMarker> | null>(null);

  // Tick elapsed timer
  useEffect(() => {
    const t = setInterval(() => setElapsed(e => e + 1), 1000);
    return () => clearInterval(t);
  }, []);

  // Poll location every 5s
  useEffect(() => {
    const poll = async () => {
      try {
        const res = await fetch(`/api/tracking/location?session_id=${params.token}`);
        if (res.ok) {
          const json = await res.json();
          setData(prev => prev ? { ...prev, lat: json.lat, lng: json.lng, updatedAt: Date.now() } : null);
        }
      } catch {
        // Backend offline — keep mock data
      }
    };
    const t = setInterval(poll, 5000);
    return () => clearInterval(t);
  }, [params.token]);

  // Init Leaflet map
  useEffect(() => {
    if (!mapRef.current || mapInst.current || !data) return;

    import('leaflet').then(L => {
      const map = L.default.map(mapRef.current!, {
        center: [data.lat, data.lng],
        zoom: 15,
        zoomControl: false,
        attributionControl: false,
      });

      L.default.tileLayer(
        'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
        { maxZoom: 19 }
      ).addTo(map);

      L.default.control.zoom({ position: 'bottomright' }).addTo(map);

      const marker = L.default.circleMarker([data.lat, data.lng], {
        radius: 14,
        color: '#E53935',
        fillColor: '#E53935',
        fillOpacity: 0.9,
        weight: 3,
      })
        .addTo(map)
        .bindPopup(`📍 ${data.userName}`)
        .openPopup();

      mapInst.current = map;
      markerRef.current = marker;
    });

    return () => {
      mapInst.current?.remove();
      mapInst.current = null;
    };
  }, [data?.lat, data?.lng]);

  // Pan map when location updates
  useEffect(() => {
    if (!mapInst.current || !markerRef.current || !data) return;
    markerRef.current.setLatLng([data.lat, data.lng]);
    mapInst.current.panTo([data.lat, data.lng]);
  }, [data?.lat, data?.lng]);

  const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  if (!data) {
    return (
      <div className="flex items-center justify-center h-screen bg-void text-mink text-sm">
        Tracking session not found or expired.
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen bg-void overflow-hidden">
      {/* Header */}
      <header className="flex-shrink-0 flex items-center gap-3 px-4 py-3 bg-abyss border-b border-white/5">
        <div className="w-8 h-8 rounded-lg bg-siren/20 flex items-center justify-center">
          <MapPin size={16} className="text-siren" />
        </div>
        <div className="flex-1">
          <p className="text-sm font-bold text-ink">🛡️ SafeTrack — Live Location</p>
          <p className="text-[11px] text-mink">{data.userName} · {data.ward}</p>
        </div>
        <div className="flex items-center gap-1.5 text-xs font-mono text-siren animate-siren-pulse">
          <span className="w-2 h-2 rounded-full bg-siren block" />
          LIVE {fmt(elapsed)}
        </div>
      </header>

      {/* Map */}
      <div className="flex-1 relative">
        <div ref={mapRef} className="absolute inset-0" />
      </div>

      {/* Status bar */}
      <footer className="flex-shrink-0 bg-abyss border-t border-white/5 px-4 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4 text-xs">
            <div className="flex items-center gap-1.5 text-mink">
              <MapPin size={11} className="text-cyber" />
              <span className="font-mono">{data.lat.toFixed(5)}, {data.lng.toFixed(5)}</span>
            </div>
            {data.batteryLevel !== undefined && (
              <div className="flex items-center gap-1.5 text-mink">
                <Battery size={11} className={data.batteryLevel < 20 ? 'text-siren' : 'text-safe'} />
                <span>{data.batteryLevel}%</span>
              </div>
            )}
            {data.heartRate && (
              <div className="flex items-center gap-1.5 text-mink">
                <Heart size={11} className="text-siren" />
                <span>{data.heartRate} BPM</span>
              </div>
            )}
          </div>
          <div className="flex items-center gap-1.5 text-[11px] text-mink">
            <Clock size={11} />
            <span>Updated {Math.round((Date.now() - data.updatedAt) / 1000)}s ago</span>
          </div>
        </div>
        <p className="text-[10px] text-faint mt-2 text-center">
          This person shared their real-time location for emergency purposes · NagarSetu SafeTrack
        </p>
      </footer>
    </div>
  );
}
