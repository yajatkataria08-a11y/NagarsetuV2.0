# 📘 NagarSetu V2.0 — Complete Project Guide & Engineering Manual

> **Document Version**: 2.0.0  
> **Target Audience**: Municipal Command Dispatchers, Evaluators, Hackathon Judges, Full-Stack Engineers, and DevOps Operators.  
> **Classification**: Comprehensive System Handbook & Operational Runbook

---

## 📑 Table of Contents
1. [Platform Overview & Architectural Blueprint](#1-platform-overview--architectural-blueprint)
2. [Monorepo Codebase & Directory Map](#2-monorepo-codebase--directory-map)
3. [Prerequisites & System Requirements](#3-prerequisites--system-requirements)
4. [Step-by-Step Installation & Bootstrapping](#4-step-by-step-installation--bootstrapping)
5. [Environment Variables & Configuration Matrix](#5-environment-variables--configuration-matrix)
6. [Command Center User Manual (12 Views)](#6-command-center-user-manual-12-views)
7. [Progressive Web App (PWA) Emergency Suite](#7-progressive-web-app-pwa-emergency-suite)
8. [Native Android & Wear OS Deployment Guide](#8-native-android--wear-os-deployment-guide)
9. [AI Engine & Machine Learning Subsystems](#9-ai-engine--machine-learning-subsystems)
10. [IoT Sensor Ingestion & Escalation Matrix](#10-iot-sensor-ingestion--escalation-matrix)
11. [Contractor Accountability Public Ledger](#11-contractor-accountability-public-ledger)
12. [DriveLegal: BIMSTEC Penal Code System](#12-drivelegal-bimstec-penal-code-system)
13. [Developer API Reference with cURL Examples](#13-developer-api-reference-with-curl-examples)
14. [Testing Suite & City-Grid Simulation](#14-testing-suite--city-grid-simulation)
15. [Docker Compose & Production Edge Deployment](#15-docker-compose--production-edge-deployment)
16. [Troubleshooting & Frequently Asked Questions](#16-troubleshooting--frequently-asked-questions)
17. [Hackathon 5-Minute Live Demonstration Script](#17-hackathon-5-minute-live-demonstration-script)

---

## 1. Platform Overview & Architectural Blueprint

NagarSetu V2.0 is a next-generation municipal command, public safety, and civic intelligence operating system designed for metropolitan urban corporations. It integrates:
* **Tactical Command Center**: Next.js 15 App Router interface featuring 12 mission-critical views.
* **API Gateway & Event Mesh**: Node.js 20 LTS (Express.js) + Socket.IO 4.8 real-time event streaming.
* **AI Intelligence Microservice**: FastAPI service running dual-city GradientBoosting spatial risk regression ($R^2 = 0.9652$), TF-IDF anti-spam NLP classification, OpenCV/YOLOv8 computer vision, and Google Gemini 1.5 Flash.
* **Dual-Tier Spatial Data Layer**: Cloud PostgreSQL 16 + PostGIS cluster with an atomic local JSON disk store fallback (`store.json`) with automated write locks.
* **Hardware & Edge Ingestion**: IoT sliding-window environmental sensor aggregator and 6-feed CCTV streaming grid.
* **Citizen Mobile Ecosystem**: 13-module native Android application (218 Kotlin files), standalone Wear OS smartwatch companion (`DirectSosDispatcher.kt`), and zero-install PWA suite.

---

## 2. Monorepo Codebase & Directory Map

```
NagarSetuV2.0/
├── ai_engine/                         # Python AI Microservice (FastAPI :8000)
│   ├── data/                          # Training data (master_crime_risk.csv, indore_crime_data.csv)
│   ├── models/                        # Pre-trained models (GradientBoosting, TF-IDF, YOLOv8 .pt stubs)
│   ├── gemini_civic_core.py           # Google Gemini 1.5 Flash multimodal multilingual client
│   ├── server.py                      # FastAPI async HTTP server exposing /verify, /risk, /vision
│   ├── train_dual_city_model.py       # Dual-city training script (Bhopal + Indore)
│   ├── unified_civic_ai.py            # Unified CLI & scikit-learn training pipeline
│   ├── vision_service.py              # OpenCV frame processor (HSV fire, Sobel pothole, waterlogging)
│   ├── requirements.txt               # Python package dependencies
│   └── Dockerfile                     # AI microservice container build
│
├── android/                           # Native Android 14 Architecture (218 Kotlin Files)
│   ├── app/                           # Root application entrypoint, Hilt DI, NavGraph
│   ├── core/                          # Core modules (model, database, network, datastore)
│   ├── feature/                       # Feature modules (auth, dashboard, reporting, tracking, sos, admin)
│   └── wear/                          # Wear OS companion app with DirectSosDispatcher.kt
│
├── backend/                           # Express.js & Socket.IO API Gateway (:4000)
│   ├── data/                          # Persistent storage & legal knowledge base
│   │   ├── bimstec_traffic_code.json  # 154 legal violations across 7 South Asian nations
│   │   └── store.json                 # Atomic JSON disk store fallback
│   ├── public/streams/                # CCTV video feeds (cam-01.mp4 through cam-06.mp4)
│   ├── src/
│   │   ├── config/index.js            # 12-factor Zod configuration & dynamic Python resolver
│   │   ├── middleware/auth.js         # JWT RBAC (SUPER_ADMIN, WARD_OFFICER, DISPATCHER, FIELD_ENGINEER)
│   │   ├── routes/                    # 15 REST route controllers (sos, incidents, hazards, risk, cctv...)
│   │   ├── services/                  # Business logic (aiVerifier, localRouter, twilio, db)
│   │   └── index.js                   # Application server bootstrap & Socket.IO initialization
│   ├── test/api.test.js               # 30-case comprehensive integration test suite
│   ├── package.json                   # Backend dependencies (express, socket.io, jsonwebtoken...)
│   └── Dockerfile                     # Backend container build
│
├── database/
│   └── schema_master.sql              # Master PostgreSQL 16 + PostGIS schema DDL with GIST indexes
│
├── frontend/                          # Next.js 15 App Router Command Center (:3000)
│   ├── public/
│   │   ├── floorplans/                # Indoor blueprint overlays (AIIMS Bhopal & Habibganj station)
│   │   └── streams/                   # Mirrored CCTV streams for surveillance grid
│   ├── src/
│   │   ├── app/                       # App Router routes (/page.tsx, /report/qr, /track/:token...)
│   │   ├── components/
│   │   │   ├── layout/                # Persistent Sidebar, TopBar, and Navigation
│   │   │   └── pages/                 # 12 Tactical & Municipal page components
│   │   ├── hooks/useStore.ts          # Central client state & Socket.IO real-time synchronization
│   │   └── lib/api.ts                 # Centralized type-safe HTTP client
│   ├── package.json                   # Frontend dependencies (next 15, react 19, tailwindcss 3.4...)
│   └── Dockerfile                     # Standalone Next.js container build
│
├── scripts/
│   ├── deploy_edge.sh                 # Linux/macOS edge production deployment script
│   ├── deploy_edge.bat                # Windows edge production deployment script
│   ├── seed_demo.js                   # Mock data seeding script for demonstrations
│   └── simulate_city_grid.py          # Continuous Python citywide event simulator
│
├── docker-compose.yml                 # 5-Service production container orchestration
├── nginx.conf                         # High-performance reverse proxy & gateway configuration
├── package.json                       # Root monorepo orchestration (npm workspaces)
├── README.md                          # Animated showcase README with 11 Mermaid diagrams
└── PROJECT_GUIDE.md                   # Complete operational project handbook (This file)
```

---

## 3. Prerequisites & System Requirements

### Recommended Hardware
* **CPU**: Quad-core Intel Core i5 / AMD Ryzen 5 or higher.
* **RAM**: 8 GB minimum (16 GB recommended for concurrent Docker composition).
* **Storage**: 5 GB free disk space.
* **Network**: Active Internet connection for initial package installation, OSRM public routing, and Twilio dispatch. (Platform supports fully air-gapped offline operation once installed).

### Software Environment
| Software | Minimum Version | Verified Version | Installation Command Check |
|---|---|---|---|
| **Node.js** | `v18.0.0` | `v20.18.0 LTS` | `node -v` |
| **npm** | `v9.0.0` | `v10.8.2` | `npm -v` |
| **Python** | `v3.10.0` | `v3.11.x` | `python --version` |
| **pip** | `v23.0` | `v24.x` | `pip --version` |
| **Git** | `v2.30` | `v2.42+` | `git --version` |
| **Docker** (Optional) | `v24.0` | `v26.x` | `docker --version` |

---

## 4. Step-by-Step Installation & Bootstrapping

### Step 1: Clone the Repository
```bash
git clone https://github.com/yajatkataria08-a11y/NagarsetuV2.0.git
cd NagarsetuV2.0
```

### Step 2: Configure Environment Files
Copy the example configuration templates:
```bash
# Root Monorepo Environment
cp .env.example .env

# Backend API Gateway Environment
cp backend/.env.example backend/.env
```

### Step 3: Install Package Dependencies

```bash
# 1. Install root workspace dependencies
npm install

# 2. Install frontend Next.js packages
cd frontend
npm install
cd ..

# 3. Install backend Express.js packages
cd backend
npm install
cd ..

# 4. Install Python AI Microservice packages
cd ai_engine
pip install -r requirements.txt
cd ..
```

---

### Step 4: Bootstrapping Services

You can launch the platform using **Option A (All-in-One)** or **Option B (Separate Terminals)**.

#### Option A: Unified Monorepo Runner (Recommended)
From the root directory:
```bash
npm run dev:all
```
*This concurrently starts the Next.js frontend, Express backend, and FastAPI AI engine.*

#### Option B: Terminal-by-Terminal Launch

**Terminal 1 — FastAPI AI Microservice (Port 8000)**:
```bash
cd ai_engine
python server.py
# Verification output: Uvicorn running on http://127.0.0.1:8000
```

**Terminal 2 — Express.js & Socket.IO API Gateway (Port 4000)**:
```bash
cd backend
npm run dev
# Verification output: NagarSetu API Gateway running on http://localhost:4000
```

**Terminal 3 — Next.js 15 Tactical Command Center (Port 3000)**:
```bash
cd frontend
npm run dev
# Verification output: Next.js App Router ready on http://localhost:3000
```

Open your browser and navigate to: **`http://localhost:3000`**

---

## 5. Environment Variables & Configuration Matrix

### Backend Configuration (`backend/.env`)

| Variable Name | Default Value | Description |
|---|---|---|
| `PORT` | `4000` | Port for Express.js API Gateway & WebSocket server |
| `NODE_ENV` | `development` | Runtime environment (`development`, `production`, `test`) |
| `FRONTEND_URL` | `http://localhost:3000`| Allowed CORS origin for web dashboard |
| `AI_SERVICE_URL` | `http://127.0.0.1:8000`| Base URL for FastAPI Python microservice |
| `DISABLE_AUTH` | `true` | When `true`, bypasses JWT checks for rapid local demo evaluation |
| `JWT_SECRET` | `nagarsetu_dev_secret_...`| Cryptographic secret used for signing citizen/officer tokens |
| `PYTHON_BIN` | Auto-detected | Path to Python 3 binary (`python3`, `python`, or virtualenv) |
| `SUPABASE_URL` | *(Optional)* | Cloud Supabase PostGIS project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | *(Optional)* | Secret administrative service role API key |
| `TWILIO_ACCOUNT_SID` | *(Optional)* | Twilio API SID for Voice IVR & SMS |
| `TWILIO_AUTH_TOKEN` | *(Optional)* | Twilio authentication token |
| `TWILIO_PHONE_NUMBER` | *(Optional)* | Registered Twilio sender phone number |
| `GEMINI_API_KEY` | *(Optional)* | Google Cloud API key for Gemini 1.5 Flash multimodal reasoning |

---

## 6. Command Center User Manual (12 Views)

Access the Command Center at **`http://localhost:3000`**. Use the left persistent sidebar to navigate between operational modules:

### 1. Municipal Overview (`dashboard`)
* **KPI Metrics**: Displays total active SOS beacons, complaints pending triage, verified complaints, rejected complaints, sanctioned budget spent, and on-duty officers.
* **Civil Defense Area Broadcast**: Click **"Civil Defense Broadcast"** to open the modal. Select target wards (or Citywide), choose alert level (`CRITICAL_EVACUATION`, `SEVERE_WEATHER`, `CIVIL_NOTICE`), enter message text, and click **"Dispatch Broadcast"**. This pushes WebSocket notifications to all connected citizens.

### 2. SOS Monitor (`sos`)
* **Live Emergency Cards**: Prioritizes emergency calls by threat level.
* **Victim Biometrics**: Monitor real-time battery percentage (`🔋 18%`) and wearable heart rate (`❤️ 112 BPM`).
* **One-Click Tracking Link**: Click **"Generate Link"** to create a public live-tracking link (`/track/:token`) for family members.
* **Responder Dispatch**: Select an available field responder from the dropdown and click **"Dispatch Unit"**.
* **Resolve SOS**: Click **"Resolve"** once field officers confirm citizen safety.

### 3. Live GIS Tactical Map (`map`)
* **Dual-City Toggle**: Use the top-left switcher to toggle between **Bhopal** (`23.2599°N, 77.4126°E`) and **Indore** (`22.7196°N, 75.8577°E`).
* **Layer Modes**: Toggle between `📍 Incidents`, `🚨 SOS Live`, `🌡️ Risk Heatmap`, and `🗺️ All Composite Layers`.
* **Indoor Floorplan Blueprints**: Click the **"Indoor Maps"** toggle to overlay architectural blueprints for:
  * **Habibganj Railway Station** (Ground Floor & Concourse Level 1)
  * **AIIMS Bhopal Campus** (Trauma Wing & Emergency Level 0)
* **Quick Ward Fly-To**: Click any ward in the right sidebar (e.g., Ward 7, Ward 24) to instantly center the camera.

### 4. CCTV Vision Wall (`surveillance`)
* **6-Feed Video Grid**: Streams live video from intersections (`cam-01` to `cam-06`).
* **YOLOv8 Dynamic Overlays**: Displays real-time bounding boxes around detected hazards (`pothole`, `waterlogging`, `crowd_surge`, `stalled_vehicle`).
* **Frame Analysis**: Click **"Analyze Snapshot"** on any camera feed to dispatch the current frame to `/api/cctv/:id/analyze-frame` for immediate computer vision triage.

### 5. Hazard Geofences (`hazards`)
* **Create Dynamic Hazard**: Click **"Create Geofence"**. Enter hazard name, select coordinates, set radius ($50\text{m} - 1000\text{m}$), and specify duration in minutes.
* **Automated TTL Countdown**: Watch the real-time countdown meter. The background 30-second TTL Janitor will automatically expire and delete the hazard once the timer reaches zero.
* **Quick Presets**: Click preset buttons for *Flash Flood Alert*, *Gas Leak Zone*, *Civil Disturbance*, or *Structural Collapse*.

### 6. Incidents & AI Triage (`incidents`)
* **Multi-Attribute Filter**: Filter tickets by keyword, status (`pending`, `verified`, `rejected`, `resolved`), ward number, and severity ($1 - 5$).
* **AI Confidence Gauge**: Inspect Scikit-Learn NLP confidence scores and citizen credibility ratings.
* **Bulk Operations**: Select multiple tickets using checkboxes, then click `Verify Selected`, `Reject Selected`, or `Assign Officer` in the floating action bar.

### 7. Analytics & SLA Trends (`analytics`)
* **Macro Performance**: Monitor statutory 48-hour SLA compliance percentages, average resolution hours, and total complaints.
* **Breach Tracking**: View tickets approaching or exceeding statutory deadlines.
* **Ward Budget Analysis**: Inspect sanctioned infrastructure funds versus disbursed contractor amounts.

### 8. Ward & Officer Board (`wards`)
* **Ward Leaderboard**: Ranks wards by resolution speed, awarding Gold, Silver, and Bronze badges.
* **Officer Dispatch Roster**: View on-duty personnel, assigned tickets, and average response times.
* **Contractor Accountability Ledger**:
  * View active infrastructure contracting firms and their real-time trust ratings ($0\% - 100\%$).
  * Click **"Penalize Contractor"** on any firm to assess a monetary deduction for delayed pothole or streetlight repairs.
  * Inspect the permanent public audit trail recording penalty history.

### 9. Citizen Trust Registry (`users`)
* **Trust Scoring**: Reviews citizen report accuracy and spam history.
* **Account Moderation**: Click **"Verify & Activate"**, **"Flag for Audit"**, or **"Ban User"** to protect the dispatch center from malicious spam reports.

### 10. DriveLegal: BIMSTEC Penal Code System (`legal`)
* **7 Jurisdictions**: Switch between India (MVA 2019 / BNS 2023), Bangladesh, Bhutan, Myanmar, Nepal, Sri Lanka, and Thailand.
* **Penalty Multiplier Calculator**: Select a violation (e.g., *Drunk Driving*), toggle **"Repeat Offense (3x)"**, **"Commercial Vehicle (1.5x)"**, or **"School Zone (2x)"**, and view the dynamically calculated penalty.
* **Hinglish AI Query Bot**: Type queries like *"Bina helmet ke pakde gaye to kya hoga?"* to get exact statutory sections and procedural rights.

### 11. Notifications Hub (`notifications`)
* **Channel Filters**: Switch between `All`, `🚨 SOS`, `📋 Incidents`, `⚠️ Hazards`, and `⚙️ System`.
* **Read Sync**: Click **"Mark All Read"** to synchronize unread counts across all active browser windows.

### 12. Settings & System APIs (`settings`)
* **Storage Toggle**: Switch between Supabase Cloud PostGIS clustering and Local Disk Persistence mode.
* **AI Sensitivity Tuning**: Adjust sliders for Auto-Verify Threshold ($\ge 0.82$) and Auto-Reject Threshold ($< 0.30$).
* **Native Integration Status**: Verify Android APK bridge and Wear OS smartwatch connectivity.

---

## 7. Progressive Web App (PWA) Emergency Suite

Designed for zero-barrier citizen protection without requiring application installation:

### 1. 3-Tap Rapid Emergency Portal (`http://localhost:3000/report/qr`)
* Accessible by scanning physical QR codes mounted on municipal streetlights and bus stops.
* Features 3 large high-contrast emergency tiles (*Medical Emergency*, *Infrastructure Hazard*, *Women's Safety Alert*).
* Acquires HTML5 browser GPS coordinates with a single tap.

### 2. PWA Fake Call Deterrent (`http://localhost:3000/report/qr/fake-call`)
* Simulates an authentic full-screen incoming phone call.
* Customizable caller identity (*"Mom"*, *"Inspector Sharma"*, *"Colleague"*).
* Generates realistic vibration patterns, audio ringtones, and an active in-call conversation timer to deter potential stalkers.

### 3. Voice Safety Coach (`http://localhost:3000/report/qr/voice-coach`)
* Interactive turn-by-turn voice companion that speaks reassuring navigation advice and safe corridor directions to pedestrians walking through isolated areas at night.

### 4. Public Live Tracking View (`http://localhost:3000/track/:token`)
* Lightweight Leaflet web interface delivered via SMS to emergency contacts.
* Displays live victim location breadcrumbs, remaining battery percentage, and responder unit arrival ETA without requiring an account.

---

## 8. Native Android & Wear OS Deployment Guide

### Building the Android Client
Open the project in **Android Studio Hedgehog (2023.1.1+)** or build via command line:

```bash
cd android
# Grant execution permissions (Linux/macOS)
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# android/app/build/outputs/apk/debug/app-debug.apk
```

### Wear OS Smartwatch Direct Dispatcher (`DirectSosDispatcher.kt`)
The companion smartwatch application includes an autonomous fallback dispatcher:
1. **Primary BLE Channel**: Attempts transmission through the paired Android smartphone via Google Play Services Wearable Data Client.
2. **Standalone Wi-Fi / LTE Fallback**: If the phone is out of battery or Bluetooth is severed, `DirectSosDispatcher` initiates a direct HTTPS POST request to `${BACKEND_URL}/api/sos` containing the wearer's real-time optical PPG heart rate (`❤️ 112 BPM`) and GPS coordinates.

---

## 9. AI Engine & Machine Learning Subsystems

### Training the Dual-City Risk Regressor
The spatial regressor can be retrained on updated incident data at any time:
```bash
cd ai_engine
python train_dual_city_model.py
```
*Outputs:*
* Model checkpoint: `ai_engine/models/risk_regressor.joblib`
* Standard scaler: `ai_engine/models/risk_scaler.joblib`
* Accuracy score: **$R^2 = 0.9652$** across 2,040+ geocoded Bhopal and Indore records.

### NLP Incident Verification CLI
Test incident triage from the command line:
```bash
cd ai_engine
python unified_civic_ai.py verify '{"description":"High voltage electric cable snapped and sparking on wet road in Arera Colony","severity":5}'
```

---

## 10. IoT Sensor Ingestion & Escalation Matrix

Hardware telemetry is ingested via `POST /api/ingest/sensor`:

```bash
curl -X POST http://localhost:4000/api/ingest/sensor \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "SENSOR-HABIB-FLOOD-01",
    "type": "water_depth",
    "value": 0.52,
    "unit": "meters",
    "lat": 23.2315,
    "lng": 77.4328,
    "ward": "Ward 12"
  }'
```

### Escalation Thresholds
* **Water Depth $> 0.45\text{ m}$** for 3 readings $\implies$ Auto-creates `Auto: Flood Alert [Ward 12]` ($500\text{m}$ radius, 2.0x risk boost, 2h TTL).
* **Gas $> 70\text{ ppm}$** for 2 readings $\implies$ Auto-creates `Auto: Gas Hazard [Ward 12]` ($300\text{m}$ radius, 3.0x risk boost, 1h TTL).
* **Temperature $> 48^\circ\text{C}$** for 2 readings $\implies$ Auto-creates `Auto: Extreme Heat [Ward 12]` ($400\text{m}$ radius, 1.5x risk boost, 3h TTL).
* **Decibel $> 110\text{ dB}$** for 4 readings $\implies$ Auto-creates `Auto: Noise/Blast Alert [Ward 12]` ($200\text{m}$ radius, 1.8x risk boost, 30m TTL).

---

## 11. Contractor Accountability Public Ledger

Civil contractors responsible for infrastructure maintenance are monitored through an automated trust ledger:
* **Penalize Contractor via API**:
```bash
curl -X POST http://localhost:4000/api/contractors/CON-001/penalize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25000,
    "reason": "Unrectified major pothole on Arera Main Road breached 48h SLA",
    "citingOfficer": "Inspector R. Sharma [MP-041]"
  }'
```
* **Audit Trail**: Every assessed deduction permanently updates the contractor's trust score and appends to the immutable audit ledger stored in `store.json`.

---

## 12. DriveLegal: BIMSTEC Penal Code System

DriveLegal enables cross-border transport legal calculations across South Asian nations:

### Querying Applicable Fines with Repeat Multipliers:
```bash
curl -X POST http://localhost:4000/api/legal/calculate-fine \
  -H "Content-Type: application/json" \
  -d '{
    "violationCode": "IN-MVA-004",
    "isRepeat": true,
    "vehicleType": "COMMERCIAL",
    "isSchoolZone": true
  }'
```

### Hinglish Natural Language Legal Bot:
```bash
curl -X POST http://localhost:4000/api/legal/query-bot \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Bina license gaadi chalane par kitna fine lagega aur kya gaadi zabt hogi?"
  }'
```

---

## 13. Developer API Reference with cURL Examples

### 1. Trigger Emergency SOS Beacon
```bash
curl -X POST http://localhost:4000/api/sos \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "usr_998",
    "userName": "Ananya Verma",
    "lat": 23.2599,
    "lng": 77.4126,
    "batteryLevel": 19,
    "heartRate": 118,
    "contacts": ["+919876543210"]
  }'
```

### 2. Submit Municipal Incident Complaint
```bash
curl -X POST http://localhost:4000/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Road Hazard",
    "description": "Deep dangerous pothole near Habibganj railway station gate 2",
    "lat": 23.2315,
    "lng": 77.4328,
    "ward": "Ward 12",
    "severity": 4
  }'
```

### 3. Calculate Triple-Path Safe Navigation Routes
```bash
curl -X POST http://localhost:4000/api/routing/calculate-routes \
  -H "Content-Type: application/json" \
  -d '{
    "startLat": 23.2599,
    "startLng": 77.4126,
    "endLat": 23.2315,
    "endLng": 77.4328,
    "timeHour": 23
  }'
```

### 4. Create Dynamic Hazard Geofence
```bash
curl -X POST http://localhost:4000/api/hazards \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Arera Pipeline Rupture",
    "lat": 23.2450,
    "lng": 77.4200,
    "radiusMeters": 250,
    "riskBoost": 35.0,
    "durationMin": 45
  }'
```

---

## 14. Testing Suite & City-Grid Simulation

### Running the 30-Case Integration Test Suite
```bash
cd backend
npm run test:api
```
*Executes automated assertions against health status, SOS triggers, NLP verification, dynamic hazard TTL, Gaussian heatmaps, BIMSTEC legal lookups, and contractor penalty logging.*

### Running the Async City-Grid Simulator
To simulate realistic municipal load with incoming citizen complaints, SOS alarms, and fluctuating sensor spikes:
```bash
python scripts/simulate_city_grid.py --interval 2.5 --city bhopal
```
*Leave this running in a separate terminal to observe dynamic live updates on the Command Center dashboard.*

---

## 15. Docker Compose & Production Edge Deployment

### Multi-Container Docker Deployment
Launch all 5 production containers with a single command:
```bash
docker-compose up --build -d
```
*Verify running containers:*
```bash
docker-compose ps
```

### Edge Production Shell Deployment
* **Linux / macOS**:
```bash
chmod +x scripts/deploy_edge.sh
./scripts/deploy_edge.sh
```
* **Windows**:
```bat
scripts\deploy_edge.bat
```

---

## 16. Troubleshooting & Frequently Asked Questions

### Q1: Python binary not recognized by the backend?
**Solution**: Ensure Python 3.11+ is installed. Set `PYTHON_BIN` explicitly in `backend/.env`:
```env
PYTHON_BIN=C:\Users\<YourUser>\AppData\Local\Programs\Python\Python311\python.exe
# or on Linux/macOS:
PYTHON_BIN=/usr/bin/python3
```

### Q2: Port 3000, 4000, or 8000 is already in use?
**Solution**: Kill conflicting processes:
* Windows PowerShell:
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 4000).OwningProcess | Stop-Process -Force
```
* Linux / macOS:
```bash
lsof -ti:4000 | xargs kill -9
```

### Q3: How to test the dashboard without logging in?
**Solution**: Ensure `DISABLE_AUTH=true` is set in `backend/.env`. This enables unrestricted administrative evaluation mode.

### Q4: CCTV video feeds fail to play?
**Solution**: Ensure `backend/public/streams/cam-01.mp4` through `cam-06.mp4` exist. If missing, generate synthetic test feeds:
```bash
python scripts/generate_cctv_streams.py
```

---

## 17. Hackathon 5-Minute Live Demonstration Script

Follow this script to demonstrate the platform to judges and evaluators:

| Timeline | Action | Screen / View | Talking Points |
|---|---|---|---|
| **0:00 - 0:45** | Introduce Problem & Overview | `Dashboard Overview` | Highlight municipal silos, 40% false alarm storm, and unlit blindspots in developing cities. Show the 6-KPI live grid. |
| **0:45 - 1:45** | Trigger Live SOS with Vitals | `SOS Monitor` | Trigger an SOS via cURL or mobile. Point out real-time battery level (`18%`) and wearable heart rate (`112 BPM`). Click "Generate Tracking Link" and show public tracking URL. |
| **1:45 - 2:30** | Tactical GIS & Indoor Blueprints | `Live GIS Map` | Toggle between Bhopal and Indore. Switch layer modes. Toggle the **Indoor Blueprints** for Habibganj Railway Station and AIIMS Bhopal. |
| **2:30 - 3:15** | CCTV Grid & YOLOv8 Vision | `CCTV Vision Wall` | Show 6 live streams. Point out bounding boxes on potholes and waterlogging. Click "Analyze Snapshot" for instant AI frame verification. |
| **3:15 - 4:00** | Contractor Accountability & DriveLegal | `Wards & Officers` & `DriveLegal` | Open Contractor Ledger. Assess a penalty deduction of ₹25,000 for delayed pothole repairs. Switch to DriveLegal and show Hinglish query bot. |
| **4:00 - 5:00** | Triple-Path Routing & Architecture | `README.md` & Architecture | Explain Safest Main-Road Arterial Bias vs Fastest route. Conclude with enterprise multi-service resilience and air-gapped fallback. |

---

*Authored by the NagarSetu Core Engineering Team.*
