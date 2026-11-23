# Compatibility record

| Device | Android | Result |
| --- | --- | --- |
| Pixel 3 emulator | API 33 | Native instrumentation and remote end-to-end tests passed. See the [Phase 1 report](phase-1.md). |
| Google APIs x86_64 emulator, 1280×720 | Android 9/API 28 | Native instrumentation and remote end-to-end tests passed locally. |
| Yasomi Optive X5, ARMv7, 1 GB RAM, D-pad | Android 9/API 28 | v0.2.6-alpha and v0.2.7-alpha automatic startup, foreground persistence and app return verified using ADB over TCP/IP. OEM HOME interception remains a limitation. |
| CI emulator | API 28 and 33 | Workflow configured. Results are available after it runs. |

## v0.2.0-alpha physical verification

The owner tested v0.1.0-alpha on the Optive X5 and reported intermittent black screens after Kodi exit. Follow the [v0.2 reproduction checklist](phase-2.md) before promoting this preview. The launcher candidate and controlled player return were exercised in emulation. Manufacturer firmware behavior is tracked separately.

## v0.2.4-alpha X5 HOME and Back

On the Android 9 X5, the package manager resolved `MAIN/HOME` to ErsinPro and listed the factory launcher as another HOME candidate. During read-only inspection, ErsinPro was resumed in the HOME stack with a `MAIN/HOME` intent. A separate standard ErsinPro task came from a shell start with an explicit component and no HOME category. The factory launcher also had a standard task. Historical ActivityManager entries show that UID 1000 explicitly started the factory component without a HOME category. Those starts bypass ordinary HOME resolution. The logs do not identify the service, key handler or boot trigger behind them.

ErsinPro uses `singleTask` and the default package task affinity. Its HOME, app launcher and TV launcher filters remain separate. Back from an app card moves focus to Settings. Elsewhere ErsinPro delegates Back to Android. Android 9 can finish a root launcher activity on Back and resume the previous task. The distinct tasks explain how an explicit shell launch can have a different Back outcome. Which code handled the previously reported key presses is unknown. The owner also observed the factory launcher after a reboot while ErsinPro remained preferred. At that checkpoint the new APK had yet to be installed. The v0.2.6-alpha and v0.2.7-alpha sections record later reboot tests. HOME and Back code and manifest routing are unchanged. [Android task and Back behavior](https://developer.android.com/guide/components/activities/tasks-and-back-stack)

The installed version during inspection was v0.2.3-alpha. For an approved upgrade, use `& $adb -s $x5 install -r .\build\ersinpro-v0.2.4-alpha-debug.apk` from the project root. Record the resolved HOME and foreground task before and after pressing HOME on the remote. Use the read-only resolver from the rollback block and `& $adb -s $x5 shell dumpsys activity activities | Select-String 'mResumedActivity|topResumedActivity'`. Press Back from a focused card, then again from Settings, recording both outcomes. Repeat from an ErsinPro session opened by its icon in the factory launcher. At supported 720p and 1080p outputs, verify that the entire lower row, outline and focused label fit. A boot test requires separate approval. Observe the first foreground activity after boot and resolve HOME again before pressing a key or sending a launch intent.

Record the previous HOME component before any approved HOME change. These PowerShell commands restore the factory HOME for owner user 0 and verify it. Set `$adb` to the Android SDK ADB executable and `$x5` to the already-authorized ADB target. To restore ErsinPro instead, substitute `org.ersinpro.launcher/.HomeActivity` in the first command.

```powershell
& $adb -s $x5 shell cmd package set-home-activity --user 0 com.zhiying.ytb.zy.tv/.activity.MainActivity
& $adb -s $x5 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

If an approved APK upgrade needs to be reversed, restore the previous same-signed debug APK from the project root. If Android rejects the downgrade or signature, stop and retain the installed data.

```powershell
& $adb -s $x5 install -r -d .\build\ersinpro-v0.2.3-alpha-debug.apk
```

## v0.2.6-alpha X5 automatic startup

One controlled reboot of the physical Android 9 X5 advanced the Android boot count from 506 to 507. The factory launcher was initially foreground. The ErsinPro receiver then logged `BOOT_COMPLETED` and one HOME launch attempt. ActivityManager recorded ErsinPro taking the foreground about 107 seconds after reboot was requested. The Windows observer sampled foreground state about every four seconds and confirmed that ErsinPro stayed foreground for 120.3 seconds. The device activity log showed no factory launcher takeover during that window. No manual input was sent during observation.

After observation, ErsinPro Settings opened. FileManager launched from its ErsinPro card and pressing Back returned to the same card. No ErsinPro crash was recorded. One memory snapshot showed 36,858 KiB PSS. The preferred Android HOME remained ErsinPro and the signed v0.2.6-alpha APK stayed installed. The same-signed v0.2.5-alpha APK is the in-place rollback for this test.

The result covers automatic startup and foreground persistence on this X5 under the tested conditions. The separate OEM HOME key interception remains a firmware limitation. The boot receiver attempts one launch per Android boot count, with no persistent service or retry loop.

## v0.2.7-alpha X5 early startup

One approved reboot advanced the physical X5 boot count from 510 to 511. In the device activity log, the first factory launcher draw is the reference. Compared with v0.2.6-alpha, the ErsinPro receiver process started at +0.181 s instead of +89.3 s, ErsinPro entered the foreground at +0.818 s instead of +89.9 s, and its first draw occurred at +3.627 s instead of +91.9 s. The first draw is a rendering milestone. Input readiness was checked separately after observation.

The early broadcast path started ErsinPro after the user unlocked. The later `BOOT_COMPLETED` delivery found the same boot already claimed and made no second launch. The receiver leaves user settings in credential-protected storage and defers to ordinary boot completion if the user is still locked.

ErsinPro stayed foreground for 120.3 seconds without manual input or a factory launcher takeover. Settings opened, FileManager launched and Back restored the FileManager card. The Android HOME preference was preserved. OEM HOME key interception remains a separate limitation. This is one physical comparison, and it does not establish a cold power-on time. The same-signed v0.2.6-alpha APK is the in-place rollback.

## Physical acceptance checklist

- Open ErsinPro from the factory launcher. Check branding, date, clock, icon loading and readable spacing.
- Move Up, Down, Left and Right. Press OK and Back. Open several actual apps and confirm the previous card regains focus.
- Favorite, reorder and hide an app. Open Favorites and toggle Focus Mode. Restart ErsinPro and confirm persistence. Confirm the empty state leaves Settings reachable.
- Switch to Turkish and German. Check date, dialogs and any clipping. Reset preferences and confirm the launcher recovers.
- Check that factory controls for HDMI, sound and projection settings remain available. Test HOME selection only when the stock picker offers a reversible choice.
- Measure performance with the [protocol](performance.md) if authorized ADB becomes available.

## Report template

Record device/model, Android/API, RAM, display resolution, app version, installation route, D-pad keys, app launches, localization, HOME behavior, rollback, benchmark results and known limitations. Omit device serials, account details, local IPs and keys.

## Screenshots and demo

Capture a synthetic emulator home with `./tools/test-ui.ps1 -Serial emulator-5554 -Capture`. Its screenshot is saved under the ignored build directory. For a demo recording, show a short remote-only flow through the two categories, player launch and Back return, Focus Mode and a language switch. Review the recording before sharing to remove personal app or account information.
