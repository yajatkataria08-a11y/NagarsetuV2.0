/**
 * NagarSetu V2.0 — Comprehensive Demonstration Seeder
 * Populates backend/data/store.json with rich, realistic telemetry
 * covering Bhopal and Indore municipal bounds.
 */

const fs = require('fs');
const path = require('path');

const STORE_PATH = path.resolve(__dirname, '../backend/data/store.json');
const DATA_DIR = path.dirname(STORE_PATH);

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

const DEMO_STORE = {
  sos_events: [
    {
      id: 'sos_001',
      user_id: 'usr001',
      user_name: 'Priya Sharma',
      phone: '+91-9876543210',
      ward: 'Ward 7 — Arera Colony',
      lat: 23.2599,
      lng: 77.4126,
      address: 'Near Arera Hills VIP Spine, Bhopal',
      battery_level: 34,
      heart_rate: 112,
      status: 'active',
      siren_type: 'CRITICAL',
      tracking_token: 'TRK-BHOPAL-01',
      contacts: ['+91-9876543220'],
      triggered_at: new Date(Date.now() - 3 * 60000).toISOString()
    },
    {
      id: 'sos_002',
      user_id: 'usr003',
      user_name: 'Ananya Singh',
      phone: '+91-9123456789',
      ward: 'Ward 3 — MP Nagar',
      lat: 23.2310,
      lng: 77.4350,
      address: 'Zone-I Commercial Complex, Bhopal',
      battery_level: 71,
      heart_rate: 94,
      status: 'acknowledged',
      siren_type: 'HIGH',
      tracking_token: 'TRK-BHOPAL-02',
      contacts: ['+91-9123456790'],
      triggered_at: new Date(Date.now() - 11 * 60000).toISOString()
    },
    {
      id: 'sos_003',
      user_id: 'usr007',
      user_name: 'Neha Verma',
      phone: '+91-9826112233',
      ward: 'Ward 18 — Rajwada',
      lat: 22.7185,
      lng: 75.8560,
      address: 'Rajwada Palace Chowk, Indore',
      battery_level: 18,
      heart_rate: 124,
      status: 'active',
      siren_type: 'CRITICAL',
      tracking_token: 'TRK-INDORE-01',
      contacts: ['+91-9826112244'],
      triggered_at: new Date(Date.now() - 1 * 60000).toISOString()
    }
  ],
  incidents: [
    {
      id: 'inc_001',
      type: 'ROAD_HAZARD',
      description: 'Severe deep asphalt pothole on TT Nagar overbridge causing vehicle skidding',
      lat: 23.2430,
      lng: 77.4190,
      address: 'TT Nagar Overbridge, Bhopal',
      ward: 'Ward 5 — TT Nagar',
      severity: 4,
      status: 'pending',
      ai_score: 0.88,
      ai_risk_level: 'HIGH',
      reported_by: 'usr002',
      reported_at: new Date(Date.now() - 15 * 60000).toISOString()
    },
    {
      id: 'inc_002',
      type: 'WATER_LOGGING',
      description: 'Severe underpass flooding — 2.5 feet water depth, traffic completely stalled',
      lat: 23.2680,
      lng: 77.4300,
      address: 'Habibganj Underpass, Bhopal',
      ward: 'Ward 9 — Habibganj',
      severity: 5,
      status: 'verified',
      ai_score: 0.95,
      ai_risk_level: 'CRITICAL',
      reported_by: 'usr004',
      reported_at: new Date(Date.now() - 28 * 60000).toISOString()
    },
    {
      id: 'inc_003',
      type: 'HARASSMENT',
      description: 'Suspicious youth group catcalling commuters near darkened bus shelter',
      lat: 23.2550,
      lng: 77.4050,
      address: 'Hamidia Road Transit Stand, Bhopal',
      ward: 'Ward 12 — Kolar',
      severity: 4,
      status: 'pending',
      ai_score: 0.79,
      ai_risk_level: 'HIGH',
      reported_by: 'usr005',
      reported_at: new Date(Date.now() - 7 * 60000).toISOString()
    },
    {
      id: 'inc_004',
      type: 'POOR_LIGHTING',
      description: 'Municipal streetlight array failed across 400m arterial segment',
      lat: 22.7533,
      lng: 75.8937,
      address: 'AB Road Corridor, Vijay Nagar, Indore',
      ward: 'Ward 24 — Vijay Nagar',
      severity: 3,
      status: 'verified',
      ai_score: 0.65,
      ai_risk_level: 'MODERATE',
      reported_by: 'usr008',
      reported_at: new Date(Date.now() - 65 * 60000).toISOString()
    }
  ],
  cctv_cameras: [
    {
      id: 'CAM-01',
      name: 'MP Nagar Zone-1 Chowk',
      ward: 'Ward 3 — MP Nagar',
      lat: 23.2340,
      lng: 77.4320,
      feed_url: '',
      status: 'online',
      fps: 30,
      resolution: '1080p',
      active_threat: null,
      threat_confidence: 0,
      detections: []
    },
    {
      id: 'CAM-02',
      name: 'TT Nagar Stadium North',
      ward: 'Ward 5 — TT Nagar',
      lat: 23.2430,
      lng: 77.4050,
      feed_url: '',
      status: 'alert',
      fps: 28,
      resolution: '1080p',
      active_threat: 'crowd_surge',
      threat_confidence: 0.91,
      detections: [{ label: 'crowd_surge', confidence: 0.91, bbox: [120, 80, 240, 160] }]
    },
    {
      id: 'CAM-03',
      name: 'Habibganj Underpass Flood Cam',
      ward: 'Ward 9 — Habibganj',
      lat: 23.2680,
      lng: 77.4300,
      feed_url: '',
      status: 'alert',
      fps: 28,
      resolution: '4K',
      active_threat: 'waterlogging',
      threat_confidence: 0.96,
      detections: [{ label: 'waterlogging', confidence: 0.96, bbox: [60, 140, 380, 200] }]
    },
    {
      id: 'CAM-04',
      name: 'Arera Hills VIP Spine',
      ward: 'Ward 7 — Arera Colony',
      lat: 23.2190,
      lng: 77.4250,
      feed_url: '',
      status: 'online',
      fps: 30,
      resolution: '4K',
      active_threat: null,
      threat_confidence: 0,
      detections: []
    },
    {
      id: 'CAM-05',
      name: 'Rajwada Historical Plaza',
      ward: 'Ward 18 — Rajwada',
      lat: 22.7185,
      lng: 75.8560,
      feed_url: '',
      status: 'online',
      fps: 30,
      resolution: '1080p',
      active_threat: null,
      threat_confidence: 0,
      detections: []
    },
    {
      id: 'CAM-06',
      name: 'Vijay Nagar Square',
      ward: 'Ward 24 — Vijay Nagar',
      lat: 22.7533,
      lng: 75.8937,
      feed_url: '',
      status: 'online',
      fps: 29,
      resolution: '1080p',
      active_threat: null,
      threat_confidence: 0,
      detections: []
    }
  ],
  hazard_zones: [
    {
      id: 'hz_001',
      name: 'Dynamic Flood Geofence — Habibganj',
      center_lat: 23.2680,
      center_lng: 77.4300,
      radius_meters: 450,
      risk_boost: 2.2,
      note: 'Automated IoT water depth escalation (>0.45m breach)',
      active: true,
      expires_at: new Date(Date.now() + 90 * 60000).toISOString(),
      created_at: new Date(Date.now() - 30 * 60000).toISOString()
    },
    {
      id: 'hz_002',
      name: 'Unlit Alleyway Alert — Kolar Transit',
      center_lat: 23.2550,
      center_lng: 77.4050,
      radius_meters: 300,
      risk_boost: 1.8,
      note: 'Multiple late-night harassment reports verified by AI',
      active: true,
      expires_at: new Date(Date.now() + 180 * 60000).toISOString(),
      created_at: new Date(Date.now() - 15 * 60000).toISOString()
    }
  ],
  wards: [
    { id: 'w1', ward_name: 'Ward 3 — MP Nagar', zone: 'Zone A', authority_name: 'Rajesh Kumar', complaint_count: 45, resolved_count: 38, budget_sanctioned: 2500000, budget_spent: 1800000, sla_breaches: 2, avg_resolution_hrs: 18, active_officers: 3, risk_level: 'MODERATE' },
    { id: 'w2', ward_name: 'Ward 7 — Arera Colony', zone: 'Zone B', authority_name: 'Priya Sharma', complaint_count: 62, resolved_count: 51, budget_sanctioned: 3200000, budget_spent: 2900000, sla_breaches: 5, avg_resolution_hrs: 24, active_officers: 5, risk_level: 'HIGH' },
    { id: 'w3', ward_name: 'Ward 9 — Habibganj', zone: 'Zone A', authority_name: 'Amit Singh', complaint_count: 28, resolved_count: 24, budget_sanctioned: 1800000, budget_spent: 1200000, sla_breaches: 1, avg_resolution_hrs: 12, active_officers: 2, risk_level: 'LOW' },
    { id: 'w4', ward_name: 'Ward 12 — Kolar', zone: 'Zone C', authority_name: 'Sunita Verma', complaint_count: 71, resolved_count: 49, budget_sanctioned: 4100000, budget_spent: 3600000, sla_breaches: 8, avg_resolution_hrs: 36, active_officers: 4, risk_level: 'CRITICAL' },
    { id: 'w5', ward_name: 'Ward 5 — TT Nagar', zone: 'Zone A', authority_name: 'Geeta Nair', complaint_count: 55, resolved_count: 40, budget_sanctioned: 2800000, budget_spent: 2100000, sla_breaches: 4, avg_resolution_hrs: 20, active_officers: 4, risk_level: 'MODERATE' },
    { id: 'w6', ward_name: 'Ward 18 — Rajwada', zone: 'Zone D', authority_name: 'Manish Joshi', complaint_count: 34, resolved_count: 29, budget_sanctioned: 2100000, budget_spent: 1750000, sla_breaches: 1, avg_resolution_hrs: 14, active_officers: 3, risk_level: 'MODERATE' },
    { id: 'w7', ward_name: 'Ward 24 — Vijay Nagar', zone: 'Zone E', authority_name: 'Kavita Patel', complaint_count: 52, resolved_count: 44, budget_sanctioned: 3500000, budget_spent: 3100000, sla_breaches: 3, avg_resolution_hrs: 16, active_officers: 4, risk_level: 'LOW' }
  ],
  responders: [
    { id: 'off001', name: 'Ravi Kumar', badge: 'MP-041', ward: 'Ward 7', phone: '+91-9001234567', status: 'on-duty', assigned_count: 8, resolved_count: 7, avg_response_min: 18 },
    { id: 'off002', name: 'Sunita Kamble', badge: 'MP-038', ward: 'Ward 9', phone: '+91-9001234568', status: 'on-duty', assigned_count: 5, resolved_count: 5, avg_response_min: 12 },
    { id: 'off003', name: 'Pradeep Yadav', badge: 'MP-052', ward: 'Ward 12', phone: '+91-9001234569', status: 'busy', assigned_count: 11, resolved_count: 7, avg_response_min: 34 },
    { id: 'off004', name: 'Anita Thakur', badge: 'MP-029', ward: 'Ward 3', phone: '+91-9001234570', status: 'on-duty', assigned_count: 4, resolved_count: 4, avg_response_min: 9 },
    { id: 'off005', name: 'Deepak Malviya', badge: 'MP-066', ward: 'Ward 18', phone: '+91-9001234571', status: 'on-duty', assigned_count: 3, resolved_count: 3, avg_response_min: 11 }
  ],
  users: [
    { id: 'usr001', name: 'Priya Sharma', phone: '+91-9876543210', ward: 'Ward 7', reports_count: 14, sos_count: 1, false_reports_count: 0, trust_score: 98, status: 'active', joined_at: Date.now() - 120 * 86400000 },
    { id: 'usr002', name: 'Mohit Agrawal', phone: '+91-9826011223', ward: 'Ward 5', reports_count: 9, sos_count: 0, false_reports_count: 0, trust_score: 92, status: 'active', joined_at: Date.now() - 95 * 86400000 },
    { id: 'usr003', name: 'Ananya Singh', phone: '+91-9123456789', ward: 'Ward 3', reports_count: 6, sos_count: 1, false_reports_count: 1, trust_score: 78, status: 'active', joined_at: Date.now() - 40 * 86400000 },
    { id: 'usr004', name: 'Vikram Chouhan', phone: '+91-9425099881', ward: 'Ward 9', reports_count: 22, sos_count: 0, false_reports_count: 0, trust_score: 99, status: 'active', joined_at: Date.now() - 210 * 86400000 },
    { id: 'usr005', name: 'Rohan Meena', phone: '+91-9893077665', ward: 'Ward 12', reports_count: 5, sos_count: 0, false_reports_count: 3, trust_score: 42, status: 'flagged', joined_at: Date.now() - 15 * 86400000 },
    { id: 'usr006', name: 'Karan Verma', phone: '+91-9755044332', ward: 'Ward 3', reports_count: 2, sos_count: 0, false_reports_count: 4, trust_score: 18, status: 'banned', joined_at: Date.now() - 60 * 86400000 }
  ],
  notifications: [
    { id: 'notif001', type: 'sos', title: 'Critical SOS Activated', message: 'Priya Sharma triggered high-priority SOS near Arera Hills (Ward 7). Responders alerted.', timestamp: Date.now() - 3 * 60000, read: false, priority: 'critical', target_page: 'sos' },
    { id: 'notif002', type: 'incident', title: 'Severe Waterlogging Verified', message: 'AI confirmed 2.5ft deep waterlogging at Habibganj Underpass. Dispatched to Ward 9 team.', timestamp: Date.now() - 8 * 60000, read: false, priority: 'high', target_page: 'incidents' },
    { id: 'notif003', type: 'alert', title: 'Dynamic Hazard Geofence Active', message: 'Active flood hazard polygon created at Habibganj Underpass with 450m radius.', timestamp: Date.now() - 15 * 60000, read: false, priority: 'high', target_page: 'hazards' }
  ]
};

fs.writeFileSync(STORE_PATH, JSON.stringify(DEMO_STORE, null, 2), 'utf-8');
console.log(`[Seed] Successfully seeded demonstration data to ${STORE_PATH}`);
console.log(`[Seed] 3 SOS beacons, 4 Incidents, 6 CCTV cameras, 2 Hazard zones, 7 Wards, 5 Responders, 6 Citizen profiles.`);
