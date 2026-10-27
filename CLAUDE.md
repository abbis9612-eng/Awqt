# CLAUDE.md — اليوم المتكامل (working name)

## What this project is
An Arabic-first Islamic day planner for Android (iOS later). It builds the user's day around the
five prayers in their city, adapts every day to the sun, computes a nightly sleep budget
("night balance": bedtime, qiyam or witr-before-sleep, nap length, honest shortfall), and covers
8 roles: worship, body, spouse, child, business, dev, learning, self. Offline, no account, no ads.

Source of truth. Open only the section you need; do not load whole files without reason:
- `docs/PRODUCT.md` — product: scope, features, design, the 10-step execution plan.
- `docs/METHOD.md` — the method: adaptive day template (section 3), night balance (section 4),
  checklist, weekly plan, and graded religious texts.
- `docs/DECISIONS.md` — decision log. Read it every session; append every new decision.
- `docs/reference/` — executable specification of the day algorithm (see "Reference data").
- `docs/design/IDENTITY.md` — the approved visual identity («الحبر»): tokens, fonts, icons, screens.
  Working prototype: `docs/design/yawm-ink.html`. Icons: `docs/design/icons/` (SVG, plain and `color/`).

## Who you are working with
The owner (DCC) gives instructions and tests on his phone. He does not write or read code.
- Technical correctness is entirely your responsibility. Never assume he will catch a mistake.
- Talk to him in simple Iraqi Arabic. Explain any technical term in one line.
- End every step with: what changed, how he checks it on his phone (exact taps), what is next.
- Ask before anything destructive or irreversible: deleting files, rewriting git history,
  changing applicationId or package names, signing keys, publishing.

## Non-negotiable rules
1. IMPORTANT — Verify, never recall. Before adding or upgrading any library, plugin, SDK,
   Kotlin/Gradle/AGP version, or relying on any platform or store policy, check the current
   stable version or rule on the official source (GitHub releases, Maven Central, official docs,
   Play Console Help, Apple Developer docs). Record version and date checked in
   `gradle/libs.versions.toml` (comment) and in `docs/DECISIONS.md`.
   Pre-release versions (alpha, beta, RC) only with the owner's explicit approval.
2. One step at a time from the execution plan in `docs/PRODUCT.md`. Show a short plan, wait for
   approval, and do not start the next step until the current step's check passes.
3. Tests are the proof. Run the tests and the Android build yourself and report the real output.
   Never say "done" or "should work" without having run it.
4. Scope lock: only the 8 MVP features. Not building: social feed, ads, full Quran reader,
   hadith library, fatwa chat, competitive badges or leaderboards. New ideas go to
   `docs/DECISIONS.md` under "Later", never straight into code.
5. Privacy: v1 has no network. Do not add the INTERNET permission, analytics, crash-reporting or
   ads SDKs. Location is used on-device only (approximate is enough); store city coordinates only.
6. Religious content: no Quran, hadith or dhikr text ships without source, grade, grader and
   reviewer approval (see Content rules). Never write or generate religious text yourself.
7. The day algorithm must match `docs/reference/` exactly. If you believe the reference is wrong,
   stop and explain it to the owner; never silently diverge.

## Verified stack — checked 2026-10-20 (re-verify per rule 1 before use)
| Item | Version | Note |
| --- | --- | --- |
| Kotlin | 2.4.21 | latest stable |
| Kotlin Toolchain | 0.13 | JetBrains-recommended for new KMP projects; pre-1.0 — fall back to the Android Studio KMP wizard if it blocks progress |
| Compose Multiplatform | 1.12.2 | built on Jetpack Compose 1.12.1; 1.13 is pre-release: do not use without approval |
| Android Gradle Plugin | 9.4.0 | requires Gradle 9.6.0 and JDK 17+ |
| compileSdk | 37 | required by Compose 1.12 (37.1 belongs to the Compose 1.13 pre-release line, not ours) |
| targetSdk | 36 | required by Google Play for new apps and updates since 2026-08-31 |
| minSdk | 26 | adhan uses kotlinx-datetime → java.time (API 26+, or core library desugaring) |
| Room | 3.0.3 (`androidx.room3`) | KMP-first |
| adhan-kotlin | `com.batoulapps.adhan:adhan2:0.0.7` | pre-1.0, depends on kotlinx-datetime 0.8.0: keep it behind `PrayerTimesEngine`, convert at the boundary to our time types, pin, test |

Tooling: the code is written, built and tested in the Claude Code cloud session (Linux); GitHub Actions
builds the debug APK, and the owner installs that APK on his phone. Android Studio on the owner's
Windows PC is optional, not required. The cloud environment must allow `dl.google.com` (Android SDK
and Google Maven); if it is blocked, stop and ask the owner to allow it in the environment settings.
Official agent skills (install once per machine, inside Claude Code):
- Kotlin: `/plugin marketplace add Kotlin/kotlin-agent-skills` then `/plugin install kotlin-agent-skills@Kotlin`
- Android: `/plugin marketplace add android/skills` then `/plugin install android-skills@android-skills`

## Architecture — AGP 9 rules, do not violate
- `:shared` — Kotlin Multiplatform library with all logic and the Compose UI.
  Plugins: `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`.
  Never `com.android.application` or `com.android.library` in this module.
- `:androidApp` — Android entry point only, `com.android.application`: MainActivity, manifest,
  applicationId, versionCode, targetSdk. Do NOT apply `org.jetbrains.kotlin.android`
  (AGP 9 has built-in Kotlin support).
- `iosApp/` — later. iOS builds need macOS and Xcode; the owner's machine is Windows 11.
- `:shared` and `:androidApp` must have different namespaces. If Compose resources are used in
  `:shared`, enable Android resources explicitly in the new KMP Android DSL.
- `:shared` commonMain packages: `prayer/` (engine wrapper), `day/` (templates, resolver,
  night balance), `data/` (Room), `content/`, `ui/`.
- Minimal dependencies. Plain constructor injection; no DI framework until a real need is logged.
- Device checks happen on the owner's real phone with the APK from GitHub Actions.

## Domain rules
### Time model
- Prayer times and blocks are absolute instants (or LocalDateTime in the city zone), never
  "minutes since midnight" alone: at high latitudes Isha can fall after midnight.
### Prayer times
- `PrayerTimesEngine(coordinates, date, method, madhab, highLatitudeRule, mosqueAdjustments)`
  returns Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. Method, Asr madhab and high-latitude rule are
  user choices; per-prayer minute adjustments match the local mosque.
- Tests: every day of `docs/reference/reference-2026.csv` (Sulaymaniyah Egyptian/Shafi,
  Sulaymaniyah Egyptian/Hanafi, London MWL/Shafi with seventh-of-the-night rule) within ±2 min.
  If a value is off by more, investigate method parameters; do not widen the tolerance.
### Night balance (METHOD.md section 4)
- bedtime = max(user bedtime, default 21:00; Isha + 30 min).
- qiyam = night sleep with wake at Fajr − 20 min is ≥ 6 h; otherwise witr before sleep and wake
  at Fajr − 10 min.
- night sleep = max(0, wake − bedtime). nap = 20 min if (7 h − night) ≤ 30 min, else 90 min.
- shortfall = max(0, 7 h − (night + nap)), shown to the user in minutes.
### Day template (METHOD.md section 3)
- The day is segments between anchors (FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, BEDTIME).
  Each segment has ordered blocks with preferred minutes, min minutes, priority (1 = most
  important), droppable, and at most one flex block. No fixed offsets.
- fitSegment order: shrink droppable blocks to min (lowest priority first) → drop droppable
  blocks (lowest priority first) → only then shrink required blocks to min → give slack to the
  flex block. Never zero or negative durations. Record every shrink/drop for the UI.
- Day types: WORKDAY and FRIDAY (METHOD.md). The qiyam block comes from the previous night's plan.
### Invariants — property tests over 365 days × the 3 reference cases, run in CI
1. Blocks are ordered and never overlap; every duration > 0.
2. No block crosses a prayer time; each prayer block (Jumu'ah on Friday) starts exactly at its time
   and is ≥ 10 min.
3. Child and family blocks are ≥ 30 min when present.
4. On workdays there is at least one deep-work block ≥ 45 min.
5. bedtime ≥ Isha + 30 min; shortfall equals 7 h − (night + nap) whenever total < 7 h.
6. A required block is never shrunk while a droppable block in the same segment still exists.
7. No segment overflows.
### Notifications (Android)
- Use `SCHEDULE_EXACT_ALARM` with `canScheduleExactAlarms()` and an explanation screen that opens
  "Alarms & reminders". Do NOT declare `USE_EXACT_ALARM` (Play restricts it to alarm, timer and
  calendar apps).
- Adhan and block starts: `setExactAndAllowWhileIdle`. `setAlarmClock` only for the wake alarm the
  user chooses (it shows as the system's next alarm). Without permission: inexact alarms plus a
  clear warning in the UI.
- Schedule 48 h ahead; reschedule on boot, time change, time-zone change and permission grant.
- Self-check screen that detects battery-optimization restrictions and gives device-specific steps.

## Reference data (`docs/reference/`)
- `resolver_reference.py` — Python reference of night balance, segment fitting and invariants.
  Port it to Kotlin with the same behavior. Run it with `pip install adhanpy` then
  `python resolver_reference.py` to regenerate the data.
- `reference-2026.csv` — 1,095 rows: prayer times, bedtime, wake, qiyam, night sleep, nap, total,
  shortfall for every day of 2026 in the 3 cases (times with "+1" are after midnight).
- `sample-days.json` — fully resolved days, minute by minute: summer, winter, Hanafi winter,
  a Friday, the worst sleep day, and London summer and winter.
- The CSV times come from adhanpy, a port of the same Adhan algorithm; the Kotlin engine must
  match within ±2 min (rounding). Resolved block times must match within ±1 min.

## Content rules
Each religious text is a JSON record: `text`, `source` (book + number), `grade`, `graded_by`,
`note` (when disputed), `reviewed_by`, `reviewed_at`. Records without review never ship.
Quran text from Tanzil is used verbatim with attribution. The app reminds and organizes;
it never issues religious rulings.

## UI and design
- Arabic-first, full RTL; English second. Mirror only directional icons.
- Identity is decided (2026-10-27): «الحبر» (Ink). Follow `docs/design/IDENTITY.md` exactly; do not
  invent other colors, fonts or icons. Until step 7, logic steps need no UI beyond a plain test screen.
- Fonts ship inside the app (v1 has no network): Aref Ruqaa (the ink word), Noto Naskh Arabic (text),
  Amiri (times and numbers). Verify each font's license (SIL OFL) and file source per rule 1 before
  bundling. No letter-spacing on Arabic.
- Icons come from `docs/design/icons/` as Vector Drawables; keep each icon's single filled part as a
  separate path so its color comes from the token.
- One numeral system everywhere (see `docs/DECISIONS.md`).
- Tokens live in one Kotlin file, `DesignTokens.kt`, in `:shared`. No token-generation tooling in v1.
- Standard controls: Material 3 with customized tokens.
- Accessibility: touch targets ≥ 48dp, text contrast ≥ 4.5:1, respect reduced motion,
  layouts must survive 200% font scale.

## Workflow per session
1. Read this file and `docs/DECISIONS.md`; open only the needed section of `docs/PRODUCT.md`.
2. Re-verify every version or policy you will touch (rule 1).
3. Plan → owner approval → implement with tests → run tests and the debug build → report.
4. One commit per finished step with a clear message. GitHub Actions CI must be green and must
   run the invariant tests. Never commit secrets, keystores or local.properties.
5. Keep the Commands section below exact and current for the cloud session (`./gradlew ...`) and CI.
6. Before ending, update `docs/DECISIONS.md`.

## Commands
Verified in step 1 (2026-10-27). Gradle 9.6.0 through the wrapper; JDK 17 in CI.

| What | Command |
| --- | --- |
| Tests, checks and the debug APK (what CI runs) | `./gradlew build assembleDebug` |
| Only the shared logic tests | `./gradlew :shared:testAndroidHostTest` |
| Debug APK only | `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/androidApp-debug.apk` |

CI: `.github/workflows/ci.yml` runs on every push and uploads the artifact `yawm-debug-apk`.
The cloud session cannot reach `dl.google.com` yet, so builds run in CI until the owner allows it.

Owner: install the APK on the phone
1. On the phone, open GitHub (browser or app) → the repository → Actions → the latest green
   "Build and test" run.
2. Under Artifacts tap `yawm-debug-apk`; it downloads a zip.
3. Open the zip in Files and tap `androidApp-debug.apk`.
4. If Android asks, allow "Install unknown apps" for that app, then tap Install.
5. Open «يوم» from the app list.
