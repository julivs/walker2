package br.com.jvsdermatologia.walker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceGPS extends Service {

    public static int serviceId = 1;
    public  static String CHANNEL_ID = "br.com.jvsdermatologia.walker";
    public static int NOTIFICATION_ID = 1;
    private Location location;
    private Timer timer;
    private static Integer counter = 0;
    private CallbackView linkToView;
    private Location preLocation;
    private static Double distance = 0d;
    private static Double preDistance = 0d;

    public ServiceGPS() {
    }

    public static Integer getCounter() {
        return counter;
    }

    public static Double getDistance() {
        return distance;
    }

    public static Double getPreDistance() {
        return preDistance;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Walker", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.walk)
                .setContentTitle("Walker")
                .setContentText("Acompanhamento da caminhada ativo.")
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        startForeground(serviceId, builder.build());
        this.location = Utils.getUtils().getLocation();
        this.linkToView = Utils.getUtils().getLinkToView();

        if (timer == null) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    performAction();
                }
            };
            timer = new Timer("Timer");
            timer.scheduleAtFixedRate(timerTask, 5 * 1000, 5 * 1000);
        }

        if (linkToView != null) linkToView.doIt();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        this.location = null;
        counter = 0;
        distance = 0d;
        preDistance = 0d;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        // System.out.println("timer=== ondestroy");
    }

    private void performAction() {
        location = Utils.getUtils().getLocation();
        if (preLocation != null) {
            preDistance = distance;
            distance += location.distanceTo(preLocation);
        }
        if (Math.floor((double)preDistance / 500) < Math.floor((double)distance / 500)) {
            try {
                playSound(distance);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        preLocation = location;
        if (linkToView != null) linkToView.doIt();
        counter ++;
    }

    private void playSound(Double distance) throws IOException {
        Integer value = (int) Math.floor(distance / 100);
        if (value >= 5 && value <= 100) {
            MediaPlayer mp = new MediaPlayer();
            AssetFileDescriptor descriptor = getAssets().openFd(value + "mil.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            mp.prepare();
            mp.start();
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.walk)
                .setContentTitle("Walker")
                .setContentText("Alcançou " + Integer.toString(value * 100) + " metros.")
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
