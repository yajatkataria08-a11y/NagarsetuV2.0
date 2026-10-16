'use client';

import { useState } from 'react';
import { Camera, ShieldAlert, Video, Eye, AlertTriangle, CheckCircle, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';

interface CameraFeed {
  id: string;
  name: string;
  location: string;
  streamUrl: string;
  status: 'ONLINE' | 'ALERT' | 'STANDBY';
  aiDetection?: string;
  confidence?: number;
  lastEvent?: string;
}

const CAMERAS: CameraFeed[] = [
  {
    id: 'CAM-01',
    name: 'MP Nagar Zone-1 Chowk',
    location: 'Ward 3 — Traffic Junction',
    streamUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=640&q=80',
    status: 'ONLINE',
    aiDetection: 'Clear flow',
    confidence: 0.98,
    lastEvent: 'Normal traffic'
  },
  {
    id: 'CAM-02',
    name: 'TT Nagar Stadium North',
    location: 'Ward 5 — Pedestrian Spine',
    streamUrl: 'https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=640&q=80',
    status: 'ALERT',
    aiDetection: 'Crowd density spike',
    confidence: 0.91,
    lastEvent: 'Harassment alert nearby'
  },
  {
    id: 'CAM-03',
    name: 'Habibganj Underpass',
    location: 'Ward 9 — Railway Culvert',
    streamUrl: 'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=640&q=80',
    status: 'ALERT',
    aiDetection: 'Water logging detected (~1.5ft)',
    confidence: 0.94,
    lastEvent: 'Road blockage hazard'
  },
  {
    id: 'CAM-04',
    name: 'Arera Hills VIP Spine',
    location: 'Ward 7 — Main Corridor',
    streamUrl: 'https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=640&q=80',
    status: 'ONLINE',
    aiDetection: 'High illumination (95%)',
    confidence: 0.96,
    lastEvent: 'Safe corridor verified'
  },
  {
    id: 'CAM-05',
    name: 'New Market Central Square',
    location: 'Ward 4 — Market Entrance',
    streamUrl: 'https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=640&q=80',
    status: 'ONLINE',
    aiDetection: 'Normal pedestrian transit',
    confidence: 0.99,
    lastEvent: 'All clear'
  },
  {
    id: 'CAM-06',
    name: 'Kolar Transit Depot',
    location: 'Ward 6 — Bus Stand',
    streamUrl: 'https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=640&q=80',
    status: 'ONLINE',
    aiDetection: 'Night vision active',
    confidence: 0.92,
    lastEvent: 'Poor lighting zone'
  }
];

export default function SurveillancePage() {
  const [selectedCam, setSelectedCam] = useState<CameraFeed | null>(null);

  return (
    <div className="flex flex-col h-full bg-void overflow-hidden">
      {/* Top Bar for CCTV */}
      <div className="flex-shrink-0 flex items-center justify-between px-4 py-2.5 bg-abyss border-b border-white/5">
        <div className="flex items-center gap-2">
          <Video size={16} className="text-cyber" />
          <h2 className="text-sm font-bold text-ink">Zero-Scroll CCTV Wall (Krisis Grid)</h2>
          <span className="text-[10px] font-bold bg-cyber/10 text-cyber border border-cyber/30 px-2 py-0.5 rounded-full">
            YOLOv8 Vision Active
          </span>
        </div>
        <div className="flex items-center gap-3 text-xs text-mink">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-safe animate-live-dot" />
            <span>4 Normal</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-siren animate-siren-pulse" />
            <span className="text-siren font-bold">2 Alerts</span>
          </div>
        </div>
      </div>

      {/* Zero-Scroll 2x3 Grid */}
      <div className="flex-1 p-3 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 overflow-hidden">
        {CAMERAS.map((cam) => {
          const isAlert = cam.status === 'ALERT';
          return (
            <div
              key={cam.id}
              onClick={() => setSelectedCam(cam)}
              className={cn(
                'glass-card relative flex flex-col overflow-hidden border cursor-pointer transition-all duration-200 group',
                isAlert ? 'border-siren shadow-glow-siren' : 'border-white/10 hover:border-cyber/40'
              )}
            >
              {/* Camera Header Overlay */}
              <div className="absolute top-2 left-2 right-2 z-10 flex items-center justify-between bg-abyss/85 backdrop-blur-sm px-2.5 py-1.5 rounded-lg border border-white/10">
                <div className="flex items-center gap-1.5">
                  <Camera size={12} className={isAlert ? 'text-siren' : 'text-cyber'} />
                  <span className="text-xs font-bold text-ink font-mono">{cam.id}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  {isAlert ? (
                    <span className="badge-critical text-[10px] py-0 px-1.5">HAZARD DETECTED</span>
                  ) : (
                    <span className="badge-low text-[10px] py-0 px-1.5">LIVE</span>
                  )}
                </div>
              </div>

              {/* Feed Image / Simulated Video Frame */}
              <div className="relative flex-1 bg-surface min-h-0 overflow-hidden">
                <img
                  src={cam.streamUrl}
                  alt={cam.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                />

                {/* Scanline overlay */}
                <div className="scanline-overlay absolute inset-0 pointer-events-none" />

                {/* Bounding box simulation for alert cameras */}
                {isAlert && (
                  <div className="absolute inset-8 border-2 border-dashed border-siren rounded flex items-start justify-start p-1 bg-siren/10">
                    <span className="text-[10px] font-bold bg-siren text-white px-1 rounded">
                      YOLO: {cam.aiDetection} ({Math.round((cam.confidence || 0) * 100)}%)
                    </span>
                  </div>
                )}
              </div>

              {/* Bottom Telemetry Bar */}
              <div className="flex-shrink-0 bg-abyss/90 border-t border-white/5 p-2 flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-ink leading-tight truncate">{cam.name}</p>
                  <p className="text-[10px] text-mink truncate">{cam.location}</p>
                </div>
                <div className="text-right">
                  <p className={cn('text-[11px] font-bold font-mono', isAlert ? 'text-siren' : 'text-cyber')}>
                    {cam.aiDetection}
                  </p>
                  <p className="text-[9px] text-faint">{cam.lastEvent}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
