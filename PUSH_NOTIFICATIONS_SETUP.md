# Push Notifications Setup Guide for Clara App

## Overview

Your Clara app already has the client-side push notification setup complete. To actually send push notifications, you need to set up Firebase Cloud Functions that will process the notification requests created by your app.

## What's Already Implemented

✅ **Client-Side Setup Complete:**
- FCM dependency added to `app/build.gradle.kts`
- `MyFirebaseMessagingService.java` created to handle incoming notifications
- FCM token management and storage
- Notification creation logic in `PostAdapter.java`
- Push notification request queuing in Firestore

## Required Setup Steps

### 1. Initialize Firebase Functions

```bash
cd /home/ray/clara/app/Clara
firebase login
firebase init functions
```

When prompted:
- Select your existing Firebase project
- Choose TypeScript or JavaScript (TypeScript recommended)
- Install dependencies with npm

### 2. Create the Cloud Function

Create or edit `functions/src/index.ts` (TypeScript) or `functions/index.js` (JavaScript):

**TypeScript Version (`functions/src/index.ts`):**

```typescript
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {initializeApp, getApps} from "firebase-admin/app";
import {getMessaging} from "firebase-admin/messaging";
import {getFirestore} from "firebase-admin/firestore";

// Initialize Firebase Admin SDK
if (getApps().length === 0) {
  initializeApp();
}

const messaging = getMessaging();
const db = getFirestore();

export const sendPushNotification = onDocumentCreated(
  "push_notifications/{docId}",
  async (event) => {
    const pushRequest = event.data?.data();
    
    if (!pushRequest) {
      console.log("No push request data found");
      return;
    }

    const {token, title, body, type, postId, userId} = pushRequest;

    if (!token || !title || !body) {
      console.log("Missing required fields:", {token, title, body});
      return;
    }

    // Prepare the FCM message
    const message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      data: {
        type: type || "",
        postId: postId || "",
        userId: userId || "",
      },
      android: {
        notification: {
          icon: "ic_notifications",
          color: "#4CAF50",
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
      },
    };

    try {
      // Send the notification
      const response = await messaging.send(message);
      console.log("Successfully sent message:", response);
      
      // Delete the processed request
      await event.data?.ref.delete();
      
    } catch (error) {
      console.error("Error sending message:", error);
    }
  }
);
```

**JavaScript Version (`functions/index.js`):**

```javascript
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp, getApps} = require("firebase-admin/app");
const {getMessaging} = require("firebase-admin/messaging");
const {getFirestore} = require("firebase-admin/firestore");

// Initialize Firebase Admin SDK
if (getApps().length === 0) {
  initializeApp();
}

const messaging = getMessaging();
const db = getFirestore();

exports.sendPushNotification = onDocumentCreated(
  "push_notifications/{docId}",
  async (event) => {
    const pushRequest = event.data?.data();
    
    if (!pushRequest) {
      console.log("No push request data found");
      return;
    }

    const {token, title, body, type, postId, userId} = pushRequest;

    if (!token || !title || !body) {
      console.log("Missing required fields:", {token, title, body});
      return;
    }

    // Prepare the FCM message
    const message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      data: {
        type: type || "",
        postId: postId || "",
        userId: userId || "",
      },
      android: {
        notification: {
          icon: "ic_notifications",
          color: "#4CAF50",
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
      },
    };

    try {
      // Send the notification
      const response = await messaging.send(message);
      console.log("Successfully sent message:", response);
      
      // Delete the processed request
      await event.data?.ref.delete();
      
    } catch (error) {
      console.error("Error sending message:", error);
    }
  }
);
```

### 3. Deploy the Cloud Function

```bash
firebase deploy --only functions
```

### 4. Update Firestore Security Rules

Add these rules to your `firestore.rules` file:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Existing rules...
    
    // FCM tokens - users can only read/write their own
    match /fcm_tokens/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Push notification requests - users can create but not read
    match /push_notifications/{docId} {
      allow create: if request.auth != null;
      // Cloud function will handle reading and deleting
    }
    
    // Notifications - users can only read their own
    match /notifications/{notificationId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

Deploy the updated rules:

```bash
firebase deploy --only firestore:rules
```

### 5. Test Push Notifications

1. **Build and install your app** on a real device (push notifications don't work in emulator)
2. **Sign in** to the app to register the FCM token
3. **React to a post** or trigger another notification event
4. **Check Firebase Console** > Functions > Logs to see if the function executed
5. **Verify** the notification appears on the device

### 6. Optional: Add Comment Notifications

To extend notifications to comments, modify your comment submission code to create notifications similar to how reactions work:

```java
// In your comment submission logic
private void createCommentNotification(Post post, String commentId) {
    if (!post.userId.equals(currentUserId)) {
        // Get current user's profile for notification
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
                Notification notification = Notification.createCommentNotification(
                    post.userId, 
                    currentUserId, 
                    fromUserName, 
                    fromUserProfilePicture, 
                    post.postId, 
                    commentId
                );
                
                // Save notification
                firestore.collection("notifications").add(notification);
                
                // Send push notification
                sendPushNotification(post.userId, notification);
            });
    }
}
```

## Troubleshooting

### Common Issues:

1. **Notifications not appearing:**
   - Check Firebase Console > Functions > Logs for errors
   - Verify FCM tokens are being saved correctly
   - Ensure the app has notification permission

2. **Function deployment fails:**
   - Check your Firebase project billing (Cloud Functions requires Blaze plan)
   - Verify you're logged into the correct Firebase account

3. **Token registration issues:**
   - Make sure `google-services.json` is in the correct location
   - Verify Firebase project configuration

### Testing Commands:

```bash
# View function logs
firebase functions:log

# Test function locally (requires Firebase emulator)
firebase emulators:start

# Check project status
firebase projects:list
```

## Security Considerations

1. **FCM tokens are sensitive** - only store them for authenticated users
2. **Validate notification content** in cloud functions to prevent spam
3. **Rate limit notifications** to prevent abuse
4. **Consider user preferences** for notification types

## Cost Considerations

- Cloud Functions are free for the first 2 million invocations per month
- FCM is free for unlimited notifications
- Consider implementing notification batching for high-volume apps

## Next Steps

After setup, you can extend the notification system by:

1. Adding notification preferences in SettingsActivity
2. Implementing notification categories (reactions, comments, friend requests)
3. Adding rich notifications with images
4. Implementing notification batching and scheduling
5. Adding email notifications as backup