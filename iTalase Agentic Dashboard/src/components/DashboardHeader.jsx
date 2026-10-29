function DashboardHeader({ buildTarget, state, serverOnline, isLiveSync, setIsLiveSync, adbInfo }) {
  return (
    <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white font-sans">
            iTalase Agentic Console
          </h1>
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-[#FF5E0E]/20 text-[#FFA03A] border border-[#FF5E0E]/30" title="Source: gradle/libs.versions.toml">
            KMP POS v{buildTarget.version || state?.projectInfo?.version || '1.0.0'} (SDK {buildTarget.targetSdk || state?.projectInfo?.targetSdk || 37})
          </span>
        </div>
        <p className="text-xs text-slate-400 mt-1 font-medium">
          Real-time Autonomous Sprint Control • JaCoCo Coverage Gate • Multiplatform POS Telemetry
        </p>
      </div>

      {/* Right Telemetry Status Bar */}
      <div className="flex items-center gap-3">
        <div 
          className={`flex items-center gap-2 px-3.5 py-1.5 rounded-full border text-xs font-mono transition ${
            serverOnline 
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' 
              : 'bg-white/5 border-white/10 text-slate-400'
          }`}
        >
          <span className={`w-2 h-2 rounded-full ${serverOnline ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`}></span>
          <span>{serverOnline ? 'Disk Server Online' : 'Local Storage'}</span>
        </div>

        <button 
          onClick={() => setIsLiveSync(!isLiveSync)}
          className={`px-3 py-1.5 rounded-full border flex items-center gap-2 text-xs font-mono transition ${
            isLiveSync 
              ? 'bg-[#FF5E0E]/15 border-[#FF5E0E]/40 text-[#FFA03A]' 
              : 'bg-white/[0.04] border-white/10 text-slate-400'
          }`}
          title="Toggle SSE Live Sync"
        >
          <span className={`w-2 h-2 rounded-full ${isLiveSync ? 'bg-[#FF5E0E] shadow-[0_0_6px_#FF5E0E]' : 'bg-slate-500'}`}></span>
          <span>{isLiveSync ? 'Live Sync' : 'Paused'}</span>
        </button>

        {/* Target Hardware / Dynamic ADB Badge */}
        <div className="flex items-center gap-2 pl-2 border-l border-white/10">
          <div className={`w-8 h-8 rounded-full p-0.5 shadow-sm transition-all ${
            adbInfo.online 
              ? 'bg-gradient-to-tr from-[#FF5E0E] to-[#FFA03A]' 
              : 'bg-white/10 border border-white/10'
          }`}>
            <div className={`w-full h-full rounded-full flex items-center justify-center text-[10px] font-bold font-mono ${
              adbInfo.online 
                ? 'bg-[#161922] text-white' 
                : 'bg-[#13151D] text-slate-400'
            }`}>
              {adbInfo.online ? (adbInfo.activeDevice?.badge || 'DEV') : 'ADB'}
            </div>
          </div>
          <div className="hidden md:block text-left font-mono">
            <div className="text-[10px] font-bold text-white leading-tight">
              {adbInfo.online ? (adbInfo.activeDevice?.label || adbInfo.activeDevice?.model || 'Device') : 'ADB Disconnected'}
            </div>
            <div className={`text-[9px] leading-tight font-semibold flex items-center gap-1 ${
              adbInfo.online ? 'text-emerald-400' : 'text-slate-500'
            }`}>
              <span className={`w-1.5 h-1.5 rounded-full ${adbInfo.online ? 'bg-emerald-400 animate-pulse' : 'bg-slate-600'}`}></span>
              <span>{adbInfo.online ? (adbInfo.activeDevice?.isEmulator ? 'Connected (VD)' : 'Connected (USB)') : 'Deactive / Standby'}</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}

if (typeof window !== 'undefined') {
  window.DashboardHeader = DashboardHeader;
}
