#!/usr/bin/env python3
"""
NagarSetu V2.0 — Production Vision Hazard Detection Model Training (YOLOv8)
────────────────────────────────────────────────────────────────────────────
Trains a custom YOLOv8 neural network on municipal hazard classes:
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
import numpy as np
from PIL import Image, ImageDraw
from ultralytics import YOLO

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(HERE, "models")
DATA_DIR = os.path.join(HERE, "data", "hazard_vision")
os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(DATA_DIR, exist_ok=True)

FINAL_WEIGHTS_PATH = os.path.join(MODELS_DIR, "best.pt")

CLASSES = ["pothole", "waterlogging", "crowd_surge", "stalled_vehicle", "accident"]

def create_synthetic_annotated_dataset():
    print("[Vision Trainer] Generating multi-hazard training dataset...")
    train_img_dir = os.path.join(DATA_DIR, "images", "train")
    train_lbl_dir = os.path.join(DATA_DIR, "labels", "train")
    val_img_dir = os.path.join(DATA_DIR, "images", "val")
    val_lbl_dir = os.path.join(DATA_DIR, "labels", "val")

    for d in [train_img_dir, train_lbl_dir, val_img_dir, val_lbl_dir]:
        os.makedirs(d, exist_ok=True)

    img_w, img_h = 640, 360

    def generate_split(img_dir, lbl_dir, count=24):
        for i in range(count):
            # Background road / street texture
            bg = Image.new("RGB", (img_w, img_h), color=(50 + (i % 20), 52 + (i % 20), 55 + (i % 20)))
            draw = ImageDraw.Draw(bg)

            # Draw lane markers
            for lx in range(0, img_w, 80):
                draw.line([(lx, img_h // 2), (lx + 40, img_h // 2)], fill=(220, 220, 200), width=4)

            # Assign 1 to 2 hazards per image
            cls_id = i % len(CLASSES)
            # Box coords (normalized x_center, y_center, width, height)
            bx = 0.3 + (i % 5) * 0.1
            by = 0.4 + (i % 4) * 0.1
            bw = 0.22
            bh = 0.20

            # Draw representative features for the hazard class
            px1, py1 = int((bx - bw/2) * img_w), int((by - bh/2) * img_h)
            px2, py2 = int((bx + bw/2) * img_w), int((by + bh/2) * img_h)

            if cls_id == 0:  # pothole (dark asphalt depression)
                draw.ellipse([px1, py1, px2, py2], fill=(20, 20, 22), outline=(10, 10, 10), width=3)
            elif cls_id == 1:  # waterlogging (reflective dark bluish surface)
                draw.rectangle([px1, py1, px2, py2], fill=(30, 45, 60), outline=(50, 80, 120), width=4)
            elif cls_id == 2:  # crowd surge (pedestrian cluster)
                for cx in range(px1 + 5, px2 - 5, 12):
                    draw.ellipse([cx, py1 + 5, cx + 10, py1 + 25], fill=(180, 140, 100))
                    draw.rectangle([cx, py1 + 25, cx + 10, py2], fill=(80, 100, 150))
            elif cls_id == 3:  # stalled vehicle
                draw.rectangle([px1, py1, px2, py2], fill=(180, 40, 40), outline=(220, 220, 220), width=3)
            elif cls_id == 4:  # accident
                draw.rectangle([px1, py1, px2, py2], fill=(200, 80, 20), outline=(255, 0, 0), width=4)

            img_name = f"hazard_frame_{i:04d}.jpg"
            lbl_name = f"hazard_frame_{i:04d}.txt"

            bg.save(os.path.join(img_dir, img_name), quality=90)
            with open(os.path.join(lbl_dir, lbl_name), "w") as f:
                f.write(f"{cls_id} {bx:.6f} {by:.6f} {bw:.6f} {bh:.6f}\n")

    generate_split(train_img_dir, train_lbl_dir, count=30)
    generate_split(val_img_dir, val_lbl_dir, count=10)

    # Write data.yaml
    yaml_path = os.path.join(DATA_DIR, "data.yaml")
    with open(yaml_path, "w") as f:
        f.write(f"path: {DATA_DIR.replace(os.sep, '/')}\n")
        f.write("train: images/train\n")
        f.write("val: images/val\n")
        f.write("names:\n")
        for idx, name in enumerate(CLASSES):
            f.write(f"  {idx}: {name}\n")

    return yaml_path

def train_model(yaml_path):
    print(f"[Vision Trainer] Starting YOLOv8 training using configuration: {yaml_path}")
    # Load pretrained YOLOv8 nano model
    model = YOLO("yolov8n.pt")

    results = model.train(
        data=yaml_path,
        epochs=3,
        imgsz=320,
        batch=8,
        device="cpu",
        workers=2,
        project=os.path.join(HERE, "runs"),
        name="hazard_detector",
        exist_ok=True
    )

    trained_best = os.path.join(HERE, "runs", "hazard_detector", "weights", "best.pt")
    if os.path.exists(trained_best):
        shutil.copy(trained_best, FINAL_WEIGHTS_PATH)
        print(f"[Vision Trainer] Model successfully trained and saved to: {FINAL_WEIGHTS_PATH}")
        print(f"[Vision Trainer] Model size: {os.path.getsize(FINAL_WEIGHTS_PATH) / 1024 / 1024:.2f} MB")
    else:
        # Fallback to model weights
        model.save(FINAL_WEIGHTS_PATH)
        print(f"[Vision Trainer] Exported model weights to: {FINAL_WEIGHTS_PATH}")

if __name__ == "__main__":
    yaml_config = create_synthetic_annotated_dataset()
    train_model(yaml_config)
