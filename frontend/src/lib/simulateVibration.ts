/**
 * Browser-side SIMULATED vibration generator for demos.
 *
 * This is NOT sensor data. It produces a noise-driven damped oscillator like the
 * one the anomaly model was trained on (ai_engine/infra/vibration_data.py), so the
 * pipeline can be exercised end-to-end without hardware. Anything it produces must
 * be labelled simulated in the UI.
 */
export const SIM_FS = 128;
const F0 = 5.0;
const ZETA = 0.02;

function gauss(): number {
  let u = 0;
  while (u === 0) u = Math.random();
  return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * Math.random());
}

/** AR(2) resonator: poles at r*exp(+-i*theta), driven by white noise. */
function resonator(n: number, f0: number, zeta: number): Float64Array {
  const w = 2 * Math.PI * f0;
  const r = Math.exp((-zeta * w) / SIM_FS);
  const theta = (w * Math.sqrt(1 - zeta * zeta)) / SIM_FS;
  const a1 = 2 * r * Math.cos(theta);
  const a2 = -r * r;
  const burn = 5 * SIM_FS;
  const out = new Float64Array(n);
  let y1 = 0, y2 = 0;
  for (let i = 0; i < n + burn; i++) {
    const y = a1 * y1 + a2 * y2 + gauss();
    y2 = y1; y1 = y;
    if (i >= burn) out[i - burn] = y;
  }
  return out;
}

// One fixed gain (from the HEALTHY configuration) so damage is not re-normalised away.
const GAIN = (() => {
  const x = resonator(20000, F0, ZETA);
  let s = 0;
  for (const v of x) s += v * v;
  return 1.7 / Math.sqrt(s / x.length);
})();

/** @param stiffnessLoss 0 = healthy, 0.3 = 30 % stiffness loss */
export function simulateWindow(stiffnessLoss: number, seconds = 10): number[] {
  const n = Math.round(seconds * SIM_FS);
  const f0 = F0 * (1 + (Math.random() * 0.04 - 0.02)) * Math.sqrt(1 - stiffnessLoss);
  const x = resonator(n, f0, ZETA * (1 + 2 * stiffnessLoss));
  const out: number[] = new Array(n);
  for (let i = 0; i < n; i++) out[i] = Math.round((x[i] * GAIN + 0.05 * gauss()) * 1e4) / 1e4;
  return out;
}
