package com.maps.appmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;

import androidx.core.app.ActivityCompat;

import java.io.File;

public class AudioHandler {

    /**
     * Method for playing saved audio
     *
     */
    public static void playAudioFromDb(Context context, Activity activity, MediaPlayer mediaPlayer, String audioPath) {
        File file = new File(audioPath);

        // Check Permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            // Request permission
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{(Manifest.permission.WRITE_EXTERNAL_STORAGE)},
                    0 //REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            );


        mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file));

        mediaPlayer.start();
    }

    /**
     * Method for pausing audio
     *
     */
    public static void pauseAudioFromDb(MediaPlayer mediaPlayer){

        if(mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Method for reseting audio and hiding pause button
     *
     */
    public static void resetAudio(Activity activity, MediaPlayer mediaPlayer){

        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        activity.findViewById(R.id.btnStopPlay).setVisibility(View.GONE);
        activity.findViewById(R.id.btnPlay).setVisibility(View.GONE);
    }
}
