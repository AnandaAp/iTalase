import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import ReactDOM from 'react-dom';

const CATEGORY_COLORS = {
  arch: 'bg-blue-500/15 text-blue-300 border-blue-500/30',
  security: 'bg-rose-500/15 text-rose-300 border-rose-500/30',
  size: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  perf: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  test: 'bg-purple-500/15 text-purple-300 border-purple-500/30',
};

const COL_ORDER = ['backlog', 'inProgress', 'verified', 'recommendations'];

// Dynamic endpoint helper: uses relative paths for VS Code DevTunnels/port-forwarding and localhost
const getApiUrl = (endpoint) => {
  if (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) {
    return endpoint;
  }
  return 'http://localhost:3300' + endpoint;
};

// Helper to format timestamps accurately in WIB & Relative time with hover breakdown
export function formatTimeInfo(timestampStr) {
  if (!timestampStr) return { displayTime: '-', localFull: '-', utc: '-', relative: '-' };
  
  let dateObj = null;
  const str = String(timestampStr).trim();

  if (str.includes('T')) {
    dateObj = new Date(str);
  } else if (str.includes('-')) {
    dateObj = new Date(str.replace(' ', 'T') + 'Z');
  } else if (str.includes(':')) {
    const today = new Date().toISOString().split('T')[0];
    dateObj = new Date(`${today}T${str}Z`);
  }

  if (!dateObj || isNaN(dateObj.getTime())) {
    return { displayTime: str, localFull: str, utc: str, relative: 'Terekam' };
  }

  const wibTime = new Intl.DateTimeFormat('id-ID', {
    timeZone: 'Asia/Jakarta',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(dateObj);

  const wibFull = new Intl.DateTimeFormat('id-ID', {
    timeZone: 'Asia/Jakarta',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(dateObj) + ' WIB';

  const utcStr = dateObj.toISOString().replace('T', ' ').substring(0, 19) + ' UTC';

  const diffSec = Math.max(0, Math.floor((Date.now() - dateObj.getTime()) / 1000));
  let relative = '';
  if (diffSec < 45) {
    relative = 'Baru saja';
  } else if (diffSec < 3600) {
    relative = `${Math.floor(diffSec / 60)} mnt lalu`;
  } else if (diffSec < 86400) {
    relative = `${Math.floor(diffSec / 3600)} jam lalu`;
  } else {
    relative = `${Math.floor(diffSec / 86400)} hari lalu`;
  }

  return {
    displayTime: wibTime,
    localFull: wibFull,
    utc: utcStr,
    relative: relative
  };
}

// Interactive Portal-Based Tooltip for Timestamps (Immune to table overflow-x clipping and window edge cutoff)
export function TimeHoverCell({ timestamp, label = "Timestamp Eksekusi Tool" }) {
  const [isOpen, setIsOpen] = useState(false);
  const [coords, setCoords] = useState({ x: 0, y: 0, placement: 'bottom', triggerCenterX: 0 });
  const triggerRef = useRef(null);
  const info = useMemo(() => formatTimeInfo(timestamp), [timestamp]);

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const tooltipWidth = 320;
    const tooltipHeight = 175;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let x = rect.left;
    if (x + tooltipWidth > viewportWidth - 16) {
      x = viewportWidth - tooltipWidth - 16;
    }
    if (x < 16) {
      x = 16;
    }

    let y = rect.bottom + 8;
    let placement = 'bottom';
    if (rect.bottom + tooltipHeight + 16 > viewportHeight && rect.top - tooltipHeight - 16 > 0) {
      y = rect.top - tooltipHeight - 8;
      placement = 'top';
    }

    setCoords({
      x,
      y,
      placement,
      triggerCenterX: rect.left + rect.width / 2
    });
  }, []);

  const handleMouseEnter = () => {
    updatePosition();
    setIsOpen(true);
  };

  const handleMouseLeave = () => {
    setIsOpen(false);
  };

  const arrowLeft = Math.max(16, Math.min(296, (coords.triggerCenterX || coords.x + 36) - coords.x));

  return (
    <div 
      ref={triggerRef}
      className="relative inline-block"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <div className="flex items-center gap-1.5 px-2 py-1 rounded-lg bg-white/[0.04] hover:bg-emerald-500/10 border border-white/10 hover:border-emerald-500/40 transition cursor-help group">
        <svg className="w-3 h-3 text-emerald-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span className="text-[11px] font-mono text-slate-300 group-hover:text-emerald-300 font-medium">
          {info.displayTime}
        </span>
        <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-white/10 text-slate-400 group-hover:text-emerald-200">
          {info.relative}
        </span>
      </div>

      {isOpen && typeof document !== 'undefined' && ReactDOM.createPortal(
        <div 
          style={{
            position: 'fixed',
            top: `${coords.y}px`,
            left: `${coords.x}px`,
            width: '320px',
            zIndex: 999999,
            pointerEvents: 'none'
          }}
          className="p-4 rounded-2xl glass-card border border-emerald-500/50 shadow-2xl shadow-black/95 text-left animate-fadeIn"
        >
          <div className="flex items-center justify-between border-b border-white/10 pb-2 mb-2.5">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="text-xs font-bold text-white font-sans">{label}</span>
            </div>
            <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/15 px-1.5 py-0.5 rounded border border-emerald-500/30 font-semibold">
              WIB (UTC+7)
            </span>
          </div>

          <div className="space-y-2 text-[11px] font-mono">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Waktu Lokal (WIB):</span>
              <span className="font-bold text-emerald-300">{info.localFull}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Waktu Server (UTC):</span>
              <span className="text-slate-200">{info.utc}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Status Waktu:</span>
              <span className="text-amber-300 font-semibold">{info.relative}</span>
            </div>
          </div>

          <div className="mt-3 pt-2.5 border-t border-white/10 text-[10px] text-slate-400 leading-relaxed font-sans">
            💡 <strong className="text-emerald-400">Bukan Limit Window:</strong> Jam di atas adalah waktu saat AI Agent mengeksekusi aksi/perintah (diurutkan kronologis dari terbaru ke terlama). Nilai waktu menyusut ke bawah karena tabel diurutkan dari aktivitas paling akhir.
          </div>

          {coords.placement === 'bottom' ? (
            <div 
              style={{ left: `${arrowLeft}px` }}
              className="absolute -top-1.5 w-3 h-3 bg-[#161B18] border-t border-l border-emerald-500/50 rotate-45"
            ></div>
          ) : (
            <div 
              style={{ left: `${arrowLeft}px` }}
              className="absolute -bottom-1.5 w-3 h-3 bg-[#161B18] border-b border-r border-emerald-500/50 rotate-45"
            ></div>
          )}
        </div>,
        document.body
      )}
    </div>
  );
}

export default function App() {
  try {
    const keysToPurge = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k && (k.includes('ITALASE') || k.includes('KANBAN') || k.includes('STATE'))) keysToPurge.push(k);
    }
    keysToPurge.forEach(k => localStorage.removeItem(k));
  } catch(e) {}

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
  const [recentlyMoved, setRecentlyMoved] = useState({});

  const cardPositionsRef = useRef({});

  const flashNotice = useCallback((msg) => {
    setStatusNotice(msg);
    setTimeout(() => setStatusNotice(''), 4500);
  }, []);

  const applyFreshState = useCallback((freshData) => {
    if (!freshData || !freshData.kanban) return;
    window.AUDIT_STATE = freshData;

    // Detect Card Movement Across Columns
    const newPositions = {};
    const newlyMoved = {};
    COL_ORDER.forEach(col => {
      (freshData.kanban[col] || []).forEach(card => {
        const key = card.key || card.id;
        newPositions[key] = col;
        const oldCol = cardPositionsRef.current[key];
        if (oldCol && oldCol !== col) {
          newlyMoved[key] = {
            card,
            fromCol: oldCol,
            toCol: col,
            timestamp: Date.now()
          };
        }
      });
    });
    cardPositionsRef.current = newPositions;

    if (Object.keys(newlyMoved).length > 0) {
      setRecentlyMoved(prev => ({ ...prev, ...newlyMoved }));
      const first = Object.values(newlyMoved)[0];
      const colTitles = {
        backlog: 'Backlog',
        inProgress: 'In Progress',
        verified: 'Verified & Tested',
        recommendations: 'Recommendations'
      };
      flashNotice(`🚀 "${first.card.title}" otomatis berpindah ke ${colTitles[first.toCol] || first.toCol}!`);

      setTimeout(() => {
        setRecentlyMoved(prev => {
          const next = { ...prev };
          Object.keys(newlyMoved).forEach(k => delete next[k]);
          return next;
        });
      }, 4500);
    }

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
  }, [flashNotice]);

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
    } catch (e) {
      console.error('[iTalase Sync Error]', e);
    }
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
      es.onerror = () => {};
    } catch (e) {}

    return () => {
      if (es) es.close();
    };
  }, [isLiveSync, applyFreshState]);

  useEffect(() => {
    syncFreshState();
  }, [syncFreshState]);

  useEffect(() => {
    if (!isLiveSync) return;
    const interval = setInterval(() => {
      syncFreshState();
    }, 1500);

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
        flashNotice('✓ Saved directly to disk (audit_state.js)!');
        setServerOnline(true);
        return;
      }
    } catch (e) {
      setServerOnline(false);
    }

    flashNotice('✓ State updated in memory');
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
    flashNotice('↺ Synchronized with disk state (audit_state.js)!');
  };

  const handleDragStart = (e, key) => {
    setDraggedCardKey(key);
    e.dataTransfer.setData('text/plain', key);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e, colKey) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (dropTargetCol !== colKey) setDropTargetCol(colKey);
  };

  const handleDrop = (e, targetColKey) => {
    e.preventDefault();
    setDropTargetCol(null);
    const cardKey = e.dataTransfer.getData('text/plain') || draggedCardKey;
    if (!cardKey || !state || !state.kanban) return;

    moveCardInState(cardKey, targetColKey);
    setDraggedCardKey(null);
  };

  const handleCreateTask = (newTask) => {
    if (!state || !state.kanban) return;
    const targetCol = newTask.column || 'backlog';
    const newKanban = {
      ...state.kanban,
      [targetCol]: [newTask, ...(state.kanban[targetCol] || [])]
    };
    const updatedState = { ...state, kanban: newKanban };
    persistState(updatedState);
    setIsNewTaskOpen(false);
  };

  const filteredKanban = useMemo(() => {
    if (!state || !state.kanban) return { backlog: [], inProgress: [], verified: [], recommendations: [] };

    const filterFn = (card) => {
      const matchesSearch = searchQuery.trim() === '' || 
        card.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        card.snippet.toLowerCase().includes(searchQuery.toLowerCase()) ||
        card.id.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesCat = selectedCategory === 'ALL' || card.category === selectedCategory.toLowerCase();
      return matchesSearch && matchesCat;
    };

    return {
      backlog: (state.kanban.backlog || []).filter(filterFn),
      inProgress: (state.kanban.inProgress || []).filter(filterFn),
      verified: (state.kanban.verified || []).filter(filterFn),
      recommendations: (state.kanban.recommendations || []).filter(filterFn),
    };
  }, [state, searchQuery, selectedCategory]);

  const filteredPermissions = useMemo(() => {
    if (!state || !state.agenticPermissions) return [];
    return state.agenticPermissions.filter(p => {
      if (toolFilter === 'ALL') return true;
      return p.tool.toLowerCase().includes(toolFilter.toLowerCase());
    });
  }, [state, toolFilter]);

  if (!state) {
    return (
      <div className="flex items-center justify-center min-h-screen text-white font-mono">
        Loading iTalase React Agentic Dashboard...
      </div>
    );
  }

  return (
    <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <header className="glass-card p-6 rounded-3xl flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 text-white font-black text-2xl">
            iT
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl font-bold tracking-tight text-white">iTalase Agentic Dashboard</h1>
              <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono text-xs border border-emerald-500/30">React 19</span>
              <span className="px-2.5 py-0.5 rounded-full bg-blue-500/20 text-blue-300 font-mono text-xs border border-blue-500/30">Interactive</span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Kotlin Multiplatform POS & Accounting • Two-Way Persistence • Apple Fluid Motion
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div 
            className={`flex items-center gap-2 px-3 py-2 rounded-2xl border text-xs font-mono transition ${
              serverOnline 
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' 
                : 'bg-white/5 border-white/10 text-slate-400'
            }`}
          >
            <span className={`w-2 h-2 rounded-full ${serverOnline ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`}></span>
            <span>{serverOnline ? 'Disk Server (Auto-save)' : 'Browser Storage'}</span>
          </div>

          <button
            onClick={() => setIsLiveSync(!isLiveSync)}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-2xl border text-xs font-mono transition ${
              isLiveSync 
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/20' 
                : 'bg-amber-500/10 border-amber-500/30 text-amber-300 hover:bg-amber-500/20'
            }`}
          >
            <span className={`w-2 h-2 rounded-full ${isLiveSync ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`}></span>
            {isLiveSync ? `Live (${lastSyncTime.toLocaleTimeString()})` : 'Sync Paused'}
          </button>

          <button
            onClick={() => setIsNewTaskOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition shadow-lg shadow-emerald-600/30"
          >
            Add Task
          </button>

          <button
            onClick={handleResetToDisk}
            className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 text-slate-300 text-xs font-mono transition"
          >
            ↺ Reset
          </button>
        </div>
      </header>

      {statusNotice && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-600 text-white px-5 py-3 rounded-2xl shadow-xl shadow-emerald-600/40 text-xs font-bold font-mono animate-bounce flex items-center gap-2">
          <span>{statusNotice}</span>
        </div>
      )}

      {/* Active Task Banner */}
      <div className="glass-card p-5 rounded-3xl border-l-4 border-l-emerald-500 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
              Current Autonomous Sprint
            </span>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 font-mono">
              <span>Started:</span>
              <TimeHoverCell timestamp={state.currentTask.startedAt} label="Waktu Mulai Sprint" align="left" />
            </div>
          </div>
          <h2 className="text-base font-bold text-white">{state.currentTask.name}</h2>
          <p className="text-xs text-slate-400 max-w-2xl">{state.currentTask.description}</p>
        </div>

        <div className="flex items-center gap-4 shrink-0">
          <div className="w-36 text-right">
            <div className="text-xs font-mono font-bold text-emerald-400">{state.currentTask.progressPercent}%</div>
            <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden mt-1">
              <div 
                className="bg-gradient-to-r from-emerald-500 to-teal-400 h-full rounded-full transition-all duration-700"
                style={{ width: `${state.currentTask.progressPercent}%` }}
              ></div>
            </div>
          </div>
          <span className="px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-medium border border-emerald-500/30">
            {state.currentTask.status}
          </span>
        </div>
      </div>

      {/* KPI Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card p-5 rounded-3xl space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Release APK Size</span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 font-mono text-[11px] font-semibold">
              {state.kpis.apkSize.delta}
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">{state.kpis.apkSize.current}</div>
          <p className="text-[11px] text-slate-400">Previous: {state.kpis.apkSize.previous}</p>
        </div>

        <div className="glass-card p-5 rounded-3xl space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Single DEX Bytecode</span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 font-mono text-[11px] font-semibold">
              {state.kpis.dexBytecode.delta}
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">{state.kpis.dexBytecode.current}</div>
          <p className="text-[11px] text-slate-400">No multidex overhead</p>
        </div>

        <div className="glass-card p-5 rounded-3xl space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>JaCoCo Instruction Coverage</span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 font-mono text-[11px] font-semibold">
              ≥ 75% Gate
            </span>
          </div>
          <div className="text-3xl font-extrabold text-emerald-400 tracking-tight">{state.kpis.testCoverage.instruction}</div>
          <p className="text-[11px] text-slate-400">{state.kpis.testCoverage.passedTests}</p>
        </div>

        <div className="glass-card p-5 rounded-3xl space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Database Security</span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 font-mono text-[11px] font-semibold">
              Verified
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">{state.kpis.securityRating.score}</div>
          <p className="text-[11px] text-slate-400">{state.kpis.securityRating.encryption}</p>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 glass-card p-4 rounded-3xl">
        <div className="relative flex-1 max-w-md">
          <input
            type="text"
            placeholder="Search tasks, IDs, or keywords..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-white/5 border border-white/10 rounded-2xl px-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500/50"
          />
          {searchQuery && (
            <button 
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-2.5 text-xs text-slate-400 hover:text-white"
            >
              ✕
            </button>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          {['ALL', 'ARCH', 'SECURITY', 'PERF', 'SIZE'].map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-xl text-xs font-mono transition ${
                selectedCategory === cat
                  ? 'bg-emerald-500 text-white font-bold shadow-md shadow-emerald-500/30'
                  : 'bg-white/5 hover:bg-white/10 text-slate-400'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Kanban Board */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            Agentic Workflow Kanban
            <span className="text-xs font-normal text-slate-400">
              (Drag & Drop cards or use arrows ← → to move across columns)
            </span>
          </h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          {COL_ORDER.map((colKey) => {
            const titles = {
              backlog: 'Backlog',
              inProgress: 'In Progress',
              verified: 'Verified & Tested',
              recommendations: 'Recommendations'
            };
            const colors = {
              backlog: 'border-slate-500/30',
              inProgress: 'border-amber-500/40',
              verified: 'border-emerald-500/40',
              recommendations: 'border-teal-500/40'
            };
            return (
              <KanbanColumn
                key={colKey}
                title={titles[colKey]}
                count={filteredKanban[colKey].length}
                cards={filteredKanban[colKey]}
                columnKey={colKey}
                recentlyMoved={recentlyMoved}
                dropActive={dropTargetCol === colKey}
                onDragOver={(e) => handleDragOver(e, colKey)}
                onDrop={(e) => handleDrop(e, colKey)}
                onDragStart={handleDragStart}
                onCardClick={setSelectedTask}
                onQuickMove={moveCardInState}
                color={colors[colKey]}
              />
            );
          })}
        </div>
      </div>

      {/* Autonomous Permissions Table */}
      <div className="glass-card p-6 rounded-3xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              Autonomous Agentic Permissions & Tool Log
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 font-mono">
                {state.agenticPermissions.length} Logged
              </span>
            </h3>
            <p className="text-xs text-slate-400">All tools executed under Turbo Mode are verified and tracked</p>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400">Filter Tool:</span>
            <select
              value={toolFilter}
              onChange={(e) => setToolFilter(e.target.value)}
              className="bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-emerald-500"
            >
              <option value="ALL">All Tools</option>
              <option value="run_command">run_command</option>
              <option value="write_file">write_file</option>
              <option value="edit_file">edit_file</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 text-[11px]">
                <th className="py-2.5 px-3 font-semibold">ID</th>
                <th className="py-2.5 px-3 font-semibold">
                  <div className="flex items-center gap-1.5" title="Jam saat perintah dieksekusi oleh AI Agent (hover baris untuk rincian WIB & UTC)">
                    <span>Time (WIB)</span>
                    <span className="text-[10px] text-slate-500 cursor-help">ℹ️</span>
                  </div>
                </th>
                <th className="py-2.5 px-3 font-semibold">Tool</th>
                <th className="py-2.5 px-3 font-semibold">Action & Context</th>
                <th className="py-2.5 px-3 font-semibold">Target Path</th>
                <th className="py-2.5 px-3 font-semibold text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filteredPermissions.slice(0, 10).map((perm, idx) => (
                <tr key={perm.id + idx} className="hover:bg-white/[0.02] transition">
                  <td className="py-2 px-3 text-emerald-400 font-bold">{perm.id}</td>
                  <td className="py-2 px-3">
                    <TimeHoverCell timestamp={perm.timestamp} label="Log Eksekusi Otonom" align="left" />
                  </td>
                  <td className="py-2 px-3">
                    <span className="px-2 py-0.5 rounded bg-white/10 text-slate-300 text-[10px]">
                      {perm.tool}
                    </span>
                  </td>
                  <td className="py-2 px-3 text-white font-sans font-medium">{perm.action}</td>
                  <td className="py-2 px-3 text-slate-400 truncate max-w-xs" title={perm.target}>{perm.target}</td>
                  <td className="py-2 px-3 text-right">
                    <span className="text-emerald-400 font-semibold">{perm.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Activity Stream Feed */}
      <div className="glass-card p-6 rounded-3xl space-y-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          Chronological Engineering Stream
          <span className="text-xs text-slate-400 font-normal">({state.activityLogs.length} Events)</span>
        </h3>

        <div className="space-y-2.5">
          {state.activityLogs.map((log, index) => (
            <div key={index} className="flex items-start gap-3 p-3.5 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-emerald-500/30 transition">
              <div className="shrink-0">
                <TimeHoverCell timestamp={log.timestamp} label="Rekam Jejak Engineering" align="left" />
              </div>
              <div className="space-y-0.5 flex-1">
                <div className="text-xs font-bold text-white">{log.title}</div>
                <div className="text-[11px] text-slate-400">{log.detail}</div>
              </div>
              <span className="text-[10px] font-mono text-emerald-400 shrink-0 font-semibold px-2 py-0.5 rounded bg-emerald-500/10">
                {log.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Task Modal */}
      {selectedTask && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
          <div className="glass-card p-6 rounded-3xl max-w-lg w-full space-y-4 border border-emerald-500/30">
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300">
                {selectedTask.id} • {selectedTask.categoryLabel || selectedTask.category}
              </span>
              <button 
                onClick={() => setSelectedTask(null)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <h3 className="text-lg font-bold text-white">{selectedTask.title}</h3>
            <p className="text-xs text-slate-300 leading-relaxed">{selectedTask.snippet}</p>

            <div className="p-3 rounded-2xl bg-white/5 space-y-1 text-xs font-mono text-slate-400">
              <div>Tag: <span className="text-white">{selectedTask.tag || '-'}</span></div>
              <div>Risk: <span className="text-white">{selectedTask.risk || 'Low Risk'}</span></div>
              <div>Key: <span className="text-white">{selectedTask.key}</span></div>
            </div>

            <div className="pt-2 border-t border-white/10 space-y-2">
              <div className="text-xs text-slate-400 font-semibold">Move Card To Column:</div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {COL_ORDER.map(col => (
                  <button
                    key={col}
                    onClick={() => {
                      moveCardInState(selectedTask.key, col);
                      setSelectedTask(null);
                    }}
                    className="px-2 py-1.5 rounded-xl bg-white/10 hover:bg-emerald-500 hover:text-white text-[11px] font-mono font-bold transition text-center"
                  >
                    {col === 'inProgress' ? 'In Progress' : col.charAt(0).toUpperCase() + col.slice(1)}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {isNewTaskOpen && (
        <NewTaskModal 
          onClose={() => setIsNewTaskOpen(false)}
          onSubmit={handleCreateTask}
        />
      )}
    </div>
  );
}

function KanbanColumn({ title, count, cards, columnKey, recentlyMoved, dropActive, onDragOver, onDrop, onDragStart, onCardClick, onQuickMove, color }) {
  const colIdx = COL_ORDER.indexOf(columnKey);
  const prevCol = colIdx > 0 ? COL_ORDER[colIdx - 1] : null;
  const nextCol = colIdx < COL_ORDER.length - 1 ? COL_ORDER[colIdx + 1] : null;

  return (
    <div 
      onDragOver={onDragOver}
      onDrop={onDrop}
      className={`glass-card p-4 rounded-3xl space-y-3.5 border ${color} kanban-col transition-colors ${
        dropActive ? 'drop-active' : ''
      }`}
    >
      <div className="flex items-center justify-between px-1">
        <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">{title}</h4>
        <span className="text-xs font-mono px-2 py-0.5 rounded-full bg-white/10 text-white font-semibold">
          {count}
        </span>
      </div>

      <div className="space-y-3 min-h-[360px]">
        {cards.length === 0 ? (
          <div className="border border-dashed border-white/10 rounded-2xl p-6 text-center text-xs text-slate-500 font-mono">
            No cards here. Drag a card here.
          </div>
        ) : (
          cards.map((card) => {
            const cardKey = card.key || card.id;
            const isMoved = Boolean(recentlyMoved && recentlyMoved[cardKey]);

            return (
              <div
                key={cardKey}
                draggable
                onDragStart={(e) => onDragStart(e, cardKey)}
                onClick={() => onCardClick(card)}
                className={`kanban-card p-4 rounded-2xl bg-white/[0.03] hover:bg-white/[0.07] border border-white/10 hover:border-emerald-500/40 space-y-2.5 relative transition-all ${
                  isMoved ? 'card-spring-in' : ''
                }`}
              >
                {isMoved && (
                  <div className="flex items-center justify-between pb-1.5 border-b border-emerald-500/30">
                    <span className="flex items-center gap-1.5 text-[10px] font-mono font-bold text-emerald-300">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                      ✨ Otomatis Berpindah
                    </span>
                    <span className="text-[9px] font-mono text-emerald-400/80">
                      dari {recentlyMoved[cardKey].fromCol}
                    </span>
                  </div>
                )}

                <div className="flex items-center justify-between text-[10px] font-mono">
                  <span className="text-emerald-400 font-bold">{card.id}</span>
                  <span className={`px-2 py-0.5 rounded-full border text-[9px] ${
                    CATEGORY_COLORS[card.category] || 'bg-white/5 text-slate-300 border-white/10'
                  }`}>
                    {card.categoryLabel || card.category}
                  </span>
                </div>

                <h5 className="text-xs font-bold text-white leading-snug">{card.title}</h5>
                <p className="text-[11px] text-slate-400 line-clamp-2">{card.snippet}</p>

                <div className="flex items-center justify-between pt-1 border-t border-white/5">
                  <span className="text-[10px] text-slate-500 font-mono">{card.risk || card.tag}</span>
                  
                  <div className="flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
                    {prevCol && (
                      <button
                        onClick={() => onQuickMove(cardKey, prevCol)}
                        className="px-2 py-0.5 rounded-lg bg-white/5 hover:bg-emerald-500 hover:text-white text-slate-300 text-xs font-bold transition"
                        title={`Move back to ${prevCol}`}
                      >
                        ←
                      </button>
                    )}
                    {nextCol && (
                      <button
                        onClick={() => onQuickMove(cardKey, nextCol)}
                        className="px-2 py-0.5 rounded-lg bg-white/5 hover:bg-emerald-500 hover:text-white text-slate-300 text-xs font-bold transition"
                        title={`Advance to ${nextCol}`}
                      >
                        →
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function NewTaskModal({ onClose, onSubmit }) {
  const [title, setTitle] = useState('');
  const [snippet, setSnippet] = useState('');
  const [category, setCategory] = useState('arch');
  const [column, setColumn] = useState('backlog');
  const [tag, setTag] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title.trim()) return;

    const newId = `#AUD-${Math.floor(10 + Math.random() * 90)}`;
    const newKey = `CARD-${Math.floor(100 + Math.random() * 900)}`;

    onSubmit({
      id: newId,
      key: newKey,
      title: title.trim(),
      snippet: snippet.trim() || 'No description provided.',
      category: category,
      categoryLabel: category.toUpperCase(),
      column: column,
      tag: tag.trim() || 'User Task',
      risk: 'Normal'
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
      <form onSubmit={handleSubmit} className="glass-card p-6 rounded-3xl max-w-md w-full space-y-4 border border-emerald-500/30">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white">Create New Task</h3>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-white">✕</button>
        </div>

        <div className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 mb-1">Task Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Implement Room Index on saleId"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Description / Snippet</label>
            <textarea
              rows="3"
              value={snippet}
              onChange={(e) => setSnippet(e.target.value)}
              placeholder="Details of the task..."
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
            ></textarea>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
              >
                <option value="arch">Architecture</option>
                <option value="security">Security</option>
                <option value="perf">Performance</option>
                <option value="size">App Size</option>
                <option value="test">Testing</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-400 mb-1">Initial Column</label>
              <select
                value={column}
                onChange={(e) => setColumn(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
              >
                <option value="backlog">Backlog</option>
                <option value="inProgress">In Progress</option>
                <option value="verified">Verified</option>
                <option value="recommendations">Recommendations</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Badge Tag</label>
            <input
              type="text"
              value={tag}
              onChange={(e) => setTag(e.target.value)}
              placeholder="e.g. Room v5, KMP"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
            />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 text-xs font-mono transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition shadow-lg shadow-emerald-600/30"
          >
            Create Task
          </button>
        </div>
      </form>
    </div>
  );
}
