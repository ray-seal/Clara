// Firebase Cloud Messaging Service Worker
// This file handles background push notifications when the app is not active

importScripts('https://www.gstatic.com/firebasejs/10.3.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.3.0/firebase-messaging-compat.js');

// Firebase configuration (same as your main app)
const firebaseConfig = {
  apiKey: "AIzaSyBcWWjr4e3jbRTSs0jsoCEyjX35P2CcxNA",
  authDomain: "supportapp-9df04.firebaseapp.com",
  projectId: "supportapp-9df04", 
  storageBucket: "supportapp-9df04.firebasestorage.app",
  messagingSenderId: "825301739515",
  appId: "1:825301739515:web:38823db2c355d7507b4d5e"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Initialize Firebase Cloud Messaging
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('Background message received:', payload);

  const { notification, data } = payload;
  
  // Customize notification
  const notificationTitle = notification?.title || 'Clara - New notification';
  const notificationOptions = {
    body: notification?.body || 'You have a new notification',
    icon: '/images/icon-192x192.png',
    badge: '/images/badge-72x72.png',
    tag: data?.type || 'general',
    data: data || {},
    actions: [
      {
        action: 'open',
        title: 'Open App',
        icon: '/images/icon-open.png'
      },
      {
        action: 'dismiss',
        title: 'Dismiss',
        icon: '/images/icon-close.png'
      }
    ],
    requireInteraction: true, // Keep notification until user interacts
    silent: false,
    timestamp: Date.now(),
    vibrate: [200, 100, 200], // Vibration pattern
    renotify: true // Allow multiple notifications with same tag
  };

  // Show notification
  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
  console.log('Notification clicked:', event);
  
  const notification = event.notification;
  const action = event.action;
  const data = notification.data || {};

  // Close the notification
  notification.close();

  if (action === 'dismiss') {
    // User dismissed notification, do nothing
    return;
  }

  // Handle notification click (open app)
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        // Check if app is already open
        for (const client of clientList) {
          if (client.url.includes(self.location.origin)) {
            // Focus existing window and navigate if needed
            client.focus();
            
            // Send message to open app about notification click
            client.postMessage({
              type: 'NOTIFICATION_CLICKED',
              notificationType: data.type,
              notificationId: data.id,
              data: data
            });
            
            return;
          }
        }
        
        // Open new window if app is not open
        const urlToOpen = self.location.origin + getUrlForNotificationType(data.type, data.id);
        return clients.openWindow(urlToOpen);
      })
  );
});

// Helper function to determine URL based on notification type
function getUrlForNotificationType(type, id) {
  switch (type) {
    case 'message':
      return '/#chat';
    case 'friend_request':
      return '/#friends';
    case 'post':
    case 'comment':
      return id ? `/#feed?post=${id}` : '/#feed';
    case 'admin':
      return '/#admin';
    default:
      return '/';
  }
}

// Handle notification close
self.addEventListener('notificationclose', (event) => {
  console.log('Notification closed:', event);
  
  // Track notification dismissal (optional)
  const data = event.notification.data || {};
  
  // You could send analytics about dismissed notifications here
  if (data.trackDismissal) {
    // Send to analytics service
  }
});

// Install event
self.addEventListener('install', (event) => {
  console.log('FCM Service Worker installed');
  self.skipWaiting();
});

// Activate event
self.addEventListener('activate', (event) => {
  console.log('FCM Service Worker activated');
  event.waitUntil(self.clients.claim());
});

// Handle messages from main app
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});