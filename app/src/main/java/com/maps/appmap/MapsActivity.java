package com.maps.appmap;

import static android.graphics.Color.RED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
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
import com.maps.appmap.databinding.ActivityMapsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Consumer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    public static final String TAG = "detailsFragment";

    // Drawing by click variables
    private boolean drawing = false;
    private Polyline polyline;
    private List<LatLng> points = new ArrayList<>();

    // Drawing by touch variables
    private View mMapShelterView;
    private GestureDetector mGestureDetector;
    private ArrayList<LatLng> mLatlngs = new ArrayList<LatLng>();
    private PolylineOptions mPolylineOptions;
    // flag to differentiate whether user is touching to draw or not
    private boolean mDrawFinished = false;
    // Variable for saving route in db
    private String pointsToSave = null;
    private ArrayList<LatLng> pointsToLoad = new ArrayList<LatLng>();
    private List<String> savedRoutes = null;
    private Polyline polylineToLoad;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Disable button for stopping drawing
        findViewById(R.id.buttonEndDraw).setVisibility(View.GONE);
        findViewById(R.id.buttonSave).setVisibility(View.GONE);

        mMapShelterView = findViewById(R.id.drawer_view);
        mGestureDetector = new GestureDetector(this, new GestureListener());
        mMapShelterView.setOnTouchListener(this::onTouch);

    }

    /**
     * Gesture listener class
     *
     */
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            return false;
        }
    }

    /**
     * Ontouch event will draw poly line along the touch points
     *
     */
    private static final int MAX_CLICK_DURATION = 200;
    private long startClickTime = 0;

    public boolean onTouch(View v, MotionEvent event) {


        int X1 = (int) event.getX();
        int Y1 = (int) event.getY();
        Point point = new Point();
        point.x = X1;
        point.y = Y1;
        LatLng firstGeoPoint = mMap.getProjection().fromScreenLocation(
                point);
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startClickTime = Calendar.getInstance().getTimeInMillis();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mDrawFinished) {
                    X1 = (int) event.getX();
                    Y1 = (int) event.getY();
                    point = new Point();
                    point.x = X1;
                    point.y = Y1;
                    LatLng geoPoint = mMap.getProjection()
                            .fromScreenLocation(point);
                    mLatlngs.add(geoPoint);
                    pointsToSave = PolyUtil.encode(mLatlngs);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(RED);
                    mPolylineOptions.width(10);
                    mPolylineOptions.addAll(mLatlngs);
                    mMap.addPolyline(mPolylineOptions);
                }
                break;
            case MotionEvent.ACTION_UP:
                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                if (clickDuration < MAX_CLICK_DURATION) {
                    //click event has occurred
                    enableDrawing();
                }
                Log.d(TAG, "Points array size " + mLatlngs.size());
                mLatlngs.add(firstGeoPoint);
                mPolylineOptions = null;
                mMapShelterView.setVisibility(View.GONE);
                mMap.getUiSettings().setZoomGesturesEnabled(true);
                mMap.getUiSettings().setAllGesturesEnabled(true);

/*                mPolygonOptions = new PolygonOptions();
                mPolygonOptions.fillColor(Color.GRAY);
                mPolygonOptions.strokeColor(RED);
                mPolygonOptions.strokeWidth(5);
                mPolygonOptions.addAll(mLatlngs);
                mMap.addPolygon(mPolygonOptions);*/
                mDrawFinished = false;
                break;
        }
        return mGestureDetector.onTouchEvent(event);
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

        MotionEvent event = null;

       // ClickListener for buttonStartDraw
        findViewById(R.id.buttonStartDraw).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Start drawing????????????? TODO:Choose 1 or fuze them
                enableDrawing();
                drawZone(findViewById(R.id.drawer_view));

                // Delete all routes which have no audio
                deleteRoutesWithNoAudio();

                // Getting saved routes from db
                savedRoutes = getAllRoutesFromDb();
                if(savedRoutes != null) {
                    drawSavedPolylines(savedRoutes);
                }

                mMap.getUiSettings().setScrollGesturesEnabled(false);
                findViewById(R.id.buttonStartDraw).setVisibility(View.GONE);
                findViewById(R.id.buttonEndDraw).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonSave).setVisibility(View.VISIBLE);
            }
        });

        // ClickListener for buttonEndDraw
        findViewById(R.id.buttonEndDraw).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mMap.clear();
                // Getting saved routes from db
                savedRoutes = getAllRoutesFromDb();
                if(savedRoutes != null) {
                    drawSavedPolylines(savedRoutes);
                }
                mMap.setOnMapClickListener(null);
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                findViewById(R.id.buttonStartDraw).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonEndDraw).setVisibility(View.GONE);
                findViewById(R.id.buttonSave).setVisibility(View.GONE);

                drawing = false; // So that deleting previous polyline would work
                mDrawFinished = false;
            }
        });

        // ClickListener for buttonSave
        findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Do only if there is something to save to db
                if(pointsToSave == null){

                    // Inform user that there is no route to save
                    Toast toast = Toast.makeText(
                            MapsActivity.this,
                            "There is no route to save to the database",
                            Toast.LENGTH_LONG);
                    // TODO: setGravity() function (find better solution)
                    //  Warning: Starting from Android Build.VERSION_CODES#R,
                    //  for apps targeting API level Build.VERSION_CODES#R or higher,
                    //  this method (setGravity) is a no-op when called on text toasts.
                    toast.setGravity(Gravity.TOP,0,0);
                    toast.show();
                }
                else {
                    mMap.setOnMapClickListener(null);
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                    drawing = false; // So that deleting previous polyline would work

                    // Save route to DB
                    saveDataToDb(pointsToSave);

                    // Navigation handled by switching activity
                    changeActivity();
                }
            }
        });

        // ClickListener for playing audio when polyline is clicked
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {
                // Get LatLng points from clicked polyline
                List<LatLng> pointsOnTheMap = polyline.getPoints();

                // Encode retrieved points to string
                String pointsOnTheMapString = PolyUtil.encode(pointsOnTheMap);

                // Get audio for a route based on encoded route field
                byte[] audioBlob = getAudioForSelectedRoute(pointsOnTheMapString);

                // Play audio
                playAudioFromDb(audioBlob);
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

                    }
                });
            } else {
                // Handle location for android previous releases
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, (LocationListener) this, null);
            }

            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Avoid assigning a null to location
            if (location != null) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                LatLng startLatLng = new LatLng(latitude, longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
            } else {
                LatLng startLatLng = new LatLng(90, 90);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
            }
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        }

    }

    /**
     * Drawing method onClick (point-to-point)
     *
     */
    public void enableDrawing(){

        // Delete the previous path
        if(polyline != null)
        {
            polyline.remove();
            points.clear();
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // Latitude and longitude of a click
/*                Toast.makeText(
                        MapsActivity.this,
                        "Lat : " + point.latitude + " , "
                                + "Long : " + point.longitude,
                        Toast.LENGTH_LONG).show();*/

                points.add(point);
                pointsToSave = PolyUtil.encode(points);

                if(!drawing) {

                    // Drawing lines
                    PolylineOptions rectOptions = new PolylineOptions()
                            .visible(true)
                            .width(10)
                            .color(RED)
                            .add(point);


                    polyline = mMap.addPolyline(rectOptions);
                    drawing = true;
                }
                else {
                    polyline.setPoints(points);
                }
            }
        });
    }

         /**
         * Method gets called on tap of draw button, It prepares the screen to draw
         * the custom polyline
         *
         * @param view
         */

        public void drawZone(View view) {
            mMap.clear();
            mLatlngs.clear();
            mPolylineOptions = null;
            mDrawFinished = true;
            mMapShelterView.setVisibility(View.VISIBLE);
            mMap.getUiSettings().setScrollGesturesEnabled(false);
        }

        public void setGestures(boolean b){
            mMap.getUiSettings().setScrollGesturesEnabled(b);
        }

        public void setOnMapClickListenerToNull(){
            mMap.setOnMapClickListener(null);
        }

        public void clearMap(){
            mMap.clear();
        }

        public void setDrawing(boolean b){
            drawing = b;
        }

        private void changeActivity(){

            Intent intent = new Intent(this, RecordSoundActivity.class);
            startActivity(intent);
        }

    /**
     *
     * Methods for handling data in db
     *
     */
    // TODO: make whole CRUD

    private void saveDataToDb(String save){

        SQLiteDatabase db = openOrCreateDatabase("LCF",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");
        db.execSQL("INSERT INTO Routes VALUES(NULL, 'admin', '" + save + "', NULL);");
    }

    private void dropDb(){
        this.deleteDatabase("LCF");
    }

    private String getLastRouteFromDb(){
        String value = null;
        SQLiteDatabase db = openOrCreateDatabase("LCF",MODE_PRIVATE,null);

        String selectQuery = "SELECT EncodedRoute from Routes;";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToLast()){
            value = cursor.getString(0);
        }
        return value;
    }

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
        MediaPlayer mp;

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

    private void deleteRoutesWithNoAudio() {
        SQLiteDatabase db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        db.delete("Routes", "Audio IS NULL", null);
    }
}