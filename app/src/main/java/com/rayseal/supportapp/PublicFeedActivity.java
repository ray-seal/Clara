package com.rayseal.supportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class PublicFeedActivity extends AppCompatActivity {
    private EditText postEditText;
    private LinearLayout categoryCheckboxes;
    private Button postButton, crisisButton;
    private Spinner categoryFilterSpinner;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<String> categories = Arrays.asList(
            "Anxiety","Depression","Insomnia","PTSD","Gender Dysphoria","Addiction","Other"
    );
    private List<CheckBox> categoryCheckBoxesList = new ArrayList<>();
    private FirebaseFirestore db;
    private List<Post> posts = new ArrayList<>();
    private String selectedFilter = "All";

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

        setupCategoryCheckboxes();
        setupCategoryFilter();
        setupRecyclerView();

        postButton.setOnClickListener(v -> submitPost());
        crisisButton.setOnClickListener(v -> showCrisisDialog());

        loadPosts();
    }

    private void setupCategoryCheckboxes() {
        categoryCheckBoxesList.clear();
        for (String cat : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(cat);
            categoryCheckboxes.addView(checkBox);
            categoryCheckBoxesList.add(checkBox);
        }
    }

    private void setupCategoryFilter() {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All");
        filterOptions.addAll(categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterOptions);
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

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", content);
        post.put("categories", selectedCategories);
        post.put("timestamp", FieldValue.serverTimestamp());

        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    postEditText.setText("");
                    for (CheckBox cb : categoryCheckBoxesList) cb.setChecked(false);
                    Toast.makeText(this, "Post added!", Toast.LENGTH_SHORT).show();
                    loadPosts();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error posting.", Toast.LENGTH_SHORT).show());
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
                    posts.add(new Post(content, cats));
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
