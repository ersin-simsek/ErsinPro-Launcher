# Architecture and decisions

## ADR 001 · Native Views and the existing toolchain

Keep Java 8-compatible source, native Views and the existing JDK 17 plus Android SDK toolchain. The existing build works offline with installed tools. This keeps the app dependency-free and reduces migration risk at the first physical validation checkpoint. Revisit Kotlin or Gradle when a concrete project need justifies the migration.

Use API 28 as the minimum for the reference projector. Package visibility declarations cover only the two launchable activity categories. The app uses a separate package so the factory launcher and prototype can coexist.

## ADR 002 · Event-driven foreground UI (original grid)

An Activity owns view composition, dialogs and navigation. ApplicationRepository queries PackageManager and caches rasterized icons in a 2 MiB LRU. GridView recycles cards. The foreground Activity receives package and time broadcasts, refreshes on resume and releases cached icons on memory pressure. There is no periodic worker, service or network permission.

## ADR 003 · Local state, focus and recovery (original grid)

LauncherState is a deterministic model for ordering, filtering, locale and focus restoration. Preferences persists one versioned JSON record in SharedPreferences. Decode failures restore defaults and overwrite the invalid record once. The reset dialog clears launcher preferences. This recovery covers application configuration.

Focus is anchored to a component identifier and falls back to a bounded position when that item disappears. GridView handles motion within the grid, with explicit toolbar transitions. Apps and Favorites retain separate session focus anchors. The settings entry remains reachable with an empty grid. Long press is a shortcut, and Settings exposes the same actions without long-press support.

Native string resources and activity configuration handle locale selection. Unsupported device languages use English. Clock and date use locale-specific Android format patterns with the device's 12/24-hour preference.

## v1.1 evaluation

Compare static and adaptive icon-cache profiles using the performance protocol before exposing a profile setting. Diagnostics and Privacy Inspector should be on-demand. Use public Android APIs, label requested and granted permissions separately, and exclude private identifiers from exports.

## ADR 004 · Two categories and external task return (original grid)

Classify actual launchable components using known streaming package IDs and Android's video category metadata. Kodi remains a tool by default. Store manual overrides per component beside existing preferences. The schema keeps its version because v0.1 records can be read with defaults for the new fields.

Keep the native grid adapter attached while refreshing its contents. Pin the launched component across pause and resume. Restore its card after the launcher window regains focus and posts the selection to the grid, so refresh and focus callbacks cannot replace the return target. Launch external activities through ordinary Android intents, and leave remote input inside the foreground app to Android.

## ADR 005 · Two simultaneous TV rows

Use independent native horizontal scrollers and route vertical D-pad movement explicitly between their last focused components and the Settings footer. Horizontal movement stays within the current row. Each card has a native focus state and a short scale animation without layout remeasurement.

Each row binds icons only in and near the viewport and releases offscreen drawable references. The existing bitmap cache stays bounded. The home dimensions derive from available dp and preserve the previously selected card density. Empty categories retain their heading and a localized state message with no fixed-height card strip.

The reference Kodi preference uses Android manufacturer and model metadata. It applies once when Kodi is first seen on a recognized Optive X5 and persists alongside the existing hidden state. An explicit unhide survives refreshes and restarts. Generic manufacturer firmware identifiers may require hiding Kodi manually through App Management.

## ADR 006 · TV-safe header and package visibility

The Activity derives symmetric safe insets from the viewport in dp. The right side of the header stacks the date and a Settings button below the clock. Up and Down follow the visual order, skipping empty rows. The rows use natural card width and a fixed gap. A bounded icon cache serves both rows. Focus changes update a state drawable and retain the card bounds.

Kodi has a nullable package preference. A manual choice takes precedence over the one-time recognized-X5 default, survives activity changes in the Kodi package, and leaves component-level visibility for other applications intact. Existing component-specific settings are retained until an explicit Kodi choice is made.

## ADR 007 · One-shot HOME launch after Android boot

The native Java Activity still owns rendering and D-pad focus. BootReceiver handles BOOT_COMPLETED and, since v0.2.7-alpha, LOCKED_BOOT_COMPLETED. It checks whether credential-protected storage is available before attempting the early launch and saves a per-boot claim to avoid launching again when ordinary boot completion arrives. The receiver requires neither root nor a resident service. Android HOME preference and OEM explicit activity starts are separate mechanisms. [Physical verification and rollback](compatibility.md)