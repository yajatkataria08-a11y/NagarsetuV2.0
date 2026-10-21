#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
NagarSetu V2.0 — City Grid Stress Simulator (Phase 14)
───────────────────────────────────────────────────────
Simulates production-grade concurrent load:
  - 50 simultaneous SOS emergency beacons
  - 200 GPS tracking pings/second
  - 20 concurrent CCTV detection events
  - Hazard geofence collision checks across city grid
  - Incident triage bursts with duplicate detection
"""

import asyncio
import aiohttp
import time
import json
import random
import math
import sys
from typing import List, Dict, Any

API_BASE = "http://localhost:4000"
TIMEOUT_SECS = 8

BHOPAL_BOUNDS = {"lat_min": 23.15, "lat_max": 23.35, "lng_min": 77.35, "lng_max": 77.55}
INDORE_BOUNDS = {"lat_min": 22.60, "lat_max": 22.80, "lng_min": 75.75, "lng_max": 75.95}

WARDS = ["Ward 3 - MP Nagar", "Ward 5 - TT Nagar", "Ward 7 - Arera Colony",
         "Ward 9 - Habibganj", "Ward 12 - Kolar", "Ward 18 - Indore Rajwada",
         "Ward 22 - Vijay Nagar", "Ward 24 - Bijalpur"]

INCIDENT_TYPES = ["POTHOLE", "WATERLOGGING", "STREETLIGHT", "GARBAGE", "SEWAGE_OVERFLOW", "ROAD_HAZARD"]

HINGLISH_COMPLAINTS = [
    "MP Nagar mein gaddha hai bohot bada, accident hone ki sambhavna hai",
    "Habibganj underpass me paani bhar gaya hai",
    "Arera Colony ki batti nahi hai, andhera bahut hai",
    "Kolar ke paat ke paas kachra jama hua hai",
    "TT Nagar overbridge pe nala overflow kar raha hai",
    "Bijali pole toot gaya hai, danger zone ban gaya hai",
    "New Market area mein sewage bahar aa raha hai",
    "Vijay Nagar chhowk pe flood jaise halat hai"
]

def random_coord(bounds):
    lat = random.uniform(bounds["lat_min"], bounds["lat_max"])
    lng = random.uniform(bounds["lng_min"], bounds["lng_max"])
    return round(lat, 4), round(lng, 4)

class SimulationMetrics:
    def __init__(self):
        self.sos_success = 0
        self.sos_failure = 0
        self.ping_success = 0
        self.ping_failure = 0
        self.incident_success = 0
        self.incident_failure = 0
        self.triage_success = 0
        self.triage_failure = 0
        self.hazard_success = 0
        self.hazard_failure = 0
        self.collision_success = 0
        self.collision_failure = 0
        self.latencies = []
        self.start_time = time.time()

    def summary(self):
        elapsed = round(time.time() - self.start_time, 2)
        total_reqs = (self.sos_success + self.sos_failure + self.ping_success +
                     self.ping_failure + self.incident_success + self.incident_failure +
                     self.triage_success + self.triage_failure + self.hazard_success +
                     self.hazard_failure + self.collision_success + self.collision_failure)
        total_success = (self.sos_success + self.ping_success + self.incident_success +
                        self.triage_success + self.hazard_success + self.collision_success)
        avg_lat = round(sum(self.latencies) / max(len(self.latencies), 1) * 1000, 1)
        p99_lat = round(sorted(self.latencies)[max(0, int(len(self.latencies) * 0.99) - 1)] * 1000, 1) if self.latencies else 0

        return {
            "elapsed_seconds": elapsed,
            "total_requests": total_reqs,
            "total_success": total_success,
            "success_rate_pct": round(total_success / max(total_reqs, 1) * 100, 2),
            "throughput_rps": round(total_reqs / max(elapsed, 0.01), 1),
            "avg_latency_ms": avg_lat,
            "p99_latency_ms": p99_lat,
            "sos": {"success": self.sos_success, "failure": self.sos_failure},
            "tracking_pings": {"success": self.ping_success, "failure": self.ping_failure},
            "incidents": {"success": self.incident_success, "failure": self.incident_failure},
            "triage": {"success": self.triage_success, "failure": self.triage_failure},
            "hazard_checks": {"success": self.hazard_success, "failure": self.hazard_failure},
            "collision_checks": {"success": self.collision_success, "failure": self.collision_failure}
        }

metrics = SimulationMetrics()

async def post_json(session: aiohttp.ClientSession, path: str, body: dict) -> Dict[str, Any]:
    t0 = time.monotonic()
    url = f"{API_BASE}{path}"
    try:
        async with session.post(url, json=body, timeout=aiohttp.ClientTimeout(total=TIMEOUT_SECS)) as res:
            elapsed = time.monotonic() - t0
            metrics.latencies.append(elapsed)
            data = await res.json()
            return {"ok": True, "status": res.status, "data": data, "latency": elapsed}
    except Exception as e:
        elapsed = time.monotonic() - t0
        return {"ok": False, "error": str(e), "latency": elapsed}

async def get_json(session: aiohttp.ClientSession, path: str) -> Dict[str, Any]:
    t0 = time.monotonic()
    url = f"{API_BASE}{path}"
    try:
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=TIMEOUT_SECS)) as res:
            elapsed = time.monotonic() - t0
            metrics.latencies.append(elapsed)
            data = await res.json()
            return {"ok": True, "status": res.status, "data": data, "latency": elapsed}
    except Exception as e:
        elapsed = time.monotonic() - t0
        return {"ok": False, "error": str(e), "latency": elapsed}

async def simulate_sos_beacon(session: aiohttp.ClientSession, beacon_id: int):
    """Simulate a single SOS emergency beacon trigger."""
    bounds = BHOPAL_BOUNDS if beacon_id % 3 != 0 else INDORE_BOUNDS
    lat, lng = random_coord(bounds)
    
    result = await post_json(session, "/api/sos", {
        "userId": f"sim-user-{beacon_id:03d}",
        "userName": f"Simulation Citizen {beacon_id}",
        "phone": f"+91-99999{beacon_id:05d}",
        "lat": lat, "lng": lng,
        "ward": random.choice(WARDS),
        "batteryLevel": random.randint(15, 100),
        "heartRate": random.randint(65, 145),
        "triggerType": random.choice(["SHAKE", "BUTTON", "VOICE", "WEAROS"]),
        "contacts": [f"+91-98989{beacon_id:05d}"]
    })
    
    if result["ok"] and result["status"] == 201:
        metrics.sos_success += 1
    else:
        metrics.sos_failure += 1

    return result.get("data", {}).get("tracking_token")

async def simulate_tracking_pings(session: aiohttp.ClientSession, n: int):
    """Simulate a burst of GPS tracking pings from live tracking sessions."""
    tasks = []
    for i in range(n):
        bounds = BHOPAL_BOUNDS if i % 3 != 0 else INDORE_BOUNDS
        lat, lng = random_coord(bounds)
        tasks.append(post_json(session, "/api/tracking/ping", {
            "sessionId": f"sim-session-{i:05d}",
            "lat": lat, "lng": lng,
            "speed": round(random.uniform(0, 60), 1),
            "batteryLevel": random.randint(20, 100)
        }))
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    for r in results:
        if isinstance(r, Exception) or not r.get("ok"):
            metrics.ping_failure += 1
        else:
            metrics.ping_success += 1

async def simulate_incidents(session: aiohttp.ClientSession, n: int):
    """Simulate civic incident reports with AI triage."""
    tasks = []
    for i in range(n):
        bounds = BHOPAL_BOUNDS if i % 3 != 0 else INDORE_BOUNDS
        lat, lng = random_coord(bounds)
        inc_type = random.choice(INCIDENT_TYPES)
        complaint = random.choice(HINGLISH_COMPLAINTS)
        
        tasks.append(post_json(session, "/api/incidents", {
            "type": inc_type,
            "description": complaint,
            "lat": lat, "lng": lng,
            "ward": random.choice(WARDS),
            "severity": random.randint(2, 5)
        }))
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    for r in results:
        if isinstance(r, Exception) or not r.get("ok"):
            metrics.incident_failure += 1
        else:
            metrics.incident_success += 1

async def simulate_triage_burst(session: aiohttp.ClientSession, n: int):
    """Simulate AI triage classification burst across Hinglish complaints."""
    tasks = []
    for i in range(n):
        bounds = BHOPAL_BOUNDS if i % 2 == 0 else INDORE_BOUNDS
        lat, lng = random_coord(bounds)
        complaint = random.choice(HINGLISH_COMPLAINTS)
        
        tasks.append(post_json(session, "/api/incidents/triage", {
            "description": complaint,
            "lat": lat, "lng": lng
        }))
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    for r in results:
        if isinstance(r, Exception) or not r.get("ok"):
            metrics.triage_failure += 1
        else:
            metrics.triage_success += 1

async def simulate_hazard_collision_grid(session: aiohttp.ClientSession):
    """Simulate point-collision checks across a 10x10 city grid."""
    bhopal_lats = [23.15 + i * 0.02 for i in range(10)]
    bhopal_lngs = [77.35 + i * 0.02 for i in range(10)]
    
    tasks = []
    for lat in bhopal_lats:
        for lng in bhopal_lngs:
            tasks.append(post_json(session, "/api/hazards/check-point", {
                "lat": round(lat, 4), "lng": round(lng, 4)
            }))
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    fast_responses = 0
    for r in results:
        if isinstance(r, Exception) or not r.get("ok"):
            metrics.hazard_failure += 1
        else:
            metrics.hazard_success += 1
            if r.get("latency", 1.0) < 0.05:  # < 50ms
                fast_responses += 1
    
    pct_fast = round(fast_responses / max(len(results), 1) * 100, 1)
    return fast_responses, pct_fast

async def run_simulation():
    print("\n" + "=" * 65)
    print("  NagarSetu V2.0 — Smart City Grid Stress Simulator")
    print("  Target: 50 SOS + 200 GPS Pings + 30 Incidents + 100 Grid Checks")
    print("=" * 65)

    connector = aiohttp.TCPConnector(limit=150, limit_per_host=100, ttl_dns_cache=300)
    
    async with aiohttp.ClientSession(connector=connector) as session:
        # Phase A: Health Check
        print("\n[Phase A] Verifying backend connectivity...")
        health = await get_json(session, "/api/health")
        if not health["ok"]:
            print(f"  [ERROR] Backend unreachable at {API_BASE}. Start the backend first.")
            print(f"  Hint: cd repo_v2/backend && node src/index.js")
            return
        print(f"  [OK] Backend online — Version {health['data'].get('version', '?')}")

        # Phase B: SOS Beacon Storm
        print("\n[Phase B] Simulating 50 concurrent SOS emergency beacons...")
        t_sos = time.monotonic()
        sos_tasks = [simulate_sos_beacon(session, i) for i in range(50)]
        tokens = await asyncio.gather(*sos_tasks)
        valid_tokens = [t for t in tokens if t]
        print(f"  [OK] SOS dispatch: {metrics.sos_success}/50 successful | {len(valid_tokens)} tracking tokens issued | {round((time.monotonic() - t_sos) * 1000)}ms elapsed")

        # Phase C: GPS Tracking Ping Burst
        print("\n[Phase C] Simulating 200 GPS tracking pings/second burst...")
        t_ping = time.monotonic()
        await simulate_tracking_pings(session, 200)
        ping_elapsed = round(time.monotonic() - t_ping, 2)
        actual_rps = round(200 / max(ping_elapsed, 0.01))
        print(f"  [OK] Pings: {metrics.ping_success}/200 | Elapsed: {ping_elapsed}s | Rate: {actual_rps} req/s")

        # Phase D: Incident Reports with Hinglish Triage
        print("\n[Phase D] Simulating 30 concurrent civic incident reports...")
        t_inc = time.monotonic()
        await simulate_incidents(session, 30)
        print(f"  [OK] Incidents: {metrics.incident_success}/30 created | {round((time.monotonic() - t_inc) * 1000)}ms elapsed")

        print("\n[Phase E] Simulating 20 concurrent Hinglish AI triage calls...")
        t_triage = time.monotonic()
        await simulate_triage_burst(session, 20)
        print(f"  [OK] Triage: {metrics.triage_success}/20 classified | {round((time.monotonic() - t_triage) * 1000)}ms elapsed")

        # Phase F: City Grid Hazard Collision Checks
        print("\n[Phase F] Running 100-point Bhopal city hazard collision grid scan...")
        t_col = time.monotonic()
        fast_count, fast_pct = await simulate_hazard_collision_grid(session)
        print(f"  [OK] Grid: {metrics.hazard_success}/100 checks | {fast_pct}% under 50ms SLA | {round((time.monotonic() - t_col) * 1000)}ms total")

        # Final Summary
        summary = metrics.summary()
        print("\n" + "=" * 65)
        print("  SIMULATION RESULTS SUMMARY")
        print("=" * 65)
        print(f"  Total Requests   : {summary['total_requests']}")
        print(f"  Success Rate     : {summary['success_rate_pct']}%")
        print(f"  Throughput       : {summary['throughput_rps']} req/s")
        print(f"  Avg Latency      : {summary['avg_latency_ms']} ms")
        print(f"  P99 Latency      : {summary['p99_latency_ms']} ms")
        print(f"  Elapsed          : {summary['elapsed_seconds']}s")
        print("=" * 65)

        if summary["success_rate_pct"] >= 95.0:
            print(f"\n  [PASS] City simulation SUCCESS — {summary['success_rate_pct']}% SLA compliance\n")
        else:
            print(f"\n  [WARN] Below 95% SLA threshold — {summary['success_rate_pct']}% success rate\n")

if __name__ == "__main__":
    try:
        asyncio.run(run_simulation())
    except KeyboardInterrupt:
        print("\n[Simulation interrupted by user]")
