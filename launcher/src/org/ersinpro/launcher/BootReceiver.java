package org.ersinpro.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "ErsinProBoot";
    private static final String LAST_BOOT = "last_boot";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!isBootAction(action)) return;
        Log.i(TAG, action + " received at elapsedMs=" + SystemClock.elapsedRealtime());
        try {
            UserManager users = context.getSystemService(UserManager.class);
            // HomeActivity reads credential-protected settings as soon as it starts.
            if (users == null || !users.isUserUnlocked()) {
                Log.i(TAG, "User storage unavailable; deferring to BOOT_COMPLETED");
                return;
            }
            int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
            if (count < 0) {
                Log.w(TAG, "Boot count unavailable; launch skipped");
                return;
            }
            if (!claimBoot(context.getSharedPreferences("boot", Context.MODE_PRIVATE), count)) {
                Log.i(TAG, "Boot already claimed or claim could not be saved: " + count);
                return;
            }

            Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.i(TAG, "Attempting HOME start for boot " + count + " at elapsedMs=" + SystemClock.elapsedRealtime());
            context.startActivity(home);
            Log.i(TAG, "HOME start request returned");
        } catch (RuntimeException problem) {
            Log.w(TAG, "HOME start failed", problem);
        }
    }

    static boolean isBootAction(String action) {
        return Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action);
    }

    static boolean claimBoot(SharedPreferences state, int count) {
        return count >= 0 && state.getInt(LAST_BOOT, -1) != count
            && state.edit().putInt(LAST_BOOT, count).commit();
    }
}
