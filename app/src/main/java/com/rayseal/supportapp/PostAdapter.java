package com.rayseal.supportapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
  private List<Post> posts;
  private FirebaseFirestore firestore;
  private String currentUserId;

  public PostAdapter(List<Post> posts) {
    this.posts = posts;
    this.firestore = FirebaseFirestore.getInstance();
    this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
        FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
  }

  @NonNull
  @Override
  public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_post, parent, false);
    return new PostViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
    try {
      Post post = posts.get(position);
      Context context = holder.itemView.getContext();
      
      // Safe string operations with null checks
      holder.postContentText.setText(post.content != null ? post.content : "");
      
      if (post.categories != null && !post.categories.isEmpty()) {
          holder.postCategoriesText.setText("Categories: " + String.join(", ", post.categories));
      } else {
          holder.postCategoriesText.setText("Categories: General");
      }
      
      // Set author info with null checks
      String displayName = (post.authorName != null && !post.authorName.isEmpty()) 
          ? post.authorName : "Anonymous";
      holder.authorNameText.setText(displayName);
      
      // Load author profile picture safely
      if (post.authorProfilePicture != null && !post.authorProfilePicture.isEmpty()) {
          try {
              Glide.with(context)
                      .load(post.authorProfilePicture)
                      .placeholder(R.drawable.ic_person)
                      .error(R.drawable.ic_person)
                      .into(holder.authorProfilePicture);
          } catch (Exception e) {
              holder.authorProfilePicture.setImageResource(R.drawable.ic_person);
          }
      } else {
          holder.authorProfilePicture.setImageResource(R.drawable.ic_person);
      }
      
      // Author section click listener to view profile - only if not anonymous
      holder.authorSection.setOnClickListener(v -> {
          if (!post.isAnonymous && post.userId != null && !post.userId.isEmpty()) {
              Intent intent = new Intent(context, ProfileActivity.class);
              intent.putExtra("userId", post.userId);
              context.startActivity(intent);
          } else if (post.isAnonymous) {
              Toast.makeText(context, "This is an anonymous post", Toast.LENGTH_SHORT).show();
          }
      });
      
      // Handle post image safely
      if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
        holder.postImageView.setVisibility(View.VISIBLE);
        try {
            Glide.with(context)
                 .load(post.imageUrl)
                 .placeholder(android.R.drawable.ic_menu_gallery)
                 .error(android.R.drawable.ic_delete)
                 .into(holder.postImageView);
                 
            // Click to enlarge image
            holder.postImageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ImageViewerActivity.class);
                intent.putExtra("imageUrl", post.imageUrl);
                context.startActivity(intent);
            });
        } catch (Exception e) {
            holder.postImageView.setVisibility(View.GONE);
        }
      } else {
        holder.postImageView.setVisibility(View.GONE);
      }
      
      // Set up reactions safely
      setupReactions(holder, post, position);
      
      // Comments section safely
      int commentCount = post.commentCount;
      holder.commentCountText.setText(commentCount == 1 ? "1 comment" : commentCount + " comments");
      holder.commentsSection.setOnClickListener(v -> showCommentsDialog(context, post, position));
      
    } catch (Exception e) {
        android.util.Log.e("PostAdapter", "Error binding post at position " + position, e);
        // Set default values to prevent crash
        holder.postContentText.setText("Error loading post");
        holder.authorNameText.setText("Anonymous");
        holder.authorProfilePicture.setImageResource(R.drawable.ic_person);
        holder.postImageView.setVisibility(View.GONE);
        holder.commentCountText.setText("0 comments");
    }
  }

  private void setupReactions(PostViewHolder holder, Post post, int position) {
    try {
        // Reaction types and their messages
        Map<String, String> reactionTypes = new HashMap<>();
        reactionTypes.put("youGotThis", "You got this");
        reactionTypes.put("notAlone", "You're not alone");
        reactionTypes.put("withYou", "Right here with you");
        reactionTypes.put("strong", "You are strong");
        reactionTypes.put("support", "Sending support");
        
        // Set reaction counts and click listeners safely
        setupReactionButton(holder.reactionYouGotThis, post, "youGotThis", "You got this", position);
        setupReactionButton(holder.reactionNotAlone, post, "notAlone", "You're not alone", position);
        setupReactionButton(holder.reactionWithYou, post, "withYou", "Right here with you", position);
        setupReactionButton(holder.reactionStrong, post, "strong", "You are strong", position);
        setupReactionButton(holder.reactionSupport, post, "support", "Sending support", position);
    } catch (Exception e) {
        android.util.Log.e("PostAdapter", "Error setting up reactions", e);
        // Set default text for all reaction buttons
        try {
            holder.reactionYouGotThis.setText("❤️ 0");
            holder.reactionNotAlone.setText("🧡 0");
            holder.reactionWithYou.setText("💛 0");
            holder.reactionStrong.setText("💚 0");
            holder.reactionSupport.setText("💙 0");
        } catch (Exception e2) {
            // If even this fails, just continue
        }
    }
  }
  
  private void setupReactionButton(TextView reactionView, Post post, String reactionType, String message, int position) {
    try {
        if (reactionView == null || post == null) return;
        
        int count = 0;
        if (post.reactions != null && post.reactions.containsKey(reactionType)) {
            Integer countValue = post.reactions.get(reactionType);
            count = countValue != null ? countValue : 0;
        }
        
        String emoji = getReactionEmoji(reactionType);
        reactionView.setText(emoji + " " + count);
        
        // Check if current user has reacted
        boolean hasReacted = false;
        if (currentUserId != null && !currentUserId.isEmpty() && 
            post.userReactions != null && post.userReactions.containsKey(reactionType) && 
            post.userReactions.get(reactionType) != null) {
            hasReacted = post.userReactions.get(reactionType).contains(currentUserId);
        }
        
        // Style based on user reaction
        if (hasReacted) {
            reactionView.setAlpha(1.0f);
            reactionView.setTextColor(0xFF4CAF50); // Green for reacted
        } else {
            reactionView.setAlpha(0.6f);
            reactionView.setTextColor(0xFF666666); // Gray for not reacted
        }
        
        reactionView.setOnClickListener(v -> {
            if (post.postId != null) {
                toggleReaction(post, reactionType, message, position);
            }
        });
    } catch (Exception e) {
        android.util.Log.e("PostAdapter", "Error setting up reaction button: " + reactionType, e);
        if (reactionView != null) {
            String emoji = getReactionEmoji(reactionType);
            reactionView.setText(emoji + " 0");
        }
    }
  }
  
  private String getReactionEmoji(String reactionType) {
    switch (reactionType) {
        case "youGotThis": return "❤️";
        case "notAlone": return "🧡";
        case "withYou": return "💛";
        case "strong": return "💚";
        case "support": return "💙";
        default: return "❤️";
    }
  }
  
  private void toggleReaction(Post post, String reactionType, String message, int position) {
    if (post.postId == null || currentUserId.isEmpty()) return;
    
    // Check current reaction status
    boolean hasReacted = false;
    if (post.userReactions != null && post.userReactions.containsKey(reactionType) && 
        post.userReactions.get(reactionType) != null) {
        hasReacted = post.userReactions.get(reactionType).contains(currentUserId);
    }
    
    String reactionPath = "userReactions." + reactionType;
    String countPath = "reactions." + reactionType;
    
    if (hasReacted) {
        // Remove reaction
        firestore.collection("posts").document(post.postId)
            .update(reactionPath, FieldValue.arrayRemove(currentUserId),
                   countPath, FieldValue.increment(-1))
            .addOnSuccessListener(aVoid -> {
                // Update local data
                if (post.userReactions.get(reactionType) != null) {
                    post.userReactions.get(reactionType).remove(currentUserId);
                }
                int currentCount = post.reactions.get(reactionType);
                post.reactions.put(reactionType, Math.max(0, currentCount - 1));
                notifyItemChanged(position);
            });
    } else {
        // Add reaction
        firestore.collection("posts").document(post.postId)
            .update(reactionPath, FieldValue.arrayUnion(currentUserId),
                   countPath, FieldValue.increment(1))
            .addOnSuccessListener(aVoid -> {
                // Update local data
                if (post.userReactions == null) post.userReactions = new HashMap<>();
                if (post.userReactions.get(reactionType) == null) {
                    post.userReactions.put(reactionType, new ArrayList<>());
                }
                post.userReactions.get(reactionType).add(currentUserId);
                
                if (post.reactions == null) post.reactions = new HashMap<>();
                int currentCount = post.reactions.containsKey(reactionType) ? post.reactions.get(reactionType) : 0;
                post.reactions.put(reactionType, currentCount + 1);
                notifyItemChanged(position);
            });
    }
  }
  
  private void showCommentsDialog(Context context, Post post, int position) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null);
    builder.setView(dialogView);
    
    RecyclerView commentsRecyclerView = dialogView.findViewById(R.id.commentsRecyclerView);
    EditText commentEditText = dialogView.findViewById(R.id.commentEditText);
    Button btnAddComment = dialogView.findViewById(R.id.btnAddComment);
    
    List<Comment> comments = new ArrayList<>();
    CommentAdapter commentAdapter = new CommentAdapter(comments);
    commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    commentsRecyclerView.setAdapter(commentAdapter);
    
    AlertDialog dialog = builder.create();
    
    // Load existing comments
    firestore.collection("comments")
        .whereEqualTo("postId", post.postId)
        .orderBy("timestamp")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Comment comment = doc.toObject(Comment.class);
                if (comment != null) {
                    comment.commentId = doc.getId();
                    comments.add(comment);
                }
            }
            commentAdapter.notifyDataSetChanged();
        });
    
    // Add comment listener
    btnAddComment.setOnClickListener(v -> {
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user profile info
        firestore.collection("profiles").document(currentUserId)
            .get()
            .addOnSuccessListener(doc -> {
                String authorName = "Anonymous";
                String authorProfilePicture = "";
                
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    if (profile != null) {
                        authorName = profile.displayName != null && !profile.displayName.isEmpty() ? 
                            profile.displayName : "Anonymous";
                        authorProfilePicture = profile.profilePictureUrl != null ? profile.profilePictureUrl : "";
                    }
                }
                
                Comment newComment = new Comment(post.postId, currentUserId, authorName, 
                    authorProfilePicture, commentText, System.currentTimeMillis());
                
                firestore.collection("comments")
                    .add(newComment)
                    .addOnSuccessListener(documentReference -> {
                        newComment.commentId = documentReference.getId();
                        commentAdapter.addComment(newComment);
                        
                        // Update post comment count
                        firestore.collection("posts").document(post.postId)
                            .update("commentCount", FieldValue.increment(1));
                        
                        post.commentCount++;
                        notifyItemChanged(position);
                        
                        commentEditText.setText("");
                        commentsRecyclerView.scrollToPosition(comments.size() - 1);
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show());
            });
    });
    
    dialog.show();
  }

  @Override
  public int getItemCount() {
    return posts.size();
  }

  public static class PostViewHolder extends RecyclerView.ViewHolder {
    TextView postContentText, postCategoriesText, authorNameText, commentCountText;
    TextView reactionYouGotThis, reactionNotAlone, reactionWithYou, reactionStrong, reactionSupport;
    ImageView postImageView, authorProfilePicture;
    LinearLayout authorSection, commentsSection;
    
    PostViewHolder(View itemView) {
      super(itemView);
      postContentText = itemView.findViewById(R.id.postContentText);
      postCategoriesText = itemView.findViewById(R.id.postCategoriesText);
      postImageView = itemView.findViewById(R.id.postImageView);
      authorNameText = itemView.findViewById(R.id.authorNameText);
      authorProfilePicture = itemView.findViewById(R.id.authorProfilePicture);
      authorSection = itemView.findViewById(R.id.authorSection);
      commentCountText = itemView.findViewById(R.id.commentCountText);
      commentsSection = itemView.findViewById(R.id.commentsSection);
      
      reactionYouGotThis = itemView.findViewById(R.id.reactionYouGotThis);
      reactionNotAlone = itemView.findViewById(R.id.reactionNotAlone);
      reactionWithYou = itemView.findViewById(R.id.reactionWithYou);
      reactionStrong = itemView.findViewById(R.id.reactionStrong);
      reactionSupport = itemView.findViewById(R.id.reactionSupport);
    }
  }
}
