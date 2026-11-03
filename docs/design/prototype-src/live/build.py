"""Builds docs/design/yawm-live.html: inlines the three screenshots of the owner's lake file as data URIs."""
import base64, pathlib, sys
here = pathlib.Path(__file__).parent
src = (here / 'yawm-live.src.html').read_text(encoding='utf-8')
for key, name in (('HIS_DAWN', 'his-dawn.jpg'), ('HIS_ASR', 'his-asr.jpg'), ('HIS_NIGHT', 'his-night.jpg')):
    b64 = base64.b64encode((here / name).read_bytes()).decode()
    src = src.replace('{{%s}}' % key, 'data:image/jpeg;base64,' + b64)
assert '{{' not in src
out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else here / '../../yawm-live.html')
out.write_text(src, encoding='utf-8')
print(out.resolve(), len(src))
