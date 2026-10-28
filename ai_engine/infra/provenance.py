"""
Weight provenance check.

A model file existing on disk does not mean it was trained for the task.
At the time this module was written, models/infra_defects.pt and
models/crack_seg.pt were byte-identical copies of models/best.pt (the hazard
detector). Loading them as "infra" models would silently produce nonsense
detections, so we compare content hashes and refuse to treat copies as trained.
"""
from __future__ import annotations

import hashlib
import json
import os
from typing import Dict

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(os.path.dirname(HERE), "models")
HAZARD_WEIGHTS = os.path.join(MODELS_DIR, "best.pt")
MANIFEST = os.path.join(MODELS_DIR, "MANIFEST.json")


def _sha256(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def weight_status(name: str) -> Dict[str, object]:
    """Return {path, exists, trained_for_task, reason} for models/<name>.

    A weight file counts as task-trained only if it exists AND its hash differs
    from the hazard detector's AND (when a manifest is present) the manifest
    marks it trained on a named dataset.
    """
    path = os.path.join(MODELS_DIR, name)
    if not os.path.exists(path) or os.path.getsize(path) < 1000:
        return {"path": path, "exists": False, "trained_for_task": False, "reason": "file missing"}
    digest = _sha256(path)
    if os.path.exists(HAZARD_WEIGHTS) and digest == _sha256(HAZARD_WEIGHTS):
        return {"path": path, "exists": True, "trained_for_task": False,
                "reason": "identical to best.pt (hazard detector) - placeholder copy, not trained"}
    if os.path.exists(MANIFEST):
        try:
            with open(MANIFEST, encoding="utf-8") as f:
                entry = json.load(f).get(name)
            if entry and entry.get("dataset") and entry.get("sha256") == digest:
                return {"path": path, "exists": True, "trained_for_task": True,
                        "reason": f"manifest: trained on {entry['dataset']}", "dataset": entry["dataset"]}
            return {"path": path, "exists": True, "trained_for_task": False,
                    "reason": "no matching manifest entry (add dataset + sha256 to models/MANIFEST.json)"}
        except Exception as e:  # corrupt manifest must not crash the service
            return {"path": path, "exists": True, "trained_for_task": False, "reason": f"manifest unreadable: {e}"}
    return {"path": path, "exists": True, "trained_for_task": False,
            "reason": "no models/MANIFEST.json - cannot confirm training provenance"}
