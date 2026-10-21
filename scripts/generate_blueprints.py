#!/usr/bin/env python3
"""
NagarSetu V2.0 — Tactical Facility Blueprint Generator (Phase 4)
Renders high-contrast architectural floorplans for municipal facility overlays.
"""

import os
import cv2
import numpy as np

OUTPUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../frontend/public/floorplans"))
os.makedirs(OUTPUT_DIR, exist_ok=True)

def generate_blueprint(filename, title, floor_name, facility):
    w, h = 1000, 700
    img = np.full((h, w, 3), 12, dtype=np.uint8) # Dark tactical blue-gray

    # Grid lines
    for x in range(0, w, 40):
        cv2.line(img, (x, 0), (x, h), (24, 28, 36), 1)
    for y in range(0, h, 40):
        cv2.line(img, (0, y), (w, y), (24, 28, 36), 1)

    # Perimeter outer walls (Thick Cyan)
    cv2.rectangle(img, (80, 80), (920, 620), (0, 229, 255), 4)

    # Inner corridors
    cv2.rectangle(img, (220, 80), (380, 480), (0, 180, 220), 2)
    cv2.rectangle(img, (540, 80), (700, 480), (0, 180, 220), 2)
    cv2.rectangle(img, (80, 480), (920, 520), (0, 180, 220), 2) # Central concourse

    # Rooms & Zones
    zones = [
        ("ZONE 1A: MAIN ENTRANCE / SECURITY", (100, 540), (480, 600), (40, 60, 40)),
        ("ZONE 1B: TICKET COUNTERS", (520, 540), (900, 600), (30, 40, 60)),
        ("ROOM 101: COMMAND DESK", (100, 100), (200, 280), (50, 40, 30)),
        ("ROOM 102: CCTV SERVER ROOM", (100, 300), (200, 460), (30, 50, 50)),
        ("CENTRAL CONCOURSE / WAITING", (240, 100), (360, 460), (20, 25, 35)),
        ("PLATFORM ACCESS CORRIDOR", (560, 100), (680, 460), (20, 25, 35)),
        ("ROOM 105: FIRST AID & TRAUMA", (720, 100), (900, 280), (40, 30, 60)),
        ("ROOM 106: FIRE SAFETY GEAR", (720, 300), (900, 460), (60, 30, 30)),
    ]

    for ztitle, p1, p2, col in zones:
        cv2.rectangle(img, p1, p2, col, -1)
        cv2.rectangle(img, p1, p2, (0, 180, 220), 1)
        cv2.putText(img, ztitle, (p1[0] + 10, p1[1] + 25), cv2.FONT_HERSHEY_SIMPLEX, 0.38, (220, 220, 220), 1)

    # Tactical Markers (Fire Hydrants 🚒, Emergency Exits 🚪, Panic Buttons 🚨)
    markers = [
        ("EXIT-1", (90, 500), (0, 255, 100)),
        ("EXIT-2", (910, 500), (0, 255, 100)),
        ("FIRE HYDRANT #1", (730, 440), (0, 80, 255)),
        ("FIRE HYDRANT #2", (190, 120), (0, 80, 255)),
        ("PANIC BEACON #04", (300, 280), (0, 0, 255)),
        ("IOT SENSOR: FLOOD", (500, 500), (255, 200, 0)),
    ]

    for label, pt, col in markers:
        cv2.circle(img, pt, 8, col, -1)
        cv2.circle(img, pt, 12, col, 1)
        cv2.putText(img, label, (pt[0] + 15, pt[1] + 5), cv2.FONT_HERSHEY_SIMPLEX, 0.35, col, 1)

    # Title Banner
    cv2.rectangle(img, (0, 0), (w, 55), (18, 22, 30), -1)
    cv2.putText(img, f"FACILITY BLUEPRINT: {facility.upper()}", (25, 26), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (0, 229, 255), 2)
    cv2.putText(img, f"LEVEL: {floor_name.upper()} | MUNICIPAL EMERGENCY GIS GRID", (25, 46), cv2.FONT_HERSHEY_SIMPLEX, 0.40, (160, 170, 190), 1)

    outpath = os.path.join(OUTPUT_DIR, filename)
    cv2.imwrite(outpath, img)
    print(f"[Blueprint] Generated {outpath} ({os.path.getsize(outpath) // 1024} KB)")

if __name__ == "__main__":
    generate_blueprint("habibganj_floor_0.png", "Habibganj Concourse", "Floor 0 (Ground)", "Habibganj Transit Center")
    generate_blueprint("habibganj_floor_1.png", "Habibganj Platform", "Floor 1 (Mezzanine)", "Habibganj Transit Center")
    generate_blueprint("aiims_floor_0.png", "AIIMS Emergency Center", "Floor 0 (Trauma Wing)", "AIIMS Bhopal")
    print("All facility blueprints generated successfully.")
