# Persona walkthrough (simulated users)

`run.js` plays four simulated people (patience, scrolling and words they avoid differ) through the same
tasks on the HTML prototypes at 15:10. Each one taps only what is visible inside the phone, chooses the
control whose words match the goal, and gives up when out of taps. `audit.js` measures the first screen
(controls, words, font sizes, contrast, small targets).

Run from a folder of your choice: `node run.js` and `node audit.js` (Playwright; the prototypes are read
from `docs/design/`). Results are hypotheses, not real-user evidence; every "ok" was checked against its
screenshot, and wrong placements counted as failures (see DECISIONS.md, 2026-11-02).
