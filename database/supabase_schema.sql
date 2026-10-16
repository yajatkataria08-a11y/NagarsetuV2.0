-- ============================================================
-- NagarSetu — Complete Supabase Schema
-- Project: (REDACTED)
-- Run this in Supabase SQL Editor (Schema: public)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- EXTENSIONS
-- ─────────────────────────────────────────────────────────────
create extension if not exists "uuid-ossp";
create extension if not exists "postgis";          -- geo queries for reports

-- ─────────────────────────────────────────────────────────────
-- 1. PROFILES  (core user identity)
-- ─────────────────────────────────────────────────────────────
create table if not exists public.profiles (
    uid          text        primary key,           -- deterministic UUID from phone
    phone        text        not null unique,
    name         text        not null default '',
    email        text        not null default '',
    city         text        not null default 'Bhopal',
    ward         text        not null default '',
    ward_number  int,
    avatar_url   text        not null default '',
    is_verified  boolean     not null default false,
    is_guest     boolean     not null default false,
    fcm_token    text,                              -- push notification token
    role         text        not null default 'citizen'  -- citizen | officer | admin
                             check (role in ('citizen','officer','admin')),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    last_seen_at timestamptz
);

comment on table public.profiles is 'One row per verified/guest user';

create index if not exists idx_profiles_phone  on public.profiles (phone);
create index if not exists idx_profiles_ward   on public.profiles (ward);

-- Auto-update updated_at
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end;
$$;

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();


-- ─────────────────────────────────────────────────────────────
-- 2. WARDS  (Bhopal Municipal Corporation ward master)
-- ─────────────────────────────────────────────────────────────
create table if not exists public.wards (
    id                  serial      primary key,
    ward_number         int         not null unique,
    ward_name           text        not null,
    zone                text        not null,       -- North / South / East / West / Central
    officer_name        text        not null default '',
    officer_phone       text        not null default '',
    officer_email       text        not null default '',
    zonal_office_name   text        not null default '',
    zonal_phone         text        not null default '',
    commissioner_phone  text        not null default '0755-2441100',
    population          int,
    area_sq_km          numeric(6,2),
    created_at          timestamptz not null default now()
);

-- Seed Bhopal's wards
insert into public.wards (ward_number, ward_name, zone, officer_name, officer_phone, zonal_office_name, zonal_phone)
values
    (1,  'Shyamla Hills',   'North', 'Rajesh Sharma',   '+917552441201', 'North Zonal Office', '+917552441101'),
    (2,  'Awadhpuri',       'North', 'Priya Verma',     '+917552441202', 'North Zonal Office', '+917552441101'),
    (3,  'MP Nagar',        'Central','Amit Tiwari',    '+917552441203', 'Central Zonal Office','+917552441102'),
    (4,  'New Market',      'Central','Sunita Patel',   '+917552441204', 'Central Zonal Office','+917552441102'),
    (5,  'TT Nagar',        'Central','Deepak Joshi',   '+917552441205', 'Central Zonal Office','+917552441102'),
    (6,  'Kolar Road',      'South',  'Anita Mishra',   '+917552441206', 'South Zonal Office', '+917552441103'),
    (7,  'Hoshangabad Road','South',  'Vikram Singh',   '+917552441207', 'South Zonal Office', '+917552441103'),
    (8,  'Bairagarh',       'West',   'Geeta Yadav',    '+917552441208', 'West Zonal Office',  '+917552441104'),
    (9,  'Berasia Road',    'East',   'Suresh Gupta',   '+917552441209', 'East Zonal Office',  '+917552441105'),
    (10, 'Govindpura',      'East',   'Meena Dubey',    '+917552441210', 'East Zonal Office',  '+917552441105')
on conflict (ward_number) do nothing;


-- ─────────────────────────────────────────────────────────────
-- 3. ROAD_REPORTS  (RoadWatch pothole / civic issue reports)
-- ─────────────────────────────────────────────────────────────
create table if not exists public.road_reports (
    id               uuid        primary key default uuid_generate_v4(),
    ticket_id        text        not null unique,   -- human-readable e.g. RW-2026-1234
    uid              text        not null references public.profiles(uid) on delete cascade,
    report_type      text        not null,          -- enum: POTHOLE | ROAD_DAMAGE | OPEN_MANHOLE …
    severity         text        not null,          -- LOW | MEDIUM | HIGH | CRITICAL
    status           text        not null default 'SUBMITTED',
    description      text        not null default '',
    lat              double precision not null,
    lng              double precision not null,
    location         geography(Point, 4326),        -- PostGIS point (synced by trigger)
    ward             text        not null default '',
    ward_number      int,
    photo_url        text        not null default '',
    ai_confidence    numeric(4,2),                  -- 0.00–1.00 from TFLite
    depth_estimate_mm numeric(6,1),                -- from PotholeDetector
    verified_by_ai   boolean     not null default false,
    upvotes          int         not null default 0,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

-- Auto-sync PostGIS location from lat/lng
create or replace function public.sync_road_report_location()
returns trigger language plpgsql as $$
begin
    new.location = st_setsrid(st_makepoint(new.lng, new.lat), 4326);
    return new;
end;
$$;

drop trigger if exists trg_road_reports_location on public.road_reports;
create trigger trg_road_reports_location
    before insert or update of lat, lng on public.road_reports
    for each row execute function public.sync_road_report_location();

drop trigger if exists trg_road_reports_updated_at on public.road_reports;
create trigger trg_road_reports_updated_at
    before update on public.road_reports
    for each row execute function public.set_updated_at();

create index if not exists idx_road_reports_uid        on public.road_reports (uid);
create index if not exists idx_road_reports_status     on public.road_reports (status);
create index if not exists idx_road_reports_location   on public.road_reports using gist (location);


-- ─────────────────────────────────────────────────────────────
-- 4. EMERGENCY_EVENTS  (Raksha / EmergencyAI SOS)
-- ─────────────────────────────────────────────────────────────
create table if not exists public.emergency_events (
    id              uuid        primary key default uuid_generate_v4(),
    uid             text        not null references public.profiles(uid) on delete cascade,
    trigger_type    text        not null,   -- SHAKE | BUTTON | VOICE | WEAROS | AUTO
    triage_level    text        not null,   -- GREEN | YELLOW | RED | BLACK
    lat             double precision not null,
    lng             double precision not null,
    location        geography(Point, 4326),
    is_resolved     boolean     not null default false,
    created_at      timestamptz not null default now()
);

drop trigger if exists trg_emergency_events_location on public.emergency_events;
create trigger trg_emergency_events_location
    before insert or update of lat, lng on public.emergency_events
    for each row execute function sync_road_report_location();


-- ─────────────────────────────────────────────────────────────
-- 5. LIVE_ALERTS  (Dashboard real-time feed)
-- ─────────────────────────────────────────────────────────────
create table if not exists public.live_alerts (
    id          uuid        primary key default uuid_generate_v4(),
    type        text        not null,   -- FLOOD|FIRE|ACCIDENT|ROAD_BLOCK|POWER_CUT|OTHER
    severity    text        not null,   -- LOW|MEDIUM|HIGH|CRITICAL
    title       text        not null,
    description text        not null default '',
    lat         double precision,
    lng         double precision,
    location    geography(Point, 4326),
    is_active   boolean     not null default true,
    created_at  timestamptz not null default now()
);

drop trigger if exists trg_live_alerts_location on public.live_alerts;
create trigger trg_live_alerts_location
    before insert or update of lat, lng on public.live_alerts
    for each row execute function sync_road_report_location();


-- ─────────────────────────────────────────────────────────────
-- 6. REALTIME PUBLICATIONS  (enable for live Dashboard / Alerts)
-- ─────────────────────────────────────────────────────────────
-- Note: Realtime must be enabled via Dashboard -> Database -> Replication
-- but this script provides a placeholder for logic if needed.


-- ─────────────────────────────────────────────────────────────
-- 7. DASHBOARD VIEW (KPIs)
-- ─────────────────────────────────────────────────────────────
create or replace view public.ward_kpi as
select
    w.ward_number,
    w.ward_name,
    w.zone,
    w.officer_name,
    count(rr.id)                                            as total_reports,
    count(rr.id) filter (where rr.status = 'RESOLVED')     as resolved_reports,
    count(rr.id) filter (where rr.severity = 'CRITICAL')   as critical_open,
    round(100.0 * count(rr.id) filter (where rr.status = 'RESOLVED')
          / nullif(count(rr.id), 0), 1)                    as resolution_pct
from public.wards w
left join public.road_reports rr on rr.ward_number = w.ward_number
group by w.ward_number, w.ward_name, w.zone, w.officer_name;

-- ─────────────────────────────────────────────────────────────
-- Fix #10 — MISSING TABLES (referenced by repositories but absent from schema)
-- ─────────────────────────────────────────────────────────────

-- civic_reports  (ReportItRepository → supabase["civic_reports"])
create table if not exists public.civic_reports (
    id           text        primary key default uuid_generate_v4()::text,
    uid          text        not null references public.profiles(uid) on delete cascade,
    issue_type   text        not null,
    description  text        not null default '',
    lat          double precision not null,
    lng          double precision not null,
    ward         text        not null default '',
    photo_url    text,
    status       text        not null default 'submitted'
                             check (status in ('submitted','in_progress','resolved')),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

comment on table public.civic_reports is 'Citizen civic issue reports (garbage, potholes, etc.)';

create index if not exists idx_civic_reports_uid  on public.civic_reports (uid);
create index if not exists idx_civic_reports_ward on public.civic_reports (ward);

drop trigger if exists trg_civic_reports_updated_at on public.civic_reports;
create trigger trg_civic_reports_updated_at
    before update on public.civic_reports
    for each row execute function public.set_updated_at();

-- charging_sessions  (ChargingRepositoryImpl → supabase["charging_sessions"])
create table if not exists public.charging_sessions (
    id                   text        primary key default uuid_generate_v4()::text,
    uid                  text        not null references public.profiles(uid) on delete cascade,
    station_id           text        not null,
    start_time           bigint      not null,
    end_time             bigint,
    energy_delivered_kwh real        not null default 0,
    cost_total           numeric(10,2) not null default 0,
    created_at           timestamptz not null default now()
);

comment on table public.charging_sessions is 'EV charging session records';

create index if not exists idx_charging_sessions_uid on public.charging_sessions (uid);

-- parking_bookings  (ParkingRepositoryImpl → supabase["parking_bookings"])
create table if not exists public.parking_bookings (
    id           text        primary key default uuid_generate_v4()::text,
    uid          text        not null references public.profiles(uid) on delete cascade,
    lot_id       text        not null,
    slot_number  text        not null,
    start_time   bigint      not null,
    end_time     bigint      not null,
    qr_code      text,
    status       text        not null default 'active'
                             check (status in ('active','completed','cancelled')),
    created_at   timestamptz not null default now()
);

comment on table public.parking_bookings is 'Parking slot reservations';

create index if not exists idx_parking_bookings_uid on public.parking_bookings (uid);

-- trusted_contacts (SettingsViewModel → Supabase)
create table if not exists public.trusted_contacts (
    id           uuid        primary key default uuid_generate_v4(),
    uid          text        not null references public.profiles(uid) on delete cascade,
    name         text        not null,
    phone        text        not null,
    relation     text        not null default '',
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

comment on table public.trusted_contacts is 'Emergency trusted contacts for SOS alerts';

create index if not exists idx_trusted_contacts_uid on public.trusted_contacts (uid);

drop trigger if exists trg_trusted_contacts_updated_at on public.trusted_contacts;
create trigger trg_trusted_contacts_updated_at
    before update on public.trusted_contacts
    for each row execute function public.set_updated_at();
