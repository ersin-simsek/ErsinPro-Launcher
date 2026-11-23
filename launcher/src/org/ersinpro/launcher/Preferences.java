package org.ersinpro.launcher;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Preferences {
    private static final String KEY = "configuration";
    private final SharedPreferences store;
    boolean recovered;

    Preferences(SharedPreferences store) {
        this.store = store;
    }

    LauncherState load() {
        try {
            String value = store.getString(KEY, null);
            return value == null ? new LauncherState() : decode(value);
        } catch (RuntimeException | JSONException invalid) {
            recovered = true;
            return new LauncherState();
        }
    }

    static LauncherState decode(String value) throws JSONException {
        JSONObject json = new JSONObject(value);
        if (json.getInt("version") != 1) throw new JSONException("Unknown schema");
        LauncherState state = new LauncherState();
        state.language = LauncherState.language(json.getString("language"));
        state.sort = json.getString("sort");
        if (!java.util.Arrays.asList("custom", "az", "za").contains(state.sort)) {
            throw new JSONException("Invalid sort order");
        }
        state.focusMode = json.getBoolean("focusMode");
        state.focused = json.getString("focused");
        read(json.getJSONArray("favorites"), state.favorites);
        read(json.getJSONArray("hidden"), state.hidden);
        read(json.getJSONArray("order"), state.order);
        state.compactCards = json.optBoolean("compactCards");
        state.kodiDefaultApplied = json.optBoolean("kodiDefaultApplied");
        if (json.has("kodiHidden")) state.kodiHidden = json.getBoolean("kodiHidden");
        JSONObject categories = json.optJSONObject("categories");
        if (categories != null) {
            java.util.Iterator<String> keys = categories.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                state.categories.put(key, categories.getBoolean(key));
            }
        }
        return state;
    }

    private static void read(JSONArray array, java.util.Collection<String> values) throws JSONException {
        if (array.length() > 10000) throw new JSONException("Invalid collection size");
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (!(value instanceof String)) throw new JSONException("Invalid component");
            values.add((String) value);
        }
    }

    static String encode(LauncherState state) {
        try {
            return new JSONObject()
                .put("version", 1)
                .put("language", state.language)
                .put("sort", state.sort)
                .put("focusMode", state.focusMode)
                .put("focused", state.focused)
                .put("favorites", new JSONArray(state.favorites))
                .put("hidden", new JSONArray(state.hidden))
                .put("compactCards", state.compactCards)
                .put("kodiDefaultApplied", state.kodiDefaultApplied)
                .put("kodiHidden", state.kodiHidden)
                .put("categories", new JSONObject(state.categories))
                .put("order", new JSONArray(state.order)).toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    void save(LauncherState state) {
        store.edit().putString(KEY, encode(state)).apply();
    }
}
