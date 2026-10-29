"""Builds docs/design/yawm-ink-v5.html: v4 plus the clearer, moving design (v5.css, v5.js).
Run: python3 build.py ../../yawm-ink-v5.html"""
import os, re, sys
here = os.path.dirname(os.path.abspath(__file__))
rd = lambda *p: open(os.path.join(here, *p), encoding='utf-8').read()
data = re.search(r'const YAWM_TIMES=.*?;\n', rd('..', '..', 'yawm-ink.html'), re.S).group(0)
s = rd('shell.html')
for k, v in {
    '/*@DATA*/': data, '/*@ENGINE*/': rd('..', 'engine.js'), '/*@CORE*/': rd('..', 'ink', 'core.js'),
    '/*@JS_A*/': rd('..', 'ink', 'cA.js'), '/*@CSS_A*/': rd('..', 'ink', 'cA.css'), '/*@APPLE_CSS*/': rd('..', 'ink', 'apple.css'),
    '/*@CSS_V2*/': rd('..', 'ink-v2', 'v2.css'), '/*@JS_V2*/': rd('..', 'ink-v2', 'v2.js'),
    '/*@CSS_V3*/': rd('..', 'ink-v3', 'v3.css'), '/*@JS_V3*/': rd('..', 'ink-v3', 'v3.js'),
    '/*@CSS_V4*/': rd('..', 'ink-v4', 'v4.css'), '/*@JS_V4*/': rd('..', 'ink-v4', 'v4.js'),
    '/*@CSS_V5*/': rd('v5.css'), '/*@JS_V5*/': rd('v5.js'),
}.items():
    assert k in s, k
    s = s.replace(k, v)
open(sys.argv[1], 'w', encoding='utf-8').write(s)
print('built', len(s))
