#!/usr/bin/env python3
"""
NagarSetu V2.0 — Production NLP Civic Grievance Triage Classifier Training
──────────────────────────────────────────────────────────────────────────
Trains a high-accuracy, multi-class text triage classifier for civic grievances
using real-world citizen grievance data:
  - ROAD_HAZARD
  - WATER_SUPPLY
  - SEWAGE_OVERFLOW
  - GARBAGE_CLEANUP
  - ELECTRICAL_SAFETY
  - WOMEN_SAFETY

Exports:
  ai_engine/models/nlp_classifier.joblib
"""

import os
import sys
import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(HERE, "models")
DATA_CSV = os.path.join(HERE, "data", "real_civic_complaints.csv")
MODEL_OUT = os.path.join(MODELS_DIR, "nlp_classifier.joblib")
os.makedirs(MODELS_DIR, exist_ok=True)

def train():
    print(f"[NLP Triage Trainer] Loading real grievance reports from {DATA_CSV}...")
    df = pd.read_csv(DATA_CSV)
    # Support both column namings: 'text' & 'category' or 'label'
    col_text = "text"
    col_label = "category" if "category" in df.columns else "label"

    print(f"[NLP Triage Trainer] Loaded {len(df)} curated real grievance records across {df[col_label].nunique()} categories.")
    print("[NLP Triage Trainer] Category Distribution:")
    for cat, count in df[col_label].value_counts().items():
        print(f"  • {cat}: {count} records")

    X_train, X_test, y_train, y_test = train_test_split(
        df[col_text], df[col_label], test_size=0.2, random_state=42, stratify=df[col_label]
    )

    model = Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 2), max_features=10000, sublinear_tf=True)),
        ("clf", LogisticRegression(C=1.5, max_iter=1000, class_weight="balanced"))
    ])

    print("\n[NLP Triage Trainer] Training TF-IDF + LogisticRegression pipeline...")
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"\n[NLP Triage Trainer] Validation Holdout Accuracy: {acc * 100:.2f}%\n")
    print(classification_report(y_test, y_pred))

    # Refit on entire dataset for maximum production vocabulary
    print("[NLP Triage Trainer] Refitting pipeline on 100% of authentic real grievance reports...")
    model.fit(df[col_text], df[col_label])

    joblib.dump(model, MODEL_OUT)
    print(f"[NLP Triage Trainer] Production weights successfully saved to: {MODEL_OUT}")
    print(f"[NLP Triage Trainer] Serialized file size: {os.path.getsize(MODEL_OUT) / 1024:.2f} KB")

if __name__ == "__main__":
    train()
