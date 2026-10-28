"""
Vibration data for the anomaly pipeline.

!!! SIMULATED DATA !!!
The LANL 3-story frame and Z24 bridge datasets could not be downloaded in the
environment this module was developed in. Everything produced by `simulate_*`
is a synthetic damped-oscillator signal and every artefact trained on it
carries data_source="simulated". Do not present results from it as results on
real structures. To use real data, implement `load_lanl()` (stub below) and
re-run train_anomaly.py with --source lanl.

Model: one dominant mode, x'' + 2*zeta*w*x' + w^2*x = broadband noise.
Damage = stiffness loss k -> natural frequency f*sqrt(1-k), damping increase.
Healthy records vary f0 by +-2 % (stand-in for temperature effects) so a small
frequency shift is NOT trivially separable - small damage levels are hard on
purpose, and the evaluation reports them per level.
"""
from __future__ import annotations

from typing import Dict, List, Tuple

import numpy as np
from scipy import signal

FS = 128.0            # Hz
F0 = 5.0              # Hz, healthy natural frequency of the simulated mode
ZETA = 0.02
DATA_SOURCE = "simulated"
DAMAGE_LEVELS = {"D1_k5": 0.05, "D2_k10": 0.10, "D3_k20": 0.20, "D4_k30": 0.30}


def simulate_record(duration_s: float, f0: float, zeta: float, rng: np.random.Generator,
                    fs: float = FS, sensor_noise: float = 0.05) -> np.ndarray:
    """One acceleration-like record from a noise-driven damped oscillator."""
    n = int(duration_s * fs)
    w = 2 * np.pi * f0
    # H(s) = w^2 / (s^2 + 2 zeta w s + w^2), discretised with the bilinear transform
    b, a = signal.bilinear([w ** 2], [1.0, 2 * zeta * w, w ** 2], fs)
    burn = int(5 * fs)
    drive = rng.standard_normal(n + burn)
    x = signal.lfilter(b, a, drive)[burn:]
    x = x / 0.9  # fixed gain so healthy RMS is ~O(1); NOT re-normalised per record
    return x + sensor_noise * rng.standard_normal(n)


def simulate_dataset(n_healthy_records: int = 30, n_damaged_per_level: int = 8,
                     duration_s: float = 60.0, seed: int = 0
                     ) -> Dict[str, List[Dict[str, object]]]:
    rng = np.random.default_rng(seed)
    recs: Dict[str, List[Dict[str, object]]] = {"healthy": [], "damaged": []}
    for i in range(n_healthy_records):
        f0 = F0 * (1 + rng.uniform(-0.02, 0.02))
        recs["healthy"].append({"id": f"H{i:03d}", "level": "healthy",
                                "x": simulate_record(duration_s, f0, ZETA * rng.uniform(0.9, 1.1), rng)})
    for lvl, k in DAMAGE_LEVELS.items():
        for i in range(n_damaged_per_level):
            f0 = F0 * (1 + rng.uniform(-0.02, 0.02)) * np.sqrt(1 - k)
            z = ZETA * (1 + 2.0 * k) * rng.uniform(0.9, 1.1)
            recs["damaged"].append({"id": f"{lvl}_{i:02d}", "level": lvl,
                                    "x": simulate_record(duration_s, f0, z, rng)})
    return recs


def load_lanl(path: str):  # pragma: no cover - intentionally unimplemented
    raise NotImplementedError(
        "LANL 3-story frame loader not implemented. Download from the LANL SHM data "
        "repository, then parse per-state channel files here and return the same "
        "{'healthy': [...], 'damaged': [...]} structure as simulate_dataset().")


def windows(x: np.ndarray, fs: float = FS, win_s: float = 2.0, hop_s: float = 1.0) -> np.ndarray:
    w, h = int(win_s * fs), int(hop_s * fs)
    if len(x) < w:
        return np.empty((0, w))
    idx = np.arange(0, len(x) - w + 1, h)
    return np.stack([x[i:i + w] for i in idx])
