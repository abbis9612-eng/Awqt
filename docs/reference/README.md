# Reference data — executable specification

These files define the day algorithm exactly. The Kotlin implementation must reproduce them.

| File | What it is |
| --- | --- |
| `resolver_reference.py` | Python reference: night balance, segment fitting, invariants, and the simulation that generates the two data files |
| `reference-2026.csv` | 1,095 rows (365 days × 3 cases): prayer times, bedtime, wake, qiyam, night sleep, nap, total sleep, shortfall. `+1` = after midnight |
| `sample-days.json` | Fully resolved days, block by block |

Cases: Sulaymaniyah (35.5613, 45.4375, Asia/Baghdad) Egyptian method with Shafi and with Hanafi Asr;
London (51.5074, −0.1278, Europe/London) Muslim World League with the seventh-of-the-night rule.

Result on 2026-10-20: 0 invariant failures in all 1,095 days. Days where 7 h of sleep cannot be
reached even with a 90-min nap are reported, not hidden: 71 in Sulaymaniyah (max 29 min short), 89 in London.

Regenerate: `pip install adhanpy` then `python resolver_reference.py` (writes to `out/`).
