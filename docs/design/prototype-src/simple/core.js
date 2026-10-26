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
const store = { get(k, d) { try { const v = localStorage.getItem('yawmS:' + k); return v ? JSON.parse(v) : d; } catch (e) { return d; } }, set(k, v) { try { localStorage.setItem('yawmS:' + k, JSON.stringify(v)); } catch (e) {} } };
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

let timer = null;
function renderAll(keepScroll) {
  const k = G.k, M = model(), root = $('screen');
  const y = window.scrollY;
  root.className = 'phone c' + k; root.innerHTML = '';
  C[k].render(root, M);
  const inf = C[k].info;
  $('info').innerHTML = `<h2>${inf.title}</h2><div class="from">${inf.from}</div>
    <h3>ليش واضح</h3><ul>${inf.why.map((w) => `<li>${w}</li>`).join('')}</ul><h3>جرّب</h3><p>${inf.try}</p>`;
  document.querySelectorAll('#pick button').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.k === k)));
  document.querySelectorAll('#times button').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.p === G.preset)));
  if (keepScroll) window.scrollTo(0, y);
}
function boot() {
  $('pick').innerHTML = Object.entries(C).map(([k, c]) => `<button data-k="${k}" aria-pressed="false"><span class="sw" style="background:${c.swatch}"></span><span><b>${c.name}</b><span>${c.sub}</span></span></button>`).join('');
  $('times').innerHTML = '<span class="lb">جرّب وقت:</span>' + PRESETS.map(([p, l]) => `<button data-p="${p}" aria-pressed="false">${l}</button>`).join('');
  $('pick').onclick = (e) => { const b = e.target.closest('button'); if (!b) return; G.k = b.dataset.k; G.shift = 0; store.set('k', G.k); renderAll(true); };
  $('times').onclick = (e) => { const b = e.target.closest('button'); if (!b) return; G.preset = b.dataset.p; G.shift = 0; renderAll(true); };
  renderAll();
  setInterval(() => {
    if (G.preset !== 'now') return;
    const c = C[G.k];
    if (new Date().getSeconds() % 30 === 0) renderAll(true); else if (c.tick) c.tick($('screen'), model());
  }, 1000);
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
