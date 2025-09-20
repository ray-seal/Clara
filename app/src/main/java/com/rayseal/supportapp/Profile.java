package com.rayseal.supportapp;

import java.util.List;

public class Profile {
    public String uid;
    public String displayName;
    public String actualName;
    public String profilePictureUrl;
    public String coverPhotoUrl;
    public List<String> supportCategories;
    public String bio;
    public String contact;
    public long memberSince;
    public int numPosts;
    public PrivacySettings privacy;

    public Profile() {}
    
    public Profile(String uid, String displayName, String actualName, String profilePictureUrl, String coverPhotoUrl, List<String> supportCategories, String bio, String contact, long memberSince, int numPosts, PrivacySettings privacy) {
        this.uid = uid;
        this.displayName = displayName;
        this.actualName = actualName;
        this.profilePictureUrl = profilePictureUrl;
        this.coverPhotoUrl = coverPhotoUrl;
        this.supportCategories = supportCategories;
        this.bio = bio;
        this.contact = contact;
        this.memberSince = memberSince;
        this.numPosts = numPosts;
        this.privacy = privacy;
}
}