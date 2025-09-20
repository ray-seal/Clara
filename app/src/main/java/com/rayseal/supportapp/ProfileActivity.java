package com.rayseal.supportapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_PROFILE_PIC = 1001;
    private static final int PICK_COVER_PHOTO = 1002;
    private static final int REQUEST_IMAGE_PERMISSION = 1003;
    private static final String TAG = "ProfileActivity";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private ImageView profilePicView, coverPhotoView;
    private EditText displayNameEdit, actualNameEdit, bioEdit, contactEdit;
    private LinearLayout categoriesLayout;
    private TextView memberSinceText, numPostsText;
    private Switch showDisplayNameSwitch, showActualNameSwitch, showProfilePicSwitch, showCoverPhotoSwitch,
            showSupportCategoriesSwitch, showBioSwitch, showContactSwitch, showStatsSwitch;
    private Button saveBtn, cancelBtn, editBtn;
    private ProgressBar progressBar;

    private Uri profilePicUri, coverPhotoUri;
    private Profile userProfile;
    private boolean isEditing = false;

    private final String[] CATEGORIES = {"Anxiety", "Depression", "Relationships", "Sleep", "Work", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.ClaraTheme);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        profilePicView = findViewById(R.id.img_profile_pic);
        coverPhotoView = findViewById(R.id.img_cover_photo);
        displayNameEdit = findViewById(R.id.edit_display_name);
        actualNameEdit = findViewById(R.id.edit_actual_name);
        bioEdit = findViewById(R.id.edit_bio);
        contactEdit = findViewById(R.id.edit_contact);
        categoriesLayout = findViewById(R.id.layout_categories);
        memberSinceText = findViewById(R.id.txt_member_since);
        numPostsText = findViewById(R.id.txt_num_posts);

        showDisplayNameSwitch = findViewById(R.id.switch_priv_display_name);
        showActualNameSwitch = findViewById(R.id.switch_priv_actual_name);
        showProfilePicSwitch = findViewById(R.id.switch_priv_profile_pic);
        showCoverPhotoSwitch = findViewById(R.id.switch_priv_cover_photo);
        showSupportCategoriesSwitch = findViewById(R.id.switch_priv_categories);
        showBioSwitch = findViewById(R.id.switch_priv_bio);
        showContactSwitch = findViewById(R.id.switch_priv_contact);
        showStatsSwitch = findViewById(R.id.switch_priv_stats);

        saveBtn = findViewById(R.id.btn_save);
        cancelBtn = findViewById(R.id.btn_cancel);
        editBtn = findViewById(R.id.btn_edit);
        progressBar = findViewById(R.id.progressBar);

        profilePicView.setOnClickListener(v -> { if (isEditing) pickImage(PICK_PROFILE_PIC); });
        coverPhotoView.setOnClickListener(v -> { if (isEditing) pickImage(PICK_COVER_PHOTO); });

        editBtn.setOnClickListener(v -> setEditing(true));
        cancelBtn.setOnClickListener(v -> { setEditing(false); loadProfile(); });
        saveBtn.setOnClickListener(v -> saveProfile());

        findViewById(R.id.btn_crisis).setOnClickListener(v -> showCrisisDialog());
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());

        setEditing(false);
        loadProfile();
    }

    private void setEditing(boolean editing) {
        isEditing = editing;
        displayNameEdit.setEnabled(editing);
        actualNameEdit.setEnabled(editing);
        bioEdit.setEnabled(editing);
        contactEdit.setEnabled(editing);

        showDisplayNameSwitch.setEnabled(editing);
        showActualNameSwitch.setEnabled(editing);
        showProfilePicSwitch.setEnabled(editing);
        showCoverPhotoSwitch.setEnabled(editing);
        showSupportCategoriesSwitch.setEnabled(editing);
        showBioSwitch.setEnabled(editing);
        showContactSwitch.setEnabled(editing);
        showStatsSwitch.setEnabled(editing);

        saveBtn.setVisibility(editing ? View.VISIBLE : View.GONE);
        cancelBtn.setVisibility(editing ? View.VISIBLE : View.GONE);
        editBtn.setVisibility(editing ? View.GONE : View.VISIBLE);

        // Enable/disable category checkboxes
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View v = categoriesLayout.getChildAt(i);
            v.setEnabled(editing);
        }
    }

    private void pickImage(int reqCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, reqCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted permission, but not retrying picker automatically for simplicity.
                Toast.makeText(this, "Permission granted, tap again to pick image.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("profiles").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                userProfile = doc.toObject(Profile.class);
            } else {
                userProfile = new Profile();
                userProfile.uid = uid;
                userProfile.displayName = "";
                userProfile.actualName = "";
                userProfile.bio = "";
                userProfile.contact = "";
                userProfile.profilePictureUrl = "";
                userProfile.coverPhotoUrl = "";
                userProfile.supportCategories = new ArrayList<>();
                userProfile.privacy = new PrivacySettings();
                userProfile.memberSince = System.currentTimeMillis();
                userProfile.numPosts = 0;
            }
            fillViews(userProfile);
            progressBar.setVisibility(View.GONE);
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Load profile failed", e);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void fillViews(Profile profile) {
        displayNameEdit.setText(profile.displayName);
        actualNameEdit.setText(profile.actualName);
        bioEdit.setText(profile.bio);
        contactEdit.setText(profile.contact);

        if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
            Glide.with(this).load(profile.profilePictureUrl).into(profilePicView);
        } else {
            profilePicView.setImageResource(R.drawable.ic_person);
        }

        if (profile.coverPhotoUrl != null && !profile.coverPhotoUrl.isEmpty()) {
            Glide.with(this).load(profile.coverPhotoUrl).into(coverPhotoView);
        } else {
            coverPhotoView.setImageResource(R.drawable.ic_image);
        }

        categoriesLayout.removeAllViews();
        for (String cat : CATEGORIES) {
            CheckBox cb = new CheckBox(this);
            cb.setText(cat);
            cb.setTextColor(Color.BLACK);
            cb.setEnabled(isEditing);
            if (profile.supportCategories != null && profile.supportCategories.contains(cat)) cb.setChecked(true);
            categoriesLayout.addView(cb);
        }

        memberSinceText.setText("Member since: " +
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(profile.memberSince)));
        numPostsText.setText("Posts: " + profile.numPosts);

        PrivacySettings priv = profile.privacy != null ? profile.privacy : new PrivacySettings();
        showDisplayNameSwitch.setChecked(priv.showDisplayName);
        showActualNameSwitch.setChecked(priv.showActualName);
        showProfilePicSwitch.setChecked(priv.showProfilePicture);
        showCoverPhotoSwitch.setChecked(priv.showCoverPhoto);
        showSupportCategoriesSwitch.setChecked(priv.showSupportCategories);
        showBioSwitch.setChecked(priv.showBio);
        showContactSwitch.setChecked(priv.showContact);
        showStatsSwitch.setChecked(priv.showStats);
    }

    private void saveProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser().getUid();
        Profile p = userProfile != null ? userProfile : new Profile();
        p.uid = uid;
        p.displayName = displayNameEdit.getText().toString().trim();
        p.actualName = actualNameEdit.getText().toString().trim();
        p.bio = bioEdit.getText().toString().trim();
        p.contact = contactEdit.getText().toString().trim();

        List<String> selectedCats = new ArrayList<>();
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View v = categoriesLayout.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                selectedCats.add(((CheckBox) v).getText().toString());
            }
        }
        p.supportCategories = selectedCats;

        PrivacySettings priv = new PrivacySettings();
        priv.showDisplayName = showDisplayNameSwitch.isChecked();
        priv.showActualName = showActualNameSwitch.isChecked();
        priv.showProfilePicture = showProfilePicSwitch.isChecked();
        priv.showCoverPhoto = showCoverPhotoSwitch.isChecked();
        priv.showSupportCategories = showSupportCategoriesSwitch.isChecked();
        priv.showBio = showBioSwitch.isChecked();
        priv.showContact = showContactSwitch.isChecked();
        priv.showStats = showStatsSwitch.isChecked();
        p.privacy = priv;

        if (profilePicUri != null) {
            uploadImage(profilePicUri, uid + "_profile.jpg", url -> {
                p.profilePictureUrl = url;
                saveCoverPhotoIfNeeded(p, uid);
            });
        } else {
            saveCoverPhotoIfNeeded(p, uid);
        }
    }

    private void saveCoverPhotoIfNeeded(Profile p, String uid) {
        if (coverPhotoUri != null) {
            uploadImage(coverPhotoUri, uid + "_cover.jpg", url -> {
                p.coverPhotoUrl = url;
                saveProfileToFirestore(p);
            });
        } else {
            saveProfileToFirestore(p);
        }
    }

    private void uploadImage(Uri uri, String filename, OnImageUploadListener listener) {
        StorageReference ref = storageRef.child("profile_images/" + filename);
        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onSuccess(uri1.toString())))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Image upload failed", e);
                    listener.onSuccess(""); // Continue with blank
                });
    }

    private void saveProfileToFirestore(Profile p) {
        db.collection("profiles").document(p.uid).set(p)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show();
                    setEditing(false);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Save profile failed", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_PROFILE_PIC) {
                profilePicUri = data.getData();
                profilePicView.setImageURI(profilePicUri);
            } else if (requestCode == PICK_COVER_PHOTO) {
                coverPhotoUri = data.getData();
                coverPhotoView.setImageURI(coverPhotoUri);
            }
        }
    }

    private interface OnImageUploadListener {
        void onSuccess(String url);
    }

    private void showCrisisDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Need Help Now?")
                .setMessage("If you are in crisis, please reach out to a trusted contact or call emergency services.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void openSettings() {
        // Start a settings activity or show settings dialog if you have one
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
    }
}
