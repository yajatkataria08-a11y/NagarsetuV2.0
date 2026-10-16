# NagarSetu Research & Real-World API Integration

This document outlines the real-world public APIs integrated into the NagarSetu Smart City Platform to ensure data authenticity and scalability.

## 1. Weather & Disaster Management
- **API**: [OpenWeatherMap](https://openweathermap.org/)
- **Usage**: Provides real-time temperature, humidity, and weather conditions for Bhopal. 
- **Future Integration**: IMD (Indian Meteorological Department) and NDMA feeds for localized disaster alerts.

## 2. EV Charging Infrastructure
- **API**: [Open Charge Map](https://openchargemap.org/site/develop/api)
- **Usage**: Dynamically fetches EV charging stations in Bhopal. No API key is required for basic usage, ensuring high availability.
- **Integration**: Mapped to `MapMode.EV_CHARGING` in the Live City Map.

## 3. Healthcare & Clinics
- **API**: [OpenStreetMap Overpass API](https://overpass-api.de/)
- **Usage**: Fetches live locations of hospitals, clinics, and doctors near the user using Overpass QL.
- **Benefit**: Completely free and community-driven, covering even smaller local clinics.

## 4. Smart Parking (ParkEase)
- **API**: [OpenStreetMap Overpass API](https://overpass-api.de/)
- **Data**: Since real-time slot availability for Bhopal isn't publicly available via API yet, we use OSM for static lot locations and a Supabase backend for real-time slot management/booking.

## 5. Transit & Route Optimization
- **Engine**: [OSRM (Open Source Routing Machine)](http://project-osrm.org/)
- **Usage**: Used in "GreenRoute" for multi-modal routing (Bus, Cycle, Walk, Auto).
- **Note**: Bhopal City Link Ltd (BCLL) GTFS data is currently simulated based on standard routes as no live GTFS feed is publicly exposed.

## 6. Air Quality Index (AQI)
- **API**: [WAQI (World Air Quality Index)](https://waqi.info/)
- **Usage**: Aggregates data from CPCB (Central Pollution Control Board) stations in Bhopal (e.g., Arera Colony, Paryavaran Parisar).

## 7. AI & Predictive Models
- **LLMs**: Google Gemini Pro & Groq (Llama 3)
- **Usage**: Powering the AI Assistant for civic queries and RAG-based analysis of city hazardous zones.

---
*Developed for NagarSetu — Empowering Bhopal through Open Data.*
