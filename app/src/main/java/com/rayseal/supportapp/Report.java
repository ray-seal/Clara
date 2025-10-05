package com.rayseal.supportapp;

import com.google.firebase.Timestamp;

/**
 * Model class for user reports (posts, users, chat messages)
 */
public class Report {
    public String reportId;
    public String reportType; // "post", "user", "chat_message"
    public String reportedItemId; // Post ID, User ID, or Message ID
    public String reportedUserId; // User being reported
    public String reporterUserId; // User making the report
    public String reporterName;
    public String reportReason; // "spam", "harassment", "inappropriate_content", "hate_speech", "other"
    public String reportDescription; // Additional details
    public Timestamp reportTimestamp;
    public String status; // "pending", "reviewed", "resolved", "dismissed"
    public String reviewedBy; // Admin user ID who reviewed
    public Timestamp reviewedAt;
    public String adminNotes; // Admin comments
    public String actionTaken; // "no_action", "warning", "content_removed", "user_banned"
    
    // For post reports
    public String postContent;
    public String postAuthor;
    
    // For chat message reports
    public String messageContent;
    public String chatRoomId;
    public String chatRoomName;
    
    public Report() {
        this.reportTimestamp = Timestamp.now();
        this.status = "pending";
    }
    
    public Report(String reportType, String reportedItemId, String reportedUserId, 
                  String reporterUserId, String reporterName, String reportReason, String reportDescription) {
        this.reportType = reportType;
        this.reportedItemId = reportedItemId;
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.reporterName = reporterName;
        this.reportReason = reportReason;
        this.reportDescription = reportDescription;
        this.reportTimestamp = Timestamp.now();
        this.status = "pending";
    }
}