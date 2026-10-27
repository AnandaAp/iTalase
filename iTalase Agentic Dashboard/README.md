# iTalase Agentic Dashboard (React.js Edition)

Interactive, real-time AI engineering telemetry dashboard & Kanban board for the iTalase project, built with **React 18**, **Tailwind CSS**, and **Apple Design Fluid Interface** principles.

---

## 🌟 Key Features

1. **React 18 Reactive Architecture**:
   - Built with modern React hooks (`useState`, `useEffect`, `useMemo`, `useCallback`, `useRef`).
   - Dynamic zero-CORS poller syncing with `audit_state.js` every 2.5 seconds.
   - Live sync toggle to pause and resume live updates.
2. **Interactive Kanban Board**:
   - Native HTML5 Drag and Drop across 4 workflow columns: *Backlog*, *In Progress*, *Verified & Tested*, and *Recommendations*.
   - Quick one-click directional navigation buttons (`←` / `→`).
   - Interactive Detail Modal with risk scope and execution metadata.
   - Interactive "+ Add Task" modal for adding new engineering tasks directly from the UI.
   - Live Search & Category Filtering (`ARCH`, `SECURITY`, `PERF`, `SIZE`).
3. **Telemetry & Autonomous Permissions Audit**:
   - Real-time KPI cards: Release APK size reduction (-13.3%), Single DEX bytecode elimination (-39%), JaCoCo test coverage (76.18% >= 75%), SQLCipher hardware encryption (100%).
   - Live filterable table of all tool permissions executed under Turbo Mode (`run_command`, `write_file`, `edit_file`).
   - Chronological engineering event feed with exact timestamps.
   - Live ADB physical hardware status (Samsung Galaxy A54 5G).
4. **Security & Offline Safety**:
   - Zero `eval()`, zero `dangerouslySetInnerHTML`, pure React JSX sanitization.
   - Works 100% offline and locally without external backend dependencies.

---

## 🚀 How to Run

### Option A: Instant Zero-Build Browser Launch (Recommended)

Simply double-click `index.html` or launch it from terminal:

```powershell
start "iTalase Agentic Dashboard/index.html"
```

No `node_modules` or build setup needed. React 18 and Tailwind render immediately in Google Chrome, Microsoft Edge, Firefox, or Safari.

### Option B: Vite React Dev Server

If you want to run the modular Vite development server:

```cmd
cd "iTalase Agentic Dashboard"
npm install
npm run dev
```

---

## 🤖 AI Agent CLI Helper

AI Agents and terminal scripts can inspect and update the dashboard state directly via the included utility:

```powershell
# Check current telemetry and kanban status
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --status

# Move a card to a different column
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --move CARD-01 --to inProgress

# Update current active sprint task
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --set-task "Refactoring Cashier Screen" --progress 75

# Log an autonomous tool permission
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --log-perm run_command "Execute Unit Tests" ":shared:testAndroidHostTest"
```
