// Feed Manager for Clara PWA
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
  startAfter,
  doc,
  getDoc,
  updateDoc,
  increment,
  where
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { ref, uploadBytes, getDownloadURL } from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-storage.js';
import { storage } from './config.js';
import { showToast, handleError, formatDateTime } from './utils.js';

export class FeedManager {
  constructor() {
    this.posts = [];
    this.lastVisible = null;
    this.loading = false;
    this.hasMore = true;
    this.POSTS_PER_PAGE = 10;
  }

  async initialize() {
    console.log('Feed Manager initialized');
    await this.loadFeed();
    this.setupEventListeners();
  }

  setupEventListeners() {
    // New post form
    const newPostBtn = document.getElementById('new-post-btn');
    if (newPostBtn) {
      newPostBtn.addEventListener('click', () => this.showNewPostModal());
    }

    // Image input for posts
    const imageInput = document.getElementById('post-image');
    if (imageInput) {
      imageInput.addEventListener('change', (e) => this.handleImageSelect(e));
    }

    // Infinite scroll
    window.addEventListener('scroll', () => {
      if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 1000 && !this.loading && this.hasMore) {
        this.loadMorePosts();
      }
    });
  }

  showNewPostModal() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) {
      showToast('Please sign in to create posts', 'error');
      return;
    }

    const modalContent = `
      <div class="modal-header">
        <h2>Create New Post</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <form id="new-post-form" class="new-post-form">
          <div class="post-input-container">
            <textarea id="post-content" placeholder="What's on your mind?" rows="4" required></textarea>
          </div>
          
          <div class="image-upload-container">
            <input type="file" id="post-image" accept="image/*" style="display: none;">
            <button type="button" class="btn btn-secondary" onclick="document.getElementById('post-image').click()">
              <span class="material-icons">photo_camera</span>
              Add Photo
            </button>
            <div id="image-preview" class="image-preview" style="display: none;">
              <img id="preview-img" src="" alt="Preview">
              <button type="button" class="remove-image-btn" onclick="feedManager.removeImagePreview()">
                <span class="material-icons">close</span>
              </button>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" onclick="hideModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">
              <span class="material-icons">send</span>
              Post
            </button>
          </div>
        </form>
      </div>
    `;

    showModal(modalContent);

    // Setup form submission
    document.getElementById('new-post-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.createPost();
    });

    // Setup image preview
    document.getElementById('post-image').addEventListener('change', (e) => {
      this.handleImageSelect(e);
    });
  }

  handleImageSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      showToast('Image must be less than 5MB', 'error');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const preview = document.getElementById('image-preview');
      const img = document.getElementById('preview-img');
      
      if (preview && img) {
        img.src = e.target.result;
        preview.style.display = 'block';
      }
    };
    reader.readAsDataURL(file);
  }

  removeImagePreview() {
    const preview = document.getElementById('image-preview');
    const input = document.getElementById('post-image');
    
    if (preview) preview.style.display = 'none';
    if (input) input.value = '';
  }

  async createPost() {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const content = document.getElementById('post-content')?.value.trim();
    const imageFile = document.getElementById('post-image')?.files[0];

    if (!content && !imageFile) {
      showToast('Please add some content or an image', 'error');
      return;
    }

    try {
      const submitBtn = document.querySelector('#new-post-form button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="loading-spinner"></span> Posting...';
      }

      let imageUrl = null;
      if (imageFile) {
        imageUrl = await this.uploadImage(imageFile);
      }

      // Create post in Firestore
      const postRef = await addDoc(collection(db, COLLECTIONS.POSTS), {
        content: content || '',
        imageUrl: imageUrl,
        userId: userProfile.uid,
        userDisplayName: userProfile.displayName || userProfile.email,
        userProfilePicture: userProfile.profilePictureUrl || '',
        likes: [],
        likesCount: 0,
        commentsCount: 0,
        createdAt: serverTimestamp()
      });

      // Update user's post count
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), {
        numPosts: increment(1)
      });

      showToast('Post created successfully!', 'success');
      hideModal();
      this.loadFeed(); // Refresh feed

    } catch (error) {
      handleError(error, 'Creating post');
    }
  }

  async uploadImage(file) {
    const userProfile = authManager.getUserProfile();
    const timestamp = Date.now();
    const imageRef = ref(storage, `posts/${userProfile.uid}/${timestamp}_${file.name}`);
    
    const snapshot = await uploadBytes(imageRef, file);
    return await getDownloadURL(snapshot.ref);
  }

  async loadFeed(refresh = true) {
    if (this.loading) return;
    
    this.loading = true;
    
    try {
      if (refresh) {
        this.posts = [];
        this.lastVisible = null;
        this.hasMore = true;
      }

      let feedQuery = query(
        collection(db, COLLECTIONS.POSTS),
        orderBy('createdAt', 'desc'),
        limit(this.POSTS_PER_PAGE)
      );

      if (this.lastVisible) {
        feedQuery = query(
          collection(db, COLLECTIONS.POSTS),
          orderBy('createdAt', 'desc'),
          startAfter(this.lastVisible),
          limit(this.POSTS_PER_PAGE)
        );
      }

      const snapshot = await getDocs(feedQuery);
      
      if (snapshot.empty) {
        this.hasMore = false;
        if (refresh) this.renderEmptyFeed();
        return;
      }

      const newPosts = [];
      for (const doc of snapshot.docs) {
        const postData = doc.data();
        
        // Get user profile for additional info
        const userDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, postData.userId));
        const userProfile = userDoc.exists() ? userDoc.data() : {};

        newPosts.push({
          id: doc.id,
          ...postData,
          userProfile: {
            displayName: userProfile.displayName || postData.userDisplayName || 'Unknown User',
            profilePictureUrl: userProfile.profilePictureUrl || postData.userProfilePicture || '',
            isAdmin: userProfile.isAdmin || false
          }
        });
      }

      this.posts = refresh ? newPosts : [...this.posts, ...newPosts];
      this.lastVisible = snapshot.docs[snapshot.docs.length - 1];
      this.hasMore = snapshot.docs.length === this.POSTS_PER_PAGE;

      this.renderFeed();

    } catch (error) {
      handleError(error, 'Loading feed');
    } finally {
      this.loading = false;
    }
  }

  async loadMorePosts() {
    if (!this.hasMore || this.loading) return;
    await this.loadFeed(false);
  }

  renderFeed() {
    const feedContainer = document.getElementById('feed-container');
    if (!feedContainer) return;

    if (this.posts.length === 0) {
      this.renderEmptyFeed();
      return;
    }

    const feedHTML = this.posts.map(post => this.renderPost(post)).join('');
    feedContainer.innerHTML = feedHTML;

    // Setup post interactions
    this.setupPostInteractions();
  }

  renderEmptyFeed() {
    const feedContainer = document.getElementById('feed-container');
    if (!feedContainer) return;

    feedContainer.innerHTML = `
      <div class="empty-feed">
        <span class="material-icons">chat_bubble_outline</span>
        <h3>No posts yet</h3>
        <p>Be the first to share something!</p>
        <button class="btn btn-primary" onclick="feedManager.showNewPostModal()">
          <span class="material-icons">add</span>
          Create First Post
        </button>
      </div>
    `;
  }

  renderPost(post) {
    const userProfile = authManager.getUserProfile();
    const currentUserId = userProfile?.uid;
    const isOwnPost = post.userId === currentUserId;
    const isAdmin = userProfile?.isAdmin || false;
    const hasLiked = post.likes?.includes(currentUserId) || false;

    return `
      <div class="post-card" data-post-id="${post.id}">
        <div class="post-header">
          <div class="post-user-info">
            <div class="user-avatar">
              ${post.userProfile.profilePictureUrl ? 
                `<img src="${post.userProfile.profilePictureUrl}" alt="${post.userProfile.displayName}">` :
                `<span class="material-icons">person</span>`
              }
            </div>
            <div class="user-details">
              <span class="user-name">
                ${post.userProfile.displayName}
                ${post.userProfile.isAdmin ? '<span class="admin-badge">Admin</span>' : ''}
              </span>
              <span class="post-time">${formatDateTime(post.createdAt?.toDate() || new Date())}</span>
            </div>
          </div>
          <button class="post-menu-btn" onclick="interactionManager.showPostActions('${post.id}', ${isOwnPost}, ${isAdmin})">
            <span class="material-icons">more_vert</span>
          </button>
        </div>

        ${post.content ? `<div class="post-content">${post.content}</div>` : ''}
        
        ${post.imageUrl ? `
          <div class="post-image">
            <img src="${post.imageUrl}" alt="Post image" onclick="showImageModal('${post.imageUrl}')">
          </div>
        ` : ''}

        <div class="post-actions">
          <button class="action-btn like-btn ${hasLiked ? 'liked' : ''}" onclick="interactionManager.toggleLike('${post.id}')">
            <span class="material-icons">${hasLiked ? 'favorite' : 'favorite_border'}</span>
            <span class="like-count">${post.likesCount || 0}</span>
          </button>
          
          <button class="action-btn comment-btn" onclick="interactionManager.showComments('${post.id}')">
            <span class="material-icons">chat_bubble_outline</span>
            <span class="comment-count">${post.commentsCount || 0}</span>
          </button>
          
          <button class="action-btn share-btn" onclick="navigator.share ? navigator.share({title: 'Clara Post', url: window.location.href + '#post-${post.id}'}) : interactionManager.copyPostLink('${post.id}')">
            <span class="material-icons">share</span>
          </button>
        </div>
      </div>
    `;
  }

  setupPostInteractions() {
    // Post interactions are handled by the InteractionManager
    // This method can be used for additional setup if needed
  }
}

// Global functions for HTML onclick handlers
window.showImageModal = (imageUrl) => {
  const modalContent = `
    <div class="modal-header">
      <button class="icon-btn" onclick="hideModal()">
        <span class="material-icons">close</span>
      </button>
    </div>
    <div class="modal-content image-modal-content">
      <img src="${imageUrl}" alt="Full size image">
    </div>
  `;
  showModal(modalContent);
};

// Make feed manager available globally
window.feedManager = new FeedManager();