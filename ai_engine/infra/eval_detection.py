#!/usr/bin/env python3
"""
Held-out detection/segmentation metrics -> infra/metrics.json (mAP50, precision, recall per class).

    python infra/eval_detection.py --task detect  --weights models/infra_defects.pt --data infra/infra.yaml --dataset-label "CODEBRIM (real)"
    python infra/eval_detection.py --task segment --weights models/crack_seg.pt     --data infra/crack_seg.yaml --dataset-label "Crack500 (real)"

Refuses weights that are byte-identical to the hazard detector, and records which
dataset the numbers came from. Evaluates the TEST split (never the training data).
Box metrics are reported for both tasks; for segmentation also see r.seg in ultralytics.
"""
import argparse, hashlib, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
from infra.provenance import HAZARD_WEIGHTS  # noqa: E402


def sha(p):
    return hashlib.sha256(open(p, "rb").read()).hexdigest()


ap = argparse.ArgumentParser()
ap.add_argument("--task", choices=["detect", "segment"], required=True)
ap.add_argument("--weights", required=True)
ap.add_argument("--data", required=True)
ap.add_argument("--dataset-label", required=True, help='e.g. "CODEBRIM (real)" or "synthetic"')
ap.add_argument("--split", default="test")
a = ap.parse_args()
if os.path.exists(HAZARD_WEIGHTS) and sha(a.weights) == sha(HAZARD_WEIGHTS):
    sys.exit("REFUSING: weights are identical to best.pt (hazard detector); nothing was trained.")
from ultralytics import YOLO  # noqa: E402

r = YOLO(a.weights).val(data=a.data, split=a.split, verbose=False)
per = {r.names[int(ci)]: {"precision": float(r.box.p[i]), "recall": float(r.box.r[i]), "mAP50": float(r.box.ap50[i])}
       for i, ci in enumerate(r.box.ap_class_index)}
res = {"dataset": a.dataset_label, "split": a.split, "weights_sha256": sha(a.weights),
       "overall": {"precision": float(r.box.mp), "recall": float(r.box.mr), "mAP50": float(r.box.map50)},
       "per_class": per}
mp = os.path.join(HERE, "metrics.json")
m = json.load(open(mp)) if os.path.exists(mp) else {}
m["detection" if a.task == "detect" else "segmentation"] = res
json.dump(m, open(mp, "w"), indent=2)
print(json.dumps(res, indent=2))
