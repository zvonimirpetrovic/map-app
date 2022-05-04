package com.maps.appmap;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    /**
     *
     * Methods for handling data in db
     *
     */
     /* TODO: make whole CRUD
            Create a class to handle database related methods*/

    public void saveDataToDb(Context context, String save){

        SQLiteDatabase db = context.openOrCreateDatabase("LCF",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");
        db.execSQL("INSERT INTO Routes VALUES(NULL, 'admin', '" + save + "', NULL);");
        db.close();
    }

    public String getLastRouteFromDb(Context context){
        String value = null;
        SQLiteDatabase db = context.openOrCreateDatabase("LCF",MODE_PRIVATE,null);

        String selectQuery = "SELECT EncodedRoute from Routes;";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToLast()){
            value = cursor.getString(0);
        }
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
        SQLiteDatabase db = context.openOrCreateDatabase("LCF", MODE_PRIVATE, null);
        byte[] byteAudio = null;

        db.execSQL("CREATE TABLE IF NOT EXISTS Routes(RoutesID integer primary key autoincrement, Username VARCHAR NOT NULL, EncodedRoute VARCHAR NOT NULL, Audio BLOB);");


        String selectQuery = "SELECT Audio from Routes WHERE EncodedRoute = '" + s + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
            byteAudio = cursor.getBlob(0);
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
        byte[] audioBlob = getAudioForSelectedRoute(context, pointsOnTheMapString);

        return audioBlob;
    }
}
