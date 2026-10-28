/* ================= «يوم» الحبر، النسخة الثالثة: منظّم وقت قبل كل شي =================
   Owner's direction (2026-10-28): the app manages his time and his alerts from morning to night;
   prayers stay fixed and shape the day, but they are items in it, not the whole screen.
   Builds on v2 (same engine, data and identity). New: the ink word is what you are doing now,
   reminders per item (start, 5 or 10 min before, end), your own alarms, a focus timer, and tasks
   that drop into the free gaps of your day. Tabs: اليوم، المهام، (+)، المنبّه، الأسبوع. */

Object.assign(G, {
  rem: store.get('v3rem', {}),
  remDefault: store.get('v3remDef', 'start'),
  endAlert: store.get('v3end', true),
  remHow: store.get('v3how', 'sound'),
  prayerAlert: store.get('v3pray', { fajr: 'notify', dhuhr: 'notify', asr: 'notify', maghrib: 'notify', isha: 'notify' }),
  tasks: store.get('v3tasks', [
    { id: 1, t: 'اتصل بالمحاسب', d: 15, done: false },
    { id: 2, t: 'راجع عرض الشركاء', d: 45, done: false },
    { id: 3, t: 'اشتري هدية لأمي', d: 30, done: false },
  ]),
  alarms: store.get('v3alarms', [
    { id: 1, at: 6 * 60 + 30, label: 'رياضة الصبح', days: [1, 2, 3, 4, 5], on: true },
    { id: 2, at: 13 * 60 + 45, label: 'الدواء', days: [0, 1, 2, 3, 4, 5, 6], on: true },
  ]),
  focus: null, add: null, editAlarm: null, draft: { t: '', d: 30 },
});
const persist = () => {
  store.set('v3rem', G.rem); store.set('v3remDef', G.remDefault); store.set('v3end', G.endAlert); store.set('v3how', G.remHow);
  store.set('v3pray', G.prayerAlert); store.set('v3tasks', G.tasks); store.set('v3alarms', G.alarms);
};
const TABS3 = [['today', 'اليوم', 'dhuhr'], ['tasks', 'المهام', 'seal'], ['add', '', ''], ['alarms', 'المنبّه', 'hourglass'], ['week', 'الأسبوع', 'week']];
const SAT_DAYS = ['السبت', 'الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'];
const DAY_SHORT = ['سبت', 'أحد', 'اثنين', 'ثلاثاء', 'أربعاء', 'خميس', 'جمعة'];
function daysText(days) {
  const d = [...days].sort((a, b) => a - b);
  if (d.length === 7) return 'كل يوم';
  if (!d.length) return 'ولا يوم';
  if (d.length >= 3 && d[d.length - 1] - d[0] === d.length - 1) return `${SAT_DAYS[d[0]]} إلى ${SAT_DAYS[d[d.length - 1]]}`;
  return d.map((i) => SAT_DAYS[i]).join('، ');
}
const SHORT = {
  fajr: 'الفجر', dhuhr: 'الظهر', jumua: 'الجمعة', asr: 'العصر', maghrib: 'المغرب', isha: 'العشاء', ghadwa: 'الورد',
  light: 'مشي', deep1: 'تركيز', deep2: 'تركيز', duha: 'استراحة', msgs1: 'الرسائل', close: 'الرسائل', work1: 'العمل',
  work2: 'العمل', nap: 'قيلولة', child: 'الابن', family: 'الأسرة', self: 'وقتك', self_am: 'وقتك', prep: 'التحضير',
  witr: 'الوتر', sleep: 'النوم', qiyam2: 'السَّحَر', jumua_prep: 'التبكير', lunch: 'الغداء', visit: 'الزيارة', free: 'فراغ', dua: 'الدعاء',
};
const shortWord = (b) => b.key === 'train' ? (/مشي/.test(b.title) ? 'مشي' : /تعلم/.test(b.title) ? 'تعلم' : 'تدريب') : SHORT[b.key] || b.title.split(/[ :،]/)[0];
const FOCUSABLE = new Set(['deep1', 'deep2', 'train', 'self']);
const FREE = new Set(['work1', 'work2', 'self', 'self_am', 'free', 'visit']);
const PKEY = { fajr: 'fajr', dhuhr: 'dhuhr', jumua: 'dhuhr', asr: 'asr', maghrib: 'maghrib', isha: 'isha' };
const remOf = (b) => b.isPrayer ? G.prayerAlert[PKEY[b.key]] : (G.rem[b.key] ?? G.remDefault);
const remText = (r) => ({ start: 'ينبّهك وكت ما تبدي', 5: 'قبلها بـ5 دقايق', 10: 'قبلها بـ10 دقايق', off: 'بدون تنبيه', adhan: 'أذان', notify: 'تنبيه', none: 'بدون' }[r]);
const bell = (b) => { const r = remOf(b); return r === 'off' || r === 'none' ? '' : `<span class="bell">${r === 'adhan' ? 'أذان' : r === 'start' || r === 'notify' ? 'تنبيه' : `قبل ${r}`}</span>`; };
const ceil5 = (m) => Math.ceil(m / 5) * 5;

/** Tasks drop into the free gaps (flex blocks) of the day, earliest first, after now. */
function placeTasks(M) {
  const from = G.shift === 0 ? M.t : 0;
  const slots = M.ext.filter((b) => FREE.has(b.key) && b.end > from).map((b) => ({ b, at: ceil5(Math.max(b.start, from)) }));
  return G.tasks.map((task) => {
    if (task.done) return { task };
    const s = slots.find((x) => x.b.end - x.at >= task.d);
    if (!s) return { task, none: true };
    const start = s.at; s.at += task.d;
    return { task, slot: s.b, start };
  });
}
function nowState(M) {
  const cur = M.cur, nb = nextUp(M);
  if (cur) return { b: cur, word: shortWord(cur), p: (M.t - cur.start) / (cur.end - cur.start) * 100, label: `${shortWord(cur)}، تخلص بعد ${durText(cur.end - M.t)}`, next: nb };
  if (nb) return { b: null, word: shortWord(nb), p: 0, label: `${shortWord(nb)} بعد ${durText(nb.start - M.t)}`, next: nb };
  return { b: null, word: 'يومك', p: 100, label: 'خلص اليوم', next: null };
}
function nextPrayerLine(M) {
  const p = M.next; if (!p) return '';
  return `<p class="pline">${tile(p.key === 'fajr2' ? 'fajr' : (p.key === 'dhuhr' && M.d.friday ? 'mosque' : p.key), false, 16, 'xs')}<span>الصلاة الجاية: ${p.name} <em>${fmt(p.m)}</em><small>بعد ${durText(p.m - M.t)}</small></span></p>`;
}
function tasksBlock(M, placed, compact) {
  const open = placed.filter((x) => !x.task.done), done = placed.filter((x) => x.task.done);
  const row = (x) => `<div class="trow${x.task.done ? ' dn' : ''}"><button class="ck${x.task.done ? ' on' : ''}" data-task="${x.task.id}" role="checkbox" aria-checked="${x.task.done}" aria-label="${esc(x.task.t)}">${x.task.done ? icon('check', 15, 3) : ''}</button>
    <span class="tx"><b>${esc(x.task.t)}</b><small>${x.task.done ? 'خلصت' : x.slot ? `${fmt(x.start)} · ضمن «${esc(x.slot.title)}» · ${x.task.d} د` : `${x.task.d} د · ما بقى فراغ يكفيها اليوم، تنتقل لباچر`}</small></span></div>`;
  return open.map(row).join('') + (compact ? '' : done.map(row).join('')) || '<p class="note">ما عندك مهام. اضغط + حتى تضيف.</p>';
}
function timeline3(M, placed) {
  const groups = {};
  for (const b of M.ext) (groups[periodOf(M, b)] ||= []).push(b);
  const order = [...PERIOD, ['night', 'الليل']];
  return `<div class="tl">${order.filter(([k]) => groups[k]).map(([k, n]) => `<h5>${n}<em>${fmt(k === 'night' ? M.plan.bedtime : M.tt[k])}</em></h5>` + groups[k].map((b) => {
    const past = b.end <= M.t && G.shift === 0, on = b.start <= M.t && M.t < b.end && G.shift === 0, done = M.done.has(b.key);
    const inside = placed.filter((x) => x.slot && x.slot.key === b.key).map((x) => `<span class="sub">${fmt(x.start)} ${esc(x.task.t)}</span>`).join('');
    const ch = b.shrunkFrom ? `<small class="chg">قصرت اليوم من ${b.shrunkFrom} د</small>` : '';
    return `<button class="row${on ? ' on' : past ? ' past' : ''}" data-sheet="${b.key}">${tile(iconOf(b), b.isPrayer, 20, 'sm')}<em>${fmt(b.start)}</em>
      <span class="tx"><b>${esc(b.title)}</b>${ch}${inside}</span>${done ? '<span class="ok">✓</span>' : `${bell(b)}<span class="dur">${dur(b)}</span>`}</button>`;
  }).join('')).join('')}</div>`;
}

SCREENS.today = function (M) {
  const s = nowState(M), placed = placeTasks(M);
  const b = s.b, canFocus = b && FOCUSABLE.has(b.key);
  const coming = M.ext.filter((x) => x.start > M.t).slice(0, 3);
  const evening = M.t >= M.tt.isha;
  return `${dayChips()}${warnBanner()}
    ${inkWord(s.word, s.p, fitFs(s.word), s.label)}
    ${b ? `<p class="when">${esc(b.title)}<br>تخلص الساعة <em>${fmt(b.end)}</em>، باقي ${durText(b.end - M.t)}</p>` : s.next ? `<p class="when">تبدي الساعة <em>${fmt(s.next.start)}</em></p>` : ''}
    ${b ? `<div class="nowacts"><button class="pri" data-done="1">${M.done.has(b.key) ? '✓ تم' : doneLabel(b)}</button>
      ${canFocus ? `<button data-focus="${b.key}">ابدأ التركيز</button>` : ''}<button data-sheet="${b.key}">التنبيه: ${remText(remOf(b))}</button></div>` : ''}
    ${nextPrayerLine(M)}
    <h4>الجاي</h4><div class="next3">${coming.map((x) => `<button class="row" data-sheet="${x.key}">${tile(iconOf(x), x.isPrayer, 20, 'sm')}<em>${fmt(x.start)}</em><span class="tx"><b>${esc(x.title)}</b><small>${remText(remOf(x))}</small></span><span class="dur">${dur(x)}</span></button>`).join('')}</div>
    <div class="h4row"><h4>مهامك اليوم</h4><button class="mini" data-add="task">+ مهمة</button></div>
    <div class="tasks">${tasksBlock(M, placed, true)}</div>
    <h4>يومك كامل</h4>${timeline3(M, placed)}
    ${evening ? `<button class="tonight" data-tab="page">${tile('page', false, 22)}<div>محاسبة المساء<span>نقاطك العشر وثلاث أسئلة، دقيقتين</span></div><i>‹</i></button>` : ''}
    <button class="tonight" data-tab="night">${tile('bed', false, 22)}<div>الليلة: تنام <b>${fmt(M.plan.bedtime)}</b> وتكعد <b>${fmt(M.plan.wake)}</b>${M.plan.qiyam ? ' للسَّحَر' : ''}
      <span>${M.plan.shortfall ? `ينقصك ${durText(M.plan.shortfall)} عن 7 ساعات` : 'نومك يكفي'}</span></div><i>‹</i></button>
    ${!evening ? `<button class="tonight" data-tab="page">${tile('page', false, 22)}<div>محاسبة المساء<span>نقاطك العشر وثلاث أسئلة</span></div><i>‹</i></button>` : ''}`;
};

SCREENS.tasks = function (M) {
  const placed = placeTasks(M), all = G.tasks.length, dn = G.tasks.filter((x) => x.done).length;
  return `${inkWord('المهام', all ? dn / all * 100 : 0, 112, `خلصت ${dn} من ${all}`)}
    <p class="when">خلصت <em>${dn}</em> من <em>${all}</em>. الباقي ينحط بأول فراغ بيومك.</p>
    <div class="addbox"><input id="taskT" type="text" maxlength="60" placeholder="شنو المهمة؟ مثلاً: اتصل بالمحاسب" value="${esc(G.draft.t)}" aria-label="شنو المهمة">
      <div class="dchips">${[15, 30, 45, 60, 90].map((d) => `<button data-dd="${d}" aria-pressed="${G.draft.d === d}">${d} د</button>`).join('')}</div>
      <button class="save" data-addtask="1">حطّها بيومي</button></div>
    <h4>بيومك</h4><div class="tasks">${tasksBlock(M, placed, false)}</div>
    <p class="note">الفراغات هي فقرات «العمل» و«وقتك» و«فراغ». إذا ما كفى الوقت اليوم، المهمة تنتقل لباچر.</p><div style="height:18px"></div>`;
};

function alarmsToday(M) {
  const wd = (weekdayOf(M.doy) + 1) % 7;
  const list = G.alarms.filter((a) => a.on && a.days.includes(wd)).map((a) => ({ at: a.at, label: a.label }));
  if (G.alarm) list.push({ at: M.plan.wake, label: M.plan.qiyam ? 'الكعدة للسَّحَر' : 'الكعدة قبل الفجر' });
  return list.sort((a, b) => a.at - b.at);
}
SCREENS.alarms = function (M) {
  const today = alarmsToday(M), nx = today.find((a) => a.at > M.t) || today[0];
  const prev = [...today].reverse().find((a) => a.at <= M.t);
  const p = nx && prev && nx.at > M.t ? (M.t - prev.at) / (nx.at - prev.at) * 100 : 0;
  const edit = G.editAlarm != null ? G.alarms.find((a) => a.id === G.editAlarm) : null;
  return `${inkWord('المنبّه', p, 112, nx ? `المنبّه الجاي ${nx.label} ${fmt(nx.at)}` : 'ما في منبّه')}
    <p class="when">${nx ? `الجاي: ${esc(nx.label)}، الساعة <em>${fmt(nx.at)}</em>` : 'ما عندك منبّه اليوم'}</p>
    <div class="h4row"><h4>منبّهاتك</h4><button class="mini" data-newalarm="1">+ منبّه</button></div>
    ${G.alarms.map((a) => `<div class="alarm${a.on ? '' : ' off'}"><button class="atime" data-ealarm="${a.id}"><b>${fmt(a.at)}</b><span>${esc(a.label)} · ${daysText(a.days)}</span></button>
      <button class="sw" role="switch" aria-checked="${a.on}" data-alarm="${a.id}" aria-label="${esc(a.label)}"></button></div>
      ${edit && edit.id === a.id ? `<div class="aedit"><div class="row"><span class="tx">الساعة</span><span class="step"><button data-at="-15" aria-label="أبكر ربع ساعة">−</button><output>${fmt(a.at)}</output><button data-at="15" aria-label="أتأخر ربع ساعة">+</button></span></div>
        <div class="dchips">${DAY_SHORT.map((n, i) => `<button data-aday="${i}" aria-pressed="${a.days.includes(i)}" aria-label="${SAT_DAYS[i]}">${n}</button>`).join('')}</div>
        <div class="nowacts"><button data-adel="${a.id}">احذفه</button><button class="pri" data-ealarm="">تم</button></div></div>` : ''}`).join('')}
    <div class="alarm"><div class="atime"><b>${fmt(M.plan.wake)}</b><span>منبّه الكعدة ${M.plan.qiyam ? 'للسَّحَر' : 'قبل الفجر'} · يتحرك وياك كل ليلة</span></div>
      <button class="sw" role="switch" aria-checked="${G.alarm}" data-sw="alarm" aria-label="منبّه الكعدة"></button></div>
    <h4>تنبيهات فقراتك</h4>
    <p class="note">لكل فقرة تكدر تغيّرها من تفاصيلها. هذا الاختيار للكل:</p>
    <div class="dchips">${[['start', 'وكت ما تبدي'], [5, 'قبلها 5'], [10, 'قبلها 10'], ['off', 'بدون']].map(([k, n]) => `<button data-remdef="${k}" aria-pressed="${String(G.remDefault) === String(k)}">${n}</button>`).join('')}</div>
    <div class="row"><span class="tx">لما تخلص الفقرة<small>«خلص وقت التركيز، الجاي: الرسائل»</small></span><button class="sw" role="switch" aria-checked="${G.endAlert}" data-sw="end" aria-label="لما تخلص الفقرة"></button></div>
    <div class="row"><span class="tx">شلون ينبّهك</span></div>
    <div class="dchips">${[['sound', 'صوت'], ['vibrate', 'هزّة'], ['silent', 'إشعار بس']].map(([k, n]) => `<button data-how="${k}" aria-pressed="${G.remHow === k}">${n}</button>`).join('')}</div>
    <h4>الصلاة</h4>
    ${[['fajr', 'الفجر'], ['dhuhr', 'الظهر'], ['asr', 'العصر'], ['maghrib', 'المغرب'], ['isha', 'العشاء']].map(([k, n]) => `<div class="prow">${tile(k, true, 18, 'xs')}<span class="tx">${n} <em>${fmt(M.tt[k])}</em></span>
      <div class="seg">${[['adhan', 'أذان'], ['notify', 'تنبيه'], ['none', 'بدون']].map(([v, l]) => `<button data-pray="${k}:${v}" aria-pressed="${G.prayerAlert[k] === v}">${l}</button>`).join('')}</div></div>`).join('')}
    <div style="height:18px"></div>`;
};

function sheet3(M) {
  const b = M.ext.find((x) => x.key === G.sheet);
  if (!b) return '';
  const done = M.done.has(b.key);
  const why = b.shrunkFrom ? `<div class="why">اليوم قصرت إلى ${durText(b.end - b.start)} بدل ${durText(b.shrunkFrom)}، لأن الوقت بين الصلاتين أقصر.</div>` : '';
  const chips = b.isPrayer
    ? [['adhan', 'أذان'], ['notify', 'تنبيه'], ['none', 'بدون']].map(([v, l]) => `<button data-pray="${PKEY[b.key]}:${v}" aria-pressed="${remOf(b) === v}">${l}</button>`).join('')
    : [['start', 'وكت ما تبدي'], [5, 'قبلها 5'], [10, 'قبلها 10'], ['off', 'بدون']].map(([v, l]) => `<button data-rem="${b.key}:${v}" aria-pressed="${String(remOf(b)) === String(v)}">${l}</button>`).join('');
  return `<div class="veil" data-close="1" style="top:VEILTOP;height:800px"><div class="sheet" role="dialog" aria-label="${esc(b.title)}">
    <div class="grab"></div><div class="hd">${tile(iconOf(b), b.isPrayer, 30, 'big')}<div><h3>${esc(b.title)}</h3>
    <p class="when2">من <em>${fmt(b.start)}</em> إلى <em>${fmt(b.end)}</em> · ${durText(b.end - b.start)}</p></div></div>${why}
    <h4>${b.isPrayer ? 'تنبيه الصلاة' : 'نبّهني'}</h4><div class="dchips">${chips}</div>
    <div class="acts"><button class="pri" data-done="1">${done ? '✓ تم، ألغيه' : doneLabel(b)}</button>
    ${FOCUSABLE.has(b.key) ? `<button data-focus="${b.key}">ابدأ التركيز</button>` : `<button data-go="set-day">عدّلها ‹</button>`}</div>
    <button class="close" data-close="1">سكّر</button></div></div>`;
}
function addMenu() {
  return `<div class="veil" data-close="1" style="top:VEILTOP;height:800px"><div class="sheet" role="dialog" aria-label="أضف">
    <div class="grab"></div><h3>شتريد تضيف؟</h3>
    <div class="grp"><button class="row" data-add="task">${tile('seal', false, 20, 'sm')}<span class="tx">مهمة<small>تنحط بأول فراغ بيومك</small></span>${chev}</button>
    <button class="row" data-newalarm="1">${tile('hourglass', false, 20, 'sm')}<span class="tx">منبّه<small>بساعة تختارها، ويتكرر بالأيام</small></span>${chev}</button>
    <button class="row" data-go="set-work">${tile('case', false, 20, 'sm')}<span class="tx">موعد بساعة ثابتة<small>مثل الدوام أو درس</small></span>${chev}</button>
    <button class="row" data-go="set-day">${tile('week', false, 20, 'sm')}<span class="tx">فقرة ثابتة بيومي<small>مثل «دراسة» كل يوم بعد العصر</small></span>${chev}</button></div>
    <button class="close" data-close="1">سكّر</button></div></div>`;
}
function focusHtml(M) {
  const f = G.focus, left = Math.max(0, f.end - Date.now()), p = 100 - left / (f.total * 60000) * 100;
  const mm = Math.floor(left / 60000), ss = Math.floor(left / 1000) % 60;
  return `<div class="focus" style="top:VEILTOP"><p class="fsmall">${esc(f.title)}</p>
    ${inkWord('تركيز', p, 138, `باقي ${mm} دقيقة`)}
    <p class="fclock">${pad(mm)}:${pad(ss)}</p><p class="when">التنبيهات الثانية ساكتة لحد ما تخلص</p>
    <div class="nowacts"><button data-fplus="1">+5 دقايق</button><button class="pri" data-fstop="1">وقّف</button></div></div>`;
}

SET.settings = function (M) {
  const m = METHODS.find((x) => x[0] === G.method);
  const health = G.alertsOk
    ? `<button class="health" data-go="set-alerts">${tile('seal', false, 24)}<div><b>التنبيهات تشتغل زين</b><span>${G.alarms.filter((a) => a.on).length} منبّهات شغّالة، وتنبيه لكل فقرة</span></div></button>`
    : `<button class="health bad" data-go="set-alerts">${tile('hourglass', false, 24)}<div><b>ناقص إذن واحد</b><span>بدونه التنبيهات ممكن تتأخر دقائق</span></div><span class="chev">‹</span></button>`;
  const row = (go, ic, title, val) => `<button class="row" ${go.startsWith('tab:') ? `data-tab="${go.slice(4)}"` : `data-go="${go}"`}>${tile(ic, false, 20, 'sm')}<span class="tx">${title}</span><span class="val">${val}</span>${chev}</button>`;
  return `<div class="sh"><h2>الإعدادات</h2><button class="back" data-go="">رجوع</button></div>${health}
    <h4>التنبيهات والمنبّهات</h4><div class="grp">
      ${row('tab:alarms', 'hourglass', 'المنبّهات والتنبيهات', `${remText(G.remDefault)}`)}
      ${row('set-alerts', 'seal', 'فحص التنبيهات', G.alertsOk ? 'كلشي تمام' : 'ناقص إذن')}</div>
    <h4>يومك</h4><div class="grp">
      ${row('set-day', 'week', 'رتّب يومك', 'فقراتك وترتيبها')}
      ${row('set-work', 'case', 'الدوام والمواعيد الثابتة', 'السبت إلى الخميس')}
      ${row('tab:tasks', 'seal', 'المهام', `${G.tasks.filter((x) => !x.done).length} باقية`)}
      <div class="row">${tile('bed', false, 20, 'sm')}<span class="tx">ساعة النوم</span><span class="step sm"><button data-n="bed-" aria-label="أبكر ربع ساعة">−</button><output>${fmt(G.bed)}</output><button data-n="bed+" aria-label="أتأخر ربع ساعة">+</button></span></div>
      ${row('set-roles', 'house', 'أدوارك', `${G.roles} من 8`)}</div>
    <h4>الصلاة</h4><div class="grp">
      ${row('set-city', 'house', 'المدينة', 'السليمانية')}
      ${row('set-method', 'page', 'طريقة الحساب', m[1])}
      ${row('set-asr', 'asr', 'وقت العصر', 'الجمهور')}
      ${row('set-mosque', 'mosque', 'طابق جامعك', adjSum() || 'بدون تعديل')}</div>
    <h4>الشكل</h4><div class="grp">${row('set-look', 'isha', 'فاتح وداكن', { auto: 'تلقائي', light: 'فاتح دايماً', dark: 'داكن دايماً' }[G.theme])}</div>
    <h4>عن «يوم»</h4><div class="grp">${row('set-about', 'seal', 'الخصوصية والمحتوى', 'كلشي على تلفونك')}</div><div style="height:24px"></div>`;
};

let focusTick = null;
C.A.render = function (root, M) {
  const dark = G.theme === 'dark' || (G.theme === 'auto' && (M.t >= M.tt.isha || M.t < M.tt.fajr));
  root.classList.toggle('dark', dark);
  const y = root.scrollTop;
  const side = G.tab === 'night' || G.tab === 'page';
  const head = G.tab === 'today' ? top2(M) : side ? `<div class="top"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div><button class="back" data-tab="today">رجوع لليوم</button></div>` : topRow(M);
  const body = G.page ? `<main class="scr">${SET[G.page](M)}</main>` : head + `<main class="scr">${SCREENS[G.tab](M)}</main>`;
  const nav = G.page ? '' : `<nav class="ios-tab t5" aria-label="الأقسام">${TABS3.map(([k, n, i]) => k === 'add'
    ? '<button class="plus" data-add="menu" aria-label="أضف">+</button>'
    : `<button data-tab="${k}"${k === G.tab ? ` aria-current="page" style="--ic:${ICOL[i]}"` : ''}>${yi(i, 26, 1.6)}<b>${n}</b></button>`).join('')}</nav>`;
  let over = '';
  if (G.focus) over = focusHtml(M);
  else if (G.add === 'menu') over = addMenu();
  else if (G.sheet && !G.page) over = sheet3(M);
  root.innerHTML = body + nav + over.replace('VEILTOP', y + 'px') + '<div class="toast" id="aToast" role="status"></div>';
  root.style.overflowY = over ? 'hidden' : '';
  root.scrollTop = y;
  clearInterval(focusTick);
  if (G.focus) focusTick = setInterval(() => { if (G.focus && Date.now() >= G.focus.end) { G.focus = null; } renderAll(true); }, 1000);
  const toast = (msg) => { const t = root.querySelector('#aToast'); t.textContent = msg; t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 2400); };
  const go = (fn, keep) => { const yy = root.scrollTop; fn(); persist(); renderAll(true); root.scrollTop = keep ? yy : 0; };
  const inp = root.querySelector('#taskT');
  if (inp) inp.oninput = () => { G.draft.t = inp.value; };
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if ((el = q('[data-close]')) && (e.target === el || el.classList.contains('close'))) return go(() => { G.sheet = null; G.add = null; }, true);
    if (q('[data-fstop]')) return go(() => { G.focus = null; }, true);
    if (q('[data-fplus]')) return go(() => { G.focus.end += 5 * 60000; G.focus.total += 5; }, true);
    if ((el = q('[data-focus]'))) { const b = M.ext.find((x) => x.key === el.dataset.focus); return go(() => { G.sheet = null; G.focus = { title: b.title, end: Date.now() + (b.end - M.t) * 60000, total: b.end - M.t }; }, true); }
    if (q('[data-done]') && G.sheet) return go(() => { markDone(M, G.sheet); }, true);
    if (q('[data-done]') && M.cur) return go(() => markDone(M, M.cur.key), true);
    if ((el = q('[data-add]'))) { const a = el.dataset.add; return go(() => { G.sheet = null; G.add = a === 'menu' ? 'menu' : null; if (a === 'task') { G.tab = 'tasks'; G.page = null; } }, a === 'menu'); }
    if (q('[data-newalarm]')) return go(() => { const id = Math.max(0, ...G.alarms.map((a) => a.id)) + 1; G.alarms.push({ id, at: 7 * 60, label: 'منبّه', days: [0, 1, 2, 3, 4, 5, 6], on: true }); G.editAlarm = id; G.add = null; G.tab = 'alarms'; G.page = null; });
    if ((el = q('[data-go]'))) return go(() => { G.page = el.dataset.go || null; G.sheet = null; G.add = null; });
    if ((el = q('[data-tab]'))) return go(() => { G.tab = el.dataset.tab; G.page = null; G.shift = 0; G.sheet = null; G.add = null; });
    if ((el = q('[data-shift]'))) return go(() => { G.shift = +el.dataset.shift; }, true);
    if ((el = q('[data-rem]'))) { const [k, v] = el.dataset.rem.split(':'); return go(() => { G.rem[k] = isNaN(+v) ? v : +v; }, true); }
    if ((el = q('[data-pray]'))) { const [k, v] = el.dataset.pray.split(':'); return go(() => { G.prayerAlert[k] = v; }, true); }
    if ((el = q('[data-remdef]'))) { const v = el.dataset.remdef; return go(() => { G.remDefault = isNaN(+v) ? v : +v; }, true); }
    if ((el = q('[data-how]'))) return go(() => { G.remHow = el.dataset.how; }, true);
    if ((el = q('[data-sheet]'))) return go(() => { G.sheet = el.dataset.sheet; }, true);
    if ((el = q('[data-task]'))) { const t = G.tasks.find((x) => x.id === +el.dataset.task); return go(() => { t.done = !t.done; buzz(); }, true); }
    if ((el = q('[data-dd]'))) return go(() => { G.draft.d = +el.dataset.dd; }, true);
    if (q('[data-addtask]')) {
      const t = (G.draft.t || '').trim();
      if (!t) return toast('اكتب المهمة أول');
      return go(() => { G.tasks.unshift({ id: Math.max(0, ...G.tasks.map((x) => x.id)) + 1, t, d: G.draft.d, done: false }); G.draft = { t: '', d: 30 }; }, true);
    }
    if ((el = q('[data-ealarm]'))) return go(() => { G.editAlarm = el.dataset.ealarm ? +el.dataset.ealarm : null; }, true);
    if ((el = q('[data-alarm]'))) { const a = G.alarms.find((x) => x.id === +el.dataset.alarm); return go(() => { a.on = !a.on; }, true); }
    if ((el = q('[data-at]'))) { const a = G.alarms.find((x) => x.id === G.editAlarm); return go(() => { a.at = (a.at + +el.dataset.at + 1440) % 1440; }, true); }
    if ((el = q('[data-aday]'))) { const a = G.alarms.find((x) => x.id === G.editAlarm), d = +el.dataset.aday; return go(() => { a.days = a.days.includes(d) ? a.days.filter((x) => x !== d) : a.days.concat(d).sort(); }, true); }
    if ((el = q('[data-adel]'))) return go(() => { G.alarms = G.alarms.filter((x) => x.id !== +el.dataset.adel); G.editAlarm = null; }, true);
    if ((el = q('[data-adj]'))) { const [k, d] = el.dataset.adj.split(':'); return go(() => { G.adj[k] = Math.max(-30, Math.min(30, (G.adj[k] || 0) + +d)); saveG('v2adj', G.adj); }, true); }
    if (q('[data-adjreset]')) return go(() => { Object.keys(G.adj).forEach((k) => { G.adj[k] = 0; }); saveG('v2adj', G.adj); }, true);
    if ((el = q('[data-method]'))) return go(() => { G.method = el.dataset.method; saveG('v2method', G.method); }, true);
    if ((el = q('[data-look]'))) return go(() => { G.theme = el.dataset.look; saveG('v2theme', G.theme); }, true);
    if ((el = q('[data-wd]'))) { const i = +el.dataset.wd, on = store.get('v2work', [0, 1, 2, 3, 4, 5]); return go(() => saveG('v2work', on.includes(i) ? on.filter((x) => x !== i) : on.concat(i)), true); }
    if (q('[data-fix]')) return go(() => { G.alertsOk = true; saveG('v2ok', true); syncOk(); }, true);
    if ((el = q('[data-sw]'))) {
      const k = el.dataset.sw;
      return go(() => { if (k === 'qiyam') { G.qiyam = !G.qiyam; saveG('qiyam', G.qiyam); } if (k === 'end') G.endAlert = !G.endAlert; if (k === 'alarm') { G.alarm = !G.alarm; saveG('alarm', G.alarm); } }, true);
    }
    if ((el = q('[data-n]'))) { const a = el.dataset.n; return go(() => { if (a === 'bed-') G.bed = Math.max(1170, G.bed - 15); if (a === 'bed+') G.bed = Math.min(1410, G.bed + 15); saveG('bed', G.bed); }, true); }
    if ((el = q('[data-toast]'))) return toast(el.dataset.toast);
    if ((el = q('.sw[role="switch"]'))) el.setAttribute('aria-checked', String(el.getAttribute('aria-checked') !== 'true'));
  };
  root.onkeydown = (e) => { if (e.key === 'Escape' && (G.sheet || G.add)) go(() => { G.sheet = null; G.add = null; }, true); };
};
