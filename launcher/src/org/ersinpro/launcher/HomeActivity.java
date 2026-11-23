package org.ersinpro.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class HomeActivity extends Activity {
    private static final int SURFACE = 0xff1d2531;
    private static final int SILVER = 0xffe8edf4;
    private static final int MUTED = 0xffaebccc;
    private static final int ICE = 0xffc3dbef;
    private static final int FOCUS_SURFACE = 0xff2a394b;
    private static final int HOME_WHITE = 0xfff6f8fc;
    private static final int PRO_BLUE = 0xff22b3ef;
    private static final int TILE_SURFACE = 0xff132032;
    private Preferences preferences;
    private LauncherState state;
    private Locale locale;
    private ApplicationRepository repository;
    private List<AppEntry> installed = Collections.emptyList();
    private AppRow entertainmentRow;
    private AppRow toolsRow;
    private LinearLayout page;
    private TextView clock;
    private TextView date;
    private Button settingsButton;
    private HomeLayout layout;
    private String returnAnchor;
    private TextView greeting;
    private TextView subtitle;
    private static final int[] GREETINGS = {R.string.morning, R.string.afternoon, R.string.evening, R.string.night};
    private static final int[] SUBTITLES = {R.string.morning_hint, R.string.afternoon_hint, R.string.evening_hint, R.string.night_hint};

    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getData() != null) {
                repository.clearIcons();
                refreshCatalog();
                entertainmentRow.invalidateIcons();
                toolsRow.invalidateIcons();
            } else {
                updateClock();
            }
        }
    };

    @Override protected void attachBaseContext(Context base) {
        preferences = new Preferences(base.getSharedPreferences("launcher", MODE_PRIVATE));
        state = preferences.load();
        locale = LauncherState.locale(state.language, base.getResources().getConfiguration().getLocales().get(0));
        Configuration localized = new Configuration(base.getResources().getConfiguration());
        localized.setLocale(locale);
        super.attachBaseContext(base.createConfigurationContext(localized));
    }

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        repository = new ApplicationRepository(getPackageManager(), getPackageName());
        setContentView(home());
        if (saved != null) {
            entertainmentRow.anchor = saved.getString("entertainmentFocus", "");
            toolsRow.anchor = saved.getString("toolsFocus", "");
        }
        if (preferences.recovered) {
            preferences.save(state);
            notice(R.string.recovered);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter packages = new IntentFilter();
        packages.addAction(Intent.ACTION_PACKAGE_ADDED);
        packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packages.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packages.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packages.addDataScheme("package");
        registerChanges(packages);
        IntentFilter time = new IntentFilter(Intent.ACTION_TIME_TICK);
        time.addAction(Intent.ACTION_TIME_CHANGED);
        time.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerChanges(time);
    }

    private void registerChanges(IntentFilter filter) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(changes, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(changes, filter);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        updateClock();
        refreshCatalog();
        if (hasWindowFocus()) restoreExternalFocus();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) restoreExternalFocus();
    }

    private void restoreExternalFocus() {
        if (returnAnchor == null || page == null) return;
        page.post(() -> {
            if (!hasWindowFocus() || returnAnchor == null) return;
            String target = returnAnchor;
            returnAnchor = null;
            restoreCard(target);
        });
    }

    @Override protected void onPause() {
        preferences.save(state);
        super.onPause();
    }

    @Override protected void onStop() {
        unregisterReceiver(changes);
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("entertainmentFocus", entertainmentRow.anchor);
        out.putString("toolsFocus", toolsRow.anchor);
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (repository != null && level >= TRIM_MEMORY_RUNNING_LOW) repository.clearIcons();
    }

    private View home() {
        Configuration config = getResources().getConfiguration();
        layout = new HomeLayout(config.screenWidthDp, config.screenHeightDp, state.compactCards);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xff060c14, 0xff0b1522}));
        page.setPadding(dp(layout.edge), dp(layout.top), dp(layout.edge), dp(layout.bottom));

        LinearLayout masthead = new LinearLayout(this);
        masthead.setGravity(Gravity.TOP);
        LinearLayout wordmark = new LinearLayout(this);
        wordmark.setOrientation(LinearLayout.VERTICAL);
        wordmark.setPadding(dp(layout.scale(9)), 0, 0, 0);
        String name = getString(R.string.app_name);
        TextView brand = text(name, layout.scale(103), HOME_WHITE, "sans-serif");
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(-0.025f);
        if (name.endsWith("Pro")) {
            SpannableString colored = new SpannableString(name);
            colored.setSpan(new ForegroundColorSpan(PRO_BLUE), name.length() - 3, name.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            brand.setText(colored);
        }
        wordmark.addView(brand);
        View accent = new View(this);
        accent.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        accent.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xff40cdfa, PRO_BLUE, 0x0022b3ef}));
        LinearLayout.LayoutParams accentSize = new LinearLayout.LayoutParams(
            dp(layout.scale(224)), dp(Math.max(2, layout.scale(8))));
        accentSize.topMargin = dp(layout.scale(5));
        wordmark.addView(accent, accentSize);
        masthead.addView(wordmark, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout time = new LinearLayout(this);
        time.setOrientation(LinearLayout.VERTICAL);
        time.setGravity(Gravity.END);
        clock = text("", layout.scale(59), HOME_WHITE, "sans-serif-medium");
        date = text("", layout.scale(27), SILVER, "sans-serif");
        clock.setGravity(Gravity.END);
        date.setGravity(Gravity.END);
        time.addView(clock);
        time.addView(date);
        settingsButton = new Button(this);
        settingsButton.setFocusableInTouchMode(true);
        settingsButton.setText(R.string.settings);
        settingsButton.setAllCaps(false);
        settingsButton.setTextColor(HOME_WHITE);
        settingsButton.setTextSize(layout.scale(31));
        settingsButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        settingsButton.setIncludeFontPadding(false);
        settingsButton.setMinWidth(0);
        settingsButton.setMinimumWidth(0);
        settingsButton.setPadding(dp(layout.scale(25)), 0, dp(layout.scale(25)), 0);
        settingsButton.setBackground(selector(10));
        settingsButton.setOnClickListener(view -> openSettings());
        settingsButton.setOnFocusChangeListener((view, focused) -> {
            if (focused && returnAnchor == null) state.focused = LauncherState.SETTINGS;
        });
        android.graphics.drawable.Drawable gear = getDrawable(R.drawable.settings_gear);
        int gearSize = dp(layout.scale(34));
        gear.setBounds(0, 0, gearSize, gearSize);
        settingsButton.setCompoundDrawablesRelative(gear, null, null, null);
        settingsButton.setCompoundDrawablePadding(dp(layout.scale(12)));
        LinearLayout.LayoutParams control = new LinearLayout.LayoutParams(-2, dp(layout.scale(64)));
        control.topMargin = dp(layout.scale(8));
        time.addView(settingsButton, control);
        masthead.addView(time);
        page.addView(masthead, new LinearLayout.LayoutParams(-1, -2));

        greeting = text("", layout.scale(63), HOME_WHITE, "sans-serif-medium");
        subtitle = text("", layout.scale(31), 0xffccd4df, "sans-serif");
        addWithGap(page, greeting, 0);
        addWithGap(page, subtitle, 2);

        entertainmentRow = new AppRow(R.string.entertainment);
        toolsRow = new AppRow(R.string.tools);
        addWithGap(page, entertainmentRow, layout.sectionGap);
        addWithGap(page, toolsRow, layout.rowGap);

        return page;
    }

    private void addWithGap(LinearLayout parent, View view, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(gap);
        parent.addView(view, params);
    }

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        int welcome = HomeContent.greeting(() -> now.get(Calendar.HOUR_OF_DAY));
        greeting.setText(GREETINGS[welcome]);
        subtitle.setText(SUBTITLES[welcome]);
        String skeleton = DateFormat.is24HourFormat(this) ? "Hm" : "hm";
        clock.setText(new java.text.SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
            .format(now.getTime()));
        date.setText(new java.text.SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEE MMM d"), locale)
            .format(now.getTime()));
    }

    private void refreshCatalog() {
        try {
            installed = repository.refresh();
            if (state.applyReferenceDefaults(android.os.Build.MANUFACTURER, android.os.Build.MODEL, installed)) {
                preferences.save(state);
            }
        } catch (RuntimeException unavailable) {
            installed = Collections.emptyList();
            notice(R.string.catalog_error);
        }
        render();
    }

    private void render() {
        String previous = state.focused;
        List<AppEntry> streaming = new ArrayList<>();
        List<AppEntry> tools = new ArrayList<>();
        for (AppEntry app : state.visible(installed, locale)) {
            (state.entertainment(app) ? streaming : tools).add(app);
        }
        boolean changed = entertainmentRow.update(streaming) | toolsRow.update(tools);
        if (changed || getCurrentFocus() == null) restoreCard(previous);
    }

    private void restoreCard(String component) {
        if (!entertainmentRow.focusComponent(component) && !toolsRow.focusComponent(component)) {
            settingsButton.requestFocus();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        View focus = getCurrentFocus();
        int key = event.getKeyCode();
        if (focus instanceof Card || focus == settingsButton) {
            boolean vertical = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
            boolean horizontal = focus instanceof Card
                && (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT);
            if (vertical || horizontal) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (horizontal) {
                        Card card = (Card) focus;
                        card.row.move(card, key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    } else if (focus == settingsButton) {
                        if (key == KeyEvent.KEYCODE_DPAD_DOWN && !entertainmentRow.restore()) toolsRow.restore();
                    } else {
                        Card card = (Card) focus;
                        if (key == KeyEvent.KEYCODE_DPAD_UP
                                && (card.row == entertainmentRow || !entertainmentRow.restore())) settingsButton.requestFocus();
                        if (key == KeyEvent.KEYCODE_DPAD_DOWN && card.row == entertainmentRow) toolsRow.restore();
                    }
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (getCurrentFocus() instanceof Card) settingsButton.requestFocus();
        else super.onBackPressed();
    }

    private void launch(String key) {
        state.focused = key;
        preferences.save(state);
        returnAnchor = key;
        if (!open(repository.intent(key))) returnAnchor = null;
    }

    private boolean open(Intent intent) {
        try {
            if (intent == null) throw new ActivityNotFoundException();
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            notice(R.string.launch_error);
            return false;
        }
    }

    private void openSettings() {
        menu(getString(R.string.settings), labels(R.string.manage, R.string.sort,
            state.focusMode ? R.string.disable_focus : R.string.enable_focus, R.string.language,
            R.string.appearance, R.string.developer, R.string.about, R.string.system_settings, R.string.reset), (dialog, index) -> {
                switch (index) {
                    case 0: manageApps(); break;
                    case 1: sorting(); break;
                    case 2: state.focusMode = !state.focusMode; saveAndRender(); break;
                    case 3: languages(); break;
                    case 4: appearance(); break;
                    case 5: information(R.string.developer, getString(R.string.developer_roadmap)); break;
                    case 6: showAbout(); break;
                    case 7: open(new Intent(Settings.ACTION_SETTINGS)); break;
                    case 8: confirmReset(); break;
                    default: throw new AssertionError(index);
                }
            });
    }

    private void manageApps() {
        List<AppEntry> ordered = state.ordered(installed, locale);
        if (ordered.isEmpty()) { notice(R.string.empty); return; }
        String[] names = new String[ordered.size()];
        for (int i = 0; i < names.length; i++) {
            AppEntry app = ordered.get(i);
            String name = app.label;
            if (state.favorites.contains(app.id)) name = getString(R.string.favorite_label, name);
            if (state.isHidden(app)) name = getString(R.string.hidden_label, name);
            names[i] = name;
        }
        menu(getString(R.string.manage), names, (dialog, i) -> appOptions(ordered.get(i)));
    }

    private void appOptions(AppEntry app) {
        menu(app.label, labels(state.favorites.contains(app.id) ? R.string.favorite_remove : R.string.favorite_add,
            state.isHidden(app) ? R.string.show : R.string.hide, R.string.earlier, R.string.later, R.string.category),
            (dialog, index) -> {
                switch (index) {
                    case 0: toggle(state.favorites, app.id); break;
                    case 1: state.toggleHidden(app); break;
                    case 2: state.move(installed, locale, app.id, -1); break;
                    case 3: state.move(installed, locale, app.id, 1); break;
                    case 4: categorize(app); return;
                    default: throw new AssertionError(index);
                }
                saveAndRender();
            });
    }

    private void categorize(AppEntry app) {
        menu(getString(R.string.category), labels(R.string.automatic, R.string.entertainment, R.string.tools),
            (dialog, index) -> {
                if (index == 0) state.categories.remove(app.id);
                else state.categories.put(app.id, index == 1);
                saveAndRender();
            });
    }

    private void appearance() {
        menu(getString(R.string.appearance), labels(R.string.spacious, R.string.compact), (dialog, index) -> {
            state.compactCards = index == 1;
            preferences.save(state);
            recreate();
        });
    }

    private void showAbout() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            information(R.string.about, getString(R.string.about_body, version));
        } catch (android.content.pm.PackageManager.NameNotFoundException unavailable) {
            notice(R.string.launch_error);
        }
    }

    private void information(int title, String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton(android.R.string.ok, null).show();
    }

    private static void toggle(Set<String> set, String id) {
        if (!set.remove(id)) set.add(id);
    }

    private void sorting() {
        menu(getString(R.string.sort), labels(R.string.custom, R.string.ascending, R.string.descending,
            R.string.restore), (dialog, index) -> {
                state.sort = new String[]{"custom", "az", "za", "custom"}[index];
                if (index == 3) state.order.clear();
                saveAndRender();
            });
    }

    private void languages() {
        menu(getString(R.string.language), labels(R.string.system, R.string.english, R.string.turkish,
            R.string.german), (dialog, index) -> {
                state.language = new String[]{LauncherState.SYSTEM, "en", "tr", "de"}[index];
                preferences.save(state);
                recreate();
            });
    }

    private void confirmReset() {
        new AlertDialog.Builder(this).setTitle(R.string.reset).setMessage(R.string.reset_message)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.confirm, (dialog, which) -> {
                state = new LauncherState();
                entertainmentRow.anchor = "";
                toolsRow.anchor = "";
                preferences.save(state);
                recreate();
            }).show();
    }

    private void saveAndRender() { preferences.save(state); render(); }

    private void menu(String title, String[] choices, DialogInterface.OnClickListener handler) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setItems(choices, handler).show();
        dialog.getListView().setFocusableInTouchMode(true);
        dialog.getListView().requestFocusFromTouch();
        dialog.getListView().setSelection(0);
    }

    private String[] labels(int... ids) {
        String[] result = new String[ids.length];
        for (int i = 0; i < ids.length; i++) result[i] = getString(ids[i]);
        return result;
    }

    private void notice(int message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private float density() { return getResources().getDisplayMetrics().density; }
    private int dp(int value) { return Math.round(value * density()); }

    private TextView text(String value, int size, int color, String font) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(font, Typeface.NORMAL));
        return view;
    }

    private GradientDrawable shape(int radius, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private android.graphics.drawable.StateListDrawable selector(int radius) {
        android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, shape(radius, FOCUS_SURFACE, ICE));
        background.addState(new int[]{android.R.attr.state_selected}, shape(radius, 0xff324d69, ICE));
        background.addState(new int[]{android.R.attr.state_activated}, shape(radius, 0xff293e54, Color.TRANSPARENT));
        background.addState(new int[]{}, shape(radius, SURFACE, Color.TRANSPARENT));
        return background;
    }

    private final class AppRow extends LinearLayout {
        final HorizontalScrollView scroll;
        final LinearLayout cards;
        final TextView empty;
        List<AppEntry> apps = Collections.emptyList();
        String anchor = "";
        int firstIcon;
        int lastIcon = -1;
        boolean initialized;

        AppRow(int title) {
            super(HomeActivity.this);
            setOrientation(VERTICAL);
            addView(text(getString(title), layout.scale(34), HOME_WHITE, "sans-serif-medium"));
            empty = text(getString(R.string.empty), 14, MUTED, "sans-serif");
            addWithGap(this, empty, 6);

            scroll = new HorizontalScrollView(HomeActivity.this);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scroll.setClipToPadding(false);
            scroll.setPadding(dp(3), dp(3), dp(3), dp(3));
            scroll.setFocusable(false);
            cards = new LinearLayout(HomeActivity.this);
            cards.setClipChildren(false);
            scroll.addView(cards, new FrameLayout.LayoutParams(-2, -1));
            scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> updateIcons());
            scroll.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> updateIcons());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(layout.cardHeight + 6));
            params.topMargin = dp(layout.headingGap);
            addView(scroll, params);
        }

        boolean update(List<AppEntry> items) {
            if (initialized && sameItems(items)) {
                // Package broadcasts can replace icons without changing the catalog.
                return false;
            }
            initialized = true;
            apps = items;
            firstIcon = 0;
            lastIcon = -1;
            cards.removeAllViews();
            for (AppEntry item : items) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(layout.cardWidth), -1);
                params.rightMargin = dp(layout.cardGap);
                cards.addView(new Card(this, item), params);
            }
            scroll.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            scroll.post(this::updateIcons);
            return true;
        }

        boolean sameItems(List<AppEntry> items) {
            if (items.size() != apps.size()) return false;
            for (int i = 0; i < items.size(); i++) {
                AppEntry old = apps.get(i);
                AppEntry next = items.get(i);
                Card card = (Card) cards.getChildAt(i);
                if (!old.id.equals(next.id) || !old.label.equals(next.label)
                        || card.favorite != state.favorites.contains(next.id)) return false;
            }
            return true;
        }

        void invalidateIcons() {
            for (int i = firstIcon; i <= lastIcon; i++) ((Card) cards.getChildAt(i)).showIcon(false);
            lastIcon = -1;
            updateIcons();
        }

        void updateIcons() {
            if (apps.isEmpty() || scroll.getWidth() == 0) return;
            int stride = dp(layout.cardWidth) + dp(layout.cardGap);
            int start = Math.min(apps.size() - 1, Math.max(0, scroll.getScrollX() / stride - 1));
            int end = Math.min(apps.size() - 1, (scroll.getScrollX() + scroll.getWidth()) / stride + 1);
            for (int i = firstIcon; i <= lastIcon; i++) {
                if (i < start || i > end) ((Card) cards.getChildAt(i)).showIcon(false);
            }
            for (int i = start; i <= end; i++) {
                if (i < firstIcon || i > lastIcon) ((Card) cards.getChildAt(i)).showIcon(true);
            }
            firstIcon = start;
            lastIcon = end;
        }

        boolean focusComponent(String component) {
            return focusAt(LauncherState.indexOf(apps, component));
        }

        boolean restore() {
            return focusAt(LauncherState.restore(apps, anchor, 0));
        }

        void move(Card current, int direction) {
            focusAt(cards.indexOfChild(current) + direction);
        }

        boolean focusAt(int index) {
            return index >= 0 && index < apps.size() && cards.getChildAt(index).requestFocus();
        }
    }

    private final class Card extends LinearLayout {
        final AppRow row;
        final AppEntry app;
        final ImageView icon;
        final boolean favorite;

        Card(AppRow row, AppEntry app) {
            super(HomeActivity.this);
            this.row = row;
            this.app = app;
            favorite = state.favorites.contains(app.id);
            setOrientation(VERTICAL);
            setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            setPadding(0, dp(HomeLayout.TILE_MARGIN), 0, 0);
            setClipToPadding(false);
            setDefaultFocusHighlightEnabled(false);
            setFocusableInTouchMode(true);
            setClickable(true);
            setContentDescription(favorite ? getString(R.string.favorite_label, app.label) : app.label);
            setOnClickListener(view -> launch(app.id));
            setOnLongClickListener(view -> { appOptions(app); return true; });
            FrameLayout tile = new FrameLayout(HomeActivity.this);
            int radius = Math.max(8, Math.round(layout.tile * 0.14f));
            GradientDrawable idleOutline = shape(radius, TILE_SURFACE, Color.TRANSPARENT);
            GradientDrawable focusOutline = shape(radius, TILE_SURFACE, Color.TRANSPARENT);
            focusOutline.setStroke(dp(2), 0xff69d1ff);
            tile.setBackground(idleOutline);
            addView(tile, new LayoutParams(dp(layout.tile), dp(layout.tile)));
            icon = new ImageView(HomeActivity.this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(icon, new FrameLayout.LayoutParams(dp(layout.icon), dp(layout.icon), Gravity.CENTER));
            TextView label = text(app.label, layout.labelSize, HOME_WHITE, "sans-serif-medium");
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            label.setVisibility(INVISIBLE);
            LayoutParams labelParams = new LayoutParams(-1, -2);
            labelParams.topMargin = dp(HomeLayout.LABEL_GAP);
            addView(label, labelParams);
            setOnFocusChangeListener((view, focused) -> {
                tile.setBackground(focused ? focusOutline : idleOutline);
                float scale = focused ? 1.03f : 1f;
                tile.animate().scaleX(scale).scaleY(scale).setDuration(100).start();
                label.setVisibility(focused ? VISIBLE : INVISIBLE);
                if (focused) {
                    row.anchor = app.id;
                    if (returnAnchor == null) state.focused = app.id;
                }
            });
        }

        void showIcon(boolean visible) {
            if (!visible) {
                icon.setImageDrawable(null);
                return;
            }
            Bitmap bitmap = repository.icon(app.id, Math.min(160, dp(layout.icon)));
            if (bitmap == null) icon.setImageResource(android.R.drawable.sym_def_app_icon);
            else icon.setImageBitmap(bitmap);
        }
    }
}
