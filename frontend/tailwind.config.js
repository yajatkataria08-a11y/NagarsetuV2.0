/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // === NagarSetu V2 Tactical Palette ===
        // Core Dark Surface
        void:    '#070714',
        abyss:   '#0E0E24',
        surface: '#13132E',
        raised:  '#1A1A3C',
        muted:   '#252548',
        // Civic Blue
        civic:   {
          DEFAULT: '#2D468C',
          light:   '#3D5AA8',
          dark:    '#1E2E5C',
        },
        // Cyber Cyan — primary accent
        cyber:   {
          DEFAULT: '#00D4FF',
          dim:     '#00AACC',
          glow:    'rgba(0,212,255,0.25)',
        },
        // SOS Alerts
        siren:   {
          DEFAULT: '#E53935',
          orange:  '#F97316',
          amber:   '#F59E0B',
        },
        // Status
        safe:    '#10B981',
        caution: '#F59E0B',
        // Text
        ink:     '#E8E8FF',
        mink:    '#8888AA',
        faint:   '#4A4A7A',
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      boxShadow: {
        'glow-cyber':  '0 0 20px rgba(0,212,255,0.30)',
        'glow-siren':  '0 0 20px rgba(229,57,53,0.40)',
        'glow-safe':   '0 0 20px rgba(16,185,129,0.30)',
        'card':        '0 4px 24px rgba(0,0,0,0.50)',
        'card-hover':  '0 8px 40px rgba(0,0,0,0.70)',
      },
      keyframes: {
        'siren-pulse': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(229,57,53,0.50)' },
          '70%':       { boxShadow: '0 0 0 10px rgba(229,57,53,0)' },
        },
        'cyber-pulse': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(0,212,255,0.40)' },
          '70%':       { boxShadow: '0 0 0 8px rgba(0,212,255,0)' },
        },
        'live-dot': {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%':      { opacity: '0.5', transform: 'scale(0.80)' },
        },
        'slide-in-right': {
          from: { opacity: '0', transform: 'translateX(16px)' },
          to:   { opacity: '1', transform: 'translateX(0)' },
        },
        'fade-up': {
          from: { opacity: '0', transform: 'translateY(10px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'scan-line': {
          '0%':   { top: '0%' },
          '100%': { top: '100%' },
        },
      },
      animation: {
        'siren-pulse':      'siren-pulse 1.4s ease-in-out infinite',
        'cyber-pulse':      'cyber-pulse 2s ease-in-out infinite',
        'live-dot':         'live-dot 2s ease-in-out infinite',
        'slide-in-right':   'slide-in-right 0.25s ease both',
        'fade-up':          'fade-up 0.28s ease both',
        'scan-line':        'scan-line 3s linear infinite',
      },
      borderRadius: {
        'xl2': '16px',
      },
    },
  },
  plugins: [],
};
