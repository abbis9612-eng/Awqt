/* ================= «يوم» الحبر، النسخة الثامنة: يومك إنت تبنيه =================
   Owner's decision (2026-10-29, 1أ 2أ 3أ 4ج): the day is his own.
   1) First run asks a few quick questions and builds the day; he sees it, edits it, confirms it.
   2) An item he adds moves with a prayer by default («بعد العصر»), or sits at a fixed clock time.
   3) When the time after a prayer is too short: shorten «عادية» items, then drop them, then
      shorten «مهمة جداً»; tell him what changed. Prayers never move.
   4) Tasks drop into free time by themselves; any task can be pinned to a time.
   The ready day of the method stays as an option. Automatic is only what has to be: prayer
   times, moving prayer-anchored items, the shortening, sleep, and placing unpinned tasks. */

G.my = store.get('v8my', { built: false, items: [] });
G.prev8 = 'today';
const persist8 = () => store.set('v8my', G.my);
const CAT = [
  { k: 'sport', t: 'رياضة', w: 'رياضة', ic: 'dumbbell', a: 'asr', d: 45, imp: 'normal' },
  { k: 'walk', t: 'مشي', w: 'مشي', ic: 'steps', a: 'fajr', d: 30, imp: 'normal' },
  { k: 'deep', t: 'شغل يحتاج تركيز', w: 'تركيز', ic: 'nib', a: 'fajr', d: 90, imp: 'high' },
  { k: 'study', t: 'دراسة أو قراءة', w: 'دراسة', ic: 'book', a: 'isha', d: 45, imp: 'normal' },
  { k: 'quran', t: 'قرآن وأذكار', w: 'الأذكار', ic: 'rahl', a: 'fajr', d: 20, imp: 'high' },
  { k: 'family', t: 'وقت ويّا الأهل', w: 'الأهل', ic: 'house', a: 'maghrib', d: 60, imp: 'high' },
  { k: 'kids', t: 'وقت ويّا الأطفال', w: 'الأطفال', ic: 'child', a: 'asr', d: 45, imp: 'high' },
  { k: 'nap', t: 'قيلولة', w: 'قيلولة', ic: 'nap', a: 'dhuhr', d: 20, imp: 'normal' },
  { k: 'learn', t: 'تعلّم شي جديد', w: 'تعلّم', ic: 'laptop', a: 'isha', d: 30, imp: 'normal' },
];
const ANCH = [['fajr', 'بعد الفجر'], ['dhuhr', 'بعد الظهر'], ['asr', 'بعد العصر'], ['maghrib', 'بعد المغرب'], ['isha', 'بعد العشاء'], ['fixed', 'بساعة ثابتة']];
const PNAME = { fajr: 'الفجر', dhuhr: 'الظهر', asr: 'العصر', maghrib: 'المغرب', isha: 'العشاء' };
const minOf = (d) => Math.max(10, Math.round(d * 0.5 / 5) * 5);

/* Build his day for M's date: prayers, work, his items, sleep; free time in between. */
function myDay(M) {
  const tt = M.tt, plan = M.plan, fri = M.d.friday, wd = (weekdayOf(M.doy) + 1) % 7;
  const notes = [], ext = [];
  const pr = [['fajr', 'صلاة الفجر', tt.fajr, 20], ['dhuhr', fri ? 'صلاة الجمعة' : 'صلاة الظهر', tt.dhuhr, fri ? 60 : 20],
    ['asr', 'صلاة العصر', tt.asr, 15], ['maghrib', 'صلاة المغرب', tt.maghrib, 15], ['isha', 'صلاة العشاء', tt.isha, 20]]
    .map(([key, title, start, d]) => ({ key: fri && key === 'dhuhr' ? 'jumua' : key, anchor: key, title, start, end: start + d, isPrayer: true }));
  const night = plan.qiyam ? [] : [{ key: 'witr', title: 'الوتر', start: plan.bedtime - 15, end: plan.bedtime, isPrayer: false }];
  const fixed = [];
  if (['job', 'study'].includes(G.life.kind) && G.life.days.includes(wd)) {
    let pieces = [[G.life.from, G.life.to]];
    pr.forEach((p) => { pieces = pieces.flatMap(([s, e]) => (p.end <= s || p.start >= e) ? [[s, e]] : [[s, p.start], [p.end, e]].filter(([a, b]) => b - a >= 5)); });
    pieces.forEach(([s, e], i) => fixed.push({ key: 'work' + i, title: G.life.kind === 'job' ? 'الدوام' : 'الدراسة', start: s, end: e, mineIc: G.life.kind === 'job' ? 'case' : 'page', fixedTime: true }));
  }
  const busy = () => pr.concat(night, fixed).sort((a, b) => a.start - b.start);
  G.my.items.filter((it) => it.anchor === 'fixed').forEach((it) => {
    let s = it.at, e = s + it.d, moved = false;
    for (let k = 0; k < 12; k++) { const hit = busy().find((b) => s < b.end && e > b.start); if (!hit) break; s = hit.end; e = s + it.d; moved = true; }
    if (moved) notes.push(`«${it.t}» انزاحت لـ${fmt(s)} لأن ${fmt(it.at)} يوقع على شي ثابت`);
    fixed.push({ key: 'my' + it.id, title: it.t, start: s, end: e, mineIc: it.ic, item: it });
  });
  pr.forEach((p, i) => {
    const segEnd = i < pr.length - 1 ? pr[i + 1].start : (night[0] ? night[0].start : plan.bedtime);
    let pieces = [[p.end, segEnd]];
    busy().forEach((b) => { pieces = pieces.flatMap(([s, e]) => (b.end <= s || b.start >= e) ? [[s, e]] : [[s, b.start], [b.end, e]].filter(([a, c]) => c - a >= 5)); });
    const items = G.my.items.filter((it) => it.anchor === p.anchor).map((it) => ({ it, len: it.d, drop: false }));
    if (!items.length) return;
    const fit = () => {
      let pi = 0, cur = pieces.length ? pieces[0][0] : 0; const placed = [], left = [];
      for (const x of items.filter((y) => !y.drop)) {
        while (pi < pieces.length && pieces[pi][1] - Math.max(cur, pieces[pi][0]) < x.len) { pi++; if (pi < pieces.length) cur = pieces[pi][0]; }
        if (pi >= pieces.length) { left.push(x); continue; }
        const s = Math.max(cur, pieces[pi][0]); placed.push({ x, s }); cur = s + x.len;
      }
      return { placed, left };
    };
    const rev = items.slice().reverse();
    const steps = [...rev.filter((x) => x.it.imp === 'normal').map((x) => () => { x.len = minOf(x.it.d); }),
      ...rev.filter((x) => x.it.imp === 'normal').map((x) => () => { x.drop = true; }),
      ...rev.filter((x) => x.it.imp === 'high').map((x) => () => { x.len = minOf(x.it.d); }),
      ...rev.filter((x) => x.it.imp === 'high').map((x) => () => { x.drop = true; })];
    let r = fit();
    for (const step of steps) { if (!r.left.length) break; step(); r = fit(); }
    items.forEach((x) => {
      if (x.drop) notes.push(`«${x.it.t}» ما انحطّت اليوم: ما بقى وقت بعد ${PNAME[p.anchor]}${x.it.imp === 'high' ? '. إذا تريدها، قصّر غيرها أو غيّر وقتها من «عدّل يومك»' : ''}`);
      else if (x.len < x.it.d) notes.push(`«${x.it.t}» صارت ${x.len} دقيقة بدل ${x.it.d}، لأن الوقت بعد ${PNAME[p.anchor]} أقصر اليوم`);
    });
    r.placed.forEach(({ x, s }) => ext.push({ key: 'my' + x.it.id, title: x.it.t, start: s, end: s + x.len, mineIc: x.it.ic, item: x.it, shrunkFrom: x.len < x.it.d ? x.it.d : null }));
  });
  const all = pr.concat(night, fixed, ext).sort((a, b) => a.start - b.start);
  all.push({ key: 'sleep', title: 'النوم', start: plan.bedtime, end: plan.wake, sleep: true });
  if (plan.qiyam) all.push({ key: 'qiyam2', title: 'صلاة الليل والوتر', start: plan.wake, end: M.nx.fajr + 1440 });
  const out = [];
  all.forEach((b, i) => {
    out.push(b);
    const n = all[i + 1];
    if (n && !b.sleep && n.start - b.end >= 10) { const key = 'gap' + i; FREE.add(key); SHORT[key] = 'وقت فاضي'; out.push({ key, title: 'وقت فاضي', start: b.end, end: n.start }); }
  });
  out.forEach((b) => {
    if (b.item) { const c = CAT.find((x) => x.k === b.item.k); SHORT[b.key] = c ? c.w : b.item.t.split(' ')[0]; if (['nib', 'book', 'laptop'].includes(b.mineIc)) FOCUSABLE.add(b.key); }
    if (b.key.startsWith('work')) SHORT[b.key] = b.title;
  });
  M.ext = out; M.notes = notes;
  M.curI = out.findIndex((b) => b.start <= M.t && M.t < b.end);
  M.cur = out[M.curI] || null;
}

/* Tasks: pinned ones sit at their time; the rest drop into free time and never overlap. */
function placeTasks(M) {
  const want = G.shift === 0 ? 'today' : G.shift === 1 ? 'tomorrow' : '';
  const from = G.shift === 0 ? M.t : 0;
  const mine = G.tasks.filter((x) => (x.when || 'today') === want);
  const busy = mine.filter((x) => x.pin != null && !x.done).map((x) => [x.pin, x.pin + x.d]);
  const slots = M.ext.filter((b) => FREE.has(b.key) && b.end > from).map((b) => ({ b, at: ceil5(Math.max(b.start, from)) }));
  const res = new Map();
  mine.forEach((task) => {
    if (task.done || task.pin == null) return;
    const b = M.ext.find((x) => x.start <= task.pin && task.pin < x.end && !x.sleep);
    res.set(task, { task, slot: b, start: task.pin, pinned: true });
  });
  mine.forEach((task) => {
    if (task.done) return res.set(task, { task });
    if (task.pin != null) return;
    let found = null;
    for (const s of slots) {
      let at = s.at;
      for (let k = 0; k < 8; k++) { const hit = busy.find(([a, e]) => at < e && at + task.d > a); if (!hit) break; at = ceil5(hit[1]); }
      if (s.b.end - at >= task.d) { found = { s, at }; break; }
    }
    if (!found) return res.set(task, { task, none: true });
    busy.push([found.at, found.at + task.d]); found.s.at = found.at + task.d;
    res.set(task, { task, slot: found.s.b, start: found.at });
  });
  return mine.map((t) => res.get(t));
}
function taskForm(id) {
  const pin = G.qd.pin;
  return `<div class="addbox"><input id="${id}" type="text" maxlength="80" placeholder="شنو عندك تسوي؟" value="${esc(G.qd.t)}" aria-label="شنو المهمة" aria-describedby="${id}H">
    <p class="qhint" id="${id}H">${qdHint()}</p>
    <div class="dchips" role="group" aria-label="متى">${WHEN.map(([k, n]) => `<button data-qwhen="${k}" aria-pressed="${qdResolved().when === k}">${n}</button>`).join('')}</div>
    <div class="dchips" role="group" aria-label="كم تاخذ">${[15, 30, 45, 60, 90].map((d) => `<button data-qd="${d}" aria-pressed="${qdResolved().d === d}">${d} د</button>`).join('')}</div>
    <div class="dchips" role="group" aria-label="وقتها"><button data-qpin="auto" aria-pressed="${pin == null}">وحدها بالفراغ</button><button data-qpin="set" aria-pressed="${pin != null}">أختار وقتها</button></div>
    ${pin != null ? `<div class="row"><span class="tx">الساعة</span><span class="step"><button data-qat="-15" aria-label="أبكر ربع ساعة">−</button><output>${fmt(pin)}</output><button data-qat="15" aria-label="أتأخر ربع ساعة">+</button></span></div>` : ''}
    <button class="save" data-qsave8="1">حطّها</button></div>`;
}

/* The builder: what you want every day, and for each one when, how long and how important. */
function wantsHtml() {
  const sel = (k) => G.my.items.some((it) => it.k === k);
  const cards = G.my.items.map((it) => `<div class="wcard"><div class="wh">${tile(it.ic, false, 20, 'sm')}<b>${esc(it.t)}</b><button class="mini" data-wdel="${it.id}">شيلها</button></div>
      <p class="lbl">شوكت؟</p><div class="dchips g3" role="radiogroup" aria-label="شوكت">${ANCH.map(([a, n]) => `<button role="radio" aria-checked="${it.anchor === a}" aria-pressed="${it.anchor === a}" data-wanc="${it.id}:${a}">${n}</button>`).join('')}</div>
      ${it.anchor === 'fixed' ? `<div class="row"><span class="tx">الساعة</span><span class="step"><button data-wat="${it.id}:-15" aria-label="أبكر ربع ساعة">−</button><output>${fmt(it.at)}</output><button data-wat="${it.id}:15" aria-label="أتأخر ربع ساعة">+</button></span></div>` : ''}
      <p class="lbl">كم تاخذ؟</p><div class="dchips" role="radiogroup" aria-label="كم تاخذ">${[15, 30, 45, 60, 90].map((d) => `<button role="radio" aria-checked="${it.d === d}" aria-pressed="${it.d === d}" data-wlen="${it.id}:${d}">${d} د</button>`).join('')}</div>
      <p class="lbl">إذا ضاك الوقت</p><div class="seg5" role="radiogroup" aria-label="أهميتها"><button role="radio" aria-checked="${it.imp === 'high'}" data-wimp="${it.id}:high">مهمة جداً</button><button role="radio" aria-checked="${it.imp === 'normal'}" data-wimp="${it.id}:normal">عادية، تقصر أول</button></div></div>`).join('');
  return `<div class="wants" role="group" aria-label="شنو تريد بيومك">${CAT.map((c) => `<button class="want" aria-pressed="${sel(c.k)}" data-want="${c.k}">${tile(c.ic, false, 20, 'sm')}<span>${c.t}</span></button>`).join('')}</div>
    <div class="addbox row2"><input id="wantT" type="text" maxlength="30" placeholder="شي ثاني؟ مثلاً: درس خصوصي" aria-label="شي ثاني"><button class="mini" data-wantadd="1">+ أضيفه</button></div>
    ${G.my.items.length ? `<h4>رتّبها</h4>${cards}` : '<p class="note">اختار من فوك. إذا ما اخترت شي، يومك يبقى الصلوات والنوم والدوام، والباقي وقت فاضي لمهامك.</p>'}`;
}
function previewHtml(M) {
  const today = M.doy - G.shift;
  const target = { today, winter: 14, summer: 171 }[G.prev8];
  const keep = G.shift; G.shift = target - (model().doy - G.shift); const Mx = model(); G.shift = keep;
  myDay(Mx);
  const dt = dateOf(Mx.doy);
  return `<div class="dchips" role="radiogroup" aria-label="أي يوم">${[['today', 'اليوم'], ['winter', 'بالشتا'], ['summer', 'بالصيف']].map(([k, n]) => `<button role="radio" aria-checked="${G.prev8 === k}" aria-pressed="${G.prev8 === k}" data-prev8="${k}">${n}</button>`).join('')}</div>
    <p class="note">${DAYS[dt.getUTCDay()]} ${dt.getUTCDate()} ${MONTHS[dt.getUTCMonth()]}: الفجر ${fmt(Mx.tt.fajr)}، المغرب ${fmt(Mx.tt.maghrib)}. شوف فقراتك شلون تمشي ويّا الصلاة.</p>
    ${Mx.notes.length ? `<div class="chg8"><b>شنو تغيّر هذا اليوم</b>${Mx.notes.map((n) => `<p>${esc(n)}</p>`).join('')}</div>` : ''}
    <div class="mini8">${Mx.ext.filter((b) => !b.key.startsWith('gap')).map((b) => `<div class="m8${b.isPrayer ? ' pr' : ''}"><em>${fmt(b.start % 1440)}</em>${tile(iconOf(b), b.isPrayer, 16, 'xs')}<span>${esc(b.title)}${b.item && b.item.anchor !== 'fixed' ? `<small>${ANCH.find((a) => a[0] === b.item.anchor)[1]}</small>` : b.fixedTime || (b.item && b.item.anchor === 'fixed') ? '<small>ساعة ثابتة</small>' : ''}</span><i>${durText(b.end - b.start)}</i></div>`).join('')}</div>
    <div class="row"><span class="tx">ساعة نومك<small>تكعد ${fmt(Mx.plan.wake % 1440)}${Mx.plan.qiyam ? ' لصلاة الليل' : ''}</small></span><span class="step"><button data-n="bed-" aria-label="أبكر ربع ساعة">−</button><output>${fmt(G.bed)}</output><button data-n="bed+" aria-label="أتأخر ربع ساعة">+</button></span></div>`;
}
SET.myday = (M) => `<div class="sh"><h2>يومك</h2><button class="back" data-go="">رجوع</button></div>
  <p class="note">هنا تغيّر شنو يصير بيومك. التغيير يبين باليوم فوراً.</p>${wantsHtml()}<h4>شلون يطلع</h4>${previewHtml(M)}
  <button class="close" data-myreset="1">رجّع اليوم الجاهز بداله</button><div style="height:24px"></div>`;

const OB8 = 6;
const w0 = w4;
SET.welcome = function (M) {
  const st = G.ob;
  const bar = `<div class="obbar o6" role="progressbar" aria-valuemin="1" aria-valuemax="${OB8}" aria-valuenow="${st + 1}" aria-label="الخطوة ${st + 1} من ${OB8}">${Array.from({ length: OB8 }, (_, i) => `<i class="${i <= st ? 'on' : ''}"></i>`).join('')}</div>
    <div class="obtop">${st ? '<button class="back" data-ob="back">رجوع</button>' : '<span></span>'}${st < OB8 - 1 ? '<button class="skip" data-ob="skip">تخطّى</button>' : ''}</div>`;
  const reuse = (k) => { const keep = G.ob; G.ob = k; const h = w0(M); G.ob = keep; return h.replace(/<div class="obbar"[^]*?<\/div>\s*<div class="obtop">[^]*?<\/div>/, bar); };
  if (st === 0) return reuse(0).replace('يلا نبدي', 'يلا نبني يومك');
  if (st === 1) return reuse(1);
  if (st === 2) return `${bar}<div class="sh"><h2>شنو تريد بيومك؟</h2></div><p class="note">اختار اللي تريده يصير كل يوم، وقلّي شوكت وكم ياخذ. تكدر تغيّره بعدين.</p>${wantsHtml()}<button class="save" data-ob="next">التالي</button>`;
  if (st === 3) return reuse(2);
  if (st === 4) return reuse(3);
  return `${bar}<div class="sh"><h2>هذا يومك</h2></div><p class="note">رتّبته من أجوبتك. بدّل بين اليوم والشتا والصيف حتى تشوف شلون يمشي ويّا الصلاة.</p>
    ${previewHtml(M)}<button class="save" data-ob="finish">تمام، هذا يومي</button><button class="close" data-ob="back">رجوع أعدّل</button>`;
};

const render7 = C.A.render;
C.A.render = function (root, M) {
  if (G.my.built) { G.scratch = false; myDay(M); }
  render7(root, M);
  if (G.my.built && G.tab === 'today' && !G.page) {
    const rh = root.querySelector('.rh');
    if (rh) {
      rh.insertAdjacentHTML('beforeend', '<button class="mini" data-go="myday">عدّل يومك</button>');
      if (M.notes && M.notes.length) rh.insertAdjacentHTML('afterend', `<div class="chg8"><b>شنو تغيّر اليوم</b>${M.notes.map((n) => `<p>${esc(n)}</p>`).join('')}</div>`);
    }
  }
  if (G.sheet && G.sheet.startsWith('my')) {
    const b = M.ext.find((x) => x.key === G.sheet), at = root.querySelector('.sheet .hd');
    if (b && b.item && at && !root.querySelector('.sheet .what')) at.insertAdjacentHTML('afterend', `<div class="what"><b>شنو هاي؟</b>فقرة إنت حطيتها: ${b.item.anchor === 'fixed' ? `بساعة ثابتة ${fmt(b.item.at)}` : `${ANCH.find((a) => a[0] === b.item.anchor)[1]}، وتمشي ويّاه طول السنة`}، ${b.item.d} دقيقة، ${b.item.imp === 'high' ? 'مهمة جداً' : 'عادية'}. <button class="linkbtn" data-go="myday">عدّلها</button></div>`);
  }
  ['qT', 'taskT'].forEach((id) => { const inp = root.querySelector('#' + id); if (inp) inp.onkeydown = (e) => { if (e.key === 'Enter') { const b = root.querySelector('[data-qsave8]'); if (b) b.click(); } }; });
  const base = root.onclick;
  const go = (fn, keep = true) => { const yy = root.scrollTop; fn(); persist8(); persist4(); renderAll(true); root.scrollTop = keep ? yy : 0; };
  const toast = (msg) => { const t = root.querySelector('#aToast'); if (!t) return; t.textContent = msg; t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 2400); };
  const item = (id) => G.my.items.find((x) => String(x.id) === String(id));
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if ((el = q('[data-want]'))) {
      const c = CAT.find((x) => x.k === el.dataset.want);
      return go(() => { const i = G.my.items.findIndex((x) => x.k === c.k); if (i >= 0) G.my.items.splice(i, 1); else G.my.items.push({ id: Date.now() % 1e9, k: c.k, t: c.t, ic: c.ic, anchor: c.a, at: 17 * 60, d: c.d, imp: c.imp }); });
    }
    if (q('[data-wantadd]')) {
      const t = (root.querySelector('#wantT') || {}).value || '';
      if (!t.trim()) return toast('اكتب شنو تريد أول');
      return go(() => { G.my.items.push({ id: Date.now() % 1e9, k: 'own', t: t.trim(), ic: 'nib', anchor: 'asr', at: 17 * 60, d: 30, imp: 'normal' }); });
    }
    if ((el = q('[data-wdel]'))) return go(() => { G.my.items = G.my.items.filter((x) => String(x.id) !== el.dataset.wdel); });
    if ((el = q('[data-wanc]'))) { const [id, a] = el.dataset.wanc.split(':'); return go(() => { item(id).anchor = a; }); }
    if ((el = q('[data-wat]'))) { const [id, d] = el.dataset.wat.split(':'); return go(() => { const it = item(id); it.at = Math.max(4 * 60, Math.min(23 * 60, it.at + +d)); }); }
    if ((el = q('[data-wlen]'))) { const [id, d] = el.dataset.wlen.split(':'); return go(() => { item(id).d = +d; }); }
    if ((el = q('[data-wimp]'))) { const [id, v] = el.dataset.wimp.split(':'); return go(() => { item(id).imp = v; }); }
    if ((el = q('[data-prev8]'))) return go(() => { G.prev8 = el.dataset.prev8; });
    if (q('[data-myreset]')) { go(() => { G.my.built = false; G.page = null; G.tab = 'today'; }, false); return toast('رجع اليوم الجاهز. يومك محفوظ إذا ترجعله'); }
    if ((el = q('[data-qpin]'))) return go(() => { G.qd.pin = el.dataset.qpin === 'set' ? Math.ceil((M.t + 30) / 15) * 15 : null; });
    if ((el = q('[data-qat]'))) return go(() => { G.qd.pin = Math.max(0, G.qd.pin + +el.dataset.qat); });
    if (q('[data-qsave8]')) {
      const r = qdResolved();
      if (!r.t) return toast('اكتب المهمة أول');
      const pin = G.qd.pin;
      go(() => { G.tasks.unshift({ id: Math.max(0, ...G.tasks.map((x) => x.id)) + 1, t: r.t, d: r.d, done: false, when: r.when, pin: pin == null ? null : pin }); G.qd = { t: '', d: 30, when: 'today' }; G.add = null; });
      return toast(pin != null ? `انحطّت الساعة ${fmt(pin)}` : r.when === 'today' ? 'انحطّت بأول فراغ اليوم' : r.when === 'tomorrow' ? 'انحطّت لباچر' : 'انحطّت بالبال');
    }
    if ((el = q('[data-ob]'))) {
      const a = el.dataset.ob;
      if (a === 'loc') return base(e);
      if (a === 'next') return go(() => { G.ob = Math.min(OB8 - 1, G.ob + 1); }, false);
      if (a === 'back') return go(() => { G.ob = Math.max(0, G.ob - 1); }, false);
      if (a === 'allow') { go(() => { G.alertsOk = true; saveG('v2ok', true); syncOk(); G.ob = OB8 - 1; }, false); return; }
      if (a === 'done') return go(() => { G.ob = OB8 - 1; }, false);
      if (a === 'finish') { go(() => { G.my.built = true; G.page = null; G.tab = 'today'; G.shift = 0; G.ob = 0; }, false); return toast('يومك جاهز، وهو مالتك'); }
      if (a === 'skip') { go(() => { G.page = null; G.tab = 'today'; G.shift = 0; G.ob = 0; }, false); return toast('بديت باليوم الجاهز. تكدر تبني يومك من الإعدادات'); }
    }
    return base(e);
  };
};
