// Push Notifications Manager for Clara PWA
import { messaging, db, COLLECTIONS } from './config.js';
import { authManager } from './auth.js';
import { 
  getToken, 
  onMessage 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-messaging.js';
import { 
  doc, 
  setDoc, 
  getDoc, 
  updateDoc, 
  serverTimestamp,
  collection,
  addDoc,
  query,
  where,
  orderBy,
  limit,
  getDocs
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showToast, handleError, formatDateTime } from './utils.js';

export class NotificationManager {
  constructor() {
    this.isSupported = 'serviceWorker' in navigator && 'PushManager' in window;
    this.token = null;
    this.notifications = [];
    this.unreadCount = 0;
  }

  async initialize() {
    if (!this.isSupported) {
      console.warn('Push notifications not supported');
      return false;
    }

    if (!messaging) {
      console.warn('Firebase messaging not initialized');
      return false;
    }

    try {
      // Request notification permission
      const permission = await this.requestPermission();
      if (permission !== 'granted') {
        console.warn('Notification permission not granted');
        return false;
      }

      // Get FCM token
      await this.getMessagingToken();
      
      // Set up foreground message listener
      this.setupForegroundListener();
      
      // Load existing notifications
      await this.loadNotifications();
      
      console.log('✅ Push notifications initialized successfully');
      return true;
      
    } catch (error) {
      console.error('Error initializing notifications:', error);
      return false;
    }
  }

  async requestPermission() {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      return permission;
    }
    return 'default';
  }

  async getMessagingToken() {
    try {
      const currentToken = await getToken(messaging, {
        vapidKey: 'BMxZbR3q5pGUF4Vm6wJYHXJp5RqKFJl4KhGNMk8oGF4fFfwGT2N4Jmn9N8lOqP4' // You'll need to generate this
      });

      if (currentToken) {
        this.token = currentToken;
        console.log('FCM Token:', currentToken);
        
        // Save token to user's profile
        await this.saveTokenToDatabase(currentToken);
        
        return currentToken;
      } else {
        console.log('No registration token available');
        return null;
      }
    } catch (error) {
      console.error('Error getting messaging token:', error);
      return null;
    }
  }

  async saveTokenToDatabase(token) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      const tokenData = {
        userId: userProfile.uid,
        token: token,
        platform: 'web',
        userAgent: navigator.userAgent,
        lastUpdated: serverTimestamp()
      };

      // Save to push notification tokens collection
      await setDoc(doc(db, COLLECTIONS.PUSH_NOTIFICATION_REQUESTS, userProfile.uid), tokenData);
      
      // Also update user profile with latest token
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), {
        fcmToken: token,
        lastTokenUpdate: serverTimestamp()
      });

      console.log('Token saved to database');
    } catch (error) {
      console.error('Error saving token to database:', error);
    }
  }

  setupForegroundListener() {
    // Handle messages when app is in foreground
    onMessage(messaging, (payload) => {
      console.log('Foreground message received:', payload);
      
      const { notification, data } = payload;
      
      // Show custom notification toast
      this.showNotificationToast(notification, data);
      
      // Add to notifications list
      this.addNotification({
        title: notification?.title || 'New notification',
        body: notification?.body || '',
        data: data || {},
        timestamp: new Date(),
        read: false
      });
      
      // Play notification sound
      this.playNotificationSound();
    });
  }

  showNotificationToast(notification, data) {
    const title = notification?.title || 'New notification';
    const body = notification?.body || '';
    
    // Create enhanced toast notification
    const toastHtml = `
      <div class="notification-toast" onclick="notificationManager.handleNotificationClick('${data?.type || 'general'}', '${data?.id || ''}')">
        <div class="notification-icon">
          <span class="material-icons">${this.getNotificationIcon(data?.type)}</span>
        </div>
        <div class="notification-content">
          <div class="notification-title">${title}</div>
          <div class="notification-body">${body}</div>
        </div>
        <button class="notification-close" onclick="event.stopPropagation(); this.parentElement.remove();">
          <span class="material-icons">close</span>
        </button>
      </div>
    `;
    
    // Add to toast container
    const container = document.getElementById('toast-container') || this.createToastContainer();
    const toastElement = document.createElement('div');
    toastElement.innerHTML = toastHtml;
    container.appendChild(toastElement.firstElementChild);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
      const toast = container.querySelector('.notification-toast');
      if (toast) toast.remove();
    }, 5000);
  }

  createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    container.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 10000;
      max-width: 400px;
    `;
    document.body.appendChild(container);
    return container;
  }

  getNotificationIcon(type) {
    const icons = {
      'message': 'chat',
      'friend_request': 'person_add',
      'post': 'article',
      'comment': 'comment',
      'like': 'favorite',
      'admin': 'admin_panel_settings',
      'system': 'info'
    };
    return icons[type] || 'notifications';
  }

  playNotificationSound() {
    // Create and play notification sound
    const audio = new Audio();
    audio.src = 'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj'; // Simple notification sound
    audio.volume = 0.3;
    audio.play().catch(e => console.log('Could not play notification sound:', e));
  }

  handleNotificationClick(type, id) {
    console.log('Notification clicked:', type, id);
    
    // Handle different notification types
    switch (type) {
      case 'message':
        // Navigate to chat
        if (window.claraApp) {
          window.claraApp.navigateToPage('chat');
        }
        break;
      case 'friend_request':
        // Navigate to friends
        if (window.claraApp) {
          window.claraApp.navigateToPage('friends');
        }
        break;
      case 'post':
      case 'comment':
        // Navigate to feed
        if (window.claraApp) {
          window.claraApp.navigateToPage('feed');
        }
        break;
      default:
        // General notification
        showToast('Notification opened', 'info');
    }
  }

  addNotification(notification) {
    this.notifications.unshift(notification);
    if (!notification.read) {
      this.unreadCount++;
    }
    this.updateNotificationBadge();
    this.saveNotificationToDatabase(notification);
  }

  updateNotificationBadge() {
    // Update notification badge in UI
    const badge = document.querySelector('.notification-badge');
    if (badge) {
      if (this.unreadCount > 0) {
        badge.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount.toString();
        badge.style.display = 'block';
      } else {
        badge.style.display = 'none';
      }
    }
  }

  async saveNotificationToDatabase(notification) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      await addDoc(collection(db, COLLECTIONS.NOTIFICATIONS), {
        userId: userProfile.uid,
        title: notification.title,
        body: notification.body,
        data: notification.data,
        read: notification.read,
        createdAt: serverTimestamp()
      });
    } catch (error) {
      console.error('Error saving notification to database:', error);
    }
  }

  async loadNotifications() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      const notificationsQuery = query(
        collection(db, COLLECTIONS.NOTIFICATIONS),
        where('userId', '==', userProfile.uid),
        orderBy('createdAt', 'desc'),
        limit(50)
      );

      const snapshot = await getDocs(notificationsQuery);
      this.notifications = [];
      this.unreadCount = 0;

      snapshot.forEach(doc => {
        const data = doc.data();
        const notification = {
          id: doc.id,
          title: data.title,
          body: data.body,
          data: data.data || {},
          read: data.read,
          timestamp: data.createdAt?.toDate() || new Date()
        };
        
        this.notifications.push(notification);
        if (!notification.read) {
          this.unreadCount++;
        }
      });

      this.updateNotificationBadge();
      this.renderNotifications();
      
    } catch (error) {
      console.error('Error loading notifications:', error);
    }
  }

  renderNotifications() {
    const container = document.getElementById('notifications-list');
    if (!container) return;

    if (this.notifications.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <span class="material-icons">notifications_none</span>
          <h3>No notifications</h3>
          <p>You're all caught up!</p>
        </div>
      `;
      return;
    }

    const notificationsHtml = this.notifications.map(notification => `
      <div class="notification-item ${notification.read ? 'read' : 'unread'}" onclick="notificationManager.markAsRead('${notification.id}')">
        <div class="notification-icon">
          <span class="material-icons">${this.getNotificationIcon(notification.data.type)}</span>
        </div>
        <div class="notification-content">
          <div class="notification-title">${notification.title}</div>
          <div class="notification-body">${notification.body}</div>
          <div class="notification-time">${formatDateTime(notification.timestamp)}</div>
        </div>
        ${!notification.read ? '<div class="unread-indicator"></div>' : ''}
      </div>
    `).join('');

    container.innerHTML = notificationsHtml;
  }

  async markAsRead(notificationId) {
    try {
      // Update in database
      await updateDoc(doc(db, COLLECTIONS.NOTIFICATIONS, notificationId), {
        read: true
      });

      // Update local state
      const notification = this.notifications.find(n => n.id === notificationId);
      if (notification && !notification.read) {
        notification.read = true;
        this.unreadCount--;
        this.updateNotificationBadge();
        this.renderNotifications();
      }
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  }

  async markAllAsRead() {
    const unreadNotifications = this.notifications.filter(n => !n.read);
    
    try {
      // Update all unread notifications in database
      const updatePromises = unreadNotifications.map(notification =>
        updateDoc(doc(db, COLLECTIONS.NOTIFICATIONS, notification.id), { read: true })
      );
      
      await Promise.all(updatePromises);
      
      // Update local state
      this.notifications.forEach(n => n.read = true);
      this.unreadCount = 0;
      this.updateNotificationBadge();
      this.renderNotifications();
      
      showToast('All notifications marked as read', 'success');
    } catch (error) {
      handleError(error, 'Marking notifications as read');
    }
  }

  // Server-side notification sending (for backend integration)
  static async sendNotificationToUser(userId, notification) {
    try {
      // This would typically be called from your backend/cloud functions
      // Add to pending notifications collection for processing
      await addDoc(collection(db, 'pendingNotifications'), {
        userId,
        title: notification.title,
        body: notification.body,
        data: notification.data || {},
        createdAt: serverTimestamp(),
        processed: false
      });
      
      console.log('Notification queued for sending');
    } catch (error) {
      console.error('Error queueing notification:', error);
    }
  }

  // Test notification function
  async sendTestNotification() {
    if (!this.token) {
      showToast('Push notifications not enabled', 'error');
      return;
    }

    // Simulate a test notification
    const testNotification = {
      title: 'Test Notification',
      body: 'This is a test notification from Clara PWA',
      data: { type: 'system', id: 'test' }
    };

    this.showNotificationToast(testNotification, testNotification.data);
    this.addNotification({
      ...testNotification,
      timestamp: new Date(),
      read: false
    });

    showToast('Test notification sent!', 'success');
  }
}

// Create global instance
window.notificationManager = new NotificationManager();

// Auto-initialize when user is authenticated
document.addEventListener('authStateChanged', (event) => {
  if (event.detail.isSignedIn) {
    window.notificationManager.initialize();
  }
});

// CSS for notifications (to be added to main CSS)
const notificationCSS = `
.notification-toast {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 16px;
  margin-bottom: 8px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 4px solid var(--primary-color);
  max-width: 400px;
}

.notification-toast:hover {
  transform: translateX(-4px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.2);
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon .material-icons {
  color: var(--primary-color);
  font-size: 20px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.notification-body {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.notification-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-close:hover {
  background: #f5f5f5;
}

.notification-close .material-icons {
  font-size: 16px;
  color: var(--text-secondary);
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  transition: background-color 0.2s ease;
  position: relative;
}

.notification-item:hover {
  background-color: #f8f9fa;
}

.notification-item.unread {
  background-color: #f0f7ff;
}

.notification-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--primary-color);
}

.notification-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.unread-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  flex-shrink: 0;
  margin-top: 6px;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #f44336;
  color: white;
  border-radius: 12px;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 500;
  min-width: 16px;
  text-align: center;
  display: none;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}

.empty-state .material-icons {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  margin-bottom: 8px;
  color: var(--text-primary);
}
`;

// Add CSS to document
const style = document.createElement('style');
style.textContent = notificationCSS;
document.head.appendChild(style);