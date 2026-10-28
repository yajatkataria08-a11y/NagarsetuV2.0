#!/usr/bin/env python3
"""
NagarSetu V2.0 -- Infrastructure Defect Vision Module
------------------------------------------------------
Serves two YOLO models for bridge / pavement inspection:

  - infra_defects.pt  : 5-class detection (crack, spalling, exposed_rebar,
                          corrosion, efflorescence)  [POST /infra/detect]
  - crack_seg.pt      : 1-class segmentation (crack polygons for width/length
                          measurement)               [POST /infra/segment]

Both models are loaded once at module import time and kept in memory.
Falls back to NumPy heuristics when YOLO/OpenCV are unavailable.
"""

from __future__ import annotations

import os
import sys
import base64
import time
from typing import Any, Dict, List, Optional, Tuple

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(HERE, "models")

INFRA_MODEL_PATH = os.path.join(MODELS_DIR, "infra_defects.pt")
SEG_MODEL_PATH   = os.path.join(MODELS_DIR, "crack_seg.pt")

# --- Class maps ---------------------------------------------------------------
INFRA_CLASSES = {
    0: {"label": "crack",          "department": "PWD_BRIDGES",   "priority": "HIGH"},
    1: {"label": "spalling",       "department": "PWD_BRIDGES",   "priority": "HIGH"},
    2: {"label": "exposed_rebar",  "department": "PWD_BRIDGES",   "priority": "CRITICAL"},
    3: {"label": "corrosion",      "department": "PWD_BRIDGES",   "priority": "HIGH"},
    4: {"label": "efflorescence",  "department": "PWD_BRIDGES",   "priority": "MEDIUM"},
}
SEG_CLASSES = {
    0: {"label": "crack", "department": "PWD_ROADS", "priority": "HIGH"},
}

# --- Lazy model handles -------------------------------------------------------
try:
    from ultralytics import YOLO as _YOLO
    _YOLO_AVAILABLE = True
except Exception as e:
    _YOLO = None  # type: ignore
    _YOLO_AVAILABLE = False

_INFRA_MODEL = None
_SEG_MODEL   = None
_INFRA_LOADED = False
_SEG_LOADED   = False


def _load_models():
    """Load both models once; called from server lifespan."""
    global _INFRA_MODEL, _SEG_MODEL, _INFRA_LOADED, _SEG_LOADED

    if not _YOLO_AVAILABLE:
        print("[InfraVision] ultralytics not available — running heuristic fallback.", file=sys.stderr)
        return

    # Never treat a copy of the hazard detector as an infra model (see infra/provenance.py)
    from infra.provenance import weight_status
    infra_ws, seg_ws = weight_status("infra_defects.pt"), weight_status("crack_seg.pt")
    for _n, _ws in (("infra_defects.pt", infra_ws), ("crack_seg.pt", seg_ws)):
        if _ws["exists"] and not _ws["trained_for_task"]:
            print(f"[InfraVision] WARNING: {_n} NOT loaded - {_ws['reason']}", file=sys.stderr)

    # infra_defects model
    if infra_ws["trained_for_task"]:
        try:
            _INFRA_MODEL = _YOLO(INFRA_MODEL_PATH)
            _INFRA_LOADED = True
            print("[InfraVision] infra_defects.pt loaded OK.")
        except Exception as e:
            print(f"[InfraVision] infra_defects.pt load notice: {e}", file=sys.stderr)
    else:
        print(f"[InfraVision] infra_defects.pt not found at {INFRA_MODEL_PATH}", file=sys.stderr)

    # crack_seg model
    if seg_ws["trained_for_task"]:
        try:
            _SEG_MODEL = _YOLO(SEG_MODEL_PATH)
            _SEG_LOADED = True
            print("[InfraVision] crack_seg.pt loaded OK.")
        except Exception as e:
            print(f"[InfraVision] crack_seg.pt load notice: {e}", file=sys.stderr)
    else:
        print(f"[InfraVision] crack_seg.pt not found at {SEG_MODEL_PATH}", file=sys.stderr)


# ── Image decode helper ────────────────────────────────────────────────────────
try:
    import cv2 as _cv2
except ImportError:
    _cv2 = None  # type: ignore

try:
    from PIL import Image as _PILImage
    import io as _io
except ImportError:
    _PILImage = None  # type: ignore
    _io = None  # type: ignore


def _decode_image(image_input: str | bytes) -> Optional[np.ndarray]:
    """Decode base64 data-URI, file path, or raw bytes -> numpy BGR array."""
    try:
        if isinstance(image_input, bytes):
            if _cv2 is not None:
                arr = np.frombuffer(image_input, np.uint8)
                return _cv2.imdecode(arr, _cv2.IMREAD_COLOR)
            elif _PILImage is not None:
                img = _PILImage.open(_io.BytesIO(image_input)).convert("RGB")
                return np.array(img)

        if isinstance(image_input, str):
            if os.path.isfile(image_input):
                if _cv2 is not None:
                    return _cv2.imread(image_input)
                elif _PILImage is not None:
                    return np.array(_PILImage.open(image_input).convert("RGB"))

            # Strip data-URI prefix
            raw = image_input
            if "base64," in raw:
                raw = raw.split("base64,")[1]
            img_bytes = base64.b64decode(raw)
            if _cv2 is not None:
                arr = np.frombuffer(img_bytes, np.uint8)
                return _cv2.imdecode(arr, _cv2.IMREAD_COLOR)
            elif _PILImage is not None:
                img = _PILImage.open(_io.BytesIO(img_bytes)).convert("RGB")
                return np.array(img)
    except Exception as e:
        print(f"[InfraVision] Image decode note: {e}", file=sys.stderr)
    return None


# ── Heuristic fallback (no YOLO) ──────────────────────────────────────────────

def _heuristic_detect(img: np.ndarray) -> List[Dict[str, Any]]:
    """
    Simple NumPy heuristic: detect likely crack/spalling regions by analysing
    dark, high-contrast edges (proxy for surface damage) in grayscale.
    Returns at most 2 mock detections with bbox [x, y, w, h].
    """
    detections: List[Dict[str, Any]] = []
    h, w = img.shape[:2]
    gray = np.mean(img, axis=2) if img.ndim == 3 else img
    # Count dark pixels (surface damage proxy)
    dark_ratio = float(np.count_nonzero(gray < 80)) / (h * w + 1e-9)
    if dark_ratio > 0.05:
        detections.append({
            "label":       "crack",
            "class_id":    0,
            "confidence":  round(min(0.92, 0.55 + dark_ratio * 2.5), 2),
            "bbox":        [int(w * 0.15), int(h * 0.20), int(w * 0.55), int(h * 0.55)],
            "department":  "PWD_BRIDGES",
            "priority":    "HIGH",
        })
    # Variance-based spalling proxy
    variance = float(np.var(gray))
    if variance > 1500:
        detections.append({
            "label":       "spalling",
            "class_id":    1,
            "confidence":  round(min(0.85, 0.50 + variance / 15000), 2),
            "bbox":        [int(w * 0.35), int(h * 0.30), int(w * 0.45), int(h * 0.40)],
            "department":  "PWD_BRIDGES",
            "priority":    "HIGH",
        })
    return detections


def _heuristic_segment(img: np.ndarray) -> Tuple[List[Dict], float, float]:
    """Returns (polygons, width_px, length_px) heuristic estimate."""
    h, w = img.shape[:2]
    gray = np.mean(img, axis=2) if img.ndim == 3 else img
    dark_ratio = float(np.count_nonzero(gray < 80)) / (h * w + 1e-9)
    polygon = [
        [int(w * 0.2), int(h * 0.4)],
        [int(w * 0.8), int(h * 0.4)],
        [int(w * 0.8), int(h * 0.6)],
        [int(w * 0.2), int(h * 0.6)],
    ]
    est_width_px  = w * dark_ratio * 0.5
    est_length_px = h * dark_ratio * 1.5
    return (
        [{"polygon": polygon, "confidence": round(min(0.88, 0.5 + dark_ratio * 3), 2)}],
        round(est_width_px, 1),
        round(est_length_px, 1),
    )


# ── Public API ─────────────────────────────────────────────────────────────────

def detect_infra_defects(image_input: Optional[str | bytes], conf_threshold: float = 0.25) -> Dict[str, Any]:
    """
    Run infra_defects.pt detection on the provided image.

    Args:
        image_input: base64 data-URI string, file path, or raw bytes
        conf_threshold: minimum confidence to keep a detection

    Returns dict with keys:
        model, detections, primary_defect, highest_priority,
        defect_count, departments, fallback, latency_ms
    """
    t0 = time.perf_counter()
    detections: List[Dict[str, Any]] = []
    used_fallback = False

    img = _decode_image(image_input) if image_input else None

    if img is None:
        # Create a blank synthetic image so heuristic still returns something
        img = np.zeros((224, 224, 3), dtype=np.uint8)

    if _INFRA_MODEL is not None and _INFRA_LOADED:
        try:
            results = _INFRA_MODEL(img, conf=conf_threshold, verbose=False)
            for r in results:
                boxes = r.boxes
                if boxes is not None:
                    for box in boxes:
                        cls_id = int(box.cls[0])
                        conf   = float(box.conf[0])
                        xyxy   = [round(float(v), 1) for v in box.xyxy[0].tolist()]
                        x1, y1, x2, y2 = xyxy
                        meta   = INFRA_CLASSES.get(cls_id, {"label": f"class_{cls_id}",
                                                             "department": "PWD_BRIDGES",
                                                             "priority": "MEDIUM"})
                        detections.append({
                            "label":       meta["label"],
                            "class_id":    cls_id,
                            "confidence":  round(conf, 3),
                            "bbox":        [int(x1), int(y1), int(max(0, x2 - x1)), int(max(0, y2 - y1))],
                            "bbox_xyxy":   xyxy,
                            "department":  meta["department"],
                            "priority":    meta["priority"],
                        })
        except Exception as e:
            print(f"[InfraVision] YOLO detect error: {e}", file=sys.stderr)
            detections = _heuristic_detect(img)
            used_fallback = True
    else:
        detections = _heuristic_detect(img)
        used_fallback = True

    # Summarise
    priority_order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}
    sorted_dets = sorted(detections, key=lambda d: priority_order.get(d.get("priority", "LOW"), 3))
    primary = sorted_dets[0]["label"] if sorted_dets else "none"
    highest = sorted_dets[0]["priority"] if sorted_dets else "NONE"
    departments = list({d["department"] for d in detections})

    elapsed = round((time.perf_counter() - t0) * 1000, 2)
    return {
        "model":           "infra_defects",
        "detections":      sorted_dets,
        "primary_defect":  primary,
        "highest_priority": highest,
        "defect_count":    len(sorted_dets),
        "departments":     departments,
        "fallback":        used_fallback,
        "infra_loaded":    _INFRA_LOADED,
        "latency_ms":      elapsed,
    }


def segment_cracks(image_input: Optional[str | bytes], conf_threshold: float = 0.25) -> Dict[str, Any]:
    """
    Run crack_seg.pt segmentation on the provided image.
    Returns polygon masks, estimated crack width and length in pixels.

    Args:
        image_input: base64 data-URI, file path, or raw bytes
        conf_threshold: minimum confidence threshold

    Returns dict with keys:
        model, polygons, crack_count, estimated_width_px, estimated_length_px,
        severity, fallback, latency_ms
    """
    t0 = time.perf_counter()
    polygons: List[Dict[str, Any]] = []
    used_fallback = False
    est_width_px   = 0.0
    est_length_px  = 0.0

    img = _decode_image(image_input) if image_input else None

    if img is None:
        img = np.zeros((224, 224, 3), dtype=np.uint8)

    if _SEG_MODEL is not None and _SEG_LOADED:
        try:
            results = _SEG_MODEL(img, conf=conf_threshold, verbose=False)
            total_area = 0.0
            for r in results:
                if r.masks is not None:
                    for mask_xy, conf_t in zip(r.masks.xy, r.boxes.conf):
                        pts = [[round(float(x), 1), round(float(y), 1)] for x, y in mask_xy]
                        conf = float(conf_t)
                        # Estimate width/length from bounding box of polygon
                        if pts:
                            xs = [p[0] for p in pts]
                            ys = [p[1] for p in pts]
                            bw = max(xs) - min(xs)
                            bh = max(ys) - min(ys)
                            est_width_px  = max(est_width_px,  min(bw, bh))
                            est_length_px = max(est_length_px, max(bw, bh))
                        polygons.append({"polygon": pts, "confidence": round(conf, 3)})
        except Exception as e:
            print(f"[InfraVision] YOLO segment error: {e}", file=sys.stderr)
            polygons, est_width_px, est_length_px = _heuristic_segment(img)
            used_fallback = True
    else:
        polygons, est_width_px, est_length_px = _heuristic_segment(img)
        used_fallback = True

    # Severity heuristic based on estimated width
    if est_width_px > 50:
        severity = "CRITICAL"
    elif est_width_px > 20:
        severity = "HIGH"
    elif est_width_px > 5:
        severity = "MEDIUM"
    else:
        severity = "LOW"

    elapsed = round((time.perf_counter() - t0) * 1000, 2)
    return {
        "model":               "crack_seg",
        "polygons":            polygons,
        "crack_count":         len(polygons),
        "estimated_width_px":  round(est_width_px, 1),
        "estimated_length_px": round(est_length_px, 1),
        "severity":            severity,
        "fallback":            used_fallback,
        "seg_loaded":          _SEG_LOADED,
        "latency_ms":          elapsed,
    }


def get_load_status() -> Dict[str, bool]:
    return {"infra_loaded": _INFRA_LOADED, "seg_loaded": _SEG_LOADED}
