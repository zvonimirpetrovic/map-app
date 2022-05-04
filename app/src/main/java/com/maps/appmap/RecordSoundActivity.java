package com.maps.appmap;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class RecordSoundActivity extends AppCompatActivity {

    // Initializing all variables..
    private TextView startTV, stopTV, playTV, stopplayTV, statusTV;
    private Button buttonSaveAudio;

    // creating a variable for media recorder
    private MediaRecorder mRecorder;

    // creating a variable for mediaplayer and file name
    private MediaPlayer mPlayer;
    private static String mFileName = null;

    // constant for storing audio permission
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sound_recording);

        // initialize all variables with their layout items.
        statusTV = findViewById(R.id.idTVstatus);
        startTV = findViewById(R.id.btnRecord);
        stopTV = findViewById(R.id.btnStop);
        playTV = findViewById(R.id.btnPlay);
        stopplayTV = findViewById(R.id.btnStopPlay);
        buttonSaveAudio = findViewById(R.id.buttonSaveRecording);
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        playTV.setBackgroundColor(getResources().getColor(R.color.gray));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));
        buttonSaveAudio.setBackgroundColor(getResources().getColor(R.color.gray));

        startTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start recording method will
                // start the recording of audio.
                startRecording();
            }
        });
        stopTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause Recording method will
                // pause the recording of audio.
                pauseRecording();
            }
        });
        playTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // play audio method will play
                // the audio which we have recorded
                playAudio();
            }
        });
        stopplayTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause play method will
                // pause the play of audio

                pausePlaying();
            }
        });

        stopplayTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause play method will
                // pause the play of audio
                pausePlaying();
            }
        });

        buttonSaveAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveAudioToDb();

                Intent intent = new Intent(RecordSoundActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
    }


    private void startRecording() {
        // check permission method is used to check
        // that the user has granted permission
        // to record and store the audio.
        if (CheckPermissions()) {

            // setbackgroundcolor method will change
            // the background color of text view.
            stopTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
            startTV.setBackgroundColor(getResources().getColor(R.color.gray));
            playTV.setBackgroundColor(getResources().getColor(R.color.gray));
            stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));

            // filename variable with the path of the recorded audio file
//            mFileName = Environment.getExternalStorageDirectory().getPath();
            mFileName = getExternalCacheDir().getAbsolutePath();
            mFileName += "/AudioRecording.3gp";

            // initialize the media recorder class
            mRecorder = new MediaRecorder();

            // initialize the audio
            // source which we are using a mic.
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // set the output format of the audio.
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            // set the audio encoder for recorded audio
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // set the output file location for recorded audio
            mRecorder.setOutputFile(mFileName);
            try {
                // prepare audio recorder class
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            }
            // start the audio recording
            mRecorder.start();
            statusTV.setText("Recording Started");
        } else {
            // if audio recording permissions are
            // not granted by user below method will
            // ask for runtime permission for mic and storage.
            RequestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // this method is called when user
        // grants the permission for audio recording.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermissions() {
        // check permission
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        // request permission for audio recording and storage
        ActivityCompat.requestPermissions(RecordSoundActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    // TODO: Find solution to the bug in which a user can click
    // multiple times on play and that many times will reproduce
    public void playAudio() {
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        startTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        playTV.setBackgroundColor(getResources().getColor(R.color.gray));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.purple_200));

        // Initialize media player class
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);

            // prepare media player
            mPlayer.prepare();

            // start media player.
            mPlayer.start();
            statusTV.setText("Recording Started Playing");
        } catch (IOException e) {
            Log.e("TAG", "prepare() failed");
        }
    }

    public void pauseRecording() {
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        startTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        playTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.purple_200));

        // stop audio recording
        mRecorder.stop();

        // reset recorder
        mRecorder.reset();

        // release media recorder class
        mRecorder.release();
        mRecorder = null;
        statusTV.setText("Recording Stopped");
    }

    public void pausePlaying() {
        // release the media player class and pause the playing of our recorded audio.
        mPlayer.release();
        mPlayer = null;
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        startTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        playTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));
        statusTV.setText("Recording Play Stopped");
    }

    private int getLastRouteIdFromDb() {
        SQLiteDatabase db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);
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

    // Method to save audio file to database
    private void saveAudioToDb(){

        SQLiteDatabase db;
        byte[] byteAudio;

        db = openOrCreateDatabase("LCF", MODE_PRIVATE, null);

        try
        {
            FileInputStream instream = new FileInputStream(getExternalCacheDir().getAbsolutePath() + "/AudioRecording.3gp");
            BufferedInputStream bif = new BufferedInputStream(instream);
            byteAudio = new byte[bif.available()];
            bif.read(byteAudio);

            ContentValues newAudio = new ContentValues();
            newAudio.put("Audio", byteAudio);
            int maxId = getLastRouteIdFromDb();
            long ret = db.update("Routes", newAudio, "RoutesID = " + maxId, null);
            if(ret>0){
                Toast.makeText(
                        RecordSoundActivity.this,
                        "\r\n Audio was successfully added to database! \r\n",
                        Toast.LENGTH_LONG).show();
            }
            else Toast.makeText(
                    RecordSoundActivity.this,
                    "\r\n Error add audio failed! \r\n",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e)
        {
            Toast.makeText(
                    RecordSoundActivity.this,
                    "\r\n!!! Error: " + e+"!!!\r\n",
                    Toast.LENGTH_LONG).show();
        }

        db.close();
    }
}