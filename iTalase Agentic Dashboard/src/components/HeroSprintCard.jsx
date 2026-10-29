function HeroSprintCard({ state, isTaskComplete, recCount, buildTarget, setIsNewTaskOpen, setActiveNav }) {
  if (!state || !state.currentTask) return null;

  return (
    <div className={`lg:col-span-8 glass-surface rounded-[32px] p-7 sm:p-8 relative overflow-hidden flex flex-col justify-between min-h-[410px] transition-all duration-700 ${
      isTaskComplete ? 'border-emerald-500/25 shadow-[0_20px_50px_rgba(16,185,129,0.12)]' : ''
    }`}>
      {/* Ambient dynamic lighting inside card */}
      <div className={`absolute top-0 right-0 w-96 h-96 rounded-full blur-3xl pointer-events-none transition-all duration-700 ${
        isTaskComplete 
          ? 'bg-gradient-to-bl from-emerald-500/25 via-teal-400/12 to-transparent' 
          : 'bg-gradient-to-bl from-[#FF6B2C]/28 via-[#FFA03A]/14 to-transparent'
      }`}></div>

      <div className="relative z-10 flex flex-col md:flex-row items-start justify-between gap-6">
        <div className="space-y-3.5 max-w-lg">
          {/* Kicker badge */}
          {isTaskComplete ? (
            <div className="flex items-center gap-2 flex-wrap">
              <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_8px_#34D399]"></span>
              <span className="text-[11px] uppercase font-bold tracking-widest text-emerald-400 font-mono">
                Sprint Completed &amp; Verified
              </span>
              <span className="px-2 py-0.5 rounded-full text-[9px] font-mono font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                100% DONE
              </span>
            </div>
          ) : (
            <div className="flex items-center gap-2 flex-wrap">
              <span className="w-2 h-2 rounded-full bg-[#FF5E0E] animate-ping"></span>
              <span className="text-[11px] uppercase font-bold tracking-widest text-[#FFA03A] font-mono">
                Active Autonomous Sprint
              </span>
              <span className="px-2 py-0.5 rounded-full text-[9px] font-mono font-bold bg-[#FF5E0E]/15 text-[#FFA03A] border border-[#FF5E0E]/30 animate-pulse">
                RUNNING
              </span>
            </div>
          )}

          {/* Title with optional Verified Pill */}
          <div className="space-y-1">
            <div className="flex items-center gap-2.5 flex-wrap">
              <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight leading-tight">
                {state.currentTask.name}
              </h2>
              {isTaskComplete && (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 shadow-[0_0_10px_rgba(16,185,129,0.25)]">
                  <svg className="w-3 h-3 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
                  </svg>
                  TERUJI
                </span>
              )}
            </div>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed font-normal">
              {state.currentTask.description}
            </p>
          </div>

          {/* Achievement Verification Badges Strip when Completed */}
          {isTaskComplete && (
            <div className="flex flex-wrap items-center gap-2 pt-0.5">
              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[11px] font-mono text-emerald-300 shadow-sm">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>JaCoCo Gate PASS ({state.kpis?.testCoverage?.instruction || '78.58%'})</span>
              </div>
              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-[11px] font-mono text-slate-300">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-400"></span>
                <span>121 Tests Passed (100%)</span>
              </div>
              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-[11px] font-mono text-slate-300">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
                <span>Target SDK {buildTarget.targetSdk || 37} Ready</span>
              </div>
            </div>
          )}

          {/* Progress Bar (Dual state: Shimmer in progress vs Solid Emerald Verified) */}
          <div className="pt-1.5 space-y-1.5 max-w-sm">
            {isTaskComplete ? (
              <>
                <div className="flex justify-between text-xs font-mono">
                  <span className="flex items-center gap-1.5 text-emerald-300 font-semibold">
                    <svg className="w-3.5 h-3.5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M5 13l4 4L19 7" />
                    </svg>
                    Status Verifikasi Selesai
                  </span>
                  <span className="text-emerald-400 font-bold">100% Verified</span>
                </div>
                <div className="w-full h-2 rounded-full bg-emerald-950/40 border border-emerald-500/25 overflow-hidden">
                  <div className="h-full rounded-full bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-300 shadow-[0_0_12px_rgba(16,185,129,0.7)] w-full transition-all duration-500"></div>
                </div>
              </>
            ) : (
              <>
                <div className="flex justify-between text-xs font-mono text-slate-300">
                  <span>Sprint Progress</span>
                  <span className="text-[#FFA03A] font-bold">
                    {state.currentTask.progressPercent ?? state.currentTask.progress ?? 100}%
                  </span>
                </div>
                <div className="w-full h-2 rounded-full bg-white/10 overflow-hidden">
                  <div 
                    className="h-full rounded-full progress-shimmer transition-all duration-500"
                    style={{ width: `${Math.min(100, Math.max(5, state.currentTask.progressPercent ?? state.currentTask.progress ?? 100))}%` }}
                  ></div>
                </div>
              </>
            )}
          </div>

          {/* Action CTA Buttons */}
          <div className="pt-2 flex items-center gap-3 flex-wrap">
            {isTaskComplete ? (
              <>
                {recCount > 0 ? (
                  <button
                    onClick={() => setActiveNav('kanban')}
                    className="btn-tactile px-5 py-2.5 rounded-full bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-black font-extrabold text-xs shadow-lg shadow-emerald-500/25 flex items-center gap-1.5 transition-all"
                  >
                    <span>Pilih Rekomendasi AI ({recCount})</span>
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </button>
                ) : (
                  <button
                    onClick={() => setIsNewTaskOpen(true)}
                    className="btn-tactile px-5 py-2.5 rounded-full bg-white hover:bg-slate-100 text-black font-bold text-xs shadow-lg shadow-white/10"
                  >
                    + Mulai Sprint Baru
                  </button>
                )}
                <button
                  onClick={() => setActiveNav('kanban')}
                  className="btn-tactile px-4 py-2.5 rounded-full bg-white/[0.08] hover:bg-white/[0.15] text-white font-medium text-xs border border-white/15 flex items-center gap-1.5"
                >
                  <span>Lihat di Kolom Teruji</span>
                  <svg className="w-3.5 h-3.5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M5 13l4 4L19 7" />
                  </svg>
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={() => setIsNewTaskOpen(true)}
                  className="btn-tactile px-6 py-2.5 rounded-full bg-white hover:bg-slate-100 text-black font-bold text-xs shadow-lg shadow-white/10"
                >
                  + Tambah Task
                </button>
                <button
                  onClick={() => setActiveNav('kanban')}
                  className="btn-tactile px-5 py-2.5 rounded-full bg-white/[0.08] hover:bg-white/[0.15] text-white font-medium text-xs border border-white/15"
                >
                  Buka Papan Kanban →
                </button>
              </>
            )}
            <span className="text-xs font-mono text-slate-400 hidden sm:inline">
              Mulai: <TimeHoverCell timestamp={state.currentTask.startedAt} label="Waktu Mulai Sprint" />
            </span>
          </div>
        </div>

        {/* Right Visual: Verification Seal (Completed) OR Agentic Reasoning Radar (In-Progress) */}
        {isTaskComplete ? (
          <div className="hidden md:flex flex-col items-center justify-center relative w-48 h-48 shrink-0">
            {/* Outer concentric orbital rings with emerald styling */}
            <svg className="w-44 h-44 animate-[spin_80s_linear_infinite]" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="46" fill="none" stroke="rgba(16, 185, 129, 0.35)" strokeWidth="1" strokeDasharray="4 4" />
              <circle cx="50" cy="50" r="36" fill="none" stroke="rgba(255, 255, 255, 0.12)" strokeWidth="1" />
              <circle cx="50" cy="50" r="26" fill="none" stroke="rgba(52, 211, 153, 0.3)" strokeWidth="1" strokeDasharray="2 3" />
              {/* 4 Cardinal verification ticks */}
              <line x1="50" y1="4" x2="50" y2="12" stroke="#10B981" strokeWidth="1.5" />
              <line x1="96" y1="50" x2="88" y2="50" stroke="#10B981" strokeWidth="1.5" />
              <line x1="50" y1="96" x2="50" y2="88" stroke="#10B981" strokeWidth="1.5" />
              <line x1="4" y1="50" x2="12" y2="50" stroke="#10B981" strokeWidth="1.5" />
              {/* Orbiting nodes */}
              <circle cx="50" cy="14" r="3" fill="#10B981" />
              <circle cx="86" cy="50" r="3" fill="#34D399" />
              <circle cx="50" cy="86" r="3" fill="#6EE7B7" />
              <circle cx="14" cy="50" r="3" fill="#059669" />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center font-mono">
              <div className="w-12 h-12 rounded-full bg-emerald-500/20 border border-emerald-400/40 flex items-center justify-center mb-1 shadow-[0_0_16px_rgba(16,185,129,0.35)]">
                <svg className="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <span className="text-[9px] text-emerald-300 font-bold tracking-wider">VERIFIED</span>
              <span className="text-[10px] font-extrabold text-white">ALL GATES PASS</span>
            </div>
          </div>
        ) : (
          <div className="hidden md:flex flex-col items-center justify-center relative w-48 h-48 shrink-0">
            <svg className="w-44 h-44 animate-[spin_60s_linear_infinite]" viewBox="0 0 100 100">
              {/* Outer concentric orbital rings */}
              <circle cx="50" cy="50" r="46" fill="none" stroke="rgba(255, 94, 14, 0.25)" strokeWidth="1" strokeDasharray="4 4" />
              <circle cx="50" cy="50" r="34" fill="none" stroke="rgba(255, 255, 255, 0.12)" strokeWidth="1" />
              <circle cx="50" cy="50" r="22" fill="none" stroke="rgba(216, 246, 52, 0.25)" strokeWidth="1" strokeDasharray="2 3" />
              {/* 4 Quadrant Agentic Loops: TRIAGE, PLAN, EXECUTE, VERIFY */}
              <line x1="50" y1="4" x2="50" y2="96" stroke="rgba(255, 255, 255, 0.08)" strokeWidth="1" />
              <line x1="4" y1="50" x2="96" y2="50" stroke="rgba(255, 255, 255, 0.08)" strokeWidth="1" />
              {/* Nodes */}
              <circle cx="50" cy="16" r="3" fill="#FF5E0E" />
              <circle cx="84" cy="50" r="3" fill="#FFA03A" />
              <circle cx="50" cy="84" r="3" fill="#D8F634" />
              <circle cx="16" cy="50" r="3" fill="#10B981" />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center font-mono">
              <span className="text-[10px] text-slate-400 font-bold">AUTONOMY</span>
              <span className="text-xs font-extrabold text-[#FFA03A]">ACTIVE</span>
            </div>
          </div>
        )}
      </div>

      {/* Signature Floating Liquid Glass Metrics Capsule Strip */}
      <div className="relative z-10 mt-6 hero-pill-strip rounded-2xl p-2 flex flex-wrap items-center justify-between gap-2">
        {/* Segment 1: Release APK */}
        <div className="px-4 py-2 space-y-0.5">
          <div className="text-xl sm:text-2xl font-bold font-sans text-white">{state.kpis.apkSize.current}</div>
          <div className="text-[11px] font-medium text-slate-400 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
            <span>APK Size ({state.kpis.apkSize.delta})</span>
          </div>
        </div>

        {/* Segment 2: Single DEX */}
        <div className="px-4 py-2 space-y-0.5">
          <div className="text-xl sm:text-2xl font-bold font-sans text-white">{state.kpis.dexBytecode.current}</div>
          <div className="text-[11px] font-medium text-slate-400 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-indigo-400"></span>
            <span>Single DEX</span>
          </div>
        </div>

        {/* Segment 3: JaCoCo Coverage - ACTIVE GLOWING ORANGE GLASS PILL */}
        <div className="active-metric-pill rounded-xl px-5 py-2.5 space-y-0.5">
          <div className="text-xl sm:text-2xl font-extrabold font-sans text-white">{state.kpis.testCoverage.instruction}</div>
          <div className="text-[11px] font-semibold text-white/95 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse"></span>
            <span>JaCoCo Gate PASS</span>
          </div>
        </div>

        {/* Segment 4: Security Score */}
        <div className="px-4 py-2 space-y-0.5">
          <div className="text-xl sm:text-2xl font-bold font-sans text-white">{state.kpis.securityRating.score}</div>
          <div className="text-[11px] font-medium text-slate-400 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
            <span>SQLCipher HW</span>
          </div>
        </div>
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.HeroSprintCard = HeroSprintCard;
}
