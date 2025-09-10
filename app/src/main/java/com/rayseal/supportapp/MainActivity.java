package com.rayseal.supportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

    public class MainActivity extends AppCmpatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView welcomeText = findViewById(R.id.welcomeText);
        Button supportButton = findViewById(R.id.supportButton);

        supportButton.setOnClickistener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                welcomeText.setText("You are not alone. Support is available!");
            }
        });
    }
}