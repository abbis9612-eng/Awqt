/* ================= C · بغداد ================= */
// flat poster palettes, picked by where the sun is: [sky, ground, sun, text on sky, text on ground]
const C_PAL = { night: ['#141A3C', '#26346F', '#EDE4CF', '#EDE4CF', '#EDE4CF', '#141A3C'], dawn: ['#2B3A82', '#141B4A', '#F2A39A', '#F6EEDF', '#F6EEDF', '#141B4A'],
  morning: ['#F2C641', '#1E4FA3', '#E2462B', '#10183A', '#F6EEDF', '#10183A'], noon: ['#F5D45A', '#1A4396', '#D93A22', '#10183A', '#F6EEDF', '#10183A'],
  asr: ['#E8892F', '#1F3570', '#FBE3A4', '#10183A', '#F6EEDF', '#10183A'], dusk: ['#C7352A', '#191E3D', '#F7C35C', '#FFF3E0', '#FFF3E0', '#191E3D'] };
C.C = {
  name: 'بغداد',
  about: 'بوستر بغداد السبعينات: ألوان مسطحة جريئة، وخط كوفي هندسي ثقيل، والشمس دائرة تصعد وتنزل حسب مكانها الحقيقي، وتختفي ورا الأرض وقت المغرب. الألوان تتبدل ويّا الوقت: أصفر وأزرق بالظهر، برتقالي بالعصر، أحمر بالمغرب، كحلي بالليل. وجوه شريط بلون الشمس بيه شغلتك الحين.',
  render(root, M) {
    const sun = sunAt(CITY, M.doy, M.t), alt = sun.alt;
    const k = alt < -12 ? 'night' : alt < 1 ? (M.t % 1440 < 720 ? 'dawn' : 'dusk') : alt < 22 ? (M.t % 1440 < 720 ? 'morning' : 'asr') : 'noon';
    const [sky, ground, sc, tx, gtx, btx] = C_PAL[k];
    root.style.cssText = `--sky:${sky};--ground:${ground};--sun:${sc};--tx:${tx};--gtx:${gtx};--btx:${btx}`;
    // the disc rides on the real altitude and slides with azimuth; below the line it hides behind the ground
    const HY = 372, R = 92, isMoon = k === 'night';
    const y = isMoon ? 190 : HY - 30 - Math.max(-60, alt) * 5.4, x = isMoon ? 120 : 195 - Math.max(-1, Math.min(1, (sun.az - 180) / 90)) * 110;
    let disc = `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${R}" fill="${sc}"/>`;
    if (isMoon) disc += `<circle cx="${(x + 40).toFixed(1)}" cy="${(y - 18).toFixed(1)}" r="${R - 4}" fill="${sky}"/>`;
    const lines = Array.from({ length: 6 }, (_, i) => `<rect x="0" y="${HY - 64 + i * 11}" width="390" height="${2 + i * 0.6}" fill="${sky}"/>`).join('');
    const h = heroOf(M), cur = M.cur;
    const P = M.PR.filter(([q]) => q !== 'sunrise'), nk = M.next && M.next.key !== 'fajr2' ? M.next.key : null;
    root.innerHTML = `<div class="sky"><svg viewBox="0 0 390 372" preserveAspectRatio="none" aria-hidden="true">${disc}${y + R > HY - 70 && !isMoon ? lines : ''}</svg>
        <div class="top"><div>${M.day} ${M.dayNum} ${M.month}<span>${M.hijri}</span></div><div class="clk">${fmt(M.t)}</div></div></div>
      ${h ? `<section class="main"><div class="w">${h.name}</div><div class="row"><span class="t">${fmt(h.m)}</span><span class="in">بعد<br>${durText(h.m - M.t)}</span></div></section>` : ''}
      <div class="five">${P.map(([q, n, m]) => `<div class="${q === nk ? 'nx' : m <= M.t ? 'ps' : ''}">${n}<b>${fmt(m)}</b></div>`).join('')}</div>
      ${cur ? `<section class="task"><div><small>هسه</small><h3>${esc(cur.title)}</h3></div>${doneBtn(M, 'sq')}</section>` : ''}`;
    bindDone(root, M);
  },
};
