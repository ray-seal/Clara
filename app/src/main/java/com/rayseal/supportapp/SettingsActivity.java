package com.rayseal.supportapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity {
    
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private ImageView backButton;
    private ImageView profilePicturePreview;
    private Button changeProfilePictureButton;
    private EditText displayNameEditText;
    private EditText bioEditText;
    private Button saveProfileButton;
    private Switch hidePostsFromFriendsSwitch;
    private Switch privateProfileSwitch;
    private Switch pushNotificationsSwitch;
    
    // Privacy control switches
    private Switch switchDisplayName, switchActualName, switchProfilePic, switchCoverPhoto,
            switchCategories, switchBio, switchContact, switchStats, switchPrivateMessages,
            switchFriendRequests, switchChatInvites;
    
    private Button logoutButton;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            finish();
            return;
        }

        // Initialize views
        initializeViews();
        
        // Load current profile data
        loadProfileData();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profilePicturePreview = findViewById(R.id.profilePicturePreview);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);
        displayNameEditText = findViewById(R.id.displayNameEditText);
        bioEditText = findViewById(R.id.bioEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        hidePostsFromFriendsSwitch = findViewById(R.id.hidePostsFromFriendsSwitch);
        privateProfileSwitch = findViewById(R.id.privateProfileSwitch);
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch);
        
        // Privacy control switches
        switchDisplayName = findViewById(R.id.switch_priv_display_name);
        switchActualName = findViewById(R.id.switch_priv_actual_name);
        switchProfilePic = findViewById(R.id.switch_priv_profile_pic);
        switchCoverPhoto = findViewById(R.id.switch_priv_cover_photo);
        switchCategories = findViewById(R.id.switch_priv_categories);
        switchBio = findViewById(R.id.switch_priv_bio);
        switchContact = findViewById(R.id.switch_priv_contact);
        switchStats = findViewById(R.id.switch_priv_stats);
        switchPrivateMessages = findViewById(R.id.switch_priv_messages);
        switchFriendRequests = findViewById(R.id.switch_priv_friend_requests);
        switchChatInvites = findViewById(R.id.switch_priv_chat_invites);
        
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        changeProfilePictureButton.setOnClickListener(v -> selectImage());
        
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void loadProfileData() {
        db.collection("profiles").document(currentUserId).get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    Profile profile = document.toObject(Profile.class);
                    if (profile != null) {
                        // Load profile picture
                        if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
                            Glide.with(this)
                                .load(profile.profilePictureUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(profilePicturePreview);
                        }
                        
                        // Load profile data
                        displayNameEditText.setText(profile.displayName != null ? profile.displayName : "");
                        bioEditText.setText(profile.bio != null ? profile.bio : "");
                        
                        // Load privacy settings
                        hidePostsFromFriendsSwitch.setChecked(profile.hidePostsFromFriends);
                        privateProfileSwitch.setChecked(profile.isPrivate);
                        
                        // Load detailed privacy controls
                        PrivacySettings privacy = profile.privacy != null ? profile.privacy : new PrivacySettings();
                        switchDisplayName.setChecked(privacy.showDisplayName);
                        switchActualName.setChecked(privacy.showActualName);
                        switchProfilePic.setChecked(privacy.showProfilePicture);
                        switchCoverPhoto.setChecked(privacy.showCoverPhoto);
                        switchCategories.setChecked(privacy.showSupportCategories);
                        switchBio.setChecked(privacy.showBio);
                        switchContact.setChecked(privacy.showContact);
                        switchStats.setChecked(privacy.showStats);
                        switchPrivateMessages.setChecked(privacy.allowPrivateMessages);
                        switchFriendRequests.setChecked(privacy.allowFriendRequests);
                        switchChatInvites.setChecked(privacy.allowChatInvites);
                        
                        // Load notification preferences
                        pushNotificationsSwitch.setChecked(true); // Default value
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            });
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK 
            && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            
            // Display selected image
            Glide.with(this)
                .load(selectedImageUri)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profilePicturePreview);
        }
    }

    private void saveProfileChanges() {
        String displayName = displayNameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();
        
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Display name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        saveProfileButton.setEnabled(false);
        saveProfileButton.setText("Saving...");

        if (selectedImageUri != null) {
            // Upload new profile picture first
            uploadProfilePicture(displayName, bio);
        } else {
            // Save profile without changing picture
            updateProfile(displayName, bio, null);
        }
    }

    private void uploadProfilePicture(String displayName, String bio) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
            .child("profile_pictures/" + currentUserId + ".jpg");
        
        storageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> 
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> 
                    updateProfile(displayName, bio, uri.toString())
                )
            )
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                resetSaveButton();
            });
    }

    private void updateProfile(String displayName, String bio, String profilePictureUrl) {
        // Get current profile first to preserve other fields
        db.collection("profiles").document(currentUserId).get()
            .addOnSuccessListener(document -> {
                Profile profile;
                if (document.exists()) {
                    profile = document.toObject(Profile.class);
                    if (profile == null) {
                        profile = new Profile();
                    }
                } else {
                    profile = new Profile();
                }
                
                // Update profile fields
                profile.displayName = displayName;
                profile.bio = bio;
                if (profilePictureUrl != null) {
                    profile.profilePictureUrl = profilePictureUrl;
                }
                profile.hidePostsFromFriends = hidePostsFromFriendsSwitch.isChecked();
                profile.isPrivate = privateProfileSwitch.isChecked();
                
                // Update detailed privacy controls
                if (profile.privacy == null) {
                    profile.privacy = new PrivacySettings();
                }
                profile.privacy.showDisplayName = switchDisplayName.isChecked();
                profile.privacy.showActualName = switchActualName.isChecked();
                profile.privacy.showProfilePicture = switchProfilePic.isChecked();
                profile.privacy.showCoverPhoto = switchCoverPhoto.isChecked();
                profile.privacy.showSupportCategories = switchCategories.isChecked();
                profile.privacy.showBio = switchBio.isChecked();
                profile.privacy.showContact = switchContact.isChecked();
                profile.privacy.showStats = switchStats.isChecked();
                profile.privacy.allowPrivateMessages = switchPrivateMessages.isChecked();
                profile.privacy.allowFriendRequests = switchFriendRequests.isChecked();
                profile.privacy.allowChatInvites = switchChatInvites.isChecked();
                
                // Save updated profile
                db.collection("profiles").document(currentUserId)
                    .set(profile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                        selectedImageUri = null; // Clear selected image
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading current profile", Toast.LENGTH_SHORT).show();
                resetSaveButton();
            });
    }

    private void resetSaveButton() {
        saveProfileButton.setEnabled(true);
        saveProfileButton.setText("Save Profile Changes");
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out", (dialog, which) -> logout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void logout() {
        auth.signOut();
        
        // Clear any cached data if needed
        
        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}