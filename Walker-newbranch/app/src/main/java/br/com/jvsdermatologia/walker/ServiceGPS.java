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
import android.os.PowerManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
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
    private static boolean comput = true;
    private Utils.CircBuffer circBuffer;
    private static boolean interrupted = false;
    private PowerManager.WakeLock wakeLock;
    private final String saveSwitch = "saveTrack";
    private MediaPlayer mp;

    private final int ciclesToStop = 360;
    private final int maxDistToStop = 960;

    public static boolean isInterrupted() {
        return interrupted;
    }

    public static void setComput(boolean comput) {
        ServiceGPS.comput = comput;
    }

    public static boolean isComput() {
        return comput;
    }

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

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Walker::ServiceWakelock");

        circBuffer = new Utils.CircBuffer(ciclesToStop);
        interrupted = false;

        counter = 0;
        distance = 0d;
        preDistance = 0d;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Walker", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.walk)
                .setContentTitle("Walker")
                .setContentText(getString(R.string.monitoring))
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

        Utils.getUtils().getLocationList().clear();
        Utils.getUtils().setLocationUser(new LocationUser() {
            @Override
            public void changedLocation(Location location) {
                Utils.getUtils().getLocationList().add(location);
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeLock.acquire();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            wakeLock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopForeground(true);
        this.location = null;
//        counter = 0;
//        distance = 0d;
//        preDistance = 0d;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        Utils.getUtils().setLocationUser(null);

        // System.out.println("timer=== ondestroy");
    }

    private void performAction() {
        if (comput) {
            location = Utils.getUtils().getLocation();
            // System.out.println("---... location: " + location);
            if (preLocation != null) {
                preDistance = distance;
                double lastDistance = location.distanceTo(preLocation);
                distance += lastDistance;
                circBuffer.insertValue(lastDistance);
//                System.out.println("---... sum: " + circBuffer.getSum() + " / last: " + lastDistance);

                if (Math.floor((double)preDistance / 500) < Math.floor((double)distance / 500)) {
                    try {
                        playSound(distance);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Double sum = circBuffer.getSum();
                boolean distReached =  stopDist(distance);
                if ((counter > ciclesToStop && sum < maxDistToStop && circBuffer.sumLast(5) < 20) || distReached) {
                    interrupted = true;
                    wakeLock.release();
                    stopForeground(true);
                    this.location = null;
//                    counter = 0;
//                    distance = 0d;
//                    preDistance = 0d;
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    Utils.getUtils().setLocationUser(null);

                    boolean saveTrack = Boolean.parseBoolean(Utils.getUtils().getOneLineStringDataFile(this, saveSwitch));

                    if (saveTrack) {
                        long end;
                        Track track;
                        if (!distReached) {
                            end = (new Date().getTime()) - ciclesToStop * 5000;
                            track = Utils.getUtils().makeTrack(end, distance, (counter - ciclesToStop) * 5.0, Utils.getUtils().getLocationList());
                        } else {
                            end = new Date().getTime();
                            track = Utils.getUtils().makeTrack(end, distance, counter * 5.0, Utils.getUtils().getLocationList());
                        }
                        String trackJson = new Gson().toJson(track);
                        String fileName = Long.toString(end);
                        Utils.getUtils().saveTrack(this, fileName, trackJson);
                    }

                    sendMessage();
                    playSoundStopService();
                }
            }
            preLocation = location;
            if (linkToView != null && !interrupted) linkToView.doIt();
            counter ++;
        } else {
            preLocation = null;
        }
    }

    private boolean stopDist(Double distance) {

        int margem = 0;

        boolean turnoffActive = Utils.getUtils().getTurnoffDistActive() == null ? false : Utils.getUtils().getTurnoffDistActive();
        if (turnoffActive) {
            return Utils.getUtils().getDistanceTurnoff() == null ? false : (distance - margem) >= Utils.getUtils().getDistanceTurnoff();
        }
        return false;
    }

    private void playSound(Double distance) throws IOException {
        Integer value = (int) Math.floor(distance / 100);

        boolean soundAllowed = Utils.getUtils().getMakeAlert() == null ? true : Utils.getUtils().getMakeAlert();

        if (value >= 5 && value <= 100 && soundAllowed) {
//            MediaPlayer mp = new MediaPlayer();
                AssetFileDescriptor descriptor;
                if (Locale.getDefault().getLanguage().equals("pt")) {
                    descriptor = getAssets().openFd(value + "mil.mp3");
                } else {
                    descriptor = getAssets().openFd(value + "milEng.m4a");
                }
                mp = new MediaPlayer();
                mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                mp.prepare();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mp.release();
                        mp = null;
                    }
                });
                mp.start();
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.walk)
                .setContentTitle("Walker")
                .setContentText(getString(R.string.alcancou) + " " + Integer.toString(value * 100) + " m.")
                .setContentIntent(PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void sendMessage() {
        Intent intent = new Intent();
        intent.setAction("br.com.jvsdermatologia.walker.STOP");
        sendBroadcast(intent);
    }

    private void playSoundStopService(){

        boolean soundAllowed = Utils.getUtils().getMakeAlert() == null ? true : Utils.getUtils().getMakeAlert();

        if (soundAllowed) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    while (true) {
                        if (mp == null) {
                            try {
                                AssetFileDescriptor descriptor;
                                if (Locale.getDefault().getLanguage().equals("pt")) {
                                    descriptor = getAssets().openFd("ended.mp3");
                                } else {
                                    descriptor = getAssets().openFd("endedEng.mp3");
                                }
                                mp = new MediaPlayer();
                                mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                                mp.prepare();
                                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mediaPlayer) {
                                        mp.release();
                                        mp = null;
                                    }
                                });
                                mp.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                            count ++;
                            if (count > 20) break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

}
