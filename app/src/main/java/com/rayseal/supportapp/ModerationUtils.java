package com.rayseal.supportapp;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for admin privileges and content moderation
 */
public class ModerationUtils {
    
    private static final String TAG = "ModerationUtils";
    
    // Keyword lists for content flagging
    private static final Set<String> PROFANITY_WORDS = new HashSet<>(Arrays.asList(
        "damn", "hell", "crap", "shit", "fuck", "bitch", "ass", "asshole"
        // Add more as needed - keeping it minimal for now
    ));
    
    private static final Set<String> HATE_SPEECH_WORDS = new HashSet<>(Arrays.asList(
        "hate", "kill yourself", "kys", "die", "suicide", "retard", "stupid", "idiot"
        // Add more hate speech patterns
    ));
    
    private static final Set<String> HARASSMENT_WORDS = new HashSet<>(Arrays.asList(
        "stalker", "creep", "ugly", "fat", "loser", "worthless", "pathetic"
        // Add more harassment terms
    ));
    
    // Patterns for more complex detection
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    
    /**
     * Check if current user is admin
     */
    public static void checkAdminStatus(OnAdminCheckListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            listener.onAdminCheck(false);
            return;
        }
        
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(currentUserId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    boolean isAdmin = profile != null && profile.isAdmin;
                    listener.onAdminCheck(isAdmin);
                } else {
                    listener.onAdminCheck(false);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error checking admin status", e);
                listener.onAdminCheck(false);
            });
    }
    
    /**
     * Check if a user is banned
     */
    public static void checkBanStatus(String userId, OnBanCheckListener listener) {
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    boolean isBanned = profile != null && profile.isBanned;
                    listener.onBanCheck(isBanned, profile);
                } else {
                    listener.onBanCheck(false, null);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error checking ban status", e);
                listener.onBanCheck(false, null);
            });
    }
    
    /**
     * Analyze content for inappropriate material
     */
    public static ContentAnalysis analyzeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ContentAnalysis(false, "", new String[0]);
        }
        
        String lowerContent = content.toLowerCase();
        Set<String> flaggedWords = new HashSet<>();
        String flagReason = "";
        
        // Check for profanity
        for (String word : PROFANITY_WORDS) {
            if (lowerContent.contains(word)) {
                flaggedWords.add(word);
                flagReason = "profanity";
            }
        }
        
        // Check for hate speech
        for (String word : HATE_SPEECH_WORDS) {
            if (lowerContent.contains(word)) {
                flaggedWords.add(word);
                flagReason = "hate_speech";
            }
        }
        
        // Check for harassment
        for (String word : HARASSMENT_WORDS) {
            if (lowerContent.contains(word)) {
                flaggedWords.add(word);
                if (flagReason.isEmpty()) flagReason = "harassment";
            }
        }
        
        // Check for personal information sharing
        if (PHONE_PATTERN.matcher(content).find() || EMAIL_PATTERN.matcher(content).find()) {
            flaggedWords.add("personal_info");
            if (flagReason.isEmpty()) flagReason = "personal_info_sharing";
        }
        
        boolean shouldFlag = !flaggedWords.isEmpty();
        String[] flaggedArray = flaggedWords.toArray(new String[0]);
        
        return new ContentAnalysis(shouldFlag, flagReason, flaggedArray);
    }
    
    /**
     * Create a report for inappropriate content or user
     */
    public static void createReport(String reportType, String reportedItemId, String reportedUserId,
                                   String reportReason, String reportDescription, Context context) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            Toast.makeText(context, "Please sign in to report content", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user's profile for reporter name
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(currentUserId)
            .get()
            .addOnSuccessListener(doc -> {
                String reporterName = "Anonymous";
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    if (profile != null && profile.displayName != null && !profile.displayName.isEmpty()) {
                        reporterName = profile.displayName;
                    }
                }
                
                // Create the report
                Report report = new Report(reportType, reportedItemId, reportedUserId,
                    currentUserId, reporterName, reportReason, reportDescription);
                
                FirebaseFirestore.getInstance()
                    .collection("reports")
                    .add(report)
                    .addOnSuccessListener(documentReference -> {
                        report.reportId = documentReference.getId();
                        documentReference.update("reportId", report.reportId);
                        
                        Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                        
                        // Notify all admins
                        notifyAdminsOfReport(report, context);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(TAG, "Error creating report", e);
                        Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show();
                    });
            });
    }
    
    /**
     * Notify all admin users of a new report
     */
    private static void notifyAdminsOfReport(Report report, Context context) {
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .whereEqualTo("isAdmin", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (Profile adminProfile : querySnapshot.toObjects(Profile.class)) {
                    if (adminProfile.uid != null && !adminProfile.uid.isEmpty()) {
                        // Create notification for admin
                        Notification notification = new Notification(
                            adminProfile.uid,
                            "admin_report",
                            "New Report",
                            "A new " + report.reportType + " has been reported for review",
                            report.reporterUserId,
                            report.reporterName,
                            ""
                        );
                        notification.relatedPostId = report.reportedItemId;
                        notification.actionData = "{\"reportId\":\"" + report.reportId + "\"}";
                        
                        FirebaseFirestore.getInstance()
                            .collection("notifications")
                            .add(notification)
                            .addOnSuccessListener(docRef -> {
                                android.util.Log.d(TAG, "Admin notification sent to: " + adminProfile.uid);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(TAG, "Failed to send admin notification", e);
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error getting admin users for notification", e);
            });
    }
    
    /**
     * General method to notify all admins
     */
    public static void notifyAdmins(String title, String message, String relatedId) {
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .whereEqualTo("isAdmin", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (Profile adminProfile : querySnapshot.toObjects(Profile.class)) {
                    if (adminProfile.uid != null && !adminProfile.uid.isEmpty()) {
                        // Create notification for admin
                        Notification notification = new Notification(
                            adminProfile.uid,
                            "admin_notification",
                            title,
                            message,
                            "",
                            "",
                            ""
                        );
                        notification.relatedPostId = relatedId;
                        
                        FirebaseFirestore.getInstance()
                            .collection("notifications")
                            .add(notification)
                            .addOnSuccessListener(docRef -> {
                                android.util.Log.d(TAG, "Admin notification sent to: " + adminProfile.uid);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(TAG, "Failed to send admin notification", e);
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error getting admin users for notification", e);
            });
    }
    
    /**
     * Check if user is blocked by current user
     */
    public static void checkIfBlocked(String userId, OnBlockCheckListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            listener.onBlockCheck(false);
            return;
        }
        
        FirebaseFirestore.getInstance()
            .collection("blocked_users")
            .whereEqualTo("blockerUserId", currentUserId)
            .whereEqualTo("blockedUserId", userId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                listener.onBlockCheck(!querySnapshot.isEmpty());
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error checking block status", e);
                listener.onBlockCheck(false);
            });
    }

    /**
     * Get the current user's name
     */
    public static void getCurrentUserName(OnUserNameListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            listener.onUserName("Anonymous");
            return;
        }
        
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(currentUserId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    String userName = (profile != null && profile.displayName != null && !profile.displayName.isEmpty()) 
                        ? profile.displayName : "Anonymous";
                    listener.onUserName(userName);
                } else {
                    listener.onUserName("Anonymous");
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error getting user name", e);
                listener.onUserName("Anonymous");
            });
    }

    /**
     * Send notification to Admin chat room
     */
    public static void sendNotificationToAdminChat(String content) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : "system";
        
        DatabaseReference adminChatRef = FirebaseDatabase.getInstance()
            .getReference("chatRooms/Admin/messages");
        
        // Create the message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", "System");
        messageData.put("content", content);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("roomId", "Admin");
        
        // Push to Firebase
        DatabaseReference newMessageRef = adminChatRef.push();
        messageData.put("messageId", newMessageRef.getKey());
        
        newMessageRef.setValue(messageData)
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d(TAG, "Admin chat notification sent successfully");
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Failed to send admin chat notification", e);
            });
    }
    
    // Callback interfaces
    public interface OnAdminCheckListener {
        void onAdminCheck(boolean isAdmin);
    }
    
    public interface OnBanCheckListener {
        void onBanCheck(boolean isBanned, Profile profile);
    }
    
    public interface OnBlockCheckListener {
        void onBlockCheck(boolean isBlocked);
    }

    public interface OnUserNameListener {
        void onUserName(String userName);
    }
    
    // Content analysis result class
    public static class ContentAnalysis {
        public boolean shouldFlag;
        public String flagReason;
        public String[] flaggedWords;
        
        public ContentAnalysis(boolean shouldFlag, String flagReason, String[] flaggedWords) {
            this.shouldFlag = shouldFlag;
            this.flagReason = flagReason;
            this.flaggedWords = flaggedWords;
        }
    }
}