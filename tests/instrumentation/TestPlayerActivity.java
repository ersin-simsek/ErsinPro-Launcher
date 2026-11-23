package org.ersinpro.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** A separate task used to exercise a real launch and Back return. */
public final class TestPlayerActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView playback = new TextView(this);
        playback.setGravity(Gravity.CENTER);
        playback.setText("Test playback");
        playback.setTextColor(0xffffffff);
        playback.setBackgroundColor(0xff000000);
        setContentView(playback);
    }
}
