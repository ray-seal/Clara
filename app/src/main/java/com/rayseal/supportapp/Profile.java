package com.rayseal.supportapp;

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
    public long memberSince = 0L;
    public int numPosts = 0;

    public Profile() {}
}
