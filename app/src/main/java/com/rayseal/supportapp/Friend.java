package com.rayseal.supportapp;

/**
 * Data model for friend relationships.
 * Stores information about friendship between users.
 */
public class Friend {
    public String friendshipId;
    public String userId1;
    public String userId2;
    public String status; // "pending", "accepted", "blocked"
    public long timestamp;
    public String requesterId; // Who sent the friend request

    public Friend() {
        // Required empty constructor for Firebase
    }

    public Friend(String userId1, String userId2, String requesterId) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.requesterId = requesterId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the other user's ID (not the current user)
     */
    public String getOtherUserId(String currentUserId) {
        return currentUserId.equals(userId1) ? userId2 : userId1;
    }

    /**
     * Check if this friendship involves a specific user
     */
    public boolean involvesUser(String userId) {
        return userId1.equals(userId) || userId2.equals(userId);
    }
}