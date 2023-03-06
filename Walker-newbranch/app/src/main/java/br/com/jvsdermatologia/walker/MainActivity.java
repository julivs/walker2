package br.com.jvsdermatologia.walker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextPaint;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    final int CODE_PERMISSION = 1;
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView kcal;
    private TextView steps;
    private TextView count;
    private ImageView logo;
    private Switch switchSave;
    private final String saveSwitch = "saveTrack";
    private Button buttonStart;
    private Button buttonStop;
    private WalkState state = WalkState.initial;
    private Receiver receiver;
    private boolean dialogStopShown = false;

    private Timer timerBlink;
    private TimerTask timerTaskBlink;

    private Timer timerMarkTime;
    private TimerTask timerTaskMarkTime;
    private Long timeStarted;
    private Long timeStoped;

    private Timer timerBlinkButton;
    private TimerTask timerTaskBlinkButton;
    private boolean notStillSaved = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = findViewById(R.id.text1);
        textView2 = findViewById(R.id.text2);
        textView3 = findViewById(R.id.textView3);
        count = findViewById(R.id.textView2);
        kcal = findViewById(R.id.textViewKcal);
        steps = findViewById(R.id.textViewStep);
        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);
        switchSave = findViewById(R.id.switch1);

        notStillSaved = true;

        String saveTrack = Utils.getUtils().getOneLineStringDataFile(this, saveSwitch);
        if (saveTrack != null) switchSave.setChecked(Boolean.parseBoolean(saveTrack));

        switchSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) Utils.getUtils().updateOneLineStringDataFile(context, saveSwitch, "true");
                else Utils.getUtils().updateOneLineStringDataFile(context, saveSwitch, "false");
            }
        });

        logo = findViewById(R.id.imageViewLogo);

        AdView madView = findViewById(R.id.adView3);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        madView.loadAd(adRequest);

        try {
            logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("Walker2.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.getUtils().setLocationManager(context);

        addListener();

        loadVariables();

        count.setText(stringTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.policy, menu);
        return true;
    }

    public void onClickConfig(View view) {
        Intent intent = new Intent(context, ConfigActivity.class);
        startActivity(intent);
    }

    public void onClickPolicy(MenuItem item) {
        Intent intent = new Intent(context, PolicyActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        receiver = new Receiver();
        registerReceiver(receiver, new IntentFilter("br.com.jvsdermatologia.walker.STOP"));

        if (ServiceGPS.isInterrupted()) {
            notStillSaved = false;
            onClickStop(null);
            dialogInterrupted();
        }

        Utils.getUtils().setLinkToView(new CallbackView() {
            @Override
            public void doIt() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (!ServiceGPS.isInterrupted()) {
                            Information information = numbers();

                            // count.setText(information.time);
                            textView1.setText(information.dist);
                            textView2.setText(information.veloc);
                            kcal.setText(information.kcal);
                            steps.setText(information.steps);
                        }

                    }
                });
            }
        });

        if (!ServiceGPS.isInterrupted() || true) { // testando marcacao final dados
            Information information = numbers();

            // count.setText(information.time);
            textView1.setText(information.dist);
            textView2.setText(information.veloc);
            kcal.setText(information.kcal);
            steps.setText(information.steps);
        }

//        Toast.makeText(this, "Chamou resume - interrupted: " + Boolean.toString(ServiceGPS.isInterrupted()), Toast.LENGTH_LONG).show();
    }

    static class Information {
        String time;
        String dist;
        String veloc;
        String kcal;
        String steps;

        Information(String time, String dist, String veloc, String kcal, String steps) {
            this.time = time;
            this.dist = dist;
            this.veloc = veloc;
            this.kcal = kcal;
            this.steps = steps;
        }
    }

    private Information numbers() {
        double tempo = (double) ServiceGPS.getCounter() / 12.0;
        double dist = ServiceGPS.getDistance();
        double veloc = 0;
        if (tempo != 0) veloc = (dist * 60) / (tempo * 1000);

        int hours = (int) Math.floor(tempo / 60);
        int minutes = (int) Math.floor(tempo % 60);
        int sec = (int) Math.floor((tempo * 60) % 60);

        double altura = 170;
        double peso = 70;
        if (Utils.getUtils().getHeight() != null) {
            altura = Utils.getUtils().getHeight();
        }
        if (Utils.getUtils().getWeight() != null) {
            peso = Utils.getUtils().getWeight();
        }

        altura = altura / 100;
        double passos = dist / (0.43 * altura);

        // Calories burned per minute = (0.035 * body weight in kg) + ((Velocity in m/s ^ 2) / Height in m)) * (0.029) * (body weight in kg)

        double velocMs = veloc / 3.6;
        double calorias = (0.035 * peso) + ((velocMs * velocMs) / altura) * 0.029 * peso;
        calorias = calorias * tempo;

        return new Information(String.format("%02d : %02d : %02d", hours, minutes, sec), String.format("%.1f m", dist), String.format(getString(R.string.meanSpeed) + ": %.2f km/h", veloc),
                String.format("%.0f kcal", calorias), String.format("%.0f " + getString(R.string.steps), passos));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.getUtils().setLinkToView(null);

        unregisterReceiver(receiver);

    }

    private void addListener() {

        int dist = Utils.LOW_SENSIB_GPS;
        String gpsSensib = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getGpsSensibVar());
        if (gpsSensib != null) {
            if (Boolean.parseBoolean(gpsSensib)) dist = Utils.HIGH_SENSIB_GPS;
        }

        Utils.listenerError listenerError = Utils.getUtils().setListenerGPS(5000, dist, context);
//        System.out.println("---... error: " + listenerError);
        if (listenerError.equals(Utils.listenerError.noPermission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, CODE_PERMISSION);
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CODE_PERMISSION);
            }
        } else if (listenerError.equals(Utils.listenerError.noManager)) {
            Utils.getUtils().setLocationManager(context);
            addListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults.length == 2) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                        addListener();
                    else
                        makeDialog(getString(R.string.localDenied), getString(R.string.messageGPS));
                } else {
                    makeDialog(getString(R.string.localDenied), getString(R.string.messageGPS));
                }
            } else {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        addListener();
                    else
                        makeDialog(getString(R.string.localDenied), getString(R.string.messageGPS));
                } else {
                    makeDialog(getString(R.string.localDenied), getString(R.string.messageGPS));
                }
            }
        }
    }

    void makeDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void dialogInterrupted() {
        if (!dialogStopShown) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.serviceInterrupted))
                    .setTitle(getString(R.string.serviceInterruptedTitle))
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialogStopShown = true;
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void onClickStart(View view) {

        dialogStopShown = false;
        notStillSaved = true;
//        textView3.setText("   Distância:   ");

        switch (state) {

            case initial:
                makeDialogForStart(getString(R.string.avisoGPSTitulo), getString(R.string.avisoGPS), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ServiceGPS.setComput(true);
                        Intent intent = new Intent(context, ServiceGPS.class);
                        blinkTime(false);
                        blinkButton(true);
                        startService(intent);
                        buttonStart.setText(getString(R.string.pausar));
                        buttonStop.setText(getString(R.string.terminar));
                        state = WalkState.walking;
                        timeStarted = new Date().getTime();
                        markTime(true);
                    }
                });
                break;

            case walking:
                ServiceGPS.setComput(false);
                blinkTime(true);
                blinkButton(false);
                buttonStart.setText(getString(R.string.retomar));
                state = WalkState.paused;
                timeStoped = new Date().getTime();
                markTime(false);
                break;

            case paused:
                ServiceGPS.setComput(true);
                blinkTime(false);
                blinkButton(true);
                buttonStart.setText(getString(R.string.pausar));
                state = WalkState.walking;
                timeStarted = timeStarted + new Date().getTime() - timeStoped;
                markTime(true);
                timeStoped = null;
                break;

        }
    }

    void makeDialogForStart(String title, String message, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("Ok", onClickListener)
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    synchronized public void onClickStop(View view) {

        double dist = ServiceGPS.getDistance();
        double tempo = ServiceGPS.getCounter() * 5.0;
        long end = new Date().getTime();

        if (switchSave.isChecked() && dist > 0 && notStillSaved) { // --------------- limites previos: dist > 1000 && tempo > 10 * 60

            notStillSaved = false;
            Track track = Utils.getUtils().makeTrack(end, dist, tempo, Utils.getUtils().getLocationList());
            String trackJson = new Gson().toJson(track);
            String fileName = Long.toString(end);
            if (Utils.getUtils().saveTrack(this, fileName, trackJson)) Toast.makeText(this, getString(R.string.trackIncluded), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(context, ServiceGPS.class);
        blinkTime(true);
        blinkButton(false);
        stopService(intent);
        buttonStart.setText(getString(R.string.iniciar));
        buttonStop.setText(getString(R.string.terminou));
        state = WalkState.initial;
        timeStoped = null;
        markTime(false);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timeStarted = null;
            }
        }, 200);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(context, ServiceGPS.class);
        stopService(intent);
    }

    public void goMap(View view) {

        String tempoString;
        String distString;

        if (Utils.getUtils().getLocation() != null) {
            double tempo = (double) ServiceGPS.getCounter() / 12.0;
            double dist = ServiceGPS.getDistance();

            int hours = (int) Math.floor(tempo / 60);
            int minutes = (int) Math.floor(tempo % 60);

            tempoString = String.format("%d h %d m", hours, minutes);
            distString = String.format("%.1f m", dist);
        } else {
            tempoString = ("0 h 0 m");
            distString = ("0.0 m");
        }

            // System.out.println("location=== " + Utils.getUtils().getLocation());
            Intent intent = new Intent(context, MapsActivity.class);
            intent.putExtra("time", tempoString);
            intent.putExtra("dist", distString);
            startActivity(intent);

    }

    void blinkTime(boolean blink) {
        if (blink) {
            if (timerBlink == null) {
                timerTaskBlink = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (count.getCurrentTextColor() == Color.BLACK)
                                    count.setTextColor(Color.RED);
                                else count.setTextColor(Color.BLACK);
                            }
                        });
                    }
                };
                timerBlink = new Timer("TimerBlink");
                timerBlink.scheduleAtFixedRate(timerTaskBlink, 1000, 1000);
            }
        } else {
            if (timerBlink != null) {
                timerBlink.cancel();
                timerBlink = null;
            }
            count.setTextColor(Color.BLACK);
        }
    }

    void blinkButton(boolean blink) {
        if (blink) {
            if (timerBlinkButton == null) {
                timerTaskBlinkButton = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (buttonStart.getCurrentTextColor() == Color.BLACK)
                                    buttonStart.setTextColor(Color.RED);
                                else buttonStart.setTextColor(Color.BLACK);
                            }
                        });
                    }
                };
                timerBlinkButton = new Timer("TimerBlinkButton");
                timerBlinkButton.scheduleAtFixedRate(timerTaskBlinkButton, 1000, 1000);
            }
            Toast.makeText(this, getString(R.string.medidaAtivada), Toast.LENGTH_LONG).show();
        } else {
            if (timerBlinkButton != null) {
                timerBlinkButton.cancel();
                timerBlinkButton = null;
            }
            buttonStart.setTextColor(Color.BLACK);
        }
    }

    void markTime(boolean update) {
        if (update) {
            if (timerMarkTime == null) {
                timerTaskMarkTime = new TimerTask() {
                    @Override
                    public void run() {
                        String pre = count.getText().toString();
                        final String pos = stringTime();
                        if (!pos.equals(pre)){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    count.setText(pos);
                                }
                            });
                        }
                    }
                };
                timerMarkTime = new Timer("TimerBlinkButton");
                timerMarkTime.scheduleAtFixedRate(timerTaskMarkTime, 1000, 200);
            }
        } else {
            if (timerMarkTime != null) {
                timerMarkTime.cancel();
                timerMarkTime = null;
            }
        }
    }

    private String stringTime() {
        if (timeStarted != null) {
            double tempo;
            if (timeStoped == null) {
                tempo = (double) (new Date().getTime() - timeStarted) / 60000;
            } else {
                tempo = (double) (timeStoped - timeStarted) / 60000;
            }
            int hours = (int) Math.floor(tempo / 60);
            int minutes = (int) Math.floor(tempo % 60);
            int sec = (int) Math.floor((tempo * 60) % 60);
            return String.format("%02d : %02d : %02d", hours, minutes, sec);
        } else {
            return "00 : 00 : 00";
        }
    }

    private void loadVariables() {

        String height = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getHeightVar());
        String weight = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getWeightVar());
        String soundAlert = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getSoundAlertVar());
        String gpsSensib = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getGpsSensibVar());
        String turnOffActive = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getTurnOffActiveVar());
        String turnOffDist = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getTurnOffDistVar());

        try {
            if (height != null) {
                Utils.getUtils().setHeight(Integer.parseInt(height));
            }
            if (weight != null) {
                Utils.getUtils().setWeight(Integer.parseInt(weight));
            }
            if (turnOffDist != null) {
                Utils.getUtils().setDistanceTurnoff(Integer.parseInt(turnOffDist));
            }
            if (turnOffActive != null) {
                Utils.getUtils().setTurnoffDistActive(Boolean.parseBoolean(turnOffActive));
            }
            if (soundAlert != null) {
                Utils.getUtils().setMakeAlert(Boolean.parseBoolean(soundAlert));
            }
            if (gpsSensib != null) {
                Utils.getUtils().setHighSensibGPS(Boolean.parseBoolean(gpsSensib));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }

    synchronized private Bitmap getScreen() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            Canvas canvas = new Canvas(bitmap);

            Paint textTitles = new Paint();
            textTitles.setColor(Color.BLACK);
            textTitles.setTextSize(60);
            textTitles.setAntiAlias(true);

            Paint textValues = new Paint();
            textValues.setColor(Color.BLACK);
            textValues.setTextSize(50);
            textValues.setAntiAlias(true);

            Paint textValuesCenter = new Paint();
            textValuesCenter.setColor(Color.BLACK);
            textValuesCenter.setTextSize(50);
            textValuesCenter.setTextAlign(Paint.Align.CENTER);
            textValuesCenter.setAntiAlias(true);

            TextPaint textApp = new TextPaint();
            textApp.setColor(Color.argb(255, 150, 50, 0));
            textApp.setTextSize(80);
            textApp.setTextAlign(Paint.Align.CENTER);
            textApp.setTypeface(Typeface.create("Arial", Typeface.BOLD));
            textApp.setAntiAlias(true);

            Information information = numbers();

            canvas.drawText("Walker App", 520, 90, textApp);
            canvas.drawText(getString(R.string.distancia), 380, 190, textTitles);
            canvas.drawText(information.dist, 380, 270, textValues);
            canvas.drawText(information.steps, 40, 380, textValues);
            canvas.drawText(information.kcal, 440, 380, textValues);
            canvas.drawText(information.time, 400, 480, textValuesCenter);
            canvas.drawText(information.veloc, 400, 560, textValuesCenter);

            InputStream inputStream = getAssets().open("Walker2.png");
            Bitmap logo = BitmapFactory.decodeStream(inputStream);

            canvas.drawBitmap(logo, null, new Rect(40, 40, 294, 318), null);

            return bitmap;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    synchronized public void onClickShare(View view) {

        Toast.makeText(this, getString(R.string.sharingPrepare), Toast.LENGTH_SHORT).show();

        File path = new File(getFilesDir(), "images/");
        path.mkdir();
        File file = new File(path, "imageToShareWalker.png");
        if (file.exists()) file.delete();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {

            Bitmap bitmap = getScreen();

            if (bitmap != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                Uri savedImageURI = FileProvider.getUriForFile(this, "br.com.jvsdermatologia.walker.fileprovider", file);
                shareIntent.putExtra(Intent.EXTRA_STREAM, savedImageURI);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                System.out.println("---... " + savedImageURI.getEncodedPath());
                shareIntent.setType("image/png");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.sharing)));
            }

        } catch (IOException e) {
            e.printStackTrace();
//            System.out.println("---... Erro fileoutputstream");
        }
    }

    private enum WalkState {
        initial,
        walking,
        paused
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // System.out.println("---... received");
                notStillSaved = false;
                onClickStop(null);
                dialogInterrupted();
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        moveTaskToBack(true);
    }

}