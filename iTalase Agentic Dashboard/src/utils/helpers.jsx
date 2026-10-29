const CATEGORY_STYLES = {
  arch: 'bg-indigo-500/15 text-indigo-300 border-indigo-400/25',
  security: 'bg-rose-500/15 text-rose-300 border-rose-400/25',
  size: 'bg-emerald-500/15 text-emerald-300 border-emerald-400/25',
  perf: 'bg-amber-500/15 text-amber-300 border-amber-400/25',
  test: 'bg-purple-500/15 text-purple-300 border-purple-400/25',
};

const COL_ORDER = ['recommendations', 'backlog', 'inProgress', 'verified'];

const getApiUrl = (endpoint) => {
  if (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) {
    return endpoint;
  }
  return 'http://localhost:3300' + endpoint;
};

function formatTimeInfo(timestampStr) {
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
    relative = `${Math.floor(diffSec / 60)}m lalu`;
  } else if (diffSec < 86400) {
    relative = `${Math.floor(diffSec / 3600)}j lalu`;
  } else {
    relative = `${Math.floor(diffSec / 86400)}h lalu`;
  }

  return {
    displayTime: wibTime,
    localFull: wibFull,
    utc: utcStr,
    relative: relative
  };
}

function TimeHoverCell({ timestamp, label = "Timestamp Eksekusi Tool" }) {
  const { useState, useMemo, useRef } = React;
  const [isOpen, setIsOpen] = useState(false);
  const [coords, setCoords] = useState({ x: 0, y: 0 });
  const triggerRef = useRef(null);
  const info = useMemo(() => formatTimeInfo(timestamp), [timestamp]);

  const handleMouseEnter = () => {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    setCoords({
      x: Math.max(12, Math.min(window.innerWidth - 300, rect.left)),
      y: rect.bottom + 6
    });
    setIsOpen(true);
  };

  return (
    <div 
      ref={triggerRef}
      className="relative inline-block"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={() => setIsOpen(false)}
    >
      <span className="font-mono text-[11px] text-slate-300 hover:text-white cursor-help underline decoration-slate-600 decoration-dotted underline-offset-2 transition-colors">
        {info.displayTime}
      </span>

      {isOpen && typeof document !== 'undefined' && ReactDOM.createPortal(
        <div 
          style={{
            position: 'fixed',
            top: `${coords.y}px`,
            left: `${coords.x}px`,
            width: '280px',
            zIndex: 999999,
            pointerEvents: 'none'
          }}
          className="p-3.5 rounded-2xl bg-[#141824]/95 border border-white/15 shadow-2xl backdrop-blur-2xl text-left animate-fadeIn text-white text-[11px]"
        >
          <div className="font-semibold text-slate-200 border-b border-white/[0.08] pb-1.5 mb-2 flex items-center justify-between">
            <span>{label}</span>
            <span className="text-[10px] font-mono text-orange-400 bg-orange-950/60 px-1.5 py-0.2 rounded border border-orange-800/40 font-bold">WIB</span>
          </div>
          <div className="space-y-1 font-mono text-slate-300">
            <div className="flex justify-between"><span className="text-slate-400">Lokal (WIB):</span> <span className="text-orange-400 font-bold">{info.localFull}</span></div>
            <div className="flex justify-between"><span className="text-slate-400">Server (UTC):</span> <span>{info.utc}</span></div>
            <div className="flex justify-between"><span className="text-slate-400">Status:</span> <span className="text-amber-400">{info.relative}</span></div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.CATEGORY_STYLES = CATEGORY_STYLES;
  window.COL_ORDER = COL_ORDER;
  window.getApiUrl = getApiUrl;
  window.formatTimeInfo = formatTimeInfo;
  window.TimeHoverCell = TimeHoverCell;
}
