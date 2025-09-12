package com.rayseal.supportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class PublicFeedActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_PICK_REQUEST = 200;
    
    private EditText postEditText;
    private LinearLayout categoryCheckboxes;
    private Button postButton, crisisButton;
    private Button selectImageButton, removeImageButton;
    private TextView imageStatusText;
    private ImageView imagePreview;
    private Spinner categoryFilterSpinner;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<String> categories = Arrays.asList(
            "Anxiety","Depression","Insomnia","PTSD","Gender Dysphoria","Addiction","Other"
    );
    private List<CheckBox> categoryCheckBoxesList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private List<Post> posts = new ArrayList<>();
    private String selectedFilter = "All";
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_feed);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        postEditText = findViewById(R.id.postEditText);
        categoryCheckboxes = findViewById(R.id.categoryCheckboxes);
        postButton = findViewById(R.id.postButton);
        crisisButton = findViewById(R.id.crisisButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        removeImageButton = findViewById(R.id.removeImageButton);
        imageStatusText = findViewById(R.id.imageStatusText);
        imagePreview = findViewById(R.id.imagePreview);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);

        setupCategoryCheckboxes();
        setupCategoryFilter();
        setupRecyclerView();
        setupImageHandlers();

        postButton.setOnClickListener(v -> submitPost());
        crisisButton.setOnClickListener(v -> showCrisisDialog());

        loadPosts();
    }

    private void setupCategoryCheckboxes() {
        categoryCheckBoxesList.clear();
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

    private void setupImageHandlers() {
        selectImageButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                openImagePicker();
            } else {
                requestPermissions();
            }
        });

        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreview.setVisibility(View.GONE);
            removeImageButton.setVisibility(View.GONE);
            imageStatusText.setText("No image selected");
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
               == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
            PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                imagePreview.setImageURI(selectedImageUri);
                imagePreview.setVisibility(View.VISIBLE);
                removeImageButton.setVisibility(View.VISIBLE);
                imageStatusText.setText("Image selected");
            }
        }
    }

    private void submitPost() {
        String content = postEditText.getText().toString().trim();
        if (content.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Please enter text or select an image.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<String> selectedCategories = new ArrayList<>();
        for (CheckBox cb : categoryCheckBoxesList)
            if (cb.isChecked()) selectedCategories.add(cb.getText().toString());

        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        // Disable post button during upload
        postButton.setEnabled(false);
        postButton.setText("Posting...");

        if (selectedImageUri != null) {
            // Upload image first, then create post
            uploadImageAndPost(content, selectedCategories, userId);
        } else {
            // Create post without image
            createPost(content, selectedCategories, userId, null);
        }
    }

    private void uploadImageAndPost(String content, List<String> categories, String userId) {
        String imageFileName = "post_images/" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(imageFileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        createPost(content, categories, userId, uri.toString());
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                        resetPostButton();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                    resetPostButton();
                });
    }

    private void createPost(String content, List<String> categories, String userId, String imageUrl) {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", content);
        post.put("categories", categories);
        post.put("timestamp", FieldValue.serverTimestamp());
        if (imageUrl != null) {
            post.put("imageUrl", imageUrl);
        }

        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    postEditText.setText("");
                    for (CheckBox cb : categoryCheckBoxesList) cb.setChecked(false);
                    selectedImageUri = null;
                    imagePreview.setVisibility(View.GONE);
                    removeImageButton.setVisibility(View.GONE);
                    imageStatusText.setText("No image selected");
                    Toast.makeText(this, "Post added!", Toast.LENGTH_SHORT).show();
                    resetPostButton();
                    loadPosts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error posting.", Toast.LENGTH_SHORT).show();
                    resetPostButton();
                });
    }

    private void resetPostButton() {
        postButton.setEnabled(true);
        postButton.setText("Post");
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
                    String imageUrl = doc.getString("imageUrl");
                    posts.add(new Post(content, cats, imageUrl));
                }
                postAdapter.notifyDataSetChanged();
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
