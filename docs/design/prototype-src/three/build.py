import sys, re
s = open('shell.html', encoding='utf-8').read()
s = s.replace('/*@DATA*/', open('data.js', encoding='utf-8').read().strip()).replace('/*@ENGINE*/', open('engine.js', encoding='utf-8').read())
for k in 'ABC':
    s = s.replace('/*@CSS_%s*/' % k, open('c%s.css' % k, encoding='utf-8').read())
    s = s.replace('/*@JS_%s*/' % k, open('c%s.js' % k, encoding='utf-8').read())
s = s.replace('/*@CORE*/', open('core.js', encoding='utf-8').read())
open(sys.argv[1], 'w', encoding='utf-8').write(s)
print('built', len(s))
