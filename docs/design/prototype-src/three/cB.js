/* ================= B · الشناشيل ================= */
const B_WALL = { day: '#D9D4CB', dusk: '#C7B8AE', night: '#1E2432' };
C.B = {
  name: 'الشناشيل',
  about: 'شمس السليمانية الحقيقية تدخل من شبّاك شناشيل وتطيح على الحايط. الضوي يتحرك ويطول ويتلون ويّا الشمس: أصفر بالصبح، أبيض بالظهر، برتقالي وقت العصر والمغرب، وبالليل ضوي قمر خفيف. فوق: الصلاة الجاية بس. وجوه رف الخشب بيه شغلتك الحين وزر واحد.',
  render(root, M) {
    const sun = sunAt(CITY, M.doy, M.t), alt = sun.alt, az = sun.az;
    const wall = alt > 6 ? B_WALL.day : alt > -6 ? mixC(B_WALL.dusk, B_WALL.day, (alt + 6) / 12) : alt > -12 ? mixC(B_WALL.night, B_WALL.dusk, (alt + 12) / 6) : B_WALL.night;
    const night = alt < -7;
    root.classList.toggle('night', night); root.style.setProperty('--wall', wall);
    // where the window's light lands: low sun throws a long, high, slanted patch; high sun a short, low one
    const day = alt > -1;
    const a = Math.max(2, alt), sk = Math.max(-38, Math.min(38, (az - 180) * 0.42));
    const top = day ? 70 + a * 3.2 : 150, ph = day ? Math.max(170, 380 - a * 4.2) : 300, pw = 236;
    const cx = day ? 195 - Math.sin((az - 180) * Math.PI / 180) * 60 : 230;
    const col = !day ? '#B9C6F0' : alt < 6 ? '#FF9F57' : alt < 18 ? '#FFD08F' : '#FFF7E8';
    const op = !day ? 0.16 : alt < 2 ? 0.45 + alt * 0.1 : 0.95;
    const blur = !day ? 2.4 : alt < 15 ? 2.2 : 1.1;
    const panes = [];
    for (let r = 0; r < 3; r++) for (let c = 0; c < 2; c++) panes.push(`<rect x="${c * (pw / 2) + 6}" y="${r * (ph / 3) + 6}" width="${pw / 2 - 12}" height="${ph / 3 - 12}"/>`);
    const svg = `<svg class="light" viewBox="0 0 390 780" preserveAspectRatio="xMidYMin slice" aria-hidden="true"><defs>
        <pattern id="bLat" width="26" height="26" patternUnits="userSpaceOnUse"><rect x="8" y="8" width="10" height="10" transform="rotate(45 13 13)" fill="#fff"/><rect x="0" y="0" width="4" height="4" fill="#fff"/><rect x="22" y="0" width="4" height="4" fill="#fff"/><rect x="0" y="22" width="4" height="4" fill="#fff"/><rect x="22" y="22" width="4" height="4" fill="#fff"/><rect x="11.5" y="0" width="3" height="5" fill="#fff"/><rect x="11.5" y="21" width="3" height="5" fill="#fff"/><rect x="0" y="11.5" width="5" height="3" fill="#fff"/><rect x="21" y="11.5" width="5" height="3" fill="#fff"/></pattern>
        <mask id="bMask"><g fill="url(#bLat)">${panes.join('')}</g></mask>
        <filter id="bSoft" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="${blur}"/></filter>
        <filter id="bGlow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="28"/></filter>
        <filter id="bGrain"><feTurbulence type="fractalNoise" baseFrequency=".9" numOctaves="2" seed="4"/><feColorMatrix values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 .07 0"/></filter></defs>
      <rect width="390" height="780" filter="url(#bGrain)"/>
      <g transform="translate(${(cx - pw / 2).toFixed(1)} ${top.toFixed(1)}) skewX(${sk.toFixed(1)})">
        <rect x="-10" y="-10" width="${pw + 20}" height="${ph + 20}" fill="${col}" opacity="${(op * 0.35).toFixed(2)}" filter="url(#bGlow)"/>
        <g filter="url(#bSoft)"><rect width="${pw}" height="${ph}" fill="${col}" opacity="${op.toFixed(2)}" mask="url(#bMask)"/></g>
      </g></svg>`;
    const h = heroOf(M), cur = M.cur, nb = upcoming(M, 1)[0];
    const P = M.PR.filter(([k]) => k !== 'sunrise'), nk = M.next && M.next.key !== 'fajr2' ? M.next.key : null;
    root.innerHTML = svg + `<div class="ui">
      <div class="top"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div><div class="clk">${fmt(M.t)}</div></div>
      ${h ? `<section class="hero"><h2>${h.name}</h2><p>بعد ${durText(h.m - M.t)}، الساعة <em>${fmt(h.m)}</em></p></section>` : ''}
      <div class="five">${P.map(([k, n, m]) => `<div class="${k === nk ? 'nx' : ''}">${n}<b>${fmt(m)}</b></div>`).join('')}</div>
      ${cur ? `<section class="sill"><div class="row"><div><small>هسه</small><h3>${esc(cur.title)}</h3></div>${doneBtn(M, 'knob')}</div>${nb ? `<div class="nb">بعدها ${esc(nb.title)}، <em>${fmt(nb.start)}</em></div>` : ''}</section>` : ''}
    </div>`;
    bindDone(root, M);
  },
};
function mixC(a, b, k) { const h = (x) => [1, 3, 5].map((i) => parseInt(x.slice(i, i + 2), 16)); const A = h(a), B = h(b); k = Math.max(0, Math.min(1, k)); return '#' + A.map((v, i) => pad(Math.round(v + (B[i] - v) * k).toString(16))).join(''); }
