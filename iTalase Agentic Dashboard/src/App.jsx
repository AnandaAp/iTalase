function App() {
  const { useState, useEffect, useMemo, useCallback } = React;

  const [state, setState] = useState(window.AUDIT_STATE || null);
  const [isLiveSync, setIsLiveSync] = useState(true);
  const [serverOnline, setServerOnline] = useState(typeof window !== 'undefined' && window.location.protocol.startsWith('http'));
  const [lastSyncTime, setLastSyncTime] = useState(new Date());
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedTask, setSelectedTask] = useState(null);
  const [isNewTaskOpen, setIsNewTaskOpen] = useState(false);
  const [toolFilter, setToolFilter] = useState('ALL');
  const [draggedCardKey, setDraggedCardKey] = useState(null);
  const [dropTargetCol, setDropTargetCol] = useState(null);
  const [statusNotice, setStatusNotice] = useState('');
  const [activeNav, setActiveNav] = useState('overview'); // 'overview' | 'kanban' | 'telemetry'
  const [chartHover, setChartHover] = useState(null);
  const [adbInfo, setAdbInfo] = useState({ online: false, count: 0, activeDevice: null });
  const [buildTarget, setBuildTarget] = useState({
    targetSdk: state?.projectInfo?.targetSdk || 37,
    minSdk: state?.projectInfo?.minSdk || 26,
    compileSdk: state?.projectInfo?.compileSdk || 37,
    version: state?.projectInfo?.version || '1.0.0',
    versionCode: state?.projectInfo?.versionCode || 100000,
    source: 'gradle/libs.versions.toml'
  });

  const fetchBuildTarget = useCallback(async () => {
    if (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) {
      try {
        const res = await fetch(getApiUrl('/api/build-target?t=' + Date.now()), { cache: 'no-store' });
        if (res.ok) {
          const data = await res.json();
          setBuildTarget(data);
          return;
        }
      } catch (e) {}
    }
  }, []);

  useEffect(() => {
    fetchBuildTarget();
  }, [fetchBuildTarget]);

  const checkAdbDevices = useCallback(async () => {
    if (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) {
      try {
        const res = await fetch(getApiUrl('/api/devices?t=' + Date.now()), { cache: 'no-store' });
        if (res.ok) {
          const data = await res.json();
          setAdbInfo(data);
          return;
        }
      } catch (e) {}
    }
    // Fallback to state store
    const dev = state?.projectInfo?.connectedDevice;
    const isConn = dev && dev.status && !dev.status.toLowerCase().includes('deactive') && !dev.status.toLowerCase().includes('offline') && !dev.model?.toLowerCase().includes('none');
    setAdbInfo({
      online: !!isConn,
      count: isConn ? 1 : 0,
      activeDevice: isConn ? { model: dev.model, label: dev.model, badge: 'A54' } : null
    });
  }, [state]);

  useEffect(() => {
    checkAdbDevices();
    const adbInterval = setInterval(checkAdbDevices, 3000);
    return () => clearInterval(adbInterval);
  }, [checkAdbDevices]);

  useEffect(() => {
    if (selectedTask || isNewTaskOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => { document.body.style.overflow = originalOverflow; };
    }
  }, [selectedTask, isNewTaskOpen]);

  const flashNotice = useCallback((msg) => {
    setStatusNotice(msg);
    setTimeout(() => setStatusNotice(''), 3500);
  }, []);

  const applyFreshState = useCallback((freshData) => {
    if (!freshData || !freshData.kanban) return;
    window.AUDIT_STATE = freshData;

    setState({
      ...freshData,
      kanban: {
        backlog: [...(freshData.kanban.backlog || [])],
        inProgress: [...(freshData.kanban.inProgress || [])],
        verified: [...(freshData.kanban.verified || [])],
        recommendations: [...(freshData.kanban.recommendations || [])],
      },
      currentTask: { ...(freshData.currentTask || {}) },
      kpis: { ...(freshData.kpis || {}) },
      agenticPermissions: [...(freshData.agenticPermissions || [])],
      activityLogs: [...(freshData.activityLogs || [])]
    });
    setLastSyncTime(new Date());
  }, []);

  const syncFreshState = useCallback(async () => {
    if (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) {
      try {
        const res = await fetch(getApiUrl('/api/state?t=' + Date.now()), {
          method: 'GET',
          cache: 'no-store',
          headers: { 'Pragma': 'no-cache', 'Cache-Control': 'no-cache' }
        });
        if (res.ok) {
          const fresh = await res.json();
          applyFreshState(fresh);
          setServerOnline(true);
          return;
        }
      } catch (e) {}
    }

    try {
      const oldScript = document.getElementById('dynamicStatePoller');
      if (oldScript) oldScript.remove();

      const script = document.createElement('script');
      script.id = 'dynamicStatePoller';
      script.src = 'audit_state.js?t=' + Date.now();
      script.onload = () => {
        if (window.AUDIT_STATE) {
          applyFreshState(window.AUDIT_STATE);
        }
      };
      document.body.appendChild(script);
    } catch (e) {}
  }, [applyFreshState]);

  useEffect(() => {
    if (!isLiveSync || typeof window === 'undefined' || !window.location.protocol.startsWith('http')) return;
    let es = null;
    try {
      es = new EventSource(getApiUrl('/api/events'));
      es.onmessage = (event) => {
        try {
          const fresh = JSON.parse(event.data);
          applyFreshState(fresh);
          setServerOnline(true);
        } catch (e) {}
      };
    } catch (e) {}

    return () => { if (es) es.close(); };
  }, [isLiveSync, applyFreshState]);

  useEffect(() => {
    syncFreshState();
  }, [syncFreshState]);

  useEffect(() => {
    if (!isLiveSync) return;
    const interval = setInterval(syncFreshState, 2000);
    return () => clearInterval(interval);
  }, [isLiveSync, syncFreshState]);

  const persistState = useCallback(async (updatedState) => {
    setState(updatedState);
    window.AUDIT_STATE = updatedState;

    try {
      const res = await fetch(getApiUrl('/api/save-state'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updatedState)
      });
      if (res.ok) {
        flashNotice('Tersimpan di disk (audit_state.js)');
        setServerOnline(true);
        return;
      }
    } catch (e) {
      setServerOnline(false);
    }
    flashNotice('Tersimpan di memory');
  }, [flashNotice]);

  const moveCardInState = (cardKey, targetColKey) => {
    if (!state || !state.kanban) return;
    const newKanban = { ...state.kanban };
    let foundCard = null;

    for (const col of Object.keys(newKanban)) {
      const list = [...(newKanban[col] || [])];
      const idx = list.findIndex(c => c.key === cardKey || c.id === cardKey);
      if (idx !== -1) {
        foundCard = list.splice(idx, 1)[0];
        newKanban[col] = list;
        break;
      }
    }

    if (foundCard) {
      if (!newKanban[targetColKey]) newKanban[targetColKey] = [];
      newKanban[targetColKey] = [foundCard, ...newKanban[targetColKey]];

      const updatedState = {
        ...state,
        kanban: newKanban,
        projectInfo: {
          ...state.projectInfo,
          lastUpdated: new Date().toISOString().replace('T', ' ').substring(0, 19)
        }
      };

      persistState(updatedState);
    }
  };

  const handleResetToDisk = async () => {
    try {
      const keysToPurge = [];
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i);
        if (k && (k.includes('ITALASE') || k.includes('KANBAN') || k.includes('STATE'))) keysToPurge.push(k);
      }
      keysToPurge.forEach(k => localStorage.removeItem(k));
    } catch (e) {}
    await syncFreshState();
    flashNotice('Disk state reloaded');
  };

  const handleDragStart = (e, key) => {
    setDraggedCardKey(key);
    e.dataTransfer.setData('text/plain', key);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e, colKey) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (dropTargetCol !== colKey) {
      setDropTargetCol(colKey);
    }
  };

  const handleDrop = (e, targetColKey) => {
    e.preventDefault();
    setDropTargetCol(null);
    const cardKey = e.dataTransfer.getData('text/plain') || draggedCardKey;
    if (!cardKey) return;
    moveCardInState(cardKey, targetColKey);
    setDraggedCardKey(null);
  };

  const handleCreateTask = (newTask) => {
    if (!state) return;
    const targetCol = newTask.column || 'recommendations';
    const updatedState = {
      ...state,
      kanban: {
        ...state.kanban,
        [targetCol]: [newTask, ...(state.kanban[targetCol] || [])]
      }
    };
    persistState(updatedState);
    setIsNewTaskOpen(false);
  };

  const COL_ORDER = window.COL_ORDER || ['recommendations', 'backlog', 'inProgress', 'verified'];

  const filteredKanban = useMemo(() => {
    if (!state || !state.kanban) {
      return { recommendations: [], backlog: [], inProgress: [], verified: [] };
    }
    const result = {};
    COL_ORDER.forEach(col => {
      result[col] = (state.kanban[col] || []).filter(card => {
        const matchesSearch = !searchQuery || 
          card.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
          card.snippet.toLowerCase().includes(searchQuery.toLowerCase()) ||
          card.id.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesCategory = selectedCategory === 'ALL' || 
          card.category.toUpperCase() === selectedCategory.toUpperCase();
        return matchesSearch && matchesCategory;
      });
    });
    return result;
  }, [state, searchQuery, selectedCategory, COL_ORDER]);

  const filteredPermissions = useMemo(() => {
    if (!state || !state.agenticPermissions) return [];
    return state.agenticPermissions.filter(p => {
      if (toolFilter === 'ALL') return true;
      return p.tool.toLowerCase().includes(toolFilter.toLowerCase());
    });
  }, [state, toolFilter]);

  const isTaskComplete = state?.currentTask?.status === 'Completed' || (state?.currentTask?.progressPercent ?? state?.currentTask?.progress ?? 100) >= 100;
  const recCount = (state?.kanban?.recommendations || []).length;

  // React side effect to manage body styling and dynamic document title
  useEffect(() => {
    document.body.className = "selection:bg-[#FF5E0E] selection:text-white p-3 sm:p-5 lg:p-7";
    document.title = "iTalase Agentic Console — Autonomous Sprint & Telemetry";
  }, []);

  if (!state) {
    return (
      <>
        <DashboardStyleProvider />
        <div className="flex items-center justify-center min-h-screen bg-[#111317]">
          <div className="text-center space-y-3">
            <div className="w-9 h-9 border-2 border-[#FF5E0E] border-t-transparent rounded-full animate-spin mx-auto"></div>
            <p className="text-xs font-mono text-orange-400 tracking-wider">Loading iTalase Console...</p>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <DashboardStyleProvider />
      <div className="max-w-[1580px] mx-auto min-h-[95vh] rounded-[2.5rem] bg-[#12151E]/75 border border-white/[0.09] border-t-white/[0.25] shadow-[0_35px_90px_-20px_rgba(0,0,0,0.85)] p-4 sm:p-6 lg:p-7 flex gap-6 relative overflow-hidden backdrop-blur-3xl">
        {/* Ambient Diffused Sunset Glow Orbs */}
        <div className="absolute top-1/4 left-1/3 w-[720px] h-[520px] bg-gradient-to-br from-[#FF6B2C]/28 via-[#FFA03A]/16 to-transparent rounded-full blur-[140px] pointer-events-none"></div>
        <div className="absolute top-1/3 right-10 w-[550px] h-[420px] bg-[#FFA03A]/20 rounded-full blur-[110px] pointer-events-none"></div>

        {/* 1. Left Functional Vertical Dock Navigation */}
        <NavigationDock 
          activeNav={activeNav}
          setActiveNav={setActiveNav}
          syncFreshState={syncFreshState}
          flashNotice={flashNotice}
          lastSyncTime={lastSyncTime}
          handleResetToDisk={handleResetToDisk}
        />

        {/* Main Content Workspace */}
        <div className="flex-1 space-y-6 flex flex-col justify-between min-w-0">
          {/* 2. Top Header */}
          <DashboardHeader
            buildTarget={buildTarget}
            state={state}
            serverOnline={serverOnline}
            isLiveSync={isLiveSync}
            setIsLiveSync={setIsLiveSync}
            adbInfo={adbInfo}
          />

          {/* TAB VIEW 1: OVERVIEW (Bento + Kanban + Activity) */}
          {activeNav === 'overview' && (
            <div className="space-y-6 view-enter">
              {/* Top Bento Grid: Hero Feature Card + Right Quality Charts */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                {/* 3. Left Large Hero Sprint Feature Card */}
                <HeroSprintCard
                  state={state}
                  isTaskComplete={isTaskComplete}
                  recCount={recCount}
                  buildTarget={buildTarget}
                  setIsNewTaskOpen={setIsNewTaskOpen}
                  setActiveNav={setActiveNav}
                />

                {/* Right Side Bento Cards */}
                <div className="lg:col-span-4 space-y-5 flex flex-col justify-between">
                  {/* 4. Test Velocity & Quality Gate Card */}
                  <QualityVelocityCard
                    state={state}
                    chartHover={chartHover}
                    setChartHover={setChartHover}
                  />

                  {/* 5. Release Target & Hardware Card */}
                  <ReleaseTargetCard
                    adbInfo={adbInfo}
                    buildTarget={buildTarget}
                    state={state}
                  />
                </div>
              </div>

              {/* 6. Kanban Board Grid */}
              <KanbanBoard
                isFullView={false}
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                selectedCategory={selectedCategory}
                setSelectedCategory={setSelectedCategory}
                filteredKanban={filteredKanban}
                dropTargetCol={dropTargetCol}
                handleDragOver={handleDragOver}
                handleDrop={handleDrop}
                handleDragStart={handleDragStart}
                setSelectedTask={setSelectedTask}
                moveCardInState={moveCardInState}
                setIsNewTaskOpen={setIsNewTaskOpen}
              />

              {/* Bottom Activity Section: Authentic Operations & Audit Trail */}
              <div className="glass-surface rounded-[32px] p-6 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/[0.08] pb-3">
                  <div>
                    <h3 className="text-base font-bold text-white">Recent Autonomous Operations &amp; Audit Trail</h3>
                    <p className="text-xs text-slate-400">Rekaman eksekusi tools agen (run_command, write_file, edit_file) &amp; audit stream real-time</p>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 font-mono">Filter:</span>
                    <select
                      value={toolFilter}
                      onChange={(e) => setToolFilter(e.target.value)}
                      className="bg-[#171B26] border border-white/10 rounded-full px-3 py-1 text-xs text-slate-200 focus:outline-none"
                    >
                      <option value="ALL">All Tools</option>
                      <option value="run_command">run_command</option>
                      <option value="write_file">write_file</option>
                      <option value="edit_file">edit_file</option>
                    </select>
                  </div>
                </div>

                {/* Horizontal Frosted Rows */}
                <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
                  {filteredPermissions.length === 0 ? (
                    <div className="text-center py-6 text-xs font-mono text-slate-500">Tidak ada log operasi untuk filter ini.</div>
                  ) : (
                    filteredPermissions.slice(0, 10).map((perm, idx) => (
                      <div 
                        key={perm.id + idx}
                        className="p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/[0.06] flex items-center justify-between gap-4 transition"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className={`w-10 h-10 rounded-xl border flex items-center justify-center text-xs font-mono font-bold shrink-0 ${
                            perm.tool === 'run_command' 
                              ? 'bg-amber-500/10 border-amber-500/30 text-amber-400' 
                              : (perm.tool === 'write_file' 
                                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' 
                                  : 'bg-sky-500/10 border-sky-500/30 text-sky-400')
                          }`}>
                            {perm.tool === 'run_command' ? 'CMD' : (perm.tool === 'write_file' ? 'NEW' : 'EDT')}
                          </div>
                          <div className="min-w-0">
                            <div className="text-xs font-bold text-white truncate">{perm.action}</div>
                            <div className="text-[11px] font-mono text-slate-400 truncate max-w-md">{perm.target}</div>
                          </div>
                        </div>

                        <div className="flex items-center gap-6 shrink-0">
                          <div className="text-right">
                            <TimeHoverCell timestamp={perm.timestamp} label="Waktu Eksekusi" />
                          </div>
                          <span className="px-3 py-0.5 rounded-full text-[11px] font-bold font-mono bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                            {perm.status}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

            </div>
          )}

          {/* TAB VIEW 2: FULL KANBAN SPRINT BOARD */}
          {activeNav === 'kanban' && (
            <div className="space-y-5 view-enter">
              <KanbanBoard
                isFullView={true}
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                selectedCategory={selectedCategory}
                setSelectedCategory={setSelectedCategory}
                filteredKanban={filteredKanban}
                dropTargetCol={dropTargetCol}
                handleDragOver={handleDragOver}
                handleDrop={handleDrop}
                handleDragStart={handleDragStart}
                setSelectedTask={setSelectedTask}
                moveCardInState={moveCardInState}
                setIsNewTaskOpen={setIsNewTaskOpen}
              />
            </div>
          )}

          {/* TAB VIEW 3: TELEMETRY & AUDIT STREAM */}
          {activeNav === 'telemetry' && (
            <TelemetryView
              state={state}
              toolFilter={toolFilter}
              setToolFilter={setToolFilter}
              filteredPermissions={filteredPermissions}
            />
          )}

        </div>

        {/* Task Detail Modal */}
        {selectedTask && typeof document !== 'undefined' && ReactDOM.createPortal(
          <div 
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xl animate-fadeIn"
            onClick={() => setSelectedTask(null)}
          >
            <div 
              className="glass-surface p-6 rounded-3xl max-w-lg w-full space-y-4 text-white shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-sm text-[#FFA03A]">{selectedTask.id}</span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono border ${
                    (window.CATEGORY_STYLES && window.CATEGORY_STYLES[selectedTask.category]) || 'bg-white/10 text-slate-300 border-white/20'
                  }`}>
                    {selectedTask.categoryLabel || selectedTask.category}
                  </span>
                </div>
                <button onClick={() => setSelectedTask(null)} className="w-7 h-7 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 flex items-center justify-center text-xs">✕</button>
              </div>

              <div className="space-y-2">
                <h4 className="text-base font-bold text-white">{selectedTask.title}</h4>
                <p className="text-xs text-slate-300 leading-relaxed">{selectedTask.snippet}</p>
              </div>

              <div className="pt-3 border-t border-white/10 space-y-2">
                <span className="text-xs text-slate-400 font-mono">Pindahkan ke kolom:</span>
                <div className="grid grid-cols-4 gap-2">
                  {COL_ORDER.map(col => {
                    const colNames = { recommendations: 'Rec', backlog: 'Backlog', inProgress: 'In Prog', verified: 'Done' };
                    return (
                      <button
                        key={col}
                        onClick={() => {
                          moveCardInState(selectedTask.key, col);
                          setSelectedTask(null);
                        }}
                        className="px-2 py-1.5 rounded-xl bg-white/[0.06] hover:bg-white hover:text-black text-slate-200 text-[11px] font-mono transition text-center border border-white/10"
                      >
                        {colNames[col] || col}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>,
          document.body
        )}

        {/* New Task Modal */}
        {isNewTaskOpen && (
          <NewTaskModal 
            onClose={() => setIsNewTaskOpen(false)}
            onSubmit={handleCreateTask}
          />
        )}

        {/* Status Toast Notice */}
        {statusNotice && (
          <div className="fixed bottom-6 right-6 z-50 bg-[#1A1D26]/95 border border-[#FF5E0E]/50 text-white px-4 py-2.5 rounded-2xl shadow-2xl backdrop-blur-2xl text-xs font-mono flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#FF5E0E] shadow-[0_0_8px_#FF5E0E]"></span>
            <span>{statusNotice}</span>
          </div>
        )}
      </div>
    </>
  );
}

if (typeof window !== 'undefined') {
  window.App = App;
}
