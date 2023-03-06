package br.com.jvsdermatologia.walker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;

public class SplashActivity extends AppCompatActivity {

    private Context context;
    private boolean started = false;
    private ImageView imageView;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imageView = findViewById(R.id.imageViewSplash);

        if (!started) {
            try {
                imageView.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("Walker2.png")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        context = this;

        runnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent (context, MainActivity.class);
                finish();
                startActivity(intent);
            }
        };

        handler = new Handler();
        handler.postDelayed(runnable, 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (started) {
            Intent intent = new Intent (context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
        }

        started = true;
    }

    public void dismissSplash(View view) {

        if (handler != null) handler.removeCallbacks(runnable);

        Intent intent = new Intent (context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(intent);
    }
}
