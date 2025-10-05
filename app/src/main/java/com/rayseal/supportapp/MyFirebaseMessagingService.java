package com.rayseal.supportapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "clara_notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            // Handle data payload here
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            
            if (title != null && body != null) {
                showNotification(title, body, remoteMessage.getData());
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Send the token to your server
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("fcm_tokens")
                .document(userId)
                .set(new FCMToken(userId, token))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
        }
    }

    private void showNotification(String title, String messageBody, java.util.Map<String, String> data) {
        Intent intent = getNotificationIntent(data);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

    private Intent getNotificationIntent(java.util.Map<String, String> data) {
        String type = data.get("type");
        String postId = data.get("postId");
        String userId = data.get("userId");
        
        Intent intent;
        
        if ("reaction".equals(type) || "comment".equals(type)) {
            intent = new Intent(this, PublicFeedActivity.class);
            if (postId != null) {
                intent.putExtra("scrollToPostId", postId);
            }
        } else if ("friend_request".equals(type)) {
            intent = new Intent(this, FriendsListActivity.class);
            intent.putExtra("showRequests", true);
        } else if ("friend_accepted".equals(type) && userId != null) {
            intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("viewingUserId", userId);
        } else {
            intent = new Intent(this, NotificationsActivity.class);
        }
        
        return intent;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Clara Notifications";
            String description = "Notifications for Clara support app";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    // FCM Token model class
    public static class FCMToken {
        public String userId;
        public String token;
        public long timestamp;
        
        public FCMToken() {
            // Required for Firestore
        }
        
        public FCMToken(String userId, String token) {
            this.userId = userId;
            this.token = token;
            this.timestamp = System.currentTimeMillis();
        }
    }
}