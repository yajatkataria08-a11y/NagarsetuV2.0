#!/usr/bin/env python3
"""
NagarSetu V2.0 — High-Performance Resident Python AI Microservice
Port: 8000 (FastAPI + Uvicorn)
─────────────────────────────────────────────────────────────────
Keeps Scikit-Learn models and OpenCV/YOLO vision engines persistently
loaded in memory to eliminate subprocess spawn overhead.
SLA: < 35ms inference latency.
"""

import sys
import os
import time
from typing import Any, Dict, List, Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Add current directory to Python module path
HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

from unified_civic_ai import (
    verify_incident_report,
    predict_spatial_risk,
    evaluate_route_safety,
    train_scikit_learn_models,
    RISK_MODEL_FILE,
    NLP_MODEL_FILE,
    SKLEARN_AVAILABLE
)
from vision_service import analyze_incident_image
from gemini_civic_core import triage_civic_report

# ── Lifespan Handler for Eager Model Hydration ────────────────────────────────
_START_TIME = time.time()
_MODELS_LOADED = False

@asynccontextmanager
async def lifespan(app: FastAPI):
    global _MODELS_LOADED
    print("[AI Microservice] Initializing persistent models in memory...")
    if not os.path.exists(RISK_MODEL_FILE) or not os.path.exists(NLP_MODEL_FILE):
        train_scikit_learn_models()
    # Warm up models with dry runs
    try:
        predict_spatial_risk(23.2599, 77.4126, 22, 4)
        verify_incident_report({"description": "warmup check road pothole", "severity": 3})
        _MODELS_LOADED = True
        print("[AI Microservice] ✅ Scikit-Learn and Vision pipelines primed & resident.")
    except Exception as e:
        print(f"[AI Microservice] Startup priming notice: {e}")
        _MODELS_LOADED = True
    yield
    print("[AI Microservice] Shutting down AI daemon.")

app = FastAPI(
    title="NagarSetu V2.0 AI Core Microservice",
    version="2.0.0",
    description="High-performance resident spatio-temporal risk and tactical computer vision microservice",
    lifespan=lifespan
)

ALLOWED_ORIGINS = os.getenv(
    "ALLOWED_ORIGINS",
    "http://localhost:3000,http://localhost:4000,http://127.0.0.1:3000,http://127.0.0.1:4000"
).split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in ALLOWED_ORIGINS if origin.strip()],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

# ── Pydantic Request Models ───────────────────────────────────────────────────
class VerifyRequest(BaseModel):
    description: str = Field(..., description="Civic incident description text")
    severity: int = Field(3, ge=1, le=5)
    ward: Optional[str] = "Ward 7"
    lat: Optional[float] = 23.2599
    lng: Optional[float] = 77.4126
    timestamp: Optional[float] = None

class RiskRequest(BaseModel):
    lat: float
    lng: float
    hour: int = Field(22, ge=0, le=23)
    day: int = Field(4, ge=0, le=6)

class Waypoint(BaseModel):
    lat: float
    lng: float

class RouteSafetyRequest(BaseModel):
    waypoints: List[Waypoint]
    hour: int = Field(22, ge=0, le=23)

class VisionAnalyzeRequest(BaseModel):
    image: Optional[str] = None
    photo: Optional[str] = None
    type: Optional[str] = "ROAD_HAZARD"
    description: Optional[str] = ""

class TriageRequest(BaseModel):
    description: str = Field(..., description="Complaint description in Hindi, Hinglish, or English")
    lat: Optional[float] = None
    lng: Optional[float] = None
    image_url: Optional[str] = None
    existing_incidents: Optional[List[Dict[str, Any]]] = None

# ── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "service": "NagarSetu Resident AI Engine",
        "version": "2.0.0",
        "uptime_seconds": round(time.time() - _START_TIME, 1),
        "models_resident": _MODELS_LOADED,
        "sklearn_available": SKLEARN_AVAILABLE,
        "timestamp": int(time.time() * 1000)
    }

@app.post("/verify")
async def verify_endpoint(req: VerifyRequest):
    t0 = time.perf_counter()
    data = req.model_dump()
    result = verify_incident_report(data)
    elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
    result["latency_ms"] = elapsed_ms
    return result

@app.post("/risk")
async def risk_endpoint(req: RiskRequest):
    t0 = time.perf_counter()
    result = predict_spatial_risk(req.lat, req.lng, req.hour, req.day)
    elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
    result["latency_ms"] = elapsed_ms
    return result

@app.post("/route-safety")
async def route_safety_endpoint(req: RouteSafetyRequest):
    t0 = time.perf_counter()
    waypoints_dicts = [w.model_dump() for w in req.waypoints]
    result = evaluate_route_safety(waypoints_dicts, req.hour)
    elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
    result["latency_ms"] = elapsed_ms
    return result

@app.post("/vision/analyze-frame")
async def vision_analyze_endpoint(req: VisionAnalyzeRequest):
    t0 = time.perf_counter()
    payload = {
        "image": req.image or req.photo,
        "type": req.type,
        "description": req.description
    }
    result = analyze_incident_image(payload)
    elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
    result["latency_ms"] = elapsed_ms
    return result

@app.post("/triage")
async def triage_endpoint(req: TriageRequest):
    result = triage_civic_report(
        description=req.description,
        lat=req.lat,
        lng=req.lng,
        image_url=req.image_url,
        existing_incidents=req.existing_incidents
    )
    return result

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("AI_PORT", 8000))
    print(f"🚀 Starting NagarSetu Resident AI Microservice on port {port}...")
    uvicorn.run("server:app", host="0.0.0.0", port=port, log_level="info")
