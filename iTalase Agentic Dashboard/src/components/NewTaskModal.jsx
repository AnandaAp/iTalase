function NewTaskModal({ onClose, onSubmit }) {
  const { useState } = React;
  const [title, setTitle] = useState('');
  const [snippet, setSnippet] = useState('');
  const [category, setCategory] = useState('arch');
  const [column, setColumn] = useState('recommendations');
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
      tag: tag.trim() || 'Custom Task',
      risk: 'Normal',
      createdAt: new Date().toISOString()
    });

    onClose();
  };

  const modalContent = (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xl animate-fadeIn"
      onClick={onClose}
    >
      <form 
        onSubmit={handleSubmit}
        onClick={(e) => e.stopPropagation()}
        className="glass-surface p-6 rounded-3xl max-w-md w-full space-y-4 text-white shadow-2xl"
      >
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#FF5E0E]"></span>
            <span>Tambah Task Baru</span>
          </h3>
          <button 
            type="button" 
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 flex items-center justify-center text-xs"
          >
            ✕
          </button>
        </div>

        <div className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-300 mb-1 font-mono">Judul Task *</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Contoh: Implementasi SQLite WAL Index"
              className="w-full bg-black/50 border border-white/15 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-[#FF5E0E]"
            />
          </div>

          <div>
            <label className="block text-slate-300 mb-1 font-mono">Deskripsi / Snippet</label>
            <textarea
              rows="3"
              value={snippet}
              onChange={(e) => setSnippet(e.target.value)}
              placeholder="Uraian singkat arsitektur atau instruksi task..."
              className="w-full bg-black/50 border border-white/15 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-[#FF5E0E]"
            ></textarea>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-300 mb-1 font-mono">Kategori</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full bg-[#171B26] border border-white/15 rounded-xl px-2.5 py-2 text-white focus:outline-none"
              >
                <option value="arch">Architecture</option>
                <option value="security">Security</option>
                <option value="perf">Performance</option>
                <option value="size">Size</option>
                <option value="test">Testing</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-300 mb-1 font-mono">Kolom Awal</label>
              <select
                value={column}
                onChange={(e) => setColumn(e.target.value)}
                className="w-full bg-[#171B26] border border-white/15 rounded-xl px-2.5 py-2 text-white focus:outline-none"
              >
                <option value="recommendations">Rekomendasi</option>
                <option value="backlog">Backlog</option>
                <option value="inProgress">In Progress</option>
                <option value="verified">Verified</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-300 mb-1 font-mono">Tag</label>
            <input
              type="text"
              value={tag}
              onChange={(e) => setTag(e.target.value)}
              placeholder="Contoh: Room v5, KMP"
              className="w-full bg-black/50 border border-white/15 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
            />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 text-xs font-mono"
          >
            Batal
          </button>
          <button
            type="submit"
            className="px-5 py-2 rounded-full bg-white hover:bg-slate-100 text-black text-xs font-bold"
          >
            Simpan
          </button>
        </div>
      </form>
    </div>
  );

  return typeof document !== 'undefined' ? ReactDOM.createPortal(modalContent, document.body) : null;
}

if (typeof window !== 'undefined') {
  window.NewTaskModal = NewTaskModal;
}
