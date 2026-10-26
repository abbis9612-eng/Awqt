/* ================= A · الحبر والقائمة ================= */
C.A = {
  name: 'الحبر والقائمة',
  about: 'فوق: كلمة الحبر للصلاة الجاية، تتعبى ويّا الوقت، وتحتها شغلتك الحين وختم «خلّصت». تحت: جدولك قائمة مثل التذكيرات، وعنوان كل قسم اسم الصلاة بالرقعة يتعبى حبر على كد ما يمشي وقته، واللي تخلّصه تأشّر عليه ويتشطب. انزل بالقائمة وشوف الكلمة الكبيرة تصغر وتثبت فوق. وبعد العشاء الشاشة تصير داكنة لحالها.',
  render(root, M) {
    const ui = G.ui.A || (G.ui.A = {});
    root.classList.add('ios');
    const dark = M.t >= M.tt.isha || M.t < M.tt.fajr;
    root.classList.toggle('dark', dark);
    const s = inkState(M), tt = M.tt, cur = M.cur;
    const S = [['الفجر', tt.fajr, tt.sunrise, 'fajr'], ['الضحى', tt.sunrise, tt.dhuhr, 'tea'], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr, tt.asr, M.d.friday ? 'mosque' : 'dhuhr'], ['العصر', tt.asr, tt.maghrib, 'asr'], ['المغرب', tt.maghrib, tt.isha, 'maghrib'], ['العشاء', tt.isha, M.plan.bedtime, 'isha'], ['الليل', M.plan.bedtime, M.nx.fajr + 1440, 'lantern']];
    const B = M.ext, prayers = B.filter((b) => b.isPrayer);
    const pD = prayers.filter((b) => M.done.has(b.key)).length, nD = B.filter((b) => M.done.has(b.key)).length;
    const nLeft = B.filter((b) => b.start > M.t).length;
    const pastN = S.filter(([, , b]) => M.t >= b).length;
    const row = (b) => {
      const now = b === cur, d = M.done.has(b.key);
      const sub = now ? `هسه · باقي ${durText(b.end - M.t)}` : `${fmt(b.start)} · ${durShort(b.end - b.start)}`;
      return `<button class="row${b.isPrayer ? ' pr' : ''}${now ? ' now' : ''}${d ? ' done' : ''}" data-k="${b.key}" aria-pressed="${d}"><span class="ti">${yi(iconOf(b), 24)}</span><span class="tx"><b>${esc(b.title)}</b><span>${sub}</span></span><span class="ck${d ? ' on' : ''}">${d ? icon('check', 15, 3) : ''}</span></button>`;
    };
    const secs = S.map(([n, a, b, ic]) => {
      if (M.t >= b && !ui.past) return '';
      const rows = B.filter((x) => x.start >= a && x.start < b); if (!rows.length) return '';
      const p = Math.max(0, Math.min(1, (M.t - a) / (b - a))) * 100;
      return `<section class="sec"><div class="sh"><span class="ink" style="--p:${p.toFixed(1)}%">${n}</span><span class="tm">${yi(ic, 20)}${fmt(a)}</span></div>${rows.map(row).join('')}</section>`;
    }).join('');
    const fs = s.word.length > 5 ? 104 : 124;
    const pastW = pastN === 1 ? 'فترة وحدة فاتت' : pastN === 2 ? 'فترتين فاتن' : `${pastN} فترات فاتت`;
    root.innerHTML = `<div class="nb">${statusBar(M)}<div class="mini" aria-hidden="true">${inkSpan(s)}<span><b>${fmt(s.tgt)}</b> · بعد ${durShort(s.tgt - M.t)}</span></div></div>
      <header class="hero" id="mHero"><div class="dt">${M.day} ${M.dayNum} ${M.month} · ${M.hijri}</div>
        <span class="ink w" style="--p:${s.p.toFixed(1)}%;--fs:${fs}px" role="img" aria-label="${s.word} بعد ${durText(s.tgt - M.t)}">${s.word}</span>
        <p>بعد ${durText(s.tgt - M.t)}، الساعة <em>${fmt(s.tgt)}</em></p></header>
      ${cur ? `<section class="nowc"><span class="ti big">${yi(iconOf(cur), 30)}</span><div><small>هسه</small><h3>${esc(cur.title)}</h3><small>لحد ${fmt(cur.end)}</small></div>${doneBtn(M, 'seal')}</section>` : ''}
      <div class="cnt"><div>${yi('mosque', 22)}<b>${pD}/5</b><span>الصلوات</span></div><div>${yi('seal', 22)}<b>${nD}</b><span>خلّصت</span></div><div>${yi('hourglass', 22)}<b>${nLeft}</b><span>باقي اليوم</span></div></div>
      ${pastN ? `<button class="past" data-x="past">${ui.past ? 'أخفي اللي فات' : pastW + '، اضغط تشوفها'}</button>` : ''}
      ${secs}<div class="end"></div>${tabBar('today')}`;
    const hero = root.querySelector('#mHero');
    const onScroll = () => { ui.st = root.scrollTop; root.classList.toggle('collapsed', root.scrollTop > hero.offsetTop + hero.offsetHeight - 70); };
    root.onscroll = onScroll;
    if (ui.st) root.scrollTop = ui.st; onScroll();
    root.onclick = (e) => {
      if (e.target.closest('[data-done]') && cur) { markDone(M, cur.key); renderAll(true); return; }
      const r = e.target.closest('[data-k]'); if (r) { markDone(M, r.dataset.k); renderAll(true); return; }
      if (e.target.closest('[data-x]')) { ui.past = !ui.past; renderAll(true); }
    };
  },
};
