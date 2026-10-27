"""Writes docs/content/review-sheet.md, the printable sheet for the reviewer, from texts.json.
Run: python3 tools/review_sheet.py
"""
import json, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
d = json.load(open(os.path.join(ROOT, 'docs/content/texts.json'), encoding='utf-8'))
kinds = {'quran': 'قرآن', 'hadith': 'حديث', 'athar': 'أثر'}
out = ['# ورقة المراجعة الشرعية لتطبيق «يوم»', '',
       'لكل نص: صحّح اللفظ من المصدر، واكتب الدرجة ومن حكم بها، ثم وافق أو ارفض. النصوص كلها منقولة كما هي من «منهج اليوم المتكامل».', '',
       '| # | id | النوع | النص | المصدر | الدرجة المقترحة | الحاكم | موافق / مرفوض | ملاحظة المراجع |',
       '| --- | --- | --- | --- | --- | --- | --- | --- | --- |']
for i, t in enumerate(d['texts'], 1):
    cell = lambda s: (s or '').replace('|', '\\|')
    out.append(f"| {i} | {t['id']} | {kinds[t['kind']]} | {cell(t['text'])} | {cell(t['source'])} | {cell(t['grade'])} | {cell(t['graded_by'])} | {'موافق' if t['reviewed_by'] else '☐ موافق ☐ مرفوض'} | {cell(t['review_note'])} |")
out += ['', '**لا تُستعمل (من المنهج):** ' + d['do_not_use'], '',
        'اسم المراجع: ____________  التاريخ: ____________  التوقيع: ____________', '']
open(os.path.join(ROOT, 'docs/content/review-sheet.md'), 'w', encoding='utf-8').write('\n'.join(out))
print(len(d['texts']), 'rows -> docs/content/review-sheet.md')
