package com.rayseal.supportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;

    public class MainActivity extends AppCompatActivity {
        private FirebaseAuth mAuth;
        private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcomeText);
        Button supportButton = findViewById(R.id.supportButton);

        supportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try Anon Sign in
                mAuth.signInAnonymously()
                .addOnCompleteListener(MainActivity.this, new onCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            welcomeText.setText("Signed in anonymously! You are not alone. Support is available!");
            } else {
                welcomeText.setText("Auth failed: " + task.getException().getMessage());
            }
        }
    });
}
});
    }
}
