function KanbanCardItem({ card, onDragStart, onCardClick, prevCol, nextCol, onQuickMove }) {
  const cardKey = card.key || card.id;

  return (
    <div
      key={cardKey}
      draggable
      onDragStart={(e) => onDragStart(e, cardKey)}
      onClick={() => onCardClick(card)}
      className="kanban-card p-3.5 space-y-2"
    >
      <div className="flex items-center justify-between text-[10px] font-mono">
        <span className="font-bold text-[#FFA03A]">{card.id}</span>
        <span className={`px-2 py-0.5 rounded-full border text-[10px] font-medium ${
          (window.CATEGORY_STYLES && window.CATEGORY_STYLES[card.category]) || 'bg-white/10 text-slate-300 border-white/15'
        }`}>
          {card.categoryLabel || card.category}
        </span>
      </div>

      <h5 className="text-xs font-bold text-white leading-snug line-clamp-2">{card.title}</h5>
      <p className="text-[11px] text-slate-300 line-clamp-2 leading-relaxed">{card.snippet}</p>

      <div className="flex items-center justify-between pt-2 border-t border-white/[0.08]">
        <span className="text-[10px] font-mono text-slate-400 truncate max-w-[120px]">{card.risk || card.tag}</span>
        
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {prevCol && (
            <button
              onClick={() => onQuickMove(cardKey, prevCol)}
              className="w-5 h-5 rounded-lg bg-white/10 hover:bg-white hover:text-black text-slate-300 text-[10px] flex items-center justify-center transition"
              title={`Pindah ke ${prevCol}`}
            >
              ←
            </button>
          )}
          {nextCol && (
            <button
              onClick={() => onQuickMove(cardKey, nextCol)}
              className="w-5 h-5 rounded-lg bg-white/10 hover:bg-white hover:text-black text-slate-300 text-[10px] flex items-center justify-center transition"
              title={`Pindah ke ${nextCol}`}
            >
              →
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.KanbanCardItem = KanbanCardItem;
}
