// Profile Manager for Clara PWA
import { db, storage, COLLECTIONS } from './config.js';
import { authManager } from './auth.js';
import { 
  doc, 
  getDoc, 
  updateDoc,
  serverTimestamp
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { 
  ref, 
  uploadBytes, 
  getDownloadURL,
  deleteObject 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-storage.js';
import { showModal, hideModal, showToast, handleError, validateEmail } from './utils.js';

export class ProfileManager {
  constructor() {
    this.currentProfile = null;
  }

  async loadProfile(userId) {
    try {
      const userDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, userId));
      if (userDoc.exists()) {
        this.currentProfile = { id: userId, ...userDoc.data() };
        this.renderProfile();
        return this.currentProfile;
      }
    } catch (error) {
      handleError(error, 'Loading profile');
    }
    return null;
  }

  renderProfile() {
    if (!this.currentProfile) return;

    // Update profile avatar
    const profileAvatar = document.getElementById('profile-avatar');
    const defaultAvatar = document.getElementById('default-avatar');
    
    if (this.currentProfile.profilePictureUrl) {
      if (profileAvatar) {
        profileAvatar.src = this.currentProfile.profilePictureUrl;
        profileAvatar.style.display = 'block';
      }
      if (defaultAvatar) defaultAvatar.style.display = 'none';
    } else {
      if (profileAvatar) profileAvatar.style.display = 'none';
      if (defaultAvatar) defaultAvatar.style.display = 'block';
    }

    // Update profile info
    const profileName = document.getElementById('profile-name');
    const profileEmail = document.getElementById('profile-email');
    const profileMemberSince = document.getElementById('profile-member-since');
    const postsCount = document.getElementById('posts-count');
    const friendsCount = document.getElementById('friends-count');

    if (profileName) profileName.textContent = this.currentProfile.displayName || 'No name set';
    if (profileEmail) profileEmail.textContent = this.currentProfile.email || '';
    if (profileMemberSince) {
      const memberSince = this.currentProfile.memberSince ? 
        new Date(this.currentProfile.memberSince.seconds * 1000).toLocaleDateString() : 
        'Unknown';
      profileMemberSince.textContent = `Member since ${memberSince}`;
    }
    if (postsCount) postsCount.textContent = this.currentProfile.numPosts || 0;
    if (friendsCount) friendsCount.textContent = (this.currentProfile.friends || []).length;
  }

  showEditProfileModal() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const profile = this.currentProfile || userProfile;

    const modalContent = `
      <div class="modal-header">
        <h2>Edit Profile</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <form id="edit-profile-form" class="edit-profile-form">
          <!-- Profile Picture -->
          <div class="form-group">
            <label>Profile Picture</label>
            <div class="profile-picture-upload">
              <div class="current-avatar">
                ${profile.profilePictureUrl ? 
                  `<img src="${profile.profilePictureUrl}" alt="Current avatar" id="preview-avatar">` :
                  `<div class="avatar-placeholder" id="preview-avatar">
                    <span class="material-icons">person</span>
                  </div>`
                }
              </div>
              <div class="upload-controls">
                <input type="file" id="avatar-upload" accept="image/*" style="display: none;">
                <button type="button" class="btn btn-secondary" onclick="document.getElementById('avatar-upload').click()">
                  <span class="material-icons">photo_camera</span>
                  Change Photo
                </button>
                ${profile.profilePictureUrl ? 
                  `<button type="button" class="btn btn-secondary" onclick="profileManager.removeProfilePicture()">
                    <span class="material-icons">delete</span>
                    Remove
                  </button>` : ''
                }
              </div>
              <div class="upload-progress" id="avatar-upload-progress" style="display: none;">
                <div class="progress-bar">
                  <div class="progress-fill" id="avatar-progress-fill"></div>
                </div>
                <span id="avatar-progress-text">0%</span>
              </div>
            </div>
          </div>

          <!-- Display Name -->
          <div class="form-group">
            <label for="display-name">Display Name</label>
            <input type="text" id="display-name" value="${profile.displayName || ''}" placeholder="Enter your display name">
          </div>

          <!-- Email (readonly) -->
          <div class="form-group">
            <label for="email">Email</label>
            <input type="email" id="email" value="${profile.email || ''}" readonly disabled>
            <small class="form-help">Email cannot be changed</small>
          </div>

          <!-- Bio -->
          <div class="form-group">
            <label for="bio">Bio</label>
            <textarea id="bio" placeholder="Tell us about yourself..." rows="4">${profile.bio || ''}</textarea>
            <small class="form-help">Share a bit about yourself with the community</small>
          </div>

          <!-- Location -->
          <div class="form-group">
            <label for="location">Location</label>
            <input type="text" id="location" value="${profile.location || ''}" placeholder="City, Country">
          </div>

          <!-- Privacy Settings -->
          <div class="form-group">
            <label>Privacy Settings</label>
            <div class="privacy-options">
              <label class="checkbox-label">
                <input type="checkbox" id="profile-public" ${profile.profilePublic !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                Make my profile public
              </label>
              <label class="checkbox-label">
                <input type="checkbox" id="show-email" ${profile.showEmail === true ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                Show my email to other users
              </label>
              <label class="checkbox-label">
                <input type="checkbox" id="allow-friend-requests" ${profile.allowFriendRequests !== false ? 'checked' : ''}>
                <span class="checkbox-custom"></span>
                Allow friend requests
              </label>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" onclick="hideModal()">Cancel</button>
            <button type="submit" class="btn btn-primary" id="save-profile-btn">
              <span class="material-icons">save</span>
              Save Changes
            </button>
          </div>
        </form>
      </div>
    `;

    showModal(modalContent);
    this.initializeProfileForm();
  }

  initializeProfileForm() {
    const form = document.getElementById('edit-profile-form');
    const avatarUpload = document.getElementById('avatar-upload');
    
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        this.saveProfile();
      });
    }

    if (avatarUpload) {
      avatarUpload.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
          this.previewAvatar(e.target.files[0]);
        }
      });
    }
  }

  previewAvatar(file) {
    // Validate file
    if (!file.type.startsWith('image/')) {
      showToast('Please select an image file', 'error');
      return;
    }

    if (file.size > 5 * 1024 * 1024) { // 5MB limit
      showToast('Image must be smaller than 5MB', 'error');
      return;
    }

    // Show preview
    const reader = new FileReader();
    reader.onload = (e) => {
      const previewAvatar = document.getElementById('preview-avatar');
      if (previewAvatar) {
        previewAvatar.innerHTML = `<img src="${e.target.result}" alt="Preview">`;
      }
    };
    reader.readAsDataURL(file);
  }

  async uploadProfilePicture(file) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) throw new Error('User not authenticated');

    // Show progress
    const progressContainer = document.getElementById('avatar-upload-progress');
    const progressFill = document.getElementById('avatar-progress-fill');
    const progressText = document.getElementById('avatar-progress-text');
    
    if (progressContainer) progressContainer.style.display = 'block';

    try {
      // Create unique filename
      const timestamp = Date.now();
      const fileExtension = file.name.split('.').pop();
      const fileName = `profile_pictures/${userProfile.uid}_${timestamp}.${fileExtension}`;
      
      // Upload to Firebase Storage
      const storageRef = ref(storage, fileName);
      
      // Upload with progress tracking
      const uploadTask = uploadBytes(storageRef, file);
      
      // Simulate progress for now (Firebase Web SDK doesn't support upload progress)
      let progress = 0;
      const progressInterval = setInterval(() => {
        progress += 10;
        if (progress <= 90) {
          if (progressFill) progressFill.style.width = `${progress}%`;
          if (progressText) progressText.textContent = `${progress}%`;
        }
      }, 100);

      const snapshot = await uploadTask;
      clearInterval(progressInterval);
      
      // Complete progress
      if (progressFill) progressFill.style.width = '100%';
      if (progressText) progressText.textContent = '100%';
      
      // Get download URL
      const downloadURL = await getDownloadURL(snapshot.ref);
      
      // Hide progress
      setTimeout(() => {
        if (progressContainer) progressContainer.style.display = 'none';
      }, 500);
      
      return downloadURL;
      
    } catch (error) {
      if (progressContainer) progressContainer.style.display = 'none';
      throw error;
    }
  }

  async removeProfilePicture() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile || !userProfile.profilePictureUrl) return;

    if (!confirm('Are you sure you want to remove your profile picture?')) return;

    try {
      // Remove from storage if it's a Firebase Storage URL
      if (userProfile.profilePictureUrl.includes('firebasestorage.googleapis.com')) {
        try {
          const storageRef = ref(storage, userProfile.profilePictureUrl);
          await deleteObject(storageRef);
        } catch (storageError) {
          console.warn('Could not delete old profile picture:', storageError);
        }
      }

      // Update profile
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), {
        profilePictureUrl: null,
        updatedAt: serverTimestamp()
      });

      // Update preview
      const previewAvatar = document.getElementById('preview-avatar');
      if (previewAvatar) {
        previewAvatar.innerHTML = `
          <div class="avatar-placeholder">
            <span class="material-icons">person</span>
          </div>
        `;
      }

      showToast('Profile picture removed', 'success');
      
    } catch (error) {
      handleError(error, 'Removing profile picture');
    }
  }

  async saveProfile() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const saveBtn = document.getElementById('save-profile-btn');
    if (saveBtn) {
      saveBtn.disabled = true;
      saveBtn.innerHTML = '<span class="material-icons spin">refresh</span> Saving...';
    }

    try {
      // Get form data
      const displayName = document.getElementById('display-name')?.value.trim();
      const bio = document.getElementById('bio')?.value.trim();
      const location = document.getElementById('location')?.value.trim();
      const profilePublic = document.getElementById('profile-public')?.checked;
      const showEmail = document.getElementById('show-email')?.checked;
      const allowFriendRequests = document.getElementById('allow-friend-requests')?.checked;
      const avatarFile = document.getElementById('avatar-upload')?.files[0];

      // Validate required fields
      if (!displayName) {
        showToast('Display name is required', 'error');
        return;
      }

      // Prepare update data
      const updateData = {
        displayName,
        bio: bio || null,
        location: location || null,
        profilePublic,
        showEmail,
        allowFriendRequests,
        updatedAt: serverTimestamp()
      };

      // Upload new avatar if selected
      if (avatarFile) {
        try {
          const newAvatarUrl = await this.uploadProfilePicture(avatarFile);
          updateData.profilePictureUrl = newAvatarUrl;
          
          // Delete old avatar if it exists and is different
          if (userProfile.profilePictureUrl && 
              userProfile.profilePictureUrl !== newAvatarUrl &&
              userProfile.profilePictureUrl.includes('firebasestorage.googleapis.com')) {
            try {
              const oldRef = ref(storage, userProfile.profilePictureUrl);
              await deleteObject(oldRef);
            } catch (deleteError) {
              console.warn('Could not delete old profile picture:', deleteError);
            }
          }
        } catch (uploadError) {
          handleError(uploadError, 'Uploading profile picture');
          return;
        }
      }

      // Update profile in Firestore
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), updateData);

      // Update current profile cache
      this.currentProfile = { ...this.currentProfile, ...updateData };
      
      // Re-render profile
      this.renderProfile();
      
      showToast('Profile updated successfully!', 'success');
      hideModal();
      
    } catch (error) {
      handleError(error, 'Saving profile');
    } finally {
      if (saveBtn) {
        saveBtn.disabled = false;
        saveBtn.innerHTML = '<span class="material-icons">save</span> Save Changes';
      }
    }
  }
}

// Make profile manager available globally
window.profileManager = new ProfileManager();

// CSS for profile editing (to be added to main CSS)
const profileEditingCSS = `
.edit-profile-form {
  max-width: 500px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: var(--text-primary);
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.form-group input:disabled {
  background-color: #f5f5f5;
  color: #666;
  cursor: not-allowed;
}

.form-help {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.profile-picture-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.current-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
}

.upload-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.upload-progress {
  width: 100%;
  margin-top: 8px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.3s ease;
  width: 0%;
}

.privacy-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
}

.checkbox-label input[type="checkbox"] {
  display: none;
}

.checkbox-custom {
  width: 18px;
  height: 18px;
  border: 2px solid #ddd;
  border-radius: 3px;
  position: relative;
  background: white;
}

.checkbox-label input[type="checkbox"]:checked + .checkbox-custom {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.checkbox-label input[type="checkbox"]:checked + .checkbox-custom::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
`;

// Add CSS to document
const style = document.createElement('style');
style.textContent = profileEditingCSS;
document.head.appendChild(style);