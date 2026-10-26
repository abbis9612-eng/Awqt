/* ================= A · الرزنامة ================= */
function wobblyRing(seed) {
  const pts = []; let r = seed;
  const rnd = () => { r = (r * 9301 + 49297) % 233280; return r / 233280; };
  for (let a = -0.5; a <= Math.PI * 2 + 0.35; a += 0.32) {
    const k = 1 + (rnd() - 0.5) * 0.06 + (a > Math.PI * 2 ? 0.06 : 0);
    pts.push([50 + 48 * k * Math.cos(a), 50 + 46 * k * Math.sin(a)]);
  }
  let d = `M${pts[0][0].toFixed(1)} ${pts[0][1].toFixed(1)}`;
  for (let i = 1; i < pts.length; i++) { const [x0, y0] = pts[i - 1], [x1, y1] = pts[i]; d += ` Q${x0.toFixed(1)} ${y0.toFixed(1)} ${((x0 + x1) / 2).toFixed(1)} ${((y0 + y1) / 2).toFixed(1)}`; }
  return `<svg class="ring" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><path vector-effect="non-scaling-stroke" d="${d}"/></svg>`;
}
const penTick = '<svg class="tick" width="16" height="14" viewBox="0 0 16 14" aria-hidden="true"><path d="M2 8 L6 12 L14 2" fill="none" stroke="#1D3FB8" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const penStrike = (fresh) => `<svg class="strike${fresh ? ' fresh' : ''}" viewBox="0 0 100 10" preserveAspectRatio="none" aria-hidden="true"><path vector-effect="non-scaling-stroke" d="M2 6 C 25 3, 45 7, 70 4 S 92 6, 99 3"/></svg>`;

C.A = {
  name: 'الرزنامة', sub: 'ورقة الحايط اللي تنشق', swatch: 'linear-gradient(135deg,#CF1F2D 50%,#fff 50%)',
  info: {
    title: 'أ · الرزنامة', from: 'من رزنامة الحايط اللي بكل بيت عراقي، اللي كل ورقة بيها التاريخين وأوقات الصلاة.',
    idea: 'كل يوم ورقة وحدة. فوق: رقم اليوم كبير بالأحمر، والهجري، وأوقات الصلاة مطبوعة. تحت: جدولك كأنه مكتوب على الورقة. وإنت تأشّر عليها بقلم أزرق وتخطط بالفسفوري، مثل ما تسوي بالورقة الحقيقية.',
    sig: 'أربع أحبار بس، وكل حبر إله معنى: الأحمر للصلاة، والأسود للجدول، والأزرق قلمك (الصلاة الجاية بدائرة، واللي خلّصته تشطب عليه)، والأصفر الفسفوري للحين. وبآخر الليل تشق الورقة وتطلع ورقة باچر.',
    why: ['تفهمها من أول نظرة لأن كل عراقي يعرف هالرزنامة', 'الصلاة الجاية مدوّرة بالقلم، ما تحتاج تدور عليها', 'الشطب يعطي إحساس إنجاز حقيقي', 'زر «خلّصت» ورقة لاصقة صفرة دايماً تحت إبهامك'],
    try: 'اضغط الورقة الصفرة «خلّصت» وشوف القلم يشطب. واسحب الحافة المثقبة فوق الورقة لتحت حتى تشقها.',
  },
  render(root, M) {
    const ui = G.ui.A || (G.ui.A = {});
    const nextK = M.next ? (M.next.key === 'fajr2' ? 'fajr' : M.next.key) : null;
    const nextIsTomorrow = M.next && M.next.key === 'fajr2';
    const pt = M.PR.map(([k, n, m], i) => {
      const ring = !nextIsTomorrow && k === nextK;
      const past = m <= M.t && k !== 'sunrise';
      return `<div class="${past ? 'past' : ''}">${ring ? wobblyRing(17 + i * 13) : ''}${past ? penTick : ''}<i>${n}</i><b>${fmt(m)}</b></div>`;
    }).join('');
    const pen = M.next ? `${M.next.name} بعد ${durText(M.next.m - M.t)}` : '';
    const list = M.ext.filter((b) => !b.sleep && b.key !== 'qiyam2');
    const ci = list.indexOf(M.cur);
    const past = list.filter((b) => b.end <= M.t), rest = list.filter((b) => b.start > M.t);
    const showPast = ui.past, showAll = ui.all;
    const row = (b) => {
      const done = M.done.has(b.key), now = b === M.cur;
      const cls = ['r', b.isPrayer ? 'pr' : '', now ? 'now' : '', done || b.end <= M.t ? 'done' : ''].join(' ');
      const du = now ? `باقي ${durShort(b.end - M.t)}` : durShort(b.end - b.start);
      return `<div class="${cls}"><span class="tm">${fmt(b.start)}</span><span class="ti"><span>${esc(b.title)}</span></span><span class="du">${du}</span>${done ? penStrike(ui.fresh === b.key) : ''}</div>`;
    };
    let rows = '';
    if (past.length) rows += showPast ? past.map(row).join('') : `<button class="more" data-a="past">${past.length} فقرات فاتت، اضغط تشوفها</button>`;
    if (M.cur && !M.cur.sleep && M.cur.key !== 'qiyam2') rows += row(M.cur);
    const shown = showAll ? rest : rest.slice(0, 5);
    rows += shown.map(row).join('');
    if (rest.length > shown.length) rows += `<button class="more" data-a="all">بعدها ${rest.length - shown.length} فقرات، اضغط تشوفها</button>`;
    const p = M.plan, asleep = M.cur && (M.cur.sleep || M.cur.key === 'qiyam2');
    const cur = M.cur, isDone = cur && M.done.has(cur.key);
    root.innerHTML = `
      <div class="board"><b>يوم</b><span>${M.city} · ${M.year}</span></div>
      <div class="pad"><article class="leaf" id="aLeaf" aria-label="ورقة ${M.day} ${M.dayNum} ${M.month}">
        <div class="perf" id="aPerf" role="button" tabindex="0" aria-label="شق الورقة وروح لباچر"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M6 13l6 6 6-6"/></svg>اسحب لتحت حتى تشق الورقة</div>
        <div class="mband">${M.month} ${M.year}</div>
        <div class="big">${M.dayNum}</div>
        <div class="wd">${M.day}</div>
        <div class="hj"><span class="r">${M.hijri}</span></div>
        <h4>أوقات الصلاة · ${M.city}</h4>
        <div class="pt">${pt}</div>
        ${pen ? `<div class="pen">${pen}</div>` : ''}
        <h4>جدولك</h4>
        <div class="rows">${rows}</div>
        <div class="night${asleep ? ' now' : ''}"><div><i>تنام</i><b>${fmt(p.bedtime)}</b></div><div><i>${p.qiyam ? 'تكعد للسَّحَر' : 'تكعد'}</i><b>${fmt(p.wake)}</b></div><div><i>نوم الليل</i><b>${hm(p.night)}</b></div>
          <div class="ok">${p.shortfall ? `ينقصك ${durText(p.shortfall)}، نام أبكر` : `مع قيلولة ${p.nap} د، نومك يكفي ✓`}</div></div>
      </article></div>
      ${cur ? `<button class="note" id="aDone"><span class="ck">${isDone ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#1C1C1C" stroke-width="3" stroke-linecap="round"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>' : ''}</span><b>${isDone ? 'مشطوبة' : doneLabel(cur)}</b><span>${cur.sleep ? 'علّم إنك كعدت' : esc(cur.title)}${isDone ? ' · اضغط ترجعها' : ''}</span></button>` : ''}
      <div class="wallnote">${G.shift ? `هذي ورقة ${M.day}. <button data-a="back">رجوع لورقة اليوم</button>` : 'كل ليلة بعد المحاسبة، شق الورقة.'}</div>`;
    ui.fresh = null;
    root.onclick = (e) => {
      const a = e.target.closest('[data-a]');
      if (a) { const k = a.dataset.a; if (k === 'past') ui.past = true; if (k === 'all') ui.all = true; if (k === 'back') G.shift = 0; renderAll(true); return; }
      if (e.target.closest('#aDone') && cur) { const was = M.done.has(cur.key); markDone(M, cur.key); ui.fresh = was ? null : cur.key; renderAll(true); }
    };
    const leaf = root.querySelector('#aLeaf'), perf = root.querySelector('#aPerf');
    const tear = () => { leaf.classList.add('falling'); leaf.style.transform = 'translateY(110%) rotate(-9deg)'; buzz(); setTimeout(() => { G.shift = Math.min(G.shift + 1, 6); ui.past = ui.all = false; renderAll(true); }, REDUCE ? 10 : 600); };
    let y0 = null, dy = 0;
    perf.onpointerdown = (e) => { y0 = e.clientY; dy = 0; perf.setPointerCapture(e.pointerId); };
    perf.onpointermove = (e) => { if (y0 == null) return; dy = Math.max(0, e.clientY - y0); leaf.style.transform = `translateY(${dy * 0.8}px) rotate(${-dy * 0.03}deg)`; };
    perf.onpointerup = perf.onpointercancel = () => { if (y0 == null) return; y0 = null; if (dy > 100) tear(); else { leaf.style.transition = 'transform .3s'; leaf.style.transform = ''; setTimeout(() => leaf.style.transition = '', 320); } };
    perf.onkeydown = (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); tear(); } };
  },
};
