// Mental Health Support Groups Manager for Clara PWA
import { db, COLLECTIONS, APP_CONFIG } from './config.js';
import { authManager } from './auth.js';
import { 
  collection, 
  doc, 
  addDoc, 
  getDoc, 
  getDocs, 
  setDoc, 
  updateDoc, 
  query, 
  where, 
  orderBy, 
  limit, 
  onSnapshot, 
  serverTimestamp,
  arrayUnion,
  arrayRemove 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showToast, handleError, sanitizeContent } from './utils.js';

export class SupportGroupsManager {
  constructor() {
    this.currentGroupId = null;
    this.messagesUnsubscribe = null;
    this.groupsUnsubscribe = null;
    this.joinedGroups = [];
  }

  async initialize() {
    try {
      // Create default support groups if they don't exist
      await this.createDefaultSupportGroups();
      
      // Load user's joined groups
      await this.loadUserGroups();
      
      // Listen for group updates
      this.listenToGroupUpdates();
      
      console.log('Support Groups Manager initialized');
    } catch (error) {
      console.error('Error initializing support groups:', error);
    }
  }

  async createDefaultSupportGroups() {
    try {
      for (const group of APP_CONFIG.SUPPORT_GROUPS) {
        const groupDoc = await getDoc(doc(db, COLLECTIONS.CHAT_ROOMS, group.id));
        
        if (!groupDoc.exists()) {
          await setDoc(doc(db, COLLECTIONS.CHAT_ROOMS, group.id), {
            id: group.id,
            name: group.name,
            description: group.description,
            icon: group.icon,
            color: group.color,
            type: 'support_group',
            isPublic: true,
            requiresApproval: false,
            memberCount: 0,
            messageCount: 0,
            createdAt: serverTimestamp(),
            lastActivity: serverTimestamp(),
            moderators: [],
            rules: [
              'Be respectful and supportive of all members',
              'No medical advice - share experiences only', 
              'Respect privacy - what happens here stays here',
              'No self-promotion or spam',
              'Report harmful content immediately',
              'Use content warnings for sensitive topics'
            ],
            guidelines: {
              supportiveLanguage: true,
              contentWarnings: true,
              noMedicalAdvice: true,
              privacyRespected: true,
              moderatedDiscussion: true
            },
            crisisProtocol: {
              enabled: true,
              autoDetection: true,
              moderatorAlert: true,
              resourcesProvided: true
            }
          });
          
          // Add welcome message for new groups
          await this.addWelcomeMessage(group.id, group.name);
        }
      }
    } catch (error) {
      console.error('Error creating default support groups:', error);
    }
  }

  async addWelcomeMessage(groupId, groupName) {
    try {
      await addDoc(collection(db, COLLECTIONS.MESSAGES), {
        chatRoomId: groupId,
        senderId: 'system',
        senderName: 'Clara Support Bot',
        content: `Welcome to ${groupName}! 🌟\n\nThis is a safe space for peer support and understanding. Please read our community guidelines and remember:\n\n• Share your experiences, not medical advice\n• Be kind and supportive\n• Respect everyone's privacy\n• Use content warnings when needed\n\nIf you're in crisis, please contact emergency services or call 988 (Suicide Prevention Lifeline) immediately. 💙`,
        type: 'system',
        timestamp: serverTimestamp(),
        edited: false,
        reactions: {},
        contentWarning: null
      });
    } catch (error) {
      console.error('Error adding welcome message:', error);
    }
  }

  async loadUserGroups() {
    if (!authManager.isSignedIn()) return;

    try {
      const userProfile = authManager.getUserProfile();
      this.joinedGroups = userProfile?.joinedSupportGroups || [];
    } catch (error) {
      console.error('Error loading user groups:', error);
    }
  }

  async joinSupportGroup(groupId) {
    if (!authManager.isSignedIn()) {
      showToast('Please sign in to join support groups', 'warning');
      return false;
    }

    try {
      const userId = authManager.getCurrentUser().uid;
      const userProfile = authManager.getUserProfile();

      // Check if already joined
      if (this.joinedGroups.includes(groupId)) {
        showToast('You are already a member of this group', 'info');
        return true;
      }

      // Add user to group
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userId), {
        joinedSupportGroups: arrayUnion(groupId),
        lastActive: serverTimestamp()
      });

      // Update group member count
      const groupRef = doc(db, COLLECTIONS.CHAT_ROOMS, groupId);
      const groupDoc = await getDoc(groupRef);
      if (groupDoc.exists()) {
        await updateDoc(groupRef, {
          memberCount: (groupDoc.data().memberCount || 0) + 1,
          lastActivity: serverTimestamp()
        });
      }

      // Add join message
      await addDoc(collection(db, COLLECTIONS.MESSAGES), {
        chatRoomId: groupId,
        senderId: 'system',
        senderName: 'Clara Support Bot',
        content: `${userProfile.displayName} joined the support group. Welcome! 💙`,
        type: 'join',
        timestamp: serverTimestamp(),
        edited: false,
        reactions: {},
        contentWarning: null
      });

      this.joinedGroups.push(groupId);
      showToast('Successfully joined support group!', 'success');
      return true;
    } catch (error) {
      handleError(error, 'Join support group');
      return false;
    }
  }

  async leaveSupportGroup(groupId) {
    if (!authManager.isSignedIn()) return false;

    try {
      const userId = authManager.getCurrentUser().uid;
      const userProfile = authManager.getUserProfile();

      // Remove user from group
      await updateDoc(doc(db, COLLECTIONS.PROFILES, userId), {
        joinedSupportGroups: arrayRemove(groupId),
        lastActive: serverTimestamp()
      });

      // Update group member count
      const groupRef = doc(db, COLLECTIONS.CHAT_ROOMS, groupId);
      const groupDoc = await getDoc(groupRef);
      if (groupDoc.exists()) {
        await updateDoc(groupRef, {
          memberCount: Math.max((groupDoc.data().memberCount || 1) - 1, 0),
          lastActivity: serverTimestamp()
        });
      }

      // Add leave message
      await addDoc(collection(db, COLLECTIONS.MESSAGES), {
        chatRoomId: groupId,
        senderId: 'system',
        senderName: 'Clara Support Bot',
        content: `${userProfile.displayName} left the support group. Take care! 💙`,
        type: 'leave',
        timestamp: serverTimestamp(),
        edited: false,
        reactions: {},
        contentWarning: null
      });

      this.joinedGroups = this.joinedGroups.filter(id => id !== groupId);
      showToast('Left support group', 'info');
      return true;
    } catch (error) {
      handleError(error, 'Leave support group');
      return false;
    }
  }

  async sendMessage(groupId, content, contentWarning = null) {
    if (!authManager.isSignedIn()) {
      showToast('Please sign in to send messages', 'warning');
      return false;
    }

    if (!this.joinedGroups.includes(groupId)) {
      showToast('You must join this support group to send messages', 'warning');
      return false;
    }

    try {
      const user = authManager.getCurrentUser();
      const userProfile = authManager.getUserProfile();
      
      // Sanitize content and check for crisis keywords
      const sanitizedContent = sanitizeContent(content);
      const containsCrisisKeywords = this.checkForCrisisKeywords(sanitizedContent);
      
      if (containsCrisisKeywords) {
        await authManager.triggerCrisisIntervention();
      }

      const messageData = {
        chatRoomId: groupId,
        senderId: user.uid,
        senderName: userProfile.displayName,
        senderIsAnonymous: userProfile.isAnonymous || false,
        content: sanitizedContent,
        type: 'message',
        timestamp: serverTimestamp(),
        edited: false,
        reactions: {},
        contentWarning: contentWarning,
        flagged: containsCrisisKeywords,
        supportive: this.assessSupportiveContent(sanitizedContent)
      };

      await addDoc(collection(db, COLLECTIONS.MESSAGES), messageData);

      // Update group last activity
      await updateDoc(doc(db, COLLECTIONS.CHAT_ROOMS, groupId), {
        lastActivity: serverTimestamp(),
        messageCount: (await getDoc(doc(db, COLLECTIONS.CHAT_ROOMS, groupId))).data().messageCount + 1
      });

      return true;
    } catch (error) {
      handleError(error, 'Send message');
      return false;
    }
  }

  checkForCrisisKeywords(content) {
    const lowerContent = content.toLowerCase();
    return APP_CONFIG.CRISIS_KEYWORDS.some(keyword => 
      lowerContent.includes(keyword.toLowerCase())
    );
  }

  assessSupportiveContent(content) {
    const supportiveWords = [
      'support', 'here for you', 'proud of you', 'you matter', 'not alone',
      'strength', 'courage', 'hope', 'healing', 'recovery', 'better',
      'care', 'love', 'understanding', 'empathy', 'listen'
    ];
    
    const lowerContent = content.toLowerCase();
    return supportiveWords.some(word => lowerContent.includes(word));
  }

  listenToMessages(groupId, callback) {
    if (this.messagesUnsubscribe) {
      this.messagesUnsubscribe();
    }

    const messagesQuery = query(
      collection(db, COLLECTIONS.MESSAGES),
      where('chatRoomId', '==', groupId),
      orderBy('timestamp', 'desc'),
      limit(50)
    );

    this.messagesUnsubscribe = onSnapshot(messagesQuery, (snapshot) => {
      const messages = [];
      snapshot.forEach((doc) => {
        messages.push({ id: doc.id, ...doc.data() });
      });
      
      // Reverse to show oldest first
      messages.reverse();
      callback(messages);
    });

    this.currentGroupId = groupId;
  }

  listenToGroupUpdates() {
    if (this.groupsUnsubscribe) {
      this.groupsUnsubscribe();
    }

    const groupsQuery = query(
      collection(db, COLLECTIONS.CHAT_ROOMS),
      where('type', '==', 'support_group'),
      orderBy('memberCount', 'desc')
    );

    this.groupsUnsubscribe = onSnapshot(groupsQuery, (snapshot) => {
      const groups = [];
      snapshot.forEach((doc) => {
        groups.push({ id: doc.id, ...doc.data() });
      });
      
      // Emit custom event for UI updates
      window.dispatchEvent(new CustomEvent('supportGroupsUpdated', { 
        detail: { groups, joinedGroups: this.joinedGroups } 
      }));
    });
  }

  async getSupportGroups() {
    try {
      const groupsQuery = query(
        collection(db, COLLECTIONS.CHAT_ROOMS),
        where('type', '==', 'support_group'),
        orderBy('memberCount', 'desc')
      );
      
      const snapshot = await getDocs(groupsQuery);
      const groups = [];
      snapshot.forEach((doc) => {
        groups.push({ id: doc.id, ...doc.data() });
      });
      
      return groups;
    } catch (error) {
      console.error('Error getting support groups:', error);
      return [];
    }
  }

  getJoinedGroups() {
    return this.joinedGroups;
  }

  isJoined(groupId) {
    return this.joinedGroups.includes(groupId);
  }

  cleanup() {
    if (this.messagesUnsubscribe) {
      this.messagesUnsubscribe();
    }
    if (this.groupsUnsubscribe) {
      this.groupsUnsubscribe();
    }
  }
}

// Create global support groups manager instance
export const supportGroupsManager = new SupportGroupsManager();
window.supportGroupsManager = supportGroupsManager;