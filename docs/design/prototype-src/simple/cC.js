/* ================= C · الحلقة ================= */
C.C = {
  name: 'الحلقة', sub: 'يومك دائرة وحدة', swatch: 'conic-gradient(#B7791F 0 40%,#EEEFF1 40%)',
  info: {
    title: 'ج · الحلقة', from: 'دائرة رفيعة هي يومك كله من الفجر للفجر، مثل ساعة اليد. الذهبي هو اللي مشى من اليوم، والنقاط الصغار هي الصلوات، والخط الأزرق جوه هو نومك. بالنص: الصلاة الجاية والعد التنازلي.',
    why: ['تشوف يومك كله وكم مشى منه بلمحة', 'بالنص جواب سؤال وحد: الصلاة الجاية بعد قديش', 'نومك باين على نفس الدائرة', 'تحتها شغلتك الحين وزر واحد'],
    try: 'جرّب «العصر» و«نايم» وشوف الذهبي يدور حول الحلقة.',
  },
  render(root, M) {
    const h = heroOf(M), cur = M.cur, f0 = M.tt.fajr;
    const R = 132, cx = 160, cy = 160;
    const ang = (m) => ((m - f0) / 1440) * 2 * Math.PI; // clockwise from the top, starting at fajr
    const P = (m, r = R) => [cx + r * Math.sin(ang(m)), cy - r * Math.cos(ang(m))];
    const arc = (a, b, r) => { const [x1, y1] = P(a, r), [x2, y2] = P(b, r); const big = b - a > 720 ? 1 : 0; return `M${x1.toFixed(1)} ${y1.toFixed(1)} A${r} ${r} 0 ${big} 1 ${x2.toFixed(1)} ${y2.toFixed(1)}`; };
    const t = Math.min(M.t, f0 + 1439);
    let o = `<circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="#EEEFF1" stroke-width="10"/>`;
    if (t > f0) o += `<path d="${arc(f0, t, R)}" fill="none" stroke="#B7791F" stroke-width="10" stroke-linecap="round"/>`;
    o += `<path d="${arc(M.plan.bedtime, M.plan.wake, R - 18)}" fill="none" stroke="#9AA9D8" stroke-width="3" stroke-linecap="round"/>`;
    const [sx, sy] = P((M.plan.bedtime + M.plan.wake) / 2, R - 32); o += `<text x="${sx.toFixed(1)}" y="${(sy + 4).toFixed(1)}" font-size="12" fill="#7484B8" text-anchor="middle">نوم</text>`;
    M.PR.filter(([k]) => k !== 'sunrise').forEach(([k, n, m]) => {
      const [x, y] = P(m), [lx, ly] = P(m, R + 24), past = m <= M.t, nx = M.next && M.next.key === k;
      o += `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${nx ? 7 : 5}" fill="${past ? '#fff' : '#fff'}" stroke="${nx ? '#B7791F' : past ? '#fff' : '#B9BDC4'}" stroke-width="2.4"/>`;
      o += `<text x="${lx.toFixed(1)}" y="${(ly + 4).toFixed(1)}" font-size="12.5" font-weight="${nx ? 600 : 400}" fill="${nx ? '#8A5A12' : '#80858E'}" text-anchor="middle">${n}</text>`;
    });
    const [nxp, nyp] = P(t); o += `<circle cx="${nxp.toFixed(1)}" cy="${nyp.toFixed(1)}" r="9" fill="#fff" stroke="#B7791F" stroke-width="3"/>`;
    const rem = h ? h.m - M.t : 0;
    const nb = upcoming(M, 1)[0];
    root.innerHTML = `<div class="dt">${M.day} ${M.dayNum} ${M.month} · ${M.hijri}</div>
      <div class="ring"><svg viewBox="0 0 320 320" role="img" aria-label="حلقة اليوم من الفجر للفجر">${o}</svg>
        ${h ? `<div class="mid"><small>${h.name === 'السَّحَر' || h.name === 'الكعدة' ? 'تكعد' : 'الصلاة الجاية'}</small><b>${h.name} ${fmt(h.m)}</b><div class="cd">${rem >= 60 ? hm(rem) : rem}<span>${rem >= 60 ? 'ساعة' : 'دقيقة'}</span></div></div>` : ''}</div>
      ${cur ? `<section class="now"><small>الحين</small><h3>${esc(cur.title)}</h3><div class="rm">باقي ${durText(cur.end - M.t)}</div>${doneBtn(M, 'go')}</section>` : ''}
      ${nb ? `<div class="nx">بعدها: <b>${esc(nb.title)}</b> · ${fmt(nb.start)}</div>` : ''}`;
    bindDone(root, M);
  },
};
