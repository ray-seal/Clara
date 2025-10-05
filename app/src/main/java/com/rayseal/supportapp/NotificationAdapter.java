package com.rayseal.supportapp;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private Context context;
    private FirebaseFirestore firestore;
    private OnNotificationClickListener clickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, Context context) {
        this.notifications = notifications;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        // Set notification content
        holder.title.setText(notification.title);
        holder.message.setText(notification.message);
        
        // Set timestamp
        if (notification.timestamp != null) {
            long timeMillis = notification.timestamp.toDate().getTime();
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                timeMillis, 
                System.currentTimeMillis(), 
                DateUtils.MINUTE_IN_MILLIS
            ).toString();
            holder.time.setText(timeAgo);
        }
        
        // Set profile picture
        if (notification.fromUserProfilePicture != null && !notification.fromUserProfilePicture.isEmpty()) {
            Glide.with(context)
                .load(notification.fromUserProfilePicture)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.profilePicture);
        } else {
            holder.profilePicture.setImageResource(R.drawable.ic_person);
        }
        
        // Set read status
        if (notification.isRead) {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(notification);
            }
            
            // Mark as read if not already read
            if (!notification.isRead) {
                markAsRead(notification, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private void markAsRead(Notification notification, int position) {
        if (notification.notificationId != null) {
            firestore.collection("notifications").document(notification.notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    notification.isRead = true;
                    notifyItemChanged(position);
                });
        }
    }

    public void markAllAsRead() {
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            if (!notification.isRead && notification.notificationId != null) {
                firestore.collection("notifications").document(notification.notificationId)
                    .update("isRead", true);
                notification.isRead = true;
            }
        }
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        ImageView profilePicture;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            time = itemView.findViewById(R.id.notificationTime);
            profilePicture = itemView.findViewById(R.id.profilePicture);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}