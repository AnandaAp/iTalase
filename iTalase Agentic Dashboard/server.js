/**
 * iTalase Agentic Dashboard — Lightweight Local Bridge Server
 * Built with pure Node.js core modules (zero npm dependencies).
 * Enables two-way real-time persistence: UI drag-and-drop directly writes to audit_state.js on disk!
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const PORT = 3300;
const BASE_DIR = __dirname;
const STATE_FILE = path.join(BASE_DIR, 'audit_state.js');

function getAdbPath() {
  const localPropPath = path.join(BASE_DIR, '../local.properties');
  if (fs.existsSync(localPropPath)) {
    try {
      const content = fs.readFileSync(localPropPath, 'utf-8');
      const match = content.match(/sdk\.dir=(.*)/);
      if (match) {
        const sdkDir = match[1].replace(/\\:/g, ':').replace(/\\\\/g, '\\').trim();
        const adb = path.join(sdkDir, 'platform-tools', 'adb.exe');
        if (fs.existsSync(adb)) return adb;
      }
    } catch (e) {}
  }
  const defaultAdb = 'C:\\Users\\anand\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe';
  if (fs.existsSync(defaultAdb)) return defaultAdb;
  return 'adb';
}

function getConnectedDevices() {
  const adb = getAdbPath();
  try {
    const raw = execSync(`"${adb}" devices -l`, { encoding: 'utf-8', timeout: 2500 });
    const lines = raw.split('\n').map(l => l.trim()).filter(l => l && !l.startsWith('List of devices'));
    const devices = [];
    for (const line of lines) {
      const parts = line.split(/\s+/);
      if (parts.length >= 2) {
        const serial = parts[0];
        const state = parts[1];
        const modelMatch = line.match(/model:(\S+)/);
        const deviceMatch = line.match(/device:(\S+)/);
        const productMatch = line.match(/product:(\S+)/);
        const rawModel = modelMatch ? modelMatch[1].replace(/_/g, ' ') : (deviceMatch ? deviceMatch[1] : serial);
        const isEmulator = serial.startsWith('emulator-');
        
        let label = rawModel;
        let badge = 'DEV';
        if (isEmulator) {
          label = 'Virtual Device (' + serial + ')';
          badge = 'VD';
        } else if (rawModel.toLowerCase().includes('a54')) {
          label = 'Samsung Galaxy A54';
          badge = 'A54';
        } else if (rawModel.length > 2) {
          badge = rawModel.substring(0, 3).toUpperCase();
        }

        devices.push({
          serial,
          state,
          model: rawModel,
          label,
          badge,
          product: productMatch ? productMatch[1] : '',
          isEmulator
        });
      }
    }
    return {
      online: devices.length > 0,
      count: devices.length,
      activeDevice: devices.length > 0 ? devices[0] : null,
      devices
    };
  } catch (e) {
    return {
      online: false,
      count: 0,
      activeDevice: null,
      devices: [],
      error: e.message
    };
  }
}

function getLibsVersions() {
  const tomlPath = path.join(BASE_DIR, '../gradle/libs.versions.toml');
  const result = {
    targetSdk: 37,
    minSdk: 26,
    compileSdk: 37,
    version: '1.0.0',
    versionCode: 100000,
    agp: '9.4.1',
    kotlin: '2.4.21',
    source: 'gradle/libs.versions.toml'
  };
  if (fs.existsSync(tomlPath)) {
    try {
      const content = fs.readFileSync(tomlPath, 'utf-8');
      const targetMatch = content.match(/android-targetSdk\s*=\s*"([^"]+)"/);
      if (targetMatch) result.targetSdk = parseInt(targetMatch[1], 10) || targetMatch[1];
      
      const minMatch = content.match(/android-minSdk\s*=\s*"([^"]+)"/);
      if (minMatch) result.minSdk = parseInt(minMatch[1], 10) || minMatch[1];
      
      const compileMatch = content.match(/android-compileSdk\s*=\s*"([^"]+)"/);
      if (compileMatch) result.compileSdk = parseInt(compileMatch[1], 10) || compileMatch[1];
      
      const versionMatch = content.match(/app-version\s*=\s*"([^"]+)"/);
      if (versionMatch) result.version = versionMatch[1];
      
      const codeMatch = content.match(/app-versioncode\s*=\s*"([^"]+)"/);
      if (codeMatch) result.versionCode = parseInt(codeMatch[1], 10) || codeMatch[1];
      
      const agpMatch = content.match(/agp\s*=\s*"([^"]+)"/);
      if (agpMatch) result.agp = agpMatch[1];
      
      const kotlinMatch = content.match(/kotlin\s*=\s*"([^"]+)"/);
      if (kotlinMatch) result.kotlin = kotlinMatch[1];
    } catch (e) {
      console.warn('[iTalase Server] Failed to parse libs.versions.toml:', e.message);
    }
  }
  return result;
}

// SSE Client Registry for instant zero-latency push to browser
const sseClients = new Set();

function broadcastState(stateJson) {
  if (sseClients.size === 0) return;
  const payload = `data: ${stateJson}\n\n`;
  for (const client of sseClients) {
    try {
      client.write(payload);
    } catch (e) {
      sseClients.delete(client);
    }
  }
}

// Watch audit_state.js for disk mutations caused by CLI update_dashboard.js or AI agents
let watchDebounce = null;
try {
  fs.watch(STATE_FILE, (eventType) => {
    if (eventType === 'change') {
      if (watchDebounce) clearTimeout(watchDebounce);
      watchDebounce = setTimeout(() => {
        try {
          const fileContent = fs.readFileSync(STATE_FILE, 'utf-8');
          const jsonMatch = fileContent.match(/window\.AUDIT_STATE\s*=\s*({[\s\S]*});?\s*$/);
          if (jsonMatch) {
            broadcastState(jsonMatch[1]);
          }
        } catch (e) {}
      }, 60);
    }
  });
} catch (e) {
  console.warn('[iTalase Server] fs.watch not active:', e.message);
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.jsx': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml'
};

function saveStateToDisk(state) {
  state.projectInfo.lastUpdated = new Date().toISOString().replace('T', ' ').substring(0, 19);
  const libs = getLibsVersions();
  state.projectInfo.targetSdk = libs.targetSdk;
  state.projectInfo.minSdk = libs.minSdk;
  state.projectInfo.compileSdk = libs.compileSdk;
  state.projectInfo.version = libs.version;
  state.projectInfo.versionCode = libs.versionCode;
  const formatted = `// iTalase Real-time Audit & Agentic State Store\n// Automatically maintained by Antigravity AI Agent Telemetry Protocol\n\nwindow.AUDIT_STATE = ${JSON.stringify(state, null, 2)};\n`;
  fs.writeFileSync(STATE_FILE, formatted, 'utf-8');
  broadcastState(JSON.stringify(state));
  console.log(`[iTalase Server] Successfully persisted state to audit_state.js at ${new Date().toLocaleTimeString()}`);
}

const server = http.createServer((req, res) => {
  // CORS & Anti-Caching Headers (Critical for DevTunnels and Port-Forwarding)
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Cache-Control, Pragma');
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // API Route: Get Fresh State JSON Directly (with dynamic libs.versions.toml sync)
  if (req.method === 'GET' && req.url.startsWith('/api/state')) {
    try {
      const fileContent = fs.readFileSync(STATE_FILE, 'utf-8');
      const jsonMatch = fileContent.match(/window\.AUDIT_STATE\s*=\s*({[\s\S]*});?\s*$/);
      if (jsonMatch) {
        const stateObj = JSON.parse(jsonMatch[1]);
        const libs = getLibsVersions();
        if (stateObj.projectInfo) {
          stateObj.projectInfo.targetSdk = libs.targetSdk;
          stateObj.projectInfo.minSdk = libs.minSdk;
          stateObj.projectInfo.compileSdk = libs.compileSdk;
          stateObj.projectInfo.version = libs.version;
          stateObj.projectInfo.versionCode = libs.versionCode;
          stateObj.projectInfo.source = libs.source;
        }
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify(stateObj));
        return;
      }
      throw new Error('Could not parse window.AUDIT_STATE from file');
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: err.message }));
      return;
    }
  }

  // API Route: Build Target Specifications from gradle/libs.versions.toml
  if (req.method === 'GET' && req.url.startsWith('/api/build-target')) {
    const libs = getLibsVersions();
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(libs));
    return;
  }

  // API Route: Save Full State
  if (req.method === 'POST' && req.url === '/api/save-state') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        saveStateToDisk(payload);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, message: 'State saved to disk' }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // API Route: Move Card
  if (req.method === 'POST' && req.url === '/api/move-card') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const { cardKey, targetCol } = JSON.parse(body);
        const fileContent = fs.readFileSync(STATE_FILE, 'utf-8');
        const jsonMatch = fileContent.match(/window\.AUDIT_STATE\s*=\s*({[\s\S]*});?\s*$/);
        if (!jsonMatch) throw new Error('Cannot parse audit_state.js');
        const state = eval('(' + jsonMatch[1] + ')');

        let foundCard = null;
        for (const col of Object.keys(state.kanban)) {
          const list = state.kanban[col];
          if (Array.isArray(list)) {
            const idx = list.findIndex(c => c.key === cardKey || c.id === cardKey);
            if (idx !== -1) {
              foundCard = list.splice(idx, 1)[0];
              break;
            }
          }
        }

        if (foundCard) {
          if (!state.kanban[targetCol]) state.kanban[targetCol] = [];
          state.kanban[targetCol].unshift(foundCard);
          saveStateToDisk(state);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, moved: foundCard.title }));
        } else {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'Card not found' }));
        }
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // API Route: Real-time Server-Sent Events (SSE) Stream
  if (req.method === 'GET' && req.url.startsWith('/api/events')) {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*'
    });
    res.write(': connected\n\n');
    sseClients.add(res);

    req.on('close', () => {
      sseClients.delete(res);
    });
    return;
  }

  // API Route: Healthcheck & Status
  if (req.method === 'GET' && req.url.startsWith('/api/status')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online', server: 'iTalase Bridge Server', port: PORT }));
    return;
  }

  // API Route: Live Connected Devices (ADB Physical & Virtual Device Probe)
  if (req.method === 'GET' && req.url.startsWith('/api/devices')) {
    const info = getConnectedDevices();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(info));
    return;
  }

  // Static File Serving
  let reqPath = req.url.split('?')[0];
  if (reqPath === '/' || reqPath === '') reqPath = '/index.html';
  const filePath = path.join(BASE_DIR, reqPath);

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    fs.createReadStream(filePath).pipe(res);
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found');
  }
});

server.listen(PORT, () => {
  console.log(`\n======================================================`);
  console.log(`🚀 iTalase React Agentic Dashboard Server Running!`);
  console.log(`📡 URL: http://localhost:${PORT}`);
  console.log(`💾 Live Two-Way Persistence: ACTIVE (Auto-writes to audit_state.js)`);
  console.log(`======================================================\n`);
});
