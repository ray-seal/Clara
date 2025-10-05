package com.rayseal.supportapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying chat messages in a RecyclerView.
 * Handles both sent and received messages with different styling.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    private List<ChatMessage> messages = new ArrayList<>();
    private String currentUserId;
    private String roomId;
    private Context context;

    public ChatMessageAdapter(String currentUserId, String roomId, Context context) {
        this.currentUserId = currentUserId;
        this.roomId = roomId;
        this.context = context;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message, currentUserId);
        setupDeleteButton(holder, message, position);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private void setupDeleteButton(MessageViewHolder holder, ChatMessage message, int position) {
        // Check if user can delete this message (owner or admin)
        ModerationUtils.checkAdminStatus(isAdmin -> {
            boolean canDelete = message.senderId.equals(currentUserId) || isAdmin;
            
            if (canDelete) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(message, position));
            } else {
                holder.deleteButton.setVisibility(View.GONE);
            }
        });
    }

    private void showDeleteConfirmation(ChatMessage message, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(ChatMessage message, int position) {
        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(roomId)
                .child("messages")
                .child(message.messageId);

        messageRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list
                    messages.remove(position);
                    notifyItemRemoved(position);
                    
                    // Send deletion notification to Admin chat
                    sendDeletionNotificationToAdminChat(message);
                    
                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete message: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendDeletionNotificationToAdminChat(ChatMessage message) {
        // Get the current user's name for the notification
        ModerationUtils.getCurrentUserName(userName -> {
            String notificationContent = "🗑️ Message deleted by " + userName + "\n" +
                    "Original sender: " + message.senderName + "\n" +
                    "Content: " + message.content + "\n" +
                    "Time: " + new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(new Date(message.timestamp));
            
            ModerationUtils.sendNotificationToAdminChat(notificationContent);
        });
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameText;
        TextView messageContentText;
        TextView timestampText;
        TextView deleteButton;
        View messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            messageContentText = itemView.findViewById(R.id.messageContentText);
            timestampText = itemView.findViewById(R.id.timestampText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(ChatMessage message, String currentUserId) {
            senderNameText.setText(message.senderName);
            messageContentText.setText(message.content);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            timestampText.setText(timeFormat.format(new Date(message.timestamp)));

            // Align message based on sender
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams();
            if (message.senderId.equals(currentUserId)) {
                // Own messages align to right
                params.leftMargin = 80;
                params.rightMargin = 8;
                messageContainer.setBackgroundResource(R.drawable.circle_sky_blue);
            } else {
                // Others' messages align to left
                params.leftMargin = 8;
                params.rightMargin = 80;
                messageContainer.setBackgroundResource(R.drawable.card_white);
            }
            messageContainer.setLayoutParams(params);
        }
    }
}
