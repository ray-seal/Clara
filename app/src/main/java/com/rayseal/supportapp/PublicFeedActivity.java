package com.rayseal.supportapp;

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
import com.google.firebase.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.*;
import android.widget.GridLayout;

public class PublicFeedActivity extends AppCompatActivity {
    private EditText postEditText;
    private FlexboxLayout categoryCheckboxes;
    private Button postButton, crisisButton, selectImageButton;
    private Spinner categoryFilterSpinner;
    private ImageView postImagePreview;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
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
    private static final String TAG = "PublicFeedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_feed);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        postEditText = findViewById(R.id.postEditText);
        categoryCheckboxes = findViewById(R.id.categoryCheckboxes);
        postButton = findViewById(R.id.postButton);
        crisisButton = findViewById(R.id.crisisButton);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        postImagePreview = findViewById(R.id.postImagePreview);
        selectImageButton = findViewById(R.id.selectImageButton);

        setupCategoryCheckboxes();
        setupCategoryFilter();
        setupRecyclerView();

        postButton.setOnClickListener(v -> submitPost());
        crisisButton.setOnClickListener(v -> showCrisisDialog());
        selectImageButton.setOnClickListener(v -> checkImagePermissionAndOpenPicker());

        loadPosts();
    }

    private void setupCategoryCheckboxes() {
        categoryCheckBoxesList.clear();
        categoryCheckboxes.removeAllViews();
        for (String cat : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(cat);
            checkBox.setTextColor(Color.parseColor("#212121"));
            categoryCheckboxes.addView(checkBox);
            categoryCheckBoxesList.add(checkBox);
        }
    }

    private void setupCategoryFilter() {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All");
        filterOptions.addAll(categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, filterOptions) {
         @Override
         public View getView(int position, View convertView, ViewGroup parent) {
             View v = super.getView(position, convertView, parent);
             ((TextView) v).setTextColor(Color.parseColor("#212121"));
             return v;
         }
         @Override
         public View getDropDownView(int position, View convertView, ViewGroup parent) {
             View v = super.getDropDownView(position, convertView, parent);
             ((TextView) v).setTextColor(Color.parseColor("#212121"));
             return v;
         }
        };
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(adapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilter = filterOptions.get(position);
                loadPosts();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(posts);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);
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
        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", content);
        post.put("categories", selectedCategories);
        post.put("timestamp", FieldValue.serverTimestamp());

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
    }

    private void uploadPostToFirestore(Map<String, Object> post) {
        db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> {
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
        Query query = db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING);
        if (!selectedFilter.equals("All")) {
            query = query.whereArrayContains("categories", selectedFilter);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                posts.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String content = doc.getString("content");
                    List<String> cats = (List<String>) doc.get("categories");
                    String imageUrl = doc.contains("imageUrl") ? doc.getString("imageUrl") : null;
                    posts.add(new Post(content, cats, imageUrl));
                }
                postAdapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error loading posts: ", task.getException());
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
}
