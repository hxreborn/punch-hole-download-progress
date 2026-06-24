package eu.hxreborn.downloadsim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class DownloadSimService extends Service {
    public static final String ACTION_STOP = "stop";
    public static final String EXTRA_STEP = "step";
    public static final String EXTRA_INTERVAL = "interval";
    public static final String EXTRA_COUNT = "count";

    private static final int NOTIF_BASE = 4242;
    private static final int MAX_DOWNLOADS = 5;
    private static final String[] FILES = {
            "ubuntu-24.04.2-desktop-amd64.iso",
            "fedora-workstation-40-1.14.iso",
            "debian-12.5.0-amd64-netinst.iso",
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int step = 2;
    private long interval = 1500L;
    private int count = 1;
    private int[] percent = {1};

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAll(nm);
            return START_NOT_STICKY;
        }
        if (intent != null) {
            step = Math.max(1, intent.getIntExtra(EXTRA_STEP, step));
            interval = Math.max(100L, intent.getLongExtra(EXTRA_INTERVAL, interval));
            count = Math.max(1, Math.min(MAX_DOWNLOADS, intent.getIntExtra(EXTRA_COUNT, count)));
        }
        percent = new int[count];
        for (int i = 0; i < count; i++) percent[i] = 1 + i * 15;
        for (int i = count; i < MAX_DOWNLOADS; i++) nm.cancel(NOTIF_BASE + i);
        startForeground(NOTIF_BASE, build(0, percent[0]),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        handler.removeCallbacksAndMessages(null);
        handler.post(tick);
        return START_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            NotificationManager nm = getSystemService(NotificationManager.class);
            for (int i = 0; i < count; i++) {
                percent[i] = percent[i] >= 99 ? 1 : percent[i] + step;
                nm.notify(NOTIF_BASE + i, build(i, percent[i]));
            }
            handler.postDelayed(this, interval);
        }
    };

    private Notification build(int index, int p) {
        PendingIntent cancel = PendingIntent.getService(
                this, 0,
                new Intent(this, DownloadSimService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, SimApp.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(fileName(index))
                .setContentText("Downloading " + p + "%")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, p, false)
                .addAction(0, "Cancel", cancel)
                .build();
    }

    private String fileName(int index) {
        return index < FILES.length ? FILES[index] : "download-" + (index + 1) + ".bin";
    }

    private void stopAll(NotificationManager nm) {
        handler.removeCallbacksAndMessages(null);
        for (int i = 0; i < MAX_DOWNLOADS; i++) nm.cancel(NOTIF_BASE + i);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
