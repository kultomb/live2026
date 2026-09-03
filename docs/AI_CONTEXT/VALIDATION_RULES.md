# VALIDATION RULES & BENCHMARKS

## 1. COMPILATION & BUILD VERIFICATION CHECKLIST
Every phase completion MUST pass the following checks:
- [ ] `./gradlew assembleDebug` compiles with 0 errors.
- [ ] `./gradlew test` passes all unit tests.
- [ ] Android Manifest contains required permissions & Foreground Service declarations.
- [ ] ProGuard / R8 rules retain MediaCodec and native USB/UVC entry points.

## 2. RUNTIME BENCHMARK TARGETS
During hardware validation on physical test devices:

| Metric | Target Benchmark | Critical Threshold (Failure) |
|---|---|---|
| **Preview Frame Rate** | 30.0 / 60.0 fps | < 24.0 fps |
| **End-to-End Latency** | < 150 ms | > 300 ms |
| **Encoder Drop Rate** | < 0.5 % | > 2.0 % |
| **A/V Sync Drift** | < 30 ms over 1 hr | > 100 ms |
| **Memory Growth Rate** | Flat (0 MB/hr after initial allocation) | > 10 MB/hr (Memory Leak) |
| **CPU Pressure** | < 35% overall system load | > 70% |
| **Thermal Threshold** | Moderate / Normal | Emergency Thermal Throttle |

## 3. CORE SCENARIO VERIFICATION SUITE
Before declaring a release candidate "DONE", run the 25 core test scenarios outlined in Section 62 of `PROJECT_CONSTITUTION.md` (USB hot-plug, stream reconnect, camera switch, thermal pressure, backgrounding).
