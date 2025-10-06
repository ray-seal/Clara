// Main App Controller for Clara PWA
import { authManager } from './auth.js';
import { FeedManager } from './feed.js';
import { InteractionManager } from './interactions.js';
import { FriendsManager } from './friends.js';
import { ProfileManager } from './profile.js';
import { NotificationManager } from './notifications.js';
import { showToast, showModal, hideModal } from './utils.js';

class ClaraApp {
  constructor() {
    this.currentTab = 'feed';
    this.managers = {};
    this.initialized = false;
  }

  async initialize() {
    if (this.initialized) return;

    try {
      // Show loading screen
      this.showLoadingScreen();

      // Initialize authentication
      await authManager.initialize();

      // Setup auth state listener
      authManager.onAuthStateChange((user, profile) => {
        if (user && profile) {
          this.showMainApp();
          this.initializeManagers();
        } else {
          this.showAuthScreen();
        }
      });

      // Setup event listeners
      this.setupEventListeners();

      // Initialize PWA features
      this.initializePWA();

      this.initialized = true;

    } catch (error) {
      console.error('Error initializing app:', error);
      showToast('Failed to initialize app', 'error');
    }
  }

  async initializeManagers() {
    try {
      // Initialize managers
      this.managers.feed = new FeedManager();
      this.managers.interaction = new InteractionManager();
      this.managers.friends = new FriendsManager();
      this.managers.profile = new ProfileManager();
      this.managers.notifications = new NotificationManager();

      // Make managers globally available
      window.feedManager = this.managers.feed;
      window.interactionManager = this.managers.interaction;
      window.friendsManager = this.managers.friends;
      window.profileManager = this.managers.profile;
      window.notificationManager = this.managers.notifications;

      // Initialize active tab
      await this.managers.feed.initialize();
      
      // Initialize notifications
      await this.managers.notifications.initialize();

    } catch (error) {
      console.error('Error initializing managers:', error);
      showToast('Some features may not work properly', 'warning');
    }
  }

  setupEventListeners() {
    // Auth form listeners
    this.setupAuthListeners();

    // Navigation listeners
    this.setupNavigationListeners();

    // PWA listeners
    this.setupPWAListeners();

    // Profile menu listener
    document.getElementById('profile-menu-btn')?.addEventListener('click', () => {
      this.showProfileMenu();
    });

    // Notifications button
    document.getElementById('notifications-btn')?.addEventListener('click', () => {
      this.showNotifications();
    });
  }

  setupAuthListeners() {
    // Sign in form
    document.getElementById('signin-form-element')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('signin-email').value;
      const password = document.getElementById('signin-password').value;
      await authManager.signIn(email, password);
    });

    // Sign up form
    document.getElementById('signup-form-element')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('signup-name').value;
      const email = document.getElementById('signup-email').value;
      const password = document.getElementById('signup-password').value;
      const confirm = document.getElementById('signup-confirm').value;

      if (password !== confirm) {
        showToast('Passwords do not match', 'error');
        return;
      }

      await authManager.signUp(email, password, name);
    });

    // Reset password form
    document.getElementById('reset-form-element')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('reset-email').value;
      await authManager.resetPassword(email);
    });

    // Form switchers
    document.getElementById('show-signup')?.addEventListener('click', (e) => {
      e.preventDefault();
      this.showSignUpForm();
    });

    document.getElementById('show-signin')?.addEventListener('click', (e) => {
      e.preventDefault();
      this.showSignInForm();
    });

    document.getElementById('forgot-password-link')?.addEventListener('click', (e) => {
      e.preventDefault();
      this.showResetForm();
    });

    document.getElementById('back-to-signin')?.addEventListener('click', (e) => {
      e.preventDefault();
      this.showSignInForm();
    });
  }

  setupNavigationListeners() {
    const navButtons = document.querySelectorAll('.nav-btn[data-tab]');
    navButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const tab = btn.dataset.tab;
        this.switchTab(tab);
      });
    });
  }

  setupPWAListeners() {
    // PWA install prompt
    let deferredPrompt;
    
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      deferredPrompt = e;
      this.showInstallBanner();
    });

    document.getElementById('install-btn')?.addEventListener('click', async () => {
      if (deferredPrompt) {
        deferredPrompt.prompt();
        const { outcome } = await deferredPrompt.userChoice;
        if (outcome === 'accepted') {
          this.hideInstallBanner();
        }
        deferredPrompt = null;
      }
    });

    document.getElementById('install-dismiss')?.addEventListener('click', () => {
      this.hideInstallBanner();
    });

    // Notification permission banner
    document.getElementById('enable-notifications')?.addEventListener('click', async () => {
      if (this.managers.notifications) {
        await this.managers.notifications.requestPermission();
      }
      this.hideNotificationBanner();
    });

    document.getElementById('dismiss-notifications')?.addEventListener('click', () => {
      this.hideNotificationBanner();
    });
  }

  async switchTab(tabName) {
    if (this.currentTab === tabName) return;

    // Update navigation
    document.querySelectorAll('.nav-btn').forEach(btn => {
      btn.classList.remove('active');
    });
    document.querySelector(`[data-tab="${tabName}"]`)?.classList.add('active');

    // Update content
    document.querySelectorAll('.tab-content').forEach(content => {
      content.classList.remove('active');
    });
    document.getElementById(`${tabName}-tab`)?.classList.add('active');

    this.currentTab = tabName;

    // Initialize tab content if needed
    switch (tabName) {
      case 'feed':
        if (this.managers.feed) {
          await this.managers.feed.loadFeed();
        }
        break;
      case 'friends':
        if (this.managers.friends) {
          await this.managers.friends.initialize();
        }
        break;
      case 'profile':
        if (this.managers.profile) {
          await this.managers.profile.initialize();
        }
        break;
      case 'chat':
        // Chat will be implemented later
        document.getElementById('chat-container').innerHTML = `
          <div class="coming-soon">
            <span class="material-icons">chat</span>
            <h3>Chat Coming Soon</h3>
            <p>Group chat feature is under development</p>
          </div>
        `;
        break;
    }
  }

  showLoadingScreen() {
    document.getElementById('loading-screen').style.display = 'flex';
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('main-app').style.display = 'none';
  }

  showAuthScreen() {
    document.getElementById('loading-screen').style.display = 'none';
    document.getElementById('auth-screen').style.display = 'flex';
    document.getElementById('main-app').style.display = 'none';
  }

  showMainApp() {
    document.getElementById('loading-screen').style.display = 'none';
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('main-app').style.display = 'flex';

    // Show notification permission banner if not granted
    setTimeout(() => {
      if (Notification.permission === 'default') {
        this.showNotificationBanner();
      }
    }, 2000);
  }

  showSignInForm() {
    document.getElementById('signin-form').style.display = 'block';
    document.getElementById('signup-form').style.display = 'none';
    document.getElementById('reset-form').style.display = 'none';
  }

  showSignUpForm() {
    document.getElementById('signin-form').style.display = 'none';
    document.getElementById('signup-form').style.display = 'block';
    document.getElementById('reset-form').style.display = 'none';
  }

  showResetForm() {
    document.getElementById('signin-form').style.display = 'none';
    document.getElementById('signup-form').style.display = 'none';
    document.getElementById('reset-form').style.display = 'block';
  }

  showInstallBanner() {
    document.getElementById('install-banner').style.display = 'block';
  }

  hideInstallBanner() {
    document.getElementById('install-banner').style.display = 'none';
  }

  showNotificationBanner() {
    document.getElementById('notification-permission-banner').style.display = 'block';
  }

  hideNotificationBanner() {
    document.getElementById('notification-permission-banner').style.display = 'none';
  }

  showProfileMenu() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const modalContent = `
      <div class="modal-header">
        <h2>Profile Menu</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="profile-menu">
          <div class="profile-menu-header">
            <div class="profile-menu-avatar">
              ${userProfile.profilePictureUrl ? 
                `<img src="${userProfile.profilePictureUrl}" alt="${userProfile.displayName}">` :
                `<span class="material-icons">person</span>`
              }
            </div>
            <div class="profile-menu-info">
              <h3>${userProfile.displayName}</h3>
              <p>${userProfile.email}</p>
              ${userProfile.isAdmin ? '<span class="admin-badge">Admin</span>' : ''}
            </div>
          </div>
          
          <div class="profile-menu-actions">
            <button class="menu-item" onclick="app.switchTab('profile'); hideModal();">
              <span class="material-icons">person</span>
              <span>View Profile</span>
            </button>
            
            <button class="menu-item" onclick="app.showSettings()">
              <span class="material-icons">settings</span>
              <span>Settings</span>
            </button>
            
            ${userProfile.isAdmin ? `
              <button class="menu-item" onclick="app.showAdminPanel()">
                <span class="material-icons">admin_panel_settings</span>
                <span>Admin Panel</span>
              </button>
            ` : ''}
            
            <button class="menu-item danger" onclick="app.signOut()">
              <span class="material-icons">logout</span>
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </div>
    `;

    showModal(modalContent);
  }

  showNotifications() {
    if (this.managers.notifications) {
      this.managers.notifications.showNotificationsModal();
    }
  }

  showSettings() {
    // Settings will be implemented later
    hideModal();
    showToast('Settings coming soon', 'info');
  }

  showAdminPanel() {
    // Admin panel will be implemented later
    hideModal();
    showToast('Admin panel coming soon', 'info');
  }

  async signOut() {
    hideModal();
    await authManager.signOut();
  }

  initializePWA() {
    // Handle online/offline status
    window.addEventListener('online', () => {
      showToast('Connection restored', 'success');
    });

    window.addEventListener('offline', () => {
      showToast('You are offline', 'warning');
    });

    // Handle app updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        showToast('App updated! Refresh to see changes.', 'info', 5000);
      });
    }
  }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.app = new ClaraApp();
  window.app.initialize();
});

// Handle app visibility changes
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && window.app && window.app.managers.notifications) {
    // App became visible, mark notifications as read
    window.app.managers.notifications.markAllAsRead();
  }
});