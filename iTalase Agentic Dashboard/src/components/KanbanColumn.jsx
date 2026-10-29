function KanbanColumn({ title, count, cards, columnKey, dropActive, onDragOver, onDrop, onDragStart, onCardClick, onQuickMove }) {
  const COL_ORDER = window.COL_ORDER || ['recommendations', 'backlog', 'inProgress', 'verified'];
  const colIdx = COL_ORDER.indexOf(columnKey);
  const prevCol = colIdx > 0 ? COL_ORDER[colIdx - 1] : null;
  const nextCol = colIdx < COL_ORDER.length - 1 ? COL_ORDER[colIdx + 1] : null;

  return (
    <div 
      onDragOver={onDragOver}
      onDrop={onDrop}
      className={`glass-surface rounded-[24px] flex flex-col h-[580px] transition-colors ${dropActive ? 'drop-active' : ''}`}
    >
      {/* Column Header */}
      <div className="px-4 py-3.5 border-b border-white/[0.08] flex items-center justify-between">
        <h4 className="text-xs font-bold text-white font-sans tracking-tight flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-[#FF5E0E]"></span>
          <span>{title}</span>
        </h4>
        <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-white/10 text-slate-300 border border-white/10">
          {count}
        </span>
      </div>

      {/* Scrollable Column Body */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2.5">
        {cards.length === 0 ? (
          <div className="h-full flex items-center justify-center p-4 text-center text-xs font-mono text-slate-500 border border-dashed border-white/10 rounded-2xl">
            Drag task ke sini
          </div>
        ) : (
          cards.map((card) => {
            const cardKey = card.key || card.id;
            return (
              <KanbanCardItem
                key={cardKey}
                card={card}
                onDragStart={onDragStart}
                onCardClick={onCardClick}
                prevCol={prevCol}
                nextCol={nextCol}
                onQuickMove={onQuickMove}
              />
            );
          })
        )}
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.KanbanColumn = KanbanColumn;
}
