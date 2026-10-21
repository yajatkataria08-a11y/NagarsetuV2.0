'use client';

import { useState } from 'react';
import {
  Settings, Flame, Database, Bot, Smartphone,
  Save, CheckCircle2, ShieldCheck, Key, Radio
} from 'lucide-react';
import { cn } from '@/lib/utils';

export default function SettingsPage() {
  const [fbProject, setFbProject] = useState('nagarsetu-prod');
  const [fbDbUrl, setFbDbUrl] = useState('https://nagarsetu-prod.firebaseio.com');
  const [fbSyncEnabled, setFbSyncEnabled] = useState(true);

  const [supabaseUrl, setSupabaseUrl] = useState('https://zkvjffgijzch.supabase.co');
  const [supabaseKey, setSupabaseKey] = useState('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');

  const [aiRejectThreshold, setAiRejectThreshold] = useState(0.25);
  const [aiVerifyThreshold, setAiVerifyThreshold] = useState(0.85);
  const [aiAutoRun, setAiAutoRun] = useState(true);

  const [smsSimulated, setSmsSimulated] = useState(true);
  const [saved, setSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  return (
    <div className="h-full overflow-y-auto p-5 space-y-6 bg-void">
      <form onSubmit={handleSave} className="max-w-4xl space-y-6">
        {/* Top Header */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-ink">System Configuration & Integrations</h2>
            <p className="text-xs text-mink">Configure cloud providers, AI thresholds, and native client bridges</p>
          </div>
          <button
            type="submit"
            className="btn-cyber text-xs py-2 px-4 flex items-center gap-2 shadow-glow-cyber"
          >
            {saved ? (
              <>
                <CheckCircle2 size={14} className="text-void" />
                <span className="font-bold">Settings Saved!</span>
              </>
            ) : (
              <>
                <Save size={14} />
                <span>Save Configuration</span>
              </>
            )}
          </button>
        </div>

        {/* 1. Firebase Configuration */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2.5 border-b border-white/5 pb-3">
            <Flame size={18} className="text-siren-orange" />
            <h3 className="text-sm font-bold text-ink">Firebase Realtime Database & FCM</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-mink mb-1 font-semibold">Firebase Project ID</label>
              <input
                className="input-dark w-full py-1.5"
                value={fbProject}
                onChange={e => setFbProject(e.target.value)}
              />
              <span className="text-[10px] text-faint mt-1 block">Identifier for Google Cloud project</span>
            </div>

            <div>
              <label className="block text-mink mb-1 font-semibold">Database URL</label>
              <input
                className="input-dark w-full py-1.5"
                value={fbDbUrl}
                onChange={e => setFbDbUrl(e.target.value)}
              />
            </div>
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-white/5">
            <div>
              <p className="text-xs font-semibold text-ink">Firebase Realtime Sync</p>
              <p className="text-[11px] text-mink">Listen to Firestore / RTDB for live mobile citizen reports</p>
            </div>
            <button
              type="button"
              onClick={() => setFbSyncEnabled(!fbSyncEnabled)}
              className={cn(
                'w-11 h-6 rounded-full transition-colors relative',
                fbSyncEnabled ? 'bg-safe' : 'bg-surface'
              )}
            >
              <span
                className={cn(
                  'w-4 h-4 rounded-full bg-white absolute top-1 transition-transform',
                  fbSyncEnabled ? 'right-1' : 'left-1'
                )}
              />
            </button>
          </div>
        </div>

        {/* 2. Supabase PostGIS Configuration */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2.5 border-b border-white/5 pb-3">
            <Database size={18} className="text-safe" />
            <h3 className="text-sm font-bold text-ink">Supabase PostGIS Cluster</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-mink mb-1 font-semibold">Project URL</label>
              <input
                className="input-dark w-full py-1.5"
                value={supabaseUrl}
                onChange={e => setSupabaseUrl(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-mink mb-1 font-semibold">Anon Public Key</label>
              <input
                type="password"
                className="input-dark w-full py-1.5 font-mono"
                value={supabaseKey}
                onChange={e => setSupabaseKey(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* 3. AI Verification Thresholds */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2.5 border-b border-white/5 pb-3">
            <Bot size={18} className="text-cyber" />
            <h3 className="text-sm font-bold text-ink">AI Engine Verification Rules</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-mink mb-1 font-semibold">
                Auto-Reject Threshold ({aiRejectThreshold})
              </label>
              <input
                type="range"
                min="0.05"
                max="0.5"
                step="0.05"
                value={aiRejectThreshold}
                onChange={e => setAiRejectThreshold(parseFloat(e.target.value))}
                className="w-full accent-siren"
              />
              <span className="text-[10px] text-faint mt-1 block">
                Reports with AI confidence score below this are auto-marked rejected.
              </span>
            </div>

            <div>
              <label className="block text-mink mb-1 font-semibold">
                Auto-Verify Threshold ({aiVerifyThreshold})
              </label>
              <input
                type="range"
                min="0.5"
                max="0.95"
                step="0.05"
                value={aiVerifyThreshold}
                onChange={e => setAiVerifyThreshold(parseFloat(e.target.value))}
                className="w-full accent-safe"
              />
              <span className="text-[10px] text-faint mt-1 block">
                Reports with AI confidence score above this are auto-verified.
              </span>
            </div>
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-white/5">
            <div>
              <p className="text-xs font-semibold text-ink">Automated YOLOv8 + Gemini Triage</p>
              <p className="text-[11px] text-mink">Execute multimodal verification script on new incoming reports</p>
            </div>
            <button
              type="button"
              onClick={() => setAiAutoRun(!aiAutoRun)}
              className={cn(
                'w-11 h-6 rounded-full transition-colors relative',
                aiAutoRun ? 'bg-safe' : 'bg-surface'
              )}
            >
              <span
                className={cn(
                  'w-4 h-4 rounded-full bg-white absolute top-1 transition-transform',
                  aiAutoRun ? 'right-1' : 'left-1'
                )}
              />
            </button>
          </div>
        </div>

        {/* 4. Android & Wear OS Native Client Bridge */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2.5 border-b border-white/5 pb-3">
            <Smartphone size={18} className="text-purple-400" />
            <h3 className="text-sm font-bold text-ink">Native Android & Wear OS Clients</h3>
          </div>

          <div className="space-y-3 text-xs text-mink">
            <div className="flex items-center justify-between p-3 rounded-lg bg-surface border border-white/5">
              <div>
                <p className="text-xs font-bold text-ink">Android Mobile App (13 Modules · 218 Kotlin files)</p>
                <p className="text-[10px] text-faint font-mono">Location: repo_v2/android/ · Min SDK 26 · Target SDK 34</p>
              </div>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-safe/10 text-safe border border-safe/20">
                Synced
              </span>
            </div>

            <div className="flex items-center justify-between p-3 rounded-lg bg-surface border border-white/5">
              <div>
                <p className="text-xs font-bold text-ink">Wear OS Watch Siren & Biometric App</p>
                <p className="text-[10px] text-faint font-mono">Features: Waveform Haptics · Heart Rate 112 BPM · 1-Tap SOS</p>
              </div>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-safe/10 text-safe border border-safe/20">
                Ready
              </span>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
