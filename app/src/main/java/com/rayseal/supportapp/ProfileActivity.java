package com.rayseal.supportapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
            switchCategories, switchBio, switchContact, switchStats, switchPrivateMessages,
            switchFriendRequests, switchChatInvites;
    private Button btnSave, btnCancel, btnEdit, btnAddFriend;
    private ProgressBar progressBar;

    private Uri profilePicUri, coverPhotoUri;
    private Profile userProfile;
    private boolean isEditing = false;
    private String viewingUserId; // ID of user whose profile we're viewing (null if viewing own profile)

    private final String[] CATEGORIES = {"Anxiety", "Depression", "Relationships", "Sleep", "Work", "Other"};
    private int pendingImageRequestCode = -1;

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
        switchPrivateMessages = findViewById(R.id.switch_priv_messages);
        switchFriendRequests = findViewById(R.id.switch_priv_friend_requests);
        switchChatInvites = findViewById(R.id.switch_priv_chat_invites);

        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnEdit = findViewById(R.id.btn_edit);
        btnAddFriend = findViewById(R.id.btn_add_friend);

        progressBar = findViewById(R.id.progressBar);

        // Check if we're viewing another user's profile
        Intent intent = getIntent();
        viewingUserId = intent.getStringExtra("userId");

        // Top bar buttons
        findViewById(R.id.btn_crisis).setOnClickListener(v -> showCrisisDialog());
        findViewById(R.id.btn_friends).setOnClickListener(v -> openFriendsActivity());
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());

        // Edit controls
        btnEdit.setOnClickListener(v -> setEditing(true));
        btnCancel.setOnClickListener(v -> {
            setEditing(false);
            loadProfile();
        });
        btnSave.setOnClickListener(v -> saveProfile());
        btnAddFriend.setOnClickListener(v -> sendFriendRequest());

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
        boolean isOwnProfile = viewingUserId == null || viewingUserId.equals(mAuth.getCurrentUser().getUid());
        
        // Only allow editing if it's the user's own profile
        if (!isOwnProfile) {
            editing = false;
            isEditing = false;
        }
        
        editDisplayName.setEnabled(editing);
        editDisplayName.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        editActualName.setEnabled(editing);
        editActualName.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        editBio.setEnabled(editing);
        editBio.setInputType(editing ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_NULL);
        editContact.setEnabled(editing);
        editContact.setInputType(editing ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);

        // Privacy switches - only show for own profile
        if (isOwnProfile) {
            switchDisplayName.setEnabled(editing);
            switchActualName.setEnabled(editing);
            switchProfilePic.setEnabled(editing);
            switchCoverPhoto.setEnabled(editing);
            switchCategories.setEnabled(editing);
            switchBio.setEnabled(editing);
            switchContact.setEnabled(editing);
            switchStats.setEnabled(editing);
            switchPrivateMessages.setEnabled(editing);
            switchFriendRequests.setEnabled(editing);
            switchChatInvites.setEnabled(editing);
        }

        // Show/hide edit/save/cancel buttons
        if (isOwnProfile) {
            btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
            btnCancel.setVisibility(editing ? View.VISIBLE : View.GONE);
            btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);
        }

        // Show/hide photo overlay buttons
        profilePhotoPlus.setVisibility(editing && isOwnProfile ? View.VISIBLE : View.GONE);
        coverPhotoPlus.setVisibility(editing && isOwnProfile ? View.VISIBLE : View.GONE);

        // Enable/disable category checkboxes
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            View v = categoriesLayout.getChildAt(i);
            v.setEnabled(editing && isOwnProfile);
        }
    }

    // Permission request logic for Android 13+ and below
    private void pickImage(int reqCode) {
        // Android 13 and above: READ_MEDIA_IMAGES, below: READ_EXTERNAL_STORAGE
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            pendingImageRequestCode = reqCode;
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_IMAGE_PERMISSION);
        } else {
            launchImagePicker(reqCode);
        }
    }

    private void launchImagePicker(int reqCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, reqCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted, tap again to pick image.", Toast.LENGTH_SHORT).show();
                if (pendingImageRequestCode != -1) {
                    launchImagePicker(pendingImageRequestCode);
                    pendingImageRequestCode = -1;
                }
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Determine which user's profile to load
        String profileUserId = viewingUserId != null ? viewingUserId : mAuth.getCurrentUser().getUid();
        boolean isOwnProfile = viewingUserId == null || viewingUserId.equals(mAuth.getCurrentUser().getUid());
        
        // Show/hide buttons based on whose profile we're viewing
        if (isOwnProfile) {
            // Viewing own profile - show edit button, hide add friend button
            btnEdit.setVisibility(View.VISIBLE);
            btnAddFriend.setVisibility(View.GONE);
        } else {
            // Viewing someone else's profile - hide edit button, show add friend button
            btnEdit.setVisibility(View.GONE);
            btnAddFriend.setVisibility(View.VISIBLE);
            
            // Check if they're already friends and update button text accordingly
            checkFriendshipStatus(viewingUserId);
        }
        
        db.collection("profiles").document(profileUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                userProfile = doc.toObject(Profile.class);
            } else {
                if (isOwnProfile) {
                    userProfile = new Profile();
                    userProfile.uid = profileUserId;
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
                } else {
                    // Other user's profile doesn't exist
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
            fillViews(userProfile, isOwnProfile);
            progressBar.setVisibility(View.GONE);
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void fillViews(Profile profile, boolean isOwnProfile) {
        // Show/hide fields based on privacy settings if viewing someone else's profile
        PrivacySettings priv = profile.privacy != null ? profile.privacy : new PrivacySettings();
        
        if (isOwnProfile || priv.showDisplayName) {
            editDisplayName.setText(profile.displayName);
            editDisplayName.setVisibility(View.VISIBLE);
        } else {
            editDisplayName.setText("Hidden");
            editDisplayName.setVisibility(View.VISIBLE);
        }
        
        if (isOwnProfile || priv.showActualName) {
            editActualName.setText(profile.actualName);
            editActualName.setVisibility(View.VISIBLE);
        } else {
            editActualName.setText("Hidden");
            editActualName.setVisibility(View.VISIBLE);
        }
        
        if (isOwnProfile || priv.showBio) {
            editBio.setText(profile.bio);
            editBio.setVisibility(View.VISIBLE);
        } else {
            editBio.setText("Hidden");
            editBio.setVisibility(View.VISIBLE);
        }
        
        if (isOwnProfile || priv.showContact) {
            editContact.setText(profile.contact);
            editContact.setVisibility(View.VISIBLE);
        } else {
            editContact.setText("Hidden");
            editContact.setVisibility(View.VISIBLE);
        }

        // Profile picture
        if (isOwnProfile || priv.showProfilePicture) {
            if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
                Glide.with(this).load(profile.profilePictureUrl).into(imgProfilePic);
            } else {
                imgProfilePic.setImageResource(R.drawable.ic_person);
            }
        } else {
            imgProfilePic.setImageResource(R.drawable.ic_person);
        }

        // Cover photo
        if (isOwnProfile || priv.showCoverPhoto) {
            if (profile.coverPhotoUrl != null && !profile.coverPhotoUrl.isEmpty()) {
                Glide.with(this).load(profile.coverPhotoUrl).into(imgCoverPhoto);
            } else {
                imgCoverPhoto.setImageResource(R.drawable.ic_image);
            }
        } else {
            imgCoverPhoto.setImageResource(R.drawable.ic_image);
        }

        // Categories
        categoriesLayout.removeAllViews();
        if (isOwnProfile || priv.showSupportCategories) {
            for (String cat : CATEGORIES) {
                CheckBox cb = new CheckBox(this);
                cb.setText(cat);
                cb.setTextColor(Color.BLACK);
                cb.setEnabled(isOwnProfile && isEditing);
                if (profile.supportCategories != null && profile.supportCategories.contains(cat)) cb.setChecked(true);
                categoriesLayout.addView(cb);
            }
        } else {
            TextView hiddenText = new TextView(this);
            hiddenText.setText("Categories: Hidden");
            hiddenText.setTextColor(Color.GRAY);
            categoriesLayout.addView(hiddenText);
        }

        // Stats
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (isOwnProfile || priv.showStats) {
            memberSinceText.setText("Member since: " + sdf.format(new Date(profile.memberSince)));
            numPostsText.setText("Posts: " + profile.numPosts);
        } else {
            memberSinceText.setText("Member since: Hidden");
            numPostsText.setText("Posts: Hidden");
        }

        // Privacy settings - only show for own profile
        if (isOwnProfile) {
            switchDisplayName.setChecked(priv.showDisplayName);
            switchActualName.setChecked(priv.showActualName);
            switchProfilePic.setChecked(priv.showProfilePicture);
            switchCoverPhoto.setChecked(priv.showCoverPhoto);
            switchCategories.setChecked(priv.showSupportCategories);
            switchBio.setChecked(priv.showBio);
            switchContact.setChecked(priv.showContact);
            switchStats.setChecked(priv.showStats);
            switchPrivateMessages.setChecked(priv.allowPrivateMessages);
            switchFriendRequests.setChecked(priv.allowFriendRequests);
            switchChatInvites.setChecked(priv.allowChatInvites);
        }
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
        priv.allowPrivateMessages = switchPrivateMessages.isChecked();
        priv.allowFriendRequests = switchFriendRequests.isChecked();
        priv.allowChatInvites = switchChatInvites.isChecked();
        p.privacy = priv;

        // Upload profile photo first
        if (profilePicUri != null) {
            uploadProfileImage(profilePicUri, uid + "_profile.jpg", url -> {
                p.profilePictureUrl = url;
                saveCoverPhotoIfNeeded(p, uid);
            });
        } else {
            saveCoverPhotoIfNeeded(p, uid);
        }
    }

    // Upload cover photo to cover_photos/
    private void saveCoverPhotoIfNeeded(Profile p, String uid) {
        if (coverPhotoUri != null) {
            uploadCoverImage(coverPhotoUri, uid + "_cover.jpg", url -> {
                p.coverPhotoUrl = url;
                saveProfileToFirestore(p);
            });
        } else {
            saveProfileToFirestore(p);
        }
    }

    // Profile photo upload (profile_images/)
    private void uploadProfileImage(Uri uri, String filename, OnImageUploadListener listener) {
        StorageReference ref = storageRef.child("profile_images/" + filename);
        ref.putFile(uri)
            .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onSuccess(uri1.toString())))
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload profile photo.", Toast.LENGTH_SHORT).show();
                listener.onSuccess(""); // Continue with blank
            });
    }

    // Cover photo upload (cover_photos/)
    private void uploadCoverImage(Uri uri, String filename, OnImageUploadListener listener) {
        StorageReference ref = storageRef.child("cover_photos/" + filename);
        ref.putFile(uri)
            .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onSuccess(uri1.toString())))
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload cover photo.", Toast.LENGTH_SHORT).show();
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

    private void openFriendsActivity() {
        Intent intent = new Intent(this, FriendsListActivity.class);
        startActivity(intent);
    }
    
    private void checkFriendshipStatus(String targetUserId) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        db.collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String status = "none"; // none, pending, accepted
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Friend friend = doc.toObject(Friend.class);
                        if (friend != null && friend.involvesUser(currentUserId) && friend.involvesUser(targetUserId)) {
                            status = friend.status;
                            break;
                        }
                    }
                    
                    // Update button text based on friendship status
                    switch (status) {
                        case "accepted":
                            btnAddFriend.setText("Friends");
                            btnAddFriend.setEnabled(false);
                            btnAddFriend.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                            break;
                        case "pending":
                            btnAddFriend.setText("Request Sent");
                            btnAddFriend.setEnabled(false);
                            btnAddFriend.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                            break;
                        default:
                            btnAddFriend.setText("Add Friend");
                            btnAddFriend.setEnabled(true);
                            btnAddFriend.setBackgroundTintList(getColorStateList(R.color.sky_blue));
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't check, just show the default
                    btnAddFriend.setText("Add Friend");
                    btnAddFriend.setEnabled(true);
                });
    }
    
    private void sendFriendRequest() {
        if (viewingUserId == null) {
            Toast.makeText(this, "Error: No user to send request to", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        // Check if friendship already exists
        db.collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean alreadyExists = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Friend existing = doc.toObject(Friend.class);
                        if (existing != null && existing.involvesUser(currentUserId) && existing.involvesUser(viewingUserId)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        Friend newFriend = new Friend(currentUserId, viewingUserId, currentUserId);
                        db.collection("friends")
                                .add(newFriend)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                    btnAddFriend.setText("Request Sent");
                                    btnAddFriend.setEnabled(false);
                                    btnAddFriend.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Friendship already exists", Toast.LENGTH_SHORT).show();
                        checkFriendshipStatus(viewingUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking friendship status", Toast.LENGTH_SHORT).show();
                });
    }
}
