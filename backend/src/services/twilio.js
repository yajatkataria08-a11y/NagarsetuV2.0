/**
 * NagarSetu V2.0 — Multi-Channel Emergency Telephony Grid (Phase 2)
 * ─────────────────────────────────────────────────────────────────
 * Ports Krisis's Twilio Voice TwiML Synthesizer and WhatsApp Business API.
 * Provides multi-tier automated fallback escalation:
 *   1. Immediate SMS to Emergency Contacts
 *   2. Instant WhatsApp Live Tracking Dispatch
 *   3. 60-second SLA monitor -> Auto-escalate to Hindi/English Voice IVR Call
 *   4. Broadcast escalation to all active Ward Field Officers
 */

const logger = require('../utils/logger');

const ACCOUNT_SID   = process.env.TWILIO_ACCOUNT_SID;
const AUTH_TOKEN    = process.env.TWILIO_AUTH_TOKEN;
const FROM_NUMBER   = process.env.TWILIO_FROM_NUMBER || '+15005550006';
const WHATSAPP_FROM = process.env.TWILIO_WHATSAPP_FROM || 'whatsapp:+14155238886';
const DEMO_MODE     = !ACCOUNT_SID || !AUTH_TOKEN || process.env.DEMO_MODE === 'true';

let client = null;
if (!DEMO_MODE) {
  try {
    const twilio = require('twilio');
    client = twilio(ACCOUNT_SID, AUTH_TOKEN);
    logger.info('[Telephony] Live Twilio Voice/SMS/WhatsApp gateway active');
  } catch (e) {
    logger.warn('[Telephony] Twilio SDK init error, using simulated gateway', { error: e.message });
  }
}

/**
 * 1. Send SMS via Twilio or Simulated Gateway
 */
async function sendSms(toNumber, messageBody) {
  const cleanNumber = String(toNumber).trim();
  if (DEMO_MODE || !client) {
    const sid = 'SMS_SIM_' + Date.now();
    logger.info(`[Telephony SIM-SMS] To: ${cleanNumber} | Body: ${messageBody.replace(/\n/g, ' ')}`);
    return { success: true, simulated: true, sid, to: cleanNumber };
  }

  try {
    const msg = await client.messages.create({
      body: messageBody,
      from: FROM_NUMBER,
      to: cleanNumber
    });
    logger.info('[Telephony SMS SENT]', { sid: msg.sid, to: cleanNumber });
    return { success: true, sid: msg.sid, to: cleanNumber };
  } catch (error) {
    logger.error('[Telephony SMS ERROR]', { error: error.message, to: cleanNumber });
    return { success: false, error: error.message, to: cleanNumber };
  }
}

/**
 * 2. Trigger Automated Voice IVR Alert with Polly.Aditi Hindi/English Speech
 */
async function triggerVoiceAlert(toNumber, speechText) {
  const cleanNumber = String(toNumber).trim();
  const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Aditi" language="hi-IN">${speechText}</Say>
  <Pause length="1"/>
  <Say voice="Polly.Aditi" language="hi-IN">NagarSetu Municipal Command Center. Please respond immediately.</Say>
</Response>`;

  if (DEMO_MODE || !client) {
    const sid = 'CALL_SIM_' + Date.now();
    logger.info(`[Telephony SIM-VOICE] Calling: ${cleanNumber} | Speech: "${speechText}" (TwiML Voice: Polly.Aditi)`);
    return { success: true, simulated: true, callSid: sid, twiml, to: cleanNumber };
  }

  try {
    const call = await client.calls.create({
      twiml: twiml,
      to: cleanNumber,
      from: FROM_NUMBER
    });
    logger.info('[Telephony VOICE CALL DISPATCHED]', { sid: call.sid, to: cleanNumber });
    return { success: true, callSid: call.sid, to: cleanNumber };
  } catch (error) {
    logger.error('[Telephony VOICE CALL ERROR]', { error: error.message, to: cleanNumber });
    return { success: false, error: error.message, to: cleanNumber };
  }
}

/**
 * 3. Send WhatsApp Alert with Live Tracking Link
 */
async function sendWhatsAppAlert(toNumber, messageBody) {
  let cleanNumber = String(toNumber).trim();
  if (!cleanNumber.startsWith('whatsapp:')) {
    cleanNumber = `whatsapp:${cleanNumber}`;
  }

  if (DEMO_MODE || !client) {
    const sid = 'WA_SIM_' + Date.now();
    logger.info(`[Telephony SIM-WHATSAPP] To: ${cleanNumber} | Body: ${messageBody.replace(/\n/g, ' ')}`);
    return { success: true, simulated: true, sid, to: cleanNumber };
  }

  try {
    const msg = await client.messages.create({
      body: messageBody,
      from: WHATSAPP_FROM,
      to: cleanNumber
    });
    logger.info('[Telephony WHATSAPP SENT]', { sid: msg.sid, to: cleanNumber });
    return { success: true, sid: msg.sid, to: cleanNumber };
  } catch (error) {
    logger.error('[Telephony WHATSAPP ERROR]', { error: error.message, to: cleanNumber });
    return { success: false, error: error.message, to: cleanNumber };
  }
}

/**
 * 4. Multi-Tier Automated Fallback Escalation (Krisis-Grade)
 *    Dispatches SMS + WhatsApp. If unacknowledged within 60s, triggers Voice IVR.
 */
function dispatchEmergencyEscalation(sosEvent, io = null) {
  const victimName = sosEvent.user_name || 'Citizen';
  const ward = sosEvent.ward || 'Bhopal Ward';
  const token = sosEvent.tracking_token || 'LIVE';
  const contacts = sosEvent.contacts || [];
  const trackingUrl = `http://localhost:3000/track/${token}`;

  const alertText = `🚨 NAGARSETU EMERGENCY SOS: ${victimName} triggered SOS in ${ward}. Live tracking: ${trackingUrl}. Heart rate: ${sosEvent.heart_rate || 110} BPM.`;
  const voiceText = `Apatkalin soochana. ${victimName} ne ${ward} mein emergency SOS trigger kiya hai. Krupiya turant sahayata bheje.`;

  // Step 1: Immediate SMS & WhatsApp Fanout
  for (const phone of contacts) {
    sendSms(phone, alertText);
    sendWhatsAppAlert(phone, alertText);
  }

  // Step 2: 60-Second Escalation Timer
  const timer = setTimeout(async () => {
    try {
      const db = require('./db');
      const currentList = await db.getSosEvents();
      const current = currentList.find(s => s.id === sosEvent.id);

      if (current && current.status === 'active') {
        logger.warn(`[Telephony ESCALATION] SOS ${sosEvent.id} unacknowledged after 60s! Triggering Voice IVR Call.`);
        
        // Voice call primary contact
        if (contacts.length > 0) {
          await triggerVoiceAlert(contacts[0], voiceText);
        }

        // Broadcast escalation event to WebSocket mesh
        if (io) {
          io.emit('sos:escalated', {
            sosId: sosEvent.id,
            victimName,
            ward,
            escalationLevel: 'VOICE_IVR_DISPATCHED',
            timestamp: Date.now()
          });
        }
      }
    } catch (err) {
      logger.error('[Telephony ESCALATION ERROR]', { error: err.message });
    }
  }, 60000);

  return { timer, alertText };
}

async function sendSmsToAll(numbers, messageBody) {
  const results = [];
  for (const num of numbers) {
    if (num && String(num).trim()) {
      results.push(await sendSms(num, messageBody));
    }
  }
  return results;
}

module.exports = {
  sendSms,
  sendSmsToAll,
  triggerVoiceAlert,
  sendWhatsAppAlert,
  dispatchEmergencyEscalation,
  isDemoMode: () => DEMO_MODE
};
