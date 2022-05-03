package com.maps.appmap;

import static android.graphics.Color.RED;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.maps.appmap.databinding.ActivityMapsUserBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MapsUserActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsUserBinding binding;
    public static final String TAG = "detailsFragment";

    // Variable for saving route in db
    private String pointsToSave = null;
    private ArrayList<LatLng> pointsToLoad = new ArrayList<LatLng>();
    private List<String> savedRoutes = null;
    private Polyline polylineToLoad;
    private PolylineOptions mPolylineOptions;
    private byte[] audioFile = null;

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

        // Setup for map options and permissions
        setUpMap();

        // Delete all routes which have no audio
        deleteRoutesWithNoAudio();

        // Getting saved routes from db
        savedRoutes = getAllRoutesFromDb();
        if(savedRoutes != null) {
            drawSavedPolylines(savedRoutes);
        }

        // ClickListener for playing audio when polyline is clicked
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {

                audioFile = retrieveAudioFromDb(polyline);

                // Enable button for playing audio
                findViewById(R.id.btnPlay).setVisibility(View.VISIBLE);
            }
        });

        // ClickListener for btnPlay
        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Play audio
                playAudioFromDb(audioFile);
                findViewById(R.id.btnPlay).setVisibility(View.GONE);
                findViewById(R.id.btnStopPlay).setVisibility(View.VISIBLE);
            }
        });

        // ClickListener for btnStopPlay
        findViewById(R.id.btnStopPlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Pause audio
                pauseAudioFromDb();
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
     * Method for initial map setup
     * Handles permissions and sets camera options
     */
    private void setUpMap() {
        // Check Permissions
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{(Manifest.permission.ACCESS_FINE_LOCATION)},
                    34 //REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            );

        } else {

            // Map Options
            mMap.setMyLocationEnabled(true);

            // Handle location for android 11 and above (Build.VERSION_CODES.R)
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ContextCompat.getMainExecutor(this), new Consumer<Location>() {
                    @Override
                    public void accept(Location location) {
                        double longitude = location.getLongitude();
                        double latitude = location.getLatitude();
                        LatLng startLatLng = new LatLng(latitude, longitude);
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                        mMap.getUiSettings().setZoomControlsEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    }
                });
            } else {
                // Handle location for android previous releases
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListenerGPS(), null);
            }

            // Avoid assigning a null to location
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                LatLng startLatLng = new LatLng(90, 90);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
                Toast.makeText(
                       this,
                       "Couldn't get location from GPS",
                       Toast.LENGTH_LONG).show();
            }
        }
    }

    // For requestSingleUpdate() function (version < Android 11)
    public class LocationListenerGPS implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            LatLng startLatLng = new LatLng(latitude, longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (Log.isLoggable("DialerProvider", Log.VERBOSE)) {
                Log.v("DialerProvider", "onProviderDisabled: " + provider);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (Log.isLoggable("DialerProvider", Log.VERBOSE)) {
                Log.v("DialerProvider", "onProviderEnabled: " + provider);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (Log.isLoggable("DialerProvider", Log.VERBOSE)) {
                Log.v("DialerProvider", "onStatusChanged: " + provider + ", " + status + ", " + extras);
            }
        }
    }

    // TODO: Create a class to handle database related methods
    private List<String> getAllRoutesFromDb() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");


        String selectQuery = "SELECT EncodedRoute from Routes;";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(0);

                list.add(name);
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();

        return list;
    }

    // Draw saved routes on the map
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

    private byte[] getAudioForSelectedRoute(String s){
        SQLiteDatabase db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        byte[] byteAudio = null;

        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");


        String selectQuery = "SELECT Audio from Routes WHERE EncodedRoute = '" + s + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
            byteAudio = cursor.getBlob(0);
        return byteAudio;
    }

    private void playAudioFromDb(byte[] audio){
        File file = null;
        FileOutputStream fos;

        try {
            // Check Permissions
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                // Request permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{(Manifest.permission.WRITE_EXTERNAL_STORAGE)},
                        0 //REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                );

            file = File.createTempFile("sound", ".3gp");
            fos = new FileOutputStream(file);
            fos.write(audio);
            fos.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        mp = MediaPlayer.create(this, Uri.fromFile(file));
        mp.start();
    }

    private void pauseAudioFromDb(){
        mp.release();
        mp = null;
    }

    private void deleteRoutesWithNoAudio() {
        SQLiteDatabase db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");

        db.delete("Routes", "Audio IS NULL", null);
    }

    private byte[] retrieveAudioFromDb(Polyline polyline){
        // Get LatLng points from clicked polyline
        List<LatLng> pointsOnTheMap = polyline.getPoints();

        // Encode retrieved points to string
        String pointsOnTheMapString = PolyUtil.encode(pointsOnTheMap);

        // Get audio for a route based on encoded route field
        byte[] audioBlob = getAudioForSelectedRoute(pointsOnTheMapString);

        return audioBlob;
    }
}