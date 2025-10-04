package com.rayseal.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing friends and friend requests.
 */
public class FriendsListActivity extends AppCompatActivity {
    private RecyclerView friendsRecyclerView;
    private FriendAdapter friendAdapter;
    private ProgressBar progressBar;
    private Button btnFriends, btnRequests, btnSearch;
    private EditText searchInput;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private String currentView = "friends"; // friends, requests, search

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to access friends", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        setupButtons();
        loadFriends();
    }

    private void initializeViews() {
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnFriends = findViewById(R.id.btnFriends);
        btnRequests = findViewById(R.id.btnRequests);
        btnSearch = findViewById(R.id.btnSearch);
        searchInput = findViewById(R.id.searchInput);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearchUser).setOnClickListener(v -> searchUsers());
    }

    private void setupRecyclerView() {
        friendAdapter = new FriendAdapter(currentUserId, new FriendAdapter.OnFriendActionListener() {
            @Override
            public void onAcceptRequest(Friend friend) {
                acceptFriendRequest(friend);
            }

            @Override
            public void onRejectRequest(Friend friend) {
                rejectFriendRequest(friend);
            }

            @Override
            public void onRemoveFriend(Friend friend) {
                removeFriend(friend);
            }

            @Override
            public void onSendRequest(String userId) {
                sendFriendRequest(userId);
            }

            @Override
            public void onViewProfile(String userId) {
                // Open profile activity for this user
                Intent intent = new Intent(FriendsListActivity.this, ProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendAdapter);
    }

    private void setupButtons() {
        btnFriends.setOnClickListener(v -> {
            currentView = "friends";
            updateButtonStyles();
            loadFriends();
        });

        btnRequests.setOnClickListener(v -> {
            currentView = "requests";
            updateButtonStyles();
            loadFriendRequests();
        });

        btnSearch.setOnClickListener(v -> {
            currentView = "search";
            updateButtonStyles();
            friendAdapter.clearItems();
        });

        updateButtonStyles();
    }

    private void updateButtonStyles() {
        // Reset all buttons
        btnFriends.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        btnRequests.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        btnSearch.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));

        // Highlight selected button and show/hide search elements
        if (currentView.equals("friends")) {
            btnFriends.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
            searchInput.setVisibility(View.GONE);
            findViewById(R.id.btnSearchUser).setVisibility(View.GONE);
        } else if (currentView.equals("requests")) {
            btnRequests.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
            searchInput.setVisibility(View.GONE);
            findViewById(R.id.btnSearchUser).setVisibility(View.GONE);
        } else {
            btnSearch.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
            searchInput.setVisibility(View.VISIBLE);
            findViewById(R.id.btnSearchUser).setVisibility(View.VISIBLE);
        }
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("friends")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<Friend> friends = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Friend friend = doc.toObject(Friend.class);
                            if (friend != null && friend.involvesUser(currentUserId)) {
                                friend.friendshipId = doc.getId();
                                friends.add(friend);
                            }
                        }
                    }
                    friendAdapter.setFriends(friends);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void loadFriendRequests() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("friends")
                .whereEqualTo("status", "pending")
                .whereEqualTo("userId2", currentUserId) // Requests sent TO current user
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<Friend> requests = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Friend friend = doc.toObject(Friend.class);
                            if (friend != null) {
                                friend.friendshipId = doc.getId();
                                requests.add(friend);
                            }
                        }
                    }
                    friendAdapter.setFriendRequests(requests);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void searchUsers() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        // Convert query to lowercase for case-insensitive search
        String lowercaseQuery = query.toLowerCase();
        
        // Search for profiles where displayName contains the query (case insensitive)
        firestore.collection("profiles")
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Profile> profiles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Profile profile = doc.toObject(Profile.class);
                        if (profile != null && !profile.uid.equals(currentUserId)) {
                            // Also check if the lowercase displayName contains the lowercase query
                            String profileName = profile.displayName != null ? profile.displayName.toLowerCase() : "";
                            if (profileName.contains(lowercaseQuery) || 
                                (profile.actualName != null && profile.actualName.toLowerCase().contains(lowercaseQuery))) {
                                profiles.add(profile);
                            }
                        }
                    }
                    
                    // If no results from the ordered search, try a broader search
                    if (profiles.isEmpty()) {
                        firestore.collection("profiles")
                                .limit(100)
                                .get()
                                .addOnSuccessListener(allQuerySnapshot -> {
                                    for (DocumentSnapshot doc : allQuerySnapshot.getDocuments()) {
                                        Profile profile = doc.toObject(Profile.class);
                                        if (profile != null && !profile.uid.equals(currentUserId)) {
                                            String profileName = profile.displayName != null ? profile.displayName.toLowerCase() : "";
                                            String actualName = profile.actualName != null ? profile.actualName.toLowerCase() : "";
                                            if (profileName.contains(lowercaseQuery) || actualName.contains(lowercaseQuery)) {
                                                profiles.add(profile);
                                            }
                                        }
                                    }
                                    
                                    if (profiles.isEmpty()) {
                                        Toast.makeText(this, "No users found matching '" + query + "'", Toast.LENGTH_SHORT).show();
                                    }
                                    
                                    friendAdapter.setSearchResults(profiles);
                                    progressBar.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                });
                    } else {
                        friendAdapter.setSearchResults(profiles);
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void sendFriendRequest(String targetUserId) {
        // Check if friendship already exists
        firestore.collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean alreadyExists = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Friend existing = doc.toObject(Friend.class);
                        if (existing != null && existing.involvesUser(currentUserId) && existing.involvesUser(targetUserId)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        Friend newFriend = new Friend(currentUserId, targetUserId, currentUserId);
                        firestore.collection("friends")
                                .add(newFriend)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Friendship already exists", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void acceptFriendRequest(Friend friend) {
        firestore.collection("friends").document(friend.friendshipId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectFriendRequest(Friend friend) {
        firestore.collection("friends").document(friend.friendshipId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend request rejected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject request", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFriend(Friend friend) {
        firestore.collection("friends").document(friend.friendshipId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove friend", Toast.LENGTH_SHORT).show();
                });
    }
}