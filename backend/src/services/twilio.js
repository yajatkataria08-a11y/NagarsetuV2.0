// repo_v2/backend/src/services/twilio.js
// Twilio SMS & Voice escalation (Inspired by Raksha & Krisis)

const logger = require('../utils/logger');

const ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID;
const AUTH_TOKEN  = process.env.TWILIO_AUTH_TOKEN;
const FROM_NUMBER = process.env.TWILIO_FROM_NUMBER;
const DEMO_MODE   = !ACCOUNT_SID || !AUTH_TOKEN || process.env.DEMO_MODE === 'true';

let client = null;
if (!DEMO_MODE) {
  try {
    const twilio = require('twilio');
    client = twilio(ACCOUNT_SID, AUTH_TOKEN);
    logger.info('[Twilio] Live SMS gateway initialized');
  } catch (e) {
    logger.warn('[Twilio] Package initialization error, falling back to simulated mode', { error: e.message });
  }
}

async function sendSms(toNumber, messageBody) {
  if (DEMO_MODE || !client) {
    logger.info(`[Twilio SIMULATED SMS] To: ${toNumber} | Message: ${messageBody.replace(/\n/g, ' ')}`);
    return { success: true, simulated: true, sid: 'SIM_' + Date.now() };
  }

  try {
    const message = await client.messages.create({
      body: messageBody,
      from: FROM_NUMBER,
      to: toNumber
    });
    logger.info('[Twilio SMS SENT]', { sid: message.sid, to: toNumber });
    return { success: true, sid: message.sid };
  } catch (error) {
    logger.error('[Twilio SMS FAILED]', { error: error.message, to: toNumber });
    return { success: false, error: error.message };
  }
}

async function sendSmsToAll(numbers, messageBody) {
  const results = [];
  for (const num of numbers) {
    if (num && num.trim()) {
      results.push(await sendSms(num.trim(), messageBody));
    }
  }
  return results;
}

module.exports = {
  sendSms,
  sendSmsToAll,
  isDemoMode: () => DEMO_MODE
};
