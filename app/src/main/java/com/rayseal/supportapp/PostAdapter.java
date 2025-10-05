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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

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
                try {
                    if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
                        android.util.Log.d("PostAdapter", "Opening image viewer with URL: " + post.imageUrl);
                        Intent intent = new Intent(context, ImageViewerActivity.class);
                        intent.putExtra("imageUrl", post.imageUrl);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } else {
                        android.util.Log.e("PostAdapter", "Image URL is null or empty");
                        Toast.makeText(context, "Image not available", Toast.LENGTH_SHORT).show();
                    }
                } catch (android.content.ActivityNotFoundException e) {
                    android.util.Log.e("PostAdapter", "ImageViewerActivity not found", e);
                    Toast.makeText(context, "Image viewer not available", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    android.util.Log.e("PostAdapter", "Error opening image viewer", e);
                    Toast.makeText(context, "Error opening image", Toast.LENGTH_SHORT).show();
                }
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
      
      // Report button click listener
      holder.reportButton.setOnClickListener(v -> showReportDialog(context, post));
      
      // Delete button - show for post owner or admins
      setupDeleteButton(holder, post, context, position);
      
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
    
    // Check current reaction status for this specific reaction type
    boolean hasReacted = false;
    if (post.userReactions != null && post.userReactions.containsKey(reactionType) && 
        post.userReactions.get(reactionType) != null) {
        hasReacted = post.userReactions.get(reactionType).contains(currentUserId);
    }
    
    // Check if user has any existing reactions (for single reaction limit)
    String existingReactionType = null;
    if (post.userReactions != null) {
        for (Map.Entry<String, List<String>> entry : post.userReactions.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(currentUserId)) {
                existingReactionType = entry.getKey();
                break;
            }
        }
    }
    
    if (hasReacted) {
        // Remove current reaction
        String reactionPath = "userReactions." + reactionType;
        String countPath = "reactions." + reactionType;
        
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
        // Add new reaction (remove existing one first if it exists)
        if (existingReactionType != null && !existingReactionType.equals(reactionType)) {
            // Remove existing reaction first
            final String finalExistingReactionType = existingReactionType;
            String oldReactionPath = "userReactions." + finalExistingReactionType;
            String oldCountPath = "reactions." + finalExistingReactionType;
            String newReactionPath = "userReactions." + reactionType;
            String newCountPath = "reactions." + reactionType;
            
            // Use batch write to ensure atomicity
            WriteBatch batch = firestore.batch();
            DocumentReference postRef = firestore.collection("posts").document(post.postId);
            
            // Remove old reaction
            batch.update(postRef, oldReactionPath, FieldValue.arrayRemove(currentUserId));
            batch.update(postRef, oldCountPath, FieldValue.increment(-1));
            
            // Add new reaction
            batch.update(postRef, newReactionPath, FieldValue.arrayUnion(currentUserId));
            batch.update(postRef, newCountPath, FieldValue.increment(1));
            
            batch.commit().addOnSuccessListener(aVoid -> {
                // Update local data
                // Remove from old reaction
                if (post.userReactions.get(finalExistingReactionType) != null) {
                    post.userReactions.get(finalExistingReactionType).remove(currentUserId);
                }
                int oldCount = post.reactions.containsKey(finalExistingReactionType) ? post.reactions.get(finalExistingReactionType) : 0;
                post.reactions.put(finalExistingReactionType, Math.max(0, oldCount - 1));
                
                // Add to new reaction
                if (post.userReactions == null) post.userReactions = new HashMap<>();
                if (post.userReactions.get(reactionType) == null) {
                    post.userReactions.put(reactionType, new ArrayList<>());
                }
                post.userReactions.get(reactionType).add(currentUserId);
                
                if (post.reactions == null) post.reactions = new HashMap<>();
                int currentCount = post.reactions.containsKey(reactionType) ? post.reactions.get(reactionType) : 0;
                post.reactions.put(reactionType, currentCount + 1);
                notifyItemChanged(position);
                
                // Create notification for reaction (only if not reacting to own post)
                if (!post.userId.equals(currentUserId)) {
                    createReactionNotification(post, reactionType);
                }
            });
        } else {
            // No existing reaction, just add new one
            String reactionPath = "userReactions." + reactionType;
            String countPath = "reactions." + reactionType;
            
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
                    
                    // Create notification for reaction (only if not reacting to own post)
                    if (!post.userId.equals(currentUserId)) {
                        createReactionNotification(post, reactionType);
                    }
                });
        }
    }
  }
  
  private void showCommentsDialog(Context context, Post post, int position) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null);
    builder.setView(dialogView);
    
    RecyclerView commentsRecyclerView = dialogView.findViewById(R.id.commentsRecyclerView);
    EditText commentEditText = dialogView.findViewById(R.id.commentEditText);
    Button btnAddComment = dialogView.findViewById(R.id.btnAddComment);
    TextView noCommentsText = dialogView.findViewById(R.id.noCommentsText);
    
    List<Comment> comments = new ArrayList<>();
    CommentAdapter commentAdapter = new CommentAdapter(comments, post.postId, context);
    commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    commentsRecyclerView.setAdapter(commentAdapter);
    
    AlertDialog dialog = builder.create();
    
    // Load existing comments
    firestore.collection("comments")
        .whereEqualTo("postId", post.postId)
        .get()
        .addOnSuccessListener(querySnapshot -> {
            android.util.Log.d("PostAdapter", "Found " + querySnapshot.size() + " comments for post " + post.postId);
            comments.clear(); // Clear existing comments first
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                try {
                    Comment comment = doc.toObject(Comment.class);
                    if (comment != null) {
                        comment.commentId = doc.getId();
                        comments.add(comment);
                        android.util.Log.d("PostAdapter", "Added comment: " + comment.content + " by " + comment.authorName);
                    } else {
                        android.util.Log.w("PostAdapter", "Comment object is null for document: " + doc.getId());
                    }
                } catch (Exception e) {
                    android.util.Log.e("PostAdapter", "Error parsing comment document: " + doc.getId(), e);
                }
            }
            
            // Sort comments by timestamp manually
            comments.sort((c1, c2) -> Long.compare(c1.timestamp, c2.timestamp));
            
            commentAdapter.notifyDataSetChanged();
            
            // Show/hide no comments message
            if (comments.isEmpty()) {
                noCommentsText.setVisibility(View.VISIBLE);
                commentsRecyclerView.setVisibility(View.GONE);
            } else {
                noCommentsText.setVisibility(View.GONE);
                commentsRecyclerView.setVisibility(View.VISIBLE);
            }
            
            android.util.Log.d("PostAdapter", "Successfully loaded and displayed " + comments.size() + " comments");
        })
        .addOnFailureListener(e -> {
            android.util.Log.e("PostAdapter", "Error loading comments", e);
            Toast.makeText(context, "Error loading comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                final String authorName;
                final String authorProfilePicture;
                
                if (doc.exists()) {
                    Profile profile = doc.toObject(Profile.class);
                    if (profile != null) {
                        authorName = profile.displayName != null && !profile.displayName.isEmpty() ? 
                            profile.displayName : "Anonymous";
                        authorProfilePicture = profile.profilePictureUrl != null ? profile.profilePictureUrl : "";
                    } else {
                        authorName = "Anonymous";
                        authorProfilePicture = "";
                    }
                } else {
                    authorName = "Anonymous";
                    authorProfilePicture = "";
                }
                
                Comment newComment = new Comment(post.postId, currentUserId, authorName, 
                    authorProfilePicture, commentText, System.currentTimeMillis());
                
                android.util.Log.d("PostAdapter", "Creating comment: " + commentText + " by " + authorName + " for post " + post.postId);
                
                firestore.collection("comments")
                    .add(newComment)
                    .addOnSuccessListener(documentReference -> {
                        newComment.commentId = documentReference.getId();
                        android.util.Log.d("PostAdapter", "Comment saved successfully with ID: " + newComment.commentId);
                        commentAdapter.addComment(newComment);
                        
                        // Show/hide no comments message
                        if (comments.isEmpty()) {
                            noCommentsText.setVisibility(View.VISIBLE);
                            commentsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noCommentsText.setVisibility(View.GONE);
                            commentsRecyclerView.setVisibility(View.VISIBLE);
                        }
                        
                        // Update post comment count
                        firestore.collection("posts").document(post.postId)
                            .update("commentCount", FieldValue.increment(1));
                        
                        post.commentCount++;
                        notifyItemChanged(position);
                        
                        // Create notification for comment (if not commenting on own post)
                        if (!currentUserId.equals(post.userId)) {
                            createCommentNotification(post, authorName);
                        }
                        
                        commentEditText.setText("");
                        commentsRecyclerView.scrollToPosition(comments.size() - 1);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("PostAdapter", "Failed to add comment", e);
                        Toast.makeText(context, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    });
    
    dialog.show();
  }

  private void createReactionNotification(Post post, String reactionType) {
    // Get current user's profile information
    firestore.collection("profiles").document(currentUserId).get()
        .addOnSuccessListener(doc -> {
            String fromUserName = "Someone";
            String fromUserProfilePicture = "";
            
            if (doc.exists()) {
                Profile profile = doc.toObject(Profile.class);
                if (profile != null) {
                    fromUserName = profile.displayName != null && !profile.displayName.isEmpty() ? 
                        profile.displayName : "Someone";
                    fromUserProfilePicture = profile.profilePictureUrl != null ? profile.profilePictureUrl : "";
                }
            }
            
            // Create notification
            Notification notification = Notification.createReactionNotification(
                post.userId, 
                currentUserId, 
                fromUserName, 
                fromUserProfilePicture, 
                post.postId, 
                reactionType
            );
            
            // Save notification to Firestore
            firestore.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    android.util.Log.d("PostAdapter", "Notification created successfully");
                    // Send push notification
                    sendPushNotification(post.userId, notification);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PostAdapter", "Error creating notification", e);
                });
        });
  }

  private void sendPushNotification(String recipientUserId, Notification notification) {
    // Get the recipient's FCM token
    firestore.collection("fcm_tokens").document(recipientUserId).get()
        .addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String fcmToken = doc.getString("token");
                if (fcmToken != null) {
                    // Create a push notification document that can be processed by a cloud function
                    PushNotificationRequest pushRequest = new PushNotificationRequest(
                        fcmToken,
                        notification.title,
                        notification.message,
                        notification.type,
                        notification.relatedPostId,
                        notification.fromUserId
                    );
                    
                    // Store in a collection that triggers cloud function
                    firestore.collection("push_notification_requests")
                        .add(pushRequest)
                        .addOnSuccessListener(pushDoc -> {
                            android.util.Log.d("PostAdapter", "Push notification queued");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("PostAdapter", "Error queuing push notification", e);
                        });
                }
            }
        });
  }

  private void createCommentNotification(Post post, String commenterName) {
    // Create notification for comment
    Notification notification = Notification.createCommentNotification(
        post.userId,
        currentUserId,
        commenterName,
        "", // profile picture will be filled by notification creation
        post.postId,
        "" // commentId - we don't have it here, will be empty
    );
    
    firestore.collection("notifications")
        .add(notification)
        .addOnSuccessListener(documentReference -> {
            android.util.Log.d("PostAdapter", "Comment notification created successfully");
            // Send push notification
            sendPushNotification(post.userId, notification);
        })
        .addOnFailureListener(e -> android.util.Log.e("PostAdapter", "Failed to create comment notification", e));
  }

  // Push notification request model
  public static class PushNotificationRequest {
    public String token;
    public String title;
    public String body;
    public String type;
    public String postId;
    public String userId;
    public long timestamp;
    
    public PushNotificationRequest() {
        // Required for Firestore
    }
    
    public PushNotificationRequest(String token, String title, String body, String type, String postId, String userId) {
        this.token = token;
        this.title = title;
        this.body = body;
        this.type = type;
        this.postId = postId;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
  }

  @Override
  public int getItemCount() {
    return posts.size();
  }

  public static class PostViewHolder extends RecyclerView.ViewHolder {
    TextView postContentText, postCategoriesText, authorNameText, commentCountText;
    TextView reactionYouGotThis, reactionNotAlone, reactionWithYou, reactionStrong, reactionSupport;
    TextView reportButton, deleteButton;
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
      reportButton = itemView.findViewById(R.id.reportButton);
      deleteButton = itemView.findViewById(R.id.deleteButton);
      
      reactionYouGotThis = itemView.findViewById(R.id.reactionYouGotThis);
      reactionNotAlone = itemView.findViewById(R.id.reactionNotAlone);
      reactionWithYou = itemView.findViewById(R.id.reactionWithYou);
      reactionStrong = itemView.findViewById(R.id.reactionStrong);
      reactionSupport = itemView.findViewById(R.id.reactionSupport);
    }
  }
  
  private void showReportDialog(Context context, Post post) {
    String[] reportReasons = {
        "Spam",
        "Harassment or bullying", 
        "Inappropriate content",
        "Hate speech",
        "Misinformation",
        "Other"
    };
    
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Report Post");
    builder.setItems(reportReasons, (dialog, which) -> {
        String selectedReason = reportReasons[which];
        
        if (selectedReason.equals("Other")) {
            showCustomReportDialog(context, post);
        } else {
            submitReport(context, post, selectedReason, "");
        }
    });
    
    builder.setNegativeButton("Cancel", null);
    builder.show();
  }
  
  private void showCustomReportDialog(Context context, Post post) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Report Post - Other");
    
    EditText input = new EditText(context);
    input.setHint("Please describe the issue...");
    input.setMinLines(3);
    builder.setView(input);
    
    builder.setPositiveButton("Submit", (dialog, which) -> {
        String customReason = input.getText().toString().trim();
        if (!customReason.isEmpty()) {
            submitReport(context, post, "Other", customReason);
        } else {
            Toast.makeText(context, "Please provide a description", Toast.LENGTH_SHORT).show();
        }
    });
    
    builder.setNegativeButton("Cancel", null);
    builder.show();
  }
  
  private void submitReport(Context context, Post post, String reason, String description) {
    if (currentUserId == null || currentUserId.isEmpty()) {
        Toast.makeText(context, "Please log in to report", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // Use ModerationUtils.createReport for proper handling of notifications and admin chat
    ModerationUtils.createReport(
        "post", 
        post.postId, 
        post.userId, 
        reason.toLowerCase().replace(" ", "_"), 
        description, 
        context,
        post.content,
        post.authorName
    );
    
    // Increment report count for the reported user
    if (post.userId != null && !post.userId.isEmpty()) {
        firestore.collection("profiles").document(post.userId)
            .update("reportCount", FieldValue.increment(1));
    }
  }
  
  private void setupDeleteButton(PostViewHolder holder, Post post, Context context, int position) {
        // Check if current user can delete this post (owner or admin)
        if (currentUserId != null) {
            firestore.collection("profiles").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = false;
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        isAdmin = profile != null && profile.isAdmin;
                    }
                    
                    // Show delete button if user is owner or admin
                    boolean canDelete = currentUserId.equals(post.userId) || isAdmin;
                    holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
                    
                    if (canDelete) {
                        holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(context, post, position));
                    }
                })
                .addOnFailureListener(e -> {
                    // Only show if user owns the post
                    boolean ownerCanDelete = currentUserId.equals(post.userId);
                    holder.deleteButton.setVisibility(ownerCanDelete ? View.VISIBLE : View.GONE);
                    if (ownerCanDelete) {
                        holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(context, post, position));
                    }
                });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }
    
    private void showDeleteConfirmation(Context context, Post post, int position) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deletePost(context, post, position))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deletePost(Context context, Post post, int position) {
        if (post.postId == null || post.postId.isEmpty()) {
            Toast.makeText(context, "Error: Cannot delete post", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Delete post from Firestore
        firestore.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                // Remove from local list and notify adapter
                if (position >= 0 && position < posts.size()) {
                    posts.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, posts.size());
                }
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Send notification to Admin chat about deletion
                sendDeletionNotificationToAdminChat(post, currentUserId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                android.util.Log.e("PostAdapter", "Failed to delete post", e);
            });
    }
    
    private void sendDeletionNotificationToAdminChat(Post post, String deletedByUserId) {
        // Get the deleter's profile info
        firestore.collection("profiles").document(deletedByUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                final String deleterName;
                final boolean isAdmin;
                
                if (documentSnapshot.exists()) {
                    Profile profile = documentSnapshot.toObject(Profile.class);
                    if (profile != null) {
                        deleterName = profile.displayName != null ? profile.displayName : "Unknown";
                        isAdmin = profile.isAdmin;
                    } else {
                        deleterName = "Unknown";
                        isAdmin = false;
                    }
                } else {
                    deleterName = "Unknown";
                    isAdmin = false;
                }
                
                // Find the Admin chat room and send notification
                FirebaseDatabase.getInstance().getReference("chatRooms")
                    .orderByChild("name")
                    .equalTo("Admin")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            final String adminRoomId;
                            if (dataSnapshot.hasChildren()) {
                                adminRoomId = dataSnapshot.getChildren().iterator().next().getKey();
                            } else {
                                adminRoomId = null;
                            }
                            
                            if (adminRoomId != null) {
                                String deletionMessage = String.format(
                                    "🗑️ POST DELETED 🗑️\n" +
                                    "Deleted by: %s %s\n" +
                                    "Original Author: %s\n" +
                                    "Content: \"%.100s%s\"\n" +
                                    "Time: %s",
                                    deleterName,
                                    isAdmin ? "(Admin)" : "(Owner)",
                                    post.authorName != null ? post.authorName : "Unknown",
                                    post.content != null ? post.content : "",
                                    post.content != null && post.content.length() > 100 ? "..." : "",
                                    new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                        .format(new java.util.Date())
                                );
                                
                                ChatMessage adminMessage = new ChatMessage(
                                    "system",
                                    "Moderation System",
                                    deletionMessage,
                                    adminRoomId
                                );
                                
                                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                                    .getReference("messages").child(adminRoomId);
                                String messageId = messagesRef.push().getKey();
                                
                                if (messageId != null) {
                                    adminMessage.messageId = messageId;
                                    messagesRef.child(messageId).setValue(adminMessage);
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            android.util.Log.e("PostAdapter", "Failed to find admin chat room for deletion notification", databaseError.toException());
                        }
                    });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("PostAdapter", "Failed to get deleter profile info", e);
            });
    }
}
