package com.maps.appmap;

import android.content.Context;
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
        Context context = this;

        setContentView(R.layout.activity_sound_recording);

        // initialize all variables with their layout items.
        statusTV = findViewById(R.id.idTVstatus);
        startTV = findViewById(R.id.btnRecord);
        stopTV = findViewById(R.id.btnStop);
        playTV = findViewById(R.id.btnPlay);
        stopplayTV = findViewById(R.id.btnStopPlay);
        buttonSaveAudio = findViewById(R.id.buttonSaveRecording);

        // Set colors for buttons
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        playTV.setBackgroundColor(getResources().getColor(R.color.gray));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));
        buttonSaveAudio.setBackgroundColor(getResources().getColor(R.color.gray));

        // Setting clickable states for buttons
        startTV.setClickable(true);
        stopTV.setClickable(false);
        playTV.setClickable(false);
        stopplayTV.setClickable(false);
        buttonSaveAudio.setEnabled(false);

        startTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start recording method will
                // start the recording of audio.
                startRecording();

                // Setting clickable states for buttons
                startTV.setClickable(false);
                stopTV.setClickable(true);
                playTV.setClickable(false);
                stopplayTV.setClickable(false);
                buttonSaveAudio.setEnabled(false);
            }
        });
        stopTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause Recording method will
                // pause the recording of audio.
                pauseRecording();

                // Setting clickable states for buttons
                startTV.setClickable(true);
                stopTV.setClickable(false);
                playTV.setClickable(true);
                stopplayTV.setClickable(false);
                buttonSaveAudio.setEnabled(true);
            }
        });
        playTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // play audio method will play
                // the audio which we have recorded
                playAudio();

                // Setting clickable states for buttons
                startTV.setClickable(true);
                stopTV.setClickable(false);
                playTV.setClickable(false);
                stopplayTV.setClickable(true);
                buttonSaveAudio.setEnabled(true);
            }
        });
        stopplayTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause play method will
                // pause the play of audio
                pausePlaying();

                // Setting clickable states for buttons
                startTV.setClickable(true);
                stopTV.setClickable(false);
                playTV.setClickable(true);
                stopplayTV.setClickable(false);
                buttonSaveAudio.setEnabled(true);
            }
        });

        buttonSaveAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pointsToSave = getIntent().getStringExtra("pointsToSave");
                DatabaseHelper.saveAudioAndRouteToDb(context, RecordSoundActivity.this, pointsToSave);

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
            int audioId = DatabaseHelper.getLastRouteIdFromDb(this);
            mFileName = getExternalCacheDir().getAbsolutePath();
            mFileName += "/AudioRecording" + audioId + ".3gp";

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
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));

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
}