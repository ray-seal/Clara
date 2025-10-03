package com.rayseal.supportapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for real-time messaging within a chat room.
 * Supports both topic-based rooms and user-created rooms.
 */
public class ChatRoomActivity extends AppCompatActivity {
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private Button btnSend;
    private TextView roomNameText, roomTopicText;
    private ChatMessageAdapter adapter;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore firestore;
    
    private String roomId;
    private String roomName;
    private String topic;
    private String currentUserId;
    private String currentUserName = "Anonymous";
    
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
        
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Get room details from intent
        roomId = getIntent().getStringExtra("roomId");
        roomName = getIntent().getStringExtra("roomName");
        topic = getIntent().getStringExtra("topic");

        if (roomId == null || roomId.isEmpty()) {
            finish();
            return;
        }

        initializeViews();
        loadUserProfile();
        setupRecyclerView();
        setupMessageListener();
        joinRoom();
    }

    private void initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        btnSend = findViewById(R.id.btnSend);
        roomNameText = findViewById(R.id.roomNameText);
        roomTopicText = findViewById(R.id.roomTopicText);

        roomNameText.setText(roomName != null ? roomName : "Chat Room");
        roomTopicText.setText(topic != null ? topic : "General");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRoomInfo).setOnClickListener(v -> showRoomInfo());
        
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadUserProfile() {
        firestore.collection("profiles").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String displayName = doc.getString("displayName");
                    if (displayName != null && !displayName.isEmpty()) {
                        currentUserName = displayName;
                    }
                }
            });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(adapter);
    }

    /**
     * Set up real-time listener for new messages in the room.
     */
    private void setupMessageListener() {
        messagesRef = mDatabase.child("messages").child(roomId);
        
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    adapter.addMessage(message);
                    messagesRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatRoomActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        };
        
        messagesRef.addChildEventListener(messagesListener);
    }

    /**
     * Join the room by adding current user to members list.
     */
    private void joinRoom() {
        DatabaseReference roomRef = mDatabase.child("chatRooms").child(roomId).child("members");
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isMember = false;
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    if (currentUserId.equals(memberSnapshot.getValue(String.class))) {
                        isMember = true;
                        break;
                    }
                }
                
                if (!isMember) {
                    // Add user to members
                    roomRef.push().setValue(currentUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Send a message to the room.
     */
    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage message = new ChatMessage(currentUserId, currentUserName, content, roomId);
        
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.messageId = messageId;
            
            messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    
                    // Update room's last message
                    DatabaseReference roomRef = mDatabase.child("chatRooms").child(roomId);
                    roomRef.child("lastMessage").setValue(content);
                    roomRef.child("lastMessageTime").setValue(message.timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatRoomActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
        }
    }

    /**
     * Show room information dialog.
     */
    private void showRoomInfo() {
        mDatabase.child("chatRooms").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ChatRoom room = snapshot.getValue(ChatRoom.class);
                if (room != null) {
                    String info = "Room: " + room.roomName + "\n" +
                                "Topic: " + room.topic + "\n" +
                                "Type: " + (room.isPrivate ? "Private" : "Public") + "\n" +
                                "Members: " + (room.members != null ? room.members.size() : 0);
                    
                    if (room.description != null && !room.description.isEmpty()) {
                        info += "\n\nDescription: " + room.description;
                    }
                    
                    new android.app.AlertDialog.Builder(ChatRoomActivity.this)
                        .setTitle("Room Information")
                        .setMessage(info)
                        .setPositiveButton("OK", null)
                        .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}
