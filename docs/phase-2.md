# v0.2.0-alpha verification

The project owner confirmed installation and use of v0.1.0-alpha on the Optive X5. Kodi opened, the remote Back behavior differed from expectations inside Kodi, and exiting sometimes left a black screen. The cause remains undetermined.

The launcher keeps its Android HOME integration and standard explicit external launch intents. It retains the grid adapter across refreshes and pins the launched component through pause and resume. Android and Kodi own input dispatch while Kodi is in the foreground. The automated fixture launches a separate task, exposes a dark playback view, exits via Back and checks that the launcher and selected card return.

Kodi uses window-specific keymaps with a global fallback, so Back during playback can behave differently from Back in a menu. Record those contexts separately before changing any mapping. [Official Kodi keymap documentation](https://kodi.wiki/view/Keymap)

## Local verification

The API 28 and API 33 emulators passed native instrumentation and end-to-end remote tests, including launch of the separate player task, Back return and card selection. The Java model checks, 54-key localization parity and Android Lint passed. The landscape preview was captured on API 28 with the test fixture installed. The signed debug APK is 41,508 bytes.

On API 33 at 1920 × 1080 and 240 dpi, PSS was 64,988 KiB five seconds after the UI suite and 35,551 KiB at the next sample, approximately twenty seconds after the suite. [Performance methodology and RSS/heap results](performance.md)

## Physical checklist

1. Install the preview over v0.1.0-alpha and launch it from the factory home. Keep the factory launcher as the default.
2. Move between Entertainment and Apps & Tools, change Kodi's category, relaunch and verify that the change persists. Test Focus Mode, favorites and hiding.
3. Open Kodi from its card. Compare the remote Back button, Kodi's own Exit action and the remote Home button. Repeat and record the route, visible result, elapsed time and whether the Kodi card regains focus.
4. Test one additional player. On a black screen, use the factory HOME or source selector to return. Preserve the manufacturer launcher and services.
5. If authorized ADB is already available, capture a short local window with `adb -s TARGET logcat -v brief ActivityTaskManager:I ActivityManager:I WindowManager:I AndroidRuntime:E '*:S'`. Start immediately before reproduction and stop with Ctrl+C. On Android 9, ActivityManager may carry the relevant events. Review the capture locally and redact account details, device identifiers, local addresses and full intents before sharing. Use `adb -s TARGET shell dumpsys activity activities` only if the active task and resumed activity need confirmation.

## Settings and roadmap

App Management supports an automatic or manual category per launchable component. Favorites stay first within each category. Focus Mode applies to both categories. Appearance offers two card densities. Developer Tools describes the v0.3.0-alpha roadmap. About shows the installed version.

Hosted CI and release promotion depend on repository access. Physical verification of this preview and its performance targets is pending.

## v0.2.1-alpha · Two-row home

The owner reported that v0.2.0-alpha ran on the physical Yasomi Optive X5, its dynamic greeting worked, and Kodi exit produced no black screen in the latest reported test. The v0.2.1-alpha preview awaits a new physical session. The workspace contains emulator captures and the written visual specification.

The home renders Entertainment and Apps & Tools together. Settings is reachable below the rows. Empty sections collapse their card strip. The window-focus return path and the original Android task behavior remain in place.

### Emulator verification

Windows debug runs passed the model tests, all 54 localization keys and Lint. Android 9 passed the full suite at 1280 × 720 on the delivery APK. Android 13 passed the full suite at 1920 × 1080 before the final width cap, which increases the number of complete cards visible and keeps the next icon outside the overflow cue. The GitHub matrix exercises the final code at both resolutions. Regression checks include inter-row and horizontal navigation, return focus, persistence, corrupt preferences, and scoped hide/unhide model behavior.

[Startup and memory observations](performance.md)

### X5 physical checklist

1. Update using the locally signed preview and keep the factory launcher selected. Confirm the two headings, large native icons, neutral cards and Settings fit the projection.
2. Scroll right through each row. Move between the rows and confirm each keeps its focus anchor. Check a long Turkish or German label and the HDMI output in regular use.
3. Check that Kodi is absent from the home and available inside App Management. Choose **Show on home** to test unhide, restart ErsinPro and verify the choice persists. Restore the desired hidden state afterward. If the firmware supplies generic model identifiers, choose **Hide from home** manually.
4. Start an installed application and return using Back. If you choose to unhide Kodi for the check, exit Kodi through its own menu and confirm the launcher view and focus recover. Keep the earlier Kodi internal Back observation separate.
5. Record display mode and any clipping, unexpected gaps or focus changes. Use the existing performance protocol only if authorized ADB is already available.

## v0.2.2-alpha · TV-safe home

The owner reported clipping near the right edge and incomplete Kodi model detection on the physical X5 with the previous preview. This release moves Settings into the top-right header, adds symmetric safe insets and tightens card spacing. The palette stays consistent with the previous preview. Firmware with generic device metadata uses the manual Kodi setting.

### Emulator evidence

Android 9 at 720p passed the full remote suite. Android 13 at 1080p passed instrumentation. The initial Windows Android 13 end-to-end run returned an empty UI Automator hierarchy. The suite now retries the snapshot and reports the resumed Activity if visibility still fails. The hosted matrix verifies both profiles before integration.

[Performance](performance.md)

The images show the rendered app area of each emulator with test fixtures installed. Android reserves part of the 1080p display for system navigation in this profile.

### X5 physical checklist

1. Update from the local debug APK with the same signing key. Open ErsinPro from the factory launcher.
2. In both display modes used on the projector, confirm the clock, date and full Settings control are visible. Check that both rows fit and labels remain readable at normal viewing distance.
3. From Settings, press Down to enter the first nonempty row. Use Up to return. Move Left and Right in each row, including overflow. Launch an app and use Back to confirm focus returns to its card.
4. Under **Settings → Manage applications → Kodi**, choose **Hide from home** if Kodi is still visible. Restart ErsinPro and verify the choice remains. Choose **Show on home**, restart again and verify the explicit reversal remains. Confirm the factory launcher, Miracast and iOSCast are still available.
5. Report the display mode and any remaining crop, glare, label or focus issues. Warm-start and memory targets need physical measurements using the existing protocol.
