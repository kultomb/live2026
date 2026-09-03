# UX FLOW & SCREEN NAVIGATION

## 1. APPLICATION SCREEN STRUCTURE

```
                    +-----------------------+
                    |   DIAGNOSTICS SCREEN  |
                    | (Hardware Inspection) |
                    +-----------------------+
                                ^
                                |
 +-----------------------+      v      +-----------------------+
 |  STREAM SETUP SCREEN  | <---------> |   LIVE STUDIO SCREEN  |
 | (URL, Stream Key, ABR)|             | (Main Production UI)  |
 +-----------------------+             +-----------------------+
                                                |
                                                v
                                       +-----------------------+
                                       |  CAMERA & SCENE SWITCH |
                                       |  (Overlay & PiP Panel)|
                                       +-----------------------+
```

## 2. USER WORKFLOW PHASES

### Phase A: Setup & Hardware Inspection
1. User launches app -> App opens **Live Studio Screen**.
2. If USB HDMI capture card is plugged in:
   - System prompts for USB Permission -> User taps "Allow".
   - Status badge turns green: `HDMI CONNECTED (1080p30)`.
3. User opens **Diagnostics Screen** to verify device hardware capability score, available camera lenses, thermal state, and codec profiles.

### Phase B: Stream Configuration
1. User taps "Stream Settings".
2. Selects destination: YouTube Live, Facebook Live, or Custom RTMP.
3. Enters / selects saved Stream Key (encrypted).
4. Chooses profile (e.g., 1080p 30fps @ 4.5 Mbps).
5. Returns to Live Studio Screen.

### Phase C: Live Streaming & Production Controls
1. User taps **GO LIVE** button.
   - Button disables, state shifts to `STARTING`.
   - Handshake with RTMP server -> state shifts to `LIVE`.
   - Button becomes red **STOP LIVE**.
2. **During Live Production**:
   - User switches video sources via bottom dock: [HDMI] [FRONT] [REAR WIDE] [REAR TELE] [PiP MODE].
   - Seamless crossfade occurs without stream disconnect.
   - User monitors Audio VU meters, active bitrate graph, dropped frame counter, thermal gauge.
3. User taps **STOP LIVE** -> Confirmation dialog -> session safely stops.

## 3. ERROR & RECOVERY USER EXPERIENCE
- **USB Unplugged during Live Stream**: Visual alert overlay "USB Disconnected. Switched to Rear Camera". App continues streaming without crash.
- **Network Drop**: Studio UI displays amber pulsing alert "Network Connection Lost - Reconnecting (Attempt 2/5)". Stream engine auto-reconnects in background.
- **Low Thermal / Storage Warning**: Subtle warning pill appears on top bar.
