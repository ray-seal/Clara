// Alternative Android implementation to call HTTP Cloud Function
// Add this to PostAdapter.java or create a new NotificationService class

import okhttp3.*;
import java.io.IOException;
import org.json.JSONObject;

public class NotificationService {
    private static final String CLOUD_FUNCTION_URL = "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/sendNotification";
    private static final OkHttpClient client = new OkHttpClient();
    
    public static void sendPushNotification(String userId, String title, String message, String type, String postId) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("title", title);
                json.put("message", message);
                json.put("type", type);
                json.put("postId", postId);
                
                RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(CLOUD_FUNCTION_URL)
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        android.util.Log.d("NotificationService", "Push notification sent successfully");
                    } else {
                        android.util.Log.e("NotificationService", "Failed to send push notification: " + response.code());
                    }
                }
                
            } catch (Exception e) {
                android.util.Log.e("NotificationService", "Error sending push notification", e);
            }
        }).start();
    }
}

// Usage in PostAdapter.java:
// Replace the existing sendPushNotification method with:
private void sendPushNotification(String recipientUserId, Notification notification) {
    NotificationService.sendPushNotification(
        recipientUserId,
        notification.title,
        notification.message,
        notification.type,
        notification.relatedPostId
    );
}