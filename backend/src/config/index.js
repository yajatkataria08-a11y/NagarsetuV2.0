/**
 * NagarSetu V2.0 — Cross-Platform Dynamic Configuration & Runtime Resolver
 * ────────────────────────────────────────────────────────────────────────
 * Implements 12-factor application configuration with Zod validation,
 * dynamic cross-platform Python binary discovery, and directory path resolution.
 */

const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');
const { z } = require('zod');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });

/**
 * Dynamically resolves an operational Python binary across Windows, Linux, macOS, and Docker.
 */
function resolvePythonBin() {
  // 1. Explicit environment variable override
  if (process.env.PYTHON_BIN) {
    if (fs.existsSync(process.env.PYTHON_BIN)) {
      return process.env.PYTHON_BIN;
    }
    // If it's a binary name like 'python3' available in PATH
    try {
      const probe = process.platform === 'win32' ? 'where' : 'which';
      const resolved = execSync(`${probe} ${process.env.PYTHON_BIN}`, { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim().split(/\r?\n/)[0];
      if (resolved && fs.existsSync(resolved)) {
        return resolved;
      }
    } catch (_) {}
    return process.env.PYTHON_BIN;
  }

  // 2. Platform-specific known candidate paths
  const isWin = process.platform === 'win32';
  const candidates = isWin
    ? [
        process.env.LOCALAPPDATA ? path.join(process.env.LOCALAPPDATA, 'Programs', 'Python', 'Python311', 'python.exe') : null,
        process.env.LOCALAPPDATA ? path.join(process.env.LOCALAPPDATA, 'Programs', 'Python', 'Python312', 'python.exe') : null,
        process.env.LOCALAPPDATA ? path.join(process.env.LOCALAPPDATA, 'Programs', 'Python', 'Python310', 'python.exe') : null,
        'C:\\Program Files\\Python311\\python.exe',
        'C:\\Program Files\\Python312\\python.exe',
        'C:\\Python311\\python.exe',
        'C:\\Python312\\python.exe',
        'python',
        'py'
      ].filter(Boolean)
    : [
        '/usr/bin/python3',
        '/usr/local/bin/python3',
        'python3',
        'python'
      ];

  for (const candidate of candidates) {
    if (path.isAbsolute(candidate) && fs.existsSync(candidate)) {
      return candidate;
    }
  }

  // 3. Probe PATH using which/where
  for (const bin of (isWin ? ['python', 'py', 'python3'] : ['python3', 'python'])) {
    try {
      const probe = isWin ? 'where' : 'which';
      const resolved = execSync(`${probe} ${bin}`, { stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim().split(/\r?\n/)[0];
      if (resolved && (path.isAbsolute(resolved) ? fs.existsSync(resolved) : true)) {
        return resolved;
      }
    } catch (_) {}
  }

  return isWin ? 'python' : 'python3';
}

// Zod Environment Schema
const EnvSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().default(4000),
  HOST: z.string().default('0.0.0.0'),
  FRONTEND_URL: z.string().default('http://localhost:3000'),
  AI_SERVICE_URL: z.string().default('http://127.0.0.1:8000'),
  PYTHON_BIN: z.string().default(() => resolvePythonBin()),
  DISABLE_AUTH: z.enum(['true', 'false']).default('false'),
  JWT_SECRET: z.string().default('nagarsetu_dev_secret_change_in_production'),
  SENSOR_API_KEY: z.string().default('nagarsetu_iot_sensor_key_2026'),
  SUPABASE_URL: z.string().optional(),
  SUPABASE_SERVICE_ROLE_KEY: z.string().optional(),
  TWILIO_ACCOUNT_SID: z.string().optional(),
  TWILIO_AUTH_TOKEN: z.string().optional(),
  TWILIO_PHONE_NUMBER: z.string().optional(),
  DATA_DIR: z.string().default(() => path.resolve(__dirname, '../../data')),
  AI_ENGINE_DIR: z.string().default(() => path.resolve(__dirname, '../../../ai_engine')),
});

const parsedEnv = EnvSchema.safeParse(process.env);
if (!parsedEnv.success) {
  const errorDetails = JSON.stringify(parsedEnv.error.format(), null, 2);
  console.error('[Config] ❌ Invalid environment configuration:\n', errorDetails);
  if (process.env.NODE_ENV === 'production') {
    throw new Error(`[Config Fatal] Invalid environment configuration in production:\n${errorDetails}`);
  }
}

const env = parsedEnv.success ? parsedEnv.data : EnvSchema.parse({});

if (env.NODE_ENV === 'production' && env.JWT_SECRET === 'nagarsetu_dev_secret_change_in_production') {
  throw new Error('[Config Fatal] JWT_SECRET must be set to a secure custom secret in production environment!');
}

// Derived and normalized paths
const config = {
  ...env,
  isProduction: env.NODE_ENV === 'production',
  isDevelopment: env.NODE_ENV === 'development',
  DATA_DIR: path.resolve(env.DATA_DIR),
  STORE_FILE: path.join(path.resolve(env.DATA_DIR), 'store.json'),
  AI_ENGINE_DIR: path.resolve(env.AI_ENGINE_DIR),
  AI_SCRIPT: path.join(path.resolve(env.AI_ENGINE_DIR), 'unified_civic_ai.py'),
  PUBLIC_DIR: path.resolve(__dirname, '../../public'),
  STREAMS_DIR: path.resolve(__dirname, '../../public/streams'),
  resolvePath: (...segments) => path.resolve(__dirname, '../..', ...segments),
  resolvePythonBin
};

// Ensure critical directories exist
try {
  if (!fs.existsSync(config.DATA_DIR)) fs.mkdirSync(config.DATA_DIR, { recursive: true });
  if (!fs.existsSync(config.PUBLIC_DIR)) fs.mkdirSync(config.PUBLIC_DIR, { recursive: true });
  if (!fs.existsSync(config.STREAMS_DIR)) fs.mkdirSync(config.STREAMS_DIR, { recursive: true });
} catch (e) {
  console.warn('[Config] Notice: Directory auto-creation skipped:', e.message);
}

module.exports = config;
