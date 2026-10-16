'use client';

import { Clock, MapPin, ShieldCheck, XCircle, Bot } from 'lucide-react';
import { cn, timeAgo, formatIncidentType, severityColor } from '@/lib/utils';
import type { Incident } from '@/types';

interface IncidentCardProps {
  incident: Incident;
  onVerify: (id: string) => void;
  onReject: (id: string) => void;
  isSelected: boolean;
  onClick: () => void;
}

const STATUS_CLASSES: Record<string, string> = {
  pending:     'badge-pending',
  ai_verified: 'badge-moderate',
  verified:    'badge-low',
  dispatched:  'badge-moderate',
  resolved:    'badge-low',
  rejected:    'bg-faint/20 text-faint border-white/5 inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold border',
};

export default function IncidentCard({ incident, onVerify, onReject, isSelected, onClick }: IncidentCardProps) {
  const isPending    = incident.status === 'pending';
  const isVerified   = incident.status === 'verified' || incident.status === 'ai_verified';
  const isRejected   = incident.status === 'rejected';
  const isResolved   = incident.status === 'resolved';
  const isCritical   = incident.severity >= 4;

  return (
    <div
      onClick={onClick}
      className={cn(
        'glass-card p-3 cursor-pointer transition-all duration-200',
        isCritical && !isResolved && !isRejected && 'border-siren-orange/30',
        isSelected && 'border-cyber/50',
        !isCritical && !isSelected && 'hover:border-white/10'
      )}
    >
      {/* Header */}
      <div className="flex items-start gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-semibold text-ink">
              {formatIncidentType(incident.type)}
            </span>
            <span className={STATUS_CLASSES[incident.status] ?? 'badge-pending'}>
              {incident.status.replace('_', ' ')}
            </span>
          </div>
          <p className="text-[11px] text-mink mt-0.5 line-clamp-1">{incident.description}</p>
        </div>
        {/* Severity */}
        <div className="flex flex-col items-center gap-0.5 flex-shrink-0">
          <span className={cn('text-sm font-bold font-mono', severityColor(incident.severity))}>
            {incident.severity}
          </span>
          <span className="text-[9px] text-faint">/5</span>
        </div>
      </div>

      {/* Meta */}
      <div className="mt-2 flex items-center gap-3 flex-wrap">
        <div className="flex items-center gap-1 text-[11px] text-mink">
          <MapPin size={10} className="text-faint" />
          <span className="truncate max-w-[120px]">{incident.ward}</span>
        </div>
        <div className="flex items-center gap-1 text-[11px] text-mink">
          <Clock size={10} className="text-faint" />
          <span>{timeAgo(incident.reportedAt)}</span>
        </div>
        {incident.aiScore != null && (
          <div className="flex items-center gap-1 text-[11px] text-cyber">
            <Bot size={10} />
            <span>AI: {Math.round(incident.aiScore * 100)}%</span>
          </div>
        )}
      </div>

      {/* Actions */}
      {isPending && (
        <div className="mt-2.5 flex gap-1.5">
          <button
            onClick={e => { e.stopPropagation(); onVerify(incident.id); }}
            className="btn-safe text-[11px] py-1 flex-1"
          >
            <ShieldCheck size={11} />
            Verify
          </button>
          <button
            onClick={e => { e.stopPropagation(); onReject(incident.id); }}
            className="btn-ghost text-[11px] py-1"
          >
            <XCircle size={11} />
            Reject
          </button>
        </div>
      )}
    </div>
  );
}
