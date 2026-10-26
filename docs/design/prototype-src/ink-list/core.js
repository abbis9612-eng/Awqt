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
  const T = [['today', 'اليوم', 'sun'], ['night', 'الليل', 'moon'], ['week', 'الأسبوع', 'cal'], ['page', 'المحاسبة', 'doc']];
  return `<nav class="ios-tab${dark ? ' dark' : ''}" aria-label="الأقسام">${T.map(([k, n, i]) => `<span class="${k === active ? 'on' : ''}">${icon(i, 25, 1.7)}<b>${n}</b></span>`).join('')}</nav>`;
}
