# DESIGN SYSTEM & STYLING SPECIFICATION

## 1. DESIGN PHILOSOPHY
A sleek, high-contrast, professional broadcast UI inspired by professional hardware switcher control surfaces (e.g. Blackmagic ATEM, Teradek, Roland).
- **Core Focus**: Maximum readability under bright daylight and dark stage environments.
- **Orientation**: Optimized primary layout for **Landscape Mode** (with adaptive Portrait fallback).
- **No Decorative Overhead**: Avoid heavy neon glow effects, unnecessary gradients, or low-contrast text.

## 2. COLOR PALETTE (DARK BROADCAST THEME)

| Palette Role | Hex Code | Purpose |
|---|---|---|
| **Background Primary** | `#0F1216` | Main studio canvas background |
| **Background Surface** | `#181C22` | Cards, control panels, bottom dock |
| **Background Elev. 2**| `#222730` | Button default backgrounds, dialogs |
| **Border / Divider** | `#2E3542` | Subtle panel boundaries |
| **Text Primary** | `#F0F4F8` | High legibility text & headings |
| **Text Secondary** | `#94A3B8` | Subtitles, labels, secondary metadata |
| **Text Disabled** | `#475569` | Disabled button state text |
| **Live Red (Active)** | `#E11D48` | Active LIVE state badge, Stop Live button |
| **Ready Green** | `#10B981` | System ready badge, healthy connection |
| **Warning Amber** | `#F59E0B` | Reconnecting state, dropped frame alert |
| **Error / Critical** | `#EF4444` | Hardware failure, stream error state |
| **Accent Blue** | `#3B82F6` | Selected video source outline, active tab |

## 3. TYPOGRAPHY
- **Primary Font**: Inter / Roboto
- **Monospace Font (Metrics & Timers)**: JetBrains Mono / Roboto Mono (for non-shifting FPS, bitrate, and duration counters).
- **Hierarchy**:
  - `Display / Live Timer`: 24sp Bold Monospace
  - `Header H1`: 20sp SemiBold
  - `Header H2`: 16sp Medium
  - `Body / Button`: 14sp Regular / Medium
  - `Caption / Metric Label`: 11sp Regular Monospace

## 4. UI COMPONENT STATES
Every interactive control MUST clearly indicate state:
- `NORMAL`: Slate container background (`#222730`), primary text.
- `ACTIVE / SELECTED`: Accent blue border (`#3B82F6`), subtle background tint.
- `PRESSED`: Scaled down 0.98x, darker container background.
- `DISABLED`: 40% opacity, non-clickable.
- `LIVE ACTIVE`: Pulsing red indicator dot (`#E11D48`).

## 5. ACCESSIBILITY & ACCURATE INDICATORS
- Never rely on color alone for critical status. Always combine color with a text label and icon (e.g. `[● LIVE]`, `[▲ RECONNECTING]`, `[✖ OFFLINE]`).
