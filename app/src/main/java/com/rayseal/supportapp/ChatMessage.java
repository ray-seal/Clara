package com.rayseal.supportapp;

/**
 * Model class representing a chat message.
 * Used for both room messages and private messages.
 */
public class ChatMessage {
    public String messageId = "";
    public String senderId = "";
    public String senderName = "";
    public String content = "";
    public long timestamp = 0L;
    public String roomId = ""; // For room messages
    public String recipientId = ""; // For private messages

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String content, String roomId) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.roomId = roomId;
    }

    public ChatMessage(String senderId, String senderName, String content, String recipientId, boolean isPrivate) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.recipientId = recipientId;
    }
}
