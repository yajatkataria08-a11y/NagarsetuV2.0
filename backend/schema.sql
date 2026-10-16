-- ============================================================
-- NagarSetu V2.0 — Supabase / PostgreSQL Schema (Phase 2)
-- Run this in the Supabase SQL editor
-- Requires: postgis extension
-- ============================================================

-- Enable PostGIS for spatial queries
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── ENUM TYPES ────────────────────────────────────────────
CREATE TYPE incident_status AS ENUM (
  'pending', 'ai_verified', 'verified', 'dispatched', 'resolved', 'rejected'
);

CREATE TYPE sos_status AS ENUM (
  'active', 'acknowledged', 'dispatched', 'resolved'
);

CREATE TYPE responder_status AS ENUM (
  'on-duty', 'busy', 'off-duty'
);

CREATE TYPE risk_level AS ENUM ('LOW', 'MODERATE', 'HIGH', 'CRITICAL');

-- ─── USERS (citizens) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS citizens (
  id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  auth_uid     UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  name         TEXT NOT NULL,
  phone        TEXT,
  ward         TEXT,
  trust_score  INT  DEFAULT 80 CHECK (trust_score BETWEEN 0 AND 100),
  report_count INT  DEFAULT 0,
  sos_count    INT  DEFAULT 0,
  false_reports INT DEFAULT 0,
  status       TEXT DEFAULT 'active',
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  last_active  TIMESTAMPTZ DEFAULT NOW()
);

-- ─── SOS EVENTS ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sos_events (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  citizen_id      UUID REFERENCES citizens(id),
  user_name       TEXT,
  phone           TEXT,
  ward            TEXT,
  geom            GEOMETRY(POINT, 4326),   -- PostGIS spatial column
  lat             FLOAT8,
  lng             FLOAT8,
  address         TEXT,
  battery_level   INT,
  heart_rate      INT,
  status          sos_status DEFAULT 'active',
  siren_type      TEXT DEFAULT 'MODERATE',
  tracking_token  TEXT UNIQUE DEFAULT substr(md5(random()::text), 1, 12),
  contacts        TEXT[],
  triggered_at    TIMESTAMPTZ DEFAULT NOW(),
  acknowledged_at TIMESTAMPTZ,
  resolved_at     TIMESTAMPTZ
);

-- Spatial index for fast proximity queries
CREATE INDEX IF NOT EXISTS sos_events_geom_idx ON sos_events USING GIST (geom);

-- Auto-populate geom from lat/lng
CREATE OR REPLACE FUNCTION sos_set_geom() RETURNS TRIGGER AS $$
BEGIN
  IF NEW.lat IS NOT NULL AND NEW.lng IS NOT NULL THEN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS sos_geom_trigger ON sos_events;
CREATE TRIGGER sos_geom_trigger BEFORE INSERT OR UPDATE ON sos_events
  FOR EACH ROW EXECUTE FUNCTION sos_set_geom();

-- ─── INCIDENTS (civic reports) ─────────────────────────────
CREATE TABLE IF NOT EXISTS incidents (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  type            TEXT NOT NULL,
  description     TEXT,
  geom            GEOMETRY(POINT, 4326),
  lat             FLOAT8,
  lng             FLOAT8,
  address         TEXT,
  ward            TEXT,
  severity        INT DEFAULT 3 CHECK (severity BETWEEN 1 AND 5),
  status          incident_status DEFAULT 'pending',
  photo_urls      TEXT[],
  reported_by     UUID REFERENCES citizens(id),
  reported_at     TIMESTAMPTZ DEFAULT NOW(),
  ai_score        FLOAT4,
  ai_risk_level   risk_level,
  ai_feedback     TEXT,
  assigned_officer UUID,
  resolved_at     TIMESTAMPTZ,
  source          TEXT DEFAULT 'android'   -- 'android' | 'qr-portal' | 'wearos'
);

CREATE INDEX IF NOT EXISTS incidents_geom_idx  ON incidents USING GIST (geom);
CREATE INDEX IF NOT EXISTS incidents_status_idx ON incidents (status);
CREATE INDEX IF NOT EXISTS incidents_ward_idx   ON incidents (ward);
CREATE INDEX IF NOT EXISTS incidents_reported_at_idx ON incidents (reported_at DESC);

-- Auto-populate geom
CREATE OR REPLACE FUNCTION incidents_set_geom() RETURNS TRIGGER AS $$
BEGIN
  IF NEW.lat IS NOT NULL AND NEW.lng IS NOT NULL THEN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS incidents_geom_trigger ON incidents;
CREATE TRIGGER incidents_geom_trigger BEFORE INSERT OR UPDATE ON incidents
  FOR EACH ROW EXECUTE FUNCTION incidents_set_geom();

-- ─── RESPONDERS ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS responders (
  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name             TEXT NOT NULL,
  badge            TEXT UNIQUE,
  ward             TEXT,
  phone            TEXT,
  status           responder_status DEFAULT 'off-duty',
  geom             GEOMETRY(POINT, 4326),
  lat              FLOAT8,
  lng              FLOAT8,
  assigned_count   INT DEFAULT 0,
  resolved_count   INT DEFAULT 0,
  avg_response_min INT DEFAULT 30,
  last_ping        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS responders_geom_idx ON responders USING GIST (geom);

-- ─── TRACKING SESSIONS ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS tracking_sessions (
  session_id   TEXT PRIMARY KEY,
  citizen_id   UUID REFERENCES citizens(id),
  user_name    TEXT,
  lat          FLOAT8,
  lng          FLOAT8,
  geom         GEOMETRY(POINT, 4326),
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  expires_at   TIMESTAMPTZ DEFAULT NOW() + INTERVAL '2 hours',
  active       BOOLEAN DEFAULT TRUE
);

-- ─── HAZARD ZONES ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hazard_zones (
  id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name           TEXT NOT NULL,
  geom           GEOMETRY(POLYGON, 4326),
  center_lat     FLOAT8,
  center_lng     FLOAT8,
  radius_meters  INT DEFAULT 200,
  risk_boost     FLOAT4 DEFAULT 1.5,
  risk_level     risk_level DEFAULT 'HIGH',
  created_by     TEXT DEFAULT 'admin',
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  expires_at     TIMESTAMPTZ,
  note           TEXT,
  active         BOOLEAN DEFAULT TRUE
);

-- ─── WARDS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wards (
  id                  TEXT PRIMARY KEY,
  ward_name           TEXT NOT NULL,
  zone                TEXT,
  authority_name      TEXT,
  complaint_count     INT DEFAULT 0,
  resolved_count      INT DEFAULT 0,
  budget_sanctioned   BIGINT DEFAULT 0,
  budget_spent        BIGINT DEFAULT 0,
  sla_breaches        INT DEFAULT 0,
  avg_resolution_hrs  INT DEFAULT 24,
  active_officers     INT DEFAULT 0,
  risk_level          risk_level DEFAULT 'LOW',
  boundary            GEOMETRY(POLYGON, 4326),
  updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ─── AI VERIFICATIONS LOG ──────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_verifications (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  incident_id     UUID REFERENCES incidents(id) ON DELETE CASCADE,
  model           TEXT DEFAULT 'heuristic-v1',
  score           FLOAT4,
  risk_level      risk_level,
  feedback        TEXT,
  tags            TEXT[],
  recommended_action TEXT,
  breakdown       JSONB,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ─── AUDIT LOG ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  entity_type TEXT NOT NULL,   -- 'incident' | 'sos' | 'hazard_zone'
  entity_id   TEXT NOT NULL,
  action      TEXT NOT NULL,   -- 'created' | 'verified' | 'rejected' | 'dispatched'
  actor       TEXT,            -- admin ID or 'system'
  old_value   JSONB,
  new_value   JSONB,
  ip_addr     TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS audit_log_entity_idx ON audit_log (entity_type, entity_id);

-- ─── ROW LEVEL SECURITY ────────────────────────────────────
ALTER TABLE citizens    ENABLE ROW LEVEL SECURITY;
ALTER TABLE sos_events  ENABLE ROW LEVEL SECURITY;
ALTER TABLE incidents   ENABLE ROW LEVEL SECURITY;

-- Citizens can only read their own row
CREATE POLICY citizens_self ON citizens
  FOR SELECT USING (auth.uid() = auth_uid);

-- Citizens can read all incidents (public transparency)
CREATE POLICY incidents_public_read ON incidents
  FOR SELECT USING (true);

-- Citizens can only insert their own incidents
CREATE POLICY incidents_citizen_insert ON incidents
  FOR INSERT WITH CHECK (reported_by IN (
    SELECT id FROM citizens WHERE auth_uid = auth.uid()
  ));

-- Admins (service role) bypass all RLS
-- (handled automatically by supabase service key)

-- ─── USEFUL SPATIAL QUERY FUNCTIONS ────────────────────────

-- Find responders within N meters of a point
CREATE OR REPLACE FUNCTION responders_near(lat FLOAT8, lng FLOAT8, radius_m INT DEFAULT 5000)
RETURNS SETOF responders AS $$
  SELECT * FROM responders
  WHERE status != 'off-duty'
    AND geom IS NOT NULL
    AND ST_DWithin(
      geom::geography,
      ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography,
      radius_m
    )
  ORDER BY
    ST_Distance(
      geom::geography,
      ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography
    ) ASC
  LIMIT 5;
$$ LANGUAGE SQL STABLE;

-- Seed default Bhopal ward data
INSERT INTO wards (id, ward_name, zone, authority_name, complaint_count, resolved_count,
                   budget_sanctioned, budget_spent, sla_breaches, avg_resolution_hrs, active_officers, risk_level)
VALUES
  ('w1', 'Ward 3 — MP Nagar',      'Zone A', 'Rajesh Kumar',  45, 38, 2500000, 1800000, 2,  18, 3, 'MODERATE'),
  ('w2', 'Ward 7 — Arera Colony',  'Zone B', 'Priya Sharma',  62, 51, 3200000, 2900000, 5,  24, 5, 'HIGH'),
  ('w3', 'Ward 9 — Habibganj',     'Zone A', 'Amit Singh',    28, 24, 1800000, 1200000, 1,  12, 2, 'LOW'),
  ('w4', 'Ward 12 — Kolar',        'Zone C', 'Sunita Verma',  71, 49, 4100000, 3600000, 8,  36, 4, 'CRITICAL'),
  ('w5', 'Ward 5 — TT Nagar',      'Zone A', 'Geeta Nair',    55, 40, 2800000, 2100000, 4,  20, 4, 'MODERATE'),
  ('w6', 'Ward 15 — Bairasia',     'Zone B', 'Dinesh Patel',  33, 30, 2100000, 1500000, 0,  8,  3, 'LOW')
ON CONFLICT (id) DO NOTHING;

-- Seed responders
INSERT INTO responders (name, badge, ward, phone, status, avg_response_min)
VALUES
  ('Ravi Kumar',    'MP-041', 'Ward 7', '+91-9001234567', 'on-duty', 18),
  ('Sunita Kamble', 'MP-038', 'Ward 9', '+91-9001234568', 'on-duty', 12),
  ('Pradeep Yadav', 'MP-052', 'Ward 12','+91-9001234569', 'busy',    34),
  ('Anita Thakur',  'MP-029', 'Ward 3', '+91-9001234570', 'on-duty', 9),
  ('Mohit Sahu',    'MP-063', 'Ward 5', '+91-9001234571', 'off-duty',45)
ON CONFLICT (badge) DO NOTHING;
