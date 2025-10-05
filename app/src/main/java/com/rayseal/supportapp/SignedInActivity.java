package com.rayseal.supportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignedInActivity extends AppCompatActivity {
  private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signed_in);

    TextView welcomeText = findViewById(R.id.welcomeText);
    Button continueButton = findViewById(R.id.continueButton);

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
      String email = user.getEmail();
      welcomeText.setText("Sign in successful! Welcome back, " + email);
    } else {
      welcomeText.setText("Sign in Successful!");
    }

    // Request notification permissions for Android 13+
    requestNotificationPermission();

    continueButton.setOnClickListener(v -> {
      Intent intent = new Intent(SignedInActivity.this, PublicFeedActivity.class);
      startActivity(intent);
      finish();
    });
  }

  private void requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
            NOTIFICATION_PERMISSION_REQUEST_CODE);
      }
    }
  }
}
