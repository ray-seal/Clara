// Friends Manager for Clara PWA
import { db, COLLECTIONS } from './config.js';
import { authManager } from './auth.js';
import { 
  collection, 
  query, 
  orderBy, 
  limit, 
  getDocs, 
  addDoc, 
  serverTimestamp,
  where,
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  arrayUnion,
  arrayRemove
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showLoading, showEmptyState, handleError, formatDate, showModal, hideModal, showToast } from './utils.js';

export class FriendsManager {
  constructor() {
    this.friends = [];
    this.friendRequests = [];
  }

  async loadFriends() {
    const container = document.getElementById('friends-list');
    if (!container) return;

    try {
      showLoading('friends-list');
      
      const userProfile = authManager.getUserProfile();
      if (!userProfile) return;

      // Load user's friends
      await this.loadUserFriends(userProfile.uid);
      
      if (this.friends.length === 0) {
        showEmptyState('friends-list', 'people', 'No friends yet', 'Connect with other community members');
        return;
      }

      this.renderFriends();
      
    } catch (error) {
      handleError(error, 'Loading friends');
      showEmptyState('friends-list', 'error', 'Error loading friends', 'Please try again later');
    }
  }

  async loadUserFriends(userId) {
    try {
      // Get user's profile to access friends list
      const userDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, userId));
      if (!userDoc.exists()) return;

      const userData = userDoc.data();
      const friendIds = userData.friends || [];

      this.friends = [];
      
      // Load friend profiles
      for (const friendId of friendIds) {
        const friendDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, friendId));
        if (friendDoc.exists()) {
          this.friends.push({ id: friendId, ...friendDoc.data() });
        }
      }
      
    } catch (error) {
      console.error('Error loading user friends:', error);
    }
  }

  renderFriends() {
    const container = document.getElementById('friends-list');
    if (!container) return;

    const friendsHTML = this.friends.map(friend => this.renderFriend(friend)).join('');
    container.innerHTML = friendsHTML;
  }

  renderFriend(friend) {
    return `
      <div class="friend-item">
        <div class="friend-avatar">
          ${friend.profilePictureUrl ? 
            `<img src="${friend.profilePictureUrl}" alt="${friend.displayName}">` :
            `<span class="material-icons">person</span>`
          }
        </div>
        <div class="friend-info">
          <div class="friend-name">${friend.displayName || friend.email}</div>
          <div class="friend-status">${this.getFriendStatus(friend)}</div>
        </div>
        <div class="friend-actions">
          <button class="btn btn-secondary" onclick="friendsManager.chatWithFriend('${friend.id}')">
            <span class="material-icons">chat</span>
            Chat
          </button>
          <button class="btn btn-secondary" onclick="friendsManager.removeFriend('${friend.id}')">
            <span class="material-icons">person_remove</span>
            Remove
          </button>
        </div>
      </div>
    `;
  }

  getFriendStatus(friend) {
    const memberSince = friend.memberSince ? formatDate(friend.memberSince) : 'Unknown';
    const postCount = friend.numPosts || 0;
    return `Member since ${memberSince} • ${postCount} posts`;
  }

  async showFindFriends() {
    try {
      showLoading('friends-list');
      
      const userProfile = authManager.getUserProfile();
      if (!userProfile) return;

      // Get all users except current user and existing friends
      const usersQuery = query(collection(db, COLLECTIONS.PROFILES), limit(50));
      const snapshot = await getDocs(usersQuery);
      
      const currentFriends = userProfile.friends || [];
      const potentialFriends = [];
      
      snapshot.forEach(doc => {
        const userData = { id: doc.id, ...doc.data() };
        if (userData.id !== userProfile.uid && !currentFriends.includes(userData.id)) {
          potentialFriends.push(userData);
        }
      });

      this.showFindFriendsModal(potentialFriends);
      
    } catch (error) {
      handleError(error, 'Finding friends');
    }
  }

  showFindFriendsModal(users) {
    const usersHTML = users.map(user => `
      <div class="friend-item">
        <div class="friend-avatar">
          ${user.profilePictureUrl ? 
            `<img src="${user.profilePictureUrl}" alt="${user.displayName}">` :
            `<span class="material-icons">person</span>`
          }
        </div>
        <div class="friend-info">
          <div class="friend-name">${user.displayName || user.email}</div>
          <div class="friend-status">${this.getFriendStatus(user)}</div>
        </div>
        <div class="friend-actions">
          <button class="btn btn-primary" onclick="friendsManager.sendFriendRequest('${user.id}')">
            <span class="material-icons">person_add</span>
            Add Friend
          </button>
        </div>
      </div>
    `).join('');

    const modalContent = `
      <div class="modal-header">
        <h2>Find Friends</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="find-friends-list">
          ${users.length > 0 ? usersHTML : '<p>No new users to connect with</p>'}
        </div>
      </div>
    `;

    showModal(modalContent);
  }

  async sendFriendRequest(friendId) {
    try {
      const userProfile = authManager.getUserProfile();
      if (!userProfile) return;

      // Create friend request
      const requestData = {
        fromUserId: userProfile.uid,
        fromUserName: userProfile.displayName || userProfile.email,
        fromUserAvatar: userProfile.profilePictureUrl || '',
        toUserId: friendId,
        status: 'pending',
        createdAt: serverTimestamp()
      };

      await addDoc(collection(db, COLLECTIONS.FRIEND_REQUESTS), requestData);
      showToast('Friend request sent!', 'success');
      
    } catch (error) {
      handleError(error, 'Sending friend request');
    }
  }

  async removeFriend(friendId) {
    if (!confirm('Are you sure you want to remove this friend?')) return;

    try {
      const userProfile = authManager.getUserProfile();
      if (!userProfile) return;

      // Remove from current user's friends list
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), {
        friends: arrayRemove(friendId)
      });

      // Remove from friend's friends list
      await updateDoc(doc(db, COLLECTIONS.PROFILES, friendId), {
        friends: arrayRemove(userProfile.uid)
      });

      showToast('Friend removed', 'success');
      this.loadFriends(); // Refresh friends list
      
    } catch (error) {
      handleError(error, 'Removing friend');
    }
  }

  async chatWithFriend(friendId) {
    // TODO: Implement direct messaging
    console.log('Chat with friend:', friendId);
    showToast('Direct messaging coming soon!', 'info');
  }
}

// Make friends manager available globally
window.friendsManager = new FriendsManager();

// Update the global find friends function
window.showFindFriends = () => {
  window.friendsManager.showFindFriends();
};