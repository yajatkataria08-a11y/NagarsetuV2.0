#!/usr/bin/env python3
"""
NagarSetu V2.0 — Surveillance Video Stream Generator (Phase 3)
Generates lightweight 30 FPS H.264/MP4 loops with tactical CCTV overlays,
timestamp counter, scanline raster, and animated simulated urban activity.
"""

import os
import cv2
import numpy as np

OUTPUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../backend/public/streams"))
os.makedirs(OUTPUT_DIR, exist_ok=True)

WIDTH, HEIGHT = 640, 360
FPS = 30
DURATION_SEC = 4
TOTAL_FRAMES = FPS * DURATION_SEC

CAMERAS = [
    {
        "id": "cam-01",
        "name": "MP NAGAR ZONE-1 CHOWK",
        "threat": None,
        "color": (255, 200, 0),
        "scene": "traffic"
    },
    {
        "id": "cam-02",
        "name": "TT NAGAR STADIUM NORTH",
        "threat": "CROWD_SURGE",
        "color": (0, 0, 255),
        "scene": "crowd"
    },
    {
        "id": "cam-03",
        "name": "HABIBGANJ UNDERPASS",
        "threat": "WATERLOGGING",
        "color": (255, 100, 0),
        "scene": "flood"
    },
    {
        "id": "cam-04",
        "name": "ARERA HILLS VIP SPINE",
        "threat": None,
        "color": (0, 255, 255),
        "scene": "arterial"
    },
    {
        "id": "cam-05",
        "name": "RAJWADA HISTORICAL PLAZA",
        "threat": None,
        "color": (0, 255, 150),
        "scene": "plaza"
    },
    {
        "id": "cam-06",
        "name": "VIJAY NAGAR SQUARE",
        "threat": None,
        "color": (255, 150, 50),
        "scene": "square"
    }
]

def render_camera_stream(cam):
    filepath = os.path.join(OUTPUT_DIR, f"{cam['id']}.mp4")
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(filepath, fourcc, FPS, (WIDTH, HEIGHT))

    if not out.isOpened():
        print(f"[CCTV-Gen] Failed to open VideoWriter for {filepath}")
        return

    print(f"[CCTV-Gen] Rendering {cam['id']}.mp4 ({cam['name']})...")

    for f in range(TOTAL_FRAMES):
        t = f / FPS
        # Dark tactical background
        frame = np.full((HEIGHT, WIDTH, 3), 18, dtype=np.uint8)

        # Subtle road pavement perspective
        cv2.fillPoly(frame, [np.array([[120, HEIGHT], [280, 160], [360, 160], [520, HEIGHT]], np.int32)], (28, 30, 34))

        # Lane markings
        dash_offset = int((t * 120) % 60)
        for y in range(160 + dash_offset, HEIGHT, 60):
            cv2.line(frame, (WIDTH // 2, y), (WIDTH // 2, min(HEIGHT, y + 25)), (180, 180, 180), 2)

        # Animated urban elements
        if cam["scene"] == "traffic":
            # Moving vehicles
            car_x = int(180 + (t * 80) % (WIDTH - 240))
            cv2.rectangle(frame, (car_x, 240), (car_x + 50, 275), (80, 120, 200), -1)
            cv2.circle(frame, (car_x + 8, 255), 4, (255, 255, 255), -1)
            cv2.circle(frame, (car_x + 42, 255), 4, (255, 255, 255), -1)

        elif cam["scene"] == "crowd":
            # Crowd anomaly blobs
            for i in range(12):
                bx = int(220 + (i * 24) + np.sin(t * 3 + i) * 10)
                by = int(220 + (i % 3) * 16 + np.cos(t * 4 + i) * 6)
                cv2.circle(frame, (bx, by), 6, (0, 0, 240), -1)
            # YOLO bounding box highlight
            cv2.rectangle(frame, (200, 195), (460, 280), (0, 0, 255), 2)
            cv2.putText(frame, "YOLO: CROWD_SURGE 91%", (200, 188), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 255), 1)

        elif cam["scene"] == "flood":
            # Water surface specular reflection
            water_poly = np.array([[100, 220], [540, 220], [600, HEIGHT], [40, HEIGHT]], np.int32)
            cv2.fillPoly(frame, [water_poly], (80, 45, 20))
            ripple_y = int(230 + (np.sin(t * 5) * 8))
            cv2.line(frame, (120, ripple_y), (520, ripple_y), (140, 80, 40), 2)
            cv2.rectangle(frame, (100, 210), (540, 330), (0, 140, 255), 2)
            cv2.putText(frame, "YOLO: WATERLOGGING 96%", (100, 200), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 140, 255), 1)

        # Scanning line effect
        scan_y = int((t * 90) % HEIGHT)
        cv2.line(frame, (0, scan_y), (WIDTH, scan_y), (255, 255, 255), 1)

        # CCTV Tactical OSD (On Screen Display)
        time_str = f"2026-10-21 22:15:{int(t):02d}.{int((t*100)%100):02d}"
        cv2.putText(frame, f"{cam['name']} [REC ●]", (18, 28), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (220, 220, 220), 1)
        cv2.putText(frame, f"{time_str} | 30 FPS", (18, HEIGHT - 16), cv2.FONT_HERSHEY_SIMPLEX, 0.40, (140, 140, 140), 1)
        cv2.putText(frame, f"NAGARSETU TACTICAL GRID | {cam['id'].upper()}", (WIDTH - 240, 28), cv2.FONT_HERSHEY_SIMPLEX, 0.38, (0, 220, 255), 1)

        out.write(frame)

    out.release()
    print(f"[CCTV-Gen] [OK] Saved: {filepath} ({os.path.getsize(filepath) // 1024} KB)")

if __name__ == "__main__":
    for c in CAMERAS:
        render_camera_stream(c)
    print("\n[CCTV-Gen] All 6 camera streams generated in backend/public/streams/")
