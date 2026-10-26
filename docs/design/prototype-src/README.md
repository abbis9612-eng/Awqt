# مصدر النموذج التفاعلي

- `app.html` — الواجهة. فيها مكانين `/*@DATA*/` و`/*@ENGINE*/` ينملون عند البناء.
- `engine.js` — نقل خوارزمية `docs/reference/resolver_reference.py` إلى JavaScript، مع نموذج NOAA لموقع الشمس.
- `test.js` — يقارن المحرك بجدول المرجع: 1095 ليلة و8 أيام مفصلة، ويقارن وقت العصر المحسوب من الظل بالجدول.

البيانات (`data.js`) تتولد من `docs/reference/reference-2026.csv`. الملف النهائي: `docs/design/yawm-prototype.html`.
النموذج للعرض والتجربة فقط؛ التطبيق الحقيقي يُبنى بـKotlin حسب `CLAUDE.md`.

## identities-2 (خمس هويات جديدة)
`identities-2/` يبني `docs/design/yawm-identities-2.html`: الرزنامة، لوحة الجامع، المسبحة، الرواق، العدسة.
انسخ `data.js` و`engine.js` من هذا المجلد جنب الملفات، وبعدين: `python3 build.py ../../yawm-identities-2.html`.

## simple (أربع تصاميم بسيطة وأنيقة)
`simple/` يبني `docs/design/yawm-simple.html`: السطر، البطاقة، الحلقة، الصفحة.
انسخ `data.js` و`engine.js` من هذا المجلد جنب الملفات، وبعدين: `python3 build.py ../../yawm-simple.html`.

## three (الحبر، الشناشيل، بغداد)
`three/` يبني `docs/design/yawm-three.html`. انسخ `data.js` و`engine.js` جنب الملفات، وبعدين: `python3 build.py ../../yawm-three.html`.

## ink-apple (الحبر وأخواته)
`ink-apple/` يبني `docs/design/yawm-ink-apple.html`: الحبر، وأربع تصاميم على طريقة تطبيقات آبل (سماء، قائمة، حلقات، شاشة القفل) كلها تحمل كلمة الحبر.
انسخ `data.js` و`engine.js` جنب الملفات، وبعدين: `python3 build.py ../../yawm-ink-apple.html`.

## ink-list (الحبر والقائمة)
`ink-list/` يبني `docs/design/yawm-ink-list.html`: المزيج المختار، ووياه «الحبر» و«قائمة» للمقارنة.
انسخ `data.js` و`engine.js` جنب الملفات، وبعدين: `python3 build.py ../../yawm-ink-list.html`.
