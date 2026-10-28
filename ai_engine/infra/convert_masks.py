#!/usr/bin/env python3
"""
Convert (image, binary-mask) pairs - Crack500 / CrackForest style - to YOLO-seg.

    python infra/convert_masks.py --images DIR --masks DIR [--out ai_engine/data/crack_seg_real]

Pairing: mask file has the same stem as the image (optionally with _mask / _gt suffix).
Splitting: 80/10/10 by PARENT IMAGE. If you pre-cropped tiles, name them
`<parent>__<tile>.ext` and every tile of a parent lands in the same split.
One YOLO-seg label line per connected component: `0 x1 y1 x2 y2 ...` (normalised).

Check each dataset's licence yourself before redistributing images or weights.
"""
from __future__ import annotations

import argparse
import random
import shutil
from collections import defaultdict
from pathlib import Path

import cv2
import numpy as np

EXTS = {".jpg", ".jpeg", ".png", ".bmp"}


def parent_id(stem: str) -> str:
    return stem.split("__")[0]


def find_mask(masks_dir: Path, stem: str):
    for suffix in ("", "_mask", "_gt", "_label"):
        for ext in EXTS:
            p = masks_dir / f"{stem}{suffix}{ext}"
            if p.exists():
                return p
    return None


def mask_to_polygons(mask: np.ndarray, min_area: float = 12.0, eps_frac: float = 0.002):
    h, w = mask.shape[:2]
    cnts, _ = cv2.findContours((mask > 127).astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    lines = []
    for c in cnts:
        if cv2.contourArea(c) < min_area and len(c) < 5:
            continue
        c = cv2.approxPolyDP(c, eps_frac * cv2.arcLength(c, True), True).reshape(-1, 2)
        if len(c) >= 3:
            coords = " ".join(f"{x / w:.6f} {y / h:.6f}" for x, y in c)
            lines.append(f"0 {coords}")
    return lines


def split_ids(ids, seed=42, ratios=(0.8, 0.1)):
    ids = sorted(ids)
    random.Random(seed).shuffle(ids)
    a, b = int(len(ids) * ratios[0]), int(len(ids) * (ratios[0] + ratios[1]))
    return {"train": set(ids[:a]), "val": set(ids[a:b]), "test": set(ids[b:])}


def convert(images: Path, masks: Path, out: Path, seed: int = 42) -> dict:
    pairs, groups = [], defaultdict(list)
    for img in sorted(images.iterdir()):
        if img.suffix.lower() in EXTS:
            m = find_mask(masks, img.stem)
            if m is not None:
                pairs.append((img, m))
                groups[parent_id(img.stem)].append(img)
    if not pairs:
        raise SystemExit("no image/mask pairs found")
    splits = split_ids(groups.keys(), seed)
    counts = {"train": 0, "val": 0, "test": 0, "labels_empty": 0}
    for img, m in pairs:
        s = next(k for k, v in splits.items() if parent_id(img.stem) in v)
        (out / "images" / s).mkdir(parents=True, exist_ok=True)
        (out / "labels" / s).mkdir(parents=True, exist_ok=True)
        shutil.copy(img, out / "images" / s / img.name)
        lines = mask_to_polygons(cv2.imread(str(m), cv2.IMREAD_GRAYSCALE))
        counts["labels_empty"] += not lines
        (out / "labels" / s / f"{img.stem}.txt").write_text("\n".join(lines))
        counts[s] += 1
    return counts


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--images", type=Path, required=True)
    ap.add_argument("--masks", type=Path, required=True)
    ap.add_argument("--out", type=Path, default=Path(__file__).resolve().parents[1] / "data" / "crack_seg_real")
    ap.add_argument("--seed", type=int, default=42)
    a = ap.parse_args()
    print(convert(a.images, a.masks, a.out, a.seed))
