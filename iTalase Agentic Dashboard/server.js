/**
 * iTalase Agentic Dashboard — Lightweight Local Bridge Server
 * Built with pure Node.js core modules (zero npm dependencies).
 * Enables two-way real-time persistence: UI drag-and-drop directly writes to audit_state.js on disk!
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3300;
const BASE_DIR = __dirname;
const STATE_FILE = path.join(BASE_DIR, 'audit_state.js');

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
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml'
};

function saveStateToDisk(state) {
  state.projectInfo.lastUpdated = new Date().toISOString().replace('T', ' ').substring(0, 19);
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

  // API Route: Get Fresh State JSON Directly
  if (req.method === 'GET' && req.url.startsWith('/api/state')) {
    try {
      const fileContent = fs.readFileSync(STATE_FILE, 'utf-8');
      const jsonMatch = fileContent.match(/window\.AUDIT_STATE\s*=\s*({[\s\S]*});?\s*$/);
      if (jsonMatch) {
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(jsonMatch[1]);
        return;
      }
      throw new Error('Could not parse window.AUDIT_STATE from file');
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: err.message }));
      return;
    }
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
  if (req.method === 'GET' && req.url === '/api/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online', server: 'iTalase Bridge Server', port: PORT }));
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
