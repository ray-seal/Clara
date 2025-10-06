// Likes and Comments Manager for Clara PWA
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
  updateDoc,
  deleteDoc,
  increment,
  arrayUnion,
  arrayRemove,
  onSnapshot
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showToast, handleError, formatDateTime, showModal, hideModal } from './utils.js';

export class InteractionManager {
  constructor() {
    this.likesCache = new Map(); // Cache likes for posts
    this.commentsCache = new Map(); // Cache comments for posts
  }

  // LIKES FUNCTIONALITY
  async toggleLike(postId) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) {
      showToast('Please sign in to like posts', 'error');
      return;
    }

    try {
      const postRef = doc(db, COLLECTIONS.POSTS, postId);
      const postDoc = await getDoc(postRef);
      
      if (!postDoc.exists()) {
        showToast('Post not found', 'error');
        return;
      }

      const postData = postDoc.data();
      const likes = postData.likes || [];
      const hasLiked = likes.includes(userProfile.uid);

      // Update UI immediately for better UX
      this.updateLikeUI(postId, !hasLiked, likes.length + (hasLiked ? -1 : 1));

      if (hasLiked) {
        // Remove like
        await updateDoc(postRef, {
          likes: arrayRemove(userProfile.uid),
          likesCount: increment(-1)
        });
      } else {
        // Add like
        await updateDoc(postRef, {
          likes: arrayUnion(userProfile.uid),
          likesCount: increment(1)
        });

        // Send notification to post author (if not self-like)
        if (postData.userId !== userProfile.uid) {
          await this.sendLikeNotification(postData.userId, userProfile, postId);
        }
      }

      // Update cache
      this.likesCache.set(postId, {
        hasLiked: !hasLiked,
        count: likes.length + (hasLiked ? -1 : 1)
      });

    } catch (error) {
      handleError(error, 'Toggling like');
      // Revert UI on error
      const cachedLike = this.likesCache.get(postId);
      if (cachedLike) {
        this.updateLikeUI(postId, !cachedLike.hasLiked, cachedLike.count);
      }
    }
  }

  updateLikeUI(postId, hasLiked, count) {
    const likeBtn = document.querySelector(`[data-post-id="${postId}"] .like-btn`);
    const likeCount = document.querySelector(`[data-post-id="${postId}"] .like-count`);
    
    if (likeBtn) {
      likeBtn.classList.toggle('liked', hasLiked);
      const icon = likeBtn.querySelector('.material-icons');
      if (icon) {
        icon.textContent = hasLiked ? 'favorite' : 'favorite_border';
      }
    }
    
    if (likeCount) {
      likeCount.textContent = count || 0;
    }
  }

  async sendLikeNotification(userId, liker, postId) {
    try {
      await addDoc(collection(db, COLLECTIONS.NOTIFICATIONS), {
        userId: userId,
        title: 'New Like',
        body: `${liker.displayName || liker.email} liked your post`,
        type: 'like',
        data: {
          postId: postId,
          likerId: liker.uid,
          likerName: liker.displayName || liker.email
        },
        read: false,
        createdAt: serverTimestamp()
      });
    } catch (error) {
      console.error('Error sending like notification:', error);
    }
  }

  // COMMENTS FUNCTIONALITY
  async loadComments(postId) {
    try {
      const commentsQuery = query(
        collection(db, COLLECTIONS.COMMENTS),
        where('postId', '==', postId),
        orderBy('createdAt', 'asc')
      );

      const snapshot = await getDocs(commentsQuery);
      const comments = [];

      for (const doc of snapshot.docs) {
        const commentData = doc.data();
        
        // Get commenter profile
        const userDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, commentData.userId));
        const userProfile = userDoc.exists() ? userDoc.data() : {};

        comments.push({
          id: doc.id,
          ...commentData,
          userProfile: {
            displayName: userProfile.displayName || 'Unknown User',
            profilePictureUrl: userProfile.profilePictureUrl || '',
            isAdmin: userProfile.isAdmin || false
          }
        });
      }

      this.commentsCache.set(postId, comments);
      return comments;

    } catch (error) {
      handleError(error, 'Loading comments');
      return [];
    }
  }

  async addComment(postId, content) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) {
      showToast('Please sign in to comment', 'error');
      return false;
    }

    if (!content.trim()) {
      showToast('Comment cannot be empty', 'error');
      return false;
    }

    try {
      // Add comment to database
      const commentRef = await addDoc(collection(db, COLLECTIONS.COMMENTS), {
        postId: postId,
        userId: userProfile.uid,
        content: content.trim(),
        createdAt: serverTimestamp()
      });

      // Update post comments count
      await updateDoc(doc(db, COLLECTIONS.POSTS, postId), {
        commentsCount: increment(1)
      });

      // Get post data for notification
      const postDoc = await getDoc(doc(db, COLLECTIONS.POSTS, postId));
      if (postDoc.exists()) {
        const postData = postDoc.data();
        
        // Send notification to post author (if not self-comment)
        if (postData.userId !== userProfile.uid) {
          await this.sendCommentNotification(postData.userId, userProfile, postId, content);
        }
      }

      // Refresh comments display
      await this.showComments(postId);
      
      showToast('Comment added!', 'success');
      return true;

    } catch (error) {
      handleError(error, 'Adding comment');
      return false;
    }
  }

  async sendCommentNotification(userId, commenter, postId, commentContent) {
    try {
      await addDoc(collection(db, COLLECTIONS.NOTIFICATIONS), {
        userId: userId,
        title: 'New Comment',
        body: `${commenter.displayName || commenter.email} commented: ${commentContent.substring(0, 50)}${commentContent.length > 50 ? '...' : ''}`,
        type: 'comment',
        data: {
          postId: postId,
          commenterId: commenter.uid,
          commenterName: commenter.displayName || commenter.email
        },
        read: false,
        createdAt: serverTimestamp()
      });
    } catch (error) {
      console.error('Error sending comment notification:', error);
    }
  }

  async deleteComment(commentId, postId) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    try {
      // Get comment to check ownership
      const commentDoc = await getDoc(doc(db, COLLECTIONS.COMMENTS, commentId));
      if (!commentDoc.exists()) {
        showToast('Comment not found', 'error');
        return;
      }

      const commentData = commentDoc.data();
      
      // Check if user can delete (own comment or admin)
      if (commentData.userId !== userProfile.uid && !userProfile.isAdmin) {
        showToast('You can only delete your own comments', 'error');
        return;
      }

      if (!confirm('Are you sure you want to delete this comment?')) return;

      // Delete comment
      await deleteDoc(doc(db, COLLECTIONS.COMMENTS, commentId));

      // Update post comments count
      await updateDoc(doc(db, COLLECTIONS.POSTS, postId), {
        commentsCount: increment(-1)
      });

      // Refresh comments display
      await this.showComments(postId);
      
      showToast('Comment deleted', 'success');

    } catch (error) {
      handleError(error, 'Deleting comment');
    }
  }

  showComments(postId) {
    const modalContent = `
      <div class="modal-header">
        <h2>Comments</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="comments-container">
          <div id="comments-list" class="comments-list">
            <div class="loading">Loading comments...</div>
          </div>
          
          <div class="add-comment-form">
            <div class="comment-input-container">
              <textarea id="comment-input" placeholder="Write a comment..." rows="3"></textarea>
              <button class="btn btn-primary" onclick="interactionManager.submitComment('${postId}')">
                <span class="material-icons">send</span>
                Post Comment
              </button>
            </div>
          </div>
        </div>
      </div>
    `;

    showModal(modalContent);
    this.loadAndDisplayComments(postId);
  }

  async loadAndDisplayComments(postId) {
    const commentsList = document.getElementById('comments-list');
    if (!commentsList) return;

    try {
      const comments = await this.loadComments(postId);
      
      if (comments.length === 0) {
        commentsList.innerHTML = `
          <div class="empty-state">
            <span class="material-icons">chat_bubble_outline</span>
            <p>No comments yet</p>
            <p>Be the first to comment!</p>
          </div>
        `;
        return;
      }

      const commentsHTML = comments.map(comment => this.renderComment(comment, postId)).join('');
      commentsList.innerHTML = commentsHTML;

    } catch (error) {
      commentsList.innerHTML = `
        <div class="error-state">
          <span class="material-icons">error</span>
          <p>Error loading comments</p>
        </div>
      `;
    }
  }

  renderComment(comment, postId) {
    const userProfile = authManager.getUserProfile();
    const canDelete = userProfile && (comment.userId === userProfile.uid || userProfile.isAdmin);

    return `
      <div class="comment-item" data-comment-id="${comment.id}">
        <div class="comment-avatar">
          ${comment.userProfile.profilePictureUrl ? 
            `<img src="${comment.userProfile.profilePictureUrl}" alt="${comment.userProfile.displayName}">` :
            `<span class="material-icons">person</span>`
          }
        </div>
        <div class="comment-content">
          <div class="comment-header">
            <span class="comment-author">
              ${comment.userProfile.displayName}
              ${comment.userProfile.isAdmin ? '<span class="admin-badge">Admin</span>' : ''}
            </span>
            <span class="comment-time">${formatDateTime(comment.createdAt?.toDate() || new Date())}</span>
            ${canDelete ? `
              <button class="comment-delete-btn" onclick="interactionManager.deleteComment('${comment.id}', '${postId}')" title="Delete comment">
                <span class="material-icons">delete</span>
              </button>
            ` : ''}
          </div>
          <div class="comment-text">${comment.content}</div>
        </div>
      </div>
    `;
  }

  async submitComment(postId) {
    const commentInput = document.getElementById('comment-input');
    if (!commentInput) return;

    const content = commentInput.value.trim();
    if (!content) {
      showToast('Please enter a comment', 'error');
      return;
    }

    const success = await this.addComment(postId, content);
    if (success) {
      commentInput.value = '';
    }
  }

  // REPORTING FUNCTIONALITY
  async reportPost(postId, reason) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) {
      showToast('Please sign in to report posts', 'error');
      return;
    }

    try {
      // Get post data
      const postDoc = await getDoc(doc(db, COLLECTIONS.POSTS, postId));
      if (!postDoc.exists()) {
        showToast('Post not found', 'error');
        return;
      }

      const postData = postDoc.data();

      // Add report to database
      await addDoc(collection(db, COLLECTIONS.REPORTS), {
        postId: postId,
        reportedUserId: postData.userId,
        reportedBy: userProfile.uid,
        reporterName: userProfile.displayName || userProfile.email,
        reason: reason,
        postContent: postData.content,
        postImageUrl: postData.imageUrl || null,
        status: 'pending',
        createdAt: serverTimestamp()
      });

      // Send urgent notification to all admins
      await this.notifyAdminsOfReport(postId, postData, userProfile, reason);

      showToast('Report submitted. Admins have been notified.', 'success');

    } catch (error) {
      handleError(error, 'Reporting post');
    }
  }

  async notifyAdminsOfReport(postId, postData, reporter, reason) {
    try {
      // Get all admin users
      const adminsQuery = query(
        collection(db, COLLECTIONS.PROFILES),
        where('isAdmin', '==', true)
      );

      const adminsSnapshot = await getDocs(adminsQuery);
      
      // Send notification to each admin
      const notificationPromises = adminsSnapshot.docs.map(adminDoc => {
        return addDoc(collection(db, COLLECTIONS.NOTIFICATIONS), {
          userId: adminDoc.id,
          title: '🚨 URGENT: Content Report',
          body: `Post reported for "${reason}" by ${reporter.displayName || reporter.email}. Requires immediate review.`,
          type: 'admin_report',
          data: {
            postId: postId,
            reportedUserId: postData.userId,
            reporterId: reporter.uid,
            reason: reason,
            priority: 'urgent'
          },
          read: false,
          createdAt: serverTimestamp()
        });
      });

      await Promise.all(notificationPromises);
      console.log(`Report notifications sent to ${adminsSnapshot.size} admins`);

    } catch (error) {
      console.error('Error notifying admins of report:', error);
    }
  }

  showReportModal(postId) {
    const modalContent = `
      <div class="modal-header">
        <h2>Report Post</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="report-form">
          <p>Why are you reporting this post?</p>
          
          <div class="report-reasons">
            <label class="reason-option">
              <input type="radio" name="report-reason" value="inappropriate">
              <span class="reason-text">
                <strong>Inappropriate Content</strong>
                <small>Offensive, inappropriate, or violates community guidelines</small>
              </span>
            </label>
            
            <label class="reason-option">
              <input type="radio" name="report-reason" value="spam">
              <span class="reason-text">
                <strong>Spam</strong>
                <small>Repetitive, promotional, or irrelevant content</small>
              </span>
            </label>
            
            <label class="reason-option">
              <input type="radio" name="report-reason" value="harassment">
              <span class="reason-text">
                <strong>Harassment or Bullying</strong>
                <small>Targeting, intimidating, or bullying behavior</small>
              </span>
            </label>
            
            <label class="reason-option">
              <input type="radio" name="report-reason" value="misinformation">
              <span class="reason-text">
                <strong>Misinformation</strong>
                <small>False or misleading information</small>
              </span>
            </label>
            
            <label class="reason-option">
              <input type="radio" name="report-reason" value="other">
              <span class="reason-text">
                <strong>Other</strong>
                <small>Another reason not listed above</small>
              </span>
            </label>
          </div>
          
          <div class="form-actions">
            <button class="btn btn-secondary" onclick="hideModal()">Cancel</button>
            <button class="btn btn-danger" onclick="interactionManager.submitReport('${postId}')">
              <span class="material-icons">flag</span>
              Submit Report
            </button>
          </div>
        </div>
      </div>
    `;

    showModal(modalContent);
  }

  submitReport(postId) {
    const selectedReason = document.querySelector('input[name="report-reason"]:checked');
    
    if (!selectedReason) {
      showToast('Please select a reason for reporting', 'error');
      return;
    }

    const reason = selectedReason.value;
    this.reportPost(postId, reason);
    hideModal();
  }

  // POST ACTIONS MENU
  showPostActions(postId, isOwnPost, isAdmin) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    const canDelete = isOwnPost || isAdmin;

    const modalContent = `
      <div class="modal-header">
        <h2>Post Actions</h2>
        <button class="icon-btn" onclick="hideModal()">
          <span class="material-icons">close</span>
        </button>
      </div>
      <div class="modal-content">
        <div class="post-actions-menu">
          ${canDelete ? `
            <button class="action-item danger" onclick="interactionManager.deletePost('${postId}')">
              <span class="material-icons">delete</span>
              <span>Delete Post</span>
            </button>
          ` : ''}
          
          ${!isOwnPost ? `
            <button class="action-item" onclick="interactionManager.showReportModal('${postId}')">
              <span class="material-icons">flag</span>
              <span>Report Post</span>
            </button>
          ` : ''}
          
          <button class="action-item" onclick="navigator.share ? navigator.share({title: 'Clara Post', url: window.location.href + '#post-${postId}'}) : interactionManager.copyPostLink('${postId}')">
            <span class="material-icons">share</span>
            <span>Share Post</span>
          </button>
        </div>
      </div>
    `;

    showModal(modalContent);
  }

  async deletePost(postId) {
    const userProfile = authManager.getUserProfile();
    if (!userProfile) return;

    if (!confirm('Are you sure you want to delete this post? This action cannot be undone.')) {
      return;
    }

    try {
      // Delete post
      await deleteDoc(doc(db, COLLECTIONS.POSTS, postId));

      // Delete associated comments
      const commentsQuery = query(
        collection(db, COLLECTIONS.COMMENTS),
        where('postId', '==', postId)
      );
      const commentsSnapshot = await getDocs(commentsQuery);
      const deletePromises = commentsSnapshot.docs.map(doc => deleteDoc(doc.ref));
      await Promise.all(deletePromises);

      // Update user's post count
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userProfile.uid), {
        numPosts: increment(-1)
      });

      showToast('Post deleted successfully', 'success');
      hideModal();
      
      // Refresh feed if we're in the feed manager
      if (window.feedManager) {
        window.feedManager.loadFeed();
      }

    } catch (error) {
      handleError(error, 'Deleting post');
    }
  }

  copyPostLink(postId) {
    const link = `${window.location.origin}${window.location.pathname}#post-${postId}`;
    navigator.clipboard.writeText(link).then(() => {
      showToast('Post link copied to clipboard', 'success');
    }).catch(() => {
      showToast('Could not copy link', 'error');
    });
  }
}

// Make interaction manager available globally
window.interactionManager = new InteractionManager();