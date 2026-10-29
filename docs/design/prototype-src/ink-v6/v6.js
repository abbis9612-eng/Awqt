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
