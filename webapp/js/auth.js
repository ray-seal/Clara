// Authentication Manager for Clara PWA
import { auth, db, COLLECTIONS } from './config.js';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut, 
  onAuthStateChanged,
  updateProfile,
  sendPasswordResetEmail
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-auth.js';
import { 
  doc, 
  getDoc, 
  setDoc, 
  updateDoc, 
  serverTimestamp 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showToast, handleError } from './utils.js';

export class AuthManager {
  constructor() {
    this.currentUser = null;
    this.userProfile = null;
    this.authStateCallbacks = [];
  }

  initialize() {
    return new Promise((resolve) => {
      onAuthStateChanged(auth, async (user) => {
        this.currentUser = user;
        
        if (user) {
          await this.loadUserProfile(user.uid);
        } else {
          this.userProfile = null;
        }

        // Notify all callbacks
        this.authStateCallbacks.forEach(callback => callback(user, this.userProfile));
        resolve(user);
      });
    });
  }

  async loadUserProfile(userId) {
    try {
      const profileDoc = await getDoc(doc(db, COLLECTIONS.PROFILES, userId));
      if (profileDoc.exists()) {
        this.userProfile = { uid: userId, ...profileDoc.data() };
      } else {
        // Create default profile if it doesn't exist
        this.userProfile = await this.createDefaultProfile(userId);
      }
    } catch (error) {
      console.error('Error loading user profile:', error);
      this.userProfile = null;
    }
  }

  async createDefaultProfile(userId) {
    const user = this.currentUser;
    const defaultProfile = {
      uid: userId,
      email: user.email,
      displayName: user.displayName || user.email.split('@')[0],
      profilePictureUrl: user.photoURL || '',
      bio: '',
      location: '',
      website: '',
      phoneNumber: '',
      isAdmin: false,
      numPosts: 0,
      numFriends: 0,
      isPrivate: false,
      createdAt: serverTimestamp(),
      lastActive: serverTimestamp()
    };

    try {
      await setDoc(doc(db, COLLECTIONS.PROFILES, userId), defaultProfile);
      return defaultProfile;
    } catch (error) {
      console.error('Error creating default profile:', error);
      return null;
    }
  }

  onAuthStateChange(callback) {
    this.authStateCallbacks.push(callback);
    
    // Immediately call with current state
    if (this.currentUser !== null) {
      callback(this.currentUser, this.userProfile);
    }
  }

  async signIn(email, password) {
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      
      // Update last active timestamp
      if (userCredential.user) {
        await updateDoc(doc(db, COLLECTIONS.PROFILES, userCredential.user.uid), {
          lastActive: serverTimestamp()
        });
      }

      showToast('Welcome back!', 'success');
      return userCredential.user;
    } catch (error) {
      handleError(error, 'Sign in');
      return null;
    }
  }

  async signUp(email, password, displayName = '') {
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      // Update profile with display name
      if (displayName) {
        await updateProfile(user, { displayName });
      }

      // Create user profile in Firestore
      await this.createDefaultProfile(user.uid);

      showToast('Account created successfully!', 'success');
      return user;
    } catch (error) {
      handleError(error, 'Sign up');
      return null;
    }
  }

  async signOut() {
    try {
      await signOut(auth);
      this.currentUser = null;
      this.userProfile = null;
      showToast('Signed out successfully', 'success');
    } catch (error) {
      handleError(error, 'Sign out');
    }
  }

  async resetPassword(email) {
    try {
      await sendPasswordResetEmail(auth, email);
      showToast('Password reset email sent!', 'success');
      return true;
    } catch (error) {
      handleError(error, 'Password reset');
      return false;
    }
  }

  getCurrentUser() {
    return this.currentUser;
  }

  getUserProfile() {
    return this.userProfile;
  }

  isSignedIn() {
    return !!this.currentUser;
  }

  isAdmin() {
    return this.userProfile?.isAdmin || false;
  }
}

// Create global auth manager instance
export const authManager = new AuthManager();
window.authManager = authManager;