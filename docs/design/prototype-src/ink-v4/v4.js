/* ================= «يوم» الحبر، النسخة الرابعة: أفكار من تطبيقات المهام =================
   Owner sent screenshots of Todoist and To-Do List (2026-10-29) and asked to take what is useful.
   Taken (all offline, no account): a first-run flow with a progress line and a skip, a welcome
   whose lines get ticked in ink, one question that shapes the day («شلون يومك؟»), notifications
   shown as a live preview before you allow them, a morning summary and an evening review, quick
   add from + with «اليوم / باچر / بالبال» and words understood inside the text («باچر نص ساعة»),
   a week strip with dots, a quiet week summary with no empty charts, and a clearer active tab.
   Left out on purpose: accounts, paid tiers and trial pop-ups, ads, e-mail, intro videos, AI voice. */

Object.assign(G, {
  life: store.get('v4life', { kind: '', from: 8 * 60, to: 14 * 60, days: [0, 1, 2, 3, 4, 5] }),
  morning: store.get('v4morning', true),
  evening: store.get('v4evening', true),
  focusLog: store.get('v4focus', []),
  ob: 0, obCity: 0, qd: { t: '', d: 30, when: 'today' },
});
G.tasks.forEach((x) => { if (!x.when) x.when = 'today'; });
if (!store.get('v4seeded', false)) {
  G.tasks.push({ id: 90, t: 'أرتب أوراق السيارة', d: 30, done: false, when: 'later' }, { id: 91, t: 'أزور خالي', d: 60, done: false, when: 'tomorrow' });
  store.set('v4seeded', true); store.set('v3tasks', G.tasks);
}
const persist4 = () => {
  store.set('v4life', G.life); store.set('v4morning', G.morning); store.set('v4evening', G.evening);
  store.set('v4focus', G.focusLog); store.set('v3tasks', G.tasks);
};
const WHEN = [['today', 'اليوم'], ['tomorrow', 'باچر'], ['later', 'بالبال']];
const LIFE = [
  ['job', 'عندي دوام', 'وظيفة أو محل بساعات ثابتة', 'case'],
  ['study', 'أدرس', 'مدرسة أو جامعة أو دورة', 'page'],
  ['home', 'أغلب يومي بالبيت', 'البيت والأهل', 'house'],
  ['free', 'شغلي حر', 'أرتّب أوقاتي بنفسي', 'nib'],
];

/* Words the quick add understands, on the phone only. */
function parseTask(raw) {
  let s = ' ' + raw.replace(/[٠-٩]/g, (c) => '٠١٢٣٤٥٦٧٨٩'.indexOf(c)) + ' ';
  let when = null, d = null;
  const take = (re, f) => { const m = s.match(re); if (m) { f(m); s = s.replace(re, ' '); } };
  take(/\s(باچر|باجر|بكرة|بكره|غداً|غدا)\s/, () => { when = 'tomorrow'; });
  take(/\s(اليوم)\s/, () => { when = 'today'; });
  take(/\s(بعدين)\s/, () => { when = 'later'; });
  take(/\sساعة ونص\s/, () => { d = 90; });
  take(/\sساعتين\s/, () => { d = 120; });
  take(/\sنص ساعة\s/, () => { d = 30; });
  take(/\sربع ساعة\s/, () => { d = 15; });
  take(/\s(\d{1,3})\s*(د|دقيقة|دقايق|دقائق)\s/, (m) => { d = +m[1]; });
  take(/\s(\d{1,2})\s*(س|ساعة|ساعات)\s/, (m) => { d = +m[1] * 60; });
  take(/\sساعة\s/, () => { d = 60; });
  return { t: s.replace(/\s+/g, ' ').trim(), when, d };
}
function qdResolved() {
  const p = parseTask(G.qd.t || '');
  const when = (!G.qd.whenTap && p.when) || G.qd.when, d = (!G.qd.dTap && p.d) || G.qd.d;
  return { t: p.t, when, d, saw: (!G.qd.whenTap && !!p.when) || (!G.qd.dTap && !!p.d) };
}
const qdHint = () => { const r = qdResolved(); return r.t ? `«${esc(r.t)}» · ${WHEN.find((w) => w[0] === r.when)[1]} · ${r.d} د${r.saw ? ' · فهمتها من كلامك' : ''}` : 'تكدر تكتب: اتصل بالمحاسب باچر نص ساعة'; };
function taskForm(id) {
  return `<div class="addbox"><input id="${id}" type="text" maxlength="80" placeholder="شنو عندك تسوي؟" value="${esc(G.qd.t)}" aria-label="شنو المهمة" aria-describedby="${id}H">
    <p class="qhint" id="${id}H">${qdHint()}</p>
    <div class="dchips" role="group" aria-label="متى">${WHEN.map(([k, n]) => `<button data-qwhen="${k}" aria-pressed="${qdResolved().when === k}">${n}</button>`).join('')}</div>
    <div class="dchips" role="group" aria-label="كم تاخذ">${[15, 30, 45, 60, 90].map((d) => `<button data-qd="${d}" aria-pressed="${qdResolved().d === d}">${d} د</button>`).join('')}</div>
    <button class="save" data-qsave="1">حطّها</button></div>`;
}
function quickSheet() {
  return `<div class="veil" data-close="1" style="top:VEILTOP;height:800px"><div class="sheet" role="dialog" aria-label="مهمة جديدة">
    <div class="grab"></div><h3>مهمة جديدة</h3>${taskForm('qT')}<button class="close" data-close="1">سكّر</button></div></div>`;
}

/* Tasks: today's drop into the free gaps; tomorrow's show on tomorrow; «بالبال» wait for you. */
function placeTasks(M) {
  const want = G.shift === 0 ? 'today' : G.shift === 1 ? 'tomorrow' : '';
  const from = G.shift === 0 ? M.t : 0;
  const slots = M.ext.filter((b) => FREE.has(b.key) && b.end > from).map((b) => ({ b, at: ceil5(Math.max(b.start, from)) }));
  return G.tasks.filter((x) => (x.when || 'today') === want).map((task) => {
    if (task.done) return { task };
    const s = slots.find((x) => x.b.end - x.at >= task.d);
    if (!s) return { task, none: true };
    const start = s.at; s.at += task.d;
    return { task, slot: s.b, start };
  });
}
SCREENS.tasks = function (M) {
  const all = G.tasks.length, dn = G.tasks.filter((x) => x.done).length;
  const real = { ...M }; const keep = G.shift; G.shift = 0; const today = placeTasks(real); G.shift = keep;
  const tom = G.tasks.filter((x) => x.when === 'tomorrow'), later = G.tasks.filter((x) => x.when === 'later');
  const mini = (x, act) => `<div class="trow${x.done ? ' dn' : ''}"><button class="ck${x.done ? ' on' : ''}" data-task="${x.id}" role="checkbox" aria-checked="${x.done}" aria-label="${esc(x.t)}">${x.done ? icon('check', 15, 3) : ''}</button>
    <span class="tx"><b>${esc(x.t)}</b><small>${x.d} د</small></span>${act}</div>`;
  if (!all) return `${inkWord('المهام', 0, 112, 'ما عندك مهام')}<p class="when">صفحتك فاضية. اكتب أول شغلة ببالك وهي تنحط بأول فراغ بيومك.</p>${taskForm('taskT')}`;
  return `${inkWord('المهام', all ? dn / all * 100 : 0, 112, `خلصت ${dn} من ${all}`)}
    <p class="when">خلصت <em>${dn}</em> من <em>${all}</em></p>
    ${taskForm('taskT')}
    <h4>اليوم</h4><div class="tasks">${tasksBlock(M, today, false)}</div>
    ${tom.length ? `<h4>باچر</h4><div class="tasks">${tom.map((x) => mini(x, `<button class="mini" data-move="${x.id}:today">اليوم</button>`)).join('')}</div>` : ''}
    ${later.length ? `<h4>بالبال</h4><p class="note">اكتبها هسه وانساها، ورتّبها لما تفضى.</p><div class="tasks">${later.map((x) => mini(x, `<button class="mini" data-move="${x.id}:today">اليوم</button>`)).join('')}</div>` : ''}
    <div style="height:18px"></div>`;
};

/* A week strip instead of «أمس، اليوم، باچر»: a dot marks a day that has open tasks. */
function dayChips() {
  const M0 = model(), today = M0.doy - G.shift, s0 = today - ((weekdayOf(today) + 1) % 7);
  return `<div class="strip" role="group" aria-label="أيام الأسبوع">${[0, 1, 2, 3, 4, 5, 6].map((i) => {
    const di = s0 + i, wd = (weekdayOf(di) + 1) % 7, sh = di - today, d = dateOf(di);
    const mine = (sh === 0 && G.tasks.some((x) => x.when === 'today' && !x.done)) || (sh === 1 && G.tasks.some((x) => x.when === 'tomorrow' && !x.done));
    return `<button data-shift="${sh}" aria-pressed="${G.shift === sh}" class="${sh === 0 ? 'tdy' : sh < 0 ? 'past' : ''}" aria-label="${SAT_DAYS[wd]} ${d.getUTCDate()}${sh === 0 ? '، اليوم' : ''}${mine ? '، بيه مهام' : ''}">
      <small>${DAY_SHORT[wd]}</small><b>${d.getUTCDate()}</b><i${mine ? '' : ' hidden'}></i></button>`;
  }).join('')}</div>`;
}

/* What the morning and evening notifications will say, shown before you turn them on. */
function morningText(M) {
  const n = G.tasks.filter((x) => x.when === 'today' && !x.done).length;
  const first = M.ext.find((b) => b.start >= M.tt.sunrise && !b.isPrayer);
  const work = ['job', 'study'].includes(G.life.kind) ? `، و${G.life.kind === 'job' ? 'الدوام' : 'الدراسة'} من ${fmt(G.life.from)} إلى ${fmt(G.life.to)}` : '';
  return `صباح الخير. اليوم عندك ${n ? `${n} مهام` : 'يوم بلا مهام'}${work}. أول شغلة: ${first ? esc(first.title) : '—'}.`;
}
function eveningText(M) {
  const n = G.tasks.filter((x) => x.when === 'today').length, d = G.tasks.filter((x) => x.when === 'today' && x.done).length;
  return `خلصت ${d} من ${n} مهام اليوم. دقيقتين محاسبة، وبعدها تنام ${fmt(M.plan.bedtime)} وتكعد ${fmt(M.plan.wake)}.`;
}
const notif = (title, body, at) => `<div class="notif" role="img" aria-label="شكل الإشعار: ${title}. ${body}"><div class="nh"><span class="nlogo">ي</span><span>يوم</span><span>· ${at}</span></div><b>${title}</b><p>${body}</p></div>`;
function dailyBlock(M) {
  const mAt = fmt(M.tt.sunrise), eAt = fmt(M.plan.bedtime - 30);
  return `<h4>ملخص الصبح ومحاسبة الليل</h4>
    ${notif('يومك', morningText(M), mAt)}
    <div class="row"><span class="tx">ملخص الصبح<small>الساعة ${mAt}، وية الشروق</small></span><button class="sw" role="switch" aria-checked="${G.morning}" data-v4sw="morning" aria-label="ملخص الصبح"></button></div>
    <div class="row"><span class="tx">محاسبة الليل<small>الساعة ${eAt}، قبل النوم بنص ساعة</small></span><button class="sw" role="switch" aria-checked="${G.evening}" data-v4sw="evening" aria-label="محاسبة الليل"></button></div>`;
}
const alarms3 = SCREENS.alarms;
SCREENS.alarms = (M) => alarms3(M).replace('<h4>تنبيهات فقراتك</h4>', dailyBlock(M) + '<h4>تنبيهات فقراتك</h4>');

/* The week: what you did, in plain words; nothing is drawn when there is nothing yet. */
const week3 = SCREENS.week;
SCREENS.week = function (M) {
  const s0 = M.doy - ((weekdayOf(M.doy) + 1) % 7);
  const fmin = G.focusLog.filter((f) => f.doy >= s0 && f.doy <= s0 + 6).reduce((a, f) => a + f.min, 0);
  const tdone = G.tasks.filter((x) => x.done).length;
  let pages = 0, items = 0;
  for (let i = 0; i < 7; i++) { const pg = store.get('page:' + (s0 + i), null); if (pg && pg.pts && pg.pts.length) pages++; items += store.get('done:' + (s0 + i), []).length; }
  const rows = [[tdone, 'seal', `خلصت ${tdone} مهام`], [fmin, 'nib', `ركّزت ${durText(fmin)}`], [items, 'week', `علّمت ${items} فقرات «خلّصت»`], [pages, 'page', `سوّيت المحاسبة ${pages} ${pages === 1 ? 'ليلة' : 'ليالي'}`]].filter((r) => r[0] > 0);
  const sum = `<h4>إنجازك هذا الأسبوع</h4>${rows.length ? `<div>${rows.map(([, ic, t]) => `<div class="row">${tile(ic, false, 20, 'sm')}<span class="tx">${t}</span></div>`).join('')}</div>`
    : '<p class="note">بعدك ما سجّلت شي هذا الأسبوع. أول شغلة تخلّصها تبين هنا.</p>'}`;
  return week3(M).replace('<h4>خطة الأسبوع</h4>', sum + '<h4>خطة الأسبوع</h4>');
};

/* First run: four short steps with a progress line, a way back and a skip. */
const OB_LINES = ['يرتّب يومك من الفجر للنوم', 'ينبّهك بوكت كل شغلة', 'يحسبلك نومك كل ليلة'];
SET.welcome = function (M) {
  const st = G.ob, N = 4;
  const bar = `<div class="obbar" role="progressbar" aria-valuemin="1" aria-valuemax="${N}" aria-valuenow="${st + 1}" aria-label="الخطوة ${st + 1} من ${N}">${[0, 1, 2, 3].map((i) => `<i class="${i <= st ? 'on' : ''}"></i>`).join('')}</div>
    <div class="obtop">${st ? '<button class="back" data-ob="back">رجوع</button>' : '<span></span>'}${st < N - 1 ? '<button class="skip" data-ob="skip">تخطّى</button>' : ''}</div>`;
  if (st === 0) return `${bar}<div class="sh"><h2>هلا بيك بـ«يوم»</h2></div>
    <div class="obcard"><p>«يوم» يساعدك…</p>${OB_LINES.map((t, i) => `<div class="obl" style="--d:${0.5 + i * 0.7}s"><span class="obck">${icon('check', 15, 3)}</span><s>${t}</s></div>`).join('')}
      <div class="obl last" style="--d:${0.5 + 3 * 0.7}s"><span class="obck"></span><span>وهسه دورك</span></div></div>
    <button class="save" data-ob="next">يلا نبدي</button>`;
  if (st === 1) {
    const k = G.life.kind, timed = ['job', 'study'].includes(k);
    return `${bar}<div class="sh"><h2>شلون يومك؟</h2></div><p class="note">حتى نرتّب يومك من أول يوم. تكدر تغيّره بعدين.</p>
      <div role="radiogroup" aria-label="شلون يومك">${LIFE.map(([v, t, s, ic]) => `<button class="opt big" role="radio" aria-checked="${k === v}" data-life="${v}">${tile(ic, false, 22, 'sm')}<span class="tx">${t}<small>${s}</small></span><span class="ck${k === v ? ' on' : ''}">${k === v ? icon('check', 15, 3) : ''}</span></button>`).join('')}</div>
      ${timed ? `<h4>${k === 'job' ? 'الدوام' : 'الدراسة'}</h4>
        <div class="row"><span class="tx">يبدي</span><span class="step"><button data-lt="from:-30" aria-label="أبكر نص ساعة">−</button><output>${fmt(G.life.from)}</output><button data-lt="from:30" aria-label="أتأخر نص ساعة">+</button></span></div>
        <div class="row"><span class="tx">يخلص</span><span class="step"><button data-lt="to:-30" aria-label="أبكر نص ساعة">−</button><output>${fmt(G.life.to)}</output><button data-lt="to:30" aria-label="أتأخر نص ساعة">+</button></span></div>
        <div class="dchips" role="group" aria-label="الأيام">${DAY_SHORT.map((n, i) => `<button data-ld="${i}" aria-pressed="${G.life.days.includes(i)}" aria-label="${SAT_DAYS[i]}">${n}</button>`).join('')}</div>` : ''}
      <button class="save" data-ob="next"${k ? '' : ' disabled'}>التالي</button>`;
  }
  if (st === 2) return `${bar}<div class="sh"><h2>وين تسكن؟</h2></div><p class="note">حتى نعرف أوقات الصلاة ونرتّب يومك عليها. موقعك يبقى بتلفونك وما يطلع لأي مكان.</p>
    <button class="mini wide" data-ob="loc">موقعي الحالي</button>
    <div role="radiogroup" aria-label="المدينة">${CITY_LIST.slice(0, 6).map((n, i) => `<button class="opt" role="radio" aria-checked="${G.obCity === i}" data-obcity="${i}"><span class="tx">${n}</span><span class="ck${G.obCity === i ? ' on' : ''}">${G.obCity === i ? icon('check', 15, 3) : ''}</span></button>`).join('')}</div>
    <button class="save" data-ob="next">التالي</button>`;
  return `${bar}<div class="sh"><h2>آخر شي</h2></div><p class="note">حتى ينبّهك بوكت كل شغلة لازم تسمحله. هيچ راح يبين الإشعار:</p>
    ${notif('يومك', morningText(M), fmt(M.tt.sunrise))}
    <div class="row"><span class="tx">ملخص الصبح<small>وية الشروق</small></span><button class="sw" role="switch" aria-checked="${G.morning}" data-v4sw="morning" aria-label="ملخص الصبح"></button></div>
    <div class="row"><span class="tx">محاسبة الليل<small>قبل النوم بنص ساعة</small></span><button class="sw" role="switch" aria-checked="${G.evening}" data-v4sw="evening" aria-label="محاسبة الليل"></button></div>
    <div class="row"><span class="tx">تنبيه كل فقرة<small>وكت ما تبدي، وتغيّره لكل فقرة بعدين</small></span></div>
    <button class="save" data-ob="allow">اسمح بالتنبيهات</button><button class="close" data-ob="done">مو هسه</button>`;
};

/* Glue: extra handlers run first, then the v3 ones. */
let lastFocus = null;
const render3 = C.A.render;
C.A.render = function (root, M) {
  if (lastFocus && !G.focus) {
    const f = lastFocus, min = Math.round(f.total - Math.max(0, (f.end - Date.now()) / 60000));
    if (min > 0) { G.focusLog.push({ doy: M.doy, min }); persist4(); }
  }
  lastFocus = G.focus ? { ...G.focus } : null;
  render3(root, M);
  if (G.add === 'task' && !G.focus) {
    root.insertAdjacentHTML('beforeend', quickSheet().replace('VEILTOP', root.scrollTop + 'px'));
    root.style.overflowY = 'hidden';
  }
  for (const id of ['qT', 'taskT']) {
    const inp = root.querySelector('#' + id);
    if (!inp) continue;
    inp.oninput = () => {
      G.qd.t = inp.value;
      const h = root.querySelector('#' + id + 'H'); if (h) h.innerHTML = qdHint();
      const r = qdResolved();
      root.querySelectorAll('[data-qwhen]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.qwhen === r.when)));
      root.querySelectorAll('[data-qd]').forEach((b) => b.setAttribute('aria-pressed', String(+b.dataset.qd === r.d)));
    };
    inp.onkeydown = (e) => { if (e.key === 'Enter') root.querySelector('[data-qsave]').click(); };
    if (id === 'qT' && document.activeElement !== inp) {
      const y0 = root.scrollTop;
      inp.focus({ preventScroll: true });
      requestAnimationFrame(() => { root.scrollTop = y0; });
    }
  }
  const base = root.onclick;
  const go = (fn, keep) => { const yy = root.scrollTop; fn(); persist4(); persist(); renderAll(true); root.scrollTop = keep ? yy : 0; };
  const toast = (msg) => { const t = root.querySelector('#aToast'); if (!t) return; t.textContent = msg; t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 2400); };
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if ((el = q('[data-add]')) && el.dataset.add === 'task') return go(() => { G.sheet = null; G.add = 'task'; }, true);
    if ((el = q('[data-qwhen]'))) return go(() => { G.qd.when = el.dataset.qwhen; G.qd.whenTap = true; }, true);
    if ((el = q('[data-qd]'))) return go(() => { G.qd.d = +el.dataset.qd; G.qd.dTap = true; }, true);
    if (q('[data-qsave]')) {
      const r = qdResolved();
      if (!r.t) return toast('اكتب المهمة أول');
      go(() => { G.tasks.unshift({ id: Math.max(0, ...G.tasks.map((x) => x.id)) + 1, t: r.t, d: r.d, done: false, when: r.when }); G.qd = { t: '', d: 30, when: 'today' }; G.add = null; }, true);
      return toast(r.when === 'today' ? 'انحطّت بأول فراغ اليوم' : r.when === 'tomorrow' ? 'انحطّت لباچر' : 'انحطّت بالبال');
    }
    if ((el = q('[data-move]'))) { const [id, w] = el.dataset.move.split(':'); return go(() => { G.tasks.find((x) => x.id === +id).when = w; }, true); }
    if ((el = q('[data-v4sw]'))) { const k = el.dataset.v4sw; return go(() => { G[k] = !G[k]; }, true); }
    if ((el = q('[data-life]'))) return go(() => { G.life.kind = el.dataset.life; }, true);
    if ((el = q('[data-lt]'))) { const [k, d] = el.dataset.lt.split(':'); return go(() => { G.life[k] = Math.max(5 * 60, Math.min(22 * 60, G.life[k] + +d)); if (G.life.to <= G.life.from) G.life.to = G.life.from + 60; }, true); }
    if ((el = q('[data-ld]'))) { const d = +el.dataset.ld; return go(() => { G.life.days = G.life.days.includes(d) ? G.life.days.filter((x) => x !== d) : G.life.days.concat(d).sort(); }, true); }
    if ((el = q('[data-obcity]'))) return go(() => { G.obCity = +el.dataset.obcity; }, true);
    if ((el = q('[data-ob]'))) {
      const a = el.dataset.ob;
      if (a === 'loc') return go(() => { G.obCity = 0; }, true) || toast('لگيناك بالسليمانية');
      if (a === 'next') return go(() => { G.ob = Math.min(3, G.ob + 1); });
      if (a === 'back') return go(() => { G.ob = Math.max(0, G.ob - 1); });
      const finish = () => { G.page = null; G.tab = 'today'; G.shift = 0; G.ob = 0; };
      if (a === 'allow') { go(() => { G.alertsOk = true; saveG('v2ok', true); syncOk(); finish(); }); return toast('يومك جاهز'); }
      go(finish); return toast(a === 'skip' ? 'تكدر ترجع لها من الإعدادات' : 'يومك جاهز. التنبيهات تكدر تشغّلها من «المنبّه»');
    }
    return base(e);
  };
};
