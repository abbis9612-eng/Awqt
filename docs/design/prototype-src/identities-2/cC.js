/* ================= C · المسبحة ================= */
const C_CURVE = (() => { // hanging thread: lowest at the centre, arc-length table for u >= 0
  const Y0 = 196, K = 0.0042, tab = [[0, 0]]; let L = 0;
  for (let u = 2; u <= 1600; u += 2) { const y1 = K * (u - 2) ** 2, y2 = K * u * u; L += Math.hypot(2, y2 - y1); tab.push([u, L]); }
  const at = (len) => { const a = Math.abs(len); let lo = 0, hi = tab.length - 1; while (hi - lo > 1) { const m = (lo + hi) >> 1; if (tab[m][1] < a) lo = m; else hi = m; } const u = tab[hi][0], s = Math.sign(len) || 1; return [195 + s * u, Y0 - K * u * u, Math.atan(-2 * K * u * s)]; };
  return { at, Y0, K };
})();
const beadR = (b) => b.isPrayer ? 19 : b.sleep ? 26 : Math.max(10, Math.min(24, 7 + 2.3 * Math.sqrt(b.end - b.start)));

C.C = {
  name: 'المسبحة', sub: 'كل فقرة حبة، تعدها بإبهامك', swatch: 'radial-gradient(circle at 35% 35%,#FFE3A6,#E39A2D 45%,#8C4F0E 80%)',
  info: {
    title: 'ج · المسبحة', from: 'من المسبحة اللي بإيد كل واحد منا: حبات كهرب، والشاهد الأسود، والشرّابة بالآخر.',
    idea: 'يومك خيط وحد، وكل فقرة حبة. الحبة الكبيرة يعني وقت أطول. الصلوات حبات سودة مثل الشاهد، والنوم حبة زرقة كبيرة، والشرّابة بالآخر. تسحب بإبهامك يمين ويسار وتعد الحبات وحدة وحدة، والتلفون يرجف رجفة خفيفة ويه كل حبة.',
    sig: 'المسبحة المتدلية: الحبة اللي بالنص هي اللي تشوف تفاصيلها، والحبات تصعد على الخيط من الجهتين.',
    why: ['شي وحد بالمرة، فما تتلخبط', 'حجم الحبة يكلك طول الفقرة بلا ما تقرا أرقام', 'الصلاة باينة دايماً (الحبة السودة)', 'شريط صغير جوه يبين المسبحة كلها، تضغط على أي حبة وتروحلها'],
    try: 'اسحب المسبحة يسار حتى تشوف الجاي، ويمين حتى ترجع. اضغط «خلّصت» وشوف الحبة تصير مكتملة.',
  },
  render(root, M) {
    const ui = G.ui.C || (G.ui.C = {});
    const B = M.ext, R = B.map(beadR), gap = 5;
    const cpos = []; B.forEach((b, i) => cpos.push(i ? cpos[i - 1] + R[i - 1] + R[i] + gap : 0));
    const curI = M.curI >= 0 ? M.curI : 0;
    if (ui.f == null || ui.f >= B.length || ui.key !== M.doy + ':' + G.preset) { ui.f = curI; ui.key = M.doy + ':' + G.preset; }
    let s = cpos[ui.f];
    const kindOf = (b) => b.isPrayer ? ['صلاة', '#17181C'] : b.sleep ? ['النوم', '#22408F'] : ['فقرة', '#E39A2D'];
    root.innerHTML = `
      <div class="hd"><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div>
      <div class="q">${M.next ? `الصلاة الجاية: <b>${M.next.name} ${fmt(M.next.m)}</b>` : ''}</div>
      <div class="stage" id="cStage" role="group" aria-label="مسبحة اليوم، اسحب يمين ويسار">
        <div class="ftime" id="cTime"></div>
        <svg id="cSvg" viewBox="0 0 390 270" aria-hidden="true"></svg>
        <div class="nav"><button id="cNext" aria-label="الحبة الجاية"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1B201D" stroke-width="2.2"><path d="M15 6l-6 6 6 6"/></svg></button><button id="cPrev" aria-label="الحبة اللي قبل"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1B201D" stroke-width="2.2"><path d="M9 6l6 6-6 6"/></svg></button></div>
      </div>
      <div class="card" id="cCard" aria-live="polite"></div>
      <div class="mini"><h4>المسبحة كلها</h4><svg id="cMini" viewBox="0 0 358 56"></svg></div>`;
    const svg = root.querySelector('#cSvg');
    const defs = `<defs>
      <radialGradient id="cAmb" cx=".38" cy=".32" r=".75"><stop offset="0" stop-color="#FFE7AE"/><stop offset=".35" stop-color="#F0AE45"/><stop offset=".8" stop-color="#C47317"/><stop offset="1" stop-color="#8C4F0E"/></radialGradient>
      <radialGradient id="cAmbD" cx=".38" cy=".32" r=".75"><stop offset="0" stop-color="#F6E3BD"/><stop offset=".5" stop-color="#D9B57A"/><stop offset="1" stop-color="#A8835A"/></radialGradient>
      <radialGradient id="cOnx" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#5A5C66"/><stop offset=".45" stop-color="#22232A"/><stop offset="1" stop-color="#0A0A0D"/></radialGradient>
      <radialGradient id="cLap" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#7E9BE6"/><stop offset=".45" stop-color="#2A4BA3"/><stop offset="1" stop-color="#132560"/></radialGradient>
      <radialGradient id="cGlow"><stop offset="0" stop-color="#FFD58A" stop-opacity=".55"/><stop offset="1" stop-color="#FFD58A" stop-opacity="0"/></radialGradient></defs>`;
    let lastNear = ui.f;
    const draw = () => {
      let o = defs + `<ellipse cx="195" cy="${C_CURVE.Y0 + 8}" rx="70" ry="40" fill="url(#cGlow)"/>`;
      const pts = []; for (let L = -700; L <= 700; L += 10) { const [x, y] = C_CURVE.at(L); if (x > -40 && x < 430) pts.push(`${x.toFixed(1)},${y.toFixed(1)}`); }
      o += `<polyline points="${pts.join(' ')}" fill="none" stroke="#6A4A2A" stroke-width="2"/>`;
      let near = 0, nd = 1e9;
      const items = B.map((b, i) => {
        const d = -(cpos[i] - s); // past to the right (RTL)
        if (Math.abs(d) < nd) { nd = Math.abs(d); near = i; }
        return [i, d];
      });
      // tassel after the last bead
      const lastD = -(cpos[B.length - 1] - s) - R[B.length - 1] - 6;
      const [tx, ty] = C_CURVE.at(lastD);
      if (tx > -60 && tx < 450) {
        o += `<circle cx="${tx.toFixed(1)}" cy="${ty.toFixed(1)}" r="7" fill="url(#cLap)"/>`;
        for (let k = -6; k <= 6; k++) o += `<path d="M${tx.toFixed(1)} ${(ty + 5).toFixed(1)} q ${(k * 1.2).toFixed(1)} 30 ${(k * 2.4).toFixed(1)} 64" stroke="${k % 2 ? '#22408F' : '#3A5FC0'}" stroke-width="1.6" fill="none" stroke-linecap="round"/>`;
        o += `<rect x="${(tx - 7).toFixed(1)}" y="${(ty + 14).toFixed(1)}" width="14" height="5" rx="2" fill="#C9A24A"/>`;
      }
      items.sort((a, b) => Math.abs(b[1]) - Math.abs(a[1])).forEach(([i, d]) => {
        const [x, y, ang] = C_CURVE.at(d); if (x < -40 || x > 430) return;
        const b = B[i], foc = Math.max(0, 1 - Math.abs(d) / 50), r = R[i] * (1 + 0.22 * foc);
        const past = b.end <= M.t, done = M.done.has(b.key);
        const fill = b.isPrayer ? 'url(#cOnx)' : b.sleep ? 'url(#cLap)' : past && !done ? 'url(#cAmbD)' : 'url(#cAmb)';
        const rx = b.isPrayer ? r * 1.22 : r, ry = b.isPrayer ? r * 0.86 : r;
        o += `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)}) rotate(${(ang * 180 / Math.PI).toFixed(1)})">
          <ellipse cx="2" cy="${(ry * 0.35 + 3).toFixed(1)}" rx="${(rx * 0.95).toFixed(1)}" ry="${(ry * 0.9).toFixed(1)}" fill="#000" opacity=".12"/>
          <ellipse rx="${rx.toFixed(1)}" ry="${ry.toFixed(1)}" fill="${fill}"/>
          ${b.isPrayer ? `<ellipse rx="${(rx * 0.55).toFixed(1)}" ry="${ry.toFixed(1)}" fill="none" stroke="#C9CED6" stroke-width="1.4" opacity=".85"/>` : ''}
          <ellipse cx="${(-rx * 0.32).toFixed(1)}" cy="${(-ry * 0.4).toFixed(1)}" rx="${(rx * 0.32).toFixed(1)}" ry="${(ry * 0.2).toFixed(1)}" fill="#fff" opacity=".55"/>
          ${done ? `<path d="M${(-r * .3).toFixed(1)} 0 l${(r * .22).toFixed(1)} ${(r * .22).toFixed(1)} l${(r * .42).toFixed(1)} ${(-r * .44).toFixed(1)}" stroke="#fff" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round" transform="rotate(${(-ang * 180 / Math.PI).toFixed(1)})"/>` : ''}
        </g>`;
        if (b.isPrayer && Math.abs(d) > 30) o += `<text x="${x.toFixed(1)}" y="${(y - ry - 9).toFixed(1)}" font-size="13" font-weight="600" fill="#1B201D" text-anchor="middle">${b.title.replace('صلاة ', '').split(' ')[0].replace('الجمعة:', 'الجمعة')}</text>`;
      });
      svg.innerHTML = o;
      const fb = B[near];
      root.querySelector('#cTime').innerHTML = `<b>${fb === M.cur ? 'الحين' : fb.end <= M.t ? 'فاتت' : 'بعد ' + durText(fb.start - M.t)}</b>`;
      if (near !== lastNear) { lastNear = near; buzz(); }
      return near;
    };
    const card = (i) => {
      const b = B[i], [kn, kc] = kindOf(b), now = b === M.cur, past = b.end <= M.t, done = M.done.has(b.key);
      const pct = now ? Math.round((M.t - b.start) / (b.end - b.start) * 100) : 0;
      const st = done ? '<span class="st ok">✓ خلّصتها</span>' : now ? `<span class="st now">الحين · باقي ${durText(b.end - M.t)}</span>` : past ? '<span class="st past">فاتت</span>' : `<span class="st fut">تبدي بعد ${durText(b.start - M.t)}</span>`;
      const canDo = now || past;
      root.querySelector('#cCard').innerHTML = `<div class="kind"><i style="background:${kc}"></i>${kn} · ${durText(b.end - b.start)}</div>
        <h3>${esc(b.title)}</h3><div class="tm">${fmt(b.start)} ← ${fmt(b.end)}</div>${st}
        ${now ? `<div class="bar"><i style="width:${pct}%"></i></div>` : ''}
        <div class="act">${canDo ? `<button class="pri" data-c="done">${done ? 'رجّعها' : doneLabel(b)}</button>` : ''}${i !== curI ? '<button class="gh" data-c="now">رجوع للحين</button>' : ''}</div>`;
    };
    const mini = () => {
      const n = B.length, w = 358 / n; let o = '';
      o += `<line x1="4" y1="22" x2="354" y2="22" stroke="#6A4A2A" stroke-width="1.2"/>`;
      B.forEach((b, i) => {
        const x = 358 - (i + 0.5) * w, r = Math.max(3, Math.min(8, R[i] / 3));
        const f = b.isPrayer ? '#17181C' : b.sleep ? '#22408F' : b.end <= M.t ? '#D2B488' : '#E39A2D';
        o += `<g class="hit" data-i="${i}"><rect x="${(x - w / 2).toFixed(1)}" y="0" width="${w.toFixed(1)}" height="56" fill="transparent"/>${i === ui.f ? `<circle cx="${x.toFixed(1)}" cy="22" r="${r + 5}" fill="none" stroke="#1B201D" stroke-width="1.6"/>` : ''}<circle cx="${x.toFixed(1)}" cy="22" r="${r}" fill="${f}"/>
          ${b.isPrayer ? `<text x="${Math.max(16, Math.min(342, x)).toFixed(1)}" y="50" font-size="11" fill="#56605A" text-anchor="middle">${fmt(b.start)}</text>` : ''}</g>`;
      });
      root.querySelector('#cMini').innerHTML = o;
    };
    const go = (i, anim = true) => {
      i = Math.max(0, Math.min(B.length - 1, i)); const from = s, to = cpos[i], t0 = performance.now(), dur = REDUCE || !anim ? 1 : 420;
      ui.f = i; card(i); mini();
      const step = (now) => { const k = Math.min(1, (now - t0) / dur), e = 1 - Math.pow(1 - k, 3); s = from + (to - from) * e; draw(); if (k < 1) requestAnimationFrame(step); };
      requestAnimationFrame(step);
    };
    draw(); card(ui.f); mini();
    const st = root.querySelector('#cStage');
    let x0 = null, s0 = 0, moved = false;
    st.onpointerdown = (e) => { if (e.target.closest('button')) return; x0 = e.clientX; s0 = s; moved = false; st.setPointerCapture(e.pointerId); };
    st.onpointermove = (e) => { if (x0 == null) return; const dx = e.clientX - x0; if (Math.abs(dx) > 4) moved = true; s = Math.max(-20, Math.min(cpos[B.length - 1] + 20, s0 + dx * 1.1)); const n = draw(); if (n !== ui.f) { ui.f = n; card(n); mini(); } };
    st.onpointerup = st.onpointercancel = () => { if (x0 == null) return; x0 = null; let best = 0; cpos.forEach((c, i) => { if (Math.abs(c - s) < Math.abs(cpos[best] - s)) best = i; }); go(best); };
    root.querySelector('#cNext').onclick = () => go(ui.f + 1);
    root.querySelector('#cPrev').onclick = () => go(ui.f - 1);
    root.querySelector('#cMini').onclick = (e) => { const g = e.target.closest('.hit'); if (g) go(+g.dataset.i); };
    root.querySelector('#cCard').onclick = (e) => {
      const b = e.target.closest('[data-c]'); if (!b) return;
      if (b.dataset.c === 'now') go(curI);
      if (b.dataset.c === 'done') { markDone(M, B[ui.f].key); M.done = new Set(store.get('done:' + M.doy, [])); card(ui.f); draw(); mini(); }
    };
    st.tabIndex = 0; st.onkeydown = (e) => { if (e.key === 'ArrowLeft') go(ui.f + 1); if (e.key === 'ArrowRight') go(ui.f - 1); };
  },
};
