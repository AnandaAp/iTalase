function NavigationDock({ activeNav, setActiveNav, syncFreshState, flashNotice, lastSyncTime, handleResetToDisk }) {
  return (
    <aside className="w-16 shrink-0 rounded-[28px] bg-[#13151D]/90 border border-white/[0.08] p-3 flex flex-col items-center justify-between shadow-xl backdrop-blur-2xl">
      {/* Top Brand Monogram */}
      <div className="w-10 h-10 rounded-2xl bg-white/[0.06] border border-white/10 flex items-center justify-center text-white shadow-sm">
        <svg className="w-5 h-5 text-orange-400" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="8" cy="8" r="3" />
          <circle cx="16" cy="8" r="3" />
          <circle cx="8" cy="16" r="3" />
          <circle cx="16" cy="16" r="3" />
          <circle cx="12" cy="12" r="2.2" />
        </svg>
      </div>

      {/* Middle Nav Icons: Interactive Switchers */}
      <div className="space-y-4 flex flex-col items-center">
        {/* Tab 1: Overview */}
        <button 
          onClick={() => setActiveNav('overview')}
          className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all ${
            activeNav === 'overview' 
              ? 'bg-white/15 text-white shadow-md border border-white/20' 
              : 'text-slate-400 hover:text-white hover:bg-white/[0.06]'
          }`}
          title="Ringkasan Overview (Sprint & Metrik)"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
          </svg>
        </button>

        {/* Tab 2: Kanban Sprint Board */}
        <button 
          onClick={() => setActiveNav('kanban')}
          className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all ${
            activeNav === 'kanban' 
              ? 'bg-white/15 text-white shadow-md border border-white/20' 
              : 'text-slate-400 hover:text-white hover:bg-white/[0.06]'
          }`}
          title="Papan Kanban Otonom"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
          </svg>
        </button>

        {/* Tab 3: Telemetry & Audit Logs */}
        <button 
          onClick={() => setActiveNav('telemetry')}
          className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all ${
            activeNav === 'telemetry' 
              ? 'bg-white/15 text-white shadow-md border border-white/20' 
              : 'text-slate-400 hover:text-white hover:bg-white/[0.06]'
          }`}
          title="Audit Trail & Telemetri Agen"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </button>

        {/* Force Refresh State */}
        <button 
          onClick={syncFreshState}
          className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/[0.06] transition-all"
          title="Sinkronkan Ulang Data"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
      </div>

      {/* Bottom Dock Controls */}
      <div className="space-y-3 flex flex-col items-center">
        {/* Notification Status */}
        <div className="relative">
          <button 
            onClick={() => flashNotice(`Terakhir sinkronisasi: ${formatTimeInfo(lastSyncTime).displayTime} WIB`)}
            className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-300 hover:text-white hover:bg-white/[0.06] transition-all"
            title="Notifikasi Sistem"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </button>
          <span className="w-2 h-2 rounded-full bg-[#FF5E0E] absolute top-2 right-2 shadow-[0_0_8px_#FF5E0E]"></span>
        </div>

        {/* Disk Reset Button */}
        <button 
          onClick={handleResetToDisk}
          className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/[0.06] transition-all"
          title="Reset Cache & Reload dari audit_state.js"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </button>
      </div>
    </aside>
  );
}

if (typeof window !== 'undefined') {
  window.NavigationDock = NavigationDock;
}
