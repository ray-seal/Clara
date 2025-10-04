package com.rayseal.supportapp;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class Post {
  public String postId;
  public String content;
  public List<String> categories;
  public String imageUrl;
  public String userId;
  public String authorName;
  public String authorProfilePicture;
  public long timestamp;
  public Map<String, Integer> reactions; // reaction type -> count
  public Map<String, List<String>> userReactions; // reaction type -> list of user IDs
  public int commentCount;
  
  public Post() {
    // Default constructor for Firebase
    this.reactions = new HashMap<>();
    this.userReactions = new HashMap<>();
    this.commentCount = 0;
  }
  
  public Post(String content, List<String> categories) {
    this.content = content;
    this.categories = categories;
    this.imageUrl = null;
    this.reactions = new HashMap<>();
    this.userReactions = new HashMap<>();
    this.commentCount = 0;
  }
  
  public Post(String content, List<String> categories, String imageUrl) {
    this.content = content;
    this.categories = categories;
    this.imageUrl = imageUrl;
    this.reactions = new HashMap<>();
    this.userReactions = new HashMap<>();
    this.commentCount = 0;
  }
  
  public Post(String postId, String content, List<String> categories, String imageUrl, 
              String userId, String authorName, String authorProfilePicture, long timestamp) {
    this.postId = postId;
    this.content = content != null ? content : "";
    this.categories = categories != null ? categories : new ArrayList<>();
    this.imageUrl = imageUrl;
    this.userId = userId != null ? userId : "";
    this.authorName = authorName != null && !authorName.isEmpty() ? authorName : "Anonymous";
    this.authorProfilePicture = authorProfilePicture != null ? authorProfilePicture : "";
    this.timestamp = timestamp;
    this.reactions = new HashMap<>();
    this.userReactions = new HashMap<>();
    this.commentCount = 0;
  }
}
