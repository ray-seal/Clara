package com.rayseal.supportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
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

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentContent, commentAuthorName;
        ImageView commentAuthorPicture;

        CommentViewHolder(View itemView) {
            super(itemView);
            commentContent = itemView.findViewById(R.id.commentContent);
            commentAuthorName = itemView.findViewById(R.id.commentAuthorName);
            commentAuthorPicture = itemView.findViewById(R.id.commentAuthorPicture);
        }
    }
}