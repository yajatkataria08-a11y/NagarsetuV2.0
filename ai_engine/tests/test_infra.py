"""Tests for the infrastructure-health pipeline (Phases 2-4)."""
import base64
import os
import sys

import numpy as np
import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from infra import anomaly, features, health, measure, rul, vibration_data as V  # noqa: E402
from infra.provenance import weight_status  # noqa: E402


# ---------------------------------------------------------------- measure
def _line_mask(width, length=200, h=120, w=300):
    m = np.zeros((h, w), bool)
    y0 = 60 - width // 2
    m[y0:y0 + width, 20:20 + length] = True
    return m


@pytest.mark.parametrize("width_px", [3, 7, 12, 20])
def test_known_width_within_10_percent(width_px):
    r = measure.measure_crack_mask(_line_mask(width_px), scale_mm_per_px=0.1)
    # <=1 px absolute error is the discretisation floor, 10 % relative otherwise
    assert abs(r["max_width_px"] - width_px) <= max(1.0, 0.10 * width_px)
    assert r["max_width_mm"] == pytest.approx(r["max_width_px"] * 0.1, rel=1e-2)


def test_one_pixel_line_is_not_reported_as_two_pixels():
    r = measure.measure_crack_mask(_line_mask(1), 1.0)
    assert r["max_width_px"] == pytest.approx(1.0, abs=0.01)


def test_no_scale_means_no_mm_and_no_severity():
    r = measure.measure_crack_mask(_line_mask(5))
    assert r["max_width_mm"] is None and r["severity"] is None and r["max_width_px"] > 0


def test_empty_mask():
    assert measure.measure_crack_mask(np.zeros((50, 50), bool))["found"] is False


@pytest.mark.parametrize("mm,sev", [(0.1, "hairline"), (0.3, "moderate"), (1.0, "moderate"), (1.5, "severe")])
def test_severity_bands(mm, sev):
    assert measure.severity_from_width(mm) == sev


def test_diagonal_length():
    m = np.zeros((300, 300), bool)
    for i in range(250):
        m[i:i + 4, i:i + 4] = True
    r = measure.measure_crack_mask(m, 1.0)
    assert r["length_px"] == pytest.approx(250 * np.sqrt(2), rel=0.10)


# ---------------------------------------------------------------- provenance
def test_identical_copy_of_hazard_weights_is_not_trained(tmp_path, monkeypatch):
    from infra import provenance as P
    fake = tmp_path / "best.pt"
    fake.write_bytes(os.urandom(4000))
    (tmp_path / "infra_defects.pt").write_bytes(fake.read_bytes())
    monkeypatch.setattr(P, "MODELS_DIR", str(tmp_path))
    monkeypatch.setattr(P, "HAZARD_WEIGHTS", str(fake))
    monkeypatch.setattr(P, "MANIFEST", str(tmp_path / "MANIFEST.json"))
    ws = P.weight_status("infra_defects.pt")
    assert ws["exists"] and not ws["trained_for_task"] and "placeholder" in ws["reason"]


# ---------------------------------------------------------------- RUL
def test_paris_m3_matches_closed_form():
    a0, ac, C, dsig, Y = 0.005, 0.05, 2e-12, 60.0, 1.12
    exact = rul.closed_form_cycles_m3(a0, ac, C, dsig, Y)
    num = rul._integrate_cycles(a0, ac, [C], [3.0], [dsig], Y)[0]
    assert num == pytest.approx(exact, rel=1e-3)


def test_paris_closed_form_hand_computed():
    # N = 2(a0^-1/2 - ac^-1/2) / (C (S Y sqrt(pi))^3), computed by hand:
    # a0=0.01 -> 10 ; ac=0.04 -> 5 ; diff=5 ; numerator=10
    # S*Y*sqrt(pi)=100*1*1.7724539=177.24539 ; cubed=5,568,328 ; *C(1e-12)=5.568328e-6
    # N = 10 / 5.568328e-6 = 1.79587e6
    n = rul.closed_form_cycles_m3(0.01, 0.04, 1e-12, 100.0, 1.0)
    assert n == pytest.approx(1.79587e6, rel=1e-4)


def test_rul_percentiles_ordered_and_monotonic():
    a = rul.estimate_rul(0.005)
    b = rul.estimate_rul(0.02)
    assert a["p10_years"] < a["p50_years"] < a["p90_years"]
    assert b["p50_years"] < a["p50_years"]          # longer crack -> less life
    assert rul.estimate_rul(0.06, a_c_m=0.05)["already_critical"] is True


def test_rul_rejects_bad_input():
    with pytest.raises(ValueError):
        rul.estimate_rul(-1)


# ---------------------------------------------------------------- health
def test_healthy_and_damaged_inputs_land_in_different_bands():
    good = health.health_score(health.defect_risk(0.1), health.anomaly_risk(0.6), health.rul_risk(80))
    bad = health.health_score(health.defect_risk(2.5), health.anomaly_risk(3.5), health.rul_risk(0.5))
    assert good["band"] == "good" and bad["band"] == "critical"
    assert good["score"] > 70 > 40 > bad["score"]


def test_missing_components_are_not_scored_as_healthy():
    r = health.health_score(health.defect_risk(2.5), None, None)
    assert r["components_used"] == ["defect"] and r["band"] != "good"
    assert health.health_score(None, None, None)["band"] == "unknown"


def test_band_edges():
    assert health.band(70) == "good" and health.band(69.9) == "watch"
    assert health.band(40) == "watch" and health.band(39.9) == "critical"


# ---------------------------------------------------------------- features / anomaly
def test_dominant_frequency_recovers_known_tone():
    t = np.arange(int(2 * V.FS)) / V.FS
    assert features.dominant_frequency(np.sin(2 * np.pi * 7.3 * t), V.FS) == pytest.approx(7.3, abs=0.05)


needs_model = pytest.mark.skipif(not anomaly.status()["available"], reason="anomaly model not trained")


@needs_model
def test_anomaly_separates_severe_damage_from_healthy():
    rng = np.random.default_rng(123)
    h = [anomaly.score_window(V.simulate_record(10, V.F0, V.ZETA, rng).tolist())["ratio"] for _ in range(10)]
    d = [anomaly.score_window(V.simulate_record(10, V.F0 * np.sqrt(0.7), V.ZETA * 1.6, rng).tolist())["ratio"]
         for _ in range(10)]
    assert np.median(d) > np.median(h)
    assert anomaly.score_window(V.simulate_record(10, V.F0, V.ZETA, rng).tolist())["data_source"] == "simulated"


@needs_model
def test_anomaly_rejects_short_and_nan_input():
    with pytest.raises(ValueError):
        anomaly.score_window([0.1] * 10)
    with pytest.raises(ValueError):
        anomaly.score_window([float("nan")] * 400)


def test_lstm_autoencoder_trains_and_scores():
    pytest.importorskip("torch")
    import torch
    from infra import anomaly_models as AM
    torch.manual_seed(0)
    rng = np.random.default_rng(0)
    seqs = np.stack([AM.to_sequence(w) for w in V.windows(V.simulate_record(60, V.F0, V.ZETA, rng))])
    mu, sd = float(seqs.mean()), float(seqs.std())
    model = AM.build_lstm_ae()
    x = torch.from_numpy(((seqs - mu) / sd).astype(np.float32)).unsqueeze(-1)
    opt = torch.optim.Adam(model.parameters(), lr=3e-3)
    first = None
    for _ in range(15):
        opt.zero_grad(); loss = ((model(x) - x) ** 2).mean(); loss.backward(); opt.step()
        first = first if first is not None else float(loss)
    assert float(loss) < first
    assert AM.reconstruction_errors(model, seqs, mu, sd).shape == (len(seqs),)


# ---------------------------------------------------------------- API
@pytest.fixture(scope="module")
def client():
    from fastapi import FastAPI
    from fastapi.testclient import TestClient
    from infra.api import router
    app = FastAPI()
    app.include_router(router)
    return TestClient(app)


def _crack_image_b64(width_px=4):
    import cv2
    rng = np.random.default_rng(1)
    img = np.clip(150 + rng.normal(0, 4, (300, 400)), 0, 255).astype(np.uint8)
    img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
    cv2.line(img, (30, 80), (370, 220), (25, 25, 25), width_px)
    return "data:image/png;base64," + base64.b64encode(cv2.imencode(".png", img)[1].tobytes()).decode()


def test_analyze_image_returns_width_in_mm_and_is_honest_about_models(client):
    r = client.post("/infra/analyze-image", json={"image": _crack_image_b64(4), "reference_mm": 10, "reference_px": 100})
    assert r.status_code == 200, r.text
    j = r.json()
    assert j["crack"]["found"] and j["annotated_image"].startswith("data:image/jpeg;base64,")
    assert 0.25 <= j["crack"]["max_width_mm"] <= 0.7           # true 0.4 mm
    assert j["mask_source"] in ("classical_cv", "crack_seg.pt")
    if j["detector"] == "unavailable":
        assert j["detections"] == []                             # never invent boxes


def test_analyze_image_rejects_garbage(client):
    assert client.post("/infra/analyze-image", json={"image": "not-an-image"}).status_code == 422


@needs_model
def test_health_one_request_returns_everything(client):
    rng = np.random.default_rng(5)
    body = {"image": _crack_image_b64(4), "reference_mm": 10, "reference_px": 100,
            "samples": V.simulate_record(10, V.F0, V.ZETA, rng).tolist(),
            "rul": {"crack_length_mm": 20}}
    j = client.post("/infra/health", json=body).json()
    assert j["defects"]["crack"]["found"] and "flag" in j["anomaly"]
    assert j["rul"]["p10_years"] <= j["rul"]["p50_years"] <= j["rul"]["p90_years"]
    assert j["health"]["band"] in ("good", "watch", "critical") and 0 <= j["health"]["score"] <= 100
    assert sorted(j["health"]["components_used"]) == ["anomaly", "defect", "rul"]


def test_health_with_no_inputs_is_unknown_not_good(client):
    assert client.post("/infra/health", json={}).json()["health"]["band"] == "unknown"


# ---------------------------------------------------------------- mask converter
def test_convert_masks_splits_by_parent_without_leakage(tmp_path):
    import cv2
    from pathlib import Path
    from infra import convert_masks as C
    (tmp_path / "i").mkdir(); (tmp_path / "m").mkdir()
    for p in range(10):
        for t in range(3):
            m = np.zeros((100, 100), np.uint8); cv2.line(m, (5, 5), (90, 60), 255, 3)
            cv2.imwrite(str(tmp_path / "i" / f"p{p}__t{t}.jpg"), np.full((100, 100, 3), 120, np.uint8))
            cv2.imwrite(str(tmp_path / "m" / f"p{p}__t{t}.png"), m)
    counts = C.convert(tmp_path / "i", tmp_path / "m", tmp_path / "out")
    assert counts["train"] + counts["val"] + counts["test"] == 30 and counts["labels_empty"] == 0
    seen = {}
    for s in ("train", "val", "test"):
        for f in (tmp_path / "out" / "images" / s).glob("*"):
            seen.setdefault(f.stem.split("__")[0], set()).add(s)
    assert all(len(v) == 1 for v in seen.values())
