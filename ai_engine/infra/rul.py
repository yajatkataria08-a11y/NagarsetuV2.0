"""
Remaining useful life from Paris' law:  da/dN = C * (dSigma * Y * sqrt(pi * a))^m

Units: a in metres, dSigma in MPa, C in m/cycle per (MPa*sqrt(m))^m.
Typical structural steel: C ~ 2e-12, m ~ 3.

SCOPE WARNING - read before trusting the output:
  Paris' law describes fatigue crack growth in METAL (steel members, welds,
  plates). A surface crack measured on a concrete photo is not a fatigue crack
  in steel, so feeding photo-measured length in as a0 is only an illustration
  of the method. Treat P10/P50/P90 as scenario numbers, not a prediction.
"""
from __future__ import annotations

from typing import Any, Dict, Optional

import numpy as np

ASSUMPTIONS = {
    "model": "Paris' law, constant-amplitude loading, through-thickness crack, no retardation",
    "material": "structural steel: C ~ lognormal(median 2e-12, sigma_ln 0.5), m ~ N(3, 0.1)",
    "stress_range": "delta_sigma uniform +-10 % around the supplied value (MPa)",
    "geometry_factor_Y": 1.12,
    "units": "a [m], delta_sigma [MPa], C [m/cycle/(MPa sqrt(m))^m]",
    "scope": "Valid for fatigue cracks in steel. NOT validated for concrete cracks.",
}


def _integrate_cycles(a0: float, ac: float, C, m, dsig, Y: float, n_grid: int = 2000) -> np.ndarray:
    """N = integral_{a0}^{ac} da / (C (dsig Y sqrt(pi a))^m), trapezoid on a log grid.

    Vectorised over draws: C, m, dsig are arrays of equal length.
    """
    C, m, dsig = (np.atleast_1d(np.asarray(v, dtype=float)) for v in (C, m, dsig))
    ln_a = np.linspace(np.log(a0), np.log(ac), n_grid)
    a = np.exp(ln_a)[None, :]
    dK = dsig[:, None] * Y * np.sqrt(np.pi * a)
    f = a / (C[:, None] * dK ** m[:, None])            # integrand in ln(a): (da = a d ln a)
    trap = getattr(np, "trapezoid", None) or np.trapz
    return trap(f, ln_a, axis=1)


def closed_form_cycles_m3(a0: float, ac: float, C: float, dsig: float, Y: float) -> float:
    """Exact N for m = 3:  N = 2 (a0^-1/2 - ac^-1/2) / (C (dsig Y sqrt(pi))^3)."""
    return 2.0 * (a0 ** -0.5 - ac ** -0.5) / (C * (dsig * Y * np.sqrt(np.pi)) ** 3)


def estimate_rul(a0_m: float, a_c_m: float = 0.05, delta_sigma_mpa: float = 50.0,
                 cycles_per_day: float = 10_000.0, Y: float = 1.12, n_draws: int = 2000,
                 seed: Optional[int] = 0) -> Dict[str, Any]:
    if a0_m <= 0 or a_c_m <= 0:
        raise ValueError("crack lengths must be positive")
    if delta_sigma_mpa <= 0 or cycles_per_day <= 0:
        raise ValueError("delta_sigma and cycles_per_day must be positive")
    if a0_m >= a_c_m:
        return {"p10_years": 0.0, "p50_years": 0.0, "p90_years": 0.0, "already_critical": True,
                "a0_m": a0_m, "a_c_m": a_c_m, "assumptions": ASSUMPTIONS, "draws": 0}
    rng = np.random.default_rng(seed)
    C = rng.lognormal(mean=np.log(2e-12), sigma=0.5, size=n_draws)
    m = rng.normal(3.0, 0.1, size=n_draws)
    dsig = delta_sigma_mpa * rng.uniform(0.9, 1.1, size=n_draws)
    cycles = _integrate_cycles(a0_m, a_c_m, C, m, dsig, Y)
    years = cycles / cycles_per_day / 365.25
    p10, p50, p90 = (float(np.percentile(years, q)) for q in (10, 50, 90))
    return {"p10_years": round(p10, 3), "p50_years": round(p50, 3), "p90_years": round(p90, 3),
            "already_critical": False, "a0_m": a0_m, "a_c_m": a_c_m, "delta_sigma_mpa": delta_sigma_mpa,
            "cycles_per_day": cycles_per_day, "draws": n_draws, "assumptions": ASSUMPTIONS}
