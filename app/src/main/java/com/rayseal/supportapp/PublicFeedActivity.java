package com.rayseal.supportapp;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.Timestamp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.*;
import android.widget.GridLayout;

public class PublicFeedActivity extends AppCompatActivity {
    private EditText postEditText;
    private Button postButton, crisisButton, selectImageButton, chatButton;
    private GridLayout categoryCheckboxes;
    private Spinner categoryFilterSpinner;
    private ImageView postImagePreview;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private CheckBox anonymousCheckbox;
    private NotificationIconHelper notificationIconHelper;
    private String scrollToPostId; // For notification navigation
    private List<String> categories = Arrays.asList(
            "Anxiety","Depression","Insomnia","PTSD","Gender Dysphoria","Addiction","Other"
    );
    private List<CheckBox> categoryCheckBoxesList = new ArrayList<>();
    private FirebaseFirestore db;
    private List<Post> posts = new ArrayList<>();
    private String selectedFilter = "All";
    private Uri imageUri = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_PERMISSION = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "PublicFeedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_feed);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Initialize FCM token
        initializeFCMToken();

        // Request notification permissions as fallback
        requestNotificationPermission();

        postEditText = findViewById(R.id.postEditText);
        categoryCheckboxes = findViewById(R.id.categoryCheckboxes);
        postButton = findViewById(R.id.postButton);
        crisisButton = findViewById(R.id.crisisButton);
        chatButton = findViewById(R.id.chatButton);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        postImagePreview = findViewById(R.id.postImagePreview);
        selectImageButton = findViewById(R.id.selectImageButton);
        anonymousCheckbox = findViewById(R.id.anonymousCheckbox);

        // Initialize notification icon with badge
        View notificationIconLayout = findViewById(R.id.notificationIconLayout);
        notificationIconHelper = new NotificationIconHelper(this, notificationIconLayout);

        // Check if we need to scroll to a specific post (from notification click)
        scrollToPostId = getIntent().getStringExtra("scrollToPostId");

        // PROFILE AVATAR IN TOP BAR
        ImageView userAvatar = findViewById(R.id.userAvatar);
        userAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(PublicFeedActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Load user profile picture from Firestore, or use default avatar
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(user.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        String photoUrl = doc.getString("profilePictureUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(PublicFeedActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(userAvatar);
                        } else {
                            userAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                })
                .addOnFailureListener(e -> userAvatar.setImageResource(R.drawable.ic_person));
        } else {
            userAvatar.setImageResource(R.drawable.ic_person);
        }

        setupCategoryCheckboxes();
        setupCategoryFilter();
        setupRecyclerView();

        postButton.setOnClickListener(v -> submitPost());
        crisisButton.setOnClickListener(v -> showCrisisDialog());
        chatButton.setOnClickListener(v -> openChatRooms());
        selectImageButton.setOnClickListener(v -> checkImagePermissionAndOpenPicker());

        // Test Firestore connectivity before loading posts
        testFirestoreConnection();
    }
    
    private void testFirestoreConnection() {
        Log.d(TAG, "Testing Firestore connection...");
        
        // Simple connectivity test
        db.collection("posts").limit(1).get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Firestore connection successful");
                loadPosts();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Firestore connection failed: " + e.getMessage(), e);
                Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // Still try to load posts in case it's a temporary issue
                loadPosts();
            });
    }

    private void setupCategoryCheckboxes() {
        categoryCheckBoxesList.clear();
        categoryCheckboxes.removeAllViews();
        categoryCheckboxes.setColumnCount(3);

        for (String cat : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(cat);
            checkBox.setTextColor(Color.parseColor("#212121"));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(8, 8, 8, 8);
            checkBox.setLayoutParams(params);

            categoryCheckboxes.addView(checkBox);
            categoryCheckBoxesList.add(checkBox);
        }
    }

    private void setupCategoryFilter() {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All");
        filterOptions.add("Friends Posts"); // Add friends filter option
        filterOptions.addAll(categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, filterOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView textView = (TextView) v;
                textView.setTextColor(Color.parseColor("#212121"));
                textView.setTextSize(16);
                textView.setPadding(16, 16, 16, 16);
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) v;
                textView.setTextColor(Color.parseColor("#212121"));
                textView.setTextSize(16);
                textView.setPadding(20, 20, 20, 20);
                textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                
                // Add slight background change for better visibility
                if (position % 2 == 0) {
                    textView.setBackgroundColor(Color.parseColor("#F8F8F8"));
                } else {
                    textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(adapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newFilter = filterOptions.get(position);
                Log.d(TAG, "Category filter selected: " + newFilter);
                if (!newFilter.equals(selectedFilter)) {
                    selectedFilter = newFilter;
                    loadPosts();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");
        
        if (postsRecyclerView == null) {
            Log.e(TAG, "postsRecyclerView is null! Check if R.id.postsRecyclerView exists in layout");
            return;
        }
        
        if (posts == null) {
            posts = new ArrayList<>();
            Log.d(TAG, "Initialized empty posts list");
        }
        
        postAdapter = new PostAdapter(posts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        
        // Important: Disable nested scrolling since RecyclerView is inside NestedScrollView
        postsRecyclerView.setNestedScrollingEnabled(false);
        
        postsRecyclerView.setLayoutManager(layoutManager);
        postsRecyclerView.setAdapter(postAdapter);
        
        // Force the RecyclerView to be visible and have proper height
        postsRecyclerView.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "RecyclerView setup complete with " + posts.size() + " posts");
    }

    // --- Image Permission Logic ---
    private void checkImagePermissionAndOpenPicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33): Use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_IMAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            // Older Android: Use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_IMAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            postImagePreview.setVisibility(View.VISIBLE);
            postImagePreview.setImageURI(imageUri);
        }
    }

    private void submitPost() {
        String content = postEditText.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter something.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> selectedCategories = new ArrayList<>();
        for (CheckBox cb : categoryCheckBoxesList)
            if (cb.isChecked()) selectedCategories.add(cb.getText().toString());
        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in to post.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "FirebaseAuth.getInstance().getCurrentUser() returned null");
            return;
        }
        String userId = user.getUid();
        
        // Get user profile info for the post
        db.collection("profiles").document(userId).get()
            .addOnSuccessListener(doc -> {
                String authorName = "Anonymous";
                String authorProfilePicture = "";
                boolean isAnonymous = anonymousCheckbox.isChecked();
                
                if (doc.exists() && !isAnonymous) {
                    Profile profile = doc.toObject(Profile.class);
                    if (profile != null) {
                        authorName = profile.displayName != null && !profile.displayName.isEmpty() ? 
                            profile.displayName : "Anonymous";
                        authorProfilePicture = profile.profilePictureUrl != null ? profile.profilePictureUrl : "";
                    }
                }
                
                Map<String, Object> post = new HashMap<>();
                post.put("userId", userId);
                post.put("authorName", authorName);
                post.put("authorProfilePicture", authorProfilePicture);
                post.put("content", content);
                post.put("categories", selectedCategories);
                post.put("timestamp", FieldValue.serverTimestamp());
                post.put("reactions", new HashMap<String, Integer>());
                post.put("userReactions", new HashMap<String, List<String>>());
                post.put("commentCount", 0);
                post.put("isAnonymous", isAnonymous);

                if (imageUri != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("post_images/" + System.currentTimeMillis() + ".jpg");
                    storageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                post.put("imageUrl", uri.toString());
                                uploadPostToFirestore(post);
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Image upload getDownloadUrl failed", e);
                            })
                        )
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Image upload putFile failed", e);
                        });
                } else {
                    uploadPostToFirestore(post);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user profile", e);
                // Continue with anonymous posting
                boolean isAnonymous = anonymousCheckbox.isChecked();
                Map<String, Object> post = new HashMap<>();
                post.put("userId", userId);
                post.put("authorName", "Anonymous");
                post.put("authorProfilePicture", "");
                post.put("content", content);
                post.put("categories", selectedCategories);
                post.put("timestamp", FieldValue.serverTimestamp());
                post.put("reactions", new HashMap<String, Integer>());
                post.put("userReactions", new HashMap<String, List<String>>());
                post.put("commentCount", 0);
                post.put("isAnonymous", isAnonymous);

                if (imageUri != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("post_images/" + System.currentTimeMillis() + ".jpg");
                    storageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                post.put("imageUrl", uri.toString());
                                uploadPostToFirestore(post);
                            }).addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Image upload failed: " + e2.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Image upload getDownloadUrl failed", e2);
                            })
                        )
                        .addOnFailureListener(e2 -> {
                            Toast.makeText(this, "Image upload failed: " + e2.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Image upload putFile failed", e2);
                        });
                } else {
                    uploadPostToFirestore(post);
                }
            });
    }

    private void uploadPostToFirestore(Map<String, Object> post) {
        Log.d(TAG, "Uploading post to Firestore: " + post.toString());
        String userId = (String) post.get("userId");
        
        db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Post uploaded successfully with ID: " + documentReference.getId());
                
                // Update user's post count in their profile
                if (userId != null && !userId.isEmpty()) {
                    db.collection("profiles").document(userId)
                        .update("numPosts", FieldValue.increment(1))
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "User post count incremented successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to increment user post count", e);
                        });
                }
                
                postEditText.setText("");
                for (CheckBox cb : categoryCheckBoxesList) cb.setChecked(false);
                imageUri = null;
                postImagePreview.setImageDrawable(null);
                postImagePreview.setVisibility(View.GONE);
                Toast.makeText(this, "Post added!", Toast.LENGTH_SHORT).show();
                loadPosts();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error posting: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Firestore add failed", e);
            });
    }

    private void loadPosts() {
        Log.d(TAG, "Starting to load posts with filter: " + selectedFilter);
        
        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getUid() : "null"));
        
        // Clear existing posts
        posts.clear();
        postAdapter.notifyDataSetChanged();
        
        // Handle Friends Posts filter specially
        if ("Friends Posts".equals(selectedFilter)) {
            loadFriendsPosts();
            return;
        }
        
        // Build query for regular filters
        Query query = db.collection("posts");
        if (!selectedFilter.equals("All")) {
            query = query.whereArrayContains("categories", selectedFilter);
            Log.d(TAG, "Applying category filter: " + selectedFilter);
        } else {
            Log.d(TAG, "Loading all posts (no filter)");
        }

        Log.d(TAG, "Executing Firestore query...");
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                Log.d(TAG, "Query successful. Document count: " + (result != null ? result.size() : 0));
                
                if (result == null || result.isEmpty()) {
                    Log.w(TAG, "No documents found in posts collection for filter: " + selectedFilter);
                    String message = selectedFilter.equals("All") ? 
                        "No posts found. Try creating one!" : 
                        "No posts found for category '" + selectedFilter + "'. Try 'All' or create a post in this category.";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                List<Post> tempPosts = new ArrayList<>();
                int successCount = 0;
                int errorCount = 0;
                
                for (QueryDocumentSnapshot doc : result) {
                    try {
                        Log.d(TAG, "Processing document: " + doc.getId());
                        
                        String postId = doc.getId();
                        String content = doc.getString("content");
                        List<String> cats = (List<String>) doc.get("categories");
                        String imageUrl = doc.contains("imageUrl") ? doc.getString("imageUrl") : null;
                        String userId = doc.getString("userId");
                        String authorName = doc.getString("authorName");
                        String authorProfilePicture = doc.getString("authorProfilePicture");
                        
                        // Debug: Log the categories for this post
                        Log.d(TAG, "Post " + postId + " categories: " + (cats != null ? cats.toString() : "null"));
                        
                        // Handle timestamp - could be server timestamp (null) or long value
                        long timestamp = System.currentTimeMillis();
                        Object timestampObj = doc.get("timestamp");
                        if (timestampObj instanceof com.google.firebase.Timestamp) {
                            timestamp = ((com.google.firebase.Timestamp) timestampObj).getSeconds() * 1000;
                        } else if (timestampObj instanceof Long) {
                            timestamp = (Long) timestampObj;
                        }
                        
                        // Validate required fields
                        if (content == null || content.trim().isEmpty()) {
                            Log.w(TAG, "Skipping post " + postId + " - empty content");
                            continue;
                        }
                        
                        // Handle backward compatibility for old posts
                        if (authorName == null || authorName.isEmpty()) {
                            authorName = "Anonymous";
                        }
                        if (authorProfilePicture == null) {
                            authorProfilePicture = "";
                        }
                        if (cats == null) {
                            cats = new ArrayList<>();
                        }
                        if (userId == null) {
                            userId = "";
                        }
                        
                        // Double-check filtering logic for debugging
                        if (!selectedFilter.equals("All")) {
                            if (cats.isEmpty() || !cats.contains(selectedFilter)) {
                                Log.d(TAG, "Post " + postId + " filtered out - doesn't match category " + selectedFilter);
                                continue;
                            }
                            Log.d(TAG, "Post " + postId + " matches filter " + selectedFilter);
                        }
                        
                        Post post = new Post(postId, content, cats, imageUrl, userId, authorName, authorProfilePicture, timestamp);
                        
                        // Load anonymous flag with null check (backward compatibility)
                        Boolean isAnonymous = doc.getBoolean("isAnonymous");
                        post.isAnonymous = isAnonymous != null ? isAnonymous : false;
                        
                        // Load reactions with null checks
                        Map<String, Object> reactions = (Map<String, Object>) doc.get("reactions");
                        if (reactions != null) {
                            for (Map.Entry<String, Object> entry : reactions.entrySet()) {
                                if (entry.getValue() instanceof Long) {
                                    post.reactions.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                                } else if (entry.getValue() instanceof Integer) {
                                    post.reactions.put(entry.getKey(), (Integer) entry.getValue());
                                }
                            }
                        }
                        
                        // Load user reactions with null checks
                        Map<String, Object> userReactions = (Map<String, Object>) doc.get("userReactions");
                        if (userReactions != null) {
                            for (Map.Entry<String, Object> entry : userReactions.entrySet()) {
                                if (entry.getValue() instanceof List) {
                                    post.userReactions.put(entry.getKey(), (List<String>) entry.getValue());
                                }
                            }
                        }
                        
                        // Load comment count with null check
                        Long commentCountLong = doc.getLong("commentCount");
                        post.commentCount = commentCountLong != null ? commentCountLong.intValue() : 0;
                        
                        tempPosts.add(post);
                        successCount++;
                        
                        Log.d(TAG, "Successfully loaded post: " + postId + " - " + content.substring(0, Math.min(content.length(), 50)));
                        
                    } catch (Exception e) {
                        errorCount++;
                        Log.e(TAG, "Error parsing post " + doc.getId() + ": " + e.getMessage(), e);
                        // Continue with next post instead of crashing
                    }
                }
                
                Log.d(TAG, "Post processing complete. Success: " + successCount + ", Errors: " + errorCount + ", Filter: " + selectedFilter);
                
                // Sort posts by timestamp (newest first)
                tempPosts.sort((p1, p2) -> Long.compare(p2.timestamp, p1.timestamp));
                posts.addAll(tempPosts);
                
                Log.d(TAG, "Total posts loaded and added to list: " + posts.size());
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    postAdapter.notifyDataSetChanged();
                    Log.d(TAG, "UI updated - postAdapter.notifyDataSetChanged() called");
                    
                    // Handle scroll to specific post if requested
                    if (scrollToPostId != null) {
                        scrollToPost(scrollToPostId);
                    }
                    
                    if (posts.isEmpty()) {
                        String message = selectedFilter.equals("All") ? 
                            "No posts to display. Try creating one!" : 
                            "No posts found for '" + selectedFilter + "'. Try 'All' or create a post in this category.";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                    // Remove the "loaded X posts" toast message as requested
                });
                
            } else {
                Exception exception = task.getException();
                Log.e(TAG, "Error loading posts: " + (exception != null ? exception.getMessage() : "Unknown error"), exception);
                
                // Show detailed error to user
                String errorMessage = "Error loading posts: ";
                if (exception != null) {
                    errorMessage += exception.getMessage();
                } else {
                    errorMessage += "Unknown error occurred";
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
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
        new android.app.AlertDialog.Builder(this)
                .setTitle("Crisis Support")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Open the chat rooms activity.
     */
    private void openChatRooms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to access chat", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatRoomListActivity.class);
        startActivity(intent);
    }

    /**
     * Request notification permissions for Android 13+
     */
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

    /**
     * Initialize FCM token for push notifications
     */
    private void initializeFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Registration Token: " + token);

                // Send token to server
                sendTokenToServer(token);
            });
    }

    /**
     * Send FCM token to server
     */
    private void sendTokenToServer(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            MyFirebaseMessagingService.FCMToken fcmToken = new MyFirebaseMessagingService.FCMToken(user.getUid(), token);
            db.collection("fcm_tokens")
                .document(user.getUid())
                .set(fcmToken)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationIconHelper != null) {
            notificationIconHelper.cleanup();
        }
    }

    /**
     * Scroll to a specific post by ID (for notification navigation)
     */
    private void scrollToPost(String postId) {
        if (postId == null || posts == null) {
            Log.w(TAG, "Cannot scroll to post - postId or posts is null");
            return;
        }
        
        Log.d(TAG, "Attempting to scroll to post: " + postId);
        
        for (int i = 0; i < posts.size(); i++) {
            if (postId.equals(posts.get(i).postId)) {
                final int position = i;
                Log.d(TAG, "Found post at position: " + position);
                
                // Scroll to position with smooth scrolling to center of screen
                LinearLayoutManager layoutManager = (LinearLayoutManager) postsRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    // Use smooth scroll to position to center the post on screen
                    layoutManager.scrollToPositionWithOffset(position, 
                        postsRecyclerView.getHeight() / 3); // Show post in upper third for better visibility
                }
                
                // Wait for scroll to complete, then highlight the post
                postsRecyclerView.postDelayed(() -> {
                    if (position < posts.size()) {
                        View targetView = layoutManager.findViewByPosition(position);
                        if (targetView != null) {
                            Log.d(TAG, "Highlighting post at position: " + position);
                            // Create a more prominent highlight effect
                            highlightPost(targetView);
                        } else {
                            Log.w(TAG, "Target view not found for position: " + position);
                            // If view isn't visible, try scrolling again
                            postsRecyclerView.scrollToPosition(position);
                            postsRecyclerView.postDelayed(() -> {
                                View retryView = layoutManager.findViewByPosition(position);
                                if (retryView != null) {
                                    highlightPost(retryView);
                                }
                            }, 300);
                        }
                    }
                }, 700); // Give more time for scroll animation
                
                // Clear the scroll intent so it doesn't trigger again
                scrollToPostId = null;
                break;
            }
        }
    }
    
    /**
     * Highlight a post with a prominent visual effect
     */
    private void highlightPost(View targetView) {
        // Save original background
        android.graphics.drawable.Drawable originalBackground = targetView.getBackground();
        
        // Create highlight animation
        android.animation.ValueAnimator colorAnimator = android.animation.ValueAnimator.ofArgb(
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.holo_blue_light),
            getResources().getColor(android.R.color.transparent)
        );
        
        colorAnimator.setDuration(2000); // 2 second animation
        colorAnimator.setRepeatCount(1); // Repeat once for double flash
        colorAnimator.addUpdateListener(animation -> {
            targetView.setBackgroundColor((Integer) animation.getAnimatedValue());
        });
        
        colorAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Restore original background
                targetView.setBackground(originalBackground);
                
                // Add a subtle border effect that lasts longer
                targetView.postDelayed(() -> {
                    targetView.setBackgroundResource(android.R.drawable.editbox_background);
                    targetView.postDelayed(() -> targetView.setBackground(originalBackground), 3000);
                }, 100);
            }
        });
        
        colorAnimator.start();
        
        // Also provide haptic feedback if available
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            targetView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        }
        
        // Show a toast to confirm the post was found
        Toast.makeText(this, "Navigated to your post", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Load posts from user's friends only
     */
    private void loadFriendsPosts() {
        Log.d(TAG, "Loading friends posts");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No current user for friends posts");
            return;
        }
        
        String currentUserId = currentUser.getUid();
        
        // First, get the list of friends
        db.collection("friends")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener(friendsSnapshot -> {
                List<String> friendIds = new ArrayList<>();
                friendIds.add(currentUserId); // Include current user's posts too
                
                for (QueryDocumentSnapshot friendDoc : friendsSnapshot) {
                    String userId1 = friendDoc.getString("userId1");
                    String userId2 = friendDoc.getString("userId2");
                    
                    // Add the friend's ID (the one that's not the current user)
                    if (currentUserId.equals(userId1)) {
                        friendIds.add(userId2);
                    } else if (currentUserId.equals(userId2)) {
                        friendIds.add(userId1);
                    }
                }
                
                Log.d(TAG, "Found " + friendIds.size() + " friends (including self)");
                
                if (friendIds.isEmpty()) {
                    Log.w(TAG, "No friends found");
                    Toast.makeText(this, "No posts from friends found", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Due to Firestore limitations, we can only use 'in' operator with up to 10 values
                // If more than 10 friends, we'll need to make multiple queries
                if (friendIds.size() <= 10) {
                    // Single query for 10 or fewer friends
                    loadPostsForFriends(friendIds);
                } else {
                    // Multiple queries for more than 10 friends
                    loadPostsForManyFriends(friendIds);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading friends", e);
                Toast.makeText(this, "Error loading friends posts", Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Load posts for 10 or fewer friends using a single query
     */
    private void loadPostsForFriends(List<String> friendIds) {
        db.collection("posts")
            .whereIn("userId", friendIds)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                processFriendsPostsResult(querySnapshot);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading friends posts", e);
                Toast.makeText(this, "Error loading friends posts", Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Load posts for more than 10 friends using multiple queries
     */
    private void loadPostsForManyFriends(List<String> friendIds) {
        Log.d(TAG, "Loading posts for " + friendIds.size() + " friends using multiple queries");
        
        // Split friend IDs into chunks of 10
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < friendIds.size(); i += 10) {
            chunks.add(friendIds.subList(i, Math.min(friendIds.size(), i + 10)));
        }
        
        List<QuerySnapshot> allResults = new ArrayList<>();
        final int[] completedQueries = {0};
        
        for (List<String> chunk : chunks) {
            db.collection("posts")
                .whereIn("userId", chunk)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allResults.add(querySnapshot);
                    completedQueries[0]++;
                    
                    // When all queries are complete, process results
                    if (completedQueries[0] == chunks.size()) {
                        // Combine all results
                        List<DocumentSnapshot> allDocs = new ArrayList<>();
                        for (QuerySnapshot result : allResults) {
                            allDocs.addAll(result.getDocuments());
                        }
                        
                        // Sort by timestamp (newest first)
                        allDocs.sort((doc1, doc2) -> {
                            Timestamp ts1 = doc1.getTimestamp("timestamp");
                            Timestamp ts2 = doc2.getTimestamp("timestamp");
                            if (ts1 == null && ts2 == null) return 0;
                            if (ts1 == null) return 1;
                            if (ts2 == null) return -1;
                            return ts2.compareTo(ts1); // Descending order
                        });
                        
                        // Process the sorted results
                        processFriendsPostsFromDocs(allDocs);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in chunk query", e);
                    completedQueries[0]++;
                    
                    // Still try to process if we have partial results
                    if (completedQueries[0] == chunks.size()) {
                        List<DocumentSnapshot> allDocs = new ArrayList<>();
                        for (QuerySnapshot result : allResults) {
                            allDocs.addAll(result.getDocuments());
                        }
                        processFriendsPostsFromDocs(allDocs);
                    }
                });
        }
    }
    
    /**
     * Process friends posts from QuerySnapshot
     */
    private void processFriendsPostsResult(QuerySnapshot querySnapshot) {
        if (querySnapshot != null) {
            processFriendsPostsFromDocs(querySnapshot.getDocuments());
        }
    }
    
    /**
     * Process friends posts from a list of documents
     */
    private void processFriendsPostsFromDocs(List<DocumentSnapshot> docs) {
        Log.d(TAG, "Processing " + docs.size() + " friends posts");
        
        if (docs.isEmpty()) {
            Log.w(TAG, "No posts found from friends");
            Toast.makeText(this, "No posts from friends found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        for (DocumentSnapshot doc : docs) {
            String postId = doc.getId();
            String content = doc.getString("content");
            String imageUrl = doc.getString("imageUrl");
            String userId = doc.getString("userId");
            String authorName = doc.getString("authorName");
            String authorProfilePicture = doc.getString("authorProfilePicture");
            Timestamp timestamp = doc.getTimestamp("timestamp");
            List<String> cats = (List<String>) doc.get("categories");
            
            // Apply null checks
            if (content == null) content = "";
            if (imageUrl == null) imageUrl = "";
            if (authorName == null) authorName = "Anonymous";
            if (authorProfilePicture == null) authorProfilePicture = "";
            if (cats == null) cats = new ArrayList<>();
            if (userId == null) userId = "";
            
            // Convert Timestamp to long for Post constructor
            long timestampMillis = timestamp != null ? timestamp.toDate().getTime() : System.currentTimeMillis();
            
            Post post = new Post(postId, content, cats, imageUrl, userId, authorName, authorProfilePicture, timestampMillis);
            
            // Load additional post data (same as regular loadPosts)
            Boolean isAnonymous = doc.getBoolean("isAnonymous");
            post.isAnonymous = isAnonymous != null ? isAnonymous : false;
            
            // Load reactions
            Map<String, Object> reactions = (Map<String, Object>) doc.get("reactions");
            if (reactions != null) {
                for (Map.Entry<String, Object> entry : reactions.entrySet()) {
                    if (entry.getValue() instanceof Long) {
                        post.reactions.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                    } else if (entry.getValue() instanceof Integer) {
                        post.reactions.put(entry.getKey(), (Integer) entry.getValue());
                    }
                }
            }
            
            // Load user reactions
            Map<String, Object> userReactions = (Map<String, Object>) doc.get("userReactions");
            if (userReactions != null) {
                for (Map.Entry<String, Object> entry : userReactions.entrySet()) {
                    if (entry.getValue() instanceof List) {
                        post.userReactions.put(entry.getKey(), (List<String>) entry.getValue());
                    }
                }
            }
            
            // Load comment count
            Long commentCountLong = doc.getLong("commentCount");
            post.commentCount = commentCountLong != null ? commentCountLong.intValue() : 0;
            
            posts.add(post);
        }
        
        // Sort posts by timestamp (newest first) - shouldn't be needed due to Firestore ordering, but just in case
        posts.sort((p1, p2) -> {
            return Long.compare(p2.timestamp, p1.timestamp); // Descending order (newest first)
        });
        
        Log.d(TAG, "Loaded " + posts.size() + " friends posts successfully");
        postAdapter.notifyDataSetChanged();
        
        // Check if we need to scroll to a specific post
        if (scrollToPostId != null) {
            scrollToPost(scrollToPostId);
        }
        
        Toast.makeText(this, "Loaded " + posts.size() + " posts from friends", Toast.LENGTH_SHORT).show();
    }
}
