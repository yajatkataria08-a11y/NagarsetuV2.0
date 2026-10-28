"""
Image -> defects + crack widths + severity + annotated image.

Honesty rules:
  * Boxes come only from a detector whose weights pass infra.provenance.
    Otherwise `detections` is empty and `detector` says why. We never emit the
    hard-coded boxes from infra_vision._heuristic_detect.
  * The crack mask comes from crack_seg.pt when genuinely trained, else from a
    classical black-hat segmenter. `mask_source` always says which.
"""
from __future__ import annotations

import base64
from typing import Any, Dict, List, Optional

import numpy as np

import infra_vision
from infra import measure
from infra.provenance import weight_status


def _decode(image: str) -> Optional[np.ndarray]:
    return infra_vision._decode_image(image)


def _seg_model_mask(img: np.ndarray, conf: float) -> Optional[np.ndarray]:
    import cv2

    model = getattr(infra_vision, "_SEG_MODEL", None)
    if model is None or not infra_vision._SEG_LOADED:
        return None
    try:
        mask = np.zeros(img.shape[:2], np.uint8)
        for r in model(img, conf=conf, verbose=False):
            if r.masks is not None:
                for poly in r.masks.xy:
                    pts = np.round(np.asarray(poly)).astype(np.int32)
                    if len(pts) >= 3:
                        cv2.fillPoly(mask, [pts], 1)
        return mask.astype(bool)
    except Exception as e:  # model failure -> fall back, but say so via mask_source
        print(f"[infra.analyze] seg model error: {e}")
        return None


def _detections(img: np.ndarray, conf: float) -> Dict[str, Any]:
    if not infra_vision._INFRA_LOADED:
        ws = weight_status("infra_defects.pt")
        return {"detector": "unavailable", "detector_reason": ws["reason"], "detections": []}
    res = infra_vision.detect_infra_defects(img_to_bytes(img), conf_threshold=conf)
    return {"detector": "infra_defects.pt", "detector_reason": None, "detections": res["detections"]}


def img_to_bytes(img: np.ndarray) -> bytes:
    import cv2

    ok, buf = cv2.imencode(".png", img)
    return buf.tobytes()


def annotate(img: np.ndarray, mask: np.ndarray, detections: List[Dict[str, Any]]) -> str:
    """Return a base64 JPEG data-URI with mask overlay, skeleton and boxes."""
    import cv2
    from skimage.morphology import skeletonize

    out = img.copy()
    if mask.any():
        overlay = out.copy()
        overlay[mask] = (0, 0, 255)
        out = cv2.addWeighted(overlay, 0.45, out, 0.55, 0)
        out[skeletonize(mask)] = (0, 255, 255)
    for d in detections:
        x, y, w, h = d["bbox"]
        cv2.rectangle(out, (x, y), (x + w, y + h), (0, 200, 0), 2)
        cv2.putText(out, f'{d["label"]} {d["confidence"]:.2f}', (x, max(12, y - 4)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 200, 0), 1, cv2.LINE_AA)
    ok, buf = cv2.imencode(".jpg", out, [cv2.IMWRITE_JPEG_QUALITY, 85])
    return "data:image/jpeg;base64," + base64.b64encode(buf.tobytes()).decode()


def analyze_image(image: str, reference_mm: Optional[float] = None, reference_px: Optional[float] = None,
                  conf_threshold: float = 0.25) -> Dict[str, Any]:
    import cv2

    img = _decode(image)
    if img is None:
        raise ValueError("could not decode image")
    det = _detections(img, conf_threshold)

    mask = _seg_model_mask(img, conf_threshold)
    mask_source = "crack_seg.pt"
    if mask is None:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        mask = measure.classical_crack_mask(gray)
        mask_source = "classical_cv"

    scale = measure.mm_per_px(reference_mm, reference_px)
    m = measure.measure_crack_mask(mask, scale)
    notes = []
    if scale is None:
        notes.append("No reference scale supplied: widths are in pixels only and severity is not assigned.")
    if mask_source == "classical_cv":
        notes.append("Crack mask from a classical (non-learned) segmenter; crack_seg.pt is not a trained model.")
    if not measure.THRESHOLDS_VERIFIED:
        notes.append("Severity thresholds (0.3 / 1.0 mm) are unverified against IS 456; check before citing.")
    return {
        "image_size": {"width": int(img.shape[1]), "height": int(img.shape[0])},
        "detector": det["detector"], "detector_reason": det["detector_reason"],
        "detections": det["detections"],
        "mask_source": mask_source,
        "crack": m,
        "severity": m["severity"],
        "annotated_image": annotate(img, mask, det["detections"]),
        "notes": notes,
    }
