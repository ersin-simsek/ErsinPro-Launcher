# Performance methodology

Targets for the Yasomi Optive X5 are launcher PSS at or below 50 MB and warm startup at or below 1 second. Physical boot-transition and PSS observations are recorded below. A controlled physical warm-start series remains pending. APK size, emulator startup and icon-cache capacity are separate metrics.

1. Document the Android version, display resolution, app count, active configuration and power state.
2. For cold startup, use `adb shell am start -S -W -n org.ersinpro.launcher/.HomeActivity` where authorized. Repeat ten times and record launch state, median and maximum `TotalTime`.
3. For warm startup, return to another foreground app while the launcher process stays resident. Repeat the same command without `-S`. Record Android's launch-state classification. Exclude hot samples from the warm result and report the sample count.
4. After initial layout settles, record `adb shell dumpsys meminfo org.ersinpro.launcher` total PSS. Repeat after scrolling through the catalog and after returning from another app. Record available system RAM with `adb shell cat /proc/meminfo`.
5. Check remote movement, launch and return on-device. With authorization, capture `adb shell dumpsys gfxinfo org.ersinpro.launcher framestats` after a repeatable navigation sequence. Retain raw logs locally and report aggregate values.
6. If optional profiles are evaluated, repeat the same protocol with the same app catalog and compare startup, PSS and frame timings against the static baseline.

When ADB is unavailable, record observed behavior and leave ADB-derived metrics unmeasured.

## Comparing the earlier Android 13 observations

The earlier debug run reported 129,853 KiB PSS immediately after the UI suite and 52,269 KiB at a later snapshot. RSS also decreased from 233,052 KiB to 127,360 KiB. Those snapshots did not control elapsed time, garbage collection, task history or emulator memory pressure, so they do not isolate a cause or establish a regression. PSS apportions shared pages, RSS counts all resident mapped pages, and managed/native heap counters track allocations in different allocators. Device RAM covers the complete system.

For comparison, record cold/warm classification, display density and dimensions, and memory at fixed points such as 5, 30 and 60 seconds after home becomes idle. Repeat after a controlled external app return. Report PSS, RSS, native and managed heap, and available system RAM separately.

## v0.2.0-alpha emulator samples

Android 13, 1920 × 1080 at 240 dpi, debug build, foreground home after the end-to-end suite. Values are KiB from `dumpsys meminfo`.

| Approximate time after suite | Total PSS | Total RSS | Java heap PSS | Native heap PSS |
| --- | ---: | ---: | ---: | ---: |
| 5 s | 64,988 | 106,400 | 4,580 | 12,448 |
| 20 s | 35,551 | 67,520 | 5,024 | 6,996 |

These are two snapshots from one run. A controlled X5 warm-start and memory series remains to be collected.

## v0.2.1-alpha two-row preview

Windows-hosted debug emulators, 160 dpi, software GPU, animation scales disabled, network disconnected and foreground launcher after the remote regression suite. Startup samples are three consecutive force-stop launches using `adb shell am start -S -W -n org.ersinpro.launcher/.HomeActivity`. Memory is total PSS from `adb shell dumpsys meminfo org.ersinpro.launcher`, sampled five seconds after the third launch.

| Emulator profile | Cold startup (ms) | Total PSS (KiB) |
| --- | --- | ---: |
| Android 9, 1280 × 720, final card geometry | 1171, 1270, 1204 | 57,553 |
| Android 13, 1920 × 1080, prior to final four-card width cap | 2475, 2637, 2729 | 101,434 |

The API 33 tool reported all three launches as COLD and 204,812 KiB RSS at the PSS snapshot. These observations describe the emulator and debug build. See the physical snapshot below. Repeatable X5 warm-start and memory measurements remain open.

## v0.2.2-alpha TV-safe preview

Use the same Windows debug-emulator protocol, at 160 dpi with software rendering and disabled animation scales. After the remote suite, run three `adb shell am start -S -W -n org.ersinpro.launcher/.HomeActivity` launches and sample `adb shell dumpsys meminfo org.ersinpro.launcher` five seconds after the third launch.

| Emulator profile | Cold startup (ms) | Total PSS (KiB) |
| --- | --- | ---: |
| Android 9, 1280 × 720 | 1314, 1306, 1244 | 58,281 |

These are debug emulator measurements. Controlled X5 warm-start and repeated memory measurements remain open.

## Physical X5 boot transition

The Android 9/API 28 ARMv7 X5 with 1 GB RAM was observed using ADB over TCP/IP and ActivityManager logcat timestamps. Each version contributed one controlled reboot. Time zero is the factory launcher’s first draw.

| Milestone | v0.2.6-alpha | v0.2.7-alpha |
| --- | ---: | ---: |
| Receiver process started | +89.3 s | +0.181 s |
| ErsinPro foreground | +89.9 s | +0.818 s |
| ErsinPro first draw | +91.9 s | +3.627 s |

The first-draw interval fell by about 96.1%. It measures a rendering milestone after the factory home draws. Total power-on time and input readiness require separate measurements. ErsinPro remained foreground for 120.3 s in each observation. One v0.2.6-alpha physical memory snapshot measured 36,858 KiB total PSS, about 36 MiB. The sample does not establish typical or peak memory use.

Repeat under matched boot conditions and collect fixed-time PSS samples and classified warm starts before generalizing. [Physical test record](compatibility.md)