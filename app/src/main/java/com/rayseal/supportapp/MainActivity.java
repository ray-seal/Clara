package com.rayseal.supportapp;

import android.view.View
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
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

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log app open event
        Bundle analyticsBundle = new Bundle();
        analyticsBundle.putString(FirebaseAnalytics.Param.METHOD, "app_start");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, analyticsBundle);

        welcomeText = findViewById(R.id.welcomeText);
        Button signUpButton = findViewById(R.id.signUpButton);
        Button signInButton = findViewById(R.id.signInButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAuthDialog(true);
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAuthDialog(false);
            }
        });
    }

    private void showAuthDialog(final boolean isSignUp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isSignUp ? "Sign Up" : "Sign In");

        // Input fields
        final EditText email = new EditText(this);
        email.setHint("Email");
        final EditText password = new EditText(this);
        password.setHint("Password");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(email);
        layout.addView(password);
        builder.setView(layout);

        builder.setPositiveButton(isSignUp ? "Sign Up" : "Sign In", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String emailText = email.getText().toString().trim();
                String passwordText = password.getText().toString();

                if (isSignUp) {
                    mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    welcomeText.setText("Sign up successful! Welcome, " + emailText);
                                    Bundle signUpBundle = new Bundle();
                                    signUpBundle.putString(FirebaseAnalytics.Param.METHOD, "email_signup");
                                    mFirebaseAnalytics.logEvent("sign_up", signUpBundle);
                                } else {
                                    welcomeText.setText("Sign up failed: " + task.getException().getMessage());
                                }
                            }
                        });
                } else {
                    mAuth.signInWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    welcomeText.setText("Sign in successful! Welcome back, " + emailText);
                                    Bundle signInBundle = new Bundle();
                                    signInBundle.putString(FirebaseAnalytics.Param.METHOD, "email_signin");
                                    mFirebaseAnalytics.logEvent("sign_in", signInBundle);
                                } else {
                                    welcomeText.setText("Sign in failed: " + task.getException().getMessage());
                                }
                            }
                        });
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
