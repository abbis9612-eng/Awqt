# Decision log

Append a row for every decision. Never delete rows; mark superseded ones instead.

| Date | Decision | Why | Revisit if |
| --- | --- | --- | --- |
| 2026-10-15 | Standalone new app, separate from رفيق الذكر | Owner's decision | — |
| 2026-10-15 | Android first, iOS later | Owner's phone and local market; iOS needs a Mac and Apple's Screen Time entitlement | A Mac becomes available |
| 2026-10-15 | Kotlin Multiplatform + Compose Multiplatform | One codebase for logic and UI on both platforms | — |
| 2026-10-15 | Room 3 (`androidx.room3`) for local storage | Official, KMP-first, stable 3.0.3 | Database encryption becomes required → SQLDelight + SQLCipher |
| 2026-10-15 | adhan-kotlin 0.0.7, wrapped behind `PrayerTimesEngine` | Tested astronomical library; pre-1.0 so isolated and pinned | Upstream breaks or is abandoned |
| 2026-10-15 | No network, no account, no analytics in v1 | Privacy promise is a core differentiator | Sync is added in v3 (client-side encryption required) |
| 2026-10-15 | Western digits (0123) everywhere | Clearer for times; consistency matters most | Owner changes it |
| 2026-10-15 | Business model: free core and religious content forever; optional paid "advanced" later | Protects trust; funds maintenance | — |
| 2026-10-15 | Closed beta (20–50 users) before Ramadan 1448; public launch after Eid al-Fitr 2027 | Real Ramadan test; less pressure | Gates in PRODUCT.md fail |
| 2026-10-15 | ~~Fixed-offset template with separate winter/summer versions~~ — SUPERSEDED 2026-10-20 | Calibrated on one day; a 365-day simulation showed overlaps on 260 days, 4-min child block on Hanafi winter days, negative blocks in London | — |
| 2026-10-20 | Adaptive segment template (preferred/min/priority/droppable/flex) for WORKDAY and FRIDAY | Passes all invariants on 365 days × 3 cases (Sulaymaniyah Shafi, Sulaymaniyah Hanafi, London) | A new day type or city fails the invariants |
| 2026-10-20 | Night balance from planned sleep: bedtime = max(user, Isha + 30); qiyam only if ≥ 6 h night remains; nap 20 or 90; shortfall shown | Old rule gave < 7 h total on 103 days while claiming to protect sleep | Scholar reviewer or owner changes the targets |
| 2026-10-20 | Absolute time instants, not minutes since midnight | Isha falls after midnight in London summer | — |
| 2026-10-20 | `docs/reference/` (Python reference + 2026 CSV + sample days) is the executable spec | Kotlin must match it; invariants run in CI | Reference is proven wrong (stop and tell the owner) |
| 2026-10-20 | Compose Multiplatform 1.12.2, compileSdk 37, targetSdk 36 | Compose 1.12 requires compileSdk 37; Play requires target 36 | Next stable Compose line |
| 2026-10-20 | `SCHEDULE_EXACT_ALARM` (not `USE_EXACT_ALARM`); `setExactAndAllowWhileIdle` for adhan and blocks; `setAlarmClock` only for the user's wake alarm | Play restricts `USE_EXACT_ALARM` to alarm/timer/calendar apps; `setAlarmClock` shows as the system's next alarm | Play policy changes |
| 2026-10-20 | Design tokens in `DesignTokens.kt`; no Style Dictionary in v1 | Six colors and two fonts do not justify JavaScript tooling on Windows | A second platform or a designer needs shared tokens |
| 2026-10-20 | Official agent skills: Kotlin/kotlin-agent-skills and android/skills | Verified repos and install commands | — |
| 2026-10-27 | Visual identity «الحبر» (Ink); spec in `docs/design/IDENTITY.md`, prototype `docs/design/yawm-ink.html`. Supersedes the sun-arc signature, placeholder palette and IBM Plex Sans Arabic in PRODUCT.md section 7 (proposals, now replaced) | Owner chose it after comparing about 15 directions on his phone | Owner changes it after real use |
| 2026-10-27 | Fonts bundled in the app: Aref Ruqaa (ink word), Noto Naskh Arabic (text), Amiri (times and numbers) | Ink identity; v1 has no network so fonts must ship inside | A license check in step 7 fails |
| 2026-10-27 | Own icon family (31 icons, `docs/design/icons/`): thin outline plus one filled part colored by meaning | Clear, specific to the day's items, matches the ink idea | — |
| 2026-10-27 | Dark theme from Isha to Fajr by default; user can pin light or dark in settings | Night use; follows the prayer day | — |
| 2026-10-27 | Screen map: first run, four tabs (Today, Night, Week, Page) and Settings; the Saturday review lives in Week | Matches the 8 features with one screen per job; tested in the prototype | — |
| 2026-10-27 | Design tokens stay in `DesignTokens.kt` with no Style Dictionary, although the identity now has three fonts and an icon palette | Still one platform and no designer tooling | A second platform starts |
| 2026-10-27 | Build here: code, tests and builds run in the Claude Code cloud session; GitHub Actions builds the debug APK the owner installs on his phone. Supersedes the Windows/Android Studio workflow in FIRST_PROMPT.md | Owner's choice; he does not need to install developer tools | Cloud builds become impossible |
| 2026-10-27 | Version check before step 1: Kotlin 2.4.21 is still the latest stable (2.5.0 is beta); Compose Multiplatform 1.12.2 latest stable (1.13 is beta); adhan2 0.0.7 latest (Maven Central). AGP and Room not yet re-checked: `dl.google.com` was blocked in the cloud environment | Rule 1 | Re-check all at the start of step 1 |
| 2026-10-27 | applicationId `com.dcc.yawm`; namespaces `com.dcc.yawm` (`:androidApp`) and `com.dcc.yawm.shared` (`:shared`) | Owner approved; neutral of the final store name | Never after the first Play upload |
| 2026-10-27 | Step 1 versions, all checked on official sources today: AGP 9.4.0 (needs Gradle 9.6.0, JDK 17), Gradle wrapper 9.6.0, Kotlin 2.4.21, Compose Multiplatform 1.12.2, androidx Activity 1.13.0; CI actions checkout v7, setup-java v6, gradle/actions v6, upload-artifact v7. Room 3.0.3 confirmed stable for step 5 | Rule 1 | Next step that touches them |
| 2026-10-27 | The Android entry point lives in `:shared` (`ComponentActivity.showYawm()`), so `:androidApp` needs no Compose compiler plugin | Keeps the app module to manifest and MainActivity, as AGP 9 rules intend | — |
| 2026-10-27 | Gradle distribution checksum not pinned yet: its checksum host was blocked from the cloud session. The wrapper jar is validated in CI by gradle/actions/setup-gradle | Pin `distributionSha256Sum` once the host is reachable | Host becomes reachable |

## Open (must be decided before the step that needs it)
- App name: check both stores first; avoid names close to "Noor" (Noor Focus exists).
- Scholar reviewer for religious content: needed before the closed beta (step 8).
- Remaining sleep shortfall on summer days (up to 29 min in Sulaymaniyah): whether to suggest
  anything beyond the 90-min nap goes to the scholar reviewer (Ibn al-Qayyim disliked sleep after Fajr).
- Saturday half-day template: add only if the owner needs it during personal use.
- Default high-latitude rule per region (London tests use seventh of the night).
- Adhan sound for step 6: source and license (not covered by any document yet).
- Notification texts for adhan and block starts (step 6).
- What exactly the user may edit in the day template (first run and settings).
- Design of the missing screens before step 7: first run, settings, exact-alarm explanation, battery self-check, Saturday review, explaining a shrunk or dropped block, empty states.
- Rule for the ink word at 200% font scale (the word is decorative; the sentence under it must scale).
- Ramadan mode spec (needed for the beta before about 8 February 2027).
- Google Play account type: personal (requires a closed test with 12+ testers for 14 continuous
  days before production) or organization (exempt, but requires business verification).

## Later (not in v1)
- Friction before distracting apps during deep-work blocks (Android first; iOS needs entitlement).
- Home-screen widget.
- Ramadan, travel and sickness modes (Ramadan mode targeted for the beta).
- Optional sharing with spouse; English UI; Kurdish (Sorani); watch; annual zakat reminder.
