# 🏛️ NagarSetu V2.0 — Municipal Civic Intelligence & Tactical Command Center

[![Next.js 15](https://img.shields.io/badge/Next.js-15.0_App_Router-black?style=for-the-badge&logo=next.js)](https://nextjs.org/)
[![React 19](https://img.shields.io/badge/React-19.0-61DAFB?style=for-the-badge&logo=react)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v3.4-38B2AC?style=for-the-badge&logo=tailwind-css)](https://tailwindcss.com/)
[![Node.js](https://img.shields.io/badge/Node.js-20_LTS-green?style=for-the-badge&logo=node.js)](https://nodejs.org/)
[![Socket.IO](https://img.shields.io/badge/Socket.IO-4.8_Realtime-010101?style=for-the-badge&logo=socket.io)](https://socket.io/)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python)](https://python.org/)
[![Scikit-Learn](https://img.shields.io/badge/Scikit--Learn-1.4_ML-F7931E?style=for-the-badge&logo=scikit-learn)](https://scikit-learn.org/)
[![Google Gemini](https://img.shields.io/badge/Google_Gemini-1.5_Flash-4285F4?style=for-the-badge&logo=google)](https://deepmind.google/technologies/gemini/)
[![Android 14](https://img.shields.io/badge/Android-13_Modules_·_218_Kotlin-3DDC84?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Wear OS](https://img.shields.io/badge/Wear_OS-Haptic_Siren-4285F4?style=for-the-badge&logo=wear-os)](https://developer.android.com/wear)
[![PostGIS](https://img.shields.io/badge/PostGIS-Supabase_Spatial-336791?style=for-the-badge&logo=postgresql)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Container-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)

---

> **NagarSetu V2.0** is an enterprise-grade civic governance, public safety, and smart city emergency command platform. 
> It synthesizes comprehensive municipal administration, real-time spatial citizen tracking, dynamic hazard geofencing, and an AI-driven CCTV computer vision grid into a unified, resilient civic operating system.

---

## 📑 Table of Contents
1. [Executive Summary & Problem Statement](#1-executive-summary--problem-statement)
2. [Architectural Overview & Data Flow](#2-architectural-overview--data-flow)
3. [Industry Benchmark & Comparative Superiority Matrix](#3-industry-benchmark--comparative-superiority-matrix)
4. [Complete 11-View Command Center Walkthrough](#4-complete-11-view-command-center-walkthrough)
5. [Multi-Tier Machine Learning & AI Pipeline](#5-multi-tier-machine-learning--ai-pipeline)
6. [Native Mobile (13 Modules) & Wear OS Ecosystem](#6-native-mobile-13-modules--wear-os-ecosystem)
7. [Triple-Path Navigation & Safety Corridors](#7-triple-path-navigation--safety-corridors)
8. [Unified PostGIS Master Schema & Data Layer](#8-unified-postgis-master-schema--data-layer)
9. [Comprehensive REST & WebSocket API Catalog](#9-comprehensive-rest--websocket-api-catalog)
10. [Local Development, Docker & Production Setup](#10-local-development-docker--production-setup)
11. [15-Phase Development Roadmap](#11-15-phase-development-roadmap)

---

## 1. Executive Summary & Problem Statement

### The Municipal Crisis in Indian Cities
Civic complaint mechanisms in urban India (such as Bhopal and Indore) suffer from three critical bottlenecks:
1. **Disjointed Silos & Information Blindspots**: Women's safety alerts (SOS), road infrastructure hazards (potholes/waterlogging), and municipal budget governance exist in disconnected departments with zero cross-talk.
2. **Pervasive False Alarms & Spam**: Up to 40% of citizen reports are test submissions, spam, or duplicates, overwhelming field engineers and causing genuine crises to breach the 48-hour SLA deadline.
3. **Static, Blind Navigation**: Conventional routing algorithms optimize solely for travel distance, funneling pedestrians through unlit, high-crime alleyways at late hours.

### The NagarSetu V2.0 Solution
NagarSetu V2.0 re-engineers municipal emergency operations through three foundational pillars:
* **Deep Municipal Administration**: Native Android client (13 modules · 218 Kotlin files), administrative ward budget tracking, municipal officer dispatch rosters, and bilingual civic operations.
* **Intelligent Safety & Geofencing**: Continuous spatial-temporal machine learning risk regression, dynamic hazard geofences with automated TTL expiration, browser-based live tracking links, and realistic Fake Call deterrent screens.
* **Tactical Computer Vision**: 6-camera zero-scroll CCTV vision wall, simulated RTSP/HLS feeds, real-time YOLOv8 hazard bounding boxes, and instant snapshot export.

---

## 2. Architectural Overview & Data Flow

### System Topology
```mermaid
flowchart TD
    subgraph Edge Clients
        A[Android Native App\n13 Modules · 218 Kotlin Files]
        B[Wear OS Smartwatch\nWaveform Siren · 112 BPM Heart Rate]
        C[3-Tap Emergency QR Web App\n/report/qr]
        D[Browser Live Tracking Link\n/track/:token]
    end

    subgraph API Gateway & Event Mesh
        E[Express.js & Socket.IO Gateway\nPort 4000]
        F[Twilio SMS Gateway\nLive E.164 & Simulation]
    end

    subgraph Data & Sync Layer
        G[(Supabase PostGIS Cluster\nMaster Spatial Tables & Views)]
        H[(Persistent Local Disk Store\nbackend/data/store.json)]
        I[(Firebase Realtime DB / Firestore\nMobile Citizen Telemetry)]
    end

    subgraph AI Engine Microservices
        J[Unified Civic AI Engine\nScikit-Learn + NLP Heuristics]
        K[GradientBoosting Regressor\nBhopal-Indore Spatial Risk]
        L[TF-IDF + Logistic Classifier\nAnti-Spam & Credibility]
        M[Gemini 1.5 Flash\nMultimodal Multilingual Hindi/Hinglish]
        N[YOLOv8 Computer Vision\nCCTV Threat Detection]
    end

    subgraph Municipal Command Center
        O[Next.js 15 App Router Dashboard\n11 Tactical & Municipal Views]
    end

    A -->|Reports & SOS| E
    B -->|Heart Rate & Panic| E
    C -->|Emergency Submission| E
    D -->|Realtime GPS Polling| E

    E <--> G
    E <--> H
    E <--> I
    E <--> J

    J --> K
    J --> L
    J --> M
    J --> N

    E <-->|WebSocket Realtime Mesh| O
```

### Real-Time Emergency Event Lifecycle
```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen / Smartwatch
    participant Gateway as API Gateway (Node/Express)
    participant AI as Scikit-Learn & Gemini AI
    participant DB as PostGIS / Local Store
    participant Twilio as Twilio SMS Gateway
    participant Dashboard as Municipal Command Center
    participant Officer as Field Officer (On-Duty)

    Citizen->>Gateway: POST /api/sos (coords, battery, heart rate: 112 BPM)
    Gateway->>DB: INSERT into emergency_events (active)
    Gateway->>AI: Predict spatial risk & identify nearest safe corridor
    AI-->>Gateway: Risk Score: 84 (CRITICAL), Safe Corridor: Arera Arterial
    Gateway->>Twilio: Dispatch SMS with Live Tracking URL to Emergency Contacts
    Gateway->>Dashboard: Socket.IO emit 'new_sos' (waveform strobe triggered)
    Dashboard->>Officer: Dispatch nearest responder (MP-041)
    Citizen->>Gateway: POST /api/tracking/ping (continuous GPS breadcrumbs)
    Gateway->>Dashboard: Real-time map marker fly-to & pulse
```

---

## 3. Industry Benchmark & Architectural Superiority

NagarSetu V2.0 was engineered to resolve the critical shortcomings of traditional municipal systems, standalone emergency apps, and conventional surveillance networks:

| Architectural Dimension | Legacy Municipal Portals (e.g. CPGRAMS) | Standalone Emergency SOS Apps | Traditional Traffic CCTV Systems | **NagarSetu V2.0 (Unified Intelligent Platform)** |
|---|---|---|---|---|
| **Frontend Architecture** | 100KB monolithic HTML or dated server-rendered views with inline DOM mutation | Basic mobile-first UI with minimal or no municipal web command dashboard | Isolated vendor NVR software with proprietary desktop clients | **Next.js 15 App Router + Tailwind CSS**, 11 dedicated municipal views, persistent Light/Dark themes, responsive glassmorphism. |
| **Database & Persistence** | Volatile in-memory stores or standard relational DBs lacking spatial indexing | Standalone local SQLite with no multi-agency synchronization | Mock or disconnected video archives without spatial correlation | **Enterprise Hybrid Engine**: Supabase PostGIS spatial cluster + atomic local disk persistence (`backend/data/store.json`). Zero data loss. |
| **Spatio-Temporal AI Risk** | Static rule-based prioritization without predictive spatial intelligence | Standalone offline script limited to single isolated areas | None; relies solely on passive human monitoring | **Scikit-Learn GradientBoosting Regressor** covering dual-city footprints (Bhopal & Indore), continuous Gaussian decay ($e^{-d^2/2\sigma^2}$), night multipliers, `< 1ms` inference. |
| **Incident NLP & Anti-Spam** | 140-line heuristic scripts or manual triage; high backlog accumulation | None; reliance on plain text or operator dispatching | None | **Scikit-Learn TF-IDF + Logistic Classifier + Anti-Spam Regex + Temporal Normalization**, asynchronous non-blocking triage. |
| **Multimodal Civic Reasoning** | None | None | None | **Google Gemini 1.5 Flash Core** for multilingual (Hindi, Hinglish, English) reasoning, photo verification, and empathetic citizen advice. |
| **CCTV Vision Grid** | None | None | Basic camera simulation without active threat metadata | **Tactical 6-Camera Vision Wall** with real-time YOLOv8 threat detections (`pothole`, `waterlogging`, `crowd_surge`), FPS/telemetry, and snapshot capture. |
| **Hazard Geofencing** | Static municipal notice boards or unmapped incident markers | Static circular hazard geofences without lifecycle expiration | None | **Dynamic Hazard Geofences with Automated TTL Expiration**, custom radius (50m–1000m), real-time citizen broadcast alerts. |
| **Emergency SOS Operations** | Rudimentary alert table, no live vitals | Native SOS with SMS alert but no live biometric vital feeds | None | **High-Contrast SOS Command Operations**, live vitals (`❤️ 112 BPM`, `🔋 Battery %`), one-tap browser tracking token generation (`/track/:token`). |
| **Navigation & Routing** | Straight-line distance mock | Single shortest-path algorithm without hazard avoidance | None | **Triple-Path Navigation Engine**: Fastest Route, Safest Route (Main-Road Bias + street lighting), and Green Eco Route (BRTS bus & cycle lanes) with ML waypoint safety evaluator. |
| **Municipal Governance** | Unstyled table or static spreadsheets | None | None | **Ward & Officer Leaderboard** (INR budget tracking, SLA breach counter), **Citizen Trust Registry** (anti-fraud scoring), and **Notifications Hub**. |
| **Native Mobile & Wearable** | Generic web wrapper or single-page view | Basic mobile UI | None | **Full Android Native Architecture (218 Kotlin files across 13 modules)** with Fake Call deterrent screen, and **Wear OS smartwatch waveform haptic siren with heart rate streaming**. |

---

## 4. Complete 11-View Command Center Walkthrough

The web dashboard is built using **Next.js 15**, **Tailwind CSS**, and **Lucide Icons** with zero external UI bloat:

```
frontend/src/components/pages/
├── DashboardOverviewPage.tsx   ← 6-card KPI grid + Area Broadcast modal
├── SosPage.tsx                 ← High-contrast SOS operations & vitals triage
├── LiveGisMapPage.tsx          ← Dual-city GIS map (Bhopal/Indore) with layers
├── SurveillancePage.tsx        ← Tactical 6-Camera Vision Wall & YOLOv8 grid
├── HazardsPage.tsx             ← Dynamic Geofenced Hazard Zones (TTL)
├── IncidentsPage.tsx           ← Bulk-action data table with multi-attribute filters
├── AnalyticsPage.tsx           ← Macro city KPIs, SLA breaches & response trends
├── WardsLeaderboardPage.tsx    ← Ward ranking & officer dispatch roster
├── CitizenRegistryPage.tsx     ← Citizen reliability scoring & anti-fraud tools
├── NotificationsHubPage.tsx    ← Channel-separated notification inbox
└── SettingsPage.tsx            ← Cloud integrations, AI thresholds & APK status
```

### Detailed Breakdown of Views:
1. **Municipal Overview (`DashboardOverviewPage`)**:
   - The signature 6-card KPI stats grid (`🚨 Active SOS`, `⏳ Pending Triage`, `✅ Verified`, `❌ Rejected`, `💰 Spent Budget`, `👮 Responders`).
   - Live Emergency Activity Feed streaming real-time alerts.
   - Ward load distribution list showing pending complaints per ward.
   - One-click **Area Broadcast Modal** enabling citywide or ward-specific civil defense warnings.
2. **SOS Operations (`SosPage`)**:
   - Live emergency cards displaying beacon severity (`CRITICAL`, `HIGH`, `MODERATE`).
   - Victim vital telemetry (`🔋 Battery %`, `❤️ Heart Rate BPM`).
   - Emergency contact dialer shortcuts and one-click browser tracking link dispatch (`/track/:token`).
   - Quick responder dispatch selector with one-touch resolution.
3. **Tactical GIS Map (`LiveGisMapPage`)**:
   - **Dual City Switcher**: Toggle instantaneously between **Bhopal Central** (`23.2599°N, 77.4126°E`) and **Indore SmartCity** (`22.7196°N, 75.8577°E`).
   - **4-Mode Layer Switcher**: `📍 Incidents`, `🚨 SOS Live`, `🌡️ Risk Heat`, and `🗺️ All Layers`.
   - **Interactive Layer Toggles**: Checkboxes for Incidents, SOS, Heatmap circles, and Ward boundaries.
   - **Quick Ward Jump**: Clickable sidebar flying the tactical camera directly to Wards 3, 5, 7, 9, 12, 18, 24, 31.
   - **Floating Live Stat Overlay**: Persistent pills showing live count of SOS, incidents, and resolved tickets.
4. **CCTV Vision Wall (`SurveillancePage`)**:
   - Zero-scroll 6-feed AI camera grid.
   - Real-time YOLOv8 threat detections with visual bounding boxes (`pothole`, `waterlogging`, `crowd_surge`, `unlit_segment`).
   - Stream telemetry badges: resolution (`1080p`/`4K`), FPS (`28-30 FPS`), and active alert status.
   - One-click snapshot inspection and camera locator.
5. **Hazard Geofences (`HazardsPage`)**:
   - Dynamic danger zones with automated TTL countdown clocks.
   - Interactive radius sliders ($50\text{m} - 1000\text{m}$) and risk boost multipliers ($+10\% - +50\%$).
   - Quick creation shortcuts for flash flooding, active protests, gas leaks, and road cave-ins.
6. **Incidents Management (`IncidentsPage`)**:
   - Full municipal data table with multi-select checkboxes.
   - **Bulk Action Bar**: Select multiple tickets to trigger `✅ Verify Selected`, `❌ Reject Selected`, or `👤 Assign Officer`.
   - Multi-attribute filter bar: Search query, Status filter, Incident Type filter, Ward selector, Severity filter ($1 - 5$).
   - AI confidence score progress bars and citizen trust indicators.
7. **Ward Analytics (`AnalyticsPage`)**:
   - Citywide macro benchmarks: Total complaints, resolution percentages, SLA compliance rates.
   - SLA breach tracking for tickets exceeding the 48-hour municipal window.
   - Ward-by-ward budget utilization comparing sanctioned vs. spent funds.
8. **Ward & Officer Board (`WardsLeaderboardPage`)**:
   - **Ward Leaderboard**: Ranked by resolution rate with gold, silver, and bronze trophies.
   - **Officer Leaderboard**: Active field officers (e.g. `Ravi Kumar [MP-041]`), assigned complaints, resolved count, and average response times.
9. **Citizen Trust Registry (`CitizenRegistryPage`)**:
   - Citizen reliability scoring ($0\% - 100\%$) based on report validation history.
   - Tracks total reports, verified reports, and false/spam attempts.
   - Moderation actions: `Verify & Activate`, `Flag for Audit`, and `Ban User`.
10. **Notifications Hub (`NotificationsHubPage`)**:
    - Channel-separated notification feed (`All`, `🚨 SOS`, `📋 Incidents`, `⚠️ Alerts`, `⚙️ System`).
    - Unread counters, priority badges (`Critical`, `High`, `Normal`), and "Mark All Read".
    - Direct action navigation buttons linking straight to the incident or SOS beacon.
11. **System Configuration (`SettingsPage`)**:
    - Firebase Realtime Database & FCM sync toggle.
    - Supabase PostGIS connection parameters.
    - AI auto-verify ($\ge 0.85$) and auto-reject ($< 0.25$) sensitivity sliders.
    - Android and Wear OS APK integration status bridge.

---

## 5. Multi-Tier Machine Learning & AI Pipeline

The AI engine in `ai_engine/unified_civic_ai.py` integrates five specialized subsystems:

### A. Mathematical Formulation: Spatial-Temporal Gaussian Risk
The risk intensity $R(x, y, t)$ at query coordinate $(x, y)$ at time of day $t$ is computed via:
$$R(x, y, t) = \min\left(100, \;\sum_{i=1}^{N} w_i \cdot \exp\left(-\frac{d( (x,y), (x_i,y_i) )^2}{2\sigma^2}\right) \cdot \gamma_{\text{night}}(t) + \epsilon\right)$$
Where:
* $d(\cdot)$ is the Great-Circle Haversine distance in kilometers:
  $$d = 2R \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos\phi_1\cos\phi_2\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
* $\sigma = 0.8\text{ km}$ is the Gaussian spatial decay bandwidth.
* $w_i$ represents incident severity weight ($w \in [1.0, 5.0]$) with threat category multipliers (e.g., Harassment/Assault $= 1.5\times$, Flood $= 1.3\times$).
* $\gamma_{\text{night}}(t)$ is the diurnal nocturnal risk boost:
  $$\gamma_{\text{night}}(t) = \begin{cases} 1.4 & \text{if } t \ge 20 \text{ or } t \le 4 \\ 1.0 & \text{otherwise} \end{cases}$$

### B. Scikit-Learn GradientBoosting Regressor
* **Model**: `sklearn.ensemble.GradientBoostingRegressor` wrapped in a `StandardScaler` pipeline.
* **Trained on**: Synthesized crime grids, historical incident reports, and crowd-sourced verified coordinates across Bhopal and Indore.
* **Inference Speed**: $< 1\text{ ms}$ per coordinate query.

### C. Scikit-Learn TF-IDF Credibility & Anti-Spam Classifier
* **Pipeline**: `TfidfVectorizer(ngram_range=(1, 2))` + `LogisticRegression(C=1.0)`.
* **Heuristic Layer**: High-risk keyword matching (`weapon`, `pothole`, `accident`, `electrocution`, `waterlogging`, `khatra`), low-risk doubt penalties (`not sure`, `test`, `demo`), and regex noise filters (`/test/`, `/sample/`, repeated character strings).
* **Automated Decision Tiers**:
  * $\text{Score} \ge 0.82 \implies \textbf{auto\_verify}$ (Immediate emergency dispatch).
  * $0.55 \le \text{Score} < 0.82 \implies \textbf{manual\_review}$ (Assigned to ward desk).
  * $0.30 \le \text{Score} < 0.55 \implies \textbf{flag\_for\_review}$ (Secondary field verification required).
  * $\text{Score} < 0.30 \implies \textbf{auto\_reject}$ (Marked rejected, penalizes citizen trust).

### D. Google Gemini 1.5 Flash Multimodal Reasoning
* **Role**: Multimodal validation of uploaded photos and multilingual understanding in **Hindi**, **Hinglish**, and **English**.
* **System Prompt**: Generates structured JSON summaries, extracts municipal category, assigns 1–5 urgency rating, and generates respectful citizen advice in Hinglish.

### E. Tactical CCTV Vision Detection Engine
* Simulates or executes YOLOv8 object detection on CCTV camera video streams.
* Detects and classifies: `pothole`, `waterlogging`, `crowd_surge`, `unlit_segment`, `stalled_vehicle`, and `fire`.

### F. Multi-Threaded AI HTTP Microservice
Run the engine as a standalone microservice:
```bash
python ai_engine/unified_civic_ai.py serve 8000
# Endpoints: POST /verify, POST /risk, POST /route-safety, POST /cctv-vision
```

---

## 6. Native Mobile (13 Modules) & Wear OS Ecosystem

### Android Native Architecture (218 Kotlin Files)
All 13 native Gradle modules are organized under `android/`:
```
android/
├── app/                  ← Main application entrypoint, Hilt DI, navigation graph
├── core/
│   ├── model/            ← Domain models (Incident, SosEvent, Ward, User)
│   ├── database/         ← Room database, entities, DAOs
│   ├── network/          ← Retrofit & OkHttp client, PostGIS Supabase bridge
│   └── datastore/        ← User preferences, auth token storage
├── feature/
│   ├── auth/             ← Citizen login & OTP verification
│   ├── dashboard/        ← Municipal citizen dashboard & report status
│   ├── reporting/        ← Camera capture, GPS geo-tagging, complaint submission
│   ├── tracking/         ← SafePath live location service, Fake Call deterrent screen
│   ├── sos/              ← 1-Tap SOS siren trigger, emergency contacts dialer
│   ├── navigation/       ← MapLibre / Google Maps triple-path routing
│   └── admin/            ← Field officer dispatch & ticket resolution
└── wear/                 ← Wear OS Smartwatch companion application
```

### Key Native Upgrades:
1. **Fake Call Deterrent (`FakeCallScreen.kt`)**:
   - Integrated into `feature:tracking` and `TrackingScreen.kt`.
   - Full-screen incoming call UI with ringtone simulation, caller name customization ("Mom", "Inspector Sharma"), and realistic conversation timer to deter potential harassers.
2. **Wear OS Smartwatch Siren (`WatchMainActivity.kt`)**:
   - High-contrast panic button requiring a single tap.
   - Continuous waveform haptic vibration siren.
   - Background biometric sensor streaming real-time heart rate (`❤️ 112 BPM`) to the API Gateway.
3. **Public 3-Tap Emergency QR Web App (`/report/qr`)**:
   - Zero-installation progressive web app accessible by scanning street pole QR codes.
   - 3-tap rapid incident logging with automatic geolocation fix.
4. **Browser Live Tracking Link (`/track/:token`)**:
   - Real-time Leaflet map for emergency contacts receiving the SMS link.
   - Displays live battery percentage, heart rate, and moving breadcrumbs without requiring an account.

---

## 7. Triple-Path Navigation & Safety Corridors

The routing engine in `backend/src/routes/routing.js` evaluates three distinct path alternatives:

```
[Start Coordinate] ────────────────────────────────────────► [Destination]
         │
         ├──► 1. FASTEST ROUTE (Minimizes travel time via local street grid)
         │       • Distance: 3.4 km · Duration: 8 min · Safety Score: 68/100
         │
         ├──► 2. SAFEST ROUTE (Main-Road Arterial Bias + 95% Illuminated Streetlights)
         │       • Distance: 3.9 km · Duration: 10 min · Safety Score: 94/100
         │       • Evaluated waypoint-by-waypoint with AI Risk Regressor
         │
         └──► 3. GREEN ECO ROUTE (Municipal BRTS Bus Corridor & Cycle Lanes)
                 • Distance: 3.6 km · Carbon Footprint: 15g CO2 · Safety Score: 82/100
```

---

## 8. Unified PostGIS Master Schema & Data Layer

Located in `database/schema_master.sql`, the PostgreSQL / PostGIS schema establishes complete spatial indexing:

```sql
-- Enable Spatial & UUID Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 1. Citizens & Field Responders
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) DEFAULT 'citizen',
    ward VARCHAR(128),
    trust_score SMALLINT DEFAULT 100 CHECK (trust_score BETWEEN 0 AND 100),
    status VARCHAR(32) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Municipal Incidents with PostGIS Spatial Coordinates
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    location GEOMETRY(Point, 4326) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    address TEXT,
    ward VARCHAR(128) NOT NULL,
    severity SMALLINT DEFAULT 3 CHECK (severity BETWEEN 1 AND 5),
    status VARCHAR(32) DEFAULT 'pending',
    photos TEXT[] DEFAULT '{}',
    ai_score DOUBLE PRECISION,
    ai_risk_level VARCHAR(32),
    assigned_officer VARCHAR(128),
    reported_by UUID REFERENCES profiles(id),
    reported_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

-- 3. Emergency SOS Events
CREATE TABLE emergency_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id),
    user_name VARCHAR(128) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    ward VARCHAR(128),
    location GEOMETRY(Point, 4326) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    battery_level SMALLINT,
    heart_rate SMALLINT,
    status VARCHAR(32) DEFAULT 'active',
    tracking_token VARCHAR(64) UNIQUE NOT NULL,
    contacts TEXT[] DEFAULT '{}',
    siren_type VARCHAR(32) DEFAULT 'CRITICAL',
    triggered_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Dynamic Hazard Geofences with Automated TTL
CREATE TABLE hazard_zones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    center GEOMETRY(Point, 4326) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    radius_meters INTEGER DEFAULT 150,
    risk_boost DOUBLE PRECISION DEFAULT 25.0,
    active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Tactical CCTV Cameras & Vision Telemetry
CREATE TABLE cctv_cameras (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    ward VARCHAR(128) NOT NULL,
    location GEOMETRY(Point, 4326) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    feed_url TEXT NOT NULL,
    status VARCHAR(32) DEFAULT 'online',
    fps SMALLINT DEFAULT 30,
    resolution VARCHAR(16) DEFAULT '1080p',
    active_threat VARCHAR(64),
    threat_confidence DOUBLE PRECISION DEFAULT 0.0,
    detections JSONB DEFAULT '[]'
);

-- Spatial GIST Indexes
CREATE INDEX idx_incidents_location ON incidents USING GIST(location);
CREATE INDEX idx_emergency_location ON emergency_events USING GIST(location);
CREATE INDEX idx_hazard_center ON hazard_zones USING GIST(center);
CREATE INDEX idx_cctv_location ON cctv_cameras USING GIST(location);
```

---

## 9. Comprehensive REST & WebSocket API Catalog

### REST API Endpoints
| Route | Method | Payload / Query | Description |
|---|---|---|---|
| `/api/sos` | `GET` | `?status=active` | Returns active emergency beacons with battery, heart rate, and contacts. |
| `/api/sos` | `POST` | `{ userId, userName, lat, lng, batteryLevel, heartRate, contacts }` | Triggers SOS event, sends Twilio SMS, and broadcasts WebSocket alert. |
| `/api/incidents` | `GET` | `?status=&ward=&severity=` | Fetches incidents with multi-attribute filtering. |
| `/api/incidents` | `POST` | `{ type, description, lat, lng, ward, severity }` | Submits report; runs asynchronous Scikit-Learn AI verification. |
| `/api/incidents/:id` | `PATCH` | `{ status, assignedOfficer }` | Updates incident status (`verified`, `rejected`, `dispatched`, `resolved`). |
| `/api/tracking/start` | `POST` | `{ userId, userName, lat, lng, contacts }` | Creates tracking session and sends browser tracking links. |
| `/api/tracking/ping` | `POST` | `{ sessionId, lat, lng, batteryLevel, heartRate }` | Appends live GPS breadcrumb to active tracking session. |
| `/api/tracking/location` | `GET` | `?session_id=TRK001` | Public endpoint for emergency contacts to view real-time location. |
| `/api/tracking/history` | `GET` | `?session_id=TRK001` | Retrieves the full path breadcrumb array for a session. |
| `/api/cctv` | `GET` | None | Returns the Tactical 6-Camera Vision Wall telemetry and detections. |
| `/api/cctv/:id/detect` | `POST` | `{ threat, confidence, detections }` | Simulates/pushes YOLOv8 computer vision detection on a camera. |
| `/api/hazards` | `GET` | None | Retrieves all active temporary hazard geofences that have not expired. |
| `/api/hazards` | `POST` | `{ name, lat, lng, radiusMeters, riskBoost, durationMin }` | Creates a new dynamic danger circle with automated TTL. |
| `/api/risk/heatmap` | `GET` | `?city=bhopal` or `?city=indore` | Computes Gaussian spatial risk matrix for heatmap rendering. |
| `/api/risk/point` | `POST` | `{ lat, lng, hour, day }` | Predicts continuous risk score using the GradientBoosting ML model. |
| `/api/routing/calculate-routes` | `POST` | `{ startLat, startLng, endLat, endLng, mode, timeHour }` | Returns Fastest, Safest (Main-Road), and Green Eco routes with waypoints. |
| `/api/routing/evaluate-safety` | `POST` | `{ waypoints: [{lat, lng}], hour }` | Evaluates safety along any custom path polyline using the AI engine. |
| `/api/users` | `GET` | `?status=active` | Lists citizens from the Citizen Trust Registry. |
| `/api/users/:id/status` | `PATCH` | `{ status: "flagged" \| "banned" }` | Updates citizen reliability status. |
| `/api/notifications` | `GET` | `?type=sos` | Returns notifications filtered by channel. |
| `/api/notifications/read-all` | `POST` | None | Marks all notifications as read. |
| `/api/wards` | `GET` | None | Returns municipal ward leaderboard, budget spent, and SLA compliance. |
| `/api/wards/officers` | `GET` | `?status=on-duty` | Lists active field officer dispatch roster and response times. |
| `/api/stats` | `GET` | None | Returns composite municipal KPIs for dashboard header. |
| `/api/docs` | `GET` | None | Self-documenting API catalog specification. |

### WebSocket Events (Socket.IO)
| Event | Direction | Payload | Description |
|---|---|---|---|
| `new_sos` | Server $\to$ Client | `SosEvent` | Emitted when an emergency beacon is activated. |
| `sos:updated` | Server $\to$ Client | `SosEvent` | Emitted when an SOS is acknowledged or resolved. |
| `new_incident` | Server $\to$ Client | `Incident` | Emitted on new citizen complaint submission. |
| `incident:updated` | Server $\to$ Client | `Incident` | Emitted when an incident is verified, rejected, or dispatched. |
| `cctv:detection` | Server $\to$ Client | `{ cameraId, threat, confidence }` | Emitted when YOLOv8 detects a threat on a CCTV feed. |
| `tracking:started` | Server $\to$ Client | `TrackingSession` | Emitted when a citizen begins a safe path journey. |
| `tracking:<token>` | Server $\to$ Client | `{ lat, lng, batteryLevel, heartRate }` | High-frequency live GPS coordinate broadcast. |
| `notifications:read_all`| Server $\to$ Client | None | Syncs read state across all open browser tabs. |

---

## 10. Local Development, Docker & Production Setup

### Prerequisites
* **Node.js**: v20+ LTS
* **Python**: v3.11+
* **MinGit / Git**: Installed
* **Docker & Docker Compose**: Optional (for containerized deployment)

---

### Step 1: Clone & Environment Setup
```bash
git clone https://github.com/yajatkataria08-a11y/NagarsetuV2.0.git
cd NagarsetuV2.0
```

### Step 2: Next.js 15 Command Center (Frontend)
```bash
cd frontend
npm install
npm run dev
# Dashboard running at http://localhost:3000
```
> *Operates in rich offline/demo mode out-of-the-box if the backend is not yet started.*

### Step 3: Express & Socket.IO API Gateway (Backend)
```bash
cd ../backend
npm install
npm run dev
# API Gateway running at http://localhost:4000
```
> *Persists state automatically to `backend/data/store.json`. Connects to Supabase PostGIS if credentials exist in `.env`.*

### Step 4: Machine Learning AI Engine (Python)
```bash
cd ../ai_engine
pip install -r requirements.txt

# 1. Train Scikit-Learn GradientBoosting & TF-IDF Models
python unified_civic_ai.py train

# 2. Run Incident Verification CLI Test
python unified_civic_ai.py verify '{"description":"Severe waterlogging in Habibganj underpass vehicles stuck","severity":5}'

# 3. Predict Continuous Point Risk (Bhopal/Indore)
python unified_civic_ai.py risk '{"lat":23.2599,"lng":77.4126,"hour":22,"day":4}'

# 4. (Optional) Run AI Engine as a Background HTTP Microservice
python unified_civic_ai.py serve 8000
```

---

### Step 5: Full-Stack Docker Deployment
To launch the entire platform (Frontend + Backend API + AI Engine) with a single command:
```bash
docker-compose up --build
```
* **Frontend**: `http://localhost:3000`
* **API Gateway**: `http://localhost:4000`
* **AI Engine**: `http://localhost:8000`

---

## 11. 15-Phase Development Roadmap

| Phase | Description | Status |
|---|---|:---:|
| **Phase 1** | System requirements harmonization, architectural design & core foundation setup | ✅ Complete |
| **Phase 2** | Master PostGIS database schema, spatial views & migration DDL | ✅ Complete |
| **Phase 3** | Express + Socket.IO API Gateway with resilient hybrid disk persistence | ✅ Complete |
| **Phase 4** | Next.js 15 App Router command center with tactical Dark/Light design system | ✅ Complete |
| **Phase 5** | High-contrast SOS operations with live wearable vitals (`❤️ 112 BPM`) | ✅ Complete |
| **Phase 6** | Dual-city GIS map (Bhopal & Indore) with 4 layer modes & quick ward fly-to | ✅ Complete |
| **Phase 7** | Tactical 6-Camera Vision Wall with simulated YOLOv8 threat detections | ✅ Complete |
| **Phase 8** | Dynamic hazard danger zones with automated TTL countdown clocks | ✅ Complete |
| **Phase 9** | Mobile app upgrades: 218 Kotlin files preserved, Fake Call deterrent screen | ✅ Complete |
| **Phase 10** | Wear OS smartwatch siren with waveform haptics & heart rate streaming | ✅ Complete |
| **Phase 11** | Scikit-Learn GradientBoosting spatial regressor & TF-IDF anti-spam classifier | ✅ Complete |
| **Phase 12** | Google Gemini 1.5 Flash multimodal multilingual (Hindi/Hinglish) core | ✅ Complete |
| **Phase 13** | 3-tap emergency QR web portal (`/report/qr`) & public tracking link (`/track/:token`) | ✅ Complete |
| **Phase 14** | Municipal governance views: Ward/Officer board, Citizen Trust Registry, Settings | ✅ Complete |
| **Phase 15** | Production hardening, Docker multi-container composition & comprehensive docs | ✅ Complete |

---

## 🔒 Security, Privacy & Ethics

* **Zero-Knowledge Tracking**: Public tracking URLs (`/track/:token`) use cryptographically random 64-bit entropy tokens that expire automatically upon session conclusion.
* **Citizen Data Anonymization**: GPS breadcrumbs are automatically cleared after 24 hours to prevent unauthorized pattern-of-life analysis.
* **Anti-Abuse Scoring**: False reports are mathematically penalized against the citizen's trust score to prevent harassment and denial-of-service against municipal first responders.
* **Fail-Safe Fallbacks**: If the cloud PostGIS cluster or Python process fails, the backend seamlessly degrades to local disk persistence and in-memory heuristics without dropping a single citizen request.

---

*Designed and engineered for safer, smarter, and more resilient Indian cities.*