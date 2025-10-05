# Firebase Push Notifications Setup Guide

This guide will help you set up Firebase Cloud Functions to handle push notifications for the Clara app.

## Prerequisites

1. **Firebase Project**: Make sure you have a Firebase project created
2. **Node.js**: Version 20 or higher (for Firebase Functions)
3. **Firebase CLI**: Version 12.0.0 or higher

## Setup Instructions

### Step 1: Update Firebase Project Configuration

1. **Update `.firebaserc`**:
   ```json
   {
     "projects": {
       "default": "your-actual-firebase-project-id"
     }
   }
   ```
   Replace `your-actual-firebase-project-id` with your real Firebase project ID.

### Step 2: Install Dependencies

```bash
cd functions
npm install
```

### Step 3: Deploy Cloud Functions

```bash
# Login to Firebase (if not already logged in)
firebase login

# Deploy functions
firebase deploy --only functions
```

### Step 4: Test the Setup

Once deployed, the functions will automatically trigger when:
- New documents are created in the `notifications` collection
- New documents are created in the `push_notification_requests` collection

## Available Cloud Functions

### 1. `sendNotificationOnCreate`
- **Trigger**: New document in `notifications/{notificationId}`
- **Purpose**: Automatically sends push notifications when app creates notifications
- **Flow**: App creates notification → Function sends FCM message → Updates notification with sent status

### 2. `processPushNotificationRequest`
- **Trigger**: New document in `push_notification_requests/{requestId}`
- **Purpose**: Processes manual push notification requests from the app
- **Flow**: App creates request → Function sends FCM message → Updates request with result

### 3. `sendPushNotification` (Callable)
- **Trigger**: Called directly from app
- **Purpose**: Send immediate push notifications
- **Usage**: For real-time notifications that can't wait for Firestore triggers

### 4. `cleanupOldNotificationRequests` (Scheduled)
- **Trigger**: Daily at midnight UTC
- **Purpose**: Cleanup old processed notification requests
- **Keeps**: System clean and reduces storage costs

## App Integration

Your Android app is already configured to work with these functions. The `PostAdapter.java` creates documents in both collections, so notifications will be sent automatically once functions are deployed.

### Current App Flow:
1. User reacts to post → `PostAdapter.createReactionNotification()`
2. Creates document in `notifications` collection
3. Creates document in `push_notification_requests` collection
4. Cloud functions detect new documents and send FCM messages

## Testing

### Test 1: React to a Post
1. Deploy functions
2. Have two users in the app
3. User A creates a post
4. User B reacts to the post
5. User A should receive a push notification

### Test 2: Comment on a Post
1. User A creates a post
2. User B comments on the post
3. User A should receive a push notification

### Test 3: Manual Testing (Firebase Console)
1. Go to Firebase Console → Firestore
2. Manually create a document in `notifications` collection:
   ```json
   {
     "userId": "recipient-user-id",
     "title": "Test Notification",
     "message": "This is a test",
     "type": "reaction",
     "fromUserId": "sender-user-id",
     "timestamp": "current-timestamp"
   }
   ```
3. Function should trigger and send notification

## Monitoring

### View Function Logs:
```bash
firebase functions:log
```

### Monitor in Firebase Console:
1. Go to Firebase Console → Functions
2. Click on function name to view logs and metrics
3. Check for errors or successful executions

## Troubleshooting

### Common Issues:

1. **Functions not deploying**:
   - Check Node.js version (must be 20+)
   - Verify Firebase CLI version
   - Check internet connection

2. **FCM tokens not found**:
   - Ensure app is properly registering FCM tokens
   - Check `fcm_tokens` collection in Firestore

3. **Notifications not reaching device**:
   - Verify device has notification permissions
   - Check if app is in background/foreground
   - Verify FCM token is valid

4. **Function errors**:
   - Check function logs: `firebase functions:log`
   - Verify Firestore security rules allow function access
   - Check Firebase project permissions

## Security Considerations

- Functions run with Firebase Admin privileges
- Firestore security rules don't apply to functions
- Consider adding additional validation in functions
- Monitor function usage and costs

## Cost Estimation

- **Function invocations**: ~$0.0000004 per invocation
- **Typical usage**: 100 notifications/day = ~$0.01/month
- **FCM messages**: Free up to 100 million/month

## Next Steps

1. Deploy the functions
2. Test with real devices
3. Monitor logs for any issues
4. Consider adding analytics/tracking
5. Set up alerts for function failures

## Support

If you encounter issues:
1. Check Firebase Console logs
2. Verify Firestore data structure
3. Test with Firebase Console messaging
4. Check device notification permissions