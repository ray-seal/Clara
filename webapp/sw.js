// Service Worker for Clara PWA
const CACHE_NAME = 'clara-v1.0.0';
const STATIC_CACHE_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/css/styles.css',
  '/js/app.js',
  '/js/auth.js',
  '/js/config.js',
  '/js/feed.js',
  '/js/interactions.js',
  '/js/utils.js',
  '/js/friends.js',
  '/js/profile.js',
  '/js/notifications.js',
  '/icons/icon-72x72.png',
  '/icons/icon-96x96.png',
  '/icons/icon-128x128.png',
  '/icons/icon-144x144.png',
  '/icons/icon-152x152.png',
  '/icons/icon-192x192.png',
  '/icons/icon-384x384.png',
  '/icons/icon-512x512.png',
  'https://fonts.googleapis.com/icon?family=Material+Icons',
  'https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap'
];

// Firebase URLs that should be cached
const FIREBASE_CACHE_URLS = [
  'https://www.gstatic.com/firebasejs/10.3.0/firebase-app.js',
  'https://www.gstatic.com/firebasejs/10.3.0/firebase-auth.js',
  'https://www.gstatic.com/firebasejs/10.3.0/firebase-firestore.js',
  'https://www.gstatic.com/firebasejs/10.3.0/firebase-storage.js',
  'https://www.gstatic.com/firebasejs/10.3.0/firebase-messaging.js'
];

// Install event
self.addEventListener('install', (event) => {
  console.log('Service Worker: Installing...');
  
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Service Worker: Caching files');
        return cache.addAll([...STATIC_CACHE_URLS, ...FIREBASE_CACHE_URLS]);
      })
      .then(() => {
        console.log('Service Worker: Cached all files');
        return self.skipWaiting();
      })
      .catch((error) => {
        console.error('Service Worker: Cache failed', error);
      })
  );
});

// Activate event
self.addEventListener('activate', (event) => {
  console.log('Service Worker: Activating...');
  
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME) {
              console.log('Service Worker: Deleting old cache', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        console.log('Service Worker: Activated');
        return self.clients.claim();
      })
  );
});

// Fetch event
self.addEventListener('fetch', (event) => {
  // Skip non-GET requests
  if (event.request.method !== 'GET') {
    return;
  }

  // Skip Chrome extension requests
  if (event.request.url.startsWith('chrome-extension://')) {
    return;
  }

  // Skip Firebase Auth requests (they need to be fresh)
  if (event.request.url.includes('identitytoolkit.googleapis.com') || 
      event.request.url.includes('securetoken.googleapis.com')) {
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then((cachedResponse) => {
        // Return cached version if available
        if (cachedResponse) {
          // For HTML files, try to fetch fresh version in background
          if (event.request.destination === 'document') {
            fetch(event.request)
              .then((response) => {
                if (response.ok) {
                  caches.open(CACHE_NAME)
                    .then((cache) => cache.put(event.request, response.clone()));
                }
              })
              .catch(() => {
                // Ignore network errors in background fetch
              });
          }
          return cachedResponse;
        }

        // Try to fetch from network
        return fetch(event.request)
          .then((response) => {
            // Don't cache failed responses
            if (!response.ok) {
              return response;
            }

            // Clone the response
            const responseToCache = response.clone();

            // Cache static assets and API responses
            if (shouldCache(event.request)) {
              caches.open(CACHE_NAME)
                .then((cache) => {
                  cache.put(event.request, responseToCache);
                });
            }

            return response;
          })
          .catch(() => {
            // Network failed, try to return offline page for navigations
            if (event.request.destination === 'document') {
              return caches.match('/index.html');
            }
            
            // For other requests, throw the error
            throw new Error('Network error and no cache available');
          });
      })
  );
});

// Helper function to determine if a request should be cached
function shouldCache(request) {
  const url = new URL(request.url);
  
  // Cache same-origin requests
  if (url.origin === location.origin) {
    return true;
  }
  
  // Cache specific external resources
  if (url.hostname === 'fonts.googleapis.com' ||
      url.hostname === 'fonts.gstatic.com' ||
      url.hostname === 'www.gstatic.com') {
    return true;
  }
  
  return false;
}

// Background sync for offline actions
self.addEventListener('sync', (event) => {
  console.log('Service Worker: Background sync', event.tag);
  
  if (event.tag === 'background-sync-posts') {
    event.waitUntil(syncPosts());
  }
  
  if (event.tag === 'background-sync-likes') {
    event.waitUntil(syncLikes());
  }
  
  if (event.tag === 'background-sync-comments') {
    event.waitUntil(syncComments());
  }
});

// Sync pending posts when back online
async function syncPosts() {
  try {
    const pendingPosts = await getFromIndexedDB('pendingPosts');
    
    for (const post of pendingPosts) {
      try {
        // Try to submit the post
        await submitPost(post);
        
        // Remove from pending if successful
        await removeFromIndexedDB('pendingPosts', post.id);
        
        // Notify the client
        self.clients.matchAll().then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'POST_SYNCED',
              data: post
            });
          });
        });
        
      } catch (error) {
        console.error('Failed to sync post:', error);
      }
    }
  } catch (error) {
    console.error('Background sync failed:', error);
  }
}

// Sync pending likes when back online
async function syncLikes() {
  try {
    const pendingLikes = await getFromIndexedDB('pendingLikes');
    
    for (const like of pendingLikes) {
      try {
        await submitLike(like);
        await removeFromIndexedDB('pendingLikes', like.id);
        
        self.clients.matchAll().then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'LIKE_SYNCED',
              data: like
            });
          });
        });
        
      } catch (error) {
        console.error('Failed to sync like:', error);
      }
    }
  } catch (error) {
    console.error('Like sync failed:', error);
  }
}

// Sync pending comments when back online
async function syncComments() {
  try {
    const pendingComments = await getFromIndexedDB('pendingComments');
    
    for (const comment of pendingComments) {
      try {
        await submitComment(comment);
        await removeFromIndexedDB('pendingComments', comment.id);
        
        self.clients.matchAll().then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'COMMENT_SYNCED',
              data: comment
            });
          });
        });
        
      } catch (error) {
        console.error('Failed to sync comment:', error);
      }
    }
  } catch (error) {
    console.error('Comment sync failed:', error);
  }
}

// IndexedDB helpers for offline storage
function getFromIndexedDB(storeName) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('ClaraOfflineDB', 1);
    
    request.onerror = () => reject(request.error);
    
    request.onsuccess = () => {
      const db = request.result;
      const transaction = db.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const getAllRequest = store.getAll();
      
      getAllRequest.onsuccess = () => resolve(getAllRequest.result);
      getAllRequest.onerror = () => reject(getAllRequest.error);
    };
    
    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains(storeName)) {
        db.createObjectStore(storeName, { keyPath: 'id' });
      }
    };
  });
}

function removeFromIndexedDB(storeName, id) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('ClaraOfflineDB', 1);
    
    request.onerror = () => reject(request.error);
    
    request.onsuccess = () => {
      const db = request.result;
      const transaction = db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);
      const deleteRequest = store.delete(id);
      
      deleteRequest.onsuccess = () => resolve();
      deleteRequest.onerror = () => reject(deleteRequest.error);
    };
  });
}

// Placeholder functions for actual API calls
async function submitPost(post) {
  // This would make actual Firebase calls
  console.log('Syncing post:', post);
}

async function submitLike(like) {
  // This would make actual Firebase calls
  console.log('Syncing like:', like);
}

async function submitComment(comment) {
  // This would make actual Firebase calls
  console.log('Syncing comment:', comment);
}

// Handle share target
self.addEventListener('fetch', (event) => {
  if (event.request.url.includes('/share-target') && event.request.method === 'POST') {
    event.respondWith(handleShareTarget(event.request));
  }
});

async function handleShareTarget(request) {
  const formData = await request.formData();
  const title = formData.get('title') || '';
  const text = formData.get('text') || '';
  const url = formData.get('url') || '';
  const image = formData.get('image');

  // Store shared data for the app to pick up
  const sharedData = {
    title,
    text,
    url,
    image: image ? await imageToBase64(image) : null,
    timestamp: Date.now()
  };

  // Store in IndexedDB or localStorage
  await storeSharedData(sharedData);

  // Redirect to app with share parameter
  return Response.redirect('/?shared=true', 302);
}

async function imageToBase64(file) {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsDataURL(file);
  });
}

async function storeSharedData(data) {
  // Store shared data for the app to retrieve
  if ('localStorage' in self) {
    localStorage.setItem('sharedData', JSON.stringify(data));
  }
}

// Push notification handling
self.addEventListener('push', (event) => {
  console.log('Service Worker: Push received');
  
  let notificationData = {};
  
  if (event.data) {
    try {
      notificationData = event.data.json();
    } catch (error) {
      notificationData = {
        title: 'Clara',
        body: event.data.text() || 'You have a new notification',
        icon: '/icons/icon-192x192.png',
        badge: '/icons/icon-72x72.png'
      };
    }
  }

  const notificationOptions = {
    title: notificationData.title || 'Clara',
    body: notificationData.body || 'You have a new notification',
    icon: notificationData.icon || '/icons/icon-192x192.png',
    badge: notificationData.badge || '/icons/icon-72x72.png',
    tag: notificationData.tag || 'default',
    data: notificationData.data || {},
    actions: notificationData.actions || [
      {
        action: 'open',
        title: 'Open App'
      },
      {
        action: 'dismiss',
        title: 'Dismiss'
      }
    ],
    requireInteraction: notificationData.requireInteraction || false,
    silent: notificationData.silent || false
  };

  event.waitUntil(
    self.registration.showNotification(notificationOptions.title, notificationOptions)
  );
});

// Notification click handling
self.addEventListener('notificationclick', (event) => {
  console.log('Service Worker: Notification clicked', event);
  
  event.notification.close();

  if (event.action === 'dismiss') {
    return;
  }

  // Default action or 'open' action
  event.waitUntil(
    clients.matchAll({ type: 'window' })
      .then((clientList) => {
        // Try to focus existing window
        for (const client of clientList) {
          if (client.url === self.location.origin && 'focus' in client) {
            return client.focus();
          }
        }
        
        // Open new window if none exists
        if (clients.openWindow) {
          let url = '/';
          
          // Handle specific notification types
          if (event.notification.data) {
            const data = event.notification.data;
            
            if (data.postId) {
              url = `/?post=${data.postId}`;
            } else if (data.chatId) {
              url = `/?chat=${data.chatId}`;
            } else if (data.userId) {
              url = `/?user=${data.userId}`;
            }
          }
          
          return clients.openWindow(url);
        }
      })
  );
});

// Message handling from main app
self.addEventListener('message', (event) => {
  console.log('Service Worker: Message received', event.data);
  
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
  
  if (event.data && event.data.type === 'CACHE_INVALIDATE') {
    // Invalidate specific cache entries
    caches.open(CACHE_NAME).then((cache) => {
      if (event.data.urls) {
        event.data.urls.forEach(url => cache.delete(url));
      }
    });
  }
});

console.log('Service Worker: Loaded');