package com.rayseal.supportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
  private List<Post> posts;

public PostAdapter(List<Post> posts) {
  this.posts = posts;
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
    Post post = posts.get(position);
    holder.postContentText.setText(post.content);
    holder.postCategoriesText.setText("Categories: " + String.join(", ", post.categories));
  }

@Override
  public int getItemCount() {
    return posts.size();
  }

public static class PostViewHolder extends RecyclerView.ViewHolder {
  TextView postContentText, postCategoriesText;
  PostViewHolder(View itemView) {
    super(itemView);
    postContentText = itemView.findViewById(R.id.postContentText);
    postCategoriesText = itemView.findViewById(R.id.postCategoriesText);
  }
}
}
