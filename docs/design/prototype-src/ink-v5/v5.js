/* ================= «يوم» الحبر، النسخة الخامسة: أوضح وأسرع، والحبر يتحرك =================
   Owner (2026-10-29): clearer, more practical, very easy and quick, icons and words that say
   exactly what they do, details that are easy to read, and motion that makes it feel alive.
   Same identity («الحبر»): paper, ink, the filling word, our own icons and three fonts.
   What changes:
   - One list for the day («خيط اليوم») instead of «الجاي» + «مهامك» + «يومك كامل». A thin line
     runs down the icons and fills with ink up to now, like the big word. What already passed
     folds into one line you can open.
   - Three clear action buttons under the word, each an icon and one or two words.
   - A reminder mark shows on a row only when it differs from your usual choice.
   - Motion, all for meaning and all off with «تقليل الحركة»: the word pours in when a screen
     opens, «خلّصت» presses like a seal and a ring of ink spreads, a done task gets an ink line
     drawn through it, the now-dot on the line breathes, and sheets rise with a small spring. */

Object.assign(G, { v5past: false });
let v5Key = '', v5Over = '', v5JustTask = null, v5JustItem = null;
const REM_SHORT = { start: 'بوقتها', 5: 'قبل 5 د', 10: 'قبل 10 د', off: 'بدون', adhan: 'أذان', notify: 'تنبيه', none: 'بدون' };
const remMark = (b) => {
  const r = remOf(b);
  if (b.isPrayer) return r === 'notify' ? '' : r === 'adhan' ? 'أذان' : 'بلا تنبيه';
  return String(r) === String(G.remDefault) ? '' : r === 'off' ? 'بلا تنبيه' : REM_SHORT[r];
};

function top2(M) {
  return `<div class="top5"><div><b>${M.day}</b><span>${M.dayNum} ${M.month} · ${M.hijri}</span></div>
    <div class="side"><span class="clk">${fmt(M.t)}</span><button class="txtbtn" data-go="settings">الإعدادات</button></div></div>`;
}

function rail5(M, placed) {
  const live = G.shift === 0, items = M.ext;
  const firstOpen = live ? items.findIndex((b) => b.end > M.t) : 0;
  const pastN = live ? Math.max(0, firstOpen) : 0;
  const open = !live || G.v5past;
  const inside = (b) => placed.filter((x) => x.slot && x.slot.key === b.key)
    .concat(placed.filter((x) => x.task.done && x.task.slotKey === b.key).map((x) => ({ task: x.task, start: x.task.at })))
    .sort((a, c) => a.start - c.start);
  const nTasks = placed.filter((x) => x.slot && !x.task.done).length;
  const rows = items.map((b, i) => {
    if (live && i < firstOpen && !open) return '';
    const past = live && b.end <= M.t, cur = live && b.start <= M.t && M.t < b.end, done = M.done.has(b.key);
    const fill = past ? 100 : cur ? (M.t - b.start) / (b.end - b.start) * 100 : 0;
    const mark = remMark(b), left = b.end - M.t;
    const sub = cur ? `<span class="pb" aria-hidden="true"><i style="width:${fill.toFixed(1)}%"></i></span><small>باقي ${durText(left)} · تخلص ${fmt(b.end)}</small>`
      : b.shrunkFrom ? `<small>قصرت اليوم من ${b.shrunkFrom} د</small>` : '';
    const tasks = inside(b).map((x) => `<div class="tk5${x.task.done ? ' dn' : ''}${v5JustTask === x.task.id ? ' just' : ''}" style="--fill:${fill.toFixed(1)}%">
        <button class="ck${x.task.done ? ' on' : ''}" data-task="${x.task.id}" role="checkbox" aria-checked="${x.task.done}" aria-label="${esc(x.task.t)}">${x.task.done ? icon('check', 14, 3) : ''}</button>
        <span><b>${esc(x.task.t)}</b><small>${fmt(x.start)} · ${x.task.d} د</small></span></div>`).join('');
    return `<div class="r5${cur ? ' cur' : past ? ' past' : ''}${b.isPrayer ? ' pr' : ''}${done ? ' dn' : ''}${v5JustItem === b.key ? ' just' : ''}" style="--fill:${fill.toFixed(1)}%">
      <button class="rb" data-sheet="${b.key}" aria-label="${esc(b.title)}، ${fmt(b.start)} إلى ${fmt(b.end)}${done ? '، خلصت' : ''}">
        <em class="t">${fmt(b.start)}</em><span class="ic">${tile(iconOf(b), b.isPrayer, 18, 'sm')}${cur ? '<i class="nowdot"></i>' : ''}</span>
        <span class="tx"><b>${esc(b.title)}</b>${sub}</span>
        <span class="d">${done ? `<span class="okm">${icon('check', 16, 2.6)}</span>` : dur(b)}${mark ? `<small>${mark}</small>` : ''}</span></button>${tasks}</div>`;
  }).join('');
  return `<div class="rh"><h4>يومك</h4><span>${items.length} فقرة${nTasks ? ` · ${nTasks} مهام بالفراغات` : ''}</span></div>
    ${pastN && live ? `<button class="pastbtn" data-v5past="1" aria-expanded="${open}">${open ? 'خفي اللي فات' : `${pastN === 1 ? 'فاتت فقرة وحدة' : pastN === 2 ? 'فاتت فقرتين' : `فاتت ${pastN} فقرات`}، شوفها`}</button>` : ''}
    <div class="rail5">${rows}</div>`;
}

SCREENS.today = function (M) {
  const s = nowState(M), placed = placeTasks(M), b = s.b, live = G.shift === 0;
  const canFocus = b && FOCUSABLE.has(b.key);
  const unplaced = placed.filter((x) => x.none);
  const hero = b
    ? `<p class="sub5">${esc(b.title)}</p><div class="facts"><span>تخلص <em>${fmt(b.end)}</em></span><span>باقي <em>${durText(b.end - M.t)}</em></span></div>`
    : s.next ? `<p class="sub5">${esc(s.next.title)}</p><div class="facts"><span>تبدي <em>${fmt(s.next.start)}</em></span></div>` : '';
  const acts = b && live ? `<div class="acts5">
      <button class="act pri" data-done="1">${yi('seal', 24, 1.8)}<b>${M.done.has(b.key) ? 'تم' : doneLabel(b)}</b><small>${M.done.has(b.key) ? 'اضغط حتى تلغي' : 'علّمها'}</small></button>
      ${canFocus ? `<button class="act" data-focus="${b.key}">${yi('nib', 24, 1.8)}<b>ركّز</b><small>${durText(b.end - M.t)}</small></button>` : ''}
      <button class="act" data-sheet="${b.key}">${yi('hourglass', 24, 1.8)}<b>نبّهني</b><small>${REM_SHORT[remOf(b)]}</small></button></div>` : '';
  return `${dayChips()}${warnBanner()}
    ${inkWord(s.word, s.p, fitFs(s.word), s.label)}${hero}${acts}
    ${nextPrayerLine(M)}
    ${rail5(M, placed)}
    ${unplaced.length ? `<p class="note">${unplaced.map((x) => `«${esc(x.task.t)}»`).join('، ')}: ما بقى فراغ يكفيها اليوم، تنتقل لباچر.</p>` : ''}
    <button class="tonight" data-tab="night">${tile('bed', false, 22)}<div>الليلة: تنام <b>${fmt(M.plan.bedtime)}</b> وتكعد <b>${fmt(M.plan.wake)}</b>${M.plan.qiyam ? ' للسَّحَر' : ''}
      <span>${M.plan.shortfall ? `ينقصك ${durText(M.plan.shortfall)} عن 7 ساعات` : 'نومك يكفي'}</span></div><i>‹</i></button>
    <button class="tonight" data-tab="page">${tile('page', false, 22)}<div>محاسبة المساء<span>عشر نقاط وثلاث أسئلة، دقيقتين</span></div><i>‹</i></button>`;
};

/* The item's sheet: what it is, when, its reminder in one row, and what you can do. */
function sheet3(M) {
  const b = M.ext.find((x) => x.key === G.sheet);
  if (!b) return '';
  const done = M.done.has(b.key), cur = G.shift === 0 && b.start <= M.t && M.t < b.end;
  const opts = b.isPrayer ? [['adhan', 'أذان'], ['notify', 'تنبيه'], ['none', 'بدون']] : [['start', 'بوقتها'], [5, 'قبل 5'], [10, 'قبل 10'], ['off', 'بدون']];
  const attr = (v) => b.isPrayer ? `data-pray="${PKEY[b.key]}:${v}"` : `data-rem="${b.key}:${v}"`;
  const why = b.shrunkFrom ? `<div class="why">اليوم صارت ${durText(b.end - b.start)} بدل ${durText(b.shrunkFrom)}، لأن الوقت بين الصلاتين أقصر.</div>` : '';
  return `<div class="veil" data-close="1" style="top:VEILTOP;height:800px"><div class="sheet s5" role="dialog" aria-label="${esc(b.title)}">
    <div class="grab"></div><div class="hd">${tile(iconOf(b), b.isPrayer, 30, 'big')}<div><h3>${esc(b.title)}</h3>
    <p class="when2"><em>${fmt(b.start)}</em> ← <em>${fmt(b.end)}</em> · ${durText(b.end - b.start)}</p></div></div>
    ${cur ? `<div class="pb big" aria-hidden="true"><i style="width:${((M.t - b.start) / (b.end - b.start) * 100).toFixed(1)}%"></i></div><p class="note">باقي ${durText(b.end - M.t)}</p>` : ''}${why}
    <h4>${yi('hourglass', 18, 1.8)} ${b.isPrayer ? 'تنبيه الصلاة' : 'نبّهني'}</h4>
    <div class="seg5" role="radiogroup">${opts.map(([v, l]) => `<button ${attr(v)} role="radio" aria-checked="${String(remOf(b)) === String(v)}" aria-pressed="${String(remOf(b)) === String(v)}">${l}</button>`).join('')}</div>
    <div class="acts5 two"><button class="act pri" data-done="1">${yi('seal', 22, 1.8)}<b>${done ? 'تم، ألغيه' : doneLabel(b)}</b></button>
    ${FOCUSABLE.has(b.key) ? `<button class="act" data-focus="${b.key}">${yi('nib', 22, 1.8)}<b>ركّز</b></button>` : b.isPrayer ? '' : `<button class="act" data-go="set-day">${yi('week', 22, 1.8)}<b>عدّلها</b></button>`}</div>
    <button class="close" data-close="1">سكّر</button></div></div>`;
}

/* Glue: entrance and overlay motion only when something new opens; stamp on «خلّصت». */
const render4 = C.A.render;
C.A.render = function (root, M) {
  render4(root, M);
  const key = [G.tab, G.page, G.shift, G.ob].join('|');
  if (key !== v5Key) {
    v5Key = key;
    root.classList.remove('enter'); void root.offsetWidth; root.classList.add('enter');
    clearTimeout(root._enterT); root._enterT = setTimeout(() => root.classList.remove('enter'), 1400);
  }
  const over = G.focus ? 'focus' : G.add ? 'add:' + G.add : G.sheet ? 'sheet:' + G.sheet : '';
  if (over && over !== v5Over) { const el = root.querySelector('.veil, .focus'); if (el) el.classList.add('opening'); }
  v5Over = over;
  v5JustTask = null; v5JustItem = null;
  const base = root.onclick;
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if (q('[data-v5past]')) { const y = root.scrollTop; G.v5past = !G.v5past; renderAll(true); root.scrollTop = y; return; }
    if ((el = q('[data-task]'))) {
      const t = G.tasks.find((x) => x.id === +el.dataset.task);
      if (t && !t.done) {
        v5JustTask = t.id;
        const x = placeTasks(M).find((y) => y.task === t);
        if (x && x.slot) { t.slotKey = x.slot.key; t.at = x.start; }
      }
      return base(e);
    }
    if ((el = q('[data-done]')) && !el.classList.contains('stamping')) {
      const k = G.sheet || (M.cur && M.cur.key);
      if (k && !M.done.has(k)) {
        v5JustItem = k; el.classList.add('stamping');
        if (matchMedia('(prefers-reduced-motion: reduce)').matches) return base(e);
        setTimeout(() => { v5JustItem = k; base(e); }, 420);
        return;
      }
    }
    return base(e);
  };
};
