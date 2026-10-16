#!/usr/bin/env python3
"""
NagarSetu V2 — Computer Vision & YOLO Incident Verifier (Phase 9)
Inspired by Krisis AI Vision Pipeline.
Provides automated verification of civic reports and surveillance feeds:
  - Road potholes & surface degradation
  - Flood water accumulation
  - Fire & smoke hazards
"""

import sys
import json
import os

def analyze_evidence(incident_data):
    """
    Simulates automated visual classification pipeline (YOLOv8 / OpenCV).
    Returns detection label, confidence score, and recommended municipal escalation.
    """
    itype = incident_data.get("type", "OTHER")
    desc = incident_data.get("description", "").lower()
    has_photo = bool(incident_data.get("photo_url") or incident_data.get("photos"))

    # Confidence scoring heuristics
    confidence = 0.85 if has_photo else 0.60

    if itype in ("FIRE", "SMOKE") or "fire" in desc or "smoke" in desc:
        label = "FIRE_HAZARD"
        confidence = max(confidence, 0.94)
        priority = "CRITICAL"
        escalation = "IMMEDIATE_DISPATCH_FIRE_BRIGADE"
    elif itype in ("FLOOD_RISK", "WATER_LOGGING") or "water" in desc or "drown" in desc:
        label = "FLOOD_RISK"
        confidence = max(confidence, 0.89)
        priority = "HIGH"
        escalation = "MUNICIPAL_DRAINAGE_UNIT"
    elif itype in ("POTHOLE", "ROAD_HAZARD") or "pothole" in desc or "crater" in desc:
        label = "ROAD_DEGRADATION"
        confidence = max(confidence, 0.88)
        priority = "MODERATE"
        escalation = "PWD_ROAD_REPAIR"
    elif itype == "HARASSMENT" or "suspicious" in desc or "stalk" in desc:
        label = "SAFETY_CONCERN"
        confidence = max(confidence, 0.91)
        priority = "HIGH"
        escalation = "POLICE_PATROL_NOTIFICATION"
    else:
        label = "CIVIC_MAINTENANCE"
        confidence = 0.75
        priority = "LOW"
        escalation = "STANDARD_REVIEW"

    return {
        "verified": confidence >= 0.75,
        "ai_label": label,
        "confidence": round(confidence, 2),
        "priority": priority,
        "recommended_escalation": escalation,
        "model": "NagarSetu-YOLOv8-Civic-v2"
    }

if __name__ == "__main__":
    if len(sys.argv) > 1:
        try:
            data = json.loads(sys.argv[1])
            res = analyze_evidence(data)
            print(json.dumps(res))
        except Exception as e:
            print(json.dumps({"error": str(e), "verified": False}))
    else:
        # Test run
        sample = {"type": "POTHOLE", "description": "Deep crater near TT Nagar bus stop", "has_photo": True}
        print(json.dumps(analyze_evidence(sample), indent=2))
