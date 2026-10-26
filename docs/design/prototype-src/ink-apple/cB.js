/* ================= B · سماء ================= */
const B_SKY = [[-90, '#070B1C', '#18213F'], [-12, '#0B1230', '#222C58'], [-5, '#272F66', '#6A4F86'], [0, '#3B4A8E', '#D9826A'], [6, '#3F70C2', '#E6B07E'], [16, '#3A7AD0', '#8EBDEE'], [90, '#2F70CF', '#79AEEB']];
function bSky(alt) { for (let i = 1; i < B_SKY.length; i++) if (alt <= B_SKY[i][0]) { const [a0, t0, h0] = B_SKY[i - 1], [a1, t1, h1] = B_SKY[i], k = (alt - a0) / (a1 - a0); return [mixHex(t0, t1, k), mixHex(h0, h1, k)]; } return [B_SKY[6][1], B_SKY[6][2]]; }
function mixHex(a, b, k) { const h = (x) => [1, 3, 5].map((i) => parseInt(x.slice(i, i + 2), 16)); const A = h(a), B = h(b); k = Math.max(0, Math.min(1, k)); return '#' + A.map((v, i) => pad(Math.round(v + (B[i] - v) * k).toString(16))).join(''); }
C.B = {
  name: 'سماء',
  about: 'على طريقة تطبيق الطقس بالآيفون: الخلفية لون السماء الحقيقي هسه، وبالنص كلمة «الحبر» البيضاء تتعبى ويّا الوقت. تحتها مربعات زجاج: شغلتك الحين وزرها، وأوقات الصلاة بسطر مثل ساعات الطقس، والجاي، ونومك الليلة.',
  render(root, M) {
    const [s1, s2] = bSky(sunAt(CITY, M.doy, M.t).alt);
    root.classList.add('ios'); root.style.cssText = `--s1:${s1};--s2:${s2}`;
    const s = inkState(M), cur = M.cur;
    const pct = cur ? (M.t - cur.start) / (cur.end - cur.start) * 100 : 0;
    const nk = M.next ? (M.next.key === 'fajr2' ? null : M.next.key) : null;
    const hr = M.PR.map(([k, n, m]) => `<div class="${k === nk ? 'nx' : m <= M.t ? 'ps' : ''}"><span>${k === nk ? 'الجاية' : n}</span>${icon(prayerIcon(k), 24, 1.7)}<b>${fmt(m)}</b></div>`).join('');
    const ls = upcoming(M, 3).map((b) => `<div><span>${fmt(b.start)}</span>${esc(b.title)}<em>${durShort(b.end - b.start)}</em></div>`).join('');
    const p = M.plan;
    root.innerHTML = statusBar(M) + `<header class="hd"><div class="city">${M.city}</div>${inkSpan(s, 'ink w')}
        <div class="t">${fmt(s.tgt)} · بعد ${durText(s.tgt - M.t)}</div><div class="d">${M.day} ${M.dayNum} ${M.month}</div></header>
      <div class="mods">
        ${cur ? `<section class="mod now"><div class="mh">${icon('check', 15, 2.2)}هسه</div><h3>${esc(cur.title)}</h3><p>باقي ${durText(cur.end - M.t)} · لحد ${fmt(cur.end)}</p>
          <div class="rng"><i style="width:${pct.toFixed(0)}%"></i><b style="inset-inline-start:${pct.toFixed(0)}%"></b></div>${doneBtn(M, 'cap')}</section>` : ''}
        <section class="mod"><div class="mh">${icon('sun', 15, 2.2)}أوقات الصلاة اليوم</div><div class="hr">${hr}</div></section>
        <section class="mod"><div class="mh">${icon('list', 15, 2.2)}بعدها</div><div class="ls">${ls}</div></section>
        <div class="tiles"><section class="mod"><div class="mh">${icon('bed', 15, 2.2)}النوم</div><div class="big">${fmt(p.bedtime)}</div><p>تكعد ${fmt(p.wake)}${p.qiyam ? ' للسَّحَر' : ''}</p></section>
          <section class="mod"><div class="mh">${icon('moon', 15, 2.2)}الليلة</div><div class="big">${hm(p.night)}</div><p>${p.shortfall ? 'ينقصك ' + durText(p.shortfall) : `مع قيلولة ${p.nap} د، يكفي`}</p></section></div>
      </div>`;
    bindDone(root, M);
  },
};
