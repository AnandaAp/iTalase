function QualityVelocityCard({ state, chartHover, setChartHover }) {
  if (!state || !state.kpis) return null;

  return (
    <div className="glass-surface rounded-[32px] p-6 space-y-3 flex-1 flex flex-col justify-between relative">
      <div className="flex items-center justify-between">
        <span className="text-sm font-bold text-white flex items-center gap-1.5">
          Test Velocity &amp; Quality Gate 💡
        </span>
        <span className="text-xs font-mono text-emerald-400 font-bold bg-emerald-500/10 px-2 py-0.5 rounded-full">
          121 Passed (100%)
        </span>
      </div>

      {/* Smooth Multi-Line Wave Graph */}
      <div className="relative py-2">
        <svg viewBox="0 0 300 110" className="w-full h-28 overflow-visible">
          <defs>
            <linearGradient id="yellowLine" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#D8F634" />
              <stop offset="100%" stopColor="#FFA03A" />
            </linearGradient>
            <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#FFA03A" stopOpacity="0.22" />
              <stop offset="100%" stopColor="#FFA03A" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* 75% Gate Benchmark Line */}
          <line x1="0" y1="28" x2="300" y2="28" stroke="rgba(255,94,14,0.30)" strokeDasharray="3 3" />
          <text x="6" y="24" fill="#FFA03A" fontSize="8" fontFamily="monospace" opacity="0.85">GATE ≥ 75%</text>

          {/* Mid guideline */}
          <line x1="0" y1="65" x2="300" y2="65" stroke="rgba(255,255,255,0.06)" strokeDasharray="3 3" />

          {/* Yellow area glow under curve */}
          <path 
            d="M 10,76 C 50,78 75,62 105,52 C 145,40 165,36 195,30 C 230,22 255,14 280,14 L 280,95 L 10,95 Z" 
            fill="url(#areaGradient)" 
          />

          {/* Sage background wave (Test volume: 42 -> 78 -> 104 -> 121) */}
          <path 
            d="M 10,85 C 50,86 75,70 105,60 C 145,48 165,44 195,36 C 230,28 255,20 280,20" 
            fill="none" 
            stroke="#84CC16" 
            strokeWidth="2.5" 
            strokeOpacity="0.75" 
          />

          {/* Luminous yellow foreground wave (JaCoCo Coverage: 58% -> 68% -> 74% -> 78.58%) */}
          <path 
            d="M 10,76 C 50,78 75,62 105,52 C 145,40 165,36 195,30 C 230,22 255,14 280,14" 
            fill="none" 
            stroke="url(#yellowLine)" 
            strokeWidth="3.5" 
          />

          {/* Interactive Points on timeline */}
          <circle 
            cx="20" cy="74" r="3.5" 
            className="cursor-pointer fill-[#84CC16] hover:r-5 transition-all opacity-80"
            onMouseEnter={() => setChartHover({ text: 'Sprint 1: 58.4% Coverage (42 unit tests)' })}
            onMouseLeave={() => setChartHover(null)}
          />
          <circle 
            cx="105" cy="52" r="3.5" 
            className="cursor-pointer fill-[#D8F634] hover:r-5 transition-all opacity-90"
            onMouseEnter={() => setChartHover({ text: 'Sprint 2: 68.2% Coverage (78 unit tests)' })}
            onMouseLeave={() => setChartHover(null)}
          />
          <circle 
            cx="195" cy="30" r="4" 
            className="cursor-pointer fill-[#FFA03A] hover:r-6 transition-all"
            onMouseEnter={() => setChartHover({ text: 'Sprint 3: 74.5% Coverage (104 tests • Pre-Gate)' })}
            onMouseLeave={() => setChartHover(null)}
          />
          <circle 
            cx="280" cy="14" r="5" 
            className="cursor-pointer fill-[#FFA03A] hover:r-7 transition-all shadow-[0_0_12px_#FFA03A]"
            stroke="#FFFFFF"
            strokeWidth="1.5"
            onMouseEnter={() => setChartHover({ text: `Current: ${state.kpis.testCoverage.instruction} Coverage (121 passed) • GATE PASS` })}
            onMouseLeave={() => setChartHover(null)}
          />

          {/* Peak Bubble Badge at Current */}
          <rect x="242" y="-14" width="48" height="20" rx="10" fill="#000000" stroke="#FFA03A" strokeWidth="1.2" />
          <text x="266" y="0" fill="#FFFFFF" fontSize="10" fontWeight="bold" textAnchor="middle" fontFamily="monospace">
            {state.kpis.testCoverage.instruction}
          </text>
        </svg>

        {/* Chart Tooltip */}
        {chartHover && (
          <div className="absolute top-0 left-1/2 -translate-x-1/2 bg-black/90 border border-orange-400 text-orange-200 text-[10px] font-mono px-2 py-1 rounded-lg pointer-events-none shadow-xl z-20">
            {chartHover.text}
          </div>
        )}

        <div className="flex justify-between text-[10px] font-mono text-slate-500 pt-2 px-1">
          <span>Sprint 1</span>
          <span>Sprint 2</span>
          <span>Sprint 3</span>
          <span className="text-[#FFA03A] font-bold">Current</span>
        </div>
      </div>
    </div>
  );
}

if (typeof window !== 'undefined') {
  window.QualityVelocityCard = QualityVelocityCard;
}
