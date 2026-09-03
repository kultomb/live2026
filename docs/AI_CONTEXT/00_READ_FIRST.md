# 00 READ FIRST — AI AGENT OPERATING INSTRUCTIONS

## 1. PROJECT OVERVIEW
This project is a **Professional Mobile Live Production and Streaming Application for Android**.
It enables real-time HDMI capture via USB UVC HDMI capture cards, seamless switching between HDMI and Android cameras (Front, Rear Main, Wide, Telephoto), real-time audio mixing, hardware video encoding via MediaCodec (H.264/AAC), and live streaming via RTMP/RTMPS with dynamic network recovery.

## 2. GOVERNING LAWS FOR ALL AI ACTIONS
1. **UNDERSTAND BEFORE MODIFYING**: Always inspect existing architecture before making changes.
2. **REALITY RULE**: Never promise or fake support. Always perform runtime capability detection (`SUPPORTED`, `PARTIALLY_SUPPORTED`, `NOT_SUPPORTED`).
3. **SINGLE OWNER RULE**: Each subsystem has ONE and ONLY ONE owner component (e.g., `UsbCaptureManager`, `CameraManager`, `StreamManager`, `LiveSessionManager`).
4. **NO DUPLICATION**: Never create duplicate pipelines, workers, activities, camera sessions, or streaming connections.
5. **STATE BEFORE ACTION**: UI must listen to state from `LiveSessionManager`; button text is never the source of truth.
6. **NO SILENT SUPPRESSION**: Errors must be structured, logged, and surfaced appropriately.

## 3. PROJECT MEMORY INDEX
Before attempting any architectural change or feature implementation, you MUST consult:

| Document | Purpose |
|---|---|
| [`AI_CURRENT_CONTEXT.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/AI_CURRENT_CONTEXT.md) | Current phase, active tasks, immediate next steps |
| [`PROJECT_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/PROJECT_ARCHITECTURE.md) | Layered architecture, single owner rule, session state machine |
| [`PRODUCT_SPECIFICATION.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/PRODUCT_SPECIFICATION.md) | Full feature scope, target hardware, performance goals |
| [`REQUIREMENTS.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/REQUIREMENTS.md) | Functional & non-functional constraints |
| [`MEDIA_PIPELINE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/MEDIA_PIPELINE.md) | Frame router, composition, encoder, muxer, PTS/DTS sync |
| [`CAMERA_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/CAMERA_ARCHITECTURE.md) | Camera2 API management, multi-lens exposure, hot-switching |
| [`USB_UVC_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/USB_UVC_ARCHITECTURE.md) | USB host permission, libuvc/UVC pipeline, ring buffers |
| [`AUDIO_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/AUDIO_ARCHITECTURE.md) | AudioRecord, USB Audio, audio routing, sample rate & mixer |
| [`STREAMING_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/STREAMING_ARCHITECTURE.md) | RTMP/RTMPS engine, keyframes, security, profile configs |
| [`NETWORK_ARCHITECTURE.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/NETWORK_ARCHITECTURE.md) | Bandwidth monitoring, exponential backoff, connection recovery |
| [`DEVICE_COMPATIBILITY.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/DEVICE_COMPATIBILITY.md) | Device HAL matrices, capability detection rules |
| [`DESIGN_SYSTEM.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/DESIGN_SYSTEM.md) | UI design system tokens, layout hierarchy, state colors |
| [`UX_FLOW.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/UX_FLOW.md) | Screen navigation, live stream workflow, user interaction flows |
| [`BUG_PREVENTION_RULES.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/BUG_PREVENTION_RULES.md) | Strict guidelines on threading, memory, memory leaks, resources |
| [`DEBUG_PROTOCOL.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/DEBUG_PROTOCOL.md) | 9-step bug diagnosis protocol |
| [`ARCHITECTURE_DECISIONS.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/ARCHITECTURE_DECISIONS.md) | ADR records explaining key design choices |
| [`VALIDATION_RULES.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/VALIDATION_RULES.md) | Build & runtime verification criteria |
| [`KNOWN_ISSUES.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/KNOWN_ISSUES.md) | Workarounds for platform/device HAL issues |
| [`CHANGELOG_AI.md`](file:///c:/Users/CMD/Desktop/LIVE%202026/docs/AI_CONTEXT/CHANGELOG_AI.md) | Architectural edit audit log |

## 4. PHASE EXECUTION RULES
1. Never jump directly to coding features.
2. Complete each Phase in the Roadmap, update `AI_CURRENT_CONTEXT.md`, obtain validation, then proceed.
