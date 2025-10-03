package com.rayseal.supportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
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

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameText;
        TextView messageContentText;
        TextView timestampText;
        View messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            messageContentText = itemView.findViewById(R.id.messageContentText);
            timestampText = itemView.findViewById(R.id.timestampText);
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
