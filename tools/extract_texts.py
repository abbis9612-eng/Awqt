"""Copies the candidate religious texts of docs/METHOD.md section 11, verbatim, into
docs/content/texts.json with the review fields empty (CLAUDE.md content rules).

Run once; afterwards texts.json is the file the reviewer's decisions are written into.
Refuses to overwrite a texts.json that already holds a review.
Run: python3 tools/extract_texts.py
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
METHOD = os.path.join(ROOT, 'docs/METHOD.md')
OUT = os.path.join(ROOT, 'docs/content/texts.json')

if os.path.exists(OUT):
    old = json.load(open(OUT, encoding='utf-8'))
    if any(r.get('reviewed_by') for r in old['texts']):
        sys.exit('texts.json already has reviews; edit it by hand instead of extracting again')

md = open(METHOD, encoding='utf-8').read()
section = md[md.index('## 11.'):]
rows = [l for l in section.splitlines() if l.startswith('| ') and not l.startswith('| id') and not l.startswith('| ---')]
texts = []
for line in rows:
    cells = [c.strip() for c in line.strip().strip('|').split(' | ')]
    assert len(cells) == 5, line
    rid, text, source, grade, usage = cells
    kind = {'q': 'quran', 'h': 'hadith', 'a': 'athar'}[rid[0]]
    texts.append({
        'id': rid,
        'kind': kind,
        'text': text,
        'source': source,
        'grade': grade,
        'graded_by': None,
        'note': 'يُعرض مع التنبيه على الخلاف' if 'مع التنبيه' in usage else None,
        'usage': usage,
        'reviewed_by': None,
        'reviewed_at': None,
        'review_note': None,
    })

do_not_use = re.search(r'\*\*لا تُستعمل:\*\* (.*)', section).group(1)
doc = {
    'about': 'النصوص الدينية المرشحة من docs/METHOD.md القسم 11، منقولة حرفياً. لا يظهر أي نص في التطبيق قبل أن يملأ المراجع الشرعي graded_by وreviewed_by وreviewed_at. نصوص القرآن تُطابق مع Tanzil حرفياً قبل المراجعة.',
    'do_not_use': do_not_use,
    'texts': texts,
}
with open(OUT, 'w', encoding='utf-8') as o:
    json.dump(doc, o, ensure_ascii=False, indent=2)
    o.write('\n')
print(len(texts), 'texts ->', os.path.relpath(OUT, ROOT))
