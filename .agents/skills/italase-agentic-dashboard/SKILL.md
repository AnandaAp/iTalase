---
name: italase-agentic-dashboard
description: Connects AI agents to the iTalase React Agentic Dashboard and Kanban telemetry system. Automatically activates when working on the iTalase POS & Accounting codebase (com.anlian.italase) to track tasks in real time, record autonomous tool permissions, and update project KPIs.
---

# iTalase Agentic Dashboard & Telemetry Connector Skill

This skill governs the integration between Antigravity AI Agents and the **iTalase React Agentic Dashboard**. It ensures that any agent operating on the iTalase repository synchronizes its workflow state, moves Kanban cards in real-time, records autonomous execution permissions, and updates engineering metrics.

---

## 🎯 Activation Conditions

Activate and execute this skill whenever:
1. Working inside the `iTalase` repository (`c:\Users\anand\StudioProjects\iTalase`).
2. Planning, starting, executing, or verifying any feature, bugfix, or refactoring in iTalase.
3. User asks about progress, Kanban status, test coverage, or dashboard telemetry.

---

## 🏛️ System Architecture

- **Dashboard UI**: Modern **React 18** Single-Page Application with Apple Design Fluid Interface at [`iTalase Agentic Dashboard/index.html`](file:///c:/Users/anand/StudioProjects/iTalase/iTalase%20Agentic%20Dashboard/index.html).
- **Reactive State Store**: [`iTalase Agentic Dashboard/audit_state.js`](file:///c:/Users/anand/StudioProjects/iTalase/iTalase%20Agentic%20Dashboard/audit_state.js) (loaded via zero-CORS dynamic script polling every 2.5 seconds).
- **CLI Helper Tool**: [`iTalase Agentic Dashboard/scripts/update_dashboard.js`](file:///c:/Users/anand/StudioProjects/iTalase/iTalase%20Agentic%20Dashboard/scripts/update_dashboard.js).

---

## 🔄 Mandatory Agent Lifecycle Protocol

Whenever an AI agent is working on a task in iTalase, the agent MUST follow this 4-phase protocol:

### Phase 1: Task Inspection & Ingestion (Sprint Start)
Before touching any Kotlin, Compose, or Gradle code, ensure background services and check current dashboard status:
```powershell
# 1. Ensure MCP and Kanban Server are running on a detached terminal (via npm start / launch_server.vbs)
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --ensure-services

# 2. Check current telemetry & kanban status
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --status
```
Update the current active task and automatically synchronize the Kanban card into `inProgress`:
```powershell
# Set the active task banner (automatically finds or creates card in inProgress)
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --set-task "<Brief Task Name>" --progress 10 --desc "<Brief description>"
```
*(The React Dashboard will instantly animate the card sliding into the In Progress column with glowing feedback).*

### Phase 2: Autonomous Tool Permission Logging
Under Turbo Mode or autonomous operation, record high-impact tool executions (`run_command`, `write_file`, `edit_file`) into the telemetry state:
```powershell
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --log-perm "<tool_name>" "<action_description>" "<target_path_or_command>"
```

### Phase 3: Verification & KPI Update
After completing the code implementation:
1. Run test suite and verify JaCoCo coverage remains $\ge 75\%$:
   ```powershell
   ./gradlew :shared:testAndroidHostTest :shared:jacocoTestCoverageVerification
   ```
2. Update coverage metric in dashboard:
   ```powershell
   node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --test-coverage 76.18
   ```

### Phase 4: Task Closure (Done & Verified)
Mark the active task completed and automatically transition the card to `verified`:
```powershell
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --set-task "<Brief Task Name>" --progress 100
```

---

## 🛡️ Critical Safety Constraints
- **NEVER RUN `git commit` OR `git push`** without explicit user permission.
- **NEVER HALT TO ASK USER FOR SELECTION**: Autonomous orchestrators must reason, decide, and execute candidates independently.
- **iOS HARDWARE QUARANTINE**: All iOS/Swift/Xcode tasks remain in `backlog` until macOS hardware is present.
- Always maintain minimum **75% JaCoCo test coverage**.
- Preserve the *Warung Liquid Glass* and *Apple Design* visual standards.
