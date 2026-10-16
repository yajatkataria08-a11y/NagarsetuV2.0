'use client';

import { AlertOctagon, CheckCircle2, Clock, MapPin, Cpu, Phone, ExternalLink } from 'lucide-react';
import { cn, timeAgo, formatIncidentType } from '@/lib/utils';
import type { SosEvent } from '@/types';

interface SosCardProps {
  sos: SosEvent;
  onAcknowledge: (id: string) => void;
  onResolve: (id: string) => void;
  isSelected: boolean;
  onClick: () => void;
}

export default function SosCard({ sos, onAcknowledge, onResolve, isSelected, onClick }: SosCardProps) {
  const isActive = sos.status === 'active';
  const isAcked  = sos.status === 'acknowledged';

  return (
    <div
      onClick={onClick}
      className={cn(
        'glass-card p-3 cursor-pointer transition-all duration-200 group',
        isActive  && 'siren-border',
        isSelected && 'border-cyber/50',
        !isActive && !isSelected && 'hover:border-white/10'
      )}
    >
      {/* Header row */}
      <div className="flex items-start gap-2">
        <div className={cn(
          'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 text-sm font-bold',
          isActive ? 'bg-siren/20 text-siren' : 'bg-siren-orange/20 text-siren-orange'
        )}>
          {sos.userName.charAt(0)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <p className="text-sm font-semibold text-ink truncate">{sos.userName}</p>
            <span className={cn(
              'text-[10px] font-bold px-1.5 py-0.5 rounded-full',
              sos.sirenType === 'CRITICAL' ? 'badge-critical' :
              sos.sirenType === 'HIGH'     ? 'badge-high' : 'badge-moderate'
            )}>
              {sos.sirenType}
            </span>
          </div>
          <p className="text-[11px] text-mink truncate">{sos.ward}</p>
        </div>
        {/* Status dot */}
        <div className={isActive ? 'dot-sos' : 'dot-offline mt-1.5'} />
      </div>

      {/* Info row */}
      <div className="mt-2 grid grid-cols-2 gap-1.5">
        <div className="flex items-center gap-1 text-[11px] text-mink">
          <MapPin size={10} className="text-faint" />
          <span className="truncate">{sos.address ?? 'Tracking…'}</span>
        </div>
        <div className="flex items-center gap-1 text-[11px] text-mink">
          <Clock size={10} className="text-faint" />
          <span>{timeAgo(sos.triggeredAt)}</span>
        </div>
        {sos.heartRate && (
          <div className="flex items-center gap-1 text-[11px] text-mink">
            <Cpu size={10} className="text-faint" />
            <span>❤️ {sos.heartRate} BPM</span>
          </div>
        )}
        <div className="flex items-center gap-1 text-[11px] text-mink">
          <span>🔋 {sos.batteryLevel}%</span>
        </div>
      </div>

      {/* Actions */}
      {(isActive || isAcked) && (
        <div className="mt-2.5 flex gap-1.5">
          {isActive && (
            <button
              onClick={e => { e.stopPropagation(); onAcknowledge(sos.id); }}
              className="btn-ghost text-[11px] py-1 flex-1"
            >
              <CheckCircle2 size={11} />
              Acknowledge
            </button>
          )}
          {sos.trackingToken && (
            <a
              href={`/track/${sos.trackingToken}`}
              target="_blank"
              rel="noopener noreferrer"
              onClick={e => e.stopPropagation()}
              className="btn-ghost text-[11px] py-1"
            >
              <ExternalLink size={11} />
              Track
            </a>
          )}
          <button
            onClick={e => { e.stopPropagation(); onResolve(sos.id); }}
            className="btn-safe text-[11px] py-1 flex-1"
          >
            <AlertOctagon size={11} />
            Resolve
          </button>
        </div>
      )}
    </div>
  );
}
