// Main App Controller for Clara Mental Health Support PWA
import { authManager } from './auth.js';
import { FeedManager } from './feed.js';
import { InteractionManager } from './interactions.js';
import { supportGroupsManager } from './support-groups.js';
import { crisisResourcesManager } from './crisis-resources.js';
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
      // Initialize mental health managers
      this.managers.feed = new FeedManager();
      this.managers.interaction = new InteractionManager();
      this.managers.supportGroups = supportGroupsManager;
      this.managers.crisisResources = crisisResourcesManager;
      this.managers.profile = new ProfileManager();
      this.managers.notifications = new NotificationManager();

      // Initialize mental health modules
      await this.managers.supportGroups.initialize();
      await this.managers.crisisResources.initialize();

      // Make managers globally available
      window.feedManager = this.managers.feed;
      window.interactionManager = this.managers.interaction;
      window.supportGroupsManager = this.managers.supportGroups;
      window.crisisResourcesManager = this.managers.crisisResources;
      window.profileManager = this.managers.profile;
      window.notificationManager = this.managers.notifications;

      // Show crisis resources button
      this.managers.crisisResources.showQuickResourcesButton();

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

    // Anonymous sign-in buttons
    document.getElementById('anonymous-signin-btn')?.addEventListener('click', async () => {
      await authManager.signInAnonymously();
    });

    document.getElementById('anonymous-signup-btn')?.addEventListener('click', async () => {
      await authManager.signInAnonymously();
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
      case 'support-groups':
        if (this.managers.supportGroups) {
          await this.loadSupportGroupsTab();
        }
        break;
      case 'wellness':
        await this.loadWellnessTab();
        break;
      case 'profile':
        if (this.managers.profile) {
          await this.managers.profile.initialize();
        }
        break;
    }
  }

  async loadSupportGroupsTab() {
    const container = document.getElementById('support-groups-container');
    if (!container) return;

    container.innerHTML = `
      <div class="support-groups-header">
        <h2>Support Groups</h2>
        <p>Connect with others who understand your journey</p>
      </div>
      <div class="support-groups-grid" id="support-groups-grid">
        <div class="loading-message">Loading support groups...</div>
      </div>
    `;

    try {
      const groups = await this.managers.supportGroups.getSupportGroups();
      const joinedGroups = this.managers.supportGroups.getJoinedGroups();
      
      document.getElementById('support-groups-grid').innerHTML = groups.map(group => `
        <div class="support-group-card" style="border-left: 4px solid ${group.color}">
          <div class="group-header">
            <span class="group-icon">${group.icon}</span>
            <h3>${group.name}</h3>
          </div>
          <p class="group-description">${group.description}</p>
          <div class="group-stats">
            <span>👥 ${group.memberCount || 0} members</span>
            <span>💬 ${group.messageCount || 0} messages</span>
          </div>
          <div class="group-actions">
            ${joinedGroups.includes(group.id) 
              ? `<button class="btn btn-secondary" onclick="supportGroupsManager.leaveSupportGroup('${group.id}')">Leave Group</button>
                 <button class="btn btn-primary" onclick="app.openSupportGroupChat('${group.id}')">Open Chat</button>`
              : `<button class="btn btn-primary" onclick="supportGroupsManager.joinSupportGroup('${group.id}')">Join Group</button>`
            }
          </div>
        </div>
      `).join('');
    } catch (error) {
      console.error('Error loading support groups:', error);
      document.getElementById('support-groups-grid').innerHTML = `
        <div class="error-message">
          <p>Unable to load support groups. Please try again later.</p>
          <button class="btn btn-primary" onclick="app.loadSupportGroupsTab()">Retry</button>
        </div>
      `;
    }
  }

  async loadWellnessTab() {
    const container = document.getElementById('wellness-container');
    if (!container) return;

    const user = authManager.getCurrentUser();
    const userProfile = authManager.getUserProfile();

    container.innerHTML = `
      <div class="wellness-header">
        <h2>Mental Wellness</h2>
        <p>Tools and resources for your mental health journey</p>
      </div>
      
      <div class="wellness-tools">
        <div class="wellness-card crisis-resources">
          <h3>🆘 Crisis Resources</h3>
          <p>Immediate help when you need it most</p>
          <button class="btn btn-primary" onclick="crisisResourcesManager.showCrisisModal()">
            View Crisis Resources
          </button>
        </div>

        <div class="wellness-card mood-check">
          <h3>💙 Wellness Check-in</h3>
          <p>How are you feeling today?</p>
          <button class="btn btn-secondary" onclick="crisisResourcesManager.showWellnessCheck()">
            Check In
          </button>
        </div>

        <div class="wellness-card meditation">
          <h3>🧘 Mindfulness & Meditation</h3>
          <p>Guided exercises for relaxation</p>
          <button class="btn btn-secondary" onclick="app.showMeditationResources()">
            Start Session
          </button>
        </div>

        <div class="wellness-card self-care">
          <h3>🌱 Self-Care Tips</h3>
          <p>Daily practices for mental wellness</p>
          <button class="btn btn-secondary" onclick="app.showSelfCareResources()">
            Explore Tips
          </button>
        </div>
      </div>

      ${user && !authManager.isAnonymous() ? `
        <div class="wellness-settings">
          <h3>Privacy & Safety Settings</h3>
          <div class="setting-item">
            <label>
              <input type="checkbox" ${userProfile?.wellnessCheckConsent ? 'checked' : ''} 
                     onchange="app.updateWellnessSettings(this.checked)">
              Enable weekly wellness check reminders
            </label>
          </div>
          <div class="setting-item">
            <label>
              <input type="checkbox" ${userProfile?.crisisContactEnabled ? 'checked' : ''} 
                     onchange="app.updateCrisisSettings(this.checked)">
              Allow crisis intervention contacts
            </label>
          </div>
        </div>
      ` : ''}
    `;
  }

  async openSupportGroupChat(groupId) {
    // This will open a modal or navigate to the chat interface
    showModal('support-group-chat', `
      <div class="chat-interface">
        <div class="chat-header">
          <h3>Support Group Chat</h3>
          <button onclick="hideModal()" class="close-btn">×</button>
        </div>
        <div id="chat-messages" class="chat-messages">
          <div class="loading-message">Loading messages...</div>
        </div>
        <div class="chat-input-area">
          <div class="content-warning-toggle">
            <label>
              <input type="checkbox" id="content-warning-check">
              Add content warning
            </label>
          </div>
          <div class="chat-input-container">
            <textarea id="chat-message-input" placeholder="Share your thoughts... (be kind and supportive)" rows="3"></textarea>
            <button onclick="app.sendSupportGroupMessage('${groupId}')" class="btn btn-primary">Send</button>
          </div>
        </div>
      </div>
    `);

    // Load messages
    this.managers.supportGroups.listenToMessages(groupId, (messages) => {
      const container = document.getElementById('chat-messages');
      if (container) {
        container.innerHTML = messages.map(message => `
          <div class="message ${message.type}">
            <div class="message-header">
              <strong>${message.senderName}</strong>
              <span class="message-time">${this.formatMessageTime(message.timestamp)}</span>
            </div>
            ${message.contentWarning ? `<div class="content-warning">⚠️ ${message.contentWarning}</div>` : ''}
            <div class="message-content">${message.content}</div>
            ${message.supportive ? '<span class="supportive-badge">💙 Supportive</span>' : ''}
          </div>
        `).join('');
        container.scrollTop = container.scrollHeight;
      }
    });
  }

  async sendSupportGroupMessage(groupId) {
    const input = document.getElementById('chat-message-input');
    const warningCheck = document.getElementById('content-warning-check');
    
    if (!input || !input.value.trim()) return;

    const content = input.value.trim();
    const contentWarning = warningCheck?.checked ? 'Sensitive content' : null;

    const success = await this.managers.supportGroups.sendMessage(groupId, content, contentWarning);
    
    if (success) {
      input.value = '';
      if (warningCheck) warningCheck.checked = false;
    }
  }

  formatMessageTime(timestamp) {
    if (!timestamp) return '';
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  showMeditationResources() {
    showModal('meditation-resources', `
      <div class="meditation-modal">
        <h3>🧘 Mindfulness & Meditation</h3>
        <div class="meditation-options">
          <button class="btn btn-primary" onclick="app.startBreathingExercise()">
            🫁 Breathing Exercise (5 min)
          </button>
          <button class="btn btn-secondary" onclick="app.startBodyScan()">
            🧘‍♀️ Body Scan Meditation (10 min)
          </button>
          <button class="btn btn-secondary" onclick="app.startLovingKindness()">
            💝 Loving-Kindness Meditation (8 min)
          </button>
        </div>
        <button onclick="hideModal()" class="btn btn-outline">Close</button>
      </div>
    `);
  }

  showSelfCareResources() {
    showModal('self-care-resources', `
      <div class="self-care-modal">
        <h3>🌱 Self-Care Tips</h3>
        <div class="self-care-tips">
          <div class="tip-card">
            <h4>🌅 Morning Routine</h4>
            <p>Start your day with intention: hydrate, stretch, and set a positive affirmation.</p>
          </div>
          <div class="tip-card">
            <h4>🌙 Evening Wind-down</h4>
            <p>Create a calming bedtime routine: no screens 1 hour before bed, gentle stretching, or journaling.</p>
          </div>
          <div class="tip-card">
            <h4>🎯 Boundary Setting</h4>
            <p>It's okay to say no. Protect your energy by setting healthy boundaries with others.</p>
          </div>
          <div class="tip-card">
            <h4>🌿 Nature Connection</h4>
            <p>Spend time outdoors daily, even if it's just 5 minutes in sunlight or fresh air.</p>
          </div>
        </div>
        <button onclick="hideModal()" class="btn btn-outline">Close</button>
      </div>
    `);
  }

  async updateWellnessSettings(enabled) {
    // Update user profile with wellness check consent
    if (authManager.isSignedIn() && !authManager.isAnonymous()) {
      // Implementation would update Firestore profile
      showToast(enabled ? 'Wellness reminders enabled' : 'Wellness reminders disabled', 'success');
    }
  }

  async updateCrisisSettings(enabled) {
    // Update user profile with crisis contact consent
    if (authManager.isSignedIn() && !authManager.isAnonymous()) {
      // Implementation would update Firestore profile
      showToast(enabled ? 'Crisis contacts enabled' : 'Crisis contacts disabled', 'success');
    }
  }
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