/* ================= D · الرواق ================= */
const D_COL = ['#2E4C9E', '#137A87', '#93670A', '#AE4529', '#8A2E4A', '#24336F', '#101A40'];
const archPath = (w, h, sp) => `M0 ${h} V${sp} C0 ${(sp * 0.42).toFixed(1)} ${(w * 0.34).toFixed(1)} ${(sp * 0.16).toFixed(1)} ${w / 2} 0 C${(w * 0.66).toFixed(1)} ${(sp * 0.16).toFixed(1)} ${w} ${(sp * 0.42).toFixed(1)} ${w} ${sp} V${h} Z`;
const starTile = (id, col, op) => `<pattern id="${id}" width="30" height="30" patternUnits="userSpaceOnUse"><g transform="translate(15 15)" fill="none" stroke="${col}" stroke-width="1" opacity="${op}"><rect x="-6.5" y="-6.5" width="13" height="13"/><rect x="-6.5" y="-6.5" width="13" height="13" transform="rotate(45)"/><circle r="2.2"/></g><g fill="none" stroke="${col}" stroke-width="1" opacity="${op * 0.7}"><path d="M0 0 L5 5 M30 0 L25 5 M0 30 L5 25 M30 30 L25 25"/></g></pattern>`;

C.D = {
  name: 'الرواق', sub: 'أقواس الجامع وألوان الكاشي', swatch: 'linear-gradient(135deg,#137A87,#2E4C9E)',
  info: {
    title: 'د · الرواق', from: 'من أروقة جوامعنا: صف أقواس، وكل قوس كاشي بلون، والضوي يدخل من القوس.',
    idea: 'يومك سبع فترات بين الصلوات، وكل فترة قوس بلون كاشي. القوس الكبير فوق هو الفترة اللي إنت بيها: بداخله شغلتك الحين بخط كوفي كبير، والقوس يتعبى ضوي على كد ما يمشي الوقت. تحته الرواق كله: سبع أقواس صغار، تضغط على أي قوس وتشوف فقراته.',
    sig: 'القوس المدبب نفسه: هو الإطار وهو مقياس التقدم. والنجمة الثمانية من الكاشي بخفة داخله.',
    why: ['تعرف وين إنت باليوم من لون القوس، حتى بلا قراءة', 'تقسيم اليوم بين الصلوات هو نفسه منطق خوارزمية التطبيق', 'شغلة وحدة كبيرة بالنص، وزر كبير واضح', 'الأقواس اللي فاتت مليانة، والجاية فارغة، فتشوف يومك كله بنظرة'],
    try: 'اضغط على الأقواس الصغار تحت (الضحى، العصر...) وشوف فقراتها. وجرّب «المغرب» و«نايم» حتى تشوف القوس يتلون.',
  },
  render(root, M) {
    const ui = G.ui.D || (G.ui.D = {});
    const tt = M.tt, per = [['الفجر', tt.fajr, tt.sunrise], ['الضحى', tt.sunrise, tt.dhuhr], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr, tt.asr], ['العصر', tt.asr, tt.maghrib], ['المغرب', tt.maghrib, tt.isha], ['العشاء', tt.isha, M.plan.bedtime], ['الليل', M.plan.bedtime, M.nx.fajr + 1440]];
    let pi = per.findIndex(([, a, b]) => M.t >= a && M.t < b); if (pi < 0) pi = 0;
    if (ui.key !== M.doy + ':' + G.preset) { ui.key = M.doy + ':' + G.preset; ui.sel = pi; }
    const col = D_COL[pi], cur = M.cur, asleep = cur && (cur.sleep || cur.key === 'qiyam2');
    const pct = cur ? Math.max(0, Math.min(1, (M.t - cur.start) / (cur.end - cur.start))) : 0;
    const W = 358, H = 392, SP = 168, isDone = cur && M.done.has(cur.key);
    const nextP = M.next ? `<svg width="14" height="14" viewBox="-7 -7 14 14" aria-hidden="true"><rect x="-4" y="-4" width="8" height="8" fill="#fff"/><rect x="-4" y="-4" width="8" height="8" fill="#fff" transform="rotate(45)"/></svg>${M.next.name} ${fmt(M.next.m)} · بعد ${durText(M.next.m - M.t)}` : '';
    const big = `<svg class="shape" viewBox="0 0 ${W} ${H}" aria-hidden="true"><defs><clipPath id="dClip"><path d="${archPath(W, H, SP)}"/></clipPath>${starTile('dStar', '#fff', 0.16)}
        <linearGradient id="dShade" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#000" stop-opacity=".18"/><stop offset=".5" stop-color="#000" stop-opacity="0"/></linearGradient></defs>
      <path d="${archPath(W, H, SP)}" fill="${col}"/>
      <g clip-path="url(#dClip)"><rect width="${W}" height="${H}" fill="url(#dStar)"/><rect y="${(H - H * pct).toFixed(1)}" width="${W}" height="${(H * pct).toFixed(1)}" fill="#fff" opacity=".13"/>
        <line x1="0" x2="${W}" y1="${(H - H * pct).toFixed(1)}" y2="${(H - H * pct).toFixed(1)}" stroke="#fff" stroke-opacity=".55" stroke-width="1.5" stroke-dasharray="3 5"/><rect width="${W}" height="${H}" fill="url(#dShade)"/></g>
      <path d="${archPath(W - 20, H - 12, SP - 10)}" transform="translate(10 12)" fill="none" stroke="#fff" stroke-opacity=".5" stroke-width="1.5"/></svg>`;
    const arc = per.map(([n, a, b], i) => {
      const past = M.t >= b, now = i === pi, k = now ? (M.t - a) / (b - a) : 0, c = D_COL[i];
      return `<button data-p="${i}" aria-pressed="${i === ui.sel}" aria-label="${n} من ${fmt(a)}"><svg viewBox="0 0 36 50" aria-hidden="true"><defs><clipPath id="dc${i}"><path d="${archPath(36, 50, 20)}"/></clipPath></defs>
        <path d="${archPath(36, 50, 20)}" fill="${past ? c : c + '1F'}"/>${now ? `<rect clip-path="url(#dc${i})" y="${(50 - 50 * k).toFixed(1)}" width="36" height="${(50 * k).toFixed(1)}" fill="${c}"/>` : ''}
        <path d="${archPath(34, 49, 19.5)}" transform="translate(1 .5)" fill="none" stroke="${c}" stroke-width="${now ? 2.4 : 1.4}"/></svg><b style="color:${c}">${n}</b><span>${fmt(a)}</span></button>`;
    }).join('');
    const [sn, sa, sb] = per[ui.sel];
    const rows = M.ext.filter((b) => b.start >= sa && b.start < sb).map((b) => {
      const now = b === cur, past = b.end <= M.t, done = M.done.has(b.key), c = D_COL[ui.sel];
      return `<div class="row${b.isPrayer ? ' pr' : ''}${now ? ' now' : ''}${past ? ' past' : ''}"><svg viewBox="0 0 16 22" aria-hidden="true"><path d="${archPath(16, 22, 9)}" fill="${b.isPrayer || now ? c : 'none'}" stroke="${c}" stroke-width="1.5"/></svg>
        <div><span class="t">${fmt(b.start)}</span><span class="n">${esc(b.title)}</span></div><span class="d" style="${now ? 'color:' + c : ''}">${done ? '✓ تم' : now ? 'الحين' : durShort(b.end - b.start)}</span></div>`;
    }).join('');
    root.innerHTML = `<svg class="frieze" viewBox="0 0 390 26" preserveAspectRatio="xMidYMid slice" aria-hidden="true"><defs><pattern id="dFr" width="26" height="26" patternUnits="userSpaceOnUse"><rect width="26" height="26" fill="#1F3F8F"/><g transform="translate(13 13)"><rect x="-6" y="-6" width="12" height="12" fill="#3FB2C1"/><rect x="-6" y="-6" width="12" height="12" fill="#3FB2C1" transform="rotate(45)"/><circle r="3" fill="#F4F5F7"/></g><circle cx="0" cy="0" r="2.4" fill="#E2B13C"/><circle cx="26" cy="0" r="2.4" fill="#E2B13C"/><circle cx="0" cy="26" r="2.4" fill="#E2B13C"/><circle cx="26" cy="26" r="2.4" fill="#E2B13C"/></pattern></defs><rect width="390" height="26" fill="url(#dFr)"/></svg>
      <div class="hd"><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div>
      <div class="arch">${big}<div class="in">
        <div class="nx">${asleep ? '' : nextP}</div>
        <span class="lbl">${asleep ? 'الليل' : 'فترة ' + per[pi][0] + ' · الحين'}</span>
        <div class="ti">${cur ? esc(cur.title) : ''}</div>
        <div class="rm">${cur ? (cur.sleep ? `تكعد ${fmt(cur.end)}، بعد ${durText(cur.end - M.t)}` : `باقي ${durText(cur.end - M.t)} · لحد ${fmt(cur.end)}`) : ''}</div>
        ${cur ? `<button class="go" id="dDone" style="color:${col}">${isDone ? '✓ تم، اضغط ترجعها' : doneLabel(cur)}</button>` : ''}
      </div></div>
      <nav class="arc" aria-label="فترات اليوم">${arc}</nav>
      <div class="list"><h4>فترة ${sn}<small>${fmt(sa)} ← ${fmt(sb)}</small></h4>${rows || '<div class="row"><span></span><span class="n">ماكو فقرات</span><span></span></div>'}</div>`;
    root.onclick = (e) => {
      const p = e.target.closest('[data-p]'); if (p) { ui.sel = +p.dataset.p; renderAll(true); return; }
      if (e.target.closest('#dDone') && cur) { markDone(M, cur.key); renderAll(true); }
    };
  },
};
