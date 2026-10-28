import sys
import os
import pytest

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

from unified_civic_ai import verify_incident_report, predict_spatial_risk, evaluate_route_safety

def test_verify_incident_pothole():
    report = {
        "description": "Deep dangerous pothole near MP Nagar Zone 2 causing vehicle skids",
        "severity": 4,
        "ward": "Ward 3",
        "lat": 23.231,
        "lng": 77.435
    }
    result = verify_incident_report(report)
    assert "score" in result
    assert "risk_level" in result
    assert result["score"] > 0

def test_predict_spatial_risk():
    result = predict_spatial_risk(23.2599, 77.4126, hour=22, day=4)
    assert "risk_score" in result
    assert 0 <= result["risk_score"] <= 100
    assert "risk_level" in result

def test_evaluate_route_safety():
    waypoints = [
        {"lat": 23.2599, "lng": 77.4126},
        {"lat": 23.268, "lng": 77.43}
    ]
    result = evaluate_route_safety(waypoints, hour=22)
    assert "safety_score" in result
    assert "average_risk" in result

def test_server_health_fields():
    from server import app
    from fastapi.testclient import TestClient
    client = TestClient(app)
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "yolo_loaded" in data
    assert "infra_loaded" in data
    assert "seg_loaded" in data

def test_infra_detect_endpoint():
    from server import app
    from fastapi.testclient import TestClient
    client = TestClient(app)
    response = client.post("/infra/detect", json={"conf_threshold": 0.25})
    assert response.status_code == 200
    data = response.json()
    assert data["model"] == "infra_defects"
    assert "detections" in data
    assert "primary_defect" in data
    assert "highest_priority" in data

def test_infra_segment_endpoint():
    from server import app
    from fastapi.testclient import TestClient
    client = TestClient(app)
    response = client.post("/infra/segment", json={"conf_threshold": 0.25})
    assert response.status_code == 200
    data = response.json()
    assert data["model"] == "crack_seg"
    assert "polygons" in data
    assert "estimated_width_px" in data
    assert "severity" in data
