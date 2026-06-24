package eu.hxreborn.downloadsim;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        EditText interval = findViewById(R.id.interval);
        EditText step = findViewById(R.id.step);
        EditText count = findViewById(R.id.count);

        findViewById(R.id.btn_slow).setOnClickListener(v -> start(1, 2000L, 1));
        findViewById(R.id.btn_fast).setOnClickListener(v -> start(6, 500L, 1));
        findViewById(R.id.btn_two).setOnClickListener(v -> start(2, 1500L, 2));
        findViewById(R.id.btn_custom).setOnClickListener(v ->
                start(read(step, 2), read(interval, 1500), read(count, 1)));
    }

    private void start(int step, long interval, int count) {
        startForegroundService(new Intent(this, DownloadSimService.class)
                .putExtra(DownloadSimService.EXTRA_STEP, step)
                .putExtra(DownloadSimService.EXTRA_INTERVAL, interval)
                .putExtra(DownloadSimService.EXTRA_COUNT, count));
    }

    private int read(EditText field, int fallback) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
