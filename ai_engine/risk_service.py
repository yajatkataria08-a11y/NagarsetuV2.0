#!/usr/bin/env python3
"""
NagarSetu V2 — Spatio-Temporal Risk Engine (Phase 10)
Inspired by Raksha ML model.
Maps (lat, lng, hour_of_day, day_of_week) -> 0..100 composite risk score.
Fixes the hardcoded Bhopal coordinates bug in NagarSetu v1 C++ calculator.
"""

import sys
import json
import math
import numpy as np

# Bhopal & Indore baseline anchors
DEFAULT_ANCHORS = [
    # Bhopal hotspots
    {"lat": 23.2599, "lng": 77.4126, "severity": 4, "type": "HARASSMENT", "hour": 22},
    {"lat": 23.2430, "lng": 77.4190, "severity": 3, "type": "POOR_LIGHTING", "hour": 21},
    {"lat": 23.2680, "lng": 77.4300, "severity": 5, "type": "FLOOD_RISK", "hour": 14},
    {"lat": 23.2310, "lng": 77.4350, "severity": 2, "type": "ROAD_HAZARD", "hour": 18},
    {"lat": 23.2550, "lng": 77.4050, "severity": 4, "type": "UNSAFE_SPOT", "hour": 23},
    # Indore hotspots (Raksha dataset integration)
    {"lat": 22.7196, "lng": 75.8577, "severity": 4, "type": "HARASSMENT", "hour": 22},
    {"lat": 22.7280, "lng": 75.8820, "severity": 3, "type": "UNSAFE_SPOT", "hour": 23},
    {"lat": 22.7533, "lng": 75.8937, "severity": 2, "type": "POOR_LIGHTING", "hour": 20},
]

def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = math.sin(dlat / 2.0)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlng / 2.0)**2
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return R * c

def compute_risk_score(lat, lng, hour=21, day=4, incidents=None):
    """
    Computes smooth Gaussian continuous risk score (0..100)
    Accounts for spatial proximity, time-of-day boost, and historical incidents.
    """
    dataset = incidents if incidents and len(incidents) > 0 else DEFAULT_ANCHORS
    raw_score = 0.0

    # Night time multiplier: after 20:00 (8 PM) risk increases
    night_multiplier = 1.6 if (hour >= 20 or hour <= 4) else 1.0

    for pt in dataset:
        p_lat = pt.get("lat") or pt.get("latitude", 0)
        p_lng = pt.get("lng") or pt.get("longitude", 0)
        sev = pt.get("severity", 3)
        itype = pt.get("type", "OTHER")

        dist_km = haversine_km(lat, lng, p_lat, p_lng)
        if dist_km > 3.0:
            continue

        # Gaussian spatial kernel (sigma = 0.6 km)
        spatial_weight = math.exp(-(dist_km ** 2) / (2 * (0.6 ** 2)))
        type_weight = 1.5 if itype in ("HARASSMENT", "UNSAFE_SPOT", "CRIME") else 1.0

        raw_score += spatial_weight * (sev / 5.0) * type_weight * night_multiplier * 25.0

    # Base ambient risk
    final_score = min(100.0, max(5.0, raw_score))
    return round(final_score, 1)

def generate_risk_grid(min_lat, max_lat, min_lng, max_lng, steps=15, hour=22, day=4):
    lats = np.linspace(min_lat, max_lat, steps)
    lngs = np.linspace(min_lng, max_lng, steps)
    grid = []

    for la in lats:
        for ln in lngs:
            score = compute_risk_score(float(la), float(ln), hour, day)
            grid.append({
                "lat": round(float(la), 4),
                "lng": round(float(ln), 4),
                "riskScore": score,
                "level": "CRITICAL" if score >= 70 else "HIGH" if score >= 45 else "MODERATE" if score >= 20 else "LOW"
            })

    return grid

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--grid":
        # Default Bhopal grid
        grid = generate_risk_grid(23.20, 23.30, 77.38, 77.48, steps=12)
        print(json.dumps({"city": "Bhopal", "cells": grid}))
    elif len(sys.argv) >= 3:
        lat = float(sys.argv[1])
        lng = float(sys.argv[2])
        score = compute_risk_score(lat, lng)
        print(json.dumps({"lat": lat, "lng": lng, "riskScore": score}))
    else:
        # Self-test
        sample_score = compute_risk_score(23.2599, 77.4126, hour=23)
        print(json.dumps({"status": "ok", "sample_score_bhopal_mp_nagar": sample_score}))
