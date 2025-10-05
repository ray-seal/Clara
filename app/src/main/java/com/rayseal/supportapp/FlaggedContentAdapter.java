package com.rayseal.supportapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FlaggedContentAdapter extends RecyclerView.Adapter<FlaggedContentAdapter.FlaggedContentViewHolder> {
    private List<FlaggedContent> flaggedContentList;
    private Context context;
    private OnFlaggedContentActionListener listener;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    public interface OnFlaggedContentActionListener {
        void onApproveContent(FlaggedContent content);
        void onRejectContent(FlaggedContent content);
        void onDeleteContent(FlaggedContent content);
        void onBanUser(FlaggedContent content);
    }

    public FlaggedContentAdapter(Context context, List<FlaggedContent> flaggedContentList, OnFlaggedContentActionListener listener) {
        this.context = context;
        this.flaggedContentList = flaggedContentList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public FlaggedContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flagged_content, parent, false);
        return new FlaggedContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlaggedContentViewHolder holder, int position) {
        FlaggedContent content = flaggedContentList.get(position);
        
        // Set content type
        holder.contentType.setText(content.contentType.toUpperCase());
        holder.contentType.setBackgroundResource(getContentTypeBackground(content.contentType));
        
        // Set flagged keywords
        StringBuilder keywords = new StringBuilder();
        for (String keyword : content.flaggedWords) {
            if (keywords.length() > 0) keywords.append(", ");
            keywords.append(keyword.toUpperCase());
        }
        holder.flaggedKeywords.setText(keywords.toString());
        
        // Set date
        holder.flaggedDate.setText(dateFormat.format(content.flaggedAt.toDate()));
        
        // Set content text (truncate if too long)
        String contentText = content.content;
        if (contentText.length() > 200) {
            contentText = contentText.substring(0, 200) + "...";
        }
        holder.contentText.setText(contentText);
        
        // Set author
        holder.contentAuthor.setText("Author: " + content.authorName);
        
        // Set confidence score (calculate from flag reason)
        double confidence = content.flagReason.equals("profanity") ? 0.9 : 
                           content.flagReason.equals("hate_speech") ? 0.8 : 
                           content.flagReason.equals("harassment") ? 0.7 : 0.6;
        holder.confidenceScore.setText(String.format(Locale.getDefault(), "Confidence: %.0f%%", confidence * 100));
        
        // Set up button actions
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveContent(content);
            }
        });
        
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectContent(content);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteContent(content);
            }
        });
        
        holder.btnBanUser.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBanUser(content);
            }
        });
        
        // Show/hide buttons based on content status
        if ("approved".equals(content.status) || "rejected".equals(content.status)) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        } else {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return flaggedContentList.size();
    }

    private int getContentTypeBackground(String contentType) {
        switch (contentType.toLowerCase()) {
            case "post":
                return R.drawable.bg_content_type_post; // Orange background
            case "comment":
                return R.drawable.bg_content_type_comment; // Blue background
            case "chat":
                return R.drawable.bg_content_type_chat; // Green background
            default:
                return R.drawable.bg_content_type_default; // Gray background
        }
    }

    public void updateData(List<FlaggedContent> newFlaggedContentList) {
        this.flaggedContentList = newFlaggedContentList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < flaggedContentList.size()) {
            flaggedContentList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class FlaggedContentViewHolder extends RecyclerView.ViewHolder {
        TextView contentType, flaggedKeywords, flaggedDate, contentText, contentAuthor, confidenceScore;
        Button btnApprove, btnReject, btnDelete, btnBanUser;

        public FlaggedContentViewHolder(@NonNull View itemView) {
            super(itemView);
            contentType = itemView.findViewById(R.id.contentType);
            flaggedKeywords = itemView.findViewById(R.id.flaggedKeywords);
            flaggedDate = itemView.findViewById(R.id.flaggedDate);
            contentText = itemView.findViewById(R.id.contentText);
            contentAuthor = itemView.findViewById(R.id.contentAuthor);
            confidenceScore = itemView.findViewById(R.id.confidenceScore);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBanUser = itemView.findViewById(R.id.btnBanUser);
        }
    }
}