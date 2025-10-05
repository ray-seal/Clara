package com.rayseal.supportapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImageViewerActivity extends AppCompatActivity {
    private static final String TAG = "ImageViewerActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_image_viewer);
            Log.d(TAG, "Layout set successfully");

            ImageView enlargedImageView = findViewById(R.id.enlargedImageView);
            ImageView closeButton = findViewById(R.id.closeButton);
            
            if (enlargedImageView == null) {
                Log.e(TAG, "enlargedImageView is null!");
                Toast.makeText(this, "Error: Image view not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            if (closeButton == null) {
                Log.e(TAG, "closeButton is null!");
                Toast.makeText(this, "Error: Close button not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String imageUrl = getIntent().getStringExtra("imageUrl");
            Log.d(TAG, "Image URL: " + imageUrl);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    Glide.with(this)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_delete)
                            .into(enlargedImageView);
                    Log.d(TAG, "Glide load initiated");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image with Glide", e);
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Log.e(TAG, "Image URL is null or empty");
                Toast.makeText(this, "No image URL provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            closeButton.setOnClickListener(v -> {
                Log.d(TAG, "Close button clicked");
                finish();
            });
            
            enlargedImageView.setOnClickListener(v -> {
                Log.d(TAG, "Image clicked");
                finish();
            });
            
            Log.d(TAG, "ImageViewerActivity setup completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error opening image viewer", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}