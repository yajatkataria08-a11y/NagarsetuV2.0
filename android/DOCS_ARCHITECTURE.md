# NagarSetu — Full-Stack Architecture & Integration Guide

## 1. Overall Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Jetpack Compose)                           │
│                                                                              │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐   │
│  │  Dashboard   │ │  RoadWatch   │ │   Raksha/    │ │  ChargeUp /      │   │
│  │  Screen +    │ │  Screen +    │ │  Emergency   │ │  ParkEase /      │   │
│  │  ViewModel   │ │  ViewModel   │ │  Screen + VM │ │  DriveLegal VMs  │   │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘   │
│         │                │                │                   │             │
│         ▼                ▼                ▼                   ▼             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │              HILT — Dependency Injection Graph                       │    │
│  │   SingletonComponent: SupabaseClient, Room DB, DataStore             │    │
│  │   ViewModelComponent: All Repositories & UseCases                   │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
└───────────────────────────────│─────────────────────────────────────────────┘
                                │
          ┌─────────────────────┼──────────────────────┐
          │                     │                      │
          ▼                     ▼                      ▼
┌─────────────────────┐ ┌───────────────────┐ ┌────────────────────────┐
│   BACKEND MODULES   │ │   LOCAL CACHE     │ │   EXTERNAL SERVICES    │
│  (Android Libraries)│ │   (offline-first) │ │                        │
│                     │ │                   │ │  • Twilio Verify (OTP) │
│  ┌───────────────┐  │ │  ┌─────────────┐  │ │  • Groq / Gemini LLM  │
│  │ AuthRepo      │  │ │  │  Room DB    │  │ │  • SendGrid Email      │
│  │ RoadWatchRepo │  │ │  └──────┬──────┘  │ │  • OpenWeatherMap      │
│  │ DashboardRepo │  │ │         │ sync    │ └────────────────────────┘
│  │ EmergencyRepo │  │ │  ┌──────▼──────┐  │
│  │ ChargingRepo  │  │ │  │  DataStore  │  │
│  │ ParkingRepo   │  │ │  │  (profile,  │  │
│  │ DriveLegalRepo│  │ │  │  prefs)     │  │
│  └───────┬───────┘  │ │  └─────────────┘  │
│          │           │ └───────────────────┘
└──────────│───────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    SUPABASE BACKEND                                           │
│                    https://lpqyqrfuxmxtvmkqrjvl.supabase.co                  │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Postgrest   │  │   Realtime   │  │   Storage    │  │    Auth      │   │
│  │  (REST API)  │  │  (WebSocket) │  │  (S3-compat) │  │  (JWT/RLS)   │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────────────┘   │
│         │                 │                  │                              │
│  ┌──────▼─────────────────▼──────────────────▼──────────────────────────┐  │
│  │                    PostgreSQL Database                                 │  │
│  │                                                                        │  │
│  │  profiles          road_reports        civic_reports                   │  │
│  │  wards             emergency_events    live_alerts                     │  │
│  │  charging_sessions parking_bookings   challans                         │  │
│  │  activity_logs     report_upvotes     (ward_kpi VIEW)                  │  │
│  │                                                                        │  │
│  │  Row Level Security on ALL tables                                      │  │
│  │  PostGIS for geo-spatial queries                                       │  │
│  │  Realtime pub on: road_reports, live_alerts, emergency_events          │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Data Flow: UseCase → Repository → Supabase

```
ViewModel
    │
    ├─ inject UseCases (domain layer)
    │       │
    │       ▼
    │   SubmitRoadReportUseCase
    │       │  validate inputs
    │       │  enforce business rules
    │       ▼
    │   RoadWatchRepository       ← injected via Hilt
    │       │
    │       ├──► PotholeDetector (TFLite) — on-device AI
    │       │
    │       ├──► Supabase Storage  — upload photo → public URL
    │       │         bucket: road-report-photos
    │       │
    │       ├──► Supabase Postgrest — insert row to road_reports
    │       │         with photo_url, ai_confidence, lat/lng, ward
    │       │
    │       ├──► Room DAO           — cache locally for offline
    │       │
    │       └──► SupabaseUserRepository.logActivity()  — audit trail
    │
    └─ collect StateFlow<List<RoadReport>> (hot stream)
            │
            ├─ seeded from Room cache (instant, offline)
            └─ refreshed from Supabase (async, 300 latest)
               + real-time Realtime channel (live inserts)
```

---

## 3. Realtime Subscriptions Summary

| Channel Name           | Table             | Events          | Consumer              |
|------------------------|-------------------|-----------------|-----------------------|
| `road_reports_live`    | road_reports      | INSERT          | RoadWatchRepository   |
| `live_alerts_feed`     | live_alerts       | INSERT          | DashboardRepository   |
| `live_alerts_update`   | live_alerts       | UPDATE          | DashboardRepository   |
| `profile_sync:{uid}`   | profiles          | UPDATE          | SupabaseUserRepository|
| `emergency_monitor`    | emergency_events  | INSERT          | Crisis management     |

---

## 4. File Upload Pattern (Supabase Storage)

```
Storage buckets:
  road-report-photos/  (public)   → reports/{reportId}.jpg
  civic-report-photos/ (public)   → reports/{reportId}.jpg
  avatars/             (public)   → {uid}/avatar.jpg

Upload flow (RoadWatchRepository.uploadPhoto):
  1. Compress Bitmap → JPEG ByteArray (quality 85)
  2. supabase.storage["road-report-photos"].upload(path, bytes, upsert=true)
  3. bucket.publicUrl(path) → https://...supabase.co/storage/v1/object/public/...
  4. Store URL in road_reports.photo_url column
```

---

## 5. Authentication Architecture

```
App Start
    │
    ▼
AuthRepository.ensureGuestSession()
    │  Checks DataStore → creates UUID-based guest profile if none
    ▼
Dashboard (guest mode, read-only features)

User taps "Login with Phone"
    │
    ▼
LoginViewModel → AuthRepository.sendOtp(phone)
    │  normalizePhone() → +91XXXXXXXXXX
    │  Rate-limit check (3 per 10 min in-memory map)
    │  TwilioOtpService.sendOtp() → POST /2010-04-01/Accounts/{SID}/Messages.json
    ▼
User enters OTP → AuthRepository.verifyOtp(phone, code)
    │  TwilioOtpService.verifyOtp() → Twilio Verify check
    │  generateUid(phone) → UUID.nameUUID(phone bytes) [deterministic]
    │  SupabaseUserRepository.fetchProfile(uid) → check existing profile
    │  SupabaseUserRepository.upsertProfile(profile) → Postgrest upsert
    │  PreferencesManager.saveUserSession(profile) → DataStore persist
    │  SupabaseUserRepository.startProfileSync(uid) → Realtime sub
    ▼
OtpState.Verified → navigate to Dashboard (full access)
```

---

## 6. gradle Dependencies to Add

```kotlin
// app/build.gradle.kts — add to dependencies block:

// Supabase BOM (use a single version for all plugins)
implementation(platform("io.github.jan-tennert.supabase:bom:2.6.0"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.github.jan-tennert.supabase:realtime-kt")
implementation("io.github.jan-tennert.supabase:storage-kt")

// Ktor engine (required by Supabase client)
implementation("io.ktor:ktor-client-android:2.3.12")
implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

// DataStore (for profile cache)
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Kotlinx serialization (for DTOs)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
```

---

## 7. Offline-First Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    OFFLINE-FIRST LAYERS                     │
│                                                             │
│  Layer 1 — DataStore:                                       │
│    • User profile (30-min TTL, auto-refresh on network)     │
│    • Auth session (persisted across restarts)               │
│    • User preferences / theme                               │
│                                                             │
│  Layer 2 — Room DB:                                         │
│    • road_reports (last 300, sync on connect)               │
│    • parking_bookings (user's own)                          │
│    • charging_sessions (user's own)                         │
│    • civic_issues                                           │
│    • chat_messages                                          │
│    • challans                                               │
│                                                             │
│  Sync strategy:                                             │
│    • Repository init: load Room → show instantly            │
│    • Repository init: fetch Supabase async → merge          │
│    • Report submission: optimistic insert to Room + UI,     │
│      then Supabase. On failure: retry queue via WorkManager  │
│                                                             │
│  TODO: Add WorkManager RetryWorker for failed submissions   │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. Security Checklist

- [x] Supabase URL + anon key from `local.properties` → `BuildConfig` (never in source)
- [x] RLS enabled on ALL tables
- [x] Storage buckets scoped (photo upload requires authenticated uid in path)
- [x] OTP rate-limiting: 3 per 10 minutes per phone (in-memory + Twilio's own limits)
- [x] Input validation: phone normalization, OTP digit-only check
- [ ] TODO: Custom Supabase JWT after Twilio verification (for strong RLS)
- [ ] TODO: Certificate pinning for Supabase host in production
- [ ] TODO: ProGuard rules to strip logging in release builds
- [ ] TODO: Obfuscate `Secrets.kt` usage — move to encrypted SharedPreferences or KMS

---

## 9. Immediate Action Items (Priority Order)

1. **Run `01_supabase_schema.sql`** in Supabase SQL Editor (one-time setup)
2. **Replace** `SupabaseProvider` with `SupabaseModule` (Hilt provides `SupabaseClient` directly)
3. **Replace** `SupabaseUserRepository` with the improved version (file 03)
4. **Replace** `RoadWatchRepository` with Supabase-connected version (file 04)
5. **Inject `SupabaseClient`** into `DashboardRepository`, `EmergencyRepository`, `ChargingRepositoryImpl`, `ParkingRepositoryImpl` using the pattern in file 05
6. **Create Storage buckets** in Supabase Dashboard: `road-report-photos`, `civic-report-photos`, `avatars`
7. **Enable Realtime** for tables: `road_reports`, `live_alerts`, `emergency_events`
8. Add `WorkManager` retry queue for offline report submissions
