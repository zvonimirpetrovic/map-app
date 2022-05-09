package com.maps.appmap;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

/* TODO: make whole CRUD
        Handle database name, table names and field names dynamically
        Find out which audio format suits best(currently 3gp)*/

public class DatabaseHelper {
    /**
     *
     * Variables for handling data in db
     *
     */
    private static final String DB_NAME = "LCF";
    private static final String TABLE_Routes = "Routes";
    private static final String KEY_Routes_RoutesID = "RoutesID";
    private static final String KEY_Routes_Username = "Username";
    private static final String KEY_Routes_EncodedRoute = "EncodedRoute";
    private static final String KEY_Routes_AudioPath = "AudioPath";

    /**
     *
     * Methods for handling data in db
     *
     */
    public static SQLiteDatabase createDb(Context context){
        SQLiteDatabase db = context.openOrCreateDatabase(DB_NAME,MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_Routes + "("
                + KEY_Routes_RoutesID + " integer primary key autoincrement,"
                + KEY_Routes_Username + " VARCHAR NOT NULL, "
                + KEY_Routes_EncodedRoute + " VARCHAR NOT NULL, "
                + KEY_Routes_AudioPath + " VARCHAR);");

        return db;
    }

    public static void saveDataToDb(Context context, String save){

        SQLiteDatabase db = createDb(context);

        db.execSQL("INSERT INTO Routes VALUES(NULL, 'admin', '" + save + "', NULL);");
        db.close();
    }

    public static String getLastRouteFromDb(Context context){
        String value = null;
        SQLiteDatabase db = createDb(context);

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
        SQLiteDatabase db = createDb(context);

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

    public static void deleteRoutesWithNoAudio(Context context) {

        SQLiteDatabase db = createDb(context);

        db.delete("Routes", "AudioPath IS NULL", null);
        db.close();
    }

    // Method to save audio file to database
    public static void saveAudioToDb(Context context, Activity activity){

        String audioPath;
        int maxId = getLastRouteIdFromDb(context);

        SQLiteDatabase db = createDb(context);

        audioPath = context.getExternalCacheDir().getAbsolutePath() + "/AudioRecording" + getLastRouteIdFromDb(context) + ".3gp";

        ContentValues newAudioPath = new ContentValues();
        newAudioPath.put("AudioPath", audioPath);

        long ret = db.update("Routes", newAudioPath, "RoutesID = " + maxId, null);
        if(ret>0){
            Toast.makeText(
                    activity,
                    "\r\n Audio was successfully added to database! \r\n",
                    Toast.LENGTH_LONG).show();
            }
        db.close();
    }


    public static int getLastRouteIdFromDb(Context context) {

        SQLiteDatabase db = createDb(context);

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

    public static int getRouteIdForSelectedRoute(Context context, String s){
        int routeId = 0;

        SQLiteDatabase db = createDb(context);

        String selectQuery = "SELECT RoutesId from Routes WHERE EncodedRoute = '" + s + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
            routeId = cursor.getInt(0);

        cursor.close();

        return routeId;
    }

    public static String getAudioPathForSelectedRoute(Context context, String s){
        String audioPath = null;

        SQLiteDatabase db = createDb(context);

        String selectQuery = "SELECT AudioPath from Routes WHERE EncodedRoute = '" + s + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
            audioPath = cursor.getString(0);

        cursor.close();

        return audioPath;
    }

// Method to save route and path to audio to database
    public static void saveAudioAndRouteToDb(Context context, Activity activity, String save){

        String audioPath;

        SQLiteDatabase db = createDb(context);

        audioPath = context.getExternalCacheDir().getAbsolutePath() + "/AudioRecording" + getLastRouteIdFromDb(context) + ".3gp";
        db.execSQL("INSERT INTO Routes VALUES(NULL, 'admin', '" + save + "', '" + audioPath + "')");

        db.close();
        }
}

