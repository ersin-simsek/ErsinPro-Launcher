# Phase 1 · ErsinPro preview

## Implementation

- Replaced the former text list with an original native TV layout, local clock, real application icons and a remote-focused grid.
- Added Favorites, custom ordering, app hiding, Focus Mode, locale switching and versioned local preferences with recovery.
- Added the Android HOME candidate and preserved installation alongside the prototype through an independent package ID.
- Added the SDK-only build, model tests, resource checks, native instrumentation, end-to-end remote scripts and GitHub Actions configuration.
- Added public project documentation, compatibility reporting, benchmark instructions and release criteria.

## Engineering decisions

The preview keeps Java and native Android Views so the existing SDK-only build remains reproducible. GridView recycles cards, and an LRU limits cached launcher icon pixels to 2 MiB. The activity uses system broadcasts while visible. Focus rings are static and use no per-frame animations.

## Validation status

- JDK 17, SDK Platform 34 and Build Tools 34.0.0 produce a debug APK signed for API 28. APK alignment, scoped Android lint and the model and localization checks pass.
- Android 33 native instrumentation passed catalog rendering, initial focus, D-pad transfer to the grid, application launch intent dispatch, persisted selection, favorites and configuration recovery.
- Android 33 end-to-end automation passed the HOME candidate check, remote navigation, Favorites empty state, Focus Mode, German selection and persistence, recovery from corrupt configuration, and opening and returning from Android Settings.
- APK build output is `build/ersinpro-debug.apk`. The instrumentation artifact is `build/ersinpro-instrumentation.apk`.
- The Android 9 image download timed out initially. Resumed transfers produced an archive matching Google's published digest. Native instrumentation and the full remote end-to-end suite then passed on the local API 28 emulator.
- GitHub Actions is configured for API 28 and API 33. The hosted workflow awaits repository publication.
- The X5 preview installation, default HOME selector, performance, and hardware-specific controls are pending the [physical-device checklist](compatibility.md).
- On the 1920×1080 Android 33 emulator, the debug application's total PSS was 129,853 KiB immediately after the UI suite and 52,269 KiB at a later snapshot. These are two observations from one emulator session. X5 PSS and startup-time targets remain unverified. The 2 MiB icon cache is a design limit.
