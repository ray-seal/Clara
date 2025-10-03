# Firebase Realtime Database Rules for Clara Chat System

## Overview
These rules secure the chat system while allowing authenticated users to participate in rooms they have access to.

## Rules Configuration

Add these rules to your Firebase Realtime Database in the Firebase Console:

```json
{
  "rules": {
    "chatRooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null && (!data.exists() || data.child('createdBy').val() === auth.uid || data.child('members').hasChild(auth.uid))",
        "members": {
          ".write": "auth != null"
        }
      }
    },
    "messages": {
      "$roomId": {
        ".read": "auth != null && (root.child('chatRooms').child($roomId).child('isPrivate').val() === false || root.child('chatRooms').child($roomId).child('members').hasChild(auth.uid))",
        ".write": "auth != null && (root.child('chatRooms').child($roomId).child('isPrivate').val() === false || root.child('chatRooms').child($roomId).child('members').hasChild(auth.uid))",
        "$messageId": {
          ".validate": "newData.hasChildren(['senderId', 'senderName', 'content', 'timestamp']) && newData.child('senderId').val() === auth.uid"
        }
      }
    },
    "privateMessages": {
      "$conversationId": {
        ".read": "auth != null && $conversationId.contains(auth.uid)",
        ".write": "auth != null && $conversationId.contains(auth.uid)",
        "$messageId": {
          ".validate": "newData.hasChildren(['senderId', 'recipientId', 'content', 'timestamp']) && newData.child('senderId').val() === auth.uid"
        }
      }
    }
  }
}
```

## Rule Explanations

### Chat Rooms (`/chatRooms`)
- **Read**: Any authenticated user can view room information
- **Write**: Only authenticated users can create rooms, and only room creators or members can modify existing rooms
- **Members Write**: Any authenticated user can add themselves to a room's member list (for joining)

### Messages (`/messages/$roomId`)
- **Read**: 
  - Public rooms: Any authenticated user
  - Private rooms: Only room members
- **Write**: 
  - Public rooms: Any authenticated user
  - Private rooms: Only room members
- **Validation**: Messages must have required fields and senderId must match authenticated user

### Private Messages (`/privateMessages`)
- **Read/Write**: Only participants in the conversation (user IDs in conversationId)
- **Validation**: Messages must have required fields and senderId must match authenticated user

## Setup Instructions

1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your Clara project
3. Navigate to "Realtime Database" in the left menu
4. Click on the "Rules" tab
5. Replace the existing rules with the rules above
6. Click "Publish" to save

## Security Notes

1. All operations require Firebase Authentication
2. Users can only send messages with their own user ID
3. Private room access is restricted to members
4. Message validation prevents malformed data
5. Timestamp validation should be added in production for additional security

## Testing Rules

After publishing the rules, test by:

1. Creating a public room (should succeed when authenticated)
2. Creating a private room (should succeed)
3. Joining a public room (should succeed)
4. Attempting to access a private room without membership (should fail)
5. Sending messages in rooms you're a member of (should succeed)

## Additional Security Recommendations

For production deployment, consider:

1. Add rate limiting to prevent spam
2. Implement server-side timestamp using `.sv: "timestamp"`
3. Add message length validation
4. Implement user reporting and blocking
5. Add moderator roles for topic rooms
6. Implement content filtering
7. Add message retention policies
8. Monitor for abuse patterns

## Troubleshooting

If you encounter permission errors:

1. Verify user is authenticated (check FirebaseAuth.getCurrentUser())
2. Check room membership for private rooms
3. Verify database URL in google-services.json
4. Check Firebase Console for rule validation errors
5. Test with Firebase Realtime Database emulator for debugging

## Database Indexes

For better query performance, add these indexes in the Firebase Console:

```json
{
  "rules": {
    ".indexOn": ["isPrivate", "topic", "createdAt", "lastMessageTime"]
  }
}
```

Add indexes to the chatRooms node for efficient filtering and sorting.
