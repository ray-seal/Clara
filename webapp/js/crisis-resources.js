// Crisis Resources and Safety Features for Clara Mental Health App
import { db, COLLECTIONS } from './config.js';
import { authManager } from './auth.js';
import { 
  doc, 
  addDoc, 
  collection, 
  updateDoc, 
  serverTimestamp,
  query,
  where,
  getDocs 
} from 'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js';
import { showToast } from './utils.js';

export class CrisisResourcesManager {
  constructor() {
    this.crisisKeywords = [
      'suicide', 'kill myself', 'end it all', 'hurt myself', 'self harm',
      'cut myself', 'want to die', 'no point living', 'better off dead',
      'overdose', 'jump off', 'hang myself', 'end my life'
    ];
    
    this.crisisResources = {
      emergency: {
        title: 'Emergency Services',
        number: '911',
        description: 'Call immediately if you are in immediate danger'
      },
      suicide: {
        title: 'National Suicide Prevention Lifeline',
        number: '988',
        description: 'Free, confidential crisis support 24/7',
        chat: 'https://suicidepreventionlifeline.org/chat/',
        text: 'Text HOME to 741741'
      },
      crisis: {
        title: 'Crisis Text Line',
        number: '741741',
        description: 'Text HOME for free crisis support',
        website: 'https://www.crisistextline.org/'
      },
      lgbtq: {
        title: 'Trevor Project (LGBTQ+ Crisis Support)',
        number: '1-866-488-7386',
        description: '24/7 crisis support for LGBTQ+ youth',
        chat: 'https://www.thetrevorproject.org/get-help/',
        text: 'Text START to 678-678'
      },
      veterans: {
        title: 'Veterans Crisis Line',
        number: '1-800-273-8255',
        description: 'Support for veterans and their families',
        chat: 'https://www.veteranscrisisline.net/get-help/chat',
        text: 'Text 838255'
      },
      eating: {
        title: 'National Eating Disorders Association',
        number: '1-800-931-2237',
        description: 'Support for eating disorders',
        chat: 'https://www.nationaleatingdisorders.org/help-support/contact-helpline'
      }
    };
  }

  initialize() {
    // Set up crisis keyword monitoring
    this.setupCrisisDetection();
    console.log('Crisis Resources Manager initialized');
  }

  setupCrisisDetection() {
    // Monitor all text inputs for crisis keywords
    document.addEventListener('input', (event) => {
      if (event.target.matches('textarea, input[type="text"]')) {
        this.scanTextForCrisisKeywords(event.target.value, event.target);
      }
    });
  }

  scanTextForCrisisKeywords(text, element) {
    const lowerText = text.toLowerCase();
    const containsCrisisKeywords = this.crisisKeywords.some(keyword => 
      lowerText.includes(keyword.toLowerCase())
    );

    if (containsCrisisKeywords) {
      this.triggerCrisisIntervention(text, element);
    }
  }

  async triggerCrisisIntervention(text, element = null) {
    const user = authManager.getCurrentUser();
    
    try {
      // Log crisis intervention (anonymized)
      await addDoc(collection(db, 'crisis_interventions'), {
        userId: user?.uid || 'anonymous',
        timestamp: serverTimestamp(),
        type: 'keyword_detection',
        anonymized: !user || authManager.isAnonymous(),
        flaggedText: text ? this.anonymizeText(text) : null,
        status: 'triggered'
      });

      // Show immediate crisis resources
      this.showCrisisModal();

      // Add visual indicator to the input if provided
      if (element) {
        this.addCrisisIndicator(element);
      }

      console.log('Crisis intervention triggered');
    } catch (error) {
      console.error('Error logging crisis intervention:', error);
      // Still show resources even if logging fails
      this.showCrisisModal();
    }
  }

  anonymizeText(text) {
    // Replace personal information with placeholders for privacy
    return text
      .replace(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g, '[EMAIL]')
      .replace(/\b\d{3}-\d{3}-\d{4}\b/g, '[PHONE]')
      .replace(/\b\d{10}\b/g, '[PHONE]')
      .substring(0, 100); // Limit length for privacy
  }

  addCrisisIndicator(element) {
    // Add subtle indicator that crisis resources are available
    element.style.borderLeft = '4px solid #ff6b6b';
    element.title = 'Crisis resources are available - click the help button if you need support';
    
    // Add help button if not already present
    if (!element.parentElement.querySelector('.crisis-help-btn')) {
      const helpBtn = document.createElement('button');
      helpBtn.className = 'crisis-help-btn';
      helpBtn.innerHTML = '🆘 Get Help';
      helpBtn.style.cssText = `
        position: absolute;
        right: 10px;
        top: 50%;
        transform: translateY(-50%);
        background: #ff6b6b;
        color: white;
        border: none;
        padding: 5px 10px;
        border-radius: 15px;
        font-size: 12px;
        cursor: pointer;
        z-index: 1000;
      `;
      helpBtn.onclick = (e) => {
        e.preventDefault();
        this.showCrisisModal();
      };
      
      element.parentElement.style.position = 'relative';
      element.parentElement.appendChild(helpBtn);
    }
  }

  showCrisisModal() {
    // Remove existing modal if present
    const existingModal = document.querySelector('.crisis-modal');
    if (existingModal) {
      existingModal.remove();
    }

    const modal = document.createElement('div');
    modal.className = 'crisis-modal';
    modal.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0,0,0,0.9);
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      box-sizing: border-box;
    `;

    modal.innerHTML = `
      <div style="background: white; border-radius: 20px; max-width: 500px; width: 100%; max-height: 90vh; overflow-y: auto; position: relative;">
        <button class="close-modal" style="position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 24px; cursor: pointer; color: #666;">×</button>
        
        <div style="padding: 30px;">
          <div style="text-align: center; margin-bottom: 25px;">
            <div style="font-size: 48px; margin-bottom: 10px;">🆘</div>
            <h2 style="color: #d63031; margin: 0 0 10px 0;">Crisis Resources</h2>
            <p style="color: #666; margin: 0;">You are not alone. Help is available right now.</p>
          </div>

          <div style="margin-bottom: 20px; padding: 20px; background: #ffe3e3; border-radius: 15px; border-left: 5px solid #d63031;">
            <h3 style="margin: 0 0 10px 0; color: #d63031;">🚨 Emergency</h3>
            <p style="margin: 0 0 10px 0; font-weight: bold;">If you are in immediate danger, call:</p>
            <a href="tel:911" style="font-size: 24px; color: #d63031; text-decoration: none; font-weight: bold;">911</a>
          </div>

          <div style="margin-bottom: 20px; padding: 20px; background: #e3f2fd; border-radius: 15px; border-left: 5px solid #2196F3;">
            <h3 style="margin: 0 0 15px 0; color: #1976D2;">💙 National Suicide Prevention Lifeline</h3>
            <div style="text-align: center; margin-bottom: 15px;">
              <a href="tel:988" style="font-size: 28px; color: #1976D2; text-decoration: none; font-weight: bold; display: block;">988</a>
              <p style="margin: 5px 0; font-size: 14px; color: #666;">Available 24/7, free and confidential</p>
            </div>
            <div style="display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;">
              <a href="https://suicidepreventionlifeline.org/chat/" target="_blank" style="background: #2196F3; color: white; padding: 8px 16px; border-radius: 20px; text-decoration: none; font-size: 14px;">💬 Chat Online</a>
            </div>
          </div>

          <div style="margin-bottom: 20px; padding: 20px; background: #f3e5f5; border-radius: 15px; border-left: 5px solid #9C27B0;">
            <h3 style="margin: 0 0 15px 0; color: #7B1FA2;">💜 Crisis Text Line</h3>
            <div style="text-align: center; margin-bottom: 15px;">
              <p style="margin: 0; font-size: 18px; color: #7B1FA2;">Text <strong>HOME</strong> to</p>
              <a href="sms:741741" style="font-size: 24px; color: #7B1FA2; text-decoration: none; font-weight: bold;">741741</a>
            </div>
          </div>

          <div style="margin-bottom: 25px; padding: 20px; background: #fff3e0; border-radius: 15px; border-left: 5px solid #FF9800;">
            <h3 style="margin: 0 0 15px 0; color: #F57C00;">🏳️‍⚧️ LGBTQ+ Support</h3>
            <p style="margin: 0 0 10px 0; font-weight: bold;">Trevor Project:</p>
            <a href="tel:1-866-488-7386" style="font-size: 18px; color: #F57C00; text-decoration: none; font-weight: bold;">1-866-488-7386</a>
            <p style="margin: 10px 0 0 0; font-size: 14px;">24/7 crisis support for LGBTQ+ youth</p>
          </div>

          <div style="text-align: center; padding-top: 20px; border-top: 1px solid #eee;">
            <button class="im-safe-btn" style="background: #4CAF50; color: white; border: none; padding: 15px 30px; border-radius: 25px; font-size: 16px; font-weight: bold; cursor: pointer; margin-right: 10px;">I'm Safe Now</button>
            <button class="save-resources-btn" style="background: #2196F3; color: white; border: none; padding: 15px 30px; border-radius: 25px; font-size: 16px; font-weight: bold; cursor: pointer;">Save Resources</button>
          </div>

          <div style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 10px; text-align: center;">
            <p style="margin: 0; font-size: 14px; color: #666; font-style: italic;">"Your life has value. You matter. There is hope, even when it doesn't feel like it."</p>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Add event listeners
    modal.querySelector('.close-modal').onclick = () => modal.remove();
    modal.querySelector('.im-safe-btn').onclick = () => {
      this.logSafetyCheck();
      modal.remove();
      showToast('Resources saved to your profile. You can access them anytime.', 'success');
    };
    modal.querySelector('.save-resources-btn').onclick = () => {
      this.saveResourcesToProfile();
      modal.remove();
      showToast('Crisis resources saved to your profile', 'success');
    };

    // Close on background click
    modal.onclick = (e) => {
      if (e.target === modal) modal.remove();
    };
  }

  async saveResourcesToProfile() {
    const user = authManager.getCurrentUser();
    if (!user) return;

    try {
      await addDoc(collection(db, 'saved_resources'), {
        userId: user.uid,
        resourceType: 'crisis_resources',
        resources: this.crisisResources,
        savedAt: serverTimestamp()
      });
    } catch (error) {
      console.error('Error saving resources:', error);
    }
  }

  async logSafetyCheck() {
    const user = authManager.getCurrentUser();
    if (!user) return;

    try {
      await addDoc(collection(db, 'safety_checks'), {
        userId: user.uid,
        status: 'safe',
        timestamp: serverTimestamp(),
        source: 'crisis_modal'
      });
    } catch (error) {
      console.error('Error logging safety check:', error);
    }
  }

  showQuickResourcesButton() {
    // Add floating crisis resources button if not already present
    if (!document.querySelector('.floating-crisis-btn')) {
      const btn = document.createElement('button');
      btn.className = 'floating-crisis-btn';
      btn.innerHTML = '🆘';
      btn.title = 'Crisis Resources - Get help immediately';
      btn.style.cssText = `
        position: fixed;
        bottom: 80px;
        right: 20px;
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(45deg, #ff6b6b, #ee5a52);
        color: white;
        border: none;
        font-size: 24px;
        cursor: pointer;
        box-shadow: 0 4px 20px rgba(255, 107, 107, 0.4);
        z-index: 1000;
        transition: all 0.3s ease;
      `;
      
      btn.onmouseover = () => {
        btn.style.transform = 'scale(1.1)';
        btn.style.boxShadow = '0 6px 25px rgba(255, 107, 107, 0.6)';
      };
      
      btn.onmouseout = () => {
        btn.style.transform = 'scale(1)';
        btn.style.boxShadow = '0 4px 20px rgba(255, 107, 107, 0.4)';
      };
      
      btn.onclick = () => this.showCrisisModal();
      
      document.body.appendChild(btn);
    }
  }

  // Content moderation for posts and messages
  moderateContent(content) {
    const flags = {
      crisis: false,
      inappropriate: false,
      spam: false,
      score: 0
    };

    const lowerContent = content.toLowerCase();

    // Check for crisis keywords
    if (this.crisisKeywords.some(keyword => lowerContent.includes(keyword))) {
      flags.crisis = true;
      flags.score += 10;
    }

    // Check for inappropriate content
    const inappropriateWords = ['hate', 'kill', 'violent', 'threat'];
    if (inappropriateWords.some(word => lowerContent.includes(word))) {
      flags.inappropriate = true;
      flags.score += 5;
    }

    // Check for spam patterns
    if (content.includes('http') && content.length < 50) {
      flags.spam = true;
      flags.score += 3;
    }

    return flags;
  }

  // Initialize wellness check reminders
  setupWellnessChecks() {
    const user = authManager.getCurrentUser();
    if (!user || authManager.isAnonymous()) return;

    // Check if user wants wellness check reminders
    const userProfile = authManager.getUserProfile();
    if (userProfile?.wellnessCheckConsent) {
      setInterval(() => {
        this.showWellnessCheck();
      }, 7 * 24 * 60 * 60 * 1000); // Weekly
    }
  }

  showWellnessCheck() {
    const modal = document.createElement('div');
    modal.className = 'wellness-check-modal';
    modal.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0,0,0,0.8);
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    `;

    modal.innerHTML = `
      <div style="background: white; border-radius: 20px; max-width: 400px; padding: 30px; text-align: center;">
        <div style="font-size: 48px; margin-bottom: 15px;">💙</div>
        <h3 style="margin-bottom: 15px; color: #333;">Weekly Wellness Check</h3>
        <p style="margin-bottom: 25px; color: #666;">How are you feeling this week? Your wellbeing matters to us.</p>
        
        <div style="display: flex; gap: 10px; justify-content: center; margin-bottom: 20px;">
          <button class="mood-btn" data-mood="great" style="padding: 10px; border: none; border-radius: 10px; background: #4CAF50; color: white; cursor: pointer;">😊 Great</button>
          <button class="mood-btn" data-mood="okay" style="padding: 10px; border: none; border-radius: 10px; background: #FFC107; color: white; cursor: pointer;">😐 Okay</button>
          <button class="mood-btn" data-mood="struggling" style="padding: 10px; border: none; border-radius: 10px; background: #FF9800; color: white; cursor: pointer;">😔 Struggling</button>
          <button class="mood-btn" data-mood="crisis" style="padding: 10px; border: none; border-radius: 10px; background: #f44336; color: white; cursor: pointer;">🆘 Crisis</button>
        </div>
        
        <button class="skip-check" style="background: #e0e0e0; color: #666; border: none; padding: 10px 20px; border-radius: 20px; cursor: pointer;">Skip for now</button>
      </div>
    `;

    document.body.appendChild(modal);

    modal.querySelectorAll('.mood-btn').forEach(btn => {
      btn.onclick = () => {
        const mood = btn.dataset.mood;
        this.logWellnessCheck(mood);
        
        if (mood === 'crisis') {
          modal.remove();
          this.showCrisisModal();
        } else {
          modal.remove();
          if (mood === 'struggling') {
            showToast('Remember: You are not alone. Support groups are here for you.', 'info');
          } else {
            showToast('Thank you for checking in! Take care of yourself.', 'success');
          }
        }
      };
    });

    modal.querySelector('.skip-check').onclick = () => modal.remove();
  }

  async logWellnessCheck(mood) {
    const user = authManager.getCurrentUser();
    if (!user) return;

    try {
      await addDoc(collection(db, COLLECTIONS.WELLNESS_CHECKS), {
        userId: user.uid,
        mood: mood,
        timestamp: serverTimestamp(),
        anonymous: authManager.isAnonymous()
      });
    } catch (error) {
      console.error('Error logging wellness check:', error);
    }
  }
}

// Create global crisis resources manager instance
export const crisisResourcesManager = new CrisisResourcesManager();
window.crisisResourcesManager = crisisResourcesManager;