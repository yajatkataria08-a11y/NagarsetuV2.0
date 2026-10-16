# NagarSetu — Best of Both Merged

## What this is
Full merge of two project versions:
- **NagarSetu_WithM4M5** (zip1): Superior backend — M4/M5 bug fixes (ShakeDetector 2.5g/3-shake, TriageEngine 3-tier, TfIdfBot 50+ Hinglish FAQs), more backend services, DriveLegalRepositoryImproved, RAGService, RiskModelService, CrisisManager, LiveAlertService, WardAuthorityRepository, RoadWatch full pipeline, predictive AI components
- **nagarsetu_pyaar** (zip2): Superior UI/UX — Dark deep-indigo design system, NagarSetuColors, animated LoginScreen with OTP boxes, clean ProfileScreen, dark DashboardScreen, 5-tab bottom nav, ServicesMenuScreen, proper EmergencyScreen with fake call, step-based ReportIt, all screens on dark theme

## Merge Strategy
| Layer | Source |
|-------|--------|
| All backend (`/backend/**`) | zip1 (M4+M5 fixes applied) |
| Theme, Colors, Typography | zip2 (dark indigo system) |
| All screen UIs | zip2 (superior dark design) |
| MainActivity + navigation | zip2 (5-tab, splash, ServicesMenuScreen) |
| Dashboard sub-components (AlertFeed, WardKpiCard, RiskRadar, etc.) | zip1 (kept for full feature coverage) |
| RoadWatch full screens (HeatmapTab, ReportTab, TrackTab) | zip1 (more complete) |
| DriveLegalComponents | zip1 |
| Predictive sub-components (BimstecCard, ForecastCard, RAGQueryBox, etc.) | zip1 |

## Key UI Improvements (zip2)
- Deep indigo dark theme (`#1A1A3E` background, `#2A2A5A` cards, `#00D4FF` electric blue accent)
- Animated OTP login with individual digit boxes + scale animation
- Profile screen with guest/verified states, clean sections
- AssistantScreen with chip suggestions and proper chat bubbles
- EmergencyScreen with live location toggle, fake call timer, trusted contacts
- DashboardScreen: animated status banner, map mode chips, quick report chips
- ReportIt: 3-step wizard with animated transitions
- All screens consistent on the dark system

## Key Backend Improvements (zip1 M4/M5)
- **ShakeDetector**: 2.5g threshold, 3 shakes within 1500ms, 3s cooldown (was 1.3g, 1 shake, 1s)
- **TriageEngine**: 3-tier (CRITICAL/HIGH/MEDIUM/LOW), 30+ keywords, Hinglish support
- **TfIdfBot**: 50+ FAQ entries, full Hinglish, Bhopal RTO contacts, comprehensive coverage
- **DriveLegalRepositoryImproved**: full BIMSTEC multi-currency, FineUseCases, FineCalculation
- **RAGService + RiskModelService**: predictive AI backend fully wired
- **CrisisManager + LiveAlertService**: real-time crisis management
- **WardAuthorityRepository**: ward-level authority management
- **RoadWatch**: full pipeline with PotholeDetector TFLite, RoadWatchUseCases

## Build
```
./gradlew :frontend:app:assembleDebug
```
