// Configuration for Clara PWA
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-app.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-auth.js';
import { getStorage } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-storage.js';
import { getMessaging } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-messaging.js';

// Firebase configuration - Updated with real Clara project
const firebaseConfig = {
  apiKey: "AIzaSyBcWWjr4e3jbRTSs0jsoCEyjX35P2CcxNA",
  authDomain: "supportapp-9df04.firebaseapp.com",
  projectId: "supportapp-9df04",
  storageBucket: "supportapp-9df04.firebasestorage.app",
  messagingSenderId: "825301739515",
  appId: "1:825301739515:web:6f6eaf0365169c6f7b4d5e",
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

// Collection names for Clara Mental Health Support App
export const COLLECTIONS = {
  PROFILES: 'profiles',
  POSTS: 'posts',
  COMMENTS: 'comments',
  FRIENDS: 'connections', // Changed to 'connections' for mental health context
  FRIEND_REQUESTS: 'connectionRequests',
  CHAT_ROOMS: 'supportGroups', // Changed to support groups
  MESSAGES: 'messages',
  NOTIFICATIONS: 'notifications',
  REPORTS: 'reports',
  FCM_TOKENS: 'fcmTokens',
  CRISIS_RESOURCES: 'crisisResources',
  WELLNESS_CHECKS: 'wellnessChecks',
  MOOD_TRACKING: 'moodTracking'
};

// App configuration for Clara Mental Health Support
export const APP_CONFIG = {
  APP_NAME: 'Clara',
  VERSION: '1.0.0',
  SUPPORT_EMAIL: 'support@clara-app.com',
  CRISIS_HOTLINE: '988', // National Suicide Prevention Lifeline
  MAX_IMAGE_SIZE: 5 * 1024 * 1024, // 5MB
  POSTS_PER_PAGE: 10,
  MESSAGES_PER_PAGE: 50,
  // Mental health specific settings
  ANONYMOUS_POSTING: true,
  CONTENT_MODERATION: true,
  CRISIS_KEYWORDS: ['suicide', 'kill myself', 'end it all', 'hurt myself', 'self harm'],
  WELLNESS_CHECK_INTERVAL: 7 * 24 * 60 * 60 * 1000, // 7 days in milliseconds
  SUPPORT_GROUPS: [
    {
      id: 'anxiety-support',
      name: 'Anxiety Support',
      description: 'A safe space to discuss anxiety, panic attacks, and coping strategies',
      icon: '💙',
      color: '#4A90E2'
    },
    {
      id: 'depression-support',
      name: 'Depression Support', 
      description: 'Connect with others who understand depression and share hope',
      icon: '💜',
      color: '#8E44AD'
    },
    {
      id: 'gender-dysphoria',
      name: 'Gender Identity Support',
      description: 'Support for gender dysphoria, transition, and identity questions',
      icon: '🏳️‍⚧️',
      color: '#FF69B4'
    },
    {
      id: 'ptsd-trauma',
      name: 'PTSD & Trauma Recovery',
      description: 'Healing together from trauma and PTSD experiences',
      icon: '🌱',
      color: '#27AE60'
    },
    {
      id: 'addiction-recovery',
      name: 'Addiction Recovery',
      description: 'Support for addiction recovery and sobriety journey',
      icon: '💪',
      color: '#E67E22'
    },
    {
      id: 'eating-disorders',
      name: 'Eating Disorder Support',
      description: 'Recovery support for eating disorders and body image issues',
      icon: '🌸',
      color: '#F39C12'
    },
    {
      id: 'bipolar-support',
      name: 'Bipolar Support',
      description: 'Managing bipolar disorder with peer support and understanding',
      icon: '⚖️',
      color: '#9B59B6'
    },
    {
      id: 'general-wellness',
      name: 'General Mental Wellness',
      description: 'Overall mental health discussions and wellness tips',
      icon: '🧠',
      color: '#3498DB'
    }
  ]
};

export default app;