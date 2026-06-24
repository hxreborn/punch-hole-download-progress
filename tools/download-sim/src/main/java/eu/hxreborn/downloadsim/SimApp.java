package eu.hxreborn.downloadsim;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class SimApp extends Application {
    public static final String CHANNEL_ID = "downloadsim.progress";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Download simulation",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Fake download progress for PHDP ring testing");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
