"""Builds docs/design/yawm-sanad.html: inlines the live-scene engine (day data, shapes, sky colours,
Scene class) from ../live/yawm-live.src.html so both pages share one engine."""
import pathlib, sys
here = pathlib.Path(__file__).parent
live = (here / '../live/yawm-live.src.html').read_text(encoding='utf-8')
start = live.index('/* ---------------- the day ---------------- */')
end = live.index('/* ---------------- the hourglass (focus) ---------------- */')
engine = live[start:end]
skin = "const SKIN=()=>document.getElementById('wrap').dataset.skin;"
assert engine.count(skin) == 1
engine = engine.replace(skin, "const SKIN=()=>'ruby';")
src = (here / 'yawm-sanad.src.html').read_text(encoding='utf-8')
assert src.count('/*@ENGINE*/') == 1
out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else here / '../../yawm-sanad.html')
out.write_text(src.replace('/*@ENGINE*/', engine), encoding='utf-8')
print(out.resolve(), len(src))
