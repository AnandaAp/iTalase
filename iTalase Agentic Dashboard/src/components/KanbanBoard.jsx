function KanbanBoard({
  isFullView = false,
  searchQuery,
  setSearchQuery,
  selectedCategory,
  setSelectedCategory,
  filteredKanban,
  dropTargetCol,
  handleDragOver,
  handleDrop,
  handleDragStart,
  setSelectedTask,
  moveCardInState,
  setIsNewTaskOpen
}) {
  const COL_ORDER = window.COL_ORDER || ['recommendations', 'backlog', 'inProgress', 'verified'];
  const colNames = {
    recommendations: 'Rekomendasi AI',
    backlog: 'Backlog',
    inProgress: 'Sedang Dikerjakan',
    verified: 'Teruji & Terverifikasi'
  };

  return (
    <div className="space-y-4">
      {/* Top Header Banner if Full Kanban View */}
      {isFullView && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 glass-surface p-4 rounded-3xl">
          <div>
            <h2 className="text-lg font-bold text-white">Autonomous Sprint Kanban Board</h2>
            <p className="text-xs text-slate-400">Triage rekomendasi, backlog, active sprint, dan task yang telah teruji JaCoCo</p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsNewTaskOpen(true)}
              className="btn-tactile px-5 py-2 rounded-full bg-white hover:bg-slate-100 text-black font-bold text-xs"
            >
              + Tambah Task Baru
            </button>
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-1">
        <div className="relative w-full sm:w-80">
          <input
            type="text"
            placeholder={isFullView ? "Filter kata kunci / ID..." : "Cari task, ID, kata kunci..."}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-[#161922] border border-white/10 focus:border-[#FF5E0E] rounded-full px-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none transition font-sans"
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} className="absolute right-3.5 top-2.5 text-xs text-slate-400 hover:text-white">✕</button>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          {['ALL', 'ARCH', 'SECURITY', 'PERF', 'SIZE', 'TEST'].map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`btn-tactile px-4 py-1.5 rounded-full text-xs font-mono font-bold transition-all ${
                selectedCategory === cat
                  ? 'bg-white text-black shadow-md'
                  : 'bg-white/[0.05] hover:bg-white/[0.10] text-slate-300 border border-white/10'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* 4 Liquid Glass Columns Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {COL_ORDER.map((colKey) => (
          <KanbanColumn
            key={colKey}
            title={colNames[colKey]}
            count={(filteredKanban && filteredKanban[colKey]?.length) || 0}
            cards={(filteredKanban && filteredKanban[colKey]) || []}
            columnKey={colKey}
            dropActive={dropTargetCol === colKey}
            onDragOver={(e) => handleDragOver(e, colKey)}
            onDrop={(e) => handleDrop(e, colKey)}
            onDragStart={handleDragStart}
            onCardClick={setSelectedTask}
            onQuickMove={moveCardInState}
          />
        ))}
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.KanbanBoard = KanbanBoard;
}
