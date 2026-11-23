package org.ersinpro.launcher;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Runs on a disposable emulator under the debug application's identity. */
public final class RegressionInstrumentation extends Instrumentation {
    private boolean capture;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        capture = arguments != null && "true".equals(arguments.getString("capture"));
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context target = getTargetContext();
            preferenceRoundTrip(target);
            bootClaim(target);
            home(target);
            result.putString("stream", "\nERSINPRO_INSTRUMENTATION_PASS\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "\n" + android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void bootClaim(Context target) {
        require(BootReceiver.isBootAction(Intent.ACTION_LOCKED_BOOT_COMPLETED), "early boot action");
        require(BootReceiver.isBootAction(Intent.ACTION_BOOT_COMPLETED), "ordinary boot fallback action");
        require(!BootReceiver.isBootAction(Intent.ACTION_USER_UNLOCKED), "unrelated action is ignored");
        SharedPreferences state = target.getSharedPreferences("regression_boot", Context.MODE_PRIVATE);
        require(state.edit().clear().commit(), "reset boot test state");
        require(!BootReceiver.claimBoot(state, -1), "unknown boot count skips launch");
        require(BootReceiver.claimBoot(state, 12), "first delivery claims boot");
        require(!BootReceiver.claimBoot(state, 12), "duplicate delivery skips launch");
        require(BootReceiver.claimBoot(state, 13), "new boot permits one launch");
    }

    private void preferenceRoundTrip(Context target) {
        SharedPreferences raw = target.getSharedPreferences("regression", Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        Preferences preferences = new Preferences(raw);
        LauncherState state = preferences.load();
        require("system".equals(state.language), "default locale");
        state.favorites.add("example/.Media");
        state.hidden.add("example/.Browser");
        state.order.add("example/.Media");
        state.language = "tr";
        state.focusMode = true;
        state.categories.put("example/.Media", true);
        state.compactCards = true;
        state.kodiDefaultApplied = true;
        state.kodiHidden = false;
        preferences.save(state);
        LauncherState saved = new Preferences(raw).load();
        require(saved.favorites.equals(state.favorites), "favorites persistence");
        require(saved.hidden.equals(state.hidden), "hidden persistence");
        require(saved.categories.equals(state.categories) && saved.compactCards, "category and appearance persistence");
        require(saved.kodiDefaultApplied && Boolean.FALSE.equals(saved.kodiHidden), "reference default marker persists after unhide");
        require(saved.order.equals(state.order), "ordering persistence");
        require(saved.focusMode && "tr".equals(saved.language), "mode and language persistence");
        raw.edit().putString("configuration", "{broken").commit();
        Preferences corrupt = new Preferences(raw);
        LauncherState recovered = corrupt.load();
        require(corrupt.recovered && recovered.favorites.isEmpty() && !recovered.focusMode, "corrupt configuration");
        raw.edit().clear().commit();
    }

    private void home(Context target) {
        SharedPreferences data = target.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        ApplicationRepository repository = new ApplicationRepository(target.getPackageManager(), target.getPackageName());
        LauncherState seed = new LauncherState();
        seed.language = "en";
        List<AppEntry> apps = seed.visible(repository.refresh(), java.util.Locale.ENGLISH);
        require(apps.size() > 2, "test emulator has launchable applications");
        AppEntry first = apps.get(0);
        require(first.label.equals("A ErsinPro Test Player"), "deterministic player fixture");
        String stream = "org.ersinpro.launcher.test/org.ersinpro.launcher.test.StreamFixture";
        require(LauncherState.indexOf(apps, stream) >= 0, "streaming fixture is installed");
        seed.categories.put(stream, true);
        new Preferences(data).save(seed);
        Intent launch = new Intent(target, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(launch);
        waitForIdleSync();
        awaitLayout(activity);
        String className = ComponentName.unflattenFromString(first.id).getClassName();
        ActivityMonitor launchMonitor = addMonitor(className, null, true);
        java.util.ArrayList<HorizontalScrollView> rows = new java.util.ArrayList<>();
        checkMainThread(() -> {
            View content = activity.findViewById(android.R.id.content);
            Button settings = (Button) findText(content, "Settings", Button.class);
            require(settings != null && settings.hasFocus(), "initial settings focus");
            require(findText(content, "ErsinPro", TextView.class) != null, "home branding");
            require(findText(content, "Entertainment", TextView.class) != null
                && findText(content, "Apps & Tools", TextView.class) != null, "both English category headings");
            findRows(content, rows);
            require(rows.size() == 2, "two horizontal scrollers");
            for (HorizontalScrollView row : rows) {
                int count = ((ViewGroup) row.getChildAt(0)).getChildCount();
                require(row.getVisibility() == View.VISIBLE && row.getWidth() > 0 && count > 0,
                    "app row should be shown, visibility=" + row.getVisibility() + ", cards=" + count);
                Rect visible = new Rect();
                require(row.getGlobalVisibleRect(visible) && visible.height() == row.getHeight(),
                    "entire app row fits vertically: " + visible.height() + "/" + row.getHeight()
                        + ", content height=" + content.getHeight());
            }
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            require(activity.getCurrentFocus() != settings && !first.label.contentEquals(activity.getCurrentFocus().getContentDescription()), "header enters entertainment");
            checkFocusedLabel(activity);
        });
        if (capture) {
            checkMainThread(() -> {
                int cards = ((ViewGroup) rows.get(0).getChildAt(0)).getChildCount();
                for (int i = 1; i < Math.min(cards, 3); i++) key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                checkFocusedLabel(activity);
            });
            captureHome(activity);
        }
        checkMainThread(() -> {
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            require(first.label.contentEquals(activity.getCurrentFocus().getContentDescription()), "remote enters tools");
            for (HorizontalScrollView row : rows) {
                ViewGroup cards = (ViewGroup) row.getChildAt(0);
                for (int i = 0; i < cards.getChildCount(); i++) {
                    ViewGroup card = (ViewGroup) cards.getChildAt(i);
                    View label = card.getChildAt(1);
                    require(label.getVisibility() == (card.hasFocus() ? View.VISIBLE : View.INVISIBLE),
                        "only the focused card shows its label");
                }
            }
        });
        waitForIdleSync();
        checkMainThread(() -> checkFocusedLabel(activity));
        checkMainThread(() -> {
            View settings = findText(activity.findViewById(android.R.id.content), "Settings", Button.class);
            key(activity, KeyEvent.KEYCODE_DPAD_UP);
            require(activity.getCurrentFocus() != settings
                && !first.label.contentEquals(activity.getCurrentFocus().getContentDescription()), "remote enters entertainment");
            key(activity, KeyEvent.KEYCODE_DPAD_UP);
            require(settings.hasFocus(), "entertainment returns to header");
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            require(first.label.contentEquals(activity.getCurrentFocus().getContentDescription()), "tools row restores anchor");
            View player = activity.getCurrentFocus();
            ViewGroup tools = (ViewGroup) rows.get(1).getChildAt(0);
            require(tools.getChildCount() > 1 && tools.getWidth() > rows.get(1).getWidth(), "tools row overflows");
            for (int step = 1; step < tools.getChildCount(); step++) key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
            require(activity.getCurrentFocus() == tools.getChildAt(tools.getChildCount() - 1)
                && rows.get(1).getScrollX() > 0 && rows.get(0).getVisibility() == View.VISIBLE, "remote scrolls only selected row");
            player.requestFocus();
            player.performClick();
            require(launchMonitor.getHits() == 1, "selection dispatches expected app intent");
            require(first.id.equals(new Preferences(data).load().focused), "selected app is saved");
            activity.finish();
        });
        removeMonitor(launchMonitor);
    }

    private void awaitLayout(Activity activity) {
        java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        long deadline = android.os.SystemClock.uptimeMillis() + 15_000;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            checkMainThread(() -> {
                View content = activity.findViewById(android.R.id.content);
                ready.set(content.isLaidOut() && content.getWidth() > 0 && content.getHeight() > 0);
            });
            if (ready.get()) return;
            android.os.SystemClock.sleep(100);
        }
        throw new AssertionError("Home layout did not complete");
    }

    private void checkFocusedLabel(Activity activity) {
        ViewGroup card = (ViewGroup) activity.getCurrentFocus();
        TextView label = (TextView) card.getChildAt(1);
        Rect visible = new Rect();
        require(label.getVisibility() == View.VISIBLE && label.getGlobalVisibleRect(visible)
            && label.getHeight() == visible.height() && label.getWidth() == visible.width(),
            "focused label fits horizontally and vertically");
    }

    private void captureHome(Activity activity) {
        File directory = activity.getExternalFilesDir(null);
        require(directory != null, "screenshot directory");
        int[] location = new int[2];
        checkMainThread(() -> {
            View tile = ((ViewGroup) activity.getCurrentFocus()).getChildAt(0);
            tile.getLocationOnScreen(location);
            location[1] += tile.getHeight() / 2;
        });
        // A software-rendered emulator can return an older frame after focus changes.
        for (int attempt = 0; attempt < 12; attempt++) {
            android.os.SystemClock.sleep(250);
            Bitmap image = getUiAutomation().takeScreenshot();
            if (image == null) continue;
            try {
                if (!hasFocusStroke(image, location)) continue;
                saveScreenshot(directory, image);
                return;
            } finally {
                image.recycle();
            }
        }
        throw new AssertionError("Screenshot did not show the focused tile");
    }

    private boolean hasFocusStroke(Bitmap image, int[] location) {
        for (int x = Math.max(0, location[0] - 6); x < Math.min(image.getWidth(), location[0] + 7); x++) {
            int pixel = image.getPixel(x, location[1]);
            if ((pixel & 0x00ffffff) == 0x0069d1ff) return true;
        }
        return false;
    }

    private void saveScreenshot(File directory, Bitmap image) {
        try (FileOutputStream stream = new FileOutputStream(new File(directory, "ersinpro-home.png"))) {
            require(image.compress(Bitmap.CompressFormat.PNG, 100, stream), "save screenshot");
        } catch (IOException error) {
            throw new IllegalStateException("Save screenshot failed", error);
        }
    }

    private void checkMainThread(Runnable check) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
            new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> {
            try {
                check.run();
            } catch (Throwable problem) {
                failure.set(problem);
            }
        });
        if (failure.get() != null) throw new AssertionError("Home checks failed", failure.get());
    }

    private static View findText(View view, String label, Class<? extends TextView> type) {
        if (type.isInstance(view) && label.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = findText(group.getChildAt(i), label, type);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void key(Activity activity, int code) {
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private static void findRows(View view, java.util.List<HorizontalScrollView> rows) {
        if (view instanceof HorizontalScrollView) rows.add((HorizontalScrollView) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) findRows(group.getChildAt(i), rows);
        }
    }

    private static void require(boolean passed, String test) {
        if (!passed) throw new AssertionError(test);
    }
}
