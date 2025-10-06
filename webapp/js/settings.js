// Settings Manager for Clara PWA
import { db, COLLECTIONS } from './config.js';
import { authManager } from './auth.js';
import { 
  doc, 
  getDoc, 
  updateDoc,
  serverTimestamp,
  deleteDoc 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showModal, hideModal, showToast, handleError } from './utils.js';

export class SettingsManager {
  constructor() {
    this.userSettings = null;
  }

  async loadSettings(userId) {
    try {
      const userDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, userId));
      if (userDoc.exists()) {
        this.userSettings = userDoc.data();
        return this.userSettings;
      }
    } catch (error) {
      handleError(error, 'Loading settings');
    }
    return null;
  }

  showSettingsModal() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const settings = this.userSettings || userProfile;

    const modalContent = `
      <div class="modal-header">
        <h2>Settings</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="settings-form">
          <!-- Notification Settings -->
          <div class="settings-section">
            <h3><span class="material-icons">notifications</span> Notifications</h3>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="push-notifications" ${settings.pushNotifications !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Push Notifications</div>
                  <div class="setting-description">Receive notifications for new messages and activities</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="email-notifications" ${settings.emailNotifications === true ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Email Notifications</div>
                  <div class="setting-description">Receive email updates for important activities</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="friend-notifications" ${settings.friendNotifications !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Friend Activity</div>
                  <div class="setting-description">Get notified when friends post or send messages</div>
                </div>
              </label>
            </div>
          </div>

          <!-- Privacy Settings -->
          <div class="settings-section">
            <h3><span class="material-icons">privacy_tip</span> Privacy</h3>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="profile-visibility" ${settings.profilePublic !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Public Profile</div>
                  <div class="setting-description">Make your profile visible to all community members</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="show-online-status" ${settings.showOnlineStatus !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Show Online Status</div>
                  <div class="setting-description">Let others see when you're online</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="allow-messages" ${settings.allowDirectMessages !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Direct Messages</div>
                  <div class="setting-description">Allow other users to send you direct messages</div>
                </div>
              </label>
            </div>
          </div>

          <!-- Chat Settings -->
          <div class="settings-section">
            <h3><span class="material-icons">chat</span> Chat</h3>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="chat-sounds" ${settings.chatSounds !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Chat Sounds</div>
                  <div class="setting-description">Play sound for new messages</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="auto-scroll" ${settings.autoScrollChat !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Auto-scroll Chat</div>
                  <div class="setting-description">Automatically scroll to new messages</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-title">Font Size</div>
                <div class="setting-description">Adjust chat text size</div>
              </div>
              <select id="chat-font-size" class="setting-select">
                <option value="small" ${settings.chatFontSize === 'small' ? 'selected' : ''}>Small</option>
                <option value="medium" ${settings.chatFontSize === 'medium' || !settings.chatFontSize ? 'selected' : ''}>Medium</option>
                <option value="large" ${settings.chatFontSize === 'large' ? 'selected' : ''}>Large</option>
              </select>
            </div>
          </div>

          <!-- App Settings -->
          <div class="settings-section">
            <h3><span class="material-icons">tune</span> App</h3>
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-title">Theme</div>
                <div class="setting-description">Choose your preferred theme</div>
              </div>
              <select id="app-theme" class="setting-select">
                <option value="light" ${settings.theme === 'light' || !settings.theme ? 'selected' : ''}>Light</option>
                <option value="dark" ${settings.theme === 'dark' ? 'selected' : ''}>Dark</option>
                <option value="auto" ${settings.theme === 'auto' ? 'selected' : ''}>Auto</option>
              </select>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="offline-mode" ${settings.offlineMode === true ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Offline Mode</div>
                  <div class="setting-description">Cache content for offline viewing</div>
                </div>
              </label>
            </div>
            <div class="setting-item">
              <label class="setting-label">
                <input type="checkbox" id="analytics" ${settings.allowAnalytics !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                <div class="setting-info">
                  <div class="setting-title">Usage Analytics</div>
                  <div class="setting-description">Help improve the app by sharing anonymous usage data</div>
                </div>
              </label>
            </div>
          </div>

          <!-- Account Actions -->
          <div class="settings-section">
            <h3><span class="material-icons">account_circle</span> Account</h3>
            <div class="setting-item">
              <button class="btn btn-secondary" onclick="settingsManager.exportData()">
                <span class="material-icons">download</span>
                Export My Data
              </button>
              <div class="setting-description">Download a copy of your data</div>
            </div>
            <div class="setting-item">
              <button class="btn btn-secondary" onclick="settingsManager.clearCache()">
                <span class="material-icons">cached</span>
                Clear Cache
              </button>
              <div class="setting-description">Clear cached data to free up storage</div>
            </div>
            <div class="setting-item">
              <button class="btn btn-danger" onclick="settingsManager.deleteAccount()">
                <span class="material-icons">delete_forever</span>
                Delete Account
              </button>
              <div class="setting-description">Permanently delete your account and all data</div>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" onclick="hideModal()">Cancel</button>
            <button type="button" class="btn btn-primary" onclick="settingsManager.saveSettings()">
              <span class="material-icons">save</span>
              Save Settings
            </button>
          </div>
        </div>
      </div>
    `;

    showModal(modalContent);
  }

  async saveSettings() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      // Collect settings from form
      const settings = {
        // Notifications
        pushNotifications: document.getElementById('push-notifications')?.checked || false,
        emailNotifications: document.getElementById('email-notifications')?.checked || false,
        friendNotifications: document.getElementById('friend-notifications')?.checked || false,
        
        // Privacy
        profilePublic: document.getElementById('profile-visibility')?.checked || false,
        showOnlineStatus: document.getElementById('show-online-status')?.checked || false,
        allowDirectMessages: document.getElementById('allow-messages')?.checked || false,
        
        // Chat
        chatSounds: document.getElementById('chat-sounds')?.checked || false,
        autoScrollChat: document.getElementById('auto-scroll')?.checked || false,
        chatFontSize: document.getElementById('chat-font-size')?.value || 'medium',
        
        // App
        theme: document.getElementById('app-theme')?.value || 'light',
        offlineMode: document.getElementById('offline-mode')?.checked || false,
        allowAnalytics: document.getElementById('analytics')?.checked || false,
        
        // Metadata
        updatedAt: serverTimestamp()
      };

      // Update user profile with settings
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), settings);

      // Apply theme immediately
      this.applyTheme(settings.theme);
      
      // Update cached settings
      this.userSettings = { ...this.userSettings, ...settings };
      
      showToast('Settings saved successfully!', 'success');
      hideModal();
      
    } catch (error) {
      handleError(error, 'Saving settings');
    }
  }

  applyTheme(theme) {
    const body = document.body;
    
    // Remove existing theme classes
    body.classList.remove('theme-light', 'theme-dark');
    
    if (theme === 'dark') {
      body.classList.add('theme-dark');
    } else if (theme === 'light') {
      body.classList.add('theme-light');
    } else if (theme === 'auto') {
      // Use system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      body.classList.add(prefersDark ? 'theme-dark' : 'theme-light');
    }
  }

  async exportData() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      showToast('Preparing data export...', 'info');
      
      // Collect user data
      const userData = {
        profile: userProfile,
        settings: this.userSettings,
        exportDate: new Date().toISOString(),
        version: '1.0.0'
      };

      // Create and download JSON file
      const dataStr = JSON.stringify(userData, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      
      const link = document.createElement('a');
      link.href = URL.createObjectURL(dataBlob);
      link.download = `clara-data-${userProfile.uid}-${new Date().toISOString().split('T')[0]}.json`;
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      showToast('Data export downloaded!', 'success');
      
    } catch (error) {
      handleError(error, 'Exporting data');
    }
  }

  async clearCache() {
    if (!confirm('This will clear all cached data. Continue?')) return;

    try {
      // Clear localStorage
      const keysToKeep = ['clara-auth-token']; // Keep auth token
      Object.keys(localStorage).forEach(key => {
        if (!keysToKeep.some(keepKey => key.includes(keepKey))) {
          localStorage.removeItem(key);
        }
      });

      // Clear service worker cache if available
      if ('serviceWorker' in navigator && 'caches' in window) {
        const cacheNames = await caches.keys();
        await Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        );
      }

      showToast('Cache cleared successfully!', 'success');
      
    } catch (error) {
      handleError(error, 'Clearing cache');
    }
  }

  async deleteAccount() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const confirmation = prompt(
      'This action cannot be undone. Type "DELETE" to confirm account deletion:'
    );

    if (confirmation !== 'DELETE') {
      showToast('Account deletion cancelled', 'info');
      return;
    }

    try {
      // Show confirmation dialog
      const finalConfirm = confirm(
        'Are you absolutely sure? This will permanently delete your account and all associated data.'
      );

      if (!finalConfirm) return;

      showToast('Deleting account...', 'info');

      // Delete user profile from Firestore
      await deleteDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid));

      // Sign out user
      await authManager.signOut();
      
      showToast('Account deleted successfully', 'success');
      hideModal();
      
    } catch (error) {
      handleError(error, 'Deleting account');
    }
  }

  // Initialize settings when app loads
  async initializeSettings(userId) {
    await this.loadSettings(userId);
    
    // Apply saved theme
    if (this.userSettings?.theme) {
      this.applyTheme(this.userSettings.theme);
    }
  }
}

// Make settings manager available globally
window.settingsManager = new SettingsManager();

// CSS for settings (to be added to main CSS)
const settingsCSS = `
.settings-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.settings-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e0e0e0;
}

.settings-section:last-of-type {
  border-bottom: none;
}

.settings-section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.setting-item {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.setting-label {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  flex: 1;
}

.setting-label input[type="checkbox"] {
  display: none;
}

.checkbox-custom {
  width: 20px;
  height: 20px;
  border: 2px solid #ddd;
  border-radius: 4px;
  position: relative;
  background: white;
  flex-shrink: 0;
  margin-top: 2px;
}

.setting-label input[type="checkbox"]:checked + .checkbox-custom {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.setting-label input[type="checkbox"]:checked + .checkbox-custom::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 14px;
  font-weight: bold;
}

.setting-info {
  flex: 1;
}

.setting-title {
  font-weight: 500;
  margin-bottom: 2px;
  color: var(--text-primary);
}

.setting-description {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.setting-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  min-width: 120px;
  font-size: 14px;
}

.setting-select:focus {
  outline: none;
  border-color: var(--primary-color);
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #d32f2f;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

/* Dark theme styles */
.theme-dark {
  --primary-color: #2196F3;
  --primary-light: #1E88E5;
  --primary-dark: #1565C0;
  --text-primary: #ffffff;
  --text-secondary: #b0b0b0;
  --background-color: #121212;
  --surface-color: #1e1e1e;
  --border-color: #333333;
}

.theme-dark .checkbox-custom {
  border-color: #555;
  background: #2a2a2a;
}

.theme-dark .setting-select {
  background: #2a2a2a;
  border-color: #555;
  color: var(--text-primary);
}

.theme-dark .settings-section {
  border-bottom-color: #333;
}
`;

// Add CSS to document
const style = document.createElement('style');
style.textContent = settingsCSS;
document.head.appendChild(style);