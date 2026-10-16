# NagarSetu V2.0

> **Smart City Civic Intelligence Platform** — Next.js 15 · Tailwind CSS · TypeScript · Socket.IO

---

## 🗂️ Project Structure

```
repo_v2/
├── frontend/           ← Next.js 15 Command Center (this)
│   ├── src/
│   │   ├── app/        ← Next.js App Router (layout, page, globals.css)
│   │   ├── components/
│   │   │   ├── layout/ ← Sidebar, TopBar
│   │   │   ├── command/← SosCard, IncidentCard, TacticalMap
│   │   │   └── pages/  ← CommandPage, IncidentsPage, AnalyticsPage, RespondersPage
│   │   ├── hooks/      ← useStore (data + Socket.IO)
│   │   ├── lib/        ← utils (cn, timeAgo, formatINR…)
│   │   └── types/      ← Shared TypeScript interfaces
│   └── public/         ← manifest.json, icons
├── backend/            ← Node.js / Express / Socket.IO API  (Phase 3)
├── ai_engine/          ← Python FastAPI + Scikit-Learn ML   (Phase 10)
└── docs/               ← Architecture & API docs
```

## 🚀 Quick Start (Frontend)

```bash
cd frontend
npm install
npm run dev
# → http://localhost:3000
```

> **No backend needed for development** — the app ships with mock SOS events, incidents, wards, and responders. Connect `NEXT_PUBLIC_API_URL` to a running backend for live data.

## 🎨 Design System

- **Palette**: Void (`#070714`) · Cyber Cyan (`#00D4FF`) · Siren Red (`#E53935`) · Safe Green (`#10B981`)
- **Font**: Plus Jakarta Sans (UI) · JetBrains Mono (values/code)
- **Layout**: Zero-scroll 3-panel tactical grid (SOS queue | Map | Incident triage)

## 📡 Environment Variables

```env
NEXT_PUBLIC_API_URL=http://localhost:4000   # Backend WebSocket URL
```

## 🗺️ 15-Phase Roadmap

See [docs/PLAN.md](docs/PLAN.md) for the full development plan.