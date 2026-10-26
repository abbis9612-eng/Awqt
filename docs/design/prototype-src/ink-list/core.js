/* ================= shared core ================= */
const $ = (id) => document.getElementById(id);
const REDUCE = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const MONTHS = ['كانون الثاني','شباط','آذار','نيسان','أيار','حزيران','تموز','آب','أيلول','تشرين الأول','تشرين الثاني','كانون الأول'];
const DAYS = ['الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة','السبت'];
const pad = (n) => String(n).padStart(2, '0');
const fmt = (m) => { m = Math.round(m); m = ((m % 1440) + 1440) % 1440; return pad(Math.floor(m / 60)) + ':' + pad(m % 60); };
const hoursW = (h) => h === 1 ? 'ساعة' : h === 2 ? 'ساعتين' : h <= 10 ? h + ' ساعات' : h + ' ساعة';
const minsW = (m) => m === 1 ? 'دقيقة' : m === 2 ? 'دقيقتين' : m <= 10 ? m + ' دقائق' : m + ' دقيقة';
const durText = (x) => { x = Math.max(0, Math.round(x)); const h = Math.floor(x / 60), m = x % 60; return !h ? minsW(m) : !m ? hoursW(h) : hoursW(h) + ' و' + minsW(m); };
const durShort = (x) => { x = Math.max(0, Math.round(x)); const h = Math.floor(x / 60), m = x % 60; return h && m ? `${h} س ${m} د` : h ? `${h} س` : `${m} د`; };
const hm = (x) => `${Math.floor(x / 60)}:${pad(Math.round(x) % 60)}`;
const dateOf = (doy) => new Date(Date.UTC(2026, 0, 1 + doy));
const doyOf = (y, m, d) => Math.round((Date.UTC(y, m - 1, d) - Date.UTC(2026, 0, 1)) / 864e5);
const hijriF = new Intl.DateTimeFormat('ar-u-ca-islamic-umalqura-nu-latn', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });
const hijri = (doy) => { const p = Object.fromEntries(hijriF.formatToParts(dateOf(doy)).map((x) => [x.type, x.value])); return `${p.day} ${p.month} ${String(p.year).replace(/[^0-9]/g, '')}`; };
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const store = { get(k, d) { try { const v = localStorage.getItem('yawm3:' + k); return v ? JSON.parse(v) : d; } catch (e) { return d; } }, set(k, v) { try { localStorage.setItem('yawm3:' + k, JSON.stringify(v)); } catch (e) {} } };
const buzz = () => { try { navigator.vibrate && navigator.vibrate(12); } catch (e) {} };

const CITY = 'sul', MADHAB = 'SHAFI';
const TRAIN = { 6: 'مشي أو راحة', 0: 'تدريب قوة', 1: 'تعلم تقني', 2: 'تدريب قوة', 3: 'مشي سريع 40 دقيقة', 4: 'تدريب قوة' };
function cityNow() {
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Baghdad', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).formatToParts(new Date()).map((x) => [x.type, x.value]));
  const y = +p.year, doy = y === 2026 ? doyOf(y, +p.month, +p.day) : doyOf(2026, 10, 26);
  return { doy, min: +p.hour * 60 + +p.minute, sec: +p.second };
}

const C = {};
const G = { k: store.get('k', 'A'), preset: 'now', shift: 0, ui: {} };
const PRESETS = [['now', 'الآن'], ['fajr', 'الفجر'], ['morning', 'الضحى'], ['dhuhr', 'الظهر'], ['asr', 'العصر'], ['maghrib', 'المغرب'], ['evening', 'المساء'], ['sleep', 'نايم']];

function model() {
  const n = cityNow();
  let doy = n.doy + G.shift, t;
  const tt0 = times(CITY, MADHAB, doy);
  const at = { fajr: tt0.fajr + 8, morning: 640, dhuhr: tt0.dhuhr + 12, asr: tt0.asr + 25, maghrib: tt0.maghrib + 8, evening: tt0.isha + 75, sleep: 1440 + 70 };
  if (G.preset === 'now') { t = n.min; if (G.shift === 0 && t < tt0.fajr) { doy -= 1; t += 1440; } }
  else if (G.preset === 'custom') t = G.tmin;
  else t = at[G.preset];
  doy = Math.max(0, Math.min(363, doy));
  const d = resolveDay(CITY, MADHAB, doy);
  const tn = TRAIN[weekdayOf(doy)]; d.blocks.forEach((b) => { if (b.key === 'train' && tn) b.title = tn; });
  const tt = d.times, nx = times(CITY, MADHAB, doy + 1), plan = d.plan;
  const ext = d.blocks.filter((b) => b.key !== 'qiyam').map((b) => ({ ...b }));
  ext.push({ key: 'sleep', title: 'النوم', role: 'BODY', start: plan.bedtime, end: plan.wake, sleep: true });
  if (plan.qiyam) ext.push({ key: 'qiyam2', title: 'السَّحَر: ركعتان والوتر', role: 'WORSHIP', start: plan.wake, end: nx.fajr + 1440 });
  const curI = ext.findIndex((b) => b.start <= t && t < b.end);
  const PR = [['fajr', 'الفجر', tt.fajr], ['sunrise', 'الشروق', tt.sunrise], ['dhuhr', d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr], ['asr', 'العصر', tt.asr], ['maghrib', 'المغرب', tt.maghrib], ['isha', 'العشاء', tt.isha]];
  const PRAY = PR.filter(([k]) => k !== 'sunrise').concat([['fajr2', 'الفجر', nx.fajr + 1440]]);
  const nxt = PRAY.find(([, , m]) => m > t);
  const dt = dateOf(doy);
  const done = new Set(store.get('done:' + doy, []));
  return { doy, t, sec: G.preset === 'now' ? n.sec : 0, live: G.preset === 'now' && G.shift === 0, d, tt, nx, plan, ext, curI, cur: ext[curI] || null,
    PR, next: nxt ? { key: nxt[0], name: nxt[1], m: nxt[2] } : null, asleep: t >= plan.bedtime,
    day: DAYS[dt.getUTCDay()], dayNum: dt.getUTCDate(), month: MONTHS[dt.getUTCMonth()], year: 2026, hijri: hijri(doy), city: CITIES[CITY].name, done };
}
function markDone(M, key) {
  const s = new Set(store.get('done:' + M.doy, []));
  if (s.has(key)) s.delete(key); else s.add(key);
  store.set('done:' + M.doy, [...s]); buzz();
}
/* what the main button should say for the current block */
function doneLabel(b) { return !b ? 'تم' : b.isPrayer ? 'صلّيت' : b.sleep ? 'صحيت' : 'خلّصت'; }
const isPast = (M, b) => b.end <= M.t;

function renderAll(keepScroll) {
  const k = G.k, M = model(), root = $('screen');
  const y = window.scrollY;
  root.className = 'phone c' + k; root.innerHTML = '';
  C[k].render(root, M);
  $('about').textContent = C[k].about;
  document.querySelectorAll('#pick button').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.k === k)));
  const sl = $('scrub'); if (document.activeElement !== sl) sl.value = String(M.t < 240 ? M.t + 1440 : M.t);
  $('scrubT').textContent = G.preset === 'now' ? 'الآن ' + fmt(M.t) : fmt(M.t);
  $('nowBtn').hidden = G.preset === 'now';
  if (keepScroll) window.scrollTo(0, y);
}
function boot() {
  $('pick').innerHTML = Object.entries(C).map(([k, c]) => `<button data-k="${k}" aria-pressed="false">${c.name}</button>`).join('');
  $('pick').onclick = (e) => { const b = e.target.closest('button'); if (!b) return; G.k = b.dataset.k; store.set('k', G.k); renderAll(true); };
  $('scrub').oninput = (e) => { G.preset = 'custom'; G.tmin = +e.target.value; renderAll(true); };
  $('nowBtn').onclick = () => { G.preset = 'now'; renderAll(true); };
  if (!C[G.k]) G.k = 'A';
  renderAll();
  setInterval(() => { if (G.preset === 'now' && new Date().getSeconds() === 0) renderAll(true); }, 1000);
}

/* ---- shared bits for the calm designs ---- */
/* what the big line at the top talks about: next prayer, or waking time while asleep */
function heroOf(M) {
  if (M.cur && M.cur.sleep) return { name: M.plan.qiyam ? 'السَّحَر' : 'الكعدة', m: M.plan.wake };
  return M.next ? { name: M.next.name, m: M.next.m } : null;
}
const upcoming = (M, n) => M.ext.filter((b) => b.start > M.t).slice(0, n);
function doneBtn(M, cls) {
  const b = M.cur; if (!b) return '';
  const d = M.done.has(b.key);
  return `<button class="${cls}${d ? ' is-done' : ''}" data-done>${d ? '✓ تم' : doneLabel(b)}</button>`;
}
function bindDone(root, M) {
  root.onclick = (e) => {
    if (e.target.closest('[data-done]') && M.cur) { markDone(M, M.cur.key); renderAll(true); return; }
    const x = e.target.closest('[data-x]'); if (x) { (G.ui[G.k] || (G.ui[G.k] = {}))[x.dataset.x] = !(G.ui[G.k] || {})[x.dataset.x]; renderAll(true); }
  };
}

/* ---- the ink word: next prayer's name fills as its time approaches ---- */
function inkState(M) {
  const P = M.PR.filter(([k]) => k !== 'sunrise');
  const all = [['isha0', 'العشاء', M.tt.isha - 1440], ...P, ['fajr2', 'الفجر', M.nx.fajr + 1440]];
  let i = all.findIndex(([, , m]) => m > M.t); if (i < 1) i = 1;
  const wake = M.cur && M.cur.sleep;
  const word = wake ? (M.plan.qiyam ? 'السَّحَر' : 'الكعدة') : all[i][1];
  const tgt = wake ? M.plan.wake : all[i][2], from = wake ? M.plan.bedtime : all[i - 1][2];
  return { word, tgt, key: wake ? 'wake' : all[i][0], p: Math.max(0, Math.min(1, (M.t - from) / (tgt - from))) * 100 };
}
const inkSpan = (s, cls = 'ink') => `<span class="${cls}" style="--p:${s.p.toFixed(1)}%">${s.word}</span>`;

/* ---- iOS-like chrome ---- */
const IC = {
  sun: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.3 5.3l1.6 1.6M17.1 17.1l1.6 1.6M5.3 18.7l1.6-1.6M17.1 6.9l1.6-1.6"/>',
  rise: '<path d="M4 18h16M7.5 18a4.5 4.5 0 0 1 9 0M12 4v6M9 7l3-3 3 3M3.5 13.5l1.5 1M20.5 13.5l-1.5 1"/>',
  set: '<path d="M4 18h16M7.5 18a4.5 4.5 0 0 1 9 0M12 10V4M9 7l3 3 3-3M3.5 13.5l1.5 1M20.5 13.5l-1.5 1"/>',
  moon: '<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/>',
  bed: '<path d="M3 18V7M3 14h18v4M21 14v-2.5a3 3 0 0 0-3-3h-7v5.5M7 11.2a1.7 1.7 0 1 0 0-.01"/>',
  list: '<path d="M9 6h11M9 12h11M9 18h11"/><circle cx="4.5" cy="6" r="1.2"/><circle cx="4.5" cy="12" r="1.2"/><circle cx="4.5" cy="18" r="1.2"/>',
  cal: '<rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
  doc: '<path d="M6 3.5h8l4 4V20.5H6z"/><path d="M14 3.5v4h4M9 12h6M9 16h6"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  dawn: '<path d="M4 18h16M7.5 18a4.5 4.5 0 0 1 9 0"/><path d="M5 9.5l1.4 1.4M19 9.5l-1.4 1.4M12 7v2"/>',
};
const icon = (n, s = 22, w = 1.8) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${IC[n]}</svg>`;
const prayerIcon = (k) => ({ fajr: 'dawn', sunrise: 'rise', dhuhr: 'sun', asr: 'sun', maghrib: 'set', isha: 'moon' }[k] || 'sun');
function statusBar(M) {
  return `<div class="ios-sb"><b>${fmt(M.t)}</b><span><svg width="18" height="11" viewBox="0 0 18 11" fill="currentColor" aria-hidden="true"><rect x="0" y="7" width="3" height="4" rx="1"/><rect x="5" y="5" width="3" height="6" rx="1"/><rect x="10" y="2.5" width="3" height="8.5" rx="1"/><rect x="15" y="0" width="3" height="11" rx="1"/></svg><svg width="26" height="12" viewBox="0 0 26 12" aria-hidden="true"><rect x=".5" y=".5" width="22" height="11" rx="3.5" fill="none" stroke="currentColor" opacity=".4"/><rect x="2" y="2" width="16" height="8" rx="2" fill="currentColor"/><rect x="23.5" y="4" width="1.8" height="4" rx=".9" fill="currentColor" opacity=".4"/></svg></span></div>`;
}
function tabBar(active, dark) {
  const T = [['today', 'اليوم', 'dhuhr'], ['night', 'الليل', 'isha'], ['week', 'الأسبوع', 'week'], ['page', 'المحاسبة', 'page']];
  return `<nav class="ios-tab${dark ? ' dark' : ''}" aria-label="الأقسام">${T.map(([k, n, i]) => `<span class="${k === active ? 'on' : ''}"${k === active ? ` style="--ic:${ICOL[i]}"` : ''}>${yi(i, 26, 1.6)}<b>${n}</b></span>`).join('')}</nav>`;
}
/* ---- «يوم» icon family: 24 grid, 1.7 stroke, round ends, and exactly one part filled with ink ---- */
const F = 'style="fill:var(--ic,currentColor);stroke:none"';
const YI = {
  fajr: `<path d="M3 17h18M7 20.5h10M12 7.5v2M5.6 10.6l1.3 1.3M18.4 10.6l-1.3 1.3"/><path ${F} d="M7.4 17a4.6 4.6 0 0 1 9.2 0z"/>`,
  dhuhr: `<path d="M4 19.5h16M12 3.2v1.6M5 10h1.6M17.4 10H19M7.1 5.1l1.1 1.1M16.9 5.1l-1.1 1.1M7.1 14.9l1.1-1.1M16.9 14.9l-1.1-1.1"/><circle ${F} cx="12" cy="10" r="3.6"/>`,
  asr: `<path d="M3 19.5h18M10 19.5V8.5M5.2 1.8v1M1.3 5.8h1M2.4 3l.7.7M8 3l-.7.7"/><path ${F} d="M10 18.6h10.2l.6 1.8H10z"/><circle cx="5.2" cy="5.8" r="1.9"/>`,
  maghrib: `<path d="M3 15.5h18M6.5 18.5h11M9.5 21h5M12 4.5v5M9.8 7.6l2.2 2.2 2.2-2.2"/><path ${F} d="M7.4 15.5a4.6 4.6 0 0 1 9.2 0z"/>`,
  isha: `<path d="M18.6 3.6l.6 1.4 1.5.2-1.1 1 .3 1.5-1.3-.8-1.3.8.3-1.5-1.1-1 1.5-.2z"/><path ${F} d="M13.4 5.2A7.6 7.6 0 1 0 19.6 16a6.1 6.1 0 0 1-6.2-10.8z"/>`,
  mosque: `<path d="M3 20.5h18M6 20.5v-6a6 6 0 0 1 12 0v6M12 8.5V6.6M20.5 20.5V10M19.6 10h1.8M20.5 10V7.6"/><path ${F} d="M10.5 20.5v-3.2a1.5 1.5 0 0 1 3 0v3.2z"/>`,
  rahl: `<path d="M6.5 20.5l11-8.2M17.5 20.5l-11-8.2M12 9.6v6.2"/><path ${F} d="M3.8 8.8c2.9-1.5 5.6-1.4 8.2.7 2.6-2.1 5.3-2.2 8.2-.7l-1.1 2.3c-2.4-1.1-4.7-.9-7.1.9-2.4-1.8-4.7-2-7.1-.9z"/>`,
  steps: `<path d="M12.6 8.2l-2.2 5.4 3.1 2.6 1 5M10.4 13.6L8 21M12.4 8.6l3.1 2.6 2.6.5M12.4 8.6l-3.3 2-1.5 3"/><circle ${F} cx="13.6" cy="4.6" r="2"/>`,
  nib: `<path d="M12 21l-5.2-9.2 2-6.3h6.4l2 6.3zM12 12.6v5M8.8 3.5h6.4"/><circle ${F} cx="12" cy="11.4" r="1.6"/>`,
  tea: `<path d="M8 5h8c0 2.6-1.6 4.4-1.6 6.6s1.4 4.2 1.2 7.2H8.4c-.2-3 1.2-5 1.2-7.2S8 7.6 8 5zM5 21h14M11 1.6c-.6.7.6 1.3 0 2M13.4 1.6c-.6.7.6 1.3 0 2"/><path ${F} d="M9.8 12.4h4.4c.3 1.8 1 3.5.9 5.6H8.9c-.1-2.1.6-3.8.9-5.6z"/>`,
  mail: `<rect x="3.5" y="6.5" width="15.5" height="12" rx="2.4"/><path d="M4 7.5l7.3 5.6 7.2-5.6"/><circle ${F} cx="19.5" cy="6" r="2.7"/>`,
  phone: `<path d="M14.6 3.6a6 6 0 0 1 5.8 5.8M14.6 7a2.6 2.6 0 0 1 2.4 2.4"/><path ${F} d="M6.6 3.5h2.5l1.5 4-2 1.5a12 12 0 0 0 6.4 6.4l1.5-2 4 1.5v2.5a2.1 2.1 0 0 1-2.2 2.1A16.6 16.6 0 0 1 4.5 5.7a2.1 2.1 0 0 1 2.1-2.2z"/>`,
  case: `<rect x="3.5" y="7.5" width="17" height="12.5" rx="2.4"/><path d="M9 7.5V6a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 6v1.5M3.5 13h7M13.5 13h7"/><rect ${F} x="10.3" y="11.4" width="3.4" height="3.4" rx=".9"/>`,
  nap: `<path d="M13.6 3.5h4.2l-4.2 4.2h4.2"/><path ${F} d="M3.6 15.2c0-3.1 2.3-5 8.4-5s8.4 1.9 8.4 5-2.3 4.3-8.4 4.3-8.4-1.2-8.4-4.3z"/>`,
  dumbbell: `<path d="M7.5 12h9M2.8 10.3v3.4M21.2 10.3v3.4"/><rect ${F} x="4.2" y="7.6" width="3.3" height="8.8" rx="1.1"/><rect x="16.5" y="7.6" width="3.3" height="8.8" rx="1.1"/>`,
  laptop: `<rect x="5" y="4.5" width="14" height="10.5" rx="1.6"/><path d="M10.2 8l-2 1.9 2 1.9M13.8 8l2 1.9-2 1.9"/><path ${F} d="M2.5 17.2h19l-1.2 2.6H3.7z"/>`,
  child: `<circle cx="8" cy="5.8" r="2.3"/><path d="M4.5 20.5v-5.2a3.5 3.5 0 0 1 7 0v5.2M11.4 15.4l2.4 1.1"/><circle ${F} cx="16.6" cy="10.4" r="1.9"/><path ${F} d="M14 20.5v-3.1a2.6 2.6 0 0 1 5.2 0v3.1z"/>`,
  house: `<path d="M3.5 11.2L12 4.4l8.5 6.8M6 9.4v11.1h12V9.4"/><path ${F} d="M10.4 20.5v-4.4a1.6 1.6 0 0 1 3.2 0v4.4z"/>`,
  book: `<path d="M3.5 6.2c3-1.1 6-1 8.5 1v12.3c-2.5-2-5.5-2.1-8.5-1z"/><path ${F} d="M20.5 6.2c-3-1.1-6-1-8.5 1v12.3c2.5-2 5.5-2.1 8.5-1z"/>`,
  page: `<rect x="5" y="3.5" width="14" height="17" rx="2.2"/><path d="M8.5 8.5h7M8.5 12h7M13 16.8h3"/><rect ${F} x="8.2" y="15.2" width="3.2" height="3.2" rx=".8"/>`,
  beads: `<circle cx="12" cy="4.6" r="1.5"/><circle cx="16.6" cy="6.3" r="1.5"/><circle cx="18.2" cy="10.4" r="1.5"/><circle cx="16.2" cy="14.2" r="1.5"/><circle cx="7.8" cy="14.2" r="1.5"/><circle cx="5.8" cy="10.4" r="1.5"/><circle cx="7.4" cy="6.3" r="1.5"/><path d="M12 18.4v2.8M10.8 21.4l1.2-.9 1.2.9"/><circle ${F} cx="12" cy="16.3" r="2"/>`,
  bed: `<path d="M3 18.8V7M3 14.6h18v4.2M21 14.6v-2.2a3 3 0 0 0-3-3h-6.6v5.2"/><circle ${F} cx="7.1" cy="11.6" r="2"/>`,
  lantern: `<path d="M12 2.6v1.8M9.2 6.4h5.6l-1-2h-3.6zM8.6 6.4h6.8l1.1 3.6-1.1 7.6H8.6L7.5 10zM9 20.5h6M10 17.6l-.4 2.9M14 17.6l.4 2.9"/><path ${F} d="M12 9.6c1.4 1.5 1.7 2.8.9 3.9-.4.5-1.4.5-1.8 0-.8-1.1-.5-2.4.9-3.9z"/>`,
  ghusl: `<rect x="13.5" y="8.5" width="7" height="12" rx="1.3"/><path d="M15.6 12h2.8M15.6 15h2.8"/><path ${F} d="M7.8 3.5c2.4 3 3.6 5.1 3.6 6.9a3.6 3.6 0 0 1-7.2 0c0-1.8 1.2-3.9 3.6-6.9z"/>`,
  meal: `<path d="M5 16.5a7 7 0 0 1 14 0M3 16.5h18M5.5 19.5h13"/><circle ${F} cx="12" cy="8" r="1.6"/>`,
  palm: `<path d="M12.1 21c.6-3.8.6-7.6-.6-10.8M11.6 10.2C9.8 7.3 7 6.7 4.3 7.7M11.6 10.2c1-3 3.9-4.4 6.9-3.5M11.6 10.2c-3-.5-5.6 1-6.6 3.6M11.6 10.2c3-.3 5.6 1.2 6.6 3.6M8.2 21h8"/><circle ${F} cx="12.9" cy="11.9" r="1.1"/><circle ${F} cx="11" cy="12.5" r="1.1"/>`,
  hands: `<path d="M11 20.8v-3C7.4 17 4.6 13.6 4.4 8.4c2.8.4 5 2.6 6.6 6.2M13 20.8v-3c3.6-.8 6.4-4.2 6.6-9.4-2.8.4-5 2.6-6.6 6.2"/><circle ${F} cx="12" cy="4.6" r="1.8"/>`,
  visit: `<path d="M3.5 11.2L12 4.4l8.5 6.8M6 9.4v11.1h12V9.4"/><path ${F} d="M12 18.4l-2.6-2.5a1.5 1.5 0 0 1 2.2-2.1l.4.4.4-.4a1.5 1.5 0 0 1 2.2 2.1z"/>`,
  hourglass: `<path d="M6.5 3.5h11M6.5 20.5h11M8 3.5c0 4 4 5 4 8.5s-4 4.5-4 8.5M16 3.5c0 4-4 5-4 8.5s4 4.5 4 8.5"/><path ${F} d="M9.1 19.6c.7-2 2.9-3 2.9-4.6 0 1.6 2.2 2.6 2.9 4.6z"/>`,
  seal: `<circle cx="12" cy="12" r="8.6"/><path stroke-width="2.6" d="M8 12.4l2.8 2.8L16.2 9.6"/>`,
  week: `<rect x="3.5" y="5" width="17" height="15" rx="2.6"/><path d="M3.5 9.6h17M8 3.2v3.4M16 3.2v3.4"/><circle cx="7.5" cy="13.5" r=".9"/><circle cx="12" cy="13.5" r=".9"/><circle cx="7.5" cy="16.8" r=".9"/><circle cx="12" cy="16.8" r=".9"/><circle ${F} cx="16.4" cy="15.1" r="2.1"/>`,
};
const YN = { fajr: 'الفجر', dhuhr: 'الظهر', asr: 'العصر: عصا وظلها بطولها', maghrib: 'المغرب', isha: 'العشاء', mosque: 'الجمعة والجماعة', rahl: 'ورد الغدوة: رحل', steps: 'مشي وضوء الشمس', nib: 'تركيز: ريشة القلم', tea: 'استراحة: استكان', mail: 'الرسائل', phone: 'اتصالات', case: 'ميدان واجتماعات', nap: 'قيلولة', dumbbell: 'تدريب', laptop: 'تعلم تقني', child: 'وقت الابن', house: 'الأسرة', book: 'قراءة ووقت لك', page: 'المحاسبة', beads: 'الوتر والأذكار: مسبحة', bed: 'النوم', lantern: 'السَّحَر: فانوس', ghusl: 'التبكير للجمعة', meal: 'غداء الأسرة', palm: 'فراغ مباح: نخلة', hands: 'الدعاء', visit: 'صلة الرحم', hourglass: 'الباقي', seal: 'خلّصت', week: 'الأسبوع' };
const yi = (n, s = 24, w = 1.7) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${YI[n] || YI.page}</svg>`;
/* which icon a block wears */
function iconOf(b) {
  const k = b.key;
  const m = { fajr: 'fajr', dhuhr: 'dhuhr', asr: 'asr', maghrib: 'maghrib', isha: 'isha', jumua: 'mosque', ghadwa: 'rahl', light: 'steps', deep1: 'nib', deep2: 'nib', duha: 'tea', msgs1: 'mail', close: 'mail', work1: 'phone', work2: 'case', nap: 'nap', child: 'child', family: 'house', self: 'book', prep: 'page', witr: 'beads', sleep: 'bed', qiyam: 'lantern', qiyam2: 'lantern', jumua_prep: 'ghusl', lunch: 'meal', visit: 'visit', free: 'palm', dua: 'hands', self_am: 'tea' };
  if (k === 'train') return /مشي/.test(b.title) ? 'steps' : /تعلم/.test(b.title) ? 'laptop' : 'dumbbell';
  return m[k] || 'page';
}

/* colour of each icon's ink part: prayers by the hour they mark, the rest by what the item is */
const ICOL = { fajr: '#5A62C6', dhuhr: '#D3950A', asr: '#D9701F', maghrib: '#CF403D', isha: '#36459A', mosque: '#1E8768',
  rahl: '#3F57A6', beads: '#3F57A6', lantern: '#3F57A6', hands: '#3F57A6', ghusl: '#2B82C4',
  nib: '#B5473F', laptop: '#7F5CC2', book: '#7F5CC2', page: '#7F5CC2',
  phone: '#0E8C9B', case: '#0E8C9B', mail: '#0E8C9B', tea: '#A2632A', steps: '#2C8C53', dumbbell: '#2C8C53',
  nap: '#5D74C9', bed: '#4A5EB3', child: '#C2821A', house: '#C2821A', meal: '#C2821A', visit: '#CC4F74', palm: '#4C8838',
  hourglass: '#8A8378', seal: '#2C8C53', week: '#3F57A6' };
/* an icon on its tile: prayers get a solid tile in their colour, everything else a light tint */
function tile(name, solid, size = 24, cls = '') {
  return `<span class="ti${solid ? ' solid' : ''}${cls ? ' ' + cls : ''}" style="--ic:${ICOL[name] || '#555'}">${yi(name, size)}</span>`;
}
