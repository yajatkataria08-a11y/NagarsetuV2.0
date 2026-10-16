# NagarSetu — Fixes Applied on Top of Base

## 🔧 Bug Fixes

### 1. ShakeDetector (emergency-ai + raksha)
- **Before:** 1.3g threshold, single shake = trigger, 1s debounce only
- **After:** 2.5g threshold, requires 3 shakes within 1500ms window, 3s cooldown
- **Files:** `backend/components/emergency-ai/.../ShakeDetector.kt`, `backend/components/raksha/.../RakshaShakeDetector.kt`

### 2. TfIdfBot (drive-legal)
- **Before:** 4 FAQ entries, English only
- **After:** 50+ FAQ entries covering speeding, helmet, signal, drunk driving, insurance, parking, registration, mobile, seat belt, documents, overloading, minor driving, Bhopal RTO contacts, and full Hinglish support
- **File:** `backend/components/drive-legal/.../TfIdfBot.kt`

### 3. TriageEngine (emergency-ai)
- **Before:** Only CRITICAL + MEDIUM keywords, missing HIGH priority
- **After:** 3-tier triage (CRITICAL/HIGH/MEDIUM/LOW) with 30+ keywords including Hinglish terms
- **File:** `backend/components/emergency-ai/.../TriageEngine.kt`

## ✅ Already Correct in Base (no changes needed)
- Bhopal coordinates (23.2599, 77.4126) — correct ✓
- AndroidManifest permissions — all present ✓
- Tab navigation numbering — sequential, starts at dashboard ✓
- Hilt DI throughout — properly wired ✓
- QR code generation — ZXing properly used ✓
- OSMDroid setup — configured in NagarSetuApp ✓
- 6 themes — CIVIC_LIGHT, CIVIC_DARK, ECO_GREEN, SUNSET, HIGH_CONTRAST, ROYAL_PURPLE ✓
