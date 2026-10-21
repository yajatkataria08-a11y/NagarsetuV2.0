'use client';

import { useState, useEffect, useCallback } from 'react';
import { ShieldAlert, Plus, Clock, MapPin, AlertTriangle, Trash2, RefreshCw, Wifi, WifiOff, Zap } from 'lucide-react';
import { cn } from '@/lib/utils';
import { hazardsApi, type HazardZoneRaw } from '@/lib/api';

// Normalize backend response field names (center_lat vs lat)
function normalizeHazard(h: HazardZoneRaw): NormalizedHazard {
  return {
    id: h.id,
    name: h.name,
    lat: h.lat ?? h.center_lat ?? 23.2599,
    lng: h.lng ?? h.center_lng ?? 77.4126,
    radiusMeters: h.radius_meters,
    riskBoost: h.risk_boost,
    expiresAt: new Date(h.expires_at).getTime(),
    note: h.note || '',
    active: h.active !== false,
  };
}

interface NormalizedHazard {
  id: string;
  name: string;
  lat: number;
  lng: number;
  radiusMeters: number;
  riskBoost: number;
  expiresAt: number;
  note: string;
  active: boolean;
}

const QUICK_HAZARDS = [
  { label: '🌊 Flash Flood', name: 'Flash Flood Alert', radius: '500', boost: '2.5', duration: '2', note: 'Active flooding — avoid low-lying roads and underpasses' },
  { label: '⚡ Power Outage', name: 'Power Outage Zone', radius: '400', boost: '1.5', duration: '3', note: 'No streetlighting — proceed with caution at night' },
  { label: '🔥 Gas Leak', name: 'Gas Leak Emergency', radius: '300', boost: '3.0', duration: '1', note: 'Evacuate immediately — fire department en route' },
  { label: '🚧 Cave-In', name: 'Road Cave-In', radius: '250', boost: '2.0', duration: '8', note: 'Road surface collapse — total closure in effect' },
  { label: '📢 Mass Event', name: 'Large Gathering Zone', radius: '600', boost: '1.8', duration: '6', note: 'High crowd density — expect restricted movement' },
];

export default function HazardsPage() {
  const [hazards, setHazards] = useState<NormalizedHazard[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [connected, setConnected] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [lat, setLat] = useState('23.2599');
  const [lng, setLng] = useState('77.4126');
  const [radius, setRadius] = useState('300');
  const [boost, setBoost] = useState('2.0');
  const [duration, setDuration] = useState('4');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');

  const fetchHazards = useCallback(async () => {
    try {
      const data = await hazardsApi.list();
      setHazards(data.map(normalizeHazard));
      setConnected(true);
    } catch {
      setConnected(false);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHazards();
    // Refresh every 30s to catch TTL-expired zones
    const interval = setInterval(fetchHazards, 30000);
    return () => clearInterval(interval);
  }, [fetchHazards]);

  const resetForm = () => {
    setName(''); setLat('23.2599'); setLng('77.4126');
    setRadius('300'); setBoost('2.0'); setDuration('4'); setNote('');
    setError('');
  };

  const applyQuick = (q: typeof QUICK_HAZARDS[0]) => {
    setName(q.name); setRadius(q.radius); setBoost(q.boost);
    setDuration(q.duration); setNote(q.note);
  };

  const addHazard = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    setError('');

    try {
      const created = await hazardsApi.create({
        name: name.trim(),
        lat: parseFloat(lat) || 23.2599,
        lng: parseFloat(lng) || 77.4126,
        radiusMeters: parseInt(radius) || 300,
        riskBoost: parseFloat(boost) || 2.0,
        durationMinutes: (parseInt(duration) || 4) * 60,
        note: note.trim(),
      });
      setHazards(prev => [normalizeHazard(created), ...prev]);
      setShowModal(false);
      resetForm();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create hazard zone';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const removeHazard = async (id: string) => {
    try {
      await hazardsApi.delete(id);
      setHazards(prev => prev.filter(h => h.id !== id));
    } catch {
      // If DELETE not yet supported, just remove from local state
      setHazards(prev => prev.filter(h => h.id !== id));
    }
  };

  const activeCount = hazards.filter(h => h.expiresAt > Date.now()).length;

  return (
    <div className="flex flex-col h-full bg-void overflow-y-auto p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-bold text-ink flex items-center gap-2">
            Dynamic Hazard Geofences
            {connected ? (
              <span className="text-[10px] flex items-center gap-1 text-safe"><Wifi size={10} /> Live</span>
            ) : (
              <span className="text-[10px] flex items-center gap-1 text-caution"><WifiOff size={10} /> Offline</span>
            )}
          </h2>
          <p className="text-xs text-mink">
            {activeCount} active danger zone{activeCount !== 1 ? 's' : ''} with automated time-to-live expiration
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={fetchHazards} className="btn-ghost text-xs flex items-center gap-1.5" disabled={loading}>
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
          </button>
          <button onClick={() => { resetForm(); setShowModal(true); }} className="btn-cyber text-xs">
            <Plus size={14} />
            Create Geofence
          </button>
        </div>
      </div>

      {/* Quick Create Shortcuts */}
      <div className="flex flex-wrap gap-2">
        {QUICK_HAZARDS.map((q) => (
          <button
            key={q.label}
            onClick={() => { applyQuick(q); setShowModal(true); }}
            className="text-[11px] bg-abyss border border-white/10 hover:border-siren/40 hover:text-siren text-mink px-3 py-1.5 rounded-lg transition-colors"
          >
            {q.label}
          </button>
        ))}
      </div>

      {/* Hazard Grid */}
      {loading ? (
        <div className="flex-1 flex items-center justify-center text-mink text-sm">
          <RefreshCw size={16} className="animate-spin mr-2" /> Loading hazard zones...
        </div>
      ) : hazards.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center text-center text-mink py-12">
          <ShieldAlert size={36} className="mb-3 text-faint" />
          <p className="text-sm font-semibold text-ink">No Active Hazard Zones</p>
          <p className="text-xs mt-1">All municipal areas are currently clear. Use "Create Geofence" to declare a new danger zone.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {hazards.map((hz) => {
            const remainingMs = hz.expiresAt - Date.now();
            const expired = remainingMs <= 0;
            const remainingMin = Math.max(0, Math.round(remainingMs / 60000));
            const hours = Math.floor(remainingMin / 60);
            const mins = remainingMin % 60;

            return (
              <div
                key={hz.id}
                className={cn(
                  'glass-card p-4 border space-y-3',
                  expired
                    ? 'border-white/10 opacity-50'
                    : 'border-siren/30 shadow-glow-siren'
                )}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div className={cn('w-8 h-8 rounded-lg flex items-center justify-center', expired ? 'bg-white/5 text-faint' : 'bg-siren/20 text-siren')}>
                      <ShieldAlert size={16} />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-ink">{hz.name}</h3>
                      <p className="text-[11px] text-mink">
                        Radius: {hz.radiusMeters}m · Risk ×{hz.riskBoost.toFixed(1)}
                        {expired && <span className="ml-2 text-faint">(EXPIRED)</span>}
                      </p>
                    </div>
                  </div>
                  <button onClick={() => removeHazard(hz.id)} className="text-faint hover:text-siren transition-colors">
                    <Trash2 size={14} />
                  </button>
                </div>

                {hz.note && (
                  <p className="text-xs text-mink bg-abyss/60 p-2.5 rounded-lg border border-white/5">
                    {hz.note}
                  </p>
                )}

                <div className="flex items-center justify-between text-xs pt-1 border-t border-white/5">
                  <div className="flex items-center gap-1.5 text-mink">
                    <MapPin size={12} className="text-cyber" />
                    <span className="font-mono">{hz.lat.toFixed(4)}, {hz.lng.toFixed(4)}</span>
                  </div>
                  <div className={cn('flex items-center gap-1 font-medium', expired ? 'text-faint' : 'text-caution')}>
                    <Clock size={12} />
                    <span>{expired ? 'Expired' : `${hours}h ${mins}m remaining`}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Geofence Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={addHazard} className="glass-card max-w-md w-full p-5 border border-white/10 space-y-4">
            <h3 className="text-sm font-bold text-ink flex items-center gap-2">
              <AlertTriangle size={16} className="text-caution" />
              Declare Temporary Hazard Geofence
            </h3>

            <div>
              <label className="text-xs text-mink block mb-1">Hazard Name / Description *</label>
              <input
                required
                className="input-dark"
                placeholder="e.g. VIP Road Waterlogging"
                value={name}
                onChange={e => setName(e.target.value)}
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="text-[11px] text-mink block mb-1">Latitude</label>
                <input type="number" step="0.0001" className="input-dark" value={lat} onChange={e => setLat(e.target.value)} />
              </div>
              <div>
                <label className="text-[11px] text-mink block mb-1">Longitude</label>
                <input type="number" step="0.0001" className="input-dark" value={lng} onChange={e => setLng(e.target.value)} />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="text-[11px] text-mink block mb-1">Radius (m)</label>
                <input type="number" className="input-dark" value={radius} onChange={e => setRadius(e.target.value)} />
              </div>
              <div>
                <label className="text-[11px] text-mink block mb-1">Risk ×</label>
                <input type="number" step="0.1" className="input-dark" value={boost} onChange={e => setBoost(e.target.value)} />
              </div>
              <div>
                <label className="text-[11px] text-mink block mb-1">TTL (Hours)</label>
                <input type="number" className="input-dark" value={duration} onChange={e => setDuration(e.target.value)} />
              </div>
            </div>

            <div>
              <label className="text-xs text-mink block mb-1">Rerouting Advisory</label>
              <textarea
                className="input-dark h-20 resize-none text-xs"
                placeholder="Advisory for navigation algorithms and field responders..."
                value={note}
                onChange={e => setNote(e.target.value)}
              />
            </div>

            {error && (
              <div className="bg-siren/10 border border-siren/30 rounded-lg px-3 py-2 text-xs text-siren">
                ⚠ {error}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={() => { setShowModal(false); resetForm(); }} className="btn-ghost text-xs">
                Cancel
              </button>
              <button type="submit" className="btn-siren text-xs flex items-center gap-1.5" disabled={submitting}>
                {submitting ? <RefreshCw size={12} className="animate-spin" /> : <Zap size={12} />}
                {submitting ? 'Activating...' : 'Activate Geofence'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
