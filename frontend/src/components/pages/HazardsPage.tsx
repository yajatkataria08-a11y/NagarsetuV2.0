'use client';

import { useState } from 'react';
import { ShieldAlert, Plus, Clock, MapPin, AlertTriangle, Trash2, CheckCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { HazardZone } from '@/types';

const INITIAL_HAZARDS: HazardZone[] = [
  {
    id: 'hz_01',
    name: 'Habibganj Underpass Flood Barrier',
    lat: 23.2680,
    lng: 77.4300,
    radiusMeters: 400,
    riskBoost: 2.5,
    expiresAt: Date.now() + 4 * 3600000,
    note: 'Underpass submerged after heavy rain — reroute cyclists and two-wheelers'
  },
  {
    id: 'hz_02',
    name: 'Hamidia Bus Stand Night Geofence',
    lat: 23.2550,
    lng: 77.4050,
    radiusMeters: 300,
    riskBoost: 1.8,
    expiresAt: Date.now() + 6 * 3600000,
    note: 'Unlit zone with recent harassment reports — enforce SafePath Main Road routing'
  }
];

export default function HazardsPage() {
  const [hazards, setHazards] = useState<HazardZone[]>(INITIAL_HAZARDS);
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [radius, setRadius] = useState('300');
  const [boost, setBoost] = useState('2.0');
  const [duration, setDuration] = useState('4');
  const [note, setNote] = useState('');

  const addHazard = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name) return;

    const newZone: HazardZone = {
      id: 'hz_' + Date.now(),
      name,
      lat: 23.2599 + (Math.random() - 0.5) * 0.02,
      lng: 77.4126 + (Math.random() - 0.5) * 0.02,
      radiusMeters: parseInt(radius) || 300,
      riskBoost: parseFloat(boost) || 1.5,
      expiresAt: Date.now() + parseInt(duration) * 3600000,
      note
    };

    setHazards([newZone, ...hazards]);
    setShowModal(false);
    setName('');
    setNote('');
  };

  const removeHazard = (id: string) => {
    setHazards(hazards.filter(h => h.id !== id));
  };

  return (
    <div className="flex flex-col h-full bg-void overflow-y-auto p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-bold text-ink">Dynamic Hazard Geofences (Raksha Engine)</h2>
          <p className="text-xs text-mink">Active temporary danger zones with automated time-to-live expiration</p>
        </div>
        <button onClick={() => setShowModal(true)} className="btn-cyber text-xs">
          <Plus size={14} />
          Create Dynamic Geofence
        </button>
      </div>

      {/* Grid of Hazard Zones */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {hazards.map((hz) => {
          const remainingMin = Math.max(0, Math.round((hz.expiresAt - Date.now()) / 60000));
          const hours = Math.floor(remainingMin / 60);
          const mins = remainingMin % 60;

          return (
            <div key={hz.id} className="glass-card p-4 border border-siren/30 shadow-glow-siren space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-siren/20 flex items-center justify-center text-siren">
                    <ShieldAlert size={16} />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-ink">{hz.name}</h3>
                    <p className="text-[11px] text-mink">Radius: {hz.radiusMeters}m · Multiplier: {hz.riskBoost}×</p>
                  </div>
                </div>
                <button onClick={() => removeHazard(hz.id)} className="text-faint hover:text-siren transition-colors">
                  <Trash2 size={14} />
                </button>
              </div>

              <p className="text-xs text-mink bg-abyss/60 p-2.5 rounded-lg border border-white/5">
                {hz.note || 'No specific note added.'}
              </p>

              <div className="flex items-center justify-between text-xs pt-1 border-t border-white/5">
                <div className="flex items-center gap-1.5 text-mink">
                  <MapPin size={12} className="text-cyber" />
                  <span className="font-mono">{hz.lat.toFixed(4)}, {hz.lng.toFixed(4)}</span>
                </div>
                <div className="flex items-center gap-1 text-caution font-medium">
                  <Clock size={12} />
                  <span>Expires in {hours}h {mins}m</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={addHazard} className="glass-card max-w-md w-full p-5 border border-white/10 space-y-4">
            <h3 className="text-sm font-bold text-ink flex items-center gap-2">
              <AlertTriangle size={16} className="text-caution" />
              Declare Temporary Hazard Geofence
            </h3>

            <div>
              <label className="text-xs text-mink block mb-1">Hazard Name / Description</label>
              <input
                required
                className="input-dark"
                placeholder="e.g. VIP Road Waterlogging or Protests"
                value={name}
                onChange={e => setName(e.target.value)}
              />
            </div>

            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="text-[11px] text-mink block mb-1">Radius (meters)</label>
                <input
                  type="number"
                  className="input-dark"
                  value={radius}
                  onChange={e => setRadius(e.target.value)}
                />
              </div>
              <div>
                <label className="text-[11px] text-mink block mb-1">Risk Multiplier</label>
                <input
                  type="number"
                  step="0.1"
                  className="input-dark"
                  value={boost}
                  onChange={e => setBoost(e.target.value)}
                />
              </div>
              <div>
                <label className="text-[11px] text-mink block mb-1">TTL (Hours)</label>
                <input
                  type="number"
                  className="input-dark"
                  value={duration}
                  onChange={e => setDuration(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label className="text-xs text-mink block mb-1">Rerouting Advisory Note</label>
              <textarea
                className="input-dark h-20 resize-none text-xs"
                placeholder="Advisory message for navigation algorithms and field responders..."
                value={note}
                onChange={e => setNote(e.target.value)}
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={() => setShowModal(false)} className="btn-ghost text-xs">
                Cancel
              </button>
              <button type="submit" className="btn-siren text-xs">
                Activate Geofence
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
