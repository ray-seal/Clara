const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Cloud Function to send push notifications when notification documents are created
 * Triggers on: /notifications/{notificationId}
 */
exports.sendNotificationOnCreate = functions.firestore
    .document('notifications/{notificationId}')
    .onCreate(async (snap, context) => {
        const notification = snap.data();
        const notificationId = context.params.notificationId;
        
        console.log('New notification created:', notificationId, notification);
        
        try {
            // Get the recipient's FCM token
            const tokenDoc = await admin.firestore()
                .collection('fcm_tokens')
                .doc(notification.userId)
                .get();
            
            if (!tokenDoc.exists) {
                console.log('No FCM token found for user:', notification.userId);
                return;
            }
            
            const fcmToken = tokenDoc.data().token;
            if (!fcmToken) {
                console.log('FCM token is empty for user:', notification.userId);
                return;
            }
            
            // Prepare the notification payload
            const payload = {
                notification: {
                    title: notification.title || 'Clara',
                    body: notification.message || 'You have a new notification',
                    icon: 'ic_notifications',
                    sound: 'default',
                    clickAction: 'FLUTTER_NOTIFICATION_CLICK'
                },
                data: {
                    type: notification.type || 'general',
                    postId: notification.relatedPostId || '',
                    userId: notification.fromUserId || '',
                    notificationId: notificationId,
                    timestamp: notification.timestamp ? notification.timestamp.toDate().getTime().toString() : ''
                }
            };
            
            // Send the notification
            const response = await admin.messaging().sendToDevice(fcmToken, payload);
            console.log('Successfully sent notification:', response);
            
            // Update notification document with sent status
            await snap.ref.update({
                pushNotificationSent: true,
                pushNotificationTimestamp: admin.firestore.FieldValue.serverTimestamp(),
                fcmResponse: {
                    successCount: response.successCount,
                    failureCount: response.failureCount
                }
            });
            
            return response;
            
        } catch (error) {
            console.error('Error sending notification:', error);
            
            // Update notification document with error status
            await snap.ref.update({
                pushNotificationSent: false,
                pushNotificationError: error.message,
                pushNotificationTimestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            
            throw error;
        }
    });

/**
 * Alternative function to process push notification requests
 * Triggers on: /push_notification_requests/{requestId}
 */
exports.processPushNotificationRequest = functions.firestore
    .document('push_notification_requests/{requestId}')
    .onCreate(async (snap, context) => {
        const request = snap.data();
        const requestId = context.params.requestId;
        
        console.log('Processing push notification request:', requestId, request);
        
        try {
            const payload = {
                notification: {
                    title: request.title || 'Clara',
                    body: request.body || request.message || 'You have a new notification',
                    icon: 'ic_notifications',
                    sound: 'default'
                },
                data: {
                    type: request.type || 'general',
                    postId: request.postId || '',
                    userId: request.userId || '',
                    requestId: requestId
                }
            };
            
            // Send to specific token
            const response = await admin.messaging().sendToDevice(request.token, payload);
            console.log('Successfully sent push notification:', response);
            
            // Update request document with result
            await snap.ref.update({
                processed: true,
                processedAt: admin.firestore.FieldValue.serverTimestamp(),
                fcmResponse: {
                    successCount: response.successCount,
                    failureCount: response.failureCount,
                    results: response.results
                }
            });
            
            return response;
            
        } catch (error) {
            console.error('Error processing push notification request:', error);
            
            // Update request document with error
            await snap.ref.update({
                processed: false,
                error: error.message,
                processedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            
            throw error;
        }
    });

/**
 * HTTPS callable function to send push notifications manually
 * Can be called from the app for immediate notifications
 */
exports.sendPushNotification = functions.https.onCall(async (data, context) => {
    // Verify authentication
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }
    
    const { recipientUserId, title, message, type, postId } = data;
    
    if (!recipientUserId || !title || !message) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
    }
    
    try {
        // Get recipient's FCM token
        const tokenDoc = await admin.firestore()
            .collection('fcm_tokens')
            .doc(recipientUserId)
            .get();
        
        if (!tokenDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'No FCM token found for user');
        }
        
        const fcmToken = tokenDoc.data().token;
        
        const payload = {
            notification: {
                title: title,
                body: message,
                icon: 'ic_notifications',
                sound: 'default'
            },
            data: {
                type: type || 'general',
                postId: postId || '',
                userId: context.auth.uid
            }
        };
        
        const response = await admin.messaging().sendToDevice(fcmToken, payload);
        
        return {
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount
        };
        
    } catch (error) {
        console.error('Error in sendPushNotification callable:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

/**
 * Cleanup function to remove old processed notification requests
 * Runs daily at midnight
 */
exports.cleanupOldNotificationRequests = functions.pubsub
    .schedule('0 0 * * *')
    .timeZone('UTC')
    .onRun(async (context) => {
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 7); // Remove requests older than 7 days
        
        const oldRequests = await admin.firestore()
            .collection('push_notification_requests')
            .where('processedAt', '<', cutoffDate)
            .get();
        
        const batch = admin.firestore().batch();
        oldRequests.docs.forEach(doc => {
            batch.delete(doc.ref);
        });
        
        await batch.commit();
        console.log(`Cleaned up ${oldRequests.size} old notification requests`);
        
        return null;
    });