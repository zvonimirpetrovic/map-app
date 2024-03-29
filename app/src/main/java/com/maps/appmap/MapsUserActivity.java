package com.maps.appmap;

import static android.graphics.Color.RED;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.maps.appmap.databinding.ActivityMapsUserBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapsUserActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsUserBinding binding;

    // Variables for loading routes and audio from db
    private ArrayList<LatLng> pointsToLoad = new ArrayList<>();
    private List<String> savedRoutes = null;
    private Polyline polylineToLoad;
    private PolylineOptions mPolylineOptions;
    private byte[] audioFile = null;
    private String pointsOnTheMapString;

    // MediaPlayer
    private MediaPlayer mp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapuser);
        mapFragment.getMapAsync(this);

        // Disable button for playing and pausing audio
        findViewById(R.id.btnPlay).setVisibility(View.GONE);
        findViewById(R.id.btnStopPlay).setVisibility(View.GONE);

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
        Context context = this;

        // Setup for map options and permissions
        MapHandler.setUpMap(this, MapsUserActivity.this, mMap);

        // Getting saved routes from db
        savedRoutes = DatabaseHelper.getAllRoutesFromDb(this);
        drawSavedPolylines(savedRoutes);

        // ClickListener for playing audio when polyline is clicked
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {
                AudioHandler.resetAudio(MapsUserActivity.this, mp);

                // Get LatLng points from clicked polyline
                List<LatLng> pointsOnTheMap = polyline.getPoints();

                // Encode retrieved points to string
                pointsOnTheMapString = PolyUtil.encode(pointsOnTheMap);

                // Enable button for playing audio
                findViewById(R.id.btnPlay).setVisibility(View.VISIBLE);
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                AudioHandler.resetAudio(MapsUserActivity.this, mp);
            }
        });

        // ClickListener for btnPlay
        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Play audio
                // Get audio for a route based on encoded route field
                String audioPath = DatabaseHelper.getAudioPathForSelectedRoute(context, pointsOnTheMapString);

                // Play audio
                AudioHandler.playAudioFromDb(context, MapsUserActivity.this, mp, audioPath);

                findViewById(R.id.btnPlay).setVisibility(View.GONE);
                findViewById(R.id.btnStopPlay).setVisibility(View.VISIBLE);
            }
        });

        // ClickListener for btnStopPlay
        findViewById(R.id.btnStopPlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Pause audio
                AudioHandler.pauseAudioFromDb(mp);
                findViewById(R.id.btnStopPlay).setVisibility(View.GONE);
                findViewById(R.id.btnPlay).setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    /**
     * Draw saved routes on the map
     *
     */
    private void drawSavedPolylines(List<String> l){

        // Iterate through passed list of lat/lng strings and decode it to geo points
        // to load on the map
        for (int i = 0; i < l.size(); i++) {

            pointsToLoad = (ArrayList<LatLng>) PolyUtil.decode(l.get(i));
            mPolylineOptions = new PolylineOptions();
            mPolylineOptions.color(RED);
            mPolylineOptions.width(10);
            polylineToLoad = mMap.addPolyline(mPolylineOptions);
            polylineToLoad.setPoints(pointsToLoad);
            polylineToLoad.setClickable(true);
        }
    }
}