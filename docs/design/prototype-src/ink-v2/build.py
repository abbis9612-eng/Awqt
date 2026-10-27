"""Builds docs/design/yawm-ink-v2.html. Data and engine come from the approved yawm-ink.html build
and ../engine.js; core, screens and styles from ../ink/; the improvements from this folder.
Run: python3 build.py ../../yawm-ink-v2.html"""
import os, re, sys
here = os.path.dirname(os.path.abspath(__file__))
rd = lambda *p: open(os.path.join(here, *p), encoding='utf-8').read()
built = rd('..', '..', 'yawm-ink.html')
data = re.search(r'const YAWM_TIMES=.*?;\n', built, re.S).group(0)
s = rd('shell.html')
for k, v in {
    '/*@DATA*/': data, '/*@ENGINE*/': rd('..', 'engine.js'), '/*@CORE*/': rd('..', 'ink', 'core.js'),
    '/*@JS_A*/': rd('..', 'ink', 'cA.js'), '/*@CSS_A*/': rd('..', 'ink', 'cA.css'), '/*@APPLE_CSS*/': rd('..', 'ink', 'apple.css'),
    '/*@CSS_V2*/': rd('v2.css'), '/*@JS_V2*/': rd('v2.js'),
}.items():
    assert k in s, k
    s = s.replace(k, v)
open(sys.argv[1], 'w', encoding='utf-8').write(s)
print('built', len(s))
