function TelemetryView({ state, toolFilter, setToolFilter, filteredPermissions }) {
  if (!state) return null;

  const permissions = filteredPermissions || state.agenticPermissions || [];
  const activityLogs = state.activityLogs || [];

  return (
    <div className="space-y-5 view-enter">
      <div className="glass-surface p-6 rounded-3xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
          <div>
            <h2 className="text-xl font-bold text-white">Engineering Telemetry &amp; Permission Audit</h2>
            <p className="text-xs text-slate-400">Rekaman forensik seluruh operasi autonomous agent dalam workspace iTalase</p>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-xs font-mono px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-slate-300">
              Total Permissions: <span className="font-bold text-[#FFA03A]">{state.agenticPermissions?.length || 0}</span>
            </div>
            <div className="text-xs font-mono px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-slate-300">
              Activity Logs: <span className="font-bold text-emerald-400">{activityLogs.length}</span>
            </div>
          </div>
        </div>

        {/* Filter Row */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            {['ALL', 'run_command', 'write_file', 'edit_file'].map((tool) => (
              <button
                key={tool}
                onClick={() => setToolFilter(tool)}
                className={`btn-tactile px-3.5 py-1.5 rounded-full text-xs font-mono transition ${
                  toolFilter === tool 
                    ? 'bg-white text-black font-bold shadow' 
                    : 'bg-white/[0.04] text-slate-300 border border-white/10 hover:bg-white/[0.08]'
                }`}
              >
                {tool}
              </button>
            ))}
          </div>
        </div>

        {/* Telemetry Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-white/10 text-slate-400">
                <th className="py-2.5 px-3">Tool</th>
                <th className="py-2.5 px-3">Aksi / Operasi</th>
                <th className="py-2.5 px-3">Target File / Command</th>
                <th className="py-2.5 px-3">Timestamp (WIB)</th>
                <th className="py-2.5 px-3 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04]">
              {permissions.map((perm, idx) => (
                <tr key={perm.id + idx} className="hover:bg-white/[0.02] transition">
                  <td className="py-2.5 px-3 font-bold text-white">
                    <span className={`px-2 py-0.5 rounded border text-[10px] ${
                      perm.tool === 'run_command' 
                        ? 'bg-amber-500/15 border-amber-500/30 text-amber-400' 
                        : (perm.tool === 'write_file' 
                            ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400' 
                            : 'bg-sky-500/15 border-sky-500/30 text-sky-400')
                    }`}>
                      {perm.tool}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-slate-200">{perm.action}</td>
                  <td className="py-2.5 px-3 text-slate-400 truncate max-w-xs">{perm.target}</td>
                  <td className="py-2.5 px-3 text-slate-300">
                    <TimeHoverCell timestamp={perm.timestamp} label="Waktu Eksekusi" />
                  </td>
                  <td className="py-2.5 px-3 text-right">
                    <span className="px-2 py-0.5 rounded-full text-[10px] bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 font-bold">
                      {perm.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Activity Log Stream */}
        <div className="pt-4 border-t border-white/10 space-y-3">
          <h3 className="text-xs font-bold text-slate-300 font-sans uppercase tracking-wider">Chronological Activity Stream</h3>
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {activityLogs.slice(0, 15).map((log, idx) => (
              <div key={log.id + idx} className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.04] flex items-center justify-between text-xs font-mono">
                <div className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#FF5E0E]"></span>
                  <span className="text-white">{log.text}</span>
                </div>
                <TimeHoverCell timestamp={log.timestamp} label="Waktu Aktivitas" />
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.TelemetryView = TelemetryView;
}
