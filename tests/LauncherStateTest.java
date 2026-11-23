package org.ersinpro.launcher;

import java.util.*;

public final class LauncherStateTest {
    public static void main(String[] args) {
        LauncherState state = new LauncherState();
        AppEntry clock = new AppEntry("clock", "Clock");
        AppEntry media = new AppEntry("media", "Media");
        AppEntry browser = new AppEntry("browser", "Browser");
        List<AppEntry> installed = Arrays.asList(media, clock, browser);
        expect(state.visible(installed, Locale.ENGLISH), browser, clock, media);
        state.sort = "za";
        expect(state.visible(installed, Locale.ENGLISH), media, clock, browser);
        state.move(installed, Locale.ENGLISH, "browser", -1);
        expect(state.visible(installed, Locale.ENGLISH), media, browser, clock);
        state.favorites.add("clock");
        expect(state.visible(installed, Locale.ENGLISH), clock, media, browser);
        state.hidden.add("clock");
        expect(state.visible(installed, Locale.ENGLISH), media, browser);
        state.focusMode = true;
        expect(state.visible(installed, Locale.ENGLISH));
        state.hidden.remove("clock");
        expect(state.visible(installed, Locale.ENGLISH), clock);
        expect(state.visible(Arrays.asList(media, browser), Locale.ENGLISH));
        state.focusMode = false;
        AppEntry radio = new AppEntry("radio", "Radio");
        expect(state.visible(Arrays.asList(radio, media, browser), Locale.ENGLISH), media, browser, radio);
        check(LauncherState.restore(Arrays.asList(media, browser), "browser", 0) == 1, "reordered focus");
        check(LauncherState.restore(Arrays.asList(media, browser), "clock", 2) == 1, "removed focus");
        check(LauncherState.restore(Collections.emptyList(), "clock", 2) == 0, "empty focus");
        check(LauncherState.locale("de", Locale.ENGLISH).equals(Locale.GERMAN), "explicit locale");
        check(LauncherState.locale("system", Locale.forLanguageTag("tr-TR")).getLanguage().equals("tr"), "device locale");
        check(LauncherState.locale("system", Locale.JAPANESE).equals(Locale.ENGLISH), "fallback locale");
        check(LauncherState.language("unknown").equals("system"), "invalid stored language");
        int[] hours = {0, 4, 5, 11, 12, 17, 18, 21, 22, 23};
        int[] expected = {3, 3, 0, 0, 1, 1, 2, 2, 3, 3};
        for (int i = 0; i < hours.length; i++) {
            final int hour = hours[i];
            check(HomeContent.greeting(() -> hour) == expected[i], "greeting at " + hour);
        }
        AppEntry youtube = new AppEntry("com.google.android.youtube/.Main", "YouTube");
        check(state.entertainment(youtube), "streaming package");
        check(state.entertainment(new AppEntry("example/.Video", "Video", 2)), "metadata");
        check(!state.entertainment(new AppEntry("org.xbmc.kodi/.Main", "Kodi", 2)), "Kodi tool");
        state.categories.put(youtube.id, false);
        check(!state.entertainment(youtube), "manual override");
        LauncherState reference = new LauncherState();
        AppEntry kodi = new AppEntry("org.xbmc.kodi/.Main", "Kodi", 2);
        List<AppEntry> catalog = Collections.singletonList(kodi);
        check(!reference.applyReferenceDefaults("Other", "X5", catalog), "unrelated X5 is unaffected");
        check(!reference.applyReferenceDefaults("Yasomi", "Optive X50", catalog), "unrelated Optive is unaffected");
        check(reference.applyReferenceDefaults("Yasomi", "Optive X5", catalog), "reference default applied");
        check(reference.visible(catalog, Locale.ENGLISH).isEmpty(), "Kodi hidden on reference");
        reference.toggleHidden(kodi);
        check(!reference.applyReferenceDefaults("Yasomi", "Optive X5", catalog), "unhide survives refresh");
        expect(reference.visible(catalog, Locale.ENGLISH), kodi);
        LauncherState generic = new LauncherState();
        AppEntry renamedKodi = new AppEntry("org.xbmc.kodi/.New", "Kodi", 2);
        generic.toggleHidden(kodi);
        check(generic.isHidden(renamedKodi), "manual hide applies across Kodi activities");
        generic.toggleHidden(renamedKodi);
        check(!generic.applyReferenceDefaults("Yasomi", "Optive X5", catalog)
            && !generic.isHidden(kodi), "explicit show prevents the model default");
        HomeLayout hd = new HomeLayout(1280, 720, false);
        HomeLayout full = new HomeLayout(1920, 1080, false);
        HomeLayout dense = new HomeLayout(1280, 720, true);
        HomeLayout projectorHd = new HomeLayout(854, 480, false);
        check(hd.icon > 80 && hd.tile > hd.icon && hd.cardWidth > hd.tile, "720p icon artwork fits inside its tile");
        check(full.cardHeight > hd.cardHeight && full.icon > hd.icon, "1080p scales when dp increases");
        check(hd.edge == 70 && hd.top == 36 && hd.bottom == 36, "TV insets reserve safe margins");
        check(HomeLayout.TILE_MARGIN >= full.tile * 0.015f
            && HomeLayout.LABEL_GAP > full.tile * 0.015f, "focused tile fits without overlapping the label");
        check(dense.cardHeight < hd.cardHeight && dense.icon < hd.icon, "stored compact appearance is retained");
        check(projectorHd.icon > 68 && hd.icon >= 135 && full.icon > 174,
            "icon sizing covers 720p at 240 dpi and both emulator layouts");
        System.out.println("PASS: ordering, visibility, favorites, catalog updates, focus and locale");
    }

    private static void expect(List<AppEntry> actual, AppEntry... expected) {
        check(actual.equals(Arrays.asList(expected)), "application order or visibility");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
