'use client';

import { useState, useEffect, useRef } from 'react';
import { Phone, PhoneOff, Mic, MicOff, Volume2, Grid, User, ShieldAlert, ArrowLeft } from 'lucide-react';
import Link from 'next/link';

export default function FakeCallPage() {
  const [callerName, setCallerName] = useState('Papa (Home)');
  const [isCalling, setIsCalling] = useState(true);
  const [isAnswered, setIsAnswered] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const [isSpeaker, setIsSpeaker] = useState(true);
  const [screenDimmed, setScreenDimmed] = useState(false);

  const audioCtxRef = useRef<AudioContext | null>(null);
  const ringIntervalRef = useRef<any>(null);

  // Synthesize realistic phone ringtone via Web Audio API
  useEffect(() => {
    if (!isAnswered && isCalling) {
      try {
        const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
        if (AudioCtx) {
          const ctx = new AudioCtx();
          audioCtxRef.current = ctx;

          const playRingTone = () => {
            if (ctx.state === 'suspended') ctx.resume();
            const osc1 = ctx.createOscillator();
            const osc2 = ctx.createOscillator();
            const gain = ctx.createGain();

            osc1.type = 'sine';
            osc1.frequency.value = 440; // Standard dial tone frequency 1
            osc2.type = 'sine';
            osc2.frequency.value = 480; // Standard dial tone frequency 2

            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 1.6);

            osc1.connect(gain);
            osc2.connect(gain);
            gain.connect(ctx.destination);

            osc1.start();
            osc2.start();
            osc1.stop(ctx.currentTime + 1.6);
            osc2.stop(ctx.currentTime + 1.6);
          };

          playRingTone();
          ringIntervalRef.current = setInterval(playRingTone, 3000);
        }
      } catch (e) {
        console.warn('Web Audio not allowed without user gesture:', e);
      }
    }

    return () => {
      if (ringIntervalRef.current) clearInterval(ringIntervalRef.current);
      if (audioCtxRef.current) {
        audioCtxRef.current.close().catch(() => {});
      }
    };
  }, [isAnswered, isCalling]);

  // Call timer once answered
  useEffect(() => {
    let timer: any;
    if (isAnswered) {
      timer = setInterval(() => setElapsed(s => s + 1), 1000);
    }
    return () => clearInterval(timer);
  }, [isAnswered]);

  const handleAnswer = () => {
    if (ringIntervalRef.current) clearInterval(ringIntervalRef.current);
    if (audioCtxRef.current) audioCtxRef.current.close().catch(() => {});
    setIsAnswered(true);
  };

  const handleDecline = () => {
    if (ringIntervalRef.current) clearInterval(ringIntervalRef.current);
    if (audioCtxRef.current) audioCtxRef.current.close().catch(() => {});
    setIsCalling(false);
    setIsAnswered(false);
    setElapsed(0);
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  return (
    <div
      onClick={() => screenDimmed && setScreenDimmed(false)}
      className={`min-h-screen select-none transition-colors duration-300 flex flex-col justify-between p-6 ${
        screenDimmed ? 'bg-black' : 'bg-gradient-to-b from-slate-900 via-slate-950 to-black text-white'
      }`}
    >
      {/* Top Header / Exit */}
      {!screenDimmed && (
        <div className="flex items-center justify-between pt-4">
          <Link
            href="/report/qr"
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-full bg-slate-800/60 border border-slate-700/50"
          >
            <ArrowLeft size={13} /> Exit Call
          </Link>

          <button
            onClick={() => setScreenDimmed(true)}
            className="text-[11px] font-mono text-cyan-400 bg-cyan-950/40 border border-cyan-800/50 px-2.5 py-1 rounded-full"
            title="Tap to blank screen while holding to ear"
          >
            Ear Mode (Blackout)
          </button>
        </div>
      )}

      {/* Caller Avatar & Name */}
      {!screenDimmed && (
        <div className="flex flex-col items-center justify-center my-auto space-y-4">
          <div className="relative">
            <div className="w-28 h-28 rounded-full bg-gradient-to-tr from-slate-700 to-slate-600 border-4 border-slate-500/30 flex items-center justify-center text-4xl shadow-2xl">
              <User size={54} className="text-slate-200" />
            </div>
            {!isAnswered && (
              <span className="absolute -inset-1 rounded-full border-2 border-cyan-400 animate-ping opacity-60 pointer-events-none" />
            )}
          </div>

          <div className="text-center space-y-1">
            <h1 className="text-2xl font-bold tracking-tight text-white">{callerName}</h1>
            <p className="text-sm font-medium text-slate-400 font-mono">
              {!isAnswered ? 'Incoming Emergency Call…' : formatTime(elapsed)}
            </p>
          </div>

          {/* Quick Caller Switcher (Pre-answer) */}
          {!isAnswered && (
            <div className="flex flex-wrap gap-1.5 justify-center max-w-xs pt-2">
              {['Papa (Home)', 'Inspector Sharma (MP Police)', 'Bhaiya', 'Control Room'].map(name => (
                <button
                  key={name}
                  onClick={() => setCallerName(name)}
                  className={`text-[10px] px-2.5 py-1 rounded-full border transition-all ${
                    callerName === name
                      ? 'bg-cyan-500/20 text-cyan-300 border-cyan-400/50 font-bold'
                      : 'bg-slate-800/60 text-slate-400 border-slate-700'
                  }`}
                >
                  {name}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* In-Call Controls or Ringing Actions */}
      {!screenDimmed && (
        <div className="pb-10 w-full max-w-xs mx-auto space-y-8">
          {isAnswered ? (
            /* Active Call Controls */
            <div className="space-y-6">
              <div className="grid grid-cols-3 gap-4 text-center">
                <button
                  onClick={() => setIsMuted(m => !m)}
                  className={`flex flex-col items-center gap-1 p-3 rounded-full border transition-all ${
                    isMuted ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' : 'bg-slate-800/60 border-slate-700 text-slate-300'
                  }`}
                >
                  {isMuted ? <MicOff size={22} /> : <Mic size={22} />}
                  <span className="text-[10px] font-medium">{isMuted ? 'Muted' : 'Mute'}</span>
                </button>
                <button
                  onClick={() => setIsSpeaker(s => !s)}
                  className={`flex flex-col items-center gap-1 p-3 rounded-full border transition-all ${
                    isSpeaker ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40' : 'bg-slate-800/60 border-slate-700 text-slate-300'
                  }`}
                >
                  <Volume2 size={22} />
                  <span className="text-[10px] font-medium">Speaker</span>
                </button>
                <div className="flex flex-col items-center gap-1 p-3 rounded-full bg-slate-800/60 border border-slate-700 text-slate-300">
                  <Grid size={22} />
                  <span className="text-[10px] font-medium">Keypad</span>
                </div>
              </div>

              <div className="flex justify-center">
                <button
                  onClick={handleDecline}
                  className="w-16 h-16 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-white shadow-lg shadow-rose-900/50 active:scale-95 transition-all"
                >
                  <PhoneOff size={28} />
                </button>
              </div>
            </div>
          ) : (
            /* Incoming Call Swipe / Action Buttons */
            <div className="flex items-center justify-around px-2">
              <div className="flex flex-col items-center gap-2">
                <button
                  onClick={handleDecline}
                  className="w-16 h-16 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-white shadow-xl shadow-rose-950/60 active:scale-95 transition-all"
                >
                  <PhoneOff size={28} />
                </button>
                <span className="text-xs font-semibold text-rose-400">Decline</span>
              </div>

              <div className="flex flex-col items-center gap-2">
                <button
                  onClick={handleAnswer}
                  className="w-16 h-16 rounded-full bg-emerald-500 hover:bg-emerald-400 flex items-center justify-center text-white shadow-xl shadow-emerald-950/60 active:scale-95 transition-all animate-bounce"
                >
                  <Phone size={28} />
                </button>
                <span className="text-xs font-semibold text-emerald-400">Answer</span>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
