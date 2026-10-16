# NagarSetu — Build & Run

## Requirements

- Android Studio Ladybug or newer
- JDK 17 (Android Studio embedded JBR works)
- Android SDK 34

## Setup

1. Open `d:\Projects\Hackathon\NagarSetu_v2_improved` in Android Studio.
2. Ensure `JAVA_HOME` points to the IDE JBR if building from terminal.
3. Sync Gradle.

## Build

```powershell
cd d:\Projects\Hackathon\NagarSetu_v2_improved
.\gradlew.bat :frontend:app:assembleDebug
```

## Architecture

| Layer | Path | Role |
|-------|------|------|
| App | `frontend/app` | NavHost, themes, AI FAB |
| Core UI + data | `frontend/common-ui` | Themes, Room, OSM, `ThemeViewModel` |
| **Backend core** | `backend/components/core` | `CivicDataHub`, **NagarSetu AI** assistant |
| UI features | `frontend/components/*` | Compose screens |
| Feature backends | `backend/components/*` | Repositories (use `CivicDataHub` where wired) |

## Themes (6)

Persisted in DataStore — change via **Home → palette icon**:

- Civic Light (default)
- Civic Dark
- Eco Green
- Sunset Amber
- High Contrast
- Royal Purple

## NagarSetu AI chatbot

- **FAB** (robot icon) on every tab except the chat screen
- **Home → AI icon** or “NagarSetu AI” service card
- Route: `assistant`
- Backend: `NagarSetuAssistant` + `AssistantRepository` (Tf-IDF + intents + live data from `app_data.json`)
- Chat history stored in Room `chat_messages`
- Suggests navigation (“Open park ease”) when relevant

## Backend improvements

- `CivicDataHub` — single source for wards, parking, EV, hospitals, emergency numbers
- Dashboard repository uses hub for police markers + city summary alerts
- Parking lots sorted by distance from user (Bhopal center in demo)

## Report It

Dashboard → **Report an Issue** or `report_it` route.
