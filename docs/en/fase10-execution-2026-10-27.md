# Phase 10 — Execution Checklist (Deploy Day)

> **✅ DEPLOY EXECUTED ON 2026-10-29.** Final results documented in
> [`fase10-deploy-2026-10-29/README.md`](fase10-deploy-2026-10-29/README.md).
> This document is preserved as the execution runbook followed in-car.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../docs/fase10-execucao-2026-10-27.md)

---

## 0. Preconditions Before Heading to Vehicle

Verify strictly in this order. Every condition must pass; otherwise **postpone deployment**:

- [ ] **AutoOk v0.5.4 remains stable** — zero false taps or LP0 issues since 2026-10-20.
- [ ] **Two physically distinct FAT32 flash drives**, clearly labeled:
  - Drive A — **ROLLBACK** (contains `tooling/ota-build/output-dalauncher-restore/<ts>/0040/SwUpdate.mdt`)
  - Drive B — **INSTALL** (contains `tooling/ota-build/output-dalauncher-fase10/<ts>/0040/SwUpdate.mdt`)
- [ ] **Fresh builds of both packages**:
  ```bash
  bash tooling/fase10/rebuild-dalauncher.sh
  python3 tooling/ota-build/build-dalauncher-fase10.py
  python3 tooling/ota-build/build-dalauncher-restore.py
  ```
- [ ] **Flash drives prepared** (specifying the output directory explicitly):
  ```bash
  sudo tooling/scripts/prepare-pendrive-install.sh /dev/sdX tooling/ota-build/output-dalauncher-fase10
  sudo tooling/scripts/prepare-pendrive-rollback.sh /dev/sdY tooling/ota-build/output-dalauncher-restore
  ```
- [ ] **SHA-256 verification passed** for both drives.

---

## 1. Materials to Bring to Car

- Flash Drive A (ROLLBACK) — clearly labeled.
- Flash Drive B (INSTALL) — clearly labeled.
- Notepad / Phone for taking timestamped notes and photos of recovery screens.

---

## 2. In-Car Execution Sequence

### §2.1 First: Validate Rollback Package (No-Op Test)
*Goal: Prove safety net works on the vehicle before introducing modified bytecode.*

1. Insert **Drive A (ROLLBACK)** into the front USB-A port.
2. Turn key to **ACC / ON** (engine off).
3. Hold **AUDIO** button on head unit for **5 seconds** until "Do you want to restart?" dialog appears.
4. Confirm restart.
5. Watch "Software update → Copying..." progress bar (0% → 100%).
6. Head unit reboots automatically.
7. Verify on USB drive: `ROLLBACK-RESTORED.OK` present.

### §2.2 Second: Deploy Phase 10 Smali Patch
1. Unplug Drive A; insert **Drive B (INSTALL)**.
2. Hold **AUDIO** button for 5 seconds; confirm restart.
3. System copies package and updates `/system/vendor/app/DaLauncher.apk`.
4. Removes `/system/vendor/app/DaLauncher.odex`.
5. Head unit reboots. Unplug flash drive when head unit starts booting.
6. Verify: `FASE10-ODEX-REMOVED.OK` present on drive.
7. Observe startup: **Warning disclaimer screen never appears.** System transitions cleanly to Eco screen and CarPlay.

---

## 3. Verification & Acceptance Criteria

- [ ] Startup warning screen is 100% bypassed.
- [ ] CarPlay projects automatically when connected.
- [ ] Factory reverse camera displays immediately on engaging reverse gear.
- [ ] Steering wheel audio controls continue functioning.
- [ ] Radio / Bluetooth playback remains stable.
