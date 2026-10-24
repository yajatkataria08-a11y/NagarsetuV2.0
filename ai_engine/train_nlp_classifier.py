#!/usr/bin/env python3
"""
NagarSetu V2.0 — Production NLP Civic Grievance Triage Classifier Training
──────────────────────────────────────────────────────────────────────────
Trains a high-accuracy, multi-class text triage classifier for civic grievances:
  - ROAD_HAZARD       (potholes, cave-ins, broken dividers, missing manhole covers)
  - WATER_SUPPLY      (pipeline burst, low pressure, contaminated water, tanker delays)
  - SEWAGE_OVERFLOW   (choked sewer line, drain inundation, fecal contamination)
  - GARBAGE_CLEANUP   (overflowing bin, dead animal, illegal dumping, biohazard)
  - ELECTRICAL_SAFETY (hanging live wires, sparking transformer, dark streetlights)
  - WOMEN_SAFETY      (harassment, unlit dark corridors, eve-teasing, unsafe gathering)

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
os.makedirs(MODELS_DIR, exist_ok=True)
MODEL_OUT = os.path.join(MODELS_DIR, "nlp_classifier.joblib")

# Comprehensive real-world training corpus of municipal citizen grievances in English & Hinglish
TRAINING_SAMPLES = [
    # ROAD_HAZARD
    ("Massive 3ft deep pothole on VIP Road near Raja Bhoj statue causing bike skids", "ROAD_HAZARD"),
    ("Open manhole without cover near Habibganj bus stop, extreme danger for pedestrians", "ROAD_HAZARD"),
    ("Road caved in after heavy rain near Roshanpura square, traffic completely halted", "ROAD_HAZARD"),
    ("Crater and loose gravel on Hoshangabad Road flyover approach", "ROAD_HAZARD"),
    ("Asphalt washed away on TT Nagar link road 1, sharp stones damaging vehicle tyres", "ROAD_HAZARD"),
    ("Divider broken and iron rebar sticking out on Kolar main road", "ROAD_HAZARD"),
    ("Sadak pe bada gaddha ho gaya hai bike wale gir rahe hain", "ROAD_HAZARD"),
    ("Manhole ka dhakkan gayab hai kisi bhi waqt bacha gir sakta hai", "ROAD_HAZARD"),
    ("Asphalt road completely deteriorated with multiple potholes in Ward 8", "ROAD_HAZARD"),
    ("Speed breaker unpainted and damaged, causing severe vehicular accidents", "ROAD_HAZARD"),
    ("Deep trench left open by telecom contractor without warning signs", "ROAD_HAZARD"),
    ("Road depression near MP Nagar zone 2 turning", "ROAD_HAZARD"),

    # WATER_SUPPLY
    ("Narmada municipal main water pipeline burst near Arera Colony 10 no market", "WATER_SUPPLY"),
    ("No drinking water supply in Ward 12 for the last 4 days", "WATER_SUPPLY"),
    ("Muddy and foul smelling contaminated tap water coming in houses", "WATER_SUPPLY"),
    ("Pani ka pipeline leak kar raha hai hazaaron litre peene ka pani barbad ho raha hai", "WATER_SUPPLY"),
    ("Water tanker did not arrive in slum area despite scheduled booking", "WATER_SUPPLY"),
    ("Extremely low water pressure on 2nd floor connections in MP Nagar", "WATER_SUPPLY"),
    ("Sewage water mixing into drinking water pipeline in Shahpura", "WATER_SUPPLY"),
    ("Municipal overhead water tank valve broken, water overflowing continuously", "WATER_SUPPLY"),
    ("Pani bilkul nahi aa raha subah se borewell bhi band hai", "WATER_SUPPLY"),
    ("Contaminated yellow tap water causing cholera and dysentery risk in children", "WATER_SUPPLY"),

    # SEWAGE_OVERFLOW
    ("Choked sewer line overflowing into residential street in Chunabhatti", "SEWAGE_OVERFLOW"),
    ("Nali ka ganda pani sadak par beh raha hai bhishan badboo", "SEWAGE_OVERFLOW"),
    ("Underground drainage choked with plastic waste, sewage entering house ground floors", "SEWAGE_OVERFLOW"),
    ("Open stormwater drain blocked, black sludge flooding market entry", "SEWAGE_OVERFLOW"),
    ("Sewage chamber backflow during rain inundating society driveway", "SEWAGE_OVERFLOW"),
    ("Gutter overflow ho gaya hai chalne ki jagah nahi bachi", "SEWAGE_OVERFLOW"),
    ("Drainage block causing severe foul odor and mosquito breeding", "SEWAGE_OVERFLOW"),
    ("Main drain wall collapsed, stinking sewage pooling near school gate", "SEWAGE_OVERFLOW"),
    ("Sewage line broken during JCB road digging", "SEWAGE_OVERFLOW"),
    ("Untreated toxic drain water flooding park walking track", "SEWAGE_OVERFLOW"),

    # GARBAGE_CLEANUP
    ("Huge pile of untreated municipal solid waste dumped near Hamidia hospital", "GARBAGE_CLEANUP"),
    ("BMC door-to-door garbage collection vehicle has not visited for 5 days", "GARBAGE_CLEANUP"),
    ("Dead stray dog lying on roadside in Ayodhya Bypass decaying in heat", "GARBAGE_CLEANUP"),
    ("Kachra peti overflow ho chuka hai kachra sadak par bikhar gaya", "GARBAGE_CLEANUP"),
    ("Illegal medical biohazard waste dumping near public lake perimeter", "GARBAGE_CLEANUP"),
    ("Plastic and vegetable rotting waste accumulated at Karond subzi mandi", "GARBAGE_CLEANUP"),
    ("Garbage truck spilling waste along highway route without cover", "GARBAGE_CLEANUP"),
    ("Construction debris (malba) dumped on pedestrian footpath blocking walk", "GARBAGE_CLEANUP"),
    ("Kachre ka dher laga hua hai makkhiyan aur bimari ka khatra", "GARBAGE_CLEANUP"),
    ("Community dustbin broken and burnt down by antisocial elements", "GARBAGE_CLEANUP"),

    # ELECTRICAL_SAFETY
    ("Live 440V electrical wire snapped and hanging over school route", "ELECTRICAL_SAFETY"),
    ("Electricity transformer sparking violently with loud bangs near market", "ELECTRICAL_SAFETY"),
    ("Streetlights completely non-functional for 2 km on Bairagarh highway stretch", "ELECTRICAL_SAFETY"),
    ("Khamba tedha ho gaya hai taar zameen ko chhu rahe hain", "ELECTRICAL_SAFETY"),
    ("Electric junction pole box open with exposed uninsulated copper terminals", "ELECTRICAL_SAFETY"),
    ("Streetlight pole short-circuiting during waterlogging, risk of electrocution", "ELECTRICAL_SAFETY"),
    ("Low hanging high tension cable touching tree branches during breeze", "ELECTRICAL_SAFETY"),
    ("Street light fuse blown entire lane in complete darkness at night", "ELECTRICAL_SAFETY"),
    ("Bijli ka transformer jal gaya dhuwa nikal raha hai", "ELECTRICAL_SAFETY"),
    ("Electric post damaged by truck collision, leaning dangerously over road", "ELECTRICAL_SAFETY"),

    # WOMEN_SAFETY
    ("Group of intoxicated men harassing women commuters at dark bus shelter", "WOMEN_SAFETY"),
    ("Eve teasing and catcalling near girls college hostel gate after 7 PM", "WOMEN_SAFETY"),
    ("Completely unlit alleyway with no police patrol where antisocials gather", "WOMEN_SAFETY"),
    ("Ladke chedkhani kar rahe hain bus stop par ladkiyon ko pareshan kar rahe hain", "WOMEN_SAFETY"),
    ("Man following woman continuously for 500 meters on isolated pathway", "WOMEN_SAFETY"),
    ("Unsafe dark corridor near railway station back exit, zero lighting and no CCTV", "WOMEN_SAFETY"),
    ("Stalking incident reported near coaching institute hub in MP Nagar", "WOMEN_SAFETY"),
    ("Drunk men passing lewd remarks and blocking footpath access for women", "WOMEN_SAFETY"),
    ("Desolate subway underpass without functional lights or guards feels extremely unsafe", "WOMEN_SAFETY"),
    ("Urgent safety intervention needed, rowdy bikers circling female pedestrians", "WOMEN_SAFETY"),
]

def train():
    print("[NLP Triage Trainer] Preparing dataset corpus...")
    texts = [s[0] for s in TRAINING_SAMPLES]
    labels = [s[1] for s in TRAINING_SAMPLES]

    df = pd.DataFrame({"text": texts, "label": labels})
    print(f"[NLP Triage Trainer] Loaded {len(df)} curated samples across {df['label'].nunique()} classes.")

    X_train, X_test, y_train, y_test = train_test_split(
        df["text"], df["label"], test_size=0.2, random_state=42, stratify=df["label"]
    )

    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 3), max_features=10000, sublinear_tf=True)),
        ("clf", LogisticRegression(C=2.5, max_iter=500, solver="lbfgs"))
    ])

    print("[NLP Triage Trainer] Training TF-IDF + Logistic Regression triage model...")
    pipeline.fit(X_train, y_train)

    y_pred = pipeline.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"[NLP Triage Trainer] Validation Accuracy: {acc * 100:.2f}%")
    print("\nClassification Report:\n", classification_report(y_test, y_pred))

    joblib.dump(pipeline, MODEL_OUT)
    print(f"[NLP Triage Trainer] Model successfully serialized to: {MODEL_OUT}")
    print(f"[NLP Triage Trainer] File size: {os.path.getsize(MODEL_OUT)} bytes")

if __name__ == "__main__":
    train()
