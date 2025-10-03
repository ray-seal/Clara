# Clara Real-Time Chat System - Implementation Guide

## Overview
This document describes the real-time chat system implementation for the Clara Android app. The chat system uses Firebase Realtime Database for real-time messaging and supports topic-based rooms, public/private rooms, and user-controlled privacy settings.

## Features Implemented

### 1. Topic-Based Chat Rooms
Six predefined topic rooms focused on specific mental health topics:
- Anxiety Support
- Depression Support  
- Insomnia Support
- Gender Dysphoria Support
- Disability Support
- Addiction Support

These rooms are automatically created and accessible to all users.

### 2. Public Chat Rooms
- Users can create public chat rooms on any topic
- Any authenticated user can join and participate
- Rooms are listed in the "Public Rooms" tab

### 3. Private Chat Rooms
- Users can create private chat rooms
- Only invited members can join and view messages
- Rooms are listed in the "Private Rooms" tab for members

### 4. Real-Time Messaging
- Messages appear instantly using Firebase Realtime Database listeners
- Auto-scroll to latest messages
- Message history is persistent
- Displays sender name, message content, and timestamp

### 5. Privacy Controls
- Users can enable/disable private messaging in their profile settings
- Toggle "Allow Private Messages" in Profile > Privacy Settings
- When disabled, other users cannot send private messages to them

## Architecture

### Data Models

**ChatRoom.java**
- `roomId`: Unique identifier
- `roomName`: Display name
- `topic`: Category (Anxiety, Depression, etc.)
- `isPrivate`: Boolean for public/private status
- `createdBy`: User ID of creator
- `members`: List of user IDs with access
- `lastMessage`: Preview of last message
- `lastMessageTime`: Timestamp of last activity

**ChatMessage.java**
- `messageId`: Unique identifier
- `senderId`: User ID of sender
- `senderName`: Display name of sender
- `content`: Message text
- `timestamp`: When message was sent
- `roomId`: Associated chat room
- `recipientId`: For private messages (future)

**PrivacySettings.java**
- Added `allowPrivateMessages` field (default: true)
- Controls whether user accepts private messages

### Activities

**ChatRoomListActivity**
- Displays available chat rooms in three tabs:
  - Topic Rooms: Predefined mental health topic rooms
  - Public Rooms: User-created public rooms
  - Private Rooms: Private rooms user is member of
- "Create Room" button to make new rooms
- Click on room to join and start chatting

**ChatRoomActivity**
- Real-time messaging interface
- RecyclerView with auto-scroll to latest
- Message input field and send button
- Room info button shows details
- Messages aligned left (others) or right (own)

### UI Components

**Layouts:**
- `activity_chat_room_list.xml`: Room list with tabs
- `activity_chat_room.xml`: Messaging interface
- `item_chat_room.xml`: Room list item card
- `item_chat_message.xml`: Message bubble

**Adapters:**
- `ChatRoomAdapter`: Displays rooms in RecyclerView
- `ChatMessageAdapter`: Displays messages with proper alignment

## Firebase Database Structure

```
chatRooms/
  ├─ topic_anxiety/
  │   ├─ roomId: "topic_anxiety"
  │   ├─ roomName: "Anxiety Support"
  │   ├─ topic: "Anxiety"
  │   ├─ isPrivate: false
  │   ├─ members: [...]
  │   └─ ...
  ├─ [user-created-room-id]/
  │   └─ ...
  
messages/
  ├─ [roomId]/
  │   ├─ [messageId]/
  │   │   ├─ senderId: "..."
  │   │   ├─ senderName: "..."
  │   │   ├─ content: "..."
  │   │   └─ timestamp: 1234567890
  │   └─ ...
```

## User Flow

### Accessing Chat
1. User logs in and reaches PublicFeedActivity
2. Click "Chat" button in top bar
3. ChatRoomListActivity opens

### Joining a Room
1. Browse rooms in Topic/Public/Private tabs
2. Click on a room to join
3. ChatRoomActivity opens with real-time messages
4. Send messages that appear instantly for all members

### Creating a Room
1. Click "+" icon in ChatRoomListActivity
2. Enter room name and select topic
3. Choose public or private
4. Room is created and user is added as member

### Managing Privacy
1. Open Profile from PublicFeedActivity
2. Scroll to Privacy Settings
3. Toggle "Allow Private Messages" switch
4. Save profile changes

## Security Considerations

1. **Authentication**: All chat features require Firebase Authentication
2. **Private Room Access**: Only members can view private room messages
3. **Privacy Controls**: Users can opt-out of private messaging
4. **Data Validation**: User inputs are validated before database writes

## Testing

To test the chat system:

1. **Topic Rooms**: 
   - Open chat, verify 6 topic rooms exist
   - Join a topic room and send messages
   
2. **Public Rooms**:
   - Create a public room
   - Join from another account
   - Verify both users can message
   
3. **Private Rooms**:
   - Create a private room
   - Verify non-members cannot access
   - Add members and test messaging
   
4. **Privacy Settings**:
   - Toggle private messaging off
   - Verify setting saves in profile

## Future Enhancements

1. Direct private messaging between users
2. Message reactions and replies
3. Image sharing in chat
4. Push notifications for new messages
5. Message search functionality
6. User blocking and reporting
7. Moderator roles for topic rooms

## Dependencies Added

```gradle
implementation("com.google.firebase:firebase-database:20.3.0")
```

## Files Modified

1. `PrivacySettings.java` - Added allowPrivateMessages field
2. `ProfileActivity.java` - Added private messaging toggle
3. `PublicFeedActivity.java` - Added chat button
4. `AndroidManifest.xml` - Registered new activities
5. `build.gradle.kts` - Added Firebase Realtime Database

## Files Created

1. `ChatRoom.java` - Chat room data model
2. `ChatMessage.java` - Message data model
3. `ChatRoomListActivity.java` - Room list and creation
4. `ChatRoomActivity.java` - Messaging interface
5. `ChatRoomAdapter.java` - Room list adapter
6. `ChatMessageAdapter.java` - Message list adapter
7. `activity_chat_room_list.xml` - Room list layout
8. `activity_chat_room.xml` - Chat room layout
9. `item_chat_room.xml` - Room item layout
10. `item_chat_message.xml` - Message bubble layout

## Support

For issues or questions about the chat system:
1. Check Firebase Console for database connectivity
2. Verify authentication is working
3. Review device logs for error messages
4. Ensure Firebase Realtime Database rules allow authenticated read/write
