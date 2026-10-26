/* ================= A · الحبر ================= */
C.A = {
  name: 'الحبر',
  about: 'اسم الصلاة الجاية مكتوب بخط الرقعة كبير، والحبر يعبّي حروفه من اليمين على كد ما يقرب وقتها. لما تكمل الكلمة يكون وقتها. تحتها شغلتك الحين، وختم أسود تدوسه لما تخلّص. وجوه الصلوات الخمس، اللي فاتت مكتوبة بالحبر كاملة.',
  render(root, M) {
    const P = M.PR.filter(([k]) => k !== 'sunrise');
    const h = heroOf(M);
    // ink fills from the previous prayer to the next one
    const all = [['isha0', 'العشاء', M.tt.isha - 1440 + 0], ...P, ['fajr2', 'الفجر', M.nx.fajr + 1440]];
    let i = all.findIndex(([, , m]) => m > M.t); if (i < 1) i = 1;
    const prev = all[i - 1][2], nxt = all[i];
    const isWake = M.cur && M.cur.sleep;
    const word = isWake ? (M.plan.qiyam ? 'السَّحَر' : 'الكعدة') : nxt[1];
    const tgt = isWake ? M.plan.wake : nxt[2], from = isWake ? M.plan.bedtime : prev;
    const p = Math.max(0, Math.min(1, (M.t - from) / (tgt - from))) * 100;
    const fs = word.length > 5 ? 116 : 138;
    const cur = M.cur, nb = upcoming(M, 2);
    const five = P.map(([k, n, m]) => { const fill = m <= M.t ? 100 : (k === nxt[0] && !isWake) ? p : 0; return `<div><b style="--p:${fill.toFixed(1)}%">${n}</b><span>${fmt(m)}</span></div>`; }).join('');
    root.innerHTML = `<div class="top"><div><b>${M.day} ${M.dayNum} ${M.month}</b><span>${M.hijri}</span></div><div class="clk">${fmt(M.t)}</div></div>
      <div class="word" style="--p:${p.toFixed(1)}%;--fs:${fs}px" role="img" aria-label="${word} بعد ${durText(tgt - M.t)}">${word}</div>
      <p class="when">بعد ${durText(tgt - M.t)}، الساعة <em>${fmt(tgt)}</em></p>
      ${cur ? `<section class="now"><div><small>هسه</small><h3>${esc(cur.title)}</h3><small>لحد ${fmt(cur.end)}</small></div>${doneBtn(M, 'seal')}</section>` : ''}
      <div class="after">${nb.map((b) => `<div><em>${fmt(b.start)}</em>${esc(b.title)}</div>`).join('')}</div>
      <div class="five">${five}</div>`;
    bindDone(root, M);
  },
};
