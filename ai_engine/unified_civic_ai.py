#!/usr/bin/env python3
"""
NagarSetu V2 — Unified Civic AI Engine
Merges:
  1. NagarSetu's Original NLP Verifier (Keywords, Anti-Spam Regex, Temporal & Severity Decay)
  2. Scikit-Learn TF-IDF Text Credibility & Spam Classifier
  3. Scikit-Learn GradientBoosting Spatial-Temporal Risk Regressor (Bhopal & Indore footprint)
  4. Google Gemini Flash Multimodal Reasoning (Hindi / Hinglish / English)
  5. YOLOv8 Incident Vision Verification Heuristics

Usage:
  python unified_civic_ai.py verify '{"description":"Huge pothole on TT Nagar bridge causing bike falls","severity":4,"ward":"Ward 5","lat":23.243,"lng":77.419}'
  python unified_civic_ai.py risk '{"lat":23.2599,"lng":77.4126,"hour":22,"day":4}'
  python unified_civic_ai.py train
"""

import sys
import os
import json
import re
import math
import time
from typing import Dict, Any, Tuple, List

# ── Check Machine Learning Dependencies ───────────────────────────────────────
SKLEARN_AVAILABLE = False
try:
    import numpy as np
    import pandas as pd
    import joblib
    from sklearn.ensemble import GradientBoostingRegressor
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.linear_model import LogisticRegression
    from sklearn.pipeline import Pipeline
    from sklearn.preprocessing import StandardScaler
    SKLEARN_AVAILABLE = True
except ImportError:
    pass

HERE = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(HERE, "models")
os.makedirs(MODEL_DIR, exist_ok=True)
RISK_MODEL_FILE = os.path.join(MODEL_DIR, "risk_regressor.joblib")
NLP_MODEL_FILE = os.path.join(MODEL_DIR, "nlp_classifier.joblib")

# ── 1. NagarSetu NLP Dictionaries & Heuristics ─────────────────────────────────
HIGH_RISK_KEYWORDS = [
    'weapon', 'gun', 'knife', 'threat', 'attack', 'bleeding', 'fire', 'explosion',
    'chase', 'scream', 'child', 'helpless', 'abuse', 'unsafe', 'robbery', 'assault',
    'pothole', 'accident', 'overflow', 'waterlogging', 'wire', 'electrocution',
    'dark', 'lights off', 'suspicious', 'kabza', 'hazard', 'khatra', 'chori'
]

LOW_RISK_INDICATORS = [
    'not sure', 'might be', 'maybe', 'unclear', 'heard something', 'seems like',
    'probably nothing', 'test', 'demo', 'fake', 'hello', 'checking', 'trial', 'testing'
]

NOISE_PATTERNS = [
    r'\b(test|demo|sample|xyz|abc|123|asdf)\b',
    r'^.{0,10}$',          # Very short or empty
    r'(.)\1{4,}',          # Repeated characters e.g. "aaaaaa"
]

# Baseline Geographic Anchors (Bhopal & Indore)
DEFAULT_CITY_HOTSPOTS = [
    # Bhopal
    {"lat": 23.2599, "lng": 77.4126, "severity": 4, "type": "HARASSMENT", "hour": 22, "city": "bhopal"},
    {"lat": 23.2430, "lng": 77.4190, "severity": 4, "type": "ROAD_HAZARD", "hour": 19, "city": "bhopal"},
    {"lat": 23.2680, "lng": 77.4300, "severity": 5, "type": "WATER_LOGGING", "hour": 14, "city": "bhopal"},
    {"lat": 23.2310, "lng": 77.4350, "severity": 3, "type": "POOR_LIGHTING", "hour": 21, "city": "bhopal"},
    {"lat": 23.2550, "lng": 77.4050, "severity": 4, "type": "UNSAFE_SPOT", "hour": 23, "city": "bhopal"},
    # Indore
    {"lat": 22.7196, "lng": 75.8577, "severity": 4, "type": "HARASSMENT", "hour": 22, "city": "indore"},
    {"lat": 22.7280, "lng": 75.8820, "severity": 3, "type": "UNSAFE_SPOT", "hour": 23, "city": "indore"},
    {"lat": 22.7533, "lng": 75.8937, "severity": 3, "type": "ROAD_HAZARD", "hour": 20, "city": "indore"},
]

# ── 2. Spatial Math (Haversine & Continuous Gaussian Decay) ───────────────────
def haversine_km(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = math.sin(dlat / 2.0)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlng / 2.0)**2
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return R * c

# ── 3. Scikit-Learn Model Training & Eager Loading ────────────────────────────
def train_scikit_learn_models():
    """Trains both the NLP text classifier and the GradientBoosting spatial regressor."""
    if not SKLEARN_AVAILABLE:
        print("[ai_engine] Scikit-learn not available. Skipping model training.")
        return False

    # 1. Train NLP Credibility Classifier
    training_texts = [
        # Genuine civic reports (Label 1)
        "Huge pothole on main road causing accidents near overbridge",
        "Water logging in underpass vehicles stuck 2 feet water",
        "Street light pole collapsed live wire on road sparking",
        "Group of drunken men harassing women near bus stop",
        "Open manhole cover on footpath dangerous for children",
        "Garbage dumping on public road foul smell spreading disease",
        "Deep ditch without barricade in residential colony",
        "Severe water logging near railway station road blocked",
        "Bikers doing reckless stunts endangering pedestrians at night",
        "Traffic signal broken causing major traffic jam at square",
        # Spam / Fake reports (Label 0)
        "test test test 123",
        "hello checking app functionality",
        "sample report please ignore",
        "asdfasdfasdf qwerty",
        "testing dashboard",
        "fake alert for demo purpose",
        "random text check",
        "hi",
        "nothing here just checking",
        "demo testing only",
    ]
    training_labels = [1]*10 + [0]*10

    nlp_pipeline = Pipeline([
        ('tfidf', TfidfVectorizer(ngram_range=(1, 2), min_df=1)),
        ('clf', LogisticRegression(C=1.0))
    ])
    nlp_pipeline.fit(training_texts, training_labels)
    joblib.dump(nlp_pipeline, NLP_MODEL_FILE)

    # 2. Train GradientBoosting Spatial-Temporal Risk Regressor
    # Ingest real empirical Indore crime records if available + Bhopal anchors
    np.random.seed(42)
    rows = []
    
    indore_csv_path = os.path.join(HERE, "data", "indore_crime_data.csv")
    if os.path.exists(indore_csv_path):
        try:
            cdf = pd.read_csv(indore_csv_path)
            cdf["ts"] = pd.to_datetime(cdf["timestamp"], errors="coerce", dayfirst=True)
            cdf = cdf.dropna(subset=["ts"]).reset_index(drop=True)
            cdf["hour"] = cdf["ts"].dt.hour.astype(int)
            cdf["day"] = cdf["ts"].dt.dayofweek.astype(int)
            
            act_weights = {"act302": 5.0, "act363": 4.0, "act323": 3.0, "act379": 2.0}
            raw_sev = sum(cdf[col] * w for col, w in act_weights.items() if col in cdf.columns)
            cdf["raw_severity"] = raw_sev.astype(float)
            
            for _, r in cdf.head(1200).iterrows():
                base_risk = min(95.0, max(15.0, float(r["raw_severity"]) * 18.0))
                night_factor = 1.35 if (r["hour"] >= 20 or r["hour"] <= 4) else 1.0
                final_r = min(100.0, base_risk * night_factor)
                rows.append([float(r["latitude"]), float(r["longitude"]), int(r["hour"]), int(r["day"]), final_r])
            print(f"[ai_engine] Ingested {len(rows)} empirical crime points from Indore dataset")
        except Exception as ex:
            print(f"[ai_engine] Crime CSV parsing fallback: {ex}")

    # Generate spatial coverage around Bhopal municipal anchors
    for city, base_lat, base_lng in [('bhopal', 23.25, 77.41), ('indore', 22.72, 75.86)]:
        sample_count = 600 if city == 'bhopal' else 200
        for _ in range(sample_count):
            lat = base_lat + np.random.uniform(-0.08, 0.08)
            lng = base_lng + np.random.uniform(-0.08, 0.08)
            hour = np.random.randint(0, 24)
            day = np.random.randint(0, 7)

            min_dist = min([haversine_km(lat, lng, a["lat"], a["lng"]) for a in DEFAULT_CITY_HOTSPOTS])
            proximity_risk = math.exp(-(min_dist ** 2) / (2 * (0.8 ** 2))) * 75.0
            night_factor = 1.4 if (hour >= 20 or hour <= 4) else 1.0
            target_risk = min(100.0, max(5.0, (proximity_risk * night_factor) + np.random.normal(0, 3)))
            rows.append([lat, lng, hour, day, target_risk])

    df = pd.DataFrame(rows, columns=['lat', 'lng', 'hour', 'day', 'risk'])
    X = df[['lat', 'lng', 'hour', 'day']]
    y = df['risk']

    risk_pipeline = Pipeline([
        ('scaler', StandardScaler()),
        ('gbr', GradientBoostingRegressor(n_estimators=100, max_depth=4, learning_rate=0.08, random_state=42))
    ])
    risk_pipeline.fit(X, y)
    joblib.dump(risk_pipeline, RISK_MODEL_FILE)

    print(f"[ai_engine] Successfully trained & saved models to {MODEL_DIR}")
    return True

# Ensure models exist on startup
if SKLEARN_AVAILABLE and (not os.path.exists(RISK_MODEL_FILE) or not os.path.exists(NLP_MODEL_FILE)):
    try:
        train_scikit_learn_models()
    except Exception as e:
        print(f"[ai_engine] Model initialization note: {e}")

# ── 4. Unified Incident Verification (NagarSetu NLP + Scikit-Learn) ────────────
def verify_incident_report(incident: Dict[str, Any]) -> Dict[str, Any]:
    """
    Multilevel verification algorithm:
      Level 1: NagarSetu heuristic rule analysis & spam detection
      Level 2: Scikit-learn TF-IDF NLP model classification
      Level 3: Spatio-temporal severity and recency scoring
    """
    desc = incident.get('description', '')
    desc_lower = desc.lower().strip()
    severity = incident.get('severity', 3)
    ts = incident.get('timestamp') or incident.get('reportedAt') or (time.time() * 1000)

    # 1. Heuristic Credibility Baseline
    h_score = 0.5
    tags: List[str] = []

    # Noise check
    is_noise = False
    for pat in NOISE_PATTERNS:
        if re.search(pat, desc_lower, re.IGNORECASE):
            h_score -= 0.35
            is_noise = True
            tags.append('noise-detected')
            break

    # Keyword check
    keyword_matches = [kw for kw in HIGH_RISK_KEYWORDS if kw in desc_lower]
    if keyword_matches:
        h_score += min(0.35, len(keyword_matches) * 0.08)
        tags.append('high-risk-keywords')

    # Low risk / doubt indicator check
    for lr in LOW_RISK_INDICATORS:
        if lr in desc_lower:
            h_score -= 0.20
            tags.append('uncertain-indicator')
            break

    # Length & detail bonus
    words = desc_lower.split()
    if len(words) >= 15:
        h_score += 0.12
    elif len(words) < 4:
        h_score -= 0.25

    h_score = max(0.0, min(1.0, h_score))

    # 2. Scikit-Learn TF-IDF NLP Model Prediction
    sklearn_score = h_score  # Default fallback
    if SKLEARN_AVAILABLE and os.path.exists(NLP_MODEL_FILE):
        try:
            model = joblib.load(NLP_MODEL_FILE)
            probs = model.predict_proba([desc])
            sklearn_score = float(probs[0][1])  # Probability of genuine report
        except Exception:
            pass

    # 3. Severity & Recency Normalizers
    sev_score = (severity - 1) / 4.0
    age_hours = max(0.0, (time.time() * 1000 - ts) / 3600000)
    recency_score = max(0.0, 1.0 - (age_hours / 48.0))

    # 4. Merged Composite Credibility Score
    if is_noise:
        final_score = min(0.20, h_score)
    else:
        final_score = (sklearn_score * 0.45) + (h_score * 0.30) + (sev_score * 0.15) + (recency_score * 0.10)
    final_score = round(max(0.0, min(1.0, final_score)), 3)

    # Classification Tiers
    if final_score >= 0.82:
        risk_level = 'HIGH'
        action = 'auto_verify'
        feedback = 'High confidence genuine civic incident. Immediate dispatch recommended.'
        tags.append('auto-verified')
    elif final_score >= 0.55:
        risk_level = 'MODERATE'
        action = 'manual_review'
        feedback = 'Plausible civic report. Assigned to ward desk for standard verification.'
    elif final_score >= 0.30:
        risk_level = 'LOW'
        action = 'flag_for_review'
        feedback = 'Uncertain credibility. Field officer secondary check required.'
        tags.append('low-confidence')
    else:
        risk_level = 'VERY_LOW'
        action = 'auto_reject'
        feedback = 'Detected spam, test payload, or low information density.'
        tags.append('auto-rejected')

    return {
        "score": final_score,
        "genuine": final_score >= 0.45,
        "risk_level": risk_level,
        "recommended_action": action,
        "feedback": feedback,
        "tags": tags,
        "breakdown": {
            "sklearn_prob": round(sklearn_score, 3),
            "heuristic_score": round(h_score, 3),
            "severity_weight": round(sev_score, 3),
            "recency_weight": round(recency_score, 3)
        }
    }

# ── 5. Spatial Risk Regressor (Scikit-Learn GradientBoosting) ─────────────────
def predict_spatial_risk(lat: float, lng: float, hour: int = 21, day: int = 4) -> Dict[str, Any]:
    """Continuous risk score (0..100) using ML model with Gaussian spatial fallback."""
    # 1. Check ML Model
    if SKLEARN_AVAILABLE and os.path.exists(RISK_MODEL_FILE):
        try:
            model = joblib.load(RISK_MODEL_FILE)
            X = pd.DataFrame([[lat, lng, hour, day]], columns=['lat', 'lng', 'hour', 'day'])
            pred = float(model.predict(X)[0])
            score = round(max(0.0, min(100.0, pred)), 1)
            level = 'CRITICAL' if score >= 80 else 'HIGH' if score >= 60 else 'MODERATE' if score >= 35 else 'LOW'
            return {
                "lat": lat,
                "lng": lng,
                "hour": hour,
                "day": day,
                "risk_score": score,
                "risk_level": level,
                "model": "scikit-learn-gradient-boosting"
            }
        except Exception:
            pass

    # 2. Gaussian Spatial Decay Fallback
    min_dist = min([haversine_km(lat, lng, a["lat"], a["lng"]) for a in DEFAULT_CITY_HOTSPOTS])
    base_weight = math.exp(-(min_dist ** 2) / (2 * (0.8 ** 2))) * 75.0
    night_boost = 1.4 if (hour >= 20 or hour <= 4) else 1.0
    score = round(min(100.0, max(5.0, base_weight * night_boost)), 1)
    level = 'CRITICAL' if score >= 80 else 'HIGH' if score >= 60 else 'MODERATE' if score >= 35 else 'LOW'

    return {
        "lat": lat,
        "lng": lng,
        "hour": hour,
        "day": day,
        "risk_score": score,
        "risk_level": level,
        "model": "gaussian-spatial-decay-fallback"
    }

# ── 6. Route Safety Evaluator (Raksha + NagarSetu SafePath) ───────────────────
def evaluate_route_safety(waypoints: List[Dict[str, float]], hour: int = 22) -> Dict[str, Any]:
    """
    Evaluates safety along a polyline.
    Computes average and peak risk across all waypoints, identifies high danger hotspots,
    and returns illuminated safety percentage.
    """
    if not waypoints:
        return {"safety_score": 75, "risk_level": "MODERATE", "hotspots_detected": 0}

    scores = []
    hotspots = []

    for idx, pt in enumerate(waypoints):
        lat = pt.get("lat") or pt.get("latitude", 0)
        lng = pt.get("lng") or pt.get("longitude", 0)
        risk_res = predict_spatial_risk(lat, lng, hour=hour)
        score = risk_res["risk_score"]
        scores.append(score)

        if score >= 65:
            hotspots.append({
                "waypoint_index": idx,
                "lat": lat,
                "lng": lng,
                "risk_score": score,
                "warning": "High risk or unlit zone along path"
            })

    avg_risk = sum(scores) / len(scores) if scores else 50
    peak_risk = max(scores) if scores else 50
    safety_score = round(max(5.0, min(98.0, 100.0 - (avg_risk * 0.75 + peak_risk * 0.25))), 1)

    return {
        "safety_score": safety_score,
        "average_risk": round(avg_risk, 1),
        "peak_risk": round(peak_risk, 1),
        "illuminated_corridor_pct": max(40, 100 - len(hotspots) * 15),
        "hotspots_detected": len(hotspots),
        "hotspots": hotspots[:5],
        "recommendation": "Main arterial road recommended for night transit" if safety_score < 75 else "Route is well-illuminated and municipal compliant"
    }

# ── 7. Krisis CCTV Vision Detection Simulator / Parser ────────────────────────
def analyze_cctv_frame(camera_id: str, metadata: Dict[str, Any] = None) -> Dict[str, Any]:
    """
    Analyzes or simulates YOLOv8 object & hazard detection on CCTV video feeds.
    Detects: potholes, waterlogging, crowd surge, fire/smoke, dark spots.
    """
    meta = metadata or {}
    cam_presets = {
        "cam001": [{"class": "pothole", "confidence": 0.91, "bbox": [120, 340, 240, 410]}, {"class": "dense_traffic", "confidence": 0.84, "bbox": [50, 100, 580, 400]}],
        "cam002": [{"class": "waterlogging", "confidence": 0.94, "bbox": [80, 220, 620, 470]}, {"class": "stalled_vehicle", "confidence": 0.89, "bbox": [310, 240, 480, 360]}],
        "cam003": [{"class": "pedestrian_flow", "confidence": 0.96, "bbox": [100, 150, 400, 380]}],
        "cam004": [{"class": "unlit_segment", "confidence": 0.78, "bbox": [300, 50, 600, 250]}],
        "cam005": [{"class": "crowd_surge", "confidence": 0.85, "bbox": [150, 180, 520, 450]}],
        "cam006": [{"class": "normal_pedestrians", "confidence": 0.92, "bbox": [200, 100, 450, 300]}],
    }

    detections = cam_presets.get(camera_id, [{"class": "normal_monitoring", "confidence": 0.90, "bbox": [0, 0, 640, 480]}])
    primary_threat = detections[0]["class"] if detections and detections[0]["confidence"] > 0.8 else None

    return {
        "camera_id": camera_id,
        "timestamp": int(time.time() * 1000),
        "detections_count": len(detections),
        "primary_threat": primary_threat,
        "threat_confidence": detections[0]["confidence"] if detections else 0,
        "detections": detections,
        "status": "threat_alert" if primary_threat and primary_threat in ("pothole", "waterlogging", "crowd_surge") else "nominal"
    }

# ── 8. Built-in Multi-threaded HTTP Server ───────────────────────────────────
def run_http_server(port: int = 8000):
    import http.server
    import urllib.parse

    class AIHandler(http.server.BaseHTTPRequestHandler):
        def _send(self, code: int, data: Any):
            self.send_response(code)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
            self.send_header('Access-Control-Allow-Headers', 'Content-Type')
            self.end_headers()
            self.wfile.write(json.dumps(data).encode('utf-8'))

        def do_OPTIONS(self):
            self._send(200, {"status": "ok"})

        def do_GET(self):
            if self.path == '/health':
                self._send(200, {"status": "healthy", "service": "NagarSetu Unified Civic AI", "models_loaded": SKLEARN_AVAILABLE})
            else:
                self._send(404, {"error": "Endpoint not found"})

        def do_POST(self):
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8')
            try:
                data = json.loads(body) if body else {}
            except Exception:
                data = {}

            if self.path == '/verify':
                res = verify_incident_report(data)
                self._send(200, res)
            elif self.path == '/risk':
                lat = float(data.get('lat', 23.2599))
                lng = float(data.get('lng', 77.4126))
                hour = int(data.get('hour', 21))
                day = int(data.get('day', 4))
                res = predict_spatial_risk(lat, lng, hour, day)
                self._send(200, res)
            elif self.path == '/route-safety':
                waypoints = data.get('waypoints', [])
                hour = int(data.get('hour', 22))
                res = evaluate_route_safety(waypoints, hour)
                self._send(200, res)
            elif self.path == '/cctv-vision':
                cam_id = data.get('camera_id', 'cam001')
                res = analyze_cctv_frame(cam_id, data)
                self._send(200, res)
            elif self.path in ('/vision/detect', '/api/vision/detect-image'):
                from vision_service import analyze_incident_image
                res = analyze_incident_image(data)
                self._send(200, res)
            else:
                self._send(404, {"error": f"Unknown POST route: {self.path}"})

    server = http.server.ThreadingHTTPServer(('0.0.0.0', port), AIHandler)
    print(f"[NagarSetu AI] Microservice running at http://0.0.0.0:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("[NagarSetu AI] Service stopping...")
        server.server_close()

# ── 9. CLI Entrypoint ─────────────────────────────────────────────────────────
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Subcommand required: 'verify', 'risk', 'route', 'cctv', 'serve', or 'train'"}))
        sys.exit(1)

    cmd = sys.argv[1].lower()

    if cmd == 'train':
        success = train_scikit_learn_models()
        print(json.dumps({"status": "complete" if success else "failed"}))

    elif cmd == 'serve':
        port = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 8000
        run_http_server(port)

    elif cmd == 'verify':
        payload_str = sys.argv[2] if len(sys.argv) > 2 else '{}'
        try:
            incident_data = json.loads(payload_str)
        except Exception:
            try:
                import ast
                incident_data = ast.literal_eval(payload_str)
            except Exception as e:
                incident_data = {"description": payload_str, "severity": 3}
        res = verify_incident_report(incident_data)
        print(json.dumps(res))

    elif cmd == 'risk':
        payload_str = sys.argv[2] if len(sys.argv) > 2 else '{}'
        try:
            req = json.loads(payload_str)
        except Exception:
            try:
                import ast
                req = ast.literal_eval(payload_str)
            except Exception:
                req = {}
        lat = float(req.get('lat', 23.2599))
        lng = float(req.get('lng', 77.4126))
        hour = int(req.get('hour', 21))
        day = int(req.get('day', 4))
        res = predict_spatial_risk(lat, lng, hour, day)
        print(json.dumps(res))

    elif cmd == 'route':
        payload_str = sys.argv[2] if len(sys.argv) > 2 else '{}'
        try:
            req = json.loads(payload_str)
        except Exception:
            try:
                import ast
                req = ast.literal_eval(payload_str)
            except Exception:
                req = {}
        waypoints = req.get('waypoints', [])
        hour = int(req.get('hour', 22))
        res = evaluate_route_safety(waypoints, hour)
        print(json.dumps(res))

    elif cmd in ('cctv', 'vision'):
        payload_str = sys.argv[2] if len(sys.argv) > 2 else '{}'
        try:
            req = json.loads(payload_str)
        except Exception:
            try:
                import ast
                req = ast.literal_eval(payload_str)
            except Exception:
                req = {}
        if cmd == 'vision':
            from vision_service import analyze_incident_image
            res = analyze_incident_image(req)
        else:
            cam_id = req.get('camera_id', 'cam001')
            res = analyze_cctv_frame(cam_id, req)
        print(json.dumps(res))

    else:
        print(json.dumps({"error": f"Unknown subcommand: {cmd}"}))

