package com.rayseal.supportapp;

import com.google.firebase.Timestamp;

/**
 * Model class for flagged content (automatic keyword detection)
 */
public class FlaggedContent {
    public String flagId;
    public String contentType; // "post", "comment", "chat_message"
    public String contentId; // Post ID, Comment ID, or Message ID
    public String authorUserId;
    public String authorName;
    public String content; // The actual content that was flagged
    public String[] flaggedWords; // Array of words that triggered the flag
    public String flagReason; // "profanity", "hate_speech", "harassment", "spam"
    public Timestamp flaggedAt;
    public String status; // "pending", "approved", "rejected"
    public String reviewedBy; // Admin user ID
    public Timestamp reviewedAt;
    public String adminNotes;
    public boolean contentVisible; // Whether content is visible to users while pending
    
    // For post content
    public String postId;
    public String[] postCategories;
    
    // For chat message content
    public String chatRoomId;
    public String chatRoomName;
    
    public FlaggedContent() {
        this.flaggedAt = Timestamp.now();
        this.status = "pending";
        this.contentVisible = false; // Hide by default until approved
    }
    
    public FlaggedContent(String contentType, String contentId, String authorUserId, 
                         String authorName, String content, String[] flaggedWords, String flagReason) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.authorUserId = authorUserId;
        this.authorName = authorName;
        this.content = content;
        this.flaggedWords = flaggedWords;
        this.flagReason = flagReason;
        this.flaggedAt = Timestamp.now();
        this.status = "pending";
        this.contentVisible = false;
    }
}