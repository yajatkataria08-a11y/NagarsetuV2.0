'use client';

/**
 * Dependency-free SVG charts for the Infrastructure Health page.
 * Colours follow the app's status palette (safe / caution / siren).
 */
import type { HealthBand, ReadingRecord, RulResult } from '@/types';

export const BAND_COLOR: Record<HealthBand, string> = {
  good: '#10B981',
  watch: '#F59E0B',
  critical: '#E53935',
  unknown: '#64748B',
};

export const BAND_LABEL: Record<HealthBand, string> = {
  good: 'GOOD',
  watch: 'WATCH',
  critical: 'CRITICAL',
  unknown: 'NOT ASSESSED',
};

// ── Health ring ──────────────────────────────────────────────────────────────
export function HealthRing({ score, band, size = 96 }: { score: number | null; band: HealthBand; size?: number }) {
  const r = size / 2 - 8;
  const c = 2 * Math.PI * r;
  const pct = score === null ? 0 : Math.max(0, Math.min(100, score)) / 100;
  const color = BAND_COLOR[band];
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img"
         aria-label={score === null ? 'Health not assessed' : `Health score ${score} out of 100, ${band}`}>
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="currentColor" strokeOpacity={0.12} strokeWidth={8} />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={8} strokeLinecap="round"
              strokeDasharray={`${c * pct} ${c}`} transform={`rotate(-90 ${size / 2} ${size / 2})`} />
      <text x="50%" y="50%" textAnchor="middle" dominantBaseline="central" fill={color}
            style={{ font: `700 ${size * 0.3}px ui-monospace, monospace` }}>
        {score === null ? '–' : Math.round(score)}
      </text>
    </svg>
  );
}

// ── RUL gauge: P10–P90 band on a log axis ────────────────────────────────────
const RUL_MIN = 0.01;
const RUL_MAX = 100;

export function RulGauge({ rul }: { rul: RulResult }) {
  const W = 400, H = 70, PAD = 14;
  const lo = Math.log10(RUL_MIN), hi = Math.log10(RUL_MAX);
  const x = (v: number) => PAD + ((Math.log10(Math.max(RUL_MIN, Math.min(RUL_MAX, v))) - lo) / (hi - lo)) * (W - 2 * PAD);
  const ticks = [0.01, 0.1, 1, 10, 100];

  if (rul.already_critical) {
    return <p className="text-xs text-siren font-mono">Crack length already exceeds the critical length — no remaining life under this model.</p>;
  }
  const x10 = x(rul.p10_years), x50 = x(rul.p50_years), x90 = x(rul.p90_years);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img"
         aria-label={`Remaining life P10 ${rul.p10_years} years, median ${rul.p50_years}, P90 ${rul.p90_years}`}>
      <rect x={PAD} y={24} width={W - 2 * PAD} height={10} rx={5} fill="currentColor" fillOpacity={0.1} />
      <rect x={x10} y={22} width={Math.max(3, x90 - x10)} height={14} rx={5} fill="#00D4FF" fillOpacity={0.35}
            stroke="#00D4FF" strokeWidth={1} />
      <line x1={x50} x2={x50} y1={16} y2={42} stroke="#00D4FF" strokeWidth={3} />
      <text x={x10} y={12} textAnchor="middle" fontSize={10} fill="currentColor" fillOpacity={0.8} fontFamily="ui-monospace,monospace">P10 {fmtYears(rul.p10_years)}</text>
      <text x={x90} y={12} textAnchor="middle" fontSize={10} fill="currentColor" fillOpacity={0.8} fontFamily="ui-monospace,monospace">P90 {fmtYears(rul.p90_years)}</text>
      <text x={x50} y={54} textAnchor="middle" fontSize={11} fontWeight={700} fill="#00D4FF" fontFamily="ui-monospace,monospace">P50 {fmtYears(rul.p50_years)}</text>
      {ticks.map(t => (
        <g key={t}>
          <line x1={x(t)} x2={x(t)} y1={36} y2={40} stroke="currentColor" strokeOpacity={0.4} />
          <text x={x(t)} y={H - 2} textAnchor="middle" fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">{t}y</text>
        </g>
      ))}
    </svg>
  );
}

export function fmtYears(y: number): string {
  if (y < 1 / 12) return `${Math.max(1, Math.round(y * 365))}d`;
  if (y < 1) return `${(y * 12).toFixed(1)}mo`;
  return `${y.toFixed(y < 10 ? 1 : 0)}y`;
}

// ── Anomaly score ratio over readings (1.0 = alarm threshold) ────────────────
export function AnomalyChart({ readings }: { readings: ReadingRecord[] }) {
  const pts = [...readings].reverse().slice(-40); // API returns newest first
  const W = 400, H = 130, L = 30, R = 8, T = 10, B = 18;
  if (pts.length === 0) return <Empty text="No telemetry received yet." />;
  const maxY = Math.max(2, ...pts.map(p => p.ratio)) * 1.1;
  const sx = (i: number) => L + (pts.length === 1 ? (W - L - R) / 2 : (i / (pts.length - 1)) * (W - L - R));
  const sy = (v: number) => T + (1 - v / maxY) * (H - T - B);
  const path = pts.map((p, i) => `${i ? 'L' : 'M'}${sx(i).toFixed(1)},${sy(p.ratio).toFixed(1)}`).join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Anomaly score relative to threshold over time">
      <line x1={L} x2={W - R} y1={sy(1)} y2={sy(1)} stroke="#E53935" strokeDasharray="4 3" strokeOpacity={0.8} />
      <text x={W - R} y={sy(1) - 3} textAnchor="end" fontSize={9} fill="#E53935" fontFamily="ui-monospace,monospace">alarm threshold</text>
      <line x1={L} x2={L} y1={T} y2={H - B} stroke="currentColor" strokeOpacity={0.2} />
      <text x={L - 4} y={sy(0) + 3} textAnchor="end" fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">0</text>
      <text x={L - 4} y={sy(1) + 3} textAnchor="end" fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">1</text>
      <path d={path} fill="none" stroke="#00D4FF" strokeWidth={1.5} />
      {pts.map((p, i) => (
        <circle key={p.id} cx={sx(i)} cy={sy(p.ratio)} r={3.5} fill={p.flag ? '#E53935' : '#00D4FF'} stroke="#0B1020" strokeWidth={1}>
          <title>{`ratio ${p.ratio.toFixed(2)}${p.flag ? ' — ANOMALY' : ''} · ${new Date(p.created_at).toLocaleTimeString()}`}</title>
        </circle>
      ))}
      <text x={L} y={H - 4} fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">oldest</text>
      <text x={W - R} y={H - 4} textAnchor="end" fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">latest</text>
    </svg>
  );
}

// ── Health score history with band zones ─────────────────────────────────────
export function HealthHistoryChart({ history }: { history: { score: number | null; at: string; source: string }[] }) {
  const pts = history.filter((h): h is { score: number; at: string; source: string } => h.score !== null).slice(-60);
  const W = 400, H = 120, L = 26, R = 8, T = 8, B = 14;
  if (pts.length === 0) return <Empty text="No health history yet." />;
  const sx = (i: number) => L + (pts.length === 1 ? (W - L - R) / 2 : (i / (pts.length - 1)) * (W - L - R));
  const sy = (v: number) => T + (1 - v / 100) * (H - T - B);
  const path = pts.map((p, i) => `${i ? 'L' : 'M'}${sx(i).toFixed(1)},${sy(p.score).toFixed(1)}`).join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Health score history">
      <rect x={L} y={sy(100)} width={W - L - R} height={sy(70) - sy(100)} fill="#10B981" fillOpacity={0.08} />
      <rect x={L} y={sy(70)} width={W - L - R} height={sy(40) - sy(70)} fill="#F59E0B" fillOpacity={0.08} />
      <rect x={L} y={sy(40)} width={W - L - R} height={sy(0) - sy(40)} fill="#E53935" fillOpacity={0.08} />
      {[0, 40, 70, 100].map(v => (
        <text key={v} x={L - 4} y={sy(v) + 3} textAnchor="end" fontSize={9} fill="currentColor" fillOpacity={0.5} fontFamily="ui-monospace,monospace">{v}</text>
      ))}
      <path d={path} fill="none" stroke="#00D4FF" strokeWidth={1.5} />
      {pts.map((p, i) => (
        <circle key={`${p.at}-${i}`} cx={sx(i)} cy={sy(p.score)} r={3} fill="#00D4FF">
          <title>{`${p.score} · ${p.source} · ${new Date(p.at).toLocaleString()}`}</title>
        </circle>
      ))}
    </svg>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="text-xs text-mink font-mono py-6 text-center">{text}</p>;
}
