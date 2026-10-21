'use client';

import { useState, useEffect, useRef } from 'react';
import { Volume2, VolumeX, Mic, ArrowLeft, Shield, AlertTriangle, MapPin, Play, Square } from 'lucide-react';
import Link from 'next/link';

const SAFETY_TIPS = [
  {
    en: "Stay on main illuminated arterial roads. Avoid dark underpasses or unlit alleys.",
    hi: "Kripya mukhya roshni wale raste par rahein. Andhere raste ya underpass se bachein."
  },
  {
    en: "Nearest 24x7 Police Assistance Kiosk is 140 meters ahead on MP Nagar Zone-1 Commercial Spine.",
    hi: "Nazdeeki 24 ghante Police Sahayata Kendra 140 meter aage MP Nagar Zone-1 par sthit hai."
  },
  {
    en: "Your GPS breadcrumb trail is being monitored by NagarSetu Municipal Safety Command.",
    hi: "Aapki live GPS location par NagarSetu Command Center dwara nigrani rakhi ja rahi hai."
  },
  {
    en: "If you feel followed, enter the nearest lit commercial store or fuel station immediately.",
    hi: "Yadi koi sandigdh vyakti picha kare, to turant nazdeeki roshan dukaan ya petrol pump par jaayein."
  }
];

export default function VoiceCoachPage() {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTipIndex, setCurrentTipIndex] = useState(0);
  const [language, setLanguage] = useState<'en' | 'hi'>('hi');
  const [speechSupported, setSpeechSupported] = useState(true);

  useEffect(() => {
    if (typeof window !== 'undefined' && !('speechSynthesis' in window)) {
      setSpeechSupported(false);
    }
  }, []);

  const speakCurrentTip = (index = currentTipIndex, lang = language) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();
    const text = SAFETY_TIPS[index][lang];
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = lang === 'hi' ? 'hi-IN' : 'en-US';
    utterance.rate = 0.92;

    utterance.onstart = () => setIsPlaying(true);
    utterance.onend = () => setIsPlaying(false);
    utterance.onerror = () => setIsPlaying(false);

    window.speechSynthesis.speak(utterance);
  };

  const handleStop = () => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsPlaying(false);
  };

  const nextTip = () => {
    const next = (currentTipIndex + 1) % SAFETY_TIPS.length;
    setCurrentTipIndex(next);
    speakCurrentTip(next, language);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col justify-between p-5 select-none font-sans">
      {/* Top Bar */}
      <div className="flex items-center justify-between pt-2">
        <Link
          href="/report/qr"
          className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-full bg-slate-900 border border-slate-800"
        >
          <ArrowLeft size={13} /> Back to QR Portal
        </Link>

        {/* Language Switcher */}
        <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-full border border-slate-800 text-xs">
          <button
            onClick={() => { setLanguage('hi'); speakCurrentTip(currentTipIndex, 'hi'); }}
            className={`px-2.5 py-0.5 rounded-full font-bold transition-all ${
              language === 'hi' ? 'bg-cyan-500 text-slate-950' : 'text-slate-400'
            }`}
          >
            हिंदी
          </button>
          <button
            onClick={() => { setLanguage('en'); speakCurrentTip(currentTipIndex, 'en'); }}
            className={`px-2.5 py-0.5 rounded-full font-bold transition-all ${
              language === 'en' ? 'bg-cyan-500 text-slate-950' : 'text-slate-400'
            }`}
          >
            English
          </button>
        </div>
      </div>

      {/* Main Visualizer & Voice Coach Banner */}
      <div className="flex flex-col items-center justify-center my-auto space-y-6 text-center max-w-sm mx-auto">
        <div className="relative">
          <div className="w-24 h-24 rounded-full bg-cyan-500/10 border-2 border-cyan-400/40 flex items-center justify-center shadow-lg shadow-cyan-950/50">
            {isPlaying ? (
              <Volume2 size={42} className="text-cyan-400 animate-pulse" />
            ) : (
              <Shield size={42} className="text-cyan-400" />
            )}
          </div>
          {isPlaying && (
            <span className="absolute -inset-2 rounded-full border-2 border-cyan-400 animate-ping opacity-40 pointer-events-none" />
          )}
        </div>

        <div>
          <h1 className="text-xl font-bold text-white">NagarSetu AI Voice Coach</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time Tactical Audio Guidance for Pedestrians</p>
        </div>

        {/* Dynamic Tip Card */}
        <div className="w-full bg-slate-900/80 border border-slate-800 p-4 rounded-2xl text-left space-y-2">
          <div className="flex items-center justify-between text-[11px] text-cyan-400 font-mono">
            <span>TIP #{currentTipIndex + 1} OF {SAFETY_TIPS.length}</span>
            <span>{isPlaying ? '● AUDIO ACTIVE' : 'READY'}</span>
          </div>
          <p className="text-sm font-medium text-slate-200 leading-relaxed">
            "{SAFETY_TIPS[currentTipIndex][language]}"
          </p>
        </div>

        {/* Animated Sound Waveform Bars */}
        <div className="flex items-center gap-1.5 h-8">
          {[12, 24, 18, 30, 16, 28, 14, 22].map((h, i) => (
            <div
              key={i}
              className={`w-1 bg-cyan-400 rounded-full transition-all duration-150 ${
                isPlaying ? 'animate-pulse' : 'opacity-30'
              }`}
              style={{ height: isPlaying ? `${h}px` : '6px' }}
            />
          ))}
        </div>
      </div>

      {/* Bottom Controls */}
      <div className="pb-6 max-w-sm mx-auto w-full space-y-3">
        <div className="flex items-center gap-3">
          {isPlaying ? (
            <button
              onClick={handleStop}
              className="flex-1 py-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm flex items-center justify-center gap-2 active:scale-95 transition-all"
            >
              <Square size={16} /> Stop Voice
            </button>
          ) : (
            <button
              onClick={() => speakCurrentTip()}
              className="flex-1 py-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-sm flex items-center justify-center gap-2 active:scale-95 transition-all shadow-lg shadow-cyan-950"
            >
              <Play size={16} /> Speak Current Tip
            </button>
          )}

          <button
            onClick={nextTip}
            className="px-4 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-xs border border-slate-700 active:scale-95 transition-all"
          >
            Next Advice →
          </button>
        </div>

        <Link
          href="/report/qr/fake-call"
          className="w-full py-2.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-cyan-500/40 text-cyan-300 text-xs font-semibold flex items-center justify-center gap-2 transition-all"
        >
          📞 Need a deterrent? Launch Fake Call Screen
        </Link>
      </div>
    </div>
  );
}
