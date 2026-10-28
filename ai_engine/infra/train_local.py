#!/usr/bin/env python3
"""
NagarSetu V2.0 — Local & Colab Headless Training Orchestrator
Executes:
  1. Multi-class Defect Detection (yolov8s.pt, 5 classes)
  2. Concrete Crack Segmentation (yolov8n-seg.pt, 1 class)
Saves exported weights under ai_engine/models/ (each < 100 MB)
"""

import os
import sys
import shutil
import argparse
from pathlib import Path

HERE = Path(__file__).resolve().parent
AI_ENGINE_DIR = HERE.parent
MODELS_DIR = AI_ENGINE_DIR / "models"
MODELS_DIR.mkdir(parents=True, exist_ok=True)

INFRA_YAML = HERE / "infra.yaml"
CRACK_SEG_YAML = HERE / "crack_seg.yaml"


def train_detection(epochs=50, imgsz=640, batch=16, model="yolov8s.pt"):
    print(f"\n=======================================================")
    print(f"  Starting Multi-Class SHM Defect Detection Training   ")
    print(f"  Model: {model} | Epochs: {epochs} | Imgsz: {imgsz}    ")
    print(f"=======================================================\n")
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] Ultralytics is not installed. Run: pip install ultralytics==8.3.40")
        return False

    yolo_model = YOLO(model)
    results = yolo_model.train(
        data=str(INFRA_YAML),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch,
        project=str(HERE / "runs" / "detect"),
        name="infra_defect_run",
        exist_ok=True
    )

    best_weight = HERE / "runs" / "detect" / "infra_defect_run" / "weights" / "best.pt"
    target_weight = MODELS_DIR / "infra_defects.pt"
    if best_weight.exists():
        shutil.copy(best_weight, target_weight)
        size_mb = target_weight.stat().st_size / (1024 * 1024)
        print(f"[OK] Exported detection weights: {target_weight} ({size_mb:.2f} MB)")
    else:
        print(f"[WARN] Training completed but best.pt not found at expected location.")
    return True


def train_segmentation(epochs=50, imgsz=640, batch=16, model="yolov8n-seg.pt"):
    print(f"\n=======================================================")
    print(f"  Starting Concrete Crack Boundary Segmentation Training")
    print(f"  Model: {model} | Epochs: {epochs} | Imgsz: {imgsz}    ")
    print(f"=======================================================\n")
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] Ultralytics is not installed. Run: pip install ultralytics==8.3.40")
        return False

    yolo_model = YOLO(model)
    results = yolo_model.train(
        data=str(CRACK_SEG_YAML),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch,
        project=str(HERE / "runs" / "segment"),
        name="crack_seg_run",
        exist_ok=True
    )

    best_weight = HERE / "runs" / "segment" / "crack_seg_run" / "weights" / "best.pt"
    target_weight = MODELS_DIR / "crack_seg.pt"
    if best_weight.exists():
        shutil.copy(best_weight, target_weight)
        size_mb = target_weight.stat().st_size / (1024 * 1024)
        print(f"[OK] Exported segmentation weights: {target_weight} ({size_mb:.2f} MB)")
    else:
        print(f"[WARN] Training completed but best.pt not found at expected location.")
    return True


def bootstrap_weights_if_missing():
    """
    Ensures infra_defects.pt and crack_seg.pt exist in ai_engine/models
    by instantiating and saving baseline weights if not already present.
    Allows subsequent evaluation and inference pipelines (Phase 2) to load weights.
    """
    detect_path = MODELS_DIR / "infra_defects.pt"
    seg_path = MODELS_DIR / "crack_seg.pt"

    if not detect_path.exists():
        print(f"[Bootstrap] Initializing baseline detection weights at {detect_path}...")
        try:
            from ultralytics import YOLO
            m = YOLO("yolov8n.pt")
            m.save(str(detect_path))
            print(f"[Bootstrap] [OK] Saved baseline infra_defects.pt")
        except Exception as e:
            # Copy existing best.pt if present
            existing = MODELS_DIR / "best.pt"
            if existing.exists():
                shutil.copy(existing, detect_path)
                print(f"[Bootstrap] [OK] Copied existing model as baseline infra_defects.pt")

    if not seg_path.exists():
        print(f"[Bootstrap] Initializing baseline segmentation weights at {seg_path}...")
        try:
            from ultralytics import YOLO
            m = YOLO("yolov8n-seg.pt")
            m.save(str(seg_path))
            print(f"[Bootstrap] [OK] Saved baseline crack_seg.pt")
        except Exception as e:
            existing = MODELS_DIR / "best.pt"
            if existing.exists():
                shutil.copy(existing, seg_path)
                print(f"[Bootstrap] [OK] Copied existing model as baseline crack_seg.pt")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="NagarSetu SHM YOLO Training")
    parser.add_argument("--task", choices=["detect", "segment", "all", "bootstrap"], default="all")
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--detect-model", default="yolov8s.pt")
    parser.add_argument("--seg-model", default="yolov8n-seg.pt")
    args = parser.parse_args()

    if args.task == "bootstrap":
        bootstrap_weights_if_missing()
        sys.exit(0)

    if args.task in ["detect", "all"]:
        train_detection(epochs=args.epochs, imgsz=args.imgsz, batch=args.batch, model=args.detect_model)

    if args.task in ["segment", "all"]:
        train_segmentation(epochs=args.epochs, imgsz=args.imgsz, batch=args.batch, model=args.seg_model)

    bootstrap_weights_if_missing()
