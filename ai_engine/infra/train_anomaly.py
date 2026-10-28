#!/usr/bin/env python3
"""
Vibration anomaly detection training.

  * Isolation Forest on window features (baseline)
  * PyTorch LSTM autoencoder (sequence length 64, hidden 32), healthy data only
  * Threshold = 99th percentile of reconstruction error on healthy VALIDATION data
  * Metrics (ROC-AUC, F1 at that threshold, per damage level) -> infra/metrics.json

Splits are by RECORD, not by window, so overlapping windows of one record never
straddle train / val / test.

DATA: defaults to SIMULATED signals (see vibration_data.py). Every output is
tagged data_source="simulated". Run: python infra/train_anomaly.py [--epochs 40]
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import time

import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.metrics import f1_score, roc_auc_score

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import joblib  # noqa: E402

from infra import anomaly_models as AM  # noqa: E402
from infra import features as F  # noqa: E402
from infra import vibration_data as V  # noqa: E402

MODELS_DIR = os.path.join(os.path.dirname(HERE), "models")
METRICS = os.path.join(HERE, "metrics.json")


def split_records(recs, rng, frac=(0.6, 0.2, 0.2)):
    idx = rng.permutation(len(recs))
    a, b = int(frac[0] * len(recs)), int((frac[0] + frac[1]) * len(recs))
    pick = lambda ix: [recs[i] for i in ix]
    return pick(idx[:a]), pick(idx[a:b]), pick(idx[b:])


def win_of(records):
    out = [V.windows(r["x"]) for r in records]
    return np.concatenate(out) if out else np.empty((0, int(2 * V.FS)))


def best_f1_threshold_report(y, s, thr):
    pred = (s > thr).astype(int)
    return float(f1_score(y, pred)), float(pred[y == 0].mean()), float(pred[y == 1].mean())


def train_lstm(train_w, val_w, epochs, seed):
    import torch

    torch.manual_seed(seed)
    seqs = np.stack([AM.to_sequence(w) for w in train_w])
    mu, sd = float(seqs.mean()), float(seqs.std() + 1e-8)
    model = AM.build_lstm_ae()
    opt = torch.optim.Adam(model.parameters(), lr=3e-3)
    x = torch.from_numpy(((seqs - mu) / sd).astype(np.float32)).unsqueeze(-1)
    for ep in range(epochs):
        model.train()
        perm = torch.randperm(len(x))
        for i in range(0, len(x), 64):
            b = x[perm[i:i + 64]]
            opt.zero_grad()
            loss = ((model(b) - b) ** 2).mean()
            loss.backward()
            opt.step()
    return model, mu, sd


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--epochs", type=int, default=40)
    ap.add_argument("--seed", type=int, default=0)
    ap.add_argument("--source", choices=["simulated", "lanl"], default="simulated")
    args = ap.parse_args()
    if args.source == "lanl":
        V.load_lanl("")  # raises NotImplementedError until implemented
    t0 = time.time()
    rng = np.random.default_rng(args.seed)
    data = V.simulate_dataset(seed=args.seed)

    h_tr, h_va, h_te = split_records(data["healthy"], rng)
    # damaged records only ever appear in TEST; the models never see damage.
    dmg_te = data["damaged"]

    w_tr, w_va, w_te = win_of(h_tr), win_of(h_va), win_of(h_te)
    baseline_freq = float(np.median([F.dominant_frequency(w, V.FS) for w in w_tr]))
    ft = lambda w: F.feature_matrix(w, V.FS, baseline_freq)
    f_tr, f_va, f_te = ft(w_tr), ft(w_va), ft(w_te)

    # ---- Isolation Forest -------------------------------------------------
    mu_f, sd_f = f_tr.mean(0), f_tr.std(0) + 1e-9
    iso = IsolationForest(n_estimators=300, random_state=args.seed).fit((f_tr - mu_f) / sd_f)
    iso_score = lambda f: -iso.score_samples((f - mu_f) / sd_f)          # higher = more anomalous
    iso_thr = float(np.percentile(iso_score(f_va), 99))

    # ---- evaluation set ---------------------------------------------------
    levels = {}
    for lvl in V.DAMAGE_LEVELS:
        levels[lvl] = win_of([r for r in dmg_te if r["level"] == lvl])
    all_dmg = np.concatenate(list(levels.values()))
    y = np.r_[np.zeros(len(w_te)), np.ones(len(all_dmg))]

    def evaluate(score_fn, thr):
        s_h, s_d = score_fn(w_te), score_fn(all_dmg)
        s = np.r_[s_h, s_d]
        f1, fpr, tpr = best_f1_threshold_report(y, s, thr)
        per = {}
        for lvl, w in levels.items():
            sl = score_fn(w)
            per[lvl] = {"roc_auc": float(roc_auc_score(np.r_[np.zeros(len(s_h)), np.ones(len(sl))],
                                                       np.r_[s_h, sl])),
                        "recall_at_threshold": float((sl > thr).mean())}
        return {"roc_auc": float(roc_auc_score(y, s)), "f1": f1, "false_positive_rate": fpr,
                "recall": tpr, "threshold": float(thr), "per_damage_level": per}

    metrics = {
        "data_source": V.DATA_SOURCE,
        "warning": "SIMULATED damped-oscillator data. Not a result on a real structure.",
        "fs_hz": V.FS, "window_s": 2.0, "baseline_freq_hz": baseline_freq,
        "split": {"unit": "record", "healthy_train_windows": int(len(w_tr)),
                  "healthy_val_windows": int(len(w_va)), "healthy_test_windows": int(len(w_te)),
                  "damaged_test_windows": int(len(all_dmg))},
        "threshold_rule": "99th percentile of healthy validation score",
        "isolation_forest": evaluate(lambda w: iso_score(ft(w)), iso_thr),
    }
    os.makedirs(MODELS_DIR, exist_ok=True)
    bundle = {"baseline_freq": baseline_freq, "mu_f": mu_f, "sd_f": sd_f, "iso": iso,
              "iso_threshold": iso_thr, "fs": V.FS, "data_source": V.DATA_SOURCE}

    # ---- LSTM autoencoder (needs torch) -----------------------------------
    if AM.torch_available():
        import torch

        model, mu, sd = train_lstm(w_tr, w_va, args.epochs, args.seed)
        seq = lambda w: np.stack([AM.to_sequence(x) for x in w])
        lstm_score = lambda w: AM.reconstruction_errors(model, seq(w), mu, sd)
        lstm_thr = float(np.percentile(lstm_score(w_va), 99))
        metrics["lstm_autoencoder"] = evaluate(lstm_score, lstm_thr)
        metrics["lstm_autoencoder"].update({"seq_len": AM.SEQ_LEN, "hidden": AM.HIDDEN, "epochs": args.epochs})
        torch.save({"state_dict": model.state_dict(), "mu": mu, "sd": sd, "threshold": lstm_thr},
                   os.path.join(MODELS_DIR, "anomaly_lstm_ae.pt"))
    else:
        metrics["lstm_autoencoder"] = {"status": "not_run", "reason": "torch not installed in this environment"}

    joblib.dump(bundle, os.path.join(MODELS_DIR, "anomaly_iforest.joblib"))
    metrics["train_seconds"] = round(time.time() - t0, 1)
    # merge so detection metrics written by eval_detection.py are preserved
    existing = json.load(open(METRICS)) if os.path.exists(METRICS) else {}
    existing["anomaly"] = metrics
    json.dump(existing, open(METRICS, "w"), indent=2)
    print(json.dumps({k: metrics[k] for k in ("data_source", "isolation_forest", "lstm_autoencoder")}, indent=2))


if __name__ == "__main__":
    main()
