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
 * Adapter for displaying chat rooms in a RecyclerView.
 */
public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.RoomViewHolder> {
    private List<ChatRoom> rooms = new ArrayList<>();
    private OnRoomClickListener listener;

    public interface OnRoomClickListener {
        void onRoomClick(ChatRoom room);
    }

    public ChatRoomAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);
        holder.bind(room, listener);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void setRooms(List<ChatRoom> rooms) {
        this.rooms = rooms;
        notifyDataSetChanged();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomNameText;
        TextView roomTopicText;
        TextView lastMessageText;
        TextView roomTypeText;
        TextView memberCountText;

        RoomViewHolder(View itemView) {
            super(itemView);
            roomNameText = itemView.findViewById(R.id.roomNameText);
            roomTopicText = itemView.findViewById(R.id.roomTopicText);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            roomTypeText = itemView.findViewById(R.id.roomTypeText);
            memberCountText = itemView.findViewById(R.id.memberCountText);
        }

        void bind(ChatRoom room, OnRoomClickListener listener) {
            roomNameText.setText(room.roomName);
            roomTopicText.setText(room.topic);
            
            if (room.lastMessage != null && !room.lastMessage.isEmpty()) {
                lastMessageText.setText(room.lastMessage);
            } else {
                lastMessageText.setText("No messages yet");
            }
            
            roomTypeText.setText(room.isPrivate ? "Private" : "Public");
            
            int memberCount = room.members != null ? room.members.size() : 0;
            memberCountText.setText(memberCount + (memberCount == 1 ? " member" : " members"));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRoomClick(room);
                }
            });
        }
    }
}
