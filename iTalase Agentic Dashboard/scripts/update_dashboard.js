/**
 * iTalase Agentic Telemetry State Controller
 * Helper CLI utility for Antigravity AI Agents to query and update audit_state.js
 * Automatically synchronizes active sprint tasks into the interactive Kanban Board.
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const { execSync, spawn } = require('child_process');

const STATE_FILE = path.resolve(__dirname, '../audit_state.js');

function loadState() {
  if (!fs.existsSync(STATE_FILE)) {
    throw new Error(`State file not found at: ${STATE_FILE}`);
  }
  const content = fs.readFileSync(STATE_FILE, 'utf-8');
  // Extract JSON string from window.AUDIT_STATE = { ... };
  const jsonMatch = content.match(/window\.AUDIT_STATE\s*=\s*({[\s\S]*});?\s*$/);
  if (!jsonMatch) {
    throw new Error('Failed to parse window.AUDIT_STATE from audit_state.js');
  }
  return eval('(' + jsonMatch[1] + ')');
}

function saveState(state) {
  state.projectInfo.lastUpdated = new Date().toISOString().replace('T', ' ').substring(0, 19);
  const formatted = `// iTalase Real-time Audit & Agentic State Store\n// Automatically maintained by Antigravity AI Agent Telemetry Protocol\n\nwindow.AUDIT_STATE = ${JSON.stringify(state, null, 2)};\n`;
  fs.writeFileSync(STATE_FILE, formatted, 'utf-8');
  console.log(`[iTalase Telemetry] Successfully synchronized state to ${STATE_FILE}`);
}

function printStatus(state) {
  console.log(`=== iTalase Agentic Dashboard Status ===`);
  console.log(`Project: ${state.projectInfo.name} (${state.projectInfo.version})`);
  console.log(`Device:  ${state.projectInfo.connectedDevice.model} (${state.projectInfo.connectedDevice.status})`);
  console.log(`Active:  ${state.currentTask.name} [${state.currentTask.progressPercent}%] - ${state.currentTask.status}`);
  console.log(`Coverage: ${state.kpis.testCoverage.instruction} (${state.kpis.testCoverage.passedTests})`);
  console.log(`Kanban:`);
  console.log(`  - Backlog:        ${(state.kanban.backlog || []).length} cards`);
  console.log(`  - In Progress:    ${(state.kanban.inProgress || []).length} cards`);
  console.log(`  - Verification:   ${(state.kanban.verified || []).length} cards`);
  console.log(`  - Recommendations:${(state.kanban.recommendations || []).length} cards`);
  console.log(`Permissions Logged: ${state.agenticPermissions.length}`);
}

function moveCard(state, cardIdOrKey, targetCol) {
  const validCols = ['backlog', 'inProgress', 'verified', 'done', 'recommendations'];
  if (!validCols.includes(targetCol)) {
    throw new Error(`Target column must be one of: ${validCols.join(', ')}`);
  }
  if (targetCol === 'done') targetCol = 'verified';

  let foundCard = null;
  for (const col of Object.keys(state.kanban)) {
    const list = state.kanban[col];
    if (Array.isArray(list)) {
      const idx = list.findIndex(c => c.id === cardIdOrKey || c.key === cardIdOrKey);
      if (idx !== -1) {
        foundCard = list.splice(idx, 1)[0];
        break;
      }
    }
  }

  if (!foundCard) {
    throw new Error(`Card "${cardIdOrKey}" not found in any Kanban column.`);
  }

  if (!state.kanban[targetCol]) {
    state.kanban[targetCol] = [];
  }
  state.kanban[targetCol].unshift(foundCard);
  console.log(`[iTalase Kanban] Moved card "${foundCard.title}" (${cardIdOrKey}) to ${targetCol}`);
}

function getNextCardId(state) {
  let maxNum = 0;
  for (const col of Object.keys(state.kanban || {})) {
    const list = state.kanban[col];
    if (Array.isArray(list)) {
      for (const card of list) {
        const idMatch = (card.id || '').match(/#AUD-(\d+)/i);
        if (idMatch) {
          const n = parseInt(idMatch[1], 10);
          if (n > maxNum) maxNum = n;
        }
        const keyMatch = (card.key || '').match(/CARD-(\d+)/i);
        if (keyMatch) {
          const n = parseInt(keyMatch[1], 10);
          if (n > maxNum) maxNum = n;
        }
      }
    }
  }
  const nextNum = maxNum + 1;
  const pad = String(nextNum).padStart(2, '0');
  return { id: `#AUD-${pad}`, key: `CARD-${pad}` };
}

function inferCategory(title, snippet) {
  const text = `${title} ${snippet}`.toLowerCase();
  if (/perf|speed|latensi|latency|optimi|fast|load|lag|cache/.test(text)) return { cat: 'perf', label: 'PERF & OPTIMIZATION' };
  if (/ui|ux|glass|shimmer|liquid|design|screen|layout|view|compose|preview|kanban|dashboard/.test(text)) return { cat: 'ui', label: 'UI / UX' };
  if (/security|keystore|sqlcipher|encrypt|auth|crypto|cinterop/.test(text)) return { cat: 'security', label: 'SECURITY' };
  if (/size|apk|dex|bundle|aab|shrink|minify|r8|proguard/.test(text)) return { cat: 'size', label: 'BUILD & SIZE' };
  if (/test|jacoco|coverage|assert|verify|instrumentation/.test(text)) return { cat: 'test', label: 'TESTING' };
  return { cat: 'arch', label: 'ARCHITECTURE' };
}

/**
 * Ensures that the active sprint task is 100% synchronized with the Kanban board.
 * If progress < 100:
 *   - Moves existing card to inProgress, or auto-creates a new card in inProgress if none exists.
 * If progress >= 100:
 *   - Moves the card to verified.
 */
function syncCurrentTaskToKanban(state, taskName, progressPercent, options = {}) {
  if (!state.kanban) state.kanban = { backlog: [], inProgress: [], verified: [], recommendations: [] };
  if (!state.kanban.inProgress) state.kanban.inProgress = [];
  if (!state.kanban.verified) state.kanban.verified = [];

  const targetCol = progressPercent >= 100 ? 'verified' : 'inProgress';
  const normTaskName = (taskName || '').trim().toLowerCase();

  // 1. Search if a card already matches this task name or specified cardKey
  let foundCard = null;
  let fromCol = null;

  for (const col of ['inProgress', 'backlog', 'recommendations', 'verified']) {
    const list = state.kanban[col] || [];
    for (let i = 0; i < list.length; i++) {
      const card = list[i];
      const cardTitle = (card.title || '').trim().toLowerCase();
      const matchesKey = options.cardKey && (card.key === options.cardKey || card.id === options.cardKey);
      const matchesExact = cardTitle === normTaskName;
      const matchesSub = normTaskName.length > 5 && cardTitle.includes(normTaskName);
      const matchesRev = cardTitle.length > 5 && normTaskName.includes(cardTitle);

      if (matchesKey || matchesExact || matchesSub || matchesRev) {
        foundCard = list.splice(i, 1)[0];
        fromCol = col;
        break;
      }
    }
    if (foundCard) break;
  }

  if (foundCard) {
    if (options.title) foundCard.title = options.title;
    if (options.snippet) foundCard.snippet = options.snippet;
    if (options.category) {
      foundCard.category = options.category;
      foundCard.categoryLabel = options.category.toUpperCase();
    }
    if (options.tag) foundCard.tag = options.tag;
    if (options.risk) foundCard.risk = options.risk;

    state.kanban[targetCol].unshift(foundCard);
    console.log(`[iTalase Kanban] Synchronized card "${foundCard.title}" (${foundCard.key}) [${fromCol} -> ${targetCol}]`);
    return foundCard;
  } else {
    // 2. Automatically create new card in targetCol so Kanban board is NEVER empty or out-of-sync
    const { id, key } = getNextCardId(state);
    const { cat, label } = inferCategory(taskName, options.snippet || options.description || '');
    const newCard = {
      id,
      key,
      title: options.title || taskName,
      category: options.category || cat,
      categoryLabel: (options.category || cat).toUpperCase(),
      snippet: options.snippet || options.description || `Autonomous sprint task: ${taskName}`,
      tag: options.tag || (targetCol === 'verified' ? 'Verified' : 'In Progress'),
      risk: options.risk || 'Normal'
    };
    state.kanban[targetCol].unshift(newCard);
    console.log(`[iTalase Kanban] Auto-created new Kanban card "${newCard.title}" (${key}) into "${targetCol}"`);
    return newCard;
  }
}

function addCard(state, column, id, title, snippet, category, tag, risk) {
  const col = column || 'recommendations';
  if (!state.kanban[col]) {
    state.kanban[col] = [];
  }
  const nextId = getNextCardId(state);
  const cardKey = nextId.key;
  const cardId = id ? (id.startsWith('#') ? id : `#${id}`) : nextId.id;
  const cat = category || 'arch';
  const newCard = {
    id: cardId,
    key: cardKey,
    title: title || 'Rekomendasi Task Otonom',
    category: cat,
    categoryLabel: cat.toUpperCase(),
    snippet: snippet || '',
    tag: tag || 'Recommendation',
    risk: risk || 'Normal'
  };
  state.kanban[col].unshift(newCard);
  console.log(`[iTalase Kanban] Added new card "${newCard.title}" (${cardKey}) to ${col}`);
  return newCard;
}

function logPermission(state, tool, action, target, status) {
  const newId = `PERM-${String(state.agenticPermissions.length + 1).padStart(2, '0')}`;
  const now = new Date();
  const timeStr = now.toISOString().replace('T', ' ').substring(0, 19);
  
  const perm = {
    id: newId,
    timestamp: timeStr,
    tool: tool || 'tool_execution',
    action: action || 'Autonomous Action',
    target: target || 'Workspace',
    permissionScope: 'Agentic Turbo Execution',
    status: status || 'EXECUTED'
  };

  state.agenticPermissions.unshift(perm);
  console.log(`[iTalase Permission] Logged ${newId}: ${tool} -> ${action}`);
}

function setCurrentTask(state, name, progressPercent, status, description, options = {}) {
  const progress = progressPercent !== undefined ? Number(progressPercent) : state.currentTask.progressPercent;
  const taskStatus = status || (progress >= 100 ? 'Completed' : 'In Progress');
  state.currentTask = {
    name: name || state.currentTask.name,
    progressPercent: progress,
    status: taskStatus,
    startedAt: state.currentTask.startedAt || new Date().toISOString().replace('T', ' ').substring(0, 19),
    description: description || state.currentTask.description
  };
  console.log(`[iTalase Task] Active task updated: "${state.currentTask.name}" (${state.currentTask.progressPercent}%)`);

  // MANDATORY AUTOMATIC KANBAN CARD SYNCHRONIZATION
  syncCurrentTaskToKanban(state, state.currentTask.name, progress, {
    description: state.currentTask.description,
    ...options
  });
}

function checkKanbanServer(port = 3300) {
  return new Promise((resolve) => {
    const req = http.get(`http://127.0.0.1:${port}/api/status`, { timeout: 1500 }, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve({ running: true, info: data });
        } else {
          resolve({ running: false });
        }
      });
    });
    req.on('error', () => resolve({ running: false }));
    req.on('timeout', () => { req.destroy(); resolve({ running: false }); });
  });
}

function checkCodebaseMcp() {
  try {
    const output = execSync('tasklist /fi "IMAGENAME eq codebase-memory-mcp.exe"', { encoding: 'utf-8', stdio: ['ignore', 'pipe', 'ignore'] });
    return output.toLowerCase().includes('codebase-memory-mcp.exe');
  } catch (e) {
    return false;
  }
}

async function ensureServices() {
  console.log(`\n=== Checking & Ensuring Background Services ===`);

  // 1. Check MCP (codebase-memory-mcp)
  const mcpRunning = checkCodebaseMcp();
  if (mcpRunning) {
    console.log(`[MCP Service] codebase-memory-mcp is ONLINE (Process active).`);
  } else {
    console.log(`[MCP Service] codebase-memory-mcp is NOT running. Launching on separate terminal...`);
    const mcpExe = 'C:\\Users\\anand\\AppData\\Local\\Programs\\codebase-memory-mcp\\codebase-memory-mcp.exe';
    if (fs.existsSync(mcpExe)) {
      try {
        const p = spawn('cmd.exe', ['/c', 'start', 'codebase-memory-mcp', mcpExe], {
          detached: true,
          stdio: 'ignore'
        });
        p.unref();
        console.log(`[MCP Service] Spawned codebase-memory-mcp in separate detached terminal.`);
      } catch (err) {
        console.warn(`[MCP Service] Failed to spawn MCP executable:`, err.message);
      }
    } else {
      console.log(`[MCP Service] Executable will be invoked on-demand by Antigravity stdio.`);
    }
  }

  // 2. Check Kanban Server
  let kanbanStatus = await checkKanbanServer(3300);
  if (kanbanStatus.running) {
    console.log(`[Kanban Server] Server is ONLINE on http://localhost:3300.`);
  } else {
    console.log(`[Kanban Server] Server is NOT running on port 3300. Launching via npm start on separate detached process...`);
    const workingDir = path.resolve(__dirname, '..');
    const vbsScript = path.resolve(__dirname, 'launch_server.vbs');
    try {
      if (process.platform === 'win32' && fs.existsSync(vbsScript)) {
        execSync(`wscript "${vbsScript}"`, { stdio: 'ignore' });
      } else {
        const p = spawn('npm', ['start'], {
          detached: true,
          stdio: 'ignore',
          cwd: workingDir,
          shell: true
        });
        p.unref();
      }
      // Wait for server to bind port
      await new Promise(r => setTimeout(r, 1500));
      kanbanStatus = await checkKanbanServer(3300);
      if (kanbanStatus.running) {
        console.log(`[Kanban Server] Successfully started with npm start and ONLINE on http://localhost:3300.`);
      } else {
        console.log(`[Kanban Server] Launch command executed. Verifying http://localhost:3300...`);
      }
    } catch (err) {
      console.error(`[Kanban Server] Failed to spawn Kanban server via npm start:`, err.message);
    }
  }
  console.log(`===============================================\n`);
}

// CLI Argument Parsing
const args = process.argv.slice(2);
if (args.length === 0 || args.includes('--help')) {
  console.log(`
Usage: node update_dashboard.js [options]

Commands:
  --status                                Print current dashboard & kanban overview
  --ensure-services                       Check and auto-start MCP & Kanban server on separate terminal
  --move <CARD_KEY> --to <COLUMN>         Move card (columns: backlog, inProgress, verified, recommendations)
  --add-rec --title <T> --snippet <S>     Add a proactive recommendation card to the Recommendations column
  --set-task <NAME> --progress <NUM>      Update active sprint task (auto-syncs or creates Kanban card)
  --start-task <NAME>                     Start new sprint task (auto-places into inProgress column)
  --complete-task <NAME>                  Complete sprint task (auto-moves into verified column)
  --log-perm <TOOL> <ACTION> <TARGET>     Record an autonomous tool permission execution
  --test-coverage <PERCENT>               Update JaCoCo test coverage metric
  `);
  process.exit(0);
}

(async () => {
  try {
    const state = loadState();

    if (args.includes('--ensure-services')) {
      await ensureServices();
    }

    if (args.includes('--status')) {
      printStatus(state);
    } else if (args.includes('--move')) {
      const cardIdx = args.indexOf('--move') + 1;
      const toIdx = args.indexOf('--to') + 1;
      if (!args[cardIdx] || !args[toIdx]) {
        throw new Error('Must provide card ID/Key and target column, e.g., --move CARD-01 --to inProgress');
      }
      moveCard(state, args[cardIdx], args[toIdx]);
      saveState(state);
    } else if (args.includes('--set-task') || args.includes('--start-task') || args.includes('--complete-task')) {
      let taskName = '';
      let progress = 10;
      let status = 'In Progress';

      if (args.includes('--start-task')) {
        taskName = args[args.indexOf('--start-task') + 1];
        progress = 10;
        status = 'In Progress';
      } else if (args.includes('--complete-task')) {
        taskName = args[args.indexOf('--complete-task') + 1];
        progress = 100;
        status = 'Completed';
      } else {
        const taskIdx = args.indexOf('--set-task') + 1;
        const progIdx = args.indexOf('--progress') + 1;
        taskName = args[taskIdx];
        progress = progIdx > 0 ? Number(args[progIdx]) : (args.includes('--complete') ? 100 : 10);
        status = progress >= 100 ? 'Completed' : 'In Progress';
      }

      const descIdx = args.indexOf('--desc') !== -1 ? args.indexOf('--desc') + 1 : (args.indexOf('--description') !== -1 ? args.indexOf('--description') + 1 : -1);
      const catIdx = args.indexOf('--cat') !== -1 ? args.indexOf('--cat') + 1 : (args.indexOf('--category') !== -1 ? args.indexOf('--category') + 1 : -1);
      const tagIdx = args.indexOf('--tag') !== -1 ? args.indexOf('--tag') + 1 : -1;
      const cardKeyIdx = args.indexOf('--card') !== -1 ? args.indexOf('--card') + 1 : (args.indexOf('--card-key') !== -1 ? args.indexOf('--card-key') + 1 : -1);
      const snippetIdx = args.indexOf('--snippet') !== -1 ? args.indexOf('--snippet') + 1 : -1;
      const riskIdx = args.indexOf('--risk') !== -1 ? args.indexOf('--risk') + 1 : -1;

      const options = {
        description: descIdx > 0 ? args[descIdx] : null,
        snippet: snippetIdx > 0 ? args[snippetIdx] : (descIdx > 0 ? args[descIdx] : null),
        category: catIdx > 0 ? args[catIdx] : null,
        tag: tagIdx > 0 ? args[tagIdx] : null,
        cardKey: cardKeyIdx > 0 ? args[cardKeyIdx] : null,
        risk: riskIdx > 0 ? args[riskIdx] : null
      };

      setCurrentTask(state, taskName, progress, status, options.description, options);
      saveState(state);
    } else if (args.includes('--log-perm')) {
      const baseIdx = args.indexOf('--log-perm');
      const tool = args[baseIdx + 1];
      const action = args[baseIdx + 2];
      const target = args[baseIdx + 3];
      logPermission(state, tool, action, target, 'EXECUTED');
      saveState(state);
    } else if (args.includes('--test-coverage')) {
      const covIdx = args.indexOf('--test-coverage') + 1;
      state.kpis.testCoverage.instruction = args[covIdx] + '%';
      console.log(`[iTalase KPI] Updated test coverage to ${args[covIdx]}%`);
      saveState(state);
    } else if (args.includes('--add-rec') || args.includes('--add-card')) {
      const col = args.includes('--col') ? args[args.indexOf('--col') + 1] : 'recommendations';
      const id = args.includes('--id') ? args[args.indexOf('--id') + 1] : null;
      const title = args.includes('--title') ? args[args.indexOf('--title') + 1] : 'Autonomous Task';
      const snippet = args.includes('--snippet') ? args[args.indexOf('--snippet') + 1] : '';
      const category = args.includes('--category') ? args[args.indexOf('--category') + 1] : (args.includes('--cat') ? args[args.indexOf('--cat') + 1] : 'arch');
      const tag = args.includes('--tag') ? args[args.indexOf('--tag') + 1] : 'Recommendation';
      const risk = args.includes('--risk') ? args[args.indexOf('--risk') + 1] : 'Normal';
      addCard(state, col, id, title, snippet, category, tag, risk);
      saveState(state);
    }

  } catch (err) {
    console.error(`Error: ${err.message}`);
    process.exit(1);
  }
})();
