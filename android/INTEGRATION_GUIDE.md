# NagarSetu – Free API Integration Guide

All seven external data integrations now use **free / open APIs** with no
mandatory API keys. This document maps each integration to its files.

---

## API Summary

| Feature          | API / Source                    | Key Required? | Files Changed |
|------------------|---------------------------------|:---:|---|
| Weather          | OpenWeatherMap (free tier)      | ✅ free | `DashboardApiService`, `DashboardRepository` |
| Disaster Alerts  | **NDMA SACHET CAP feed**        | ❌ | `NdmaApiService` *(new)*, `DisasterAlertRepository` |
| Air Quality      | **OpenAQ v2** (replaces WAQI)   | ❌ | `DashboardApiService`, `DashboardRepository` |
| EV Chargers      | Open Charge Map v3              | ❌ anonymous | `ChargingApiService`, `ChargingRepositoryImpl` |
| Hospitals/Clinics| **OSM Overpass API**            | ❌ | `HealthApiService`, `HealthDataRepositoryImpl` |
| Parking          | **OSM Overpass API** (replaces TomTom) | ❌ | `OsmApiService` *(new)*, `ParkingRepositoryImpl` |
| Routing/Transit  | **OSRM** (replaces OpenRouteService)   | ❌ | `OsrmApiService` *(new)*, `GtfsRepositoryImpl` |
| Crime/Police     | Curated seed data               | ❌ | `CrimeDataLoader` (unchanged) |
| Civic News       | GNews (free tier)               | ✅ free | `DashboardApiService` |
| Epidemic Data    | data.gov.in                     | ✅ free | `HealthApiService` |

---

## Keys needed in `ExternalApiConfig`

```kotlin
ExternalApiConfig(
    openWeatherApiKey = BuildConfig.OPEN_WEATHER_KEY,  // free tier at openweathermap.org
    dataGovInKey      = BuildConfig.DATA_GOV_IN_KEY,   // free at data.gov.in/ogdtoolkit
    openChargeMapKey  = "",                             // empty = anonymous (rate-limited)
    gNewsApiKey       = BuildConfig.GNEWS_KEY           // free tier at gnews.io
)
```

Add to `local.properties` (never commit):
```
OPEN_WEATHER_KEY=your_key_here
DATA_GOV_IN_KEY=your_key_here
GNEWS_KEY=your_key_here
```

---

## Integration Details

### 1 · Weather – OpenWeatherMap
- **Endpoint**: `GET /data/2.5/weather?q=Bhopal,IN&appid={key}&units=metric`
- **Free tier**: 1 000 calls/day
- **Files**: `DashboardApiService.getCurrentWeather()`

### 2 · Disaster Alerts – NDMA SACHET CAP
- **Endpoint**: `GET https://sachet.ndma.gov.in/cap_public_website/getAllCAPAlerts`
- **No key, no rate limit** (public government feed)
- Filters to Madhya Pradesh / Bhopal area
- Falls back to Hindi-localised Bhopal seed alerts on network failure
- **Files**: `NdmaApiService`, `DisasterAlertRepositoryImpl`

### 3 · Air Quality – OpenAQ v2
- **Endpoint**: `GET https://api.openaq.org/v2/latest?city=Bhopal&parameter=pm25`
- **No key needed**, 10 req/s public limit
- PM2.5 converted to AQI using US EPA breakpoints
- Hindi status labels (अच्छा / संतोषजनक / अस्वस्थ …)
- **Files**: `DashboardApiService.getAirQuality()`, `bhopaleAqi()` extension

### 4 · EV Chargers – Open Charge Map v3
- **Endpoint**: `GET https://api.openchargemap.io/v3/poi/?latitude=23.25&longitude=77.41&distance=25&distanceunit=KM`
- **Anonymous access** (empty key) is rate-limited to ~10 req/min – fine for civic app
- Optional: register free key at openchargemap.org to remove limit
- **Files**: `ChargingApiService`, `ChargingRepositoryImpl`

### 5 · Hospitals & Clinics – OSM Overpass
- **Endpoint**: `GET https://overpass-api.de/api/interpreter?data=<QL>`
- Queries `amenity=hospital|clinic|doctors|health_post` within 5 km of Bhopal
- Returns live OSM data with name (incl. Hindi `name:hi`), phone, opening_hours
- Falls back to asset JSON seed on failure
- **Files**: `HealthApiService.queryHospitals()`, `HealthOverpassQuery`, `HealthDataRepositoryImpl`

### 6 · Parking – OSM Overpass (replaces TomTom)
- **Endpoint**: `GET https://overpass-api.de/api/interpreter?data=<QL>`
- Queries `amenity=parking` nodes & ways within 2.5 km
- Exposes `capacity` and `fee` OSM tags
- `TomTomApiService` is now a deprecated tombstone (ERROR level)
- **Files**: `OsmApiService`, `OverpassQueries.parking()`, `ParkingRepositoryImpl`

### 7 · Routing / Transit – OSRM
- **Endpoint**: `GET https://router.project-osrm.org/route/v1/{profile}/{lng1,lat1;lng2,lat2}`
- Profiles: `driving`, `cycling`, `foot`
- Returns `duration` (s) and `distance` (m) – no key, no quota
- Replaces paid OpenRouteService (ORS) key
- **For production**: self-host with an India OSM extract for better coverage
- **Files**: `OsrmApiService`, `GtfsRepositoryImpl`

---

## Removed / Deprecated

| Was | Replaced By | Reason |
|-----|-------------|--------|
| WAQI `api.waqi.info` | OpenAQ v2 | Requires paid token for reliable access |
| TomTom Search + Traffic | OSM Overpass + OSRM | Paid subscription ($) |
| OpenRouteService | OSRM | Free key quota too low for civic scale |
| `waqiToken` field | — | Removed from `ExternalApiConfig` |
| `tomTomApiKey` field | — | Removed from `ExternalApiConfig` |
| `openRouteServiceKey` field | — | Removed from `ExternalApiConfig` |

---

## Architecture Notes

- All repositories follow the **seed-first → API-overlay → fallback-to-seed** pattern.
- The `NdmaApiService` uses raw OkHttp (not Retrofit) because the NDMA endpoint returns loosely-structured JSON that benefits from manual parsing with `org.json`.
- `OsmOverpassApiService` and `HealthApiService.queryHospitals()` both hit `overpass-api.de`; they are kept in separate modules (park-ease vs health-watch) to honour the module boundary.
- `GreenRouteApiService` is now a Kotlin `typealias` for `OsrmApiService` to maintain backward compatibility for any existing callers.
