package com.maps.appmap;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* TODO: make whole CRUD
        Handle database name, table names and field names dynamically*/

public class DatabaseHelper {
    /**
     *
     * Methods for handling data in db
     *
     */
    public static void saveDataToDb(Context context, String save){

        SQLiteDatabase db = context.openOrCreateDatabase("LCF",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");
        db.execSQL("INSERT INTO Routes VALUES(NULL, 'admin', '" + save + "', NULL);");
        db.close();
    }

    public static String getLastRouteFromDb(Context context){
        String value = null;
        SQLiteDatabase db = context.openOrCreateDatabase("LCF",MODE_PRIVATE,null);

        String selectQuery = "SELECT EncodedRoute from Routes;";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToLast()){
            value = cursor.getString(0);
        }

        cursor.close();

        return value;
    }

    public static List<String> getAllRoutesFromDb(Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);
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

    public static byte[] getAudioForSelectedRoute(Context context, String s){
        byte[] byteAudio = null;

        SQLiteDatabase db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");

        String selectQuery = "SELECT Audio from Routes WHERE EncodedRoute = '" + s + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
            byteAudio = cursor.getBlob(0);

        cursor.close();

        return byteAudio;
    }

    public static void deleteRoutesWithNoAudio(Context context) {

        SQLiteDatabase db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");
        db.delete("Routes", "Audio IS NULL", null);
        db.close();
    }

    public static byte[] getAudioFromDb(Context context, Polyline polyline){
        // Get LatLng points from clicked polyline
        List<LatLng> pointsOnTheMap = polyline.getPoints();

        // Encode retrieved points to string
        String pointsOnTheMapString = PolyUtil.encode(pointsOnTheMap);

        // Get audio for a route based on encoded route field

        return getAudioForSelectedRoute(context, pointsOnTheMapString);
    }

    // Method to save audio file to database
    public static void saveAudioToDb(Context context, Activity activity){

        SQLiteDatabase db;
        byte[] byteAudio;

        db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);

        try
        {
            FileInputStream instream = new FileInputStream(context.getExternalCacheDir().getAbsolutePath() + "/AudioRecording.3gp");
            BufferedInputStream bif = new BufferedInputStream(instream);
            byteAudio = new byte[bif.available()];

            ContentValues newAudio = new ContentValues();
            newAudio.put("Audio", byteAudio);
            int maxId = getLastRouteIdFromDb(context);
            long ret = db.update("Routes", newAudio, "RoutesID = " + maxId, null);
            if(ret>0){
                Toast.makeText(
                        activity,
                        "\r\n Audio was successfully added to database! \r\n",
                        Toast.LENGTH_LONG).show();
            }
            else Toast.makeText(
                    activity,
                    "\r\n Error add audio failed! \r\n",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e)
        {
            Toast.makeText(
                    activity,
                    "\r\n!!! Error: " + e+"!!!\r\n",
                    Toast.LENGTH_LONG).show();
        }

        db.close();
    }

    public static int getLastRouteIdFromDb(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        int maxRow = 0;
        String selectQuery = "SELECT RoutesID from Routes;";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToLast()) {
            maxRow = cursor.getInt(0);
        }
        cursor.close();
        db.close();

        return maxRow;
    }
}

