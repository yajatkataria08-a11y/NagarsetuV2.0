'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Activity, AlertTriangle, FlaskConical, Landmark, Lock, UploadCloud } from 'lucide-react';
import TacticalMap from '@/components/command/TacticalMap';
import { AnomalyChart, BAND_COLOR, BAND_LABEL, HealthHistoryChart, HealthRing, RulGauge } from '@/components/infra/charts';
import { API_BASE, api, authApi, clearToken, getToken, structuresApi } from '@/lib/api';
import { SIM_FS, simulateWindow } from '@/lib/simulateVibration';
import { cn } from '@/lib/utils';
import type {
  HealthBand, InspectionSummary, StructureHealthEvent, StructureHistory, StructureSummary,
} from '@/types';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const STREAM_INTERVAL_MS = 6000;   // stays under the API rate limiter (200 req / 15 min / IP)
const STREAM_MAX_WINDOWS = 20;
const BAND_ORDER: Record<HealthBand, number> = { critical: 0, watch: 1, good: 2, unknown: 3 };

type StructureDetail = StructureSummary & { latest_inspection: InspectionSummary | null };

const errMsg = (e: unknown) => (e instanceof Error ? e.message : String(e));
const isUnauthorized = (e: unknown) => (e as { status?: number })?.status === 401;
const fmt = (n: number | null | undefined, d = 2) => (n === null || n === undefined ? '—' : n.toFixed(d));

// ── Login gate (the app has no other login flow) ─────────────────────────────
function LoginGate({ onLoggedIn, notice }: { onLoggedIn: () => void; notice?: string }) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      await authApi.login(phone.trim(), password);
      onLoggedIn();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="h-full flex items-center justify-center p-6">
      <div className="w-full max-w-sm bg-surface border border-white/5 rounded-xl p-6 shadow-card">
        <div className="flex items-center gap-2 mb-1 text-ink font-semibold"><Lock size={16} /> Staff sign-in required</div>
        <p className="text-xs text-mink mb-4">{notice || 'Structural health data is restricted to municipal staff.'}</p>
        <label className="block text-[11px] font-mono text-mink mb-1" htmlFor="infra-phone">Phone</label>
        <input id="infra-phone" value={phone} onChange={e => setPhone(e.target.value)} placeholder="+91…" autoComplete="username"
               className="w-full mb-3 px-3 py-2 rounded-lg bg-raised border border-white/10 text-sm text-ink outline-none focus:border-cyber" />
        <label className="block text-[11px] font-mono text-mink mb-1" htmlFor="infra-pw">Password</label>
        <input id="infra-pw" type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password"
               onKeyDown={e => e.key === 'Enter' && submit()}
               className="w-full mb-3 px-3 py-2 rounded-lg bg-raised border border-white/10 text-sm text-ink outline-none focus:border-cyber" />
        {error && <p role="alert" className="text-xs text-siren mb-3">{error}</p>}
        <button onClick={submit} disabled={busy || !phone || !password}
                className="w-full py-2 rounded-lg bg-cyber text-void font-semibold text-sm disabled:opacity-40">
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </div>
    </div>
  );
}

// ── Small presentational helpers ─────────────────────────────────────────────
function Section({ title, icon, children, right }: { title: string; icon?: React.ReactNode; children: React.ReactNode; right?: React.ReactNode }) {
  return (
    <section className="bg-surface border border-white/5 rounded-xl p-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="flex items-center gap-2 text-[11px] font-mono font-bold tracking-wider uppercase text-mink">{icon}{title}</h3>
        {right}
      </div>
      {children}
    </section>
  );
}

function Badge({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: 'neutral' | 'warn' | 'bad' }) {
  const cls = tone === 'warn' ? 'bg-amber-500/15 text-amber-300 border-amber-500/30'
    : tone === 'bad' ? 'bg-siren/15 text-siren border-siren/30' : 'bg-white/5 text-mink border-white/10';
  return <span className={cn('inline-block px-1.5 py-0.5 rounded border text-[10px] font-mono font-bold tracking-wide', cls)}>{children}</span>;
}

// ── Page ─────────────────────────────────────────────────────────────────────
export default function InfraHealthPage() {
  const [ready, setReady] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [authNotice, setAuthNotice] = useState('');
  const [structures, setStructures] = useState<StructureSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<StructureDetail | null>(null);
  const [history, setHistory] = useState<StructureHistory | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [loadError, setLoadError] = useState('');
  const [live, setLive] = useState(false);

  const selectedRef = useRef<string | null>(null);
  selectedRef.current = selectedId;

  const handleAuthFailure = useCallback((e: unknown) => {
    if (isUnauthorized(e)) {
      clearToken();
      setAuthed(false);
      setAuthNotice('Session expired or not signed in.');
      return true;
    }
    return false;
  }, []);

  const loadList = useCallback(async () => {
    try {
      setStructures(await structuresApi.list());
      setLoadError('');
    } catch (e) {
      if (!handleAuthFailure(e)) setLoadError(`Could not load structures: ${errMsg(e)}`);
    }
  }, [handleAuthFailure]);

  const loadDetail = useCallback(async (id: string) => {
    try {
      const [d, h] = await Promise.all([
        api.get<StructureDetail>(`/api/structures/${id}`),
        structuresApi.history(id),
      ]);
      if (selectedRef.current !== id) return;   // user switched away meanwhile
      setDetail(d);
      setHistory(h);
    } catch (e) {
      if (!handleAuthFailure(e)) setLoadError(`Could not load structure: ${errMsg(e)}`);
    }
  }, [handleAuthFailure]);

  // initial auth check
  useEffect(() => {
    setAuthed(!!getToken());
    setReady(true);
  }, []);

  useEffect(() => { if (authed) loadList(); }, [authed, loadList]);

  useEffect(() => {
    setDetail(null);
    setHistory(null);
    if (selectedId) loadDetail(selectedId);
  }, [selectedId, loadDetail]);

  // annotated image (behind auth -> blob URL)
  const latest = history?.inspections[0] ?? null;
  useEffect(() => {
    let url: string | null = null;
    let cancelled = false;
    setImageUrl(null);
    if (latest?.image_url) {
      structuresApi.imageBlobUrl(latest.image_url)
        .then(u => { if (cancelled) URL.revokeObjectURL(u); else { url = u; setImageUrl(u); } })
        .catch(() => { /* image is optional */ });
    }
    return () => { cancelled = true; if (url) URL.revokeObjectURL(url); };
  }, [latest?.id, latest?.image_url]);

  // realtime: staff-only socket event
  useEffect(() => {
    if (!authed) return;
    let socket: { disconnect: () => void } | null = null;
    let closed = false;
    import('socket.io-client').then(({ io }) => {
      if (closed) return;
      const s = io(API_BASE, { auth: { token: getToken() }, reconnectionAttempts: 5 });
      s.on('connect', () => setLive(true));
      s.on('disconnect', () => setLive(false));
      s.on('structure:health_update', (ev: StructureHealthEvent) => {
        setStructures(list => list.map(x => x.id === ev.id
          ? { ...x, health: { ...x.health, score: ev.score, band: ev.band, updated_at: ev.updated_at, components: ev.components } }
          : x));
        if (selectedRef.current === ev.id) loadDetail(ev.id);
      });
      socket = s;
    });
    return () => { closed = true; socket?.disconnect(); setLive(false); };
  }, [authed, loadDetail]);

  const sorted = useMemo(() => [...structures].sort((a, b) =>
    BAND_ORDER[a.health.band] - BAND_ORDER[b.health.band] || (a.health.score ?? 101) - (b.health.score ?? 101)), [structures]);
  const counts = useMemo(() => {
    const c: Record<HealthBand, number> = { good: 0, watch: 0, critical: 0, unknown: 0 };
    structures.forEach(s => { c[s.health.band]++; });
    return c;
  }, [structures]);
  const selected = structures.find(s => s.id === selectedId) ?? null;
  const center = useMemo<[number, number] | undefined>(
    () => (structures[0] ? [structures[0].lat, structures[0].lng] : undefined), [structures.length > 0]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!ready) return null;
  if (!authed) return <LoginGate notice={authNotice} onLoggedIn={() => { setAuthNotice(''); setAuthed(true); }} />;

  return (
    <div className="flex h-full">
      {/* ── Map + list ─────────────────────────────────────────────── */}
      <div className="flex-1 min-w-0 flex flex-col">
        <div className="flex-shrink-0 flex items-center gap-4 px-5 py-3 border-b border-white/5">
          <div className="flex items-center gap-2 text-ink font-semibold"><Landmark size={18} className="text-cyber" /> Infrastructure Health</div>
          <div className="flex items-center gap-3 text-xs font-mono">
            {(['critical', 'watch', 'good', 'unknown'] as HealthBand[]).map(b => (
              <span key={b} className="flex items-center gap-1.5 text-mink">
                <span className="w-2.5 h-2.5 rounded-sm" style={{ background: BAND_COLOR[b] }} />{counts[b]} {BAND_LABEL[b].toLowerCase()}
              </span>
            ))}
          </div>
          <span className={cn('ml-auto flex items-center gap-1.5 text-[11px] font-mono', live ? 'text-safe' : 'text-mink')}>
            <span className={cn('w-2 h-2 rounded-full', live ? 'bg-safe animate-pulse' : 'bg-faint')} />{live ? 'LIVE' : 'OFFLINE'}
          </span>
        </div>
        {loadError && <p role="alert" className="px-5 py-2 text-xs text-siren border-b border-white/5">{loadError}</p>}

        <div className="flex-1 min-h-0 p-3">
          <TacticalMap sos={[]} incidents={[]} structures={structures} selectedStructureId={selectedId}
                       onSelectStructure={setSelectedId} center={center} />
        </div>

        <div className="flex-shrink-0 max-h-44 overflow-y-auto border-t border-white/5">
          <table className="w-full text-xs">
            <thead className="sticky top-0 bg-abyss text-mink font-mono text-[10px] uppercase">
              <tr><th className="text-left px-5 py-1.5">Structure</th><th className="text-left">Type</th><th className="text-left">Ward</th><th className="text-right pr-3">Score</th><th className="text-left pr-5">Band</th></tr>
            </thead>
            <tbody>
              {sorted.map(s => (
                <tr key={s.id} onClick={() => setSelectedId(s.id)} tabIndex={0}
                    onKeyDown={e => (e.key === 'Enter' || e.key === ' ') && setSelectedId(s.id)}
                    className={cn('cursor-pointer border-t border-white/5 hover:bg-white/5', s.id === selectedId && 'bg-cyber/10')}>
                  <td className="px-5 py-1.5 text-ink">{s.name}</td>
                  <td className="text-mink">{s.type.replace(/_/g, ' ')}</td>
                  <td className="text-mink">{s.ward}</td>
                  <td className="text-right pr-3 font-mono text-ink">{fmt(s.health.score, 1)}</td>
                  <td className="pr-5 font-mono font-bold" style={{ color: BAND_COLOR[s.health.band] }}>{BAND_LABEL[s.health.band]}</td>
                </tr>
              ))}
              {sorted.length === 0 && <tr><td colSpan={5} className="px-5 py-4 text-mink text-center">No structures registered. Run <code>node scripts/seed_infra.js</code> or POST /api/structures.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Detail panel ───────────────────────────────────────────── */}
      <aside className="w-[440px] flex-shrink-0 border-l border-white/5 bg-abyss overflow-y-auto p-4 space-y-4 text-ink">
        {!selected && <p className="text-sm text-mink text-center mt-16">Select a structure on the map or in the list.</p>}
        {selected && (
          <>
            <div className="flex items-center gap-4">
              <HealthRing score={selected.health.score} band={selected.health.band} />
              <div className="min-w-0">
                <h2 className="font-semibold text-ink leading-tight">{selected.name}</h2>
                <p className="text-xs text-mink">{selected.type.replace(/_/g, ' ')} · {selected.ward}</p>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  <Badge>{BAND_LABEL[selected.health.band]}</Badge>
                  {selected.fictional && <Badge tone="warn">FICTIONAL DEMO STRUCTURE</Badge>}
                </div>
                <p className="text-[11px] text-mink mt-1 font-mono">
                  {selected.health.updated_at ? `updated ${new Date(selected.health.updated_at).toLocaleString()}` : 'never assessed'}
                </p>
              </div>
            </div>

            <ComponentsStrip detail={detail} />
            <InspectionSection latest={latest} imageUrl={imageUrl} />
            <SensorSection structureId={selected.id} history={history} onSent={() => loadDetail(selected.id)} />
            <RulSection latest={latest} />
            <Section title="Health history" icon={<Activity size={13} />}>
              <div className="text-ink"><HealthHistoryChart history={history?.health_history ?? []} /></div>
            </Section>
            <UploadSection structureId={selected.id} onDone={() => { loadDetail(selected.id); loadList(); }} />
          </>
        )}
      </aside>
    </div>
  );
}

// ── Risk components ──────────────────────────────────────────────────────────
function ComponentsStrip({ detail }: { detail: StructureDetail | null }) {
  const c = detail?.health.components;
  const items: [string, number | null | undefined][] = [['Defect', c?.defect], ['Vibration', c?.anomaly], ['Fatigue life', c?.rul]];
  return (
    <div className="grid grid-cols-3 gap-2">
      {items.map(([label, v]) => (
        <div key={label} className="bg-surface border border-white/5 rounded-lg px-3 py-2">
          <div className="text-[10px] font-mono text-mink uppercase">{label} risk</div>
          <div className="font-mono text-lg text-ink">{v === null || v === undefined ? 'n/a' : v.toFixed(0)}</div>
        </div>
      ))}
      <p className="col-span-3 text-[10px] text-mink">Higher = worse. “n/a” means never measured — it is left out of the score, not counted as healthy.</p>
    </div>
  );
}

// ── Inspection: annotated image + crack table ────────────────────────────────
function InspectionSection({ latest, imageUrl }: { latest: InspectionSummary | null; imageUrl: string | null }) {
  const crack = latest?.crack;
  return (
    <Section title="Latest inspection" icon={<FlaskConical size={13} />}
             right={latest ? <span className="text-[10px] font-mono text-mink">{new Date(latest.created_at).toLocaleString()}</span> : undefined}>
      {!latest && <p className="text-xs text-mink">No inspection uploaded yet.</p>}
      {latest && (
        <div className="space-y-3">
          {imageUrl && /* eslint-disable-next-line @next/next/no-img-element */
            <img src={imageUrl} alt="Annotated inspection photo: crack mask in red, centreline in yellow" className="w-full rounded-lg border border-white/10" />}
          {crack && crack.found ? (
            <table className="w-full text-xs">
              <tbody className="divide-y divide-white/5">
                <tr><td className="py-1 text-mink">Crack length</td><td className="text-right font-mono">{crack.length_mm !== null ? `${fmt(crack.length_mm, 1)} mm` : `${fmt(crack.length_px, 0)} px`}</td></tr>
                <tr><td className="py-1 text-mink">Max width</td><td className="text-right font-mono">{crack.max_width_mm !== null ? `${fmt(crack.max_width_mm, 2)} mm` : `${fmt(crack.max_width_px, 1)} px`}</td></tr>
                <tr><td className="py-1 text-mink">Mean width</td><td className="text-right font-mono">{crack.mean_width_mm !== null ? `${fmt(crack.mean_width_mm, 2)} mm` : `${fmt(crack.mean_width_px, 1)} px`}</td></tr>
                <tr><td className="py-1 text-mink">Severity</td><td className="text-right font-mono">{crack.severity ?? 'needs reference scale'}</td></tr>
              </tbody>
            </table>
          ) : <p className="text-xs text-mink">No crack detected in this photo.</p>}
          <div className="flex flex-wrap gap-1.5">
            <Badge tone={latest.detector === 'unavailable' ? 'warn' : 'neutral'}>
              {latest.detector === 'unavailable' ? 'DEFECT DETECTOR: NOT TRAINED' : `DETECTOR: ${latest.detector}`}
            </Badge>
            <Badge tone={latest.mask_source === 'classical_cv' ? 'warn' : 'neutral'}>MASK: {latest.mask_source}</Badge>
          </div>
          {latest.notes.length > 0 && (
            <ul className="text-[11px] text-mink list-disc pl-4 space-y-0.5">{latest.notes.map(n => <li key={n}>{n}</li>)}</ul>
          )}
        </div>
      )}
    </Section>
  );
}

// ── Sensor: anomaly chart + simulated stream ─────────────────────────────────
function SensorSection({ structureId, history, onSent }: { structureId: string; history: StructureHistory | null; onSent: () => void }) {
  const [streaming, setStreaming] = useState(false);
  const [mode, setMode] = useState<'healthy' | 'damaged'>('healthy');
  const [sent, setSent] = useState(0);
  const [status, setStatus] = useState('');
  const modeRef = useRef(mode);
  modeRef.current = mode;
  const sentRef = useRef(0);

  const sendOne = useCallback(async () => {
    try {
      const r = await structuresApi.telemetry(structureId, {
        samples: simulateWindow(modeRef.current === 'damaged' ? 0.3 : 0, 10), fs: SIM_FS,
      });
      sentRef.current += 1;
      setSent(sentRef.current);
      setStatus(`window ${sentRef.current}: ratio ${r.anomaly.ratio.toFixed(2)} ${r.anomaly.flag ? '→ ANOMALY' : '→ normal'} · health ${r.health.score ?? '—'} (${r.health.band})`);
      onSent();
      return true;
    } catch (e) {
      setStatus(`send failed: ${errMsg(e)}`);
      return false;
    }
  }, [structureId, onSent]);

  useEffect(() => { sentRef.current = 0; setSent(0); setStatus(''); setStreaming(false); }, [structureId]);

  useEffect(() => {
    if (!streaming) return;
    let stopped = false;
    const tick = async () => {
      if (stopped) return;
      const ok = await sendOne();
      if (!ok || sentRef.current >= STREAM_MAX_WINDOWS) setStreaming(false);
    };
    tick();
    const t = setInterval(tick, STREAM_INTERVAL_MS);
    return () => { stopped = true; clearInterval(t); };
  }, [streaming, sendOne]);

  return (
    <Section title="Vibration anomaly" icon={<Activity size={13} />} right={<Badge tone="warn">SIMULATED-DATA MODEL</Badge>}>
      <div className="text-ink"><AnomalyChart readings={history?.readings ?? []} /></div>
      <p className="text-[11px] text-mink mt-2">
        The anomaly model was trained on simulated damped-oscillator data, not on real structures. Treat flags as a pipeline demo until it is retrained on LANL/Z24 or site data.
      </p>
      <div className="mt-3 flex items-center gap-2">
        <select value={mode} onChange={e => setMode(e.target.value as 'healthy' | 'damaged')} aria-label="Simulated condition"
                className="bg-raised border border-white/10 rounded-lg px-2 py-1.5 text-xs text-ink">
          <option value="healthy">simulated healthy</option>
          <option value="damaged">simulated 30% stiffness loss</option>
        </select>
        <button onClick={() => setStreaming(s => !s)}
                className={cn('px-3 py-1.5 rounded-lg text-xs font-semibold', streaming ? 'bg-siren text-white' : 'bg-cyber text-void')}>
          {streaming ? 'Stop stream' : 'Stream simulated telemetry'}
        </button>
        <button onClick={sendOne} disabled={streaming} className="px-3 py-1.5 rounded-lg text-xs border border-white/10 text-ink disabled:opacity-40">Send 1 window</button>
      </div>
      {status && <p role="status" className="mt-2 text-[11px] font-mono text-mink">{status} {streaming && `(${sent}/${STREAM_MAX_WINDOWS})`}</p>}
    </Section>
  );
}

// ── RUL ──────────────────────────────────────────────────────────────────────
function RulSection({ latest }: { latest: InspectionSummary | null }) {
  const rul = latest?.rul ?? null;
  return (
    <Section title="Remaining fatigue life (scenario)" icon={<AlertTriangle size={13} />}>
      {!rul ? (
        <p className="text-xs text-mink">Not computed. Tick “estimate remaining life” when uploading an inspection with a reference scale.</p>
      ) : (
        <>
          <div className="text-ink"><RulGauge rul={rul} /></div>
          <p className="text-[11px] text-amber-300 mt-2">
            Paris’ law models fatigue cracks in <b>steel</b>. A crack measured on a photo of concrete is not one, so read this as an illustration of the method, not a prediction.
          </p>
          <details className="mt-2 text-[11px] text-mink">
            <summary className="cursor-pointer">Assumptions</summary>
            <ul className="list-disc pl-4 mt-1 space-y-0.5">
              {Object.entries(rul.assumptions).map(([k, v]) => <li key={k}><span className="font-mono">{k}</span>: {String(v)}</li>)}
              <li>starting crack {fmt(rul.a0_m * 1000, 1)} mm → critical {fmt(rul.a_c_m * 1000, 0)} mm
                {rul.delta_sigma_mpa ? `, Δσ ${rul.delta_sigma_mpa} MPa` : ''}{rul.cycles_per_day ? `, ${rul.cycles_per_day} cycles/day` : ''}</li>
            </ul>
          </details>
        </>
      )}
    </Section>
  );
}

// ── Inspector upload ─────────────────────────────────────────────────────────
function UploadSection({ structureId, onDone }: { structureId: string; onDone: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [refMm, setRefMm] = useState('');
  const [refPx, setRefPx] = useState('');
  const [wantRul, setWantRul] = useState(false);
  const [dSigma, setDSigma] = useState('50');
  const [cpd, setCpd] = useState('10000');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  const num = (s: string) => (s.trim() === '' ? undefined : Number(s));
  const mm = num(refMm), px = num(refPx);
  const scaleInvalid = (refMm !== '' || refPx !== '') && !(mm && px && mm > 0 && px > 0);
  const rulNeedsScale = wantRul && !(mm && px);

  const submit = async () => {
    if (!file) return;
    setMsg(null);
    if (file.size > MAX_IMAGE_BYTES) { setMsg({ ok: false, text: 'Image is larger than 5 MB.' }); return; }
    if (!/^image\/(jpeg|png)$/.test(file.type)) { setMsg({ ok: false, text: 'Use a JPEG or PNG photo.' }); return; }
    setBusy(true);
    try {
      const dataUri = await new Promise<string>((resolve, reject) => {
        const r = new FileReader();
        r.onload = () => resolve(String(r.result));
        r.onerror = () => reject(new Error('could not read file'));
        r.readAsDataURL(file);
      });
      const res = await structuresApi.inspect(structureId, {
        image: dataUri,
        ...(mm && px ? { reference_mm: mm, reference_px: px } : {}),
        ...(wantRul ? { rul: { delta_sigma_mpa: num(dSigma), cycles_per_day: num(cpd) } } : {}),
      });
      setMsg({ ok: true, text: `Saved. Health ${res.health.score ?? '—'} (${res.health.band}).${res.escalation ? ' Hazard zone raised — needs engineer verification.' : ''}` });
      setFile(null);
      onDone();
    } catch (e) {
      setMsg({ ok: false, text: errMsg(e) });
    } finally {
      setBusy(false);
    }
  };

  const input = 'w-full px-2 py-1.5 rounded-lg bg-raised border border-white/10 text-xs text-ink outline-none focus:border-cyber';
  return (
    <Section title="Upload inspection photo" icon={<UploadCloud size={13} />}>
      <input type="file" accept="image/jpeg,image/png" aria-label="Inspection photo"
             onChange={e => setFile(e.target.files?.[0] ?? null)} className="block w-full text-xs text-mink mb-3" />
      <fieldset className="mb-3">
        <legend className="text-[11px] font-mono text-mink mb-1">Reference scale — a ruler or object of known size in the photo</legend>
        <div className="grid grid-cols-2 gap-2">
          <label className="text-[11px] text-mink">Real length (mm)
            <input inputMode="decimal" value={refMm} onChange={e => setRefMm(e.target.value)} placeholder="e.g. 100" className={input} /></label>
          <label className="text-[11px] text-mink">Length in image (px)
            <input inputMode="decimal" value={refPx} onChange={e => setRefPx(e.target.value)} placeholder="e.g. 842" className={input} /></label>
        </div>
        {scaleInvalid && <p role="alert" className="text-[11px] text-siren mt-1">Enter both values as positive numbers, or leave both empty (widths then stay in pixels, no severity).</p>}
      </fieldset>
      <label className="flex items-center gap-2 text-xs text-ink mb-2">
        <input type="checkbox" checked={wantRul} onChange={e => setWantRul(e.target.checked)} /> Estimate remaining fatigue life (steel model)
      </label>
      {wantRul && (
        <div className="grid grid-cols-2 gap-2 mb-2">
          <label className="text-[11px] text-mink">Stress range Δσ (MPa)<input inputMode="decimal" value={dSigma} onChange={e => setDSigma(e.target.value)} className={input} /></label>
          <label className="text-[11px] text-mink">Load cycles / day<input inputMode="decimal" value={cpd} onChange={e => setCpd(e.target.value)} className={input} /></label>
          {rulNeedsScale && <p className="col-span-2 text-[11px] text-amber-300">Without a reference scale no crack length in mm exists, so life cannot be estimated.</p>}
        </div>
      )}
      <button onClick={submit} disabled={!file || busy || scaleInvalid}
              className="w-full py-2 rounded-lg bg-cyber text-void font-semibold text-sm disabled:opacity-40">
        {busy ? 'Analysing…' : 'Analyse & save inspection'}
      </button>
      {msg && <p role={msg.ok ? 'status' : 'alert'} className={cn('mt-2 text-xs', msg.ok ? 'text-safe' : 'text-siren')}>{msg.text}</p>}
    </Section>
  );
}
