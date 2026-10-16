import { type ClassValue, clsx } from 'clsx';

/** Merge Tailwind class names safely */
export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

/** Format relative time (e.g. "3 min ago") */
export function timeAgo(ts: number): string {
  const diff = Date.now() - ts;
  const sec  = Math.floor(diff / 1000);
  if (sec < 60)  return `${sec}s ago`;
  const min  = Math.floor(sec / 60);
  if (min < 60)  return `${min}m ago`;
  const hr   = Math.floor(min / 60);
  if (hr  < 24)  return `${hr}h ago`;
  return `${Math.floor(hr / 24)}d ago`;
}

/** Format currency in INR */
export function formatINR(amount: number): string {
  if (amount >= 10_000_000) return `₹${(amount / 10_000_000).toFixed(1)}Cr`;
  if (amount >= 100_000)    return `₹${(amount / 100_000).toFixed(1)}L`;
  if (amount >= 1_000)      return `₹${(amount / 1_000).toFixed(0)}K`;
  return `₹${amount}`;
}

/** Severity colour class */
export function severityColor(sev: number): string {
  if (sev >= 5) return 'text-siren';
  if (sev >= 4) return 'text-siren-orange';
  if (sev >= 3) return 'text-caution';
  return 'text-safe';
}

/** Incident type human label */
export function formatIncidentType(type: string): string {
  const map: Record<string, string> = {
    ROAD_HAZARD:          '🚧 Road Hazard',
    HARASSMENT:           '⚠️ Harassment',
    FLOOD_RISK:           '🌊 Flood Risk',
    POOR_LIGHTING:        '💡 Poor Lighting',
    UNSAFE_SPOT:          '🚨 Unsafe Spot',
    SUSPICIOUS_ACTIVITY:  '👁️ Suspicious Activity',
    FIRE:                 '🔥 Fire',
    MEDICAL:              '🏥 Medical',
    POTHOLE:              '🕳️ Pothole',
    SEWAGE:               '🚰 Sewage',
    WATER_LOGGING:        '💧 Water Logging',
    GARBAGE:              '🗑️ Garbage',
    CRIME:                '🚓 Crime',
    OTHER:                '📍 Other',
  };
  return map[type] || type;
}

/** SOS severity from string */
export function sosBadgeClass(type: 'CRITICAL' | 'HIGH' | 'MODERATE'): string {
  if (type === 'CRITICAL') return 'badge-critical';
  if (type === 'HIGH')     return 'badge-high';
  return 'badge-moderate';
}

/** Generate a 6-char alphanumeric tracking token */
export function shortToken(id: string): string {
  return id.replace(/[^a-zA-Z0-9]/g, '').slice(0, 6).toUpperCase();
}
