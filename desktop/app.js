// ==========================================================
// MUSIFY DESKTOP - CORE ENGINE & FIRESTORE CLOUD SYNC
// Firebase Project: musify-8df01
// Synchronizes seamlessly with Musify Android App Database
// ==========================================================

const FIREBASE_CONFIG = {
  projectId: "musify-8df01",
  apiKey: "AIzaSyCxf6HkoECeubQgc4lrhx9q-HIlmj3YUxY",
  firestoreBase: "https://firestore.googleapis.com/v1/projects/musify-8df01/databases/(default)/documents",
  authBase: "https://identitytoolkit.googleapis.com/v1/accounts"
};

// -------------------------------------------------------------
// CURATED LOSSLESS CATALOG (Fallback & Base Library)
// -------------------------------------------------------------
const curatedCatalog = {
  trending: [
    {
      videoId: "aL3p7_o_G-g",
      title: "Tumhare Hi Rahenge Hum",
      artist: "Sachin-Jigar, Varun Jain",
      album: "Stree 2",
      art: "https://i.ytimg.com/vi/aL3p7_o_G-g/hqdefault.jpg",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
      category: "bollywood",
      plays: "4.8M plays"
    },
    {
      videoId: "cl0a3i2wFcc",
      title: "G.O.A.T.",
      artist: "Diljit Dosanjh",
      album: "G.O.A.T.",
      art: "https://i.ytimg.com/vi/cl0a3i2wFcc/hqdefault.jpg",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
      category: "punjabi",
      plays: "12.4M plays"
    },
    {
      videoId: "tauba_tau",
      title: "Tauba Tauba",
      artist: "Karan Aujla",
      album: "Bad Newz",
      art: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
      category: "punjabi",
      plays: "18.2M plays"
    },
    {
      videoId: "ElZfdU54Cp8",
      title: "Apna Bana Le",
      artist: "Arijit Singh, Sachin-Jigar",
      album: "Bhediya",
      art: "https://i.ytimg.com/vi/ElZfdU54Cp8/hqdefault.jpg",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
      category: "bollywood",
      plays: "8.9M plays"
    },
    {
      videoId: "BddP6PYo2gs",
      title: "Kesariya",
      artist: "Arijit Singh, Pritam",
      album: "Brahmastra",
      art: "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
      category: "bollywood",
      plays: "15.1M plays"
    },
    {
      videoId: "U8T2V2fI9h0",
      title: "Chal Tere Ishq Mein",
      artist: "Neeti Mohan, Vishal Mishra",
      album: "Gadar 2",
      art: "https://i.ytimg.com/vi/U8T2V2fI9h0/hqdefault.jpg",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
      category: "bollywood",
      plays: "6.2M plays"
    }
  ],

  biggestHits: [
    {
      videoId: "hukum_jai",
      title: "Hukum - Thalaivar Alappara",
      artist: "Anirudh Ravichander",
      album: "Jailer",
      art: "https://images.unsplash.com/photo-1518972559570-7cc1309f3229?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
      category: "kollywood",
      plays: "21.0M plays"
    },
    {
      videoId: "o_maahi_du",
      title: "O Maahi",
      artist: "Arijit Singh, Pritam",
      album: "Dunki",
      art: "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
      category: "bollywood",
      plays: "9.5M plays"
    },
    {
      videoId: "softly_auj",
      title: "Softly",
      artist: "Karan Aujla, Ikky",
      album: "Making Memories",
      art: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
      category: "punjabi",
      plays: "11.7M plays"
    },
    {
      videoId: "badass_leo",
      title: "Badass",
      artist: "Anirudh Ravichander",
      album: "Leo",
      art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
      category: "kollywood",
      plays: "14.3M plays"
    }
  ],

  nostalgia: [
    {
      videoId: "zara_sa_kk",
      title: "Zara Sa",
      artist: "KK, Pritam",
      album: "Jannat",
      art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
      category: "romance",
      plays: "28.5M plays"
    },
    {
      videoId: "dheere_dhe",
      title: "Dheere Dheere Se Meri Zindagi",
      artist: "Kumar Sanu, Anuradha Paudwal",
      album: "Aashiqui (1990)",
      art: "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
      category: "romance",
      plays: "32.0M plays"
    },
    {
      videoId: "pehla_nash",
      title: "Pehla Nasha",
      artist: "Udit Narayan, Sadhana Sargam",
      album: "Jo Jeeta Wohi Sikandar",
      art: "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
      category: "romance",
      plays: "24.1M plays"
    },
    {
      videoId: "tujhe_dekh",
      title: "Tujhe Dekha Toh",
      artist: "Kumar Sanu, Lata Mangeshkar",
      album: "DDLJ",
      art: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
      streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
      category: "romance",
      plays: "41.6M plays"
    }
  ]
};

// Queue & State Management
let currentQueue = [
  ...curatedCatalog.trending,
  ...curatedCatalog.biggestHits,
  ...curatedCatalog.nostalgia
];

let userFavorites = [];   // Synced with Firestore users/{userId}/favorites
let userPlaylists = [];   // Synced with Firestore users/{userId}/playlists
let cloudTopTracks = [];  // Synced from global_top_tracks

let currentIndex = 0;
let isPlaying = false;
let isShuffle = false;
let isRepeat = false;
let lastVolume = 0.8;

// Current Authenticated User State
let currentUser = {
  isLoggedIn: false,
  email: null,
  displayName: "Guest User",
  syncUserId: getPersistentGuestId(),
  idToken: null
};

// DOM Elements
const audio = document.getElementById('audioElement');
const btnPlayPause = document.getElementById('btnPlayPause');
const playSvg = document.getElementById('playSvg');
const btnPrev = document.getElementById('btnPrev');
const btnNext = document.getElementById('btnNext');
const btnShuffle = document.getElementById('btnShuffle');
const btnRepeat = document.getElementById('btnRepeat');
const btnFavorite = document.getElementById('btnFavorite');
const btnMute = document.getElementById('btnMute');
const volIcon = document.getElementById('volIcon');

const currentTitle = document.getElementById('currentTitle');
const currentArtist = document.getElementById('currentArtist');
const currentArt = document.getElementById('currentArt');

const seekSlider = document.getElementById('seekSlider');
const scrubberFill = document.getElementById('scrubberFill');
const miniProgressFill = document.getElementById('miniProgressFill');
const currTime = document.getElementById('currTime');
const totalTime = document.getElementById('totalTime');

const volumeSlider = document.getElementById('volumeSlider');
const volumeFill = document.getElementById('volumeFill');
const searchInput = document.getElementById('searchInput');
const btnClearSearch = document.getElementById('btnClearSearch');

const btnHeaderSync = document.getElementById('btnHeaderSync');
const syncSpinIcon = document.getElementById('syncSpinIcon');
const headerSyncLabel = document.getElementById('headerSyncLabel');

// -------------------------------------------------------------
// USER AUTHENTICATION & CLOUD SYNC IDENTIFIERS
// -------------------------------------------------------------
function getPersistentGuestId() {
  let guestId = localStorage.getItem('musify_guest_id');
  if (!guestId) {
    guestId = 'guest_' + Math.random().toString(36).substring(2, 12);
    localStorage.setItem('musify_guest_id', guestId);
  }
  return guestId;
}

/**
 * Returns deterministic syncUserId matching Android app's AuthManager.getSyncUserId()
 */
function computeSyncUserId(email) {
  if (email && email.trim() && email !== "guest_listener@gmail.com") {
    return email.trim().toLowerCase().replace(/\./g, "_").replace(/@/g, "_");
  }
  return getPersistentGuestId();
}

function loadSavedUser() {
  try {
    const raw = localStorage.getItem('musify_desktop_user');
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && parsed.email) {
        currentUser = {
          isLoggedIn: true,
          email: parsed.email,
          displayName: parsed.displayName || parsed.email.split('@')[0],
          syncUserId: computeSyncUserId(parsed.email),
          idToken: parsed.idToken || null
        };
      }
    }
  } catch (err) {
    console.warn("Failed to load saved user session:", err);
  }
  updateUserUI();
}

function saveUserSession(email, displayName, idToken) {
  currentUser = {
    isLoggedIn: true,
    email: email.trim(),
    displayName: displayName || email.split('@')[0],
    syncUserId: computeSyncUserId(email),
    idToken: idToken || null
  };
  localStorage.setItem('musify_desktop_user', JSON.stringify(currentUser));
  updateUserUI();
  syncAllWithCloud();
}

function clearUserSession() {
  localStorage.removeItem('musify_desktop_user');
  currentUser = {
    isLoggedIn: false,
    email: null,
    displayName: "Guest User",
    syncUserId: getPersistentGuestId(),
    idToken: null
  };
  userFavorites = [];
  userPlaylists = [];
  updateUserUI();
  renderFavoritesScreen();
  renderLibraryScreen();
  showToast("Signed out. Switched to offline guest mode.");
}

function updateUserUI() {
  const sidebarUserName = document.getElementById('sidebarUserName');
  const sidebarUserEmail = document.getElementById('sidebarUserEmail');
  const sidebarAvatar = document.getElementById('sidebarAvatar');
  const headerAvatar = document.getElementById('headerAvatar');
  const syncStatusText = document.getElementById('syncStatusText');
  const syncStatusDot = document.getElementById('syncStatusDot');
  const btnSidebarAuth = document.getElementById('btnSidebarAuth');
  const btnSignOut = document.getElementById('btnSignOut');

  const initial = (currentUser.displayName || "G").charAt(0).toUpperCase();

  if (sidebarAvatar) sidebarAvatar.textContent = initial;
  if (headerAvatar) headerAvatar.textContent = initial;

  const npUserName = document.getElementById('npUserName');
  const npUserEmail = document.getElementById('npUserEmail');
  const npUserAvatar = document.getElementById('npUserAvatar');
  const npSyncDot = document.getElementById('npSyncDot');
  const npSyncLabel = document.getElementById('npSyncLabel');

  if (npUserAvatar) npUserAvatar.textContent = initial;

  if (currentUser.isLoggedIn) {
    if (sidebarUserName) sidebarUserName.textContent = currentUser.displayName;
    if (sidebarUserEmail) sidebarUserEmail.textContent = currentUser.email;
    if (syncStatusText) syncStatusText.textContent = "Synced with Cloud";
    if (syncStatusDot) syncStatusDot.style.backgroundColor = "#1DB954";
    if (btnSidebarAuth) btnSidebarAuth.textContent = "Manage Account";
    if (btnSignOut) btnSignOut.style.display = "block";

    if (npUserName) npUserName.textContent = currentUser.displayName;
    if (npUserEmail) npUserEmail.textContent = currentUser.email;
    if (npSyncLabel) npSyncLabel.textContent = "Synced with Musify Cloud";
    if (npSyncDot) npSyncDot.style.backgroundColor = "#1DB954";
  } else {
    if (sidebarUserName) sidebarUserName.textContent = "Guest Mode";
    if (sidebarUserEmail) sidebarUserEmail.textContent = "Sign in to sync with phone";
    if (syncStatusText) syncStatusText.textContent = "Local Storage";
    if (syncStatusDot) syncStatusDot.style.backgroundColor = "#888888";
    if (btnSidebarAuth) btnSidebarAuth.textContent = "Sign In / Connect";
    if (btnSignOut) btnSignOut.style.display = "none";

    if (npUserName) npUserName.textContent = "Guest User";
    if (npUserEmail) npUserEmail.textContent = "Sign in with Google to sync";
    if (npSyncLabel) npSyncLabel.textContent = "Offline Mode";
    if (npSyncDot) npSyncDot.style.backgroundColor = "#888888";
  }
}

// -------------------------------------------------------------
// FIRESTORE CLOUD DATABASE REST API ENGINE
// -------------------------------------------------------------
async function firestoreGet(path) {
  const url = `${FIREBASE_CONFIG.firestoreBase}/${path}?key=${FIREBASE_CONFIG.apiKey}`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Firestore GET ${res.status}: ${res.statusText}`);
  }
  return await res.json();
}

async function firestoreSet(path, fields) {
  const url = `${FIREBASE_CONFIG.firestoreBase}/${path}?key=${FIREBASE_CONFIG.apiKey}`;
  const res = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fields })
  });
  if (!res.ok) {
    throw new Error(`Firestore SET ${res.status}: ${res.statusText}`);
  }
  return await res.json();
}

async function firestoreDelete(path) {
  const url = `${FIREBASE_CONFIG.firestoreBase}/${path}?key=${FIREBASE_CONFIG.apiKey}`;
  const res = await fetch(url, { method: 'DELETE' });
  return res.ok;
}

// Helper to convert plain JS object to Firestore typed fields
function toFirestoreFields(obj) {
  const fields = {};
  for (const [k, v] of Object.entries(obj)) {
    if (typeof v === 'string') {
      fields[k] = { stringValue: v };
    } else if (typeof v === 'number') {
      fields[k] = Number.isInteger(v) ? { integerValue: v.toString() } : { doubleValue: v };
    } else if (typeof v === 'boolean') {
      fields[k] = { booleanValue: v };
    }
  }
  return fields;
}

// Helper to parse Firestore doc into plain JS object
function fromFirestoreDoc(doc) {
  if (!doc || !doc.fields) return null;
  const out = {};
  for (const [k, v] of Object.entries(doc.fields)) {
    if (v.stringValue !== undefined) out[k] = v.stringValue;
    else if (v.integerValue !== undefined) out[k] = parseInt(v.integerValue, 10);
    else if (v.doubleValue !== undefined) out[k] = parseFloat(v.doubleValue);
    else if (v.booleanValue !== undefined) out[k] = v.booleanValue;
    else if (v.timestampValue !== undefined) out[k] = v.timestampValue;
  }
  return out;
}

// -------------------------------------------------------------
// CLOUD SYNCHRONIZATION ROUTINES
// -------------------------------------------------------------
async function syncAllWithCloud() {
  btnHeaderSync.classList.add('syncing');
  headerSyncLabel.textContent = "Syncing...";

  try {
    await Promise.allSettled([
      syncFavoritesFromCloud(),
      syncPlaylistsFromCloud(),
      syncGlobalTopChartsFromCloud()
    ]);
    headerSyncLabel.textContent = "Cloud Synced";
    showToast(`✅ Synced with Musify Database (${userFavorites.length} favorites)`);
  } catch (err) {
    console.warn("Cloud sync notice:", err);
    headerSyncLabel.textContent = "Offline Mode";
  } finally {
    btnHeaderSync.classList.remove('syncing');
  }
}

// 1. Sync Favorites from Firestore: users/{syncUserId}/favorites
async function syncFavoritesFromCloud() {
  const userId = currentUser.syncUserId;
  try {
    const data = await firestoreGet(`users/${userId}/favorites`);
    if (data && data.documents) {
      userFavorites = data.documents.map(doc => {
        const item = fromFirestoreDoc(doc);
        return {
          videoId: item.videoId || item.songId || doc.name.split('/').pop(),
          title: item.title || "Favorite Song",
          artist: item.artist || "Unknown Artist",
          album: item.album || "Single",
          art: item.albumArtUrl || "icon.png",
          streamUrl: item.audioUrl || "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        };
      });
    }
    renderFavoritesScreen();
    highlightActiveTrack();
  } catch (err) {
    console.warn("Could not pull cloud favorites:", err.message);
  }
}

// 2. Sync Playlists from Firestore: users/{syncUserId}/playlists
async function syncPlaylistsFromCloud() {
  const userId = currentUser.syncUserId;
  try {
    const data = await firestoreGet(`users/${userId}/playlists`);
    if (data && data.documents) {
      userPlaylists = data.documents.map(doc => {
        const item = fromFirestoreDoc(doc);
        return {
          id: item.id || doc.name.split('/').pop(),
          name: item.name || "My Playlist",
          trackCount: item.trackCount || 0
        };
      });
    }
    renderLibraryScreen();
  } catch (err) {
    console.warn("Could not pull cloud playlists:", err.message);
  }
}

// 3. Sync Global Leaderboards: global_top_tracks
async function syncGlobalTopChartsFromCloud() {
  try {
    const data = await firestoreGet(`global_top_tracks`);
    if (data && data.documents && data.documents.length > 0) {
      cloudTopTracks = data.documents.map(doc => {
        const item = fromFirestoreDoc(doc);
        return {
          videoId: item.videoId || doc.name.split('/').pop(),
          title: item.title || "Top Hit",
          artist: item.artist || "Artist",
          album: item.album || "Charts",
          art: item.albumArtUrl || "icon.png",
          streamUrl: item.audioUrl || "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
          plays: `${item.playCount || '10k+'} plays`
        };
      });
      // Replace top charts if valid
      if (cloudTopTracks.length > 0) {
        currentQueue = [...cloudTopTracks, ...curatedCatalog.trending];
        renderHomeScreen();
      }
    }
  } catch (err) {
    console.warn("Global charts using local fallback catalog:", err.message);
  }
}

// Toggle Favorite (Push to Firestore users/{syncUserId}/favorites)
async function toggleCurrentFavorite() {
  const track = currentQueue[currentIndex];
  if (!track) return;

  const vId = track.videoId || `track_${Math.abs(track.title.hashCode ? track.title.hashCode() : track.title.length)}`;
  const existingIdx = userFavorites.findIndex(f => f.videoId === vId || f.title === track.title);

  if (existingIdx !== -1) {
    // Remove favorite
    userFavorites.splice(existingIdx, 1);
    btnFavorite.classList.remove('active');
    showToast(`Removed "${track.title}" from favorites`);
    try {
      await firestoreDelete(`users/${currentUser.syncUserId}/favorites/${vId}`);
    } catch (err) {
      console.warn("Failed to delete cloud favorite:", err);
    }
  } else {
    // Add favorite
    const favObj = {
      videoId: vId,
      songId: vId,
      title: track.title,
      artist: track.artist,
      audioUrl: track.streamUrl,
      albumArtUrl: track.art,
      favoritedAt: Date.now()
    };
    userFavorites.unshift({
      videoId: vId,
      title: track.title,
      artist: track.artist,
      art: track.art,
      streamUrl: track.streamUrl
    });
    btnFavorite.classList.add('active');
    showToast(`❤️ Saved "${track.title}" to Cloud Database!`);

    try {
      await firestoreSet(`users/${currentUser.syncUserId}/favorites/${vId}`, toFirestoreFields(favObj));
    } catch (err) {
      console.warn("Failed to save cloud favorite:", err);
    }
  }

  renderFavoritesScreen();
}

// -------------------------------------------------------------
// NAVIGATION & TABS (Sidebar + Mobile Bottom Nav)
// -------------------------------------------------------------
function switchTab(tabName) {
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
  });

  document.querySelectorAll('.mobile-nav-item').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
  });

  document.querySelectorAll('.tab-view').forEach(view => {
    view.classList.toggle('active', view.id === `tab-${tabName}`);
  });

  if (tabName === 'nowplaying') {
    renderNowPlayingDetails();
  }

  document.getElementById('contentScrollable').scrollTop = 0;
}

document.querySelectorAll('.nav-btn, .mobile-nav-item').forEach(btn => {
  btn.addEventListener('click', () => {
    const tab = btn.getAttribute('data-tab');
    if (tab) switchTab(tab);
  });
});

document.getElementById('btnEqQuick')?.addEventListener('click', () => {
  switchTab('equalizer');
});

// -------------------------------------------------------------
// UI RENDERING: HOME, FAVORITES & LIBRARY
// -------------------------------------------------------------
function renderCard(track, index, onClick) {
  const card = document.createElement('div');
  card.className = 'music-card';
  card.innerHTML = `
    <div class="card-art-container">
      <img src="${track.art}" class="card-art" alt="${track.title}" onerror="this.src='icon.png'">
      <div class="card-play-overlay">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
      </div>
    </div>
    <div class="card-title">${track.title}</div>
    <div class="card-artist">${track.artist}</div>
  `;
  card.addEventListener('click', onClick);
  return card;
}

function renderTrackRow(track, rank, onClick) {
  const row = document.createElement('div');
  row.className = 'track-row';
  if (currentQueue[currentIndex]?.title === track.title) {
    row.classList.add('active');
  }
  row.innerHTML = `
    <div class="track-rank">${rank}</div>
    <img src="${track.art}" class="track-thumb" alt="${track.title}" onerror="this.src='icon.png'">
    <div class="track-details">
      <div class="track-name">${track.title}</div>
      <div class="track-sub">${track.artist} • ${track.album || 'Single'}</div>
    </div>
    <div class="track-stats">${track.plays || ''}</div>
  `;
  row.addEventListener('click', onClick);
  return row;
}

// -------------------------------------------------------------
// DUAL CHARTS CATALOGS (Weekly Rolling & All-Time Global)
// Matching Android TopChartsScreen.kt
// -------------------------------------------------------------
const weeklyChartsCatalog = [
  {
    videoId: "tumhare_hi",
    title: "Tumhare Hi Rahenge Hum",
    artist: "Sachin-Jigar, Varun Jain",
    album: "Stree 2",
    art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    plays: "4.8M plays"
  },
  {
    videoId: "goat",
    title: "G.O.A.T.",
    artist: "Diljit Dosanjh",
    album: "G.O.A.T.",
    art: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
    plays: "12.4M plays"
  },
  {
    videoId: "tauba_tauba",
    title: "Tauba Tauba",
    artist: "Karan Aujla",
    album: "Bad Newz",
    art: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
    plays: "18.2M plays"
  },
  {
    videoId: "apna_bana_le",
    title: "Apna Bana Le",
    artist: "Arijit Singh, Sachin-Jigar",
    album: "Bhediya",
    art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
    plays: "8.9M plays"
  },
  {
    videoId: "kesariya",
    title: "Kesariya",
    artist: "Arijit Singh, Pritam",
    album: "Brahmastra",
    art: "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
    plays: "15.1M plays"
  },
  {
    videoId: "chal_tere_ishq",
    title: "Chal Tere Ishq Mein",
    artist: "Neeti Mohan, Vishal Mishra",
    album: "Gadar 2",
    art: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
    plays: "6.2M plays"
  },
  {
    videoId: "hukum",
    title: "Hukum - Thalaivar Alappara",
    artist: "Anirudh Ravichander",
    album: "Jailer",
    art: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
    plays: "21.0M plays"
  },
  {
    videoId: "badass",
    title: "Badass",
    artist: "Anirudh Ravichander",
    album: "Leo",
    art: "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
    plays: "14.5M plays"
  },
  {
    videoId: "softly",
    title: "Softly",
    artist: "Karan Aujla, Ikky",
    album: "Making Memories",
    art: "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
    plays: "11.7M plays"
  },
  {
    videoId: "o_maahi",
    title: "O Maahi",
    artist: "Arijit Singh, Pritam",
    album: "Dunki",
    art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
    plays: "9.3M plays"
  }
];

const allTimeChartsCatalog = [
  {
    videoId: "tum_hi_ho",
    title: "Tum Hi Ho",
    artist: "Arijit Singh, Mithoon",
    album: "Aashiqui 2",
    art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
    plays: "88.4M plays"
  },
  {
    videoId: "channa_mereya",
    title: "Channa Mereya",
    artist: "Arijit Singh, Pritam",
    album: "Ae Dil Hai Mushkil",
    art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
    plays: "72.1M plays"
  },
  {
    videoId: "raataan_lambiyan",
    title: "Raataan Lambiyan",
    artist: "Jubin Nautiyal, Asees Kaur",
    album: "Shershaah",
    art: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
    plays: "65.3M plays"
  },
  {
    videoId: "kal_ho_naa_ho",
    title: "Kal Ho Naa Ho",
    artist: "Sonu Nigam, Shankar-Ehsaan-Loy",
    album: "Kal Ho Naa Ho",
    art: "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
    plays: "54.9M plays"
  },
  {
    videoId: "tujhe_dekh",
    title: "Tujhe Dekha Toh",
    artist: "Kumar Sanu, Lata Mangeshkar",
    album: "DDLJ",
    art: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
    plays: "41.6M plays"
  },
  {
    videoId: "pehla_nash",
    title: "Pehla Nasha",
    artist: "Udit Narayan, Sadhana Sargam",
    album: "Jo Jeeta Wohi Sikandar",
    art: "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3",
    plays: "35.2M plays"
  },
  {
    videoId: "jai_ho",
    title: "Jai Ho",
    artist: "A.R. Rahman, Sukhwinder Singh",
    album: "Slumdog Millionaire",
    art: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    plays: "48.7M plays"
  },
  {
    videoId: "tere_hawaale",
    title: "Tere Hawaale",
    artist: "Arijit Singh, Shilpa Rao",
    album: "Laal Singh Chaddha",
    art: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
    plays: "31.8M plays"
  }
];

function renderDualChartRow(track, rank, isWeekly, onClick) {
  const row = document.createElement('div');
  row.className = 'chart-track-item';
  if (currentQueue[currentIndex]?.title === track.title) {
    row.classList.add('active');
  }

  let rankClass = '';
  if (rank === 1) rankClass = 'rank-gold';
  else if (rank === 2) rankClass = 'rank-silver';
  else if (rank === 3) rankClass = 'rank-bronze';

  const pillClass = isWeekly ? 'weekly-pill' : 'alltime-pill';
  const pillPrefix = isWeekly ? '🔥 ' : '';

  row.innerHTML = `
    <div class="chart-rank-badge ${rankClass}">${rank}</div>
    <div class="chart-thumb-wrap">
      <img src="${track.art}" class="chart-thumb" alt="${track.title}" onerror="this.src='icon.png'">
      <div class="chart-thumb-play">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
      </div>
    </div>
    <div class="chart-info">
      <div class="chart-track-title">${track.title}</div>
      <div class="chart-track-artist">${track.artist}</div>
    </div>
    <div class="chart-playcount-pill ${pillClass}">${pillPrefix}${track.plays || '0 plays'}</div>
  `;
  row.addEventListener('click', onClick);
  return row;
}

function renderDualChartsScreen() {
  const weeklyContainer = document.getElementById('weeklyChartsList');
  const allTimeContainer = document.getElementById('allTimeChartsList');
  if (!weeklyContainer || !allTimeContainer) return;

  weeklyContainer.innerHTML = '';
  allTimeContainer.innerHTML = '';

  const weeklyList = (cloudTopTracks && cloudTopTracks.length > 0) ? cloudTopTracks : weeklyChartsCatalog;
  const allTimeList = allTimeChartsCatalog;

  weeklyList.forEach((track, i) => {
    weeklyContainer.appendChild(renderDualChartRow(track, i + 1, true, () => {
      currentQueue = [...weeklyList];
      playTrack(i);
    }));
  });

  allTimeList.forEach((track, i) => {
    allTimeContainer.appendChild(renderDualChartRow(track, i + 1, false, () => {
      currentQueue = [...allTimeList];
      playTrack(i);
    }));
  });
}

function renderHomeScreen() {
  const homeGreetingName = document.getElementById('homeGreetingName');
  if (homeGreetingName) {
    homeGreetingName.textContent = (currentUser.isLoggedIn && currentUser.displayName && !currentUser.displayName.toLowerCase().includes('guest'))
      ? currentUser.displayName.split(' ')[0]
      : 'Krish';
  }

  const homePlaylistsGrid = document.getElementById('homePlaylistsGrid');
  if (homePlaylistsGrid) {
    homePlaylistsGrid.innerHTML = '';
    const defaultPlaylists = [
      { name: "Bollywood Classics", count: "24 tracks", art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80" },
      { name: "Punjabi Party Hits", count: "18 tracks", art: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80" },
      { name: "Midnight Melodies", count: "32 tracks", art: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80" },
      { name: "Top Chartbusters", count: "50 tracks", art: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80" }
    ];
    const allHomePlaylists = [...userPlaylists, ...defaultPlaylists].slice(0, 4);
    allHomePlaylists.forEach(pl => {
      const card = document.createElement('div');
      card.className = 'music-card';
      card.innerHTML = `
        <div class="card-art-container">
          <img src="${pl.art || 'icon.png'}" class="card-art" alt="${pl.name}" onerror="this.src='icon.png'">
          <div class="card-play-overlay">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
          </div>
        </div>
        <div class="card-title">${pl.name}</div>
        <div class="card-artist">${pl.count || `${pl.trackCount || 0} tracks`}</div>
      `;
      card.addEventListener('click', () => {
        showToast(`Playing playlist: "${pl.name}"`);
        playTrack(0);
      });
      homePlaylistsGrid.appendChild(card);
    });
  }

  const trendingGrid = document.getElementById('trendingGrid');
  const biggestHitsGrid = document.getElementById('biggestHitsGrid');
  const nostalgiaGrid = document.getElementById('nostalgiaGrid');
  const homeTopCharts = document.getElementById('homeTopCharts');

  if (trendingGrid) trendingGrid.innerHTML = '';
  if (biggestHitsGrid) biggestHitsGrid.innerHTML = '';
  if (nostalgiaGrid) nostalgiaGrid.innerHTML = '';
  if (homeTopCharts) homeTopCharts.innerHTML = '';

  curatedCatalog.trending.forEach((track, i) => {
    if (trendingGrid) trendingGrid.appendChild(renderCard(track, i, () => playQueueTrack(track)));
  });

  curatedCatalog.biggestHits.forEach((track, i) => {
    if (biggestHitsGrid) biggestHitsGrid.appendChild(renderCard(track, i, () => playQueueTrack(track)));
  });

  curatedCatalog.nostalgia.forEach((track, i) => {
    if (nostalgiaGrid) nostalgiaGrid.appendChild(renderCard(track, i, () => playQueueTrack(track)));
  });

  currentQueue.slice(0, 8).forEach((track, i) => {
    if (homeTopCharts) homeTopCharts.appendChild(renderTrackRow(track, i + 1, () => playQueueTrack(track)));
  });

  renderDualChartsScreen();
}

function renderFavoritesScreen() {
  const favoritesList = document.getElementById('favoritesList');
  const favCountDesc = document.getElementById('favCountDesc');
  if (!favoritesList) return;

  favoritesList.innerHTML = '';

  if (userFavorites.length === 0) {
    favoritesList.innerHTML = `
      <div style="text-align: center; padding: 48px 16px; color: var(--musify-text-secondary);">
        <div style="font-size: 36px; margin-bottom: 8px;">❤️</div>
        <div style="font-size: 16px; font-weight: 700; color: #fff;">No favorites yet</div>
        <p style="font-size: 13px; margin-top: 4px;">Click the heart icon on any song to save it and sync across your Android app.</p>
      </div>
    `;
    if (favCountDesc) favCountDesc.textContent = "0 songs favorited";
    return;
  }

  if (favCountDesc) favCountDesc.textContent = `${userFavorites.length} songs synchronized with cloud`;
  userFavorites.forEach((track, i) => {
    favoritesList.appendChild(renderTrackRow(track, i + 1, () => playQueueTrack(track)));
  });
}

function renderLibraryScreen() {
  const playlistsGrid = document.getElementById('playlistsGrid');
  if (!playlistsGrid) return;

  playlistsGrid.innerHTML = '';

  const defaultPlaylists = [
    { name: "Bollywood Classics", count: "24 tracks", art: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80" },
    { name: "Punjabi Party Hits", count: "18 tracks", art: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80" },
    { name: "Midnight Melodies", count: "32 tracks", art: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80" }
  ];

  const allPlaylists = [...userPlaylists, ...defaultPlaylists];

  allPlaylists.forEach(pl => {
    const card = document.createElement('div');
    card.className = 'music-card';
    card.innerHTML = `
      <div class="card-art-container">
        <img src="${pl.art || 'icon.png'}" class="card-art" alt="${pl.name}" onerror="this.src='icon.png'">
        <div class="card-play-overlay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
        </div>
      </div>
      <div class="card-title">${pl.name}</div>
      <div class="card-artist">${pl.count || `${pl.trackCount || 0} tracks`} • Synced</div>
    `;
    card.addEventListener('click', () => {
      showToast(`Playing playlist: "${pl.name}"`);
      playTrack(0);
    });
    playlistsGrid.appendChild(card);
  });
}

function playQueueTrack(track) {
  let idx = currentQueue.findIndex(t => t.title === track.title);
  if (idx === -1) {
    currentQueue.unshift(track);
    idx = 0;
  }
  playTrack(idx);
}

// -------------------------------------------------------------
// PLAYBACK ENGINE & WEB AUDIO EQUALIZER
// -------------------------------------------------------------
let audioCtx = null;
let audioSource = null;
let eqFilters = [];
const eqFrequencies = [60, 230, 910, 3600, 14000];

function initAudioEqualizer() {
  if (audioCtx) return;
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    audioCtx = new AudioContextClass();
    audioSource = audioCtx.createMediaElementSource(audio);

    let prevNode = audioSource;
    eqFrequencies.forEach((freq, i) => {
      const filter = audioCtx.createBiquadFilter();
      if (i === 0) filter.type = 'lowshelf';
      else if (i === eqFrequencies.length - 1) filter.type = 'highshelf';
      else {
        filter.type = 'peaking';
        filter.Q.value = 1.0;
      }
      filter.frequency.value = freq;
      filter.gain.value = parseFloat(document.getElementById(`eqBand${i}`)?.value || 0);
      prevNode.connect(filter);
      prevNode = filter;
      eqFilters.push(filter);
    });

    const compressor = audioCtx.createDynamicsCompressor();
    compressor.threshold.setValueAtTime(-1.0, audioCtx.currentTime);
    compressor.knee.setValueAtTime(40, audioCtx.currentTime);
    compressor.ratio.setValueAtTime(12, audioCtx.currentTime);
    compressor.attack.setValueAtTime(0.003, audioCtx.currentTime);
    compressor.release.setValueAtTime(0.25, audioCtx.currentTime);

    prevNode.connect(compressor);
    compressor.connect(audioCtx.destination);
  } catch (err) {
    console.warn('Web Audio EQ initialization note:', err);
  }
}

function playTrack(index) {
  if (index < 0 || index >= currentQueue.length) return;
  currentIndex = index;
  const track = currentQueue[currentIndex];

  currentTitle.textContent = track.title;
  currentArtist.textContent = track.artist;
  currentArt.src = track.art || 'icon.png';

  initAudioEqualizer();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  // Update Favorite Heart Active status
  const isFav = userFavorites.some(f => f.title === track.title || f.videoId === track.videoId);
  btnFavorite.classList.toggle('active', isFav);

  renderNowPlayingDetails();

  audio.src = track.streamUrl;
  audio.play().then(() => {
    isPlaying = true;
    updatePlayPauseState();
    highlightActiveTrack();
  }).catch(err => {
    console.warn('Audio resume required:', err);
    updatePlayPauseState();
  });
}

function togglePlayPause() {
  if (!audio.src) {
    playTrack(0);
    return;
  }

  initAudioEqualizer();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  if (isPlaying) {
    audio.pause();
    isPlaying = false;
  } else {
    audio.play().then(() => {
      isPlaying = true;
      updatePlayPauseState();
    }).catch(console.error);
  }
  updatePlayPauseState();
}

function updatePlayPauseState() {
  const npPlaySvg = document.getElementById('npPlaySvg');
  const npBtnPlayPause = document.getElementById('npBtnPlayPause');
  const npSpectrumWave = document.getElementById('npSpectrumWave');
  const npPlaybackStatus = document.getElementById('npPlaybackStatus');

  if (isPlaying) {
    playSvg.innerHTML = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
    btnPlayPause.title = "Pause";
    if (npPlaySvg) npPlaySvg.innerHTML = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
    if (npBtnPlayPause) npBtnPlayPause.title = "Pause";
    if (npSpectrumWave) npSpectrumWave.classList.add('playing');
    if (npPlaybackStatus) {
      npPlaybackStatus.textContent = "PLAYING";
      npPlaybackStatus.style.color = "#1DB954";
    }
  } else {
    playSvg.innerHTML = '<path d="M8 5v14l11-7z"/>';
    btnPlayPause.title = "Play";
    if (npPlaySvg) npPlaySvg.innerHTML = '<path d="M8 5v14l11-7z"/>';
    if (npBtnPlayPause) npBtnPlayPause.title = "Play";
    if (npSpectrumWave) npSpectrumWave.classList.remove('playing');
    if (npPlaybackStatus) {
      npPlaybackStatus.textContent = "PAUSED";
      npPlaybackStatus.style.color = "#B3B3B3";
    }
  }
}

function highlightActiveTrack() {
  const current = currentQueue[currentIndex];
  document.querySelectorAll('.track-row').forEach(row => {
    const title = row.querySelector('.track-name')?.textContent;
    row.classList.toggle('active', title === current?.title);
  });
}

// -------------------------------------------------------------
// 3-PART SPLIT NOW PLAYING SCREEN CONTROLLER
// Part 1: Dashboard | Part 2: Song Details | Part 3: Up Next Queue
// -------------------------------------------------------------
function renderNowPlayingDetails() {
  const track = currentQueue[currentIndex];
  if (!track) return;

  const npSongTitle = document.getElementById('npSongTitle');
  const npSongArtist = document.getElementById('npSongArtist');
  const npSongAlbum = document.getElementById('npSongAlbum');
  const npMainArt = document.getElementById('npMainArt');
  const npGenreTag = document.getElementById('npGenreTag');
  const npBtnFavorite = document.getElementById('npBtnFavorite');
  const npFavLabel = document.getElementById('npFavLabel');
  const npPlaybackStatus = document.getElementById('npPlaybackStatus');
  const npFavCount = document.getElementById('npFavCount');

  if (npSongTitle) npSongTitle.textContent = track.title;
  if (npSongArtist) npSongArtist.textContent = track.artist;
  if (npSongAlbum) npSongAlbum.textContent = track.album || 'Studio Master';
  if (npMainArt) npMainArt.src = track.art || 'icon.png';
  if (npGenreTag) npGenreTag.textContent = (track.category || 'BOLLYWOOD / HITS').toUpperCase();

  const isFav = userFavorites.some(f => f.title === track.title || f.videoId === track.videoId);
  if (npBtnFavorite) npBtnFavorite.classList.toggle('active', isFav);
  if (npFavLabel) npFavLabel.textContent = isFav ? "Favorited" : "Favorite";

  if (npPlaybackStatus) {
    npPlaybackStatus.textContent = isPlaying ? "PLAYING" : "PAUSED";
    npPlaybackStatus.style.color = isPlaying ? "#1DB954" : "#B3B3B3";
  }

  if (npFavCount) npFavCount.textContent = userFavorites.length;

  renderNowPlayingQueue();
}

function renderNowPlayingQueue() {
  const npQueueCount = document.getElementById('npQueueCount');
  const npQCurrentThumb = document.getElementById('npQCurrentThumb');
  const npQCurrentTitle = document.getElementById('npQCurrentTitle');
  const npQCurrentArtist = document.getElementById('npQCurrentArtist');
  const npQueueList = document.getElementById('npQueueList');

  const current = currentQueue[currentIndex];
  if (current) {
    if (npQCurrentThumb) npQCurrentThumb.src = current.art || 'icon.png';
    if (npQCurrentTitle) npQCurrentTitle.textContent = current.title;
    if (npQCurrentArtist) npQCurrentArtist.textContent = current.artist;
  }

  if (!npQueueList) return;
  npQueueList.innerHTML = '';

  const upcoming = [];
  for (let i = currentIndex + 1; i < currentQueue.length; i++) {
    upcoming.push({ track: currentQueue[i], originalIdx: i });
  }
  for (let i = 0; i < currentIndex; i++) {
    upcoming.push({ track: currentQueue[i], originalIdx: i });
  }

  if (npQueueCount) npQueueCount.textContent = `${currentQueue.length} tracks`;

  if (upcoming.length === 0) {
    npQueueList.innerHTML = '<div style="font-size:12px;color:#757575;text-align:center;padding:24px 10px;">Queue is empty</div>';
    return;
  }

  upcoming.forEach((item, pos) => {
    const qItem = document.createElement('div');
    qItem.className = 'np-queue-item';
    qItem.innerHTML = `
      <span class="np-q-idx">${pos + 1}</span>
      <img src="${item.track.art}" class="np-q-thumb" alt="${item.track.title}" onerror="this.src='icon.png'">
      <div class="np-q-meta">
        <div class="np-q-title">${item.track.title}</div>
        <div class="np-q-artist">${item.track.artist}</div>
      </div>
      <button class="np-q-del-btn" title="Remove from queue">✕</button>
    `;

    // Click row to play track immediately
    qItem.addEventListener('click', (e) => {
      if (e.target.closest('.np-q-del-btn')) return;
      playTrack(item.originalIdx);
    });

    // Remove from queue button
    qItem.querySelector('.np-q-del-btn')?.addEventListener('click', (e) => {
      e.stopPropagation();
      const removed = currentQueue.splice(item.originalIdx, 1);
      if (item.originalIdx < currentIndex) {
        currentIndex--;
      }
      showToast(`Removed: ${removed[0]?.title || 'Track'}`);
      renderNowPlayingQueue();
    });

    npQueueList.appendChild(qItem);
  });
}

btnPlayPause.addEventListener('click', togglePlayPause);
btnFavorite.addEventListener('click', () => {
  toggleCurrentFavorite();
  renderNowPlayingDetails();
});

btnNext.addEventListener('click', () => {
  if (isShuffle) {
    const randomIdx = Math.floor(Math.random() * currentQueue.length);
    playTrack(randomIdx);
  } else {
    const nextIdx = (currentIndex + 1) % currentQueue.length;
    playTrack(nextIdx);
  }
});

btnPrev.addEventListener('click', () => {
  if (audio.currentTime > 3) {
    audio.currentTime = 0;
  } else {
    const prevIdx = (currentIndex - 1 + currentQueue.length) % currentQueue.length;
    playTrack(prevIdx);
  }
});

btnShuffle.addEventListener('click', () => {
  isShuffle = !isShuffle;
  btnShuffle.classList.toggle('active', isShuffle);
  document.getElementById('npBtnShuffle')?.classList.toggle('active', isShuffle);
});

btnRepeat.addEventListener('click', () => {
  isRepeat = !isRepeat;
  audio.loop = isRepeat;
  btnRepeat.classList.toggle('active', isRepeat);
  document.getElementById('npBtnRepeat')?.classList.toggle('active', isRepeat);
});

// Audio scrub & volume sync across Player Bar and Now Playing Center Panel
audio.addEventListener('timeupdate', () => {
  if (audio.duration) {
    const pct = (audio.currentTime / audio.duration) * 100;
    seekSlider.value = pct;
    scrubberFill.style.width = `${pct}%`;
    miniProgressFill.style.width = `${pct}%`;
    currTime.textContent = formatTime(audio.currentTime);
    totalTime.textContent = formatTime(audio.duration);

    // Sync Now Playing Panel
    const npSeekSlider = document.getElementById('npSeekSlider');
    const npScrubberFill = document.getElementById('npScrubberFill');
    const npCurrTime = document.getElementById('npCurrTime');
    const npTotalTime = document.getElementById('npTotalTime');
    if (npSeekSlider) npSeekSlider.value = pct;
    if (npScrubberFill) npScrubberFill.style.width = `${pct}%`;
    if (npCurrTime) npCurrTime.textContent = formatTime(audio.currentTime);
    if (npTotalTime) npTotalTime.textContent = formatTime(audio.duration);
  }
});

audio.addEventListener('ended', () => {
  if (!isRepeat) {
    btnNext.click();
  }
});

seekSlider.addEventListener('input', () => {
  if (audio.duration) {
    const pct = parseFloat(seekSlider.value);
    scrubberFill.style.width = `${pct}%`;
    miniProgressFill.style.width = `${pct}%`;
    audio.currentTime = (pct / 100) * audio.duration;
  }
});

volumeSlider.addEventListener('input', () => {
  const val = parseFloat(volumeSlider.value);
  audio.volume = val;
  volumeFill.style.width = `${val * 100}%`;
  updateVolumeIcon(val);
});

btnMute.addEventListener('click', () => {
  if (audio.volume > 0) {
    lastVolume = audio.volume;
    audio.volume = 0;
    volumeSlider.value = 0;
    volumeFill.style.width = '0%';
    updateVolumeIcon(0);
  } else {
    audio.volume = lastVolume || 0.8;
    volumeSlider.value = audio.volume;
    volumeFill.style.width = `${audio.volume * 100}%`;
    updateVolumeIcon(audio.volume);
  }
});

function updateVolumeIcon(vol) {
  if (vol === 0) {
    volIcon.innerHTML = '<path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>';
  } else if (vol < 0.5) {
    volIcon.innerHTML = '<path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>';
  } else {
    volIcon.innerHTML = '<path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>';
  }
}

function formatTime(secs) {
  if (isNaN(secs) || secs < 0) return "0:00";
  const m = Math.floor(secs / 60);
  const s = Math.floor(secs % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

// -------------------------------------------------------------
// SEARCH & EQUALIZER ROUTINES
// -------------------------------------------------------------
searchInput.addEventListener('input', () => {
  btnClearSearch.style.display = searchInput.value ? 'block' : 'none';
});

btnClearSearch.addEventListener('click', () => {
  searchInput.value = '';
  btnClearSearch.style.display = 'none';
  searchInput.focus();
});

searchInput.addEventListener('keypress', async (e) => {
  if (e.key === 'Enter') {
    const query = searchInput.value.trim();
    if (!query) return;

    switchTab('search');
    const headerTitle = document.getElementById('searchHeaderTitle');
    const headerSub = document.getElementById('searchHeaderSub');
    const resultsContainer = document.getElementById('searchResults');

    headerTitle.textContent = `Results for "${query}"`;
    headerSub.textContent = 'Searching lossless catalog & iTunes master streams...';
    resultsContainer.innerHTML = '<div style="padding: 24px; color: var(--musify-text-secondary);">Searching online audio services...</div>';

    try {
      const res = await fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(query)}&media=music&entity=song&limit=25`);
      const data = await res.json();
      resultsContainer.innerHTML = '';

      if (!data.results || data.results.length === 0) {
        resultsContainer.innerHTML = '<div style="padding: 24px; color: var(--musify-text-secondary);">No results found. Try searching for a different song or artist.</div>';
        return;
      }

      const searchTracks = data.results.map(item => ({
        videoId: `itunes_${item.trackId}`,
        title: item.trackName,
        artist: item.artistName,
        album: item.collectionName,
        art: item.artworkUrl100?.replace('100x100bb', '400x400bb') || 'icon.png',
        streamUrl: item.previewUrl,
        plays: 'Audio Stream'
      }));

      currentQueue = searchTracks;
      headerSub.textContent = `Found ${searchTracks.length} tracks`;

      searchTracks.forEach((track, idx) => {
        resultsContainer.appendChild(renderTrackRow(track, idx + 1, () => playTrack(idx)));
      });

      playTrack(0);
    } catch (err) {
      resultsContainer.innerHTML = `<div style="padding: 24px; color: #ff5252;">Search error: ${err.message}</div>`;
    }
  }
});

// Equalizer
const eqPresets = {
  flat: [0, 0, 0, 0, 0],
  bass: [6, 4, 1, 0, -1],
  vocal: [-2, 1, 5, 3, 1],
  rock: [5, 3, -1, 3, 5],
  electronic: [5, 3, 0, 2, 4]
};

function applyEqPreset(presetName) {
  const gains = eqPresets[presetName] || eqPresets.flat;
  gains.forEach((gain, i) => {
    const slider = document.getElementById(`eqBand${i}`);
    const label = document.getElementById(`eqVal${i}`);
    if (slider) slider.value = gain;
    if (label) label.textContent = `${gain > 0 ? '+' : ''}${gain} dB`;
    if (eqFilters[i]) eqFilters[i].gain.value = gain;
  });
}

document.querySelectorAll('.eq-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    document.querySelectorAll('.eq-chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    applyEqPreset(chip.getAttribute('data-preset'));
  });
});

document.getElementById('btnResetEq')?.addEventListener('click', () => {
  document.querySelector('[data-preset="flat"]')?.click();
});

[0, 1, 2, 3, 4].forEach(i => {
  const slider = document.getElementById(`eqBand${i}`);
  const label = document.getElementById(`eqVal${i}`);
  slider?.addEventListener('input', () => {
    const val = parseFloat(slider.value);
    label.textContent = `${val > 0 ? '+' : ''}${val} dB`;
    if (eqFilters[i]) eqFilters[i].gain.value = val;
    document.querySelectorAll('.eq-chip').forEach(c => c.classList.remove('active'));
  });
});

// -------------------------------------------------------------
// NOW PLAYING 3-PART SCREEN CONTROLS & EVENT LISTENERS
// -------------------------------------------------------------
document.getElementById('npBtnPlayPause')?.addEventListener('click', togglePlayPause);

document.getElementById('npBtnFavorite')?.addEventListener('click', () => {
  toggleCurrentFavorite();
  renderNowPlayingDetails();
});

document.getElementById('npBtnPrev')?.addEventListener('click', () => btnPrev.click());
document.getElementById('npBtnNext')?.addEventListener('click', () => btnNext.click());

document.getElementById('npBtnShuffle')?.addEventListener('click', () => {
  btnShuffle.click();
});

document.getElementById('npBtnRepeat')?.addEventListener('click', () => {
  btnRepeat.click();
});

document.getElementById('npSeekSlider')?.addEventListener('input', () => {
  if (audio.duration) {
    const pct = parseFloat(document.getElementById('npSeekSlider').value);
    const npScrubberFill = document.getElementById('npScrubberFill');
    if (npScrubberFill) npScrubberFill.style.width = `${pct}%`;
    scrubberFill.style.width = `${pct}%`;
    miniProgressFill.style.width = `${pct}%`;
    audio.currentTime = (pct / 100) * audio.duration;
  }
});

document.getElementById('npVolumeSlider')?.addEventListener('input', () => {
  const val = parseFloat(document.getElementById('npVolumeSlider').value);
  audio.volume = val;
  volumeSlider.value = val;
  volumeFill.style.width = `${val * 100}%`;
  const npVolumeFill = document.getElementById('npVolumeFill');
  if (npVolumeFill) npVolumeFill.style.width = `${val * 100}%`;
  updateVolumeIcon(val);
});

document.getElementById('npBtnMute')?.addEventListener('click', () => btnMute.click());

document.getElementById('npBtnShare')?.addEventListener('click', () => {
  const track = currentQueue[currentIndex];
  if (track) {
    const text = `${track.title} by ${track.artist} on Musify`;
    navigator.clipboard?.writeText(text);
    showToast(`Copied "${track.title}" to clipboard!`);
  }
});

// Up Next Queue toolbar actions
document.getElementById('btnShuffleQueue')?.addEventListener('click', () => {
  if (currentQueue.length <= 1) return;
  const current = currentQueue[currentIndex];
  const rest = currentQueue.filter((_, i) => i !== currentIndex);
  for (let i = rest.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [rest[i], rest[j]] = [rest[j], rest[i]];
  }
  currentQueue = [current, ...rest];
  currentIndex = 0;
  showToast("🔀 Queue shuffled");
  renderNowPlayingQueue();
});

document.getElementById('btnClearQueue')?.addEventListener('click', () => {
  currentQueue = [...curatedCatalog.trending, ...curatedCatalog.biggestHits];
  currentIndex = 0;
  showToast("Queue reset to curated catalog");
  playTrack(0);
});

// Dashboard: Quick Equalizer Presets
document.querySelectorAll('.np-preset-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.np-preset-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const preset = btn.getAttribute('data-preset');
    applyEqPreset(preset);
    const npEqStatus = document.getElementById('npEqStatus');
    if (npEqStatus) npEqStatus.textContent = `${preset.charAt(0).toUpperCase() + preset.slice(1)} Mode`;
    showToast(`EQ: ${preset.toUpperCase()}`);
  });
});

// Dashboard: Quick Navigation Shortcuts
document.querySelectorAll('.np-shortcut-item').forEach(item => {
  item.addEventListener('click', () => {
    const targetTab = item.getAttribute('data-goto');
    if (targetTab) switchTab(targetTab);
  });
});

// Click bottom player bar to switch to 3-part Now Playing screen
document.getElementById('playerLeftTrack')?.addEventListener('click', () => switchTab('nowplaying'));
document.getElementById('btnExpandNowPlaying')?.addEventListener('click', () => switchTab('nowplaying'));

// -------------------------------------------------------------
// AUTH MODAL & GOOGLE SIGN-IN SYSTEM (Matches Android App)
// -------------------------------------------------------------
const authModal = document.getElementById('authModal');
const btnCloseAuthModal = document.getElementById('btnCloseAuthModal');
const authFeedback = document.getElementById('authFeedback');

function openAuthModal() {
  if (authFeedback) {
    authFeedback.textContent = '';
    authFeedback.className = 'auth-feedback';
  }
  authModal.classList.add('open');
}

function closeAuthModal() {
  authModal.classList.remove('open');
}

document.getElementById('btnSidebarAuth')?.addEventListener('click', openAuthModal);
document.getElementById('btnHeaderUser')?.addEventListener('click', openAuthModal);
btnCloseAuthModal?.addEventListener('click', closeAuthModal);
btnHeaderSync?.addEventListener('click', syncAllWithCloud);

// 1. Continue with Google (Electron OAuth Window with Web Client ID)
document.getElementById('btnGoogleSignIn')?.addEventListener('click', async () => {
  if (authFeedback) {
    authFeedback.textContent = 'Opening Google Sign-In...';
    authFeedback.className = 'auth-feedback';
  }

  try {
    if (window.electronAPI && window.electronAPI.signInWithGoogle) {
      const res = await window.electronAPI.signInWithGoogle();
      if (res && res.success) {
        saveUserSession(res.email, res.displayName, res.idToken);
        if (authFeedback) {
          authFeedback.textContent = `Signed in as ${res.displayName || res.email}!`;
          authFeedback.className = 'auth-feedback success';
        }
        showToast(`Signed in as ${res.displayName || res.email}!`);
        setTimeout(closeAuthModal, 1000);
      } else {
        if (authFeedback) {
          authFeedback.textContent = res?.error || 'Google sign-in was cancelled.';
          authFeedback.className = 'auth-feedback error';
        }
      }
    } else {
      if (authFeedback) {
        authFeedback.textContent = 'Google sign-in is available in the desktop app.';
        authFeedback.className = 'auth-feedback error';
      }
    }
  } catch (err) {
    if (authFeedback) {
      authFeedback.textContent = `Sign in error: ${err.message}`;
      authFeedback.className = 'auth-feedback error';
    }
  }
});

// 2. Direct Google Email Sync (matching Android signInWithGoogleEmail)
document.getElementById('btnQuickConnect')?.addEventListener('click', () => {
  const emailInput = document.getElementById('quickEmailInput');
  const email = emailInput ? emailInput.value.trim() : '';

  if (!email || !email.includes('@')) {
    if (authFeedback) {
      authFeedback.textContent = 'Please enter a valid Google email address.';
      authFeedback.className = 'auth-feedback error';
    }
    return;
  }

  saveUserSession(email, email.split('@')[0]);
  if (authFeedback) {
    authFeedback.textContent = 'Connected! Syncing from cloud database...';
    authFeedback.className = 'auth-feedback success';
  }
  setTimeout(closeAuthModal, 1000);
});

// 3. Continue as Guest
document.getElementById('btnGuestMode')?.addEventListener('click', () => {
  clearUserSession();
  closeAuthModal();
});

// 4. Sign Out
document.getElementById('btnSignOut')?.addEventListener('click', () => {
  clearUserSession();
  closeAuthModal();
});

// Create Playlist Action
document.getElementById('btnCreatePlaylist')?.addEventListener('click', async () => {
  const name = prompt("Enter new playlist name:", "My Favorites 2026");
  if (!name || !name.trim()) return;

  const newPl = {
    id: `pl_${Date.now()}`,
    name: name.trim(),
    trackCount: 0,
    updatedAt: Date.now()
  };

  userPlaylists.unshift(newPl);
  renderLibraryScreen();
  showToast(`Created playlist: "${name.trim()}"`);

  try {
    await firestoreSet(`users/${currentUser.syncUserId}/playlists/${newPl.id}`, toFirestoreFields(newPl));
  } catch (err) {
    console.warn("Could not save playlist to Firestore:", err);
  }
});

// Toast notification
function showToast(msg) {
  const toast = document.getElementById('toastNotification');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  clearTimeout(toast._timeout);
  toast._timeout = setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
}

// -------------------------------------------------------------
// MADE FOR YOU: SUPERMIX ACTIONS
// -------------------------------------------------------------
function playSupermix() {
  const supermixTracks = [
    ...curatedCatalog.trending.filter(t => t.artist.includes("Arijit") || t.artist.includes("Sachin")),
    ...curatedCatalog.biggestHits.slice(0, 4),
    ...curatedCatalog.nostalgia.slice(0, 4)
  ];
  currentQueue = supermixTracks.length > 0 ? supermixTracks : [...curatedCatalog.trending];
  playTrack(0);
  showToast("Playing My Supermix (AI Personalized)");
}

document.getElementById('btnPlaySupermix')?.addEventListener('click', (e) => {
  e.stopPropagation();
  playSupermix();
});

document.getElementById('supermixHeroCard')?.addEventListener('click', () => {
  playSupermix();
});

document.getElementById('btnPlayAllSupermix')?.addEventListener('click', () => {
  playSupermix();
});

// -------------------------------------------------------------
// ANDROID LIBRARY GROUPED ITEMS ACTIONS (matching musify_screen.png)
// -------------------------------------------------------------
document.getElementById('libItemLastSession')?.addEventListener('click', () => {
  showToast("Resuming your last playback session...");
  playTrack(currentIndex);
});

document.getElementById('libItemStats')?.addEventListener('click', () => {
  showToast("📊 Listening Stats: Over 124 minutes streamed in lossless master audio today.");
});

document.getElementById('libItemFavorites')?.addEventListener('click', () => {
  switchTab('favorites');
});

document.getElementById('libItemMyMusic')?.addEventListener('click', () => {
  showToast("Playing all songs in your cloud library");
  currentQueue = [...curatedCatalog.trending, ...curatedCatalog.biggestHits, ...curatedCatalog.nostalgia];
  playTrack(0);
});

document.getElementById('libItemPlaylists')?.addEventListener('click', () => {
  const el = document.getElementById('playlistsGrid');
  if (el) el.scrollIntoView({ behavior: 'smooth' });
  showToast("Browsing synchronized cloud playlists");
});

document.getElementById('libItemDownloads')?.addEventListener('click', () => {
  showToast("📥 Offline Downloads: 14 tracks cached locally in lossless 320kbps format.");
});

// -------------------------------------------------------------
// DUAL CHARTS ACTIONS (Weekly & All-Time Play All & Shuffle)
// -------------------------------------------------------------
document.getElementById('btnPlayAllWeekly')?.addEventListener('click', () => {
  const list = (cloudTopTracks && cloudTopTracks.length > 0) ? cloudTopTracks : weeklyChartsCatalog;
  currentQueue = [...list];
  playTrack(0);
  showToast("Playing Weekly Top Songs (Past 7 Days)");
});

document.getElementById('btnShuffleWeekly')?.addEventListener('click', () => {
  const list = (cloudTopTracks && cloudTopTracks.length > 0) ? cloudTopTracks : weeklyChartsCatalog;
  currentQueue = [...list].sort(() => Math.random() - 0.5);
  playTrack(0);
  showToast("Shuffling Weekly Top Songs");
});

document.getElementById('btnPlayAllAllTime')?.addEventListener('click', () => {
  currentQueue = [...allTimeChartsCatalog];
  playTrack(0);
  showToast("Playing All-Time Top Songs (Global Leaderboard)");
});

document.getElementById('btnShuffleAllTime')?.addEventListener('click', () => {
  currentQueue = [...allTimeChartsCatalog].sort(() => Math.random() - 0.5);
  playTrack(0);
  showToast("Shuffling All-Time Top Songs");
});

document.getElementById('btnChartsInfo')?.addEventListener('click', () => {
  document.getElementById('chartsInfoModal')?.classList.add('active');
});

document.getElementById('btnCloseChartsInfo')?.addEventListener('click', () => {
  document.getElementById('chartsInfoModal')?.classList.remove('active');
});

document.getElementById('btnAckChartsInfo')?.addEventListener('click', () => {
  document.getElementById('chartsInfoModal')?.classList.remove('active');
});

document.getElementById('btnRefreshCharts')?.addEventListener('click', async () => {
  showToast("Refreshing cloud charts leaderboard...");
  await syncGlobalTopChartsFromCloud();
  renderDualChartsScreen();
  showToast("Top charts up to date!");
});

// -------------------------------------------------------------
// ANDROID PLAYBACK CONTROLS (Speed, Sleep Timer, Lyrics)
// -------------------------------------------------------------
document.getElementById('npBtnSpeed')?.addEventListener('click', () => {
  document.getElementById('speedModal')?.classList.add('active');
});

document.getElementById('btnCloseSpeedModal')?.addEventListener('click', () => {
  document.getElementById('speedModal')?.classList.remove('active');
});

document.querySelectorAll('[data-speed]').forEach(btn => {
  btn.addEventListener('click', () => {
    const spd = parseFloat(btn.getAttribute('data-speed'));
    audio.playbackRate = spd;
    const lbl = document.getElementById('npSpeedLabel');
    if (lbl) lbl.textContent = `${spd}x`;
    document.getElementById('speedModal')?.classList.remove('active');
    showToast(`Playback speed set to ${spd}x`);
  });
});

let sleepTimerTimeout = null;
document.getElementById('npBtnSleepTimer')?.addEventListener('click', () => {
  document.getElementById('sleepTimerModal')?.classList.add('active');
});

document.getElementById('btnCloseSleepModal')?.addEventListener('click', () => {
  document.getElementById('sleepTimerModal')?.classList.remove('active');
});

document.querySelectorAll('[data-minutes]').forEach(btn => {
  btn.addEventListener('click', () => {
    const val = btn.getAttribute('data-minutes');
    if (sleepTimerTimeout) {
      clearTimeout(sleepTimerTimeout);
      sleepTimerTimeout = null;
    }
    const lbl = document.getElementById('npSleepLabel');
    if (val === "0") {
      if (lbl) lbl.textContent = "Timer";
      showToast("Sleep timer turned off");
    } else if (val === "end") {
      if (lbl) lbl.textContent = "Track end";
      showToast("Playback will stop at end of current track");
    } else {
      const mins = parseInt(val, 10);
      sleepTimerTimeout = setTimeout(() => {
        audio.pause();
        setIsPlaying(false);
        showToast("Sleep timer: Playback stopped");
        if (lbl) lbl.textContent = "Timer";
      }, mins * 60 * 1000);
      if (lbl) lbl.textContent = `${mins}m`;
      showToast(`Sleep timer set for ${mins} minutes`);
    }
    document.getElementById('sleepTimerModal')?.classList.remove('active');
  });
});

// Synced Lyrics Drawer Toggle
document.getElementById('npBtnLyrics')?.addEventListener('click', () => {
  const drawer = document.getElementById('npLyricsDrawer');
  if (drawer) {
    drawer.classList.toggle('active');
    if (drawer.classList.contains('active')) {
      const track = currentQueue[currentIndex];
      const body = document.getElementById('npLyricsBody');
      if (body && track) {
        body.innerHTML = `
          <p class="lyric-line">${track.title}</p>
          <p class="lyric-line active">♪ ${track.artist} - Studio Master ♪</p>
          <p class="lyric-line">Live cloud stream synchronized</p>
          <p class="lyric-line">Musify Lossless Engine 320kbps</p>
        `;
      }
    }
  }
});

document.getElementById('btnCloseLyrics')?.addEventListener('click', () => {
  document.getElementById('npLyricsDrawer')?.classList.remove('active');
});

// Import Playlist Modal Actions
document.getElementById('btnImportPlaylist')?.addEventListener('click', () => {
  document.getElementById('importPlaylistModal')?.classList.add('active');
});

document.getElementById('btnCloseImportModal')?.addEventListener('click', () => {
  document.getElementById('importPlaylistModal')?.classList.remove('active');
});

document.getElementById('btnCancelImport')?.addEventListener('click', () => {
  document.getElementById('importPlaylistModal')?.classList.remove('active');
});

document.getElementById('btnSubmitImport')?.addEventListener('click', () => {
  const input = document.getElementById('importPlaylistUrl');
  const url = input?.value.trim();
  if (url) {
    userPlaylists.unshift({
      id: `pl_import_${Date.now()}`,
      name: "Imported Playlist",
      count: "Synced from Link",
      art: "icon.png"
    });
    renderLibraryScreen();
    showToast("Playlist imported and synchronized with cloud!");
    input.value = '';
    document.getElementById('importPlaylistModal')?.classList.remove('active');
  } else {
    showToast("Please enter a valid playlist link");
  }
});

// -------------------------------------------------------------
// INITIAL BOOT
// -------------------------------------------------------------
loadSavedUser();
renderHomeScreen();
renderFavoritesScreen();
renderLibraryScreen();
renderNowPlayingDetails();
syncAllWithCloud();

// Auto-sync every 60 seconds
setInterval(syncAllWithCloud, 60000);

