package com.rayseal.supportapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {
    private List<Profile> userList;
    private Context context;
    private OnUserActionListener listener;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    public interface OnUserActionListener {
        void onBanUser(Profile user);
        void onUnbanUser(Profile user);
        void onWarnUser(Profile user);
        void onPromoteToAdmin(Profile user);
        void onDemoteFromAdmin(Profile user);
        void onViewProfile(Profile user);
    }

    public UserManagementAdapter(Context context, List<Profile> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Profile user = userList.get(position);
        
        // Set user profile image
        if (user.profilePictureUrl != null && !user.profilePictureUrl.isEmpty()) {
            Glide.with(context)
                    .load(user.profilePictureUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile_pic)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.default_profile_pic);
        }
        
        // Set user info
        holder.userName.setText(user.displayName);
        holder.userEmail.setText(user.actualName); // Using actualName as email equivalent
        
        // Set user stats
        holder.reportCount.setText(String.format(Locale.getDefault(), "Reports: %d", user.reportCount));
        holder.warningCount.setText(String.format(Locale.getDefault(), "Warnings: %d", user.warningCount));
        
        // Set admin status
        if (user.isAdmin) {
            holder.adminStatus.setText("ADMIN");
            holder.adminStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.adminStatus.setBackgroundResource(R.drawable.bg_admin_badge);
            holder.adminStatus.setVisibility(View.VISIBLE);
        } else {
            holder.adminStatus.setVisibility(View.GONE);
        }
        
        // Set ban status
        if (user.isBanned) {
            holder.banStatus.setText("BANNED");
            holder.banStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.banStatus.setBackgroundResource(R.drawable.bg_banned_badge);
            holder.banStatus.setVisibility(View.VISIBLE);
            
            if (user.bannedAt > 0) {
                holder.banInfo.setText(String.format("Banned on: %s", dateFormat.format(new java.util.Date(user.bannedAt))));
                holder.banInfo.setVisibility(View.VISIBLE);
            }
            
            if (user.banReason != null && !user.banReason.isEmpty()) {
                holder.banReason.setText(String.format("Reason: %s", user.banReason));
                holder.banReason.setVisibility(View.VISIBLE);
            }
        } else {
            holder.banStatus.setVisibility(View.GONE);
            holder.banInfo.setVisibility(View.GONE);
            holder.banReason.setVisibility(View.GONE);
        }
        
        // Set up button actions
        holder.btnViewProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(user);
            }
        });
        
        holder.btnWarn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWarnUser(user);
            }
        });
        
        // Configure ban/unban button
        if (user.isBanned) {
            holder.btnBan.setText("Unban");
            holder.btnBan.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.holo_green_dark));
            holder.btnBan.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnbanUser(user);
                }
            });
        } else {
            holder.btnBan.setText("Ban");
            holder.btnBan.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.holo_red_dark));
            holder.btnBan.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBanUser(user);
                }
            });
        }
        
        // Configure admin promotion/demotion button
        if (user.isAdmin) {
            holder.btnAdmin.setText("Remove Admin");
            holder.btnAdmin.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.holo_orange_dark));
            holder.btnAdmin.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDemoteFromAdmin(user);
                }
            });
        } else {
            holder.btnAdmin.setText("Make Admin");
            holder.btnAdmin.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.holo_blue_dark));
            holder.btnAdmin.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromoteToAdmin(user);
                }
            });
        }
        
        // Hide admin button for banned users (can't promote banned users)
        if (user.isBanned) {
            holder.btnAdmin.setVisibility(View.GONE);
        } else {
            holder.btnAdmin.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<Profile> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < userList.size()) {
            userList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView userName, userEmail, reportCount, warningCount, adminStatus, banStatus, banInfo, banReason;
        Button btnViewProfile, btnWarn, btnBan, btnAdmin;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            reportCount = itemView.findViewById(R.id.reportCount);
            warningCount = itemView.findViewById(R.id.warningCount);
            adminStatus = itemView.findViewById(R.id.adminStatus);
            banStatus = itemView.findViewById(R.id.banStatus);
            banInfo = itemView.findViewById(R.id.banInfo);
            banReason = itemView.findViewById(R.id.banReason);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnWarn = itemView.findViewById(R.id.btnWarn);
            btnBan = itemView.findViewById(R.id.btnBan);
            btnAdmin = itemView.findViewById(R.id.btnAdmin);
        }
    }
}