package org.ersinpro.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import java.util.*;

final class ApplicationRepository {
    private final PackageManager manager;
    private final String ownPackage;
    private final Map<String, Intent> intents = new LinkedHashMap<>();
    private final LruCache<String, Bitmap> icons = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };

    ApplicationRepository(PackageManager manager, String ownPackage) {
        this.manager = manager;
        this.ownPackage = ownPackage;
    }

    List<AppEntry> refresh() {
        Map<String, AppEntry> entries = new LinkedHashMap<>();
        intents.clear();
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER, entries);
        collect(Intent.CATEGORY_LAUNCHER, entries);
        return new ArrayList<>(entries.values());
    }

    private void collect(String category, Map<String, AppEntry> entries) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : manager.queryIntentActivities(query, 0)) {
            String name = info.activityInfo.packageName;
            if (ownPackage.equals(name) || !info.activityInfo.exported) continue;
            ComponentName component = new ComponentName(name, info.activityInfo.name);
            String key = component.flattenToString();
            if (entries.containsKey(key)) continue;
            CharSequence label = info.loadLabel(manager);
            entries.put(key, new AppEntry(key, label == null || label.length() == 0 ? name : label.toString(),
                info.activityInfo.applicationInfo.category));
            intents.put(key, new Intent(query).setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        }
    }

    Intent intent(String key) { return intents.get(key); }

    Bitmap icon(String key, int size) {
        Bitmap cached = icons.get(key);
        if (cached != null) return cached;
        try {
            Drawable drawable = manager.getActivityIcon(ComponentName.unflattenFromString(key));
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            int width = Math.max(1, drawable.getIntrinsicWidth());
            int height = Math.max(1, drawable.getIntrinsicHeight());
            float scale = (float) size / Math.max(width, height);
            int x = Math.round(width * scale);
            int y = Math.round(height * scale);
            drawable.setBounds((size - x) / 2, (size - y) / 2, (size + x) / 2, (size + y) / 2);
            drawable.draw(new Canvas(bitmap));
            icons.put(key, bitmap);
            return bitmap;
        } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            return null;
        }
    }

    void clearIcons() { icons.evictAll(); }
}
