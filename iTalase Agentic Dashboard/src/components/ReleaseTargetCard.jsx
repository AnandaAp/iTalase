function ReleaseTargetCard({ adbInfo, buildTarget, state }) {
  return (
    <div className="glass-surface rounded-[32px] p-5 grid grid-cols-2 gap-3 items-center">
      <div className="space-y-1">
        <div className="text-xs text-slate-400 font-medium">Release Target</div>
        <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-bold">
          <span>~ 100% Verified</span>
        </div>
        <div className="text-2xl sm:text-3xl font-extrabold text-emerald-400 tracking-tight pt-1">
          v1.0.0
        </div>
        <div className="text-[11px] text-slate-400 font-mono">Signed Release APK</div>
      </div>

      {/* Hardware / Runtime Target Inset Frame */}
      <div className="bg-white/[0.04] rounded-2xl p-3 border border-white/[0.08] text-center space-y-1">
        <div className={`w-10 h-10 mx-auto rounded-xl flex items-center justify-center text-lg shadow-lg ${
          adbInfo.online 
            ? 'bg-gradient-to-tr from-slate-800 to-slate-700 text-white' 
            : 'bg-white/[0.05] text-slate-300 border border-white/5'
        }`}>
          {adbInfo.online ? (adbInfo.activeDevice?.isEmulator ? '💻' : '📱') : '🤖'}
        </div>
        <div className="text-xs font-bold text-white truncate">
          {adbInfo.online ? adbInfo.activeDevice?.label : 'Android Platform'}
        </div>
        <div className="text-[10px] text-[#FFA03A] font-mono font-bold" title="Parsed dynamically from gradle/libs.versions.toml">
          Target SDK {buildTarget.targetSdk || state?.projectInfo?.targetSdk || 37}
        </div>
        <div className="text-[9px] text-slate-400 font-mono">
          {adbInfo.online ? (adbInfo.activeDevice?.isEmulator ? 'VD Active' : 'USB Active') : `Min SDK ${buildTarget.minSdk || state?.projectInfo?.minSdk || 26} (Android 8+)`}
        </div>
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.ReleaseTargetCard = ReleaseTargetCard;
}
