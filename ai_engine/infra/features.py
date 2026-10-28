"""
Window features: 2 s windows -> RMS, kurtosis, dominant frequency (FFT),
spectral centroid, natural-frequency shift.

natural_freq_shift is dominant_freq minus a baseline taken from healthy training
data (stored with the model), so it is 0 for a healthy structure and negative
when stiffness is lost.
"""
from __future__ import annotations

from typing import Optional

import numpy as np
from scipy import stats

FEATURE_NAMES = ["rms", "kurtosis", "dominant_freq", "spectral_centroid", "natural_freq_shift"]
NFFT = 8192           # zero-padding: bin spacing = fs/NFFT (~0.016 Hz), not true resolution
MIN_FREQ = 0.5        # ignore DC / drift when locating the dominant peak


def _spectrum(x: np.ndarray, fs: float):
    xw = (x - x.mean()) * np.hanning(len(x))
    mag = np.abs(np.fft.rfft(xw, n=NFFT))
    freqs = np.fft.rfftfreq(NFFT, 1.0 / fs)
    return freqs, mag


def dominant_frequency(x: np.ndarray, fs: float) -> float:
    freqs, mag = _spectrum(x, fs)
    lo = int(np.searchsorted(freqs, MIN_FREQ))
    k = lo + int(np.argmax(mag[lo:]))
    if 0 < k < len(mag) - 1:  # parabolic interpolation around the peak
        a, b, c = np.log(mag[k - 1] + 1e-12), np.log(mag[k] + 1e-12), np.log(mag[k + 1] + 1e-12)
        denom = a - 2 * b + c
        if denom != 0:
            return float(freqs[k] + 0.5 * (a - c) / denom * (freqs[1] - freqs[0]))
    return float(freqs[k])


def window_features(x: np.ndarray, fs: float, baseline_freq: Optional[float] = None) -> np.ndarray:
    x = np.asarray(x, dtype=float)
    freqs, mag = _spectrum(x, fs)
    power = mag ** 2
    centroid = float((freqs * power).sum() / (power.sum() + 1e-12))
    f_dom = dominant_frequency(x, fs)
    shift = 0.0 if baseline_freq is None else f_dom - baseline_freq
    return np.array([
        float(np.sqrt(np.mean(x ** 2))),
        float(stats.kurtosis(x, fisher=False)),
        f_dom, centroid, shift,
    ])


def feature_matrix(win: np.ndarray, fs: float, baseline_freq: Optional[float] = None) -> np.ndarray:
    if len(win) == 0:
        return np.empty((0, len(FEATURE_NAMES)))
    return np.stack([window_features(w, fs, baseline_freq) for w in win])
