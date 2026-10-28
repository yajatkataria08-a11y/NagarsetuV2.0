-- NagarSetu V2.0 — Infrastructure health tables (Supabase / PostGIS path)
-- STATUS: schema only. backend/src/services/db.js currently persists these
-- collections in the local store; wiring it to Supabase is not done yet.
-- Run after supabase_schema.sql (needs the postgis extension).

create extension if not exists postgis;

create table if not exists structures (
  id           text primary key,
  name         text not null,
  type         text not null,
  ward         text,
  fictional    boolean not null default false,
  location     geography(Point, 4326) not null,
  health_score numeric(5,1) check (health_score between 0 and 100),
  health_band  text not null default 'unknown' check (health_band in ('good','watch','critical','unknown')),
  health_components jsonb not null default '{"defect":null,"anomaly":null,"rul":null}',
  health_updated_at timestamptz,
  created_at   timestamptz not null default now()
);
create index if not exists structures_location_gix on structures using gist (location);

create table if not exists inspections (
  id            text primary key,
  structure_id  text not null references structures(id) on delete cascade,
  inspector     text,
  defects       jsonb not null,         -- detector, mask_source, crack{length/width px+mm}, severity, detections[]
  rul           jsonb,                  -- P10/P50/P90 years + assumptions
  image_path    text,                   -- annotated image in storage; never inline base64
  health_after  jsonb,
  created_at    timestamptz not null default now()
);
create index if not exists inspections_structure_idx on inspections (structure_id, created_at desc);

create table if not exists readings (
  id            text primary key,
  structure_id  text not null references structures(id) on delete cascade,
  source        text,
  fs            numeric,
  n_samples     integer not null,
  score         numeric, threshold numeric, ratio numeric,
  flag          boolean not null,
  features      jsonb,
  data_source   text,                   -- 'simulated' until a real-data model is trained
  created_at    timestamptz not null default now()
);
create index if not exists readings_structure_idx on readings (structure_id, created_at desc);

create table if not exists structure_health_history (
  id            bigserial primary key,
  structure_id  text not null references structures(id) on delete cascade,
  score         numeric(5,1), band text, source text,
  created_at    timestamptz not null default now()
);

alter table structures enable row level security;
alter table inspections enable row level security;
alter table readings enable row level security;
alter table structure_health_history enable row level security;
-- No policies on purpose: only the service-role key (backend) can read/write.
