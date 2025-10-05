package com.rayseal.supportapp;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Profile {
    public String uid = "";
    public String displayName = "";
    public String actualName = "";
    public String bio = "";
    public String contact = "";
    public String profilePictureUrl = "";
    public String coverPhotoUrl = "";
    public List<String> supportCategories = new ArrayList<>();
    public PrivacySettings privacy = new PrivacySettings();
    public Timestamp memberSince = null; // Will be set properly when profile is created
    public int numPosts = 0;
    
    // Privacy settings for new features
    public boolean hidePostsFromFriends = false;
    public boolean isPrivate = false;
    
    // Admin and moderation features
    public boolean isAdmin = false;
    public boolean isBanned = false;
    public String bannedBy = ""; // Admin user ID who banned this user
    public long bannedAt = 0L;
    public String banReason = "";
    
    // Moderation history
    public int reportCount = 0; // Number of times this user has been reported
    public int warningCount = 0; // Number of warnings given to this user
    public long lastWarning = 0L;

    public Profile() {}

    /**
     * Get the member since date as a long timestamp for compatibility
     */
    public long getMemberSinceMillis() {
        if (memberSince != null) {
            return memberSince.getSeconds() * 1000;
        }
        return System.currentTimeMillis(); // Fallback to current time
    }

    /**
     * Set member since from a long timestamp (for migration from old format)
     */
    public void setMemberSinceFromMillis(long millis) {
        if (millis > 0) {
            memberSince = new Timestamp(millis / 1000, 0);
        } else {
            memberSince = Timestamp.now();
        }
    }
}
