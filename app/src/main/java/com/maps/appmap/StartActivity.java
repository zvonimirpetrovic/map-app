package com.maps.appmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    // Initializing all variables..
    private Button buttonGoToAdmin, buttonGoToUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        // initialize all variables with their layout items.
        buttonGoToAdmin = findViewById(R.id.buttonGoToMapsActivity);
        buttonGoToUser = findViewById(R.id.buttonGoToMapsUserActivity);
        buttonGoToAdmin.setBackgroundColor(getResources().getColor(R.color.gray));
        buttonGoToUser.setBackgroundColor(getResources().getColor(R.color.gray));

        buttonGoToAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(StartActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
        buttonGoToUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(StartActivity.this, MapsUserActivity.class);
                startActivity(intent);
            }
        });
    }
}
