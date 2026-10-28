"""
Structural health score.

    score = 100 - (0.40*defect_risk + 0.35*anomaly_risk + 0.25*rul_risk)

Each *_risk is 0-100 (higher = worse). If a component has no input, the weights
of the remaining components are renormalised and `components_used` says so - a
missing measurement is never silently scored as "healthy".
Bands: >=70 good, 40-70 watch, <40 critical.
"""
from __future__ import annotations

from typing import Any, Dict, Optional

import numpy as np

WEIGHTS = {"defect": 0.40, "anomaly": 0.35, "rul": 0.25}
# risk contributed by non-crack defect classes (0-100); judgement values, adjust with an engineer
CLASS_RISK = {"exposed_rebar": 80.0, "spalling": 60.0, "corrosion": 55.0, "efflorescence": 25.0, "crack": 0.0}


def band(score: float) -> str:
    return "good" if score >= 70 else ("watch" if score >= 40 else "critical")


def defect_risk(max_width_mm: Optional[float] = None, detections: Optional[list] = None) -> float:
    """Crack width -> risk by piecewise-linear map (0 mm:0, 0.3:35, 1.0:70, >=2.0:100);
    other defect classes contribute CLASS_RISK * confidence. Takes the worst."""
    risks = [0.0]
    if max_width_mm is not None:
        risks.append(float(np.interp(max_width_mm, [0, 0.3, 1.0, 2.0], [0, 35, 70, 100])))
    for d in detections or []:
        risks.append(CLASS_RISK.get(d.get("label"), 30.0) * float(d.get("confidence", 1.0)))
    return float(min(100.0, max(risks)))


def anomaly_risk(ratio: Optional[float]) -> Optional[float]:
    """ratio = score/threshold. <=1 -> 0-30; 1..3 -> 30-100; >=3 -> 100."""
    if ratio is None:
        return None
    return float(np.interp(ratio, [0, 1, 3], [0, 30, 100]))


def rul_risk(p10_years: Optional[float]) -> Optional[float]:
    """P10 life: >=50 y -> 0, <=1 y -> 100, log-linear between."""
    if p10_years is None:
        return None
    if p10_years <= 1:
        return 100.0
    return float(np.clip(100.0 * (1 - np.log(p10_years) / np.log(50.0)), 0.0, 100.0))


def health_score(defect: Optional[float], anomaly: Optional[float], rul: Optional[float]) -> Dict[str, Any]:
    comps = {"defect": defect, "anomaly": anomaly, "rul": rul}
    used = {k: v for k, v in comps.items() if v is not None}
    if not used:
        return {"score": None, "band": "unknown", "components": comps, "components_used": []}
    wsum = sum(WEIGHTS[k] for k in used)
    risk = sum(WEIGHTS[k] * v for k, v in used.items()) / wsum
    score = float(np.clip(100.0 - risk, 0.0, 100.0))
    return {"score": round(score, 1), "band": band(score),
            "components": {k: (None if v is None else round(v, 1)) for k, v in comps.items()},
            "components_used": sorted(used), "weights": {k: round(WEIGHTS[k] / wsum, 3) for k in used}}
