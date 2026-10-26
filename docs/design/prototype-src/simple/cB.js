/* ================= B · البطاقة ================= */
C.B = {
  name: 'البطاقة', sub: 'شغلة وحدة بالمرة', swatch: '#3446A8',
  info: {
    title: 'ب · البطاقة', from: 'بطاقة بيضاء كبيرة وحدة: شغلتك الحين، وكم باقي إلها برقم كبير، وزر واحد. وراها بطاقات خفيفة تلمّح إن بعدها شغلات، وتحت تشوف شنو الجاي.',
    why: ['تركز على شي وحد بس، فما تتشتت', 'الوقت الباقي رقم كبير تقراه من بعيد', 'الصلاة الجاية دايماً فوق بزاوية صغيرة', 'أوقات الصلاة الخمسة تحت بسطر هادي'],
    try: 'جرّب «الظهر» و«المغرب»: البطاقة تصير صلاة، والزر يصير «صلّيت».',
  },
  render(root, M) {
    const h = heroOf(M), cur = M.cur;
    const rem = cur ? cur.end - M.t : 0, pct = cur ? (M.t - cur.start) / (cur.end - cur.start) * 100 : 0;
    const P = M.PR.filter(([k]) => k !== 'sunrise'), nk = M.next && M.next.key !== 'fajr2' ? M.next.key : null;
    root.innerHTML = `<div class="top"><span class="dt">${M.day} ${M.dayNum} ${M.month}</span>${h ? `<span class="pill"><i></i>${h.name} ${fmt(h.m)}</span>` : ''}</div>
      ${cur ? `<div class="stack"><div class="ghost g1"></div><div class="ghost g2"></div><article class="card">
        <small>${cur.isPrayer ? 'وقت الصلاة' : cur.sleep ? 'وقت النوم' : 'الحين'}</small><h2>${esc(cur.title)}</h2><div class="tm">${fmt(cur.start)} ← ${fmt(cur.end)}</div>
        <div class="big"><b>${rem >= 60 ? hm(rem) : rem}</b><span>${rem >= 60 ? 'ساعة باقية' : 'دقيقة باقية'}</span></div>
        <div class="bar"><i style="width:${pct.toFixed(0)}%"></i></div>${doneBtn(M, 'go')}</article></div>` : ''}
      <div class="after">${upcoming(M, 2).map((b) => `<div class="${b.isPrayer ? 'pr' : ''}"><span>${fmt(b.start)}</span>${esc(b.title)}</div>`).join('')}</div>
      <div class="times">${P.map(([k, n, m]) => `<div class="${k === nk ? 'nx' : ''}">${n}<b>${fmt(m)}</b></div>`).join('')}</div>`;
    bindDone(root, M);
  },
};
