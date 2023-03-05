package br.com.jvsdermatologia.walker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    final int CODE_PERMISSION = 1;
    private TextView textView1;
    private TextView textView2;
    private TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = findViewById(R.id.text1);
        textView2 = findViewById(R.id.text2);
        count = findViewById(R.id.textView2);

        Utils.getUtils().setLocationManager(context);

        addListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Utils.getUtils().setLinkToView(new CallbackView() {
            @Override
            public void doIt() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        double tempo = (double)ServiceGPS.getCounter() / 12.0;
                        double dist = ServiceGPS.getDistance();
                        double veloc = 0;
                        if (tempo != 0) veloc = (dist * 60) / (tempo * 1000);

                        int hours = (int) Math.floor(tempo / 60);
                        int minutes = (int) Math.floor(tempo % 60);

                        count.setText(String.format("%d h %d m", hours, minutes));
                        textView1.setText(String.format("%.1f metros", dist));
                        textView2.setText(String.format("%.2f km/h", veloc));
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.getUtils().setLinkToView(null);
    }

    private void addListener(){

        class listener implements LocationListener {

            @Override
            public void onLocationChanged(Location location) {
                Utils.getUtils().setLocationChanged(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }

        }

        Utils.listenerError listenerError = Utils.getUtils().setListenerGPS(new listener(), 5000, 15, context);
        if (listenerError.equals(Utils.listenerError.noPermission)) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, CODE_PERMISSION);
        } else if (listenerError.equals(Utils.listenerError.noManager)) {
            Utils.getUtils().setLocationManager(context);
            addListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_PERMISSION) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) addListener();
            } else {
                makeDialog("Erro", "É necessário o uso da localização por GPS neste aplicativo.");
            }
        } else {
            makeDialog("Erro", "É necessário o uso da localização por GPS neste aplicativo.");
        }
    }

    void makeDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onClickStart(View view) {
        Intent intent = new Intent(context, ServiceGPS.class);
        startService(intent);
    }

    public void onClickStop(View view) {
        Intent intent = new Intent(context, ServiceGPS.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(context, ServiceGPS.class);
        stopService(intent);
    }
}
