const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Simple HTTP endpoint to send push notifications
 * POST /sendNotification
 * Body: { userId, title, message, type, postId }
 */
exports.sendNotification = functions.https.onRequest(async (req, res) => {
    // Set CORS headers
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    
    if (req.method !== 'POST') {
        res.status(405).send('Method Not Allowed');
        return;
    }
    
    const { userId, title, message, type, postId, authToken } = req.body;
    
    // Basic validation
    if (!userId || !title || !message) {
        res.status(400).json({ error: 'Missing required fields: userId, title, message' });
        return;
    }
    
    try {
        // Optional: Verify the auth token if needed
        // const decodedToken = await admin.auth().verifyIdToken(authToken);
        
        // Get FCM token for user
        const tokenDoc = await admin.firestore()
            .collection('fcm_tokens')
            .doc(userId)
            .get();
        
        if (!tokenDoc.exists) {
            res.status(404).json({ error: 'No FCM token found for user' });
            return;
        }
        
        const fcmToken = tokenDoc.data().token;
        
        if (!fcmToken) {
            res.status(404).json({ error: 'FCM token is empty' });
            return;
        }
        
        // Prepare notification payload
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
                timestamp: Date.now().toString()
            }
        };
        
        // Send notification
        const response = await admin.messaging().sendToDevice(fcmToken, payload);
        
        console.log('Push notification sent:', {
            userId,
            title,
            successCount: response.successCount,
            failureCount: response.failureCount
        });
        
        res.status(200).json({
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount,
            response: response.results
        });
        
    } catch (error) {
        console.error('Error sending push notification:', error);
        res.status(500).json({ error: error.message });
    }
});

module.exports = { sendNotification };