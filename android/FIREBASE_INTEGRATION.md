# Firebase × Supabase Hybrid Integration Guide

## Architecture Overview

NagarSetu uses both Firebase and Supabase in a **complementary hybrid model** where each service does what it is best at.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    NagarSetu Hybrid Backend                              │
│                                                                          │
│  FIREBASE  (real-time, push, observability)                             │
│  ─────────────────────────────────────────                              │
│  • FCM push notifications   → city alerts, SOS ACK, ward broadcasts     │
│  • Realtime Database        → live GPS tracking (Raksha LiveTrack)      │
│  • Firestore                → ephemeral alert feed (< 24h TTL)          │
│  • Firebase Auth            → optional Google Sign-In                   │
│  • Analytics + Crashlytics  → usage funnels, crash-free rate            │
│                                                                          │
│  SUPABASE  (persistent data, authoritative auth)                        │
│  ────────────────────────────────────────────────                       │
│  • Phone OTP Auth (Twilio)  → primary citizen login                     │
│  • profiles table           → user identity + fcm_token storage         │
│  • incidents / wards / KPI  → PostGIS-enabled civic PostgreSQL          │
│  • Realtime (CDC)           → officer alert inserts → Dashboard         │
│  • Storage                  → report photo uploads                      │
│  • Edge Functions           → AI triage, hazard scoring                 │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## New Files Added

### Backend module: `:backend:components:firebase`

| File | Purpose |
|------|---------|
| `di/FirebaseModule.kt` | Hilt singleton providers for all Firebase SDK instances and NagarSetu wrappers |
| `fcm/FcmTokenManager.kt` | Retrieves FCM token, syncs it to Supabase `profiles.fcm_token`, manages topic subscriptions |
| `fcm/NagarSetuFirebaseMessagingService.kt` | `FirebaseMessagingService` — handles `onNewToken` (syncs to Supabase) and `onMessageReceived` (shows typed notifications) |
| `auth/FirebaseAuthManager.kt` | Manages optional Google Sign-In as secondary auth provider linked to the Supabase uid |
| `realtime/GpsTrackingRepository.kt` | Writes/reads live GPS points to Firebase Realtime Database for Raksha LiveTrack |
| `realtime/LiveAlertFirebaseSource.kt` | Firestore `city_alerts` collection — publishes and observes ephemeral city alerts |
| `analytics/NagarSetuAnalytics.kt` | Typed wrapper over Firebase Analytics + Crashlytics (prevents raw string typos across 13 modules) |

### New files in existing modules

| File | Module | Purpose |
|------|--------|---------|
| `data/SupabaseTokenUploaderImpl.kt` | `:backend:components:auth` | **Hybrid bridge** — implements `SupabaseTokenUploader` interface using Supabase PostgREST to store FCM tokens |
| `di/AuthModule.kt` (updated) | `:backend:components:auth` | Binds `SupabaseTokenUploaderImpl` → `SupabaseTokenUploader` |
| `data/LiveTrackManager.kt` | `:backend:components:raksha` | Connects `FusedLocationProviderClient` GPS fixes → `GpsTrackingRepository` → Firebase RTDB |
| `data/HybridAlertBridge.kt` | `:backend:components:dashboard` | Merges Supabase Realtime + Firestore alert streams into a single deduplicated `StateFlow<List<UnifiedAlert>>` |

### Database / Config files

| File | Purpose |
|------|---------|
| `database/firebase_rtdb_rules.json` | Security rules for Realtime Database — owner-only write, public read via session link |
| `database/firestore_rules.rules` | Firestore rules — public read for `city_alerts`, server-only write |
| `frontend/app/google-services.json.template` | Placeholder showing required structure; replace with real file from Firebase Console |

---

## Setup Steps

### 1. Firebase Console Setup

1. Go to [Firebase Console](https://console.firebase.google.com) → **Create project** → name it `NagarSetu`
2. **Add Android app**:
   - Package: `com.nagarsetu`
   - SHA-1: run `./gradlew :frontend:app:signingReport` to get your debug key SHA-1
3. **Download `google-services.json`** → place at `frontend/app/google-services.json`
4. Enable these services:
   - **Authentication** → Sign-in method → Enable **Google**
   - **Realtime Database** → Create database → Start in **test mode** → then apply rules below
   - **Firestore Database** → Create database → **Production mode** → apply rules below
   - **Cloud Messaging** → auto-enabled
   - **Analytics** → auto-enabled by google-services plugin
   - **Crashlytics** → enable in the console (no extra SDK needed, already included)

### 2. Apply Security Rules

**Realtime Database** (Firebase Console → Realtime Database → Rules tab):
```
Paste contents of: database/firebase_rtdb_rules.json
```

**Firestore** (Firebase Console → Firestore → Rules tab):
```
Paste contents of: database/firestore_rules.rules
```

### 3. Supabase Schema Update

Run this SQL in your Supabase SQL Editor to add the `firebase_uid` column used for Google Sign-In linking:

```sql
-- Add Firebase UID column for optional Google Sign-In linking
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS firebase_uid text UNIQUE;

CREATE INDEX IF NOT EXISTS idx_profiles_firebase_uid
    ON public.profiles (firebase_uid)
    WHERE firebase_uid IS NOT NULL;

COMMENT ON COLUMN public.profiles.firebase_uid IS
    'Firebase Auth UID — populated only when user links Google Sign-In. '
    'Used by Firebase Admin SDK to verify identity for targeted push.';
```

The `fcm_token` column is already in the existing schema:
```sql
-- Already exists in supabase_schema.sql:
-- fcm_token text  -- push notification token
```

### 4. Build Verification

```bash
# Verify the firebase module compiles
./gradlew :backend:components:firebase:assembleDebug

# Verify the auth bridge compiles
./gradlew :backend:components:auth:assembleDebug

# Verify dashboard hybrid bridge compiles
./gradlew :backend:components:dashboard:assembleDebug

# Full build
./gradlew assembleDebug
```

---

## Data Flow Diagrams

### FCM Token Lifecycle (Hybrid Bridge)

```
App Launch / Token Refresh
        │
        ▼
NagarSetuFirebaseMessagingService.onNewToken(token)
        │
        ├─── uid available? ──YES──► SupabaseTokenUploaderImpl.upsertFcmToken(uid, token)
        │                                    │
        │                                    ▼
        │                           PATCH /rest/v1/profiles?uid=eq.<uid>
        │                           { "fcm_token": "<token>" }  ← Supabase
        │
        └─── uid NOT available? ──► Store token locally in SharedPrefs
                                    (will sync on next successful login)

User completes Supabase OTP login
        │
        ▼
LoginViewModel.verifyOtp() succeeds
        │
        ▼
FcmTokenManager.syncTokenToSupabase(uid, uploader)
        │
        ▼
SupabaseTokenUploaderImpl.upsertFcmToken(uid, token)
```

### Raksha LiveTrack GPS Flow

```
User taps "Share Location" in RakshaScreen
        │
        ▼
LiveTrackManager.startTracking(ownerUid)
        │
        ├─► GpsTrackingRepository.startSession(ownerUid)
        │           │
        │           ▼
        │   Firebase RTDB: live_tracks/{sessionId} created
        │
        └─► FusedLocationProviderClient requests updates (5s interval)
                    │
                    ▼ (every GPS fix)
            LiveTrackManager.onNewLocation(sessionId, location)
                    │
                    ▼
            GpsTrackingRepository.pushLocation(sessionId, lat, lng, accuracy)
                    │
                    ▼
            Firebase RTDB: live_tracks/{sessionId}/locations/{pushKey}
                    │
                    ▼ (real-time push to observers)
            Trusted contact's device observes via:
            GpsTrackingRepository.observeLatestLocation(sessionId)
                    │
                    ▼
            RakshaViewModel → LiveTrackScreen UI updates
```

### Dashboard Alert Merge Flow

```
Supabase live_alerts table INSERT (officer posts alert)
        │
        ▼ (Supabase Realtime CDC)
DashboardRepository.startLiveAlertSubscription()
        │
        ▼
HybridAlertBridge.mergeSupabaseAlerts(alerts)
        │
        ├── Deduplicated + sorted ──►┐
        │                            │
Firebase Firestore city_alerts       │   StateFlow<List<UnifiedAlert>>
 (NDMA / AI / city-wide broadcast)   │              │
        │                            │              ▼
        ▼                            │   DashboardViewModel.mergedAlerts
LiveAlertFirebaseSource              │              │
 .observeAlerts(wardNumber)          │              ▼
        │                            │   Dashboard AlertFeed Composable
        └── Deduplicated + sorted ──►┘
```

---

## Module Dependency Graph (updated)

```
:frontend:app
    ├── :backend:components:firebase      ← NEW
    ├── :backend:components:auth
    │       └── :backend:components:firebase  (for SupabaseTokenUploader interface)
    ├── :backend:components:dashboard
    │       └── :backend:components:firebase  (for HybridAlertBridge)
    └── :backend:components:raksha
            └── :backend:components:firebase  (for GpsTrackingRepository)
```

---

## Adding Firebase to More Modules

If a new module needs Firebase (e.g. for Analytics event tracking), add to its `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":backend:components:firebase"))
    // No need to add firebase BOM — it's handled inside the firebase module
    // Just inject NagarSetuAnalytics, FcmTokenManager, etc. via Hilt
}
```

Then inject via constructor:
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val analytics: NagarSetuAnalytics
) : ViewModel() {
    init {
        analytics.onFeatureOpened(Feature.MY_FEATURE)
    }
}
```

---

## Notification Payload Reference

When sending pushes from your Supabase Edge Function via Firebase Admin SDK:

```javascript
// In your Supabase Edge Function (TypeScript):
import { initializeApp, cert } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

// Send to specific device
await getMessaging().send({
  token: profile.fcm_token,
  data: {
    type: 'SOS_ACK',          // WARD_ALERT | SOS_ACK | INCIDENT_VERIFIED | CITY_BROADCAST
    title: 'SOS Acknowledged',
    body: 'Your SOS has been received. Help is on the way.',
  },
  android: {
    priority: 'high',
    notification: { channelId: 'sos_alerts' }
  }
});

// Send to ward topic (all citizens in ward 42)
await getMessaging().send({
  topic: 'ward_42_alerts',
  data: {
    type: 'WARD_ALERT',
    title: 'Flash Flood Warning',
    body: 'Kolar area: avoid low-lying roads.',
    ward: '42',
    severity: 'HIGH'
  }
});
```
