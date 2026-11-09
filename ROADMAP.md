# Roadmap

> What is working, what is currently under discussion for future phases, and what has been discarded (with reasoning). For what has **already been discovered** (platform, protocols, data formats), see [`DISCOVERIES.md`](DISCOVERIES.md).
>
> *Language:* 🇺🇸 **English** · [🇧🇷 Português](ROADMAP.pt-BR.md)

---

## Current Status (2026-10-29)

- 🎯 **Phase 10 Completed (2026-10-29)** — The boot disclaimer warning has been eliminated from the boot flow via a 1-instruction smali patch in `DaLauncher.apk`. The intermediate Eco screen was reduced to ~1 s via a second patch (Phase 10.1, same day). CarPlay projects immediately, navigation functions normally, 0 crashes. Details in [`docs/en/fase10-deploy-2026-10-29/README.md`](docs/en/fase10-deploy-2026-10-29/README.md). DaLauncher rollback package verified during the same deployment.
- **AutoOk v0.5.4 APK** retained in `/system/app/` as a safety net. Suppresses disclaimer on cold boot (AUDIO 5s) and LP0 resume. Zero false taps, zero interference with Bluetooth CarPlay calls.
- v0.5.4 completed 4 rapid iterations resolving: CarPlay mute bug (AlarmManager + loose needle from v0.4.0); EPERM regression; two Activities triggered during LP0 resume (`PRECEDING_STARTUP` logo screen vs `NORMAL_STARTUP` actual disclaimer); malformed `-v brief` flag; Android 4.2.2 bug failing to combine `-b A -b B`.
- Permanent setuid `su` installed in `/system/xbin/` — identified as KingoSU (no-op broadcast); candidate replacement with Chainfire SR1 documented in [`docs/en/raio-x/08-github-landscape-2026-10-27.md §4`](docs/en/raio-x/08-github-landscape-2026-10-27.md).
- EvilValet OTA deployment verified, including the infinite loop fix in `build.py` (renaming `SwUpdate2.txt → .done`).
- Rollback packages (AutoOk + DaLauncher) built and fully verified in-car on 2026-10-29.

**The original project goal is fully achieved.** None of the exploratory roadmap items below compromise the stability of the active deployment. Everything listed is optional/exploratory.

---

## Short-Term (Candidates for Next Phases)

Prioritized by value vs. cost:

### Phase 9 — Rear Camera via Official Platform API

- **Current state:** AutoOk detects reverse gear engagement via logcat polling (`logcat` child process spawned via `su`). Functional, but logcat child processes can terminate on LP0 sleep.
- **Official API pathway:** The platform Camera service exposes `ICameraAPServiceReverseSignalListener` via AIDL binder IPC, and PowerManager emits `ACTION_RESUME_CAMERA` + `ACTION_RESUME_COMPLETED_CAMERA` dedicated broadcasts. In addition, `com.mitsubishielectric.android.action.camera.EXIT_REAR_WIDE_CAMERA` signals reverse exit.
- **Requirements:** Confirm required permissions (`VEHICLE_RW`), bind to `ICameraAPService`, register reverse signal listener, and remove logcat polling. Low risk as existing flow serves as fallback.
- See [`docs/en/raio-x/01-apis-aidl.md`](docs/en/raio-x/01-apis-aidl.md) and [`docs/en/raio-x/02-broadcasts.md`](docs/en/raio-x/02-broadcasts.md).

### Validating `DISCLAIMER_COMPLETE` as Canonical "Warning Dismissed" Signal

- The action `com.mitsubishielectric.ada.app.dalauncher.action.DISCLAIMER_COMPLETE` is broadcast by the launcher when `DisclaimerFragment` finishes.
- Emitted equivalently on human tap and automated tap.
- Allows internal logging to verify tap success without screen rescans, and allows other background tools to react without polling.
- See [`docs/en/raio-x/02-broadcasts.md`](docs/en/raio-x/02-broadcasts.md).

### Dumping the True `com.honda.displayaudio.whitelist.apk`

- The carrier APK for the whitelist XML resides at `/system/vendor/etc/honda/com.honda.displayaudio.whitelist.apk` (14 KB according to textual scan), but binary bytes were omitted in the initial raw dump.
- Useful to verify the exact on-disk schema and explore Route B (custom carrier entry) if user-friendly names in AppManager are ever desired.
- See [`docs/en/whitelist-mitsubishi-2026-10-19.md §2.3`](docs/en/whitelist-mitsubishi-2026-10-19.md).

### Experimenting with `persist.sys.keyoff_reboot=1`

- This system property dictates whether the unit triggers a **cold boot** vs. **suspend/resume (LP0)** on key-off.
- Default is suspend (which is why `AUDIO 5s` is required to force cold boot). Setting to `1` would theoretically make every ignition event a clean cold boot.
- Eliminates LP0 state complexity, but boot time increases slightly (3–5s vs instantaneous).
- See [`docs/en/raio-x/04-system-properties.md`](docs/en/raio-x/04-system-properties.md).

---

## Medium-Term (Promising Targets)

### Diagnostic APKs — Hidden Menus & Service PINs

- 4 Diagnostic APKs (`DealerDiag`, `DeveloperDiag`, `PreShipmentDiag`, `LetDiag`) and `DiagApService.apk`.
- Classified 91 DealerDiag screens (all PIN-free).
- Potential for unlocking hidden developer screens and engineering diagnostics.
- See [`docs/en/raio-x/06-diag-apks.md`](docs/en/raio-x/06-diag-apks.md).

### Steering Wheel Controls API

- `IStrgSwApService` + 8 listeners handle steering wheel key events (`IStrgSwRotaryKeyEventApServiceListener`, `IStrgSwMotionMenuApServiceListener`, etc.).
- Exposes raw hardware keypress events for mapping custom shortcut actions.
- See [`docs/en/raio-x/01-apis-aidl.md`](docs/en/raio-x/01-apis-aidl.md).

### Comprehensive Vehicle Telemetry

- `IVehicleInfoManagerApService` exposes 37 listeners covering current gear, speed, IG voltage, steering wheel angle, VIN, active FOB key ID, and reverse parking sensors.
- Provides deep vehicle telemetry for custom car status dashboards.
- See [`docs/en/raio-x/01-apis-aidl.md`](docs/en/raio-x/01-apis-aidl.md).

### Audio Pipeline (Audio Ducking Bypass)

- `AV.apk` runs as `android.uid.phone` with access to `IAvApService` (14 callback interfaces).
- Decoupling this allows third-party navigation apps to output audio without unwanted system muting.
- See [`docs/en/raio-x/01-apis-aidl.md`](docs/en/raio-x/01-apis-aidl.md).

---

## Long-Term (High Cost / High Risk)

- **Ghidra reverse engineering of `/system/bin/daupdater`**: Exact byte-level modeling of OTA verification in the recovery binary.
- **Hardware HALs in `/system/vendor/lib/*.so`**: Display, audio, CAN bus HALs. (Hardware brick risk).
- **Bootloader modification**: Unrecoverable without eMMC desoldering. Out of scope.

---

## Discarded Routes (Documented)

| Route | Reason for Abandonment |
|---|---|
| White-listing in Mitsubishi Whitelist | Whitelist is UX-only for installations. Does not affect LP0. |
| SuperSU as mediator | Replaces `su` with an interactive GUI prompt, breaking automated headless scripts. |
| Xposed Framework | High risk of bootloop; zero precedent on Andromeda Tegra 3. |

---

## Tooling & Documentation Improvements

- Translate remaining historical phase narratives into English.
- Complete documentation mirroring between Portuguese and English.
- Continuous Integration tests for python OTA builders.
- Detailed compatibility matrix for non-BR trims (US, EU, JP, AU).

For contributing guidelines, see [`CONTRIBUTING.md`](CONTRIBUTING.md).
