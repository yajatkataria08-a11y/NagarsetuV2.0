#!/usr/bin/env python3
"""
NagarSetu V2.0 — Production YOLOv8 Hazard Detection Model Training
──────────────────────────────────────────────────────────────────
Trains a custom YOLOv8 model for municipal road hazard detection:
  0: pothole
  1: waterlogging
  2: crowd_surge
  3: stalled_vehicle
  4: accident

Outputs production weights to:
  ai_engine/models/best.pt
"""

import os
import sys
import shutil
import torch
from ultralytics import YOLO

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(HERE, "models")
DATASET_YAML = os.path.join(HERE, "data", "hazard_vision", "dataset.yaml")
FINAL_WEIGHTS_PATH = os.path.join(MODELS_DIR, "best.pt")

os.makedirs(MODELS_DIR, exist_ok=True)

def train():
    device = 0 if torch.cuda.is_available() else "cpu"
    print(f"[Vision Trainer] Initializing YOLOv8 nano on compute device: {device}")
    model = YOLO("yolov8n.pt")

    # Select epochs: if GPU is available 50 epochs, on CPU use optimized 5 epochs for fast training
    epochs = 50 if torch.cuda.is_available() else 5
    imgsz = 640 if torch.cuda.is_available() else 320

    print(f"[Vision Trainer] Starting training with data={DATASET_YAML}, epochs={epochs}, imgsz={imgsz}")
    results = model.train(
        data=DATASET_YAML,
        epochs=epochs,
        imgsz=imgsz,
        batch=8,
        device=device,
        save=True,
        project=os.path.join(HERE, "runs"),
        name="municipal_hazards",
        exist_ok=True
    )

    trained_weights = os.path.join(HERE, "runs", "municipal_hazards", "weights", "best.pt")
    if os.path.exists(trained_weights):
        shutil.copy(trained_weights, FINAL_WEIGHTS_PATH)
        print(f"[Vision Trainer] Successfully saved production weights to {FINAL_WEIGHTS_PATH}")
    else:
        # Save model directly if best.pt wasn't exported
        model.save(FINAL_WEIGHTS_PATH)
        print(f"[Vision Trainer] Exported weights directly to {FINAL_WEIGHTS_PATH}")

    file_size_mb = os.path.getsize(FINAL_WEIGHTS_PATH) / (1024 * 1024)
    print(f"[Vision Trainer] Production model size: {file_size_mb:.2f} MB")

if __name__ == "__main__":
    train()
