# Changelog

## 0.2.7-alpha · Early startup

- Added early boot handling when user storage is available, with ordinary boot completion as fallback and a single launch claim per boot.
- On the tested X5, reduced the factory-to-ErsinPro first-draw interval from about 91.9 s to 3.627 s. Verified 120.3 s in the foreground and checked Settings, FileManager and Back focus restoration.

## 0.2.6-alpha · Automatic startup

- Added a one-shot ErsinPro launch after Android boot completion.
- Verified automatic startup and 120.3 s of foreground persistence on the X5. OEM HOME interception remained possible.

## 0.2.5-alpha · Modern Clean

- Introduced the Modern Clean home layout and documented the English 1080p and 720p emulator views.

## 0.2.4-alpha · Projector layout

- Refined the projector home layout and recorded X5 HOME resolution and Back observations.

## 0.2.3-alpha · Focus and layout

- Refined home focus and TV layout and refreshed the emulator captures.

## 0.2.2-alpha · Preview

- Moved Settings below the clock and date inside symmetric TV-safe insets.
- Tightened the two app rows with natural-width cards, larger icons and a fixed focus outline.
- Persisted manual Kodi visibility by package, including explicit unhide.
- Added parallel English and German project guides and current emulator captures.

## 0.2.1-alpha · Preview

- Replaced category tabs with two always-visible app rows and independent horizontal D-pad scrolling.
- Increased installed icon size and introduced neutral landscape cards with an ice-blue focus outline.
- Added a one-time Kodi hide preference for the reference projector profile. App Management can show Kodi again.
- Retained external task return, per-row focus, favorites, sorting, language preferences and Focus Mode.

## 0.2.0-alpha · Preview

- Redesigned the TV home with larger cards, a localized greeting and two app categories.
- Added automatic classification, persistent manual overrides and selectable card density.
- Retain the grid adapter and restore the launched component after returning from an external task.
- Added greeting boundaries and a separate external-task regression fixture. Kodi behavior on the Optive X5 awaits physical verification.

## 0.1.0-alpha · Preview

- Added the ErsinPro TV interface, remote-operated grid, installed icons, Favorites, application ordering and Focus Mode.
- Added English, Turkish and German localization with a saved runtime selector.
- Added versioned preferences, configuration recovery and a manual reset.
- Added deterministic model and translation tests, an emulator smoke suite and GitHub Actions configuration.
- Retained the early X5 prototype in the development archive.
