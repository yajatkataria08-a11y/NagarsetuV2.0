-- ============================================================
-- NagarSetu V2.0 — Master Unified Supabase & PostGIS Schema
-- Compatible with both:
--   1. Android App (13 modules: RoadWatch, Raksha, EmergencyAI)
--   2. Web Command Center (Next.js 15, Live Tracking, CCTV Wall)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ─── 1. PROFILES & CITIZENS ──────────────────────────────────
CREATE TABLE IF NOT EXISTS public.profiles (
    uid          TEXT PRIMARY KEY,
    phone        TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL DEFAULT '',
    email        TEXT NOT NULL DEFAULT '',
    city         TEXT NOT NULL DEFAULT 'Bhopal',
    ward         TEXT NOT NULL DEFAULT '',
    ward_number  INT,
    avatar_url   TEXT NOT NULL DEFAULT '',
    is_verified  BOOLEAN NOT NULL DEFAULT false,
    is_guest     BOOLEAN NOT NULL DEFAULT false,
    fcm_token    TEXT,
    role         TEXT NOT NULL DEFAULT 'citizen' CHECK (role IN ('citizen','officer','admin')),
    trust_score  INT DEFAULT 80 CHECK (trust_score BETWEEN 0 AND 100),
    report_count INT DEFAULT 0,
    sos_count    INT DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_profiles_phone ON public.profiles (phone);
CREATE INDEX IF NOT EXISTS idx_profiles_ward  ON public.profiles (ward);

-- ─── 2. WARDS MASTER ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.wards (
    id                  SERIAL PRIMARY KEY,
    ward_number         INT NOT NULL UNIQUE,
    ward_name           TEXT NOT NULL,
    zone                TEXT NOT NULL,
    officer_name        TEXT NOT NULL DEFAULT '',
    officer_phone       TEXT NOT NULL DEFAULT '',
    officer_email       TEXT NOT NULL DEFAULT '',
    zonal_office_name   TEXT NOT NULL DEFAULT '',
    zonal_phone         TEXT NOT NULL DEFAULT '',
    commissioner_phone  TEXT NOT NULL DEFAULT '0755-2441100',
    population          INT,
    area_sq_km          NUMERIC(6,2),
    complaint_count     INT DEFAULT 0,
    resolved_count      INT DEFAULT 0,
    budget_sanctioned   BIGINT DEFAULT 2500000,
    budget_spent        BIGINT DEFAULT 1800000,
    sla_breaches        INT DEFAULT 0,
    avg_resolution_hrs  INT DEFAULT 24,
    active_officers     INT DEFAULT 3,
    risk_level          TEXT DEFAULT 'MODERATE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed Bhopal Wards
INSERT INTO public.wards (ward_number, ward_name, zone, officer_name, officer_phone, complaint_count, resolved_count, budget_sanctioned, budget_spent, sla_breaches, avg_resolution_hrs, risk_level)
VALUES
    (1,  'Shyamla Hills',    'North',   'Rajesh Sharma', '+917552441201', 33, 30, 2100000, 1500000, 0, 8,  'LOW'),
    (2,  'Awadhpuri',        'North',   'Priya Verma',   '+917552441202', 40, 32, 2300000, 1650000, 2, 14, 'MODERATE'),
    (3,  'MP Nagar',         'Central', 'Amit Tiwari',   '+917552441203', 45, 38, 2500000, 1800000, 2, 18, 'MODERATE'),
    (4,  'New Market',       'Central', 'Sunita Patel',  '+917552441204', 38, 31, 2200000, 1700000, 3, 20, 'MODERATE'),
    (5,  'TT Nagar',         'Central', 'Geeta Nair',    '+917552441205', 55, 40, 2800000, 2100000, 4, 20, 'MODERATE'),
    (6,  'Kolar Road',       'South',   'Sunita Verma',  '+917552441206', 71, 49, 4100000, 3600000, 8, 36, 'CRITICAL'),
    (7,  'Arera Colony',     'South',   'Priya Sharma',  '+917552441207', 62, 51, 3200000, 2900000, 5, 24, 'HIGH'),
    (8,  'Bairagarh',        'West',    'Geeta Yadav',   '+917552441208', 29, 25, 1900000, 1300000, 1, 12, 'LOW'),
    (9,  'Habibganj',        'East',    'Amit Singh',    '+917552441209', 28, 24, 1800000, 1200000, 1, 12, 'LOW'),
    (10, 'Govindpura',       'East',    'Meena Dubey',   '+917552441210', 48, 37, 2700000, 2050000, 3, 22, 'MODERATE')
ON CONFLICT (ward_number) DO UPDATE SET
    complaint_count = EXCLUDED.complaint_count,
    resolved_count = EXCLUDED.resolved_count,
    risk_level = EXCLUDED.risk_level;

-- ─── 3. ROAD & CIVIC REPORTS (RoadWatch) ─────────────────────
CREATE TABLE IF NOT EXISTS public.road_reports (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_id         TEXT NOT NULL UNIQUE,
    uid               TEXT NOT NULL REFERENCES public.profiles(uid) ON DELETE CASCADE,
    report_type       TEXT NOT NULL,
    severity          TEXT NOT NULL, -- LOW | MEDIUM | HIGH | CRITICAL
    status            TEXT NOT NULL DEFAULT 'SUBMITTED',
    description       TEXT NOT NULL DEFAULT '',
    lat               DOUBLE PRECISION NOT NULL,
    lng               DOUBLE PRECISION NOT NULL,
    location          GEOGRAPHY(Point, 4326),
    ward              TEXT NOT NULL DEFAULT '',
    ward_number       INT,
    photo_url         TEXT NOT NULL DEFAULT '',
    ai_confidence     NUMERIC(4,2),
    depth_estimate_mm NUMERIC(6,1),
    verified_by_ai    BOOLEAN NOT NULL DEFAULT false,
    upvotes           INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auto-sync PostGIS Point
CREATE OR REPLACE FUNCTION public.sync_road_report_location()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_road_reports_location ON public.road_reports;
CREATE TRIGGER trg_road_reports_location
    BEFORE INSERT OR UPDATE OF lat, lng ON public.road_reports
    FOR EACH ROW EXECUTE FUNCTION public.sync_road_report_location();

-- ─── 4. EMERGENCY & SOS EVENTS (Raksha / WearOS) ─────────────
CREATE TABLE IF NOT EXISTS public.emergency_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    uid             TEXT NOT NULL REFERENCES public.profiles(uid) ON DELETE CASCADE,
    user_name       TEXT NOT NULL DEFAULT '',
    phone           TEXT NOT NULL DEFAULT '',
    trigger_type    TEXT NOT NULL,   -- SHAKE | BUTTON | VOICE | WEAROS | AUTO
    triage_level    TEXT NOT NULL,   -- CRITICAL | HIGH | MODERATE
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    location        GEOGRAPHY(Point, 4326),
    ward            TEXT NOT NULL DEFAULT '',
    battery_level   INT DEFAULT 50,
    heart_rate      INT,
    tracking_token  TEXT UNIQUE DEFAULT substr(md5(random()::text), 1, 12),
    is_resolved     BOOLEAN NOT NULL DEFAULT false,
    acknowledged    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ
);

DROP TRIGGER IF EXISTS trg_emergency_events_location ON public.emergency_events;
CREATE TRIGGER trg_emergency_events_location
    BEFORE INSERT OR UPDATE OF lat, lng ON public.emergency_events
    FOR EACH ROW EXECUTE FUNCTION public.sync_road_report_location();

-- ─── 5. LIVE TRACKING SESSIONS (Raksha Web Tracking) ──────────
CREATE TABLE IF NOT EXISTS public.tracking_sessions (
    session_id   TEXT PRIMARY KEY,
    user_id      TEXT REFERENCES public.profiles(uid) ON DELETE CASCADE,
    user_name    TEXT NOT NULL DEFAULT '',
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    battery_level INT DEFAULT 50,
    heart_rate   INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '2 hours'),
    active       BOOLEAN NOT NULL DEFAULT true
);

-- ─── 6. DYNAMIC HAZARD ZONES (Raksha Dynamic Geofencing) ──────
CREATE TABLE IF NOT EXISTS public.hazard_zones (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           TEXT NOT NULL,
    center_lat     DOUBLE PRECISION NOT NULL,
    center_lng     DOUBLE PRECISION NOT NULL,
    radius_meters  INT NOT NULL DEFAULT 300,
    risk_boost     DOUBLE PRECISION NOT NULL DEFAULT 1.5,
    risk_level     TEXT NOT NULL DEFAULT 'HIGH',
    note           TEXT,
    created_by     TEXT NOT NULL DEFAULT 'admin',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ,
    active         BOOLEAN NOT NULL DEFAULT true
);

-- ─── 7. RESPONDERS ROSTER (Krisis Smart Notification Engine) ──
CREATE TABLE IF NOT EXISTS public.responders (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             TEXT NOT NULL,
    badge            TEXT NOT NULL UNIQUE,
    ward             TEXT NOT NULL,
    phone            TEXT NOT NULL,
    status           TEXT NOT NULL DEFAULT 'on-duty', -- on-duty | busy | off-duty
    lat              DOUBLE PRECISION,
    lng              DOUBLE PRECISION,
    assigned_count   INT NOT NULL DEFAULT 0,
    resolved_count   INT NOT NULL DEFAULT 0,
    avg_response_min INT NOT NULL DEFAULT 15,
    last_ping        TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO public.responders (name, badge, ward, phone, status, avg_response_min)
VALUES
    ('Ravi Kumar',    'MP-041', 'Arera Colony', '+919001234567', 'on-duty', 18),
    ('Sunita Kamble', 'MP-038', 'Habibganj',    '+919001234568', 'on-duty', 12),
    ('Pradeep Yadav', 'MP-052', 'Kolar Road',   '+919001234569', 'busy',    34),
    ('Anita Thakur',  'MP-029', 'MP Nagar',     '+919001234570', 'on-duty', 9),
    ('Mohit Sahu',    'MP-063', 'TT Nagar',     '+919001234571', 'off-duty',45)
ON CONFLICT (badge) DO NOTHING;

-- ─── 8. LIVE ALERTS & CCTV STREAMS (Krisis Surveillance) ──────
CREATE TABLE IF NOT EXISTS public.cctv_cameras (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    location_desc TEXT NOT NULL,
    lat           DOUBLE PRECISION NOT NULL,
    lng           DOUBLE PRECISION NOT NULL,
    stream_url    TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'ONLINE',
    ai_vision     BOOLEAN NOT NULL DEFAULT true,
    last_event    TEXT
);

INSERT INTO public.cctv_cameras (id, name, location_desc, lat, lng, stream_url)
VALUES
    ('cam_01', 'CAM-01 · MP Nagar Zone-1', 'Main Chowk / Traffic Signal', 23.2310, 77.4350, 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=640&q=80'),
    ('cam_02', 'CAM-02 · TT Nagar Stadium', 'Stadium North Gate Corridor', 23.2430, 77.4190, 'https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=640&q=80'),
    ('cam_03', 'CAM-03 · Habibganj Underpass', 'Underpass Flood Level Watch', 23.2680, 77.4300, 'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=640&q=80'),
    ('cam_04', 'CAM-04 · Arera Hills Spine', 'VIP Road Entry Point', 23.2599, 77.4126, 'https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=640&q=80'),
    ('cam_05', 'CAM-05 · New Market Square', 'Pedestrian Walkway & Parking', 23.2380, 77.4010, 'https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=640&q=80'),
    ('cam_06', 'CAM-06 · Kolar Transit Hub', 'Bus Terminal & Depot', 23.1890, 77.4240, 'https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=640&q=80')
ON CONFLICT (id) DO NOTHING;

-- ─── 9. VIEWS COMPATIBILITY (Bridge Android & Web) ────────────
CREATE OR REPLACE VIEW public.incidents AS
SELECT
    id,
    report_type as type,
    description,
    lat,
    lng,
    ward,
    CASE 
        WHEN severity = 'CRITICAL' THEN 5
        WHEN severity = 'HIGH' THEN 4
        WHEN severity = 'MEDIUM' THEN 3
        ELSE 2
    END as severity,
    LOWER(status) as status,
    photo_url as photos,
    uid as reported_by,
    created_at as reported_at,
    ai_confidence as ai_score
FROM public.road_reports;

CREATE OR REPLACE VIEW public.sos_events AS
SELECT
    id,
    uid as user_id,
    user_name,
    phone,
    ward,
    lat,
    lng,
    battery_level,
    heart_rate,
    CASE WHEN is_resolved THEN 'resolved' WHEN acknowledged THEN 'acknowledged' ELSE 'active' END as status,
    triage_level as siren_type,
    tracking_token,
    created_at as triggered_at,
    resolved_at
FROM public.emergency_events;
