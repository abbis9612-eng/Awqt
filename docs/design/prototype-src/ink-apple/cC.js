/* ================= C · قائمة ================= */
const C_SEC = ['#FF9500', '#007AFF', '#34C759', '#FF6B3D', '#AF52DE', '#5856D6', '#30475E'];
C.C = {
  name: 'قائمة',
  about: 'على طريقة تطبيق التذكيرات: عنوان كبير «اليوم»، وتحته كلمة الحبر للصلاة الجاية، وأربع مربعات سريعة (الصلوات، اللي خلّصته، الباقي، نومك). وبعدها يومك قوائم مرتبة حسب الصلوات، وكل فقرة إلها دائرة تأشّر عليها لما تخلّصها.',
  render(root, M) {
    const ui = G.ui.C || (G.ui.C = {});
    root.classList.add('ios');
    const s = inkState(M), tt = M.tt;
    const S = [['الفجر', tt.fajr, tt.sunrise], ['الصبح', tt.sunrise, tt.dhuhr], [M.d.friday ? 'الجمعة' : 'الظهر', tt.dhuhr, tt.asr], ['العصر', tt.asr, tt.maghrib], ['المغرب', tt.maghrib, tt.isha], ['العشاء', tt.isha, M.plan.bedtime], ['الليل', M.plan.bedtime, M.nx.fajr + 1440]];
    const blocks = M.ext, prayers = blocks.filter((b) => b.isPrayer);
    const nDone = blocks.filter((b) => M.done.has(b.key)).length, nLeft = blocks.filter((b) => b.start > M.t && !M.done.has(b.key)).length;
    const pDone = prayers.filter((b) => M.done.has(b.key)).length;
    const pastSecs = S.filter(([, , b]) => M.t >= b).length;
    const row = (b) => {
      const now = b === M.cur, d = M.done.has(b.key);
      const sub = now ? `هسه · باقي ${durText(b.end - M.t)}` : `${fmt(b.start)} · ${durShort(b.end - b.start)}`;
      return `<button class="row${b.isPrayer ? ' pr' : ''}${now ? ' now' : ''}${d ? ' done' : ''}" data-k="${b.key}" aria-pressed="${d}"><span class="ck${d ? ' on' : ''}">${d ? icon('check', 15, 3) : ''}</span><span class="tx"><b>${esc(b.title)}</b><span>${sub}</span></span></button>`;
    };
    const secs = S.map(([n, a, b], i) => {
      if (M.t >= b && !ui.past) return '';
      const rows = blocks.filter((x) => x.start >= a && x.start < b);
      return rows.length ? `<section class="sec" style="--sc:${C_SEC[i]}"><h3>${n}<small>${fmt(a)}</small></h3><div class="grp">${rows.map(row).join('')}</div></section>` : '';
    }).join('');
    root.innerHTML = statusBar(M) + `<header class="lt"><h2>اليوم</h2><p>${M.day} ${M.dayNum} ${M.month} · ${M.hijri}</p></header>
      <div class="la">${inkSpan(s)}<div><b>${fmt(s.tgt)}</b><span>بعد ${durText(s.tgt - M.t)}</span></div></div>
      <div class="smart">
        <div><i style="background:#34C759">${icon('sun', 18, 2.2)}</i><b>${pDone}/5</b><span>الصلوات</span></div>
        <div><i style="background:#5856D6">${icon('check', 18, 2.6)}</i><b>${nDone}</b><span>خلّصت</span></div>
        <div><i style="background:#FF9500">${icon('list', 18, 2.2)}</i><b>${nLeft}</b><span>باقي اليوم</span></div>
        <div><i style="background:#30475E">${icon('bed', 18, 2)}</i><b>${fmt(M.plan.bedtime)}</b><span>نومك</span></div>
      </div>
      ${pastSecs ? `<button class="past" data-x="past">${ui.past ? 'أخفي اللي فات' : `عرض اللي فات (${pastSecs} ${pastSecs > 2 ? 'فترات' : pastSecs === 2 ? 'فترتين' : 'فترة'})`}</button>` : ''}
      ${secs}<div class="gap"></div>${tabBar('today')}`;
    root.onclick = (e) => {
      const r = e.target.closest('[data-k]'); if (r) { markDone(M, r.dataset.k); renderAll(true); return; }
      const x = e.target.closest('[data-x]'); if (x) { ui.past = !ui.past; renderAll(true); }
    };
  },
};
