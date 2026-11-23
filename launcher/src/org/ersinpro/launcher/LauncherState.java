package org.ersinpro.launcher;

import java.text.Collator;
import java.util.*;

final class LauncherState {
    static final String SETTINGS = ":settings";
    static final String SYSTEM = "system";
    String language = SYSTEM;
    String sort = "custom";
    boolean focusMode;
    String focused = SETTINGS;
    final Set<String> favorites = new LinkedHashSet<>();
    final Set<String> hidden = new LinkedHashSet<>();
    boolean compactCards;
    boolean kodiDefaultApplied;
    Boolean kodiHidden;
    final Map<String, Boolean> categories = new HashMap<>();
    final List<String> order = new ArrayList<>();

    static String language(String value) {
        return Arrays.asList("en", "tr", "de").contains(value) ? value : SYSTEM;
    }

    static Locale locale(String preference, Locale system) {
        String tag = SYSTEM.equals(preference) ? system.getLanguage() : preference;
        return SYSTEM.equals(language(tag)) ? Locale.ENGLISH : Locale.forLanguageTag(tag);
    }

    List<AppEntry> ordered(List<AppEntry> installed, Locale locale) {
        List<AppEntry> result = new ArrayList<>(installed);
        Collator collator = Collator.getInstance(locale);
        Comparator<AppEntry> alphabetical = (a, b) -> {
            int label = collator.compare(a.label, b.label);
            return label == 0 ? a.id.compareTo(b.id) : label;
        };
        switch (sort) {
            case "za": result.sort(alphabetical.reversed()); break;
            case "custom":
                Map<String, Integer> ranks = new HashMap<>();
                for (int i = 0; i < order.size(); i++) ranks.put(order.get(i), i);
                result.sort(Comparator.comparingInt((AppEntry app) ->
                    ranks.getOrDefault(app.id, Integer.MAX_VALUE)).thenComparing(alphabetical));
                break;
            default: result.sort(alphabetical);
        }
        return result;
    }

    List<AppEntry> visible(List<AppEntry> installed, Locale locale) {
        List<AppEntry> result = ordered(installed, locale);
        result.removeIf(app -> isHidden(app) || (focusMode && !favorites.contains(app.id)));
        result.sort(Comparator.comparing(app -> !favorites.contains(app.id)));
        return result;
    }

    void move(List<AppEntry> installed, Locale locale, String id, int direction) {
        List<AppEntry> sorted = ordered(installed, locale);
        int from = indexOf(sorted, id);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= sorted.size()) return;
        Collections.swap(sorted, from, to);
        order.clear();
        for (AppEntry item : sorted) order.add(item.id);
        sort = "custom";
    }

    boolean isHidden(AppEntry app) {
        return isKodi(app) && kodiHidden != null ? kodiHidden : hidden.contains(app.id);
    }

    void toggleHidden(AppEntry app) {
        if (isKodi(app)) {
            kodiHidden = !isHidden(app);
            hidden.removeIf(id -> id.startsWith("org.xbmc.kodi/"));
        } else if (!hidden.remove(app.id)) {
            hidden.add(app.id);
        }
    }

    private static boolean isKodi(AppEntry app) {
        return app.id.startsWith("org.xbmc.kodi/");
    }

    boolean applyReferenceDefaults(String manufacturer, String model, List<AppEntry> installed) {
        if (kodiHidden != null || kodiDefaultApplied || !HomeContent.isReferenceProjector(manufacturer, model)) return false;
        for (AppEntry app : installed) {
            if (isKodi(app)) {
                kodiHidden = true;
                kodiDefaultApplied = true;
            }
        }
        return kodiDefaultApplied;
    }

    boolean entertainment(AppEntry app) {
        return HomeContent.entertainment(app.id, app.category, categories.get(app.id));
    }

    static int indexOf(List<AppEntry> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(id)) return i;
        return -1;
    }

    static int restore(List<AppEntry> list, String id, int previous) {
        int found = indexOf(list, id);
        return found >= 0 ? found : Math.max(0, Math.min(previous, list.size() - 1));
    }
}
