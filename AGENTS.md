# iTalase Agentic Rules & Telemetry Protocol

You are operating inside the **iTalase** project repository (`com.anlian.italase`). All AI agents working on this project must strictly comply with the following mandatory rules and telemetry protocols:

---

## 1. Mandatory Dashboard & Kanban Synchronization
The project maintains a live, interactive **React 18 Agentic Dashboard** located at `iTalase Agentic Dashboard/index.html`.
The telemetry state store is stored at `iTalase Agentic Dashboard/audit_state.js`.

### Telemetry Lifecycle Protocol:
1. **At Task Start**:
   - Ensure Kanban Dashboard Server & MCP are running on a separate non-blocking terminal:
     Run: `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --ensure-services`
     (Or run `npm start` in `iTalase Agentic Dashboard` in a separate external terminal. NEVER run the server as a blocking foreground command in the conversation session).
   - Check status: `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --status`
   - Set active task banner and move relevant Kanban card to `inProgress`:
     `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --set-task "<Task Name>" --progress 10`
     `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --move "<CARD_KEY>" --to inProgress`
2. **During Autonomous Tool Executions**:
   - When running significant commands (`run_command`), creating files (`write_file`), or modifying code (`edit_file`), record the operation:
     `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --log-perm <tool> "<action>" "<target>"`
3. **At Task Verification & Completion**:
   - Verify JaCoCo coverage remains $\ge 75\%$: `./gradlew :shared:testAndroidHostTest :shared:jacocoTestCoverageVerification`
   - Move card to `verified`:
     `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --move "<CARD_KEY>" --to verified`
   - Set active task to completed:
     `node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --set-task "<Task Name>" --progress 100`

---

## 2. Absolute Safety Rule: Zero Unauthorized Git Commits
- **DO NOT EXECUTE `git commit` OR `git push`** under any circumstance unless the user gives an explicit, verbatim instruction to commit in their prompt.
- Always leave modifications and new files cleanly staged or in the working tree for user inspection.

---

## 3. Code Coverage & Architecture Standards
- **Test Coverage Gate**: Minimum **75% instruction coverage** verified by JaCoCo (`:shared:jacocoTestCoverageVerification`).
- **UI Design System**: Preserve *Warung Liquid Glass* tokens (specular rim, blurred glass cards, dark warung emerald palette, Apple spring physics).
- **Orientation**: Maintain responsive support for both Portrait and Landscape (e.g. POS split pane in Cashier, dual-grid in Dashboard, scrollable dialog containers).

---

## 4. Autonomous Reasoning Engine & iOS Hardware Quarantine
- **Zero Interruption Autonomy**: In scheduled `/automation` sidecars or autonomous sprints, the agent MUST NOT halt or ask the user "Which task should I do?". The agent must autonomously reason through candidates and make decisions independently.
- **iOS Hardware Quarantine**: The current development environment is Windows without macOS/Xcode hardware. All iOS-specific tasks (e.g. `CARD-02`, Swift, NSData cinterop, iOS lifecycle) MUST remain quarantined in `backlog` with the tag `iOS Quarantine`. NEVER pull iOS tasks into `inProgress`.
- **Mandatory Proactive Daily Recommendations (4 Pillars)**:
  At the beginning of every daily automation window (09:00 - 14:00 WIB), the agent MUST NOT only check the backlog. It MUST actively inspect the codebase with `codebase-memory-mcp` and synthesize fresh candidate tasks across the 4 core pillars:
  1. **Improvement & Enhancement**: UI/UX refinements (Warung design tokens, responsive split pane layouts, receipt/export improvements).
  2. **Optimization**: Database query indexes (`debts.saleId`, `sales.createdAt`), eliminating unnecessary recompositions in Compose, memory safety, Baseline Profiles, bytecode/AAB shrinking.
  3. **General Tasks & Code Hygiene**: Dependency deprecation audit, dead code elimination, SQLCipher hardware key protection, ADR updates.
  4. **Verification & Testing**: Expanding JaCoCo test coverage ($\ge 75\%$), automated Room migration tests (`MigrationTestHelper`), on-device connected instrumentation tests on Samsung Galaxy A54 / virtual emulator, screen rotation stress tests.
  These tasks MUST be registered into the `recommendations` column in `audit_state.js` (`--add-rec`).
- **Dual-Source Triage & Task Decomposition**:
  The agent reasons across BOTH existing non-iOS `backlog` items AND its newly synthesized `recommendations`. It selects the highest-value 1–2 candidates to execute today, promotes them to `inProgress`, and leaves the rest in `recommendations` for user visibility. Complex tasks must be decomposed into small, incremental, interrelated sub-tasks for easy tracking.
