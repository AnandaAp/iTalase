// React Theme & Tailwind Configuration Module
const TAILWIND_CONFIG = {
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      colors: {
        dribbble: {
          bg: '#111317',
          dock: 'rgba(20, 23, 31, 0.88)',
          card: 'rgba(23, 27, 37, 0.72)',
          cardHover: 'rgba(30, 35, 48, 0.85)',
          surface: 'rgba(255, 255, 255, 0.04)',
          border: 'rgba(255, 255, 255, 0.08)',
          borderTop: 'rgba(255, 255, 255, 0.22)',
          orange: '#FF5E0E',
          orangeGlow: '#FFA03A',
          lime: '#D8F634',
          sage: '#84CC16',
          emerald: '#10B981',
          textMuted: '#8E95A5',
          textSubtle: '#5F677A',
        }
      }
    }
  }
};

if (typeof window !== 'undefined') {
  window.tailwind = window.tailwind || {};
  window.tailwind.config = TAILWIND_CONFIG;
}

/**
 * DashboardStyleProvider Component
 * Encapsulates design tokens, CSS custom properties, and UI styles inside React JSX.
 */
function DashboardStyleProvider() {
  return (
    <style>{`
      :root {
        --spring-ease: cubic-bezier(0.16, 1, 0.3, 1);
        --font-sans: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
        --font-mono: 'JetBrains Mono', monospace;
        --color-orange: #FF5E0E;
        --color-orange-glow: #FFA03A;
        --color-emerald: #10B981;
      }
      
      body {
        background-color: #0c0e14;
        color: #F8FAFC;
        font-family: var(--font-sans);
        overflow-x: hidden;
        min-height: 100vh;
        -webkit-font-smoothing: antialiased;
        background-image: 
          radial-gradient(circle at 55% 40%, rgba(255, 107, 44, 0.34) 0%, rgba(234, 88, 12, 0.16) 38%, transparent 68%),
          radial-gradient(circle at 86% 28%, rgba(245, 158, 11, 0.26) 0%, transparent 52%),
          radial-gradient(circle at 16% 18%, rgba(99, 102, 241, 0.14) 0%, transparent 45%),
          radial-gradient(circle at 50% 92%, rgba(16, 185, 129, 0.08) 0%, transparent 55%);
        background-attachment: fixed;
      }

      /* High-Fidelity Translucent Frosted Glass Cards (Liquid Glass UI) */
      .glass-surface {
        background: rgba(22, 26, 36, 0.52);
        backdrop-filter: blur(36px) saturate(190%);
        -webkit-backdrop-filter: blur(36px) saturate(190%);
        border: 1px solid rgba(255, 255, 255, 0.09);
        border-top: 1px solid rgba(255, 255, 255, 0.28);
        box-shadow: 
          0 24px 50px -12px rgba(0, 0, 0, 0.75),
          inset 0 1px 1px 0 rgba(255, 255, 255, 0.18);
        transition: transform 0.25s var(--spring-ease), box-shadow 0.25s var(--spring-ease), border-color 0.25s ease, background 0.25s ease;
      }

      .glass-surface:hover {
        background: rgba(28, 33, 46, 0.65);
        border-color: rgba(255, 255, 255, 0.15);
        border-top-color: rgba(255, 255, 255, 0.38);
        box-shadow: 
          0 28px 60px -12px rgba(0, 0, 0, 0.85),
          inset 0 1px 2px rgba(255, 255, 255, 0.24);
      }

      /* Hero Liquid Pill Metrics Strip */
      .hero-pill-strip {
        background: rgba(16, 20, 28, 0.65);
        backdrop-filter: blur(32px) saturate(210%);
        -webkit-backdrop-filter: blur(32px) saturate(210%);
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-top: 1px solid rgba(255, 255, 255, 0.30);
        box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.8), inset 0 1px 1px rgba(255, 255, 255, 0.20);
      }

      /* Active Highlighted Metric Pill (Translucent Frosted Orange Glass) */
      .active-metric-pill {
        background: linear-gradient(135deg, rgba(255, 94, 14, 0.94) 0%, rgba(235, 87, 87, 0.88) 100%);
        box-shadow: 0 12px 32px -4px rgba(255, 94, 14, 0.65), inset 0 1px 2px rgba(255, 255, 255, 0.45);
        border: 1px solid rgba(255, 255, 255, 0.38);
        backdrop-filter: blur(20px);
      }

      /* Kanban Drag & Drop Styles with Frosted Translucency */
      .kanban-card {
        background: rgba(28, 33, 46, 0.50);
        backdrop-filter: blur(24px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-top: 1px solid rgba(255, 255, 255, 0.18);
        border-radius: 1.15rem;
        transition: transform 0.20s var(--spring-ease), box-shadow 0.20s var(--spring-ease), border-color 0.20s ease, background 0.20s ease;
        cursor: grab;
      }
      .kanban-card:hover {
        transform: translateY(-2px);
        background: rgba(35, 41, 58, 0.68);
        border-color: rgba(255, 255, 255, 0.15);
        border-top-color: rgba(255, 255, 255, 0.32);
        box-shadow: 0 14px 32px -8px rgba(0, 0, 0, 0.7);
      }
      .kanban-card:active {
        cursor: grabbing;
        transform: scale(0.98);
      }
      .kanban-card.dragging {
        opacity: 0.35;
        transform: scale(0.96);
      }
      .drop-active {
        border-color: #FF5E0E !important;
        background: rgba(255, 94, 14, 0.08) !important;
      }

      /* Button feedback */
      .btn-tactile {
        transition: transform 0.15s var(--spring-ease), filter 0.15s ease, background 0.15s ease;
      }
      .btn-tactile:active {
        transform: scale(0.96);
      }

      /* Shimmer effect for progress bar */
      @keyframes shimmerProgress {
        0% { background-position: -200% 0; }
        100% { background-position: 200% 0; }
      }
      .progress-shimmer {
        background: linear-gradient(90deg, #FF5E0E 0%, #FFA03A 40%, #FFE27D 50%, #FFA03A 60%, #FF5E0E 100%);
        background-size: 200% 100%;
        animation: shimmerProgress 3s linear infinite;
      }

      /* View transition animation */
      @keyframes viewSpringEnter {
        0% { opacity: 0; transform: translateY(10px) scale(0.995); }
        100% { opacity: 1; transform: translateY(0) scale(1); }
      }
      .view-enter {
        animation: viewSpringEnter 0.28s var(--spring-ease) forwards;
      }

      /* Custom Minimal Scrollbar */
      ::-webkit-scrollbar { width: 5px; height: 5px; }
      ::-webkit-scrollbar-track { background: transparent; }
      ::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.12); border-radius: 999px; }
      ::-webkit-scrollbar-thumb:hover { background: rgba(255, 255, 255, 0.25); }

      @keyframes fadeInSlide {
        from { opacity: 0; transform: translateY(6px); }
        to { opacity: 1; transform: translateY(0); }
      }
      .animate-fadeIn {
        animation: fadeInSlide 0.20s var(--spring-ease) forwards;
      }
    `}</style>
  );
}

if (typeof window !== 'undefined') {
  window.DashboardStyleProvider = DashboardStyleProvider;
}
