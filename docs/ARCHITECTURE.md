# 🏙️ NagarSetu V2.0 — Architecture & 15-Phase Master Implementation

> **Smart City Civic Platform & Municipal Command Grid**  
> Covering Bhopal & Indore · Android (13 Modules) · Wear OS · Next.js 15 Command Center · Spatio-Temporal AI

---

## 🧭 System Overview

NagarSetu V2.0 transforms disconnected municipal signals into an integrated, real-time civic response grid:
1. **Citizens**: Native Android app (Jetpack Compose) + Wear OS companion + 3-Tap Mobile Emergency Web Portal.
2. **Municipal Command Center**: Zero-Scroll Tactical Dashboard (Next.js 15 + Leaflet + WebSockets + CCTV Grid).
3. **Responders**: Automated SMS dispatch & E.164 notifications via Twilio + PostGIS proximity routing.
4. **AI Core**: Scikit-Learn spatio-temporal risk modeling + YOLOv8 visual evidence verifier + Gemini Flash Multimodal assistant.

---

## 🧬 Advanced Tactical Pillars & Design Philosophy

### Tactical Safety & Citizen Protection:
- **Web Live Tracking Sessions (`/track/:token`)**: When an SOS is triggered, emergency contacts receive an interactive web link via SMS, allowing real-time tracking on any mobile browser without installing the native app.
- **Spatio-Temporal Risk Heatmap (Hour × Day × Geo)**: Real Gaussian distance decay modeling crime and hazard probabilities across Bhopal & Indore.
- **Dynamic Hazard Geofencing with TTL**: Municipal coordinators can declare temporary hazard zones (flooding, unlit corridors) with auto-expiring radius multipliers.
- **Triple-Path Navigation**: Fastest vs. Safest (Main Road Bias) vs. Eco GreenRoute.
- **Fake Call Deterrent**: Realistic incoming call simulator in Compose with system ringtone and audio timer to deter harassers in dark streets.

### Tactical Municipal Command Grid:
- **Zero-Scroll CCTV Wall**: High-density 6-feed surveillance grid optimized for mission-critical monitoring without vertical scrolling.
- **Dynamic QR Code Emergency Portal (`/report/qr`)**: 3-tap incident reporting (Category ➔ Micro-Location ➔ Evidence) for rapid citizen reporting without app install.
- **Automated AI Vision Pipeline**: Real-time visual verification of potholes, waterlogging, and fire hazards.
- **Smart Notification Routing**: Proximity-aware responder fanout with SMS and status escalation.

---

## 🗺️ 15-Phase Implementation Status

| Phase | Description | Status | Deliverables |
|:---:|---|:---:|---|
| **Phase 1** | Monorepo Setup & Repo Unification | ✅ Completed | Decoupled structure (`android/`, `frontend/`, `backend/`, `ai_engine/`, `database/`) |
| **Phase 2** | PostGIS & Supabase Spatial Engine | ✅ Completed | Master SQL schema (`schema_master.sql`) with spatial triggers and compatibility views |
| **Phase 3** | Real-Time Event Bus & WebSockets | ✅ Completed | Express API Gateway (`backend/src/index.js`) + Socket.IO + Supabase Realtime forwarders |
| **Phase 4** | Ground-Up Frontend Rebuild | ✅ Completed | Next.js 15 App Router + Tailwind Tactical Dark Theme + shared UI components |
| **Phase 5** | Zero-Scroll Tactical Command Center | ✅ Completed | 3-panel split view (SOS priority queue, CartoDB dark map, incident triage) |
| **Phase 6** | Interactive GIS Map & Spatial Overlays | ✅ Completed | Client-only Leaflet map with animated SOS beacons, scanline effects, flyTo |
| **Phase 7** | Web Live Tracking & Twilio SMS Fanout | ✅ Completed | Public `/track/[token]` portal + Twilio SMS automated link generator |
| **Phase 8** | Dynamic QR Mobile Emergency Portal | ✅ Completed | Public `/report/qr` 3-tap rapid emergency submission flow |
| **Phase 9** | AI Vision Pipeline & YOLO Verifier | ✅ Completed | `ai_engine/vision_service.py` automated visual triage |
| **Phase 10**| Spatio-Temporal Risk Engine | ✅ Completed | `ai_engine/risk_service.py` Gaussian spatial decay + night multipliers |
| **Phase 11**| Multimodal Gemini Flash AI Core | ✅ Completed | `ai_engine/gemini_civic_core.py` Hindi/Hinglish structured reasoning |
| **Phase 12**| Triple-Path Eco & Safe Navigation | ✅ Completed | `/api/routing/calculate-routes` (Fastest, Safest Main-Road, Green Eco) |
| **Phase 13**| Android Compose & Wear OS Upgrades | ✅ Completed | Fake Call deterrent screen + Wear OS haptic vibration and heart-rate SOS |
| **Phase 14**| Security Hardening & Audit Logs | ✅ Completed | Database audit trails + helmet + rate-limiting |
| **Phase 15**| Containerized Deployment & Testing | ✅ Completed | `docker-compose.yml` + individual service Dockerfiles |

---

## 🚀 Running NagarSetu V2 Locally

### 1. Web Command Center
```bash
cd frontend
npm install
npm run dev
# Open http://localhost:3000
```

### 2. API Gateway
```bash
cd backend
npm install
npm run dev
# Running on http://localhost:4000
```

### 3. AI Engine
```bash
cd ai_engine
pip install -r requirements.txt
python risk_service.py --grid
```

### 4. Full Docker Stack
```bash
docker compose up --build
```
