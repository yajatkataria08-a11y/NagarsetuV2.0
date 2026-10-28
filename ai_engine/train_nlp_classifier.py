#!/usr/bin/env python3
"""
NagarSetu V2.0 — NLP Civic Grievance Triage Classifier Training
──────────────────────────────────────────────────────────────
Trains a multi-class text triage classifier for civic grievances using
a curated municipal benchmark corpus across 6 municipal departments:
  - ROAD_HAZARD
  - WATER_SUPPLY
  - SEWAGE_OVERFLOW
  - GARBAGE_CLEANUP
  - ELECTRICAL_SAFETY
  - WOMEN_SAFETY

Methodology:
  - Dataset: Curated municipal ticket benchmark (102 domain-verified samples,
    balanced across 6 categories in bilingual English & Hindi/Hinglish).
  - Validation: Rigorous 5-Fold Stratified Cross-Validation with out-of-fold metrics.
  - Final Model: Refit on full curated benchmark corpus for maximum vocabulary coverage.

Exports:
  ai_engine/models/nlp_classifier.joblib
"""

import os
import sys
import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, accuracy_score
from sklearn.model_selection import StratifiedKFold, cross_val_score, cross_val_predict
from sklearn.pipeline import Pipeline

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(HERE, "models")
DATA_CSV = os.path.join(HERE, "data", "real_civic_complaints.csv")
MODEL_OUT = os.path.join(MODELS_DIR, "nlp_classifier.joblib")
os.makedirs(MODELS_DIR, exist_ok=True)

def train():
    print("=" * 70)
    print(" NagarSetu V2 — Civic Grievance NLP Classifier (5-Fold Stratified CV)")
    print("=" * 70)
    print(f"[NLP Trainer] Loading curated benchmark corpus from {DATA_CSV}...")
    df = pd.read_csv(DATA_CSV)

    col_text = "text"
    col_label = "category" if "category" in df.columns else "label"

    total_records = len(df)
    num_classes = df[col_label].nunique()
    print(f"[NLP Trainer] Corpus Scale: {total_records} curated civic tickets across {num_classes} departments.")
    print("[NLP Trainer] Class Distribution (Balanced Benchmark):")
    for cat, count in df[col_label].value_counts().items():
        print(f"  • {cat:18}: {count} tickets ({count/total_records*100:.1f}%)")

    # Pipeline definition
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 2), max_features=10000, sublinear_tf=True)),
        ("clf", LogisticRegression(C=1.5, max_iter=1000, class_weight="balanced", solver="lbfgs"))
    ])

    # 5-Fold Stratified Cross-Validation
    print("\n[NLP Trainer] Running 5-Fold Stratified Cross-Validation...")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    fold_scores = cross_val_score(pipeline, df[col_text], df[col_label], cv=skf, scoring="accuracy")

    for fold_idx, score in enumerate(fold_scores, 1):
        print(f"  Fold {fold_idx}: {score * 100:.2f}% accuracy")

    mean_acc = np.mean(fold_scores)
    std_acc = np.std(fold_scores)
    print(f"\n[NLP Trainer] 5-Fold Cross-Validated Accuracy: {mean_acc * 100:.2f}% (± {std_acc * 100:.2f}%)")

    # Generate Out-Of-Fold Predictions across the entire dataset
    oof_predictions = cross_val_predict(pipeline, df[col_text], df[col_label], cv=skf)
    print("\n[NLP Trainer] Out-Of-Fold Evaluation Report (All 102 Samples Evaluated as Unseen Test Folds):\n")
    print(classification_report(df[col_label], oof_predictions, digits=3))

    # Refit on 100% of the curated benchmark corpus for production inference
    print("[NLP Trainer] Refitting pipeline on 100% of benchmark corpus for deployment...")
    pipeline.fit(df[col_text], df[col_label])

    joblib.dump(pipeline, MODEL_OUT)
    print(f"[NLP Trainer] Successfully serialized model to: {MODEL_OUT}")
    print(f"[NLP Trainer] File size: {os.path.getsize(MODEL_OUT) / 1024:.2f} KB")
    print("=" * 70)

if __name__ == "__main__":
    train()
