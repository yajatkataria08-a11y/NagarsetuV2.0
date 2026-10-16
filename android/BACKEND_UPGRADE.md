# NagarSetu Backend Upgrade — Auth + Supabase + Twilio

## What Was Added

### 1. Twilio OTP Login (Phone Authentication)
**Files:**
- `backend/components/auth/src/.../TwilioOtpService.kt` — Twilio Verify V2 REST calls
- `backend/components/auth/src/.../AuthRepository.kt` — OTP + session orchestration
- `backend/components/auth/src/.../AuthModule.kt` — Hilt DI wiring
- `frontend/components/auth/src/.../LoginViewModel.kt` — 2-step OTP state machine
- `frontend/components/auth/src/.../LoginScreen.kt` — Full Compose UI with animated OTP boxes

**Flow:**
```
Enter phone ──▶ Twilio Verify sends SMS ──▶ User types 6-digit code
   ──▶ Twilio checks code ──▶ Supabase profile upserted ──▶ Session saved (AES-256)
```

### 2. Supabase Cloud Database
**Files:**
- `frontend/common-ui/src/.../SupabaseProvider.kt` — Supabase client singleton
- `frontend/common-ui/src/.../SupabaseUserRepository.kt` — CRUD for profiles, reports, emergencies

**Tables created (run in Supabase SQL editor):**
| Table | Purpose |
|-------|---------|
| `profiles` | User accounts (uid, phone, name, city, ward, verified) |
| `civic_reports` | Submitted civic issues for city-wide analytics |
| `emergency_events` | SOS events for monitoring dashboard |

### 3. Secure Session Storage
`PreferencesManager` upgraded with **AES-256 GCM** EncryptedSharedPreferences (AndroidX Security Crypto). The user's JWT / profile JSON never touches unencrypted storage.

### 4. Login Gate in MainActivity
App now shows `LoginScreen` before the main nav — skip only if a valid session exists.

---

## Setup Steps

### Step 1 — Twilio
1. Sign up at https://console.twilio.com
2. Copy **Account SID** and **Auth Token** from the Dashboard
3. Go to **Verify → Services → Create a Service**
4. Copy the **Service SID** (starts with `VA`)

### Step 2 — Supabase
1. Create a project at https://supabase.com
2. Go to **Project Settings → API**
3. Copy the **Project URL** and **anon public key**
4. Open the **SQL Editor** and run the CREATE TABLE statements from `local.properties.example`
5. Optionally enable RLS policies for production

### Step 3 — local.properties
Add to your `local.properties` (already in `.gitignore`):
```properties
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_VERIFY_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
SUPABASE_URL=https://xxxxxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Step 4 — Sync and Build
```bash
# Sync Gradle
./gradlew :frontend:app:assembleDebug
```

---

## Architecture Overview

```
LoginScreen (Compose)
    │
    ▼
LoginViewModel (Hilt)
    │── SendOtpUseCase ──▶ TwilioOtpService ──▶ Twilio REST API (SMS)
    │── VerifyOtpUseCase ─▶ TwilioOtpService ──▶ Twilio check
    │                       ▶ SupabaseUserRepository ──▶ Supabase Postgrest
    │                       ▶ PreferencesManager (AES-256 session save)
    └── GetSessionUseCase ──▶ PreferencesManager (auto-login)
```

---

## Supabase RLS (Production Security)

For production, tighten Row Level Security:
```sql
-- Profiles: users can only read/write their own row
create policy "profiles_own" on public.profiles
  for all using (uid = auth.uid()::text);

-- Civic reports: authenticated users can insert, all can read
create policy "reports_insert" on public.civic_reports
  for insert with check (uid = auth.uid()::text);
create policy "reports_read" on public.civic_reports
  for select using (true);
```

---

## Dependencies Added

| Library | Version | Purpose |
|---------|---------|---------|
| `supabase-kt-bom` | 2.5.4 | Supabase BOM |
| `postgrest-kt` | via BOM | Supabase database |
| `auth-kt` | via BOM | Supabase auth |
| `ktor-client-android` | 2.3.12 | Supabase HTTP engine |
| `security-crypto` | 1.1.0-alpha06 | AES-256 encrypted prefs |
| `okhttp` | existing | Twilio REST calls |

---

## Phone Number Formats Supported
The auth module normalizes these formats automatically:
- `9876543210` → `+919876543210`
- `09876543210` → `+919876543210`
- `+919876543210` → `+919876543210` (unchanged)
