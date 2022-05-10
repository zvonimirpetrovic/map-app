package com.maps.appmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class MapHandler {

    /**
     * Method for initial map setup
     * Handles permissions and sets camera options
     */
    public static void setUpMap(Context context, Activity activity, GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{(Manifest.permission.ACCESS_FINE_LOCATION)},
                    34 //REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            );

        } else {

            // Map Options
            map.setMyLocationEnabled(true);

            // Handle location for android 11 and above (Build.VERSION_CODES.R)
            LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ContextCompat.getMainExecutor(context), location -> {
                });
            } else {
                // Handle location for android previous releases
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListenerGPS(), null);
            }

            // Avoid assigning a null to location
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                LatLng startLatLng = new LatLng(90, 90);
                map.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
                Toast.makeText(
                        activity,
                        "Couldn't get location from GPS",
                        Toast.LENGTH_LONG).show();
            }
            else{
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                LatLng startLatLng = new LatLng(latitude, longitude);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(startLatLng, 18);
                map.animateCamera(cameraUpdate);
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }
    }

    // LocationListener for requestSingleUpdate() function (version < Android 11)
    public static class LocationListenerGPS implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
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
}
