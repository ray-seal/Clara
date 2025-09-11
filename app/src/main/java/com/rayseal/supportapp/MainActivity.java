package com.rayseal.supportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;

    public class MainActivity extends AppCompatActivity {
        private FirebaseAuth mAuth;
        private TextView welcomeText;
        private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Init firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log App open event
        Bundle analyticsBundle = new Bundle();
        analyticsBundle.putString(FirebaseAnalytics.Param.METHOD, "app_start");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, analyticsBundle);

        welcomeText = findViewById(R.id.welcomeText);
        Button supportButton = findViewById(R.id.supportButton);

        supportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signInAnonymously()
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            welcomeText.setText("Signed in anonymously! You are not alone. Support is available!");

                            // Log a custom event when a user signs in anon
                            Bundle signInBundle = new Bundle();
                            signInBundle.putString(FirebaseAnalytics.Param.METHOD, "anonymous");
                            mFirebaseAnalytics.logEvent("anonymous_sign_in", signInBundle);
            } else {
                welcomeText.setText("Auth failed: " + task.getException().getMessage());
            }
        }
    });
}
});
    }
}
