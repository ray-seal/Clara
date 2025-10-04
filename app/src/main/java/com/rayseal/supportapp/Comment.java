package com.rayseal.supportapp;

public class Comment {
    public String commentId;
    public String postId;
    public String userId;
    public String authorName;
    public String authorProfilePicture;
    public String content;
    public long timestamp;
    
    public Comment() {
        // Default constructor for Firebase
    }
    
    public Comment(String postId, String userId, String authorName, 
                  String authorProfilePicture, String content, long timestamp) {
        this.postId = postId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorProfilePicture = authorProfilePicture;
        this.content = content;
        this.timestamp = timestamp;
    }
}