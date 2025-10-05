package com.rayseal.supportapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class NotificationIconHelper {
    private final Context context;
    private final View notificationIconLayout;
    private final TextView badgeText;
    private ListenerRegistration unreadListener;
    
    public NotificationIconHelper(Context context, View notificationIconLayout) {
        this.context = context;
        this.notificationIconLayout = notificationIconLayout;
        this.badgeText = notificationIconLayout.findViewById(R.id.notificationBadge);
        
        setupClickListener();
        startListeningForUnreadNotifications();
    }
    
    private void setupClickListener() {
        notificationIconLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, NotificationsActivity.class);
            context.startActivity(intent);
        });
    }
    
    private void startListeningForUnreadNotifications() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            android.util.Log.w("NotificationIconHelper", "No current user, cannot listen for notifications");
            return;
        }
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        android.util.Log.d("NotificationIconHelper", "Starting notification listener for user: " + currentUserId);
        
        unreadListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    android.util.Log.e("NotificationIconHelper", "Error listening for notifications", error);
                    return;
                }
                
                if (querySnapshot == null) {
                    android.util.Log.w("NotificationIconHelper", "QuerySnapshot is null");
                    return;
                }
                
                int unreadCount = querySnapshot.size();
                android.util.Log.d("NotificationIconHelper", "Found " + unreadCount + " unread notifications");
                updateBadge(unreadCount);
            });
    }
    
    private void updateBadge(int count) {
        android.util.Log.d("NotificationIconHelper", "Updating badge with count: " + count);
        if (badgeText == null) {
            android.util.Log.e("NotificationIconHelper", "Badge text view is null!");
            return;
        }
        
        if (count > 0) {
            badgeText.setVisibility(View.VISIBLE);
            if (count > 99) {
                badgeText.setText("99+");
            } else {
                badgeText.setText(String.valueOf(count));
            }
            android.util.Log.d("NotificationIconHelper", "Badge set to visible with text: " + badgeText.getText());
        } else {
            badgeText.setVisibility(View.GONE);
            android.util.Log.d("NotificationIconHelper", "Badge hidden (no unread notifications)");
        }
    }
    
    public void cleanup() {
        if (unreadListener != null) {
            unreadListener.remove();
        }
    }
}