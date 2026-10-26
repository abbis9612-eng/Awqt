/* ================= E · العدسة ================= */
const E_COL = [['#CFD8F6', 0], ['#FFF0C2', 0], ['#FFE08A', 0], ['#FFC9A0', 0], ['#F7AFAF', 0], ['#5A5FB8', 1], ['#262B5E', 1]];
C.E = {
  name: 'العدسة', sub: 'اليوم كله بشاشة وحدة', swatch: 'linear-gradient(180deg,#CFD8F6,#FFF0C2,#FFE08A,#FFC9A0,#F7AFAF,#5A5FB8,#262B5E)',
  info: {
    title: 'هـ · العدسة', from: 'من فكرة العدسة المكبرة: يومك كله قدامك، والوقت اللي إنت بيه يتكبّر.',
    idea: 'سبع شرائط ملونة، من الفجر لليل، كل شريطة فترة بين صلاتين، ولونها يمشي ويه ضوي اليوم: أزرق فاتح، أصفر، برتقالي، وردي، بنفسجي، كحلي. الفترة اللي إنت بيها تنفتح وتاخذ أغلب الشاشة، وبداخلها فقراتك وخط «الحين» يمشي عليها. الباقي يصغر بس يبقى باين.',
    sig: 'اليوم كله بشاشة وحدة بلا تمرير، والعدسة على الحين. تضغط أي شريطة وتنفتح بحركة ناعمة.',
    why: ['ما تحتاج تمرر أبداً لتشوف يومك', 'خط «الحين» يكلك بالضبط وين إنت داخل الفترة', 'طول الفقرة على الشاشة على كد وقتها', 'زر واحد تحت الفترة المفتوحة'],
    try: 'اضغط على الشرائط المسكرة (العصر، المغرب...) وشوف العدسة تنتقل. وجرّب أوقات مختلفة فوق.',
  },
  render(root, M) {
    const ui = G.ui.E || (G.ui.E = {});
    const tt = M.tt, per = [['الفجر', tt.fajr, tt.sunrise], ['الضحى', tt.sunrise, tt.dhuhr], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr, tt.asr], ['العصر', tt.asr, tt.maghrib], ['المغرب', tt.maghrib, tt.isha], ['العشاء', tt.isha, M.plan.bedtime], ['الليل', M.plan.bedtime, M.nx.fajr + 1440]];
    let pi = per.findIndex(([, a, b]) => M.t >= a && M.t < b); if (pi < 0) pi = 0;
    if (ui.key !== M.doy + ':' + G.preset) { ui.key = M.doy + ':' + G.preset; ui.open = pi; }
    const TOT = 700, CH = 48, GAP = 4, OPEN = TOT - CH * (per.length - 1) - GAP * (per.length - 1);
    const cur = M.cur;
    const bodyOf = (i) => {
      const [, a, b] = per[i], bl = M.ext.filter((x) => x.start >= a && x.start < b);
      const avail = OPEN - CH - 78, ppm = avail / (b - a);
      let y = 0, nowY = null, h = '';
      bl.forEach((x) => {
        const hh = Math.min(150, Math.max(50, (x.end - x.start) * ppm)), past = x.end <= M.t, done = M.done.has(x.key), now = x === cur;
        if (now) nowY = y;
        const pct = now ? Math.round((M.t - x.start) / (x.end - x.start) * 100) : 0;
        h += `<div class="blk${x.isPrayer ? ' pr' : ''}${past ? ' past' : ''}${now ? ' cur' : ''}" style="min-height:${hh.toFixed(0)}px${now ? `;--p:${pct}%` : ''}"><span class="t">${fmt(x.start)}</span><span class="n">${esc(x.title)}</span><span class="d">${done ? '<span class="ck">✓</span>' : now ? `<span class="pill">باقي ${durShort(x.end - M.t)}</span>` : durShort(x.end - x.start)}</span></div>`;
        y += hh;
      });
      return { h, nowY, bl };
    };
    const bands = per.map(([n, a, b], i) => {
      const [bg, dark] = E_COL[i], open = i === ui.open;
      const bl = M.ext.filter((x) => x.start >= a && x.start < b), dn = bl.filter((x) => M.done.has(x.key)).length;
      const ct = M.t >= b ? (dn ? `✓ ${dn} من ${bl.length}` : 'فاتت') : i === pi ? 'الحين' : (bl.length === 1 ? 'فقرة وحدة' : bl.length === 2 ? 'فقرتين' : `${bl.length} ${bl.length < 11 ? 'فقرات' : 'فقرة'}`);
      let inner = '';
      if (open) {
        const bd = bodyOf(i), isDone = cur && M.done.has(cur.key);
        inner = `<div class="body" id="eBody" data-ny="${bd.nowY ?? ''}"><div style="position:relative">${bd.h}</div></div>`;
        inner += i === pi && cur ? `<div class="foot"><button id="eDone">${isDone ? '✓ تم' : doneLabel(cur)}<span>${cur.sleep ? 'من النوم' : esc(cur.title)}</span></button></div>` : `<div class="foot"><button data-e="${pi}">رجوع للحين</button></div>`;
      }
      return `<section class="band${open ? ' open' : ''}${dark ? ' dark' : ''}" style="background:${bg};height:${open ? OPEN : CH}px"><button class="bh" data-e="${i}" aria-expanded="${open}"><b>${n}</b><span class="rg">${fmt(a)} ← ${fmt(b)}</span><span class="ct">${ct}</span></button>${inner}</section>`;
    }).join('');
    root.innerHTML = `<div class="hd"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div><div class="clk">${fmt(M.t)}</div></div><div class="bands">${bands}</div>`;
    const body = root.querySelector('#eBody');
    if (body && body.dataset.ny) body.scrollTop = Math.max(0, +body.dataset.ny - 90);
    root.onclick = (e) => {
      const b = e.target.closest('[data-e]'); if (b) { if (+b.dataset.e !== ui.open) { ui.open = +b.dataset.e; buzz(); renderAll(true); } return; }
      if (e.target.closest('#eDone') && cur) { markDone(M, cur.key); renderAll(true); }
    };
  },
};
