/* ================= D · الصفحة ================= */
C.D = {
  name: 'الصفحة', sub: 'يومك مكتوب مثل كتاب', swatch: 'linear-gradient(135deg,#1A1B20 50%,#B4372F 50%)',
  info: {
    title: 'د · الصفحة', from: 'بلا رسومات أبداً: بس كتابة مرتبة. يومك صفحة، والصلوات عناوينها، وتحت كل صلاة فقراتها. اللي فات يسكر لحاله، واللي إنت بيه عليه نقطة حمرة. وشريط صغير تحت بيه الحين وزر واحد.',
    why: ['أبسط شي ممكن: تقرا يومك من فوق لجوه', 'الصلاة عنوان، فتعرف وين إنت من اليوم', 'اللي فات يختفي، فالصفحة دايماً قصيرة', 'نقطة حمرة وحدة للحين، وماكو ألوان غيرها'],
    try: 'اضغط على عنوان صلاة فاتت (مثل الفجر) حتى تفتحها. وجرّب «المساء».',
  },
  render(root, M) {
    const ui = G.ui.D || (G.ui.D = {});
    const tt = M.tt, cur = M.cur;
    const S = [['الفجر', tt.fajr, tt.sunrise], ['الصباح', tt.sunrise, tt.dhuhr], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr, tt.asr], ['العصر', tt.asr, tt.maghrib], ['المغرب', tt.maghrib, tt.isha], ['العشاء', tt.isha, M.plan.bedtime], ['الليل', M.plan.bedtime, M.nx.fajr + 1440]];
    const secs = S.map(([n, a, b], i) => {
      const past = M.t >= b, open = !past || ui['s' + i];
      const items = M.ext.filter((x) => x.start >= a && x.start < b).map((x) => {
        const c = x === cur, p = x.end <= M.t, ok = M.done.has(x.key);
        return `<div class="it${c ? ' cur' : ''}${p ? ' past' : ''}${ok ? ' ok' : ''}"><div>${esc(x.title)}${c ? `<small>باقي ${durText(x.end - M.t)}</small>` : ''}</div><span>${fmt(x.start)}</span></div>`;
      }).join('');
      return `<section class="sec${past ? ' past' : ''}"><button class="sh" data-x="s${i}" ${past ? '' : 'disabled style="cursor:default"'}><b>${n}</b><span>${fmt(a)}</span></button>${open ? items : ''}</section>`;
    }).join('');
    root.innerHTML = `<header class="hd"><h2>${M.day}</h2><p>${M.dayNum} ${M.month} · ${M.hijri}</p></header>${secs}<div class="end"></div>
      ${cur ? `<div class="bar"><div><small>الحين</small><b>${esc(cur.title)}</b></div>${doneBtn(M, 'go')}</div>` : ''}`;
    bindDone(root, M);
  },
};
