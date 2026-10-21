'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { Camera, Video, AlertTriangle, RefreshCw, Wifi, WifiOff, Maximize2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { cctvApi, type CctvCamera } from '@/lib/api';

// Fallback seed cameras used when backend is unreachable
const SEED_CAMERAS: CctvCamera[] = [
  { id: 'CAM-01', name: 'MP Nagar Zone-1 Chowk', ward: 'Ward 3', lat: 23.2340, lng: 77.4320, feed_url: '', status: 'online', fps: 30, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-02', name: 'TT Nagar Stadium North', ward: 'Ward 5', lat: 23.2430, lng: 77.4050, feed_url: '', status: 'alert', fps: 28, resolution: '1080p', active_threat: 'crowd_surge', threat_confidence: 0.91, detections: [{ label: 'crowd_surge', confidence: 0.91 }] },
  { id: 'CAM-03', name: 'Habibganj Underpass', ward: 'Ward 9', lat: 23.2680, lng: 77.4300, feed_url: '', status: 'alert', fps: 28, resolution: '4K', active_threat: 'waterlogging', threat_confidence: 0.94, detections: [{ label: 'waterlogging', confidence: 0.94 }] },
  { id: 'CAM-04', name: 'Arera Hills VIP Spine', ward: 'Ward 7', lat: 23.2190, lng: 77.4250, feed_url: '', status: 'online', fps: 30, resolution: '4K', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-05', name: 'New Market Central Square', ward: 'Ward 4', lat: 23.2520, lng: 77.4060, feed_url: '', status: 'online', fps: 30, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
  { id: 'CAM-06', name: 'Kolar Transit Depot', ward: 'Ward 6', lat: 23.1990, lng: 77.4670, feed_url: '', status: 'online', fps: 28, resolution: '1080p', active_threat: undefined, threat_confidence: 0, detections: [] },
];

// Cycle through video stream loop — if a real stream URL is provided use it,
// otherwise render a deterministic static SVG frame that simulates scan-line video
function CameraFrame({ cam, isAlert }: { cam: CctvCamera; isAlert: boolean }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // Animate a simulated "video static" effect on canvas when no real stream
  useEffect(() => {
    if (cam.feed_url) return; // let <video> handle real feeds
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    let frameId: number;

    const cityColors = isAlert
      ? ['#FF2D55', '#FF6B35', '#FF2D5540']
      : ['#00E5FF', '#0066CC', '#00E5FF20'];

    function drawFrame() {
      if (!ctx || !canvas) return;
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      // Dark background
      ctx.fillStyle = '#0A0F1A';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Grid lines (simulated street/zone overlay)
      ctx.strokeStyle = isAlert ? '#FF2D5518' : '#00E5FF12';
      ctx.lineWidth = 1;
      for (let i = 0; i < canvas.width; i += 32) {
        ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height); ctx.stroke();
      }
      for (let j = 0; j < canvas.height; j += 32) {
        ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(canvas.width, j); ctx.stroke();
      }

      // Noise pixels for realism
      const imageData = ctx.createImageData(canvas.width, canvas.height);
      for (let p = 0; p < imageData.data.length; p += 4) {
        const noise = Math.random() * 18;
        imageData.data[p] = noise;
        imageData.data[p + 1] = noise;
        imageData.data[p + 2] = isAlert ? noise * 1.6 : noise;
        imageData.data[p + 3] = 45;
      }
      ctx.putImageData(imageData, 0, 0);

      // Scan line
      const scanY = (Date.now() / 8) % canvas.height;
      const grad = ctx.createLinearGradient(0, scanY - 4, 0, scanY + 4);
      grad.addColorStop(0, 'transparent');
      grad.addColorStop(0.5, cityColors[0] + '60');
      grad.addColorStop(1, 'transparent');
      ctx.fillStyle = grad;
      ctx.fillRect(0, scanY - 4, canvas.width, 8);

      // Bounding box for alert cams
      if (isAlert && cam.detections?.length) {
        ctx.strokeStyle = '#FF2D55';
        ctx.lineWidth = 2;
        ctx.setLineDash([6, 4]);
        const bw = canvas.width * 0.45, bh = canvas.height * 0.50;
        const bx = (canvas.width - bw) * 0.55, by = (canvas.height - bh) * 0.3;
        ctx.strokeRect(bx, by, bw, bh);
        ctx.setLineDash([]);
        // Label
        ctx.fillStyle = '#FF2D55';
        ctx.fillRect(bx, by - 16, bw * 0.85, 16);
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 9px monospace';
        ctx.fillText(
          `YOLO: ${cam.detections[0].label.toUpperCase()} ${Math.round(cam.threat_confidence * 100)}%`,
          bx + 4, by - 4
        );
      }

      // Crosshair center
      ctx.strokeStyle = cityColors[0] + '80';
      ctx.lineWidth = 1;
      const cx = canvas.width / 2, cy = canvas.height / 2;
      ctx.beginPath(); ctx.moveTo(cx - 12, cy); ctx.lineTo(cx + 12, cy); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(cx, cy - 12); ctx.lineTo(cx, cy + 12); ctx.stroke();

      frameId = requestAnimationFrame(drawFrame);
    }

    drawFrame();
    return () => cancelAnimationFrame(frameId);
  }, [cam, isAlert]);

  if (cam.feed_url) {
    return (
      <video
        src={cam.feed_url}
        className="w-full h-full object-cover"
        autoPlay
        muted
        loop
        playsInline
      />
    );
  }

  return (
    <canvas
      ref={canvasRef}
      width={320}
      height={200}
      className="w-full h-full object-cover"
    />
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
                <button className="btn-ghost text-xs flex-1 flex items-center justify-center gap-1.5">
                  <Maximize2 size={12} /> Fullscreen
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
