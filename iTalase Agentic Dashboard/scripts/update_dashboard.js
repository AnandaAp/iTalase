/**
 * iTalase Agentic Telemetry State Controller
 * Helper CLI utility for Antigravity AI Agents to query and update audit_state.js
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

function addCard(state, column, id, title, snippet, category, tag, risk) {
  const col = column || 'recommendations';
  if (!state.kanban[col]) {
    state.kanban[col] = [];
  }
  const cardKey = `CARD-${Math.floor(10 + Math.random() * 90)}`;
  const cardId = id ? (id.startsWith('#') ? id : `#${id}`) : `#AUD-${Math.floor(10 + Math.random() * 90)}`;
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

function setCurrentTask(state, name, progressPercent, status, description) {
  state.currentTask = {
    name: name || state.currentTask.name,
    progressPercent: progressPercent !== undefined ? Number(progressPercent) : state.currentTask.progressPercent,
    status: status || state.currentTask.status,
    startedAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
    description: description || state.currentTask.description
  };
  console.log(`[iTalase Task] Active task updated: "${state.currentTask.name}" (${state.currentTask.progressPercent}%)`);
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
  --status                              Print current dashboard & kanban overview
  --ensure-services                     Check and auto-start MCP & Kanban server on separate terminal
  --move <CARD_KEY> --to <COLUMN>       Move card (columns: backlog, inProgress, verified, recommendations)
  --add-rec --title <T> --snippet <S>   Add a proactive recommendation card to the Recommendations column
  --set-task <NAME> --progress <NUM>    Update current active task and percentage
  --log-perm <TOOL> <ACTION> <TARGET>   Record an autonomous tool permission execution
  --test-coverage <PERCENT>             Update JaCoCo test coverage metric
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
    } else if (args.includes('--set-task')) {
      const taskIdx = args.indexOf('--set-task') + 1;
      const progIdx = args.indexOf('--progress') + 1;
      const taskName = args[taskIdx];
      const progress = progIdx > 0 ? args[progIdx] : 50;
      setCurrentTask(state, taskName, progress, 'In Progress');
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
