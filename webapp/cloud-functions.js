// Firebase Cloud Functions for Push Notifications
// This file should be deployed as Firebase Cloud Functions

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Send notification when a new message is posted in chat
 */
exports.onNewChatMessage = functions.firestore
  .document('chatMessages/{messageId}')
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const messageId = context.params.messageId;

    try {
      // Get chat room info
      const roomDoc = await db.collection('chatRooms').doc(message.roomId).get();
      if (!roomDoc.exists) return;

      const room = roomDoc.data();
      
      // Get sender info
      const senderDoc = await db.collection('profiles').doc(message.userId).get();
      const sender = senderDoc.exists ? senderDoc.data() : { displayName: 'Someone' };

      // Get room members (excluding sender)
      const memberIds = (room.members || []).filter(id => id !== message.userId);

      // Get FCM tokens for room members
      const tokens = await getFCMTokensForUsers(memberIds);

      if (tokens.length === 0) return;

      // Create notification payload
      const notification = {
        title: `${sender.displayName} in ${room.name}`,
        body: message.text.length > 100 ? message.text.substring(0, 100) + '...' : message.text,
      };

      const data = {
        type: 'message',
        roomId: message.roomId,
        messageId: messageId,
        senderId: message.userId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      };

      // Send notifications
      const response = await messaging.sendMulticast({
        tokens: tokens,
        notification: notification,
        data: data,
        android: {
          priority: 'high',
          notification: {
            channelId: 'chat_messages',
            priority: 'high',
            defaultSound: true,
            defaultVibrateTimings: true
          }
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
              badge: 1
            }
          }
        },
        webpush: {
          notification: {
            icon: '/images/icon-192x192.png',
            badge: '/images/badge-72x72.png',
            tag: 'chat_message',
            requireInteraction: false,
            actions: [
              {
                action: 'reply',
                title: 'Reply'
              },
              {
                action: 'dismiss',
                title: 'Dismiss'
              }
            ]
          }
        }
      });

      console.log('Chat message notifications sent:', response.successCount);

      // Store notification in database
      await storeNotificationsForUsers(memberIds, {
        title: notification.title,
        body: notification.body,
        type: 'message',
        data: data
      });

    } catch (error) {
      console.error('Error sending chat message notification:', error);
    }
  });

/**
 * Send notification when a new friend request is sent
 */
exports.onFriendRequest = functions.firestore
  .document('friendRequests/{requestId}')
  .onCreate(async (snap, context) => {
    const request = snap.data();

    try {
      // Get sender info
      const senderDoc = await db.collection('profiles').doc(request.fromUserId).get();
      const sender = senderDoc.exists ? senderDoc.data() : { displayName: 'Someone' };

      // Get recipient's FCM token
      const tokens = await getFCMTokensForUsers([request.toUserId]);

      if (tokens.length === 0) return;

      // Create notification payload
      const notification = {
        title: 'New Friend Request',
        body: `${sender.displayName} wants to be your friend`
      };

      const data = {
        type: 'friend_request',
        requestId: context.params.requestId,
        fromUserId: request.fromUserId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      };

      // Send notification
      const response = await messaging.sendMulticast({
        tokens: tokens,
        notification: notification,
        data: data,
        android: {
          priority: 'high',
          notification: {
            channelId: 'friend_requests',
            priority: 'high',
            defaultSound: true
          }
        },
        webpush: {
          notification: {
            icon: '/images/icon-192x192.png',
            tag: 'friend_request',
            requireInteraction: true
          }
        }
      });

      console.log('Friend request notification sent:', response.successCount);

      // Store notification in database
      await storeNotificationsForUsers([request.toUserId], {
        title: notification.title,
        body: notification.body,
        type: 'friend_request',
        data: data
      });

    } catch (error) {
      console.error('Error sending friend request notification:', error);
    }
  });

/**
 * Send notification when a new post is created
 */
exports.onNewPost = functions.firestore
  .document('posts/{postId}')
  .onCreate(async (snap, context) => {
    const post = snap.data();
    const postId = context.params.postId;

    try {
      // Get post author info
      const authorDoc = await db.collection('profiles').doc(post.userId).get();
      const author = authorDoc.exists ? authorDoc.data() : { displayName: 'Someone' };

      // Get author's friends
      const friendIds = author.friends || [];
      
      if (friendIds.length === 0) return;

      // Get FCM tokens for friends
      const tokens = await getFCMTokensForUsers(friendIds);

      if (tokens.length === 0) return;

      // Create notification payload
      const notification = {
        title: `${author.displayName} shared a new post`,
        body: post.content.length > 100 ? post.content.substring(0, 100) + '...' : post.content
      };

      const data = {
        type: 'post',
        postId: postId,
        authorId: post.userId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      };

      // Send notifications to friends only (not all users)
      const response = await messaging.sendMulticast({
        tokens: tokens,
        notification: notification,
        data: data,
        android: {
          priority: 'normal',
          notification: {
            channelId: 'posts',
            priority: 'normal',
            defaultSound: true
          }
        },
        webpush: {
          notification: {
            icon: '/images/icon-192x192.png',
            tag: 'new_post'
          }
        }
      });

      console.log('New post notifications sent:', response.successCount);

      // Store notification in database for friends
      await storeNotificationsForUsers(friendIds, {
        title: notification.title,
        body: notification.body,
        type: 'post',
        data: data
      });

    } catch (error) {
      console.error('Error sending new post notification:', error);
    }
  });

/**
 * Send notification when someone comments on a post
 */
exports.onNewComment = functions.firestore
  .document('comments/{commentId}')
  .onCreate(async (snap, context) => {
    const comment = snap.data();

    try {
      // Get post info
      const postDoc = await db.collection('posts').doc(comment.postId).get();
      if (!postDoc.exists) return;

      const post = postDoc.data();
      
      // Don't notify if commenter is the post author
      if (comment.userId === post.userId) return;

      // Get commenter info
      const commenterDoc = await db.collection('profiles').doc(comment.userId).get();
      const commenter = commenterDoc.exists ? commenterDoc.data() : { displayName: 'Someone' };

      // Get post author's FCM token
      const tokens = await getFCMTokensForUsers([post.userId]);

      if (tokens.length === 0) return;

      // Create notification payload
      const notification = {
        title: `${commenter.displayName} commented on your post`,
        body: comment.content.length > 100 ? comment.content.substring(0, 100) + '...' : comment.content
      };

      const data = {
        type: 'comment',
        postId: comment.postId,
        commentId: context.params.commentId,
        commenterId: comment.userId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      };

      // Send notification
      const response = await messaging.sendMulticast({
        tokens: tokens,
        notification: notification,
        data: data,
        android: {
          priority: 'high',
          notification: {
            channelId: 'comments',
            priority: 'high',
            defaultSound: true
          }
        },
        webpush: {
          notification: {
            icon: '/images/icon-192x192.png',
            tag: 'comment'
          }
        }
      });

      console.log('Comment notification sent:', response.successCount);

      // Store notification in database
      await storeNotificationsForUsers([post.userId], {
        title: notification.title,
        body: notification.body,
        type: 'comment',
        data: data
      });

    } catch (error) {
      console.error('Error sending comment notification:', error);
    }
  });

/**
 * Clean up old notifications (runs daily)
 */
exports.cleanupOldNotifications = functions.pubsub
  .schedule('0 2 * * *') // Run at 2 AM daily
  .timeZone('UTC')
  .onRun(async (context) => {
    try {
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const oldNotifications = await db.collection('notifications')
        .where('createdAt', '<', thirtyDaysAgo)
        .get();

      const batch = db.batch();
      oldNotifications.docs.forEach(doc => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      console.log(`Cleaned up ${oldNotifications.size} old notifications`);

    } catch (error) {
      console.error('Error cleaning up old notifications:', error);
    }
  });

/**
 * Helper function to get FCM tokens for multiple users
 */
async function getFCMTokensForUsers(userIds) {
  const tokens = [];
  
  try {
    const tokenPromises = userIds.map(userId => 
      db.collection('push_notification_requests').doc(userId).get()
    );
    
    const tokenDocs = await Promise.all(tokenPromises);
    
    tokenDocs.forEach(doc => {
      if (doc.exists) {
        const data = doc.data();
        if (data.token) {
          tokens.push(data.token);
        }
      }
    });
  } catch (error) {
    console.error('Error getting FCM tokens:', error);
  }
  
  return tokens;
}

/**
 * Helper function to store notifications in database
 */
async function storeNotificationsForUsers(userIds, notificationData) {
  try {
    const batch = db.batch();
    
    userIds.forEach(userId => {
      const notificationRef = db.collection('notifications').doc();
      batch.set(notificationRef, {
        userId: userId,
        title: notificationData.title,
        body: notificationData.body,
        type: notificationData.type,
        data: notificationData.data || {},
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });
    
    await batch.commit();
  } catch (error) {
    console.error('Error storing notifications:', error);
  }
}

/**
 * Manual notification sending endpoint (for admin use)
 */
exports.sendNotification = functions.https.onCall(async (data, context) => {
  // Check if user is authenticated and is admin
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  try {
    const { userIds, title, body, type, notificationData } = data;

    // Get FCM tokens
    const tokens = await getFCMTokensForUsers(userIds);

    if (tokens.length === 0) {
      return { success: false, message: 'No valid tokens found' };
    }

    // Send notification
    const response = await messaging.sendMulticast({
      tokens: tokens,
      notification: { title, body },
      data: {
        type: type || 'general',
        ...notificationData
      }
    });

    // Store in database
    await storeNotificationsForUsers(userIds, {
      title,
      body,
      type: type || 'general',
      data: notificationData || {}
    });

    return {
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount
    };

  } catch (error) {
    console.error('Error sending manual notification:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send notification');
  }
});