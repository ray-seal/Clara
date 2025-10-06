// Authentication Manager for Clara PWA
import { auth, db, COLLECTIONS } from './config.js';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signInAnonymously,
  linkWithCredential,
  EmailAuthProvider,
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
    const isAnonymous = user.isAnonymous;
    
    const defaultProfile = {
      uid: userId,
      email: user.email || null,
      displayName: user.displayName || (isAnonymous ? 'Anonymous User' : user.email?.split('@')[0] || 'User'),
      profilePictureUrl: user.photoURL || '',
      bio: '',
      location: '',
      website: '',
      phoneNumber: '',
      isAdmin: false,
      isAnonymous: isAnonymous,
      numPosts: 0,
      numConnections: 0, // Changed from numFriends for mental health context
      isPrivate: true, // Default to private for mental health app
      allowDirectMessages: !isAnonymous, // Anonymous users can't receive DMs by default
      showOnlineStatus: false, // Default to hidden for privacy
      createdAt: serverTimestamp(),
      lastActive: serverTimestamp(),
      // Mental health specific fields
      joinedSupportGroups: [],
      crisisContactEnabled: false,
      wellnessCheckConsent: false
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

  async signInAnonymously() {
    try {
      const userCredential = await signInAnonymously(auth);
      showToast('Signed in anonymously - your privacy is protected', 'success');
      return userCredential.user;
    } catch (error) {
      handleError(error, 'Anonymous sign in');
      return null;
    }
  }

  async linkAnonymousAccount(email, password) {
    try {
      if (!this.currentUser || !this.currentUser.isAnonymous) {
        throw new Error('No anonymous user to link');
      }

      const credential = EmailAuthProvider.credential(email, password);
      const userCredential = await linkWithCredential(this.currentUser, credential);
      
      // Update profile to reflect it's no longer anonymous
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userCredential.user.uid), {
        email: email,
        isAnonymous: false,
        allowDirectMessages: true,
        lastActive: serverTimestamp()
      });

      showToast('Account linked successfully! You can now use email/password to sign in.', 'success');
      return userCredential.user;
    } catch (error) {
      handleError(error, 'Link account');
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

  isAnonymous() {
    return this.currentUser?.isAnonymous || false;
  }

  canSendDirectMessages() {
    return this.userProfile?.allowDirectMessages && !this.isAnonymous();
  }

  // Crisis intervention method
  async triggerCrisisIntervention(userId = null) {
    const targetUser = userId || this.currentUser?.uid;
    if (!targetUser) return;

    try {
      // Log crisis intervention trigger
      await setDoc(doc(db, 'crisis_interventions', `${targetUser}_${Date.now()}`), {
        userId: targetUser,
        triggeredAt: serverTimestamp(),
        type: 'automated_detection',
        status: 'pending'
      });

      // Show crisis resources immediately
      this.showCrisisResources();
    } catch (error) {
      console.error('Error triggering crisis intervention:', error);
    }
  }

  showCrisisResources() {
    const crisisModal = document.createElement('div');
    crisisModal.innerHTML = `
      <div style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); z-index: 10000; display: flex; align-items: center; justify-content: center;">
        <div style="background: white; padding: 30px; border-radius: 15px; max-width: 500px; margin: 20px; text-align: center;">
          <h2 style="color: #d63031; margin-bottom: 20px;">🆘 Crisis Resources</h2>
          <p style="margin-bottom: 20px;">If you're having thoughts of self-harm or suicide, please reach out for help immediately:</p>
          <div style="margin: 20px 0; padding: 15px; background: #f8f9fa; border-radius: 10px;">
            <strong>National Suicide Prevention Lifeline</strong><br>
            <a href="tel:988" style="font-size: 24px; color: #d63031;">988</a><br>
            <small>Available 24/7</small>
          </div>
          <div style="margin: 20px 0; padding: 15px; background: #f8f9fa; border-radius: 10px;">
            <strong>Crisis Text Line</strong><br>
            Text <strong>HOME</strong> to <a href="sms:741741" style="color: #d63031;">741741</a>
          </div>
          <button onclick="this.closest('div').remove()" style="background: #00b894; color: white; border: none; padding: 12px 24px; border-radius: 25px; margin-top: 20px; cursor: pointer;">I'm Safe Now</button>
        </div>
      </div>
    `;
    document.body.appendChild(crisisModal);
  }

}

// Create global auth manager instance
export const authManager = new AuthManager();
window.authManager = authManager;