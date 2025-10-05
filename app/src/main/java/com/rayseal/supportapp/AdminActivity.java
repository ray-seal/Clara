package com.rayseal.supportapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin panel for moderation and user management
 */
public class AdminActivity extends AppCompatActivity {
    
    private Button btnReports, btnFlaggedContent, btnUserManagement, btnBanUsers;
    private RecyclerView adminRecyclerView;
    private ProgressBar progressBar;
    private TextView adminTitle;
    
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentView = "reports"; // "reports", "flagged", "users", "banned"
    
    // Adapters for different views
    private ReportAdapter reportAdapter;
    private FlaggedContentAdapter flaggedAdapter;
    private UserManagementAdapter userAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        
        db = FirebaseFirestore.getInstance();
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Check if user is admin
        ModerationUtils.checkAdminStatus(isAdmin -> {
            if (!isAdmin) {
                Toast.makeText(this, "Access denied: Admin privileges required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            initializeViews();
            setupButtons();
            loadReports(); // Default view
        });
    }
    
    private void initializeViews() {
        adminRecyclerView = findViewById(R.id.adminRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        adminTitle = findViewById(R.id.adminTitle);
        
        btnReports = findViewById(R.id.btnReports);
        btnFlaggedContent = findViewById(R.id.btnFlaggedContent);
        btnUserManagement = findViewById(R.id.btnUserManagement);
        btnBanUsers = findViewById(R.id.btnBanUsers);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupButtons() {
        btnReports.setOnClickListener(v -> {
            currentView = "reports";
            updateButtonStyles();
            loadReports();
        });
        
        btnFlaggedContent.setOnClickListener(v -> {
            currentView = "flagged";
            updateButtonStyles();
            loadFlaggedContent();
        });
        
        btnUserManagement.setOnClickListener(v -> {
            currentView = "users";
            updateButtonStyles();
            loadUsers();
        });
        
        btnBanUsers.setOnClickListener(v -> {
            currentView = "banned";
            updateButtonStyles();
            loadBannedUsers();
        });
        
        updateButtonStyles();
    }
    
    private void updateButtonStyles() {
        // Reset all buttons
        btnReports.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        btnFlaggedContent.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        btnUserManagement.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        btnBanUsers.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        
        // Highlight selected button
        switch (currentView) {
            case "reports":
                btnReports.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
                adminTitle.setText("Pending Reports");
                break;
            case "flagged":
                btnFlaggedContent.setBackgroundTintList(getColorStateList(android.R.color.holo_orange_light));
                adminTitle.setText("Flagged Content");
                break;
            case "users":
                btnUserManagement.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
                adminTitle.setText("User Management");
                break;
            case "banned":
                btnBanUsers.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
                adminTitle.setText("Banned Users");
                break;
        }
    }
    
    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("reports")
            .whereEqualTo("status", "pending")
            .orderBy("reportTimestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Report> reports = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Report report = doc.toObject(Report.class);
                    report.reportId = doc.getId();
                    reports.add(report);
                }
                
                reportAdapter = new ReportAdapter(reports, this::handleReportAction);
                adminRecyclerView.setAdapter(reportAdapter);
                progressBar.setVisibility(View.GONE);
                
                if (reports.isEmpty()) {
                    Toast.makeText(this, "No pending reports", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading reports", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }
    
    private void loadFlaggedContent() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("flagged_content")
            .whereEqualTo("status", "pending")
            .orderBy("flaggedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<FlaggedContent> flaggedContent = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    FlaggedContent content = doc.toObject(FlaggedContent.class);
                    content.flagId = doc.getId();
                    flaggedContent.add(content);
                }
                
                                        flaggedAdapter = new FlaggedContentAdapter(this, flaggedContent, new FlaggedContentAdapter.OnFlaggedContentActionListener() {
                            @Override
                            public void onApproveContent(FlaggedContent content) {
                                handleFlaggedContentAction("approve", content);
                            }
                            
                            @Override
                            public void onRejectContent(FlaggedContent content) {
                                handleFlaggedContentAction("reject", content);
                            }
                            
                            @Override
                            public void onDeleteContent(FlaggedContent content) {
                                handleFlaggedContentAction("delete", content);
                            }
                            
                            @Override
                            public void onBanUser(FlaggedContent content) {
                                handleFlaggedContentAction("ban", content);
                            }
                        });
                adminRecyclerView.setAdapter(flaggedAdapter);
                progressBar.setVisibility(View.GONE);
                
                if (flaggedContent.isEmpty()) {
                    Toast.makeText(this, "No flagged content", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading flagged content", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }
    
    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("profiles")
            .whereEqualTo("isBanned", false)
            .orderBy("displayName")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Profile> users = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Profile profile = doc.toObject(Profile.class);
                    profile.uid = doc.getId();
                    if (!profile.uid.equals(currentUserId)) { // Don't show current admin
                        users.add(profile);
                    }
                }
                
                                        userAdapter = new UserManagementAdapter(this, users, new UserManagementAdapter.OnUserActionListener() {
                            @Override
                            public void onBanUser(Profile user) {
                                handleUserAction("ban", user);
                            }
                            
                            @Override
                            public void onUnbanUser(Profile user) {
                                handleUserAction("unban", user);
                            }
                            
                            @Override
                            public void onWarnUser(Profile user) {
                                handleUserAction("warn", user);
                            }
                            
                            @Override
                            public void onPromoteToAdmin(Profile user) {
                                handleUserAction("promote", user);
                            }
                            
                            @Override
                            public void onDemoteFromAdmin(Profile user) {
                                handleUserAction("demote", user);
                            }
                            
                            @Override
                            public void onViewProfile(Profile user) {
                                handleUserAction("view", user);
                            }
                        });
                adminRecyclerView.setAdapter(userAdapter);
                progressBar.setVisibility(View.GONE);
                
                if (users.isEmpty()) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }
    
    private void loadBannedUsers() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("profiles")
            .whereEqualTo("isBanned", true)
            .orderBy("bannedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Profile> bannedUsers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Profile profile = doc.toObject(Profile.class);
                    profile.uid = doc.getId();
                    bannedUsers.add(profile);
                }
                
                                        userAdapter = new UserManagementAdapter(this, bannedUsers, new UserManagementAdapter.OnUserActionListener() {
                            @Override
                            public void onBanUser(Profile user) {
                                handleBannedUserAction("ban", user);
                            }
                            
                            @Override
                            public void onUnbanUser(Profile user) {
                                handleBannedUserAction("unban", user);
                            }
                            
                            @Override
                            public void onWarnUser(Profile user) {
                                handleBannedUserAction("warn", user);
                            }
                            
                            @Override
                            public void onPromoteToAdmin(Profile user) {
                                handleBannedUserAction("promote", user);
                            }
                            
                            @Override
                            public void onDemoteFromAdmin(Profile user) {
                                handleBannedUserAction("demote", user);
                            }
                            
                            @Override
                            public void onViewProfile(Profile user) {
                                handleBannedUserAction("view", user);
                            }
                        });
                adminRecyclerView.setAdapter(userAdapter);
                progressBar.setVisibility(View.GONE);
                
                if (bannedUsers.isEmpty()) {
                    Toast.makeText(this, "No banned users", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading banned users", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }
    
    private void handleReportAction(Report report, String action) {
        // Handle report actions: approve, dismiss, ban user, etc.
        switch (action) {
            case "dismiss":
                dismissReport(report);
                break;
            case "resolve":
                resolveReport(report);
                break;
            case "ban_user":
                showBanUserDialog(report.reportedUserId, report);
                break;
            case "delete_content":
                deleteReportedContent(report);
                break;
        }
    }
    
    private void handleFlaggedContentAction(String action, FlaggedContent content) {
        // Handle flagged content actions: approve, reject, edit
        switch (action) {
            case "approve":
                approveFlaggedContent(content);
                break;
            case "reject":
                rejectFlaggedContent(content);
                break;
            case "delete":
                deleteFlaggedContent(content);
                break;
            case "ban":
                // For ban action, we need to find the user first
                db.collection("users").document(content.authorUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Profile user = documentSnapshot.toObject(Profile.class);
                            if (user != null) {
                                banUser(user.uid, "Inappropriate content", null);
                            }
                        }
                    });
                break;
        }
    }
    
    private void handleUserAction(String action, Profile user) {
        // Handle user management actions: ban, warn, make admin
        switch (action) {
            case "ban":
                banUser(user.uid, "Banned by admin", null);
                break;
            case "unban":
                unbanUser(user);
                break;
            case "warn":
                warnUser(user);
                break;
            case "promote":
                makeUserAdmin(user);
                break;
            case "demote":
                demoteFromAdmin(user);
                break;
            case "view":
                viewUserProfile(user);
                break;
        }
    }
    
    private void handleBannedUserAction(String action, Profile user) {
        // Handle banned user actions: unban, view details
        switch (action) {
            case "unban":
                unbanUser(user);
                break;
            case "view":
                viewBanDetails(user);
                break;
        }
    }
    
    private void dismissReport(Report report) {
        db.collection("reports").document(report.reportId)
            .update("status", "dismissed", "reviewedBy", currentUserId, "reviewedAt", com.google.firebase.Timestamp.now())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Report dismissed", Toast.LENGTH_SHORT).show();
                loadReports(); // Refresh the list
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error dismissing report", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void resolveReport(Report report) {
        db.collection("reports").document(report.reportId)
            .update("status", "resolved", "reviewedBy", currentUserId, "reviewedAt", com.google.firebase.Timestamp.now())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Report resolved", Toast.LENGTH_SHORT).show();
                loadReports();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error resolving report", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showBanUserDialog(String userId, Report report) {
        // Show dialog for banning user with reason input
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Ban User");
        
        EditText reasonInput = new EditText(this);
        reasonInput.setHint("Reason for ban...");
        builder.setView(reasonInput);
        
        builder.setPositiveButton("Ban", (dialog, which) -> {
            String reason = reasonInput.getText().toString().trim();
            banUser(userId, reason, report);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void banUser(String userId, String reason, Report report) {
        db.collection("profiles").document(userId)
            .update(
                "isBanned", true,
                "bannedBy", currentUserId,
                "bannedAt", System.currentTimeMillis(),
                "banReason", reason
            )
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User banned successfully", Toast.LENGTH_SHORT).show();
                
                // Update report if provided
                if (report != null) {
                    db.collection("reports").document(report.reportId)
                        .update("status", "resolved", "actionTaken", "user_banned",
                               "reviewedBy", currentUserId, "reviewedAt", com.google.firebase.Timestamp.now());
                }
                
                // Refresh current view
                if (currentView.equals("reports")) {
                    loadReports();
                } else {
                    loadUsers();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error banning user", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void approveFlaggedContent(FlaggedContent content) {
        db.collection("flagged_content").document(content.flagId)
            .update("status", "approved", "reviewedBy", currentUserId, 
                   "reviewedAt", com.google.firebase.Timestamp.now(), "contentVisible", true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Content approved", Toast.LENGTH_SHORT).show();
                loadFlaggedContent();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error approving content", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void rejectFlaggedContent(FlaggedContent content) {
        db.collection("flagged_content").document(content.flagId)
            .update("status", "rejected", "reviewedBy", currentUserId, 
                   "reviewedAt", com.google.firebase.Timestamp.now(), "contentVisible", false)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Content rejected", Toast.LENGTH_SHORT).show();
                loadFlaggedContent();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error rejecting content", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void deleteFlaggedContent(FlaggedContent content) {
        // Delete the actual content based on type
        String collection = "";
        switch (content.contentType.toLowerCase()) {
            case "post":
                collection = "posts";
                break;
            case "comment":
                collection = "comments";
                break;
            case "chat":
                collection = "chat_messages";
                break;
        }
        
        if (!collection.isEmpty()) {
            db.collection(collection).document(content.contentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update flagged content status
                    db.collection("flagged_content").document(content.flagId)
                        .update("status", "deleted", "reviewedBy", currentUserId, 
                               "reviewedAt", com.google.firebase.Timestamp.now())
                        .addOnSuccessListener(aVoid2 -> {
                            Toast.makeText(this, "Content deleted", Toast.LENGTH_SHORT).show();
                            loadFlaggedContent();
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting content", Toast.LENGTH_SHORT).show();
                });
        }
    }
    
    private void deleteReportedContent(Report report) {
        // Delete the actual content based on type
        String collection = "";
        switch (report.reportType.toLowerCase()) {
            case "post":
                collection = "posts";
                break;
            case "comment":
                collection = "comments";
                break;
            case "chat":
                collection = "chat_messages";
                break;
        }
        
        if (!collection.isEmpty() && report.reportedItemId != null) {
            db.collection(collection).document(report.reportedItemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update report status
                    db.collection("reports").document(report.reportId)
                        .update("status", "resolved", "actionTaken", "content_deleted",
                               "reviewedBy", currentUserId, "reviewedAt", com.google.firebase.Timestamp.now())
                        .addOnSuccessListener(aVoid2 -> {
                            Toast.makeText(this, "Content deleted", Toast.LENGTH_SHORT).show();
                            loadReports();
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting content", Toast.LENGTH_SHORT).show();
                });
        }
    }
    
    private void warnUser(Profile user) {
        int newWarningCount = user.warningCount + 1;
        
        db.collection("users").document(user.uid)
            .update("warningCount", newWarningCount)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User warned", Toast.LENGTH_SHORT).show();
                
                // Send notification to user
                Notification notification = new Notification();
                notification.type = "warning";
                notification.title = "Warning Received";
                notification.message = "You have received a warning from an administrator.";
                notification.timestamp = com.google.firebase.Timestamp.now();
                notification.userId = user.uid;
                
                db.collection("notifications").add(notification);
                
                loadUsers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error warning user", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void makeUserAdmin(Profile user) {
        db.collection("users").document(user.uid)
            .update("isAdmin", true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User promoted to admin", Toast.LENGTH_SHORT).show();
                
                // Send notification to user
                Notification notification = new Notification();
                notification.type = "promotion";
                notification.title = "Admin Promotion";
                notification.message = "You have been promoted to administrator.";
                notification.timestamp = com.google.firebase.Timestamp.now();
                notification.userId = user.uid;
                
                db.collection("notifications").add(notification);
                
                loadUsers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error promoting user", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void demoteFromAdmin(Profile user) {
        db.collection("users").document(user.uid)
            .update("isAdmin", false)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Admin privileges removed", Toast.LENGTH_SHORT).show();
                loadUsers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error removing admin privileges", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void viewUserProfile(Profile user) {
        // Create intent to view user profile
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("userId", user.uid);
        startActivity(intent);
    }
    
    private void unbanUser(Profile user) {
        db.collection("users").document(user.uid)
            .update("isBanned", false, "bannedBy", null, "bannedAt", 0L, "banReason", null)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User unbanned", Toast.LENGTH_SHORT).show();
                
                // Send notification to user
                Notification notification = new Notification();
                notification.type = "unban";
                notification.title = "Account Unbanned";
                notification.message = "Your account has been unbanned. Welcome back!";
                notification.timestamp = com.google.firebase.Timestamp.now();
                notification.userId = user.uid;
                
                db.collection("notifications").add(notification);
                
                loadBannedUsers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error unbanning user", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void viewBanDetails(Profile user) {
        if (user.isBanned) {
            String banDetails = "User: " + user.displayName + "\n";
            if (user.bannedAt > 0) {
                banDetails += "Banned on: " + new java.util.Date(user.bannedAt).toString() + "\n";
            }
            if (user.bannedBy != null && !user.bannedBy.isEmpty()) {
                banDetails += "Banned by: " + user.bannedBy + "\n";
            }
            if (user.banReason != null && !user.banReason.isEmpty()) {
                banDetails += "Reason: " + user.banReason;
            }
            
            new AlertDialog.Builder(this)
                .setTitle("Ban Details")
                .setMessage(banDetails)
                .setPositiveButton("OK", null)
                .show();
        }
    }
}