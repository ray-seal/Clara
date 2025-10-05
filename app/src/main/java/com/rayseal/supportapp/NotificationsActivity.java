package com.rayseal.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    
    private RecyclerView notificationsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ImageView backButton, markAllReadButton;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            finish();
            return;
        }

        // Initialize views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        markAllReadButton = findViewById(R.id.markAllReadButton);

        // Setup RecyclerView
        notificationAdapter = new NotificationAdapter(notifications, this);
        notificationAdapter.setOnNotificationClickListener(this);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);

        // Setup click listeners
        backButton.setOnClickListener(v -> finish());
        markAllReadButton.setOnClickListener(v -> markAllAsRead());

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        db.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    android.util.Log.e("NotificationsActivity", "Error loading notifications", error);
                    return;
                }

                notifications.clear();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.notificationId = doc.getId();
                        notifications.add(notification);
                    }
                }

                notificationAdapter.notifyDataSetChanged();
                
                // Show/hide empty state
                if (notifications.isEmpty()) {
                    notificationsRecyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                } else {
                    notificationsRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                }
            });
    }

    private void markAllAsRead() {
        notificationAdapter.markAllAsRead();
    }

    @Override
    public void onNotificationClick(Notification notification) {
        // Handle notification click based on type
        switch (notification.type) {
            case "reaction":
            case "comment":
                if (notification.relatedPostId != null) {
                    // Navigate to the specific post
                    Intent intent = new Intent(this, PublicFeedActivity.class);
                    intent.putExtra("scrollToPostId", notification.relatedPostId);
                    startActivity(intent);
                }
                break;
            case "friend_request":
                // Navigate to friends list
                Intent friendsIntent = new Intent(this, FriendsListActivity.class);
                friendsIntent.putExtra("showRequests", true);
                startActivity(friendsIntent);
                break;
            case "friend_accepted":
                if (notification.fromUserId != null) {
                    // Navigate to the friend's profile
                    Intent profileIntent = new Intent(this, ProfileActivity.class);
                    profileIntent.putExtra("viewingUserId", notification.fromUserId);
                    startActivity(profileIntent);
                }
                break;
        }
    }
}