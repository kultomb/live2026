# NETWORK ARCHITECTURE & RECOVERY

## 1. NETWORK MONITORING & CONNECTION TYPES
`NetworkManager` tracks active interfaces via `ConnectivityManager` and `NetworkCapabilities`:
- Wi-Fi
- 5G / 4G Cellular
- Ethernet (via USB-C hub)

Metrics gathered continuously:
- **Round Trip Time (RTT)**: Ping/socket write latency.
- **Upload Bandwidth**: Measured send byte throughput per second.
- **Buffer Queue Backlog**: Pending unsent RTMP packets in memory.

## 2. ADAPTIVE BITRATE & FRAME DROPPING STRATEGY
When upload bandwidth drops below target stream bitrate:
1. **Level 1 (Minor Congestion)**: Buffer queue depth increases (< 1.5 seconds). Continue normal delivery.
2. **Level 2 (Moderate Congestion)**: Buffer queue reaches 1.5 - 3.0 seconds. Drop non-reference video frames (P-frames) or drop oldest un-sent video frames from queue. **NEVER drop audio frames or H.264 SPS/PPS/IDR keyframes**.
3. **Level 3 (Severe Congestion)**: Buffer queue exceeds 3.0 seconds. Dynamically request `MediaCodec` to adjust video encoding bitrate (`Bundle` with `KEY_VIDEO_BITRATE`).
4. **Level 4 (Network Failure)**: Socket write fails or times out (5 seconds). Transition session state to `RECONNECTING`.

## 3. EXPONENTIAL BACKOFF RECONNECT ALGORITHM
When connection breaks during a live stream:
- State switches to `RECONNECTING`. UI shows visual reconnect overlay with countdown.
- **Retry Schedule**:
  - Attempt 1: Immediate (0s delay)
  - Attempt 2: 2s delay
  - Attempt 3: 4s delay
  - Attempt 4: 8s delay
  - Attempt 5: 16s delay
- If Attempt 5 fails, switch state to `FAILED` with `STREAM_INTERRUPTED` diagnostic code and prompt user.

## 4. FUTURE MULTI-PATH / BONDING ABSTRACTION
`NetworkManager` defines an interface `NetworkTransport` separating socket operations from transport selection. This allows future insertion of a Multipath / Network Bonding Engine (combining Wi-Fi + Cellular) without altering the RTMP stream layer or application logic.
