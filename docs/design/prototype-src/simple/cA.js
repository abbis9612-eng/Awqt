/* ================= A · السطر ================= */
C.A = {
  name: 'السطر', sub: 'سطر واحد يبين يومك', swatch: '#0F6E63',
  info: {
    title: 'أ · السطر', from: 'شاشة بيضاء هادية. فوق: الصلاة الجاية بجملة وحدة. تحتها سطر رفيع بيه محطات اليوم الست، ونقطة تبين وين إنت. وبعدها شغلتك الحين وزر واحد.',
    why: ['أهم معلومة (الصلاة الجاية) أكبر شي بالشاشة', 'السطر يبين يومك كله بنظرة، مثل خط المترو', 'لون وحد بس (أخضر غامق) لكل شي مهم', 'الباقي مخفي لحد ما تريده'],
    try: 'جرّب الأوقات فوق، وشوف النقطة تمشي على السطر. واضغط «اليوم كله» تحت.',
  },
  render(root, M) {
    const ui = G.ui.A || (G.ui.A = {});
    const h = heroOf(M), tt = M.tt;
    const stops = [['الفجر', tt.fajr], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr], ['العصر', tt.asr], ['المغرب', tt.maghrib], ['العشاء', tt.isha], ['النوم', M.plan.bedtime]];
    const X = (i) => 358 - 358 / 12 - i * (358 / 6);
    let pos = null; // fractional stop index of "now"
    for (let i = 0; i < 5; i++) if (M.t >= stops[i][1] && M.t < stops[i + 1][1]) pos = i + (M.t - stops[i][1]) / (stops[i + 1][1] - stops[i][1]);
    if (M.t >= stops[5][1]) pos = 5; if (M.t < stops[0][1]) pos = 0;
    const px = X(0) - pos * (358 / 6);
    const nextI = stops.findIndex(([, m]) => m > M.t);
    let svg = `<line x1="${X(0)}" x2="${X(5)}" y1="10" y2="10" stroke="#E2E6EA" stroke-width="2"/><line x1="${X(0)}" x2="${px.toFixed(1)}" y1="10" y2="10" stroke="#16191F" stroke-width="2"/>`;
    stops.forEach(([, m], i) => { const past = m <= M.t; svg += `<circle cx="${X(i)}" cy="10" r="${i === nextI ? 6 : 4.5}" fill="${past ? '#16191F' : '#fff'}" stroke="${i === nextI ? '#0F6E63' : past ? '#16191F' : '#C5CAD2'}" stroke-width="2"/>`; });
    svg += `<circle cx="${px.toFixed(1)}" cy="10" r="9" fill="#0F6E63" opacity=".18"/><circle cx="${px.toFixed(1)}" cy="10" r="5" fill="#0F6E63"/>`;
    const lbls = stops.map(([n, m], i) => `<div class="${i === nextI ? 'nx' : m <= M.t ? 'ps' : ''}"><b>${n}</b>${fmt(m)}</div>`).join('');
    const cur = M.cur, pct = cur ? (M.t - cur.start) / (cur.end - cur.start) * 100 : 0;
    const list = ui.all ? M.ext.filter((b) => b !== cur) : upcoming(M, 3);
    root.innerHTML = `<div class="dt">${M.day} ${M.dayNum} ${M.month} · ${M.hijri}</div>
      <section class="hero">${h ? `<h2>${h.name} ${fmt(h.m)}</h2><p>بعد <b>${durText(h.m - M.t)}</b></p>` : ''}</section>
      <div class="stops" role="img" aria-label="محطات اليوم"><svg viewBox="0 0 358 20" aria-hidden="true">${svg}</svg><div class="lbls">${lbls}</div></div>
      ${cur ? `<section class="now"><small>الحين</small><h3>${esc(cur.title)}</h3><div class="rm">باقي ${durText(cur.end - M.t)}، لحد ${fmt(cur.end)}</div><div class="bar"><i style="width:${pct.toFixed(0)}%"></i></div>${doneBtn(M, 'go')}</section>` : ''}
      <h4>${ui.all ? 'اليوم كله' : 'بعدها'}</h4>
      ${list.map((b) => `<div class="row${b.isPrayer ? ' pr' : ''}${b.end <= M.t ? ' past' : ''}"><span>${fmt(b.start)}</span>${esc(b.title)}${M.done.has(b.key) ? '<span class="tag">✓</span>' : ''}</div>`).join('')}
      <button class="all" data-x="all">${ui.all ? 'أخفي' : 'اليوم كله'}</button>`;
    bindDone(root, M);
  },
};
