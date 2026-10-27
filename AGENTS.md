# iTalase Agentic Rules & Telemetry Protocol

You are operating inside the **iTalase** project repository (`com.anlian.italase`). All AI agents working on this project must strictly comply with the following mandatory rules and telemetry protocols:

---

## 1. Mandatory Dashboard & Kanban Synchronization
The project maintains a live, interactive **React 18 Agentic Dashboard** located at `iTalase Agentic Dashboard/index.html`.
The telemetry state store is stored at `iTalase Agentic Dashboard/audit_state.js`.

### Telemetry Lifecycle Protocol:
1. **At Task Start**:
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
