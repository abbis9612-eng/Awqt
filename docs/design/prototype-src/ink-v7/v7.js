/* ================= «يوم» الحبر، النسخة السابعة: التنبيهات والأصوات، ويومك من الصفر =================
   Owner (2026-10-29): design the notifications and their sounds precisely; show how different
   alerts and tasks fit together without a mess; and let him build his own day from the start.
   - Every kind of notification has its own look, sound and vibration, shown as real phone
     notifications you can hear here. The sounds are original, made from plain tones on the phone
     (no recordings, no rights to clear). The adhan is never made: the user picks it from the phone.
   - One set of rules keeps them in order (same minute → one notification; a sound at most once in
     two minutes; only alarms during sleep; focus silences all but alarms and prayers), and the
     page lists today's real notifications after the rules, so nothing is a promise on paper.
   - Start choice: a ready day you change, or from zero (prayers and sleep only) where you add your
     own items; free time between them is «وقت فاضي» and your tasks drop into it.
   Android facts checked 2026-10-29: a channel's sound and importance cannot change after it is
   created (developer.android.com, notification channels); full-screen alarms on Android 14+ are a
   special access granted by default only to apps whose core function is alarms or calls, with a
   Play Console declaration (Play Console Help 13392821). */

Object.assign(G, { scratch: store.get('v7scratch', false), mine: store.get('v7mine', []), ring: null, addMine: null, qAll: false });
const persist7 = () => { store.set('v7scratch', G.scratch); store.set('v7mine', G.mine); };

/* ---- sounds: original, made here from plain tones ---- */
let AC = null, ringBus = null, ringT = null;
const ac = () => (AC = AC || new (window.AudioContext || window.webkitAudioContext)());
function tone(t0, f, d, o = {}) {
  const c = ac(), osc = c.createOscillator(), g = c.createGain();
  osc.type = o.type || 'sine'; osc.frequency.setValueAtTime(f, t0);
  if (o.f2) osc.frequency.exponentialRampToValueAtTime(o.f2, t0 + d);
  g.gain.setValueAtTime(0.0001, t0); g.gain.linearRampToValueAtTime(o.v || .25, t0 + (o.a || .006)); g.gain.exponentialRampToValueAtTime(0.0001, t0 + d);
  osc.connect(g).connect(o.dest || c.destination); osc.start(t0); osc.stop(t0 + d + .05);
}
const SOUNDS = {
  drop: { name: 'قطرة', use: 'تبدي فقرة', vib: [40], play(t) { tone(t, 1046, .5, { f2: 620, v: .3 }); tone(t + .02, 2093, .18, { v: .05 }); } },
  tap: { name: 'نقرتين', use: 'قبل الفقرة بـ5 أو 10 دقايق', vib: [20, 90, 20], play(t) { tone(t, 1568, .09, { v: .2 }); tone(t + .15, 1568, .09, { v: .2 }); } },
  seal: { name: 'ختم', use: 'تخلص فقرة', vib: [70], play(t) { tone(t, 150, .28, { v: .55, f2: 70 }); tone(t + .03, 784, .7, { v: .1 }); } },
  task: { name: 'ورقة', use: 'وقت مهمة', vib: [30, 60, 30], play(t) { [659, 784, 988].forEach((f, i) => tone(t + i * .11, f, .32, { v: .18 })); } },
  prayer: { name: 'جرس هادي', use: 'تنبيه الصلاة', vib: [80, 140, 80], play(t) { tone(t, 587, 1.5, { v: .2 }); tone(t, 1174, 1, { v: .05 }); tone(t + .65, 880, 1.8, { v: .18 }); tone(t + .65, 1760, 1.1, { v: .04 }); } },
  focus: { name: 'نَفَس', use: 'خلص التركيز', vib: [50, 100, 50, 100, 50], play(t) { [880, 740, 587].forEach((f, i) => tone(t + i * .3, f, .8, { v: .18 })); } },
  alarm: { name: 'صحيان', use: 'المنبّه والصحيان، يعلى شوية شوية', vib: [400, 200, 400, 200, 400], play(t, n = 4, dest) {
    for (let i = 0; i < n; i++) { const v = .06 + .3 * Math.min(1, i / 8); [784, 988, 1175].forEach((f, j) => tone(t + i * 1.1 + j * .2, f, .22, { v, type: 'triangle', dest })); } } },
};
function playSound(k) {
  try { const c = ac(); if (c.state === 'suspended') c.resume(); SOUNDS[k].play(c.currentTime + .05); if (navigator.vibrate) navigator.vibrate(SOUNDS[k].vib); } catch (e) { /* no sound here */ }
}
function ringStart() {
  try {
    const c = ac(); if (c.state === 'suspended') c.resume();
    ringBus = c.createGain(); ringBus.connect(c.destination);
    SOUNDS.alarm.play(c.currentTime + .05, 28, ringBus);
    if (navigator.vibrate) navigator.vibrate([400, 200, 400, 200, 400, 200, 400]);
  } catch (e) { /* no sound here */ }
}
function ringStop() { try { if (ringBus) { ringBus.gain.setValueAtTime(0, ac().currentTime); ringBus.disconnect(); } if (navigator.vibrate) navigator.vibrate(0); } catch (e) {} ringBus = null; clearInterval(ringT); }

/* ---- one set of rules for every notification of the day ---- */
const KIND = {
  alarm: { name: 'منبّه', snd: 'alarm', rank: 5 }, wake: { name: 'الصحيان', snd: 'alarm', rank: 5 },
  prayer: { name: 'صلاة', snd: 'prayer', rank: 4 }, start: { name: 'تبدي', snd: 'drop', rank: 3 },
  task: { name: 'مهمة', snd: 'task', rank: 3 }, end: { name: 'تخلص', snd: 'seal', rank: 2 },
  before: { name: 'قبلها', snd: 'tap', rank: 2 }, summary: { name: 'ملخص', snd: 'drop', rank: 1 },
};
function dayNotifs(M) {
  const ev = [], add = (at, kind, text, ic) => ev.push({ at: Math.round(at), kind, text, ic });
  M.ext.forEach((b) => {
    if (b.sleep || b.key.startsWith('gap')) return;
    if (b.isPrayer) { const r = G.prayerAlert[PKEY[b.key]]; if (r !== 'none') add(b.start, 'prayer', `${b.title}${r === 'adhan' ? '، بالأذان' : ''}`, iconOf(b)); return; }
    const r = remOf(b);
    if (r === 'start') add(b.start, 'start', `هسه: ${b.title}`, iconOf(b));
    else if (r === 5 || r === 10) add(b.start - r, 'before', `بعد ${r} دقايق: ${b.title}`, iconOf(b));
    if (G.endAlert && b.key !== 'qiyam2') add(b.end, 'end', `خلص: ${b.title}`, iconOf(b));
  });
  const keep = G.shift; G.shift = 0; const placed = placeTasks(M); G.shift = keep;
  placed.filter((x) => x.slot && !x.task.done).forEach((x) => add(x.start, 'task', `مهمة: ${x.task.t} (${x.task.d} د)`, 'seal'));
  alarmsToday(M).forEach((a) => add(a.at < M.tt.fajr ? a.at + 1440 : a.at, a.at === M.plan.wake ? 'wake' : 'alarm', a.label, 'hourglass'));
  if (G.morning) add(M.tt.sunrise, 'summary', 'ملخص الصبح', 'page');
  if (G.evening) add(M.plan.bedtime - 30, 'summary', 'محاسبة الليل', 'page');
  ev.sort((a, b) => a.at - b.at || KIND[b.kind].rank - KIND[a.kind].rank);
  const out = [];
  for (const e of ev) {
    const last = out[out.length - 1];
    if (last && last.at === e.at) { last.lines.push(e); continue; }
    out.push({ at: e.at, kind: e.kind, ic: e.ic, lines: [e] });
  }
  let lastSound = -99;
  for (const n of out) {
    const alarmish = KIND[n.kind].rank >= 5;
    if (!alarmish && n.at > M.plan.bedtime && n.at < M.plan.wake) n.mute = 'وقت نومك';
    else if (!alarmish && n.at - lastSound < 2) n.mute = 'رن شي قبل أقل من دقيقتين';
    if (!n.mute) lastSound = n.at;
  }
  return out;
}

/* ---- a day from zero: prayers and sleep stay, the rest is yours ---- */
function iconOf(b) {
  const k = b.key;
  if (b.mineIc) return b.mineIc;
  if (k.startsWith('gap')) return 'palm';
  const m = { fajr: 'fajr', dhuhr: 'dhuhr', asr: 'asr', maghrib: 'maghrib', isha: 'isha', jumua: 'mosque', ghadwa: 'rahl', light: 'steps', deep1: 'nib', deep2: 'nib', duha: 'tea', msgs1: 'mail', close: 'mail', work1: 'phone', work2: 'case', nap: 'nap', child: 'child', family: 'house', self: 'book', prep: 'page', witr: 'beads', sleep: 'bed', qiyam: 'lantern', qiyam2: 'lantern', jumua_prep: 'ghusl', lunch: 'meal', visit: 'visit', free: 'palm', dua: 'hands', self_am: 'tea' };
  if (k === 'train') return /مشي/.test(b.title) ? 'steps' : /تعلم/.test(b.title) ? 'laptop' : 'dumbbell';
  return m[k] || 'page';
}
const MINE_IC = ['nib', 'case', 'book', 'laptop', 'dumbbell', 'steps', 'house', 'child', 'tea', 'phone'];
function scratchDay(M) {
  const fixed = M.ext.filter((b) => b.isPrayer || b.sleep || ['qiyam2', 'witr'].includes(b.key)).map((b) => ({ ...b }));
  const mine = [];
  G.mine.slice().sort((a, b) => a.at - b.at).forEach((m) => {
    let s = Math.max(m.at, fixed[0].start), e = s + m.d;
    for (let k = 0; k < 12; k++) {
      const hit = fixed.concat(mine).find((f) => s < f.end && e > f.start && f.start <= s);
      if (!hit) break; s = hit.end; e = s + m.d;
    }
    const nxt = fixed.concat(mine).filter((f) => f.start >= s).sort((a, b) => a.start - b.start)[0];
    if (nxt && e > nxt.start) e = nxt.start;
    if (e - s >= 10) mine.push({ key: 'mine' + m.id, title: m.t, start: s, end: e, isPrayer: false, mineIc: m.ic, shrunkFrom: e - s < m.d ? m.d : null });
  });
  const all = fixed.concat(mine).sort((a, b) => a.start - b.start), ext = [];
  all.forEach((b, i) => {
    ext.push(b);
    const n = all[i + 1];
    if (n && n.start - b.end >= 10 && !b.sleep) { const key = 'gap' + i; FREE.add(key); ext.push({ key, title: 'وقت فاضي', start: b.end, end: n.start, isPrayer: false }); }
  });
  M.ext = ext;
  M.curI = ext.findIndex((b) => b.start <= M.t && M.t < b.end);
  M.cur = ext[M.curI] || null;
}
Object.assign(WHAT, { gap: 'وقت ما حاطط بيه شي. مهامك تنحط هنا وحدها، أو اضغط حتى تحط فقرة إلك.' });

/* ---- what the notifications look like ---- */
const nf = (o) => `<div class="nf${o.big ? ' big' : ''}"><div class="nh"><span class="nlogo">ي</span><span>يوم</span><span>· ${o.at}</span>${o.silent ? '<span class="sil">بلا صوت</span>' : ''}</div>
  <div class="nb">${tile(o.ic, o.solid, 22, 'sm')}<div><b>${o.title}</b><p>${o.body}</p>${o.bar != null ? `<span class="pb" aria-hidden="true"><i style="width:${o.bar}%"></i></span>` : ''}</div></div>
  ${o.acts ? `<div class="na">${o.acts.map((a) => `<button data-toast="بالتطبيق: «${a}»">${a}</button>`).join('')}</div>` : ''}
  ${o.snd ? `<div class="ns"><span>الصوت: <b>${SOUNDS[o.snd].name}</b> · رجفة ${SOUNDS[o.snd].vib.length > 1 ? `${Math.ceil(SOUNDS[o.snd].vib.length / 2)} مرات` : 'وحدة'}</span><button class="play" data-play="${o.snd}" aria-label="اسمع صوت ${SOUNDS[o.snd].name}">اسمع</button></div>` : ''}</div>`;
SET.notifs = function (M) {
  const up = M.ext.filter((b) => b.start > M.t && !b.isPrayer && !b.sleep && !b.key.startsWith('gap'));
  const a = up[0] || M.ext[0], b2 = up[1] || a, cur = M.cur && !M.cur.isPrayer ? M.cur : a, nextAfter = M.ext.find((x) => cur && x.start >= cur.end) || b2;
  const keep = G.shift; G.shift = 0; const task = placeTasks(M).find((x) => x.slot && !x.task.done); G.shift = keep;
  const pr = M.ext.find((x) => x.isPrayer && x.start > M.t) || M.ext.find((x) => x.isPrayer);
  const q = dayNotifs(M), merged = q.filter((n) => n.lines.length > 1).length, muted = q.filter((n) => n.mute).length;
  const shown = G.qAll ? q : q.filter((n) => n.at >= M.t);
  return `<div class="sh"><h2>التنبيهات</h2><button class="back" data-go="">رجوع</button></div>
    <p class="note">كل نوع إله شكل وصوت ورجفة خاصة بيه، حتى تعرف شنو هو قبل لا تطلّع بالتلفون. اضغط «اسمع».</p>
    <h4>شكل كل تنبيه</h4>
    ${nf({ at: fmt(a.start), ic: iconOf(a), title: `هسه: ${esc(a.title)}`, body: `لحد ${fmt(a.end)} · ${durText(a.end - a.start)}`, acts: ['خلّصت', 'بعد 5 دقايق'], snd: 'drop' })}
    ${nf({ at: fmt(b2.start - 5), ic: iconOf(b2), title: `بعد 5 دقايق: ${esc(b2.title)}`, body: `تبدي ${fmt(b2.start)}`, acts: ['تمام'], snd: 'tap' })}
    ${nf({ at: fmt(cur.end), ic: iconOf(nextAfter), title: `هسه: ${esc(nextAfter.title)}`, body: `وخلص قبلها: ${esc(cur.title)}. تنبيهين بنفس الدقيقة صاروا واحد.`, acts: ['خلّصت', 'مدّد 10 دقايق'], snd: 'drop' })}
    ${task ? nf({ at: fmt(task.start), ic: 'seal', title: `مهمة: ${esc(task.task.t)}`, body: `${task.task.d} دقيقة، ضمن «${esc(task.slot.title)}»`, acts: ['خلّصت', 'أجّلها لباچر'], snd: 'task' }) : ''}
    ${nf({ at: fmt(pr.start), ic: iconOf(pr), solid: true, title: esc(pr.title), body: G.prayerAlert[PKEY[pr.key]] === 'adhan' ? 'بالأذان اللي اخترته من تلفونك' : 'وقت الصلاة', acts: ['صلّيت'], snd: 'prayer' })}
    ${nf({ at: fmt(M.t), ic: 'nib', title: 'تركيز · باقي 42:10', body: 'التنبيهات الثانية ساكتة إلا المنبّه والصلاة', bar: 35, acts: ['وقّف', '+5 دقايق'], silent: true })}
    ${nf({ at: fmt(M.tt.sunrise), ic: 'page', title: 'يومك', body: morningText(M), snd: 'drop' })}
    <div class="ringcard"><div><b>شاشة المنبّه</b><span>تطلع على الشاشة كلها حتى لو التلفون مقفول، والصوت يعلى شوية شوية.</span></div><button class="save" data-ring="1">شغّلها هسه</button></div>
    <h4>الأصوات</h4>
    <div>${Object.entries(SOUNDS).map(([k, s]) => `<div class="row snd"><span class="tx">${s.name}<small>${s.use}</small></span><span class="vib" aria-label="الرجفة">${s.vib.filter((_, i) => i % 2 === 0).map((d) => `<i style="width:${Math.max(6, d / 10)}px"></i>`).join('')}</span><button class="play" data-play="${k}" aria-label="اسمع ${s.name}">اسمع</button></div>`).join('')}</div>
    <p class="note">هاي الأصوات مسوّية إلنا من نغمات بسيطة، فما بيها حقوق لأحد. الأذان ما نصنعه: تختاره من أصوات تلفونك لكل صلاة. وتكدر تغيّر صوت أي نوع من إعدادات التلفون.</p>
    <h4>شلون تمشي سوا</h4>
    <div class="rules">
      <p><b>نفس الدقيقة:</b> إذا صار تنبيهين سوا، يجيك واحد بيه الثنين.</p>
      <p><b>ما يرن ورا بعض:</b> إذا رن شي قبل أقل من دقيقتين، الثاني يبين بلا صوت.</p>
      <p><b>بوقت نومك:</b> ما يرن إلا المنبّه والصحيان.</p>
      <p><b>بالتركيز:</b> يسكت كلشي إلا المنبّهات والصلاة.</p>
      <p><b>المهام:</b> كل مهمة تاخذ مكان بالفراغ وما تتداخل ويّا غيرها. تخلص وحدة، اللي بعدها تتقدم.</p></div>
    <h4>تنبيهات اليوم بعد هاي القواعد</h4>
    <p class="note">${q.length} تنبيه اليوم. ${merged ? `${merged} منها بيها أكثر من شي لأنها بنفس الدقيقة` : 'ولا وحدة بنفس دقيقة غيرها'}، و${muted ? `${muted} تبين بلا صوت` : 'كلها برنّة'}.</p>
    <div class="q7">${shown.map((n) => `<div class="qr${n.at < M.t ? ' past' : ''}"><em>${fmt(n.at)}</em>${tile(n.ic, n.kind === 'prayer', 18, 'sm')}
      <span class="tx"><b>${esc(n.lines.slice().sort((x, y) => KIND[y.kind].rank - KIND[x.kind].rank)[0].text)}</b>${n.lines.length > 1 ? n.lines.slice().sort((x, y) => KIND[y.kind].rank - KIND[x.kind].rank).slice(1).map((l) => `<small>+ ${esc(l.text)}</small>`).join('') : ''}</span>
      <span class="qs">${n.mute ? `<i class="m">بلا صوت</i><small>${n.mute}</small>` : `<i>${SOUNDS[KIND[n.lines.slice().sort((x, y) => KIND[y.kind].rank - KIND[x.kind].rank)[0].kind].snd].name}</i>`}</span></div>`).join('')}</div>
    ${q.some((n) => n.at < M.t) ? `<button class="pastbtn" data-qall="1">${G.qAll ? 'خفي اللي فات' : 'شوف اللي فات هم'}</button>` : ''}
    <div style="height:24px"></div>`;
};
function ringHtml(M) {
  const s = Math.floor((Date.now() - G.ring.at) / 1000), p = Math.min(100, s / 30 * 100);
  return `<div class="ring7 opening" role="alertdialog" aria-label="المنبّه يرن" style="top:VEILTOP">
    <p class="rt">${fmt(M.plan.wake % 1440)}</p><p class="rl">منبّه الصحيان لصلاة الليل</p>
    ${inkWord('اصحى', p, 120, 'المنبّه يرن')}
    <p class="when">الصوت يعلى شوية شوية خلال 30 ثانية</p>
    <div class="acts5 two"><button class="act pri" data-ringstop="1">${yi('seal', 24, 1.8)}<b>صحيت</b></button><button class="act" data-ringsnooze="1">${yi('hourglass', 24, 1.8)}<b>بعد 5 دقايق</b></button></div></div>`;
}
function addMineHtml() {
  const a = G.addMine;
  return `<div class="veil opening" data-close7="1" style="top:VEILTOP;height:800px"><div class="sheet s5" role="dialog" aria-label="فقرة إلك">
    <div class="grab"></div><h3>فقرة إلك</h3>
    <div class="addbox"><input id="mineT" type="text" maxlength="40" placeholder="شنو تسوي؟ مثلاً: دراسة" value="${esc(a.t)}" aria-label="اسم الفقرة"></div>
    <div class="row"><span class="tx">تبدي</span><span class="step"><button data-mt="-15" aria-label="أبكر ربع ساعة">−</button><output>${fmt(a.at)}</output><button data-mt="15" aria-label="أتأخر ربع ساعة">+</button></span></div>
    <div class="dchips" role="group" aria-label="كم تاخذ">${[30, 60, 90, 120].map((d) => `<button data-md="${d}" aria-pressed="${a.d === d}">${d < 60 ? d + ' د' : d === 60 ? 'ساعة' : d === 90 ? 'ساعة ونص' : 'ساعتين'}</button>`).join('')}</div>
    <h4>أيقونتها</h4><div class="icpick" role="radiogroup" aria-label="الأيقونة">${MINE_IC.map((i) => `<button role="radio" aria-checked="${a.ic === i}" data-mic="${i}">${tile(i, false, 22, 'sm')}</button>`).join('')}</div>
    <p class="note">إذا وقعت على صلاة، تنزاح لبعدها. الصلاة ما تتحرك.</p>
    <button class="save" data-msave="1">حطّها بيومي</button><button class="close" data-close7="1">سكّر</button></div></div>`;
}

/* ---- glue ---- */
const w4 = SET.welcome;
SET.welcome = (M) => {
  const h = w4(M);
  if (G.ob !== 1) return h;
  const pick = `<h4>شلون تبدي؟</h4><div role="radiogroup" aria-label="شلون تبدي">
    <button class="opt big" role="radio" aria-checked="${!G.scratch}" data-scratch="0">${tile('week', false, 22, 'sm')}<span class="tx">يوم جاهز وأعدّله<small>كل الفقرات حاضرة، وتغيّر أو تشيل اللي تريده</small></span><span class="ck${!G.scratch ? ' on' : ''}">${!G.scratch ? icon('check', 15, 3) : ''}</span></button>
    <button class="opt big" role="radio" aria-checked="${G.scratch}" data-scratch="1">${tile('nib', false, 22, 'sm')}<span class="tx">أبدي من الصفر<small>بس الصلوات والنوم، وأنت تحط فقراتك</small></span><span class="ck${G.scratch ? ' on' : ''}">${G.scratch ? icon('check', 15, 3) : ''}</span></button></div>`;
  return h.replace('<button class="save" data-ob="next"', pick + '<button class="save" data-ob="next"');
};
let v7Over = '';
const render6b = C.A.render;
C.A.render = function (root, M) {
  if (G.scratch) scratchDay(M);
  render6b(root, M);
  const y = root.scrollTop;
  if (G.tab === 'today' && !G.page && G.scratch) {
    const rh = root.querySelector('.rh');
    if (rh) rh.insertAdjacentHTML('afterend', '<p class="scratchnote">بديت يومك من الصفر: الصلوات والنوم ثابتة، والباقي إلك. <button data-scratch="0">رجّع اليوم الجاهز</button></p>');
    const rail = root.querySelector('.rail5');
    if (rail) rail.insertAdjacentHTML('afterend', '<button class="addmine" data-mineopen="1">+ أضف فقرة إلك</button>');
  }
  if (G.tab === 'alarms' && !G.page) {
    const w = root.querySelector('.scr .when');
    if (w) w.insertAdjacentHTML('afterend', `<button class="tonight" data-go="notifs">${tile('hourglass', false, 22)}<div>شكل التنبيهات وأصواتها<span>اسمعها، وشوف شلون تطلع وشلون تمشي سوا</span></div><i>‹</i></button>`);
  }
  if (G.sheet && G.sheet.startsWith('mine')) {
    const at = root.querySelector('.sheet .acts5');
    if (at) at.insertAdjacentHTML('afterend', `<button class="close" data-mdel="${G.sheet.slice(4)}">احذف هاي الفقرة</button>`);
  }
  if (G.sheet && G.sheet.startsWith('gap')) {
    const at = root.querySelector('.sheet .hd');
    if (at && !root.querySelector('.sheet .what')) at.insertAdjacentHTML('afterend', `<div class="what"><b>شنو هاي؟</b>${WHAT.gap}</div>`);
  }
  let over = '';
  if (G.ring) over = ringHtml(M);
  else if (G.addMine) over = addMineHtml();
  if (over) {
    const key = G.ring ? 'ring' : 'mine';
    root.insertAdjacentHTML('beforeend', over.replace('VEILTOP', y + 'px'));
    root.style.overflowY = 'hidden';
    if (key === v7Over) root.querySelectorAll('.ring7, .veil[data-close7]').forEach((el) => el.classList.remove('opening'));
    v7Over = key;
    fitWords(root);
  } else v7Over = '';
  /* Overlays sit where you are looking. renderAll empties the screen first (scroll jumps to 0) and
     the scroll is put back only after render, so place every sheet once the frame settles. */
  requestAnimationFrame(() => root.querySelectorAll('.veil, .focus, .ring7').forEach((el) => { el.style.top = root.scrollTop + 'px'; }));
  const mi = root.querySelector('#mineT');
  if (mi) mi.oninput = () => { G.addMine.t = mi.value; };
  const base = root.onclick;
  const go = (fn, keep = true) => { const yy = root.scrollTop; fn(); persist7(); renderAll(true); root.scrollTop = keep ? yy : 0; };
  const toast = (msg) => { const t = root.querySelector('#aToast'); if (!t) return; t.textContent = msg; t.classList.add('on'); setTimeout(() => t.classList.remove('on'), 2400); };
  root.onclick = (e) => {
    const q = (s) => e.target.closest(s);
    let el;
    if ((el = q('[data-play]'))) return playSound(el.dataset.play);
    if (q('[data-ring]')) { go(() => { G.ring = { at: Date.now() }; }); ringStart();
      ringT = setInterval(() => {
        const w = document.querySelector('.ring7 .word');
        if (!G.ring || !w) return;
        w.style.setProperty('--p', Math.min(100, (Date.now() - G.ring.at) / 300) + '%');
      }, 1000);
      return; }
    if (q('[data-ringstop]')) { ringStop(); go(() => { G.ring = null; }); return toast('صحيت. يومك بدا'); }
    if (q('[data-ringsnooze]')) { ringStop(); go(() => { G.ring = null; }); return toast('ينبّهك مرة ثانية بعد 5 دقايق'); }
    if (q('[data-qall]')) return go(() => { G.qAll = !G.qAll; });
    if ((el = q('[data-scratch]'))) return go(() => { G.scratch = el.dataset.scratch === '1'; });
    if (q('[data-mineopen]')) return go(() => { G.addMine = { t: '', at: Math.ceil((M.t + 1) / 15) * 15, d: 60, ic: 'nib' }; });
    if ((el = q('[data-sheet]')) && el.dataset.sheet.startsWith('gap')) { const g = M.ext.find((b) => b.key === el.dataset.sheet); return go(() => { G.addMine = { t: '', at: g ? g.start : M.t, d: 60, ic: 'nib' }; }); }
    if ((el = q('[data-close7]')) && (e.target === el || el.classList.contains('close'))) return go(() => { G.addMine = null; });
    if ((el = q('[data-mt]'))) return go(() => { G.addMine.at = Math.max(0, G.addMine.at + +el.dataset.mt); });
    if ((el = q('[data-md]'))) return go(() => { G.addMine.d = +el.dataset.md; });
    if ((el = q('[data-mic]'))) return go(() => { G.addMine.ic = el.dataset.mic; });
    if (q('[data-msave]')) {
      const t = (G.addMine.t || '').trim();
      if (!t) return toast('اكتب اسم الفقرة أول');
      go(() => { G.mine.push({ id: Date.now() % 1e9, t, at: G.addMine.at, d: G.addMine.d, ic: G.addMine.ic }); G.addMine = null; G.scratch = G.scratch || false; });
      return toast('انحطّت بيومك');
    }
    if ((el = q('[data-mdel]'))) return go(() => { G.mine = G.mine.filter((m) => String(m.id) !== el.dataset.mdel); G.sheet = null; });
    return base(e);
  };
};
