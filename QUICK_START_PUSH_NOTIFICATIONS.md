# Quick Start: Firebase Push Notifications

## Choose Your Approach

### Option 1: Firestore Triggers (Recommended) ⭐
**Pros**: Automatic, no app changes needed, reliable
**Cons**: Requires Cloud Functions deployment

### Option 2: HTTP Cloud Function
**Pros**: Simple, direct control
**Cons**: Requires app changes, manual calls

## Quick Setup (Option 1 - Recommended)

### 1. Update Firebase Project ID
Edit `.firebaserc`:
```json
{
  "projects": {
    "default": "YOUR_ACTUAL_FIREBASE_PROJECT_ID"
  }
}
```

### 2. Install Dependencies & Deploy
```bash
# Navigate to functions directory
cd functions

# Install dependencies
npm install

# Deploy functions (requires Firebase CLI with Node 20+)
firebase deploy --only functions
```

### 3. Test
1. React to someone's post in your app
2. Check if they receive a push notification
3. Monitor logs: `firebase functions:log`

## Firebase Project ID

You can find your Firebase Project ID in:
1. Firebase Console → Project Settings → General tab
2. Or in your `google-services.json` file under `project_id`

## Required Firebase Services

Make sure these are enabled in Firebase Console:
- ✅ **Cloud Firestore** (already enabled)
- ✅ **Firebase Authentication** (already enabled)  
- ✅ **Cloud Messaging** (already enabled)
- ⚠️ **Cloud Functions** (needs to be enabled)

## Firestore Collections Used

The functions monitor these collections:
- `notifications` - Created by your app when reactions/comments happen
- `push_notification_requests` - Alternative collection for manual requests
- `fcm_tokens` - FCM tokens stored by your app

## Testing Steps

### 1. Deploy Functions
```bash
firebase deploy --only functions
```

### 2. Test in App
1. Install latest app version
2. Make sure two users are logged in (on different devices)
3. User A creates a post
4. User B reacts to the post
5. User A should get a push notification

### 3. Manual Test (Firebase Console)
1. Go to Firestore in Firebase Console
2. Create a test document in `notifications` collection:
```json
{
  "userId": "test-user-id",
  "title": "Test Notification", 
  "message": "Testing push notifications",
  "type": "reaction",
  "fromUserId": "sender-id",
  "timestamp": "2025-10-05T10:00:00Z"
}
```

## Troubleshooting

### No Notifications Received?
1. **Check FCM Tokens**: Go to Firestore → `fcm_tokens` collection → verify tokens exist
2. **Check Function Logs**: `firebase functions:log` 
3. **Check App Permissions**: Settings → Apps → Clara → Notifications (should be enabled)
4. **Test Firebase Messaging**: Firebase Console → Cloud Messaging → Send test message

### Function Deploy Errors?
1. **Node Version**: Functions require Node.js 20+
2. **Firebase CLI**: Update with `npm install -g firebase-tools`
3. **Project ID**: Make sure `.firebaserc` has correct project ID
4. **Billing**: Cloud Functions require Blaze plan (pay-as-you-go)

### Still Having Issues?
1. Check function execution in Firebase Console → Functions
2. Verify Firestore security rules allow function access
3. Test with Firebase Console test messaging first
4. Check if app is in foreground vs background (affects notification display)

## Cost

- **Typical usage**: ~$0.01/month for 100 notifications/day
- **Free tier**: 125K function invocations/month
- **FCM**: Free up to 100 million messages/month

## Success Indicators

✅ Functions deployed successfully  
✅ Function logs show successful execution  
✅ Notifications appear in `processed` state in Firestore  
✅ Users receive push notifications on their devices  
✅ Clicking notifications navigates to correct posts  

Your app is already configured to work with these functions - no code changes needed for Option 1!