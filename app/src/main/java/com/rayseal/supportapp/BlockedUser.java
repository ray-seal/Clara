package com.rayseal.supportapp;

import com.google.firebase.Timestamp;

/**
 * Model class for blocked users
 */
public class BlockedUser {
    public String blockId;
    public String blockerUserId; // User who blocked
    public String blockedUserId; // User who was blocked
    public String blockedUserName;
    public Timestamp blockedAt;
    public String reason; // Optional reason
    
    public BlockedUser() {
        this.blockedAt = Timestamp.now();
    }
    
    public BlockedUser(String blockerUserId, String blockedUserId, String blockedUserName) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
        this.blockedUserName = blockedUserName;
        this.blockedAt = Timestamp.now();
    }
    
    public BlockedUser(String blockerUserId, String blockedUserId, String blockedUserName, String reason) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
        this.blockedUserName = blockedUserName;
        this.reason = reason;
        this.blockedAt = Timestamp.now();
    }
}