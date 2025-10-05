package com.rayseal.supportapp;

import com.google.firebase.Timestamp;

public class Notification {
    public String notificationId;
    public String userId; // recipient user ID
    public String type; // "reaction", "comment", "friend_request", "friend_accepted", "post_mention"
    public String title;
    public String message;
    public String fromUserId; // user who triggered the notification
    public String fromUserName;
    public String fromUserProfilePicture;
    public String relatedPostId; // for post-related notifications
    public String relatedCommentId; // for comment-related notifications
    public Timestamp timestamp;
    public boolean isRead;
    public String actionData; // JSON string for additional action data

    public Notification() {
        // Default constructor required for calls to DataSnapshot.getValue(Notification.class)
        this.isRead = false;
        this.timestamp = Timestamp.now();
    }

    public Notification(String userId, String type, String title, String message, String fromUserId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.fromUserId = fromUserId;
        this.timestamp = Timestamp.now();
        this.isRead = false;
    }

    public Notification(String userId, String type, String title, String message, String fromUserId, 
                       String fromUserName, String fromUserProfilePicture) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserProfilePicture = fromUserProfilePicture;
        this.timestamp = Timestamp.now();
        this.isRead = false;
    }

    public Notification(String userId, String type, String title, String message, String fromUserId, 
                       String fromUserName, String fromUserProfilePicture, String relatedPostId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserProfilePicture = fromUserProfilePicture;
        this.relatedPostId = relatedPostId;
        this.timestamp = Timestamp.now();
        this.isRead = false;
    }

    // Helper methods for notification types
    public static Notification createReactionNotification(String recipientUserId, String fromUserId, 
                                                         String fromUserName, String fromUserProfilePicture, 
                                                         String postId, String reactionType) {
        String title = "New Reaction";
        String message = fromUserName + " reacted to your post";
        Notification notification = new Notification(recipientUserId, "reaction", title, message, 
                                                    fromUserId, fromUserName, fromUserProfilePicture, postId);
        notification.actionData = "{\"reactionType\":\"" + reactionType + "\"}";
        return notification;
    }

    public static Notification createCommentNotification(String recipientUserId, String fromUserId, 
                                                        String fromUserName, String fromUserProfilePicture, 
                                                        String postId, String commentId) {
        String title = "New Comment";
        String message = fromUserName + " commented on your post";
        Notification notification = new Notification(recipientUserId, "comment", title, message, 
                                                    fromUserId, fromUserName, fromUserProfilePicture, postId);
        notification.relatedCommentId = commentId;
        return notification;
    }

    public static Notification createFriendRequestNotification(String recipientUserId, String fromUserId, 
                                                              String fromUserName, String fromUserProfilePicture) {
        String title = "Friend Request";
        String message = fromUserName + " sent you a friend request";
        return new Notification(recipientUserId, "friend_request", title, message, 
                               fromUserId, fromUserName, fromUserProfilePicture);
    }

    public static Notification createFriendAcceptedNotification(String recipientUserId, String fromUserId, 
                                                               String fromUserName, String fromUserProfilePicture) {
        String title = "Friend Request Accepted";
        String message = fromUserName + " accepted your friend request";
        return new Notification(recipientUserId, "friend_accepted", title, message, 
                               fromUserId, fromUserName, fromUserProfilePicture);
    }
}