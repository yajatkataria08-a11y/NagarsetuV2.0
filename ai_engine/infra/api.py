"""FastAPI router: /infra/analyze-image, /infra/anomaly, /infra/health."""
from __future__ import annotations

from typing import Any, Dict, List, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from infra import analyze, anomaly, health, rul
from infra.provenance import weight_status

router = APIRouter(prefix="/infra", tags=["infrastructure"])


class AnalyzeImageRequest(BaseModel):
    image: str = Field(..., description="base64 image or data URI")
    reference_mm: Optional[float] = Field(None, gt=0, description="real length of a reference object (mm)")
    reference_px: Optional[float] = Field(None, gt=0, description="that object's length in the image (px)")
    conf_threshold: float = Field(0.25, ge=0.01, le=1.0)


class AnomalyRequest(BaseModel):
    samples: List[float] = Field(..., min_length=16, max_length=200_000)
    fs: Optional[float] = Field(None, gt=0, le=20_000)


class RulParams(BaseModel):
    crack_length_mm: float = Field(..., gt=0)
    critical_length_mm: float = Field(50.0, gt=0)
    delta_sigma_mpa: float = Field(50.0, gt=0)
    cycles_per_day: float = Field(10_000.0, gt=0)


class HealthRequest(BaseModel):
    image: Optional[str] = None
    reference_mm: Optional[float] = Field(None, gt=0)
    reference_px: Optional[float] = Field(None, gt=0)
    samples: Optional[List[float]] = Field(None, min_length=16, max_length=200_000)
    fs: Optional[float] = Field(None, gt=0, le=20_000)
    rul: Optional[RulParams] = None


@router.get("/status")
async def infra_status():
    return {"infra_defects": weight_status("infra_defects.pt"), "crack_seg": weight_status("crack_seg.pt"),
            "anomaly": anomaly.status()}


@router.post("/analyze-image")
async def analyze_image(req: AnalyzeImageRequest):
    try:
        return analyze.analyze_image(req.image, req.reference_mm, req.reference_px, req.conf_threshold)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))


@router.post("/anomaly")
async def anomaly_endpoint(req: AnomalyRequest):
    try:
        return anomaly.score_window(req.samples, req.fs)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))


@router.post("/health")
async def health_endpoint(req: HealthRequest):
    """One request -> defects, anomaly flag, RUL range, health score (any subset of inputs)."""
    out: Dict[str, Any] = {"defects": None, "anomaly": None, "rul": None}
    d_risk = a_risk = r_risk = None
    try:
        max_w_mm = None
        if req.image:
            out["defects"] = analyze.analyze_image(req.image, req.reference_mm, req.reference_px)
            max_w_mm = out["defects"]["crack"]["max_width_mm"]
            d_risk = health.defect_risk(max_w_mm, out["defects"]["detections"])
        if req.samples:
            out["anomaly"] = anomaly.score_window(req.samples, req.fs)
            a_risk = health.anomaly_risk(out["anomaly"]["ratio"])
        if req.rul:
            out["rul"] = rul.estimate_rul(req.rul.crack_length_mm / 1000.0, req.rul.critical_length_mm / 1000.0,
                                          req.rul.delta_sigma_mpa, req.rul.cycles_per_day)
            r_risk = health.rul_risk(out["rul"]["p10_years"])
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    out["health"] = health.health_score(d_risk, a_risk, r_risk)
    return out
