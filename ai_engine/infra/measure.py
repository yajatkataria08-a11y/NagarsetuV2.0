"""
Crack geometry from a binary mask.

    skeletonize(mask) -> centreline
    distance_transform_edt(mask) sampled on the centreline -> half-width
    width_px = 2*d - 1   (pixel-centre correction, see below)
    mm = px * mm_per_px, with mm_per_px = reference_mm / reference_px

Deviation from "width = 2 x dt": on a discrete grid a 1-px-wide line has d = 1,
so 2*d reports 2 px (100 % error). Distances are measured between pixel
centres, the true edge is half a pixel closer, so width = 2*(d - 0.5) = 2d - 1.
That is exact for odd widths and at most 1 px low for even widths.
"""
from __future__ import annotations

from typing import Dict, Optional

import numpy as np
from scipy import ndimage as ndi
from skimage.morphology import skeletonize

# Crack-width severity bands in mm. UNVERIFIED against IS 456:2000 - check
# cl. 35.3.2 in your own copy before citing 0.3 mm in any report. Exposure
# condition changes the permissible limit.
SEVERITY_BANDS_MM = {"hairline_max": 0.3, "moderate_max": 1.0}
THRESHOLDS_VERIFIED = False


def mm_per_px(reference_mm: Optional[float], reference_px: Optional[float]) -> Optional[float]:
    if reference_mm and reference_px and reference_mm > 0 and reference_px > 0:
        return float(reference_mm) / float(reference_px)
    return None


def severity_from_width(max_width_mm: Optional[float]) -> Optional[str]:
    if max_width_mm is None:
        return None
    if max_width_mm < SEVERITY_BANDS_MM["hairline_max"]:
        return "hairline"
    if max_width_mm <= SEVERITY_BANDS_MM["moderate_max"]:
        return "moderate"
    return "severe"


def _skeleton_length_px(skel: np.ndarray) -> float:
    """Length of a 1-px skeleton: orthogonal links = 1, diagonal links = sqrt(2)."""
    s = skel.astype(bool)
    ortho = np.count_nonzero(s[:, 1:] & s[:, :-1]) + np.count_nonzero(s[1:, :] & s[:-1, :])
    # A diagonal link only counts when the two pixels are not already joined
    # through an orthogonal neighbour (avoids double counting at corners).
    d1 = s[1:, 1:] & s[:-1, :-1] & ~(s[1:, :-1] | s[:-1, 1:])
    d2 = s[1:, :-1] & s[:-1, 1:] & ~(s[1:, 1:] | s[:-1, :-1])
    diag = np.count_nonzero(d1) + np.count_nonzero(d2)
    return float(ortho + np.sqrt(2.0) * diag)


def measure_crack_mask(mask: np.ndarray, scale_mm_per_px: Optional[float] = None,
                       min_component_px: int = 20) -> Dict[str, object]:
    """Measure length and width of every crack in a binary mask.

    Returns pixel values always; mm values and severity only when a scale is given.
    """
    m = np.asarray(mask).astype(bool)
    if m.ndim != 2:
        raise ValueError("mask must be 2-D")
    labels, n = ndi.label(m, structure=np.ones((3, 3)))
    if n:
        sizes = ndi.sum(m, labels, index=np.arange(1, n + 1))
        for i, sz in enumerate(sizes, start=1):
            if sz < min_component_px:
                m[labels == i] = False
    if not m.any():
        return {"found": False, "length_px": 0.0, "max_width_px": 0.0, "mean_width_px": 0.0,
                "mm_per_px": scale_mm_per_px, "length_mm": None, "max_width_mm": None,
                "mean_width_mm": None, "severity": None, "components": 0}

    skel = skeletonize(m)
    dt = ndi.distance_transform_edt(m)
    widths_px = np.maximum(2.0 * dt[skel] - 1.0, 1.0)
    length_px = _skeleton_length_px(skel)
    _, comps = ndi.label(m, structure=np.ones((3, 3)))

    out: Dict[str, object] = {
        "found": True,
        "length_px": round(length_px, 2),
        "max_width_px": round(float(widths_px.max()), 2),
        "mean_width_px": round(float(widths_px.mean()), 2),
        "mm_per_px": scale_mm_per_px,
        "components": int(comps),
        "length_mm": None, "max_width_mm": None, "mean_width_mm": None, "severity": None,
    }
    if scale_mm_per_px:
        out["length_mm"] = round(length_px * scale_mm_per_px, 2)
        out["max_width_mm"] = round(float(widths_px.max()) * scale_mm_per_px, 3)
        out["mean_width_mm"] = round(float(widths_px.mean()) * scale_mm_per_px, 3)
        out["severity"] = severity_from_width(out["max_width_mm"])
    return out


def classical_crack_mask(gray: np.ndarray) -> np.ndarray:
    """Non-learned fallback: cracks are thin and darker than their surroundings.

    Black-hat morphology isolates dark structures *narrower than its kernel*, so a
    single kernel misses wide cracks. We take the maximum response over several
    kernel sizes (up to ~1/6 of the image) and threshold with Otsu. Large dark
    regions (shadows, holes) can still produce false positives - this is a
    stand-in until crack_seg.pt is genuinely trained; responses that use it are
    labelled mask_source='classical_cv'.
    """
    import cv2

    g = cv2.GaussianBlur(gray.astype(np.uint8), (3, 3), 0)
    kmax = max(9, min(g.shape) // 6)
    sizes = sorted({k | 1 for k in (9, 15, 25, 41, 65) if k <= kmax} or {9})
    bh = np.zeros_like(g)
    for k in sizes:
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k, k))
        bh = np.maximum(bh, cv2.morphologyEx(g, cv2.MORPH_BLACKHAT, kernel))
    if bh.max() < 12:  # nothing darker than its surroundings by a meaningful margin
        return np.zeros_like(g, dtype=bool)
    t, bw = cv2.threshold(bh, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    if t < 12:
        _, bw = cv2.threshold(bh, 12, 255, cv2.THRESH_BINARY)
    bw = cv2.morphologyEx(bw, cv2.MORPH_OPEN, np.ones((2, 2), np.uint8))
    return bw.astype(bool)
