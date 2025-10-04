package com.rayseal.supportapp;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class ImageViewerActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView enlargedImageView = findViewById(R.id.enlargedImageView);
        ImageView closeButton = findViewById(R.id.closeButton);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        
        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(enlargedImageView);
        }

        closeButton.setOnClickListener(v -> finish());
        enlargedImageView.setOnClickListener(v -> finish());
    }
}