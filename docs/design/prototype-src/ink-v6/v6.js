/* ================= «يوم» الحبر، النسخة السادسة: كلمة تنقرا من أول نظرة =================
   Owner (2026-10-29): the big word («تركيز») is hard to read, make it and the other display words
   clear, and make the motion better and stronger.
   - The unfilled part of the word is no longer a pale ghost: every letter has a clear outline, so
     the whole word reads at once; the ink then fills it from the right with a soft wet edge.
   - The display font can be tried three ways: Noto Kufi Arabic (clear and bold, our pick; Reem Kufi was tried and dropped because its medial خ loses its dot), Noto Naskh
     bold (already in the app) and Aref Ruqaa (the current one). One choice changes every display
     word: the big word, the day name, «يومك», the period names and page titles.
   - Motion: the word soaks into the paper (from blurred to sharp) and the ink pours to now; a
     slow shine passes over the wet ink every few seconds; the word fits its width by itself. */

G.font = store.get('v6font', 'kufi');
const FONTS = [['kufi', 'كوفي'], ['naskh', 'نسخ'], ['ruqaa', 'رقعة']];

function fitWords(root) {
  root.querySelectorAll('.word').forEach((el) => {
    el.dataset.w = el.textContent.trim();
    const base = parseFloat(getComputedStyle(el).fontSize);
    let fs = base;
    el.style.fontSize = fs + 'px';
    while (el.scrollWidth > el.clientWidth + 1 && fs > 44) { fs -= 4; el.style.fontSize = fs + 'px'; }
  });
}
function applyFont(root) {
  FONTS.forEach(([k]) => root.classList.toggle('f-' + k, G.font === k));
}

const render5 = C.A.render;
C.A.render = function (root, M) {
  applyFont(root);
  render5(root, M);
  fitWords(root);
};
function setFont(k) {
  G.font = k; store.set('v6font', k);
  document.querySelectorAll('[data-font]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.font === k)));
  const root = document.getElementById('screen');
  root.classList.remove('enter'); renderAll(true);
  void root.offsetWidth; root.classList.add('enter');
  clearTimeout(root._enterT); root._enterT = setTimeout(() => root.classList.remove('enter'), 1600);
}
if (document.fonts) document.fonts.ready.then(() => { if (typeof renderAll === 'function') renderAll(true); });

/* Plain words (owner, 2026-10-29: «السحر ما أدري شنو كاتب»). Item names say what you do, in
   everyday Iraqi words, and every item's sheet says in one line what it is. Only the words on
   screen change; the day and its times stay exactly as the reference resolves them. */
const PLAIN = {
  qiyam: 'صلاة الليل والوتر', qiyam2: 'صلاة الليل والوتر', ghadwa: 'أذكار الصبح وقرآن، بلا تلفون',
  deep1: 'تركيز: أصعب شغلة عندك', deep2: 'تركيز: شغلة مهمة ثانية', duha: 'استراحة وصلاة الضحى',
  msgs1: 'رد على الرسائل', close: 'الرسائل وتسكير شغل اليوم', work1: 'الشغل: اتصالات ومتابعة',
  work2: 'الشغل: برّة واجتماعات', jumua_prep: 'تجهّز للجمعة: غُسل وسورة الكهف', free: 'وقت حر ويّا الأهل',
  self: 'وقتك: قراءة أو تعلّم خفيف', self_am: 'وقتك أو ويّا الأهل', visit: 'زيارة الأهل وطلعة ويّا الأطفال',
  prep: 'تجهّز للنوم: محاسبة وترتيب باچر', child: 'وقت ويّا ابنك وأذكار المساء', family: 'الأسرة: عشاء وسوالف',
  light: 'مشي بالشمس وفطور', lunch: 'غداء ويّا الأهل',
};
Object.assign(SHORT, { qiyam2: 'صلاة الليل', ghadwa: 'الأذكار', duha: 'استراحة', msgs1: 'الرسائل', close: 'الرسائل',
  work1: 'الشغل', work2: 'الشغل', jumua_prep: 'تجهّز', prep: 'تجهّز', visit: 'زيارة', self: 'وقتك', self_am: 'وقتك', free: 'وقت حر' });
const WHAT = {
  qiyam2: 'تصحى قبل الفجر بشوية وتصلّي صلاة الليل والوتر. التطبيق يحطها بس إذا يبقى إلك نوم 6 ساعات أو أكثر، وإذا ما يبقى يحط الوتر قبل النوم بدالها.',
  qiyam: 'تصحى قبل الفجر بشوية وتصلّي صلاة الليل والوتر. التطبيق يحطها بس إذا يبقى إلك نوم 6 ساعات أو أكثر.',
  witr: 'تصلّي الوتر قبل لا تنام، لأن نومك الليلة ما يكفي حتى تصحى قبل الفجر.',
  ghadwa: 'بعد صلاة الفجر: أذكار الصبح وشوية قرآن، والتلفون بعيد عنك.',
  deep1: 'أصعب شغلة عندك اليوم، وأنت بعدك فايق. اضغط «ركّز» حتى تسكت التنبيهات لحد ما تخلص.',
  deep2: 'شغلة مهمة ثانية تحتاج تركيز. اضغط «ركّز» حتى تسكت التنبيهات لحد ما تخلص.',
  msgs1: 'وقت قصير ترد بيه على الرسائل والمكالمات مرة وحدة، بدل ما تقطع شغلك كل شوية.',
  close: 'ترد على اللي بقى من الرسائل وتسكّر شغل اليوم.',
  work1: 'شغلك: اتصالات ومتابعة. مهامك تنحط هنا إذا بيها فراغ.', work2: 'شغلك برّة أو اجتماعات. مهامك تنحط هنا إذا بيها فراغ.',
  duha: 'استراحة قصيرة، وإذا تحب صلاة الضحى.', nap: 'قيلولة قصيرة تعوّض شوية من نوم الليل.',
  prep: 'قبل النوم: محاسبة دقيقتين وترتيب شغلات باچر.', sleep: 'نومك. التطبيق يحسبه من العشاء لحد ما تصحى.',
  free: 'وقت حر ويّا أهلك. مهامك تنحط هنا إذا بيها فراغ.', self: 'وقت إلك. مهامك تنحط هنا إذا بيها فراغ.',
  self_am: 'وقت إلك أو ويّا أهلك. مهامك تنحط هنا إذا بيها فراغ.', light: 'تطلع للشمس وتمشي شوية، وتفطر.',
  jumua_prep: 'تتجهّز لصلاة الجمعة: غُسل، وتقرا سورة الكهف، وتروح بدري.',
};
const WORDS = [
  ['منبّه الكعدة للسَّحَر', 'منبّه الصحيان لصلاة الليل'], ['منبّه الكعدة قبل الفجر', 'منبّه الصحيان قبل الفجر'], ['منبّه الكعدة', 'منبّه الصحيان'],
  ['الكعدة للسَّحَر', 'الصحيان لصلاة الليل'], ['السَّحَر قبل الفجر', 'صلاة الليل قبل الفجر'], ['وتكعد', 'وتصحى'],
  ['للسَّحَر', 'لصلاة الليل'], ['السَّحَر', 'صلاة الليل'], ['الكعدة', 'الصحيان'], ['تكعد', 'تصحى'],
];
function plainText(root) {
  const fix = (s) => WORDS.reduce((a, [f, t]) => a.split(f).join(t), s);
  const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let n = w.nextNode(); n; n = w.nextNode()) if (/الكعدة|السَّحَر|تكعد/.test(n.nodeValue)) n.nodeValue = fix(n.nodeValue);
  root.querySelectorAll('[aria-label]').forEach((el) => { const a = el.getAttribute('aria-label'); if (/الكعدة|السَّحَر|تكعد/.test(a)) el.setAttribute('aria-label', fix(a)); });
}
const render6 = C.A.render;
C.A.render = function (root, M) {
  M.ext.forEach((b) => { if (PLAIN[b.key]) b.title = PLAIN[b.key]; });
  if (M.cur && PLAIN[M.cur.key]) M.cur.title = PLAIN[M.cur.key];
  render6(root, M);
  plainText(root);
  if (G.sheet && WHAT[G.sheet]) {
    const at = root.querySelector('.sheet .hd');
    if (at && !root.querySelector('.sheet .what')) at.insertAdjacentHTML('afterend', `<div class="what"><b>شنو هاي؟</b>${WHAT[G.sheet]}</div>`);
  }
  fitWords(root);
};
