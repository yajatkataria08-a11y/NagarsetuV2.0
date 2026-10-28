"""Anomaly inference: window of samples -> {score, threshold, flag}."""
from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

import numpy as np

from infra import anomaly_models as AM
from infra import features as F

MODELS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models")
_IFOREST = os.path.join(MODELS_DIR, "anomaly_iforest.joblib")
_LSTM = os.path.join(MODELS_DIR, "anomaly_lstm_ae.pt")
_cache: Dict[str, Any] = {}


def _load():
    if "bundle" in _cache:
        return _cache
    import joblib

    _cache["bundle"] = joblib.load(_IFOREST) if os.path.exists(_IFOREST) else None
    _cache["lstm"] = None
    if _cache["bundle"] is not None and os.path.exists(_LSTM) and AM.torch_available():
        import torch

        ck = torch.load(_LSTM, map_location="cpu", weights_only=False)
        m = AM.build_lstm_ae()
        m.load_state_dict(ck["state_dict"])
        _cache["lstm"] = (m, ck["mu"], ck["sd"], ck["threshold"])
    return _cache


def status() -> Dict[str, Any]:
    c = _load()
    b = c["bundle"]
    return {"available": b is not None, "lstm_loaded": c["lstm"] is not None,
            "data_source": b["data_source"] if b else None}


def score_window(samples: List[float], fs: Optional[float] = None) -> Dict[str, Any]:
    c = _load()
    b = c["bundle"]
    if b is None:
        raise RuntimeError("anomaly model not trained: run ai_engine/infra/train_anomaly.py")
    fs = float(fs or b["fs"])
    x = np.asarray(samples, dtype=float)
    if len(x) < int(1.0 * fs):
        raise ValueError(f"need at least {int(fs)} samples (1 s) at fs={fs}; got {len(x)}")
    if not np.all(np.isfinite(x)):
        raise ValueError("samples contain NaN/inf")
    win = int(2.0 * fs)
    ws = [x[i:i + win] for i in range(0, max(1, len(x) - win + 1), win)] if len(x) >= win else [x]
    feats = F.feature_matrix(np.stack([w for w in ws if len(w) == len(ws[0])]), fs, b["baseline_freq"])
    s = -b["iso"].score_samples((feats - b["mu_f"]) / b["sd_f"])
    score = float(np.max(s))                 # worst window drives the flag
    out = {
        "model": "isolation_forest", "score": round(score, 4), "threshold": round(b["iso_threshold"], 4),
        "ratio": round(score / b["iso_threshold"], 3), "flag": bool(score > b["iso_threshold"]),
        "windows_scored": int(len(s)), "features": dict(zip(F.FEATURE_NAMES, np.round(feats.mean(0), 4).tolist())),
        "data_source": b["data_source"],
    }
    if c["lstm"] is not None and abs(fs - b["fs"]) < 1e-6 and len(ws[0]) >= AM.SEQ_LEN:
        m, mu, sd, thr = c["lstm"]
        e = AM.reconstruction_errors(m, np.stack([AM.to_sequence(w) for w in ws]), mu, sd)
        out["lstm"] = {"score": round(float(e.max()), 5), "threshold": round(float(thr), 5),
                       "flag": bool(e.max() > thr)}
    if b["data_source"] == "simulated":
        out["warning"] = "Model trained on SIMULATED vibration data; scores are not validated on real structures."
    return out
