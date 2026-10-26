/* ================= «يوم» بهوية الحبر: اليوم، الليل، الأسبوع، المحاسبة ================= */
// docs/METHOD.md section 5, Saturday first
const WEEKPLAN = [
  ['التخطيط', 'مراجعة أسبوعية 45 د وكتلة عمق', 'مشي أو راحة', 'جلسة الشورى مع الزوجة'],
  ['العمل العميق', 'كتلتان تركيز', 'تدريب قوة', 'الأسرة'],
  ['العمل', 'كتلة: قرارات العمل، واجتماع الشركاء', 'تعلم تقني', 'الأسرة'],
  ['العمل العميق', 'كتلتان تركيز', 'تدريب قوة', 'تعلم شرعي'],
  ['الميدان', 'كتلة، ثم زيارات', 'مشي سريع 40 دقيقة', 'تعلم تقني'],
  ['الإنجاز', 'كتلة إنجاز ومراجعة، وإغلاق الأسبوع', 'تدريب قوة', 'خروج مع الزوجة'],
  ['الروح والأهل', 'غُسل والتبكير للجمعة، وصلة الرحم', 'بلا تلفون حتى المغرب', 'فراغ مباح'],
];
const WEEK_IC = [['page', 'steps', 'house'], ['nib', 'dumbbell', 'house'], ['case', 'laptop', 'house'], ['nib', 'dumbbell', 'book'], ['case', 'steps', 'laptop'], ['seal', 'dumbbell', 'house'], ['mosque', 'hands', 'palm']];
const SAT_FIRST = ['السبت', 'الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'];
// docs/METHOD.md: the ten daily points and the three evening questions
const POINTS = [['الصلوات الخمس في وقتها', 'mosque'], ['ورد الغدوة', 'rahl'], ['بلا تلفون أول ساعة وآخر ساعة', 'phone'], ['كتلة عمق وحدة على الأقل', 'nib'], ['تدريب أو مشي', 'steps'],
  ['قيلولة أو راحة الظهر', 'nap'], ['20 دقيقة مع الزوجة', 'house'], ['30 دقيقة مع الأبناء', 'child'], ['تعلم أو خلوة تفكر', 'book'], ['النوم بالموعد', 'bed']];
const QUESTIONS = ['ماذا فعلت اليوم لله، ولأهلي، ولبدني، ولعملي؟', 'أين ضاع وقت بلا فائدة، ولماذا؟', 'ما أهم 3 مهام غداً؟'];
const TABS = [['today', 'اليوم', 'dhuhr'], ['night', 'الليل', 'isha'], ['week', 'الأسبوع', 'week'], ['page', 'المحاسبة', 'page']];

const inkWord = (word, p, fs, label, cls = '') => `<div class="word ${cls}" style="--p:${Math.max(0, Math.min(100, p)).toFixed(1)}%;--fs:${fs}px" role="img" aria-label="${label}">${word}</div>`;
const fitFs = (w, big = 138) => w.length > 7 ? 88 : w.length > 5 ? 112 : big;
const topRow = (M) => `<div class="top"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div>${G.shift ? '<button class="back" data-n="back">رجوع لليوم</button>' : `<div class="clk">${fmt(M.t)}</div>`}</div>`;

const SCREENS = {
  today(M) {
    const P = M.PR.filter(([k]) => k !== 'sunrise'), s = inkState(M), cur = M.cur, nb = upcoming(M, 2);
    const PI = { fajr: 'fajr', dhuhr: M.d.friday ? 'mosque' : 'dhuhr', asr: 'asr', maghrib: 'maghrib', isha: 'isha' };
    const five = P.map(([k, n, m]) => { const nx = k === s.key, fill = m <= M.t ? 100 : nx ? s.p : 0; return `<div class="${nx ? 'nx' : ''}"><i style="--ic:${ICOL[PI[k]]}">${yi(PI[k], 26)}</i><b style="--p:${fill.toFixed(1)}%">${n}</b><span>${fmt(m)}</span></div>`; }).join('');
    return `${inkWord(s.word, s.p, fitFs(s.word), `${s.word} بعد ${durText(s.tgt - M.t)}`)}
      <p class="when">بعد ${durText(s.tgt - M.t)}، الساعة <em>${fmt(s.tgt)}</em></p>
      ${cur ? `<section class="now">${tile(iconOf(cur), cur.isPrayer, 30, 'big')}<div><small>هسه</small><h3>${esc(cur.title)}</h3><small>لحد ${fmt(cur.end)}</small></div>${doneBtn(M, 'seal')}</section>` : ''}
      <div class="after">${nb.map((b) => `<div>${tile(iconOf(b), b.isPrayer, 20, 'sm')}<em>${fmt(b.start)}</em><span>${esc(b.title)}</span></div>`).join('')}</div>
      <div class="five">${five}</div>`;
  },
  night(M) {
    const p = M.plan, tt = M.tt, asleep = M.t >= p.bedtime;
    const s = asleep ? inkState(M) : { word: 'النوم', tgt: p.bedtime, p: (M.t - tt.isha) / (p.bedtime - tt.isha) * 100 };
    const lbl = asleep ? `${s.word} بعد ${durText(s.tgt - M.t)}` : M.t < tt.isha ? `تنام الساعة ${fmt(p.bedtime)}` : `تنام بعد ${durText(p.bedtime - M.t)}`;
    // the night as one ink line: sleep, then suhur up to Fajr
    const a = p.bedtime, z = M.nx.fajr + 1440, X = (m) => 338 - (m - a) / (z - a) * 334;
    let ln = `<line x1="4" x2="338" y1="22" y2="22" stroke="var(--faint)" stroke-width="10" stroke-linecap="round"/>
      <line x1="${X(a)}" x2="${X(p.wake)}" y1="22" y2="22" stroke="var(--ink)" stroke-width="10" stroke-linecap="round"/>`;
    if (p.qiyam) ln += `<line x1="${X(p.wake) - 7}" x2="${X(z)}" y1="22" y2="22" stroke="${ICOL.lantern}" stroke-width="10" stroke-linecap="round"/>`;
    [[a, 'تنام'], [p.wake, p.qiyam ? `السَّحَر، والفجر ${fmt(z)}` : `تكعد، والفجر ${fmt(z)}`]].forEach(([m, n], i) => { const x = i ? 4 : X(m);
      ln += `<text x="${x.toFixed(1)}" y="50" font-family="Amiri" font-size="17" fill="var(--ink)" text-anchor="${i ? 'start' : 'end'}" direction="ltr">${fmt(m)}</text><text x="${x.toFixed(1)}" y="66" font-size="12.5" fill="var(--grey)" text-anchor="${i ? 'end' : 'start'}">${n}</text>`; });
    if (M.t >= a && M.t < z) ln += `<circle cx="${X(M.t).toFixed(1)}" cy="22" r="8" fill="var(--bgc,#fff)" stroke="var(--ink)" stroke-width="3"/>`;
    const ok = !p.shortfall;
    const steps = [['isha', 'صلاة العشاء', tt.isha], ['page', 'الاستعداد للنوم: محاسبة وترتيب الغد', M.ext.find((b) => b.key === 'prep')?.start], ...(p.qiyam ? [] : [['beads', 'الوتر وأذكار النوم', M.ext.find((b) => b.key === 'witr')?.start]]),
      ['bed', `النوم، ${hm(p.night)} ساعات`, p.bedtime], ...(p.qiyam ? [['lantern', 'السَّحَر: ركعتان والوتر', p.wake]] : []), ['fajr', 'صلاة الفجر', z]].filter((x) => x[2] != null);
    const curStep = steps.reduce((acc, x, i) => (M.t >= x[2] ? i : acc), -1);
    return `${inkWord(s.word, s.p, fitFs(s.word), lbl)}
      <p class="when">${asleep ? `بعد ${durText(s.tgt - M.t)}، الساعة <em>${fmt(s.tgt)}</em>` : `تنام <em>${fmt(p.bedtime)}</em>، وتكعد <em>${fmt(p.wake)}</em>${p.qiyam ? ' للسَّحَر' : ''}`}</p>
      <div class="nline"><svg viewBox="0 0 342 68" role="img" aria-label="من ${fmt(a)} لحد الفجر ${fmt(z)}">${ln}</svg></div>
      <div class="verdict">${tile(ok ? 'seal' : 'hourglass', false, 24)}<div>${ok ? 'نومك يكفي' : `ينقصك ${durText(p.shortfall)}`}<small>ليل ${hm(p.night)} وقيلولة ${p.nap} د، المجموع ${hm(p.total)} من 7 ساعات</small></div></div>
      <h4>ليلتك خطوة بخطوة</h4>
      <div>${steps.map(([ic, t, m], i) => `<div class="row${i === curStep ? ' on' : i < curStep ? ' past' : ''}">${tile(ic, ['isha', 'fajr'].includes(ic), 20, 'sm')}<em>${fmt(m)}</em><span class="tx">${t}</span></div>`).join('')}</div>
      <h4>عدّل ليلتك</h4>
      <div class="ctl"><div class="row"><span class="tx">ساعة نومك<small>ما تنام قبل العشاء بنص ساعة</small></span><span class="step"><button data-n="bed-" aria-label="أبكر ربع ساعة">−</button><output>${fmt(G.bed)}</output><button data-n="bed+" aria-label="أتأخر ربع ساعة">+</button></span></div>
        <div class="row"><span class="tx">السَّحَر قبل الفجر<small>إذا بقى للنوم 6 ساعات أو أكثر</small></span><button class="sw" role="switch" aria-checked="${G.qiyam}" data-n="qiyam" aria-label="السحر قبل الفجر"></button></div>
        <div class="row"><span class="tx">منبّه الكعدة ${fmt(p.wake)}<small>يتحرك وياك كل ليلة</small></span><button class="sw" role="switch" aria-checked="${!!store.get('alarm', false)}" data-n="alarm" aria-label="منبه الكعدة"></button></div></div>
      <div style="height:18px"></div>`;
  },
  week(M) {
    const s0 = M.doy - ((weekdayOf(M.doy) + 1) % 7), today = M.doy;
    const days = SAT_FIRST.map((_, i) => { const di = s0 + i; if (di < 0 || di > 363) return null; const d = resolveDay(CITY, MADHAB, di, G.bed, G.qiyam); return d; });
    const valid = days.filter(Boolean);
    const sum = (f) => valid.reduce((a, d) => a + d.blocks.filter(f).reduce((q, b) => q + b.end - b.start, 0), 0);
    const deep = sum((b) => b.role === 'DEV'), fam = sum((b) => b.role === 'SPOUSE' || b.role === 'CHILD'), body = sum((b) => ['light', 'train'].includes(b.key)), sl = valid.reduce((a, d) => a + d.plan.total, 0) / valid.length;
    const ix = today - s0, frac = (ix + Math.min(1, Math.max(0, (M.t - M.tt.fajr) / (M.plan.bedtime - M.tt.fajr)))) / 7 * 100;
    const f = dateOf(Math.max(0, s0)), l = dateOf(Math.min(364, s0 + 6));
    const rows = days.map((d, i) => { if (!d) return ''; const di = s0 + i, [focus, am, asr, eve] = WEEKPLAN[i], dd = dateOf(di);
      return `<button class="day${di === today ? ' today' : ''}${di < today ? ' past' : ''}" data-day="${di}"><span class="dn"><b>${SAT_FIRST[i]}</b><span>${dd.getUTCDate()}</span></span>
        <span class="fc"><b>${focus}</b><span>${am}، والعصر: ${asr}</span></span><span class="ics">${WEEK_IC[i].map((n) => tile(n, false, 18, 'xs')).join('')}</span></button>`; }).join('');
    const satNext = (6 - weekdayOf(today) + 7) % 7, satD = dateOf(Math.min(364, today + satNext));
    return `${inkWord('الأسبوع', frac, 112, `مشى ${ix + 1} من 7 أيام`)}
      <p class="when">من السبت ${f.getUTCDate()} إلى الجمعة ${l.getUTCDate()} ${MONTHS[l.getUTCMonth()]}</p>
      <div class="tot"><div>${tile('nib', false, 22)}<span><b>${durShort(deep)}</b>تركيز، الهدف 10 إلى 15 س</span></div><div>${tile('house', false, 22)}<span><b>${durShort(fam)}</b>ويّا الأهل</span></div>
        <div>${tile('steps', false, 22)}<span><b>${durShort(body)}</b>حركة، الهدف 150 د</span></div><div>${tile('bed', false, 22)}<span><b>${hm(sl)}</b>نوم بالمعدل</span></div></div>
      <h4>خطة الأسبوع</h4><div>${rows}</div>
      <div class="review">${tile('week', false, 22)}<div><b>${satNext === 0 ? 'اليوم مراجعة الأسبوع، 45 دقيقة' : `مراجعة الأسبوع: السبت ${satD.getUTCDate()} ${MONTHS[satD.getUTCMonth()]}`}</b>خمس خطوات، وتغيّر شي واحد بس على نظامك.</div></div>`;
  },
  page(M) {
    const pg = store.get('page:' + M.doy, { pts: [], q: ['', '', ''] }), n = pg.pts.length;
    return `${inkWord('المحاسبة', n * 10, 92, `${n} من 10`)}
      <p class="when"><em>${n}</em> من <em>10</em> · الهدف 7 بالمعدل، والفوت مرة مقبول</p>
      <h4>نقاط اليوم</h4>
      <div>${POINTS.map(([t, ic], i) => { const on = pg.pts.includes(i); return `<button class="row" role="checkbox" aria-checked="${on}" data-pt="${i}">${tile(ic, false, 20, 'sm')}<span class="tx">${t}</span><span class="ck${on ? ' on' : ''}">${on ? icon('check', 15, 3) : ''}</span></button>`; }).join('')}</div>
      <h4>ثلاث أسئلة قبل النوم</h4>
      ${QUESTIONS.map((q, i) => `<label class="q" for="pq${i}">${q}</label><textarea id="pq${i}" data-q="${i}">${esc(pg.q[i] || '')}</textarea>`).join('')}
      <button class="save" data-n="save">احفظ صحيفة اليوم</button><div class="toast" id="aToast" role="status"></div>`;
  },
};

C.A = {
  name: 'الحبر',
  about: 'هوية «يوم» المعتمدة. بكل شاشة عنوانها كلمة بالرقعة تتعبى حبر بمقياسها: اليوم يتعبى للصلاة الجاية، والليل لوقت النوم، والأسبوع بالأيام اللي مشت، والمحاسبة بنقاطك. الأيقونات خاصة بـ«يوم»: خط حبر وجزء ملون بمعناه. التبويبات تحت تشتغل.',
  render(root, M) {
    root.classList.toggle('dark', M.t >= M.tt.isha || M.t < M.tt.fajr);
    const tab = G.tab;
    root.innerHTML = topRow(M) + `<main class="scr">${SCREENS[tab](M)}</main>
      <nav class="ios-tab" aria-label="الأقسام">${TABS.map(([k, n, i]) => `<button data-tab="${k}"${k === tab ? ` aria-current="page" style="--ic:${ICOL[i]}"` : ''}>${yi(i, 26, 1.6)}<b>${n}</b></button>`).join('')}</nav>`;
    root.onclick = (e) => {
      const t = e.target.closest('[data-tab]'); if (t) { G.tab = t.dataset.tab; if (G.tab !== 'today') G.shift = 0; renderAll(true); root.scrollTop = 0; return; }
      if (e.target.closest('[data-done]') && M.cur) { markDone(M, M.cur.key); renderAll(true); return; }
      const d = e.target.closest('[data-day]'); if (d) { G.shift = +d.dataset.day - M.doy + G.shift; G.tab = 'today'; renderAll(true); root.scrollTop = 0; return; }
      const pt = e.target.closest('[data-pt]');
      if (pt) { const k = 'page:' + M.doy, c = store.get(k, { pts: [], q: ['', '', ''] }), i = +pt.dataset.pt; c.pts = c.pts.includes(i) ? c.pts.filter((x) => x !== i) : c.pts.concat(i); store.set(k, c); buzz(); const y = root.scrollTop; renderAll(true); root.scrollTop = y; return; }
      const nn = e.target.closest('[data-n]'); if (!nn) return;
      const y = root.scrollTop, a = nn.dataset.n;
      if (a === 'bed-') { G.bed = Math.max(1170, G.bed - 15); store.set('bed', G.bed); }
      if (a === 'bed+') { G.bed = Math.min(1410, G.bed + 15); store.set('bed', G.bed); }
      if (a === 'qiyam') { G.qiyam = !G.qiyam; store.set('qiyam', G.qiyam); }
      if (a === 'alarm') store.set('alarm', !store.get('alarm', false));
      if (a === 'back') G.shift = 0;
      if (a === 'save') {
        const k = 'page:' + M.doy, c = store.get(k, { pts: [], q: ['', '', ''] });
        c.q = [0, 1, 2].map((i) => root.querySelector('#pq' + i).value); store.set(k, c);
        const tt = root.querySelector('#aToast'); tt.textContent = 'انحفظت صحيفة اليوم على جهازك'; tt.classList.add('on'); setTimeout(() => tt.classList.remove('on'), 2200); return;
      }
      renderAll(true); root.scrollTop = y;
    };
  },
};
