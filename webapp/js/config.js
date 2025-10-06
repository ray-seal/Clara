// Configuration for Clara PWA
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-app.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-auth.js';
import { getStorage } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-storage.js';
import { getMessaging } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-messaging.js';

// Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyCbg4dsZvnQFdN1P9n1R5j5OHl4_Jnml9k",
  authDomain: "supportapp-96b9b.firebaseapp.com",
  projectId: "supportapp-96b9b",
  storageBucket: "supportapp-96b9b.appspot.com",
  messagingSenderId: "1047319629998",
  appId: "1:1047319629998:web:your-web-app-id",
  measurementId: "G-XXXXXXXXXX"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase services
export const db = getFirestore(app);
export const auth = getAuth(app);
export const storage = getStorage(app);

// Initialize messaging if supported
let messaging = null;
try {
  if ('serviceWorker' in navigator && 'PushManager' in window) {
    messaging = getMessaging(app);
  }
} catch (error) {
  console.log('Messaging not supported in this environment');
}
export { messaging };

// Collection names
export const COLLECTIONS = {
  PROFILES: 'profiles',
  POSTS: 'posts',
  COMMENTS: 'comments',
  FRIENDS: 'friends',
  FRIEND_REQUESTS: 'friendRequests',
  CHAT_ROOMS: 'chatRooms',
  MESSAGES: 'messages',
  NOTIFICATIONS: 'notifications',
  REPORTS: 'reports',
  FCM_TOKENS: 'fcmTokens'
};

// App configuration
export const APP_CONFIG = {
  APP_NAME: 'Clara',
  VERSION: '1.0.0',
  SUPPORT_EMAIL: 'support@clara-app.com',
  MAX_IMAGE_SIZE: 5 * 1024 * 1024, // 5MB
  POSTS_PER_PAGE: 10,
  MESSAGES_PER_PAGE: 50
};

export default app;