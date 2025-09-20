package com.rayseal.supportapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private ImageView imgProfilePic, imgCoverPhoto;
    private ImageView profilePhotoPlus, coverPhotoPlus;
    private EditText editDisplayName, editActualName, editBio, editContact;
    private LinearLayout categoriesLayout;
    private TextView memberSinceText, numPostsText;
    private Switch switchDisplayName, switchActualName, switchProfilePic, switchCoverPhoto,
            switchCategories, switchBio, switchContact, switchStats;
    private Button btnSave, btnCancel, btnEdit;
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

        imgProfilePic = findViewById(R.id.img_profile_pic);
        imgCoverPhoto = findViewById(R.id.img_cover_photo);
        profilePhotoPlus = findViewById(R.id.profile_photo_plus);
        coverPhotoPlus = findViewById(R.id.cover_photo_plus);

        editDisplayName = findViewById(R.id.edit_display_name);
        editActualName = findViewById(R.id.edit_actual_name);
        editBio = findViewById(R.id.edit_bio);
        editContact = findViewById(R.id.edit_contact);
        categoriesLayout = findViewById(R.id.layout_categories);

        memberSinceText = findViewById(R.id.txt_member_since);
        numPostsText = findViewById(R.id.txt_num_posts);

        switchDisplayName = findViewById(R.id.switch_priv_display_name);
        switchActualName = findViewById(R.id.switch_priv_actual_name);
        switchProfilePic = findViewById(R.id.switch_priv_profile_pic);
        switchCoverPhoto = findViewById(R.id.switch_priv_cover_photo);
        switchCategories = findViewById(R.id.switch_priv_categories);
        switchBio = findViewById(R.id.switch_priv_bio);
        switchContact = findViewById(R.id.switch_priv_contact);
        switchStats = findViewById(R.id.switch_priv_stats);

        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnEdit = findViewById(R.id.btn_edit);

        progressBar = findViewById(R.id.progressBar);

        // Top bar buttons
        findViewById(R.id.btn_crisis).setOnClickListener(v -> showCrisisDialog());
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());

        // Edit controls
        btnEdit.setOnClickListener(v -> setEditing(true));
        btnCancel.setOnClickListener(v -> {
            setEditing(false);
            loadProfile();
        });
        btnSave.setOnClickListener(v -> saveProfile());

        // Photo pickers (+ overlays)
        imgProfilePic.setOnClickListener(v -> {
            if (isEditing) pickImage(PICK_PROFILE_PIC);
        });
        profilePhotoPlus.setOnClickListener(v -> pickImage(PICK_PROFILE_PIC));

        imgCoverPhoto.setOnClickListener(v -> {
            if (isEditing) pickImage(PICK_COVER_PHOTO);
        });
        coverPhotoPlus.setOnClickListener(v -> pickImage(PICK_COVER_PHOTO));

        setEditing(false);
        loadProfile();
    }

    private void setEditing(boolean editing) {
        isEditing = editing;
        editDisplayName.setEnabled(editing);
        editDisplayName.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        editActualName.setEnabled(editing);
        editActualName.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        editBio.setEnabled(editing);
        editBio.setInputType(editing ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_NULL);
        editContact.setEnabled(editing);
        editContact.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);

        switchDisplayName.setEnabled(editing);
        switchActualName.setEnabled(editing);
        switchProfilePic.setEnabled(editing);
        switchCoverPhoto.setEnabled(editing);
        switchCategories.setEnabled(editing);
        switchBio.setEnabled(editing);
        switchContact.setEnabled(editing);
        switchStats.setEnabled(editing);

        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(editing ? View.VISIBLE : View.GONE);
        btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);

        profilePhotoPlus.setVisibility(editing ? View.VISIBLE : View.GONE);
        coverPhotoPlus.setVisibility(editing ? View.VISIBLE : View.GONE);

        // Enable/disable category checkboxes
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View v = categoriesLayout.getChildAt(i);
            v.setEnabled(editing);
        }
    }

    private void pickImage(int reqCode) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_PERMISSION);
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
            progressBar.setVisibility(View.GONE);
        });
    }

    private void fillViews(Profile profile) {
        editDisplayName.setText(profile.displayName);
        editActualName.setText(profile.actualName);
        editBio.setText(profile.bio);
        editContact.setText(profile.contact);

        if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
            Glide.with(this).load(profile.profilePictureUrl).into(imgProfilePic);
        } else {
            imgProfilePic.setImageResource(R.drawable.ic_person);
        }

        if (profile.coverPhotoUrl != null && !profile.coverPhotoUrl.isEmpty()) {
            Glide.with(this).load(profile.coverPhotoUrl).into(imgCoverPhoto);
        } else {
            imgCoverPhoto.setImageResource(R.drawable.ic_image);
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        memberSinceText.setText("Member since: " +
                sdf.format(new Date(profile.memberSince)));
        numPostsText.setText("Posts: " + profile.numPosts);

        PrivacySettings priv = profile.privacy != null ? profile.privacy : new PrivacySettings();
        switchDisplayName.setChecked(priv.showDisplayName);
        switchActualName.setChecked(priv.showActualName);
        switchProfilePic.setChecked(priv.showProfilePicture);
        switchCoverPhoto.setChecked(priv.showCoverPhoto);
        switchCategories.setChecked(priv.showSupportCategories);
        switchBio.setChecked(priv.showBio);
        switchContact.setChecked(priv.showContact);
        switchStats.setChecked(priv.showStats);
    }

    private void saveProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser().getUid();
        Profile p = userProfile != null ? userProfile : new Profile();
        p.uid = uid;
        p.displayName = editDisplayName.getText().toString().trim();
        p.actualName = editActualName.getText().toString().trim();
        p.bio = editBio.getText().toString().trim();
        p.contact = editContact.getText().toString().trim();

        List<String> selectedCats = new ArrayList<>();
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View v = categoriesLayout.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                selectedCats.add(((CheckBox) v).getText().toString());
            }
        }
        p.supportCategories = selectedCats;

        PrivacySettings priv = new PrivacySettings();
        priv.showDisplayName = switchDisplayName.isChecked();
        priv.showActualName = switchActualName.isChecked();
        priv.showProfilePicture = switchProfilePic.isChecked();
        priv.showCoverPhoto = switchCoverPhoto.isChecked();
        priv.showSupportCategories = switchCategories.isChecked();
        priv.showBio = switchBio.isChecked();
        priv.showContact = switchContact.isChecked();
        priv.showStats = switchStats.isChecked();
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
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_PROFILE_PIC) {
                profilePicUri = data.getData();
                imgProfilePic.setImageURI(profilePicUri);
            } else if (requestCode == PICK_COVER_PHOTO) {
                coverPhotoUri = data.getData();
                imgCoverPhoto.setImageURI(coverPhotoUri);
            }
        }
    }

    private interface OnImageUploadListener {
        void onSuccess(String url);
    }

    private void showCrisisDialog() {
        String country = Locale.getDefault().getCountry();
        String messageUK = "Immediate help (UK):\n\n" +
                "Call 999 in an emergency\n" +
                "Call NHS 111 for urgent advice\n" +
                "Text 'SHOUT' to 85258\n" +
                "Samaritans: 116 123\n";
        String message = country.equals("GB") ? messageUK :
                "For help, please reach out to local emergency and support services.";
        new AlertDialog.Builder(this)
                .setTitle("Crisis Support")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openSettings() {
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
    }
}
