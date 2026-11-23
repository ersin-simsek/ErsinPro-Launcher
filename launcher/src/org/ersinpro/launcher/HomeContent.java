package org.ersinpro.launcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class HomeContent {
    interface TimeSource { int localHour(); }

    private static final Set<String> STREAMING = new HashSet<>(Arrays.asList(
        "com.netflix.mediaclient", "com.netflix.ninja", "com.google.android.youtube",
        "com.google.android.youtube.tv", "com.hbo.hbonow", "com.wbd.stream",
        "com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom",
        "com.exxen.android", "com.blu.android", "com.turkcell.ott"));

    static boolean entertainment(String component, int category, Boolean override) {
        if (override != null) return override;
        int slash = component.indexOf('/');
        String packageName = slash < 0 ? component : component.substring(0, slash);
        // Android ApplicationInfo.CATEGORY_VIDEO is 2.
        return STREAMING.contains(packageName) || (category == 2 && !"org.xbmc.kodi".equals(packageName));
    }

    static boolean isReferenceProjector(String manufacturer, String model) {
        String make = manufacturer.toLowerCase(java.util.Locale.ROOT);
        String name = model.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return name.equals("optivex5") || name.equals("yasomioptivex5")
            || (make.contains("yasomi") && name.equals("x5"));
    }

    static int greeting(TimeSource time) {
        int hour = time.localHour();
        if (hour >= 5 && hour < 12) return 0;
        if (hour >= 12 && hour < 18) return 1;
        if (hour >= 18 && hour < 22) return 2;
        return 3;
    }
}
