/* ================= E · شاشة القفل ================= */
C.E = {
  name: 'شاشة القفل',
  about: 'شلون يعيش التطبيق على تلفونك بلا ما تفتحه: فوق بالجزيرة عد تنازلي للصلاة الجاية. تحت الساعة أدوات صغيرة: الصلاة الجاية، وصلواتك اليوم، وساعة نومك. وجوه بطاقة مباشرة بيها كلمة الحبر تتعبى، وشغلتك الحين، وزر «خلّصت» تدوسه من شاشة القفل نفسها.',
  render(root, M) {
    const [s1, s2] = bSky(sunAt(CITY, M.doy, M.t).alt);
    root.classList.add('ios'); root.style.cssText = `--s1:${s1};--s2:${s2}`;
    const s = inkState(M), cur = M.cur, rem = s.tgt - M.t;
    const pD = M.ext.filter((b) => b.isPrayer && M.done.has(b.key)).length;
    const gauge = (f, col) => { const r = 28, c = 2 * Math.PI * r; return `<svg class="g" viewBox="0 0 64 64" aria-hidden="true"><circle cx="32" cy="32" r="${r}" fill="none" stroke="rgba(255,255,255,.22)" stroke-width="4"/><circle cx="32" cy="32" r="${r}" fill="none" stroke="${col}" stroke-width="4" stroke-linecap="round" stroke-dasharray="${(c * f).toFixed(1)} ${c.toFixed(1)}" transform="rotate(-90 32 32)"/></svg>`; };
    root.innerHTML = statusBar(M) + `<div class="di" aria-hidden="true"><span>${s.word.replace('السَّحَر', 'السحر')}</span><em>${rem >= 60 ? hm(rem) : rem + ' د'}</em></div>
      <div class="lk"><p>${M.day} ${M.dayNum} ${M.month}</p><b>${fmt(M.t)}</b></div>
      <div class="wg"><div>${gauge(s.p / 100, '#FF9F0A')}<span>${s.word.replace('السَّحَر', 'السحر')}<b>${fmt(s.tgt)}</b></span></div>
        <div>${gauge(pD / 5, '#FA2D5A')}<span>صلوات<b>${pD}/5</b></span></div>
        <div><span>${icon('bed', 18, 1.9)}<b>${fmt(M.plan.bedtime)}</b></span></div></div>
      <section class="la" aria-label="بطاقة مباشرة"><div class="app"><i>ي</i>يوم</div>
        <div class="top">${inkSpan(s)}<div><b>${rem >= 60 ? hm(rem) : rem + ' د'}</b><span>${fmt(s.tgt)}</span></div></div>
        <div class="bar"><i style="width:${s.p.toFixed(0)}%"></i></div>
        ${cur ? `<div class="row"><div><small>هسه</small><h4>${esc(cur.title)}</h4></div>${doneBtn(M, 'cap')}</div>` : ''}</section>
      <div class="bt"><span>${icon('sun', 22, 1.8)}</span><span>${icon('moon', 22, 1.8)}</span></div><div class="home"></div>`;
    bindDone(root, M);
  },
};
