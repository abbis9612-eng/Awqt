"""Builds docs/design/yawm-final.html: the complete prototype.
Inlines the live-scene engine (../live) and the companion's words and cards (../rafiq/yawm-sanad.src.html)."""
import pathlib, sys
here = pathlib.Path(__file__).parent
live = (here / '../live/yawm-live.src.html').read_text(encoding='utf-8')
engine = live[live.index('/* ---------------- the day ---------------- */'):live.index('/* ---------------- the hourglass (focus) ---------------- */')]
skin = "const SKIN=()=>document.getElementById('wrap').dataset.skin;"
assert engine.count(skin) == 1
engine = engine.replace(skin, "const SKIN=()=>'ruby';")
sanad = (here / '../rafiq/yawm-sanad.src.html').read_text(encoding='utf-8')
script = sanad[sanad.index("/* ---------------- what سند says ---------------- */"):sanad.index('/* each face renders the same conversation in its own way */')]
src = (here / 'yawm-final.src.html').read_text(encoding='utf-8')
for marker in ('/*@ENGINE*/', '/*@SCRIPT*/'):
    assert src.count(marker) == 1, marker
src = src.replace('/*@ENGINE*/', engine).replace('/*@SCRIPT*/', script)
out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else here / '../../yawm-final.html')
out.write_text(src, encoding='utf-8')
print(out.resolve(), len(src))
