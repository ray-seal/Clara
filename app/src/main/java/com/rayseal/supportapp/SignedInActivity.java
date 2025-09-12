package com.rayseal.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignedInActivity extends AppCompatActivity {
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

  continueButton.setOnClickListener(v -> {
    Intent intent = new Intent(SignedInActivity.this, PublicFeedActivity.class);
    startActivity(intent);
    finish();
  });
  }
}
