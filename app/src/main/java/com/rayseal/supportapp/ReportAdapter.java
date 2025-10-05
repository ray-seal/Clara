package com.rayseal.supportapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying reports in admin panel
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    
    private List<Report> reports;
    private OnReportActionListener actionListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    
    public interface OnReportActionListener {
        void onReportAction(Report report, String action);
    }
    
    public ReportAdapter(List<Report> reports, OnReportActionListener actionListener) {
        this.reports = reports;
        this.actionListener = actionListener;
    }
    
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        
        // Report header
        holder.reportType.setText(report.reportType.toUpperCase());
        holder.reportReason.setText(report.reportReason.replace("_", " ").toUpperCase());
        
        // Reporter info
        holder.reporterName.setText("Reported by: " + (report.reporterName != null ? report.reporterName : "Anonymous"));
        
        // Timestamp
        if (report.reportTimestamp != null) {
            Date date = report.reportTimestamp.toDate();
            holder.reportDate.setText(dateFormat.format(date));
        }
        
        // Content based on report type
        if ("post".equals(report.reportType)) {
            holder.contentText.setText("Post: " + (report.postContent != null ? 
                truncateText(report.postContent, 100) : "Content not available"));
            holder.contentAuthor.setText("Author: " + (report.postAuthor != null ? report.postAuthor : "Unknown"));
        } else if ("user".equals(report.reportType)) {
            holder.contentText.setText("User ID: " + (report.reportedUserId != null ? report.reportedUserId : "Unknown"));
            holder.contentAuthor.setText("User being reported");
        } else if ("chat_message".equals(report.reportType)) {
            holder.contentText.setText("Message: " + (report.messageContent != null ? 
                truncateText(report.messageContent, 100) : "Content not available"));
            holder.contentAuthor.setText("Room: " + (report.chatRoomName != null ? report.chatRoomName : "Unknown"));
        }
        
        // Description
        if (report.reportDescription != null && !report.reportDescription.trim().isEmpty()) {
            holder.reportDescription.setText("Details: " + report.reportDescription);
            holder.reportDescription.setVisibility(View.VISIBLE);
        } else {
            holder.reportDescription.setVisibility(View.GONE);
        }
        
        // Action buttons
        holder.btnDismiss.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReportAction(report, "dismiss");
            }
        });
        
        holder.btnResolve.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReportAction(report, "resolve");
            }
        });
        
        holder.btnBanUser.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReportAction(report, "ban_user");
            }
        });
        
        holder.btnDeleteContent.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReportAction(report, "delete_content");
            }
        });
        
        // Show/hide ban button based on report type
        if ("user".equals(report.reportType) || report.reportedUserId != null) {
            holder.btnBanUser.setVisibility(View.VISIBLE);
        } else {
            holder.btnBanUser.setVisibility(View.GONE);
        }
        
        // Show/hide delete content button
        if ("post".equals(report.reportType) || "chat_message".equals(report.reportType)) {
            holder.btnDeleteContent.setVisibility(View.VISIBLE);
        } else {
            holder.btnDeleteContent.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return reports.size();
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView reportType, reportReason, reporterName, reportDate;
        TextView contentText, contentAuthor, reportDescription;
        Button btnDismiss, btnResolve, btnBanUser, btnDeleteContent;
        
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportType = itemView.findViewById(R.id.reportType);
            reportReason = itemView.findViewById(R.id.reportReason);
            reporterName = itemView.findViewById(R.id.reporterName);
            reportDate = itemView.findViewById(R.id.reportDate);
            contentText = itemView.findViewById(R.id.contentText);
            contentAuthor = itemView.findViewById(R.id.contentAuthor);
            reportDescription = itemView.findViewById(R.id.reportDescription);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
            btnResolve = itemView.findViewById(R.id.btnResolve);
            btnBanUser = itemView.findViewById(R.id.btnBanUser);
            btnDeleteContent = itemView.findViewById(R.id.btnDeleteContent);
        }
    }
}