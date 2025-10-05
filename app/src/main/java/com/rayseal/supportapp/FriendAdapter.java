package com.rayseal.supportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying friends, friend requests, and search results.
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    private List<Object> items = new ArrayList<>(); // Can contain Friend objects or Profile objects
    private String currentUserId;
    private OnFriendActionListener listener;
    private FirebaseFirestore firestore;

    public interface OnFriendActionListener {
        void onAcceptRequest(Friend friend);
        void onRejectRequest(Friend friend);
        void onRemoveFriend(Friend friend);
        void onSendRequest(String userId);
        void onViewProfile(String userId);
    }

    public FriendAdapter(String currentUserId, OnFriendActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        try {
            if (position < 0 || position >= items.size()) {
                android.util.Log.w("FriendAdapter", "Invalid position: " + position);
                return;
            }
            
            Object item = items.get(position);
            if (item == null) {
                android.util.Log.w("FriendAdapter", "Null item at position: " + position);
                return;
            }

            if (item instanceof Friend) {
                bindFriend(holder, (Friend) item);
            } else if (item instanceof Profile) {
                bindProfile(holder, (Profile) item);
            }
        } catch (Exception e) {
            android.util.Log.e("FriendAdapter", "Error binding view holder at position " + position, e);
        }
    }

    private void bindFriend(FriendViewHolder holder, Friend friend) {
        String otherUserId = friend.getOtherUserId(currentUserId);
        
        // Load profile for the other user
        firestore.collection("profiles").document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Profile profile = doc.toObject(Profile.class);
                        if (profile != null) {
                            String displayName = profile.displayName;
                            holder.nameText.setText((displayName == null || displayName.isEmpty()) ? "Anonymous" : displayName);
                            
                            if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(profile.profilePictureUrl)
                                        .placeholder(R.drawable.ic_person)
                                        .into(holder.profileImage);
                            } else {
                                holder.profileImage.setImageResource(R.drawable.ic_person);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FriendAdapter", "Failed to load profile for friend", e);
                    holder.nameText.setText("Anonymous");
                    holder.profileImage.setImageResource(R.drawable.ic_person);
                });

        if (friend.status.equals("pending") && !friend.requesterId.equals(currentUserId)) {
            // Incoming friend request
            holder.actionButton1.setText("Accept");
            holder.actionButton2.setText("Reject");
            holder.actionButton1.setVisibility(View.VISIBLE);
            holder.actionButton2.setVisibility(View.VISIBLE);
            holder.statusText.setText("Friend request");

            holder.actionButton1.setOnClickListener(v -> listener.onAcceptRequest(friend));
            holder.actionButton2.setOnClickListener(v -> listener.onRejectRequest(friend));
        } else if (friend.status.equals("accepted")) {
            // Existing friend
            holder.actionButton1.setText("Remove");
            holder.actionButton1.setVisibility(View.VISIBLE);
            holder.actionButton2.setVisibility(View.GONE);
            holder.statusText.setText("Friend");

            holder.actionButton1.setOnClickListener(v -> listener.onRemoveFriend(friend));
        } else {
            // Outgoing pending request
            holder.actionButton1.setVisibility(View.GONE);
            holder.actionButton2.setVisibility(View.GONE);
            holder.statusText.setText("Request sent");
        }

        holder.itemView.setOnClickListener(v -> listener.onViewProfile(otherUserId));
    }

    private void bindProfile(FriendViewHolder holder, Profile profile) {
        String displayName = profile.displayName;
        holder.nameText.setText((displayName == null || displayName.isEmpty()) ? "Anonymous" : displayName);
        holder.statusText.setText(""); // Clear status for search results

        if (profile.profilePictureUrl != null && !profile.profilePictureUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profile.profilePictureUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_person);
        }

        holder.actionButton1.setText("Add Friend");
        holder.actionButton1.setVisibility(View.VISIBLE);
        holder.actionButton2.setVisibility(View.GONE);

        holder.actionButton1.setOnClickListener(v -> listener.onSendRequest(profile.uid));
        holder.itemView.setOnClickListener(v -> listener.onViewProfile(profile.uid));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setFriends(List<Friend> friends) {
        this.items.clear();
        this.items.addAll(friends);
        notifyDataSetChanged();
    }

    public void setFriendRequests(List<Friend> requests) {
        this.items.clear();
        this.items.addAll(requests);
        notifyDataSetChanged();
    }

    public void setSearchResults(List<Profile> profiles) {
        try {
            this.items.clear();
            if (profiles != null) {
                this.items.addAll(profiles);
            }
            notifyDataSetChanged();
        } catch (Exception e) {
            android.util.Log.e("FriendAdapter", "Error setting search results", e);
        }
    }

    public void clearItems() {
        this.items.clear();
        notifyDataSetChanged();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, statusText;
        Button actionButton1, actionButton2;

        FriendViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            statusText = itemView.findViewById(R.id.statusText);
            actionButton1 = itemView.findViewById(R.id.actionButton1);
            actionButton2 = itemView.findViewById(R.id.actionButton2);
        }
    }
}