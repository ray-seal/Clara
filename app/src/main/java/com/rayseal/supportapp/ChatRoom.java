package com.rayseal.supportapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a chat room.
 * Supports both public and private rooms, with topic-based categorization.
 */
public class ChatRoom {
    public String roomId = "";
    public String roomName = "";
    public String topic = ""; // Anxiety, Depression, Insomnia, Gender Dysphoria, Disability, Addiction, or Other
    public String description = "";
    public boolean isPrivate = false;
    public String createdBy = ""; // User ID of creator
    public long createdAt = 0L;
    public List<String> members = new ArrayList<>(); // List of user IDs who are members
    public String lastMessage = "";
    public long lastMessageTime = 0L;

    public ChatRoom() {}

    public ChatRoom(String roomId, String roomName, String topic, boolean isPrivate, String createdBy) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.topic = topic;
        this.isPrivate = isPrivate;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.members = new ArrayList<>();
        if (createdBy != null && !createdBy.isEmpty()) {
            this.members.add(createdBy);
        }
    }
}
