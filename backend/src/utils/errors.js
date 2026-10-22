/**
 * Safe Error Handler & Response Sanitizer
 * ───────────────────────────────────────
 * Prevents internal server details, database paths, and sensitive stack traces
 * from leaking to API clients in production environments.
 */

const logger = require('./logger');

/**
 * Sends a sanitized error response to the client.
 * In development, returns the actual error message.
 * In production, returns a generic user-friendly message.
 */
function sendError(res, err, statusCode = 500, fallback = 'Internal server error') {
  const isDev = process.env.NODE_ENV !== 'production';
  const errorMessage = err?.message || fallback;

  logger.error(`[API Error ${statusCode}]`, {
    error: errorMessage,
    stack: err?.stack,
    code: err?.code
  });

  return res.status(statusCode).json({
    error: isDev ? errorMessage : fallback,
    statusCode
  });
}

module.exports = { sendError };
