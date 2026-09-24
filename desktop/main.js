const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const path = require('path');
const https = require('https');

let mainWindow;

function createWindow() {
  Menu.setApplicationMenu(null);

  mainWindow = new BrowserWindow({
    width: 1240,
    height: 840,
    minWidth: 360,
    minHeight: 480,
    backgroundColor: '#000000',
    title: 'Musify - Pure Lossless Audio',
    autoHideMenuBar: true,
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// Window controls
ipcMain.on('window-minimize', () => mainWindow?.minimize());
ipcMain.on('window-maximize', () => {
  if (mainWindow?.isMaximized()) mainWindow.unmaximize();
  else mainWindow?.maximize();
});
ipcMain.on('window-close', () => mainWindow?.close());

// -------------------------------------------------------------
// GOOGLE OAUTH 2.0 SIGN-IN HANDLER
// Uses Web Client ID from google-services.json
// -------------------------------------------------------------
function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = Buffer.from(base64, 'base64').toString('utf8');
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

ipcMain.handle('google-sign-in', async () => {
  return new Promise((resolve) => {
    const clientId = '466360010676-gjcnp1isug7vem9nk2b5vtebha376u4c.apps.googleusercontent.com';
    const redirectUri = 'https://musify-8df01.firebaseapp.com/__/auth/handler';
    const scope = encodeURIComponent('openid profile email');
    const nonce = `musify_${Date.now()}`;
    const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&response_type=token%20id_token&redirect_uri=${encodeURIComponent(redirectUri)}&scope=${scope}&nonce=${nonce}&prompt=select_account`;

    const authWindow = new BrowserWindow({
      width: 520,
      height: 680,
      show: true,
      parent: mainWindow,
      modal: true,
      backgroundColor: '#121212',
      title: 'Sign In with Google - Musify',
      icon: path.join(__dirname, 'icon.png'),
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true
      }
    });

    // Use standard modern Chrome User-Agent to comply with Google OAuth requirements
    authWindow.webContents.setUserAgent(
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
    );

    let isHandled = false;

    function handleRedirect(url) {
      if (!url || isHandled) return;

      if (url.startsWith(redirectUri)) {
        isHandled = true;
        try {
          const urlObj = new URL(url);
          const hashParams = new URLSearchParams(urlObj.hash.replace(/^#/, ''));
          const queryParams = urlObj.searchParams;

          const idToken = hashParams.get('id_token') || queryParams.get('id_token');
          const accessToken = hashParams.get('access_token') || queryParams.get('access_token');
          const error = hashParams.get('error') || queryParams.get('error');

          if (error) {
            resolve({ success: false, error: error });
            authWindow.close();
            return;
          }

          let profile = null;
          if (idToken) {
            profile = parseJwt(idToken);
          }

          if (profile && profile.email) {
            resolve({
              success: true,
              email: profile.email,
              displayName: profile.name || profile.email.split('@')[0],
              photoUrl: profile.picture || null,
              idToken: idToken,
              accessToken: accessToken
            });
            authWindow.close();
          } else if (accessToken) {
            // Fetch profile using access token
            https.get(`https://www.googleapis.com/oauth2/v3/userinfo?access_token=${accessToken}`, (res) => {
              let body = '';
              res.on('data', chunk => body += chunk);
              res.on('end', () => {
                try {
                  const info = JSON.parse(body);
                  resolve({
                    success: true,
                    email: info.email,
                    displayName: info.name || info.email.split('@')[0],
                    photoUrl: info.picture || null,
                    idToken: idToken,
                    accessToken: accessToken
                  });
                } catch (e) {
                  resolve({ success: false, error: 'Failed to parse user profile' });
                }
                authWindow.close();
              });
            }).on('error', (err) => {
              resolve({ success: false, error: err.message });
              authWindow.close();
            });
          } else {
            resolve({ success: false, error: 'No tokens received from Google' });
            authWindow.close();
          }
        } catch (e) {
          resolve({ success: false, error: e.message });
          authWindow.close();
        }
      }
    }

    authWindow.webContents.on('will-navigate', (event, url) => {
      handleRedirect(url);
    });

    authWindow.webContents.on('will-redirect', (event, url) => {
      handleRedirect(url);
    });

    authWindow.webContents.on('did-navigate', (event, url) => {
      handleRedirect(url);
    });

    authWindow.on('closed', () => {
      if (!isHandled) {
        resolve({ success: false, error: 'Sign in cancelled by user' });
      }
    });

    authWindow.loadURL(authUrl);
  });
});
