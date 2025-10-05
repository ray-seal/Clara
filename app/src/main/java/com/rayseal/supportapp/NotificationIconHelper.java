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
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        unreadListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("recipientId", currentUserId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null || querySnapshot == null) {
                    return;
                }
                
                int unreadCount = querySnapshot.size();
                updateBadge(unreadCount);
            });
    }
    
    private void updateBadge(int count) {
        if (count > 0) {
            badgeText.setVisibility(View.VISIBLE);
            if (count > 99) {
                badgeText.setText("99+");
            } else {
                badgeText.setText(String.valueOf(count));
            }
        } else {
            badgeText.setVisibility(View.GONE);
        }
    }
    
    public void cleanup() {
        if (unreadListener != null) {
            unreadListener.remove();
        }
    }
}