const fs = require('fs');
eval(fs.readFileSync('data.js', 'utf8').replace('const YAWM_TIMES', 'global.YAWM_TIMES'));
const E = require('./engine.js');
const csv = fs.readFileSync('../yawm/yawm-v2/docs/reference/reference-2026.csv', 'utf8').trim().split('\n').slice(1).map(l => l.trim().split(','));
const hm = v => v.slice(0,5);
const fmt = m => String(Math.floor(((m%1440)+1440)%1440/60)).padStart(2,'0') + ':' + String(((m%60)+60)%60).padStart(2,'0');
const caseMap = { 'sulaymaniyah-shafi': ['sul','SHAFI'], 'sulaymaniyah-hanafi': ['sul','HANAFI'], 'london-shafi': ['lon','SHAFI'] };
let bad = 0, n = 0;
for (const r of csv) {
  const [cs, date] = r; const [city, mad] = caseMap[cs];
  const doy = Math.round((Date.parse(date) - Date.UTC(2026,0,1)) / 864e5);
  const d = E.resolveDay(city, mad, doy);
  const p = d.plan;
  // reference: bedtime,wake,qiyam,night,nap,total,short at cols 8..14
  const got = [fmt(p.bedtime), fmt(p.wake), p.qiyam ? '1' : '0', String(p.night), String(p.nap), String(p.total), String(p.shortfall)];
  const exp = r.slice(8, 15);
  n++;
  // last day of year: reference uses next-day 2027 data we don't have
  if (got.join() !== exp.join()) { bad++; if (bad < 6) console.log('MISMATCH', cs, date, got.join(), '|', exp.join()); }
}
console.log('night plan rows', n, 'mismatches', bad);
const samples = JSON.parse(fs.readFileSync('../yawm/yawm-v2/docs/reference/sample-days.json', 'utf8'));
let sb = 0;
for (const [k, s] of Object.entries(samples)) {
  const [cs, date] = k.split(':'); const [city, mad] = caseMap[cs];
  const doy = Math.round((Date.parse(date) - Date.UTC(2026,0,1)) / 864e5);
  const d = E.resolveDay(city, mad, doy);
  const a = d.blocks.map(b => b.key + fmt(b.start) + fmt(b.end)).join(' ');
  const e = s.blocks.map(b => b.key + b.start + b.end).join(' ');
  if (a !== e) { sb++; console.log('BLOCKS DIFF', k, '\n', a, '\n', e); }
  if (d.notes.join() !== s.notes.join()) { sb++; console.log('NOTES DIFF', k, d.notes, s.notes); }
}
console.log('sample days', Object.keys(samples).length, 'diffs', sb);
// sundial vs table: solar noon vs dhuhr, and shadow-based asr vs table asr
let worstNoon = 0, worstAsr = 0;
for (const [city, mad, factor] of [['sul','SHAFI',1],['sul','HANAFI',2],['lon','SHAFI',1]]) {
  for (let doy = 0; doy < 365; doy++) {
    const t = E.times(city, mad, doy);
    let best = 0, bestAlt = -99;
    for (let m = t.dhuhr - 30; m <= t.dhuhr + 30; m += 0.25) { const s = E.sunAt(city, doy, m); if (s.alt > bestAlt) { bestAlt = s.alt; best = m; } }
    worstNoon = Math.max(worstNoon, Math.abs(t.dhuhr - best - 1)); // adhan adds +1 min to dhuhr for these methods
    const noonRatio = 1 / Math.tan(bestAlt * Math.PI / 180);
    let asr = null;
    for (let m = best; m < t.maghrib; m += 0.25) { const s = E.sunAt(city, doy, m); if (1 / Math.tan(s.alt * Math.PI / 180) >= factor + noonRatio) { asr = m; break; } }
    worstAsr = Math.max(worstAsr, Math.abs(asr - t.asr));
  }
}
console.log('sundial check: worst |dhuhr-1-transit|', worstNoon.toFixed(2), 'min; worst |shadow asr - table asr|', worstAsr.toFixed(2), 'min');
