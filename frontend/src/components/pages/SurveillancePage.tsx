'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { Camera, Video, AlertTriangle, RefreshCw, Wifi, WifiOff, Maximize2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { cctvApi, type CctvCamera } from '@/lib/api';
import Hls from 'hls.js';

// Fallback seed cameras with hardware-accelerated streaming loops
const SEED_CAMERAS: CctvCamera[] = [
  { id: 'CAM-01', name: 'MP Nagar Zone-1 Chowk', ward: 'Ward 3', lat: 23.2340, lng: 77.4320, feed_url: '/streams/cam-01.mp4', status: 'online', fps: 30, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-02', name: 'TT Nagar Stadium North', ward: 'Ward 5', lat: 23.2430, lng: 77.4050, feed_url: '/streams/cam-02.mp4', status: 'alert', fps: 28, resolution: '1080p', active_threat: 'crowd_surge', threat_confidence: 0.91, detections: [{ label: 'crowd_surge', confidence: 0.91, bbox: [200, 195, 260, 85] }] },
  { id: 'CAM-03', name: 'Habibganj Underpass Flood Cam', ward: 'Ward 9', lat: 23.2680, lng: 77.4300, feed_url: '/streams/cam-03.mp4', status: 'alert', fps: 28, resolution: '4K', active_threat: 'waterlogging', threat_confidence: 0.96, detections: [{ label: 'waterlogging', confidence: 0.96, bbox: [100, 210, 440, 120] }] },
  { id: 'CAM-04', name: 'Arera Hills VIP Spine', ward: 'Ward 7', lat: 23.2190, lng: 77.4250, feed_url: '/streams/cam-04.mp4', status: 'online', fps: 30, resolution: '4K', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-05', name: 'Rajwada Historical Plaza', ward: 'Ward 18', lat: 22.7185, lng: 75.8560, feed_url: '/streams/cam-05.mp4', status: 'online', fps: 30, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-06', name: 'Vijay Nagar Square', ward: 'Ward 24', lat: 22.7533, lng: 75.8937, feed_url: '/streams/cam-06.mp4', status: 'online', fps: 28, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
];

// Continuous video player with HLS (.m3u8) + MP4 support and overlaid SVG bounding box layer
function CameraFrame({ cam, isAlert, videoRef }: { cam: CctvCamera; isAlert: boolean; videoRef?: React.RefObject<HTMLVideoElement> }) {
  const localRef = useRef<HTMLVideoElement>(null);
  const activeRef = videoRef || localRef;

  useEffect(() => {
    if (!cam.feed_url) return;
    const video = activeRef.current;
    if (!video) return;

    if (cam.feed_url.endsWith('.m3u8')) {
      if (Hls.isSupported()) {
        const hls = new Hls({ enableWorker: true, lowLatencyMode: true });
        hls.loadSource(cam.feed_url);
        hls.attachMedia(video);
        return () => {
          hls.destroy();
        };
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = cam.feed_url;
      }
    } else {
      video.src = cam.feed_url;
    }
  }, [cam.feed_url, activeRef]);

  if (cam.feed_url) {
    return (
      <div className="relative w-full h-full bg-black overflow-hidden flex items-center justify-center">
        <video
          ref={activeRef}
          src={cam.feed_url.endsWith('.m3u8') ? undefined : cam.feed_url}
          className="w-full h-full object-cover"
          autoPlay
          muted
          loop
          playsInline
        />
        {/* Real-time YOLOv8 SVG bounding box overlay layer */}
        {isAlert && cam.detections && cam.detections.length > 0 && (
          <svg className="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 640 360" preserveAspectRatio="none">
            {cam.detections.map((d: any, idx: number) => {
              const bbox = d.bbox || [180, 160, 280, 140];
              const [x, y, w, h] = bbox;
              return (
                <g key={idx}>
                  <rect
                    x={x}
                    y={y}
                    width={w}
                    height={h}
                    fill="none"
                    stroke="#FF2D55"
                    strokeWidth="3"
                    strokeDasharray="6,4"
                    className="animate-pulse"
                  />
                  <rect
                    x={x}
                    y={Math.max(0, y - 24)}
                    width={Math.min(260, w + 40)}
                    height="24"
                    fill="#FF2D55"
                    rx="3"
                  />
                  <text
                    x={x + 6}
                    y={Math.max(16, y - 7)}
                    fill="#FFFFFF"
                    fontSize="11"
                    fontFamily="monospace"
                    fontWeight="bold"
                  >
                    {`YOLO: ${d.label?.replace(/_/g, ' ').toUpperCase()} ${Math.round((d.confidence || 0.9) * 100)}%`}
                  </text>
                </g>
              );
            })}
          </svg>
        )}
      </div>
    );
  }

  return (
    <div className="relative w-full h-full bg-slate-900 flex items-center justify-center text-xs text-mink">
      <span>NO SIGNAL</span>
    </div>
  );
}

export default function SurveillancePage() {
  const [cameras, setCameras] = useState<CctvCamera[]>(SEED_CAMERAS);
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [selectedCam, setSelectedCam] = useState<CctvCamera | null>(null);
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

  const fetchCameras = useCallback(async () => {
    try {
      const data = await cctvApi.list();
      if (data && data.length > 0) {
        setCameras(data);
        setConnected(true);
      }
    } catch {
      // backend not available — use seed data
      setConnected(false);
    } finally {
      setLoading(false);
      setLastRefresh(new Date());
    }
  }, []);

  useEffect(() => {
    fetchCameras();
    // Auto-refresh detection state every 8 seconds
    const interval = setInterval(fetchCameras, 8000);
    return () => clearInterval(interval);
  }, [fetchCameras]);

  const alertCount = cameras.filter(c => c.status === 'alert').length;
  const onlineCount = cameras.filter(c => c.status === 'online').length;

  return (
    <div className="flex flex-col h-full bg-void overflow-hidden">
      {/* Top Bar */}
      <div className="flex-shrink-0 flex items-center justify-between px-4 py-2.5 bg-abyss border-b border-white/5">
        <div className="flex items-center gap-2">
          <Video size={16} className="text-cyber" />
          <h2 className="text-sm font-bold text-ink">Zero-Scroll CCTV Vision Wall</h2>
          <span className="text-[10px] font-bold bg-cyber/10 text-cyber border border-cyber/30 px-2 py-0.5 rounded-full">
            YOLOv8 Vision Active
          </span>
          {loading && (
            <RefreshCw size={12} className="text-mink animate-spin" />
          )}
        </div>
        <div className="flex items-center gap-4 text-xs text-mink">
          <div className="flex items-center gap-1.5">
            {connected ? (
              <><Wifi size={12} className="text-safe" /><span className="text-safe text-[10px]">Live API</span></>
            ) : (
              <><WifiOff size={12} className="text-caution" /><span className="text-caution text-[10px]">Seed Data</span></>
            )}
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-safe animate-live-dot" />
            <span>{onlineCount} Normal</span>
          </div>
          {alertCount > 0 && (
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-siren animate-siren-pulse" />
              <span className="text-siren font-bold">{alertCount} Alerts</span>
            </div>
          )}
          <button
            onClick={fetchCameras}
            className="flex items-center gap-1 text-[10px] text-cyber hover:text-ink transition-colors"
            title={`Last refresh: ${lastRefresh.toLocaleTimeString()}`}
          >
            <RefreshCw size={10} />
            Refresh
          </button>
        </div>
      </div>

      {/* 2×3 Camera Grid */}
      <div className="flex-1 p-3 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 overflow-hidden">
        {cameras.map((cam) => {
          const isAlert = cam.status === 'alert' || (cam.active_threat != null && cam.active_threat !== '');
          return (
            <div
              key={cam.id}
              onClick={() => setSelectedCam(cam)}
              className={cn(
                'glass-card relative flex flex-col overflow-hidden border cursor-pointer transition-all duration-200 group',
                isAlert ? 'border-siren shadow-glow-siren' : 'border-white/10 hover:border-cyber/40'
              )}
            >
              {/* Header Overlay */}
              <div className="absolute top-2 left-2 right-2 z-10 flex items-center justify-between bg-abyss/85 backdrop-blur-sm px-2.5 py-1.5 rounded-lg border border-white/10">
                <div className="flex items-center gap-1.5">
                  <Camera size={12} className={isAlert ? 'text-siren' : 'text-cyber'} />
                  <span className="text-xs font-bold text-ink font-mono">{cam.id}</span>
                  <span className="text-[9px] text-faint">{cam.resolution}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  {isAlert ? (
                    <span className="badge-critical text-[10px] py-0 px-1.5 animate-siren-pulse">THREAT DETECTED</span>
                  ) : (
                    <span className="badge-low text-[10px] py-0 px-1.5">LIVE</span>
                  )}
                  <span className="text-[9px] text-mink font-mono">{cam.fps} FPS</span>
                </div>
              </div>

              {/* Camera Feed */}
              <div className="relative flex-1 bg-surface min-h-[140px] overflow-hidden">
                <CameraFrame cam={cam} isAlert={isAlert} />
                {/* Scanline overlay */}
                <div className="scanline-overlay absolute inset-0 pointer-events-none" />
              </div>

              {/* Bottom Telemetry */}
              <div className="flex-shrink-0 bg-abyss/90 border-t border-white/5 p-2 flex items-center justify-between">
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-ink leading-tight truncate">{cam.name}</p>
                  <p className="text-[10px] text-mink truncate">{cam.ward}</p>
                </div>
                <div className="text-right flex-shrink-0 ml-2">
                  {isAlert ? (
                    <>
                      <p className="text-[11px] font-bold font-mono text-siren truncate">
                        {cam.active_threat?.replace(/_/g, ' ').toUpperCase()}
                      </p>
                      <p className="text-[9px] text-faint">
                        {Math.round((cam.threat_confidence || 0) * 100)}% confidence
                      </p>
                    </>
                  ) : (
                    <>
                      <p className="text-[11px] font-bold font-mono text-cyber">All Clear</p>
                      <p className="text-[9px] text-faint">No threats</p>
                    </>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Camera Detail Modal */}
      {selectedCam && (
        <div
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
          onClick={() => setSelectedCam(null)}
        >
          <div
            className="glass-card max-w-2xl w-full border border-white/10 overflow-hidden"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
              <div className="flex items-center gap-2">
                <Camera size={14} className="text-cyber" />
                <span className="text-sm font-bold text-ink">{selectedCam.name}</span>
                <span className="text-[10px] text-mink font-mono">{selectedCam.id}</span>
              </div>
              <button onClick={() => setSelectedCam(null)} className="text-faint hover:text-ink">✕</button>
            </div>
            <div className="p-4 space-y-3">
              <div className="aspect-video bg-surface rounded-lg overflow-hidden">
                <CameraFrame cam={selectedCam} isAlert={selectedCam.status === 'alert'} />
              </div>
              <div className="grid grid-cols-4 gap-3 text-xs">
                <div className="bg-abyss rounded-lg p-2 text-center">
                  <p className="text-faint text-[10px]">Resolution</p>
                  <p className="text-ink font-bold font-mono">{selectedCam.resolution}</p>
                </div>
                <div className="bg-abyss rounded-lg p-2 text-center">
                  <p className="text-faint text-[10px]">FPS</p>
                  <p className="text-ink font-bold font-mono">{selectedCam.fps}</p>
                </div>
                <div className="bg-abyss rounded-lg p-2 text-center">
                  <p className="text-faint text-[10px]">Ward</p>
                  <p className="text-ink font-bold">{selectedCam.ward}</p>
                </div>
                <div className="bg-abyss rounded-lg p-2 text-center">
                  <p className="text-faint text-[10px]">Status</p>
                  <p className={cn('font-bold', selectedCam.status === 'alert' ? 'text-siren' : 'text-safe')}>
                    {selectedCam.status?.toUpperCase()}
                  </p>
                </div>
              </div>
              {selectedCam.detections && selectedCam.detections.length > 0 && (
                <div className="bg-siren/10 border border-siren/30 rounded-lg p-3 space-y-1.5">
                  <p className="text-xs font-bold text-siren flex items-center gap-1.5">
                    <AlertTriangle size={12} /> Active YOLOv8 Detections
                  </p>
                  {selectedCam.detections.map((d, i) => (
                    <div key={i} className="flex items-center justify-between text-xs">
                      <span className="text-ink font-mono">{d.label.replace(/_/g, ' ')}</span>
                      <span className="text-caution font-bold">{Math.round(d.confidence * 100)}%</span>
                    </div>
                  ))}
                </div>
              )}
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    const canvas = document.createElement('canvas');
                    canvas.width = 640;
                    canvas.height = 360;
                    const ctx = canvas.getContext('2d');
                    if (ctx) {
                      ctx.fillStyle = '#0A0F1A';
                      ctx.fillRect(0, 0, 640, 360);
                      ctx.fillStyle = '#00E5FF';
                      ctx.font = 'bold 14px monospace';
                      ctx.fillText(`NAGARSETU TACTICAL CCTV EVIDENCE`, 20, 35);
                      ctx.fillStyle = '#FFFFFF';
                      ctx.font = 'bold 16px sans-serif';
                      ctx.fillText(`${selectedCam.name} [${selectedCam.id}]`, 20, 65);
                      ctx.font = '12px monospace';
                      ctx.fillStyle = '#94A3B8';
                      ctx.fillText(`Timestamp: ${new Date().toISOString()} | Ward: ${selectedCam.ward}`, 20, 95);
                      if (selectedCam.active_threat) {
                        ctx.fillStyle = '#FF2D55';
                        ctx.font = 'bold 14px monospace';
                        ctx.fillText(`DETECTION: ${selectedCam.active_threat.toUpperCase()} (${Math.round((selectedCam.threat_confidence || 0.9) * 100)}% CONFIDENCE)`, 20, 130);
                        ctx.strokeStyle = '#FF2D55';
                        ctx.lineWidth = 2;
                        ctx.strokeRect(180, 160, 280, 140);
                      }
                      const a = document.createElement('a');
                      a.href = canvas.toDataURL('image/png');
                      a.download = `nagarsetu-${selectedCam.id}-snapshot-${Date.now()}.png`;
                      a.click();
                    }
                  }}
                  className="btn-ghost text-xs flex-1 flex items-center justify-center gap-1.5"
                >
                  <Camera size={12} /> 📸 Export Snapshot
                </button>
                <button
                  onClick={fetchCameras}
                  className="btn-cyber text-xs flex-1 flex items-center justify-center gap-1.5"
                >
                  <RefreshCw size={12} /> Refresh Feed
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
