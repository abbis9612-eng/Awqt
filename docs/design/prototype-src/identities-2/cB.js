/* ================= B · لوحة الجامع ================= */
const SEG = { 0: 'abcdef', 1: 'bc', 2: 'abged', 3: 'abgcd', 4: 'fgbc', 5: 'afgcd', 6: 'afgedc', 7: 'abc', 8: 'abcdefg', 9: 'abcdfg', '-': 'g', ' ': '' };
function segShape(s, t = 2.1) {
  const g = 0.45, h = (x1, x2, y) => `${x1},${y} ${x1 + t / 2},${y - t / 2} ${x2 - t / 2},${y - t / 2} ${x2},${y} ${x2 - t / 2},${y + t / 2} ${x1 + t / 2},${y + t / 2}`,
    v = (x, y1, y2) => `${x},${y1} ${x + t / 2},${y1 + t / 2} ${x + t / 2},${y2 - t / 2} ${x},${y2} ${x - t / 2},${y2 - t / 2} ${x - t / 2},${y1 + t / 2}`;
  return { a: h(1 + g, 9 - g, 1), b: v(9, 1 + g, 9 - g), c: v(9, 9 + g, 17 - g), d: h(1 + g, 9 - g, 17), e: v(1, 9 + g, 17 - g), f: v(1, 1 + g, 9 - g), g: h(1 + g, 9 - g, 9) }[s];
}
/* seven-segment readout; unlit segments stay faintly visible like a real board */
function seven(str, hPx, color, cls = 'led') {
  const k = hPx / 18; let x = 0, body = '';
  for (const ch of str) {
    if (ch === ':') { body += `<g transform="translate(${x} 0)"><circle cx="2" cy="5.6" r="1.25" fill="${color}"/><circle cx="1.3" cy="12.4" r="1.25" fill="${color}"/></g>`; x += 4.5; continue; }
    const on = SEG[ch] || '';
    body += `<g transform="translate(${x} 0)">` + [...'abcdefg'].map((sg) => `<polygon points="${segShape(sg)}" fill="${color}" opacity="${on.includes(sg) ? 1 : 0.07}"/>`).join('') + '</g>';
    x += 12.5;
  }
  const w = x - 2.5;
  return `<svg class="seg ${cls}" width="${(w * k).toFixed(1)}" height="${hPx}" viewBox="-1 -1 ${w + 2} 20" aria-hidden="true"><g transform="skewX(-6) translate(1 0)">${body}</g></svg>`;
}
const hms = (s) => { s = Math.max(0, Math.round(s)); const h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60), x = s % 60; return `${h}:${pad(m)}:${pad(x)}`; };

C.B = {
  name: 'لوحة الجامع', sub: 'أرقام مضوية تعد تنازلي', swatch: 'radial-gradient(circle at 40% 40%,#FF3B27 0 22%,#060807 26%)',
  info: {
    title: 'ب · لوحة الجامع', from: 'من لوحة أوقات الصلاة الرقمية المعلقة بجوامعنا: إطار خشب، زجاج أسود، وأرقام حمرة مضوية، وشريط مكتوب يمشي.',
    idea: 'التطبيق كله لوحة وحدة: الساعة الحين كبيرة، وتحتها عد تنازلي بالثواني للصلاة الجاية، والأوقات الستة بشبابيك، وشريط يمشي يكلك شنو المفروض تسوي هسه. تحتها أزرار حقيقية تنداس.',
    sig: 'الأرقام المضوية (سبع قطع) والقطع المطفية باينة بخفة مثل اللوحة الحقيقية، والشريط اللي يمشي من اليسار لليمين.',
    why: ['أوضح شي بالعالم للأرقام: تقرأها من بعيد', 'العد التنازلي بالثواني يعطيك إحساس الوقت الحقيقي', 'الصلاة الجاية شباكها مضوي بالأصفر', 'زر واحد كبير «صلّيت» أو «خلّصت» تحت إبهامك'],
    try: 'خليه على «الآن» وشوف الثواني تعد. جرّب «نايم» وشوف اللوحة تصير عد للكعدة.',
  },
  render(root, M) {
    const ui = G.ui.B || (G.ui.B = { list: true });
    const asleep = M.cur && (M.cur.sleep || M.cur.key === 'qiyam2');
    const target = asleep && M.cur.sleep ? { name: M.plan.qiyam ? 'السَّحَر' : 'الكعدة', m: M.plan.wake, lbl: 'تكعد بعد' } : M.next ? { name: M.next.name, m: M.next.m, lbl: `${M.next.name} بعد` } : null;
    const nextK = M.next && M.next.key !== 'fajr2' ? M.next.key : null;
    const wins = M.PR.map(([k, n, m]) => `<div class="win${k === nextK ? ' next' : m <= M.t ? ' past' : ''}"><b>${n}</b>${seven(fmt(m), 30, k === nextK ? '#FFB21E' : '#FF3B27', k === nextK ? 'amb' : 'led')}</div>`).join('');
    const cur = M.cur, nb = M.ext[M.curI + 1];
    const mq = asleep ? `نوم الهنا ◆ المنبّه ${fmt(M.plan.wake)} ◆ نوم الليل ${hm(M.plan.night)} ◆ مع قيلولة ${M.plan.nap} د ${M.plan.shortfall ? 'ينقصك ' + durText(M.plan.shortfall) : 'نومك يكفي'}`
      : cur ? `الحين: ${cur.title} ◆ باقي ${durText(cur.end - M.t)} ◆ بعدها: ${nb ? nb.title + ' ' + fmt(nb.start) : ''}` : '';
    const list = M.ext.filter((b) => b.end > M.t).slice(0, 7).map((b) => {
      const now = b === cur, done = M.done.has(b.key);
      return `<div class="li${b.isPrayer ? ' pr' : ''}${now ? ' now' : ''}"><span class="t">${fmt(b.start)}</span><span class="n">${now ? '<span class="dot"></span>' : ''}${esc(b.title)}</span><span class="d">${done ? '<span class="ok">✓</span>' : now ? 'الحين' : durShort(b.end - b.start)}</span></div>`;
    }).join('');
    const isDone = cur && M.done.has(cur.key);
    root.innerHTML = `<div class="frame"><div class="glass" role="img" aria-label="${M.day} ${M.dayNum} ${M.month}. ${target ? target.lbl + ' ' + durText(target.m - M.t) : ''}">
        <div class="top"><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div>
        <div class="clock" id="bClock"></div>
        <div class="cd"><div class="l" id="bLbl">${target ? target.lbl : ''}<small>${target ? fmt(target.m) : ''}</small></div><div id="bCount"></div></div>
        <div class="wins">${wins}</div>
        <div class="mq" aria-hidden="true"><div class="run">${esc(mq)}</div></div>
      </div></div>
      <div class="keys">
        ${cur ? `<button class="key" id="bDone">${isDone ? '✓ ' : ''}${isDone ? 'تم' : doneLabel(cur)}<small>${cur.sleep ? 'من النوم' : esc(cur.title)}</small></button>` : '<span></span>'}
        <button class="key sec${ui.list ? ' on' : ''}" id="bList" aria-pressed="${!!ui.list}">الجدول</button>
      </div>
      ${ui.list ? `<div class="list">${list}</div>` : ''}
      <div class="plate">${M.city} · حسب ${CITIES[CITY].method}</div>`;
    this.tick(root, M, target);
    root.onclick = (e) => {
      if (e.target.closest('#bDone') && cur) { markDone(M, cur.key); renderAll(true); }
      if (e.target.closest('#bList')) { ui.list = !ui.list; renderAll(true); }
    };
  },
  tick(root, M, target) {
    if (!target) { const asleep = M.cur && M.cur.sleep; target = asleep ? { m: M.plan.wake } : M.next; }
    const c = root.querySelector('#bClock'), k = root.querySelector('#bCount'); if (!c) return;
    const mm = ((M.t % 1440) + 1440) % 1440;
    c.innerHTML = seven(fmt(mm), 78, '#FF3B27') + seven(pad(M.sec), 30, '#FF3B27');
    if (target) k.innerHTML = seven(hms((target.m - M.t) * 60 - M.sec), 46, '#FFB21E', 'amb');
  },
};
