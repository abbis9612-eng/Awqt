/* ================= D · حلقات ================= */
C.D = {
  name: 'حلقات',
  about: 'على طريقة حلقات اللياقة بالآيفون: ثلاث حلقات تكمل لما تخلّص يومك. الحمرة صلواتك الخمس، والخضرة ساعات التركيز، والزرقة وقت الأهل. تحتها الصلاة الجاية بكلمة الحبر وعد تنازلي، وشغلتك الحين، والصلوات تأشّر عليها وحدة وحدة وتشوف الحلقة تكمل.',
  render(root, M) {
    root.classList.add('ios');
    const B = M.ext, D = (b) => M.done.has(b.key);
    const prayers = B.filter((b) => b.isPrayer), pD = prayers.filter(D).length;
    const sum = (f) => B.filter(f).reduce((a, b) => a + (b.end - b.start), 0);
    const deepAll = sum((b) => b.deep), deepD = sum((b) => b.deep && D(b));
    const fam = (b) => b.role === 'SPOUSE' || b.role === 'CHILD', famAll = sum(fam), famD = sum((b) => fam(b) && D(b));
    const fr = [pD / Math.max(1, prayers.length), deepD / Math.max(1, deepAll), famD / Math.max(1, famAll)];
    const cols = ['#FA2D5A', '#9BE22D', '#1EDCEB'], R = [64, 46, 28];
    let o = '';
    fr.forEach((f, i) => {
      const r = R[i], c = 2 * Math.PI * r;
      o += `<circle cx="75" cy="75" r="${r}" fill="none" stroke="${cols[i]}" stroke-opacity=".22" stroke-width="15"/>`;
      if (f > 0) o += `<circle cx="75" cy="75" r="${r}" fill="none" stroke="${cols[i]}" stroke-width="15" stroke-linecap="round" stroke-dasharray="${(c * Math.min(1, f)).toFixed(1)} ${c.toFixed(1)}" transform="rotate(-90 75 75)"/>`;
    });
    const s = inkState(M), cur = M.cur;
    const rows = M.PR.filter(([k]) => k !== 'sunrise').map(([k, n, m]) => { const b = prayers.find((x) => x.start === m) || {}; const d = b.key && D(b); return `<button class="pr" ${b.key ? `data-k="${b.key}"` : ''} aria-pressed="${!!d}"><span class="tx">${n}<span>${fmt(m)}</span></span><span class="ck${d ? ' on' : ''}">${d ? icon('check', 15, 3) : ''}</span></button>`; }).join('');
    root.innerHTML = statusBar(M) + `<header class="lt"><p>${M.day} ${M.dayNum} ${M.month}</p><h2>الملخص</h2></header>
      <section class="card"><h3>حلقات اليوم</h3><div class="rings"><svg viewBox="0 0 150 150" role="img" aria-label="الصلوات ${pD} من 5">${o}</svg>
        <div class="st"><div><span style="color:#FA2D5A">الصلوات</span><b style="color:#FA2D5A">${pD}<small>/5</small></b></div>
          <div><span style="color:#9BE22D">التركيز</span><b style="color:#9BE22D">${hm(deepD)}<small>/${hm(deepAll)} س</small></b></div>
          <div><span style="color:#1EDCEB">الأهل</span><b style="color:#1EDCEB">${hm(famD)}<small>/${hm(famAll)} س</small></b></div></div></div></section>
      <section class="card"><div class="nx">${inkSpan(s)}<div><b>${fmt(s.tgt)}</b><span>بعد ${durText(s.tgt - M.t)}</span></div></div></section>
      ${cur ? `<section class="card now"><small>هسه</small><h4>${esc(cur.title)}</h4><p>باقي ${durText(cur.end - M.t)} · لحد ${fmt(cur.end)}</p>${doneBtn(M, 'cap')}</section>` : ''}
      <section class="card"><h3>الصلوات</h3>${rows}</section><div class="gap"></div>${tabBar('today', true)}`;
    root.onclick = (e) => {
      if (e.target.closest('[data-done]') && cur) { markDone(M, cur.key); renderAll(true); return; }
      const r = e.target.closest('[data-k]'); if (r) { markDone(M, r.dataset.k); renderAll(true); }
    };
  },
};
