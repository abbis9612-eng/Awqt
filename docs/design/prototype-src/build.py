import sys
src, dst = sys.argv[1], sys.argv[2]
s = open(src, encoding='utf-8').read()
out = s.replace('/*@DATA*/', open('data.js', encoding='utf-8').read().strip()).replace('/*@ENGINE*/', open('engine.js', encoding='utf-8').read())
open(dst, 'w', encoding='utf-8').write(out)
