package br.com.jvsdermatologia.walker;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polyline polyline;
    private TextView time;
    private TextView dist;
    private Track track;
    private boolean sourceList;
    private ImageView imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        time = findViewById(R.id.textTime);
        dist = findViewById(R.id.textDistance);
        imageButton = findViewById(R.id.imageButton);
        AdView madView = findViewById(R.id.adView);
        AdView madView2 = findViewById(R.id.adView2);

        Intent intent = getIntent();
        sourceList = intent.getBooleanExtra("fromList", false);
        if (Utils.getUtils().getLocation() == null && !sourceList) onClickList(null);

        try {
            imageButton.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("ic_history.webp")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        madView.loadAd(adRequest);
        madView2.loadAd(adRequest);

        if (!sourceList) {
            imageButton.setVisibility(View.VISIBLE);
            String tempoString = intent.getStringExtra("time");
            String distString = intent.getStringExtra("dist");
            time.setText(tempoString);
            dist.setText(distString);
        } else {
            imageButton.setVisibility(View.INVISIBLE);
            track = ListTracks.getSentTrack();
            double tempo = track.getTime() / 60.0;
            double distance = track.getDistance();
            int hours = (int) Math.floor(tempo / 60);
            int minutes = (int) Math.floor(tempo % 60);
            String tempoString = String.format("%d h %d m", hours, minutes);
            String distString = String.format("%.1f m", distance);
            time.setText(tempoString);
            dist.setText(distString);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        Location location = Utils.getUtils().getLocation();
        if (location != null) {
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        }
        if (sourceList) makeLine2(mMap);
        else makeLine(mMap);
        mMap.setMinZoomPreference(15);
        mMap.setMaxZoomPreference(20);
    }

    private void makeLine(GoogleMap mMap) {
        List<Location> list = Utils.getUtils().getLocationList();

        PolylineOptions polylineOptions = new PolylineOptions();

        if (list.size() > 1) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (int i = 0; i < list.size(); i++) {
                LatLng latLng = new LatLng(list.get(i).getLatitude(), list.get(i).getLongitude());
                polylineOptions.add(latLng);
                builder.include(latLng);
            }

            polylineOptions.color(Color.RED)
                    .width(7);
            polyline = mMap.addPolyline(polylineOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));

        }

    }

    private void makeLine2(GoogleMap mMap) {

        List<Double[]> points = track.getPoints();

        PolylineOptions polylineOptions = new PolylineOptions();

        if (points.size() > 1) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (int i = 0; i < points.size(); i++) {
                LatLng latLng = new LatLng(points.get(i)[0], points.get(i)[1]);
                polylineOptions.add(latLng);
                builder.include(latLng);
            }

            polylineOptions.color(Color.RED)
                    .width(7);
            polyline = mMap.addPolyline(polylineOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (polyline != null) polyline.remove();

    }

    public void onClickList(View view) {
        Intent intent = new Intent(this, ListTracks.class);
        finish();
        startActivity(intent);
    }
}
