package com.rayseal.supportapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> comments;
    private String postId;
    private Context context;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public CommentAdapter(List<Comment> comments, String postId, Context context) {
        this.comments = comments;
        this.postId = postId;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        android.util.Log.d("CommentAdapter", "Binding comment at position " + position + ": " + comment.content + " by " + comment.authorName);
        
        holder.commentContent.setText(comment.content);
        holder.commentAuthorName.setText(comment.authorName != null && !comment.authorName.isEmpty() 
                ? comment.authorName : "Anonymous");

        // Load author profile picture
        if (comment.authorProfilePicture != null && !comment.authorProfilePicture.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.authorProfilePicture)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.commentAuthorPicture);
        } else {
            holder.commentAuthorPicture.setImageResource(R.drawable.ic_person);
        }

        // Setup delete button
        setupDeleteButton(holder, comment, position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void addComment(Comment comment) {
        android.util.Log.d("CommentAdapter", "Adding new comment: " + comment.content + " by " + comment.authorName);
        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
        android.util.Log.d("CommentAdapter", "Total comments now: " + comments.size());
    }

    private void setupDeleteButton(CommentViewHolder holder, Comment comment, int position) {
        // Check if user can delete this comment (owner or admin)
        ModerationUtils.checkAdminStatus(isAdmin -> {
            boolean canDelete = comment.userId.equals(currentUserId) || isAdmin;
            
            if (canDelete) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(comment, position));
            } else {
                holder.deleteButton.setVisibility(View.GONE);
            }
        });
    }

    private void showDeleteConfirmation(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(Comment comment, int position) {
        firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .document(comment.commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list
                    comments.remove(position);
                    notifyItemRemoved(position);
                    
                    // Send deletion notification to Admin chat
                    sendDeletionNotificationToAdminChat(comment);
                    
                    Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete comment: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendDeletionNotificationToAdminChat(Comment comment) {
        // Get the current user's name for the notification
        ModerationUtils.getCurrentUserName(userName -> {
            String notificationContent = "🗑️ Comment deleted by " + userName + "\n" +
                    "Original author: " + comment.authorName + "\n" +
                    "Content: " + comment.content + "\n" +
                    "Time: " + new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(new Date(comment.timestamp));
            
            ModerationUtils.sendNotificationToAdminChat(notificationContent);
        });
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentContent, commentAuthorName, deleteButton;
        ImageView commentAuthorPicture;

        CommentViewHolder(View itemView) {
            super(itemView);
            commentContent = itemView.findViewById(R.id.commentContent);
            commentAuthorName = itemView.findViewById(R.id.commentAuthorName);
            commentAuthorPicture = itemView.findViewById(R.id.commentAuthorPicture);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}