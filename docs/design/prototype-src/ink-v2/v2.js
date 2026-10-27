/* ================= «يوم» الحبر، النسخة الثانية =================
   Same identity, engine and data as yawm-ink.html. New: a clearer Today (yesterday/today/tomorrow,
   the five prayers on top, the whole day in periods, tap any item for its details), a warning when
   the adhan may be late, and Settings as short groups with their own pages. */

Object.assign(G, {
  page: null, sheet: null, alertsOk: store.get('v2ok', true),
  adj: store.get('v2adj', { fajr: 0, sunrise: 0, dhuhr: 0, asr: 0, maghrib: 0, isha: 0 }),
  method: store.get('v2method', 'EG'), madhab: 'SHAFI', theme: store.get('v2theme', 'auto'),
  blocksRem: store.get('v2blocks', true), alarm: store.get('alarm', true), roles: 8,
});
const METHODS = [['EG', 'الهيئة المصرية العامة للمساحة', 'المستعملة بالعراق غالباً'], ['MWL', 'رابطة العالم الإسلامي', ''], ['UQ', 'أم القرى', ''], ['KR', 'جامعة العلوم الإسلامية بكراتشي', ''], ['TR', 'رئاسة الشؤون الدينية التركية', '']];
const CITY_LIST = ['السليمانية', 'أربيل', 'دهوك', 'كركوك', 'الموصل', 'بغداد', 'كربلاء', 'النجف', 'البصرة', 'لندن'];
const SIX = [['fajr', 'الفجر'], ['sunrise', 'الشروق'], ['dhuhr', 'الظهر'], ['asr', 'العصر'], ['maghrib', 'المغرب'], ['isha', 'العشاء']];
const PERIOD = [['fajr', 'الفجر'], ['sunrise', 'الضحى'], ['dhuhr', 'الظهر'], ['asr', 'العصر'], ['maghrib', 'المغرب'], ['isha', 'العشاء']];
const saveG = (k, v) => store.set(k, v);
const nextUp = (M) => M.ext.find((b) => b.start > M.t);
const chev = '<span class="chev" aria-hidden="true">‹</span>';
const adjSum = () => SIX.filter(([k]) => G.adj[k]).map(([k, n]) => `${n} ${G.adj[k] > 0 ? '+' : ''}${G.adj[k]}`).join('، ');

function top2(M) {
  return `<div class="top"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div>
    <div class="side"><div class="clk">${fmt(M.t)}</div><button class="gear" data-go="settings">الإعدادات</button></div></div>`;
}
function dayChips() {
  return `<div class="days" role="group" aria-label="أي يوم">${[[-1, 'أمس'], [0, 'اليوم'], [1, 'باچر']].map(([s, n]) =>
    `<button data-shift="${s}" aria-pressed="${G.shift === s}">${n}</button>`).join('')}</div>`;
}
function warnBanner() {
  if (G.alertsOk) return '';
  return `<button class="warn" data-go="set-alerts">${tile('hourglass', false, 22)}<div><b>الأذان ممكن يتأخر</b><span>ناقص إذن «المنبهات والتذكيرات»</span></div><i>صلّحها</i></button>`;
}
function fivePrayers(M) {
  const s = inkState(M);
  const PI = { fajr: 'fajr', dhuhr: M.d.friday ? 'mosque' : 'dhuhr', asr: 'asr', maghrib: 'maghrib', isha: 'isha' };
  return `<div class="five top5">${M.PR.filter(([k]) => k !== 'sunrise').map(([k, n, m]) => {
    const nx = k === s.key, fill = m <= M.t ? 100 : nx ? s.p : 0;
    return `<div class="${nx ? 'nx' : ''}"><i style="--ic:${ICOL[PI[k]]}">${yi(PI[k], 24)}</i><b style="--p:${fill.toFixed(1)}%">${n}</b><span>${fmt(m)}</span></div>`;
  }).join('')}</div>`;
}
function periodOf(M, b) {
  if (b.sleep || b.key === 'qiyam2') return 'night';
  let p = 'fajr';
  for (const [k] of PERIOD) if (M.tt[k] <= b.start) p = k;
  return p;
}
const dur = (b) => durShort(b.end - b.start);
function timeline(M) {
  const groups = {};
  for (const b of M.ext) (groups[periodOf(M, b)] ||= []).push(b);
  const order = [...PERIOD, ['night', 'الليل']];
  return `<div class="tl">${order.filter(([k]) => groups[k]).map(([k, n]) => {
    const at = k === 'night' ? M.plan.bedtime : M.tt[k];
    return `<h5>${n}<em>${fmt(at)}</em></h5>` + groups[k].map((b) => {
      const past = b.end <= M.t, on = b.start <= M.t && M.t < b.end, done = M.done.has(b.key);
      const ch = b.shrunkFrom ? `<small class="chg">قصّرناها اليوم من ${b.shrunkFrom} د</small>` : '';
      return `<button class="row${on ? ' on' : past ? ' past' : ''}" data-sheet="${b.key}">${tile(iconOf(b), b.isPrayer, 20, 'sm')}<em>${fmt(b.start)}</em>
        <span class="tx"><b>${esc(b.title)}</b>${ch}</span>${done ? '<span class="ok">✓</span>' : `<span class="dur">${dur(b)}</span>`}</button>`;
    }).join('');
  }).join('')}</div>`;
}

SCREENS.today = function (M) {
  const s = inkState(M), cur = M.cur, nb = nextUp(M);
  return `${dayChips()}${warnBanner()}
    ${inkWord(s.word, s.p, fitFs(s.word), `${s.word} بعد ${durText(s.tgt - M.t)}`)}
    <p class="when">بعد ${durText(s.tgt - M.t)}، الساعة <em>${fmt(s.tgt)}</em></p>
    ${fivePrayers(M)}
    ${cur ? `<section class="now">${tile(iconOf(cur), cur.isPrayer, 30, 'big')}<div data-sheet="${cur.key}" role="button" tabindex="0"><small>هسه</small><h3>${esc(cur.title)}</h3>
      <small>لحد ${fmt(cur.end)}</small>${nb ? `<div class="nexty">بعدها ${fmt(nb.start)}: ${esc(nb.title)}</div>` : ''}</div>${doneBtn(M, 'seal')}</section>` : ''}
    <h4>يومك كامل</h4>${timeline(M)}
    <button class="tonight" data-tab="night">${tile('bed', false, 22)}<div>الليلة: تنام <b>${fmt(M.plan.bedtime)}</b> وتكعد <b>${fmt(M.plan.wake)}</b>${M.plan.qiyam ? ' للسَّحَر' : ''}
      <span>${M.plan.shortfall ? `ينقصك ${durText(M.plan.shortfall)} عن 7 ساعات` : 'نومك يكفي'}</span></div><i>‹</i></button>`;
};

function sheetHtml(M) {
  const b = M.ext.find((x) => x.key === G.sheet);
  if (!b) return '';
  const done = M.done.has(b.key), label = done ? '✓ تم، ألغيه' : doneLabel(b);
  const why = b.shrunkFrom ? `<div class="why">اليوم قصّرناها إلى ${durText(b.end - b.start)} بدل ${durText(b.shrunkFrom)}، لأن الوقت بين الصلاتين أقصر. صلاتك ما تقصر عن وقتها.</div>`
    : b.isPrayer ? `<div class="why" style="background:color-mix(in srgb,#1E8768 9%,var(--bgc,#fff))">وقتها ثابت حسب مدينتك، وتكدر تطابقها ويّا جامعك من الإعدادات.</div>` : '';
  return `<div class="veil" data-close="1" style="top:VEILTOP;height:800px"><div class="sheet" role="dialog" aria-label="${esc(b.title)}">
    <div class="grab"></div><div class="hd">${tile(iconOf(b), b.isPrayer, 30, 'big')}<div><h3>${esc(b.title)}</h3>
    <p class="when2">من <em>${fmt(b.start)}</em> إلى <em>${fmt(b.end)}</em> · ${durText(b.end - b.start)}</p></div></div>${why}
    <div class="acts"><button class="pri" data-done="1">${label}</button><button data-go="set-day">عدّلها ‹</button></div>
    <button class="close" data-close="1">سكّر</button></div></div>`;
}

/* ---- settings ---- */
const SET = {
  settings(M) {
    const m = METHODS.find((x) => x[0] === G.method);
    const health = G.alertsOk
      ? `<button class="health" data-go="set-alerts">${tile('seal', false, 24)}<div><b>التنبيهات تشتغل زين</b><span>التنبيه الجاي: ${M.next ? `${M.next.name} ${fmt(M.next.m)}` : 'الفجر'}</span></div></button>`
      : `<button class="health bad" data-go="set-alerts">${tile('hourglass', false, 24)}<div><b>ناقص إذن واحد</b><span>بدونه الأذان ممكن يتأخر دقائق</span></div><span class="chev">‹</span></button>`;
    const row = (go, ic, title, val, cls = '') => `<button class="row${cls}" data-go="${go}">${tile(ic, false, 20, 'sm')}<span class="tx">${title}</span><span class="val">${val}</span>${chev}</button>`;
    const sw = (key, ic, title, on, sub) => `<div class="row">${tile(ic, false, 20, 'sm')}<span class="tx">${title}${sub ? `<small>${sub}</small>` : ''}</span><button class="sw" role="switch" aria-checked="${on}" data-sw="${key}" aria-label="${title}"></button></div>`;
    return `<div class="sh"><h2>الإعدادات</h2><button class="back" data-go="">رجوع</button></div>${health}
      <h4>مكانك ومواقيتك</h4><div class="grp">
        ${row('set-city', 'house', 'المدينة', 'السليمانية')}
        ${row('set-method', 'page', 'طريقة الحساب', m[1])}
        ${row('set-asr', 'asr', 'وقت العصر', 'الجمهور')}
        ${row('set-mosque', 'mosque', 'طابق جامعك', adjSum() || 'بدون تعديل')}</div>
      <h4>يومك</h4><div class="grp">
        ${row('set-day', 'week', 'رتّب يومك', 'فقراتك، والدوام، والمواعيد')}
        ${row('set-work', 'case', 'أيام الدوام', 'السبت إلى الخميس')}
        <div class="row">${tile('bed', false, 20, 'sm')}<span class="tx">ساعة النوم<small>ما تنام قبل العشاء بنص ساعة</small></span><span class="step sm"><button data-n="bed-" aria-label="أبكر ربع ساعة">−</button><output>${fmt(G.bed)}</output><button data-n="bed+" aria-label="أتأخر ربع ساعة">+</button></span></div>
        ${sw('qiyam', 'lantern', 'السَّحَر قبل الفجر', G.qiyam, 'إذا بقى للنوم 6 ساعات أو أكثر')}
        ${row('set-roles', 'house', 'أدوارك', `${G.roles} من 8`)}</div>
      <h4>التنبيهات</h4><div class="grp">
        ${row('set-alerts', 'fajr', 'الأذان وفحص التنبيهات', G.alertsOk ? 'كلشي تمام' : 'ناقص إذن', G.alertsOk ? '' : ' danger')}
        ${sw('blocks', 'tea', 'تذكير ببداية كل فقرة', G.blocksRem, 'تذكير خفيف بلا صوت أذان')}
        ${sw('alarm', 'bed', 'منبّه الكعدة قبل الفجر', G.alarm, `الليلة ${fmt(M.plan.wake)}`)}</div>
      <h4>الشكل</h4><div class="grp">${row('set-look', 'isha', 'فاتح وداكن', { auto: 'تلقائي', light: 'فاتح دايماً', dark: 'داكن دايماً' }[G.theme])}</div>
      <h4>عن «يوم»</h4><div class="grp">${row('set-about', 'seal', 'الخصوصية والمحتوى', 'كلشي على تلفونك')}</div><div style="height:24px"></div>`;
  },
  'set-mosque'(M) {
    return `<div class="sh"><h2>طابق جامعك</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">افتح تقويم جامعك وقارن ويّا هالأوقات. كل دقيقة تضيفها أو تنقصها تنطبق على كل الأيام.</p>
      <div class="mos">${SIX.map(([k, n]) => { const a = G.adj[k] || 0; return `<div class="row">${tile(k === 'sunrise' ? 'fajr' : k, k !== 'sunrise', 20, 'sm')}<span class="tx">${n}</span>
        <span class="big">${fmt(M.tt[k] + a)}</span><span class="adj${a ? ' ch' : ''}">${a > 0 ? '+' : ''}${a}</span>
        <span class="step sm"><button data-adj="${k}:-1" aria-label="${n} دقيقة أبكر">−</button><button data-adj="${k}:1" aria-label="${n} دقيقة أتأخر">+</button></span></div>`; }).join('')}</div>
      <button class="test" data-adjreset="1">رجّعها مثل ما كانت</button>`;
  },
  'set-alerts'() {
    const ok = G.alertsOk;
    const c = (good, title, sub, fix) => `<div class="chk">${tile(good ? 'seal' : 'hourglass', false, 22)}<div><b>${title}</b><span>${sub}</span>${good ? '' : `<br><button class="fix" data-fix="1">${fix}</button>`}</div></div>`;
    return `<div class="sh"><h2>التنبيهات</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">ثلاث شغلات لازم تكون مسموحة حتى يطلع الأذان بوقته حتى لو التلفون مقفول.</p>
      ${c(true, 'الإشعارات', 'مسموحة')}
      ${c(ok, 'الأذان بالدقيقة', ok ? '«المنبهات والتذكيرات» مسموحة' : 'بدونها الأذان ممكن يتأخر دقائق. اضغط وفعّل «السماح بضبط المنبهات والتذكيرات».', 'افتح الإعداد')}
      ${c(true, 'البطارية ما توقف التطبيق', '«يوم» بلا قيود')}
      <h4>تلفونك شاومي؟ سوّي هذني بعد</h4>
      <p class="note">الإعدادات ← التطبيقات ← «يوم» ← التشغيل التلقائي: شغّله.<br>الإعدادات ← التطبيقات ← «يوم» ← توفير البطارية: بلا قيود.</p>
      <div class="grp"><button class="row" data-toast="ينفتح اختيار الصوت من إعدادات تلفونك">${tile('mosque', false, 20, 'sm')}<span class="tx">صوت الأذان<small>اختاره من أصوات تلفونك</small></span>${chev}</button></div>
      <button class="test" data-toast="بعد 10 ثواني يطلع أذان تجريبي">جرّب الأذان هسه</button>`;
  },
  'set-method'() {
    return `<div class="sh"><h2>طريقة الحساب</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">اختار اللي يستعملها جامعك. إذا ما تعرف، خليها على الأولى وطابق الدقائق من «طابق جامعك».</p>
      ${METHODS.map(([k, n, s]) => `<button class="opt" role="radio" aria-checked="${G.method === k}" data-method="${k}"><span class="tx">${n}${s ? `<small>${s}</small>` : ''}</span><span class="ck${G.method === k ? ' on' : ''}">${G.method === k ? icon('check', 15, 3) : ''}</span></button>`).join('')}`;
  },
  'set-city'() {
    return `<div class="sh"><h2>المدينة</h2><button class="back" data-go="settings">رجوع</button></div>
      <button class="opt" data-toast="بالتطبيق يطلب الموقع التقريبي مرة وحدة"><span class="tx">موقعي الحالي<small>تقريبي، يبقى على تلفونك</small></span>${chev}</button>
      ${CITY_LIST.map((n, i) => `<button class="opt" role="radio" aria-checked="${i === 0}"><span class="tx">${n}</span><span class="ck${i === 0 ? ' on' : ''}">${i === 0 ? icon('check', 15, 3) : ''}</span></button>`).join('')}`;
  },
  'set-asr'() {
    return `<div class="sh"><h2>وقت العصر</h2><button class="back" data-go="settings">رجوع</button></div>
      ${[['SHAFI', 'الجمهور', 'لما يصير ظل الشي مثله'], ['HANAFI', 'الحنفي', 'لما يصير ظل الشي مثلينه']].map(([k, n, s]) => `<button class="opt" role="radio" aria-checked="${G.madhab === k}"><span class="tx">${n}<small>${s}</small></span><span class="ck${G.madhab === k ? ' on' : ''}">${G.madhab === k ? icon('check', 15, 3) : ''}</span></button>`).join('')}`;
  },
  'set-look'() {
    return `<div class="sh"><h2>فاتح وداكن</h2><button class="back" data-go="settings">رجوع</button></div>
      ${[['auto', 'تلقائي', 'داكن من العشاء لحد الفجر'], ['light', 'فاتح دايماً', ''], ['dark', 'داكن دايماً', '']].map(([k, n, s]) => `<button class="opt" role="radio" aria-checked="${G.theme === k}" data-look="${k}"><span class="tx">${n}${s ? `<small>${s}</small>` : ''}</span><span class="ck${G.theme === k ? ' on' : ''}">${G.theme === k ? icon('check', 15, 3) : ''}</span></button>`).join('')}`;
  },
  'set-work'() {
    const D = ['السبت', 'الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'];
    const on = store.get('v2work', [0, 1, 2, 3, 4, 5]);
    return `<div class="sh"><h2>أيام الدوام</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">الأيام المعلّمة دوام، والباقي عطلة وإلها جدولها. الجمعة إلها جدولها دايماً.</p>
      <div class="dchips">${D.map((n, i) => `<button data-wd="${i}" aria-pressed="${i < 6 && on.includes(i)}"${i === 6 ? ' disabled' : ''}>${n.slice(0, i === 6 ? 5 : 3)}</button>`).join('')}</div>
      <h4>مواعيد بساعة ثابتة</h4>
      <div class="grp"><button class="row" data-go="set-day">${tile('case', false, 20, 'sm')}<span class="tx">الدوام<small>08:00 إلى 14:00 · الأحد للخميس</small></span>${chev}</button>
      <button class="row" data-go="set-day"><span class="tx"><b>+ أضف موعد</b></span></button></div>`;
  },
  'set-roles'() {
    const R = [['العبادة', 'beads'], ['الجسم والنوم', 'steps'], ['الزوج أو الزوجة', 'house'], ['الأبناء', 'child'], ['العمل', 'case'], ['التركيز والتطوير', 'nib'], ['التعلم', 'book'], ['وقتك لنفسك', 'tea']];
    return `<div class="sh"><h2>أدوارك</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">النقاط العشر تتبع أدوارك. الدور اللي تطفيه تختفي نقطته.</p>
      ${R.map(([n, ic]) => `<div class="row">${tile(ic, false, 20, 'sm')}<span class="tx">${n}</span><button class="sw" role="switch" aria-checked="true" aria-label="${n}"></button></div>`).join('')}`;
  },
  'set-day'(M) {
    const seg = [['من الفجر للشروق', ['fajr', 'ghadwa']], ['من الشروق للظهر', ['light', 'deep1', 'duha', 'deep2', 'msgs1', 'work1']], ['من الظهر للعصر', ['dhuhr', 'nap', 'work2']]];
    const by = Object.fromEntries(M.ext.map((b) => [b.key, b]));
    return `<div class="sh"><h2>رتّب يومك</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">الصلوات ثابتة بوقتها. اضغط أي فقرة حتى تغيّر اسمها أو مدتها أو تخفيها أو تقدّمها. الباقي ينضبط وحده ويّا الشمس.</p>
      <div class="days">${['يوم الدوام', 'يوم العطلة', 'الجمعة'].map((n, i) => `<button aria-pressed="${i === 0}">${n}</button>`).join('')}</div>
      ${seg.map(([h, keys]) => `<h4>${h}</h4>` + keys.filter((k) => by[k]).map((k) => { const b = by[k]; return `<div class="row">${tile(iconOf(b), b.isPrayer, 20, 'sm')}<span class="tx">${esc(b.title)}<small>${b.isPrayer ? 'صلاة، ثابتة' : dur(b)}</small></span>${b.isPrayer ? '' : chev}</div>`; }).join('') + '<div class="row"><span class="tx"><b>+ أضف فقرة هنا</b></span></div>').join('')}
      <p class="note">… وباقي اليوم بنفس الشكل.</p>`;
  },
  'set-about'() {
    return `<div class="sh"><h2>عن «يوم»</h2><button class="back" data-go="settings">رجوع</button></div>
      <p class="note">بلا إنترنت، بلا حساب، بلا إعلانات. كل شي يبقى على تلفونك. التطبيق يذكّر وينظّم، وما يفتي.</p>
      <p class="note">الأذكار والآيات تنضاف بس بعد ما يراجعها مراجع شرعي، ووياها مصدرها ودرجتها.</p>`;
  },
};

C.A.render = function (root, M) {
  const dark = G.theme === 'dark' || (G.theme === 'auto' && (M.t >= M.tt.isha || M.t < M.tt.fajr));
  root.classList.toggle('dark', dark);
  const y = root.scrollTop;
  const body = G.page ? `<main class="scr">${SET[G.page](M)}</main>` : (G.tab === 'today' ? top2(M) : topRow(M)) + `<main class="scr">${SCREENS[G.tab](M)}</main>`;
  root.innerHTML = body + (G.page ? '' : `<nav class="ios-tab" aria-label="الأقسام">${TABS.map(([k, n, i]) => `<button data-tab="${k}"${k === G.tab ? ` aria-current="page" style="--ic:${ICOL[i]}"` : ''}>${yi(i, 26, 1.6)}<b>${n}</b></button>`).join('')}</nav>`)
    + (G.sheet && !G.page ? sheetHtml(M).replace('VEILTOP', y + 'px') : '') + '<div class="toast" id="aToast" role="status"></div>';
  root.style.overflowY = G.sheet && !G.page ? 'hidden' : '';
  root.scrollTop = y;
  const toast = (msg) => { const t = root.querySelector('#aToast'); t.textContent = msg; t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 2200); };
  const go = (fn, keep) => { const yy = root.scrollTop; fn(); renderAll(true); root.scrollTop = keep ? yy : 0; };
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if ((el = q('[data-close]')) && (e.target === el || el.classList.contains('close'))) return go(() => { G.sheet = null; }, true);
    if (q('[data-done]') && G.sheet) return go(() => { markDone(M, G.sheet); }, true);
    if ((el = q('[data-go]'))) return go(() => { G.page = el.dataset.go || null; G.sheet = null; });
    if ((el = q('[data-tab]'))) return go(() => { G.tab = el.dataset.tab; G.shift = 0; G.sheet = null; });
    if ((el = q('[data-shift]'))) return go(() => { G.shift = +el.dataset.shift; }, true);
    if ((el = q('[data-sheet]'))) return go(() => { G.sheet = el.dataset.sheet; }, true);
    if (q('[data-done]') && M.cur) return go(() => markDone(M, M.cur.key), true);
    if ((el = q('[data-adj]'))) { const [k, d] = el.dataset.adj.split(':'); return go(() => { G.adj[k] = Math.max(-30, Math.min(30, (G.adj[k] || 0) + +d)); saveG('v2adj', G.adj); }, true); }
    if (q('[data-adjreset]')) return go(() => { Object.keys(G.adj).forEach((k) => { G.adj[k] = 0; }); saveG('v2adj', G.adj); }, true);
    if ((el = q('[data-method]'))) return go(() => { G.method = el.dataset.method; saveG('v2method', G.method); }, true);
    if ((el = q('[data-look]'))) return go(() => { G.theme = el.dataset.look; saveG('v2theme', G.theme); }, true);
    if ((el = q('[data-wd]'))) { const i = +el.dataset.wd, on = store.get('v2work', [0, 1, 2, 3, 4, 5]); return go(() => saveG('v2work', on.includes(i) ? on.filter((x) => x !== i) : on.concat(i)), true); }
    if (q('[data-fix]')) return go(() => { G.alertsOk = true; saveG('v2ok', true); syncOk(); }, true);
    if ((el = q('[data-sw]'))) {
      const k = el.dataset.sw;
      return go(() => { if (k === 'qiyam') { G.qiyam = !G.qiyam; saveG('qiyam', G.qiyam); } if (k === 'blocks') { G.blocksRem = !G.blocksRem; saveG('v2blocks', G.blocksRem); } if (k === 'alarm') { G.alarm = !G.alarm; saveG('alarm', G.alarm); } }, true);
    }
    if ((el = q('[data-n]'))) { const a = el.dataset.n; return go(() => { if (a === 'bed-') G.bed = Math.max(1170, G.bed - 15); if (a === 'bed+') G.bed = Math.min(1410, G.bed + 15); saveG('bed', G.bed); }, true); }
    if ((el = q('[data-toast]'))) return toast(el.dataset.toast);
    if ((el = q('.sw[role="switch"]'))) { el.setAttribute('aria-checked', String(el.getAttribute('aria-checked') !== 'true')); }
  };
  root.onkeydown = (e) => { if (e.key === 'Escape' && G.sheet) go(() => { G.sheet = null; }, true); };
};

function syncOk() { const c = document.getElementById('okToggle'); if (c) c.checked = !G.alertsOk; }
document.addEventListener('change', (e) => {
  if (e.target.id === 'okToggle') { G.alertsOk = !e.target.checked; saveG('v2ok', G.alertsOk); renderAll(true); }
});
