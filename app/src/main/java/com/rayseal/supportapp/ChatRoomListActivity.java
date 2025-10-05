package com.rayseal.supportapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for displaying and managing chat rooms.
 * Shows topic-based rooms, public rooms, and private rooms.
 */
public class ChatRoomListActivity extends AppCompatActivity {
    private RecyclerView roomsRecyclerView;
    private ChatRoomAdapter adapter;
    private ProgressBar progressBar;
    private Button btnTopicRooms, btnPublicRooms, btnPrivateRooms;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore firestore;

    private String currentUserId;
    private String currentFilter = "topic"; // topic, public, or private

    private static final String[] TOPIC_ROOMS = {
        "Anxiety", "Depression", "Insomnia",
        "Gender Dysphoria", "Disability", "Addiction"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_list);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to access chat", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        setupButtons();
        initializeTopicRooms();
        loadRooms("topic");
    }

    private void initializeViews() {
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnTopicRooms = findViewById(R.id.btnTopicRooms);
        btnPublicRooms = findViewById(R.id.btnPublicRooms);
        btnPrivateRooms = findViewById(R.id.btnPrivateRooms);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreateRoom).setOnClickListener(v -> showCreateRoomDialog());
    }

    private void setupRecyclerView() {
        adapter = new ChatRoomAdapter(room -> {
            // Only private rooms require the user to be a member (unless admin)
            if (room.isPrivate) {
                // Check if current user is admin first
                ModerationUtils.checkAdminStatus(isAdmin -> {
                    if (isAdmin) {
                        // Admins have access to all private rooms
                        Intent intent = new Intent(ChatRoomListActivity.this, ChatRoomActivity.class);
                        intent.putExtra("roomId", room.roomId);
                        intent.putExtra("roomName", room.roomName);
                        intent.putExtra("topic", room.topic);
                        startActivity(intent);
                        return;
                    }
                    
                    // Regular access check for non-admin users
                    if (room.members == null || !room.members.contains(currentUserId)) {
                        Toast.makeText(this, "You don't have access to this private room", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // User has access, proceed to chat room
                    Intent intent = new Intent(ChatRoomListActivity.this, ChatRoomActivity.class);
                    intent.putExtra("roomId", room.roomId);
                    intent.putExtra("roomName", room.roomName);
                    intent.putExtra("topic", room.topic);
                    startActivity(intent);
                });
            } else {
                // Public and topic rooms: anyone can enter
                Intent intent = new Intent(ChatRoomListActivity.this, ChatRoomActivity.class);
                intent.putExtra("roomId", room.roomId);
                intent.putExtra("roomName", room.roomName);
                intent.putExtra("topic", room.topic);
                startActivity(intent);
            }
        });

        roomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomsRecyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        btnTopicRooms.setOnClickListener(v -> {
            currentFilter = "topic";
            updateButtonStyles();
            loadRooms("topic");
        });

        btnPublicRooms.setOnClickListener(v -> {
            currentFilter = "public";
            updateButtonStyles();
            loadRooms("public");
        });

        btnPrivateRooms.setOnClickListener(v -> {
            currentFilter = "private";
            updateButtonStyles();
            loadRooms("private");
        });

        updateButtonStyles();
    }

    private void updateButtonStyles() {
        // Reset all buttons
        btnTopicRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        btnPublicRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        btnPrivateRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));

        // Highlight selected button
        int activeColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
        if (currentFilter.equals("topic")) {
            btnTopicRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_light));
        } else if (currentFilter.equals("public")) {
            btnPublicRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_light));
        } else {
            btnPrivateRooms.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_light));
        }
    }

    /**
     * Initialize predefined topic-based rooms if they don't exist.
     */
    private void initializeTopicRooms() {
        DatabaseReference topicRoomsRef = mDatabase.child("chatRooms");

        for (String topic : TOPIC_ROOMS) {
            String roomId = "topic_" + topic.toLowerCase().replace(" ", "_");

            topicRoomsRef.child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        // Create the topic room
                        ChatRoom room = new ChatRoom(
                                roomId,
                                topic + " Support",
                                topic,
                                false,
                                "system"
                        );
                        room.description = "A safe space to discuss " + topic.toLowerCase() + " and related topics.";
                        topicRoomsRef.child(roomId).setValue(room);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }
    }

    /**
     * Load rooms based on the current filter.
     */
    private void loadRooms(String filter) {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference roomsRef = mDatabase.child("chatRooms");
        Query query;

        if (filter.equals("topic")) {
            // Load topic-based rooms
            List<String> topicIds = new ArrayList<>();
            for (String topic : TOPIC_ROOMS) {
                topicIds.add("topic_" + topic.toLowerCase().replace(" ", "_"));
            }
            loadSpecificRooms(topicIds);
        } else if (filter.equals("public")) {
            // Load public rooms
            query = roomsRef.orderByChild("isPrivate").equalTo(false);
            loadRoomsFromQuery(query);
        } else {
            // Load private rooms where user is a member
            loadPrivateRooms();
        }
    }

    private void loadSpecificRooms(List<String> roomIds) {
        List<ChatRoom> rooms = new ArrayList<>();
        final int[] loadedCount = {0};

        for (String roomId : roomIds) {
            mDatabase.child("chatRooms").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ChatRoom room = snapshot.getValue(ChatRoom.class);
                    if (room != null) {
                        rooms.add(room);
                    }
                    loadedCount[0]++;

                    if (loadedCount[0] == roomIds.size()) {
                        adapter.setRooms(rooms);
                        progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == roomIds.size()) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void loadRoomsFromQuery(Query query) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatRoom> rooms = new ArrayList<>();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    ChatRoom room = roomSnapshot.getValue(ChatRoom.class);
                    if (room != null && !room.roomId.startsWith("topic_")) {
                        rooms.add(room);
                    }
                }
                adapter.setRooms(rooms);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatRoomListActivity.this, "Error loading rooms", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadPrivateRooms() {
        mDatabase.child("chatRooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatRoom> rooms = new ArrayList<>();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    ChatRoom room = roomSnapshot.getValue(ChatRoom.class);
                    if (room != null && room.isPrivate && room.members != null && room.members.contains(currentUserId)) {
                        rooms.add(room);
                    }
                }
                adapter.setRooms(rooms);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatRoomListActivity.this, "Error loading rooms", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Show dialog to create a new room.
     */
    private void showCreateRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Chat Room");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText roomNameInput = new EditText(this);
        roomNameInput.setHint("Room Name");
        layout.addView(roomNameInput);

        final Spinner topicSpinner = new Spinner(this);
        String[] topics = {"Other", "Anxiety", "Depression", "Insomnia", "Gender Dysphoria", "Disability", "Addiction"};
        ArrayAdapter<String> topicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topics);
        topicSpinner.setAdapter(topicAdapter);
        layout.addView(topicSpinner);

        final CheckBox privateCheckBox = new CheckBox(this);
        privateCheckBox.setText("Make this room private");
        layout.addView(privateCheckBox);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String roomName = roomNameInput.getText().toString().trim();
            String topic = topicSpinner.getSelectedItem().toString();
            boolean isPrivate = privateCheckBox.isChecked();

            if (roomName.isEmpty()) {
                Toast.makeText(this, "Please enter a room name", Toast.LENGTH_SHORT).show();
                return;
            }

            createRoom(roomName, topic, isPrivate);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Create a new chat room in Firebase.
     */
    private void createRoom(String roomName, String topic, boolean isPrivate) {
        String roomId = mDatabase.child("chatRooms").push().getKey();
        if (roomId == null) return;

        ChatRoom room = new ChatRoom(roomId, roomName, topic, isPrivate, currentUserId);

        mDatabase.child("chatRooms").child(roomId).setValue(room)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Room created successfully!", Toast.LENGTH_SHORT).show();
                loadRooms(currentFilter);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to create room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
